package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado inicial de um Pedido recém-registrado no checkout.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i>. Transições permitidas: {@code AGUARDANDO_PAGAMENTO} (PIX/boleto), {@code PAGO}
 * (cartão aprovado online), {@code PAGAMENTO_RECUSADO} (cartão recusado) e {@code CANCELADO}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoCriadoState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#CRIADO}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.CRIADO;
    }

    /**
     * Cobrança assíncrona emitida (PIX/boleto): o pedido passa a aguardar a confirmação do gateway.
     *
     * <p>Papel no Design Pattern: transição válida {@code CRIADO -> AGUARDANDO_PAGAMENTO}.</p>
     *
     * @param pedido agregado raiz (Context)
     */
    @Override
    public void aguardarPagamento(Pedido pedido) {
        pedido.mudarEstado(new PedidoAguardandoPagamentoState());
    }

    /**
     * Pagamento aprovado online no próprio checkout (cartão de crédito).
     *
     * <p>Papel no Design Pattern: transição válida {@code CRIADO -> PAGO}.</p>
     *
     * @param pedido agregado raiz (Context)
     * @param transacaoId identificador da autorização no gateway
     */
    @Override
    public void pagar(Pedido pedido, String transacaoId) {
        pedido.mudarEstado(new PedidoPagoState());
    }

    /**
     * Pagamento recusado online no próprio checkout (cartão de crédito).
     *
     * <p>Papel no Design Pattern: transição válida {@code CRIADO -> PAGAMENTO_RECUSADO}.</p>
     *
     * @param pedido agregado raiz (Context)
     * @param motivo motivo informado pela operadora
     */
    @Override
    public void recusarPagamento(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoPagamentoRecusadoState());
    }

    /**
     * Cancelamento antes de qualquer cobrança.
     *
     * <p>Papel no Design Pattern: transição válida {@code CRIADO -> CANCELADO}.</p>
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
