package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado terminal de pedido cancelado (com reserva liberada ou estoque devolvido e, se pago, estorno simulado).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i> terminal. Qualquer operação posterior — inclusive um webhook de pagamento atrasado — é
 * recusada com {@code TransicaoEstadoInvalidaException}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoCanceladoState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#CANCELADO}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.CANCELADO;
    }
}
