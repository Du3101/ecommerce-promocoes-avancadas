package br.edu.fatec.ecommerce.modulo3.pagamento.dominio;

/**
 * Forma como um meio de pagamento é liquidado.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> atributo da família de pagamento (Abstract Factory) usado pela
 * {@code CheckoutFacade} para escolher a especialização do <b>Template Method</b> de checkout, sem
 * {@code if (metodo == CARTAO)}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public enum TipoLiquidacao {

    /** A resposta do gateway já é definitiva no checkout (cartão de crédito: aprovado ou recusado na hora). */
    ONLINE,

    /** O checkout só emite a cobrança; a confirmação chega depois pelo webhook (PIX e boleto). */
    ASSINCRONA
}
