package br.edu.fatec.ecommerce.modulo3.pagamento.fabrica;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.CartaoCreditoGatewayAdapter;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.GatewayPagamentoPort;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;
import org.springframework.stereotype.Component;

/**
 * Família de pagamento por cartão de crédito.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Factory</i> da Abstract Factory.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class FabricaPagamentoCartaoCredito implements FabricaProvedorPagamento {

    private final CartaoCreditoGatewayAdapter gateway;

    /**
     * Construtor com o adaptador da operadora injetado.
     *
     * @param gateway adaptador da operadora de cartão
     */
    public FabricaPagamentoCartaoCredito(CartaoCreditoGatewayAdapter gateway) {
        this.gateway = gateway;
    }

    /**
     * Meio atendido.
     *
     * @return {@link MetodoPagamento#CARTAO_CREDITO}
     */
    @Override
    public MetodoPagamento metodo() {
        return MetodoPagamento.CARTAO_CREDITO;
    }

    /**
     * Cartão é autorizado (ou recusado) na hora.
     *
     * @return {@link TipoLiquidacao#ONLINE}
     */
    @Override
    public TipoLiquidacao tipoLiquidacao() {
        return TipoLiquidacao.ONLINE;
    }

    /**
     * Fornece o adaptador da operadora.
     *
     * @return gateway de cartão
     */
    @Override
    public GatewayPagamentoPort criarGateway() {
        return gateway;
    }

    /**
     * Instruções (na verdade, o resultado) do pagamento com cartão.
     *
     * @return formatador que informa aprovação ou recusa
     */
    @Override
    public FormatadorInstrucoesPagamento criarFormatadorInstrucoes() {
        return (resposta, valor) -> resposta.sucesso()
                ? String.format("Pagamento de R$ %s aprovado. %s", valor.toPlainString(), resposta.mensagem())
                : String.format("Pagamento de R$ %s não aprovado: %s", valor.toPlainString(), resposta.mensagem());
    }
}
