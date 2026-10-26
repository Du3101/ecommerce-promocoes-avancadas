package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agregado raiz que representa um produto comercial do catálogo e agrupa suas variações
 * comercializáveis ({@link ProdutoVariacaoLeaf}) (FR-005).
 *
 * <p>Aplicação do Padrão GoF: Construído de forma fluente e validada pelo {@link ProdutoBuilder}
 * (<b>Builder</b>) e instanciado pela hierarquia {@code CriadorItemCatalogo} (<b>Factory Method</b>).</p>
 */
@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", length = 160, nullable = false)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(name = "marca", length = 100, nullable = false)
    private String marca;

    @Column(name = "preco_base", precision = 15, scale = 2, nullable = false)
    private BigDecimal precoBase;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaPrincipalId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "produto_atributos_especificos",
        joinColumns = @JoinColumn(name = "produto_id")
    )
    @MapKeyColumn(name = "chave_atributo", length = 80)
    @Column(name = "valor_atributo", length = 255, nullable = false)
    private Map<String, String> atributosEspecificos = new LinkedHashMap<>();

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProdutoVariacaoLeaf> variacoes = new ArrayList<>();

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta em favor do {@link ProdutoBuilder}.</p>
     */
    protected Produto() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor de pacote invocado pelo {@link ProdutoBuilder}.
     *
     * <p>Objetivo Funcional: Inicializar a entidade Produto com todos os metadados validados.</p>
     * <p>Papel no Design Pattern: Produto concreto construído pelo padrão <b>Builder</b>.</p>
     *
     * @param id UUID do produto
     * @param nome nome comercial
     * @param descricao descrição detalhada
     * @param marca marca ou fabricante
     * @param precoBase preço base estritamente positivo ({@code > 0.00})
     * @param categoriaPrincipalId UUID da categoria principal
     * @param atributosEspecificos mapa de especificações técnicas da ficha do produto
     * @param ativo status inicial de ativação
     */
    Produto(
        UUID id,
        String nome,
        String descricao,
        String marca,
        BigDecimal precoBase,
        UUID categoriaPrincipalId,
        Map<String, String> atributosEspecificos,
        boolean ativo
    ) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.marca = marca;
        this.precoBase = precoBase.setScale(2, RoundingMode.HALF_UP);
        this.categoriaPrincipalId = categoriaPrincipalId;
        if (atributosEspecificos != null) {
            this.atributosEspecificos.putAll(atributosEspecificos);
        }
        this.ativo = ativo;
        this.criadoEm = Instant.now();
    }

    /**
     * Anexa uma variação comercializável ({@link ProdutoVariacaoLeaf}) a este produto.
     *
     * <p>Objetivo Funcional: Associar cada SKU físico ao produto pai garantindo preço final positivo.</p>
     * <p>Papel no Design Pattern: Método de montagem estrutural utilizado pelo {@link ProdutoBuilder}.</p>
     *
     * @param variacao instância de {@link ProdutoVariacaoLeaf}
     */
    void adicionarVariacaoInterna(ProdutoVariacaoLeaf variacao) {
        variacao.vincularAoProduto(this);
        if (variacao.calcularPrecoVigente().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception(
                "O preço final da variação SKU '" + variacao.obterSku()
                    + "' (preço base + adicional) deve ser estritamente maior que zero."
            );
        }
        this.variacoes.add(variacao);
    }

    /**
     * Altera o preço base do produto validando que nenhuma variação fique com preço final inválido.
     *
     * <p>Objetivo Funcional: Atualizar preço base em operações administrativas auditadas (FR-005, FR-012).</p>
     * <p>Papel no Design Pattern: Mutação controlada do agregado.</p>
     *
     * @param novoPrecoBase novo preço base estritamente positivo
     */
    public void alterarPrecoBase(BigDecimal novoPrecoBase) {
        if (novoPrecoBase == null || novoPrecoBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception("O novo preço base do produto deve ser maior que zero.");
        }
        BigDecimal ajustado = novoPrecoBase.setScale(2, RoundingMode.HALF_UP);
        for (ProdutoVariacaoLeaf variacao : this.variacoes) {
            if (ajustado.add(variacao.obterAdicionalPreco()).compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraNegocioModulo1Exception(
                    "O novo preço base tornaria o preço final da variação '" + variacao.obterSku() + "' menor ou igual a zero."
                );
            }
        }
        this.precoBase = ajustado;
    }

    /**
     * Altera o estado ativo/inativo do produto e propaga para suas variações quando inativado.
     *
     * <p>Objetivo Funcional: Permitir ativação/inativação lógica auditada sem exclusão física.</p>
     * <p>Papel no Design Pattern: Controle de ciclo de vida do agregado.</p>
     *
     * @param ativo novo estado de ativação
     */
    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * Obtém o identificador único do produto.
     *
     * <p>Objetivo Funcional: Identificar o produto no catálogo.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID do produto
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o nome comercial do produto.
     *
     * <p>Objetivo Funcional: Apresentar título principal do produto.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return nome do produto
     */
    public String obterNome() {
        return this.nome;
    }

    /**
     * Obtém a descrição detalhada do produto.
     *
     * <p>Objetivo Funcional: Exibir ficha descritiva na página do produto.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return descrição textual
     */
    public String obterDescricao() {
        return this.descricao;
    }

    /**
     * Obtém a marca do produto.
     *
     * <p>Objetivo Funcional: Informar fabricante/marca comercial.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return marca do produto
     */
    public String obterMarca() {
        return this.marca;
    }

    /**
     * Obtém o preço base do produto em BRL.
     *
     * <p>Objetivo Funcional: Servir de base para o cálculo do preço de cada variação.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return preço base com 2 casas decimais
     */
    public BigDecimal obterPrecoBase() {
        return this.precoBase;
    }

    /**
     * Obtém o identificador da categoria principal do produto.
     *
     * <p>Objetivo Funcional: Associar o produto à árvore de categorias.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID da categoria principal
     */
    public UUID obterCategoriaPrincipalId() {
        return this.categoriaPrincipalId;
    }

    /**
     * Retorna uma visão imutável do mapa de atributos específicos do produto.
     *
     * <p>Objetivo Funcional: Expor especificações técnicas (ex.: "Garantia" -> "12 meses").</p>
     * <p>Papel no Design Pattern: Encapsulamento defensivo de coleção.</p>
     *
     * @return mapa imutável de atributos específicos
     */
    public Map<String, String> obterAtributosEspecificos() {
        return Collections.unmodifiableMap(this.atributosEspecificos);
    }

    /**
     * Retorna uma visão imutável das variações comercializáveis deste produto.
     *
     * <p>Objetivo Funcional: Listar todos os SKUs pertencentes ao produto.</p>
     * <p>Papel no Design Pattern: Acesso às folhas {@link ProdutoVariacaoLeaf} do produto.</p>
     *
     * @return lista imutável de {@link ProdutoVariacaoLeaf}
     */
    public List<ProdutoVariacaoLeaf> obterVariacoes() {
        return Collections.unmodifiableList(this.variacoes);
    }

    /**
     * Indica se o produto está ativo no catálogo.
     *
     * <p>Objetivo Funcional: Verificar disponibilidade comercial do produto.</p>
     * <p>Papel no Design Pattern: Getter explícito de estado.</p>
     *
     * @return {@code true} se ativo; {@code false} caso contrário
     */
    public boolean estaAtivo() {
        return this.ativo;
    }

    /**
     * Obtém o instante de criação do produto.
     *
     * <p>Objetivo Funcional: Rastreabilidade temporal do cadastro.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return instante UTC de criação
     */
    public Instant obterCriadoEm() {
        return this.criadoEm;
    }
}

