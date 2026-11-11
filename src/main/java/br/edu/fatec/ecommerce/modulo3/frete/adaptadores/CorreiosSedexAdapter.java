package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para frete expresso prioritário dos Correios (SEDEX).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Adapter do padrão <b>Adapter</b>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CorreiosSedexAdapter implements CalculadoraFretePort {

    @Override
    public boolean suporta(ModalidadeFrete modalidade) {
        return modalidade == ModalidadeFrete.CORREIOS_SEDEX;
    }

    @Override
    public CotacaoFrete calcularFrete(String cepDestino, int pesoTotalGramas, BigDecimal subtotalLiquido) {
        BigDecimal base = new BigDecimal("35.00");
        BigDecimal adicionalPeso = BigDecimal.valueOf(Math.max(0, pesoTotalGramas))
                .multiply(new BigDecimal("0.008"))
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal valorOriginal = base.add(adicionalPeso).setScale(2, RoundingMode.HALF_EVEN);

        return new CotacaoFrete(
                ModalidadeFrete.CORREIOS_SEDEX,
                ModalidadeFrete.CORREIOS_SEDEX.getTransportadora(),
                valorOriginal,
                valorOriginal,
                false,
                2
        );
    }
}
