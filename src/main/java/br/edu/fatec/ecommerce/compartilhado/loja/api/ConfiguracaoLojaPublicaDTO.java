package br.edu.fatec.ecommerce.compartilhado.loja.api;

import java.math.BigDecimal;

/**
 * Projeção pública de configurações não sensíveis da loja virtual para a interface web.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * DTO de saída imutável da fronteira REST pública, expondo ao front-end o sinalizador de modo
 * demonstração e os parâmetros informativos da regra de frete grátis sem vazar tokens ou
 * credenciais internas.</p>
 *
 * @param modoDemonstracaoAtivo {@code true} quando recursos e contas de demonstração estão habilitados
 * @param freteGratisValorMinimo subtotal líquido mínimo para elegibilidade a frete grátis no PAC
 * @param freteGratisPesoMaximoGramas peso máximo total em gramas aceito pela regra de frete grátis
 */
public record ConfiguracaoLojaPublicaDTO(
    boolean modoDemonstracaoAtivo,
    BigDecimal freteGratisValorMinimo,
    int freteGratisPesoMaximoGramas
) {

    /**
     * Cria uma instância de {@link ConfiguracaoLojaPublicaDTO} a partir das propriedades de ambiente.
     *
     * <p>Objetivo Funcional: Padronizar a construção do DTO público de configuração da loja.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> de montagem de DTO imutável.</p>
     *
     * @param modoDemonstracaoAtivo indica se o ambiente opera em modo demonstração
     * @param freteGratisValorMinimo valor mínimo líquido configurado para frete grátis
     * @param freteGratisPesoMaximoGramas peso máximo permitido em gramas para frete grátis
     * @return instância imutável de {@link ConfiguracaoLojaPublicaDTO}
     */
    public static ConfiguracaoLojaPublicaDTO criar(
        boolean modoDemonstracaoAtivo,
        BigDecimal freteGratisValorMinimo,
        int freteGratisPesoMaximoGramas
    ) {
        BigDecimal valorNormalizado = freteGratisValorMinimo != null
            ? freteGratisValorMinimo
            : new BigDecimal("250.00");
        return new ConfiguracaoLojaPublicaDTO(
            modoDemonstracaoAtivo,
            valorNormalizado,
            freteGratisPesoMaximoGramas
        );
    }
}
