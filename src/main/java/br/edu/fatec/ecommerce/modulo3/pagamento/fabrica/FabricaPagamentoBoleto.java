package br.edu.fatec.ecommerce.modulo3.pagamento.fabrica;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.BoletoGatewayAdapter;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.GatewayPagamentoPort;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;
import org.springframework.stereotype.Component;

/**
 * Família de pagamento por boleto bancário.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Factory</i> da Abstract Factory.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class FabricaPagamentoBoleto implements FabricaProvedorPagamento {

    private final BoletoGatewayAdapter gateway;

    /**
     * Construtor com o adaptador do emissor injetado.
     *
     * @param gateway adaptador do emissor de boletos
     */
    public FabricaPagamentoBoleto(BoletoGatewayAdapter gateway) {
        this.gateway = gateway;
    }

    /**
     * Meio atendido.
     *
     * @return {@link MetodoPagamento#BOLETO}
     */
    @Override
    public MetodoPagamento metodo() {
        return MetodoPagamento.BOLETO;
    }

    /**
     * Boleto é confirmado depois, por webhook.
     *
     * @return {@link TipoLiquidacao#ASSINCRONA}
     */
    @Override
    public TipoLiquidacao tipoLiquidacao() {
        return TipoLiquidacao.ASSINCRONA;
    }

    /**
     * Fornece o adaptador do emissor de boletos.
     *
     * @return gateway de boleto
     */
    @Override
    public GatewayPagamentoPort criarGateway() {
        return gateway;
    }

    /**
     * Instruções de pagamento do boleto.
     *
     * @return formatador que exibe a linha digitável e o vencimento
     */
    @Override
    public FormatadorInstrucoesPagamento criarFormatadorInstrucoes() {
        return (resposta, valor) -> String.format(
                "Pague o boleto de R$ %s até %s. Linha digitável: %s.",
                valor.toPlainString(), resposta.expiraEm(), resposta.linhaDigitavelBoleto());
    }
}
