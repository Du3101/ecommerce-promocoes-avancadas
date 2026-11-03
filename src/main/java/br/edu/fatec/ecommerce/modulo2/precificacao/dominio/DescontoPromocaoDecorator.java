package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Decorador concreto que aplica a parcela de desconto concedida por uma campanha promocional.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como <i>ConcreteDecorator</i> no padrão <i>Decorator</i> (Constituição, Princípio II).
 * Subtrai o valor da promoção do total líquido acumulado pela camada anterior,
 * aplicando a garantia obrigatória de piso zero ({@link OperacoesMonetarias#aplicarDescontoComPisoZero})
 * e registrando a respectiva parcela descritiva no histórico de auditoria.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class DescontoPromocaoDecorator extends DescontoDecoratorAbstrato {

    private final String nomePromocao;
    private final BigDecimal valorDesconto;

    /**
     * Construtor do decorador de promoção.
     *
     * @param decorado Componente da camada anterior.
     * @param nomePromocao Nome descritivo da campanha aplicada.
     * @param valorDesconto Valor monetário do desconto a ser abatido.
     */
    public DescontoPromocaoDecorator(PrecoCalculadoComponent decorado, String nomePromocao, BigDecimal valorDesconto) {
        super(decorado);
        this.nomePromocao = Objects.requireNonNull(nomePromocao, "nomePromocao não pode ser nulo");
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
            lista.add(new ParcelaDescontoVO("PROMOCAO", nomePromocao, valorDesconto));
        }
        return lista;
    }
}

