package br.edu.fatec.ecommerce.modulo3.compartilhado.excecao;

/**
 * Exceção lançada quando o carrinho ficou inconsistente com o catálogo entre a montagem e o fechamento
 * (preço alterado ou item desativado) — HTTP 409.
 *
 * <p><b>Papel Arquitetural:</b> atende à responsabilidade do Integrante 3 na Constituição: "detecção de mudanças
 * de preço/promoção/estoque desde a adição ao carrinho". O cliente é orientado a revisar o carrinho em vez de
 * pagar um valor diferente do anunciado.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class ConflitoCheckoutException extends RuntimeException {

    /**
     * Cria a exceção com mensagem explicativa.
     *
     * @param mensagem descrição do conflito
     */
    public ConflitoCheckoutException(String mensagem) {
        super(mensagem);
    }
}
