package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Condição folha (Leaf) que valida se o subtotal bruto do carrinho atinge um valor monetário mínimo.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Leaf</i> no padrão <i>Composite</i> (Constituição, Princípio II).
 * Executa a avaliação atômica sobre o subtotal do carrinho.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoValorMinimoCarrinho implements CondicaoElegibilidadeComponent {

    private final BigDecimal valorMinimo;

    /**
     * Construtor da condição de valor mínimo.
     *
     * @param valorMinimo Valor monetário mínimo exigido.
     */
    public CondicaoValorMinimoCarrinho(BigDecimal valorMinimo) {
        this.valorMinimo = OperacoesMonetarias.normalizar(
                Objects.requireNonNull(valorMinimo, "Valor mínimo não pode ser nulo")
        );
    }

    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        if (contexto == null || contexto.getSubtotalCarrinho() == null) {
            return false;
        }
        return contexto.getSubtotalCarrinho().compareTo(valorMinimo) >= 0;
    }

    @Override
    public String descrever() {
        return "Subtotal >= R$ " + valorMinimo;
    }

    public BigDecimal getValorMinimo() {
        return valorMinimo;
    }
}
