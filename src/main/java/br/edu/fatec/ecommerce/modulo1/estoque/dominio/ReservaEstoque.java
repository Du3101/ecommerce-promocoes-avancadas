package br.edu.fatec.ecommerce.modulo1.estoque.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.compartilhado.excecao.TransicaoEstadoReservaInvalidaException;
import br.edu.fatec.ecommerce.modulo1.configuracao.ConfiguracaoModuloCatalogoEstoqueSingleton;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agregado raiz que representa uma reserva temporária e atômica de estoque vinculada a uma operação
 * de checkout/pedido (Clarificação Q3 / FR-010, FR-011).
 *
 * <p>Aplicação Conjunta dos Padrões GoF / Catálogo (Princípio II da Constituição):</p>
 * <ul>
 *   <li><b>Static Factory Method</b> ({@link #criarReservaAtivaComExpiracao}): encapsula a criação
 *   da reserva no estado inicial {@link EstadoReservaEstoque#ATIVA} com cálculo automático do
 *   instante de expiração.</li>
 *   <li><b>Singleton</b> ({@link ConfiguracaoModuloCatalogoEstoqueSingleton}): fornece o TTL padrão
 *   de expiração da reserva quando não especificado.</li>
 * </ul>
 */
@Entity
@Table(name = "reservas_estoque")
public class ReservaEstoque {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "chave_operacao_pedido", length = 100, nullable = false, unique = true, updatable = false)
    private String chaveOperacaoPedido;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoReservaEstoque estado;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemReservaEstoque> itens = new ArrayList<>();

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta em favor do <b>Static Factory Method</b>.</p>
     */
    protected ReservaEstoque() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor privado acionado pelo Static Factory Method {@link #criarReservaAtivaComExpiracao}.
     *
     * <p>Objetivo Funcional: Inicializar a reserva ativa com seus itens e tempo limite de expiração.</p>
     * <p>Papel no Design Pattern: Construtor interno encapsulado pelo <b>Static Factory Method</b>.</p>
     */
    private ReservaEstoque(
        String chaveOperacaoPedido,
        UUID clienteId,
        Instant criadaEm,
        Instant expiraEm,
        Map<String, Integer> quantidadesPorSkuFisico
    ) {
        this.id = UUID.randomUUID();
        this.chaveOperacaoPedido = chaveOperacaoPedido.trim();
        this.clienteId = clienteId;
        this.estado = EstadoReservaEstoque.ATIVA;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
        for (Map.Entry<String, Integer> entrada : quantidadesPorSkuFisico.entrySet()) {
            this.itens.add(new ItemReservaEstoque(this, entrada.getKey(), entrada.getValue()));
        }
    }

    /**
     * Cria uma nova {@link ReservaEstoque} no estado {@link EstadoReservaEstoque#ATIVA} calculando
     * seu instante de expiração com base no TTL global do {@link ConfiguracaoModuloCatalogoEstoqueSingleton}.
     *
     * <p>Objetivo Funcional: Instanciar reserva atômica vinculada a um pedido/checkout com prazo de validade.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> que consome o <b>Singleton</b> de configuração.</p>
     *
     * @param chaveOperacaoPedido identificador único da operação de pedido/checkout
     * @param clienteId UUID do cliente comprador
     * @param quantidadesPorSkuFisico mapa de SKUs físicos folha e respectivas quantidades a reservar
     * @return nova instância ativa de {@link ReservaEstoque}
     */
    public static ReservaEstoque criarReservaAtivaComExpiracao(
        String chaveOperacaoPedido,
        UUID clienteId,
        Map<String, Integer> quantidadesPorSkuFisico
    ) {
        int ttlMinutos = ConfiguracaoModuloCatalogoEstoqueSingleton.obterInstancia()
            .obterTtlReservaEstoqueMinutos();
        Instant agora = Instant.now();
        Instant expiraEm = agora.plusSeconds(ttlMinutos * 60L);
        return criarReservaAtivaComInstanteExpiracaoExplicito(
            chaveOperacaoPedido,
            clienteId,
            quantidadesPorSkuFisico,
            agora,
            expiraEm
        );
    }

    /**
     * Cria uma nova {@link ReservaEstoque} ativa permitindo informar explicitamente os instantes
     * de criação e expiração (útil para testes determinísticos de vencimento de TTL).
     *
     * <p>Objetivo Funcional: Validar todos os parâmetros obrigatórios da reserva antes da construção.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> parametrizado.</p>
     *
     * @param chaveOperacaoPedido identificador único da operação
     * @param clienteId UUID do cliente
     * @param quantidadesPorSkuFisico mapa de SKUs físicos e quantidades ({@code >= 1})
     * @param criadaEm instante de criação
     * @param expiraEm instante limite de expiração
     * @return instância validada de {@link ReservaEstoque}
     */
    public static ReservaEstoque criarReservaAtivaComInstanteExpiracaoExplicito(
        String chaveOperacaoPedido,
        UUID clienteId,
        Map<String, Integer> quantidadesPorSkuFisico,
        Instant criadaEm,
        Instant expiraEm
    ) {
        if (chaveOperacaoPedido == null || chaveOperacaoPedido.isBlank()) {
            throw new RegraNegocioModulo1Exception(
                "A chave da operação de pedido/checkout é obrigatória para criar uma reserva."
            );
        }
        if (clienteId == null) {
            throw new RegraNegocioModulo1Exception("O identificador do cliente é obrigatório na reserva.");
        }
        if (quantidadesPorSkuFisico == null || quantidadesPorSkuFisico.isEmpty()) {
            throw new RegraNegocioModulo1Exception("A reserva deve conter ao menos um SKU físico.");
        }
        if (criadaEm == null || expiraEm == null) {
            throw new RegraNegocioModulo1Exception("Os instantes de criação e expiração são obrigatórios.");
        }
        return new ReservaEstoque(chaveOperacaoPedido, clienteId, criadaEm, expiraEm, quantidadesPorSkuFisico);
    }

    /**
     * Transiciona a reserva para {@link EstadoReservaEstoque#CONFIRMADA} de forma idempotente (FR-011).
     *
     * <p>Objetivo Funcional: Marcar a reserva como confirmada após aprovação do pagamento. Se já estiver
     * {@code CONFIRMADA}, retorna {@code false} indicando chamada repetida idempotente (sem duplicar baixa).
     * Se já estiver {@code LIBERADA} ou {@code EXPIRADA}, lança conflito 409.</p>
     * <p>Papel no Design Pattern: Máquina de estados explícita com proteção de idempotência.</p>
     *
     * @return {@code true} se a transição {@code ATIVA -> CONFIRMADA} ocorreu agora;
     *         {@code false} se já estava {@code CONFIRMADA} (idempotência)
     * @throws TransicaoEstadoReservaInvalidaException se a reserva já estava {@code LIBERADA} ou {@code EXPIRADA}
     */
    public boolean confirmarBaixaDefinitiva() {
        if (this.estado == EstadoReservaEstoque.CONFIRMADA) {
            return false; // Chamada repetida idempotente
        }
        if (this.estado == EstadoReservaEstoque.LIBERADA || this.estado == EstadoReservaEstoque.EXPIRADA) {
            throw new TransicaoEstadoReservaInvalidaException(
                "Não é possível confirmar a reserva '" + this.chaveOperacaoPedido
                    + "' pois ela já se encontra no estado " + this.estado + "."
            );
        }
        this.estado = EstadoReservaEstoque.CONFIRMADA;
        this.finalizadaEm = Instant.now();
        return true;
    }

    /**
     * Transiciona a reserva para {@link EstadoReservaEstoque#LIBERADA} de forma idempotente (FR-011).
     *
     * <p>Objetivo Funcional: Liberar a reserva por cancelamento ou pagamento recusado. Se já estiver
     * {@code LIBERADA} ou {@code EXPIRADA}, retorna {@code false} sem duplicar devolução ao estoque.
     * Se já estiver {@code CONFIRMADA}, lança conflito 409.</p>
     * <p>Papel no Design Pattern: Máquina de estados explícita com proteção de idempotência.</p>
     *
     * @return {@code true} se a transição {@code ATIVA -> LIBERADA} ocorreu agora;
     *         {@code false} se já estava {@code LIBERADA} ou {@code EXPIRADA} (idempotência)
     * @throws TransicaoEstadoReservaInvalidaException se a reserva já estava {@code CONFIRMADA}
     */
    public boolean liberarReserva() {
        if (this.estado == EstadoReservaEstoque.LIBERADA || this.estado == EstadoReservaEstoque.EXPIRADA) {
            return false; // Chamada repetida idempotente
        }
        if (this.estado == EstadoReservaEstoque.CONFIRMADA) {
            throw new TransicaoEstadoReservaInvalidaException(
                "Não é possível liberar a reserva '" + this.chaveOperacaoPedido
                    + "' pois sua baixa definitiva já foi CONFIRMADA."
            );
        }
        this.estado = EstadoReservaEstoque.LIBERADA;
        this.finalizadaEm = Instant.now();
        return true;
    }

    /**
     * Transiciona a reserva para {@link EstadoReservaEstoque#EXPIRADA} quando seu TTL venceu (FR-011).
     *
     * <p>Objetivo Funcional: Expirar automaticamente reservas abandonadas no checkout.</p>
     * <p>Papel no Design Pattern: Transição temporal de estado com proteção de idempotência.</p>
     *
     * @return {@code true} se a transição {@code ATIVA -> EXPIRADA} ocorreu agora;
     *         {@code false} caso já não estivesse ativa
     */
    public boolean expirarPorTtl() {
        if (this.estado != EstadoReservaEstoque.ATIVA) {
            return false;
        }
        this.estado = EstadoReservaEstoque.EXPIRADA;
        this.finalizadaEm = Instant.now();
        return true;
    }

    /**
     * Verifica se a reserva está ativa e já ultrapassou seu instante limite {@code expiraEm}.
     *
     * <p>Objetivo Funcional: Identificar reservas vencidas prontas para expurgo.</p>
     * <p>Papel no Design Pattern: Predicado temporal do agregado.</p>
     *
     * @param instanteReferencia instante de comparação (normalmente {@code Instant.now()})
     * @return {@code true} se estiver {@code ATIVA} e vencida
     */
    public boolean estaVencidaEm(Instant instanteReferencia) {
        return this.estado == EstadoReservaEstoque.ATIVA
            && instanteReferencia != null
            && !this.expiraEm.isAfter(instanteReferencia);
    }

    /**
     * Obtém o identificador único da reserva.
     *
     * <p>Objetivo Funcional: Identificar a reserva no banco.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID da reserva
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém a chave única da operação de pedido/checkout vinculada à reserva.
     *
     * <p>Objetivo Funcional: Permitir confirmação ou liberação idempotente pela chave de negócio.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return chave da operação
     */
    public String obterChaveOperacaoPedido() {
        return this.chaveOperacaoPedido;
    }

    /**
     * Obtém o identificador do cliente comprador.
     *
     * <p>Objetivo Funcional: Rastrear para qual cliente o estoque foi reservado.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID do cliente
     */
    public UUID obterClienteId() {
        return this.clienteId;
    }

    /**
     * Obtém o estado atual da reserva ({@link EstadoReservaEstoque}).
     *
     * <p>Objetivo Funcional: Informar situação da reserva ao checkout e pedidos.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return estado atual
     */
    public EstadoReservaEstoque obterEstado() {
        return this.estado;
    }

    /**
     * Retorna uma visão imutável dos itens físicos reservados.
     *
     * <p>Objetivo Funcional: Listar os SKUs e quantidades retidas nesta reserva.</p>
     * <p>Papel no Design Pattern: Encapsulamento defensivo da coleção do agregado.</p>
     *
     * @return lista imutável de {@link ItemReservaEstoque}
     */
    public List<ItemReservaEstoque> obterItens() {
        return Collections.unmodifiableList(this.itens);
    }

    /**
     * Obtém o instante de criação da reserva.
     *
     * <p>Objetivo Funcional: Auditoria temporal do início da reserva.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return instante UTC de criação
     */
    public Instant obterCriadaEm() {
        return this.criadaEm;
    }

    /**
     * Obtém o instante limite de validade (TTL) da reserva.
     *
     * <p>Objetivo Funcional: Informar quando a reserva expira caso o pagamento não seja confirmado.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return instante UTC de expiração
     */
    public Instant obterExpiraEm() {
        return this.expiraEm;
    }

    /**
     * Obtém o instante em que a reserva foi confirmada, liberada ou expirada.
     *
     * <p>Objetivo Funcional: Rastrear conclusão do ciclo de vida da reserva.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return instante UTC de finalização ou {@code null} se ainda ativa
     */
    public Instant obterFinalizadaEm() {
        return this.finalizadaEm;
    }
}

