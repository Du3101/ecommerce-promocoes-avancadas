package br.edu.fatec.ecommerce.modulo3.eventos;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando o gateway recusa o pagamento de um pedido (a reserva de estoque já foi liberada).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> mensagem do <b>Observer</b>, publicada pelo
 * {@code CoordenadorCancelamentoMediator} ao final da compensação da recusa.
 *
 * @param eventoId identificador da ocorrência
 * @param ocorridoEm instante da recusa
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial do pedido
 * @param emailCliente e-mail do cliente
 * @param nomeCliente nome do cliente
 * @param motivo motivo informado pelo gateway
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record PagamentoRecusadoEvent(
        UUID eventoId,
        Instant ocorridoEm,
        UUID pedidoId,
        String numeroPedido,
        String emailCliente,
        String nomeCliente,
        String motivo
) implements EventoDominio {

    /**
     * Cria o evento a partir do pedido recusado.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param pedido pedido com pagamento recusado
     * @param motivo motivo da recusa
     * @return novo evento imutável
     */
    public static PagamentoRecusadoEvent de(Pedido pedido, String motivo) {
        return new PagamentoRecusadoEvent(
                UUID.randomUUID(),
                Instant.now(),
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getEmailCliente(),
                pedido.getNomeCliente(),
                motivo);
    }

    /**
     * Descreve o evento para logs e notificações.
     *
     * @return resumo legível
     */
    @Override
    public String resumo() {
        return String.format("O pagamento do pedido %s foi recusado (%s). Os itens voltaram ao estoque; "
                + "faça um novo pedido com outra forma de pagamento.", numeroPedido, motivo);
    }
}
