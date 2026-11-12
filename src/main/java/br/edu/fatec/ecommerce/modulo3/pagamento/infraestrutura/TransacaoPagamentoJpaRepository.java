package br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TransacaoPagamento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para transações financeiras de pagamento.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Repositório de infraestrutura da Camada Anticorrupção (ACL) do padrão <i>Adapter</i>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Repository
public interface TransacaoPagamentoJpaRepository extends JpaRepository<TransacaoPagamento, UUID> {

    /**
     * Localiza a transação pela chave única de idempotência.
     *
     * @param chaveIdempotencia Chave de idempotência da requisição.
     * @return Optional contendo a transação se existir.
     */
    Optional<TransacaoPagamento> findByChaveIdempotencia(String chaveIdempotencia);

    /**
     * Localiza todas as transações vinculadas a um determinado pedido.
     *
     * @param pedidoId UUID do pedido.
     * @return Lista de transações do pedido.
     */
    List<TransacaoPagamento> findByPedidoIdOrderByProcessadoEmDesc(UUID pedidoId);

    /**
     * Localiza a transação mais recente de um pedido (cada pedido tem uma cobrança emitida no checkout).
     *
     * @param pedidoId UUID do pedido.
     * @return Optional com a transação mais recente.
     */
    Optional<TransacaoPagamento> findFirstByPedidoIdOrderByProcessadoEmDesc(UUID pedidoId);
}
