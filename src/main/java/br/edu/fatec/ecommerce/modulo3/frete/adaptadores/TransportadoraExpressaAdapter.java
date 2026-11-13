package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo.ApiTransportadoraExpressaSimulada;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.RegiaoEntrega;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Adaptador da transportadora privada expressa.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Concrete Adapter</i> / Anti-Corruption Layer. Traduz um
 * contrato bem diferente do dos Correios: preço em centavos ({@code long}) e prazo em horas, que viram
 * {@link BigDecimal} em reais e dias úteis no modelo interno.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class TransportadoraExpressaAdapter implements CalculadoraFretePort {

    private static final String CEP_ORIGEM_CENTRO_DISTRIBUICAO = "01310100";

    private final ApiTransportadoraExpressaSimulada api;

    /**
     * Construtor com a API externa injetada.
     *
     * @param api cliente da API da transportadora (Adaptee)
     */
    public TransportadoraExpressaAdapter(ApiTransportadoraExpressaSimulada api) {
        this.api = api;
    }

    /**
     * Modalidade atendida.
     *
     * @return {@link ModalidadeFrete#TRANSPORTADORA_EXPRESSA}
     */
    @Override
    public ModalidadeFrete modalidade() {
        return ModalidadeFrete.TRANSPORTADORA_EXPRESSA;
    }

    /**
     * Traduz a cotação da transportadora para o modelo interno.
     *
     * <p>Papel no Design Pattern: método adaptado ({@code request()} do Target → {@code specificRequest()} do Adaptee).</p>
     *
     * @param cepDestino CEP de destino
     * @param pesoTotalGramas peso total em gramas
     * @return cotação no modelo interno
     */
    @Override
    public CotacaoFrete cotar(String cepDestino, int pesoTotalGramas) {
        // 1. Monta o pedido no formato da transportadora
        ApiTransportadoraExpressaSimulada.PedidoCotacao pedido = new ApiTransportadoraExpressaSimulada.PedidoCotacao(
                CEP_ORIGEM_CENTRO_DISTRIBUICAO, RegiaoEntrega.normalizarCep(cepDestino), Math.max(0, pesoTotalGramas));

        // 2. Chama o sistema externo
        ApiTransportadoraExpressaSimulada.RespostaCotacao resposta = api.cotar(pedido);

        // 3. Traduz centavos -> reais e horas -> dias úteis (arredondando para cima)
        BigDecimal valorReais = BigDecimal.valueOf(resposta.precoCentavos()).movePointLeft(2);
        int prazoDias = (resposta.prazoHoras() + 23) / 24;
        return CotacaoFrete.semBeneficio(modalidade(), valorReais, prazoDias);
    }
}
