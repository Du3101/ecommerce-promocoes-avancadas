package br.edu.fatec.ecommerce.modulo3.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo3.BaseIntegracaoModulo3Test;
import br.edu.fatec.ecommerce.modulo3.eventos.BarramentoEventosDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.EventoDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.ObservadorEventoDominio;
import br.edu.fatec.ecommerce.modulo3.eventos.PagamentoConfirmadoEvent;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Testes de integração do webhook de pagamento com idempotência estrita (T017).
 *
 * <p>Cobre o item 7 do Princípio VI da Constituição: recebimento duplicado da mesma confirmação de pagamento.</p>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class PagamentoIdempotenciaIntegrationTest extends BaseIntegracaoModulo3Test {

    @Autowired
    private BarramentoEventosDominio barramentoEventos;

    private final List<EventoDominio> eventosRecebidos = new ArrayList<>();

    private final ObservadorEventoDominio espiao = new ObservadorEventoDominio() {
        @Override
        public String nomeCanal() {
            return "ESPIAO-TESTE";
        }

        @Override
        public boolean interessadoEm(EventoDominio evento) {
            return true;
        }

        @Override
        public void notificar(EventoDominio evento) {
            eventosRecebidos.add(evento);
        }
    };

    /**
     * Inscreve um observador espião no barramento (Observer: attach em tempo de execução).
     */
    @BeforeEach
    void inscreverEspiao() {
        barramentoEventos.inscrever(espiao);
    }

    /**
     * Remove o espião (detach) para não afetar outros testes.
     */
    @AfterEach
    void removerEspiao() {
        barramentoEventos.cancelarInscricao(espiao);
    }

    /**
     * Cria um pedido PIX aguardando pagamento com 1 teclado.
     *
     * @return JSON do pedido
     * @throws Exception erro do MockMvc
     */
    private JsonNode pedidoPixPendente() throws Exception {
        String email = criarClienteComEndereco("webhook");
        adicionarAoCarrinho(email, TECLADO, 1);
        return checkoutComSucesso(email, "PIX", "CORREIOS_SEDEX", null);
    }

    @Test
    @DisplayName("Confirmação repetida: a 1ª paga e baixa o estoque; a 2ª responde 200 sem repetir baixa nem evento")
    void confirmacaoDuplicadaDeveSerIdempotente() throws Exception {
        JsonNode pedido = pedidoPixPendente();
        String numero = pedido.get("numeroPedido").asText();
        BigDecimal total = pedido.get("snapshotFinanceiro").get("valorTotalLiquido").decimalValue();

        MvcResult primeira = postarWebhook(numero, true, total, TOKEN_GATEWAY);
        int disponivelAposPrimeira = disponivel(TECLADO);
        MvcResult segunda = postarWebhook(numero, true, total, TOKEN_GATEWAY);

        assertEquals(200, primeira.getResponse().getStatus());
        assertEquals(200, segunda.getResponse().getStatus());
        JsonNode r1 = json(primeira);
        JsonNode r2 = json(segunda);
        assertEquals("PAGO", r1.get("novoStatusPedido").asText());
        assertFalse(r1.get("idempotenteReexecutado").asBoolean());
        assertTrue(r2.get("idempotenteReexecutado").asBoolean());
        assertEquals("PAGO", r2.get("novoStatusPedido").asText());

        // Estoque: baixa definitiva uma única vez
        assertEquals(9, disponivelAposPrimeira);
        assertEquals(9, disponivel(TECLADO));
        assertEquals(0, reservado(TECLADO));
        assertEquals(EstadoReservaEstoque.CONFIRMADA, estadoReserva(numero));

        // Observer: um único PagamentoConfirmadoEvent
        long confirmacoes = eventosRecebidos.stream().filter(e -> e instanceof PagamentoConfirmadoEvent).count();
        assertEquals(1, confirmacoes);
    }

    @Test
    @DisplayName("Webhook sem token ou com token errado é recusado com 403")
    void webhookSemTokenValidoDeveSerRecusado() throws Exception {
        String numero = pedidoPixPendente().get("numeroPedido").asText();

        assertEquals(403, postarWebhook(numero, true, null, null).getResponse().getStatus());
        assertEquals(403, postarWebhook(numero, true, null, "token-falso").getResponse().getStatus());
        assertEquals(1, reservado(TECLADO), "Nada mudou no estoque");
    }

    @Test
    @DisplayName("Valor pago divergente do total é rejeitado com 400 e o pedido continua aguardando")
    void valorDivergenteDeveSerRejeitado() throws Exception {
        String numero = pedidoPixPendente().get("numeroPedido").asText();

        MvcResult resultado = postarWebhook(numero, true, new BigDecimal("1.00"), TOKEN_GATEWAY);

        assertEquals(400, resultado.getResponse().getStatus());
        assertEquals("AGUARDANDO_PAGAMENTO", pedidoRepository.findByNumeroPedido(numero).orElseThrow().getStatus().name());
    }

    @Test
    @DisplayName("Recusa pelo webhook: PAGAMENTO_RECUSADO, reserva liberada; recusa repetida é idempotente")
    void recusaDeveLiberarReservaDeFormaIdempotente() throws Exception {
        String numero = pedidoPixPendente().get("numeroPedido").asText();

        JsonNode primeira = json(postarWebhook(numero, false, null, TOKEN_GATEWAY));
        JsonNode segunda = json(postarWebhook(numero, false, null, TOKEN_GATEWAY));

        assertEquals("PAGAMENTO_RECUSADO", primeira.get("novoStatusPedido").asText());
        assertTrue(segunda.get("idempotenteReexecutado").asBoolean());
        assertEquals(10, disponivel(TECLADO));
        assertEquals(0, reservado(TECLADO));
        assertEquals(EstadoReservaEstoque.LIBERADA, estadoReserva(numero));
    }

    @Test
    @DisplayName("Pedido inexistente: 404")
    void pedidoInexistenteDeveRetornar404() throws Exception {
        assertEquals(404, postarWebhook("PED-000000-NAOEXISTE", true, null, TOKEN_GATEWAY).getResponse().getStatus());
    }

    @Test
    @DisplayName("Webhook tardio de cartão já aprovado no checkout responde como repetição idempotente")
    void webhookTardioDeCartaoDeveSerIdempotente() throws Exception {
        String email = criarClienteComEndereco("cartaotardio");
        adicionarAoCarrinho(email, TECLADO, 1);
        JsonNode pedido = checkoutComSucesso(email, "CARTAO_CREDITO", "CORREIOS_SEDEX", "5555444433332222");

        JsonNode resposta = json(postarWebhook(pedido.get("numeroPedido").asText(), true, null, TOKEN_GATEWAY));

        assertTrue(resposta.get("idempotenteReexecutado").asBoolean());
        assertEquals(9, disponivel(TECLADO));
    }
}
