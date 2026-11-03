package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Componente concreto base do padrão Decorator contendo o subtotal inicial do carrinho.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>ConcreteComponent</i> no padrão <i>Decorator</i>.
 * Representa o ponto de partida do pipeline de formação de preço antes da aplicação
 * de qualquer decorador de desconto ou encargo.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class PrecoBaseCarrinho implements PrecoCalculadoComponent {

    private final BigDecimal subtotalBruto;

    /**
     * Construtor com o subtotal bruto inicial.
     *
     * @param subtotalBruto Valor somado das linhas do carrinho.
     */
    public PrecoBaseCarrinho(BigDecimal subtotalBruto) {
        this.subtotalBruto = OperacoesMonetarias.normalizar(subtotalBruto);
    }

    @Override
    public BigDecimal obterSubtotalBruto() {
        return subtotalBruto;
    }

    @Override
    public BigDecimal obterTotalLiquido() {
        return subtotalBruto;
    }

    @Override
    public List<ParcelaDescontoVO> obterDiscriminativoDescontos() {
        return Collections.emptyList();
    }
}

