package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import br.edu.fatec.ecommerce.compartilhado.excecao.CicloHierarquicoException;
import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Entidade composta que representa um Combo ou Kit Promocional formado recursivamente por
 * variações unitárias ({@link ProdutoVariacaoLeaf}) e/ou outros Combos/Kits (FR-008).
 *
 * <p>Aplicação do Padrão GoF: <b>Composite</b> — papel <i>Composite</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Um combo promocional (ex.: "Kit Escritório Completo" contendo "Kit Periféricos" +
 * "Monitor 27") deve ser tratado pelo catálogo, carrinho, promoções e frete exatamente como qualquer
 * {@link ItemCatalogoComponent}, calculando recursivamente seu preço, peso total, dimensões consolidadas,
 * disponibilidade máxima em estoque e decomposição de SKUs físicos sem permitir ciclos.</p>
 */
@Entity
@Table(name = "combos_kits")
public class ComboKitComposite implements ItemCatalogoComponent {

    private static final BigDecimal CEM = new BigDecimal("100.00");

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", length = 64, nullable = false, unique = true)
    private String sku;

    @Column(name = "nome", length = 180, nullable = false)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT", nullable = false)
    private String descricao;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaPrincipalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_preco", length = 40, nullable = false)
    private ModoPrecoCombo modoPreco;

    @Column(name = "percentual_desconto_kit", precision = 5, scale = 2, nullable = false)
    private BigDecimal percentualDescontoKit;

