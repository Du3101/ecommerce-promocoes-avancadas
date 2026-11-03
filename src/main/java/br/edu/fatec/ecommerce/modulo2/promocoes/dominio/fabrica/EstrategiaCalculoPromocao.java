package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica;

import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;

import java.math.BigDecimal;

/**
 * Interface Strategy para cálculo do benefício financeiro de uma mecânica promocional.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Representa a interface <i>Strategy</i> consumida pela <i>Dynamic Factory</i>
 * ({@link FabricaDinamicaEstrategiaPromocao}). Garante o Princípio Aberto-Fechado (OCP),
 * pois novas modalidades comerciais de promoção são adicionadas criando uma nova implementação
 * desta interface, que é automaticamente registrada e resolvida dinamicamente pelo Spring.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public interface EstrategiaCalculoPromocao {

    /**
     * Identifica qual mecânica promocional esta estratégia calcula.
     *
     * <p>Objetivo funcional: Informa o discriminador para registro dinâmico no catálogo da fábrica.
     * <p>Papel no padrão: Chave de mapeamento no Dynamic Factory / Service Registry.
     *
     * @return TipoMecanicaPromocao correspondente.
     */
    TipoMecanicaPromocao obterTipoMecanica();

    /**
     * Calcula o valor financeiro absoluto de desconto a ser concedido ao carrinho.
     *
     * <p>Objetivo funcional: Executa a fórmula matemática específica da mecânica (ex.: Leve X Pague Y, %, fixo).
     * <p>Papel no padrão: Método de execução da Strategy com retorno normalizado em BigDecimal.
     *
     * @param contexto Dados do carrinho, parâmetros da campanha e porta de catálogo.
     * @return Valor do desconto a ser abatido (com escala 2, nunca nulo).
     */
    BigDecimal calcularDesconto(ContextoCalculoPromocao contexto);
}
