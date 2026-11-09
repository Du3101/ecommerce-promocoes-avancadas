package br.edu.fatec.ecommerce.modulo2.carrinho.dominio;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.visitor.ElementoCarrinhoVisitor;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade base abstrata que representa uma linha de item persistente no carrinho de compras.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Serve como base polimórfica para os elementos visitáveis do padrão <i>Visitor</i>
 * ({@link ElementoCarrinhoComponent}), implementando o contrato comum de cálculo monetário
 * via {@link OperacoesMonetarias} e delegando o segundo despacho do <i>Double Dispatch</i>
 * para as subclasses concretas {@link ItemCarrinhoProduto} e {@link ItemCarrinhoComboKit}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Entity
@Table(name = "itens_carrinho")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_item", discriminatorType = DiscriminatorType.STRING)
public abstract class ItemCarrinho implements ElementoCarrinhoComponent {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "carrinho_id", nullable = false, insertable = false, updatable = false)
    private UUID carrinhoId;

    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Column(name = "nome", nullable = false, length = 200)
    private String nome;

    @Column(name = "preco_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "quantidade", nullable = false)
    private int quantidade;

    @Column(name = "peso_gramas", nullable = false)
    private int pesoGramas;

    @Column(name = "categoria_id")
    private UUID categoriaId;

    /**
     * Construtor padrão protegido exigido pelo JPA.
     */
    protected ItemCarrinho() {
    }

    /**
     * Construtor protegido com todos os parâmetros essenciais do item.
     *
     * <p>Objetivo funcional: Inicializa uma linha de item validada para o carrinho.
     * <p>Papel no padrão: Inicialização base para os nós visitáveis do padrão Visitor.
     *
     * @param id Identificador único da linha.
     * @param carrinhoId Identificador do carrinho proprietário.
     * @param sku Código de referência do catálogo.
     * @param nome Descrição comercial do item.
     * @param precoUnitario Preço unitário vigente.
     * @param quantidade Quantidade desejada (mínimo 1).
     * @param pesoGramas Peso unitário em gramas.
     * @param categoriaId Identificador da categoria no catálogo.
     */
    protected ItemCarrinho(
            UUID id,
            UUID carrinhoId,
            String sku,
            String nome,
            BigDecimal precoUnitario,
            int quantidade,
            int pesoGramas,
            UUID categoriaId
    ) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade no carrinho deve ser estritamente positiva.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.carrinhoId = Objects.requireNonNull(carrinhoId, "carrinhoId não pode ser nulo");
        this.sku = Objects.requireNonNull(sku, "SKU não pode ser nulo").trim();
        this.nome = Objects.requireNonNull(nome, "Nome não pode ser nulo").trim();
        this.precoUnitario = OperacoesMonetarias.normalizar(precoUnitario);
        this.quantidade = quantidade;
        this.pesoGramas = Math.max(0, pesoGramas);
        this.categoriaId = categoriaId;
    }

    @Override
    public abstract void aceitar(ElementoCarrinhoVisitor visitor);

    @Override
    public BigDecimal calcularSubtotalItem() {
        return OperacoesMonetarias.normalizar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    @Override
    public int calcularPesoTotalLinhaGramas() {
        return pesoGramas * quantidade;
    }

    /**
     * Altera a quantidade de unidades deste item no carrinho.
     *
     * <p>Objetivo funcional: Permite atualizar a quantidade comprada na mutação do carrinho.
     * <p>Papel no padrão: Mutação controlada de estado com validação de invariante.
     *
     * @param novaQuantidade Nova quantidade inteira (deve ser maior que zero).
     */
    public void atualizarQuantidade(int novaQuantidade) {
        if (novaQuantidade <= 0) {
            throw new IllegalArgumentException("Nova quantidade deve ser estritamente maior que zero.");
        }
        this.quantidade = novaQuantidade;
    }

    /**
     * Incrementa a quantidade atual de unidades deste item.
     *
     * <p>Objetivo funcional: Facilita a adição repetida do mesmo SKU no carrinho.
     *
     * @param incremento Quantidade a ser somada.
     */
    public void incrementarQuantidade(int incremento) {
        if (incremento <= 0) {
            throw new IllegalArgumentException("Incremento de quantidade deve ser positivo.");
        }
        this.quantidade += incremento;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCarrinhoId() {
        return carrinhoId;
    }

    public void setCarrinhoId(UUID carrinhoId) {
        this.carrinhoId = carrinhoId;
    }

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    @Override
    public int getQuantidade() {
        return quantidade;
    }

    public int getPesoGramas() {
        return pesoGramas;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }
}
