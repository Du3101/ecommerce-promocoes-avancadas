package br.edu.fatec.ecommerce.modulo2.integracao;

import java.math.BigDecimal;

/**
 * DTO de fronteira imutável com uma linha do carrinho de compras.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Objeto de transferência da porta {@link PortaConsultaCarrinhoPromocoes}. Permite ao Módulo 3 congelar, no
 * snapshot do pedido, o nome e o preço unitário que o cliente viu no carrinho, sem acessar as entidades
 * {@code ItemCarrinho} do Módulo 2.
 *
 * @param sku Código SKU do produto ou combo.
 * @param nome Nome de exibição do item no momento da adição ao carrinho.
 * @param tipoItem {@code "PRODUTO"} ou {@code "COMBO_KIT"}.
 * @param precoUnitario Preço unitário de tabela praticado no carrinho (antes de descontos).
 * @param quantidade Quantidade de unidades na linha.
 * @param pesoTotalGramas Peso total da linha (peso unitário × quantidade) em gramas.
 *
 * @author Henrique Sandrini (Integrante 3) — extensão da porta do Módulo 2 para o checkout
 */
public record ItemResumoCarrinhoDTO(
        String sku,
        String nome,
        String tipoItem,
        BigDecimal precoUnitario,
        int quantidade,
        int pesoTotalGramas
) {
}
