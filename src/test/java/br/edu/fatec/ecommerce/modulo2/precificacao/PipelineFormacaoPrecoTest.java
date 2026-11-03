package br.edu.fatec.ecommerce.modulo2.precificacao;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoComboKit;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoProduto;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.visitor.CalculadoraDescontoVisitor;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.DescontoCupomDecorator;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.DescontoPromocaoDecorator;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.MemoriaCalculoPrecoVO;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.PrecoBaseCarrinho;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.PrecoCalculadoComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários do padrão Decorator, Visitor com Double Dispatch e piso zero.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
class PipelineFormacaoPrecoTest {

    @Test
    @DisplayName("Padrão Decorator: Deve empilhar camadas de promoção e cupom com memória de cálculo detalhada")
    void deveEmpilharDecoratorsDePrecoComMemoriaCalculo() {
        // Base: R$ 500,00
        PrecoCalculadoComponent base = new PrecoBaseCarrinho(new BigDecimal("500.00"));

        // Decorador 1: Promoção R$ 50,00 OFF
        PrecoCalculadoComponent camada1 = new DescontoPromocaoDecorator(base, "Promoção Black Friday", new BigDecimal("50.00"));
        assertEquals(new BigDecimal("450.00"), camada1.obterTotalLiquido());

        // Decorador 2: Cupom R$ 30,00 OFF
        PrecoCalculadoComponent camada2 = new DescontoCupomDecorator(camada1, "PROMO30", new BigDecimal("30.00"));
        assertEquals(new BigDecimal("420.00"), camada2.obterTotalLiquido());

        MemoriaCalculoPrecoVO memoria = camada2.gerarMemoriaCalculo();
        assertEquals(new BigDecimal("500.00"), memoria.subtotalBruto());
        assertEquals(new BigDecimal("80.00"), memoria.totalDescontos());
        assertEquals(new BigDecimal("420.00"), memoria.totalLiquido());
        assertEquals(2, memoria.descontosDiscriminados().size());
        assertEquals("Promoção Black Friday", memoria.descontosDiscriminados().get(0).nomeBeneficio());
        assertEquals("Cupom PROMO30", memoria.descontosDiscriminados().get(1).nomeBeneficio());
    }

    @Test
    @DisplayName("Piso Zero: Descontos que excedem o subtotal devem cravar em R$ 0,00 sem valor negativo")
    void deveGarantirPisoZeroQuandoDescontoExcedeSubtotal() {
        PrecoCalculadoComponent base = new PrecoBaseCarrinho(new BigDecimal("100.00"));

        // Aplica promoção de R$ 150,00 em um carrinho de R$ 100,00
        PrecoCalculadoComponent decoradorExagerado = new DescontoPromocaoDecorator(base, "Super Promo", new BigDecimal("150.00"));

        assertEquals(new BigDecimal("0.00"), decoradorExagerado.obterTotalLiquido());
        MemoriaCalculoPrecoVO memoria = decoradorExagerado.gerarMemoriaCalculo();
        assertEquals(new BigDecimal("0.00"), memoria.totalLiquido());
        assertTrue(memoria.totalLiquido().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    @DisplayName("Padrão Visitor & Double Dispatch: Deve inspecionar polimorficamente produtos e combos")
    void deveExecutarDoubleDispatchComVisitor() {
        UUID catPerifericos = UUID.randomUUID();
        UUID catGames = UUID.randomUUID();

        ItemCarrinhoProduto teclado = new ItemCarrinhoProduto(
                UUID.randomUUID(), UUID.randomUUID(), "TEC", "Teclado", new BigDecimal("300.00"), 2, 850, catPerifericos
        );
        ItemCarrinhoComboKit kit = new ItemCarrinhoComboKit(
                UUID.randomUUID(), UUID.randomUUID(), "KIT", "Kit Gamer", new BigDecimal("500.00"), 1, 1200, catGames
        );

        CalculadoraDescontoVisitor visitorGeral = new CalculadoraDescontoVisitor();

        // Double Dispatch: item.aceitar(visitor) -> visitor.visitarProduto(this) / visitor.visitarComboKit(this)
        teclado.aceitar(visitorGeral);
        kit.aceitar(visitorGeral);

        assertEquals(new BigDecimal("600.00"), visitorGeral.getSubtotalProdutos()); // 300 * 2
        assertEquals(new BigDecimal("500.00"), visitorGeral.getSubtotalCombos()); // 500 * 1
        assertEquals(new BigDecimal("1100.00"), visitorGeral.getSubtotalGeral());
        assertEquals(2, visitorGeral.getQuantidadeProdutos());
        assertEquals(1, visitorGeral.getQuantidadeCombos());
        assertEquals(3, visitorGeral.getQuantidadeTotal());

        // Testando visitor com filtro de categoria
        CalculadoraDescontoVisitor visitorPerifericos = new CalculadoraDescontoVisitor(catPerifericos);
        teclado.aceitar(visitorPerifericos);
        kit.aceitar(visitorPerifericos);

        assertEquals(new BigDecimal("600.00"), visitorPerifericos.getSubtotalGeral());
        assertEquals(2, visitorPerifericos.getQuantidadeTotal());
    }

    @Test
    @DisplayName("Operações Monetárias: Rateio proporcional sem perda residual de centavos")
    void deveRatearDescontoProporcionalmenteSemPerdaDeCentavos() {
        // Ratear R$ 10,00 entre 3 itens de R$ 100,00 cada
        List<BigDecimal> bases = List.of(new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"));
        List<BigDecimal> rateado = OperacoesMonetarias.ratearDescontoProporcionalmente(new BigDecimal("10.00"), bases);

        assertEquals(3, rateado.size());
        BigDecimal somaRateada = rateado.get(0).add(rateado.get(1)).add(rateado.get(2));
        assertEquals(new BigDecimal("10.00"), OperacoesMonetarias.normalizar(somaRateada));
    }
}
