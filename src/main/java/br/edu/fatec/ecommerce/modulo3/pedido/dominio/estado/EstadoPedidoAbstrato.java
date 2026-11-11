package br.edu.fatec.ecommerce.modulo3.pedido.dominio.estado;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.TransicaoEstadoInvalidaException;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;

/**
 * Classe base abstrata para implementação das classes de estado do Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Classe abstrata base do padrão <i>State</i>. Por padrão, todas as operações lançam
 * {@link TransicaoEstadoInvalidaException}, forçando cada subclasse concreta a sobrescrever
 * exclusivamente as transições válidas a partir do seu estado.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public abstract class EstadoPedidoAbstrato implements EstadoPedido {

    @Override
    public void pagar(Pedido pedido, String transacaoId) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Confirmar Pagamento");
    }

    @Override
    public void recusarPagamento(Pedido pedido, String motivo) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Recusar Pagamento");
    }

    @Override
    public void preparar(Pedido pedido) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Iniciar Preparação");
    }

    @Override
    public void despachar(Pedido pedido, String codigoRastreio) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Despachar Pedido");
    }

    @Override
    public void entregar(Pedido pedido) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Confirmar Entrega");
    }

    @Override
    public void cancelar(Pedido pedido, String motivo) {
        throw new TransicaoEstadoInvalidaException(getStatus(), "Cancelar Pedido");
    }
}
