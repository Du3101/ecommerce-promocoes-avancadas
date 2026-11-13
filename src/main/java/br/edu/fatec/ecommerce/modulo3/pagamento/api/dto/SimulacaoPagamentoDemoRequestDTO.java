package br.edu.fatec.ecommerce.modulo3.pagamento.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Payload de entrada para simulação controlada de confirmação/recusa de pagamento em ambiente de demonstração.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * DTO de comando validado na fronteira REST sem exigir que o cliente web conheça ou envie o segredo
 * {@code X-Gateway-Token} do webhook real.</p>
 *
 * @param sucesso {@code true} para simular liquidação aprovada ({@code PAGO}); {@code false} para recusa
 * @param motivoRecusa motivo opcional exibido quando {@code sucesso = false}
 */
public record SimulacaoPagamentoDemoRequestDTO(
    @NotNull(message = "Informe se a simulação do pagamento teve sucesso.") Boolean sucesso,
    String motivoRecusa
) {
}
