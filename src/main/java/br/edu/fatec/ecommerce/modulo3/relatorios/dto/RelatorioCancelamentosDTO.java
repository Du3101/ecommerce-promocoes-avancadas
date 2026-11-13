package br.edu.fatec.ecommerce.modulo3.relatorios.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicadores de cancelamentos e recusas de pagamento.
 *
 * @param totalPedidos total de pedidos registrados
 * @param pedidosCancelados pedidos no estado CANCELADO
 * @param pedidosComPagamentoRecusado pedidos no estado PAGAMENTO_RECUSADO
 * @param taxaCancelamentoPercentual (cancelados + recusados) ÷ total × 100
 * @param motivosMaisFrequentes motivos agrupados por frequência
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record RelatorioCancelamentosDTO(
        long totalPedidos,
        long pedidosCancelados,
        long pedidosComPagamentoRecusado,
        BigDecimal taxaCancelamentoPercentual,
        List<MotivoDTO> motivosMaisFrequentes
) {

    /**
     * Frequência de um motivo.
     *
     * @param motivo motivo registrado
     * @param quantidade número de pedidos com o motivo
     */
    public record MotivoDTO(String motivo, long quantidade) {
    }
}
