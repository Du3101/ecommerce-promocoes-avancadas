package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.flyweight;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;

/**
 * Objeto imutável que encapsula o estado intrínseco (compartilhável) de um atributo de variação
 * de produto, como {@code (TAMANHO, "M")} ou {@code (COR, "Preto")} (FR-006).
 *
 * <p>Aplicação do Padrão GoF: <b>Flyweight</b> — papel <i>ConcreteFlyweight</i> (Princípio II da Constituição).</p>
 * <p>Justificativa: Em um catálogo com milhares de SKUs, os pares {@code (tipo, valor, codigoApresentacao)}
 * repetem-se intensamente. Enquanto o estado extrínseco (SKU específico, preço adicional, saldo em estoque)
 * fica em {@code ProdutoVariacaoLeaf}, o estado intrínseco é imutável e compartilhado pela
 * {@link FabricaAtributoVariacaoFlyweight}.</p>
 */
@Entity
@Table(
    name = "atributos_variacao_flyweight",
    uniqueConstraints = @UniqueConstraint(name = "uk_flyweight_tipo_valor", columnNames = {"tipo", "valor"})
)
public class AtributoVariacaoFlyweight {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 40, nullable = false, updatable = false)
    private TipoAtributoVariacao tipo;

    @Column(name = "valor", length = 80, nullable = false, updatable = false)
    private String valor;

    @Column(name = "codigo_apresentacao", length = 80, nullable = false, updatable = false)
    private String codigoApresentacao;

    /**
     * Construtor protegido para uso exclusivo do provedor JPA.
     *
     * <p>Objetivo Funcional: Permitir reconstrução da entidade pelo Hibernate.</p>
     * <p>Papel no Design Pattern: Impede instanciação avulsa fora da {@link FabricaAtributoVariacaoFlyweight}.</p>
     */
    protected AtributoVariacaoFlyweight() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor de pacote utilizado exclusivamente pela {@link FabricaAtributoVariacaoFlyweight}.
     *
     * <p>Objetivo Funcional: Inicializar uma instância intrínseca imutável de atributo de variação.</p>
     * <p>Papel no Design Pattern: Instancia o <i>ConcreteFlyweight</i> controlado pelo pool da fábrica.</p>
     *
     * @param id UUID único do atributo
     * @param tipo classificação do atributo ({@link TipoAtributoVariacao})
     * @param valor valor nominal padronizado (ex.: "GG", "Preto")
     * @param codigoApresentacao código auxiliar visual ou hexadecimal (ex.: "#000000", "TAM-GG")
     */
    AtributoVariacaoFlyweight(
        UUID id,
        TipoAtributoVariacao tipo,
        String valor,
        String codigoApresentacao
    ) {
        if (tipo == null) {
            throw new RegraNegocioModulo1Exception("O tipo do atributo Flyweight é obrigatório.");
        }
        if (valor == null || valor.isBlank() || valor.trim().length() > 80) {
            throw new RegraNegocioModulo1Exception(
                "O valor do atributo Flyweight é obrigatório e deve ter até 80 caracteres."
            );
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.tipo = tipo;
        this.valor = valor.trim();
        this.codigoApresentacao = (codigoApresentacao != null && !codigoApresentacao.isBlank())
            ? codigoApresentacao.trim()
            : this.valor;
    }

    /**
     * Obtém o identificador único do Flyweight.
     *
     * <p>Objetivo Funcional: Identificar a tupla intrínseca no banco e na memória.</p>
     * <p>Papel no Design Pattern: Getter explícito do estado intrínseco.</p>
     *
     * @return UUID do Flyweight
     */
    public UUID obterId() {
        return this.id;
    }

    /**
     * Obtém o tipo do atributo de variação.
     *
     * <p>Objetivo Funcional: Classificar a dimensão de variação (tamanho, cor, voltagem, etc.).</p>
     * <p>Papel no Design Pattern: Parte da chave intrínseca do <b>Flyweight</b>.</p>
     *
     * @return {@link TipoAtributoVariacao}
     */
    public TipoAtributoVariacao obterTipo() {
        return this.tipo;
    }

    /**
     * Obtém o valor textual do atributo de variação.
     *
     * <p>Objetivo Funcional: Informar o valor da característica (ex.: "Preto", "220V").</p>
     * <p>Papel no Design Pattern: Parte da chave intrínseca do <b>Flyweight</b>.</p>
     *
     * @return valor do atributo
     */
    public String obterValor() {
        return this.valor;
    }

    /**
     * Obtém o código de apresentação visual associado ao atributo.
     *
     * <p>Objetivo Funcional: Auxiliar renderização visual (ex.: código hexadecimal de cor ou sigla).</p>
     * <p>Papel no Design Pattern: Estado intrínseco compartilhado do <b>Flyweight</b>.</p>
     *
     * @return código de apresentação
     */
    public String obterCodigoApresentacao() {
        return this.codigoApresentacao;
    }

    /**
     * Formata uma etiqueta legível combinando o estado intrínseco com o SKU extrínseco.
     *
     * <p>Objetivo Funcional: Demonstrar a combinação entre estado intrínseco (guardado no Flyweight)
     * e estado extrínseco (informado pela variação/SKU que o utiliza).</p>
     * <p>Papel no Design Pattern: Operação clássica do <b>Flyweight</b> que recebe o estado extrínseco por parâmetro.</p>
     *
     * @param skuExtrinseco código SKU da variação que utiliza este atributo
     * @return descrição combinada {@code "[SKU] TIPO: VALOR (CODIGO)"}
     */
    public String formatarComContextoExtrinseco(String skuExtrinseco) {
        return String.format("[%s] %s: %s (%s)", skuExtrinseco, this.tipo.name(), this.valor, this.codigoApresentacao);
    }

    /**
     * Compara igualdade semântica baseada no par intrínseco {@code (tipo, valor)}.
     *
     * <p>Objetivo Funcional: Garantir unicidade lógica em coleções e conjuntos.</p>
     * <p>Papel no Design Pattern: Identidade por valor intrínseco do <b>Flyweight</b>.</p>
     *
     * @param outro objeto a comparar
     * @return {@code true} se possuir mesmo tipo e valor (ignorando caixa)
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof AtributoVariacaoFlyweight that)) {
            return false;
        }
        return this.tipo == that.tipo
            && this.valor != null
            && that.valor != null
            && this.valor.equalsIgnoreCase(that.valor);
    }

    /**
     * Calcula o código hash baseado no par intrínseco {@code (tipo, valor)}.
     *
     * <p>Objetivo Funcional: Suportar indexação consistente em mapas e conjuntos.</p>
     * <p>Papel no Design Pattern: Hash consistente com {@link #equals(Object)}.</p>
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.tipo, this.valor != null ? this.valor.toUpperCase() : "");
    }
}

