package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado terminal de um Pedido cujo pagamento foi expressamente recusado pelo gateway financeiro.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State terminal do padrão <i>State</i>. Estado de encerramento sem cobrança efetiva.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoPagamentoRecusadoState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.PAGAMENTO_RECUSADO;
    }
}
