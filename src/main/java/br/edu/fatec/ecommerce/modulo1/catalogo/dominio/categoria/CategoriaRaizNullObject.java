package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * Representação polimórfica e imutável da ausência de categoria pai para categorias de nível raiz (FR-007).
 *
 * <p>Aplicação do Padrão GoF / Catálogo: <b>Null Object / Missing Object</b> integrado ao
 * <b>Composite</b> (Princípio II da Constituição).</p>
 * <p>Justificativa: Categorias de topo (ex.: "Vestuário", "Eletrônicos") não possuem categoria pai no banco.
 * Em vez de retornar {@code null} em {@code obterCategoriaPai()}, retorna-se esta instância neutra,
 * permitindo que algoritmos recursivos de caminho hierárquico e elegibilidade operem sem
 * {@code NullPointerException}.</p>
 */
public final class CategoriaRaizNullObject implements ComponenteCategoria {

    private static final UUID ID_RAIZ_NEUTRA = new UUID(0L, 0L);
    private static final CategoriaRaizNullObject INSTANCIA = new CategoriaRaizNullObject();

    /**
     * Construtor privado para reutilização da instância única imutável.
     *
     * <p>Objetivo Funcional: Evitar instanciações desnecessárias do objeto neutro de categoria raiz.</p>
     * <p>Papel no Design Pattern: Garante instância única para o <b>Null Object</b>.</p>
     */
    private CategoriaRaizNullObject() {
        // Instância única imutável
    }

    /**
     * Obtém a instância compartilhada de {@link CategoriaRaizNullObject}.
     *
     * <p>Objetivo Funcional: Fornecer a referência neutra sempre que uma categoria não possuir pai.</p>
     * <p>Papel no Design Pattern: Ponto de acesso estático ao <b>Null Object</b>.</p>
     *
     * @return instância imutável de {@link CategoriaRaizNullObject}
     */
    public static CategoriaRaizNullObject obterInstancia() {
        return INSTANCIA;
    }

    /**
     * Retorna o UUID neutro zeros ({@code 00000000-0000-0000-0000-000000000000}).
     *
     * <p>Objetivo Funcional: Evitar retorno nulo ao consultar o ID do pai de uma categoria raiz.</p>
     * <p>Papel no Design Pattern: Comportamento neutro do <b>Null Object</b>.</p>
     *
     * @return UUID neutro
     */
    @Override
    public UUID obterId() {
        return ID_RAIZ_NEUTRA;
    }

    /**
     * Retorna string vazia como nome da raiz ausente.
     *
     * <p>Objetivo Funcional: Não poluir a concatenação de nomes na trilha de categorias.</p>
     * <p>Papel no Design Pattern: Comportamento neutro do <b>Null Object</b>.</p>
     *
     * @return string vazia ({@code ""})
     */
    @Override
    public String obterNome() {
        return "";
    }

    /**
     * Retorna string vazia como slug da raiz ausente.
     *
     * <p>Objetivo Funcional: Fornecer slug neutro sem referência nula.</p>
     * <p>Papel no Design Pattern: Comportamento neutro do <b>Null Object</b>.</p>
     *
     * @return string vazia ({@code ""})
     */
    @Override
    public String obterSlug() {
        return "";
    }

    /**
     * Retorna string vazia como caminho hierárquico base para encerrar a recursão de categorias.
     *
     * <p>Objetivo Funcional: Servir como caso base polimórfico da recursão de caminho hierárquico.</p>
     * <p>Papel no Design Pattern: Caso base neutro do <b>Null Object</b> no <b>Composite</b>.</p>
     *
     * @return string vazia ({@code ""})
     */
    @Override
    public String obterCaminhoHierarquico() {
        return "";
    }

    /**
     * Indica que este objeto é a raiz nula (ausência de categoria pai).
     *
     * <p>Objetivo Funcional: Confirmar que a categoria filha já está no topo da árvore.</p>
     * <p>Papel no Design Pattern: Identificação semântica do <b>Null Object</b>.</p>
     *
     * @return sempre {@code true}
     */
    @Override
    public boolean eRaizNula() {
        return true;
    }

    /**
     * Retorna conjunto vazio de IDs para a raiz nula.
     *
     * <p>Objetivo Funcional: Não adicionar IDs fictícios em consultas de subárvore.</p>
     * <p>Papel no Design Pattern: Coleção neutra vazia do <b>Null Object</b>.</p>
     *
     * @return conjunto imutável vazio
     */
    @Override
    public Set<UUID> obterIdsSubarvoreRecursivo() {
        return Collections.emptySet();
    }
}

