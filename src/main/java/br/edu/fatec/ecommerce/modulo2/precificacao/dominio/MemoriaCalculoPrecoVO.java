package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Value Object consolidado com a memória de cálculo completa e transparente da formação de preço.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Resultado consolidado final do encadeamento do padrão <i>Decorator</i>.
 * Expõe o subtotal bruto inicial, a lista detalhada de cada abatimento aplicado
 * e o valor líquido final com piso zero garantido.
 *
 * @param subtotalBruto Valor bruto original antes de qualquer desconto.
 * @param totalDescontos Soma total de todas as parcelas de abatimento.
 * @param totalLiquido Valor final a ser pago pelo cliente (com piso zero).
 * @param descontosDiscriminados Lista detalhada de todas as parcelas concedidas.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public record MemoriaCalculoPrecoVO(
        BigDecimal subtotalBruto,
        BigDecimal totalDescontos,
        BigDecimal totalLiquido,
        List<ParcelaDescontoVO> descontosDiscriminados
) {
    public MemoriaCalculoPrecoVO {
        subtotalBruto = OperacoesMonetarias.normalizar(subtotalBruto);
        totalDescontos = OperacoesMonetarias.normalizar(totalDescontos);
        totalLiquido = OperacoesMonetarias.normalizar(totalLiquido);
        descontosDiscriminados = descontosDiscriminados != null ? List.copyOf(descontosDiscriminados) : Collections.emptyList();
    }
}

