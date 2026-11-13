package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado terminal de pedido cujo pagamento foi recusado pelo gateway (reserva de estoque liberada).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i> terminal. O cliente precisa fazer um novo checkout para tentar outra forma de pagamento.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoPagamentoRecusadoState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#PAGAMENTO_RECUSADO}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.PAGAMENTO_RECUSADO;
    }
}
