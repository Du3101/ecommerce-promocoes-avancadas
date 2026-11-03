package br.edu.fatec.ecommerce.modulo2.cupons.dominio;

/**
 * Define a forma de cálculo do benefício financeiro de um cupom promocional.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Discrimina a regra de cálculo monetário do cupom, permitindo que a estratégia
 * de precificação decida se aplica um percentual ou um abatimento fixo nominal.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public enum TipoDescontoCupom {

    /**
     * Cupom percentual (ex.: 10% de desconto sobre o subtotal elegível).
     */
    PERCENTUAL,

    /**
     * Cupom em valor fixo (ex.: R$ 25,00 de desconto nominal).
     */
    VALOR_FIXO
}

