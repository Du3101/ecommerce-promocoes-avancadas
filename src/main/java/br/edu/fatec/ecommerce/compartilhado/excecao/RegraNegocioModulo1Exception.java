package br.edu.fatec.ecommerce.compartilhado.excecao;

/**
 * Exceção base de domínio para violações de regras de negócio do Módulo 1.
 *
 * <p>Responsabilidade Comercial: Sinalizar entradas ou estados inválidos detectados
 * nas entidades, Builders e serviços de domínio.</p>
 */
public class RegraNegocioModulo1Exception extends RuntimeException {

    /**
     * Constrói uma nova exceção de regra de negócio com mensagem explícita.
     *
     * <p>Objetivo Funcional: Encapsular a descrição legível da violação de negócio.</p>
     * <p>Papel no Design Pattern: Exceção base utilizada pelas validações de <b>Builder</b>,
     * <b>Static Factory Method</b> e <b>Composite</b>.</p>
     *
     * @param mensagem descrição detalhada do erro de negócio
     */
    public RegraNegocioModulo1Exception(String mensagem) {
        super(mensagem);
    }
}

