package br.edu.fatec.ecommerce.modulo2.promocoes;

import br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoProduto;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoAndComposite;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoCategoriaProduto;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoItemEspecificoSku;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoNotComposite;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoOrComposite;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoPrimeiraCompraCliente;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoRegiaoEntrega;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoValorMinimoCarrinho;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.ContextoElegibilidade;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias.PromocaoComboCruzadoCasado;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias.PromocaoLeveXPagueY;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias.PromocaoNula;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias.PromocaoPercentualCondicional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias.PromocaoValorFixoMetaCarrinho;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.FabricaDinamicaEstrategiaPromocao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do padrão Composite (Elegibilidade) e das 4 estratégias de promoção (Dynamic Factory).
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
class MotorPromocoesElegibilidadeTest {

    @Test
    @DisplayName("Padrão Composite: Deve avaliar árvore de elegibilidade com AND, OR, NOT e folhas")
    void deveAvaliarArvoreCompositeDeElegibilidade() {
        UUID categoriaEletronicos = UUID.randomUUID();
        PortaConsultaCatalogoCliente portaMock = Mockito.mock(PortaConsultaCatalogoCliente.class);
        when(portaMock.itemPertenceACategoriaOuSubcategoria(eq("SKU-TEC"), eq(categoriaEletronicos))).thenReturn(true);

        // Árvore: (ValorMinimo(200) AND PrimeiraCompra) OR (CategoriaEletronicos AND NOT(Regiao(RJ)))
        CondicaoAndComposite ramoEsquerdo = new CondicaoAndComposite()
                .adicionar(new CondicaoValorMinimoCarrinho(new BigDecimal("200.00")))
                .adicionar(new CondicaoPrimeiraCompraCliente());

        CondicaoAndComposite ramoDireito = new CondicaoAndComposite()
                .adicionar(new CondicaoCategoriaProduto(categoriaEletronicos))
                .adicionar(new CondicaoNotComposite(new CondicaoRegiaoEntrega("RJ")));

        CondicaoOrComposite arvoreGeral = new CondicaoOrComposite()
                .adicionar(ramoEsquerdo)
                .adicionar(ramoDireito);

        // Cenário 1: Cliente não é primeira compra, mas tem item de eletrônicos e entrega em SP -> true pelo ramo direito
        ItemCarrinhoProduto item1 = new ItemCarrinhoProduto(
                UUID.randomUUID(), UUID.randomUUID(), "SKU-TEC", "Teclado", new BigDecimal("100.00"), 1, 100, null
        );
        ContextoElegibilidade ctx1 = new ContextoElegibilidade(
                UUID.randomUUID(), false, "SP", new BigDecimal("100.00"), List.of(item1), portaMock
        );
        assertTrue(arvoreGeral.avaliar(ctx1));

        // Cenário 2: Cliente não é primeira compra, eletrônicos mas entrega no RJ -> false por ambos os ramos
        ContextoElegibilidade ctx2 = new ContextoElegibilidade(
                UUID.randomUUID(), false, "RJ", new BigDecimal("100.00"), List.of(item1), portaMock
        );
        assertFalse(arvoreGeral.avaliar(ctx2));

        // Cenário 3: Cliente em primeira compra com R$ 250,00 -> true pelo ramo esquerdo (mesmo entregando no RJ)
        ContextoElegibilidade ctx3 = new ContextoElegibilidade(
                UUID.randomUUID(), true, "RJ", new BigDecimal("250.00"), List.of(item1), portaMock
        );
        assertTrue(arvoreGeral.avaliar(ctx3));
    }

