package br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.ContextoElegibilidade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Objeto de requisição trafegado entre os elos da Cadeia de Responsabilidade.
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Atua como o <i>Request</i> no padrão <i>Chain of Responsibility</i> (Constituição, Princípio II).
 * Transporta o carrinho, a campanha sob avaliação, o contexto de elegibilidade e a lista de
 * promoções que já foram aprovadas na iteração atual para checagem de regras de exclusividade mútua.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
public class RequisicaoValidacaoPromocao {

    private final CarrinhoCompras carrinho;
    private final CampanhaPromocional campanha;
    private final ContextoElegibilidade contextoElegibilidade;
    private final List<CampanhaPromocional> promocoesJaAprovadas;

    /**
     * Construtor da requisição de validação.
     *
     * @param carrinho Carrinho de compras.
     * @param campanha Campanha sendo avaliada.
     * @param contextoElegibilidade Contexto para a árvore de elegibilidade.
     * @param promocoesJaAprovadas Lista de promoções já aprovadas no pipeline.
     */
    public RequisicaoValidacaoPromocao(
            CarrinhoCompras carrinho,
            CampanhaPromocional campanha,
            ContextoElegibilidade contextoElegibilidade,
            List<CampanhaPromocional> promocoesJaAprovadas
    ) {
        this.carrinho = Objects.requireNonNull(carrinho, "carrinho não pode ser nulo");
        this.campanha = Objects.requireNonNull(campanha, "campanha não pode ser nula");
        this.contextoElegibilidade = Objects.requireNonNull(contextoElegibilidade, "contextoElegibilidade não pode ser nulo");
        this.promocoesJaAprovadas = promocoesJaAprovadas != null ? new ArrayList<>(promocoesJaAprovadas) : new ArrayList<>();
    }

    public CarrinhoCompras getCarrinho() {
        return carrinho;
    }

    public CampanhaPromocional getCampanha() {
        return campanha;
    }

    public ContextoElegibilidade getContextoElegibilidade() {
        return contextoElegibilidade;
    }

    public List<CampanhaPromocional> getPromocoesJaAprovadas() {
        return Collections.unmodifiableList(promocoesJaAprovadas);
    }
}