    @Column(name = "preco_fixo_opcional", precision = 15, scale = 2)
    private BigDecimal precoFixoOpcional;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemComponenteKit> componentes = new ArrayList<>();

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta em favor do {@link ComboKitBuilder}.</p>
     */
    protected ComboKitComposite() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor de pacote invocado exclusivamente pelo {@link ComboKitBuilder}.
     *
     * <p>Objetivo Funcional: Inicializar o elemento <i>Composite</i> com seus metadados e política de preço.</p>
     * <p>Papel no Design Pattern: Instancia o nó <i>Composite</i> do padrão <b>Composite</b>.</p>
     *
     * @param id UUID do combo
     * @param sku código SKU único do combo
     * @param nome nome comercial do combo
     * @param descricao descrição comercial
     * @param categoriaPrincipalId UUID da categoria principal
     * @param modoPreco modo de cálculo de preço ({@link ModoPrecoCombo})
     * @param percentualDescontoKit percentual de desconto para soma recursiva ({@code 0.00} a {@code 90.00})
     * @param precoFixoOpcional preço fixo dedicado quando em {@code PRECO_FIXO_DEDICADO}
     * @param ativo status inicial de ativação
     */
    ComboKitComposite(
        UUID id,
        String sku,
        String nome,
        String descricao,
        UUID categoriaPrincipalId,
        ModoPrecoCombo modoPreco,
        BigDecimal percentualDescontoKit,
        BigDecimal precoFixoOpcional,
        boolean ativo
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.sku = sku.trim().toUpperCase(Locale.ROOT);
        this.nome = nome.trim();
        this.descricao = descricao.trim();
        this.categoriaPrincipalId = categoriaPrincipalId;
        this.modoPreco = modoPreco;
        this.percentualDescontoKit = (percentualDescontoKit != null ? percentualDescontoKit : BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
        this.precoFixoOpcional = precoFixoOpcional != null
            ? precoFixoOpcional.setScale(2, RoundingMode.HALF_UP)
            : null;
        this.ativo = ativo;
        this.criadoEm = Instant.now();
    }

    /**
     * Adiciona um item filho ({@link ProdutoVariacaoLeaf} ou outro {@link ComboKitComposite}) a este combo,
     * impedindo recursivamente a formação de ciclos de composição (FR-008).
     *
     * <p>Objetivo Funcional: Estruturar a composição do kit garantindo ausência de auto-referência
     * direta ou indireta e consolidando quantidades caso o SKU já faça parte do combo.</p>
     * <p>Papel no Design Pattern: Operação {@code add(Component)} do padrão <b>Composite</b> com
     * verificação recursiva de grafo acíclico.</p>
     *
     * @param componente item filho comercializável
     * @param quantidadePorKit quantidade de unidades do filho em cada kit ({@code >= 1})
     * @throws CicloHierarquicoException se o filho for o próprio combo ou contiver este combo em sua subárvore
     */
    public void adicionarComponente(ItemCatalogoComponent componente, int quantidadePorKit) {
        if (componente == null) {
            throw new RegraNegocioModulo1Exception("O componente adicionado ao combo não pode ser nulo.");
        }
        if (this.sku.equalsIgnoreCase(componente.obterSku())) {
            throw new CicloHierarquicoException(
                "Operação bloqueada: o combo '" + this.sku + "' não pode conter a si mesmo."
            );
        }
        if (componente instanceof ComboKitComposite outroCombo
            && outroCombo.contemSkuNaSubarvore(this.sku)) {
            throw new CicloHierarquicoException(
                "Operação bloqueada: adicionar o combo '" + outroCombo.obterSku()
                    + "' dentro de '" + this.sku + "' formaria um ciclo recursivo de composição."
            );
        }

        for (ItemComponenteKit existente : this.componentes) {
            if (existente.obterComponenteSku().equalsIgnoreCase(componente.obterSku())) {
                throw new RegraNegocioModulo1Exception(
                    "O componente '" + componente.obterSku() + "' já foi adicionado a este combo."
                );
            }
        }

        ItemComponenteKit vinculo = new ItemComponenteKit(componente, quantidadePorKit);
        vinculo.vincularAoCombo(this);
        this.componentes.add(vinculo);
    }

    /**
     * Verifica recursivamente se um determinado código SKU já participa da subárvore deste combo.
     *
     * <p>Objetivo Funcional: Impedir ciclos diretos ou indiretos entre combos aninhados (ex.: A -> B -> A).</p>
     * <p>Papel no Design Pattern: Travessia recursiva em profundidade (DFS) no padrão <b>Composite</b>.</p>
     *
     * @param skuProcurado código SKU a pesquisar
     * @return {@code true} se o SKU estiver presente em qualquer nível abaixo deste combo
     */
    public boolean contemSkuNaSubarvore(String skuProcurado) {
        if (skuProcurado == null || skuProcurado.isBlank()) {
            return false;
        }
        if (this.sku.equalsIgnoreCase(skuProcurado.trim())) {
            return true;
        }
        for (ItemComponenteKit item : this.componentes) {
            if (item.obterComponenteSku().equalsIgnoreCase(skuProcurado.trim())) {
                return true;
            }
            ItemCatalogoComponent filho = item.obterComponente();
            if (filho instanceof ComboKitComposite subCombo
                && subCombo.contemSkuNaSubarvore(skuProcurado)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calcula o preço unitário vigente do combo conforme o {@link ModoPrecoCombo} configurado (FR-008).
     *
     * <p>Objetivo Funcional: No modo {@code SOMA_RECURSIVA_COMPONENTES}, soma recursivamente os preços dos
     * componentes multiplicados por suas quantidades e aplica o percentual de desconto do kit; no modo
     * {@code PRECO_FIXO_DEDICADO}, retorna o preço fixo promocional.</p>
     * <p>Papel no Design Pattern: Operação polimórfica recursiva do <i>Composite</i> em {@link ItemCatalogoComponent}.</p>
     *
     * @return preço vigente do combo em BRL com 2 casas decimais ({@code > 0.00})
     */
    @Override
    public BigDecimal calcularPrecoVigente() {
        if (this.modoPreco == ModoPrecoCombo.PRECO_FIXO_DEDICADO) {
            return this.precoFixoOpcional.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal somaBruta = BigDecimal.ZERO;
        for (ItemComponenteKit item : this.componentes) {
            BigDecimal precoFilho = item.obterComponente().calcularPrecoVigente();
            BigDecimal subtotalFilho = precoFilho.multiply(BigDecimal.valueOf(item.obterQuantidadePorKit()));
            somaBruta = somaBruta.add(subtotalFilho);
        }

        BigDecimal fatorMultiplicador = CEM.subtract(this.percentualDescontoKit)
            .divide(CEM, 6, RoundingMode.HALF_UP);
        BigDecimal precoComDesconto = somaBruta.multiply(fatorMultiplicador)
            .setScale(2, RoundingMode.HALF_UP);

        if (precoComDesconto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception(
                "O preço calculado do combo '" + this.sku + "' resultou em valor menor ou igual a zero."
            );
        }
        return precoComDesconto;
    }

    /**
     * Calcula recursivamente o peso total em gramas do combo somando o peso de todos os componentes
     * multiplicado pelas respectivas quantidades por kit (FR-008).
     *
     * <p>Objetivo Funcional: Fornecer o peso físico exato do pacote completo para o cálculo de frete.</p>
     * <p>Papel no Design Pattern: Agregação recursiva de folhas e sub-combos no padrão <b>Composite</b>.</p>
     *
     * @return peso total em gramas ({@code > 0})
     */
    @Override
    public int obterPesoTotalGramas() {
        int pesoAcumulado = 0;
        for (ItemComponenteKit item : this.componentes) {
            pesoAcumulado += item.obterComponente().obterPesoTotalGramas() * item.obterQuantidadePorKit();
        }
        return pesoAcumulado;
    }

    /**
     * Consolida recursivamente as dimensões físicas do combo a partir das dimensões de seus componentes.
     *
     * <p>Objetivo Funcional: Empilhar alturas proporcionalmente às quantidades e adotar a maior largura
     * e profundidade entre os componentes para estimativa logística de embalagem.</p>
     * <p>Papel no Design Pattern: Operação composta que retorna um {@link DimensoesFisicas} via
     * <b>Static Factory Method</b>.</p>
     *
     * @return Value Object {@link DimensoesFisicas} consolidado do combo
     */
    @Override
    public DimensoesFisicas obterDimensoesFisicas() {
        int pesoTotal = obterPesoTotalGramas();
        BigDecimal alturaTotal = BigDecimal.ZERO;
        BigDecimal larguraMaxima = BigDecimal.ZERO;
        BigDecimal profundidadeMaxima = BigDecimal.ZERO;

        for (ItemComponenteKit item : this.componentes) {
            DimensoesFisicas dimFilho = item.obterComponente().obterDimensoesFisicas();
            BigDecimal qtd = BigDecimal.valueOf(item.obterQuantidadePorKit());
            alturaTotal = alturaTotal.add(dimFilho.obterAlturaCm().multiply(qtd));
            if (dimFilho.obterLarguraCm().compareTo(larguraMaxima) > 0) {
                larguraMaxima = dimFilho.obterLarguraCm();
            }
            if (dimFilho.obterProfundidadeCm().compareTo(profundidadeMaxima) > 0) {
                profundidadeMaxima = dimFilho.obterProfundidadeCm();
            }
        }

        return DimensoesFisicas.emCentimetrosEGramas(
            pesoTotal,
            alturaTotal,
            larguraMaxima,
            profundidadeMaxima
        );
    }

    /**
     * Calcula recursivamente a quantidade máxima de kits que podem ser montados com o estoque atual (FR-008).
     *
     * <p>Objetivo Funcional: Aplicar a fórmula $\min(\lfloor \text{disponivel}_i / \text{qtdNoKit}_i \rfloor)$
     * considerando tanto a disponibilidade recursiva de cada componente imediato quanto a soma consolidada
     * de cada SKU folha (caso um mesmo SKU apareça em mais de um sub-kit).</p>
     * <p>Papel no Design Pattern: Operação recursiva de disponibilidade do padrão <b>Composite</b>.</p>
     *
     * @param consultorSaldoDisponivelPorSku função que consulta o saldo físico disponível por SKU folha
     * @return quantidade máxima de combos disponíveis para venda ({@code >= 0})
     */
    @Override
    public int calcularDisponibilidadeMaxima(Function<String, Integer> consultorSaldoDisponivelPorSku) {
        if (!estaAtivo() || this.componentes.isEmpty() || consultorSaldoDisponivelPorSku == null) {
            return 0;
        }

        int disponibilidadeMinima = Integer.MAX_VALUE;
        for (ItemComponenteKit item : this.componentes) {
            int dispComponente = item.obterComponente().calcularDisponibilidadeMaxima(consultorSaldoDisponivelPorSku);
            int kitsPossiveis = dispComponente / item.obterQuantidadePorKit();
            disponibilidadeMinima = Math.min(disponibilidadeMinima, kitsPossiveis);
        }

        // Verifica também os SKUs folhas consolidados para cobrir repetição do mesmo SKU em ramos distintos
        Map<String, Integer> folhasNecessarias = expandirSkusFisicosComMultiplicador();
        for (Map.Entry<String, Integer> entrada : folhasNecessarias.entrySet()) {
            Integer saldoFolha = consultorSaldoDisponivelPorSku.apply(entrada.getKey());
            int dispFolha = saldoFolha != null && saldoFolha > 0 ? saldoFolha : 0;
            int kitsPorFolha = dispFolha / entrada.getValue();
            disponibilidadeMinima = Math.min(disponibilidadeMinima, kitsPorFolha);
        }

        return disponibilidadeMinima == Integer.MAX_VALUE ? 0 : Math.max(0, disponibilidadeMinima);
    }

    /**
     * Decompõe recursivamente este combo em todos os SKUs físicos folha necessários para formar 1 kit.
     *
     * <p>Objetivo Funcional: Permitir que a reserva atômica de estoque baixe a quantidade exata de
     * cada SKU físico que integra o combo (inclusive combos dentro de combos).</p>
     * <p>Papel no Design Pattern: Travessia recursiva de folhas do padrão <b>Composite</b>.</p>
     *
     * @return mapa associando cada SKU folha à quantidade total exigida por 1 unidade deste combo
     */
    @Override
    public Map<String, Integer> expandirSkusFisicosComMultiplicador() {
        Map<String, Integer> acumuladorFolhas = new LinkedHashMap<>();
        for (ItemComponenteKit item : this.componentes) {
            int multiplicadorRamo = item.obterQuantidadePorKit();
            Map<String, Integer> subFolhas = item.obterComponente().expandirSkusFisicosComMultiplicador();
            for (Map.Entry<String, Integer> folha : subFolhas.entrySet()) {
                acumuladorFolhas.merge(
                    folha.getKey(),
                    folha.getValue() * multiplicadorRamo,
                    Integer::sum
                );
            }
        }
        return Collections.unmodifiableMap(acumuladorFolhas);
    }

    /**
     * Decompõe recursivamente este combo multiplicando cada SKU folha pela quantidade de kits solicitada.
     *
     * <p>Objetivo Funcional: Determinar quantas unidades físicas de cada SKU folha devem ser reservadas
     * no estoque quando um cliente compra {@code quantidadeKits} unidades deste combo (FR-008, FR-010).</p>
     * <p>Papel no Design Pattern: Operação composta recursiva do padrão <b>Composite</b>.</p>
     *
     * @param quantidadeKits quantidade de combos solicitada ({@code >= 1})
     * @return mapa associando cada SKU folha à quantidade total física necessária
     */
    @Override
    public Map<String, Integer> decomporSkusFisicosParaReserva(int quantidadeKits) {
        if (quantidadeKits < 1) {
            throw new RegraNegocioModulo1Exception("A quantidade de combos para reserva deve ser no mínimo 1.");
        }
        Map<String, Integer> porUmKit = expandirSkusFisicosComMultiplicador();
        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entrada : porUmKit.entrySet()) {
            resultado.put(entrada.getKey(), entrada.getValue() * quantidadeKits);
        }
        return Collections.unmodifiableMap(resultado);
    }

    /**
     * Atualiza a política de precificação ou o preço do combo em operação administrativa auditada.
     *
     * <p>Objetivo Funcional: Permitir reajuste do preço fixo ou do percentual de desconto do kit.</p>
     * <p>Papel no Design Pattern: Mutação controlada do nó <i>Composite</i>.</p>
     *
     * @param novoValor novo preço fixo (se {@code PRECO_FIXO_DEDICADO}) ou novo percentual de desconto
     *                  (se {@code SOMA_RECURSIVA_COMPONENTES})
     */
    public void atualizarParametroPreco(BigDecimal novoValor) {
        if (novoValor == null) {
            throw new RegraNegocioModulo1Exception("O novo valor de precificação do combo é obrigatório.");
        }
        if (this.modoPreco == ModoPrecoCombo.PRECO_FIXO_DEDICADO) {
            if (novoValor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraNegocioModulo1Exception(
                    "O preço fixo dedicado do combo deve ser estritamente maior que zero."
                );
            }
            this.precoFixoOpcional = novoValor.setScale(2, RoundingMode.HALF_UP);
        } else {
            if (novoValor.compareTo(BigDecimal.ZERO) < 0 || novoValor.compareTo(new BigDecimal("90.00")) > 0) {
                throw new RegraNegocioModulo1Exception(
                    "O percentual de desconto do combo deve estar entre 0.00% e 90.00%."
                );
            }
            this.percentualDescontoKit = novoValor.setScale(2, RoundingMode.HALF_UP);
        }
    }

    /**
     * Indica se o próprio combo e todos os seus componentes filhos estão ativos para comercialização.
     *
     * <p>Objetivo Funcional: Impedir venda de combo que tenha sido inativado ou que possua algum
     * componente obrigatório inativado.</p>
     * <p>Papel no Design Pattern: Verificação recursiva de estado no padrão <b>Composite</b>.</p>
     *
     * @return {@code true} se o combo e todos os filhos estiverem ativos
     */
    @Override
    public boolean estaAtivo() {
        if (!this.ativo || this.componentes.isEmpty()) {
            return false;
        }
        for (ItemComponenteKit item : this.componentes) {
            if (!item.obterComponente().estaAtivo()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Altera o status ativo/inativo próprio do combo.
     *
     * <p>Objetivo Funcional: Permitir ativação ou inativação lógica administrativa do combo.</p>
     * <p>Papel no Design Pattern: Mutação explícita de estado.</p>
     *
     * @param ativo novo status
     */
    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    /**
     * Indica que este item de catálogo é um combo/kit composto.
     *
     * <p>Objetivo Funcional: Distinguir o <i>Composite</i> das folhas {@link ProdutoVariacaoLeaf}.</p>
     * <p>Papel no Design Pattern: Identifica o papel <i>Composite</i>.</p>
     *
     * @return sempre {@code true}
     */
    @Override
    public boolean eComboKit() {
        return true;
    }

    /**
     * Obtém o identificador único do combo.
     *
     * <p>Objetivo Funcional: Identificar o combo na persistência.</p>
     * <p>Papel no Design Pattern: Implementação de {@link ItemCatalogoComponent#obterId()}.</p>
     *
     * @return UUID do combo
     */
    @Override
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o código SKU único do combo.
     *
     * <p>Objetivo Funcional: Identificar o combo no catálogo e nas vendas.</p>
     * <p>Papel no Design Pattern: Implementação de {@link ItemCatalogoComponent#obterSku()}.</p>
     *
     * @return código SKU do combo
     */
    @Override
    public String obterSku() {
        return this.sku;
    }

    /**
     * Obtém o nome comercial de exibição do combo.
     *
     * <p>Objetivo Funcional: Apresentar título do kit na vitrine.</p>
     * <p>Papel no Design Pattern: Implementação de {@link ItemCatalogoComponent#obterNomeExibicao()}.</p>
     *
     * @return nome do combo
     */
    @Override
    public String obterNomeExibicao() {
        return this.nome;
    }

    /**
     * Obtém a descrição detalhada do combo.
     *
     * <p>Objetivo Funcional: Descrever os benefícios do kit promocional.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return descrição textual
     */
    public String obterDescricao() {
        return this.descricao;
    }

    /**
     * Obtém o identificador da categoria principal do combo.
     *
     * <p>Objetivo Funcional: Associar o combo à árvore de categorias.</p>
     * <p>Papel no Design Pattern: Implementação de {@link ItemCatalogoComponent#obterCategoriaPrincipalId()}.</p>
     *
     * @return UUID da categoria principal
     */
    @Override
    public UUID obterCategoriaPrincipalId() {
        return this.categoriaPrincipalId;
    }

    /**
     * Obtém o modo de precificação configurado para o combo.
     *
     * <p>Objetivo Funcional: Informar se o preço é recursivo com desconto ou fixo dedicado.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return {@link ModoPrecoCombo}
     */
    public ModoPrecoCombo obterModoPreco() {
        return this.modoPreco;
    }

    /**
     * Obtém o percentual de desconto do kit (quando em modo {@code SOMA_RECURSIVA_COMPONENTES}).
     *
     * <p>Objetivo Funcional: Expor o desconto embutido no pacote.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return percentual de desconto ({@code 0.00} a {@code 90.00})
     */
    public BigDecimal obterPercentualDescontoKit() {
        return this.percentualDescontoKit;
    }

    /**
     * Obtém o preço fixo opcional (quando em modo {@code PRECO_FIXO_DEDICADO}).
     *
     * <p>Objetivo Funcional: Expor o valor fixo dedicado do pacote.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return preço fixo ou {@code null}
     */
    public BigDecimal obterPrecoFixoOpcional() {
        return this.precoFixoOpcional;
    }

    /**
     * Retorna uma visão imutável dos vínculos de componentes filhos deste combo.
     *
     * <p>Objetivo Funcional: Inspecionar a estrutura imediata do kit.</p>
     * <p>Papel no Design Pattern: Operação {@code getChildren()} do padrão <b>Composite</b>.</p>
     *
     * @return lista imutável de {@link ItemComponenteKit}
     */
    public List<ItemComponenteKit> obterComponentes() {
        return Collections.unmodifiableList(this.componentes);
    }
}
