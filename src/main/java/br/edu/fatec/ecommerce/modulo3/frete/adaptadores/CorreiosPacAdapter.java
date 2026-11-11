package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Adaptador simulado para cálculo de frete econômico dos Correios (PAC).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Concrete Adapter do padrão <b>Adapter</b>. Converte parâmetros físicos e de destino
 * em cotação de frete econômica, aplicando a regra de isenção de frete grátis quando qualificado.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class CorreiosPacAdapter implements CalculadoraFretePort {

    private static final BigDecimal META_VALOR_FRETE_GRATIS = new BigDecimal("250.00");
    private static final int PESO_MAXIMO_FRETE_GRATIS_GRAMAS = 5000;

    @Override
    public boolean suporta(ModalidadeFrete modalidade) {
        return modalidade == ModalidadeFrete.CORREIOS_PAC;
    }

    @Override
    public CotacaoFrete calcularFrete(String cepDestino, int pesoTotalGramas, BigDecimal subtotalLiquido) {
        BigDecimal base = new BigDecimal("20.00");
        BigDecimal adicionalPeso = BigDecimal.valueOf(Math.max(0, pesoTotalGramas))
                .multiply(new BigDecimal("0.005"))
                .setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal valorOriginal = base.add(adicionalPeso).setScale(2, RoundingMode.HALF_EVEN);

        boolean elegivelFreteGratis = subtotalLiquido != null
                && subtotalLiquido.compareTo(META_VALOR_FRETE_GRATIS) >= 0
                && pesoTotalGramas <= PESO_MAXIMO_FRETE_GRATIS_GRAMAS;

        BigDecimal valorCobrado = elegivelFreteGratis ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN) : valorOriginal;

        return new CotacaoFrete(
                ModalidadeFrete.CORREIOS_PAC,
                ModalidadeFrete.CORREIOS_PAC.getTransportadora(),
                valorOriginal,
                valorCobrado,
                elegivelFreteGratis,
                6
        );
    }
}
