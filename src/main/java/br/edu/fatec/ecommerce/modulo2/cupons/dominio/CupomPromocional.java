package br.edu.fatec.ecommerce.modulo2.cupons.dominio;

import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio que representa um cupom de desconto promocional.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a abstração de cupom comercial auditável, contendo regras de cota de utilização,
 * expiração temporal e cálculo de desconto via {@link OperacoesMonetarias}.
 * Interage com o padrão <i>Null Object</i> ({@link CupomNulo}) para representação polimórfica
 * quando o cliente não tiver inserido nenhum cupom.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Entity
@Table(name = "cupons_promocionais")
public class CupomPromocional {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, unique = true, length = 50)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_desconto", nullable = false, length = 40)
    private TipoDescontoCupom tipoDesconto;

    @Column(name = "valor_desconto", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "valor_minimo_carrinho", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorMinimoCarrinho;

    @Column(name = "quantidade_maxima_usos", nullable = false)
    private int quantidadeMaximaUsos;

    @Column(name = "quantidade_usos_atuais", nullable = false)
    private int quantidadeUsosAtuais;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "cumulativo_com_promocoes", nullable = false)
    private boolean cumulativoComPromocoes;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    /**
     * Construtor padrão protegido exigido pelo JPA.
     */
    protected CupomPromocional() {
    }

    /**
     * Construtor completo do cupom promocional.
     *
     * <p>Objetivo funcional: Criação validada de um novo cupom.
     * <p>Papel no padrão: Construtor explícito de entidade rica de domínio.
     *
     * @param id Identificador do cupom.
     * @param codigo Código alfanumérico digitado pelo cliente.
     * @param tipoDesconto Tipo do desconto (PERCENTUAL ou VALOR_FIXO).
     * @param valorDesconto Alíquota percentual ou valor nominal.
     * @param valorMinimoCarrinho Subtotal mínimo exigido para validade.
     * @param quantidadeMaximaUsos Limite de utilizações (0 = ilimitado).
     * @param expiraEm Data/hora limite de validade.
     * @param cumulativoComPromocoes Se pode acumular com outras promoções.
     * @param ativo Se o cupom está liberado para uso.
     */
    public CupomPromocional(
            UUID id,
            String codigo,
            TipoDescontoCupom tipoDesconto,
            BigDecimal valorDesconto,
            BigDecimal valorMinimoCarrinho,
            int quantidadeMaximaUsos,
            Instant expiraEm,
            boolean cumulativoComPromocoes,
            boolean ativo
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.codigo = Objects.requireNonNull(codigo, "Código do cupom não pode ser nulo").trim().toUpperCase();
        this.tipoDesconto = Objects.requireNonNull(tipoDesconto, "Tipo de desconto não pode ser nulo");
        this.valorDesconto = OperacoesMonetarias.normalizar(valorDesconto);
        this.valorMinimoCarrinho = OperacoesMonetarias.normalizar(valorMinimoCarrinho);
        this.quantidadeMaximaUsos = Math.max(0, quantidadeMaximaUsos);
        this.quantidadeUsosAtuais = 0;
        this.expiraEm = expiraEm;
        this.cumulativoComPromocoes = cumulativoComPromocoes;
        this.ativo = ativo;
        this.criadoEm = Instant.now();
    }

    /**
     * Verifica se o cupom está apto a ser aplicado considerando vigência, cota e valor mínimo.
     *
     * <p>Objetivo funcional: Valida as restrições de uso do cupom antes da precificação.
     * <p>Papel no padrão: Regra de negócio de integridade de cupom.
     *
     * @param agora Instante atual.
     * @param subtotalCarrinho Subtotal bruto atual do carrinho.
     * @return true se o cupom puder ser aplicado; false caso contrário.
     */
    public boolean isDisponivelParaUso(Instant agora, BigDecimal subtotalCarrinho) {
        if (!ativo) {
            return false;
        }
        if (expiraEm != null && agora.isAfter(expiraEm)) {
            return false;
        }
        if (quantidadeMaximaUsos > 0 && quantidadeUsosAtuais >= quantidadeMaximaUsos) {
            return false;
        }
        if (subtotalCarrinho != null && valorMinimoCarrinho.compareTo(BigDecimal.ZERO) > 0) {
            return subtotalCarrinho.compareTo(valorMinimoCarrinho) >= 0;
        }
        return true;
    }

    /**
     * Calcula o desconto nominal concedido por este cupom sobre um subtotal base.
     *
     * <p>Objetivo funcional: Executa a fórmula percentual ou nominal com piso zero.
     * <p>Papel no padrão: Cálculo monetário seguro via OperacoesMonetarias.
     *
     * @param subtotalBase Valor sobre o qual o cupom será aplicado.
     * @return BigDecimal normalizado com o desconto calculado.
     */
    public BigDecimal calcularDesconto(BigDecimal subtotalBase) {
        if (subtotalBase == null || subtotalBase.compareTo(BigDecimal.ZERO) <= 0) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }
        if (tipoDesconto == TipoDescontoCupom.PERCENTUAL) {
            return OperacoesMonetarias.calcularPercentual(subtotalBase, valorDesconto);
        } else {
            // Valor fixo limitado ao subtotal da base (piso zero)
            if (valorDesconto.compareTo(subtotalBase) > 0) {
                return OperacoesMonetarias.normalizar(subtotalBase);
            }
            return OperacoesMonetarias.normalizar(valorDesconto);
        }
    }

    /**
     * Registra o consumo de uma cota deste cupom de forma atômica/idempotente.
     *
     * <p>Objetivo funcional: Incrementa a contagem de usos do cupom.
     */
    public void registrarUso() {
        this.quantidadeUsosAtuais++;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoDescontoCupom getTipoDesconto() {
        return tipoDesconto;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public BigDecimal getValorMinimoCarrinho() {
        return valorMinimoCarrinho;
    }

    public int getQuantidadeMaximaUsos() {
        return quantidadeMaximaUsos;
    }

    public int getQuantidadeUsosAtuais() {
        return quantidadeUsosAtuais;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public boolean isCumulativoComPromocoes() {
        return cumulativoComPromocoes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}

