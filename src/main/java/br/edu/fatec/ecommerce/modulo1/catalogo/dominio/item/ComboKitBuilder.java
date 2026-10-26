package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Construtor fluente passo a passo para criação validada de pacotes compostos {@link ComboKitComposite} (FR-008).
 *
 * <p>Aplicação Conjunta dos Padrões GoF: <b>Builder</b> construindo uma estrutura <b>Composite</b>
 * (Princípio II da Constituição).</p>
 * <p>Justificativa: Garante que todo combo/kit seja criado com SKU válido, nome, descrição, categoria,
 * ao menos 1 componente filho válido (sem ciclos de auto-referência) e política de precificação
 * consistente ({@code SOMA_RECURSIVA_COMPONENTES} com desconto {@code 0..90%} ou
 * {@code PRECO_FIXO_DEDICADO > 0.00}).</p>
 */
public class ComboKitBuilder {

    private UUID id;
    private String sku;
    private String nome;
    private String descricao;
    private UUID categoriaPrincipalId;
    private ModoPrecoCombo modoPreco = ModoPrecoCombo.SOMA_RECURSIVA_COMPONENTES;
    private BigDecimal percentualDescontoKit = BigDecimal.ZERO;
    private BigDecimal precoFixoOpcional;
    private boolean ativo = true;
    private final List<EntradaComponente> entradasComponentes = new ArrayList<>();

    /**
     * Construtor padrão explícito do Builder de combos/kits.
     *
     * <p>Objetivo Funcional: Iniciar a configuração fluente de um novo {@link ComboKitComposite}.</p>
     * <p>Papel no Design Pattern: Inicializa o <i>ConcreteBuilder</i> do padrão <b>Builder</b>.</p>
     */
    public ComboKitBuilder() {
        // Inicialização padrão do Builder
    }

