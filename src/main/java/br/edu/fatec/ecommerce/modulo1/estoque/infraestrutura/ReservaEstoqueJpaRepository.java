package br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura;

import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ReservaEstoque;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para persistência e consulta de agregados {@link ReservaEstoque}.
 *
 * <p>Responsabilidade na Arquitetura: Suportar busca por chave de operação de pedido e varredura
 * de reservas vencidas por TTL (FR-010, FR-011).</p>
 */
@Repository
public interface ReservaEstoqueJpaRepository extends JpaRepository<ReservaEstoque, UUID> {

    /**
     * Busca uma reserva pela chave única da operação de pedido/checkout.
     *
     * <p>Objetivo Funcional: Localizar reserva para consulta ou verificação de idempotência.</p>
     * <p>Papel no Design Pattern: Consulta de agregado por chave de negócio.</p>
     *
     * @param chaveOperacaoPedido chave da operação
     * @return {@link Optional} contendo a {@link ReservaEstoque}
     */
    Optional<ReservaEstoque> findByChaveOperacaoPedido(String chaveOperacaoPedido);

    /**
     * Busca uma reserva pela chave da operação aplicando lock pessimista para transição de estado segura.
     *
     * <p>Objetivo Funcional: Serializar chamadas concorrentes de confirmação, liberação ou expiração
     * sobre a mesma reserva.</p>
     * <p>Papel no Design Pattern: Garantia de idempotência sob concorrência.</p>
     *
     * @param chaveOperacaoPedido chave da operação
     * @return {@link Optional} contendo a {@link ReservaEstoque} bloqueada para escrita
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaEstoque r WHERE r.chaveOperacaoPedido = :chave")
    Optional<ReservaEstoque> buscarPorChaveOperacaoComLock(@Param("chave") String chaveOperacaoPedido);

    /**
     * Lista todas as reservas em determinado estado cujo instante {@code expiraEm} seja anterior ou igual
     * ao instante de referência informado.
     *
     * <p>Objetivo Funcional: Identificar reservas {@code ATIVA} vencidas para devolução automática ao estoque.</p>
     * <p>Papel no Design Pattern: Consulta de expurgo por TTL.</p>
     *
     * @param estado estado pesquisado (ex.: {@link EstadoReservaEstoque#ATIVA})
     * @param instanteReferencia instante limite de comparação
     * @return lista de reservas vencidas
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaEstoque r WHERE r.estado = :estado AND r.expiraEm <= :instanteReferencia")
    List<ReservaEstoque> buscarReservasVencidasComLock(
        @Param("estado") EstadoReservaEstoque estado,
        @Param("instanteReferencia") Instant instanteReferencia
    );
}

