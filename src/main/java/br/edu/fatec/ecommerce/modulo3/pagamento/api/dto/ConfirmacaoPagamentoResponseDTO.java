package br.edu.fatec.ecommerce.modulo3.pagamento.api.dto;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.util.UUID;

/**
 * Resposta do webhook de confirmação de pagamento.
 *
 * @param statusProcessamento {@code PROCESSADO_SUCESSO} ou {@code PROCESSADO_RECUSA}
 * @param pedidoId UUID do pedido
 * @param numeroPedido número do pedido
 * @param novoStatusPedido status do pedido após o processamento
 * @param mensagem mensagem legível
 * @param idempotenteReexecutado {@code true} quando a resposta veio do registro de idempotência (repetição)
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record ConfirmacaoPagamentoResponseDTO(
        String statusProcessamento,
        UUID pedidoId,
        String numeroPedido,
        StatusPedido novoStatusPedido,
        String mensagem,
        boolean idempotenteReexecutado
) {

    /** Status de processamento para pagamento aprovado. */
    public static final String PROCESSADO_SUCESSO = "PROCESSADO_SUCESSO";

    /** Status de processamento para pagamento recusado. */
    public static final String PROCESSADO_RECUSA = "PROCESSADO_RECUSA";

    /**
     * Devolve uma cópia marcada como reexecução idempotente.
     *
     * @return mesma resposta com {@code idempotenteReexecutado = true}
     */
    public ConfirmacaoPagamentoResponseDTO comoReexecucao() {
        return new ConfirmacaoPagamentoResponseDTO(statusProcessamento, pedidoId, numeroPedido, novoStatusPedido,
                "Confirmação já processada anteriormente; nenhuma ação foi repetida. " + mensagem, true);
    }
}
