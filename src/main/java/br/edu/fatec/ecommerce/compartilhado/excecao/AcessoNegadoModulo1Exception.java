package br.edu.fatec.ecommerce.compartilhado.excecao;

/**
 * Exceção lançada quando um usuário autenticado tenta executar operação incompatível com seu perfil.
 */
public class AcessoNegadoModulo1Exception extends RegraNegocioModulo1Exception {

    /**
     * Constrói a exceção de autorização negada.
     *
     * <p>Objetivo Funcional: Garantir segregação estrita de funções (RBAC) e mapear para HTTP 403 Forbidden.</p>
     * <p>Papel no Design Pattern: Lançada pelos Protection Proxies (<b>Proxy</b>) de catálogo e estoque.</p>
     *
     * @param mensagem motivo da negação de permissão
     */
    public AcessoNegadoModulo1Exception(String mensagem) {
        super(mensagem);
    }
}

