package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Nó Composto (Composite) que executa a disjunção lógica booleana OR entre suas condições filhas.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Composite</i> no padrão <i>Composite</i>.
 * Avalia como verdadeiro se PELO MENOS UMA condição filha for verdadeira.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoOrComposite implements CondicaoElegibilidadeComponent {

    private final List<CondicaoElegibilidadeComponent> condicoesFilhas = new ArrayList<>();

    public CondicaoOrComposite() {
    }

    public CondicaoOrComposite(List<CondicaoElegibilidadeComponent> condicoes) {
        if (condicoes != null) {
            this.condicoesFilhas.addAll(condicoes);
        }
    }

    /**
     * Adiciona uma nova condição filha à disjunção lógica OR.
     *
     * @param condicao Condição filha.
     * @return a própria instância para encadeamento fluente.
     */
    public CondicaoOrComposite adicionar(CondicaoElegibilidadeComponent condicao) {
        Objects.requireNonNull(condicao, "Condição filha não pode ser nula");
        this.condicoesFilhas.add(condicao);
        return this;
    }

    /**
     * Avalia recursivamente as condições filhas com curto-circuito booleano.
     *
     * @param contexto Contexto de avaliação.
     * @return true se pelo menos uma filha for verdadeira; false se todas falharem.
     */
    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        if (condicoesFilhas.isEmpty()) {
            return true;
        }
        for (CondicaoElegibilidadeComponent filha : condicoesFilhas) {
            if (filha.avaliar(contexto)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String descrever() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < condicoesFilhas.size(); i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(condicoesFilhas.get(i).descrever());
        }
        sb.append(")");
        return sb.toString();
    }

    public List<CondicaoElegibilidadeComponent> getCondicoesFilhas() {
        return Collections.unmodifiableList(condicoesFilhas);
    }
}

