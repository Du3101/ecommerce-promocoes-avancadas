package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado que representa um Pedido em separação física e empacotamento no centro de distribuição.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete State do padrão <i>State</i>. Permite transição para {@code ENVIADO}
 * ou cancelamento antes da entrega à transportadora (transição para {@code CANCELADO}).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoEmPreparacaoState extends EstadoPedidoAbstrato {

    @Override
    public StatusPedido getStatus() {
        return StatusPedido.EM_PREPARACAO;
    }

    @Override
    public void despachar(Pedido pedido, String codigoRastreio) {
        pedido.definirCodigoRastreio(codigoRastreio);
        pedido.mudarEstado(new PedidoEnviadoState());
    }

    @Override
    public void cancelar(Pedido pedido, String motivo) {
        pedido.definirMotivoCancelamento(motivo);
        pedido.mudarEstado(new PedidoCanceladoState());
    }
}
