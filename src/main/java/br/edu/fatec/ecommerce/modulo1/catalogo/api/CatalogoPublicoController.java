package br.edu.fatec.ecommerce.modulo1.catalogo.api;

import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoConsultaCatalogo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoVariacaoLeaf;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST público para navegação na árvore recursiva de categorias e consulta
 * uniforme de itens do catálogo (variações unitárias e combos/kits) (FR-005, FR-006, FR-007, FR-008).
 */
@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoPublicoController {

    private final ServicoConsultaCatalogo servicoConsultaCatalogo;

    /**
     * Construtor com injeção explícita do serviço de consulta do catálogo.
     *
     * <p>Objetivo Funcional: Expor consultas públicas de vitrine sem necessidade de autenticação.</p>
     * <p>Papel no Design Pattern: Controller REST cliente dos padrões <b>Composite</b>,
     * <b>Null Object</b> e <b>Flyweight</b>.</p>
     *
     * @param servicoConsultaCatalogo serviço de leitura do catálogo
     */
    public CatalogoPublicoController(ServicoConsultaCatalogo servicoConsultaCatalogo) {
        this.servicoConsultaCatalogo = servicoConsultaCatalogo;
    }

    /**
     * Retorna a árvore hierárquica completa de categorias e subcategorias do catálogo (FR-007).
     *
     * <p>Objetivo Funcional: Exibir menu multinível de categorias com caminhos hierárquicos.</p>
     * <p>Papel no Design Pattern: Serializa recursivamente os nós {@link CategoriaComp} (<b>Composite</b>).</p>
     *
     * @return resposta HTTP 200 OK com lista de categorias raiz e suas subárvores
     */
    @GetMapping("/categorias/arvore")
    public ResponseEntity<List<Map<String, Object>>> listarArvoreCategorias() {
        List<CategoriaComp> raizes = this.servicoConsultaCatalogo.listarArvoreCategorias();
        List<Map<String, Object>> resposta = raizes.stream()
            .map(this::mapearCategoriaRecursivamente)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resposta);
    }

    /**
     * Lista os itens comercializáveis ativos (variações e combos), permitindo filtro recursivo por categoria pai.
     *
     * <p>Objetivo Funcional: Listar produtos simples e combos da categoria selecionada e de todas as
     * suas subcategorias descendentes (FR-007, FR-008).</p>
     * <p>Papel no Design Pattern: Trata uniformemente {@link ProdutoVariacaoLeaf} e
     * {@link ComboKitComposite} via interface {@link ItemCatalogoComponent} (<b>Composite</b>).</p>
     *
     * @param categoriaId UUID opcional da categoria para filtro recursivo
     * @return resposta HTTP 200 OK com lista de itens comercializáveis
     */
    @GetMapping("/itens")
    public ResponseEntity<List<Map<String, Object>>> listarItensComercializaveis(
        @RequestParam(name = "categoriaId", required = false) UUID categoriaId
    ) {
        List<ItemCatalogoComponent> itens =
            this.servicoConsultaCatalogo.listarItensComercializaveisAtivos(categoriaId);
        List<Map<String, Object>> resposta = itens.stream()
            .map(this::mapearItemCatalogoUniforme)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resposta);
    }

    /**
     * Consulta os detalhes completos de um item comercializável (variação ou combo) pelo seu SKU.
     *
     * <p>Objetivo Funcional: Retornar preço vigente, peso total, dimensões físicas, disponibilidade
     * máxima em estoque e atributos Flyweight ou componentes do kit.</p>
     * <p>Papel no Design Pattern: Consulta uniforme de {@link ItemCatalogoComponent} (<b>Composite</b>).</p>
     *
     * @param sku código SKU do item ou combo
     * @return resposta HTTP 200 OK com detalhes do item ou 404 se não encontrado
     */
    @GetMapping("/itens/{sku}")
    public ResponseEntity<Map<String, Object>> consultarItemPorSku(@PathVariable String sku) {
        ItemCatalogoComponent item = this.servicoConsultaCatalogo.buscarItemComercializavelPorSku(sku);
        return ResponseEntity.ok(mapearItemCatalogoUniforme(item));
    }

    /**
     * Converte recursivamente uma {@link CategoriaComp} e suas subcategorias em estrutura JSON.
     *
     * <p>Objetivo Funcional: Representar toda a subárvore da categoria com seu caminho hierárquico.</p>
     * <p>Papel no Design Pattern: Travessia recursiva sobre o padrão <b>Composite</b>.</p>
     *
     * @param categoria categoria a serializar
     * @return mapa JSON da categoria e suas subcategorias
     */
    private Map<String, Object> mapearCategoriaRecursivamente(CategoriaComp categoria) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", categoria.obterId());
        mapa.put("nome", categoria.obterNome());
        mapa.put("slug", categoria.obterSlug());
        mapa.put("caminhoHierarquico", categoria.obterCaminhoHierarquico());
        mapa.put("eRaiz", categoria.obterCategoriaPai().eRaizNula());
        mapa.put("ativa", categoria.estaAtiva());
        mapa.put("idsSubarvore", categoria.obterIdsSubarvoreRecursivo());
        mapa.put("subcategorias", categoria.obterSubcategorias().stream()
            .map(this::mapearCategoriaRecursivamente)
            .collect(Collectors.toList()));
        return mapa;
    }

    /**
     * Converte polimorficamente qualquer {@link ItemCatalogoComponent} em mapa JSON padronizado.
     *
     * <p>Objetivo Funcional: Expor preço vigente, peso, dimensões e disponibilidade de forma idêntica
     * para folhas e combos.</p>
     * <p>Papel no Design Pattern: Serialização uniforme do <i>Component</i> no padrão <b>Composite</b>.</p>
     *
     * @param item instância de {@link ItemCatalogoComponent}
     * @return mapa JSON detalhado do item
     */
    private Map<String, Object> mapearItemCatalogoUniforme(ItemCatalogoComponent item) {
        DimensoesFisicas dim = item.obterDimensoesFisicas();
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", item.obterId());
        mapa.put("sku", item.obterSku());
        mapa.put("nomeExibicao", item.obterNomeExibicao());
        mapa.put("eComboKit", item.eComboKit());
        mapa.put("ativo", item.estaAtivo());
        mapa.put("precoVigente", item.calcularPrecoVigente());
        mapa.put("pesoTotalGramas", item.obterPesoTotalGramas());
        mapa.put("dimensoesFisicas", Map.of(
            "pesoGramas", dim.obterPesoGramas(),
            "alturaCm", dim.obterAlturaCm(),
            "larguraCm", dim.obterLarguraCm(),
            "profundidadeCm", dim.obterProfundidadeCm(),
            "volumeCubicoCm3", dim.calcularVolumeCubicoCm3()
        ));
        mapa.put("categoriaPrincipalId", item.obterCategoriaPrincipalId());
        mapa.put("disponibilidadeMaximaEstoque", this.servicoConsultaCatalogo.calcularDisponibilidadeEstoque(item));

        if (item instanceof ProdutoVariacaoLeaf variacao) {
            mapa.put("atributosFlyweight", variacao.obterAtributosFlyweight().stream()
                .map(attr -> Map.of(
                    "id", attr.obterId(),
                    "tipo", attr.obterTipo(),
                    "valor", attr.obterValor(),
                    "codigoApresentacao", attr.obterCodigoApresentacao()
                ))
                .collect(Collectors.toList()));
        } else if (item instanceof ComboKitComposite combo) {
            mapa.put("modoPreco", combo.obterModoPreco());
            mapa.put("percentualDescontoKit", combo.obterPercentualDescontoKit());
            mapa.put("skusFisicosDecompostos", combo.expandirSkusFisicosComMultiplicador());
        }
        return mapa;
    }
}

