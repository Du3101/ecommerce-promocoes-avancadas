package br.edu.fatec.ecommerce.modulo3.frete.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável com o resultado da cotação de uma modalidade de frete.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> modelo <b>interno</b> do domínio. Cada adaptador de
 * transportadora (Adapter / Anti-Corruption Layer) traduz o formato proprietário da API externa para este objeto,
 * de modo que o restante do sistema nunca vê os formatos das transportadoras.
 *
 * @param modalidade modalidade cotada
 * @param transportadora empresa responsável
 * @param valorOriginal preço calculado pela transportadora
 * @param valorCobrado preço efetivamente cobrado do cliente (zero se frete grátis)
 * @param freteGratisAplicado se a política de frete grátis zerou esta cotação
 * @param prazoDiasUteis prazo de entrega em dias úteis
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public record CotacaoFrete(
        ModalidadeFrete modalidade,
        String transportadora,
        BigDecimal valorOriginal,
        BigDecimal valorCobrado,
        boolean freteGratisAplicado,
        int prazoDiasUteis
) {

    /**
     * Construtor canônico com validação e padronização monetária (escala 2, HALF_EVEN).
     *
     * @param modalidade modalidade cotada
     * @param transportadora empresa responsável
     * @param valorOriginal preço calculado
     * @param valorCobrado preço cobrado
     * @param freteGratisAplicado indicador de frete grátis
     * @param prazoDiasUteis prazo em dias úteis (mínimo 1)
     */
    public CotacaoFrete {
        Objects.requireNonNull(modalidade, "Modalidade é obrigatória.");
        Objects.requireNonNull(transportadora, "Transportadora é obrigatória.");
        valorOriginal = Objects.requireNonNull(valorOriginal, "Valor original é obrigatório.")
                .setScale(2, RoundingMode.HALF_EVEN);
        valorCobrado = Objects.requireNonNull(valorCobrado, "Valor cobrado é obrigatório.")
                .setScale(2, RoundingMode.HALF_EVEN);
        prazoDiasUteis = Math.max(1, prazoDiasUteis);
    }

    /**
     * Cria uma cotação sem benefício (valor cobrado = valor original).
     *
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> usado pelos adaptadores.</p>
     *
     * @param modalidade modalidade cotada
     * @param valor preço calculado
     * @param prazoDiasUteis prazo em dias úteis
     * @return nova cotação
     */
    public static CotacaoFrete semBeneficio(ModalidadeFrete modalidade, BigDecimal valor, int prazoDiasUteis) {
        return new CotacaoFrete(modalidade, modalidade.getTransportadora(), valor, valor, false, prazoDiasUteis);
    }

    /**
     * Devolve uma nova cotação com o valor cobrado zerado pela política de frete grátis.
     *
     * <p>Papel no Design Pattern: operação imutável do Value Object (retorna nova instância).</p>
     *
     * @return cópia com {@code valorCobrado = 0,00} e {@code freteGratisAplicado = true}
     */
    public CotacaoFrete comFreteGratis() {
        return new CotacaoFrete(modalidade, transportadora, valorOriginal, BigDecimal.ZERO, true, prazoDiasUteis);
    }
}
