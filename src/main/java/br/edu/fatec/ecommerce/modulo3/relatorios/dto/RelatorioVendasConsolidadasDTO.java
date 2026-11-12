package br.edu.fatec.ecommerce.modulo3.relatorios.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vendas consolidadas dos pedidos faturados (PAGO, EM_PREPARACAO, ENVIADO e ENTREGUE).
 *
 * @param quantidadePedidosFaturados número de pedidos faturados
 * @param faturamentoBruto soma dos subtotais brutos
 * @param totalDescontosConcedidos soma dos descontos (promoções + cupons)
 * @param faturamentoLiquido soma dos totais líquidos efetivamente cobrados (inclui frete)
 * @param descontosPorCupom descontos agrupados por cupom utilizado
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record RelatorioVendasConsolidadasDTO(
        long quantidadePedidosFaturados,
        BigDecimal faturamentoBruto,
        BigDecimal totalDescontosConcedidos,
        BigDecimal faturamentoLiquido,
        List<DescontoPorCupomDTO> descontosPorCupom
) {

    /**
     * Desconto concedido por um cupom.
     *
     * @param cupom código do cupom
     * @param quantidadePedidos pedidos faturados com o cupom
     * @param totalDescontos descontos totais desses pedidos
     */
    public record DescontoPorCupomDTO(String cupom, long quantidadePedidos, BigDecimal totalDescontos) {
    }
}
