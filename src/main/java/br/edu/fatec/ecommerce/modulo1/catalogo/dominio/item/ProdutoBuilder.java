package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.AtributoVariacaoFlyweight;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Construtor fluente passo a passo para montagem validada de agregados {@link Produto}
 * e suas variações comercializáveis {@link ProdutoVariacaoLeaf} (FR-005).
 *
 * <p>Aplicação do Padrão GoF: <b>Builder</b> (Princípio II da Constituição).</p>
 * <p>Justificativa: Um produto comercial possui múltiplos elementos obrigatórios e opcionais
 * (preço base positivo, categoria vinculada, mapa de especificações técnicas e ao menos uma
 * variação física com SKU único, peso/dimensões válidas e preço final positivo). O {@link ProdutoBuilder}
 * evita construtores telescópicos e impede a criação de produtos incompletos.</p>
 */
public class ProdutoBuilder {

    private UUID id;
    private String nome;
    private String descricao;
    private String marca;
    private BigDecimal precoBase;
    private UUID categoriaPrincipalId;
    private final Map<String, String> atributosEspecificos = new LinkedHashMap<>();
    private final List<ProdutoVariacaoLeaf> variacoes = new ArrayList<>();
    private boolean ativo = true;

    /**
     * Construtor padrão para iniciar a montagem fluente de um {@link Produto}.
     *
     * <p>Objetivo Funcional: Inicializar o estado de montagem do produto.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>ConcreteBuilder</i> do padrão <b>Builder</b>.</p>
     */
    public ProdutoBuilder() {
        // Inicialização padrão do Builder
    }

