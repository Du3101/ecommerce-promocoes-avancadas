package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface Component do padrão Decorator para a formação de preço do carrinho.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a interface <i>Component</i> no padrão <i>Decorator</i> (Constituição, Princípio II).
 * Padroniza os métodos de consulta ao subtotal bruto, total líquido e à lista discriminada
 * de descontos acumulados pelas sucessivas camadas decoradoras.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public interface PrecoCalculadoComponent {

    /**
     * Retorna o subtotal bruto original dos produtos (sem descontos).
     *
     * @return BigDecimal normalizado com o valor bruto.
     */
    BigDecimal obterSubtotalBruto();

    /**
     * Retorna o valor líquido acumulado até a camada atual com aplicação do piso zero.
     *
     * @return BigDecimal normalizado do valor líquido.
     */
    BigDecimal obterTotalLiquido();

    /**
     * Retorna a lista discriminada de todas as parcelas de desconto aplicadas até o momento.
     *
     * @return Lista imutável de ParcelaDescontoVO.
     */
    List<ParcelaDescontoVO> obterDiscriminativoDescontos();

    /**
     * Gera a memória de cálculo completa e consolidada.
     *
     * @return Objeto de valor MemoriaCalculoPrecoVO.
     */
    default MemoriaCalculoPrecoVO gerarMemoriaCalculo() {
        BigDecimal subtotal = obterSubtotalBruto();
        BigDecimal liquido = obterTotalLiquido();
        BigDecimal totalDescontos = OperacoesMonetarias.normalizar(subtotal.subtract(liquido));
        if (totalDescontos.compareTo(BigDecimal.ZERO) < 0) {
            totalDescontos = OperacoesMonetarias.ZERO_MONETARIO;
        }
        return new MemoriaCalculoPrecoVO(subtotal, totalDescontos, liquido, obterDiscriminativoDescontos());
    }
}

