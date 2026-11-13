package br.edu.fatec.ecommerce.modulo3.compartilhado.excecao;

import br.edu.fatec.ecommerce.compartilhado.excecao.RecursoNaoEncontradoException;
import java.util.UUID;

/**
 * Exceção lançada quando uma consulta a um Pedido não localiza o registro solicitado.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Especialização de {@code RecursoNaoEncontradoException} traduzida pela camada HTTP REST
 * no código de status 404 Not Found.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoNaoEncontradoException extends RecursoNaoEncontradoException {

    /**
     * Construtor da exceção utilizando o identificador UUID do pedido.
     *
     * @param pedidoId Identificador único universal não encontrado.
     */
    public PedidoNaoEncontradoException(UUID pedidoId) {
        super(String.format("Pedido com identificador '%s' não foi encontrado.", pedidoId));
    }

    /**
     * Construtor da exceção utilizando o número amigável de pedido.
     *
     * @param numeroPedido Código textual legível do pedido (ex.: PED-202611-0001).
     */
    public PedidoNaoEncontradoException(String numeroPedido) {
        super(String.format("Pedido com número '%s' não foi encontrado.", numeroPedido));
    }
}
