package br.edu.fatec.ecommerce.modulo2.integracao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de fronteira imutável contendo o snapshot comercial completo do carrinho para o Módulo 3.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Serve como objeto de transferência desacoplado de fronteira (<i>Data Transfer Object</i>)
 * entre o Módulo 2 (Carrinho e Promoções) e o Módulo 3 (Checkout e Pagamentos).
 *
 * @param carrinhoId UUID do carrinho.
 * @param clienteId UUID do cliente (se autenticado).
 * @param sessaoId Token de sessão (se anônimo).
 * @param quantidadePorSku Mapa com os SKUs dos itens e quantidades para reserva atômica no estoque.
 * @param pesoTotalGramas Peso total acumulado em gramas para cálculo de frete.
 * @param subtotalBruto Subtotal dos itens antes de promoções e cupons.
 * @param totalDescontos Soma total dos abatimentos concedidos.
 * @param totalLiquido Valor final líquido comercial do pedido (com piso zero).
 * @param cupomUtilizado Código do cupom aplicado (ou null se nenhum).
 * @param nomesPromocoesAplicadas Lista dos nomes das campanhas que concederam desconto.
 * @param itens Linhas do carrinho (SKU, nome, tipo, preço unitário, quantidade e peso) para o snapshot do pedido
 *              no Módulo 3 (adicionado na integração com o Módulo 3 — Henrique Sandrini).
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public record ResumoCarrinhoCheckoutDTO(
        UUID carrinhoId,
        UUID clienteId,
        String sessaoId,
        Map<String, Integer> quantidadePorSku,
        int pesoTotalGramas,
        BigDecimal subtotalBruto,
        BigDecimal totalDescontos,
        BigDecimal totalLiquido,
        String cupomUtilizado,
        List<String> nomesPromocoesAplicadas,
        List<ItemResumoCarrinhoDTO> itens
) {

    /**
     * Indica se o carrinho não possui nenhum item.
     *
     * <p>Objetivo Funcional: Permitir ao checkout rejeitar o fechamento de um carrinho vazio.</p>
     * <p>Papel no Design Pattern: Predicado de conveniência do DTO de fronteira.</p>
     *
     * @return {@code true} se não houver itens no carrinho
     */
    public boolean estaVazio() {
        return itens == null || itens.isEmpty();
    }
}

