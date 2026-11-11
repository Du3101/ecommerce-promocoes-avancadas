package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado terminal de um Pedido entregue ao cliente final.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State terminal do padrão <i>State</i>. Não permite nenhuma transição subsequente.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoEntregueState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.ENTREGUE;
    }
}
