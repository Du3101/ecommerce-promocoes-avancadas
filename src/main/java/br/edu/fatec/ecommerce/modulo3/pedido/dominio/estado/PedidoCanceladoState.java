package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado terminal de um Pedido que foi cancelado.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State terminal do padrão <i>State</i>. Qualquer tentativa de reativar, pagar
 * ou alterar um pedido cancelado lança {@code TransicaoEstadoInvalidaException}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoCanceladoState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.CANCELADO;
    }
}