    @Test
    @DisplayName("Modalidade 1 (Leve X Pague Y): Deve bonificar unidades excedentes abatendo os itens de menor valor")
    void deveCalcularLeveXPagueY() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);
        // 3 itens: R$ 100, R$ 150 e R$ 200 -> Leve 3 Pague 2 bonifica o de menor valor (R$ 100.00)
        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "A", "Item A", new BigDecimal("100.00"), 1, 10, null
        ));
        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "B", "Item B", new BigDecimal("150.00"), 1, 10, null
        ));
        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "C", "Item C", new BigDecimal("200.00"), 1, 10, null
        ));

        CampanhaPromocional campanha = new CampanhaPromocional(
                UUID.randomUUID(), "Leve 3 Pague 2", "Regra 3x2", TipoMecanicaPromocao.LEVE_X_PAGUE_Y,
                "{\"qtdX\": 3, \"qtdY\": 2}", 10, true, true, null, null, "{}", null
        );

        PromocaoLeveXPagueY estrategia = new PromocaoLeveXPagueY();
        ContextoCalculoPromocao ctx = new ContextoCalculoPromocao(carrinho, campanha, carrinho.calcularSubtotalBruto(), null);

        BigDecimal desconto = estrategia.calcularDesconto(ctx);
        assertEquals(new BigDecimal("100.00"), desconto);
    }

    @Test
    @DisplayName("Modalidade 2 (Percentual): Deve calcular o percentual sobre o subtotal base")
    void deveCalcularDescontoPercentual() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);
        CampanhaPromocional campanha = new CampanhaPromocional(
                UUID.randomUUID(), "15% OFF", "15% desc", TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
                "{\"percentual\": 15.00}", 10, true, true, null, null, "{}", null
        );

        PromocaoPercentualCondicional estrategia = new PromocaoPercentualCondicional();
        ContextoCalculoPromocao ctx = new ContextoCalculoPromocao(carrinho, campanha, new BigDecimal("400.00"), null);

        BigDecimal desconto = estrategia.calcularDesconto(ctx);
        assertEquals(new BigDecimal("60.00"), desconto); // 15% de 400 = 60.00
    }

    @Test
    @DisplayName("Modalidade 3 (Fixo por Meta): Deve aplicar desconto fixo apenas se meta for atingida")
    void deveCalcularDescontoFixoPorMeta() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);
        CampanhaPromocional campanha = new CampanhaPromocional(
                UUID.randomUUID(), "R$ 50 OFF acima de 300", "Meta 300", TipoMecanicaPromocao.VALOR_FIXO_META_CARRINHO,
                "{\"metaMinima\": 300.00, \"valorDesconto\": 50.00}", 10, true, true, null, null, "{}", null
        );

        PromocaoValorFixoMetaCarrinho estrategia = new PromocaoValorFixoMetaCarrinho();

        // Subtotal R$ 250 (abaixo da meta) -> 0.00
        ContextoCalculoPromocao ctxAbaixo = new ContextoCalculoPromocao(carrinho, campanha, new BigDecimal("250.00"), null);
        assertEquals(new BigDecimal("0.00"), estrategia.calcularDesconto(ctxAbaixo));

        // Subtotal R$ 350 (atingiu meta) -> 50.00
        ContextoCalculoPromocao ctxAcima = new ContextoCalculoPromocao(carrinho, campanha, new BigDecimal("350.00"), null);
        assertEquals(new BigDecimal("50.00"), estrategia.calcularDesconto(ctxAcima));
    }

    @Test
    @DisplayName("Modalidade 4 (Combo Cruzado): Deve calcular desconto de 20% sobre pares casados de Teclado e Mouse")
    void deveCalcularComboCruzadoCasado() {
        CarrinhoCompras carrinho = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);
        // 2 Teclados (R$ 300 cada) + 3 Mouses (R$ 150 cada) -> 2 pares casados
        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "DEMO-TEC-PRO-PRETO", "Teclado", new BigDecimal("300.00"), 2, 850, null
        ));
        carrinho.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinho.getId(), "DEMO-MOU-PREC-PRETO", "Mouse", new BigDecimal("150.00"), 3, 120, null
        ));

        CampanhaPromocional campanha = new CampanhaPromocional(
                UUID.randomUUID(), "Combo Teclado + Mouse", "20% no par", TipoMecanicaPromocao.COMBO_CRUZADO_CASADO,
                "{\"skuA\": \"DEMO-TEC-PRO-PRETO\", \"skuB\": \"DEMO-MOU-PREC-PRETO\", \"percentual\": 20.00}",
                10, true, true, null, null, "{}", null
        );

        PromocaoComboCruzadoCasado estrategia = new PromocaoComboCruzadoCasado();
        ContextoCalculoPromocao ctx = new ContextoCalculoPromocao(carrinho, campanha, carrinho.calcularSubtotalBruto(), null);

        // Valor de 1 par = 300 + 150 = 450. Dois pares = 900. 20% de 900 = 180.00
        BigDecimal desconto = estrategia.calcularDesconto(ctx);
        assertEquals(new BigDecimal("180.00"), desconto);
    }

    @Test
    @DisplayName("Dynamic Factory: Deve resolver estratégias dinamicamente e retornar Null Object para tipo nulo")
    void deveResolverEstrategiasPelaFabricaDinamica() {
        List<EstrategiaCalculoPromocao> lista = List.of(
                new PromocaoLeveXPagueY(),
                new PromocaoPercentualCondicional(),
                new PromocaoValorFixoMetaCarrinho(),
                new PromocaoComboCruzadoCasado()
        );

        FabricaDinamicaEstrategiaPromocao fabrica = new FabricaDinamicaEstrategiaPromocao(lista);
        assertEquals(4, fabrica.quantidadeEstrategiasRegistradas());

        assertTrue(fabrica.obterEstrategia(TipoMecanicaPromocao.LEVE_X_PAGUE_Y) instanceof PromocaoLeveXPagueY);
        assertTrue(fabrica.obterEstrategia(TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL) instanceof PromocaoPercentualCondicional);
        assertTrue(fabrica.obterEstrategia(TipoMecanicaPromocao.VALOR_FIXO_META_CARRINHO) instanceof PromocaoValorFixoMetaCarrinho);
        assertTrue(fabrica.obterEstrategia(TipoMecanicaPromocao.COMBO_CRUZADO_CASADO) instanceof PromocaoComboCruzadoCasado);

        // Nulo deve retornar PromocaoNula (Null Object)
        assertTrue(fabrica.obterEstrategia(null) instanceof PromocaoNula);
    }
}