    /**
     * Define opcionalmente o identificador único do combo.
     *
     * <p>Objetivo Funcional: Permitir UUID pré-definido em testes ou migrações.</p>
     * <p>Papel no Design Pattern: Passo fluente opcional do <b>Builder</b>.</p>
     *
     * @param id UUID do combo
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comId(UUID id) {
        this.id = id;
        return this;
    }

    /**
     * Define o código SKU único do combo/kit.
     *
     * <p>Objetivo Funcional: Identificar o pacote promocional no catálogo.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param sku código SKU (3 a 64 caracteres)
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comSku(String sku) {
        this.sku = sku;
        return this;
    }

    /**
     * Define o nome comercial de exibição do combo/kit.
     *
     * <p>Objetivo Funcional: Nomear o pacote promocional na vitrine.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param nome nome do combo (até 180 caracteres)
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comNome(String nome) {
        this.nome = nome;
        return this;
    }

    /**
     * Define a descrição detalhada do combo/kit.
     *
     * <p>Objetivo Funcional: Explicar a composição comercial do kit.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param descricao descrição textual
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    /**
     * Define a categoria principal à qual o combo/kit está vinculado.
     *
     * <p>Objetivo Funcional: Associar o combo a um nó da árvore de categorias.</p>
     * <p>Papel no Design Pattern: Passo fluente obrigatório do <b>Builder</b>.</p>
     *
     * @param categoriaPrincipalId UUID da categoria
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder naCategoria(UUID categoriaPrincipalId) {
        this.categoriaPrincipalId = categoriaPrincipalId;
        return this;
    }

    /**
     * Configura o combo para calcular seu preço pela soma recursiva dos componentes com percentual de desconto.
     *
     * <p>Objetivo Funcional: Definir o modo {@link ModoPrecoCombo#SOMA_RECURSIVA_COMPONENTES} (FR-008).</p>
     * <p>Papel no Design Pattern: Passo fluente de configuração da política de preço no <b>Builder</b>.</p>
     *
     * @param percentualDescontoKit percentual de desconto entre {@code 0.00} e {@code 90.00}
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comPrecoSomaRecursivaEDescontoPercentual(BigDecimal percentualDescontoKit) {
        this.modoPreco = ModoPrecoCombo.SOMA_RECURSIVA_COMPONENTES;
        this.percentualDescontoKit = percentualDescontoKit != null ? percentualDescontoKit : BigDecimal.ZERO;
        this.precoFixoOpcional = null;
        return this;
    }

    /**
     * Configura o combo para utilizar um preço promocional fixo dedicado.
     *
     * <p>Objetivo Funcional: Definir o modo {@link ModoPrecoCombo#PRECO_FIXO_DEDICADO} (FR-008).</p>
     * <p>Papel no Design Pattern: Passo fluente de configuração da política de preço no <b>Builder</b>.</p>
     *
     * @param precoFixo valor fixo dedicado estritamente maior que zero
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comPrecoFixoDedicado(BigDecimal precoFixo) {
        this.modoPreco = ModoPrecoCombo.PRECO_FIXO_DEDICADO;
        this.precoFixoOpcional = precoFixo;
        this.percentualDescontoKit = BigDecimal.ZERO;
        return this;
    }

    /**
     * Configura explicitamente o modo de preço, percentual de desconto e preço fixo opcional (para uso de DTOs).
     *
     * <p>Objetivo Funcional: Permitir configuração direta a partir dos parâmetros recebidos na API REST.</p>
     * <p>Papel no Design Pattern: Passo fluente parametrizado do <b>Builder</b>.</p>
     *
     * @param modoPreco modo de precificação escolhido
     * @param percentualDesconto percentual de desconto (quando em modo recursivo)
     * @param precoFixo preço fixo (quando em modo fixo dedicado)
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comConfiguracaoPreco(
        ModoPrecoCombo modoPreco,
        BigDecimal percentualDesconto,
        BigDecimal precoFixo
    ) {
        this.modoPreco = modoPreco;
        this.percentualDescontoKit = percentualDesconto != null ? percentualDesconto : BigDecimal.ZERO;
        this.precoFixoOpcional = precoFixo;
        return this;
    }

    /**
     * Adiciona um item componente ({@link ProdutoVariacaoLeaf} ou outro {@link ComboKitComposite}) ao kit.
     *
     * <p>Objetivo Funcional: Incluir um componente filho com sua respectiva quantidade por kit.</p>
     * <p>Papel no Design Pattern: Passo fluente de montagem da árvore <b>Composite</b> dentro do <b>Builder</b>.</p>
     *
     * @param componente instância de {@link ItemCatalogoComponent}
     * @param quantidadePorKit quantidade de unidades deste item por kit ({@code >= 1})
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder adicionarItemComponente(ItemCatalogoComponent componente, int quantidadePorKit) {
        this.entradasComponentes.add(new EntradaComponente(componente, quantidadePorKit));
        return this;
    }

    /**
     * Define se o combo nasce ativo no catálogo.
     *
     * <p>Objetivo Funcional: Controlar estado inicial de comercialização.</p>
     * <p>Papel no Design Pattern: Passo fluente opcional do <b>Builder</b>.</p>
     *
     * @param ativo {@code true} para ativo; {@code false} para inativo
     * @return a própria instância de {@link ComboKitBuilder}
     */
    public ComboKitBuilder comAtivo(boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    /**
     * Valida todas as invariantes de composição e precificação e constrói o {@link ComboKitComposite}.
     *
     * <p>Objetivo Funcional: Impedir criação de combos vazios, com descontos fora da faixa {@code 0..90%},
     * com preço fixo inválido ou com ciclos hierárquicos.</p>
     * <p>Papel no Design Pattern: Método finalizador ({@code build()}) do padrão <b>Builder</b>.</p>
     *
     * @return instância íntegra e validada de {@link ComboKitComposite}
     * @throws RegraNegocioModulo1Exception se qualquer invariante for violada
     */
    public ComboKitComposite construir() {
        if (this.sku == null || this.sku.isBlank() || this.sku.trim().length() < 3 || this.sku.trim().length() > 64) {
            throw new RegraNegocioModulo1Exception(
                "O SKU do combo/kit é obrigatório e deve ter entre 3 e 64 caracteres."
            );
        }
        if (this.nome == null || this.nome.isBlank() || this.nome.trim().length() > 180) {
            throw new RegraNegocioModulo1Exception(
                "O nome do combo/kit é obrigatório e deve ter até 180 caracteres."
            );
        }
        if (this.descricao == null || this.descricao.isBlank()) {
            throw new RegraNegocioModulo1Exception("A descrição do combo/kit é obrigatória.");
        }
        if (this.categoriaPrincipalId == null) {
            throw new RegraNegocioModulo1Exception(
                "O combo/kit deve estar vinculado a uma categoria principal."
            );
        }
        if (this.modoPreco == null) {
            throw new RegraNegocioModulo1Exception("O modo de precificação do combo/kit é obrigatório.");
        }
        if (this.entradasComponentes.isEmpty()) {
            throw new RegraNegocioModulo1Exception(
                "O combo/kit deve conter ao menos um componente comercializável."
            );
        }
        int totalUnidadesNoKit = this.entradasComponentes.stream()
            .mapToInt(EntradaComponente::quantidadePorKit)
            .sum();
        if (totalUnidadesNoKit < 2) {
            throw new RegraNegocioModulo1Exception(
                "Um combo/kit promocional deve agrupar no mínimo 2 unidades somadas de componentes."
            );
        }

        if (this.modoPreco == ModoPrecoCombo.SOMA_RECURSIVA_COMPONENTES) {
            if (this.percentualDescontoKit == null
                || this.percentualDescontoKit.compareTo(BigDecimal.ZERO) < 0
                || this.percentualDescontoKit.compareTo(new BigDecimal("90.00")) > 0) {
                throw new RegraNegocioModulo1Exception(
                    "O percentual de desconto do combo deve estar entre 0.00% e 90.00%."
                );
            }
        } else if (this.modoPreco == ModoPrecoCombo.PRECO_FIXO_DEDICADO) {
            if (this.precoFixoOpcional == null || this.precoFixoOpcional.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegraNegocioModulo1Exception(
                    "No modo PRECO_FIXO_DEDICADO, o preço fixo deve ser obrigatório e maior que zero."
                );
            }
        }

        ComboKitComposite combo = new ComboKitComposite(
            this.id,
            this.sku,
            this.nome,
            this.descricao,
            this.categoriaPrincipalId,
            this.modoPreco,
            this.percentualDescontoKit,
            this.precoFixoOpcional,
            this.ativo
        );

        for (EntradaComponente entrada : this.entradasComponentes) {
            combo.adicionarComponente(entrada.componente(), entrada.quantidadePorKit());
        }

        return combo;
    }

    /**
     * Registro auxiliar interno para armazenar os pares (componente, quantidade) durante a montagem.
     *
     * @param componente item filho comercializável
     * @param quantidadePorKit unidades por kit
     */
    private record EntradaComponente(ItemCatalogoComponent componente, int quantidadePorKit) {
    }
}
