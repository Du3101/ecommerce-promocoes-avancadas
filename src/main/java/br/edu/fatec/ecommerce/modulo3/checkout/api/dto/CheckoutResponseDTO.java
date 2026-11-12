package br.edu.fatec.ecommerce.modulo3.checkout.api.dto;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto.RespostaPagamentoDTO;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TransacaoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.api.dto.PedidoResponseDTO;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.Pedido;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resposta do checkout ({@code 201 Created}), no formato do contrato da API.
 *
 * @param pedidoId UUID do pedido
 * @param numeroPedido número comercial
 * @param status status após o checkout ({@code AGUARDANDO_PAGAMENTO}, {@code PAGO} ou {@code PAGAMENTO_RECUSADO})
 * @param metodoPagamento método escolhido
 * @param dadosPagamento dados para o cliente pagar (chave PIX, boleto) ou resultado do cartão
 * @param snapshotFinanceiro memória de cálculo congelada
 * @param entrega dados de entrega congelados
 * @param itens linhas congeladas
 * @param cupomUtilizado cupom aplicado
 * @param promocoesAplicadas promoções que deram desconto
 * @param reaproveitado {@code true} se o checkout foi repetido com a mesma chave e o pedido existente foi devolvido
 * @param criadoEm instante de criação
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record CheckoutResponseDTO(
        UUID pedidoId,
        String numeroPedido,
        StatusPedido status,
        MetodoPagamento metodoPagamento,
        DadosPagamentoDTO dadosPagamento,
        PedidoResponseDTO.SnapshotFinanceiroDTO snapshotFinanceiro,
        PedidoResponseDTO.EntregaDTO entrega,
        List<PedidoResponseDTO.ItemPedidoDTO> itens,
        String cupomUtilizado,
        List<String> promocoesAplicadas,
        boolean reaproveitado,
        Instant criadoEm
) {

    /**
     * Dados de pagamento devolvidos ao cliente.
     *
     * @param identificadorTransacao identificador no gateway
     * @param statusTransacao status da transação
     * @param comprovante chave PIX copia-e-cola, linha digitável ou mensagem do cartão
     * @param qrCodeBase64 QR Code PIX (somente na primeira resposta)
     * @param expiraEm validade da cobrança (PIX/boleto)
     * @param instrucoes instruções formatadas pela família de pagamento
     */
    public record DadosPagamentoDTO(String identificadorTransacao, String statusTransacao, String comprovante,
                                    String qrCodeBase64, Instant expiraEm, String instrucoes) {
    }

    /**
     * Monta a resposta de um checkout recém-processado.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> de mapeamento.</p>
     *
     * @param pedido pedido criado
     * @param resposta resposta do gateway
     * @param instrucoes instruções de pagamento formatadas
     * @return DTO de saída
     */
    public static CheckoutResponseDTO novo(Pedido pedido, RespostaPagamentoDTO resposta, String instrucoes) {
        DadosPagamentoDTO dados = new DadosPagamentoDTO(resposta.identificadorGateway(),
                resposta.statusTransacao().name(), resposta.comprovante(), resposta.qrCodeBase64(),
                resposta.expiraEm(), instrucoes);
        return montar(pedido, dados, false);
    }

    /**
     * Monta a resposta de um checkout repetido (mesma chave de idempotência).
     *
     * @param pedido pedido existente
     * @param transacao transação existente (pode ser nula)
     * @return DTO de saída marcado como reaproveitado
     */
    public static CheckoutResponseDTO existente(Pedido pedido, TransacaoPagamento transacao) {
        DadosPagamentoDTO dados = transacao == null ? null : new DadosPagamentoDTO(transacao.getIdentificadorGateway(),
                transacao.getStatus().name(), transacao.getComprovanteOuQrCode(), null, null,
                "Pedido já registrado anteriormente com esta chave de idempotência.");
        return montar(pedido, dados, true);
    }

    /**
     * Monta o DTO a partir do pedido e dos dados de pagamento.
     *
     * @param pedido pedido
     * @param dados dados de pagamento
     * @param reaproveitado se é repetição
     * @return DTO de saída
     */
    private static CheckoutResponseDTO montar(Pedido pedido, DadosPagamentoDTO dados, boolean reaproveitado) {
        return new CheckoutResponseDTO(
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getStatus(),
                pedido.getMetodoPagamento(),
                dados,
                PedidoResponseDTO.SnapshotFinanceiroDTO.de(pedido.getFinanceiro()),
                PedidoResponseDTO.EntregaDTO.de(pedido),
                pedido.getItens().stream().map(PedidoResponseDTO.ItemPedidoDTO::de).toList(),
                pedido.getCupomUtilizado(),
                pedido.getPromocoesAplicadas(),
                reaproveitado,
                pedido.getCriadoEm());
    }
}
