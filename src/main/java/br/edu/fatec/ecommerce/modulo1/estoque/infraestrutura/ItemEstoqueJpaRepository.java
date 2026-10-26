package br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e controle concorrente de {@link ItemEstoque}.
 *
 * <p>Responsabilidade na Arquitetura: Fornecer consultas simples e consultas com bloqueio
 * pessimista de escrita ({@code PESSIMISTIC_WRITE / SELECT ... FOR UPDATE}) ordenadas por SKU
 * para prevenir deadlocks e overselling em reservas simultâneas (FR-009, FR-010).</p>
 */
@Repository
public interface ItemEstoqueJpaRepository extends JpaRepository<ItemEstoque, String> {

    /**
     * Busca a posição de estoque de um único SKU aplicando lock pessimista de escrita ({@code FOR UPDATE}).
     *
     * <p>Objetivo Funcional: Serializar ajustes e reservas concorrentes sobre o mesmo SKU.</p>
     * <p>Papel no Design Pattern: Controle transacional de concorrência na camada de persistência.</p>
     *
     * @param sku código SKU em maiúsculas
     * @return {@link Optional} contendo o {@link ItemEstoque} bloqueado para a transação atual
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ItemEstoque i WHERE UPPER(i.sku) = UPPER(:sku)")
    Optional<ItemEstoque> buscarPorSkuComLockPessimista(@Param("sku") String sku);

    /**
     * Busca múltiplos itens de estoque com lock pessimista de escrita ordenados alfabeticamente pelo SKU.
     *
     * <p>Objetivo Funcional: Adquirir locks de banco sempre em ordem determinística (A -> Z) durante a
     * reserva de carrinhos ou combos com múltiplos SKUs, eliminando qualquer risco de deadlock.</p>
     * <p>Papel no Design Pattern: Prevenção de deadlock em transações multi-item.</p>
     *
     * @param skus coleção de códigos SKU em maiúsculas
     * @return lista ordenada de {@link ItemEstoque} com lock pessimista adquirido
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM ItemEstoque i WHERE i.sku IN :skus ORDER BY i.sku ASC")
    List<ItemEstoque> buscarMultiplosPorSkusComLockOrdenado(@Param("skus") Collection<String> skus);

    /**
     * Busca uma posição de estoque pelo SKU ignorando caixa (sem lock pessimista, para leitura rápida).
     *
     * <p>Objetivo Funcional: Consultar disponibilidade de estoque na vitrine e no catálogo.</p>
     * <p>Papel no Design Pattern: Consulta somente leitura de repositório.</p>
     *
     * @param sku código SKU
     * @return {@link Optional} contendo o {@link ItemEstoque} se cadastrado
     */
    Optional<ItemEstoque> findBySkuIgnoreCase(String sku);
}

