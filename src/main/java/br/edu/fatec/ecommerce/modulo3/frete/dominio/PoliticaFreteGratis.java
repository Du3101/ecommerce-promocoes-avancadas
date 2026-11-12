package br.edu.fatec.ecommerce.modulo3.frete.dominio;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Política comercial de frete grátis da loja.
 *
 * <p>Regra: se o subtotal líquido dos produtos (já com promoções e cupons) for pelo menos o valor mínimo
 * (padrão R$ 250,00) e o peso total não passar do limite (padrão 5 kg), a modalidade <b>econômica</b> sai de graça.
 * As demais modalidades continuam cobradas normalmente.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b> regra de negócio isolada em uma classe própria (Single
 * Responsibility). Antes ela ficava escondida dentro do adaptador do PAC, o que misturava a tradução da API dos
 * Correios (Adapter) com uma regra comercial da loja. Os limites são configuráveis em
 * {@code ecommerce.modulo3.frete-gratis.*} sem recompilar.
 *
 * @author Henrique Sandrini (Integrante 3)
 */
@Component
public class PoliticaFreteGratis {

    private final BigDecimal valorMinimo;
    private final int pesoMaximoGramas;

    /**
     * Cria a política com os limites configurados.
     *
     * @param valorMinimo subtotal líquido mínimo para o benefício
     * @param pesoMaximoGramas peso máximo, em gramas, para o benefício
     */
    public PoliticaFreteGratis(
            @Value("${ecommerce.modulo3.frete-gratis.valor-minimo:250.00}") BigDecimal valorMinimo,
            @Value("${ecommerce.modulo3.frete-gratis.peso-maximo-gramas:5000}") int pesoMaximoGramas) {
        this.valorMinimo = valorMinimo;
        this.pesoMaximoGramas = pesoMaximoGramas;
    }

    /**
     * Verifica se o carrinho/pedido atende aos requisitos de valor e peso.
     *
     * @param subtotalLiquido subtotal dos produtos com descontos
     * @param pesoTotalGramas peso total em gramas
     * @return {@code true} se o pedido for elegível ao frete grátis
     */
    public boolean elegivel(BigDecimal subtotalLiquido, int pesoTotalGramas) {
        return subtotalLiquido != null
                && subtotalLiquido.compareTo(valorMinimo) >= 0
                && pesoTotalGramas <= pesoMaximoGramas;
    }

    /**
     * Aplica a política a uma cotação: zera somente a modalidade econômica de pedidos elegíveis.
     *
     * @param cotacao cotação original da transportadora
     * @param subtotalLiquido subtotal dos produtos com descontos
     * @param pesoTotalGramas peso total em gramas
     * @return a mesma cotação ou uma cópia com frete grátis
     */
    public CotacaoFrete aplicar(CotacaoFrete cotacao, BigDecimal subtotalLiquido, int pesoTotalGramas) {
        if (cotacao.modalidade().isEconomica() && elegivel(subtotalLiquido, pesoTotalGramas)) {
            return cotacao.comFreteGratis();
        }
        return cotacao;
    }

    /**
     * Retorna o subtotal mínimo configurado.
     *
     * @return valor mínimo
     */
    public BigDecimal getValorMinimo() {
        return valorMinimo;
    }

    /**
     * Retorna o peso máximo configurado.
     *
     * @return peso máximo em gramas
     */
    public int getPesoMaximoGramas() {
        return pesoMaximoGramas;
    }
}
