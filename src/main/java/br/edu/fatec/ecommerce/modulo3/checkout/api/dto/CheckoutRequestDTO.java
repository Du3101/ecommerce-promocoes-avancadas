package br.edu.fatec.ecommerce.modulo3.checkout.api.dto;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requisição para fechamento de compra no endpoint de checkout.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Objeto de transferência de dados (DTO) e parâmetro de comando recebido pela
 * fachada {@code CheckoutFacade} (GoF Facade).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class CheckoutRequestDTO {

    @NotNull(message = "O método de pagamento é obrigatório.")
    private MetodoPagamento metodoPagamento;

    @NotNull(message = "A modalidade de frete é obrigatória.")
    private ModalidadeFrete modalidadeFrete;

    private String chaveIdempotenciaCliente;

    private String numeroCartaoFicticio;

    private String titularCartao;

    /**
     * Construtor padrão para desserialização Jackson.
     */
    public CheckoutRequestDTO() {
    }

    /**
     * Construtor explícito para montagem do comando de checkout.
     *
     * @param metodoPagamento Modalidade escolhida (PIX, CARTAO_CREDITO, BOLETO).
     * @param modalidadeFrete Opção logística (CORREIOS_PAC, CORREIOS_SEDEX, TRANSPORTADORA_EXPRESSA).
     * @param chaveIdempotenciaCliente Chave enviada pelo cliente para evitar checkout duplicado.
     */
    public CheckoutRequestDTO(
            MetodoPagamento metodoPagamento,
            ModalidadeFrete modalidadeFrete,
            String chaveIdempotenciaCliente) {
        this.metodoPagamento = metodoPagamento;
        this.modalidadeFrete = modalidadeFrete;
        this.chaveIdempotenciaCliente = chaveIdempotenciaCliente;
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(MetodoPagamento metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public ModalidadeFrete getModalidadeFrete() {
        return modalidadeFrete;
    }

    public void setModalidadeFrete(ModalidadeFrete modalidadeFrete) {
        this.modalidadeFrete = modalidadeFrete;
    }

    public String getChaveIdempotenciaCliente() {
        return chaveIdempotenciaCliente;
    }

    public void setChaveIdempotenciaCliente(String chaveIdempotenciaCliente) {
        this.chaveIdempotenciaCliente = chaveIdempotenciaCliente;
    }

    public String getNumeroCartaoFicticio() {
        return numeroCartaoFicticio;
    }

    public void setNumeroCartaoFicticio(String numeroCartaoFicticio) {
        this.numeroCartaoFicticio = numeroCartaoFicticio;
    }

    public String getTitularCartao() {
        return titularCartao;
    }

    public void setTitularCartao(String titularCartao) {
        this.titularCartao = titularCartao;
    }
}
