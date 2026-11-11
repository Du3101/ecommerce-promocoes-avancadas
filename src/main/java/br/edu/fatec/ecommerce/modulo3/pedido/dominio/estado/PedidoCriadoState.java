package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado inicial de um Pedido recém-registrado na base.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State do padrão <i>State</i>. Permite transição para {@code AGUARDANDO_PAGAMENTO},
 * {@code PAGO} (se aprovado online), {@code PAGAMENTO_RECUSADO} ou {@code CANCELADO}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoCriadoState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.CRIADO;
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
