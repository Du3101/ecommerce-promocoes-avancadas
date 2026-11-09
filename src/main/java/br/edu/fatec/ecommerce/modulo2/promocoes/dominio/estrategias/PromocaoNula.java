package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.estrategias;

import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.ContextoCalculoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.fabrica.EstrategiaCalculoPromocao;

import java.math.BigDecimal;

/**
 * Objeto nulo que representa a ausência de desconto ou promoção aplicável.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Implementa o padrão <i>Null Object / Missing Object</i> (Constituição, Princípio II).
 * Evita o retorno de referências {@code null} quando nenhum desconto é concedido,
 * respondendo com comportamento neutro ({@code BigDecimal.ZERO}) e eliminando checagens
 * defensivas e riscos de {@link NullPointerException}.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public final class PromocaoNula implements EstrategiaCalculoPromocao {

    private static final PromocaoNula INSTANCIA = new PromocaoNula();

    /**
     * Construtor privado para garantir o padrão Singleton dentro do Null Object.
     */
    private PromocaoNula() {
    }

    /**
     * Retorna a instância única do Null Object de promoção.
     *
     * <p>Objetivo funcional: Ponto global de acesso para representar promoção inexistente.
     * <p>Papel no padrão: Static Factory Method para obtenção do Null Object singleton.
     *
     * @return Instância única de PromocaoNula.
     */
    public static PromocaoNula obterInstancia() {
        return INSTANCIA;
    }

    @Override
    public TipoMecanicaPromocao obterTipoMecanica() {
        return null;
    }

    /**
     * Retorna desconto neutro (R$ 0,00).
     *
     * <p>Objetivo funcional: Não altera o valor do carrinho.
     * <p>Papel no padrão: Comportamento seguro e inerte do Null Object.
     *
     * @param contexto Contexto de cálculo.
     * @return ZERO_MONETARIO (0.00).
     */
    @Override
    public BigDecimal calcularDesconto(ContextoCalculoPromocao contexto) {
        return OperacoesMonetarias.ZERO_MONETARIO;
    }
}
