package br.edu.fatec.ecommerce.modulo3.pedido.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável que registra a memória de cálculo financeiro consolidada de um Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa o <i>Snapshot Comercial Imutável</i> (Constituição, Princípio IV) com garantia de
 * piso zero e integridade monetária através de {@link BigDecimal} com escala 2 e arredondamento {@code HALF_EVEN}.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Embeddable
public class SnapshotFinanceiro {

    @Column(name = "subtotal_bruto", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalBruto;

    @Column(name = "total_descontos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDescontos;

    @Column(name = "valor_frete", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorFrete;

    @Column(name = "frete_gratis", nullable = false)
    private boolean freteGratis;

    @Column(name = "valor_total_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotalLiquido;

    /**
     * Construtor protegido exigido pela JPA.
     */
    protected SnapshotFinanceiro() {
    }

    /**
     * Construtor completo com validação de escala e piso zero inegociável.
     *
     * @param subtotalBruto Soma bruta dos itens do pedido.
     * @param totalDescontos Total de descontos rateados (promoções e cupons).
     * @param valorFrete Custo cobrado pelo frete.
     * @param freteGratis Indicador se a política de frete grátis foi aplicada.
     * @param valorTotalLiquido Total final efetivamente faturado.
     */
    public SnapshotFinanceiro(
            BigDecimal subtotalBruto,
            BigDecimal totalDescontos,
            BigDecimal valorFrete,
            boolean freteGratis,
            BigDecimal valorTotalLiquido) {
        this.subtotalBruto = padronizar(Objects.requireNonNull(subtotalBruto, "Subtotal bruto é obrigatório."));
        this.totalDescontos = padronizar(totalDescontos != null ? totalDescontos : BigDecimal.ZERO);
        this.valorFrete = padronizar(valorFrete != null ? valorFrete : BigDecimal.ZERO);
        this.freteGratis = freteGratis;
        this.valorTotalLiquido = padronizar(Objects.requireNonNull(valorTotalLiquido, "Valor líquido é obrigatório."));
    }

    /**
     * Método fábrica estático que computa os totais e garante o piso zero e consistência.
     *
     * @param subtotalBruto Soma dos itens.
     * @param totalDescontos Descontos acumulados.
     * @param valorFrete Frete cobrado.
     * @param freteGratis Indicador de frete grátis.
     * @return Instância imutável de SnapshotFinanceiro.
     */
    public static SnapshotFinanceiro criarComPisoZero(
            BigDecimal subtotalBruto,
            BigDecimal totalDescontos,
            BigDecimal valorFrete,
            boolean freteGratis) {
        BigDecimal seguroSubtotal = padronizar(subtotalBruto);
        BigDecimal seguroDescontos = padronizar(totalDescontos != null ? totalDescontos : BigDecimal.ZERO);
        BigDecimal seguroFrete = freteGratis ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN) : padronizar(valorFrete != null ? valorFrete : BigDecimal.ZERO);

        BigDecimal liquidoItens = seguroSubtotal.subtract(seguroDescontos);
        if (liquidoItens.compareTo(BigDecimal.ZERO) < 0) {
            liquidoItens = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal totalFinal = liquidoItens.add(seguroFrete).setScale(2, RoundingMode.HALF_EVEN);
        return new SnapshotFinanceiro(seguroSubtotal, seguroDescontos, seguroFrete, freteGratis, totalFinal);
    }

    /**
     * Ajusta e padroniza valores para 2 casas decimais com arredondamento bancário HALF_EVEN.
     *
     * @param valor Valor monetário bruto.
     * @return BigDecimal formatado com escala 2.
     */
    private static BigDecimal padronizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Retorna o subtotal bruto dos itens.
     *
     * @return Subtotal dos produtos sem descontos.
     */
    public BigDecimal getSubtotalBruto() {
        return subtotalBruto;
    }

    /**
     * Retorna o total de descontos deduzidos.
     *
     * @return Valor consolidado de descontos.
     */
    public BigDecimal getTotalDescontos() {
        return totalDescontos;
    }

    /**
     * Retorna o valor cobrado pelo frete.
     *
     * @return Valor do frete.
     */
    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    /**
     * Indica se a entrega possui o benefício de frete grátis.
     *
     * @return {@code true} se o frete foi isentado.
     */
    public boolean isFreteGratis() {
        return freteGratis;
    }

    /**
     * Retorna o valor total líquido final faturado ao cliente.
     *
     * @return Total líquido a pagar.
     */
    public BigDecimal getValorTotalLiquido() {
        return valorTotalLiquido;
    }
}
