package br.edu.fatec.ecommerce.modulo3.frete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.modulo3.checkout.aplicacao.CalculadoraRateioDesconto;
import br.edu.fatec.ecommerce.modulo3.compartilhado.excecao.RegraNegocioModulo3Exception;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.CorreiosPacAdapter;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.CorreiosSedexAdapter;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.TransportadoraExpressaAdapter;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo.ApiCorreiosSimulada;
import br.edu.fatec.ecommerce.modulo3.frete.adaptadores.externo.ApiTransportadoraExpressaSimulada;
import br.edu.fatec.ecommerce.modulo3.frete.aplicacao.ServicoCalculoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.CotacaoFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.ModalidadeFrete;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.PoliticaFreteGratis;
import br.edu.fatec.ecommerce.modulo3.frete.dominio.RegiaoEntrega;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários do subsistema de frete (Adapters/ACL + política de frete grátis) e do rateio de descontos (T024).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class CalculoFreteEPoliticaFreteGratisTest {

    private static final String CEP_SAO_PAULO = "01310-100";
    private static final String CEP_MANAUS = "69005-000";

    private ServicoCalculoFrete servicoFrete;

    /**
     * Monta o serviço com os adaptadores reais e as APIs externas simuladas, sem Spring.
     */
    @BeforeEach
    void configurar() {
        ApiCorreiosSimulada apiCorreios = new ApiCorreiosSimulada();
        servicoFrete = new ServicoCalculoFrete(
                List.of(new CorreiosPacAdapter(apiCorreios), new CorreiosSedexAdapter(apiCorreios),
                        new TransportadoraExpressaAdapter(new ApiTransportadoraExpressaSimulada())),
                new PoliticaFreteGratis(new BigDecimal("250.00"), 5000));
    }

    @Test
    @DisplayName("Retorna as 3 modalidades com valores e prazos, ordenadas pelo menor valor (1.200 g para SP)")
    void deveCotarTresModalidadesParaSaoPaulo() {
        List<CotacaoFrete> cotacoes = servicoFrete.cotarTodasModalidades(CEP_SAO_PAULO, 1200, new BigDecimal("100.00"));

        assertEquals(3, cotacoes.size());
        assertEquals(ModalidadeFrete.CORREIOS_PAC, cotacoes.get(0).modalidade());
        // PAC: (18,00 + 4,50 x 1,2 kg) x 1,00 = 23,40 ; 6 dias
        assertEquals(new BigDecimal("23.40"), cotacoes.get(0).valorCobrado());
        assertEquals(6, cotacoes.get(0).prazoDiasUteis());
        // SEDEX: (30,00 + 7,00 x 1,2) = 38,40 ; 2 dias
        CotacaoFrete sedex = servicoFrete.cotarModalidade(ModalidadeFrete.CORREIOS_SEDEX, CEP_SAO_PAULO, 1200, BigDecimal.TEN);
        assertEquals(new BigDecimal("38.40"), sedex.valorCobrado());
        assertEquals(2, sedex.prazoDiasUteis());
        // Transportadora: (40,00 + 9,00 x 1,2) + 5,00 seguro = 55,80 ; 24 h = 1 dia
        CotacaoFrete expressa = servicoFrete.cotarModalidade(ModalidadeFrete.TRANSPORTADORA_EXPRESSA, CEP_SAO_PAULO, 1200, BigDecimal.TEN);
        assertEquals(new BigDecimal("55.80"), expressa.valorCobrado());
        assertEquals(1, expressa.prazoDiasUteis());
        assertTrue(cotacoes.stream().noneMatch(CotacaoFrete::freteGratisAplicado));
    }

    @Test
    @DisplayName("Frete grátis zera somente a modalidade econômica (R$ 350,00 e 2 kg)")
    void deveAplicarFreteGratisSomenteNoPac() {
        List<CotacaoFrete> cotacoes = servicoFrete.cotarTodasModalidades(CEP_SAO_PAULO, 2000, new BigDecimal("350.00"));

        CotacaoFrete pac = cotacoes.stream().filter(c -> c.modalidade() == ModalidadeFrete.CORREIOS_PAC).findFirst().orElseThrow();
        assertTrue(pac.freteGratisAplicado());
        assertEquals(new BigDecimal("0.00"), pac.valorCobrado());
        assertTrue(pac.valorOriginal().signum() > 0, "O valor original da transportadora continua visível");
        assertTrue(cotacoes.stream().filter(c -> c.modalidade() != ModalidadeFrete.CORREIOS_PAC)
                .noneMatch(CotacaoFrete::freteGratisAplicado));
    }

    @Test
    @DisplayName("Sem frete grátis abaixo do valor mínimo ou acima do peso máximo")
    void naoDeveAplicarFreteGratisForaDaRegra() {
        CotacaoFrete abaixoDoMinimo = servicoFrete.cotarModalidade(ModalidadeFrete.CORREIOS_PAC, CEP_SAO_PAULO, 2000, new BigDecimal("249.99"));
        CotacaoFrete pesado = servicoFrete.cotarModalidade(ModalidadeFrete.CORREIOS_PAC, CEP_SAO_PAULO, 5001, new BigDecimal("900.00"));
        CotacaoFrete limiteExato = servicoFrete.cotarModalidade(ModalidadeFrete.CORREIOS_PAC, CEP_SAO_PAULO, 5000, new BigDecimal("250.00"));

        assertFalse(abaixoDoMinimo.freteGratisAplicado());
        assertFalse(pesado.freteGratisAplicado());
        assertTrue(limiteExato.freteGratisAplicado());
    }

    @Test
    @DisplayName("Região Norte (CEP 69xxx) é mais cara e mais demorada que São Paulo")
    void deveRegionalizarPrecoEPrazoPeloCep() {
        CotacaoFrete sp = servicoFrete.cotarModalidade(ModalidadeFrete.CORREIOS_SEDEX, CEP_SAO_PAULO, 1000, BigDecimal.TEN);
        CotacaoFrete manaus = servicoFrete.cotarModalidade(ModalidadeFrete.CORREIOS_SEDEX, CEP_MANAUS, 1000, BigDecimal.TEN);

        assertEquals(RegiaoEntrega.NORTE, RegiaoEntrega.deCep(CEP_MANAUS));
        assertTrue(manaus.valorCobrado().compareTo(sp.valorCobrado()) > 0);
        assertEquals(sp.prazoDiasUteis() + 5, manaus.prazoDiasUteis());
    }

    @Test
    @DisplayName("CEP inválido é rejeitado com erro de regra de negócio")
    void deveRejeitarCepInvalido() {
        assertThrows(RegraNegocioModulo3Exception.class,
                () -> servicoFrete.cotarTodasModalidades("1234", 1000, BigDecimal.TEN));
    }

    @Test
    @DisplayName("Rateio de desconto: proporcional ao valor bruto da linha, somando exatamente o total")
    void deveRatearDescontoProporcionalmente() {
        List<BigDecimal> unitarios = CalculadoraRateioDesconto.calcularDescontoUnitario(List.of(
                new CalculadoraRateioDesconto.LinhaRateio(new BigDecimal("300.00"), 1),
                new CalculadoraRateioDesconto.LinhaRateio(new BigDecimal("150.00"), 2)), new BigDecimal("110.00"));

        // Linhas com o mesmo valor bruto (300 e 300) dividem o desconto ao meio: 55 + 55
        assertEquals(new BigDecimal("55.00"), unitarios.get(0));
        assertEquals(new BigDecimal("27.50"), unitarios.get(1));
    }

    @Test
    @DisplayName("Rateio sem desconto devolve zero para todas as linhas")
    void rateioSemDescontoDeveSerZero() {
        List<BigDecimal> unitarios = CalculadoraRateioDesconto.calcularDescontoUnitario(List.of(
                new CalculadoraRateioDesconto.LinhaRateio(new BigDecimal("10.00"), 3)), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), unitarios.get(0));
    }
}
