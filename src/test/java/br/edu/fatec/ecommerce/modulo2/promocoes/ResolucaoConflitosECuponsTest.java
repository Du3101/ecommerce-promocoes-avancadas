package br.edu.fatec.ecommerce.modulo2.promocoes;

import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.CarrinhoCompras;
import br.edu.fatec.ecommerce.modulo2.carrinho.dominio.ItemCarrinhoProduto;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomNulo;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.CupomPromocional;
import br.edu.fatec.ecommerce.modulo2.cupons.dominio.TipoDescontoCupom;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.RequisicaoValidacaoPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.ValidadorCarrinhoNaoVazio;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.ValidadorConflitoExclusividade;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.cadeia.ValidadorElegibilidadeCampanha;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.CondicaoValorMinimoCarrinho;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.composite.ContextoElegibilidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários de Comparable (ordenação determinística), Chain of Responsibility e Cupons.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
class ResolucaoConflitosECuponsTest {

    @Test
    @DisplayName("Padrão Comparable: Deve ordenar promoções prioritariamente pela maior prioridade numérica")
    void deveOrdenarPromocoesPorPrioridadeDecrescente() {
        CampanhaPromocional baixa = new CampanhaPromocional(
                UUID.randomUUID(), "Promoção Baixa", "Desc", TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
                "{}", 5, true, true, null, null, "{}", null
        );
        CampanhaPromocional alta = new CampanhaPromocional(
                UUID.randomUUID(), "Promoção Alta", "Desc", TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
                "{}", 20, true, true, null, null, "{}", null
        );
        CampanhaPromocional media = new CampanhaPromocional(
                UUID.randomUUID(), "Promoção Média", "Desc", TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
                "{}", 10, true, true, null, null, "{}", null
        );

        List<CampanhaPromocional> lista = new ArrayList<>(List.of(baixa, alta, media));
        Collections.sort(lista);

        // Ordem decrescente de prioridade: 20 -> 10 -> 5
        assertEquals("Promoção Alta", lista.get(0).getNome());
        assertEquals("Promoção Média", lista.get(1).getNome());
        assertEquals("Promoção Baixa", lista.get(2).getNome());
    }

    @Test
    @DisplayName("Chain of Responsibility: Deve interromper validação em carrinho vazio ou exclusividade mútua")
    void deveValidarCadeiaDeResponsabilidade() {
        ValidadorCarrinhoNaoVazio elo1 = new ValidadorCarrinhoNaoVazio();
        ValidadorElegibilidadeCampanha elo2 = new ValidadorElegibilidadeCampanha();
        ValidadorConflitoExclusividade elo3 = new ValidadorConflitoExclusividade();
        elo1.definirProximo(elo2).definirProximo(elo3);

        CarrinhoCompras carrinhoVazio = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);
        CampanhaPromocional campanha = new CampanhaPromocional(
                UUID.randomUUID(), "Promo Exclusiva", "Desc", TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
                "{}", 10, false, true, null, null, "{}", new CondicaoValorMinimoCarrinho(new BigDecimal("100.00"))
        );

        ContextoElegibilidade ctx = new ContextoElegibilidade(
                UUID.randomUUID(), false, "SP", BigDecimal.ZERO, Collections.emptyList(), null
        );

        // 1. Carrinho vazio é barrado no elo 1
        RequisicaoValidacaoPromocao req1 = new RequisicaoValidacaoPromocao(carrinhoVazio, campanha, ctx, Collections.emptyList());
        assertFalse(elo1.validar(req1));

