package br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.RegiaoEntrega;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Simulação determinística da API de cotação de uma transportadora privada expressa (sistema externo).
 *
 * <p>Imita uma API moderna, mas com contrato diferente do nosso: recebe peso em <b>gramas</b> e CEPs em campos
 * próprios, e devolve o preço em <b>centavos</b> ({@code long}) e o prazo em <b>horas</b>, com seguro de carga
 * já embutido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <i>Adaptee</i> do padrão <b>Adapter</b>, traduzido por
 * {@code TransportadoraExpressaAdapter}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class ApiTransportadoraExpressaSimulada {

    /**
     * Pedido de cotação no formato da transportadora.
     *
     * @param cepColeta CEP de coleta
     * @param cepEntrega CEP de entrega
     * @param pesoGramas peso em gramas
     */
    public record PedidoCotacao(String cepColeta, String cepEntrega, int pesoGramas) {
    }

    /**
     * Resposta no formato da transportadora.
     *
     * @param precoCentavos preço total (frete + seguro) em centavos
     * @param prazoHoras prazo de entrega em horas
     * @param seguroIncluido se o seguro de carga está incluído
     */
    public record RespostaCotacao(long precoCentavos, int prazoHoras, boolean seguroIncluido) {
    }

    /**
     * Cota uma entrega expressa.
     *
     * <p>Fórmula simulada: (R$ 40,00 + R$ 9,00/kg) × fator da região + R$ 5,00 de seguro; prazo 24 h + 24 h por
     * dia adicional da região.</p>
     *
     * @param pedido pedido de cotação
     * @return resposta no formato da transportadora
     */
    public RespostaCotacao cotar(PedidoCotacao pedido) {
        // 1. Região de destino a partir do CEP
        RegiaoEntrega regiao = RegiaoEntrega.deCep(pedido.cepEntrega());

        // 2. Preço regionalizado + seguro, convertido para centavos
        BigDecimal pesoKg = BigDecimal.valueOf(pedido.pesoGramas()).divide(new BigDecimal("1000"), 3, RoundingMode.HALF_EVEN);
        BigDecimal valor = new BigDecimal("40.00").add(new BigDecimal("9.00").multiply(pesoKg))
                .multiply(regiao.getFatorPreco())
                .add(new BigDecimal("5.00"))
                .setScale(2, RoundingMode.HALF_EVEN);
        long centavos = valor.movePointRight(2).longValueExact();

        // 3. Prazo em horas
        int prazoHoras = 24 + regiao.getDiasAdicionais() * 24;
        return new RespostaCotacao(centavos, prazoHoras, true);
    }
}
