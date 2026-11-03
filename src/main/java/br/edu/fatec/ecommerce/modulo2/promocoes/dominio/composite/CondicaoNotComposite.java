package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import java.util.Objects;

/**
 * Nó Composto (Composite unário) que inverte o resultado booleano de sua condição filha (NOT).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como operador unário de negação no padrão <i>Composite</i>.
 * Avalia como verdadeiro se e somente se a condição filha for falsa.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoNotComposite implements CondicaoElegibilidadeComponent {

    private final CondicaoElegibilidadeComponent condicaoFilha;

    /**
     * Construtor da negação lógica.
     *
     * @param condicaoFilha Condição cuja resposta será invertida.
     */
    public CondicaoNotComposite(CondicaoElegibilidadeComponent condicaoFilha) {
        this.condicaoFilha = Objects.requireNonNull(condicaoFilha, "Condição filha para NOT não pode ser nula");
    }

    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        return !condicaoFilha.avaliar(contexto);
    }

    @Override
    public String descrever() {
        return "NOT (" + condicaoFilha.descrever() + ")";
    }

    public CondicaoElegibilidadeComponent getCondicaoFilha() {
        return condicaoFilha;
    }
}

