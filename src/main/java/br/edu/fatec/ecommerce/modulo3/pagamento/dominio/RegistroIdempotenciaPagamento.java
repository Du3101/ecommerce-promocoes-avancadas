package br.edu.fatec.ecommerce.modulo3.pagamento.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidade de controle de idempotência estrita para webhooks e operações concorrentes de pagamento.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Implementa a salvaguarda de <i>Idempotência Estrita</i> (Constituição, Princípio IV).
 * Garante que a mesma notificação repetida pelo gateway não resulte em duplicidade de baixa
 * de estoque, múltiplos eventos ou transições duplicadas no pedido.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Entity
@Table(name = "registro_idempotencia_pagamento")
public class RegistroIdempotenciaPagamento {

    @Id
    @Column(name = "chave_operacao", nullable = false, length = 120)
    private String chaveOperacao;

    @Column(name = "status_resposta", nullable = false, length = 40)
    private String statusResposta;

    @Column(name = "payload_resposta_json", nullable = false, columnDefinition = "TEXT")
    private String payloadRespostaJson;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    /**
     * Construtor protegido exigido pelo JPA.
     */
    protected RegistroIdempotenciaPagamento() {
    }

    /**
     * Construtor completo do registro de idempotência.
     *
     * @param chaveOperacao Chave primária natural e única da operação.
     * @param statusResposta Identificador do resultado processado.
     * @param payloadRespostaJson Corpo da resposta serializado para retorno repetido.
     */
    public RegistroIdempotenciaPagamento(String chaveOperacao, String statusResposta, String payloadRespostaJson) {
        this.chaveOperacao = Objects.requireNonNull(chaveOperacao, "Chave da operação é obrigatória.");
        this.statusResposta = Objects.requireNonNull(statusResposta, "Status da resposta é obrigatório.");
        this.payloadRespostaJson = Objects.requireNonNull(payloadRespostaJson, "Payload JSON é obrigatório.");
        this.registradoEm = Instant.now();
    }

    /**
     * Retorna a chave única da operação.
     *
     * @return Chave de idempotência.
     */
    public String getChaveOperacao() {
        return chaveOperacao;
    }

    /**
     * Retorna o status gravado.
     *
     * @return Status da resposta.
     */
    public String getStatusResposta() {
        return statusResposta;
    }

    /**
     * Retorna o payload em formato JSON retornado na primeira execução.
     *
     * @return JSON serializado.
     */
    public String getPayloadRespostaJson() {
        return payloadRespostaJson;
    }

    /**
     * Retorna o instante de registro original.
     *
     * @return Timestamp.
     */
    public Instant getRegistradoEm() {
        return registradoEm;
    }
}
