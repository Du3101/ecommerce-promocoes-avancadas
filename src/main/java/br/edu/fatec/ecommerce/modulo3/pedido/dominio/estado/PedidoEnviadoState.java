package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;

/**
 * Estado de pedido despachado e em trânsito na transportadora.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * <i>Concrete State</i>. Única transição permitida: {@code ENTREGUE}. O cancelamento direto é recusado com uma
 * mensagem específica, pois um pedido em trânsito exige o fluxo de devolução pós-entrega.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoEnviadoState extends EstadoPedidoAbstrato {

    /**
     * Retorna o status persistido deste estado.
     *
     * @return {@link StatusPedido#ENVIADO}
     */
    @Override
    public StatusPedido getStatus() {
        return StatusPedido.ENVIADO;
    }

    /**
     * Transportadora confirmou a entrega.
     *
     * <p>Papel no Design Pattern: transição válida {@code ENVIADO -> ENTREGUE}.</p>
     *
     * @param pedido agregado raiz (Context)
     */
    @Override
    public void entregar(Pedido pedido) {
        pedido.mudarEstado(new PedidoEntregueState());
    }

    /**
     * Recusa o cancelamento de um pedido em trânsito com orientação ao cliente.
     *
     * <p>Papel no Design Pattern: transição proibida com mensagem específica do estado (spec US5, cenário 3).</p>
     *
     * @param pedido agregado raiz (não alterado)
     * @param motivo motivo informado (ignorado)
     * @throws TransicaoEstadoInvalidaException sempre
     */
    @Override
    public void cancelar(Pedido pedido, String motivo) {
        throw new TransicaoEstadoInvalidaException(
                "O pedido já foi enviado e está em transporte; o cancelamento direto não é permitido. "
                        + "Aguarde a entrega e solicite o processo de devolução.");
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
