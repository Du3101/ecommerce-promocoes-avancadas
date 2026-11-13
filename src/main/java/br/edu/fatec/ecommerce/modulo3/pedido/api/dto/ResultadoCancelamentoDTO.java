package br.edu.fatec.ecommerce.modulo3.pedido.api.dto;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.util.UUID;

/**
 * Resultado do cancelamento de um pedido.
 *
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial
 * @param statusAnterior status antes do cancelamento
 * @param novoStatus status após o cancelamento ({@code CANCELADO})
 * @param motivo motivo registrado
 * @param estornoRealizado se houve estorno financeiro (pedido já pago)
 * @param protocoloEstorno protocolo do estorno no provedor (ou {@code null})
 * @param acaoEstoque {@code RESERVA_LIBERADA} (não pago) ou {@code ESTOQUE_DEVOLVIDO} (pago)
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record ResultadoCancelamentoDTO(
        UUID pedidoId,
        String numeroPedido,
        StatusPedido statusAnterior,
        StatusPedido novoStatus,
        String motivo,
        boolean estornoRealizado,
        String protocoloEstorno,
        String acaoEstoque
) {
}
