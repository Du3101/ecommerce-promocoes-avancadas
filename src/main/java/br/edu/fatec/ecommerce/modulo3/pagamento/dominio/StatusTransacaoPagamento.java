package br.edu.fatec.ecommerce.modulo3.pagamento.dominio;

/**
 * Situação transacional de uma cobrança ou liquidação financeira no gateway.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa o estado imutável da transação registrado na Camada Anticorrupção (ACL)
 * do padrão <i>Adapter</i>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public enum StatusTransacaoPagamento {

    /**
     * Transação financeira criada e aguardando liquidação pelo cliente/banco.
     */
    PENDENTE,

    /**
     * Cobrança liquidada e aprovada com sucesso pela instituição financeira.
     */
    CONFIRMADA,

    /**
     * Cobrança recusada por falta de limite, dados inválidos ou suspeita de fraude.
     */
    RECUSADA,

    /**
     * Valor integralmente estornado de volta ao cliente em razão de cancelamento.
     */
    ESTORNADA,

    /**
     * Cobrança pendente (PIX/boleto) invalidada porque o pedido foi cancelado antes do pagamento.
     */
    CANCELADA
}
