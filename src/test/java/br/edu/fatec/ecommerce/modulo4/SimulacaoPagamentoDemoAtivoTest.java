package br.edu.fatec.ecommerce.modulo4;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.fatec.ecommerce.modulo1.catalogo.infra.InicializadorDadosDemonstracaoModulo1Runner;
import br.edu.fatec.ecommerce.modulo1.identidade.aplicacao.ServicoIdentidadeECliente;
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
 * Testes de integração dedicados ao modo demonstração ativo ({@code ecommerce.modulo3.seed-demo-ativo=true}).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Valida a coerência entre {@code GET /api/v1/loja/configuracao} e a simulação controlada de pagamento
 * em {@code SimulacaoPagamentoDemoController} (autorização de dono, transição para {@code PAGO}
 * e idempotência estrita em reexecução).</p>
 */
@SpringBootTest(properties = "ecommerce.modulo3.seed-demo-ativo=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SimulacaoPagamentoDemoAtivoTest {

    private static final String SKU_TECLADO = InicializadorDadosDemonstracaoModulo1Runner.SKU_TECLADO_DEMO;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InicializadorDadosDemonstracaoModulo1Runner seedCatalogo;

    @Autowired
    private ServicoIdentidadeECliente servicoIdentidade;

    /**
     * Garante catálogo e contas de demonstração carregados na transação de teste.
     *
     * <p>Objetivo Funcional: Disponibilizar itens vendáveis antes dos fluxos de checkout demo.</p>
     * <p>Papel no Design Pattern: Preparação idempotente de fixture.</p>
     */
    @BeforeEach
    void prepararDados() {
        seedCatalogo.executarCargaDemonstracaoIdempotente();
    }

    /**
     * Cadastra cliente exclusivo com endereço ativo em São Paulo para o teste.
     *
     * <p>Objetivo Funcional: Criar titular único por cenário evitando colisão de dados.</p>
     * <p>Papel no Design Pattern: Test Data Builder auxiliar encapsulado na classe de teste.</p>
     *
     * @param prefixo prefixo de identificação do e-mail
     * @return e-mail do cliente criado
     */
    private String criarClienteComEndereco(String prefixo) {
        String email = prefixo + "." + UUID.randomUUID().toString().substring(0, 8) + "@fatec.edu.br";
        servicoIdentidade.cadastrarClientePublico("Demo " + prefixo, email, gerarCpfValido(), "11977776666", "ClienteFatec@2026");
        servicoIdentidade.adicionarEnderecoAoCliente(
            email,
            "Casa Demo",
            "01310100",
            "Av. Paulista",
            "1500",
            null,
            "Bela Vista",
            "São Paulo",
            "SP",
            true
        );
        return email;
    }

    /**
     * Gera CPF aleatório com dígitos verificadores válidos.
     *
     * <p>Objetivo Funcional: Atender às regras de validação de CPF no cadastro de clientes.</p>
     * <p>Papel no Design Pattern: Utilitário local de fixture.</p>
     *
     * @return CPF válido de 11 dígitos
     */
    private String gerarCpfValido() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(1, 9);
        }
        int s1 = 0;
        for (int i = 0; i < 9; i++) {
            s1 += d[i] * (10 - i);
        }
        int r1 = (s1 * 10) % 11;
        d[9] = r1 == 10 ? 0 : r1;

        int s2 = 0;
        for (int i = 0; i < 10; i++) {
            s2 += d[i] * (11 - i);
        }
        int r2 = (s2 * 10) % 11;
        d[10] = r2 == 10 ? 0 : r2;

        StringBuilder sb = new StringBuilder(11);
        for (int n : d) {
            sb.append(n);
        }
        return sb.toString();
    }

    /**
     * Valida que com {@code seed-demo-ativo=true}: a configuração pública reflete modo demo ativo,
     * outro cliente recebe 403 ao tentar simular pedido alheio, o dono consegue aprovar PIX para {@code PAGO}
     * e a segunda simulação comprova idempotência sem duplicação ({@code idempotenteReexecutado=true}).
     *
     * <p>Objetivo Funcional: Comprovar segurança de propriedade e idempotência da simulação demo (T029.2).</p>
     * <p>Papel no Design Pattern: Verificação ponta a ponta do Proxy controlador sobre o Webhook oficial.</p>
     *
     * @throws Exception em falha na execução MockMvc
     */
    @Test
    @DisplayName("T029.2 - Deve sinalizar modoDemonstracaoAtivo=true, validar dono e confirmar idempotência na simulação demo")
    public void devePermitirSimulacaoAoDonoEComprovarIdempotenciaQuandoDemoAtivo() throws Exception {
        mockMvc.perform(get("/api/v1/loja/configuracao"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modoDemonstracaoAtivo", is(true)));

        String dono = criarClienteComEndereco("dono");
        String outroCliente = criarClienteComEndereco("intruso");

        mockMvc.perform(post("/api/v1/carrinho/itens")
                .with(user(dono).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sku", SKU_TECLADO, "quantidade", 1))))
            .andExpect(status().isCreated());

        String respostaCheckout = mockMvc.perform(post("/api/v1/checkout")
                .with(user(dono).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "metodoPagamento", "PIX",
                    "modalidadeFrete", "CORREIOS_PAC"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status", is("AGUARDANDO_PAGAMENTO")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String pedidoId = objectMapper.readTree(respostaCheckout).get("pedidoId").asText();

        // Outro cliente não pode simular confirmação de pedido alheio
        mockMvc.perform(post("/api/v1/pedidos/" + pedidoId + "/simular-confirmacao-pagamento")
                .with(user(outroCliente).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sucesso", true))))
            .andExpect(status().isForbidden());

        // Primeira simulação pelo dono: transita AGUARDANDO_PAGAMENTO -> PAGO
        mockMvc.perform(post("/api/v1/pedidos/" + pedidoId + "/simular-confirmacao-pagamento")
                .with(user(dono).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sucesso", true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusProcessamento", is("PROCESSADO_SUCESSO")))
            .andExpect(jsonPath("$.novoStatusPedido", is("PAGO")))
            .andExpect(jsonPath("$.idempotenteReexecutado", is(false)));

        // Segunda simulação idêntica: comprova idempotenteReexecutado=true
        mockMvc.perform(post("/api/v1/pedidos/" + pedidoId + "/simular-confirmacao-pagamento")
                .with(user(dono).roles("CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("sucesso", true))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.novoStatusPedido", is("PAGO")))
            .andExpect(jsonPath("$.idempotenteReexecutado", is(true)));
    }
}
