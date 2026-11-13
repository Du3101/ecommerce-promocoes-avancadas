package br.edu.fatec.ecommerce.compartilhado.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
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
            .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BASIC_AUTH))
            .tags(criarTagsEspecificacaoApi());
    }

    /**
     * Cria as tags unificadas dos Módulos 1, 2, 3 e 4 exibidas e agrupadas no Swagger UI.
     *
     * <p>Objetivo Funcional: Organizar os endpoints de Catálogo, Estoque, Identidade, Auditoria,
     * Carrinho, Promoções, Cupons, Checkout, Pedidos, Pagamentos, Frete, Relatórios e Configuração
     * Pública em seções alinhadas exatamente às anotações {@code @Tag} dos controladores REST.</p>
     * <p>Papel no Design Pattern: Método auxiliar privado de documentação abrangente da fronteira REST.</p>
     *
     * @return lista ordenada de tags da especificação OpenAPI 3
     */
    private List<Tag> criarTagsEspecificacaoApi() {
        return List.of(
            new Tag().name("Módulo 1 — Catálogo Público").description(
                "Vitrine, árvore de categorias (Composite) e disponibilidade de estoque"),
            new Tag().name("Módulo 1 — Gestão Administrativa de Catálogo").description(
                "CRUD auditado de categorias, produtos, variações Flyweight e combos (Proxy)"),
            new Tag().name("Módulo 1 — Gestão de Estoque e Reservas").description(
                "Reservas atômicas concorrentes, entradas, bloqueios e expiração TTL"),
            new Tag().name("Módulo 1 — Identidade, Autenticação e Clientes").description(
                "Auto-cadastro de clientes, perfil, usuários internos e endereços (Null Object)"),
            new Tag().name("Módulo 1 — Auditoria Administrativa").description(
                "Consulta imutável da trilha de operações administrativas registradas pelos Proxies"),
            new Tag().name("Módulo 2 — Carrinho de Compras").description(
                "Endpoints para gerenciamento do carrinho, mesclagem visitante→cliente e descontos"),
            new Tag().name("Promoções Públicas").description(
                "Consulta pública de campanhas ativas na vitrine da loja"),
            new Tag().name("Módulo 2 — Gestão Administrativa de Promoções").description(
                "Criação e ativação de regras e campanhas promocionais (ADMIN e RESPONSAVEL_PROMOCOES)"),
            new Tag().name("Módulo 2 — Gestão Administrativa de Cupons").description(
                "Criação e ativação de cupons de desconto (ADMIN e RESPONSAVEL_PROMOCOES)"),
            new Tag().name("Checkout").description("Módulo 3 — Fechamento de compra (Facade + Template Method)"),
            new Tag().name("Pedidos").description("Módulo 3 — Consulta, listagem geral, cancelamento e expedição (State + Mediator)"),
            new Tag().name("Pagamentos").description(
                "Módulo 3 — Webhook idempotente (Adapter + Abstract Factory) e simulação controlada demo"),
            new Tag().name("Frete").description("Módulo 3 — Cotação de frete (Adapter/ACL) e frete grátis"),
            new Tag().name("Relatórios").description("Módulo 3 — Relatórios gerenciais (somente ADMIN)"),
            new Tag().name("Configuração Pública da Loja").description(
                "Módulo 4 — Parâmetros públicos de frete grátis e modo demonstração")
        );
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
            .title("Sistema de E-commerce com Promoções Avançadas — Módulo 1, Módulo 2, Módulo 3 & Portal Web (FATEC)")
            .version("1.0.0")
            .description(
                "API REST completa do Sistema de E-commerce com Portal Web integrado em `http://localhost:8080/`.\n\n"
                + "**Módulo 1 (Identidade, Catálogo, Estoque Concorrente e Auditoria)**: "
                + "Composite (Categorias e Combos/Kits), Flyweight (Atributos), Builder, Factory Method, "
                + "Proxy (RBAC e Auditoria Imutável), Singleton e Null Object (Endereço e Categoria Raiz).\n\n"
                + "**Módulo 2 (Carrinho, Promoções e Cupons)**: Carrinho anônimo via `X-Sessao-Carrinho` e autenticado com "
                + "mesclagem no login (`POST /api/v1/carrinho/mesclar`), Pipeline Decorator + Composite de cálculo, "
                + "4 mecânicas promocionais (Strategy + Dynamic Factory + Chain of Responsibility) e vitrine pública "
                + "(`GET /api/v1/promocoes/ativas`).\n\n"
                + "**Módulo 3 (Checkout, Pedidos, Pagamentos Idempotentes, Frete e Relatórios)**: "
                + "Facade, Template Method, State, Adapter/ACL, Abstract Factory, Observer, Mediator, "
                + "listagem geral admin (`GET /api/v1/admin/pedidos`) e simulação controlada em modo demo.\n\n"
                + "**Credencial Admin (Botão Authorize)**: `admin@ecommerce.fatec.edu.br` / `AdminFatec@2026` | "
                + "**Cliente Demo**: `cliente.demo@ecommerce.fatec.edu.br` / `ClienteFatec@2026` | "
                + "**Webhook**: header `X-Gateway-Token: gateway-simulado-fatec`."
            )
            .contact(new Contact().name("FATEC — Equipe E-commerce Promoções Avançadas"));
    }
}

