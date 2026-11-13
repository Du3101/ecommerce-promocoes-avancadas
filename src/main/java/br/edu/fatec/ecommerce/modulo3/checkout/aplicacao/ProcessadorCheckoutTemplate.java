package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import br.edu.fatec.ecommerce.modulo1.integracao.VisaoItemCatalogoDTO;
import br.edu.fatec.ecommerce.modulo2.integracao.ItemResumoCarrinhoDTO;
import br.edu.fatec.ecommerce.modulo3.checkout.api.dto.CheckoutResponseDTO;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.ConflitoCheckoutException;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.eventos.PedidoCriadoEvent;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RequisicaoCobrancaDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TransacaoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.StatusTransacaoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.EnderecoEntregaSnapshot;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.SnapshotFinanceiro;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Esqueleto invariante do fechamento de compra.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Template Method</i> com <i>Hook Methods</i>.
 * {@link #processarCheckout(ContextoCheckout)} é {@code final}: nenhuma especialização consegue pular a reserva de
 * estoque, trocar a ordem das etapas ou esquecer de notificar o cliente. O que varia entre meios de pagamento fica
 * em pontos de extensão explícitos:
 * <ul>
 *   <li>{@link #tratarResultadoCobranca(ContextoCheckout)} — <b>abstrato</b>: cartão (online) confirma ou recusa na
 *       hora; PIX/boleto (assíncrono) deixam o pedido aguardando o webhook;</li>
 *   <li>{@link #validarDadosPagamento(ContextoCheckout)} — <b>hook</b> (padrão: nada a validar);</li>
 *   <li>{@link #deveEsvaziarCarrinho(ContextoCheckout)} — <b>hook</b> (padrão: sim).</li>
 * </ul>
 *
 * <p><b>Ordem das etapas</b>: identificar cliente → reaproveitar checkout repetido → validar pagamento → validar
 * cliente e endereço → ler carrinho → validar preços vigentes → cotar frete → reservar estoque → criar pedido →
 * emitir cobrança → publicar {@code PedidoCriadoEvent} → tratar resultado → esvaziar carrinho → resposta.
 *
 * <p>A transação é aberta pela {@code CheckoutFacade}: se qualquer etapa falhar, tudo é desfeito, inclusive a
 * reserva de estoque (Módulo 1) e o pedido.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public abstract class ProcessadorCheckoutTemplate {

    /** Recursos compartilhados pelas etapas comuns. */
    protected final RecursosCheckout recursos;

    /**
     * Construtor com os recursos comuns.
     *
     * @param recursos colaboradores do checkout
     */
    protected ProcessadorCheckoutTemplate(RecursosCheckout recursos) {
        this.recursos = recursos;
    }

    /**
     * Forma de liquidação atendida por esta especialização (usada pela Facade para escolhê-la).
     *
     * @return tipo de liquidação
     */
    public abstract TipoLiquidacao tipoLiquidacaoAtendida();

    /**
     * MÉTODO TEMPLATE: executa o checkout completo na ordem invariante.
     *
     * @param ctx contexto da execução
     * @return resposta do checkout
     */
    public final CheckoutResponseDTO processarCheckout(ContextoCheckout ctx) {
        // 1. Quem está comprando (IDs dos Módulos 1 e 2) e chaves da operação
        identificarClienteEChaves(ctx);

        // 2. Idempotência do checkout: mesma chave do cliente => devolve o mesmo pedido
        Optional<Pedido> existente = recursos.pedidoRepository().findByChaveOperacaoEstoque(ctx.chaveOperacaoEstoque);
        if (existente.isPresent()) {
            Pedido pedido = existente.get();
            TransacaoPagamento transacao = recursos.transacaoRepository()
                    .findFirstByPedidoIdOrderByProcessadoEmDesc(pedido.getId()).orElse(null);
            return CheckoutResponseDTO.existente(pedido, transacao);
        }

        // 3. Hook: validações específicas do meio de pagamento
        validarDadosPagamento(ctx);
        // 4. Cliente e endereço (Módulo 1)
        validarClienteEEndereco(ctx);
        // 5. Carrinho congelado com preços e descontos (Módulo 2)
        obterResumoCarrinho(ctx);
        // 6. O preço mudou no catálogo desde que o item foi para o carrinho?
        validarPrecosVigentes(ctx);
        // 7. Frete da modalidade escolhida (Adapters + política de frete grátis)
        calcularFrete(ctx);
        // 8. Reserva atômica de estoque (Módulo 1) — lança 409 se faltar estoque
        reservarEstoque(ctx);
        // 9. Pedido com snapshot imutável (Builder)
        criarPedido(ctx);
        // 10. Cobrança no provedor da família escolhida (Abstract Factory + Adapter)
        emitirCobranca(ctx);
        // 11. Observer: cliente recebe resumo e instruções de pagamento
        recursos.barramentoEventos().publicar(PedidoCriadoEvent.de(ctx.pedido, ctx.instrucoesPagamento));
        // 12. Ponto variável: confirmar/recusar na hora (cartão) ou aguardar webhook (PIX/boleto)
        tratarResultadoCobranca(ctx);
        // 13. Hook: esvaziar o carrinho (não esvazia se o cartão foi recusado)
        if (deveEsvaziarCarrinho(ctx)) {
            recursos.portaCarrinho().esvaziarCarrinhoAposCheckout(ctx.identificadorCarrinho, null);
        }
        // 14. Resposta
        return CheckoutResponseDTO.novo(ctx.pedido, ctx.respostaCobranca, ctx.instrucoesPagamento);
    }

    /**
     * Etapa variável obrigatória: o que fazer com a resposta do gateway.
     *
     * <p>Papel no Design Pattern: <b>método abstrato</b> do Template Method.</p>
     *
     * @param ctx contexto (contém {@code pedido} e {@code respostaCobranca})
     */
    protected abstract void tratarResultadoCobranca(ContextoCheckout ctx);

    /**
     * Hook: validações de dados específicas do meio de pagamento, antes de qualquer efeito colateral.
     *
     * <p>Papel no Design Pattern: <b>Hook Method</b> com implementação padrão vazia.</p>
     *
     * @param ctx contexto
     */
    protected void validarDadosPagamento(ContextoCheckout ctx) {
        // Padrão: meios assíncronos não exigem dados adicionais do cliente.
    }

    /**
     * Hook: decide se o carrinho deve ser esvaziado ao final.
     *
     * <p>Papel no Design Pattern: <b>Hook Method</b> com implementação padrão.</p>
     *
     * @param ctx contexto
     * @return {@code true} por padrão
     */
    protected boolean deveEsvaziarCarrinho(ContextoCheckout ctx) {
        return true;
    }

    /**
     * Resolve os IDs do cliente nos Módulos 1 e 2 e define número do pedido e chave da operação.
     *
     * <p>Chave da operação: {@code CHK-<clienteId>-<chaveDoCliente>} quando o cliente envia uma chave de
     * idempotência (escopo por cliente, para que dois clientes não colidam), senão {@code CHK-<numeroPedido>}.</p>
     *
     * @param ctx contexto
     */
    private void identificarClienteEChaves(ContextoCheckout ctx) {
        ctx.clienteId = recursos.portaCliente().resolverClienteIdPorEmailUsuario(ctx.emailUsuario);
        ctx.identificadorCarrinho = recursos.portaCarrinho().resolverIdentificadorCarrinhoPorEmail(ctx.emailUsuario);
        ctx.numeroPedido = recursos.geradorNumeroPedido().gerar();
        String chaveCliente = ctx.requisicao.chaveIdempotenciaCliente();
        ctx.chaveOperacaoEstoque = chaveCliente != null && !chaveCliente.isBlank()
                ? "CHK-" + ctx.clienteId + "-" + chaveCliente
                : "CHK-" + ctx.numeroPedido;
    }

    /**
     * Valida o cliente e obtém o endereço de entrega completo (Módulo 1).
     *
     * @param ctx contexto
     * @throws RegraNegocioModulo3Exception (HTTP 400) se não houver endereço de entrega selecionado
     */
    private void validarClienteEEndereco(ContextoCheckout ctx) {
        ctx.cliente = recursos.portaCliente().consultarClienteParaCompra(ctx.clienteId);
        ctx.endereco = recursos.portaCliente().consultarEnderecoEntregaSelecionado(ctx.clienteId)
                .orElseThrow(() -> new RegraNegocioModulo3Exception(
                        "Nenhum endereço de entrega válido selecionado. Cadastre e selecione um endereço antes de finalizar a compra."));
    }

    /**
     * Lê o carrinho congelado do Módulo 2.
     *
     * @param ctx contexto
     * @throws RegraNegocioModulo3Exception (HTTP 400) se o carrinho estiver vazio
     */
    private void obterResumoCarrinho(ContextoCheckout ctx) {
        ctx.carrinho = recursos.portaCarrinho().obterResumoCarrinhoParaCheckout(ctx.identificadorCarrinho, null);
        if (ctx.carrinho.estaVazio()) {
            throw new RegraNegocioModulo3Exception("O carrinho está vazio. Adicione itens antes de finalizar a compra.");
        }
    }

    /**
     * Compara o preço de cada item no carrinho com o preço vigente no catálogo.
     *
     * <p>Objetivo Funcional: impedir que o cliente pague um preço antigo (ou seja surpreendido por um preço novo)
     * quando o catálogo mudou depois que o item foi colocado no carrinho.</p>
     *
     * @param ctx contexto
     * @throws ConflitoCheckoutException (HTTP 409) se algum preço mudou ou item foi desativado
     */
    private void validarPrecosVigentes(ContextoCheckout ctx) {
        for (ItemResumoCarrinhoDTO item : ctx.carrinho.itens()) {
            VisaoItemCatalogoDTO vigente = recursos.portaCliente().consultarItemPorSku(item.sku());
            if (!vigente.ativo()) {
                throw new ConflitoCheckoutException("O item " + item.sku() + " não está mais disponível para venda. "
                        + "Remova-o do carrinho para continuar.");
            }
            if (vigente.precoVigente().compareTo(item.precoUnitario()) != 0) {
                throw new ConflitoCheckoutException(String.format(
                        "O preço de '%s' mudou de R$ %s para R$ %s desde que foi adicionado ao carrinho. "
                                + "Revise o carrinho antes de finalizar.",
                        item.nome(), item.precoUnitario(), vigente.precoVigente()));
            }
        }
    }

    /**
     * Cota o frete da modalidade escolhida para o CEP do endereço selecionado.
     *
     * @param ctx contexto
     */
    private void calcularFrete(ContextoCheckout ctx) {
        ctx.cotacaoFrete = recursos.servicoFrete().cotarModalidade(
                ctx.requisicao.modalidadeFrete(), ctx.endereco.cep(), ctx.carrinho.pesoTotalGramas(),
                ctx.carrinho.totalLiquido());
    }

    /**
     * Reserva atomicamente o estoque no Módulo 1.
     *
     * @param ctx contexto
     */
    private void reservarEstoque(ContextoCheckout ctx) {
        recursos.portaEstoque().reservarItensParaCheckout(
                ctx.chaveOperacaoEstoque, ctx.clienteId, ctx.carrinho.quantidadePorSku());
    }

    /**
     * Monta e persiste o pedido com o snapshot comercial imutável (padrão Builder).
     *
     * @param ctx contexto
     */
    private void criarPedido(ContextoCheckout ctx) {
        // 1. Valores totais oficiais (Módulo 2) + frete
        SnapshotFinanceiro financeiro = SnapshotFinanceiro.criarComPisoZero(
                ctx.carrinho.subtotalBruto(), ctx.carrinho.totalDescontos(),
                ctx.cotacaoFrete.valorCobrado(), ctx.cotacaoFrete.freteGratisAplicado());

        // 2. Endereço congelado
        EnderecoEntregaSnapshot endereco = new EnderecoEntregaSnapshot(
                ctx.endereco.apelido(), ctx.endereco.cep(), ctx.endereco.logradouro(), ctx.endereco.numero(),
                ctx.endereco.complemento(), ctx.endereco.bairro(), ctx.endereco.cidade(), ctx.endereco.uf());

        // 3. Builder com dados de cliente, entrega, pagamento, reserva e benefícios
        Pedido.Builder builder = Pedido.builder()
                .comNumero(ctx.numeroPedido)
                .paraCliente(ctx.clienteId, ctx.emailUsuario, ctx.cliente.nomeCompleto())
                .comPagamento(ctx.requisicao.metodoPagamento())
                .comEntrega(ctx.requisicao.modalidadeFrete(), ctx.cotacaoFrete.prazoDiasUteis(), endereco)
                .comReservaEstoque(ctx.chaveOperacaoEstoque)
                .comFinanceiro(financeiro)
                .comBeneficios(ctx.carrinho.cupomUtilizado(), ctx.carrinho.nomesPromocoesAplicadas());

        // 4. Linhas com desconto rateado proporcionalmente
        List<ItemResumoCarrinhoDTO> itens = ctx.carrinho.itens();
        List<BigDecimal> descontos = CalculadoraRateioDesconto.calcularDescontoUnitario(
                itens.stream().map(i -> new CalculadoraRateioDesconto.LinhaRateio(i.precoUnitario(), i.quantidade())).toList(),
                ctx.carrinho.totalDescontos());
        for (int i = 0; i < itens.size(); i++) {
            ItemResumoCarrinhoDTO item = itens.get(i);
            builder.adicionarItem(item.sku(), item.nome(), item.tipoItem(), item.precoUnitario(),
                    item.quantidade(), descontos.get(i), item.pesoTotalGramas());
        }

        // 5. Persiste (usa a instância gerenciada devolvida pelo repositório)
        ctx.pedido = recursos.pedidoRepository().save(builder.construir());
    }

    /**
     * Emite a cobrança no provedor da família escolhida e registra a transação financeira.
     *
     * @param ctx contexto
     */
    private void emitirCobranca(ContextoCheckout ctx) {
        Pedido pedido = ctx.pedido;
        BigDecimal valor = pedido.getFinanceiro().getValorTotalLiquido();
        RequisicaoCobrancaDTO requisicao = new RequisicaoCobrancaDTO(
                pedido.getId(), pedido.getNumeroPedido(), pedido.getMetodoPagamento(), valor,
                "COB-" + pedido.getNumeroPedido(), ctx.cliente.nomeCompleto(), ctx.cliente.cpf(),
                ctx.requisicao.numeroCartaoFicticio(), ctx.requisicao.titularCartao());

        // 1. Abstract Factory: gateway e formatador da MESMA família
        ctx.respostaCobranca = ctx.fabricaPagamento.criarGateway().processarCobranca(requisicao);
        ctx.instrucoesPagamento = ctx.fabricaPagamento.criarFormatadorInstrucoes().formatar(ctx.respostaCobranca, valor);

        // 2. Registro financeiro sempre nasce PENDENTE; a confirmação/recusa muda o status depois
        recursos.transacaoRepository().save(new TransacaoPagamento(
                null, pedido, requisicao.chaveIdempotencia(), pedido.getMetodoPagamento(), valor,
                StatusTransacaoPagamento.PENDENTE, ctx.respostaCobranca.identificadorGateway(),
                ctx.respostaCobranca.comprovante()));
    }
}
