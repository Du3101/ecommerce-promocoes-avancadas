package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Locale;
import java.util.UUID;

/**
 * Associação quantificada entre um {@link ComboKitComposite} e um {@link ItemCatalogoComponent} filho
 * (que pode ser uma folha {@link ProdutoVariacaoLeaf} ou outro {@link ComboKitComposite} aninhado) (FR-008).
 *
 * <p>Papel no Design Pattern: Aresta ponderada ({@code quantidadePorKit >= 1}) entre o nó
 * <i>Composite</i> e seus filhos no padrão <b>Composite</b>.</p>
 */
@Entity
@Table(name = "combo_itens_componentes")
public class ItemComponenteKit {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combo_id", nullable = false)
    private ComboKitComposite combo;

    @Column(name = "componente_sku", length = 64, nullable = false)
    private String componenteSku;

    @Column(name = "quantidade_por_kit", nullable = false)
    private int quantidadePorKit;

    @Transient
    private ItemCatalogoComponent componenteResolvido;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação da associação pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Preserva construção validada.</p>
     */
    protected ItemComponenteKit() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor explícito que vincula um componente filho e sua quantidade por kit.
     *
     * <p>Objetivo Funcional: Garantir que todo item participante de um combo possua referência válida
     * e multiplicador inteiro estritamente positivo ({@code >= 1}).</p>
     * <p>Papel no Design Pattern: Representa a ligação estrutural pai-filho no padrão <b>Composite</b>.</p>
     *
     * @param componente item filho ({@link ProdutoVariacaoLeaf} ou {@link ComboKitComposite})
     * @param quantidadePorKit unidades deste componente necessárias para formar 1 combo ({@code >= 1})
     */
    public ItemComponenteKit(ItemCatalogoComponent componente, int quantidadePorKit) {
        if (componente == null || componente.obterSku() == null || componente.obterSku().isBlank()) {
            throw new RegraNegocioModulo1Exception(
                "O componente filho do combo/kit é obrigatório e deve possuir SKU válido."
            );
        }
        if (quantidadePorKit < 1) {
            throw new RegraNegocioModulo1Exception(
                "A quantidade por kit do componente '" + componente.obterSku() + "' deve ser no mínimo 1."
            );
        }
        this.id = UUID.randomUUID();
        this.componenteSku = componente.obterSku().trim().toUpperCase(Locale.ROOT);
        this.quantidadePorKit = quantidadePorKit;
        this.componenteResolvido = componente;
    }

    /**
     * Define a referência ao combo pai durante a montagem do agregado.
     *
     * <p>Objetivo Funcional: Sincronizar chave estrangeira {@code combo_id} para persistência JPA.</p>
     * <p>Papel no Design Pattern: Manutenção de integridade relacional do <b>Composite</b>.</p>
     *
     * @param combo instância do {@link ComboKitComposite} pai
     */
    void vincularAoCombo(ComboKitComposite combo) {
        this.combo = combo;
    }

    /**
     * Hidrata em memória a referência polimórfica ao {@link ItemCatalogoComponent} quando carregado do banco.
     *
     * <p>Objetivo Funcional: Reconectar a árvore de objetos em memória após leitura JPA.</p>
     * <p>Papel no Design Pattern: Resolução polimórfica do filho (<i>Leaf</i> ou <i>Composite</i>).</p>
     *
     * @param componente instância resolvida de {@link ItemCatalogoComponent}
     */
    public void vincularComponenteResolvido(ItemCatalogoComponent componente) {
        if (componente == null) {
            throw new RegraNegocioModulo1Exception(
                "Não foi possível resolver o componente de SKU: " + this.componenteSku
            );
        }
        this.componenteResolvido = componente;
    }

    /**
     * Obtém o identificador único do vínculo de componente.
     *
     * <p>Objetivo Funcional: Identificar o registro na tabela {@code combo_itens_componentes}.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade associativa.</p>
     *
     * @return UUID do vínculo
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o código SKU do componente filho.
     *
     * <p>Objetivo Funcional: Identificar qual SKU integra o kit.</p>
     * <p>Papel no Design Pattern: Chave de referência ao filho no <b>Composite</b>.</p>
     *
     * @return código SKU do componente
     */
    public String obterComponenteSku() {
        return this.componenteSku;
    }

    /**
     * Obtém a quantidade de unidades deste componente exigida por cada unidade do combo.
     *
     * <p>Objetivo Funcional: Multiplicar preço, peso e reserva de estoque proporcionalmente.</p>
     * <p>Papel no Design Pattern: Peso multiplicador da aresta no padrão <b>Composite</b>.</p>
     *
     * @return quantidade por kit ({@code >= 1})
     */
    public int obterQuantidadePorKit() {
        return this.quantidadePorKit;
    }

    /**
     * Obtém a referência polimórfica ao componente filho ({@link ItemCatalogoComponent}).
     *
     * <p>Objetivo Funcional: Permitir delegação recursiva de preço, peso, dimensões e estoque.</p>
     * <p>Papel no Design Pattern: Acesso ao objeto filho no padrão <b>Composite</b>.</p>
     *
     * @return instância de {@link ItemCatalogoComponent}
     * @throws RegraNegocioModulo1Exception se o componente ainda não tiver sido resolvido em memória
     */
    public ItemCatalogoComponent obterComponente() {
        if (this.componenteResolvido == null) {
            throw new RegraNegocioModulo1Exception(
                "O componente filho '" + this.componenteSku + "' do combo precisa estar resolvido em memória."
            );
        }
        return this.componenteResolvido;
    }
}

