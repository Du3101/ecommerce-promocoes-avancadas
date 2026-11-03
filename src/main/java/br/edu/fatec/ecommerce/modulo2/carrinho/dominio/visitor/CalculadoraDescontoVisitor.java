package br.edu.fatec.ecommerce.modulo2.carrinho.dominio.visitor;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoProduto;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Visitante concreto que calcula subtotais e descontos específicos inspecionando itens de produtos e combos.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>ConcreteVisitor</i> no padrão <i>Visitor</i> com <i>Double Dispatch</i> (Constituição, Princípio II).
 * Permite filtrar e acumular subtotais elegíveis por categoria ou tipo de item sem recorrer a
 * {@code instanceof} ou downcasting procedural.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CalculadoraDescontoVisitor implements ElementoCarrinhoVisitor {

    private final UUID categoriaFiltro;
    private BigDecimal subtotalProdutos = BigDecimal.ZERO;
    private BigDecimal subtotalCombos = BigDecimal.ZERO;
    private int quantidadeProdutos = 0;
    private int quantidadeCombos = 0;

    /**
     * Construtor padrão que avalia todos os itens sem filtro específico de categoria.
     */
    public CalculadoraDescontoVisitor() {
        this.categoriaFiltro = null;
    }

    /**
     * Construtor com filtro de categoria alvo.
     *
     * @param categoriaFiltro UUID da categoria a ser acumulada (ou null para todas).
     */
    public CalculadoraDescontoVisitor(UUID categoriaFiltro) {
        this.categoriaFiltro = categoriaFiltro;
    }

    /**
     * Segundo despacho (Double Dispatch) para produtos individuais.
     *
     * <p>Objetivo funcional: Acumula as métricas específicas de produtos simples.
     * <p>Papel no padrão: Implementação da visita para nó folha do Visitor.
     *
     * @param produto Item do tipo produto simples ou variação.
     */
    @Override
    public void visitarProduto(ItemCarrinhoProduto produto) {
        if (produto == null) {
            return;
        }
        if (categoriaFiltro == null || categoriaFiltro.equals(produto.getCategoriaId())) {
            this.subtotalProdutos = this.subtotalProdutos.add(produto.calcularSubtotalItem());
            this.quantidadeProdutos += produto.getQuantidade();
        }
    }

    /**
     * Segundo despacho (Double Dispatch) para Combos/Kits.
     *
     * <p>Objetivo funcional: Acumula as métricas específicas de combos/kits.
     * <p>Papel no padrão: Implementação da visita para nó composto do Visitor.
     *
     * @param combo Item do tipo combo/kit composto.
     */
    @Override
    public void visitarComboKit(ItemCarrinhoComboKit combo) {
        if (combo == null) {
            return;
        }
        if (categoriaFiltro == null || categoriaFiltro.equals(combo.getCategoriaId())) {
            this.subtotalCombos = this.subtotalCombos.add(combo.calcularSubtotalItem());
            this.quantidadeCombos += combo.getQuantidade();
        }
    }

    public BigDecimal getSubtotalProdutos() {
        return OperacoesMonetarias.normalizar(subtotalProdutos);
    }

    public BigDecimal getSubtotalCombos() {
        return OperacoesMonetarias.normalizar(subtotalCombos);
    }

    public BigDecimal getSubtotalGeral() {
        return OperacoesMonetarias.normalizar(subtotalProdutos.add(subtotalCombos));
    }

    public int getQuantidadeProdutos() {
        return quantidadeProdutos;
    }

    public int getQuantidadeCombos() {
        return quantidadeCombos;
    }

    public int getQuantidadeTotal() {
        return quantidadeProdutos + quantidadeCombos;
    }
}

