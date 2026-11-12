package br.edu.fatec.ecommerce.modulo3.relatorios.dto;

import java.math.BigDecimal;

/**
 * Linha do ranking de produtos mais vendidos.
 *
 * @param sku SKU do produto ou combo
 * @param nome nome no momento da venda
 * @param quantidadeVendida unidades faturadas
 * @param receitaLiquida receita líquida das linhas (com descontos rateados)
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record ItemMaisVendidoDTO(String sku, String nome, long quantidadeVendida, BigDecimal receitaLiquida) {
}
