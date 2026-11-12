package br.edu.fatec.ecommerce.modulo3.eventos;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando um pedido é cancelado (pelo cliente ou por um administrador).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> mensagem do <b>Observer</b>, publicada pelo
 * {@code CoordenadorCancelamentoMediator} depois que estoque e pagamento foram compensados.
 *
 * @param eventoId identificador da ocorrência
 * @param ocorridoEm instante do cancelamento
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial do pedido
 * @param emailCliente e-mail do cliente
 * @param nomeCliente nome do cliente
 * @param motivo motivo do cancelamento
 * @param estornoRealizado {@code true} se o pedido já estava pago e o valor foi estornado
 * @param valorEstornado valor devolvido ao cliente (zero se não havia pagamento)
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record PedidoCanceladoEvent(
        UUID eventoId,
        Instant ocorridoEm,
        UUID pedidoId,
        String numeroPedido,
        String emailCliente,
        String nomeCliente,
        String motivo,
        boolean estornoRealizado,
        BigDecimal valorEstornado
) implements EventoDominio {

    /**
     * Cria o evento a partir do pedido cancelado.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param pedido pedido cancelado
     * @param estornoRealizado se houve estorno financeiro
     * @return novo evento imutável
     */
    public static PedidoCanceladoEvent de(Pedido pedido, boolean estornoRealizado) {
        return new PedidoCanceladoEvent(
                UUID.randomUUID(),
                Instant.now(),
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getEmailCliente(),
                pedido.getNomeCliente(),
                pedido.getMotivoCancelamento(),
                estornoRealizado,
                estornoRealizado ? pedido.getFinanceiro().getValorTotalLiquido() : BigDecimal.ZERO);
    }

    /**
     * Descreve o evento para logs e notificações.
     *
     * @return resumo legível
     */
    @Override
    public String resumo() {
        String parteEstorno = estornoRealizado
                ? String.format(" O estorno de R$ %s foi solicitado.", valorEstornado)
                : " Nenhuma cobrança havia sido efetivada.";
        return String.format("Pedido %s cancelado (%s).%s", numeroPedido, motivo, parteEstorno);
    }
}
