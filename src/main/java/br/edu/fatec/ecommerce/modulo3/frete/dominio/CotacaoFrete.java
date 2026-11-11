package br.edu.fatec.ecommerce.modulo3.frete.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável que representa o resultado da cotação de frete para uma modalidade específica.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Value Object do domínio logístico encapsulando os cálculos do padrão <i>Adapter</i>.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public class CotacaoFrete {

    private final ModalidadeFrete modalidade;
    private final String transportadora;
    private final BigDecimal valorOriginal;
    private final BigDecimal valorCobrado;
    private final boolean freteGratisAplicado;
    private final int prazoDiasUteis;

    /**
     * Construtor completo da cotação com garantia de escala monetária.
     *
     * @param modalidade Modalidade do frete.
     * @param transportadora Nome da empresa responsável.
     * @param valorOriginal Valor de tabela da cotação.
     * @param valorCobrado Valor efetivo após regras de frete grátis.
     * @param freteGratisAplicado Indicador se houve isenção de taxa.
     * @param prazoDiasUteis Prazo estimado em dias úteis.
     */
    public CotacaoFrete(
            ModalidadeFrete modalidade,
            String transportadora,
            BigDecimal valorOriginal,
            BigDecimal valorCobrado,
            boolean freteGratisAplicado,
            int prazoDiasUteis) {
        this.modalidade = Objects.requireNonNull(modalidade, "Modalidade é obrigatória.");
        this.transportadora = Objects.requireNonNull(transportadora, "Transportadora é obrigatória.");
        this.valorOriginal = valorOriginal.setScale(2, RoundingMode.HALF_EVEN);
        this.valorCobrado = valorCobrado.setScale(2, RoundingMode.HALF_EVEN);
        this.freteGratisAplicado = freteGratisAplicado;
        this.prazoDiasUteis = Math.max(1, prazoDiasUteis);
    }

    public ModalidadeFrete getModalidade() { return modalidade; }
    public String getTransportadora() { return transportadora; }
    public BigDecimal getValorOriginal() { return valorOriginal; }
    public BigDecimal getValorCobrado() { return valorCobrado; }
    public boolean isFreteGratisAplicado() { return freteGratisAplicado; }
    public int getPrazoDiasUteis() { return prazoDiasUteis; }
}
