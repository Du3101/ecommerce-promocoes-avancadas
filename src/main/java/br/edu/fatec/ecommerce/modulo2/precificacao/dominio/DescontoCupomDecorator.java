package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Decorador concreto que aplica a parcela de desconto concedida por um cupom promocional.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como <i>ConcreteDecorator</i> no padrão <i>Decorator</i>.
 * Subtrai o valor do cupom do total líquido da camada decorada anterior com garantia de piso zero
 * e adiciona a rubrica descritiva do cupom à memória de cálculo.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class DescontoCupomDecorator extends DescontoDecoratorAbstrato {

    private final String codigoCupom;
    private final BigDecimal valorDesconto;

    /**
     * Construtor do decorador de cupom.
     *
     * @param decorado Componente da camada anterior.
     * @param codigoCupom Código do cupom aplicado.
     * @param valorDesconto Valor monetário abatido pelo cupom.
     */
    public DescontoCupomDecorator(PrecoCalculadoComponent decorado, String codigoCupom, BigDecimal valorDesconto) {
        super(decorado);
        this.codigoCupom = Objects.requireNonNull(codigoCupom, "codigoCupom não pode ser nulo");
        this.valorDesconto = OperacoesMonetarias.normalizar(valorDesconto);
    }

    @Override
    public BigDecimal obterTotalLiquido() {
        BigDecimal base = decorado.obterTotalLiquido();
        return OperacoesMonetarias.aplicarDescontoComPisoZero(base, valorDesconto);
    }

    @Override
    public List<ParcelaDescontoVO> obterDiscriminativoDescontos() {
        List<ParcelaDescontoVO> lista = super.obterDiscriminativoDescontos();
        if (valorDesconto.compareTo(BigDecimal.ZERO) > 0) {
            lista.add(new ParcelaDescontoVO("CUPOM", "Cupom " + codigoCupom, valorDesconto));
        }
        return lista;
    }
}

