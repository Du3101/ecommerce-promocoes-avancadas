package br.edu.fatec.ecommerce.modulo3.pedido.infraestrutura;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório JPA para persistência e consulta do agregado raiz {@link Pedido}.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Componente de persistência e acesso a dados (Repository Pattern), fornecendo
 * consultas otimizadas e relatórios analíticos agregados para o perfil administrativo.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Repository
public interface PedidoJpaRepository extends JpaRepository<Pedido, UUID> {

    /**
     * Localiza um pedido pelo seu número comercial legível (ex.: PED-202611-0001).
     *
     * @param numeroPedido Número do pedido.
     * @return Optional contendo o pedido se existir.
     */
    Optional<Pedido> findByNumeroPedido(String numeroPedido);

    /**
     * Localiza um pedido pela chave única de reserva enviada ao estoque (Módulo 1).
     *
     * @param chaveOperacaoEstoque Chave de operação do estoque.
     * @return Optional contendo o pedido se existir.
     */
    Optional<Pedido> findByChaveOperacaoEstoque(String chaveOperacaoEstoque);

    /**
     * Carrega o pedido pelo número comercial com bloqueio pessimista de escrita ({@code SELECT ... FOR UPDATE}).
     *
     * <p>Objetivo Funcional: serializar confirmações de pagamento concorrentes para o mesmo pedido (webhook
     * reenviado ao mesmo tempo), base da garantia de idempotência.</p>
     *
     * @param numeroPedido número do pedido
     * @return Optional com o pedido bloqueado até o fim da transação
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.numeroPedido = :numeroPedido")
    Optional<Pedido> buscarPorNumeroComLock(@Param("numeroPedido") String numeroPedido);

    /**
     * Carrega o pedido pelo ID com bloqueio pessimista de escrita.
     *
     * <p>Objetivo Funcional: serializar cancelamento, transições administrativas e confirmação de pagamento
     * concorrentes sobre o mesmo pedido (edge case "cancelamento x aprovação do gateway" da spec).</p>
     *
     * @param id UUID do pedido
     * @return Optional com o pedido bloqueado até o fim da transação
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> buscarPorIdComLock(@Param("id") UUID id);

    /**
     * Verifica se já existe pedido com o número informado (geração de números únicos).
     *
     * @param numeroPedido número candidato
     * @return {@code true} se o número já estiver em uso
     */
    boolean existsByNumeroPedido(String numeroPedido);

    /**
     * Conta clientes distintos com pedidos nos status informados.
     *
     * @param statuses status considerados
     * @return quantidade de clientes distintos
     */
    @Query("SELECT COUNT(DISTINCT p.clienteId) FROM Pedido p WHERE p.status IN :statuses")
    long contarClientesDistintosPorStatus(@Param("statuses") List<StatusPedido> statuses);

    /**
     * Agrupa os pedidos faturados por cupom: [codigoCupom, quantidadePedidos, somaDescontos].
     *
     * @param statuses status faturados
     * @return linhas agregadas ordenadas pelo maior desconto concedido
     */
    @Query("SELECT p.cupomUtilizado, COUNT(p), SUM(p.financeiro.totalDescontos) FROM Pedido p "
            + "WHERE p.status IN :statuses AND p.cupomUtilizado IS NOT NULL "
            + "GROUP BY p.cupomUtilizado ORDER BY SUM(p.financeiro.totalDescontos) DESC")
    List<Object[]> agruparDescontosPorCupom(@Param("statuses") List<StatusPedido> statuses);

    /**
     * Agrupa os pedidos cancelados ou recusados por motivo: [motivo, quantidade].
     *
     * @param statuses status de cancelamento/recusa
     * @return linhas agregadas ordenadas pela maior frequência
     */
    @Query("SELECT p.motivoCancelamento, COUNT(p) FROM Pedido p WHERE p.status IN :statuses "
            + "GROUP BY p.motivoCancelamento ORDER BY COUNT(p) DESC")
    List<Object[]> agruparMotivosCancelamento(@Param("statuses") List<StatusPedido> statuses);

    /**
     * Retorna o histórico de pedidos de um cliente ordenados do mais recente ao mais antigo.
     *
     * @param clienteId Identificador universal do cliente.
     * @return Lista de pedidos do cliente.
     */
    List<Pedido> findByClienteIdOrderByCriadoEmDesc(UUID clienteId);

    /**
     * Conta a quantidade de pedidos que atingiram determinado status.
     *
     * @param status Status do pedido.
     * @return Quantidade de pedidos.
     */
    long countByStatus(StatusPedido status);

    /**
     * Soma o total líquido faturado em pedidos com status que representam faturamento concluído (PAGO, EM_PREPARACAO, ENVIADO, ENTREGUE).
     *
     * @param statuses Lista de status faturados.
     * @return Soma monetária total.
     */
    @Query("SELECT COALESCE(SUM(p.financeiro.valorTotalLiquido), 0.00) FROM Pedido p WHERE p.status IN :statuses")
    BigDecimal somarFaturamentoLiquidoPorStatus(@Param("statuses") List<StatusPedido> statuses);

    /**
     * Soma o subtotal bruto de pedidos faturados.
     *
     * @param statuses Lista de status faturados.
     * @return Soma bruta.
     */
    @Query("SELECT COALESCE(SUM(p.financeiro.subtotalBruto), 0.00) FROM Pedido p WHERE p.status IN :statuses")
    BigDecimal somarSubtotalBrutoPorStatus(@Param("statuses") List<StatusPedido> statuses);

    /**
     * Soma os descontos promocionais concedidos em pedidos faturados.
     *
     * @param statuses Lista de status faturados.
     * @return Total de descontos.
     */
    @Query("SELECT COALESCE(SUM(p.financeiro.totalDescontos), 0.00) FROM Pedido p WHERE p.status IN :statuses")
    BigDecimal somarDescontosConcedidosPorStatus(@Param("statuses") List<StatusPedido> statuses);

    /**
     * Calcula o ticket médio de pedidos faturados.
     *
     * @param statuses Lista de status faturados.
     * @return Média monetária por pedido.
     */
    @Query("SELECT COALESCE(AVG(p.financeiro.valorTotalLiquido), 0.00) FROM Pedido p WHERE p.status IN :statuses")
    BigDecimal calcularTicketMedioPorStatus(@Param("statuses") List<StatusPedido> statuses);
}
