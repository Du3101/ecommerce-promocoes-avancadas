package br.edu.fatec.ecommerce.modulo1.estoque.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro histórico imutável de qualquer movimentação ocorrida no estoque de um SKU (FR-009, FR-010, FR-011).
 *
 * <p>Aplicação do Padrão GoF / Catálogo: <b>Static Factory Method</b> (Princípio II da Constituição).</p>
 * <p>Justificativa: O construtor é privado/protegido e a criação ocorre exclusivamente por métodos
 * fábrica estáticos com nomes explícitos para cada evento de negócio ({@link #registrarEntradaOuAjusteManual},
 * {@link #registrarRetencaoPorReserva}, {@link #registrarBaixaDefinitivaReserva},
 * {@link #registrarEstornoDeReserva}).</p>
 */
@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", length = 64, nullable = false, updatable = false)
    private String sku;

    @Column(name = "tipo_movimentacao", length = 40, nullable = false, updatable = false)
    private String tipoMovimentacao;

    @Column(name = "quantidade", nullable = false, updatable = false)
    private int quantidade;

    @Column(name = "saldo_disponivel_anterior", nullable = false, updatable = false)
    private int saldoDisponivelAnterior;

    @Column(name = "saldo_disponivel_posterior", nullable = false, updatable = false)
    private int saldoDisponivelPosterior;

    @Column(name = "motivo", length = 255, nullable = false, updatable = false)
    private String motivo;

    @Column(name = "autor_operacao", length = 180, nullable = false, updatable = false)
    private String autorOperacao;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir leitura pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta em favor dos <b>Static Factory Methods</b>.</p>
     */
    protected MovimentacaoEstoque() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor privado acionado pelos Static Factory Methods.
     *
     * <p>Objetivo Funcional: Inicializar todos os campos imutáveis da movimentação de estoque.</p>
     * <p>Papel no Design Pattern: Construtor interno encapsulado pelo padrão <b>Static Factory Method</b>.</p>
     */
    private MovimentacaoEstoque(
        String sku,
        String tipoMovimentacao,
        int quantidade,
        int saldoDisponivelAnterior,
        int saldoDisponivelPosterior,
        String motivo,
        String autorOperacao
    ) {
        this.id = UUID.randomUUID();
        this.sku = sku;
        this.tipoMovimentacao = tipoMovimentacao;
        this.quantidade = quantidade;
        this.saldoDisponivelAnterior = saldoDisponivelAnterior;
        this.saldoDisponivelPosterior = saldoDisponivelPosterior;
        this.motivo = motivo;
        this.autorOperacao = autorOperacao != null ? autorOperacao : "SISTEMA";
        this.ocorridoEm = Instant.now();
    }

    /**
     * Cria uma movimentação de entrada de lote ou ajuste manual administrativo de estoque (FR-009).
     *
     * <p>Objetivo Funcional: Documentar reposições ou baixas manuais com saldo anterior/posterior e motivo.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico para ajustes manuais.</p>
     *
     * @param sku código SKU ajustado
     * @param deltaQuantidade variação aplicada
     * @param saldoAnterior saldo disponível antes do ajuste
     * @param saldoPosterior saldo disponível após o ajuste
     * @param motivo justificativa operacional
     * @param autor identificação do responsável de estoque ou admin
     * @return instância imutável de {@link MovimentacaoEstoque}
     */
    public static MovimentacaoEstoque registrarEntradaOuAjusteManual(
        String sku,
        int deltaQuantidade,
        int saldoAnterior,
        int saldoPosterior,
        String motivo,
        String autor
    ) {
        String tipo = deltaQuantidade >= 0 ? "ENTRADA_REPOSICAO_MANUAL" : "SAIDA_AJUSTE_MANUAL";
        return new MovimentacaoEstoque(
            sku,
            tipo,
            Math.abs(deltaQuantidade),
            saldoAnterior,
            saldoPosterior,
            motivo,
            autor
        );
    }

    /**
     * Cria uma movimentação de retenção de unidades por reserva de checkout (FR-010).
     *
     * <p>Objetivo Funcional: Registrar o bloqueio temporário de saldo disponível para uma reserva ativa.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico para retenção em reserva.</p>
     *
     * @param sku código SKU reservado
     * @param quantidade quantidade reservada
     * @param saldoAnterior saldo disponível antes da reserva
     * @param saldoPosterior saldo disponível após a reserva
     * @param chaveOperacaoPedido chave do pedido/checkout
     * @param autor autor ou cliente
     * @return instância imutável de {@link MovimentacaoEstoque}
     */
    public static MovimentacaoEstoque registrarRetencaoPorReserva(
        String sku,
        int quantidade,
        int saldoAnterior,
        int saldoPosterior,
        String chaveOperacaoPedido,
        String autor
    ) {
        return new MovimentacaoEstoque(
            sku,
            "RETENCAO_RESERVA_CHECKOUT",
            quantidade,
            saldoAnterior,
            saldoPosterior,
            "Reserva de estoque para operação: " + chaveOperacaoPedido,
            autor
        );
    }

    /**
     * Cria uma movimentação de baixa definitiva após confirmação do pagamento de uma reserva (FR-011).
     *
     * <p>Objetivo Funcional: Registrar a saída definitiva das unidades que estavam reservadas.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico para confirmação de venda.</p>
     *
     * @param sku código SKU baixado
     * @param quantidade quantidade baixada
     * @param saldoDisponivelAtual saldo disponível vigente
     * @param chaveOperacaoPedido chave da reserva confirmada
     * @param autor autor da confirmação
     * @return instância imutável de {@link MovimentacaoEstoque}
     */
    public static MovimentacaoEstoque registrarBaixaDefinitivaReserva(
        String sku,
        int quantidade,
        int saldoDisponivelAtual,
        String chaveOperacaoPedido,
        String autor
    ) {
        return new MovimentacaoEstoque(
            sku,
            "BAIXA_DEFINITIVA_VENDA",
            quantidade,
            saldoDisponivelAtual,
            saldoDisponivelAtual,
            "Baixa definitiva da reserva confirmada: " + chaveOperacaoPedido,
            autor
        );
    }

    /**
     * Cria uma movimentação de estorno/devolução de unidades reservadas ao estoque disponível (FR-011).
     *
     * <p>Objetivo Funcional: Registrar o retorno de unidades ao saldo disponível por cancelamento ou TTL.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico para estorno de reserva.</p>
     *
     * @param sku código SKU estornado
     * @param quantidade quantidade devolvida ao disponível
     * @param saldoAnterior saldo disponível antes da devolução
     * @param saldoPosterior saldo disponível após a devolução
     * @param motivoEstorno motivo do cancelamento ou expiração
     * @param autor autor da liberação
     * @return instância imutável de {@link MovimentacaoEstoque}
     */
    public static MovimentacaoEstoque registrarEstornoDeReserva(
        String sku,
        int quantidade,
        int saldoAnterior,
        int saldoPosterior,
        String motivoEstorno,
        String autor
    ) {
        return new MovimentacaoEstoque(
            sku,
            "ESTORNO_LIBERACAO_RESERVA",
            quantidade,
            saldoAnterior,
            saldoPosterior,
            motivoEstorno,
            autor
        );
    }

    /**
     * Obtém o identificador único da movimentação.
     *
     * <p>Objetivo Funcional: Identificar o evento no histórico.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return UUID da movimentação
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o código SKU movimentado.
     *
     * <p>Objetivo Funcional: Identificar o item afetado.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return código SKU
     */
    public String obterSku() {
        return this.sku;
    }

    /**
     * Obtém o tipo semântico da movimentação.
     *
     * <p>Objetivo Funcional: Classificar o evento de estoque.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return tipo da movimentação
     */
    public String obterTipoMovimentacao() {
        return this.tipoMovimentacao;
    }

    /**
     * Obtém a quantidade movimentada.
     *
     * <p>Objetivo Funcional: Informar o volume de unidades do evento.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return quantidade positiva
     */
    public int obterQuantidade() {
        return this.quantidade;
    }

    /**
     * Obtém o saldo disponível antes da movimentação.
     *
     * <p>Objetivo Funcional: Auditoria de saldo anterior.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return saldo disponível anterior
     */
    public int obterSaldoDisponivelAnterior() {
        return this.saldoDisponivelAnterior;
    }

    /**
     * Obtém o saldo disponível após a movimentação.
     *
     * <p>Objetivo Funcional: Auditoria de saldo posterior.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return saldo disponível posterior
     */
    public int obterSaldoDisponivelPosterior() {
        return this.saldoDisponivelPosterior;
    }

    /**
     * Obtém o motivo registrado para a movimentação.
     *
     * <p>Objetivo Funcional: Explicar a razão comercial ou operacional da mudança.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return motivo descritivo
     */
    public String obterMotivo() {
        return this.motivo;
    }

    /**
     * Obtém o autor da operação de estoque.
     *
     * <p>Objetivo Funcional: Identificar o operador ou sistema responsável.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return autor da operação
     */
    public String obterAutorOperacao() {
        return this.autorOperacao;
    }

    /**
     * Obtém o instante UTC em que a movimentação ocorreu.
     *
     * <p>Objetivo Funcional: Carimbo de tempo do evento.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade imutável.</p>
     *
     * @return instante UTC
     */
    public Instant obterOcorridoEm() {
        return this.ocorridoEm;
    }
}

