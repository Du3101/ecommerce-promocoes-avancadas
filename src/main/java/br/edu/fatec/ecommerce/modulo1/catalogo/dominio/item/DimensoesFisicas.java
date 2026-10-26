package br.edu.fatec.ecommerce.modulo1.catalogo.dominio.item;

import br.edu.fatec.ecommerce.compartilhado.excecao.RegraNegocioModulo1Exception;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável que representa o peso em gramas e as dimensões físicas tridimensionais
 * (altura, largura e profundidade em centímetros) de um item do catálogo ou combo (FR-005, FR-008).
 *
 * <p>Aplicação do Padrão GoF / Catálogo: <b>Static Factory Method</b> (Princípio II da Constituição).</p>
 * <p>Justificativa: O construtor é privado/protegido e a criação ocorre exclusivamente pelo método
 * estático semântico {@link #emCentimetrosEGramas(int, BigDecimal, BigDecimal, BigDecimal)}, que
 * impede a criação de itens físicos com peso ou dimensões nulas, negativas ou zeradas.</p>
 */
@Embeddable
public class DimensoesFisicas {

    @Column(name = "peso_gramas", nullable = false)
    private int pesoGramas;

    @Column(name = "altura_cm", precision = 8, scale = 2, nullable = false)
    private BigDecimal alturaCm;

    @Column(name = "largura_cm", precision = 8, scale = 2, nullable = false)
    private BigDecimal larguraCm;

    @Column(name = "profundidade_cm", precision = 8, scale = 2, nullable = false)
    private BigDecimal profundidadeCm;

    /**
     * Construtor protegido para uso exclusivo do JPA/Hibernate.
     *
     * <p>Objetivo Funcional: Permitir hidratação do Value Object embutido pelo ORM.</p>
     * <p>Papel no Design Pattern: Oculta a construção direta em favor do <b>Static Factory Method</b>.</p>
     */
    protected DimensoesFisicas() {
        // Uso exclusivo do JPA
    }

    /**
     * Construtor privado acionado pelos Static Factory Methods após validação das medidas.
     *
     * <p>Objetivo Funcional: Inicializar os atributos físicos imutáveis com escala decimal padronizada.</p>
     * <p>Papel no Design Pattern: Construtor interno encapsulado pelo <b>Static Factory Method</b>.</p>
     *
     * @param pesoGramas peso positivo em gramas
     * @param alturaCm altura positiva em centímetros
     * @param larguraCm largura positiva em centímetros
     * @param profundidadeCm profundidade positiva em centímetros
     */
    private DimensoesFisicas(
        int pesoGramas,
        BigDecimal alturaCm,
        BigDecimal larguraCm,
        BigDecimal profundidadeCm
    ) {
        this.pesoGramas = pesoGramas;
        this.alturaCm = alturaCm.setScale(2, RoundingMode.HALF_UP);
        this.larguraCm = larguraCm.setScale(2, RoundingMode.HALF_UP);
        this.profundidadeCm = profundidadeCm.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cria uma instância validada de {@link DimensoesFisicas} expressa em centímetros e gramas.
     *
     * <p>Objetivo Funcional: Garantir que todo item físico cadastrado no catálogo possua peso e
     * medidas estritamente positivas para cálculo de frete pelo Módulo 3.</p>
     * <p>Papel no Design Pattern: <b>Static Factory Method</b> explícito com nome autodocumentado
     * das unidades de medida.</p>
     *
     * @param pesoGramas peso em gramas ({@code > 0})
     * @param alturaCm altura em centímetros ({@code > 0})
     * @param larguraCm largura em centímetros ({@code > 0})
     * @param profundidadeCm profundidade em centímetros ({@code > 0})
     * @return instância imutável e validada de {@link DimensoesFisicas}
     * @throws RegraNegocioModulo1Exception se qualquer medida for nula, zero ou negativa
     */
    public static DimensoesFisicas emCentimetrosEGramas(
        int pesoGramas,
        BigDecimal alturaCm,
        BigDecimal larguraCm,
        BigDecimal profundidadeCm
    ) {
        if (pesoGramas <= 0) {
            throw new RegraNegocioModulo1Exception(
                "O peso físico do item deve ser estritamente maior que zero gramas."
            );
        }
        validarMedidaPositiva(alturaCm, "altura (cm)");
        validarMedidaPositiva(larguraCm, "largura (cm)");
        validarMedidaPositiva(profundidadeCm, "profundidade (cm)");

        return new DimensoesFisicas(pesoGramas, alturaCm, larguraCm, profundidadeCm);
    }

    /**
     * Calcula o volume cúbico total da embalagem em centímetros cúbicos ($\text{cm}^3$).
     *
     * <p>Objetivo Funcional: Apoiar cálculo de cubagem logística no transporte.</p>
     * <p>Papel no Design Pattern: Operação derivada sobre o Value Object imutável.</p>
     *
     * @return volume em $\text{cm}^3$ com 2 casas decimais
     */
    public BigDecimal calcularVolumeCubicoCm3() {
        return this.alturaCm
            .multiply(this.larguraCm)
            .multiply(this.profundidadeCm)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Obtém o peso físico em gramas.
     *
     * <p>Objetivo Funcional: Informar peso para cálculo de frete.</p>
     * <p>Papel no Design Pattern: Getter explícito do Value Object.</p>
     *
     * @return peso em gramas
     */
    public int obterPesoGramas() {
        return this.pesoGramas;
    }

    /**
     * Obtém a altura em centímetros.
     *
     * <p>Objetivo Funcional: Informar altura física da embalagem.</p>
     * <p>Papel no Design Pattern: Getter explícito do Value Object.</p>
     *
     * @return altura em centímetros
     */
    public BigDecimal obterAlturaCm() {
        return this.alturaCm;
    }

    /**
     * Obtém a largura em centímetros.
     *
     * <p>Objetivo Funcional: Informar largura física da embalagem.</p>
     * <p>Papel no Design Pattern: Getter explícito do Value Object.</p>
     *
     * @return largura em centímetros
     */
    public BigDecimal obterLarguraCm() {
        return this.larguraCm;
    }

    /**
     * Obtém a profundidade em centímetros.
     *
     * <p>Objetivo Funcional: Informar profundidade física da embalagem.</p>
     * <p>Papel no Design Pattern: Getter explícito do Value Object.</p>
     *
     * @return profundidade em centímetros
     */
    public BigDecimal obterProfundidadeCm() {
        return this.profundidadeCm;
    }

    /**
     * Valida se uma dimensão em centímetros é não nula e estritamente maior que zero.
     *
     * <p>Objetivo Funcional: Rejeitar medidas zeradas ou negativas antes de construir o Value Object.</p>
     * <p>Papel no Design Pattern: Validador interno do <b>Static Factory Method</b>.</p>
     *
     * @param valor valor decimal da medida
     * @param nomeCampo nome descritivo da medida para mensagem de erro
     */
    private static void validarMedidaPositiva(BigDecimal valor, String nomeCampo) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioModulo1Exception(
                "A medida física '" + nomeCampo + "' é obrigatória e deve ser maior que zero."
            );
        }
    }

    /**
     * Compara igualdade por valor de todas as medidas físicas.
     *
     * <p>Objetivo Funcional: Garantir semântica de Value Object em testes e coleções.</p>
     * <p>Papel no Design Pattern: Igualdade estrutural de Value Object.</p>
     *
     * @param outro objeto a comparar
     * @return {@code true} se peso e dimensões forem iguais
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof DimensoesFisicas that)) {
            return false;
        }
        return this.pesoGramas == that.pesoGramas
            && this.alturaCm.compareTo(that.alturaCm) == 0
            && this.larguraCm.compareTo(that.larguraCm) == 0
            && this.profundidadeCm.compareTo(that.profundidadeCm) == 0;
    }

    /**
     * Calcula código hash baseado no peso e nas dimensões.
     *
     * <p>Objetivo Funcional: Consistência com {@link #equals(Object)}.</p>
     * <p>Papel no Design Pattern: Hash de Value Object.</p>
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.pesoGramas, this.alturaCm, this.larguraCm, this.profundidadeCm);
    }
}

