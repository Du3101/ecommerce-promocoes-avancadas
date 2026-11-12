package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado de pedido com pagamento confirmado e baixa definitiva de estoque realizada.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i>. Transições permitidas: {@code EM_PREPARACAO} e {@code CANCELADO} (com estorno financeiro
 * e devolução das unidades ao estoque, coordenados pelo Mediator de cancelamento).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoPagoState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#PAGO}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.PAGO;
    }

    /**
     * A expedição começa a separar o pedido.
     *
     * <p>Papel no Design Pattern: transição válida {@code PAGO -> EM_PREPARACAO}.</p>
     *
     * @param pedido agregado raiz (Context)
     */
    @Override
    public void preparar(Pedido pedido) {
        pedido.mudarEstado(new PedidoEmPreparacaoState());
    }

    /**
     * Cancelamento de pedido pago (exige estorno).
     *
     * <p>Papel no Design Pattern: transição válida {@code PAGO -> CANCELADO}.</p>
     *
     * @param pedido agregado raiz (Context)
     * @param motivo motivo do cancelamento
     */
    @Override
    public void cancelar(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoCanceladoState());
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
