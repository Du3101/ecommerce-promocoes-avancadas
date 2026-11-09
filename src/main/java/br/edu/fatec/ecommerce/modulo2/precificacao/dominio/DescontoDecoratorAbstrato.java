package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Decorador abstrato base do padrão Decorator para formação de preço.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a classe abstrata <i>Decorator</i> no padrão <i>Decorator</i>.
 * Mantém a referência protegida para o componente {@link PrecoCalculadoComponent} decorado
 * e delega as operações padrão de subtotal original e agregação de histórico de parcelas.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public abstract class DescontoDecoratorAbstrato implements PrecoCalculadoComponent {

    protected final PrecoCalculadoComponent decorado;

    /**
     * Construtor que recebe o componente a ser decorado.
     *
     * @param decorado Componente da camada anterior.
     */
    protected DescontoDecoratorAbstrato(PrecoCalculadoComponent decorado) {
        this.decorado = Objects.requireNonNull(decorado, "Componente decorado não pode ser nulo");
    }

    @Override
    public BigDecimal obterSubtotalBruto() {
        return decorado.obterSubtotalBruto();
    }

    @Override
    public List<ParcelaDescontoVO> obterDiscriminativoDescontos() {
        return new ArrayList<>(decorado.obterDiscriminativoDescontos());
    }
}

