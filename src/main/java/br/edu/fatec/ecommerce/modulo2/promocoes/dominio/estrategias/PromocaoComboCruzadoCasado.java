package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Estratégia de cálculo para Desconto em Combo Cruzado / Venda Casada.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa uma <i>Concrete Strategy</i> gerenciada pela <i>Dynamic Factory</i>.
 * Implementa a modalidade 4 da Constituição: identifica a presença conjunta de dois produtos
 * (ex.: Teclado + Mouse) e aplica um desconto percentual sobre o valor dos pares casados.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Component
public class PromocaoComboCruzadoCasado implements EstrategiaCalculoPromocao {

    @Override
    public TipoMecanicaPromocao obterTipoMecanica() {
        return TipoMecanicaPromocao.COMBO_CRUZADO_CASADO;
    }

    /**
     * Calcula o desconto sobre os pares casados de SKU A e SKU B.
     *
     * <p>Objetivo funcional: Concede desconto percentual sobre a soma de cada par casado.
     * <p>Papel no padrão: Algoritmo concreto da Strategy.
     *
     * @param contexto Contexto de cálculo.
     * @return Desconto em BigDecimal normalizado.
     */
    @Override
    public BigDecimal calcularDesconto(ContextoCalculoPromocao contexto) {
        if (contexto == null || contexto.getCarrinho() == null) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        String skuA = "DEMO-TEC-PRO-PRETO";
        String skuB = "DEMO-MOU-PREC-PRETO";
        BigDecimal percentualDesconto = new BigDecimal("20.00"); // 20% de desconto no conjunto casado

        String params = contexto.getCampanha().getParametrosJson();
        if (params != null) {
            if (params.contains("skuA")) {
                skuA = extrairString(params, "skuA", skuA);
            }
            if (params.contains("skuB")) {
                skuB = extrairString(params, "skuB", skuB);
            }
            if (params.contains("percentual")) {
                percentualDesconto = extrairDecimal(params, "percentual", percentualDesconto);
            }
        }

        Optional<ItemCarrinho> itemA = contexto.getCarrinho().buscarItemPorSku(skuA);
        Optional<ItemCarrinho> itemB = contexto.getCarrinho().buscarItemPorSku(skuB);

        if (itemA.isEmpty() || itemB.isEmpty()) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        int paresCasados = Math.min(itemA.get().getQuantidade(), itemB.get().getQuantidade());
        if (paresCasados <= 0) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        BigDecimal valorParUnitario = itemA.get().getPrecoUnitario().add(itemB.get().getPrecoUnitario());
        BigDecimal valorBasePares = valorParUnitario.multiply(BigDecimal.valueOf(paresCasados));

        return OperacoesMonetarias.calcularPercentual(valorBasePares, percentualDesconto);
    }

    private String extrairString(String json, String chave, String padrao) {
        try {
            int idx = json.indexOf("\"" + chave + "\"");
            if (idx != -1) {
                int start = json.indexOf(":", idx) + 1;
                int end = json.indexOf(",", start);
                if (end == -1) {
                    end = json.indexOf("}", start);
                }
                return json.substring(start, end).replaceAll("[\"'\r\n ]", "").trim();
            }
        } catch (Exception e) {
            // fallback
        }
        return padrao;
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
