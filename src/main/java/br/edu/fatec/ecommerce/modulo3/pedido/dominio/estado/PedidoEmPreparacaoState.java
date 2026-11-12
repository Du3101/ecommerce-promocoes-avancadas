package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado em que o pedido pago está sendo separado e embalado no centro de distribuição.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i>. Transições permitidas: {@code ENVIADO} e {@code CANCELADO} (o pedido ainda não saiu
 * para a transportadora, então o cancelamento com estorno ainda é possível).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoEmPreparacaoState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#EM_PREPARACAO}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.EM_PREPARACAO;
    }

    /**
     * Pedido entregue à transportadora com código de rastreio.
     *
     * <p>Papel no Design Pattern: transição válida {@code EM_PREPARACAO -> ENVIADO}.</p>
     *
     * @param pedido agregado raiz (Context)
     * @param codigoRastreio código emitido pela transportadora
     */
    @Override
    public void despachar(Pedido pedido, String codigoRastreio) {
        pedido.definirCodigoRastreio(codigoRastreio);
        pedido.mudarEstado(new PedidoEnviadoState());
    }

    /**
     * Cancelamento antes do envio (exige estorno).
     *
     * <p>Papel no Design Pattern: transição válida {@code EM_PREPARACAO -> CANCELADO}.</p>
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
