package br.edu.fatec.ecommerce.compartilhado.excecao;

/**
 * Exceção lançada quando se tenta realizar uma transição inválida no ciclo de vida de uma ReservaEstoque.
 */
public class TransicaoEstadoReservaInvalidaException extends ConflitoDeEstadoOuDuplicidadeException {

    /**
     * Constrói a exceção de transição de estado ilegal na reserva de estoque.
     *
     * <p>Objetivo Funcional: Garantir integridade da reserva (impedindo confirmar reserva já liberada/expirada
     * ou liberar reserva já confirmada em baixa definitiva).</p>
     * <p>Papel no Design Pattern: Proteção de invariante de estado da ReservaEstoque.</p>
     *
     * @param mensagem detalhes do estado atual e da operação tentada
     */
    public TransicaoEstadoReservaInvalidaException(String mensagem) {
        super(mensagem);
    }
}

