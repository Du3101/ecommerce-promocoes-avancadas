package br.edu.fatec.ecommerce.modulo2.carrinho.dominio.visitor;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoProduto;

/**
 * Interface do padrão Visitor para inspeção e cálculo polimórfico sobre elementos do carrinho.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a interface <i>Visitor</i> com suporte a <i>Double Dispatch</i> (Constituição, Princípio II).
 * Permite que algoritmos de precificação, tributação ou relatórios percorram itens individuais de produtos
 * e combos/kits executando comportamentos específicos para cada tipo concreto sem violar o encapsulamento
 * e sem recorrer a verificações com {@code instanceof} ou casting reflexivo.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public interface ElementoCarrinhoVisitor {

    /**
     * Visita um item de produto simples ou variação folha do carrinho.
     *
     * <p>Objetivo funcional: Processa a regra específica aplicável a produtos individuais.
     * <p>Papel no padrão: Primeiro método de visitação do Visitor para o elemento folha.
     *
     * @param produto Item do carrinho representando um produto simples ou variação.
     */
    void visitarProduto(ItemCarrinhoProduto produto);

    /**
     * Visita um item de combo/kit composto do carrinho.
     *
     * <p>Objetivo funcional: Processa a regra específica aplicável a conjuntos e kits compostos.
     * <p>Papel no padrão: Segundo método de visitação do Visitor para o elemento composto.
     *
     * @param combo Item do carrinho representando um combo/kit promocional.
     */
    void visitarComboKit(ItemCarrinhoComboKit combo);
}

