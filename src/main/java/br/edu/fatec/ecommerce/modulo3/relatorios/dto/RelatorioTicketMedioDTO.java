package br.edu.fatec.ecommerce.modulo3.relatorios.dto;

import java.math.BigDecimal;

/**
 * Ticket médio da loja.
 *
 * @param quantidadePedidosFaturados número de pedidos faturados
 * @param quantidadeClientesCompradores clientes distintos com pedidos faturados
 * @param faturamentoLiquido soma dos totais líquidos
 * @param ticketMedioPorPedido faturamento líquido ÷ pedidos
 * @param valorMedioPorCliente faturamento líquido ÷ clientes
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record RelatorioTicketMedioDTO(
        long quantidadePedidosFaturados,
        long quantidadeClientesCompradores,
        BigDecimal faturamentoLiquido,
        BigDecimal ticketMedioPorPedido,
        BigDecimal valorMedioPorCliente
) {
}
