package br.edu.fatec.ecommerce.modulo3.pedido;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.edu.fatec.ecommerce.modulo1.auditoria.infraestrutura.RegistroAuditoriaJpaRepository;
import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo3.BaseIntegracaoModulo3Test;
import br.edu.fatec.ecommerce.modulo3.pagamento.dominio.StatusTransacaoPagamento;
import br.edu.fatec.ecommerce.modulo3.pagamento.infraestrutura.TransacaoPagamentoJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Testes de integração do cancelamento (Template Method + Mediator + State + auditoria) (T029).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class CancelamentoPedidoMediatorTest extends BaseIntegracaoModulo3Test {

    @Autowired
    private TransacaoPagamentoJpaRepository transacaoRepository;

    @Autowired
    private RegistroAuditoriaJpaRepository auditoriaRepository;

    /**
     * Chama {@code DELETE /api/v1/pedidos/{id}/cancelamento}.
     *
     * @param pedidoId UUID do pedido
     * @param email usuário
     * @param perfil CLIENTE ou ADMIN
     * @return resultado HTTP
     * @throws Exception erro do MockMvc
     */
    private MvcResult cancelar(String pedidoId, String email, String perfil) throws Exception {
        return mockMvc.perform(delete("/api/v1/pedidos/" + pedidoId + "/cancelamento")
                .param("motivo", "Teste de cancelamento")
                .with(user(email).roles(perfil))).andReturn();
    }

    @Test
    @DisplayName("Cliente cancela pedido AGUARDANDO_PAGAMENTO: reserva liberada, cobrança cancelada, sem estorno, auditado")
    void clienteDeveCancelarPedidoAguardandoPagamento() throws Exception {
        String email = criarClienteComEndereco("cancela");
        adicionarAoCarrinho(email, TECLADO, 1);
        JsonNode pedido = checkoutComSucesso(email, "PIX", "CORREIOS_PAC", null);
        String numero = pedido.get("numeroPedido").asText();

        MvcResult resultado = cancelar(pedido.get("pedidoId").asText(), email, "CLIENTE");

        assertEquals(200, resultado.getResponse().getStatus());
        JsonNode corpo = json(resultado);
        assertEquals("AGUARDANDO_PAGAMENTO", corpo.get("statusAnterior").asText());
        assertEquals("CANCELADO", corpo.get("novoStatus").asText());
        assertFalse(corpo.get("estornoRealizado").asBoolean());
        assertEquals("RESERVA_LIBERADA", corpo.get("acaoEstoque").asText());
        assertTrue(corpo.get("motivo").asText().startsWith("[CLIENTE]"));

        assertEquals(10, disponivel(TECLADO));
        assertEquals(0, reservado(TECLADO));
        assertEquals(EstadoReservaEstoque.LIBERADA, estadoReserva(numero));
        assertEquals(StatusTransacaoPagamento.CANCELADA, transacaoRepository
                .findFirstByPedidoIdOrderByProcessadoEmDesc(UUID.fromString(pedido.get("pedidoId").asText()))
                .orElseThrow().getStatus());
        assertEquals(1, auditoriaRepository.findByIdentificadorAlvoOrderByOcorridoEmAsc(numero).size());
    }

    @Test
    @DisplayName("Admin cancela pedido PAGO: estorno no gateway e unidades devolvidas ao estoque")
    void adminDeveCancelarPedidoPagoComEstornoEDevolucao() throws Exception {
        String email = criarClienteComEndereco("cancelapago");
        adicionarAoCarrinho(email, TECLADO, 1);
        JsonNode pedido = checkoutComSucesso(email, "CARTAO_CREDITO", "CORREIOS_SEDEX", "4111111111111111");
        String numero = pedido.get("numeroPedido").asText();
        assertEquals(9, disponivel(TECLADO));

        JsonNode corpo = json(cancelar(pedido.get("pedidoId").asText(), EMAIL_ADMIN, "ADMIN"));

        assertEquals("PAGO", corpo.get("statusAnterior").asText());
        assertTrue(corpo.get("estornoRealizado").asBoolean());
        assertTrue(corpo.get("protocoloEstorno").asText().startsWith("CC-EST-"));
        assertEquals("ESTOQUE_DEVOLVIDO", corpo.get("acaoEstoque").asText());
        assertTrue(corpo.get("motivo").asText().startsWith("[ADMINISTRADOR]"));

        assertEquals(10, disponivel(TECLADO));
        assertEquals(EstadoReservaEstoque.ESTORNADA_POS_BAIXA, estadoReserva(numero));
        assertEquals(StatusTransacaoPagamento.ESTORNADA, transacaoRepository
                .findFirstByPedidoIdOrderByProcessadoEmDesc(UUID.fromString(pedido.get("pedidoId").asText()))
                .orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Pedido ENVIADO não pode ser cancelado (400) e o estoque não muda")
    void pedidoEnviadoNaoDeveSerCancelado() throws Exception {
        String email = criarClienteComEndereco("enviado");
        adicionarAoCarrinho(email, TECLADO, 1);
        JsonNode pedido = checkoutComSucesso(email, "CARTAO_CREDITO", "CORREIOS_SEDEX", "4111111111111111");
        String pedidoId = pedido.get("pedidoId").asText();
        assertEquals(200, transicaoAdmin(pedidoId, "preparar").getResponse().getStatus());
        JsonNode despachado = json(transicaoAdmin(pedidoId, "despachar"));
        assertEquals("ENVIADO", despachado.get("status").asText());
        assertTrue(despachado.get("entrega").get("codigoRastreio").asText().matches("BR\\d{9}BR"));

        MvcResult resultado = cancelar(pedidoId, email, "CLIENTE");

        assertEquals(400, resultado.getResponse().getStatus());
        assertTrue(resultado.getResponse().getContentAsString().contains("devolução"));
        assertEquals(9, disponivel(TECLADO));
        assertEquals("ENVIADO", pedidoRepository.findById(UUID.fromString(pedidoId)).orElseThrow().getStatus().name());
    }

    @Test
    @DisplayName("Cliente não pode cancelar nem consultar pedido de outro cliente (403)")
    void clienteNaoDeveAcessarPedidoDeOutro() throws Exception {
        String dono = criarClienteComEndereco("dono");
        String intruso = criarClienteComEndereco("intruso");
        adicionarAoCarrinho(dono, TECLADO, 1);
        String pedidoId = checkoutComSucesso(dono, "PIX", "CORREIOS_PAC", null).get("pedidoId").asText();

        assertEquals(403, cancelar(pedidoId, intruso, "CLIENTE").getResponse().getStatus());
        assertEquals(403, mockMvc.perform(get("/api/v1/pedidos/" + pedidoId).with(user(intruso).roles("CLIENTE")))
                .andReturn().getResponse().getStatus());
        assertEquals(200, mockMvc.perform(get("/api/v1/pedidos/" + pedidoId).with(user(dono).roles("CLIENTE")))
                .andReturn().getResponse().getStatus());
        assertEquals(1, reservado(TECLADO));
    }

    @Test
    @DisplayName("Cancelar duas vezes: a segunda tentativa é bloqueada pelo State (400)")
    void cancelamentoDuplicadoDeveSerBloqueado() throws Exception {
        String email = criarClienteComEndereco("duplo");
        adicionarAoCarrinho(email, TECLADO, 1);
        String pedidoId = checkoutComSucesso(email, "BOLETO", "CORREIOS_PAC", null).get("pedidoId").asText();

        assertEquals(200, cancelar(pedidoId, email, "CLIENTE").getResponse().getStatus());
        assertEquals(400, cancelar(pedidoId, email, "CLIENTE").getResponse().getStatus());
        assertEquals(10, disponivel(TECLADO));
    }

    @Test
    @DisplayName("Webhook de pagamento que chega depois do cancelamento é rejeitado (400)")
    void webhookAposCancelamentoDeveSerRejeitado() throws Exception {
        String email = criarClienteComEndereco("tardio");
        adicionarAoCarrinho(email, TECLADO, 1);
        JsonNode pedido = checkoutComSucesso(email, "PIX", "CORREIOS_PAC", null);
        cancelar(pedido.get("pedidoId").asText(), email, "CLIENTE");

        MvcResult webhook = postarWebhook(pedido.get("numeroPedido").asText(), true, null, TOKEN_GATEWAY);

        assertEquals(400, webhook.getResponse().getStatus());
        assertEquals(10, disponivel(TECLADO));
    }
}
