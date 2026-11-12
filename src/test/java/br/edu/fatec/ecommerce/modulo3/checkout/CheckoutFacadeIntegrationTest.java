package br.edu.fatec.ecommerce.modulo3.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.edu.fatec.ecommerce.modulo1.estoque.dominio.EstadoReservaEstoque;
import br.edu.fatec.ecommerce.modulo3.BaseIntegracaoModulo3Test;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Testes de integração do checkout (Facade + Template Method + Snapshot imutável) (T007).
 *
 * <p>Cobre o item 3 do Princípio VI da Constituição (compra com estoque insuficiente / última unidade) e o
 * item 5 (pagamento recusado com liberação da reserva).</p>
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class CheckoutFacadeIntegrationTest extends BaseIntegracaoModulo3Test {

    @Test
    @DisplayName("PIX: cria pedido AGUARDANDO_PAGAMENTO com snapshot, reserva o estoque e esvazia o carrinho")
    void checkoutPixDeveCriarPedidoComSnapshotReservaECarrinhoVazio() throws Exception {
        String email = criarClienteComEndereco("pix");
        adicionarAoCarrinho(email, TECLADO, 1);
        adicionarAoCarrinho(email, MOUSE, 2);

        JsonNode pedido = checkoutComSucesso(email, "PIX", "CORREIOS_SEDEX", null);

        // Pedido e snapshot
        assertEquals("AGUARDANDO_PAGAMENTO", pedido.get("status").asText());
        assertTrue(pedido.get("numeroPedido").asText().startsWith("PED-"));
        assertEquals(2, pedido.get("itens").size());
        JsonNode financeiro = pedido.get("snapshotFinanceiro");
        BigDecimal bruto = financeiro.get("subtotalBruto").decimalValue();
        BigDecimal descontos = financeiro.get("totalDescontos").decimalValue();
        BigDecimal frete = financeiro.get("valorFrete").decimalValue();
        assertEquals(0, new BigDecimal("600.00").compareTo(bruto));
        assertTrue(frete.signum() > 0, "SEDEX não tem frete grátis");
        assertEquals(0, bruto.subtract(descontos).add(frete).compareTo(financeiro.get("valorTotalLiquido").decimalValue()));
        assertTrue(pedido.get("entrega").get("enderecoDestino").asText().contains("Avenida Paulista"));
        assertTrue(pedido.get("dadosPagamento").get("comprovante").asText().startsWith("000201"),
                "Chave PIX copia-e-cola no padrão BR Code");

        // Estoque reservado (Módulo 1) e carrinho esvaziado (Módulo 2)
        assertEquals(9, disponivel(TECLADO));
        assertEquals(1, reservado(TECLADO));
        assertEquals(3, disponivel(MOUSE));
        assertEquals(EstadoReservaEstoque.ATIVA, estadoReserva(pedido.get("numeroPedido").asText()));
        assertTrue(portaCarrinho.obterResumoCarrinhoParaCheckout(
                portaCarrinho.resolverIdentificadorCarrinhoPorEmail(email), null).estaVazio());

        // Histórico do cliente
        MvcResult historico = mockMvc.perform(get("/api/v1/pedidos/me").with(user(email).roles("CLIENTE"))).andReturn();
        assertEquals(200, historico.getResponse().getStatus());
        assertEquals(1, json(historico).size());
    }

    @Test
    @DisplayName("Sem endereço selecionado: 400 antes de reservar estoque ou criar pedido")
    void checkoutSemEnderecoDeveSerRejeitado() throws Exception {
        String email = criarClienteSemEndereco("semendereco");
        adicionarAoCarrinho(email, TECLADO, 1);

        MvcResult resultado = postarCheckout(email, "PIX", "CORREIOS_PAC", null, null);

        assertEquals(400, resultado.getResponse().getStatus());
        assertTrue(resultado.getResponse().getContentAsString().contains("endereço"));
        assertEquals(10, disponivel(TECLADO));
        assertEquals(0, pedidoRepository.count());
    }

    @Test
    @DisplayName("Carrinho vazio: 400")
    void checkoutComCarrinhoVazioDeveSerRejeitado() throws Exception {
        String email = criarClienteComEndereco("vazio");

        MvcResult resultado = postarCheckout(email, "PIX", "CORREIOS_PAC", null, null);

        assertEquals(400, resultado.getResponse().getStatus());
        assertEquals(0, pedidoRepository.count());
    }

    @Test
    @DisplayName("Última unidade: o segundo cliente recebe 409 e nenhum pedido dele é criado")
    void segundoClienteSemEstoqueDeveReceber409() throws Exception {
        String clienteA = criarClienteComEndereco("clientea");
        String clienteB = criarClienteComEndereco("clienteb");
        adicionarAoCarrinho(clienteA, MOUSE, 5);
        adicionarAoCarrinho(clienteB, MOUSE, 5);

        checkoutComSucesso(clienteA, "PIX", "CORREIOS_PAC", null);
        MvcResult resultadoB = postarCheckout(clienteB, "PIX", "CORREIOS_PAC", null, null);

        assertEquals(409, resultadoB.getResponse().getStatus());
        assertEquals(1, pedidoRepository.count());
        assertEquals(0, disponivel(MOUSE));
        assertFalse(portaCarrinho.obterResumoCarrinhoParaCheckout(
                portaCarrinho.resolverIdentificadorCarrinhoPorEmail(clienteB), null).estaVazio(),
                "O carrinho de quem não conseguiu comprar continua intacto");
    }

    @Test
    @DisplayName("Checkout repetido com a mesma chave de idempotência devolve o mesmo pedido sem reservar de novo")
    void checkoutRepetidoDeveSerIdempotente() throws Exception {
        String email = criarClienteComEndereco("repetido");
        adicionarAoCarrinho(email, TECLADO, 1);
        String chave = "CHK-" + UUID.randomUUID().toString().substring(0, 8);

        JsonNode primeiro = json(postarCheckout(email, "BOLETO", "CORREIOS_PAC", chave, null));
        MvcResult segundoResultado = postarCheckout(email, "BOLETO", "CORREIOS_PAC", chave, null);
        JsonNode segundo = json(segundoResultado);

        assertEquals(201, segundoResultado.getResponse().getStatus());
        assertEquals(primeiro.get("pedidoId").asText(), segundo.get("pedidoId").asText());
        assertFalse(primeiro.get("reaproveitado").asBoolean());
        assertTrue(segundo.get("reaproveitado").asBoolean());
        assertEquals(1, pedidoRepository.count());
        assertEquals(1, reservado(TECLADO));
    }

    @Test
    @DisplayName("Cartão aprovado online: pedido PAGO e baixa definitiva imediata do estoque")
    void cartaoAprovadoDeveFicarPagoComBaixaDefinitiva() throws Exception {
        String email = criarClienteComEndereco("cartao");
        adicionarAoCarrinho(email, TECLADO, 1);

        JsonNode pedido = checkoutComSucesso(email, "CARTAO_CREDITO", "TRANSPORTADORA_EXPRESSA", "4111111111111111");

        assertEquals("PAGO", pedido.get("status").asText());
        assertEquals("CONFIRMADA", pedido.get("dadosPagamento").get("statusTransacao").asText());
        assertEquals(9, disponivel(TECLADO));
        assertEquals(0, reservado(TECLADO));
        assertEquals(EstadoReservaEstoque.CONFIRMADA, estadoReserva(pedido.get("numeroPedido").asText()));
    }

    @Test
    @DisplayName("Cartão recusado: pedido PAGAMENTO_RECUSADO, reserva liberada e carrinho mantido")
    void cartaoRecusadoDeveLiberarReservaEManterCarrinho() throws Exception {
        String email = criarClienteComEndereco("recusado");
        adicionarAoCarrinho(email, TECLADO, 1);

        JsonNode pedido = checkoutComSucesso(email, "CARTAO_CREDITO", "CORREIOS_PAC", "4000000000000000");

        assertEquals("PAGAMENTO_RECUSADO", pedido.get("status").asText());
        assertEquals(10, disponivel(TECLADO));
        assertEquals(0, reservado(TECLADO));
        assertEquals(EstadoReservaEstoque.LIBERADA, estadoReserva(pedido.get("numeroPedido").asText()));
        assertFalse(portaCarrinho.obterResumoCarrinhoParaCheckout(
                portaCarrinho.resolverIdentificadorCarrinhoPorEmail(email), null).estaVazio());
    }

    @Test
    @DisplayName("Cartão sem número: 400 antes de qualquer efeito colateral (hook do Template Method)")
    void cartaoSemNumeroDeveSerRejeitado() throws Exception {
        String email = criarClienteComEndereco("semcartao");
        adicionarAoCarrinho(email, TECLADO, 1);

        MvcResult resultado = postarCheckout(email, "CARTAO_CREDITO", "CORREIOS_PAC", null, null);

        assertEquals(400, resultado.getResponse().getStatus());
        assertEquals(0, reservado(TECLADO));
        assertEquals(0, pedidoRepository.count());
    }

    @Test
    @DisplayName("Frete grátis do PAC entra no snapshot do pedido quando o carrinho é qualificado")
    void checkoutQualificadoDeveTerFreteGratisNoPac() throws Exception {
        String email = criarClienteComEndereco("fretegratis");
        adicionarAoCarrinho(email, TECLADO, 1);

        JsonNode pedido = checkoutComSucesso(email, "PIX", "CORREIOS_PAC", null);

        JsonNode financeiro = pedido.get("snapshotFinanceiro");
        if (financeiro.get("subtotalBruto").decimalValue().subtract(financeiro.get("totalDescontos").decimalValue())
                .compareTo(new BigDecimal("250.00")) >= 0) {
            assertTrue(financeiro.get("freteGratis").asBoolean());
            assertEquals(0, BigDecimal.ZERO.compareTo(financeiro.get("valorFrete").decimalValue()));
        } else {
            assertFalse(financeiro.get("freteGratis").asBoolean());
        }
    }

    @Test
    @DisplayName("Cotação de frete é pública e retorna 3 modalidades")
    void cotacaoDeFreteDeveSerPublica() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/frete/calcular")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cepDestino\":\"01310-100\",\"pesoTotalGramas\":1700,\"subtotalLiquido\":490.00}"))
                .andReturn();

        assertEquals(200, resultado.getResponse().getStatus());
        JsonNode resposta = json(resultado);
        assertEquals(3, resposta.get("opcoes").size());
        assertTrue(resposta.get("freteGratisDisponivel").asBoolean());
        assertEquals("CORREIOS_PAC", resposta.get("opcoes").get(0).get("modalidade").asText());
    }
}
