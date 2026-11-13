package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.StatusTransacaoPagamento;
import java.time.Instant;

/**
 * Resposta de cobrança já traduzida para o modelo interno.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> saída da Anti-Corruption Layer. Independentemente do provedor,
 * o checkout só enxerga este formato.
 *
 * @param sucesso {@code false} se a cobrança foi recusada
 * @param statusTransacao {@code PENDENTE} (PIX/boleto), {@code CONFIRMADA} (cartão aprovado) ou {@code RECUSADA}
 * @param identificadorGateway identificador da transação no provedor
 * @param chavePixCopiaECola chave PIX copia-e-cola (somente PIX)
 * @param qrCodeBase64 imagem do QR Code em Base64 (somente PIX)
 * @param linhaDigitavelBoleto linha digitável (somente boleto)
 * @param expiraEm validade da cobrança (PIX e boleto)
 * @param mensagem mensagem legível do provedor
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record RespostaPagamentoDTO(
        boolean sucesso,
        StatusTransacaoPagamento statusTransacao,
        String identificadorGateway,
        String chavePixCopiaECola,
        String qrCodeBase64,
        String linhaDigitavelBoleto,
        Instant expiraEm,
        String mensagem
) {

    /**
     * Cobrança PIX emitida, aguardando pagamento.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param idGateway identificador no provedor
     * @param chavePix chave copia-e-cola
     * @param qrCode QR Code em Base64
     * @param expiraEm validade da cobrança
     * @return resposta pendente
     */
    public static RespostaPagamentoDTO pixEmitido(String idGateway, String chavePix, String qrCode, Instant expiraEm) {
        return new RespostaPagamentoDTO(true, StatusTransacaoPagamento.PENDENTE, idGateway, chavePix, qrCode, null,
                expiraEm, "Chave PIX e QR Code gerados com sucesso.");
    }

    /**
     * Cartão aprovado online.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param idGateway código de autorização
     * @param finalCartao 4 últimos dígitos
     * @return resposta confirmada
     */
    public static RespostaPagamentoDTO cartaoAprovado(String idGateway, String finalCartao) {
        return new RespostaPagamentoDTO(true, StatusTransacaoPagamento.CONFIRMADA, idGateway, null, null, null,
                null, "Transação aprovada no cartão final " + finalCartao + ".");
    }

    /**
     * Boleto emitido, aguardando pagamento.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param idGateway identificador no provedor
     * @param linhaDigitavel linha digitável
     * @param vencimento vencimento do boleto
     * @return resposta pendente
     */
    public static RespostaPagamentoDTO boletoEmitido(String idGateway, String linhaDigitavel, Instant vencimento) {
        return new RespostaPagamentoDTO(true, StatusTransacaoPagamento.PENDENTE, idGateway, null, null,
                linhaDigitavel, vencimento, "Boleto bancário gerado com sucesso.");
    }

    /**
     * Cobrança recusada pelo provedor.
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> semântico.</p>
     *
     * @param idGateway identificador da tentativa (pode ser nulo)
     * @param mensagem motivo da recusa
     * @return resposta recusada
     */
    public static RespostaPagamentoDTO recusa(String idGateway, String mensagem) {
        return new RespostaPagamentoDTO(false, StatusTransacaoPagamento.RECUSADA, idGateway, null, null, null,
                null, mensagem);
    }

    /**
     * Texto que comprova a cobrança, gravado na transação (chave PIX, linha digitável ou mensagem de autorização).
     *
     * @return comprovante resumido
     */
    public String comprovante() {
        if (chavePixCopiaECola != null) {
            return chavePixCopiaECola;
        }
        if (linhaDigitavelBoleto != null) {
            return linhaDigitavelBoleto;
        }
        return mensagem;
    }
}
