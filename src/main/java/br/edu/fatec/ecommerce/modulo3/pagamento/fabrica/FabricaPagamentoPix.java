package br.edu.fatec.ecommerce.modulo3.pagamento.fabrica;

import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.GatewayPagamentoPort;
import br.edu.fatec.ecommerce.modulo3.pagamento.adaptadores.PixGatewayAdapter;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.MetodoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.TipoLiquidacao;
import org.springframework.stereotype.Component;

/**
 * Família de pagamento PIX.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Factory</i> da Abstract Factory.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class FabricaPagamentoPix implements FabricaProvedorPagamento {

    private final PixGatewayAdapter gateway;

    /**
     * Construtor com o adaptador PIX injetado.
     *
     * @param gateway adaptador do provedor PIX
     */
    public FabricaPagamentoPix(PixGatewayAdapter gateway) {
        this.gateway = gateway;
    }

    /**
     * Meio atendido.
     *
     * @return {@link MetodoPagamento#PIX}
     */
    @Override
    public MetodoPagamento metodo() {
        return MetodoPagamento.PIX;
    }

    /**
     * PIX é confirmado depois, por webhook.
     *
     * @return {@link TipoLiquidacao#ASSINCRONA}
     */
    @Override
    public TipoLiquidacao tipoLiquidacao() {
        return TipoLiquidacao.ASSINCRONA;
    }

    /**
     * Fornece o adaptador PIX.
     *
     * @return gateway PIX
     */
    @Override
    public GatewayPagamentoPort criarGateway() {
        return gateway;
    }

    /**
     * Instruções de pagamento PIX.
     *
     * @return formatador que exibe a chave copia-e-cola e a validade
     */
    @Override
    public FormatadorInstrucoesPagamento criarFormatadorInstrucoes() {
        return (resposta, valor) -> String.format(
                "Pague R$ %s com o PIX copia-e-cola: %s (válido até %s).",
                valor.toPlainString(), resposta.chavePixCopiaECola(), resposta.expiraEm());
    }
}
