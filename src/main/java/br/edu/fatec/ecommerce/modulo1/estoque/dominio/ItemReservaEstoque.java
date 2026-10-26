package br.edu.fatec.ecommerce.modulo1.estoque.dominio;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.UUID;

/**
 * Linha individual de SKU físico e quantidade dentro de uma {@link ReservaEstoque} (FR-010).
 *
 * <p>Responsabilidade Comercial: Registrar exatamente quantas unidades físicas de cada SKU folha
 * foram retidas na reserva.</p>
 */
@Entity
@Table(name = "itens_reserva_estoque")
public class ItemReservaEstoque {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id", nullable = false)
    private ReservaEstoque reserva;

    @Column(name = "sku", length = 64, nullable = false)
    private String sku;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir hidratação pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Preserva construção controlada dentro do agregado {@link ReservaEstoque}.</p>
     */
    protected ItemReservaEstoque() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor explícito de pacote utilizado pelo agregado {@link ReservaEstoque}.
     *
     * <p>Objetivo Funcional: Criar linha validada de SKU reservado.</p>
     * <p>Papel no Design Pattern: Entidade interna do agregado construída via Static Factory Method de {@link ReservaEstoque}.</p>
     *
     * @param reserva referência à reserva pai
     * @param sku código SKU físico
     * @param quantidadeReservada quantidade reservada ({@code >= 1})
     */
    ItemReservaEstoque(ReservaEstoque reserva, String sku, int quantidadeReservada) {
        if (sku == null || sku.isBlank() || quantidadeReservada <= 0) {
            throw new RegraNegocioModulo1Exception(
                "Todo item da reserva deve possuir SKU válido e quantidade maior que zero."
            );
        }
        this.id = UUID.randomUUID();
        this.reserva = reserva;
        this.sku = sku.trim().toUpperCase(Locale.ROOT);
        this.quantidadeReservada = quantidadeReservada;
    }

    /**
     * Obtém o identificador único do item da reserva.
     *
     * <p>Objetivo Funcional: Identificar o registro na tabela {@code itens_reserva_estoque}.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return UUID do item de reserva
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o código SKU reservado.
     *
     * <p>Objetivo Funcional: Identificar qual SKU físico deve ser baixado ou estornado.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return código SKU
     */
    public String obterSku() {
        return this.sku;
    }

    /**
     * Obtém a quantidade de unidades reservadas para este SKU.
     *
     * <p>Objetivo Funcional: Informar a quantidade exata bloqueada na reserva.</p>
     * <p>Papel no Design Pattern: Getter explícito da entidade.</p>
     *
     * @return quantidade reservada ({@code >= 1})
     */
    public int obterQuantidadeReservada() {
        return this.quantidadeReservada;
    }
}

