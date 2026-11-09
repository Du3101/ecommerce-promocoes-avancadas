package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias;

import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Estratégia de cálculo para Desconto Percentual Condicional.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa uma <i>Concrete Strategy</i> gerenciada pela <i>Dynamic Factory</i>.
 * Implementa a modalidade 2 da Constituição: concede uma alíquota percentual de desconto
 * (ex.: 15.00%) sobre a base subtotal do carrinho caso a campanha seja elegível.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Component
public class PromocaoPercentualCondicional implements EstrategiaCalculoPromocao {

    @Override
    public TipoMecanicaPromocao obterTipoMecanica() {
        return TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL;
    }

    /**
     * Calcula o desconto percentual sobre o subtotal base.
     *
     * <p>Objetivo funcional: Aplica a fórmula {@code (subtotalBase * percentual) / 100} com piso zero.
     * <p>Papel no padrão: Algoritmo concreto da Strategy.
     *
     * @param contexto Contexto de cálculo.
     * @return Desconto em BigDecimal com escala 2.
     */
    @Override
    public BigDecimal calcularDesconto(ContextoCalculoPromocao contexto) {
        if (contexto == null || contexto.getSubtotalBase() == null) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        BigDecimal percentual = new BigDecimal("10.00"); // padrão 10%

        String params = contexto.getCampanha().getParametrosJson();
        if (params != null && params.contains("percentual")) {
            percentual = extrairDecimal(params, "percentual", percentual);
        }

        return OperacoesMonetarias.calcularPercentual(contexto.getSubtotalBase(), percentual);
    }

    private BigDecimal extrairDecimal(String json, String chave, BigDecimal padrao) {
        try {
            int idx = json.indexOf("\"" + chave + "\"");
            if (idx != -1) {
                int start = json.indexOf(":", idx) + 1;
                int end = json.indexOf(",", start);
                if (end == -1) {
                    end = json.indexOf("}", start);
                }
                String valorStr = json.substring(start, end).replaceAll("[^0-9.]", "").trim();
                return new BigDecimal(valorStr);
            }
        } catch (Exception e) {
            // fallback
        }
        return padrao;
    }
}
