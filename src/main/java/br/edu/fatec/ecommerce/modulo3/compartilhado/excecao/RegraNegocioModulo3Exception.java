package br.edu.fatec.ecommerce.modulo3.compartilhado.excecao;

/**
 * Exceção base de domínio para violações de regras de negócio do Módulo 3.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Serve como raiz da hierarquia de falhas de domínio do subsistema de Checkout, Pedidos,
 * Pagamentos e Frete, permitindo captura e tratamento polimórfico na camada REST.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class RegraNegocioModulo3Exception extends RuntimeException {

    /**
     * Construtor da exceção com a mensagem detalhada do erro.
     *
     * @param mensagem Descrição do motivo da violação de negócio.
     */
    public RegraNegocioModulo3Exception(String mensagem) {
        super(mensagem);
    }
}
