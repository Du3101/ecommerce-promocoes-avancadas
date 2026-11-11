package br.edu.fatec.ecommerce.modulo3.pedido.dominio;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.EstadoPedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoAguardandoPagamentoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoCanceladoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoCriadoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoEmPreparacaoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoEntregueState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoEnviadoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoPagamentoRecusadoState;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado.PedidoPagoState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado raiz que representa a transação de um Pedido de compra no e-commerce.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Context</i> no padrão <b>State</b> (GoF State Pattern), delegando todas as transições
 * de ciclo de vida para a instância polimórfica de {@link EstadoPedido}.
 * Além disso, encapsula o <i>Snapshot Comercial Imutável</i> (Constituição, Princípio IV) com
 * {@link EnderecoEntregaSnapshot}, {@link SnapshotFinanceiro} e a lista de {@link ItemPedidoSnapshot}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    private UUID id;

    @Column(name = "numero_pedido", nullable = false, unique = true, length = 40)
    private String numeroPedido;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusPedido status;

    @Transient
    private EstadoPedido estadoAtual;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false, length = 30)
    private MetodoPagamento metodoPagamento;

    @Column(name = "modalidade_frete", nullable = false, length = 50)
    private String modalidadeFrete;

    @Column(name = "prazo_entrega_dias", nullable = false)
    private int prazoEntregaDias;

    @Column(name = "codigo_rastreio", length = 60)
    private String codigoRastreio;

    @Column(name = "chave_operacao_estoque", nullable = false, unique = true, length = 120)
    private String chaveOperacaoEstoque;

    @Embedded
    private SnapshotFinanceiro financeiro;

    @Embedded
    private EnderecoEntregaSnapshot enderecoEntrega;

    @Column(name = "motivo_cancelamento", length = 255)
    private String motivoCancelamento;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemPedidoSnapshot> itens = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    /**
     * Construtor protegido padrão exigido pela JPA.
     */
    protected Pedido() {
    }

    /**
     * Construtor de criação de um novo Pedido a partir do checkout.
     *
     * @param id Identificador universal do pedido.
     * @param numeroPedido Código comercial legível (ex.: PED-202611-0001).
     * @param clienteId Identificador do cliente comprador (Módulo 1).
     * @param metodoPagamento Modalidade de pagamento selecionada.
     * @param modalidadeFrete Nome da opção de frete escolhida.
     * @param prazoEntregaDias Prazo previsto de entrega em dias úteis.
     * @param chaveOperacaoEstoque Chave de idempotência enviada para reserva no Módulo 1.
     * @param enderecoEntrega Snapshot do endereço de entrega selecionado.
     * @param financeiro Memória financeira calculada no Módulo 2 e frete.
     */
    public Pedido(
            UUID id,
            String numeroPedido,
            UUID clienteId,
            MetodoPagamento metodoPagamento,
            String modalidadeFrete,
            int prazoEntregaDias,
            String chaveOperacaoEstoque,
            EnderecoEntregaSnapshot enderecoEntrega,
            SnapshotFinanceiro financeiro) {
        this.id = id != null ? id : UUID.randomUUID();
        this.numeroPedido = Objects.requireNonNull(numeroPedido, "Número do pedido é obrigatório.");
        this.clienteId = Objects.requireNonNull(clienteId, "ID do cliente é obrigatório.");
        this.metodoPagamento = Objects.requireNonNull(metodoPagamento, "Método de pagamento é obrigatório.");
        this.modalidadeFrete = Objects.requireNonNull(modalidadeFrete, "Modalidade de frete é obrigatória.");
        this.prazoEntregaDias = Math.max(1, prazoEntregaDias);
        this.chaveOperacaoEstoque = Objects.requireNonNull(chaveOperacaoEstoque, "Chave de operação é obrigatória.");
        this.enderecoEntrega = Objects.requireNonNull(enderecoEntrega, "Endereço de entrega é obrigatório.");
        this.financeiro = Objects.requireNonNull(financeiro, "Snapshot financeiro é obrigatório.");

        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
        this.mudarEstado(new PedidoCriadoState());
    }

    /**
     * Reconstitui o objeto polimórfico de estado do padrão State após carregamento do banco de dados.
     */
    @PostLoad
    protected void inicializarEstadoPolimorfico() {
        if (this.status == null) {
            this.estadoAtual = new PedidoCriadoState();
            return;
        }
        this.estadoAtual = switch (this.status) {
            case CRIADO -> new PedidoCriadoState();
            case AGUARDANDO_PAGAMENTO -> new PedidoAguardandoPagamentoState();
            case PAGO -> new PedidoPagoState();
            case EM_PREPARACAO -> new PedidoEmPreparacaoState();
            case ENVIADO -> new PedidoEnviadoState();
            case ENTREGUE -> new PedidoEntregueState();
            case CANCELADO -> new PedidoCanceladoState();
            case PAGAMENTO_RECUSADO -> new PedidoPagamentoRecusadoState();
        };
    }

    /**
     * Altera o estado polimórfico do pedido e sincroniza o enum de persistência.
     *
     * @param novoEstado Nova instância concreta do padrão State.
     */
    public void mudarEstado(EstadoPedido novoEstado) {
        this.estadoAtual = Objects.requireNonNull(novoEstado, "Novo estado não pode ser nulo.");
        this.status = novoEstado.getStatus();
        this.atualizadoEm = Instant.now();
    }

    /**
     * Avança o pedido para aguardando confirmação de liquidação.
     */
    public void aguardarConfirmacaoPagamento() {
        this.mudarEstado(new PedidoAguardandoPagamentoState());
    }

    /**
     * Confirmação de quitação financeira delegada para o padrão State.
     *
     * @param transacaoId Identificador retornado pelo gateway.
     */
    public void confirmarPagamento(String transacaoId) {
        garantirEstadoInicializado();
        this.estadoAtual.pagar(this, transacaoId);
    }

    /**
     * Recusa definitiva de pagamento delegada para o padrão State.
     *
     * @param motivo Justificativa da recusa.
     */
    public void recusarPagamento(String motivo) {
        garantirEstadoInicializado();
        this.estadoAtual.recusarPagamento(this, motivo);
    }

    /**
     * Início de preparação e separação física delegado para o padrão State.
     */
    public void iniciarPreparacao() {
        garantirEstadoInicializado();
        this.estadoAtual.preparar(this);
    }

    /**
     * Despacho para transporte com geração de rastreio delegado para o padrão State.
     *
     * @param codigoRastreio Código de rastreamento da transportadora.
     */
    public void despacharParaEntrega(String codigoRastreio) {
        garantirEstadoInicializado();
        this.estadoAtual.despachar(this, codigoRastreio);
    }

    /**
     * Confirmação de entrega final delegada para o padrão State.
     */
    public void confirmarEntrega() {
        garantirEstadoInicializado();
        this.estadoAtual.entregar(this);
    }

    /**
     * Cancelamento do pedido delegado para o padrão State.
     *
     * @param motivo Justificativa gravada no pedido.
     */
    public void cancelar(String motivo) {
        garantirEstadoInicializado();
        this.estadoAtual.cancelar(this, motivo);
    }

    /**
     * Adiciona uma linha de item imutável ao pedido.
     *
     * @param item Linha de snapshot do item.
     */
    public void adicionarItem(ItemPedidoSnapshot item) {
        if (item != null) {
            item.associarPedido(this);
            this.itens.add(item);
        }
    }

    /**
     * Grava o código de rastreamento logístico.
     *
     * @param codigoRastreio Código emitido pela transportadora.
     */
    public void definirCodigoRastreio(String codigoRastreio) {
        this.codigoRastreio = codigoRastreio;
    }

    /**
     * Grava o motivo de cancelamento.
     *
     * @param motivo Justificativa do cancelamento.
     */
    public void definirMotivoCancelamento(String motivo) {
        this.motivoCancelamento = motivo;
    }

    /**
     * Garante que o estado polimórfico esteja inicializado antes de invocar métodos.
     */
    private void garantirEstadoInicializado() {
        if (this.estadoAtual == null) {
            inicializarEstadoPolimorfico();
        }
    }

    // --- Getters ---

    public UUID getId() {
        return id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public EstadoPedido getEstadoAtual() {
        garantirEstadoInicializado();
        return estadoAtual;
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    public String getModalidadeFrete() {
        return modalidadeFrete;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public String getCodigoRastreio() {
        return codigoRastreio;
    }

    public String getChaveOperacaoEstoque() {
        return chaveOperacaoEstoque;
    }

    public SnapshotFinanceiro getFinanceiro() {
        return financeiro;
    }

    public EnderecoEntregaSnapshot getEnderecoEntrega() {
        return enderecoEntrega;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public List<ItemPedidoSnapshot> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
