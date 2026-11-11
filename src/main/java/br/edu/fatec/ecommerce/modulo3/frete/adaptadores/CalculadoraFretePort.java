package br.edu.fatec.ecommerce.modulo3.frete.adaptadores;

import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import java.math.BigDecimal;

/**
 * Porta de integração logística (Interface de Adaptação) para cotações de frete.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Interface <i>Target</i> do padrão <b>Adapter</b> (GoF Adapter). Define o contrato unificado
 * para comunicação com os provedores simulados de entrega (Correios PAC, SEDEX e Transportadora).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
public interface CalculadoraFretePort {

    /**
     * Verifica se este adaptador suporta a modalidade solicitada.
     *
     * @param modalidade Modalidade de frete a verificar.
     * @return {@code true} se o adaptador processa a modalidade.
     */
    boolean suporta(ModalidadeFrete modalidade);

    /**
     * Executa a cotação de valor e prazo de entrega.
     *
     * @param cepDestino CEP de 8 dígitos numéricos.
     * @param pesoTotalGramas Peso acumulado em gramas.
     * @param subtotalLiquido Subtotal líquido dos itens para análise de frete grátis.
     * @return Cotação calculada.
     */
    CotacaoFrete calcularFrete(String cepDestino, int pesoTotalGramas, BigDecimal subtotalLiquido);
}
