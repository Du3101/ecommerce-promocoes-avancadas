package br.edu.fatec.ecommerce.modulo3.compartilhado.excecao;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Exceção lançada quando há uma tentativa ilegal de transição no ciclo de vida do Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Disparada pelos métodos de comportamento das classes concretas do padrão <i>State</i>
 * (ex.: tentar pagar um pedido já cancelado, ou cancelar um pedido já entregue).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class TransicaoEstadoInvalidaException extends RegraNegocioModulo3Exception {

    private final StatusPedido statusOrigem;
    private final String operacaoTentada;

    /**
     * Construtor da exceção detalhando o estado de origem e a ação inválida solicitada.
     *
     * @param statusOrigem Estado em que o pedido se encontra atualmente.
     * @param operacaoTentada Ação que foi bloqueada pelo objeto de estado.
     */
    public TransicaoEstadoInvalidaException(StatusPedido statusOrigem, String operacaoTentada) {
        super(String.format("Operação '%s' não é permitida para o pedido no estado atual '%s'.",
                operacaoTentada, statusOrigem.getDescricaoAmigavel()));
        this.statusOrigem = statusOrigem;
        this.operacaoTentada = operacaoTentada;
    }

    /**
     * Construtor alternativo com mensagem personalizada.
     *
     * @param mensagem Mensagem detalhada da violação de estado.
     */
    public TransicaoEstadoInvalidaException(String mensagem) {
        super(mensagem);
        this.statusOrigem = null;
        this.operacaoTentada = null;
    }

    /**
     * Retorna o status de origem que rejeitou a transição.
     *
     * @return StatusPedido atual.
     */
    public StatusPedido getStatusOrigem() {
        return statusOrigem;
    }

    /**
     * Retorna o nome da operação bloqueada.
     *
     * @return Descrição da operação inválida.
     */
    public String getOperacaoTentada() {
        return operacaoTentada;
    }
}
