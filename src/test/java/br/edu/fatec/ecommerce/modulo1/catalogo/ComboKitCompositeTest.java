package br.edu.fatec.ecommerce.modulo1.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.compartilhado.excecao.CicloHierarquicoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.EstoqueInsuficienteException;
import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoAdminCatalogoReal;
import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoConsultaCatalogo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitBuilder;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ModoPrecoCombo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import br.edu.fatec.ecommerce.modulo1.estoque.aplicacao.ServicoReservaEstoqueConcorrente;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.ItemEstoque;
import br.edu.fatec.ecommerce.modulo1.estoque.infraestrutura.ItemEstoqueJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suíte de testes automatizados para a User Story 3: Composição Recursiva de Produtos em Combos
 * e Kits Comerciais (padrões <b>Composite</b>, <b>Builder</b> e <b>Factory Method</b>).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ComboKitCompositeTest {

    @Autowired
    private ServicoAdminCatalogoReal servicoAdminCatalogoReal;

    @Autowired
    private ServicoConsultaCatalogo servicoConsultaCatalogo;

    @Autowired
    private ServicoReservaEstoqueConcorrente servicoReservaEstoque;

    @Autowired
    private ItemEstoqueJpaRepository estoqueRepository;

    /**
     * Valida o cálculo recursivo de preço, peso total, dimensões e disponibilidade pelo item gargalo
     * ($\min(\lfloor \text{disponivel}_i / \text{qtdNoKit}_i \rfloor)$), além da reserva atômica tudo-ou-nada.
     *
     * <p>Objetivo Funcional: Validar FR-008 e FR-010 para Combos/Kits.</p>
     * <p>Papel no Design Pattern: Exercita o <b>Composite</b> ({@link ComboKitComposite}) tratando
     * folhas e kits de maneira uniforme.</p>
     */
    @Test
    @DisplayName("Deve calcular preço, peso e disponibilidade pelo item gargalo e reservar componentes atomicamente")
    void deveCalcularPrecoPesoDisponibilidadePeloGargaloEReservarAtomicamente() {
        CategoriaComp catInformatica = this.servicoAdminCatalogoReal.criarCategoria(
            "Informática",
            "informatica",
            null
        );

        // Produto A: Teclado (R$ 200,00 | 800g | Estoque = 10)
        ProdutoBuilder builderTeclado = new ProdutoBuilder()
            .comNome("Teclado Gamer")
            .comDescricao("Teclado mecânico")
            .comMarca("HyperFatec")
            .comPrecoBase(new BigDecimal("200.00"))
            .naCategoria(catInformatica.obterId())
            .adicionarVariacao(
                "SKU-TECLADO-A",
                null,
                "Teclado Gamer Padrão",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(
                    800,
                    new BigDecimal("4.00"),
                    new BigDecimal("45.00"),
                    new BigDecimal("15.00")
                ),
                List.of()
            );
        this.servicoAdminCatalogoReal.cadastrarProdutoComVariacoes(
            builderTeclado,
            Map.of("SKU-TECLADO-A", 10)
        );

        // Produto B: Mouse (R$ 100,00 | 150g | Estoque = 5)
        ProdutoBuilder builderMouse = new ProdutoBuilder()
            .comNome("Mouse Óptico")
            .comDescricao("Mouse 16000 DPI")
            .comMarca("HyperFatec")
            .comPrecoBase(new BigDecimal("100.00"))
            .naCategoria(catInformatica.obterId())
            .adicionarVariacao(
                "SKU-MOUSE-B",
                null,
                "Mouse Óptico Padrão",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(
                    150,
                    new BigDecimal("4.00"),
                    new BigDecimal("12.00"),
                    new BigDecimal("6.00")
                ),
                List.of()
            );
        this.servicoAdminCatalogoReal.cadastrarProdutoComVariacoes(
            builderMouse,
            Map.of("SKU-MOUSE-B", 5)
        );

        // Cria Combo exigindo 1 Teclado (estoque 10) + 2 Mouses (estoque 5) com 10% de desconto na soma recursiva
        // Preço bruto = 1*200 + 2*100 = 400.00 -> com 10% de desconto = R$ 360.00
        // Peso total = 1*800g + 2*150g = 1100g
        // Disponibilidade máxima = min(10/1, floor(5/2)) = min(10, 2) = 2 kits!
        ComboKitComposite combo = this.servicoAdminCatalogoReal.cadastrarComboKitPromocional(
            "KIT-DUPLO-GAMER",
            "Kit 1 Teclado + 2 Mouses",
            "Kit promocional para estação dupla",
            catInformatica.obterId(),
            ModoPrecoCombo.SOMA_RECURSIVA_COMPONENTES,
            new BigDecimal("10.00"),
            null,
            Map.of("SKU-TECLADO-A", 1, "SKU-MOUSE-B", 2)
        );

        assertTrue(combo.eComboKit());
        assertEquals(new BigDecimal("360.00"), combo.calcularPrecoVigente());
        assertEquals(1100, combo.obterPesoTotalGramas());
        assertEquals(2, this.servicoConsultaCatalogo.calcularDisponibilidadeEstoque(combo));

        // Tentar reservar 3 kits deve falhar integralmente (tudo-ou-nada) pois só há 5 mouses (precisaria de 6)
        assertThrows(EstoqueInsuficienteException.class, () ->
            this.servicoReservaEstoque.reservarItensAtomicamente(
                "PEDIDO-COMBO-EXCESSO",
                UUID.randomUUID(),
                Map.of("KIT-DUPLO-GAMER", 3)
            )
        );

        // Reservar 2 kits deve funcionar e baixar 2 Teclados (sobra 8) e 4 Mouses (sobra 1)
        this.servicoReservaEstoque.reservarItensAtomicamente(
            "PEDIDO-COMBO-VALIDO",
            UUID.randomUUID(),
            Map.of("KIT-DUPLO-GAMER", 2)
        );

        ItemEstoque estTeclado = this.estoqueRepository.findBySkuIgnoreCase("SKU-TECLADO-A").orElseThrow();
        ItemEstoque estMouse = this.estoqueRepository.findBySkuIgnoreCase("SKU-MOUSE-B").orElseThrow();
        assertEquals(8, estTeclado.obterQuantidadeDisponivel());
        assertEquals(2, estTeclado.obterQuantidadeReservada());
        assertEquals(1, estMouse.obterQuantidadeDisponivel());
        assertEquals(4, estMouse.obterQuantidadeReservada());
        assertEquals(0, this.servicoConsultaCatalogo.calcularDisponibilidadeEstoque(combo));
    }

    /**
     * Valida sub-combos aninhados (Combo dentro de Combo), modo {@code PRECO_FIXO_DEDICADO} e
     * bloqueio de ciclos circulares entre combos.
     *
     * <p>Objetivo Funcional: Comprovar recursividade real em múltiplos níveis e detecção de ciclo (FR-008).</p>
     * <p>Papel no Design Pattern: Valida invariantes estruturais do <b>Composite</b> e do <b>Builder</b>.</p>
     */
    @Test
    @DisplayName("Deve suportar combos aninhados com preço fixo dedicado e impedir ciclos circulares")
    void deveSuportarCombosAninhadosEBloquearCicloCircular() {
        CategoriaComp cat = this.servicoAdminCatalogoReal.criarCategoria("Escritório", "escritorio", null);

        ProdutoBuilder prodMonitor = new ProdutoBuilder()
            .comNome("Monitor 27")
            .comDescricao("Monitor IPS")
            .comMarca("FatecView")
            .comPrecoBase(new BigDecimal("1000.00"))
            .naCategoria(cat.obterId())
            .adicionarVariacao(
                "MON-27-IPS",
                null,
                "Monitor 27 Polegadas",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(
                    4000,
                    new BigDecimal("40.00"),
                    new BigDecimal("60.00"),
                    new BigDecimal("15.00")
                ),
                List.of()
            );
        this.servicoAdminCatalogoReal.cadastrarProdutoComVariacoes(prodMonitor, Map.of("MON-27-IPS", 6));

        ProdutoBuilder prodSuporte = new ProdutoBuilder()
            .comNome("Suporte Articulado")
            .comDescricao("Suporte de mesa")
            .comMarca("FatecView")
            .comPrecoBase(new BigDecimal("200.00"))
            .naCategoria(cat.obterId())
            .adicionarVariacao(
                "SUP-ART-01",
                null,
                "Suporte Pistão a Gás",
                BigDecimal.ZERO,
                DimensoesFisicas.emCentimetrosEGramas(
                    1500,
                    new BigDecimal("10.00"),
                    new BigDecimal("30.00"),
                    new BigDecimal("10.00")
                ),
                List.of()
            );
        this.servicoAdminCatalogoReal.cadastrarProdutoComVariacoes(prodSuporte, Map.of("SUP-ART-01", 6));

        // Sub-combo 1: 1 Monitor + 1 Suporte com preço fixo dedicado de R$ 1099,00
        ComboKitComposite kitEstacaoSimples = this.servicoAdminCatalogoReal.cadastrarComboKitPromocional(
            "KIT-ESTACAO-SIMPLES",
            "Kit Monitor + Suporte",
            "Estação individual",
            cat.obterId(),
            ModoPrecoCombo.PRECO_FIXO_DEDICADO,
            BigDecimal.ZERO,
            new BigDecimal("1099.00"),
            Map.of("MON-27-IPS", 1, "SUP-ART-01", 1)
        );
        assertEquals(new BigDecimal("1099.00"), kitEstacaoSimples.calcularPrecoVigente());
        assertEquals(5500, kitEstacaoSimples.obterPesoTotalGramas());

        // Super-combo aninhado: contém 2 unidades do "KIT-ESTACAO-SIMPLES"
        ComboKitComposite kitEstacaoDupla = this.servicoAdminCatalogoReal.cadastrarComboKitPromocional(
            "KIT-ESTACAO-DUPLA",
            "Kit Estação Dupla (2 Kits Simples)",
            "Duas estações completas",
            cat.obterId(),
            ModoPrecoCombo.SOMA_RECURSIVA_COMPONENTES,
            new BigDecimal("5.00"),
            null,
            Map.of("KIT-ESTACAO-SIMPLES", 2)
        );

        // Peso do super-combo = 2 * 5500g = 11000g
        assertEquals(11000, kitEstacaoDupla.obterPesoTotalGramas());
        // Disponibilidade do super-combo = floor(6 / 2) = 3
        assertEquals(3, this.servicoConsultaCatalogo.calcularDisponibilidadeEstoque(kitEstacaoDupla));

        // Tentar adicionar o super-combo dentro do sub-combo deve lançar CicloHierarquicoException
        assertThrows(CicloHierarquicoException.class, () ->
            kitEstacaoSimples.adicionarComponente(kitEstacaoDupla, 1)
        );

        // Tentar criar um combo contendo a si mesmo deve lançar CicloHierarquicoException
        assertThrows(CicloHierarquicoException.class, () ->
            new ComboKitBuilder()
                .comSku("KIT-ESTACAO-SIMPLES")
                .comNome("Auto Ciclo")
                .comDescricao("Teste ciclo")
                .naCategoria(cat.obterId())
                .adicionarItemComponente(kitEstacaoSimples, 2)
                .construir()
        );
    }
}

