package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo.ApiCorreiosSimulada;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.RegiaoEntrega;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Base comum dos adaptadores dos serviços dos Correios (PAC e SEDEX).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> <b>Adapter</b> + <b>Template Method</b>. O algoritmo de
 * tradução (gramas → quilos em texto, chamada da API, texto com vírgula → {@link BigDecimal}) é o mesmo para os
 * dois serviços; as subclasses só informam o código do serviço e a modalidade.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public abstract class AdaptadorCorreiosBase implements CalculadoraFretePort {

    /** CEP do centro de distribuição da loja (origem de todas as entregas). */
    protected static final String CEP_ORIGEM_CENTRO_DISTRIBUICAO = "01310100";

    private final ApiCorreiosSimulada apiCorreios;

    /**
     * Construtor com a API externa injetada.
     *
     * @param apiCorreios cliente da API dos Correios (Adaptee)
     */
    protected AdaptadorCorreiosBase(ApiCorreiosSimulada apiCorreios) {
        this.apiCorreios = apiCorreios;
    }

    /**
     * Código do serviço na API dos Correios.
     *
     * <p>Papel no Design Pattern: método abstrato do Template Method.</p>
     *
     * @return código do serviço (ex.: {@code "04510"})
     */
    protected abstract String codigoServico();

    /**
     * Traduz a chamada interna para a API dos Correios e a resposta de volta para {@link CotacaoFrete}.
     *
     * <p>Papel no Design Pattern: método template (fluxo fixo de tradução da Anti-Corruption Layer).</p>
     *
     * @param cepDestino CEP de destino
     * @param pesoTotalGramas peso total em gramas
     * @return cotação no modelo interno
     * @throws RegraNegocioModulo3Exception se a API externa devolver erro
     */
    @Override
    public final CotacaoFrete cotar(String cepDestino, int pesoTotalGramas) {
        // 1. Tradução de entrada: gramas (int) -> quilos em texto com vírgula, CEP sem máscara
        String pesoKg = BigDecimal.valueOf(Math.max(0, pesoTotalGramas))
                .divide(new BigDecimal("1000"), 3, RoundingMode.HALF_EVEN)
                .toPlainString()
                .replace(".", ",");
        String cep = RegiaoEntrega.normalizarCep(cepDestino);

        // 2. Chamada ao sistema externo
        ApiCorreiosSimulada.RespostaPrecoPrazo resposta =
                apiCorreios.calcularPrecoPrazo(codigoServico(), CEP_ORIGEM_CENTRO_DISTRIBUICAO, cep, pesoKg);
        if (resposta.erro() != null) {
            throw new RegraNegocioModulo3Exception("Correios indisponível: " + resposta.erro());
        }

        // 3. Tradução de saída: "28,50" -> BigDecimal 28.50
        BigDecimal valor = new BigDecimal(resposta.valor().replace(".", "").replace(",", "."));
        ModalidadeFrete modalidade = modalidade();
        return CotacaoFrete.semBeneficio(modalidade, valor, resposta.prazoEntrega());
    }
}
