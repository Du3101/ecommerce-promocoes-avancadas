package br.edu.fatec.ecommerce.compartilhado.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração institucional do OpenAPI 3 / Swagger UI para o Sistema de E-commerce com Promoções Avançadas.
 *
 * <p>Habilita o botão interativo <b>Authorize (HTTP Basic Auth)</b> no topo do Swagger UI e
 * documenta os 8 Padrões de Projeto GoF aplicados no Módulo 1 (Princípios II, III e V da Constituição).</p>
 */
@Configuration
public class ConfiguracaoOpenApiSwagger {

    /** Nome identificador do esquema de segurança HTTP Basic no contrato OpenAPI 3. */
    public static final String ESQUEMA_BASIC_AUTH = "basicAuth";

    /**
     * Construtor padrão explícito da classe de configuração do Swagger/OpenAPI.
     *
     * <p>Objetivo Funcional: Permitir instanciação pelo contêiner de inversão de controle do Spring.</p>
     * <p>Papel no Design Pattern: Fábrica de configuração de infraestrutura que expõe a especificação
     * OpenAPI dos serviços protegidos por <b>Proxy</b> e <b>RBAC</b>.</p>
     */
    public ConfiguracaoOpenApiSwagger() {
        // Inicialização padrão gerenciada pelo Spring Context
    }

    /**
     * Constrói e registra o bean {@link OpenAPI} contendo os metadados arquiteturais do Módulo 1
     * e o esquema global de autenticação HTTP Basic ({@code basicAuth}).
     *
     * <p>Objetivo Funcional: Exibir no Swagger UI o cabeçalho descritivo do projeto acadêmico FATEC
     * e habilitar o botão verde <b>Authorize</b> para autenticação imediata com a conta {@code ADMIN}
     * ou contas de clientes/operadores (Princípio V da Constituição).</p>
     * <p>Papel no Design Pattern: Método criador de infraestrutura (@Bean) que documenta a fronteira
     * REST dos componentes <b>Composite</b>, <b>Flyweight</b>, <b>Builder</b>, <b>Factory Method</b>,
     * <b>Static Factory Method</b>, <b>Null Object</b>, <b>Singleton</b> e <b>Proxy</b>.</p>
     *
     * @return instância configurada de {@link OpenAPI} com requisito de segurança {@code basicAuth}
     */
    @Bean
    public OpenAPI configurarOpenApiModulo1() {
        SecurityScheme esquemaSegurancaBasic = criarEsquemaSegurancaBasicAuth();
        Info informacoesApi = criarInformacoesInstitucionaisApi();

        return new OpenAPI()
            .info(informacoesApi)
            .components(new Components().addSecuritySchemes(ESQUEMA_BASIC_AUTH, esquemaSegurancaBasic))
            .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BASIC_AUTH));
    }

    /**
     * Cria a definição do esquema de segurança HTTP Basic para o OpenAPI 3.
     *
     * <p>Objetivo Funcional: Permitir que o Swagger UI capture usuário e senha e envie o cabeçalho
     * {@code Authorization: Basic <base64>} em todas as requisições de teste.</p>
     * <p>Papel no Design Pattern: Método auxiliar privado de construção do descritor de segurança.</p>
     *
     * @return objeto {@link SecurityScheme} configurado para HTTP Basic Auth
     */
    private SecurityScheme criarEsquemaSegurancaBasicAuth() {
        return new SecurityScheme()
            .name(ESQUEMA_BASIC_AUTH)
            .type(SecurityScheme.Type.HTTP)
            .scheme("basic")
            .description(
                "Autenticação HTTP Basic. Para testar rotas administrativas na apresentação, utilize: "
                + "Usuário: admin@ecommerce.fatec.edu.br | Senha: AdminFatec@2026"
            );
    }

    /**
     * Cria os metadados descritivos da API REST do Módulo 1 (título, versão, contato e resumo de padrões).
     *
     * <p>Objetivo Funcional: Orientar a avaliação acadêmica diretamente na tela inicial do Swagger UI.</p>
     * <p>Papel no Design Pattern: Método auxiliar privado para montagem das informações da especificação.</p>
     *
     * @return objeto {@link Info} preenchido com a documentação do Módulo 1
     */
    private Info criarInformacoesInstitucionaisApi() {
        return new Info()
            .title("Sistema de E-commerce com Promoções Avançadas — Módulo 1 (FATEC)")
            .version("1.0.0")
            .description(
                "API REST do Módulo 1 (Integrante 1): Identidade, Catálogo, Composição de Produtos, "
                + "Estoque Concorrente e Trilha de Auditoria.\n\n"
                + "**Padrões GoF aplicados neste módulo**: Composite (Categorias e Combos/Kits), "
                + "Flyweight (Atributos de Variação), Builder (Produto, ComboKit, Cliente e Endereço), "
                + "Factory Method, Static Factory Method, Proxy (Proteção RBAC e Auditoria Imutável), "
                + "Singleton e Null Object (Endereço e Categoria Raiz).\n\n"
                + "**Credencial Inicial (Botão Authorize)**: `admin@ecommerce.fatec.edu.br` / `AdminFatec@2026`"
            )
            .contact(new Contact().name("FATEC — Equipe E-commerce Promoções Avançadas — Módulo 1"));
    }
}

