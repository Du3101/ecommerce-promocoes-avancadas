package br.edu.fatec.ecommerce.modulo3.relatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import br.edu.fatec.ecommerce.modulo3.BaseIntegracaoModulo3Test;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Testes de integração dos relatórios gerenciais (T036).
 *
 * @author Henrique Sandrini (Integrante 3)
 */
class RelatoriosGerenciaisAdminTest extends BaseIntegracaoModulo3Test {

    /**
     * Chama um relatório como administrador.
     *
     * @param caminho sufixo da rota
     * @return JSON da resposta
     * @throws Exception erro do MockMvc
     */
    private JsonNode relatorio(String caminho) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/admin/relatorios/" + caminho)
                .with(user(EMAIL_ADMIN).roles("ADMIN"))).andReturn();
        assertEquals(200, resultado.getResponse().getStatus());
        return json(resultado);
    }

    @Test
    @DisplayName("Relatórios consideram só pedidos pagos no faturamento e contam recusas na taxa de cancelamento")
    void relatoriosDevemSerAritmeticamenteConsistentes() throws Exception {
        String email = criarClienteComEndereco("relatorio");
        adicionarAoCarrinho(email, TECLADO, 1);
        JsonNode pago = checkoutComSucesso(email, "CARTAO_CREDITO", "CORREIOS_SEDEX", "4111111111111111");
        adicionarAoCarrinho(email, MOUSE, 1);
        checkoutComSucesso(email, "PIX", "CORREIOS_SEDEX", null); // pendente: fora do faturamento
        adicionarAoCarrinho(email, MOUSE, 1);
        checkoutComSucesso(email, "CARTAO_CREDITO", "CORREIOS_SEDEX", "4000000000000000"); // recusado
        BigDecimal totalPago = pago.get("snapshotFinanceiro").get("valorTotalLiquido").decimalValue();

        JsonNode vendas = relatorio("vendas-consolidadas");
        assertEquals(1, vendas.get("quantidadePedidosFaturados").asLong());
        assertEquals(0, totalPago.compareTo(vendas.get("faturamentoLiquido").decimalValue()));
        assertEquals(0, new BigDecimal("300.00").compareTo(vendas.get("faturamentoBruto").decimalValue()));

        JsonNode ticket = relatorio("ticket-medio");
        assertEquals(1, ticket.get("quantidadeClientesCompradores").asLong());
        assertEquals(0, totalPago.compareTo(ticket.get("ticketMedioPorPedido").decimalValue()));

        JsonNode ranking = relatorio("produtos-mais-vendidos");
        assertEquals(1, ranking.size());
        assertEquals(TECLADO, ranking.get(0).get("sku").asText());
        assertEquals(1, ranking.get(0).get("quantidadeVendida").asLong());

        JsonNode cancelamentos = relatorio("cancelamentos");
        assertEquals(3, cancelamentos.get("totalPedidos").asLong());
        assertEquals(1, cancelamentos.get("pedidosComPagamentoRecusado").asLong());
        assertEquals(0, new BigDecimal("33.33").compareTo(cancelamentos.get("taxaCancelamentoPercentual").decimalValue()));
        assertTrue(cancelamentos.get("motivosMaisFrequentes").get(0).get("motivo").asText().contains("limite"));
    }

    @Test
    @DisplayName("Cliente não acessa relatórios gerenciais (403)")
    void clienteNaoDeveAcessarRelatorios() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/admin/relatorios/vendas-consolidadas")
                .with(user("qualquer@teste.com").roles("CLIENTE"))).andReturn();
        assertEquals(403, resultado.getResponse().getStatus());
    }
}
