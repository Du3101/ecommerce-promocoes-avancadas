package br.edu.fatec.ecommerce.modulo1.catalogo.api;

import br.edu.fatec.ecommerce.modulo1.catalogo.aplicacao.ServicoAdminCatalogoPort;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.categoria.CategoriaComp;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.AtributoVariacaoFlyweight;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.FabricaAtributoVariacaoFlyweight;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.TipoAtributoVariacao;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ComboKitComposite;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.DimensoesFisicas;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ItemCatalogoComponent;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ModoPrecoCombo;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.Produto;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item.ProdutoBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST administrativo para criação e manutenção de Categorias, Produtos, Variações,
 * Preços, Status e Combos/Kits Promocionais (FR-005 a FR-008, FR-012).
 *
 * <p>Delega todas as operações para a porta {@link ServicoAdminCatalogoPort}, sendo interceptado
 * transparentemente pelo {@code CatalogoAdminAuditoriaProxy} (padrão <b>Proxy</b>).</p>
 */
@RestController
@RequestMapping("/api/v1/admin/catalogo")
public class CatalogoAdminController {

    private final ServicoAdminCatalogoPort servicoAdminCatalogo;
    private final FabricaAtributoVariacaoFlyweight fabricaFlyweight;

    /**
     * Construtor com injeção da porta administrativa (Proxy) e da fábrica Flyweight.
     *
     * <p>Objetivo Funcional: Expor endpoints administrativos protegidos por RBAC e auditados.</p>
     * <p>Papel no Design Pattern: Cliente do padrão <b>Proxy</b> ({@link ServicoAdminCatalogoPort})
     * e da <b>FlyweightFactory</b> ({@link FabricaAtributoVariacaoFlyweight}).</p>
     *
     * @param servicoAdminCatalogo porta administrativa do catálogo (injetada via {@code @Primary} Proxy)
     * @param fabricaFlyweight fábrica de atributos intrínsecos compartilhados
     */
    public CatalogoAdminController(
        ServicoAdminCatalogoPort servicoAdminCatalogo,
        FabricaAtributoVariacaoFlyweight fabricaFlyweight
    ) {
        this.servicoAdminCatalogo = servicoAdminCatalogo;
        this.fabricaFlyweight = fabricaFlyweight;
    }

