package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado que representa um Pedido com quitação financeira confirmada e estoque baixado.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State do padrão <i>State</i>. Permite transição para {@code EM_PREPARACAO}
 * ou cancelamento excepcional com estorno pré-expedição (transição para {@code CANCELADO}).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoPagoState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.PAGO;
    }

    @Override
    public void preparar(Pedido pedido) {
        pedido.mudarEstado(new PedidoEmPreparacaoState());
    }

    @Override
    public void cancelar(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoCanceladoState());
    }
}
