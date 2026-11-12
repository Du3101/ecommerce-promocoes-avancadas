package br.edu.fatec.ecommerce.modulo3.compartilhado.excecao;

/**
 * Exceção lançada quando um cliente tenta acessar ou alterar um pedido que não é dele (HTTP 403).
 *
 * <p><b>Papel Arquitetural:</b> materializa a segregação de dados do Princípio V da Constituição
 * ("clientes acessam apenas os próprios pedidos").
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class AcessoNegadoModulo3Exception extends RuntimeException {

    /**
     * Cria a exceção com mensagem explicativa.
     *
     * @param mensagem descrição da violação de acesso
     */
    public AcessoNegadoModulo3Exception(String mensagem) {
        super(mensagem);
    }
}
