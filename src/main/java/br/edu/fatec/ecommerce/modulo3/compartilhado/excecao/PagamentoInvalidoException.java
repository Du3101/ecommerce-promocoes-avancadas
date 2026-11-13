package br.edu.fatec.ecommerce.modulo3.compartilhado.excecao;

/**
 * Exceção lançada quando os dados de pagamento são inconsistentes ou rejeitados pelo gateway.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Disparada pelos adaptadores do padrão <i>Adapter</i> e pelas etapas variáveis do <i>Template Method</i>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PagamentoInvalidoException extends RegraNegocioModulo3Exception {

    /**
     * Construtor da exceção com o motivo da recusa ou inconsistência.
     *
     * @param mensagem Justificativa do erro financeiro.
     */
    public PagamentoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
