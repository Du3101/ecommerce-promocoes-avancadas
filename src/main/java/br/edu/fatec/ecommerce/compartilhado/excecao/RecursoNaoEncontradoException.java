package br.edu.fatec.ecommerce.compartilhado.excecao;

/**
 * Exceção lançada quando uma entidade (Produto, Categoria, Cliente, Reserva) não é localizada.
 */
public class RecursoNaoEncontradoException extends RegraNegocioModulo1Exception {

    /**
     * Constrói a exceção indicando o recurso ausente.
     *
     * <p>Objetivo Funcional: Mapear falhas de busca para HTTP 404 Not Found.</p>
     * <p>Papel no Design Pattern: Sinalização explícita de ausência quando não aplicável um Null Object.</p>
     *
     * @param mensagem detalhes do recurso não encontrado
     */
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

