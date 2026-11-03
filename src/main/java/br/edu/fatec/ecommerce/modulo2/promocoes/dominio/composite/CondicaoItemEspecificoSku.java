package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;

import java.util.Objects;

/**
 * Condição folha (Leaf) que valida se determinado SKU está presente no carrinho
 * com uma quantidade mínima especificada.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Leaf</i> no padrão <i>Composite</i> para regras de elegibilidade baseadas em SKUs específicos.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoItemEspecificoSku implements CondicaoElegibilidadeComponent {

    private final String skuEsperado;
    private final int quantidadeMinima;

    /**
     * Construtor da condição por SKU.
     *
     * @param skuEsperado SKU do produto procurado.
     * @param quantidadeMinima Quantidade mínima exigida no carrinho.
     */
    public CondicaoItemEspecificoSku(String skuEsperado, int quantidadeMinima) {
        this.skuEsperado = Objects.requireNonNull(skuEsperado, "SKU não pode ser nulo").trim();
        this.quantidadeMinima = Math.max(1, quantidadeMinima);
    }

    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        if (contexto == null || contexto.getItens().isEmpty()) {
            return false;
        }
        for (ItemCarrinho item : contexto.getItens()) {
            if (item.getSku().equalsIgnoreCase(skuEsperado) && item.getQuantidade() >= quantidadeMinima) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String descrever() {
        return "Contém SKU '" + skuEsperado + "' (Qtd >= " + quantidadeMinima + ")";
    }

    public String getSkuEsperado() {
        return skuEsperado;
    }

    public int getQuantidadeMinima() {
        return quantidadeMinima;
    }
}

