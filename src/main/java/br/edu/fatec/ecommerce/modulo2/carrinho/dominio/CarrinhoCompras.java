package br.edu.fatec.ecommerce.modulo2.carrinho.dominio;

import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Agregado raiz que gerencia o carrinho de compras de um cliente ou sessão de navegação.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa o <i>Aggregate Root</i> do subdomínio de carrinho. Responsável por garantir
 * a consistência transacional das mutações (adição, alteração e exclusão de itens),
 * coordenar o cálculo automático de totais, peso acumulado e integração com o cupom aplicado,
 * além de servir de base para o pipeline de decoração de preços ({@code PrecoCalculadoComponent}).
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Entity
@Table(name = "carrinhos")
public class CarrinhoCompras {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "cliente_id")
    private UUID clienteId;

    @Column(name = "sessao_id", length = 100)
    private String sessaoId;

    @Column(name = "cupom_codigo", length = 50)
    private String cupomCodigo;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "carrinho_id", nullable = false)
    private List<ItemCarrinho> itens = new ArrayList<>();

    /**
     * Construtor padrão protegido exigido pelo JPA.
     */
    protected CarrinhoCompras() {
    }

    /**
     * Construtor para novo carrinho de cliente ou sessão anônima.
     *
     * <p>Objetivo funcional: Inicializa um carrinho limpo vinculado a um cliente ou sessão.
     * <p>Papel no padrão: Construtor explícito de entidade raiz.
     *
     * @param id Identificador único do carrinho.
     * @param clienteId Identificador do cliente (opcional).
     * @param sessaoId Identificador da sessão anônima (opcional se clienteId presente).
     */
    public CarrinhoCompras(UUID id, UUID clienteId, String sessaoId) {
        if (clienteId == null && (sessaoId == null || sessaoId.isBlank())) {
            throw new IllegalArgumentException("O carrinho deve estar vinculado a um cliente ou a um identificador de sessão.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.clienteId = clienteId;
        this.sessaoId = sessaoId != null ? sessaoId.trim() : null;
        this.cupomCodigo = null;
        this.atualizadoEm = Instant.now();
        this.itens = new ArrayList<>();
    }

    /**
     * Adiciona um item ao carrinho ou incrementa sua quantidade caso o SKU já exista.
     *
     * <p>Objetivo funcional: Permite a inclusão rápida de produtos mantendo a unicidade de SKU por linha.
     * <p>Papel no padrão: Mutação com garantia de invariante do agregado.
     *
     * @param item Item a ser adicionado (produto ou combo).
     */
    public void adicionarOuIncrementarItem(ItemCarrinho item) {
        Objects.requireNonNull(item, "Item não pode ser nulo");
        item.setCarrinhoId(this.id);
        Optional<ItemCarrinho> itemExistente = buscarItemPorSku(item.getSku());

        if (itemExistente.isPresent()) {
            itemExistente.get().incrementarQuantidade(item.getQuantidade());
        } else {
            this.itens.add(item);
        }
        this.atualizadoEm = Instant.now();
    }

    /**
     * Atualiza a quantidade de unidades de um SKU existente no carrinho.
     *
     * <p>Objetivo funcional: Ajusta a quantidade informada pelo cliente no painel.
     *
     * @param sku Código do SKU a atualizar.
     * @param novaQuantidade Nova quantidade desejada (mínimo 1).
     */
    public void atualizarQuantidadeItem(String sku, int novaQuantidade) {
        Objects.requireNonNull(sku, "SKU não pode ser nulo");
        ItemCarrinho item = buscarItemPorSku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Item com SKU '" + sku + "' não encontrado no carrinho."));
        item.atualizarQuantidade(novaQuantidade);
        this.atualizadoEm = Instant.now();
    }

    /**
     * Remove um item do carrinho pelo SKU.
     *
     * <p>Objetivo funcional: Exclusão de produto indesejado.
     *
     * @param sku Código do SKU a ser excluído.
     * @return true se o item foi removido; false se não existia no carrinho.
     */
    public boolean removerItem(String sku) {
        if (sku == null) {
            return false;
        }
        boolean removido = this.itens.removeIf(item -> item.getSku().equalsIgnoreCase(sku.trim()));
        if (removido) {
            this.atualizadoEm = Instant.now();
        }
        return removido;
    }

    /**
     * Esvazia completamente todos os itens e remove o cupom aplicado do carrinho.
     *
     * <p>Objetivo funcional: Limpeza completa do carrinho (ex.: após checkout concluído).
     */
    public void esvaziar() {
        this.itens.clear();
        this.cupomCodigo = null;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Associa um código de cupom promocional a este carrinho.
     *
     * <p>Objetivo funcional: Aplica o código digitado pelo usuário.
     *
     * @param codigo Código do cupom (ou null para remover).
     */
    public void aplicarCupom(String codigo) {
        this.cupomCodigo = codigo != null && !codigo.isBlank() ? codigo.trim().toUpperCase() : null;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Remove o cupom atualmente associado ao carrinho.
     */
    public void removerCupom() {
        this.cupomCodigo = null;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Calcula o subtotal bruto acumulado pela soma de todos os itens do carrinho.
     *
     * <p>Objetivo funcional: Ponto de partida monetário para o pipeline de precificação.
     * <p>Papel no padrão: Operação de cálculo base antes da aplicação dos Decorators.
     *
     * @return BigDecimal normalizado com a soma dos subtotais de cada linha.
     */
    public BigDecimal calcularSubtotalBruto() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(item.calcularSubtotalItem());
        }
        return OperacoesMonetarias.normalizar(subtotal);
    }

    /**
     * Calcula a quantidade total de unidades físicas de itens presentes no carrinho.
     *
     * @return Total somado de quantidades inteiras.
     */
    public int calcularQuantidadeTotalItens() {
        int total = 0;
        for (ItemCarrinho item : itens) {
            total += item.getQuantidade();
        }
        return total;
    }

    /**
     * Calcula o peso total acumulado de todos os itens em gramas.
     *
     * <p>Objetivo funcional: Fornece o peso agregado necessário para o cálculo de frete no Módulo 3.
     *
     * @return Peso total em gramas.
     */
    public int calcularPesoTotalGramas() {
        int pesoTotal = 0;
        for (ItemCarrinho item : itens) {
            pesoTotal += item.calcularPesoTotalLinhaGramas();
        }
        return pesoTotal;
    }

    /**
     * Localiza um item no carrinho pelo SKU.
     *
     * @param sku SKU procurado.
     * @return Optional contendo o item se encontrado.
     */
    public Optional<ItemCarrinho> buscarItemPorSku(String sku) {
        if (sku == null) {
            return Optional.empty();
        }
        String skuNormalizado = sku.trim();
        return itens.stream()
                .filter(item -> item.getSku().equalsIgnoreCase(skuNormalizado))
                .findFirst();
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public void setClienteId(UUID clienteId) {
        this.clienteId = clienteId;
        this.atualizadoEm = Instant.now();
    }

    public String getSessaoId() {
        return sessaoId;
    }

    public String getCupomCodigo() {
        return cupomCodigo;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public List<ItemCarrinho> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
