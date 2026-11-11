package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado em que o pedido aguarda confirmação de liquidação do gateway financeiro (PIX, Boleto ou Cartão).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State do padrão <i>State</i>. Permite confirmar pagamento (transição para {@code PAGO}),
 * registrar recusa (transição para {@code PAGAMENTO_RECUSADO}) ou cancelar (transição para {@code CANCELADO}).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoAguardandoPagamentoState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.AGUARDANDO_PAGAMENTO;
    }

    @Override
    public void pagar(Pedido pedido, String transacaoId) {
        pedido.mudarEstado(new PedidoPagoState());
    }

    @Override
    public void recusarPagamento(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoPagamentoRecusadoState());
    }

    @Override
    public void cancelar(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoCanceladoState());
    }
}
