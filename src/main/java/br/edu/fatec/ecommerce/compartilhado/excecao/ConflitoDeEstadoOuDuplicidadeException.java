package br.edu.fatec.ecommerce.compartilhado.excecao;

/**
 * Exceção lançada quando há duplicidade de chave única (e-mail, CPF, SKU, slug) ou conflito de estado.
 */
public class ConflitoDeEstadoOuDuplicidadeException extends RegraNegocioModulo1Exception {

    /**
     * Constrói a exceção de conflito comercial.
     *
     * <p>Objetivo Funcional: Mapear duplicidades de cadastro e conflitos para HTTP 409 Conflict.</p>
     * <p>Papel no Design Pattern: Validação de unicidade nas fábricas e serviços de domínio.</p>
     *
     * @param mensagem descrição do conflito detectado
     */
    public ConflitoDeEstadoOuDuplicidadeException(String mensagem) {
        super(mensagem);
    }
}

