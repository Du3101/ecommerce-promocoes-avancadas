package br.edu.fatec.ecommerce.modulo1.catalogo.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoVariacaoLeaf;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para consulta e persistência de folhas comercializáveis {@link ProdutoVariacaoLeaf}.
 *
 * <p>Responsabilidade na Arquitetura: Permitir localização direta de variações físicas pelo seu
 * código SKU único (FR-005, FR-006).</p>
 */
@Repository
public interface VariacaoProdutoJpaRepository extends JpaRepository<ProdutoVariacaoLeaf, UUID> {

    /**
     * Busca uma variação comercializável pelo código SKU ignorando caixa.
     *
     * <p>Objetivo Funcional: Resolver um SKU unitário nas consultas de catálogo, combos e reservas.</p>
     * <p>Papel no Design Pattern: Consulta da folha (<i>Leaf</i>) do padrão <b>Composite</b>.</p>
     *
     * @param sku código SKU da variação
     * @return {@link Optional} contendo a {@link ProdutoVariacaoLeaf} se encontrada
     */
    Optional<ProdutoVariacaoLeaf> findBySkuIgnoreCase(String sku);

    /**
     * Verifica se já existe uma variação cadastrada com o código SKU informado.
     *
     * <p>Objetivo Funcional: Impedir duplicidade de SKU entre variações e combos.</p>
     * <p>Papel no Design Pattern: Predicado de unicidade na persistência.</p>
     *
     * @param sku código SKU a verificar
     * @return {@code true} se o SKU já estiver em uso
     */
    boolean existsBySkuIgnoreCase(String sku);
}

