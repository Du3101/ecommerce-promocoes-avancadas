package br.edu.fatec.ecommerce.modulo3.eventos;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento emitido quando o Pedido é despachado e entregue à transportadora.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Event do padrão <b>Observer</b>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoEnviadoEvent implements EventoDominio {

    private final UUID eventoId;
    private final Instant ocorridoEm;
    private final UUID pedidoId;
    private final String numeroPedido;
    private final String codigoRastreio;

    public PedidoEnviadoEvent(UUID pedidoId, String numeroPedido, String codigoRastreio) {
        this.eventoId = UUID.randomUUID();
        this.ocorridoEm = Instant.now();
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.codigoRastreio = codigoRastreio;
    }

    @Override public UUID getEventoId() { return eventoId; }
    @Override public Instant getOcorridoEm() { return ocorridoEm; }
    public UUID getPedidoId() { return pedidoId; }
    public String getNumeroPedido() { return numeroPedido; }
    public String getCodigoRastreio() { return codigoRastreio; }
}
