package br.edu.fatec.ecommerce.modulo2.cupons.dominio;

import br.edu.fatec.ecommerce.modulo2.precificacao.dominio.OperacoesMonetarias;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Objeto Nulo que representa a ausência de cupom de desconto aplicado ao carrinho.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Implementação do padrão <i>Null Object / Missing Object</i> (Constituição, Princípio II).
 * Herda polimorficamente de {@link CupomPromocional}, fornecendo respostas neutras
 * (código vazio, desconto 0.00, indisponível para uso), eliminando checagens de nulo
 * no pipeline de formação de preço e nos controllers REST.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public final class CupomNulo extends CupomPromocional {

    private static final CupomNulo INSTANCIA = new CupomNulo();

    /**
     * Construtor privado para garantir o padrão Singleton dentro do Null Object.
     */
    private CupomNulo() {
        super(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                "",
                TipoDescontoCupom.VALOR_FIXO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                null,
                true,
                false
        );
    }

    /**
     * Retorna a instância única do Null Object de cupom.
     *
     * <p>Objetivo funcional: Ponto global de acesso para representar ausência de cupom.
     * <p>Papel no padrão: Static Factory Method do Null Object singleton.
     *
     * @return Instância única e imutável de CupomNulo.
     */
    public static CupomNulo obterInstancia() {
        return INSTANCIA;
    }

    @Override
    public boolean isDisponivelParaUso(Instant agora, BigDecimal subtotalCarrinho) {
        return false;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalBase) {
        return OperacoesMonetarias.ZERO_MONETARIO;
    }

    @Override
    public void registrarUso() {
        // No-op para o objeto nulo
    }
}

