package br.edu.fatec.ecommerce.modulo3.checkout.api.dto;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pedido.dominio.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta emitido após a criação e processamento inicial bem-sucedido do checkout.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * DTO de retorno da fachada {@code CheckoutFacade} expondo a confirmação e o resumo imutável.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class CheckoutResponseDTO {

    private UUID pedidoId;
    private String numeroPedido;
    private StatusPedido status;
    private MetodoPagamento metodoPagamento;
    private String chavePixCopiaECola;
    private String qrCodeBase64;
    private String linhaDigitavelBoleto;
    private String identificadorTransacao;
    private BigDecimal subtotalBruto;
    private BigDecimal totalDescontos;
    private BigDecimal valorFrete;
    private boolean freteGratis;
    private BigDecimal valorTotalLiquido;
    private String modalidadeFrete;
    private int prazoEntregaDias;
    private String enderecoDestinoFormatado;
    private List<ItemResumoDTO> itens;
    private Instant criadoEm;

    public CheckoutResponseDTO() {
    }

    public static class ItemResumoDTO {
        private String sku;
        private String nome;
        private int quantidade;
        private BigDecimal precoUnitarioTabela;
        private BigDecimal descontoUnitario;
        private BigDecimal subtotalLiquido;

        public ItemResumoDTO() {
        }

        public ItemResumoDTO(String sku, String nome, int quantidade, BigDecimal precoUnitarioTabela, BigDecimal descontoUnitario, BigDecimal subtotalLiquido) {
            this.sku = sku;
            this.nome = nome;
            this.quantidade = quantidade;
            this.precoUnitarioTabela = precoUnitarioTabela;
            this.descontoUnitario = descontoUnitario;
            this.subtotalLiquido = subtotalLiquido;
        }

        public String getSku() { return sku; }
        public String getNome() { return nome; }
        public int getQuantidade() { return quantidade; }
        public BigDecimal getPrecoUnitarioTabela() { return precoUnitarioTabela; }
        public BigDecimal getDescontoUnitario() { return descontoUnitario; }
        public BigDecimal getSubtotalLiquido() { return subtotalLiquido; }
    }

    // --- Getters e Setters ---

    public UUID getPedidoId() { return pedidoId; }
    public void setPedidoId(UUID pedidoId) { this.pedidoId = pedidoId; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public MetodoPagamento getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }

    public String getChavePixCopiaECola() { return chavePixCopiaECola; }
    public void setChavePixCopiaECola(String chavePixCopiaECola) { this.chavePixCopiaECola = chavePixCopiaECola; }

    public String getQrCodeBase64() { return qrCodeBase64; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }

    public String getLinhaDigitavelBoleto() { return linhaDigitavelBoleto; }
    public void setLinhaDigitavelBoleto(String linhaDigitavelBoleto) { this.linhaDigitavelBoleto = linhaDigitavelBoleto; }

    public String getIdentificadorTransacao() { return identificadorTransacao; }
    public void setIdentificadorTransacao(String identificadorTransacao) { this.identificadorTransacao = identificadorTransacao; }

    public BigDecimal getSubtotalBruto() { return subtotalBruto; }
    public void setSubtotalBruto(BigDecimal subtotalBruto) { this.subtotalBruto = subtotalBruto; }

    public BigDecimal getTotalDescontos() { return totalDescontos; }
    public void setTotalDescontos(BigDecimal totalDescontos) { this.totalDescontos = totalDescontos; }

    public BigDecimal getValorFrete() { return valorFrete; }
    public void setValorFrete(BigDecimal valorFrete) { this.valorFrete = valorFrete; }

    public boolean isFreteGratis() { return freteGratis; }
    public void setFreteGratis(boolean freteGratis) { this.freteGratis = freteGratis; }

    public BigDecimal getValorTotalLiquido() { return valorTotalLiquido; }
    public void setValorTotalLiquido(BigDecimal valorTotalLiquido) { this.valorTotalLiquido = valorTotalLiquido; }

    public String getModalidadeFrete() { return modalidadeFrete; }
    public void setModalidadeFrete(String modalidadeFrete) { this.modalidadeFrete = modalidadeFrete; }

    public int getPrazoEntregaDias() { return prazoEntregaDias; }
    public void setPrazoEntregaDias(int prazoEntregaDias) { this.prazoEntregaDias = prazoEntregaDias; }

    public String getEnderecoDestinoFormatado() { return enderecoDestinoFormatado; }
    public void setEnderecoDestinoFormatado(String enderecoDestinoFormatado) { this.enderecoDestinoFormatado = enderecoDestinoFormatado; }

    public List<ItemResumoDTO> getItens() { return itens; }
    public void setItens(List<ItemResumoDTO> itens) { this.itens = itens; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