    /**
     * Define opcionalmente o identificador único do produto.
     *
     * <p>Objetivo Funcional: Permitir definição de UUID específico em testes ou importações.</p>
     * <p>Papel no Design Pattern: Passo fluente opcional do <b>Builder</b>.</p>
     *
     * @param id UUID do produto
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder comId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Define o nome comercial do produto.
     *
     * <p>Objetivo Funcional: Informar o título principal do produto (3 a 160 caracteres).</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param nome nome comercial
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder comNome(String nome) {
        this.nome = nome;
        return this;
    }

    /**
     * Define a descrição detalhada do produto.
     *
     * <p>Objetivo Funcional: Registrar texto descritivo completo para exibição no catálogo.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param descricao descrição do produto
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder comDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    /**
     * Define a marca ou fabricante do produto.
     *
     * <p>Objetivo Funcional: Identificar a marca comercial (até 100 caracteres).</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param marca marca do produto
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder comMarca(String marca) {
        this.marca = marca;
        return this;
    }

    /**
     * Define o preço base do produto em reais (BRL).
     *
     * <p>Objetivo Funcional: Configurar o valor base sobre o qual as variações calculam seu preço final.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param precoBase valor decimal estritamente maior que zero
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder comPrecoBase(BigDecimal precoBase) {
        this.precoBase = precoBase;
        return this;
    }

    /**
     * Define o identificador da categoria principal à qual o produto pertence.
     *
     * <p>Objetivo Funcional: Vincular o produto a um nó da árvore {@code CategoriaComp}.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param categoriaPrincipalId UUID da categoria
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder naCategoria(UUID categoriaPrincipalId) {
        this.categoriaPrincipalId = categoriaPrincipalId;
        return this;
    }

    /**
     * Adiciona um par chave-valor de atributo específico da ficha técnica do produto.
     *
     * <p>Objetivo Funcional: Enriquecer a ficha técnica (ex.: "Garantia" -> "12 meses").</p>
     * <p>Papel no Design Pattern: Passo fluente incremental do <b>Builder</b>.</p>
     *
     * @param chave nome do atributo técnico
     * @param valor valor descritivo do atributo
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder adicionarAtributoEspecifico(String chave, String valor) {
        if (chave == null || chave.isBlank() || valor == null || valor.isBlank()) {
            throw new RegraNegocioModulo1Exception(
                "Chave e valor de atributo específico do produto não podem ser vazios."
            );
        }
        this.atributosEspecificos.put(chave.trim(), valor.trim());
        return this;
    }

    /**
     * Adiciona um mapa de atributos específicos à ficha técnica do produto.
     *
     * <p>Objetivo Funcional: Carregar múltiplos atributos específicos de uma só vez.</p>
     * <p>Papel no Design Pattern: Passo fluente em lote do <b>Builder</b>.</p>
     *
     * @param atributos mapa de especificações técnicas
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder comAtributosEspecificos(Map<String, String> atributos) {
        if (atributos != null) {
            atributos.forEach(this::adicionarAtributoEspecifico);
        }
        return this;
    }

    /**
     * Adiciona uma instância já construída de {@link ProdutoVariacaoLeaf} ao produto.
     *
     * <p>Objetivo Funcional: Incluir um SKU comercializável no agregado do produto.</p>
     * <p>Papel no Design Pattern: Passo de composição de folhas no <b>Builder</b>.</p>
     *
     * @param variacao instância de {@link ProdutoVariacaoLeaf}
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder adicionarVariacao(ProdutoVariacaoLeaf variacao) {
        if (variacao == null) {
            throw new RegraNegocioModulo1Exception("A variação adicionada ao produto não pode ser nula.");
        }
        this.variacoes.add(variacao);
        return this;
    }

    /**
     * Cria e adiciona uma nova variação comercializável ({@link ProdutoVariacaoLeaf}) diretamente no builder.
     *
     * <p>Objetivo Funcional: Facilitar a declaração fluente de variações com seus atributos Flyweight.</p>
     * <p>Papel no Design Pattern: Passo fluente conveniência do <b>Builder</b>.</p>
     *
     * @param sku código SKU único
     * @param codigoBarras código EAN opcional
     * @param nomeVariacao nome comercial da variação
     * @param adicionalPreco diferencial de preço em relação ao preço base
     * @param dimensoesFisicas peso e dimensões físicas positivas
     * @param atributosFlyweight lista de atributos intrínsecos compartilhados ({@link AtributoVariacaoFlyweight})
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder adicionarVariacao(
        String sku,
        String codigoBarras,
        String nomeVariacao,
        BigDecimal adicionalPreco,
        DimensoesFisicas dimensoesFisicas,
        List<AtributoVariacaoFlyweight> atributosFlyweight
    ) {
        ProdutoVariacaoLeaf novaVariacao = new ProdutoVariacaoLeaf(
            sku,
            codigoBarras,
            nomeVariacao,
            adicionalPreco,
            dimensoesFisicas,
            atributosFlyweight
        );
        this.variacoes.add(novaVariacao);
        return this;
    }

    /**
     * Define o estado inicial ativo/inativo do produto.
     *
     * <p>Objetivo Funcional: Configurar disponibilidade imediata do produto (padrão {@code true}).</p>
     * <p>Papel no Design Pattern: Passo fluente opcional do <b>Builder</b>.</p>
     *
     * @param ativo {@code true} para ativo; {@code false} para inativo
     * @return a própria instância de {@link ProdutoBuilder}
     */
    public ProdutoBuilder comAtivo(boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    /**
     * Valida todas as invariantes de negócio e constrói o agregado {@link Produto} com suas variações.
     *
     * <p>Objetivo Funcional: Garantir que o produto tenha nome, descrição, marca, preço base positivo,
     * categoria associada e ao menos uma variação com SKU único e preço final positivo.</p>
     * <p>Papel no Design Pattern: Método finalizador ({@code build()}) do padrão <b>Builder</b>.</p>
     *
     * @return instância íntegra e pronta para uso de {@link Produto}
     * @throws RegraNegocioModulo1Exception se qualquer invariante for violada
     */
    public Produto construir() {
        if (this.nome == null || this.nome.isBlank()
            || this.nome.trim().length() < 3 || this.nome.trim().length() > 160) {
            throw new RegraNegocioModulo1Exception(
                "O nome do produto é obrigatório e deve ter entre 3 e 160 caracteres."
            );
        }
        if (this.descricao == null || this.descricao.isBlank()) {
            throw new RegraNegocioModulo1Exception("A descrição do produto é obrigatória.");
        }
        if (this.marca == null || this.marca.isBlank() || this.marca.trim().length() > 100) {
            throw new RegraNegocioModulo1Exception(
                "A marca do produto é obrigatória e deve ter até 100 caracteres."
            );
        }
        if (this.precoBase == null || this.precoBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception(
                "O preço base do produto é obrigatório e deve ser estritamente maior que zero."
            );
        }
        if (this.categoriaPrincipalId == null) {
            throw new RegraNegocioModulo1Exception(
                "O produto deve estar obrigatoriamente vinculado a uma categoria principal."
            );
        }
        if (this.variacoes.isEmpty()) {
            throw new RegraNegocioModulo1Exception(
                "O produto deve possuir ao menos uma variação comercializável (SKU)."
            );
        }

        Set<String> skusVistos = new HashSet<>();
        for (ProdutoVariacaoLeaf variacao : this.variacoes) {
            if (!skusVistos.add(variacao.obterSku())) {
                throw new RegraNegocioModulo1Exception(
                    "SKU duplicado dentro do mesmo produto: " + variacao.obterSku()
                );
            }
        }

        UUID idFinal = this.id != null ? this.id : UUID.randomUUID();
        Produto produto = new Produto(
            idFinal,
            this.nome.trim(),
            this.descricao.trim(),
            this.marca.trim(),
            this.precoBase,
            this.categoriaPrincipalId,
            this.atributosEspecificos,
            this.ativo
        );

        for (ProdutoVariacaoLeaf variacao : this.variacoes) {
            produto.adicionarVariacaoInterna(variacao);
        }

        return produto;
    }
}