        // 2. Carrinho com item, mas subtotal abaixo da condição (R$ 50 < R$ 100) -> barrado no elo 2
        CarrinhoCompras carrinhoComItem = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);
        carrinhoComItem.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinhoComItem.getId(), "SKU-1", "Prod", new BigDecimal("50.00"), 1, 10, null
        ));
        ContextoElegibilidade ctx2 = new ContextoElegibilidade(
                UUID.randomUUID(), false, "SP", new BigDecimal("50.00"), carrinhoComItem.getItens(), null
        );
        RequisicaoValidacaoPromocao req2 = new RequisicaoValidacaoPromocao(carrinhoComItem, campanha, ctx2, Collections.emptyList());
        assertFalse(elo1.validar(req2));

        // 3. Subtotal R$ 150 (elegível) e sem promoções anteriores -> aprovado por todos os elos
        CarrinhoCompras carrinhoElegivel = new CarrinhoCompras(UUID.randomUUID(), UUID.randomUUID(), null);
        carrinhoElegivel.adicionarOuIncrementarItem(new ItemCarrinhoProduto(
                UUID.randomUUID(), carrinhoElegivel.getId(), "SKU-1", "Prod", new BigDecimal("150.00"), 1, 10, null
        ));
        ContextoElegibilidade ctx3 = new ContextoElegibilidade(
                UUID.randomUUID(), false, "SP", new BigDecimal("150.00"), carrinhoElegivel.getItens(), null
        );
        RequisicaoValidacaoPromocao req3 = new RequisicaoValidacaoPromocao(carrinhoElegivel, campanha, ctx3, Collections.emptyList());
        assertTrue(elo1.validar(req3));

        // 4. Se a campanha for exclusiva e já houver outra promoção aprovada -> barrado no elo 3
        CampanhaPromocional promoJaAprovada = new CampanhaPromocional(
                UUID.randomUUID(), "Outra Promo", "Desc", TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
                "{}", 15, true, true, null, null, "{}", null
        );
        RequisicaoValidacaoPromocao req4 = new RequisicaoValidacaoPromocao(carrinhoElegivel, campanha, ctx3, List.of(promoJaAprovada));
        assertFalse(elo1.validar(req4));
    }

    @Test
    @DisplayName("Cupons Promocionais: Validação de vigência, cotas, valor mínimo e Null Object")
    void deveValidarCuponsPromocionaisENullObject() {
        Instant agora = Instant.now();

        // 1. Cupom válido de 10%
        CupomPromocional cupomValido = new CupomPromocional(
                UUID.randomUUID(), "DESC10", TipoDescontoCupom.PERCENTUAL, new BigDecimal("10.00"),
                new BigDecimal("100.00"), 5, agora.plus(1, ChronoUnit.DAYS), true, true
        );
        assertTrue(cupomValido.isDisponivelParaUso(agora, new BigDecimal("150.00")));
        assertEquals(new BigDecimal("15.00"), cupomValido.calcularDesconto(new BigDecimal("150.00")));

        // 2. Rejeição por subtotal inferior ao mínimo exigido (R$ 80 < R$ 100)
        assertFalse(cupomValido.isDisponivelParaUso(agora, new BigDecimal("80.00")));

        // 3. Rejeição por expiração temporal
        CupomPromocional cupomExpirado = new CupomPromocional(
                UUID.randomUUID(), "EXPIRADO", TipoDescontoCupom.VALOR_FIXO, new BigDecimal("20.00"),
                BigDecimal.ZERO, 10, agora.minus(1, ChronoUnit.HOURS), true, true
        );
        assertFalse(cupomExpirado.isDisponivelParaUso(agora, new BigDecimal("200.00")));

        // 4. Rejeição por esgotamento de cotas
        CupomPromocional cupomEsgotado = new CupomPromocional(
                UUID.randomUUID(), "ESGOTADO", TipoDescontoCupom.VALOR_FIXO, new BigDecimal("20.00"),
                BigDecimal.ZERO, 1, agora.plus(1, ChronoUnit.DAYS), true, true
        );
        cupomEsgotado.registrarUso();
        assertFalse(cupomEsgotado.isDisponivelParaUso(agora, new BigDecimal("200.00")));

        // 5. CupomNulo (Padrão Null Object)
        CupomNulo cupomNulo = CupomNulo.obterInstancia();
        assertFalse(cupomNulo.isDisponivelParaUso(agora, new BigDecimal("1000.00")));
        assertEquals(new BigDecimal("0.00"), cupomNulo.calcularDesconto(new BigDecimal("1000.00")));
        assertEquals("", cupomNulo.getCodigo());
    }
}
