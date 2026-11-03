package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utilitário e Value Object de operações aritméticas seguras para o domínio financeiro.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atende ao requisito de <i>Operações Matemáticas com BigDecimal</i> estipulado na Constituição
 * (Princípio II). Garante o encapsulamento das regras de arredondamento bancário padronizado
 * ({@link RoundingMode#HALF_EVEN}), precisão decimal fixa de 2 dígitos (centavos), prevenção de
 * divisões por zero, garantia inegociável de piso zero (para evitar preços líquidos negativos)
 * e algoritmo determinístico de rateio de descontos proporcionais sem perda residual de centavos.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public final class OperacoesMonetarias {

    /**
     * Escala monetária padrão para a moeda corrente (2 casas decimais para centavos).
     */
    public static final int ESCALA_PADRAO = 2;

    /**
     * Modo de arredondamento bancário padrão (arredondamento para o par mais próximo).
     */
    public static final RoundingMode ARREDONDAMENTO_PADRAO = RoundingMode.HALF_EVEN;

    /**
     * Representação segura de valor zero com escala 2.
     */
    public static final BigDecimal ZERO_MONETARIO = BigDecimal.ZERO.setScale(ESCALA_PADRAO, ARREDONDAMENTO_PADRAO);

    /**
     * Construtor privado para impedir instanciação direta deste utilitário de domínio.
     *
     * <p>Objetivo funcional: Classe puramente estática e imutável.
     * <p>Papel no padrão: Garante que o utilitário não possua estado mutável compartilhado.
     */
    private OperacoesMonetarias() {
        // Construtor privado para evitar instanciação
    }

    /**
     * Normaliza um valor numérico para a escala de 2 casas decimais e arredondamento HALF_EVEN.
     *
     * <p>Objetivo funcional: Elimina variações de escala em valores de entrada, garantindo centavos padronizados.
     * <p>Papel no padrão: Operação fundamental de higienização de escala em BigDecimal.
     *
     * @param valor Valor monetário bruto (pode ser nulo).
     * @return BigDecimal normalizado com 2 casas decimais; se nulo, retorna ZERO_MONETARIO.
     */
    public static BigDecimal normalizar(BigDecimal valor) {
        if (valor == null) {
            return ZERO_MONETARIO;
        }
        return valor.setScale(ESCALA_PADRAO, ARREDONDAMENTO_PADRAO);
    }

    /**
     * Calcula o valor percentual de desconto sobre uma quantia base monetária.
     *
     * <p>Objetivo funcional: Aplica uma alíquota percentual (ex.: 15.00%) sobre um subtotal.
     * <p>Papel no padrão: Encapsula a fórmula {@code (base * percentual) / 100} com controle de precisão.
     *
     * @param base Valor monetário base sobre o qual incide a taxa.
     * @param percentual Taxa percentual (ex.: 10 para 10%).
     * @return Valor monetário resultante do percentual, normalizado em 2 casas decimais.
     */
    public static BigDecimal calcularPercentual(BigDecimal base, BigDecimal percentual) {
        if (base == null || percentual == null || base.compareTo(BigDecimal.ZERO) <= 0 || percentual.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_MONETARIO;
        }
        BigDecimal baseNorm = normalizar(base);
        BigDecimal divisorCem = BigDecimal.valueOf(100);
        BigDecimal descontoBruto = baseNorm.multiply(percentual).divide(divisorCem, ESCALA_PADRAO, ARREDONDAMENTO_PADRAO);
        return normalizar(descontoBruto);
    }

    /**
     * Subtrai o desconto de um valor base aplicando a política obrigatória de piso zero.
     *
     * <p>Objetivo funcional: Impede que descontos agressivos acumulados tornem o total a pagar negativo.
     * <p>Papel no padrão: Implementação do Piso Zero {@code Math.max(0, base - desconto)} da Constituição.
     *
     * @param valorBase Valor base original antes do abatimento.
     * @param valorDesconto Quantia a ser abatida.
     * @return Valor líquido resultante, com garantia de nunca ser inferior a 0.00.
     */
    public static BigDecimal aplicarDescontoComPisoZero(BigDecimal valorBase, BigDecimal valorDesconto) {
        BigDecimal base = normalizar(valorBase);
        BigDecimal desc = normalizar(valorDesconto);
        BigDecimal liquido = base.subtract(desc);
        if (liquido.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO_MONETARIO;
        }
        return normalizar(liquido);
    }

    /**
     * Realiza o rateio proporcional de um valor de desconto fixo global entre múltiplos valores individuais,
     * garantindo que a soma dos descontos rateados seja exatamente igual ao desconto total (sem desvios de centavos).
     *
     * <p>Objetivo funcional: Ratear R$ 10,00 de desconto entre 3 itens de R$ 100,00 cada de forma justa e balanceada.
     * <p>Papel no padrão: Algoritmo de conservação monetária para evitar dízimas residuais.
     *
     * @param descontoTotalGlobal Desconto global a ser distribuído.
     * @param valoresBasesItens Lista com os valores individuais de cada item elegível.
     * @return Lista com os descontos atribuídos a cada item na mesma ordem da entrada.
     */
    public static List<BigDecimal> ratearDescontoProporcionalmente(BigDecimal descontoTotalGlobal, List<BigDecimal> valoresBasesItens) {
        Objects.requireNonNull(valoresBasesItens, "Lista de valores base não pode ser nula");
        List<BigDecimal> resultado = new ArrayList<>();
        if (valoresBasesItens.isEmpty() || descontoTotalGlobal == null || descontoTotalGlobal.compareTo(BigDecimal.ZERO) <= 0) {
            for (int i = 0; i < valoresBasesItens.size(); i++) {
                resultado.add(ZERO_MONETARIO);
            }
            return resultado;
        }

        BigDecimal somaTotalBases = BigDecimal.ZERO;
        for (BigDecimal v : valoresBasesItens) {
            somaTotalBases = somaTotalBases.add(normalizar(v));
        }

        if (somaTotalBases.compareTo(BigDecimal.ZERO) <= 0) {
            for (int i = 0; i < valoresBasesItens.size(); i++) {
                resultado.add(ZERO_MONETARIO);
            }
            return resultado;
        }

        BigDecimal descontoNormalizado = normalizar(descontoTotalGlobal);
        BigDecimal descontoDistribuidoAteAgora = BigDecimal.ZERO;

        for (int i = 0; i < valoresBasesItens.size(); i++) {
            BigDecimal baseItem = normalizar(valoresBasesItens.get(i));
            if (i == valoresBasesItens.size() - 1) {
                // Último item absorve o centavo residual de arredondamento
                BigDecimal parcelaFinal = descontoNormalizado.subtract(descontoDistribuidoAteAgora);
                if (parcelaFinal.compareTo(BigDecimal.ZERO) < 0) {
                    parcelaFinal = ZERO_MONETARIO;
                }
                resultado.add(normalizar(parcelaFinal));
            } else {
                BigDecimal fracao = baseItem.divide(somaTotalBases, 6, ARREDONDAMENTO_PADRAO);
                BigDecimal parcela = fracao.multiply(descontoNormalizado).setScale(ESCALA_PADRAO, ARREDONDAMENTO_PADRAO);
                descontoDistribuidoAteAgora = descontoDistribuidoAteAgora.add(parcela);
                resultado.add(parcela);
            }
        }

        return resultado;
    }
}

