package br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta de agregados {@link Produto}.
 *
 * <p>Responsabilidade na Arquitetura: Abstrair o acesso à tabela {@code produtos} e suas
 * coleções associadas (FR-005, FR-007).</p>
 */
@Repository
public interface ProdutoJpaRepository extends JpaRepository<Produto, UUID> {

    /**
     * Lista todos os produtos vinculados a qualquer uma das categorias do conjunto informado.
     *
     * <p>Objetivo Funcional: Suportar busca recursiva de produtos em uma categoria pai e todas as
     * suas subcategorias descendentes (via {@code CategoriaComp.obterIdsSubarvoreRecursivo()}).</p>
     * <p>Papel no Design Pattern: Integração de consulta com a subárvore do padrão <b>Composite</b>.</p>
     *
     * @param categoriaIds coleção de UUIDs da subárvore de categorias
     * @return lista de produtos pertencentes à subárvore
     */
    List<Produto> findByCategoriaPrincipalIdIn(Collection<UUID> categoriaIds);
}

