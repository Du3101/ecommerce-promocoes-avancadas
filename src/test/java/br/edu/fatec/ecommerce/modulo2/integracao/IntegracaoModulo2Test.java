package br.edu.fatec.ecommerce.modulo2.integracao;

import br.edu.fatec.ecommerce.modulo1.catalogo.infra.InicializadorDadosDemonstracaoModulo1Runner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração ponta a ponta dos endpoints REST e da porta Java do Módulo 2.
 *
 * @author Eduardo Juncker Toras (Integrante 2)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IntegracaoModulo2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PortaConsultaCarrinhoPromocoes portaCarrinho;

    @Autowired
    private InicializadorDadosDemonstracaoModulo1Runner seedDemonstracaoRunner;

    @BeforeEach
    void setUp() {
        seedDemonstracaoRunner.executarCargaDemonstracaoIdempotente();
    }

    @Test
    @DisplayName("REST API: Ciclo de vida do carrinho, adição de itens de demonstração, aplicação de cupom e formação de preço")
    void deveManipularCarrinhoEEfetuarCalculoDePrecoViaRest() throws Exception {
        String sessaoId = "sessao-teste-" + UUID.randomUUID();

        // 1. Adicionar Teclado DEMO-TEC-PRO-PRETO (R$ 300,00)
        String payloadItem1 = objectMapper.writeValueAsString(Map.of(
                "sku", "DEMO-TEC-PRO-PRETO",
                "quantidade", 1
        ));

        mockMvc.perform(post("/api/v1/carrinho/itens")
                        .param("sessaoId", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadItem1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeTotalItens", is(1)))
                .andExpect(jsonPath("$.pesoTotalGramas", is(850)))
                .andExpect(jsonPath("$.memoriaCalculo.subtotalBruto", is(300.00)));

        // 2. Adicionar Mouse DEMO-MOU-PREC-PRETO (R$ 150,00) -> Subtotal R$ 450,00
        // Ativa Promoção 4 (Combo Casado Teclado + Mouse: 20% no par = R$ 90,00 OFF)
        // Ativa Promoção 3 (R$ 50 OFF acima de R$ 300)
        String payloadItem2 = objectMapper.writeValueAsString(Map.of(
                "sku", "DEMO-MOU-PREC-PRETO",
                "quantidade", 1
        ));

        mockMvc.perform(post("/api/v1/carrinho/itens")
                        .param("sessaoId", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadItem2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantidadeTotalItens", is(2)))
                .andExpect(jsonPath("$.memoriaCalculo.subtotalBruto", is(450.00)))
                .andExpect(jsonPath("$.memoriaCalculo.totalDescontos", greaterThanOrEqualTo(50.00)));

        // 3. Aplicar cupom BEMVINDO10
        String payloadCupom = objectMapper.writeValueAsString(Map.of(
                "codigoCupom", "BEMVINDO10"
        ));

        mockMvc.perform(post("/api/v1/carrinho/cupom")
                        .param("sessaoId", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadCupom))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cupomCodigo", is("BEMVINDO10")));

        // 4. Consultar carrinho consolidado
        mockMvc.perform(get("/api/v1/carrinho")
                        .param("sessaoId", sessaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.memoriaCalculo.totalLiquido", greaterThanOrEqualTo(0.00)));

        // 5. Remover cupom
        mockMvc.perform(delete("/api/v1/carrinho/cupom")
                        .param("sessaoId", sessaoId))
                .andExpect(status().isOk());

        // 6. Esvaziar carrinho
        mockMvc.perform(delete("/api/v1/carrinho")
                        .param("sessaoId", sessaoId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("REST API Admin: Endpoints de promoções e cupons com segurança RBAC")
    @WithMockUser(username = "admin@ecommerce.fatec.edu.br", roles = {"ADMIN"})
    void deveAcessarEndpointsAdminComPerfilAutorizado() throws Exception {
        mockMvc.perform(get("/api/v1/admin/promocoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))));

        mockMvc.perform(get("/api/v1/admin/cupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("REST API Admin: Cliente comum deve receber 403 Forbidden ao tentar acessar admin")
    @WithMockUser(username = "cliente@exemplo.com", roles = {"CLIENTE"})
    void deveNegarAcessoAdminParaCliente() throws Exception {
        mockMvc.perform(get("/api/v1/admin/promocoes"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/cupons"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Porta de Integração: PortaConsultaCarrinhoPromocoes deve fornecer snapshot consistente para Checkout (Módulo 3)")
    void deveIntegrarComModulo3ViaPortaDesacoplada() throws Exception {
        String sessaoId = "sessao-checkout-" + UUID.randomUUID();

        // Adiciona itens
        String payload = objectMapper.writeValueAsString(Map.of(
                "sku", "DEMO-TEC-PRO-PRETO",
                "quantidade", 2
        ));
        mockMvc.perform(post("/api/v1/carrinho/itens")
                        .param("sessaoId", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Consome a porta interna fornecida para o Módulo 3
        ResumoCarrinhoCheckoutDTO resumo = portaCarrinho.obterResumoCarrinhoParaCheckout(null, sessaoId);

        assertNotNull(resumo);
        assertEquals(1, resumo.quantidadePorSku().size());
        assertEquals(2, resumo.quantidadePorSku().get("DEMO-TEC-PRO-PRETO"));
        assertEquals(1700, resumo.pesoTotalGramas());
        assertEquals(new BigDecimal("600.00"), resumo.subtotalBruto());

        // Limpeza após checkout
        portaCarrinho.esvaziarCarrinhoAposCheckout(null, sessaoId);
        ResumoCarrinhoCheckoutDTO resumoAposCheckout = portaCarrinho.obterResumoCarrinhoParaCheckout(null, sessaoId);
        assertTrue(resumoAposCheckout.quantidadePorSku().isEmpty());
    }
}
