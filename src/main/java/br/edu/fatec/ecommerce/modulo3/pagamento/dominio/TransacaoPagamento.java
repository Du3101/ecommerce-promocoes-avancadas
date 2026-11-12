package br.edu.fatec.ecommerce.modulo3.pagamento.dominio;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que registra a liquidação ou tentativa financeira associada a um Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a transação originada pela Camada Anticorrupção (ACL) do padrão <i>Adapter</i>
 * para os gateways simulados de pagamento.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Entity
@Table(name = "transacoes_pagamento")
public class TransacaoPagamento {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 120)
    private String chaveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo", nullable = false, length = 30)
    private MetodoPagamento metodo;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusTransacaoPagamento status;

    @Column(name = "identificador_gateway", length = 120)
    private String identificadorGateway;

    @Column(name = "comprovante_ou_qrcode", columnDefinition = "TEXT")
    private String comprovanteOuQrCode;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm;

    /**
     * Construtor protegido exigido pela JPA.
     */
    protected TransacaoPagamento() {
    }

    /**
     * Construtor completo da transação financeira.
     *
     * @param id Identificador único.
     * @param pedido Pedido associado.
     * @param chaveIdempotencia Chave única para controle de duplicação.
     * @param metodo Método de pagamento.
     * @param valor Valor da transação.
     * @param status Status inicial.
     * @param identificadorGateway Código gerado pelo gateway.
     * @param comprovanteOuQrCode Linha digitável, QR Code ou comprovante.
     */
    public TransacaoPagamento(
            UUID id,
            Pedido pedido,
            String chaveIdempotencia,
            MetodoPagamento metodo,
            BigDecimal valor,
            StatusTransacaoPagamento status,
            String identificadorGateway,
            String comprovanteOuQrCode) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pedido = Objects.requireNonNull(pedido, "Pedido é obrigatório.");
        this.chaveIdempotencia = Objects.requireNonNull(chaveIdempotencia, "Chave de idempotência é obrigatória.");
        this.metodo = Objects.requireNonNull(metodo, "Método de pagamento é obrigatório.");
        this.valor = Objects.requireNonNull(valor, "Valor é obrigatório.").setScale(2, RoundingMode.HALF_EVEN);
        this.status = status != null ? status : StatusTransacaoPagamento.PENDENTE;
        this.identificadorGateway = identificadorGateway;
        this.comprovanteOuQrCode = comprovanteOuQrCode;
        this.processadoEm = Instant.now();
    }

    /**
     * Atualiza a situação da transação financeira para confirmada.
     *
     * @param identificadorGateway Código retornado pelo gateway.
     */
    public void confirmar(String identificadorGateway) {
        this.status = StatusTransacaoPagamento.CONFIRMADA;
        if (identificadorGateway != null) {
            this.identificadorGateway = identificadorGateway;
        }
    }

    /**
     * Registra a recusa da transação.
     */
    public void recusar() {
        this.status = StatusTransacaoPagamento.RECUSADA;
    }

    /**
     * Invalida uma cobrança pendente porque o pedido foi cancelado antes do pagamento.
     *
     * <p>Papel no Design Pattern: participante coordenado pelo {@code CoordenadorCancelamentoMediator}.</p>
     */
    public void cancelar() {
        this.status = StatusTransacaoPagamento.CANCELADA;
    }

    /**
     * Registra o estorno financeiro da transação.
     */
    public void estornar() {
        this.status = StatusTransacaoPagamento.ESTORNADA;
    }

    /**
     * Retorna o ID da transação.
     *
     * @return UUID da transação.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retorna o Pedido.
     *
     * @return Pedido pai.
     */
    public Pedido getPedido() {
        return pedido;
    }

    /**
     * Retorna a chave de idempotência.
     *
     * @return Chave única.
     */
    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    /**
     * Retorna o método de pagamento.
     *
     * @return MetodoPagamento.
     */
    public MetodoPagamento getMetodo() {
        return metodo;
    }

    /**
     * Retorna o valor.
     *
     * @return Valor.
     */
    public BigDecimal getValor() {
        return valor;
    }

    /**
     * Retorna o status da transação.
     *
     * @return StatusTransacaoPagamento.
     */
    public StatusTransacaoPagamento getStatus() {
        return status;
    }

    /**
     * Retorna o identificador do gateway.
     *
     * @return Código externo.
     */
    public String getIdentificadorGateway() {
        return identificadorGateway;
    }

    /**
     * Retorna o comprovante, linha digitável ou QR Code.
     *
     * @return Comprovante em texto ou base64.
     */
    public String getComprovanteOuQrCode() {
        return comprovanteOuQrCode;
    }

    /**
     * Retorna o instante de processamento.
     *
     * @return Timestamp.
     */
    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
