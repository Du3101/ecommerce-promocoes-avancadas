package br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta da árvore de categorias {@link CategoriaComp}.
 *
 * <p>Responsabilidade na Arquitetura: Abstrair o acesso à tabela autorreferenciada {@code categorias} (FR-007).</p>
 */
@Repository
public interface CategoriaJpaRepository extends JpaRepository<CategoriaComp, UUID> {

    /**
     * Lista todas as categorias de nível raiz (aquelas que não possuem categoria pai no banco).
     *
     * <p>Objetivo Funcional: Carregar os nós de topo para montagem da árvore mercadológica completa.</p>
     * <p>Papel no Design Pattern: Consulta de raízes do padrão <b>Composite</b>.</p>
     *
     * @return lista de categorias raiz
     */
    List<CategoriaComp> findByCategoriaPaiPersistenciaIsNull();

    /**
     * Busca uma categoria pelo seu slug ignorando caixa.
     *
     * <p>Objetivo Funcional: Localizar categorias por identificador amigável.</p>
     * <p>Papel no Design Pattern: Consulta de repositório por chave de negócio.</p>
     *
     * @param slug slug da categoria
     * @return {@link Optional} contendo a categoria se encontrada
     */
    Optional<CategoriaComp> findBySlugIgnoreCase(String slug);

    /**
     * Verifica se já existe uma categoria com o slug informado.
     *
     * <p>Objetivo Funcional: Garantir unicidade do slug antes de persistir nova categoria.</p>
     * <p>Papel no Design Pattern: Predicado de unicidade na camada de persistência.</p>
     *
     * @param slug slug a verificar
     * @return {@code true} se o slug já existir
     */
    boolean existsBySlugIgnoreCase(String slug);
}

