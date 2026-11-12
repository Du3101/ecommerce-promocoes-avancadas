package br.edu.fatec.ecommerce.modulo3.eventos;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando o pagamento de um pedido é confirmado (cartão aprovado online ou webhook de PIX/boleto).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> mensagem do <b>Observer</b>. Por causa da idempotência do
 * webhook, este evento é publicado <b>uma única vez</b> por pedido, mesmo que o gateway reenvie a confirmação.
 *
 * @param eventoId identificador da ocorrência
 * @param ocorridoEm instante da confirmação
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial do pedido
 * @param emailCliente e-mail do cliente
 * @param nomeCliente nome do cliente
 * @param identificadorTransacao identificador da transação no gateway
 * @param valorPago valor confirmado
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record PagamentoConfirmadoEvent(
        UUID eventoId,
        Instant ocorridoEm,
        UUID pedidoId,
        String numeroPedido,
        String emailCliente,
        String nomeCliente,
        String identificadorTransacao,
        BigDecimal valorPago
) implements EventoDominio {

    /**
     * Cria o evento a partir do pedido pago.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param pedido pedido que acabou de ser pago
     * @param identificadorTransacao identificador da transação no gateway
     * @return novo evento imutável
     */
    public static PagamentoConfirmadoEvent de(Pedido pedido, String identificadorTransacao) {
        return new PagamentoConfirmadoEvent(
                UUID.randomUUID(),
                Instant.now(),
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getEmailCliente(),
                pedido.getNomeCliente(),
                identificadorTransacao,
                pedido.getFinanceiro().getValorTotalLiquido());
    }

    /**
     * Descreve o evento para logs e notificações.
     *
     * @return resumo legível
     */
    @Override
    public String resumo() {
        return String.format("Pagamento de R$ %s do pedido %s confirmado (transação %s). O pedido segue para a expedição.",
                valorPago, numeroPedido, identificadorTransacao);
    }
}
