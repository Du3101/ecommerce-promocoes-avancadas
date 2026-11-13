package br.edu.fatec.ecommerce.modulo3.checkout.aplicacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Distribui o desconto total do carrinho entre as linhas do pedido.
 *
 * <p><b>Por que é necessário:</b> o Módulo 2 informa apenas o desconto total (promoções + cupom). O snapshot do
 * pedido precisa do desconto por item para relatórios por produto e para devoluções parciais futuras.
 *
 * <p><b>Regra (rateio proporcional):</b> cada linha recebe uma parcela proporcional ao seu valor bruto
 * ({@code preço × quantidade}). As parcelas são arredondadas em 2 casas (HALF_EVEN); a última linha recebe o
 * que sobrar, para que a soma das parcelas seja exatamente o desconto total. O desconto por unidade é a parcela
 * dividida pela quantidade.
 *
 * <p><b>Papel Arquitetural:</b> objeto de <b>Operações Matemáticas</b> da Constituição (aritmética monetária
 * explícita com {@link BigDecimal}, arredondamento definido e piso zero).
 *
 * <p><b>Limitação conhecida:</b> ao dividir a parcela pela quantidade, o desconto unitário é arredondado; por isso
 * a soma dos subtotais das linhas pode diferir em centavos do total. O valor cobrado é sempre o total oficial do
 * {@code SnapshotFinanceiro}, nunca a soma das linhas.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public final class CalculadoraRateioDesconto {

    /**
     * Classe utilitária: não instanciável.
     */
    private CalculadoraRateioDesconto() {
    }

    /**
     * Uma linha a ratear.
     *
     * @param precoUnitario preço unitário sem desconto
     * @param quantidade quantidade
     */
    public record LinhaRateio(BigDecimal precoUnitario, int quantidade) {
    }

    /**
     * Calcula o desconto por unidade de cada linha.
     *
     * @param linhas linhas do pedido, na ordem
     * @param descontoTotal desconto total a distribuir
     * @return desconto unitário de cada linha (mesma ordem de {@code linhas})
     */
    public static List<BigDecimal> calcularDescontoUnitario(List<LinhaRateio> linhas, BigDecimal descontoTotal) {
        List<BigDecimal> resultado = new ArrayList<>();
        BigDecimal desconto = descontoTotal == null ? BigDecimal.ZERO : descontoTotal.max(BigDecimal.ZERO);

        // 1. Valor bruto de cada linha e soma geral
        List<BigDecimal> brutos = linhas.stream()
                .map(l -> l.precoUnitario().multiply(BigDecimal.valueOf(l.quantidade())))
                .toList();
        BigDecimal somaBruta = brutos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Sem desconto ou sem valor bruto: todas as linhas sem desconto
        if (desconto.signum() == 0 || somaBruta.signum() == 0) {
            linhas.forEach(l -> resultado.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN)));
            return resultado;
        }

        // 3. Parcela proporcional por linha; a última absorve a sobra de arredondamento
        BigDecimal distribuido = BigDecimal.ZERO;
        for (int i = 0; i < linhas.size(); i++) {
            BigDecimal parcela;
            if (i == linhas.size() - 1) {
                parcela = desconto.subtract(distribuido);
            } else {
                parcela = desconto.multiply(brutos.get(i)).divide(somaBruta, 2, RoundingMode.HALF_EVEN);
                distribuido = distribuido.add(parcela);
            }
            // 4. Piso zero e teto no valor bruto da linha
            parcela = parcela.max(BigDecimal.ZERO).min(brutos.get(i));
            // 5. Desconto por unidade
            int quantidade = Math.max(1, linhas.get(i).quantidade());
            resultado.add(parcela.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_EVEN));
        }
        return resultado;
    }
}
