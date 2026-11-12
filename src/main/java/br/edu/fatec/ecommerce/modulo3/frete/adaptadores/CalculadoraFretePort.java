package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;

/**
 * Porta interna de cotação de frete.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> interface <i>Target</i> do padrão <b>Adapter</b>. O domínio só
 * conhece este contrato; cada transportadora tem um adaptador que traduz sua API proprietária para ele
 * (Anti-Corruption Layer).
 *
 * <p><b>Guia de extensão:</b> nova transportadora = nova constante em {@link ModalidadeFrete} + nova classe
 * {@code @Component} implementando esta interface. O {@code ServicoCalculoFrete} a descobre por injeção.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface CalculadoraFretePort {

    /**
     * Modalidade atendida por este adaptador.
     *
     * @return modalidade de frete
     */
    ModalidadeFrete modalidade();

    /**
     * Cota a entrega já traduzida para o modelo interno, sem aplicar benefícios comerciais.
     *
     * @param cepDestino CEP de destino (8 dígitos, com ou sem máscara)
     * @param pesoTotalGramas peso total em gramas
     * @return cotação no modelo interno ({@link CotacaoFrete})
     */
    CotacaoFrete cotar(String cepDestino, int pesoTotalGramas);
}
