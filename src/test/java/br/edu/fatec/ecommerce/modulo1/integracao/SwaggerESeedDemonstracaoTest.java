package br.edu.fatec.ecommerce.modulo1.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.fatec.ecommerce.compartilhado.config.ConfiguracaoOpenApiSwagger;
import br.edu.fatec.ecommerce.modulo1.catalogo.infra.InicializadorDadosDemonstracaoModulo1Runner;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suíte de testes de integração validando a configuração OpenAPI/Swagger (botão Authorize HTTP Basic)
 * e a carga inicial idempotente de dados de demonstração do Módulo 1 (T055–T057).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class SwaggerESeedDemonstracaoTest {

    @Autowired
    private OpenAPI openApiSpec;

    @Autowired
    private InicializadorDadosDemonstracaoModulo1Runner seedDemonstracaoRunner;

    @Autowired
    private PortaConsultaCatalogoCliente portaConsultaCatalogoCliente;

    /**
     * Verifica se o bean {@link OpenAPI} expõe os metadados institucionais do Módulo 1 e o esquema
     * de autenticação {@code basicAuth} para habilitar o botão Authorize no Swagger UI.
     */
    @Test
    @DisplayName("T055 - Deve configurar OpenAPI 3 com esquema HTTP Basic Auth (botão Authorize no Swagger UI)")
    public void deveConfigurarOpenApiComBasicAuthNoSwagger() {
        assertNotNull(this.openApiSpec, "O bean OpenAPI deve estar registrado no contexto Spring");
        assertNotNull(this.openApiSpec.getInfo(), "Os metadados Info do OpenAPI devem estar preenchidos");
        assertTrue(
            this.openApiSpec.getInfo().getTitle().contains("Módulo 1"),
            "O título do Swagger UI deve identificar o Módulo 1"
        );

        SecurityScheme esquemaBasic = this.openApiSpec
            .getComponents()
            .getSecuritySchemes()
            .get(ConfiguracaoOpenApiSwagger.ESQUEMA_BASIC_AUTH);
        assertNotNull(esquemaBasic, "O esquema de segurança basicAuth deve estar presente para exibir o botão Authorize");
        assertEquals(SecurityScheme.Type.HTTP, esquemaBasic.getType());
        assertEquals("basic", esquemaBasic.getScheme());
    }

    /**
     * Verifica se a carga de demonstração cria a árvore de categorias Composite, os produtos com
     * atributos Flyweight compartilhados, os saldos de estoque e o ComboKitComposite com cálculo
     * recursivo de preço (R$ 540,00), peso (1090g) e gargalo de estoque (2 kits).
     */
    @Test
    @DisplayName("T056/T057 - Deve popular dados de demonstração de forma idempotente e calcular ComboKit recursivamente")
    public void devePopularDadosDeDemonstracaoIdempotentemente() {
        boolean inseriuPrimeiraVez = this.seedDemonstracaoRunner.executarCargaDemonstracaoIdempotente();
        assertTrue(inseriuPrimeiraVez, "A primeira execução deve inserir os dados de demonstração");

        boolean inseriuSegundaVez = this.seedDemonstracaoRunner.executarCargaDemonstracaoIdempotente();
        assertFalse(inseriuSegundaVez, "A segunda execução deve ser idempotente e não duplicar registros");

        VisaoItemCatalogoDTO visaoCombo = this.portaConsultaCatalogoCliente.consultarItemPorSku(
            InicializadorDadosDemonstracaoModulo1Runner.SKU_COMBO_GAMER_DEMO
        );
        assertNotNull(visaoCombo);
        assertTrue(visaoCombo.eComboKit(), "O item DEMO-KIT-GAMER-01 deve ser identificado como ComboKitComposite");
        assertEquals(0, new BigDecimal("540.00").compareTo(visaoCombo.precoVigente()));
        assertEquals(1090, visaoCombo.pesoTotalGramas());
        assertEquals(2, visaoCombo.disponibilidadeMaximaEstoque());
        assertEquals(2, visaoCombo.skusFisicosDecompostos().size());
    }
}
