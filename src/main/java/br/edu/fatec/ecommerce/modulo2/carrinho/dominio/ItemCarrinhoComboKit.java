package br.edu.fatec.ecommerce.modulo2.carrinho.dominio;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.visitor.ElementoCarrinhoVisitor;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representa um Combo ou Kit promocional composto no carrinho de compras.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>ConcreteElement</i> (Composição) no padrão <i>Visitor</i>.
 * Implementa o método {@link #aceitar(ElementoCarrinhoVisitor)} realizando o segundo
 * despacho (<i>Double Dispatch</i>) chamando especificamente {@link ElementoCarrinhoVisitor#visitarComboKit(ItemCarrinhoComboKit)}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Entity
@DiscriminatorValue("COMBO_KIT")
public class ItemCarrinhoComboKit extends ItemCarrinho {

    /**
     * Construtor protegido exigido pelo JPA.
     */
    protected ItemCarrinhoComboKit() {
        super();
    }

    /**
     * Construtor completo para item de combo/kit.
     *
     * <p>Objetivo funcional: Instancia um combo/kit no carrinho de compras.
     * <p>Papel no padrão: Instanciação do elemento concreto composto visitável.
     *
     * @param id Identificador do item.
     * @param carrinhoId Identificador do carrinho.
     * @param sku SKU do Combo/Kit.
     * @param nome Nome comercial do conjunto.
     * @param precoUnitario Preço comercial vigente do conjunto.
     * @param quantidade Quantidade de kits.
     * @param pesoGramas Peso total agregado do conjunto.
     * @param categoriaId Categoria do combo.
     */
    public ItemCarrinhoComboKit(
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
     * Implementação do Double Dispatch para Combos/Kits.
     *
     * <p>Objetivo funcional: Notifica o visitor sobre o tipo concreto de Combo/Kit para processamento específico.
     * <p>Papel no padrão: Segundo despacho do Double Dispatch ({@code visitor.visitarComboKit(this)}).
     *
     * @param visitor Visitante contendo o algoritmo a ser executado.
     */
    @Override
    public void aceitar(ElementoCarrinhoVisitor visitor) {
        if (visitor != null) {
            visitor.visitarComboKit(this);
        }
    }
}

