package br.edu.fatec.ecommerce.modulo2.carrinho.dominio;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.visitor.ElementoCarrinhoVisitor;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representa um produto simples ou variação física de item no carrinho de compras.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>ConcreteElement</i> no padrão <i>Visitor</i>.
 * Implementa o método {@link #aceitar(ElementoCarrinhoVisitor)} realizando o segundo
 * despacho (<i>Double Dispatch</i>) chamando especificamente {@link ElementoCarrinhoVisitor#visitarProduto(ItemCarrinhoProduto)}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Entity
@DiscriminatorValue("PRODUTO")
public class ItemCarrinhoProduto extends ItemCarrinho {

    /**
     * Construtor protegido exigido pelo JPA.
     */
    protected ItemCarrinhoProduto() {
        super();
    }

    /**
     * Construtor completo para item de produto simples.
     *
     * <p>Objetivo funcional: Instancia um produto individual no carrinho.
     * <p>Papel no padrão: Instanciação do elemento concreto folha visitável.
     *
     * @param id Identificador do item.
     * @param carrinhoId Identificador do carrinho.
     * @param sku SKU da variação ou produto.
     * @param nome Nome descritivo.
     * @param precoUnitario Preço unitário vigente.
     * @param quantidade Quantidade de unidades.
     * @param pesoGramas Peso em gramas.
     * @param categoriaId Categoria do produto.
     */
    public ItemCarrinhoProduto(
            UUID id,
            UUID carrinhoId,
            String sku,
            String nome,
            BigDecimal precoUnitario,
            int quantidade,
            int pesoGramas,
            UUID categoriaId
    ) {
        super(id, carrinhoId, sku, nome, precoUnitario, quantidade, pesoGramas, categoriaId);
    }

    /**
     * Implementação do Double Dispatch para produtos simples.
     *
     * <p>Objetivo funcional: Notifica o visitor sobre o tipo concreto de produto para processamento específico.
     * <p>Papel no padrão: Segundo despacho do Double Dispatch ({@code visitor.visitarProduto(this)}).
     *
     * @param visitor Visitante contendo o algoritmo a ser executado.
     */
    @Override
    public void aceitar(ElementoCarrinhoVisitor visitor) {
        if (visitor != null) {
            visitor.visitarProduto(this);
        }
    }
}

