package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado terminal de sucesso: o pedido foi entregue ao destinatário.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i> terminal. Não sobrescreve nenhuma transição, então qualquer operação é recusada pela
 * política "negar por padrão" de {@link EstadoPedidoAbstrato}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoEntregueState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#ENTREGUE}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.ENTREGUE;
    }

    /**
     * Neste estado o pagamento já foi capturado.
     *
     * @return {@code true}
     */
    @Override
    public boolean pagamentoJaCapturado() {
        return true;
    }
}
