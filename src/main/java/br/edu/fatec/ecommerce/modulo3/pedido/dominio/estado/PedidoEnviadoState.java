package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado que representa um Pedido que foi entregue à transportadora e está em trânsito.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State do padrão <i>State</i>. Permite registrar entrega (transição para {@code ENTREGUE}).
 * O cancelamento direto neste estado é explicitamente proibido (lança exceção de transição inválida).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoEnviadoState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.ENVIADO;
    }

    @Override
    public void entregar(Pedido pedido) {
        pedido.mudarEstado(new PedidoEntregueState());
    }
}
