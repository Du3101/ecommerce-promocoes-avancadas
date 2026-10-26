package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

/**
 * Estratégia de precificação de um {@link ComboKitComposite} (Clarificação Q2 / FR-008).
 *
 * <p>Papel no Design Pattern: Parametriza o comportamento de {@code calcularPrecoVigente()}
 * no elemento <i>Composite</i> do catálogo.</p>
 */
public enum ModoPrecoCombo {

    /**
     * Soma recursivamente o preço vigente de todos os componentes multiplicados por suas quantidades
     * no kit, aplicando opcionalmente um percentual de desconto ({@code 0.00} a {@code 90.00}).
     */
    SOMA_RECURSIVA_COMPONENTES,

    /**
     * Utiliza um preço promocional fixo dedicado para o combo, mantendo peso, dimensões e
     * disponibilidade em estoque calculados recursivamente a partir dos componentes.
     */
    PRECO_FIXO_DEDICADO
}

