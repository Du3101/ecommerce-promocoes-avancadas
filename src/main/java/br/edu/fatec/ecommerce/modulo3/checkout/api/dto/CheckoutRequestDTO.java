package br.edu.fatec.ecommerce.modulo3.checkout.api.dto;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição {@code POST /api/v1/checkout}.
 *
 * <p>Não há itens nem preços aqui de propósito: eles vêm do carrinho do Módulo 2 e do catálogo do Módulo 1. O
 * cliente só escolhe como pagar e como receber, o que impede manipulação de preço pelo navegador.
 *
 * @param metodoPagamento PIX, CARTAO_CREDITO ou BOLETO
 * @param modalidadeFrete CORREIOS_PAC, CORREIOS_SEDEX ou TRANSPORTADORA_EXPRESSA
 * @param chaveIdempotenciaCliente chave opcional para reenvio seguro: repetir o checkout com a mesma chave
 *                                 devolve o mesmo pedido em vez de criar outro
 * @param numeroCartaoFicticio número de cartão fictício (obrigatório para cartão; final 0000 é recusado)
 * @param titularCartao nome impresso no cartão fictício
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record CheckoutRequestDTO(
        @NotNull(message = "O método de pagamento é obrigatório.") MetodoPagamento metodoPagamento,
        @NotNull(message = "A modalidade de frete é obrigatória.") ModalidadeFrete modalidadeFrete,
        @Size(max = 60, message = "A chave de idempotência deve ter no máximo 60 caracteres.")
        @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "A chave de idempotência aceita apenas letras, números, '-' e '_'.")
        String chaveIdempotenciaCliente,
        String numeroCartaoFicticio,
        String titularCartao
) {
}
