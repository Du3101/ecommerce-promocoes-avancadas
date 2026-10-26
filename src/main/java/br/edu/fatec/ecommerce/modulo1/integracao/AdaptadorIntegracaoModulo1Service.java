package br.edu.fatec.ecommerce.modulo1.integracao;

import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoConsultaCatalogo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.estoque.aplicacao.ServicoReservaEstoqueConcorrente;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.Cliente;
import br.edu.fatec.ecommerce.modulo1.identidade.dominio.OpcaoEnderecoEntrega;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação concreta das portas de integração do Módulo 1 ({@link PortaConsultaCatalogoCliente}
 * e {@link PortaComandoReservaEstoque}) para consumo pelos Módulos 2 e 3 (FR-013).
 *
 * <p>Garante que os demais integrantes do grupo interajam apenas com DTOs imutáveis e contratos
 * bem definidos, sem acoplamento às entidades JPA internas do Módulo 1.</p>
 */
@Service
public class AdaptadorIntegracaoModulo1Service
    implements PortaConsultaCatalogoCliente, PortaComandoReservaEstoque {

    private final ServicoConsultaCatalogo servicoConsultaCatalogo;
    private final ServicoIdentidadeECliente servicoIdentidade;
    private final ServicoReservaEstoqueConcorrente servicoReservaEstoque;

    /**
     * Construtor com injeção explícita dos serviços de aplicação do Módulo 1.
     *
     * <p>Objetivo Funcional: Conectar as portas de integração aos casos de uso de Catálogo,
     * Identidade e Estoque.</p>
     * <p>Papel no Design Pattern: Facade/Adapter de fronteira modular.</p>
     *
     * @param servicoConsultaCatalogo serviço de leitura do catálogo
     * @param servicoIdentidade serviço de identidade e clientes
     * @param servicoReservaEstoque serviço transacional de reservas
     */
    public AdaptadorIntegracaoModulo1Service(
        ServicoConsultaCatalogo servicoConsultaCatalogo,
        ServicoIdentidadeECliente servicoIdentidade,
        ServicoReservaEstoqueConcorrente servicoReservaEstoque
    ) {
        this.servicoConsultaCatalogo = servicoConsultaCatalogo;
        this.servicoIdentidade = servicoIdentidade;
        this.servicoReservaEstoque = servicoReservaEstoque;
    }

    /**
     * Consulta qualquer item do catálogo (variação ou combo) pelo SKU e converte para {@link VisaoItemCatalogoDTO}.
     *
     * <p>Objetivo Funcional: Fornecer dados uniformes ao Carrinho (Módulo 2) e ao Frete (Módulo 3).</p>
     * <p>Papel no Design Pattern: Consome polimorficamente {@link ItemCatalogoComponent} (<b>Composite</b>).</p>
     *
     * @param sku código SKU
     * @return DTO {@link VisaoItemCatalogoDTO}
     */
    @Override
    @Transactional(readOnly = true)
    public VisaoItemCatalogoDTO consultarItemPorSku(String sku) {
        ItemCatalogoComponent item = this.servicoConsultaCatalogo.buscarItemComercializavelPorSku(sku);
        DimensoesFisicas dim = item.obterDimensoesFisicas();
        int disponibilidade = this.servicoConsultaCatalogo.calcularDisponibilidadeEstoque(item);

        return new VisaoItemCatalogoDTO(
            item.obterId(),
            item.obterSku(),
            item.obterNomeExibicao(),
            item.calcularPrecoVigente(),
            item.obterPesoTotalGramas(),
            dim.obterAlturaCm(),
            dim.obterLarguraCm(),
            dim.obterProfundidadeCm(),
            item.obterCategoriaPrincipalId(),
            item.estaAtivo(),
            item.eComboKit(),
            disponibilidade,
            item.expandirSkusFisicosComMultiplicador()
        );
    }

    /**
     * Verifica recursivamente se o item de SKU informado pertence à categoria alvo ou a qualquer subcategoria.
     *
     * <p>Objetivo Funcional: Apoiar elegibilidade de promoções por categoria no Módulo 2.</p>
     * <p>Papel no Design Pattern: Consome a operação recursiva do <b>Composite</b> de categorias.</p>
     *
     * @param sku código SKU do item ou combo
     * @param categoriaAlvoId UUID da categoria alvo
     * @return {@code true} se pertencer à categoria ou subárvore
     */
    @Override
    @Transactional(readOnly = true)
    public boolean itemPertenceACategoriaOuSubcategoria(String sku, UUID categoriaAlvoId) {
        ItemCatalogoComponent item = this.servicoConsultaCatalogo.buscarItemComercializavelPorSku(sku);
        Set<UUID> subarvore = this.servicoConsultaCatalogo.obterIdsSubarvoreCategoria(categoriaAlvoId);
        return subarvore.contains(item.obterCategoriaPrincipalId());
    }

    /**
     * Consulta a elegibilidade do cliente e sua opção de endereço de entrega (real ou Null Object).
     *
     * <p>Objetivo Funcional: Informar elegibilidade de primeira compra e destino de entrega sem {@code null}.</p>
     * <p>Papel no Design Pattern: Traduz o retorno de {@link OpcaoEnderecoEntrega} (<b>Null Object</b>) para DTO.</p>
     *
     * @param clienteId UUID do cliente
     * @return DTO {@link VisaoClienteElegibilidadeDTO}
     */
    @Override
    @Transactional(readOnly = true)
    public VisaoClienteElegibilidadeDTO consultarClienteParaCompra(UUID clienteId) {
        Cliente cliente = this.servicoIdentidade.obterClientePorId(clienteId);
        OpcaoEnderecoEntrega endereco = cliente.obterEnderecoSelecionado();

        return new VisaoClienteElegibilidadeDTO(
            cliente.obterId(),
            cliente.obterNomeCompleto(),
            cliente.obterCpf(),
            cliente.jaConcluiuPrimeiraCompra(),
            endereco.possuiEnderecoValido(),
            endereco.obterCepDestino(),
            endereco.obterUfDestino(),
            endereco.obterResumoFormatado()
        );
    }

    /**
     * Executa reserva atômica de estoque para o Checkout (Módulo 3) retornando {@link ResumoReservaEstoqueDTO}.
     *
     * <p>Objetivo Funcional: Reservar SKUs físicos ou combos com prevenção de concorrência.</p>
     * <p>Papel no Design Pattern: Adapta o retorno de {@link ReservaEstoque} para o contrato de fronteira.</p>
     *
     * @param chaveOperacaoPedido chave única da operação
     * @param clienteId UUID do cliente
     * @param quantidadePorSku mapa de SKUs e quantidades
     * @return DTO {@link ResumoReservaEstoqueDTO}
     */
    @Override
    @Transactional
    public ResumoReservaEstoqueDTO reservarItensParaCheckout(
        String chaveOperacaoPedido,
        UUID clienteId,
        Map<String, Integer> quantidadePorSku
    ) {
        ReservaEstoque reserva = this.servicoReservaEstoque.reservarItensAtomicamente(
            chaveOperacaoPedido,
            clienteId,
            quantidadePorSku
        );
        return converterReservaParaDto(reserva);
    }

    /**
     * Confirma a baixa definitiva de uma reserva de forma idempotente para o Módulo 3.
     *
     * <p>Objetivo Funcional: Efetivar saída definitiva após pagamento aprovado.</p>
     * <p>Papel no Design Pattern: Adapta transição idempotente de {@link ReservaEstoque}.</p>
     *
     * @param chaveOperacaoPedido chave única da operação
     * @return DTO {@link ResumoReservaEstoqueDTO}
     */
    @Override
    @Transactional
    public ResumoReservaEstoqueDTO confirmarBaixaDefinitiva(String chaveOperacaoPedido) {
        ReservaEstoque reserva = this.servicoReservaEstoque.confirmarBaixaDefinitivaReserva(chaveOperacaoPedido);
        return converterReservaParaDto(reserva);
    }

    /**
     * Libera integralmente o estoque reservado de forma idempotente para o Módulo 3.
     *
     * <p>Objetivo Funcional: Devolver saldo ao disponível após recusa de pagamento ou cancelamento.</p>
     * <p>Papel no Design Pattern: Adapta transição compensatória de {@link ReservaEstoque}.</p>
     *
     * @param chaveOperacaoPedido chave única da operação
     * @param motivo motivo do cancelamento ou recusa
     * @return DTO {@link ResumoReservaEstoqueDTO}
     */
    @Override
    @Transactional
    public ResumoReservaEstoqueDTO liberarReservaPorCancelamentoOuRecusa(
        String chaveOperacaoPedido,
        String motivo
    ) {
        ReservaEstoque reserva = this.servicoReservaEstoque.liberarReservaPorFalhaOuCancelamento(
            chaveOperacaoPedido,
            motivo
        );
        return converterReservaParaDto(reserva);
    }

    /**
     * Converte o agregado {@link ReservaEstoque} em {@link ResumoReservaEstoqueDTO}.
     *
     * <p>Objetivo Funcional: Desacoplar o Módulo 3 das entidades JPA internas do Módulo 1.</p>
     * <p>Papel no Design Pattern: Mapeamento explícito na fronteira do adaptador.</p>
     *
     * @param reserva agregado ReservaEstoque
     * @return DTO imutável {@link ResumoReservaEstoqueDTO}
     */
    private ResumoReservaEstoqueDTO converterReservaParaDto(ReservaEstoque reserva) {
        Map<String, Integer> itensMapa = new LinkedHashMap<>();
        for (ItemReservaEstoque item : reserva.obterItens()) {
            itensMapa.put(item.obterSku(), item.obterQuantidadeReservada());
        }
        return new ResumoReservaEstoqueDTO(
            reserva.obterId(),
            reserva.obterChaveOperacaoPedido(),
            reserva.obterClienteId(),
            reserva.obterEstado(),
            reserva.obterCriadaEm(),
            reserva.obterExpiraEm(),
            reserva.obterFinalizadaEm(),
            itensMapa
        );
    }
}

