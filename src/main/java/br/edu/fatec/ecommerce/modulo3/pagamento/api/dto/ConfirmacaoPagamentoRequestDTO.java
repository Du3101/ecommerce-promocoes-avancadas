package br.edu.fatec.ecommerce.modulo3.pagamento.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Notificação enviada pelo gateway ao webhook {@code POST /api/v1/pagamentos/confirmacao}.
 *
 * @param chaveOperacaoPedido número do pedido (chave de idempotência da confirmação)
 * @param idTransacaoGateway identificador da liquidação no gateway
 * @param sucesso {@code true} para pagamento aprovado, {@code false} para recusado
 * @param valorPago valor liquidado (opcional; se informado, precisa bater com o total do pedido)
 * @param motivoRecusa motivo da recusa (quando {@code sucesso = false})
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record ConfirmacaoPagamentoRequestDTO(
        @NotBlank(message = "A chave de operação do pedido é obrigatória.") String chaveOperacaoPedido,
        @NotBlank(message = "O identificador da transação no gateway é obrigatório.") String idTransacaoGateway,
        @NotNull(message = "Informe se o pagamento foi aprovado (sucesso).") Boolean sucesso,
        BigDecimal valorPago,
        String motivoRecusa
) {
}
