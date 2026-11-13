package br.edu.fatec.ecommerce.modulo3.pedido.api.dto;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TransacaoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.EnderecoEntregaSnapshot;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.ItemPedidoSnapshot;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.SnapshotFinanceiro;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representação HTTP completa de um pedido (consulta pelo cliente ou administrador).
 *
 * <p><b>Papel Arquitetural:</b> DTO de saída. Expõe a memória de cálculo transparente exigida pelo Princípio IV
 * (subtotal bruto, descontos, frete ou frete grátis, total líquido, cupom e promoções aplicadas).
 *
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial
 * @param status status atual
 * @param statusDescricao descrição amigável do status
 * @param metodoPagamento método de pagamento
 * @param pagamento situação da cobrança (pode ser nulo)
 * @param snapshotFinanceiro memória de cálculo congelada
 * @param entrega dados de entrega congelados
 * @param itens linhas congeladas
 * @param cupomUtilizado cupom aplicado (pode ser nulo)
 * @param promocoesAplicadas nomes das promoções que deram desconto
 * @param motivoCancelamento motivo de cancelamento/recusa (pode ser nulo)
 * @param criadoEm instante de criação
 * @param atualizadoEm instante da última transição
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record PedidoResponseDTO(
        UUID pedidoId,
        String numeroPedido,
        StatusPedido status,
        String statusDescricao,
        MetodoPagamento metodoPagamento,
        PagamentoDTO pagamento,
        SnapshotFinanceiroDTO snapshotFinanceiro,
        EntregaDTO entrega,
        List<ItemPedidoDTO> itens,
        String cupomUtilizado,
        List<String> promocoesAplicadas,
        String motivoCancelamento,
        Instant criadoEm,
        Instant atualizadoEm
) {

    /**
     * Converte o agregado em DTO.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> de mapeamento na fronteira HTTP.</p>
     *
     * @param pedido agregado
     * @param transacao transação financeira do pedido (pode ser nula)
     * @return DTO de saída
     */
    public static PedidoResponseDTO de(Pedido pedido, TransacaoPagamento transacao) {
        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getStatus(),
                pedido.getStatus().getDescricaoAmigavel(),
                pedido.getMetodoPagamento(),
                transacao == null ? null : PagamentoDTO.de(transacao),
                SnapshotFinanceiroDTO.de(pedido.getFinanceiro()),
                EntregaDTO.de(pedido),
                pedido.getItens().stream().map(ItemPedidoDTO::de).toList(),
                pedido.getCupomUtilizado(),
                pedido.getPromocoesAplicadas(),
                pedido.getMotivoCancelamento(),
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm());
    }

    /**
     * Memória de cálculo financeira.
     *
     * @param subtotalBruto soma dos itens sem desconto
     * @param totalDescontos descontos de promoções e cupons
     * @param valorFrete frete cobrado
     * @param freteGratis se a política de frete grátis foi aplicada
     * @param valorTotalLiquido total pago/a pagar
     */
    public record SnapshotFinanceiroDTO(BigDecimal subtotalBruto, BigDecimal totalDescontos, BigDecimal valorFrete,
                                        boolean freteGratis, BigDecimal valorTotalLiquido) {

        /**
         * Converte o Value Object em DTO.
         *
         * @param financeiro snapshot financeiro
         * @return DTO
         */
        public static SnapshotFinanceiroDTO de(SnapshotFinanceiro financeiro) {
            return new SnapshotFinanceiroDTO(financeiro.getSubtotalBruto(), financeiro.getTotalDescontos(),
                    financeiro.getValorFrete(), financeiro.isFreteGratis(), financeiro.getValorTotalLiquido());
        }
    }

    /**
     * Dados de entrega.
     *
     * @param modalidade modalidade de frete
     * @param nomeModalidade nome de exibição da modalidade
     * @param prazoDiasUteis prazo em dias úteis
     * @param codigoRastreio código de rastreio (após o despacho)
     * @param enderecoDestino endereço formatado
     * @param cep CEP de destino
     */
    public record EntregaDTO(ModalidadeFrete modalidade, String nomeModalidade, int prazoDiasUteis,
                             String codigoRastreio, String enderecoDestino, String cep) {

        /**
         * Extrai os dados de entrega do pedido.
         *
         * @param pedido agregado
         * @return DTO
         */
        public static EntregaDTO de(Pedido pedido) {
            EnderecoEntregaSnapshot endereco = pedido.getEnderecoEntrega();
            return new EntregaDTO(pedido.getModalidadeFrete(), pedido.getModalidadeFrete().getNomeExibicao(),
                    pedido.getPrazoEntregaDias(), pedido.getCodigoRastreio(), endereco.getResumoFormatado(),
                    endereco.getEnderecoCep());
        }
    }

    /**
     * Linha do pedido.
     *
     * @param sku SKU
     * @param nome nome congelado
     * @param tipoItem PRODUTO ou COMBO_KIT
     * @param quantidade quantidade
     * @param precoUnitarioTabela preço unitário sem desconto
     * @param descontoUnitario desconto rateado por unidade
     * @param subtotalLiquido subtotal da linha com desconto
     */
    public record ItemPedidoDTO(String sku, String nome, String tipoItem, int quantidade,
                                BigDecimal precoUnitarioTabela, BigDecimal descontoUnitario, BigDecimal subtotalLiquido) {

        /**
         * Converte a linha congelada em DTO.
         *
         * @param item linha do pedido
         * @return DTO
         */
        public static ItemPedidoDTO de(ItemPedidoSnapshot item) {
            return new ItemPedidoDTO(item.getSku(), item.getNome(), item.getTipoItem(), item.getQuantidade(),
                    item.getPrecoUnitarioTabela(), item.getDescontoUnitario(), item.getSubtotalLiquido());
        }
    }

    /**
     * Situação da cobrança.
     *
     * @param identificadorTransacao identificador no gateway
     * @param statusTransacao status da transação
     * @param comprovante chave PIX, linha digitável ou mensagem de autorização
     */
    public record PagamentoDTO(String identificadorTransacao, String statusTransacao, String comprovante) {

        /**
         * Converte a transação em DTO.
         *
         * @param transacao transação financeira
         * @return DTO
         */
        public static PagamentoDTO de(TransacaoPagamento transacao) {
            return new PagamentoDTO(transacao.getIdentificadorGateway(), transacao.getStatus().name(),
                    transacao.getComprovanteOuQrCode());
        }
    }
}
