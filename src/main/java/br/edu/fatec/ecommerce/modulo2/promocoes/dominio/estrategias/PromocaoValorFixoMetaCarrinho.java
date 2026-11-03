package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias;

import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Estratégia de cálculo para Desconto Fixo por Meta de Valor no Carrinho.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa uma <i>Concrete Strategy</i> gerenciada pela <i>Dynamic Factory</i>.
 * Implementa a modalidade 3 da Constituição: se o subtotal bruto atingir ou ultrapassar
 * um patamar financeiro (ex.: R$ 300,00), concede um abatimento fixo em dinheiro (ex.: R$ 50,00).
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Component
public class PromocaoValorFixoMetaCarrinho implements EstrategiaCalculoPromocao {

    @Override
    public TipoMecanicaPromocao obterTipoMecanica() {
        return TipoMecanicaPromocao.VALOR_FIXO_META_CARRINHO;
    }

    /**
     * Calcula o abatimento fixo se a meta de valor for atingida.
     *
     * <p>Objetivo funcional: Concede desconto nominal (ex.: R$ 50,00) limitado à base disponível.
     * <p>Papel no padrão: Algoritmo concreto da Strategy.
     *
     * @param contexto Contexto de cálculo.
     * @return Desconto em BigDecimal normalizado.
     */
    @Override
    public BigDecimal calcularDesconto(ContextoCalculoPromocao contexto) {
        if (contexto == null || contexto.getSubtotalBase() == null) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        BigDecimal metaMinima = new BigDecimal("300.00");
        BigDecimal valorDescontoFixo = new BigDecimal("50.00");

        String params = contexto.getCampanha().getParametrosJson();
        if (params != null) {
            if (params.contains("metaMinima")) {
                metaMinima = extrairDecimal(params, "metaMinima", metaMinima);
            }
            if (params.contains("valorDesconto")) {
                valorDescontoFixo = extrairDecimal(params, "valorDesconto", valorDescontoFixo);
            }
        }

        if (contexto.getSubtotalBase().compareTo(metaMinima) >= 0) {
            // Limita o desconto ao subtotal base (piso zero)
            if (valorDescontoFixo.compareTo(contexto.getSubtotalBase()) > 0) {
                return OperacoesMonetarias.normalizar(contexto.getSubtotalBase());
            }
            return OperacoesMonetarias.normalizar(valorDescontoFixo);
        }

        return OperacoesMonetarias.ZERO_MONETARIO;
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
