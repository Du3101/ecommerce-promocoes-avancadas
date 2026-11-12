package br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.RegiaoEntrega;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Simulação determinística da API de preço e prazo dos Correios (sistema externo, fora do nosso controle).
 *
 * <p>Imita características típicas de uma API legada: serviços identificados por <b>códigos</b>
 * ({@code "04510"} = PAC, {@code "04014"} = SEDEX), peso informado em <b>quilos como texto com vírgula</b> e
 * valores devolvidos como <b>texto no formato brasileiro</b> ({@code "28,50"}).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> é o <i>Adaptee</i> do padrão <b>Adapter</b>. Nenhuma classe do
 * domínio conversa com ela diretamente: só os adaptadores {@code CorreiosPacAdapter} e
 * {@code CorreiosSedexAdapter}, que fazem a tradução (Anti-Corruption Layer).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class ApiCorreiosSimulada {

    /** Código de serviço do PAC na API dos Correios. */
    public static final String SERVICO_PAC = "04510";

    /** Código de serviço do SEDEX na API dos Correios. */
    public static final String SERVICO_SEDEX = "04014";

    /**
     * Resposta no formato proprietário dos Correios.
     *
     * @param codigoServico código do serviço consultado
     * @param valor valor em reais no formato brasileiro (ex.: {@code "28,50"})
     * @param prazoEntrega prazo em dias úteis
     * @param erro mensagem de erro ou {@code null}
     */
    public record RespostaPrecoPrazo(String codigoServico, String valor, int prazoEntrega, String erro) {
    }

    /**
     * Calcula preço e prazo de um serviço dos Correios.
     *
     * <p>Fórmula simulada: (tarifa base + valor por kg × peso) × fator da região do CEP; prazo base + dias da região.</p>
     *
     * @param codigoServico {@link #SERVICO_PAC} ou {@link #SERVICO_SEDEX}
     * @param cepOrigem CEP de origem (centro de distribuição)
     * @param cepDestino CEP de destino
     * @param pesoKg peso em quilos no formato brasileiro (ex.: {@code "1,700"})
     * @return resposta no formato dos Correios
     */
    public RespostaPrecoPrazo calcularPrecoPrazo(String codigoServico, String cepOrigem, String cepDestino, String pesoKg) {
        // 1. Interpreta os parâmetros no formato da API externa
        BigDecimal peso = new BigDecimal(pesoKg.replace(",", "."));
        RegiaoEntrega regiao = RegiaoEntrega.deCep(cepDestino);
        boolean pac = SERVICO_PAC.equals(codigoServico);

        // 2. Tabela tarifária simulada do serviço
        BigDecimal tarifaBase = pac ? new BigDecimal("18.00") : new BigDecimal("30.00");
        BigDecimal valorPorKg = pac ? new BigDecimal("4.50") : new BigDecimal("7.00");
        int prazoBase = pac ? 6 : 2;

        // 3. Preço final regionalizado e prazo
        BigDecimal valor = tarifaBase.add(valorPorKg.multiply(peso))
                .multiply(regiao.getFatorPreco())
                .setScale(2, RoundingMode.HALF_EVEN);
        int prazo = prazoBase + regiao.getDiasAdicionais();

        // 4. Devolve no formato proprietário (texto com vírgula)
        DecimalFormat formatoBrasileiro = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
        return new RespostaPrecoPrazo(codigoServico, formatoBrasileiro.format(valor), prazo, null);
    }
}
