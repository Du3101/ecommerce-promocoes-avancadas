package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria;

import java.util.Set;
import java.util.UUID;

/**
 * Interface comum para nós da árvore hierárquica de categorias do catálogo (FR-007).
 *
 * <p>Aplicação Conjunta dos Padrões GoF: <b>Composite</b> (papel <i>Component</i>) e
 * <b>Null Object</b> (Princípio II da Constituição).</p>
 * <p>Justificativa: Permite tratar uniformemente categorias compostas ({@link CategoriaComp})
 * e a raiz nula ({@link CategoriaRaizNullObject}), eliminando checagens de {@code null} na
 * montagem de caminhos hierárquicos (breadcrumbs) e buscas recursivas.</p>
 */
public interface ComponenteCategoria {

    /**
     * Obtém o identificador único do componente de categoria.
     *
     * <p>Objetivo Funcional: Identificar a categoria em consultas e filtros.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i> no padrão <b>Composite</b>.</p>
     *
     * @return UUID da categoria (ou UUID neutro na raiz nula)
     */
    UUID obterId();

    /**
     * Obtém o nome de exibição da categoria.
     *
     * <p>Objetivo Funcional: Apresentar o nome comercial da categoria.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i>.</p>
     *
     * @return nome da categoria ou string vazia no Null Object
     */
    String obterNome();

    /**
     * Obtém o identificador textual amigável (slug) da categoria.
     *
     * <p>Objetivo Funcional: Compor URLs amigáveis e chaves de busca.</p>
     * <p>Papel no Design Pattern: Operação uniforme do <i>Component</i>.</p>
     *
     * @return slug normalizado ou string vazia no Null Object
     */
    String obterSlug();

    /**
     * Constrói recursivamente o caminho hierárquico completo (ex.: {@code "Vestuário > Camisetas > Esportivas"}).
     *
     * <p>Objetivo Funcional: Exibir a trilha de navegação (breadcrumb) completa de qualquer categoria.</p>
     * <p>Papel no Design Pattern: Operação recursiva do padrão <b>Composite</b> apoiada pelo
     * <b>Null Object</b> na raiz.</p>
     *
     * @return caminho hierárquico formatado
     */
    String obterCaminhoHierarquico();

    /**
     * Indica se este componente representa a raiz ausente (Null Object).
     *
     * <p>Objetivo Funcional: Distinguir semanticamente uma categoria de topo sem pai.</p>
     * <p>Papel no Design Pattern: Predicado explícito do padrão <b>Null Object</b>.</p>
     *
     * @return {@code true} se for {@link CategoriaRaizNullObject}; {@code false} se for categoria real
     */
    boolean eRaizNula();

    /**
     * Coleta recursivamente o identificador desta categoria e de todas as suas subcategorias descendentes ativas.
     *
     * <p>Objetivo Funcional: Permitir que a consulta de produtos por uma categoria pai inclua
     * automaticamente os itens de todas as suas subcategorias filhas, netas, etc. (FR-007).</p>
     * <p>Papel no Design Pattern: Operação de varredura em árvore do padrão <b>Composite</b>.</p>
     *
     * @return conjunto de UUIDs da subárvore de categorias
     */
    Set<UUID> obterIdsSubarvoreRecursivo();
}

