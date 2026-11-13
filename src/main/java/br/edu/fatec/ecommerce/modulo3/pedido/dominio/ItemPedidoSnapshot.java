package br.edu.fatec.ecommerce.modulo3.pedido.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que registra a linha comercial imutável de um item pertencente a um Pedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Componente folha do <i>Snapshot Comercial Imutável</i> (Constituição, Princípio IV).
 * Preserva os preços praticados, descontos unitários rateados e dados físicos vigentes no instante
 * do checkout, garantindo que alterações no catálogo do Módulo 1 não alterem pedidos passados.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Entity
@Table(name = "itens_pedido_snapshot")
public class ItemPedidoSnapshot {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "tipo_item", nullable = false, length = 40)
    private String tipoItem;

    @Column(name = "preco_unitario_tabela", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoUnitarioTabela;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Column(name = "desconto_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal descontoUnitario;

    @Column(name = "subtotal_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalLiquido;

    @Column(name = "peso_total_gramas", nullable = false)
    private int pesoTotalGramas;

    /**
     * Construtor padrão protegido exigido pelo JPA.
     */
    protected ItemPedidoSnapshot() {
    }

    /**
     * Construtor completo explícito para instanciação de linha imutável de pedido.
     *
     * @param id Identificador único da linha.
     * @param pedido Pedido ao qual esta linha pertence.
     * @param sku Código SKU do produto ou combo.
     * @param nome Nome ou descrição do produto no instante da compra.
     * @param tipoItem Categoria estrutural ("PRODUTO" ou "COMBO_KIT").
     * @param precoUnitarioTabela Preço unitário original sem descontos.
     * @param quantidade Número de unidades adquiridas.
     * @param descontoUnitario Parcela de desconto concedida por unidade.
     * @param pesoTotalGramas Peso total acumulado da linha em gramas.
     */
    public ItemPedidoSnapshot(
            UUID id,
            Pedido pedido,
            String sku,
            String nome,
            String tipoItem,
            BigDecimal precoUnitarioTabela,
            int quantidade,
            BigDecimal descontoUnitario,
            int pesoTotalGramas) {
        this.id = id != null ? id : UUID.randomUUID();
        this.pedido = pedido;
        this.sku = Objects.requireNonNull(sku, "SKU é obrigatório.");
        this.nome = Objects.requireNonNull(nome, "Nome do item é obrigatório.");
        this.tipoItem = tipoItem != null ? tipoItem : "PRODUTO";
        this.precoUnitarioTabela = precoUnitarioTabela.setScale(2, RoundingMode.HALF_EVEN);
        this.quantidade = Math.max(1, quantidade);
        this.descontoUnitario = (descontoUnitario != null ? descontoUnitario : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);
        this.pesoTotalGramas = Math.max(0, pesoTotalGramas);

        BigDecimal precoLiquidoUnitario = this.precoUnitarioTabela.subtract(this.descontoUnitario);
        if (precoLiquidoUnitario.compareTo(BigDecimal.ZERO) < 0) {
            precoLiquidoUnitario = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        this.subtotalLiquido = precoLiquidoUnitario.multiply(BigDecimal.valueOf(this.quantidade)).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Associa o pedido pai a esta linha de snapshot.
     *
     * @param pedido Pedido proprietário.
     */
    public void associarPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    /**
     * Retorna o ID único da linha.
     *
     * @return UUID da linha.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Retorna o Pedido pai.
     *
     * @return Pedido pai.
     */
    public Pedido getPedido() {
        return pedido;
    }

    /**
     * Retorna o SKU.
     *
     * @return SKU.
     */
    public String getSku() {
        return sku;
    }

    /**
     * Retorna o nome do item.
     *
     * @return Nome congelado.
     */
    public String getNome() {
        return nome;
    }

    /**
     * Retorna o tipo de item ("PRODUTO" ou "COMBO_KIT").
     *
     * @return Tipo estrutural.
     */
    public String getTipoItem() {
        return tipoItem;
    }

    /**
     * Retorna o preço de tabela praticado no momento da compra.
     *
     * @return Preço de tabela.
     */
    public BigDecimal getPrecoUnitarioTabela() {
        return precoUnitarioTabela;
    }

    /**
     * Retorna a quantidade de itens.
     *
     * @return Quantidade.
     */
    public int getQuantidade() {
        return quantidade;
    }

    /**
     * Retorna o desconto rateado por unidade.
     *
     * @return Desconto unitário.
     */
    public BigDecimal getDescontoUnitario() {
        return descontoUnitario;
    }

    /**
     * Retorna o subtotal líquido faturado por esta linha.
     *
     * @return Subtotal líquido final.
     */
    public BigDecimal getSubtotalLiquido() {
        return subtotalLiquido;
    }

    /**
     * Retorna o peso total em gramas desta linha.
     *
     * @return Peso em gramas.
     */
    public int getPesoTotalGramas() {
        return pesoTotalGramas;
    }
}
