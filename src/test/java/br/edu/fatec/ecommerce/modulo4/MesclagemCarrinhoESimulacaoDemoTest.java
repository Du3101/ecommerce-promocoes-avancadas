package br.edu.fatec.ecommerce.modulo4;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.fatec.ecommerce.modulo1.catalogo.infra.InicializadorDadosDemonstracaoModulo1Runner;
import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
import br.edu.fatec.ecommerce.modulo2.carrinho.infraestrutura.CarrinhoComprasJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração para carrinho anônimo via {@code X-Sessao-Carrinho}, mesclagem de carrinho
 * visitante→cliente (US2/US3), simulação controlada de pagamento demo e listagem admin (US4/US5).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Valida os fluxos ponta a ponta da camada web com os Módulos 1, 2 e 3 sobre banco H2 transacional.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MesclagemCarrinhoESimulacaoDemoTest {

    private static final String HEADER_SESSAO = "X-Sessao-Carrinho";
    private static final String SKU_TECLADO = InicializadorDadosDemonstracaoModulo1Runner.SKU_TECLADO_DEMO;
    private static final String SKU_MOUSE = InicializadorDadosDemonstracaoModulo1Runner.SKU_MOUSE_DEMO;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InicializadorDadosDemonstracaoModulo1Runner seedCatalogo;

    @Autowired
    private ServicoIdentidadeECliente servicoIdentidade;

    @Autowired
    private CarrinhoComprasJpaRepository carrinhoRepository;

    /**
     * Garante catálogo e usuários de demonstração do Módulo 1 semeados antes de cada cenário.
     *
     * <p>Objetivo Funcional: Disponibilizar SKUs reais {@code DEMO-TEC-PRO-PRETO} e {@code DEMO-MOU-PREC-PRETO}.</p>
     * <p>Papel no Design Pattern: Fixture idempotente de teste.</p>
     */
    @BeforeEach
    void prepararDadosDemonstracao() {
        seedCatalogo.executarCargaDemonstracaoIdempotente();
    }

    /**
     * Cria cliente de teste com ou sem endereço cadastrado e devolve o e-mail utilizado como principal HTTP.
     *
     * <p>Objetivo Funcional: Isolar criação de clientes únicos entre testes transacionais.</p>
     * <p>Papel no Design Pattern: Test Data Builder auxiliar local à suíte.</p>
     *
     * @param prefixo prefixo identificador do e-mail
     * @param comEndereco se deve vincular endereço selecionado em SP
     * @return e-mail do cliente criado
     */
    private String criarClienteTeste(String prefixo, boolean comEndereco) {
        String email = prefixo + "." + UUID.randomUUID().toString().substring(0, 8) + "@fatec.edu.br";
        String cpf = gerarCpfValidoAleatorio();
        servicoIdentidade.cadastrarClientePublico("Cliente " + prefixo, email, cpf, "11988887777", "ClienteFatec@2026");
        if (comEndereco) {
            servicoIdentidade.adicionarEnderecoAoCliente(
                email,
                "Casa",
                "01310100",
                "Av. Paulista",
                "1000",
                "Sala 1",
                "Bela Vista",
                "São Paulo",
                "SP",
                true
            );
        }
        return email;
    }

    /**
     * Gera um CPF matematicamente válido para cadastro no Módulo 1.
     *
     * <p>Objetivo Funcional: Satisfazer validação de dígito verificador em cadastros dinâmicos.</p>
     * <p>Papel no Design Pattern: Gerador determinístico de fixture.</p>
     *
     * @return 11 dígitos numéricos válidos
     */
    private String gerarCpfValidoAleatorio() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(1, 9);
        }
        int soma1 = 0;
        for (int i = 0; i < 9; i++) {
            soma1 += d[i] * (10 - i);
        }
        int r1 = (soma1 * 10) % 11;
        d[9] = r1 == 10 ? 0 : r1;

        int soma2 = 0;
        for (int i = 0; i < 10; i++) {
            soma2 += d[i] * (11 - i);
        }
        int r2 = (soma2 * 10) % 11;
        d[10] = r2 == 10 ? 0 : r2;

        StringBuilder sb = new StringBuilder(11);
        for (int digito : d) {
            sb.append(digito);
        }
        return sb.toString();
    }

    /**
     * Valida o ciclo completo de visitante anônimo usando exclusivamente o cabeçalho HTTP
     * {@code X-Sessao-Carrinho}: inclusão, ajuste de quantidade, cupom válido/inválido e memória de cálculo.
     *
     * <p>Objetivo Funcional: Comprovar que o visitante gerencia o carrinho sem cookie e sem login (T020).</p>
     * <p>Papel no Design Pattern: Integração HTTP com {@code ServicoCarrinhoCompras} e {@code PipelineFormacaoPrecoService}.</p>
     *
     * @throws Exception em falha na execução MockMvc
     */
    @Test
    @DisplayName("T020 - Deve persistir carrinho anônimo via header X-Sessao-Carrinho e calcular cupom e descontos")
    public void deveGerenciarCarrinhoVisitanteViaHeaderXSessaoCarrinho() throws Exception {
        String sessao = "vis-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/carrinho/itens")
                .header(HEADER_SESSAO, sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_TECLADO, "quantidade", 1))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessaoId", is(sessao)))
            .andExpect(jsonPath("$.quantidadeTotalItens", is(1)))
            .andExpect(jsonPath("$.memoriaCalculo.subtotalBruto", is(300.00)));

        mockMvc.perform(put("/api/v1/carrinho/itens/" + SKU_TECLADO)
                .header(HEADER_SESSAO, sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantidade", 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantidadeTotalItens", is(2)))
            .andExpect(jsonPath("$.memoriaCalculo.subtotalBruto", is(600.00)));

        mockMvc.perform(post("/api/v1/carrinho/cupom")
                .header(HEADER_SESSAO, sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("codigoCupom", "BEMVINDO10"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cupomCodigo", is("BEMVINDO10")))
            .andExpect(jsonPath("$.memoriaCalculo.totalDescontos", greaterThan(0.0)));

        mockMvc.perform(post("/api/v1/carrinho/cupom")
                .header(HEADER_SESSAO, sessao)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("codigoCupom", "CUPOMINEXISTENTE999"))))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/carrinho")
                .header(HEADER_SESSAO, sessao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itens", hasSize(1)))
            .andExpect(jsonPath("$.itens[0].quantidade", is(2)))
            .andExpect(jsonPath("$.cupomCodigo", is("BEMVINDO10")));
    }

    /**
     * Valida a mesclagem aditiva do carrinho visitante no carrinho do cliente ao autenticar (T025):
     * soma quantidades de SKUs iguais, copia cupom válido quando cliente ainda não possui, não sobrescreve
     * cupom pré-existente do cliente, remove o carrinho visitante e rejeita chamadas sem login.
     *
     * <p>Objetivo Funcional: Garantir transição contínua do carrinho ao autenticar na interface web.</p>
     * <p>Papel no Design Pattern: Validação ponta a ponta de {@code POST /api/v1/carrinho/mesclar}.</p>
     *
     * @throws Exception em falha na execução MockMvc
     */
    @Test
    @DisplayName("T025 - Deve mesclar carrinho visitante aditivamente no login, preservar cupom e exigir CLIENTE")
    public void deveMesclarCarrinhoVisitanteAditivamenteSemSobrescreverCupomExistente() throws Exception {
        mockMvc.perform(post("/api/v1/carrinho/mesclar")
                .header(HEADER_SESSAO, "sessao-anonima-qualquer")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"));

        String emailClienteA = criarClienteTeste("mergea", false);
        String sessaoA = "merge-a-" + UUID.randomUUID();

        // Cliente A já possui 1 teclado sem cupom
        mockMvc.perform(post("/api/v1/carrinho/itens")
                .with(user(emailClienteA).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_TECLADO, "quantidade", 1))))
            .andExpect(status().isCreated());

        // Visitante A adicionou 2 teclados + 1 mouse + cupom BEMVINDO10
        mockMvc.perform(post("/api/v1/carrinho/itens")
                .header(HEADER_SESSAO, sessaoA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_TECLADO, "quantidade", 2))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/carrinho/itens")
                .header(HEADER_SESSAO, sessaoA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_MOUSE, "quantidade", 1))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/carrinho/cupom")
                .header(HEADER_SESSAO, sessaoA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("codigoCupom", "BEMVINDO10"))))
            .andExpect(status().isOk());

        // Mescla: 1 + 2 = 3 teclados, 1 mouse, cupom BEMVINDO10 copiado
        mockMvc.perform(post("/api/v1/carrinho/mesclar")
                .with(user(emailClienteA).roles("CLIENTE"))
                .header(HEADER_SESSAO, sessaoA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantidadeTotalItens", is(4)))
            .andExpect(jsonPath("$.cupomCodigo", is("BEMVINDO10")));

        assertTrue(carrinhoRepository.findBySessaoId(sessaoA).isEmpty(),
            "Carrinho da sessão visitante deve ser excluído após a mesclagem.");

        // Cenário 2: Cliente B já possui cupom FATEC50 -> cupom visitante BEMVINDO10 não sobrescreve
        String emailClienteB = criarClienteTeste("mergeb", false);
        String sessaoB = "merge-b-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/carrinho/itens")
                .with(user(emailClienteB).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_TECLADO, "quantidade", 2))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/carrinho/cupom")
                .with(user(emailClienteB).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("codigoCupom", "FATEC50"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/carrinho/itens")
                .header(HEADER_SESSAO, sessaoB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_MOUSE, "quantidade", 1))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/carrinho/cupom")
                .header(HEADER_SESSAO, sessaoB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("codigoCupom", "BEMVINDO10"))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/carrinho/mesclar")
                .with(user(emailClienteB).roles("CLIENTE"))
                .header(HEADER_SESSAO, sessaoB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantidadeTotalItens", is(3)))
            .andExpect(jsonPath("$.cupomCodigo", is("FATEC50")));
    }

    /**
     * Verifica que, quando {@code ecommerce.modulo3.seed-demo-ativo=false} (perfil {@code test}),
     * a simulação controlada de pagamento retorna HTTP 403 Forbidden (T029).
     *
     * <p>Objetivo Funcional: Garantir que o botão demo nunca opere em produção com flag desligada.</p>
     * <p>Papel no Design Pattern: Guarda de segurança ambiental em {@code SimulacaoPagamentoDemoController}.</p>
     *
     * @throws Exception em falha na execução MockMvc
     */
    @Test
    @DisplayName("T029.1 - Deve bloquear simulação demo de pagamento (HTTP 403) quando seed-demo-ativo=false")
    public void deveBloquearSimulacaoPagamentoDemoQuandoModoDemonstracaoDesativado() throws Exception {
        String email = criarClienteTeste("sem.demo", true);

        mockMvc.perform(post("/api/v1/carrinho/itens")
                .with(user(email).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_TECLADO, "quantidade", 1))))
            .andExpect(status().isCreated());

        String respostaCheckout = mockMvc.perform(post("/api/v1/checkout")
                .with(user(email).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "metodoPagamento", "PIX",
                    "modalidadeFrete", "CORREIOS_PAC"
                ))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String pedidoId = objectMapper.readTree(respostaCheckout).get("pedidoId").asText();

        mockMvc.perform(post("/api/v1/pedidos/" + pedidoId + "/simular-confirmacao-pagamento")
                .with(user(email).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sucesso", true))))
            .andExpect(status().isForbidden());
    }

    /**
     * Verifica {@code GET /api/v1/admin/pedidos} (T035): acesso negado a {@code CLIENTE} (403),
     * acesso permitido para {@code ADMIN} e {@code OPERADOR_COMERCIAL}, ordenação decrescente e filtro {@code ?status=PAGO}.
     *
     * <p>Objetivo Funcional: Validar a listagem geral de pedidos usada pela aba de expedição do painel.</p>
     * <p>Papel no Design Pattern: Verificação RBAC e Query Method ordenada no endpoint administrativo.</p>
     *
     * @throws Exception em falha na execução MockMvc
     */
    @Test
    @DisplayName("T035 - Deve listar pedidos em /api/v1/admin/pedidos por ordem decrescente e filtrar por status com RBAC")
    public void deveListarPedidosAdminComRbacOrdenacaoEFiltroPorStatus() throws Exception {
        String emailCliente = criarClienteTeste("adminlist", true);

        // Pedido 1 (mais antigo): PIX -> AGUARDANDO_PAGAMENTO
        mockMvc.perform(post("/api/v1/carrinho/itens")
                .with(user(emailCliente).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_TECLADO, "quantidade", 1))))
            .andExpect(status().isCreated());

        String resp1 = mockMvc.perform(post("/api/v1/checkout")
                .with(user(emailCliente).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "metodoPagamento", "PIX",
                    "modalidadeFrete", "CORREIOS_PAC"
                ))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        JsonNode json1 = objectMapper.readTree(resp1);
        String numeroPix = json1.get("numeroPedido").asText();

        // Pedido 2 (mais recente): Cartão aprovado -> PAGO
        mockMvc.perform(post("/api/v1/carrinho/itens")
                .with(user(emailCliente).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_MOUSE, "quantidade", 1))))
            .andExpect(status().isCreated());

        String resp2 = mockMvc.perform(post("/api/v1/checkout")
                .with(user(emailCliente).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "metodoPagamento", "CARTAO_CREDITO",
                    "modalidadeFrete", "CORREIOS_SEDEX",
                    "numeroCartaoFicticio", "4111111111111111",
                    "titularCartao", "TESTE ADMIN"
                ))))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String numeroPago = objectMapper.readTree(resp2).get("numeroPedido").asText();

        // CLIENTE comum deve receber 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/pedidos")
                .with(user(emailCliente).roles("CLIENTE")))
            .andExpect(status().isForbidden());

        // OPERADOR_COMERCIAL deve visualizar todos os pedidos do mais recente para o mais antigo
        mockMvc.perform(get("/api/v1/admin/pedidos")
                .with(user("operador@fatec.edu.br").roles("OPERADOR_COMERCIAL")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].numeroPedido", is(numeroPago)))
            .andExpect(jsonPath("$[*].numeroPedido", hasItem(numeroPix)));

        // ADMIN filtrando por status=PAGO vê somente pedidos PAGO
        mockMvc.perform(get("/api/v1/admin/pedidos")
                .param("status", "PAGO")
                .with(user("admin@ecommerce.fatec.edu.br").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].numeroPedido", hasItem(numeroPago)))
            .andExpect(jsonPath("$[?(@.status != 'PAGO')]").isEmpty());
    }
}
