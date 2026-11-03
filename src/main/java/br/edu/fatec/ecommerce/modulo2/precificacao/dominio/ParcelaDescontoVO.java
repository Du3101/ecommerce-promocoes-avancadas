package br.edu.fatec.ecommerce.modulo2.precificacao.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object imutável que discrimina uma parcela específica de desconto concedido.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Item de auditoria e transparência gerado por cada camada do padrão <i>Decorator</i>
 * ({@link DescontoDecoratorAbstrato}), garantindo a rastreabilidade da memória de cálculo
 * exigida na Constituição (Princípio IV).
 *
 * @param origem Origem do benefício ("PROMOCAO" ou "CUPOM").
 * @param nomeBeneficio Nome comercial legível da promoção ou cupom.
 * @param valorAbatido Valor monetário exato abatido por esta parcela.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public record ParcelaDescontoVO(
        String origem,
        String nomeBeneficio,
        BigDecimal valorAbatido
) {
    public ParcelaDescontoVO {
        Objects.requireNonNull(origem, "origem não pode ser nula");
        Objects.requireNonNull(nomeBeneficio, "nomeBeneficio não pode ser nulo");
        valorAbatido = OperacoesMonetarias.normalizar(valorAbatido);
    }
}

