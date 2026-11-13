# Implementation Plan: Portal Web Completo de E-commerce e Painel Administrativo

**Branch**: `004-loja-web-ecommerce` | **Date**: 2026-11-12 | **Spec**: [spec.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/spec.md)

**Input**: Feature specification from `specs/004-loja-web-ecommerce/spec.md` (esclarecida em `Q1`–`Q3` para mesclagem de carrinho no login, simulação controlada de pagamento sem expor token no front-end e acesso rápido condicional ao modo demonstração).

## Summary

Construir a interface web completa do Sistema de E-commerce com Promoções Avançadas em **HTML5, CSS3 modular responsivo e JavaScript ES Modules puros**, servida diretamente pelo servidor **Spring Boot 3.4** em `http://localhost:8080/` (`src/main/resources/static/`), integrando visualmente todos os casos de uso dos Módulos 1, 2 e 3 sem Node.js nem etapa externa de build. Para suportar a experiência de loja real com segurança e fidelidade arquitetural, o plano inclui ajustes pontuais na camada HTTP/aplicação Java: (1) liberar recursos estáticos e configurar `AuthenticationEntryPoint` JSON sem o header `WWW-Authenticate` em `/api/**` (eliminando popup nativo Basic Auth), (2) expor `GET /api/v1/promocoes/ativas` e `GET /api/v1/loja/configuracao` para banners e detecção de modo demonstração, (3) ler `X-Sessao-Carrinho` e expor `POST /api/v1/carrinho/mesclar` para somar o carrinho anônimo ao do cliente no login, (4) expor `GET /api/v1/admin/pedidos` para o painel de expedição e `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento` restrito ao modo demonstração e ao dono/ADMIN (sem vazar `X-Gateway-Token`), e (5) entregar `README.md` unificado, correções pontuais nos quickstarts/Javadocs pendentes (`InicializadorDemonstracaoModulo2Runner`) e suíte de testes MockMvc.

## Technical Context

