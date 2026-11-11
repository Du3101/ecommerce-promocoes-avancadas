package br.edu.fatec.ecommerce.modulo3.eventos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento emitido quando um novo Pedido é criado com sucesso no checkout.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Event do padrão <b>Observer</b>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PedidoCriadoEvent implements EventoDominio {

    private final UUID eventoId;
    private final Instant ocorridoEm;
    private final UUID pedidoId;
    private final String numeroPedido;
    private final UUID clienteId;
    private final BigDecimal valorTotalLiquido;

    public PedidoCriadoEvent(UUID pedidoId, String numeroPedido, UUID clienteId, BigDecimal valorTotalLiquido) {
        this.eventoId = UUID.randomUUID();
        this.ocorridoEm = Instant.now();
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.clienteId = clienteId;
        this.valorTotalLiquido = valorTotalLiquido;
    }

    @Override public UUID getEventoId() { return eventoId; }
    @Override public Instant getOcorridoEm() { return ocorridoEm; }
    public UUID getPedidoId() { return pedidoId; }
    public String getNumeroPedido() { return numeroPedido; }
    public UUID getClienteId() { return clienteId; }
    public BigDecimal getValorTotalLiquido() { return valorTotalLiquido; }
}
