package br.edu.fatec.ecommerce.modulo4;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.CampanhaPromocional;
import br.edu.fatec.ecommerce.modulo2.promocoes.dominio.TipoMecanicaPromocao;
import br.edu.fatec.ecommerce.modulo2.promocoes.infraestrutura.CampanhaPromocionalJpaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
 * Testes de integração do Portal Web e das extensões públicas de leitura da API (US1 / T016).
 *
 * <p><b>Papel Arquitetural no Design Pattern:</b>
 * Valida a entrega estática servida diretamente pelo Spring Boot, a configuração pública da loja,
 * a filtragem temporal de campanhas vigentes em {@code GET /api/v1/promocoes/ativas} e a resposta
 * JSON HTTP 401 sem cabeçalho {@code WWW-Authenticate} (evitando popup nativo no navegador).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PortalWebEEndpointsPublicosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampanhaPromocionalJpaRepository campanhaRepository;

    /**
     * Verifica se a raiz {@code /} entrega o HTML5 da loja virtual sem exigir autenticação.
     *
     * <p>Objetivo Funcional: Confirmar que a vitrine estática principal está acessível em HTTP 200.</p>
     * <p>Papel no Design Pattern: Verificação de borda HTTP na cadeia de segurança Spring Security.</p>
     *
     * @throws Exception caso ocorra erro na simulação MockMvc
     */
    @Test
    @DisplayName("T016.1 - Deve encaminhar GET / para index.html e entregar recursos estáticos sem autenticação (HTTP 200)")
    public void deveEntregarPortalHtmlNaRaizSemAutenticacao() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("FATEC Store")))
            .andExpect(content().string(containsString("src=\"/js/main.js\"")));

        mockMvc.perform(get("/css/tokens.css"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/js/main.js"))
            .andExpect(status().isOk());
    }

    /**
     * Verifica se {@code GET /api/v1/loja/configuracao} expõe política de frete grátis e
     * {@code modoDemonstracaoAtivo=false} sob o perfil {@code test} sem vazar segredos internos.
     *
     * <p>Objetivo Funcional: Validar contrato público de configuração usado pelo bootstrapper JavaScript.</p>
     * <p>Papel no Design Pattern: Verificação do DTO público {@code ConfiguracaoLojaPublicaDTO}.</p>
     *
     * @throws Exception caso ocorra erro na simulação MockMvc
     */
    @Test
    @DisplayName("T016.2 - Deve retornar configuração pública sem segredos e modoDemonstracaoAtivo=false no perfil test")
    public void deveRetornarConfiguracaoPublicaSemSegredos() throws Exception {
        mockMvc.perform(get("/api/v1/loja/configuracao"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.modoDemonstracaoAtivo").value(false))
            .andExpect(jsonPath("$.freteGratisValorMinimo").isNumber())
            .andExpect(jsonPath("$.freteGratisPesoMaximoGramas").isNumber())
            .andExpect(content().string(not(containsString("gateway-simulado"))))
            .andExpect(content().string(not(containsString("AdminFatec"))))
            .andExpect(content().string(not(containsString("ClienteFatec"))));
    }

    /**
     * Verifica se {@code GET /api/v1/promocoes/ativas} exclui campanhas inativas ou fora da
     * janela temporal e ordena campanhas vigentes por prioridade decrescente.
     *
     * <p>Objetivo Funcional: Garantir que apenas campanhas válidas apareçam nos banners da vitrine.</p>
     * <p>Papel no Design Pattern: Teste de filtragem pública preservando a ordenação do repositório.</p>
     *
     * @throws Exception caso ocorra erro na simulação MockMvc
     */
    @Test
    @DisplayName("T016.3 - Deve listar apenas campanhas ativas e vigentes ordenadas por prioridade decrescente")
    public void deveListarApenasCampanhasAtivasEVigentesOrdenadas() throws Exception {
        campanhaRepository.deleteAll();

        Instant agora = Instant.now();
        CampanhaPromocional vigenteAlta = new CampanhaPromocional(
            UUID.randomUUID(),
            "Promo Alta Prioridade T016",
            "Campanha vigente com prioridade máxima",
            TipoMecanicaPromocao.PERCENTUAL_CONDICIONAL,
            "{}",
            999,
            true,
            true,
            agora.minus(1, ChronoUnit.DAYS),
            agora.plus(10, ChronoUnit.DAYS),
            "{}",
            null
        );
        CampanhaPromocional vigenteMenor = new CampanhaPromocional(
            UUID.randomUUID(),
            "Promo Menor Prioridade T016",
            "Campanha vigente com prioridade menor",
            TipoMecanicaPromocao.VALOR_FIXO_META_CARRINHO,
            "{}",
            10,
            false,
            true,
            agora.minus(2, ChronoUnit.HOURS),
            null,
            "{}",
            null
        );
        CampanhaPromocional campanhaInativa = new CampanhaPromocional(
            UUID.randomUUID(),
            "Promo Inativa T016",
            "Não deve aparecer",
            TipoMecanicaPromocao.LEVE_X_PAGUE_Y,
            "{}",
            500,
            true,
            false,
            agora.minus(1, ChronoUnit.DAYS),
            agora.plus(10, ChronoUnit.DAYS),
            "{}",
            null
        );
        CampanhaPromocional campanhaExpirada = new CampanhaPromocional(
            UUID.randomUUID(),
            "Promo Expirada T016",
            "Janela encerrada",
            TipoMecanicaPromocao.COMBO_CRUZADO_CASADO,
            "{}",
            800,
            true,
            true,
            agora.minus(10, ChronoUnit.DAYS),
            agora.minus(1, ChronoUnit.HOURS),
            "{}",
            null
        );

        campanhaRepository.save(vigenteMenor);
        campanhaRepository.save(campanhaInativa);
        campanhaRepository.save(campanhaExpirada);
        campanhaRepository.save(vigenteAlta);

        mockMvc.perform(get("/api/v1/promocoes/ativas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].nome").value("Promo Alta Prioridade T016"))
            .andExpect(jsonPath("$[0].prioridade").value(999))
            .andExpect(jsonPath("$[1].nome").value("Promo Menor Prioridade T016"))
            .andExpect(jsonPath("$[*].nome", not(hasItem("Promo Inativa T016"))))
            .andExpect(jsonPath("$[*].nome", not(hasItem("Promo Expirada T016"))));
    }

    /**
     * Verifica que chamadas não autenticadas às rotas protegidas da API retornam HTTP 401 JSON
     * sem o cabeçalho {@code WWW-Authenticate}, evitando popup nativo do navegador.
     *
     * <p>Objetivo Funcional: Assegurar fluxo de login integrado dentro da própria página web.</p>
     * <p>Papel no Design Pattern: Validação do {@code AuthenticationEntryPoint} customizado.</p>
     *
     * @throws Exception caso ocorra erro na simulação MockMvc
     */
    @Test
    @DisplayName("T016.4 - Deve retornar 401 JSON sem header WWW-Authenticate em endpoints protegidos")
    public void deveRetornar401JsonSemWwwAuthenticateEmRotasProtegidas() throws Exception {
        mockMvc.perform(get("/api/v1/clientes/me").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
            .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/api/v1/admin/pedidos").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }
}