**Language/Version**: Java 21 (LTS) no back-end; HTML5 semântico, CSS3 e JavaScript ES Modules (`ES2022+`) no front-end  
**Primary Dependencies**: Spring Boot `3.4.5` (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`), Springdoc OpenAPI `2.8.5`, Flyway (`core` + `postgresql`), H2 (dev/test) e PostgreSQL (prod); zero bibliotecas NPM ou bundlers externos no front-end  
**Storage**: Banco relacional H2 / PostgreSQL sobre as migrations existentes `V1`, `V2` e `V3` (nenhuma nova migration `V4` necessária); `localStorage` (`ecommerce_sessao_carrinho`) e `sessionStorage` (`ecommerce_auth`) no navegador  
**Testing**: JUnit 5, `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")` (H2 em memória `ecommerce_test_db`)  
**Target Platform**: Tomcat embarcado JVM 21 (`http://localhost:8080/`) e navegadores modernos desktop/mobile (Chrome, Edge, Firefox)  
**Project Type**: Monólito modular Java Spring Boot integrando 3 subdomínios REST + portal web estático servido em `classpath:/static/`  
**Performance Goals**: Carregamento inicial interativo da vitrine em menos de 2 segundos em ambiente local (`SC-001`), atualizações assíncronas via `fetch` sem reload de página (`drawer` do carrinho, cotação de frete e abas RBAC)  
**Constraints**: Zero Node.js/build externo; zero popups nativos Basic Auth em chamadas `fetch()` (`FR-006`); `X-Gateway-Token` nunca enviado ao navegador (`Q2`); cumprimento estrito de Javadoc em 100% dos métodos Java novos/editados e regularização do `InicializadorDemonstracaoModulo2Runner` (Princípio III); preservação da substring `"Módulo 1"` no título OpenAPI (`SwaggerESeedDemonstracaoTest`)  
**Scale/Scope**: Loja completa pública (`Vitrine`, `Banners`, `Árvore de Categorias`, `Carrinho Visitante`), `Minha Conta` (`Cadastro`, `Login/Demo`, `Endereços + Null Object`, `Pedidos + Timeline + Snapshot`), `Checkout` (`Frete CORREIOS_PAC/CORREIOS_SEDEX/TRANSPORTADORA_EXPRESSA + Frete Grátis no PAC`, `PIX/Cartão aprovado ou final 0000 recusado/Boleto`, `Simulação Idempotente Demo`) e `Painel Admin RBAC` (6 abas dinâmicas cobrindo todos os endpoints administrativos dos Módulos 1, 2 e 3)

## Constitution Check

*GATE: Validado antes da Fase 0 e revalidado após os contratos da Fase 1.*

| Princípio da Constituição | Avaliação Pré-Pesquisa | Avaliação Pós-Design |
|---------------------------|------------------------|----------------------|
| **I. Arquitetura Orientada a Design Patterns e Extensibilidade (OCP)** | Compatível | As extensões Java reutilizam `Facade` (`ServicoProcessamentoPagamento`, `ServicoCarrinhoCompras`), `Null Object` (`OpcaoEnderecoEntrega`, `CupomNulo`) e estratégias polimórficas existentes. No front-end, `api-client.js` (**Facade**), `store.js` (**Observer**) e `MAPA_ABAS_RBAC` orientado a tabela evitam sequências monolíticas `if/else`. |
| **II. Catálogo Obrigatório de Padrões e Metodologias** | Compatível | Nenhum padrão artificial criado; a UI evidencia visualmente na experiência de usuário o **Composite** (árvore de categorias + ComboKits), **Null Object** (endereço não selecionado e cupom ausente), **Decorator/Chain** (extrato detalhado da formação de preço), **State** (linha do tempo e transições do pedido) e **Proxy/RBAC** (abas por perfil e auditoria). |
| **III. Código Explícito e Documentação Obrigatória em Todos os Métodos** | Requer atenção | Todo novo arquivo/método Java (`public`, `protected`, `private`) terá Javadoc completo (objetivo funcional, papel no Design Pattern, parâmetros/retornos/exceções). Além disso, o plano corrige a lacuna existente em `InicializadorDemonstracaoModulo2Runner` e nos métodos alterados de `CarrinhoComprasController`. |
| **IV. Integridade Transacional, Idempotência e Imutabilidade Comercial** | Compatível | O checkout continua congelando snapshots imutáveis no servidor; a mesclagem aditiva ocorre transacionalmente em `ServicoCarrinhoCompras`; a simulação de pagamento delega para `ServicoProcessamentoPagamento` preservando a chave idempotente `WEBHOOK:<numeroPedido>`. |
| **V. Separação Arquitetural em Camadas, Segurança e Auditoria** | Compatível | A mesclagem de carrinho permanece no Módulo 2 (sem acoplamento inverso no Módulo 1); `ConfiguracaoSegurancaHttp` preserva todas as regras RBAC existentes e nunca expõe `X-Gateway-Token` nem ativa recursos de demonstração quando `seed-demo-ativo=false`. |
| **VI. Testes Automatizados e Documentação (UML)** | Compatível | Novos testes de integração `MockMvc` cobrirão estáticos, `401` sem `WWW-Authenticate`, promoções ativas, configuração pública, merge `X-Sessao-Carrinho`, listagem admin de pedidos e simulação idempotente de pagamento, além de `README.md` raiz unificado. |

## Project Structure

### Documentation (this feature)

```text
specs/004-loja-web-ecommerce/
├── plan.md                                       # Este plano (/speckit-plan)
├── research.md                                   # Fase 0: decisões técnicas e achados de auditoria
├── data-model.md                                 # Fase 1: DTOs, estado da loja e mesclagem de carrinho
├── quickstart.md                                 # Fase 1: validação automatizada e roteiro E2E no browser
└── contracts/
    └── modulo4-web-and-api-extensions.md         # Fase 1: contratos HTTP/JSON e fronteiras dos ES Modules
```

### Source Code (repository root)

```text
README.md                                                                 # NOVO: documentação unificada na raiz do repositório
specs/003-checkout-pedidos-pagamentos/quickstart.md                       # EDIT: corrigir credenciais demo divergentes (AdminFatec@2026 / ClienteFatec@2026)

src/main/java/br/edu/fatec/ecommerce/
├── compartilhado/
│   ├── config/
│   │   └── ConfiguracaoOpenApiSwagger.java                               # EDIT: descrição unificada Módulos 1-3 + Portal Web (mantendo "Módulo 1" no título)
│   ├── loja/
│   │   └── api/
│   │       ├── ConfiguracaoLojaPublicaDTO.java                         # NOVO: record público de estado demo + frete grátis
│   │       └── LojaConfiguracaoPublicaController.java                  # NOVO: GET /api/v1/loja/configuracao (permitAll)
│   └── seguranca/
│       ├── ConfiguracaoSegurancaHttp.java                                # EDIT: liberar estáticos, GET promoções/configuração, CLIENTE no merge, custom EntryPoint
│       └── EntradaAutenticacaoRestSemPopup.java                          # NOVO: AuthenticationEntryPoint 401 JSON sem WWW-Authenticate em /api/**
├── modulo2/
│   ├── carrinho/
│   │   ├── api/
│   │   │   └── CarrinhoComprasController.java                            # EDIT: priorizar header X-Sessao-Carrinho + POST /api/v1/carrinho/mesclar + Javadocs
│   │   └── aplicacao/
│   │       └── ServicoCarrinhoCompras.java                               # EDIT: adicionar mesclarCarrinhoVisitante(UUID, String)
│   ├── integracao/
│   │   └── InicializadorDemonstracaoModulo2Runner.java                   # EDIT: @Order(3) + Javadocs completos (Princípio III)
│   └── promocoes/
│       └── api/
│           ├── CampanhaPublicaResponseDTO.java                           # NOVO: projeção pública segura de campanha ativa
│           └── PromocoesPublicoController.java                           # NOVO: GET /api/v1/promocoes/ativas (permitAll)
└── modulo3/
    ├── pagamento/
    │   └── api/
    │       ├── dto/SimulacaoPagamentoDemoRequestDTO.java                 # NOVO: {sucesso, motivoRecusa}
    │       └── SimulacaoPagamentoDemoController.java                     # NOVO: POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento (condicional demo + dono/ADMIN)
    └── pedido/
        ├── api/PedidoAdminController.java                                # EDIT: GET /api/v1/admin/pedidos?status=
        ├── aplicacao/ServicoConsultaPedido.java                          # EDIT: listarTodosPedidos(StatusPedido) + consultarEntidadeAutorizada(...)
        └── infraestrutura/PedidoJpaRepository.java                       # EDIT: findAllByOrderByCriadoEmDesc / findByStatusOrderByCriadoEmDesc

src/main/resources/static/
├── index.html                                                            # NOVO: shell SPA semântico com vitrine, drawer carrinho, modais e seções modulares
├── css/
│   ├── tokens.css                                                        # NOVO: paleta, tipografia, espaçamentos, raios e tokens semânticos de status
│   ├── layout.css                                                        # NOVO: header sticky, grid vitrine + árvore lateral, drawer e responsividade mobile
│   └── components.css                                                    # NOVO: cards produto/combo, timeline pedido, badges RBAC, modais e formulários admin
└── js/
    ├── store.js                                                          # NOVO: Observer Store reativo + persistência de sessaoCarrinhoId e auth
    ├── api-client.js                                                     # NOVO: Facade HTTP (Basic Auth, X-Sessao-Carrinho, normalização 401/403/400/409)
    ├── auth-cliente.js                                                   # NOVO: modal login/cadastro, acesso rápido demo, auto-merge pós-login, perfil e endereços (Null Object)
    ├── catalogo.js                                                       # NOVO: árvore recursiva Composite, banners promoções ativas, busca, variações e estoque
    ├── carrinho.js                                                       # NOVO: drawer lateral, mutações SKU, cupom e extrato detalhado memória de cálculo
    ├── checkout-pedidos.js                                               # NOVO: simulação frete CORREIOS_PAC/CORREIOS_SEDEX/TRANSPORTADORA_EXPRESSA + selo Grátis, cartão aprovado/final 0000 recusado, checkout, timeline, snapshot, simulação demo & cancelamento
    ├── admin.js                                                          # NOVO: painel dinâmico MAPA_ABAS_RBAC (expedição, catálogo, estoque, promoções/cupons, operadores, auditoria, relatórios)
    └── main.js                                                           # NOVO: bootstrap inicial (configuração + catálogo + carrinho), roteamento de abas e notificações toast

src/test/java/br/edu/fatec/ecommerce/modulo4/
├── PortalWebEEndpointsPublicosTest.java                                  # NOVO: MockMvc home estática, /api/v1/promocoes/ativas, /api/v1/loja/configuracao e 401 sem popup
└── MesclagemCarrinhoESimulacaoDemoTest.java                              # NOVO: MockMvc X-Sessao-Carrinho, merge no login, GET /api/v1/admin/pedidos e simulação pagamento demo/idempotência
```

**Structure Decision**: Optou-se pela estrutura unificada Spring Boot Web já estabelecida no projeto (`src/main/java/br/edu/fatec/ecommerce/{compartilhado,modulo1,modulo2,modulo3}` + `src/main/resources/static/`), mantendo o front-end modular desacoplado por domínio em `css/` e `js/` (ES Modules nativos) e agrupando os novos testes integrados HTTP sob `src/test/java/br/edu/fatec/ecommerce/modulo4/` para espelhar as frentes dos Módulos 1, 2 e 3 sem introduzir módulos Maven ou repositórios separados.

---

## Architecture & Phased Implementation Blueprint

1. **Camada de Segurança, Configuração Pública e Entrega Estática**:
   - Criar `EntradaAutenticacaoRestSemPopup` implementando `AuthenticationEntryPoint`, devolvendo HTTP `401` JSON compatível com `ManipuladorGlobalExcecoesRest` sem emitir `WWW-Authenticate`.
   - Atualizar `ConfiguracaoSegurancaHttp` para liberar `/`, `/index.html`, `/css/**`, `/js/**`, `/img/**`, `/favicon.ico`, `GET /api/v1/promocoes/ativas` e `GET /api/v1/loja/configuracao`, restringir `POST /api/v1/carrinho/mesclar` a `CLIENTE` e registrar o custom entry point em `httpBasic(...)` e `exceptionHandling(...)`.
   - Criar `LojaConfiguracaoPublicaController` (`GET /api/v1/loja/configuracao`) e regularizar `InicializadorDemonstracaoModulo2Runner` (`@Order(3)` + Javadocs) e `ConfiguracaoOpenApiSwagger`.

2. **Extensões REST dos Módulos 2 e 3**:
   - **Módulo 2**: Criar `PromocoesPublicoController` (`GET /api/v1/promocoes/ativas` filtrando campanhas ativas e vigentes), atualizar `CarrinhoComprasController.resolverSessao` para priorizar o header `X-Sessao-Carrinho` e implementar `POST /api/v1/carrinho/mesclar` + `ServicoCarrinhoCompras.mesclarCarrinhoVisitante`.
   - **Módulo 3**: Adicionar `findAllByOrderByCriadoEmDesc()` e `findByStatusOrderByCriadoEmDesc(StatusPedido)` em `PedidoJpaRepository`, `listarTodosPedidos(...)` em `ServicoConsultaPedido`, `GET /api/v1/admin/pedidos` em `PedidoAdminController` e `SimulacaoPagamentoDemoController` (`POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento`) respeitando `seed-demo-ativo` e titularidade.

3. **Portal Web Estático — Fundações, Vitrine e Carrinho Visitante/Cliente (`src/main/resources/static/`)**:
   - Construir `index.html`, `css/{tokens,layout,components}.css`, `js/store.js`, `js/api-client.js`, `js/catalogo.js` e `js/carrinho.js` com design de e-commerce profissional, árvore recursiva interativa, banners das promoções ativas, seletor de variações/combos com estoque real e drawer de carrinho com extrato transparente e barra de frete grátis.

4. **Portal Web Estático — Conta do Cliente, Checkout, Pedidos e Painel Administrativo RBAC**:
   - Construir `js/auth-cliente.js` (login sem popup + cards condicionais Acesso Rápido Demo + cadastro + auto-merge + CRUD de endereços tratando explicitamente `possuiEnderecoValido === false`), `js/checkout-pedidos.js` (cotação de frete real, checkout `PIX`/`Cartão Aprovado ou Recusado`/`Boleto`, timeline visual, snapshot imutável, simulação demo com prova de idempotência e cancelamento) e `js/admin.js` + `js/main.js` (tabs dinâmicas por perfil RBAC cobrindo expedição, catálogo, estoque, promoções/cupons, operadores, auditoria e relatórios).

5. **Documentação Raiz e Suíte Integrada de Verificação**:
   - Criar `README.md` unificado na raiz do repositório, corrigir `specs/003-checkout-pedidos-pagamentos/quickstart.md` e implementar os testes JUnit 5 / MockMvc (`PortalWebEEndpointsPublicosTest`, `MesclagemCarrinhoESimulacaoDemoTest`) validando `.\mvnw.cmd clean test` e inicialização real.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *Nenhuma violação da Constituição* | N/A | N/A — todas as decisões respeitam integralmente os Princípios I a VI. |
