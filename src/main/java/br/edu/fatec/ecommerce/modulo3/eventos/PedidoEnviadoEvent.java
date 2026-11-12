package br.edu.fatec.ecommerce.modulo3.eventos;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando o pedido é despachado para a transportadora.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> mensagem do <b>Observer</b>. Os ouvintes enviam o código de
 * rastreio ao cliente por e-mail e SMS.
 *
 * @param eventoId identificador da ocorrência
 * @param ocorridoEm instante do despacho
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial do pedido
 * @param emailCliente e-mail do cliente
 * @param nomeCliente nome do cliente
 * @param codigoRastreio código de rastreio da transportadora
 * @param modalidadeFrete nome da modalidade de entrega
 * @param prazoDiasUteis prazo previsto em dias úteis
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record PedidoEnviadoEvent(
        UUID eventoId,
        Instant ocorridoEm,
        UUID pedidoId,
        String numeroPedido,
        String emailCliente,
        String nomeCliente,
        String codigoRastreio,
        String modalidadeFrete,
        int prazoDiasUteis
) implements EventoDominio {

    /**
     * Cria o evento a partir do pedido despachado.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param pedido pedido despachado
     * @return novo evento imutável
     */
    public static PedidoEnviadoEvent de(Pedido pedido) {
        return new PedidoEnviadoEvent(
                UUID.randomUUID(),
                Instant.now(),
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getEmailCliente(),
                pedido.getNomeCliente(),
                pedido.getCodigoRastreio(),
                pedido.getModalidadeFrete().getNomeExibicao(),
                pedido.getPrazoEntregaDias());
    }

    /**
     * Descreve o evento para logs e notificações.
     *
     * @return resumo legível
     */
    @Override
    public String resumo() {
        return String.format("Pedido %s enviado via %s. Código de rastreio: %s (prazo: %d dias úteis).",
                numeroPedido, modalidadeFrete, codigoRastreio, prazoDiasUteis);
    }
}