    /**
     * Cria uma nova categoria raiz ou subcategoria filha na árvore do catálogo (FR-007).
     *
     * <p>Objetivo Funcional: Permitir estruturação da árvore mercadológica por {@code ADMIN} ou
     * {@code OPERADOR_COMERCIAL}.</p>
     * <p>Papel no Design Pattern: Invoca o <b>Proxy</b> de auditoria e o <b>Composite</b> de categorias.</p>
     *
     * @param requisicao payload com nome, slug opcional e ID da categoria pai opcional
     * @return resposta HTTP 201 Created com os dados da categoria criada
     */
    @PostMapping("/categorias")
    public ResponseEntity<Map<String, Object>> criarCategoria(
        @Valid @RequestBody RequisicaoCriarCategoriaDTO requisicao
    ) {
        CategoriaComp categoria = this.servicoAdminCatalogo.criarCategoria(
            requisicao.nome(),
            requisicao.slug(),
            requisicao.categoriaPaiId()
        );

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id", categoria.obterId());
        resposta.put("nome", categoria.obterNome());
        resposta.put("slug", categoria.obterSlug());
        resposta.put("caminhoHierarquico", categoria.obterCaminhoHierarquico());
        resposta.put("eRaiz", categoria.obterCategoriaPai().eRaizNula());
        resposta.put("ativa", categoria.estaAtiva());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /**
     * Cadastra um novo produto com suas variações físicas reutilizando atributos {@code Flyweight} (FR-005, FR-006).
     *
     * <p>Objetivo Funcional: Construir o produto via {@link ProdutoBuilder}, compartilhar atributos
     * via {@link FabricaAtributoVariacaoFlyweight} e inicializar saldos de estoque.</p>
     * <p>Papel no Design Pattern: Orquestra <b>Builder</b>, <b>Flyweight</b>, <b>Static Factory Method</b>
     * ({@link DimensoesFisicas}), <b>Factory Method</b> e <b>Proxy</b>.</p>
     *
     * @param requisicao payload com dados do produto e lista de variações
     * @return resposta HTTP 201 Created com dados do produto e SKUs criados
     */
    @PostMapping("/produtos")
    public ResponseEntity<Map<String, Object>> cadastrarProduto(
        @Valid @RequestBody RequisicaoCadastrarProdutoDTO requisicao
    ) {
        ProdutoBuilder builder = new ProdutoBuilder()
            .comNome(requisicao.nome())
            .comDescricao(requisicao.descricao())
            .comMarca(requisicao.marca())
            .comPrecoBase(requisicao.precoBase())
            .naCategoria(requisicao.categoriaId())
            .comAtributosEspecificos(requisicao.atributosEspecificos());

        Map<String, Integer> estoqueInicialPorSku = new LinkedHashMap<>();

        for (VariacaoEntradaDTO varDto : requisicao.variacoes()) {
            DimensoesFisicas dimensoes = DimensoesFisicas.emCentimetrosEGramas(
                varDto.pesoGramas(),
                varDto.alturaCm(),
                varDto.larguraCm(),
                varDto.profundidadeCm()
            );

            List<AtributoVariacaoFlyweight> atributosCompartilhados = new ArrayList<>();
            if (varDto.atributos() != null) {
                for (AtributoFlyweightEntradaDTO attrDto : varDto.atributos()) {
                    atributosCompartilhados.add(
                        this.fabricaFlyweight.obterOuCriar(
                            attrDto.tipo(),
                            attrDto.valor(),
                            attrDto.codigoApresentacao()
                        )
                    );
                }
            }

            builder.adicionarVariacao(
                varDto.sku(),
                varDto.codigoBarras(),
                varDto.nomeVariacao(),
                varDto.adicionalPreco(),
                dimensoes,
                atributosCompartilhados
            );

            if (varDto.estoqueInicial() != null && varDto.estoqueInicial() > 0) {
                estoqueInicialPorSku.put(varDto.sku().trim().toUpperCase(), varDto.estoqueInicial());
            }
        }

        Produto produtoSalvo = this.servicoAdminCatalogo.cadastrarProdutoComVariacoes(
            builder,
            estoqueInicialPorSku
        );

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("produtoId", produtoSalvo.obterId());
        resposta.put("nome", produtoSalvo.obterNome());
        resposta.put("marca", produtoSalvo.obterMarca());
        resposta.put("precoBase", produtoSalvo.obterPrecoBase());
        resposta.put("categoriaId", produtoSalvo.obterCategoriaPrincipalId());
        resposta.put("skusCriados", produtoSalvo.obterVariacoes().stream()
            .map(v -> v.obterSku())
            .collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /**
     * Cadastra um novo Combo/Kit promocional composto recursivamente por itens existentes (FR-008).
     *
     * <p>Objetivo Funcional: Criar pacote promocional com modo recursivo ou preço fixo dedicado.</p>
     * <p>Papel no Design Pattern: Aciona <b>Composite</b>, <b>Builder</b>, <b>Factory Method</b> e <b>Proxy</b>.</p>
     *
     * @param requisicao payload com dados do combo e lista de componentes
     * @return resposta HTTP 201 Created com dados do combo criado
     */
    @PostMapping("/combos")
    public ResponseEntity<Map<String, Object>> cadastrarComboKit(
        @Valid @RequestBody RequisicaoCadastrarComboDTO requisicao
    ) {
        Map<String, Integer> componentesMapa = new LinkedHashMap<>();
        for (ComponenteComboEntradaDTO comp : requisicao.componentes()) {
            componentesMapa.put(comp.componenteSku(), comp.quantidadePorKit());
        }

        ComboKitComposite combo = this.servicoAdminCatalogo.cadastrarComboKitPromocional(
            requisicao.skuCombo(),
            requisicao.nomeCombo(),
            requisicao.descricao(),
            requisicao.categoriaId(),
            requisicao.modoPreco(),
            requisicao.percentualDescontoKit(),
            requisicao.precoFixoOpcional(),
            componentesMapa
        );

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("comboId", combo.obterId());
        resposta.put("skuCombo", combo.obterSku());
        resposta.put("nomeCombo", combo.obterNomeExibicao());
        resposta.put("modoPreco", combo.obterModoPreco());
        resposta.put("precoVigente", combo.calcularPrecoVigente());
        resposta.put("pesoTotalGramas", combo.obterPesoTotalGramas());
        resposta.put("skusFisicosDecompostos", combo.expandirSkusFisicosComMultiplicador());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    /**
     * Altera o preço vigente de um item unitário ou o parâmetro de preço de um combo (FR-005, FR-008, FR-012).
     *
     * <p>Objetivo Funcional: Reajustar preço gerando registro automático na trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Operação interceptada pelo {@code CatalogoAdminAuditoriaProxy} (<b>Proxy</b>).</p>
     *
     * @param sku código SKU da variação ou combo
     * @param requisicao payload com o novo valor
     * @return resposta HTTP 200 OK com o novo preço vigente
     */
    @PatchMapping("/itens/{sku}/preco")
    public ResponseEntity<Map<String, Object>> alterarPrecoItem(
        @PathVariable String sku,
        @Valid @RequestBody RequisicaoAlterarPrecoDTO requisicao
    ) {
        ItemCatalogoComponent atualizado = this.servicoAdminCatalogo.alterarPrecoItemCatalogo(
            sku,
            requisicao.novoValor()
        );
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("sku", atualizado.obterSku());
        resposta.put("nomeExibicao", atualizado.obterNomeExibicao());
        resposta.put("precoVigenteAtualizado", atualizado.calcularPrecoVigente());
        return ResponseEntity.ok(resposta);
    }

    /**
     * Ativa ou inativa logicamente um item ou combo do catálogo pelo SKU (FR-005, FR-012).
     *
     * <p>Objetivo Funcional: Controlar disponibilidade comercial registrando trilha de auditoria.</p>
     * <p>Papel no Design Pattern: Operação interceptada pelo {@code CatalogoAdminAuditoriaProxy} (<b>Proxy</b>).</p>
     *
     * @param sku código SKU da variação ou combo
     * @param requisicao payload com flag {@code ativo}
     * @return resposta HTTP 200 OK com o novo status
     */
    @PatchMapping("/itens/{sku}/status")
    public ResponseEntity<Map<String, Object>> alterarStatusItem(
        @PathVariable String sku,
        @Valid @RequestBody RequisicaoAlterarStatusDTO requisicao
    ) {
        ItemCatalogoComponent atualizado = this.servicoAdminCatalogo.alterarStatusAtivacaoItemCatalogo(
            sku,
            requisicao.ativo()
        );
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("sku", atualizado.obterSku());
        resposta.put("ativo", atualizado.estaAtivo());
        return ResponseEntity.ok(resposta);
    }

    /**
     * DTO de requisição para criação de categoria ou subcategoria.
     *
     * @param nome nome da categoria
     * @param slug slug opcional
     * @param categoriaPaiId UUID opcional da categoria pai
     */
    public record RequisicaoCriarCategoriaDTO(
        @NotBlank(message = "O nome da categoria é obrigatório.") String nome,
        String slug,
        UUID categoriaPaiId
    ) {
    }

    /**
     * DTO de requisição para cadastro completo de produto e variações.
     *
     * @param nome nome do produto
     * @param descricao descrição detalhada
     * @param marca marca
     * @param precoBase preço base
     * @param categoriaId UUID da categoria principal
     * @param atributosEspecificos mapa opcional de especificações técnicas
     * @param variacoes lista de variações comercializáveis
     */
    public record RequisicaoCadastrarProdutoDTO(
        @NotBlank(message = "O nome do produto é obrigatório.") String nome,
        @NotBlank(message = "A descrição é obrigatória.") String descricao,
        @NotBlank(message = "A marca é obrigatória.") String marca,
        @NotNull(message = "O preço base é obrigatório.") BigDecimal precoBase,
        @NotNull(message = "A categoria é obrigatória.") UUID categoriaId,
        Map<String, String> atributosEspecificos,
        @NotEmpty(message = "O produto deve possuir ao menos uma variação.") List<VariacaoEntradaDTO> variacoes
    ) {
    }

    /**
     * DTO de entrada para uma variação física dentro do cadastro de produto.
     *
     * @param sku código SKU único
     * @param codigoBarras código EAN opcional
     * @param nomeVariacao nome de exibição da variação
     * @param adicionalPreco diferencial de preço
     * @param pesoGramas peso em gramas
     * @param alturaCm altura em cm
     * @param larguraCm largura em cm
     * @param profundidadeCm profundidade em cm
     * @param estoqueInicial saldo inicial opcional de estoque
     * @param atributos atributos Flyweight da variação
     */
    public record VariacaoEntradaDTO(
        @NotBlank(message = "O SKU é obrigatório.") String sku,
        String codigoBarras,
        @NotBlank(message = "O nome da variação é obrigatório.") String nomeVariacao,
        BigDecimal adicionalPreco,
        int pesoGramas,
        @NotNull BigDecimal alturaCm,
        @NotNull BigDecimal larguraCm,
        @NotNull BigDecimal profundidadeCm,
        Integer estoqueInicial,
        List<AtributoFlyweightEntradaDTO> atributos
    ) {
    }

    /**
     * DTO de entrada para um atributo intrínseco Flyweight de variação.
     *
     * @param tipo tipo do atributo
     * @param valor valor nominal
     * @param codigoApresentacao código visual opcional
     */
    public record AtributoFlyweightEntradaDTO(
        @NotNull TipoAtributoVariacao tipo,
        @NotBlank String valor,
        String codigoApresentacao
    ) {
    }

    /**
     * DTO de requisição para criação de Combo/Kit promocional.
     *
     * @param skuCombo código SKU do combo
     * @param nomeCombo nome comercial do combo
     * @param descricao descrição textual
     * @param categoriaId UUID da categoria
     * @param modoPreco modo de precificação
     * @param percentualDescontoKit percentual de desconto (para soma recursiva)
     * @param precoFixoOpcional preço fixo (para modo fixo dedicado)
     * @param componentes lista de componentes filhos e quantidades
     */
    public record RequisicaoCadastrarComboDTO(
        @NotBlank(message = "O SKU do combo é obrigatório.") String skuCombo,
        @NotBlank(message = "O nome do combo é obrigatório.") String nomeCombo,
        @NotBlank(message = "A descrição do combo é obrigatória.") String descricao,
        @NotNull(message = "A categoria é obrigatória.") UUID categoriaId,
        @NotNull(message = "O modo de preço é obrigatório.") ModoPrecoCombo modoPreco,
        BigDecimal percentualDescontoKit,
        BigDecimal precoFixoOpcional,
        @NotEmpty(message = "Informe os componentes do combo.") List<ComponenteComboEntradaDTO> componentes
    ) {
    }

    /**
     * DTO de entrada para um componente filho de um Combo/Kit.
     *
     * @param componenteSku código SKU do componente filho
     * @param quantidadePorKit unidades por kit
     */
    public record ComponenteComboEntradaDTO(
        @NotBlank String componenteSku,
        int quantidadePorKit
    ) {
    }

    /**
     * DTO de requisição para alteração de preço de item ou combo.
     *
     * @param novoValor novo preço final ou novo percentual de desconto
     */
    public record RequisicaoAlterarPrecoDTO(
        @NotNull(message = "O novo valor é obrigatório.") BigDecimal novoValor
    ) {
    }

    /**
     * DTO de requisição para ativação/inativação lógica de item ou combo.
     *
     * @param ativo novo status booleano
     */
    public record RequisicaoAlterarStatusDTO(
        @NotNull(message = "O campo ativo é obrigatório.") Boolean ativo
    ) {
    }
}

