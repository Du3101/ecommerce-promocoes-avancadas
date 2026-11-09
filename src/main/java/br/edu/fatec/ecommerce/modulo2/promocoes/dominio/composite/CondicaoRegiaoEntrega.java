package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import java.util.Objects;

/**
 * Condição folha (Leaf) que valida se a UF de entrega do cliente coincide com a região da promoção.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Leaf</i> no padrão <i>Composite</i> para campanhas regionalizadas.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoRegiaoEntrega implements CondicaoElegibilidadeComponent {

    private final String ufEsperada;

    /**
     * Construtor da condição por região/UF.
     *
     * @param ufEsperada Sigla de 2 caracteres da UF (ex.: "SP").
     */
    public CondicaoRegiaoEntrega(String ufEsperada) {
        this.ufEsperada = Objects.requireNonNull(ufEsperada, "UF não pode ser nula").trim().toUpperCase();
    }

    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        if (contexto == null || contexto.getUfEntrega() == null) {
            return false;
        }
        return this.ufEsperada.equalsIgnoreCase(contexto.getUfEntrega().trim());
    }

    @Override
    public String descrever() {
        return "Entrega na UF " + ufEsperada;
    }

    public String getUfEsperada() {
        return ufEsperada;
    }
}
