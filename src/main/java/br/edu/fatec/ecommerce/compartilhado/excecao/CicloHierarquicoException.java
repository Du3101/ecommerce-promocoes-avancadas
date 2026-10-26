package br.edu.fatec.ecommerce.compartilhado.excecao;

/**
 * Exceção lançada quando uma operação em estrutura recursiva (Categoria ou ComboKit) tenta criar um ciclo.
 */
public class CicloHierarquicoException extends RegraNegocioModulo1Exception {

    /**
     * Constrói a exceção de ciclo hierárquico detectado.
     *
     * <p>Objetivo Funcional: Proteger árvores recursivas contra loops infinitos.</p>
     * <p>Papel no Design Pattern: Validação estrutural obrigatória nas composições recursivas (<b>Composite</b>).</p>
     *
     * @param mensagem descrição do ciclo detectado
     */
    public CicloHierarquicoException(String mensagem) {
        super(mensagem);
    }
}

