package br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.dto;

import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO com dados de entrada para envio à Camada Anticorrupção (ACL) do gateway.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Objeto de entrada da interface do padrão <i>Adapter</i>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class RequisicaoCobrancaDTO {

    private UUID pedidoId;
    private String numeroPedido;
    private MetodoPagamento metodo;
    private BigDecimal valor;
    private String chaveIdempotencia;
    private String nomeCliente;
    private String cpfCliente;
    private String numeroCartaoFicticio;
    private String titularCartao;

    public RequisicaoCobrancaDTO() {
    }

    public RequisicaoCobrancaDTO(UUID pedidoId, String numeroPedido, MetodoPagamento metodo, BigDecimal valor, String chaveIdempotencia, String nomeCliente, String cpfCliente) {
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.metodo = metodo;
        this.valor = valor;
        this.chaveIdempotencia = chaveIdempotencia;
        this.nomeCliente = nomeCliente;
        this.cpfCliente = cpfCliente;
    }

    public UUID getPedidoId() { return pedidoId; }
    public void setPedidoId(UUID pedidoId) { this.pedidoId = pedidoId; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public MetodoPagamento getMetodo() { return metodo; }
    public void setMetodo(MetodoPagamento metodo) { this.metodo = metodo; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }

    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    public String getCpfCliente() { return cpfCliente; }
    public void setCpfCliente(String cpfCliente) { this.cpfCliente = cpfCliente; }

    public String getNumeroCartaoFicticio() { return numeroCartaoFicticio; }
    public void setNumeroCartaoFicticio(String numeroCartaoFicticio) { this.numeroCartaoFicticio = numeroCartaoFicticio; }

    public String getTitularCartao() { return titularCartao; }
    public void setTitularCartao(String titularCartao) { this.titularCartao = titularCartao; }
}
