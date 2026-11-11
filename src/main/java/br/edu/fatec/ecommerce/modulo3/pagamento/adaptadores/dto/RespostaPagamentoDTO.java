package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.StatusTransacaoPagamento;

/**
 * DTO que padroniza o retorno dos gateways externos de pagamento simulados.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Objeto unificado produzido pelos adaptadores do padrão <i>Adapter</i>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class RespostaPagamentoDTO {

    private boolean sucesso;
    private StatusTransacaoPagamento statusTransacao;
    private String identificadorGateway;
    private String chavePixCopiaECola;
    private String qrCodeBase64;
    private String linhaDigitavelBoleto;
    private String mensagem;

    public RespostaPagamentoDTO() {
    }

    public static RespostaPagamentoDTO sucessoPix(String idGateway, String chavePix, String qrCode) {
        RespostaPagamentoDTO dto = new RespostaPagamentoDTO();
        dto.sucesso = true;
        dto.statusTransacao = StatusTransacaoPagamento.PENDENTE;
        dto.identificadorGateway = idGateway;
        dto.chavePixCopiaECola = chavePix;
        dto.qrCodeBase64 = qrCode;
        dto.mensagem = "Chave PIX e QR Code gerados com sucesso.";
        return dto;
    }

    public static RespostaPagamentoDTO sucessoCartao(String idGateway) {
        RespostaPagamentoDTO dto = new RespostaPagamentoDTO();
        dto.sucesso = true;
        dto.statusTransacao = StatusTransacaoPagamento.CONFIRMADA;
        dto.identificadorGateway = idGateway;
        dto.mensagem = "Transação aprovada online pela operadora do cartão.";
        return dto;
    }

    public static RespostaPagamentoDTO sucessoBoleto(String idGateway, String linhaDigitavel) {
        RespostaPagamentoDTO dto = new RespostaPagamentoDTO();
        dto.sucesso = true;
        dto.statusTransacao = StatusTransacaoPagamento.PENDENTE;
        dto.identificadorGateway = idGateway;
        dto.linhaDigitavelBoleto = linhaDigitavel;
        dto.mensagem = "Boleto bancário gerado com código de barras válido.";
        return dto;
    }

    public static RespostaPagamentoDTO recusa(String mensagem) {
        RespostaPagamentoDTO dto = new RespostaPagamentoDTO();
        dto.sucesso = false;
        dto.statusTransacao = StatusTransacaoPagamento.RECUSADA;
        dto.mensagem = mensagem;
        return dto;
    }

    public boolean isSucesso() { return sucesso; }
    public StatusTransacaoPagamento getStatusTransacao() { return statusTransacao; }
    public String getIdentificadorGateway() { return identificadorGateway; }
    public String getChavePixCopiaECola() { return chavePixCopiaECola; }
    public String getQrCodeBase64() { return qrCodeBase64; }
    public String getLinhaDigitavelBoleto() { return linhaDigitavelBoleto; }
    public String getMensagem() { return mensagem; }
}
