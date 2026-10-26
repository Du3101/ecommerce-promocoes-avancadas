package br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.estoque.dominio.MovimentacaoEstoque;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta do histórico {@link MovimentacaoEstoque}.
 *
 * <p>Responsabilidade na Arquitetura: Manter o razão contábil de entradas, saídas, retenções e
 * estornos por SKU (FR-009, FR-010, FR-011).</p>
 */
@Repository
public interface MovimentacaoEstoqueJpaRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

    /**
     * Lista todas as movimentações de um determinado SKU ordenadas da mais recente para a mais antiga.
     *
     * <p>Objetivo Funcional: Consultar o extrato histórico de movimentações de um SKU.</p>
     * <p>Papel no Design Pattern: Consulta de histórico na camada de persistência.</p>
     *
     * @param sku código SKU
     * @return lista ordenada de movimentações do SKU
     */
    List<MovimentacaoEstoque> findBySkuIgnoreCaseOrderByOcorridoEmDesc(String sku);
}

