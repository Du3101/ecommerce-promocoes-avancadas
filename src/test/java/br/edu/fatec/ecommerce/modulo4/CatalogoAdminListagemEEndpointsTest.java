package br.edu.fatec.ecommerce.modulo4;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.fatec.ecommerce.modulo1.catalogo.infra.InicializadorDadosDemonstracaoModulo1Runner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de integração MockMvc validando a listagem administrativa completa do catálogo
 * ({@code GET /api/v1/admin/catalogo/itens}) com inclusão de itens inativados, RBAC e reativação (T044).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CatalogoAdminListagemEEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InicializadorDadosDemonstracaoModulo1Runner seedCatalogo;

    /**
     * Garante catálogo de demonstração do Módulo 1 semeado antes de cada cenário.
     *
     * <p>Objetivo Funcional: Disponibilizar variações simples e combos reais para validar listagem,
     * inativação e reativação administrativa.</p>
     * <p>Papel no Design Pattern: Fixture idempotente de integração.</p>
     */
    @BeforeEach
    void prepararDadosDemonstracao() {
        this.seedCatalogo.executarCargaDemonstracaoIdempotente();
    }

    /**
     * Valida controle de acesso RBAC em {@code GET /api/v1/admin/catalogo/itens}:
     * exige perfil {@code ADMIN} ou {@code OPERADOR_COMERCIAL}, negando {@code CLIENTE} e {@code RESPONSAVEL_ESTOQUE} (HTTP 403).
     *
     * <p>Objetivo Funcional: Garantir segregação de papéis na consulta completa do catálogo administrativo.</p>
     * <p>Papel no Design Pattern: Verificação de segurança HTTP declarativa da camada de borda REST.</p>
     *
     * @throws Exception em caso de falha na execução MockMvc
     */
    @Test
    @DisplayName("T044.1 - Deve exigir ADMIN ou OPERADOR_COMERCIAL para listar todo o catálogo em /api/v1/admin/catalogo/itens")
    public void deveRestringirListagemCompletaDoCatalogoPorPerfilRbac() throws Exception {
        mockMvc.perform(get("/api/v1/admin/catalogo/itens")
                .with(user("cliente@ecommerce.fatec.edu.br").roles("CLIENTE")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/catalogo/itens")
                .with(user("estoque@ecommerce.fatec.edu.br").roles("RESPONSAVEL_ESTOQUE")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/catalogo/itens")
                .with(user("comercial@ecommerce.fatec.edu.br").roles("OPERADOR_COMERCIAL")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThan(0)));

        mockMvc.perform(get("/api/v1/admin/catalogo/itens")
                .with(user("admin@ecommerce.fatec.edu.br").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    /**
     * Valida o ciclo completo inativar -> listar no catálogo administrativo -> reativar:
     * após inativar um SKU via {@code PATCH /api/v1/admin/catalogo/itens/{sku}/status}, o endpoint público
     * {@code GET /api/v1/catalogo/itens} oculta o item, enquanto {@code GET /api/v1/admin/catalogo/itens}
     * mantém o item visível com {@code ativo = false} permitindo sua reativação imediata.
     *
     * <p>Objetivo Funcional: Assegurar que itens inativos continuem gerenciáveis e reativáveis no painel administrativo.</p>
     * <p>Papel no Design Pattern: Integração entre consulta <b>Composite</b> irrestrita e mutação auditada via <b>Proxy</b>.</p>
     *
     * @throws Exception em caso de falha na execução MockMvc
     */
    @Test
    @DisplayName("T044.2 - Deve manter item inativado visível em /api/v1/admin/catalogo/itens para reativação")
    public void deveManterItemInativadoVisivelParaReativacaoNoCatalogoAdmin() throws Exception {
        String listaJson = mockMvc.perform(get("/api/v1/admin/catalogo/itens")
                .with(user("admin@ecommerce.fatec.edu.br").roles("ADMIN")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode arrayItens = objectMapper.readTree(listaJson);
        String skuAlvo = null;
        for (JsonNode item : arrayItens) {
            if (!item.get("eComboKit").asBoolean() && item.get("ativo").asBoolean()) {
                skuAlvo = item.get("sku").asText();
                break;
            }
        }
        if (skuAlvo == null) {
            throw new IllegalStateException("Nenhum SKU simples ativo encontrado para teste de inativação.");
        }

        try {
            mockMvc.perform(patch("/api/v1/admin/catalogo/itens/" + skuAlvo + "/status")
                    .with(user("admin@ecommerce.fatec.edu.br").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("ativo", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", is(false)));

            mockMvc.perform(get("/api/v1/catalogo/itens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", not(hasItem(skuAlvo))));

            mockMvc.perform(get("/api/v1/admin/catalogo/itens")
                    .with(user("admin@ecommerce.fatec.edu.br").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.sku == '" + skuAlvo + "')].ativo", hasItem(false)));

            mockMvc.perform(patch("/api/v1/admin/catalogo/itens/" + skuAlvo + "/status")
                    .with(user("admin@ecommerce.fatec.edu.br").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("ativo", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo", is(true)));

            mockMvc.perform(get("/api/v1/catalogo/itens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", hasItem(skuAlvo)));
        } finally {
            mockMvc.perform(patch("/api/v1/admin/catalogo/itens/" + skuAlvo + "/status")
                    .with(user("admin@ecommerce.fatec.edu.br").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("ativo", true))));
        }
    }

    /**
     * Valida o alinhamento das tags OpenAPI entre os controladores REST dos Módulos 1 a 4
     * e a especificação agrupada exposta em {@code /v3/api-docs} (T048).
     *
     * <p>Objetivo Funcional: Garantir que {@code /api/v1/loja/configuracao} utilize a tag exata
     * {@code "Configuração Pública da Loja"} sem duplicação nem tags órfãs.</p>
     * <p>Papel no Design Pattern: Teste de regressão do contrato OpenAPI/Swagger UI.</p>
     *
     * @throws Exception em caso de falha na execução MockMvc
     */
    @Test
    @DisplayName("T048 - Deve expor tags OpenAPI alinhadas para Módulos 1, 2, 3 e Configuração Pública em /v3/api-docs")
    public void deveExporTagsOpenApiAlinhadasSemTagsOrfasNoSwaggerDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tags[*].name", hasItem("Configuração Pública da Loja")))
            .andExpect(jsonPath("$.tags[*].name", hasItem("Módulo 1 — Catálogo Público")))
            .andExpect(jsonPath("$.tags[*].name", hasItem("Módulo 2 — Carrinho de Compras")))
            .andExpect(jsonPath("$.tags[*].name", hasItem("Checkout")))
            .andExpect(jsonPath("$.paths['/api/v1/loja/configuracao'].get.tags",
                hasItem("Configuração Pública da Loja")))
            .andExpect(jsonPath("$.paths['/api/v1/loja/configuracao'].get.tags",
                not(hasItem("Loja Web — Configuração Pública"))));
    }
}
