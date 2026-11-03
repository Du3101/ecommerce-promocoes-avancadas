package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Nó Composto (Composite) que executa a conjunção lógica booleana AND entre suas condições filhas.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Composite</i> no padrão <i>Composite</i> (Constituição, Princípio II).
 * Contém uma coleção de elementos {@link CondicaoElegibilidadeComponent} e avalia como
 * verdadeiro se e somente se TODAS as condições filhas forem verdadeiras.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class CondicaoAndComposite implements CondicaoElegibilidadeComponent {

    private final List<CondicaoElegibilidadeComponent> condicoesFilhas = new ArrayList<>();

    /**
     * Construtor padrão que inicializa lista vazia de condições filhas.
     */
    public CondicaoAndComposite() {
    }

    /**
     * Construtor de conveniência aceitando condições iniciais.
     *
     * @param condicoes Lista de condições filhas.
     */
    public CondicaoAndComposite(List<CondicaoElegibilidadeComponent> condicoes) {
        if (condicoes != null) {
            this.condicoesFilhas.addAll(condicoes);
        }
    }

    /**
     * Adiciona uma nova condição filha à conjunção lógica AND.
     *
     * <p>Objetivo funcional: Permite a montagem dinâmica da árvore de elegibilidade.
     * <p>Papel no padrão: Método de gerenciamento de filhos do nó Composite.
     *
     * @param condicao Condição filha (folha ou sub-árvore).
     * @return a própria instância para permitir encadeamento fluente.
     */
    public CondicaoAndComposite adicionar(CondicaoElegibilidadeComponent condicao) {
        Objects.requireNonNull(condicao, "Condição filha não pode ser nula");
        this.condicoesFilhas.add(condicao);
        return this;
    }

    /**
     * Avalia recursivamente todas as condições filhas com curto-circuito booleano.
     *
     * <p>Objetivo funcional: Retorna true apenas se todos os critérios forem atendidos.
     * <p>Papel no padrão: Operação polimórfica recursiva do Composite.
     *
     * @param contexto Contexto de avaliação contendo dados do cliente e carrinho.
     * @return true se todas forem verdadeiras (ou lista vazia); false se qualquer uma falhar.
     */
    @Override
    public boolean avaliar(ContextoElegibilidade contexto) {
        if (condicoesFilhas.isEmpty()) {
            return true;
        }
        for (CondicaoElegibilidadeComponent filha : condicoesFilhas) {
            if (!filha.avaliar(contexto)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String descrever() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < condicoesFilhas.size(); i++) {
            if (i > 0) {
                sb.append(" AND ");
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

