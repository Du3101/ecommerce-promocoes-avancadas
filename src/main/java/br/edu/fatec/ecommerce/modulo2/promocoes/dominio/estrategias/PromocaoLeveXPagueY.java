package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinho;
import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estratégia de cálculo para a promoção "Leve X e Pague Y".
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa uma <i>Concrete Strategy</i> gerenciada pela <i>Dynamic Factory</i>
 * ({@link br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.FabricaDinamicaEstrategiaPromocao}).
 * Implementa a modalidade 1 da Constituição: a cada lote de {@code X} unidades de itens elegíveis,
 * o cliente paga apenas {@code Y} unidades, sendo o desconto equivalente a {@code (X - Y)} unidades
 * bonificadas (abatendo as unidades de menor valor do conjunto).
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@Component
public class PromocaoLeveXPagueY implements EstrategiaCalculoPromocao {

    @Override
    public TipoMecanicaPromocao obterTipoMecanica() {
        return TipoMecanicaPromocao.LEVE_X_PAGUE_Y;
    }

    /**
     * Calcula o desconto concedido na mecânica Leve X Pague Y.
     *
     * <p>Objetivo funcional: Concede desconto das unidades excedentes para cada lote de X unidades.
     * <p>Papel no padrão: Algoritmo concreto da Strategy.
     *
     * @param contexto Dados do carrinho e parâmetros da promoção.
     * @return Valor do desconto calculado em BigDecimal normalizado.
     */
    @Override
    public BigDecimal calcularDesconto(ContextoCalculoPromocao contexto) {
        if (contexto == null || contexto.getCarrinho() == null) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        // Parâmetros padrão: Leve 3 Pague 2 (se não especificado em JSON)
        int x = 3;
        int y = 2;

        String params = contexto.getCampanha().getParametrosJson();
        if (params != null && params.contains("qtdX") && params.contains("qtdY")) {
            // Extração simples de parâmetros estruturados sem dependências pesadas
            x = extrairInteiro(params, "qtdX", 3);
            y = extrairInteiro(params, "qtdY", 2);
        }

        if (x <= 0 || y <= 0 || x <= y) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        // Expande os preços unitários de todos os itens do carrinho em lista plana
        List<BigDecimal> precosIndividuais = new ArrayList<>();
        for (ItemCarrinho item : contexto.getCarrinho().getItens()) {
            for (int i = 0; i < item.getQuantidade(); i++) {
                precosIndividuais.add(item.getPrecoUnitario());
            }
        }

        if (precosIndividuais.size() < x) {
            return OperacoesMonetarias.ZERO_MONETARIO;
        }

        // Ordena do menor preço para o maior preço (as unidades grátis são as mais baratas)
        Collections.sort(precosIndividuais);

        int totalLotes = precosIndividuais.size() / x;
        int unidadesGratis = totalLotes * (x - y);

        BigDecimal descontoTotal = BigDecimal.ZERO;
        for (int i = 0; i < unidadesGratis && i < precosIndividuais.size(); i++) {
            descontoTotal = descontoTotal.add(precosIndividuais.get(i));
        }

        return OperacoesMonetarias.normalizar(descontoTotal);
    }

    private int extrairInteiro(String json, String chave, int padrao) {
        try {
            int idx = json.indexOf("\"" + chave + "\"");
            if (idx != -1) {
                int start = json.indexOf(":", idx) + 1;
                int end = json.indexOf(",", start);
                if (end == -1) {
                    end = json.indexOf("}", start);
                }
                String valorStr = json.substring(start, end).replaceAll("[^0-9]", "").trim();
                return Integer.parseInt(valorStr);
            }
        } catch (Exception e) {
            // Em caso de falha de parsing, adota o padrão seguro
        }
        return padrao;
    }
}
