package br.edu.fatec.ecommerce.modulo3.frete.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Corpo da requisição {@code POST /api/v1/frete/calcular}.
 *
 * <p><b>Papel Arquitetural:</b> DTO de entrada da camada HTTP, validado com Bean Validation.
 *
 * @param cepDestino CEP de destino (com ou sem máscara)
 * @param pesoTotalGramas peso total do carrinho em gramas
 * @param subtotalLiquido subtotal dos produtos já com descontos (usado na regra de frete grátis)
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record CalculoFreteRequestDTO(
        @NotBlank(message = "O CEP de destino é obrigatório.") String cepDestino,
        @Min(value = 1, message = "O peso total deve ser de pelo menos 1 grama.") int pesoTotalGramas,
        @NotNull(message = "O subtotal líquido é obrigatório.")
        @DecimalMin(value = "0.00", message = "O subtotal líquido não pode ser negativo.") BigDecimal subtotalLiquido
) {
}
