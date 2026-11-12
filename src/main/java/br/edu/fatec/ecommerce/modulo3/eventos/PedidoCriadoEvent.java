package br.edu.fatec.ecommerce.modulo3.eventos;

import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado quando o checkout cria um novo pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> mensagem do <b>Observer</b>. Os ouvintes enviam ao cliente o
 * resumo da compra e as instruções de pagamento (chave PIX, linha digitável do boleto ou autorização do cartão).
 *
 * @param eventoId identificador da ocorrência
 * @param ocorridoEm instante da criação do pedido
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial do pedido
 * @param emailCliente e-mail do cliente
 * @param nomeCliente nome do cliente
 * @param metodoPagamento método escolhido (texto amigável)
 * @param valorTotalLiquido valor total a pagar
 * @param instrucoesPagamento instruções formatadas pela família de pagamento (Abstract Factory)
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record PedidoCriadoEvent(
        UUID eventoId,
        Instant ocorridoEm,
        UUID pedidoId,
        String numeroPedido,
        String emailCliente,
        String nomeCliente,
        String metodoPagamento,
        BigDecimal valorTotalLiquido,
        String instrucoesPagamento
) implements EventoDominio {

    /**
     * Cria o evento a partir do pedido recém-criado.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param pedido pedido criado no checkout
     * @param instrucoesPagamento instruções de pagamento já formatadas
     * @return novo evento imutável
     */
    public static PedidoCriadoEvent de(Pedido pedido, String instrucoesPagamento) {
        return new PedidoCriadoEvent(
                UUID.randomUUID(),
                Instant.now(),
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getEmailCliente(),
                pedido.getNomeCliente(),
                pedido.getMetodoPagamento().getDescricaoExibicao(),
                pedido.getFinanceiro().getValorTotalLiquido(),
                instrucoesPagamento);
    }

    /**
     * Descreve o evento para logs e notificações.
     *
     * @return resumo legível
     */
    @Override
    public String resumo() {
        return String.format("Pedido %s criado no valor de R$ %s (%s). %s",
                numeroPedido, valorTotalLiquido, metodoPagamento, instrucoesPagamento);
    }
}
