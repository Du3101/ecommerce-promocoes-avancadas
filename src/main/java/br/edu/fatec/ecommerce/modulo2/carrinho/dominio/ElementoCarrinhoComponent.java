package br.edu.fatec.ecommerce.modulo2.carrinho.dominio;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.visitor.ElementoCarrinhoVisitor;

import java.math.BigDecimal;

/**
 * Interface comum para os elementos que compõem as linhas do carrinho de compras.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Element</i> no padrão <i>Visitor</i> com <i>Double Dispatch</i>.
 * Declara o método {@link #aceitar(ElementoCarrinhoVisitor)} que recebe o visitante
 * e despacha a chamada para o método concreto correspondente, além de métodos de cálculo
 * uniforme de subtotal e peso.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public interface ElementoCarrinhoComponent {

    /**
     * Ponto de entrada do padrão Visitor (Double Dispatch).
     *
     * <p>Objetivo funcional: Permite a execução de algoritmos externos desacoplados sobre o item.
     * <p>Papel no padrão: Primeiro despacho dinâmico que encaminha a chamada para o visitor com {@code visitor.visitar...(this)}.
     *
     * @param visitor Visitante contendo o algoritmo a ser executado sobre o item.
     */
    void aceitar(ElementoCarrinhoVisitor visitor);

    /**
     * Calcula o subtotal bruto desta linha de item (preço unitário multiplicado pela quantidade).
     *
     * <p>Objetivo funcional: Obter o valor monetário total correspondente a este item no carrinho.
     * <p>Papel no padrão: Operação aritmética polimórfica comum a folhas e composições.
     *
     * @return BigDecimal normalizado com o subtotal da linha.
     */
    BigDecimal calcularSubtotalItem();

    /**
     * Retorna a quantidade de unidades deste item no carrinho.
     *
     * @return Quantidade inteira positiva.
     */
    int getQuantidade();

    /**
     * Retorna o peso total acumulado desta linha em gramas.
     *
     * @return Peso total em gramas (peso unitário multiplicado pela quantidade).
     */
    int calcularPesoTotalLinhaGramas();
}

