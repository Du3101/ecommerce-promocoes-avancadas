package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight.AtributoVariacaoFlyweight;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Representa uma variação física comercializável (SKU unitário) de um {@link Produto} (FR-005, FR-006).
 *
 * <p>Aplicação Conjunta dos Padrões GoF (Princípio II da Constituição):</p>
 * <ul>
 *   <li><b>Composite</b> (papel <i>Leaf</i>): implementa {@link ItemCatalogoComponent}, podendo ser
 *   vendida isoladamente ou compor um {@code ComboKitComposite}.</li>
 *   <li><b>Flyweight</b> (contexto extrínseco): mantém o estado extrínseco (SKU, preço adicional,
 *   dimensões) e referencia instâncias compartilhadas de {@link AtributoVariacaoFlyweight} para o
 *   estado intrínseco (ex.: Tamanho, Cor, Voltagem).</li>
 * </ul>
 */
@Entity
@Table(name = "variacoes_produto")
public class ProdutoVariacaoLeaf implements ItemCatalogoComponent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "sku", length = 64, nullable = false, unique = true)
    private String sku;

    @Column(name = "codigo_barras", length = 64)
    private String codigoBarras;

    @Column(name = "nome_variacao", length = 180, nullable = false)
    private String nomeVariacao;

    @Column(name = "adicional_preco", precision = 15, scale = 2, nullable = false)
    private BigDecimal adicionalPreco;

    @Embedded
    private DimensoesFisicas dimensoesFisicas;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "variacao_atributos_flyweight",
        joinColumns = @JoinColumn(name = "variacao_id"),
        inverseJoinColumns = @JoinColumn(name = "flyweight_id")
    )
    private List<AtributoVariacaoFlyweight> atributosFlyweight = new ArrayList<>();

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Preserva construção controlada via {@link ProdutoBuilder}.</p>
     */
    protected ProdutoVariacaoLeaf() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor explícito para criação de uma folha comercializável do catálogo.
     *
     * <p>Objetivo Funcional: Criar um SKU físico validando código SKU, nome, dimensões e atributos Flyweight.</p>
     * <p>Papel no Design Pattern: Instancia o elemento <i>Leaf</i> do padrão <b>Composite</b> associado
     * aos objetos compartilhados do padrão <b>Flyweight</b>.</p>
     *
     * @param sku código SKU único (3 a 64 caracteres)
     * @param codigoBarras código EAN/GTIN opcional
     * @param nomeVariacao descrição da variação (ex.: "Camiseta DryFit - Preta - M")
     * @param adicionalPreco valor somado ou subtraído do preço base do produto
     * @param dimensoesFisicas Value Object com peso e dimensões positivas
     * @param atributosFlyweight lista de atributos intrínsecos compartilhados
     */
    public ProdutoVariacaoLeaf(
        String sku,
        String codigoBarras,
        String nomeVariacao,
        BigDecimal adicionalPreco,
        DimensoesFisicas dimensoesFisicas,
        List<AtributoVariacaoFlyweight> atributosFlyweight
    ) {
        if (sku == null || sku.isBlank() || sku.trim().length() < 3 || sku.trim().length() > 64) {
            throw new RegraNegocioModulo1Exception(
                "O SKU da variação é obrigatório e deve ter entre 3 e 64 caracteres."
            );
        }
        if (nomeVariacao == null || nomeVariacao.isBlank() || nomeVariacao.trim().length() > 180) {
            throw new RegraNegocioModulo1Exception(
                "O nome da variação é obrigatório e deve ter até 180 caracteres."
            );
        }
        if (dimensoesFisicas == null) {
            throw new RegraNegocioModulo1Exception(
                "As dimensões físicas (peso, altura, largura e profundidade) são obrigatórias para a variação."
            );
        }

        this.id = UUID.randomUUID();
        this.sku = sku.trim().toUpperCase(Locale.ROOT);
        this.codigoBarras = codigoBarras != null && !codigoBarras.isBlank() ? codigoBarras.trim() : null;
        this.nomeVariacao = nomeVariacao.trim();
        this.adicionalPreco = (adicionalPreco != null ? adicionalPreco : BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
        this.dimensoesFisicas = dimensoesFisicas;
        if (atributosFlyweight != null) {
            this.atributosFlyweight.addAll(atributosFlyweight);
        }
        this.ativo = true;
    }

    /**
     * Vincula esta variação ao seu agregado {@link Produto} pai.
     *
     * <p>Objetivo Funcional: Estabelecer o relacionamento bidirecional N:1 com o produto.</p>
     * <p>Papel no Design Pattern: Método auxiliar de pacote utilizado pelo {@link ProdutoBuilder}.</p>
     *
     * @param produto instância do produto pai
     */
    void vincularAoProduto(Produto produto) {
        this.produto = produto;
    }

    /**
     * Calcula o preço unitário vigente desta variação somando o preço base do produto ao adicional da variação.
     *
     * <p>Objetivo Funcional: Determinar o preço final de venda do SKU.</p>
     * <p>Papel no Design Pattern: Implementação do método polimórfico {@code calcularPrecoVigente()}
     * do <i>Leaf</i> no padrão <b>Composite</b>.</p>
     *
     * @return preço vigente com 2 casas decimais ({@code precoBase + adicionalPreco})
     */
    @Override
    public BigDecimal calcularPrecoVigente() {
        BigDecimal base = this.produto != null ? this.produto.obterPrecoBase() : BigDecimal.ZERO;
        return base.add(this.adicionalPreco).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Altera o valor adicional de preço desta variação garantindo que o preço final continue positivo.
     *
     * <p>Objetivo Funcional: Permitir ajuste fino de preço por SKU em operação administrativa auditada.</p>
     * <p>Papel no Design Pattern: Atualização do estado extrínseco da folha.</p>
     *
     * @param novoAdicionalPreco novo valor adicional em reais
     * @throws RegraNegocioModulo1Exception se o preço resultante for menor ou igual a zero
     */
    public void alterarAdicionalPreco(BigDecimal novoAdicionalPreco) {
        BigDecimal candidato = (novoAdicionalPreco != null ? novoAdicionalPreco : BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = this.produto != null ? this.produto.obterPrecoBase() : BigDecimal.ZERO;
        if (base.add(candidato).compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception(
                "O preço final da variação SKU '" + this.sku + "' deve permanecer maior que zero."
            );
        }
        this.adicionalPreco = candidato;
    }

    /**
     * Altera diretamente o preço final desta variação ajustando seu adicional em relação ao preço base do produto.
     *
     * <p>Objetivo Funcional: Facilitar a alteração direta do preço final do SKU pelo operador comercial.</p>
     * <p>Papel no Design Pattern: Operação de negócio sobre o <i>Leaf</i>.</p>
     *
     * @param novoPrecoFinal novo preço final desejado ({@code > 0.00})
     */
    public void definirNovoPrecoFinal(BigDecimal novoPrecoFinal) {
        if (novoPrecoFinal == null || novoPrecoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception("O novo preço final do SKU deve ser maior que zero.");
        }
        BigDecimal base = this.produto != null ? this.produto.obterPrecoBase() : BigDecimal.ZERO;
        this.adicionalPreco = novoPrecoFinal.setScale(2, RoundingMode.HALF_UP).subtract(base);
    }

    /**
     * Obtém o peso físico unitário desta variação em gramas.
     *
     * <p>Objetivo Funcional: Fornecer o peso do item para cálculo de frete ou soma em combos.</p>
     * <p>Papel no Design Pattern: Caso base (<i>Leaf</i>) do cálculo recursivo de peso no <b>Composite</b>.</p>
     *
     * @return peso em gramas
     */
    @Override
    public int obterPesoTotalGramas() {
        return this.dimensoesFisicas.obterPesoGramas();
    }

    /**
     * Obtém as dimensões físicas desta variação.
     *
     * <p>Objetivo Funcional: Informar altura, largura, profundidade e peso do SKU.</p>
     * <p>Papel no Design Pattern: Implementação do contrato {@link ItemCatalogoComponent}.</p>
     *
     * @return Value Object {@link DimensoesFisicas}
     */
    @Override
    public DimensoesFisicas obterDimensoesFisicas() {
        return this.dimensoesFisicas;
    }

    /**
     * Obtém o identificador da categoria principal herdada do produto pai.
     *
     * <p>Objetivo Funcional: Vincular o SKU à árvore de categorias.</p>
     * <p>Papel no Design Pattern: Implementação do contrato {@link ItemCatalogoComponent}.</p>
     *
     * @return UUID da categoria principal
     */
    @Override
    public UUID obterCategoriaPrincipalId() {
        return this.produto != null ? this.produto.obterCategoriaPrincipalId() : null;
    }

    /**
     * Indica se esta variação e seu produto pai estão ativos para comercialização.
     *
     * <p>Objetivo Funcional: Impedir venda de variação inativa ou cujo produto pai esteja inativo.</p>
     * <p>Papel no Design Pattern: Caso base de ativação do <i>Leaf</i> no <b>Composite</b>.</p>
     *
     * @return {@code true} se ambos estiverem ativos; {@code false} caso contrário
     */
    @Override
    public boolean estaAtivo() {
        return this.ativo && (this.produto == null || this.produto.estaAtivo());
    }

    /**
     * Altera o status ativo/inativo desta variação.
     *
     * <p>Objetivo Funcional: Permitir ativação ou inativação lógica do SKU.</p>
     * <p>Papel no Design Pattern: Mutação explícita de estado.</p>
     *
     * @param ativo novo status
     */
    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * Indica que este componente é uma folha unitária, e não um combo/kit.
     *
     * <p>Objetivo Funcional: Distinguir item simples de combo promocional.</p>
     * <p>Papel no Design Pattern: Identifica o papel <i>Leaf</i> no padrão <b>Composite</b>.</p>
     *
     * @return sempre {@code false}
     */
    @Override
    public boolean eComboKit() {
        return false;
    }

    /**
     * Calcula a disponibilidade máxima desta folha consultando diretamente o saldo disponível do seu SKU.
     *
     * <p>Objetivo Funcional: Retornar o saldo disponível em estoque para este SKU (ou 0 se inativo).</p>
     * <p>Papel no Design Pattern: Caso base (<i>Leaf</i>) do cálculo recursivo de disponibilidade no <b>Composite</b>.</p>
     *
     * @param consultorSaldoDisponivelPorSku função de consulta de saldo por SKU
     * @return quantidade disponível ({@code >= 0})
     */
    @Override
    public int calcularDisponibilidadeMaxima(Function<String, Integer> consultorSaldoDisponivelPorSku) {
        if (!estaAtivo() || consultorSaldoDisponivelPorSku == null) {
            return 0;
        }
        Integer saldo = consultorSaldoDisponivelPorSku.apply(this.sku);
        return saldo != null && saldo > 0 ? saldo : 0;
    }

    /**
     * Retorna o mapa unitário contendo o próprio SKU com multiplicador {@code 1}.
     *
     * <p>Objetivo Funcional: Servir como caso base na decomposição de SKUs físicos para reserva de estoque.</p>
     * <p>Papel no Design Pattern: Caso base (<i>Leaf</i>) da expansão recursiva no <b>Composite</b>.</p>
     *
     * @return mapa imutável {@code {sku -> 1}}
     */
    @Override
    public Map<String, Integer> expandirSkusFisicosComMultiplicador() {
        return Map.of(this.sku, 1);
    }

    /**
     * Retorna o mapa contendo o próprio SKU multiplicado pela quantidade solicitada para reserva.
     *
     * <p>Objetivo Funcional: Informar quantas unidades físicas deste SKU precisam ser reservadas.</p>
     * <p>Papel no Design Pattern: Caso base (<i>Leaf</i>) da decomposição para reserva no <b>Composite</b>.</p>
     *
     * @param quantidadeSolicitada quantidade desejada ({@code >= 1})
     * @return mapa imutável {@code {sku -> quantidadeSolicitada}}
     */
    @Override
    public Map<String, Integer> decomporSkusFisicosParaReserva(int quantidadeSolicitada) {
        if (quantidadeSolicitada < 1) {
            throw new RegraNegocioModulo1Exception("A quantidade solicitada para reserva deve ser no mínimo 1.");
        }
        return Map.of(this.sku, quantidadeSolicitada);
    }

    /**
     * Obtém o identificador único da variação.
     *
     * <p>Objetivo Funcional: Identificar a variação na persistência.</p>
     * <p>Papel no Design Pattern: Implementação de {@link ItemCatalogoComponent#obterId()}.</p>
     *
     * @return UUID da variação
     */
    @Override
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o código SKU único da variação.
     *
     * <p>Objetivo Funcional: Chave de negócio do item no catálogo e no estoque.</p>
     * <p>Papel no Design Pattern: Implementação de {@link ItemCatalogoComponent#obterSku()}.</p>
     *
     * @return código SKU
     */
    @Override
    public String obterSku() {
        return this.sku;
    }

    /**
     * Obtém o nome completo de exibição da variação.
     *
     * <p>Objetivo Funcional: Apresentar o nome comercial do SKU.</p>
     * <p>Papel no Design Pattern: Implementação de {@link ItemCatalogoComponent#obterNomeExibicao()}.</p>
     *
     * @return nome da variação
     */
    @Override
    public String obterNomeExibicao() {
        return this.nomeVariacao;
    }

    /**
     * Obtém o código de barras (EAN/GTIN) da variação, quando cadastrado.
     *
     * <p>Objetivo Funcional: Identificação logística por leitor óptico.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return código de barras ou {@code null}
     */
    public String obterCodigoBarras() {
        return this.codigoBarras;
    }

    /**
     * Obtém o valor adicional de preço da variação em relação ao produto base.
     *
     * <p>Objetivo Funcional: Expor a diferença de preço específica do SKU.</p>
     * <p>Papel no Design Pattern: Getter explícito do estado extrínseco.</p>
     *
     * @return adicional de preço
     */
    public BigDecimal obterAdicionalPreco() {
        return this.adicionalPreco;
    }

    /**
     * Obtém a referência ao produto pai desta variação.
     *
     * <p>Objetivo Funcional: Acessar marca, descrição e preço base do produto agrupador.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return instância de {@link Produto}
     */
    public Produto obterProduto() {
        return this.produto;
    }

    /**
     * Retorna uma visão imutável da lista de atributos intrínsecos compartilhados ({@link AtributoVariacaoFlyweight}).
     *
     * <p>Objetivo Funcional: Consultar as características de variação (Cor, Tamanho, Voltagem) deste SKU.</p>
     * <p>Papel no Design Pattern: Acesso às referências compartilhadas do padrão <b>Flyweight</b>.</p>
     *
     * @return lista imutável de {@link AtributoVariacaoFlyweight}
     */
    public List<AtributoVariacaoFlyweight> obterAtributosFlyweight() {
        return Collections.unmodifiableList(this.atributosFlyweight);
    }
}
