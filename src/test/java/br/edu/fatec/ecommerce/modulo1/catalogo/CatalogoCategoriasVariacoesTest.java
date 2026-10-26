package br.edu.fatec.ecommerce.modulo1.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.compartilhado.excecao.CicloHierarquicoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoAdminCatalogoReal;
import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoConsultaCatalogo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaRaizNullObject;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.AtributoVariacaoFlyweight;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.FabricaAtributoVariacaoFlyweight;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.TipoAtributoVariacao;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suíte de testes automatizados para a User Story 2: Catálogo de Produtos, Variações Físicas
 * e Categorias Hierárquicas Recursivas (padrões <b>Composite</b>, <b>Null Object</b>,
 * <b>Flyweight</b>, <b>Builder</b>, <b>Factory Method</b> e <b>Static Factory Method</b>).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogoCategoriasVariacoesTest {

    @Autowired
    private ServicoAdminCatalogoReal servicoAdminCatalogoReal;

    @Autowired
    private ServicoConsultaCatalogo servicoConsultaCatalogo;

    @Autowired
    private FabricaAtributoVariacaoFlyweight fabricaFlyweight;

    /**
     * Verifica a navegação recursiva em 3 níveis de categorias ({@code Vestuário > Camisetas > Esportivas}),
     * o comportamento polimórfico de {@link CategoriaRaizNullObject} e o bloqueio de ciclos.
     *
     * <p>Objetivo Funcional: Validar FR-007.</p>
     * <p>Papel no Design Pattern: Exercita o <b>Composite</b> de categorias e o <b>Null Object</b>.</p>
     */
    @Test
    @DisplayName("Deve navegar na árvore recursiva de categorias, usar CategoriaRaizNullObject e bloquear ciclos")
    void deveNavegarNaArvoreDeCategoriasEBloquearCicloHierarquico() {
        CategoriaComp vestuario = new CategoriaComp("Vestuário", "vestuario");
        CategoriaComp camisetas = new CategoriaComp("Camisetas", "camisetas");
        CategoriaComp esportivas = new CategoriaComp("Esportivas", "camisetas-esportivas");

        assertSame(CategoriaRaizNullObject.obterInstancia(), vestuario.obterCategoriaPai());
        assertTrue(vestuario.obterCategoriaPai().eRaizNula());

        vestuario.adicionarSubcategoria(camisetas);
        camisetas.adicionarSubcategoria(esportivas);

        assertFalse(camisetas.obterCategoriaPai().eRaizNula());
        assertEquals("Vestuário > Camisetas > Esportivas", esportivas.obterCaminhoHierarquico());

        Set<UUID> idsSubarvore = vestuario.obterIdsSubarvoreRecursivo();
        assertEquals(3, idsSubarvore.size());
        assertTrue(idsSubarvore.contains(vestuario.obterId()));
        assertTrue(idsSubarvore.contains(camisetas.obterId()));
        assertTrue(idsSubarvore.contains(esportivas.obterId()));

        // Tentativa de adicionar um ancestral ('Vestuário') como subcategoria de sua neta ('Esportivas') -> ciclo!
        assertThrows(CicloHierarquicoException.class, () -> esportivas.adicionarSubcategoria(vestuario));
        assertThrows(CicloHierarquicoException.class, () -> vestuario.adicionarSubcategoria(vestuario));
    }

    /**
     * Comprova que a {@link FabricaAtributoVariacaoFlyweight} compartilha exatamente a mesma
     * referência de objeto em memória ({@code assertSame}) para variações que usam o mesmo
     * par {@code (tipo, valor)}.
     *
     * <p>Objetivo Funcional: Validar FR-006 e SC-003 (reuso de instâncias Flyweight).</p>
     * <p>Papel no Design Pattern: Comprova o funcionamento do pool da <b>FlyweightFactory</b>.</p>
     */
    @Test
    @DisplayName("Deve reutilizar a mesma instância em memória (assertSame) na FabricaAtributoVariacaoFlyweight")
    void deveCompartilharMesmaReferenciaEmMemoriaNoFlyweight() {
        this.fabricaFlyweight.limparPoolEmMemoria();

        AtributoVariacaoFlyweight corPreta1 = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.COR,
            "Preto",
            "#000000"
        );
        AtributoVariacaoFlyweight corPreta2 = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.COR,
            "preto",
            "#000000"
        );
        AtributoVariacaoFlyweight tamM1 = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.TAMANHO,
            "M",
            "TAM-M"
        );
        AtributoVariacaoFlyweight tamM2 = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.TAMANHO,
            "m",
            "TAM-M"
        );

        assertSame(corPreta1, corPreta2);
        assertSame(tamM1, tamM2);
        assertEquals(2, this.fabricaFlyweight.obterTotalInstanciasNoPool());
    }

    /**
     * Verifica o cadastro de produtos em subcategorias e a busca recursiva a partir da categoria pai.
     *
     * <p>Objetivo Funcional: Validar FR-005, FR-006 e FR-007 integrados no banco H2.</p>
     * <p>Papel no Design Pattern: Integra <b>Composite</b>, <b>Flyweight</b>, <b>Builder</b>,
     * <b>Factory Method</b> e <b>Static Factory Method</b> ({@link DimensoesFisicas}).</p>
     */
    @Test
    @DisplayName("Deve cadastrar produto via ProdutoBuilder e retornar itens das subcategorias ao filtrar pela categoria pai")
    void deveCadastrarProdutoERetornarItensRecursivamentePelaCategoriaPai() {
        CategoriaComp eletronicos = this.servicoAdminCatalogoReal.criarCategoria("Eletrônicos", "eletronicos", null);
        CategoriaComp perifericos = this.servicoAdminCatalogoReal.criarCategoria(
            "Periféricos",
            "perifericos",
            eletronicos.obterId()
        );
        CategoriaComp teclados = this.servicoAdminCatalogoReal.criarCategoria(
            "Teclados Mecânicos",
            "teclados-mecanicos",
            perifericos.obterId()
        );

        AtributoVariacaoFlyweight switchRed = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.ESPECIFICACAO,
            "Switch Red",
            "SW-RED"
        );
        AtributoVariacaoFlyweight corPreta = this.fabricaFlyweight.obterOuCriar(
            TipoAtributoVariacao.COR,
            "Preto",
            "#000000"
        );

        DimensoesFisicas dimTeclado = DimensoesFisicas.emCentimetrosEGramas(
            850,
            new BigDecimal("4.50"),
            new BigDecimal("44.00"),
            new BigDecimal("14.00")
        );

        ProdutoBuilder builder = new ProdutoBuilder()
            .comNome("Teclado Mecânico Pro")
            .comDescricao("Teclado mecânico ABNT2 com iluminação RGB")
            .comMarca("FatecGear")
            .comPrecoBase(new BigDecimal("350.00"))
            .naCategoria(teclados.obterId())
            .adicionarAtributoEspecifico("Layout", "ABNT2")
            .adicionarVariacao(
                "TEC-PRO-RED-PT",
                "7890000111111",
                "Teclado Mecânico Pro - Preto - Switch Red",
                new BigDecimal("20.00"),
                dimTeclado,
                List.of(switchRed, corPreta)
            );

        Produto produtoSalvo = this.servicoAdminCatalogoReal.cadastrarProdutoComVariacoes(
            builder,
            Map.of("TEC-PRO-RED-PT", 15)
        );
        assertNotNull(produtoSalvo.obterId());

        // Busca pela categoria avó ("Eletrônicos") deve retornar o item cadastrado na categoria neta ("Teclados Mecânicos")
        List<ItemCatalogoComponent> itensNaSubarvore =
            this.servicoConsultaCatalogo.listarItensComercializaveisAtivos(eletronicos.obterId());
        assertEquals(1, itensNaSubarvore.size());

        ItemCatalogoComponent itemEncontrado = itensNaSubarvore.get(0);
        assertEquals("TEC-PRO-RED-PT", itemEncontrado.obterSku());
        assertEquals(new BigDecimal("370.00"), itemEncontrado.calcularPrecoVigente());
        assertEquals(850, itemEncontrado.obterPesoTotalGramas());
        assertEquals(15, this.servicoConsultaCatalogo.calcularDisponibilidadeEstoque(itemEncontrado));
    }

    /**
     * Verifica que {@link DimensoesFisicas} e {@link ProdutoBuilder} rejeitam preços ou medidas zeradas/negativas.
     *
     * <p>Objetivo Funcional: Garantir proteção contra dados físicos ou financeiros inválidos.</p>
     * <p>Papel no Design Pattern: Validações do <b>Static Factory Method</b> e do <b>Builder</b>.</p>
     */
    @Test
    @DisplayName("Deve rejeitar peso ou dimensão menor ou igual a zero e produto sem variação")
    void deveRejeitarDimensoesInvalidasOuProdutoSemVariacao() {
        assertThrows(RegraNegocioModulo1Exception.class, () ->
            DimensoesFisicas.emCentimetrosEGramas(0, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)
        );

        assertThrows(RegraNegocioModulo1Exception.class, () ->
            new ProdutoBuilder()
                .comNome("Produto Incompleto")
                .comDescricao("Sem variações")
                .comMarca("Marca X")
                .comPrecoBase(new BigDecimal("100.00"))
                .naCategoria(UUID.randomUUID())
                .construir()
        );
    }
}

