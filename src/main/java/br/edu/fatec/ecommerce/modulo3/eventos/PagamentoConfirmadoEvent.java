package br.edu.fatec.ecommerce.modulo3.eventos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento emitido quando a liquidação financeira de um Pedido é aprovada pelo gateway.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Event do padrão <b>Observer</b>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class PagamentoConfirmadoEvent implements EventoDominio {

    private final UUID eventoId;
    private final Instant ocorridoEm;
    private final UUID pedidoId;
    private final String numeroPedido;
    private final String chaveOperacaoEstoque;
    private final BigDecimal valorPago;

    public PagamentoConfirmadoEvent(UUID pedidoId, String numeroPedido, String chaveOperacaoEstoque, BigDecimal valorPago) {
        this.eventoId = UUID.randomUUID();
        this.ocorridoEm = Instant.now();
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.chaveOperacaoEstoque = chaveOperacaoEstoque;
        this.valorPago = valorPago;
    }

    @Override public UUID getEventoId() { return eventoId; }
    @Override public Instant getOcorridoEm() { return ocorridoEm; }
    public UUID getPedidoId() { return pedidoId; }
    public String getNumeroPedido() { return numeroPedido; }
    public String getChaveOperacaoEstoque() { return chaveOperacaoEstoque; }
    public BigDecimal getValorPago() { return valorPago; }
}
