package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado em que o pedido aguarda a confirmação do pagamento pelo gateway (webhook de PIX ou boleto).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i>. Transições permitidas: {@code PAGO}, {@code PAGAMENTO_RECUSADO} e {@code CANCELADO}.
 * A reserva de estoque continua {@code ATIVA} no Módulo 1 enquanto o pedido estiver neste estado.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoAguardandoPagamentoState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#AGUARDANDO_PAGAMENTO}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.AGUARDANDO_PAGAMENTO;
    }

    /**
     * Webhook confirmou a liquidação.
     *
     * <p>Papel no Design Pattern: transição válida {@code AGUARDANDO_PAGAMENTO -> PAGO}.</p>
     *
     * @param pedido agregado raiz (Context)
     * @param transacaoId identificador da liquidação no gateway
     */
    @Override
    public void pagar(Pedido pedido, String transacaoId) {
        pedido.mudarEstado(new PedidoPagoState());
    }

    /**
     * Webhook informou a recusa do pagamento.
     *
     * <p>Papel no Design Pattern: transição válida {@code AGUARDANDO_PAGAMENTO -> PAGAMENTO_RECUSADO}.</p>
     *
     * @param pedido agregado raiz (Context)
     * @param motivo motivo informado pelo gateway
     */
    @Override
    public void recusarPagamento(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoPagamentoRecusadoState());
    }

    /**
     * Cancelamento antes da confirmação do pagamento (sem estorno financeiro).
     *
     * <p>Papel no Design Pattern: transição válida {@code AGUARDANDO_PAGAMENTO -> CANCELADO}.</p>
     *
     * @param pedido agregado raiz (Context)
     * @param motivo motivo do cancelamento
     */
    @Override
    public void cancelar(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoCanceladoState());
    }
}
