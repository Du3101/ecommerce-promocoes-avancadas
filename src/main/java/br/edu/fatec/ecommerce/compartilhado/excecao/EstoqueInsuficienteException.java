package br.edu.fatec.ecommerce.compartilhado.excecao;

/**
 * Exceção lançada quando a quantidade solicitada para reserva ou ajuste excede o saldo físico disponível.
 */
public class EstoqueInsuficienteException extends ConflitoDeEstadoOuDuplicidadeException {

    /**
     * Constrói a exceção de falta de estoque disponível.
     *
     * <p>Objetivo Funcional: Impedir vendas sem cobertura física (overselling) e retornar HTTP 409 Conflict.</p>
     * <p>Papel no Design Pattern: Lançada na reserva atômica de produtos simples (Leaf) ou Combos (<b>Composite</b>).</p>
     *
     * @param mensagem detalhes do SKU e quantidades envolvidas
     */
    public EstoqueInsuficienteException(String mensagem) {
        super(mensagem);
    }
}

