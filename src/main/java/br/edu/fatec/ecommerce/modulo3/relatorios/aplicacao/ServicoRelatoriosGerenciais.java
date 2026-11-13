package br.edu.fatec.ecommerce.modulo3.relatorios.aplicacao;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.ItemPedidoSnapshotJpaRepository;
import br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura.PedidoJpaRepository;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.ItemMaisVendidoDTO;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.RelatorioCancelamentosDTO;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.RelatorioTicketMedioDTO;
import br.edu.fatec.ecommerce.modulo3.relatorios.dto.RelatorioVendasConsolidadasDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios gerenciais calculados por consultas agregadas sobre o snapshot imutável dos pedidos.
 *
 * <p>Como os valores ficam congelados no pedido (Princípio IV), os relatórios refletem exatamente o que foi
 * cobrado, mesmo que preços e promoções mudem depois. Só entram no faturamento os pedidos com pagamento
 * confirmado (PAGO, EM_PREPARACAO, ENVIADO, ENTREGUE).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Service
@Transactional(readOnly = true)
public class ServicoRelatoriosGerenciais {

    /** Status que representam venda faturada. */
    public static final List<StatusPedido> STATUS_FATURADOS =
            List.of(StatusPedido.PAGO, StatusPedido.EM_PREPARACAO, StatusPedido.ENVIADO, StatusPedido.ENTREGUE);

    /** Status de venda perdida. */
    private static final List<StatusPedido> STATUS_PERDIDOS =
            List.of(StatusPedido.CANCELADO, StatusPedido.PAGAMENTO_RECUSADO);

    private final PedidoJpaRepository pedidoRepository;
    private final ItemPedidoSnapshotJpaRepository itemRepository;

    /**
     * Construtor com repositórios injetados.
     *
     * @param pedidoRepository repositório de pedidos
     * @param itemRepository repositório de itens de pedido
     */
    public ServicoRelatoriosGerenciais(PedidoJpaRepository pedidoRepository, ItemPedidoSnapshotJpaRepository itemRepository) {
        this.pedidoRepository = pedidoRepository;
        this.itemRepository = itemRepository;
    }

    /**
     * Faturamento bruto, descontos, faturamento líquido e descontos por cupom.
     *
     * @return relatório consolidado
     */
    public RelatorioVendasConsolidadasDTO vendasConsolidadas() {
        long quantidade = contarFaturados();
        List<RelatorioVendasConsolidadasDTO.DescontoPorCupomDTO> porCupom =
                pedidoRepository.agruparDescontosPorCupom(STATUS_FATURADOS).stream()
                        .map(linha -> new RelatorioVendasConsolidadasDTO.DescontoPorCupomDTO(
                                (String) linha[0], ((Number) linha[1]).longValue(), moeda((BigDecimal) linha[2])))
                        .toList();
        return new RelatorioVendasConsolidadasDTO(
                quantidade,
                moeda(pedidoRepository.somarSubtotalBrutoPorStatus(STATUS_FATURADOS)),
                moeda(pedidoRepository.somarDescontosConcedidosPorStatus(STATUS_FATURADOS)),
                moeda(pedidoRepository.somarFaturamentoLiquidoPorStatus(STATUS_FATURADOS)),
                porCupom);
    }

    /**
     * Ticket médio por pedido e valor médio por cliente.
     *
     * @return relatório de ticket médio
     */
    public RelatorioTicketMedioDTO ticketMedio() {
        long pedidos = contarFaturados();
        long clientes = pedidoRepository.contarClientesDistintosPorStatus(STATUS_FATURADOS);
        BigDecimal faturamento = moeda(pedidoRepository.somarFaturamentoLiquidoPorStatus(STATUS_FATURADOS));
        return new RelatorioTicketMedioDTO(pedidos, clientes, faturamento,
                dividir(faturamento, pedidos), dividir(faturamento, clientes));
    }

    /**
     * Ranking dos produtos mais vendidos por quantidade.
     *
     * @param limite quantidade máxima de linhas
     * @return ranking
     */
    public List<ItemMaisVendidoDTO> produtosMaisVendidos(int limite) {
        return itemRepository.encontrarProdutosMaisVendidos(STATUS_FATURADOS).stream()
                .limit(Math.max(1, limite))
                .map(linha -> new ItemMaisVendidoDTO((String) linha[0], (String) linha[1],
                        ((Number) linha[2]).longValue(), moeda((BigDecimal) linha[3])))
                .toList();
    }

    /**
     * Taxa de cancelamento (incluindo recusas) e motivos mais frequentes.
     *
     * @return relatório de cancelamentos
     */
    public RelatorioCancelamentosDTO cancelamentos() {
        long total = pedidoRepository.count();
        long cancelados = pedidoRepository.countByStatus(StatusPedido.CANCELADO);
        long recusados = pedidoRepository.countByStatus(StatusPedido.PAGAMENTO_RECUSADO);
        BigDecimal taxa = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                : BigDecimal.valueOf((cancelados + recusados) * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_EVEN);
        List<RelatorioCancelamentosDTO.MotivoDTO> motivos = pedidoRepository.agruparMotivosCancelamento(STATUS_PERDIDOS).stream()
                .map(linha -> new RelatorioCancelamentosDTO.MotivoDTO((String) linha[0], ((Number) linha[1]).longValue()))
                .toList();
        return new RelatorioCancelamentosDTO(total, cancelados, recusados, taxa, motivos);
    }

    /**
     * Conta os pedidos faturados.
     *
     * @return quantidade
     */
    private long contarFaturados() {
        return STATUS_FATURADOS.stream().mapToLong(pedidoRepository::countByStatus).sum();
    }

    /**
     * Padroniza um valor monetário (escala 2, HALF_EVEN; nulo vira zero).
     *
     * @param valor valor
     * @return valor padronizado
     */
    private static BigDecimal moeda(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Divide com proteção contra divisão por zero.
     *
     * @param valor dividendo
     * @param divisor divisor
     * @return quociente com 2 casas, ou zero
     */
    private static BigDecimal dividir(BigDecimal valor, long divisor) {
        return divisor == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)
                : valor.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_EVEN);
    }
}
