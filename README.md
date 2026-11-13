# Sistema de E-commerce com Promoções Avançadas & Portal Web Integrado

Aplicação monolítica modular em **Java 21 + Spring Boot 3.4.5** com **Portal Web HTML5 / CSS3 / ES Modules puro** servido diretamente na raiz da própria aplicação Spring Boot em **[http://localhost:8080/](http://localhost:8080/)**, sem necessidade de Node.js ou processo de build externo.

---

## Visão Geral dos Módulos

| Módulo | Responsabilidade Principal | Padrões de Projeto Aplicados |
| :--- | :--- | :--- |
| **Módulo 1** | Identidade RBAC (`ADMIN`, `OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE`, `RESPONSAVEL_PROMOCOES`, `CLIENTE`), Catálogo recursivo, Variações & Combos/Kits, Estoque concorrente e Auditoria | Composite, Flyweight, Builder, Factory Method, Static Factory Method, Proxy (Segurança & Auditoria), Singleton, Null Object |
| **Módulo 2** | Carrinho persistente (visitante via `X-Sessao-Carrinho` e cliente logado), 4 mecânicas de promoção, resolução de conflitos, cupons e memória de cálculo transparente (piso R$ 0,00) | Strategy, Dynamic Factory, Chain of Responsibility, Decorator + Composite (Formação de Preço), Null Object (`CupomNulo`) |
| **Módulo 3** | Checkout com snapshot imutável, máquina de estados do pedido, pagamentos idempotentes com webhook, cotação de frete PAC/SEDEX/Expressa + Frete Grátis, eventos e relatórios gerenciais | Facade, Template Method/Hook, State, Adapter/ACL, Abstract Factory, Service Locator, Observer, Mediator, Builder |
| **Módulo 4 (Portal Web)** | Interface SPA completa em `src/main/resources/static/` + `401` JSON sem popup HTTP Basic + endpoints de vitrine pública, mesclagem no login (`POST /api/v1/carrinho/mesclar`), listagem admin geral (`GET /api/v1/admin/pedidos`) e simulação demo controlada | Observer (Store reativa), Facade HTTP (`api-client.js`), Table-Driven Dispatch RBAC (`admin.js`), Proxy Server-Side de Simulação Demo |

---

## Pré-requisitos e Execução Rápida

- **JDK 21** (ou superior) instalado e configurado (`JAVA_HOME`).
- Nenhuma instalação de Node.js, npm ou Docker é obrigatória: o projeto inclui Maven Wrapper (`mvnw.cmd` / `./mvnw`) e banco H2 em memória com migrações **Flyway** (`V1`, `V2`, `V3`).

### 1. Subir a aplicação completa (API + Portal Web em Demonstração)

```powershell
.\mvnw.cmd spring-boot:run
```

> Em `src/main/resources/application.yml`, a propriedade `ecommerce.modulo3.seed-demo-ativo` está habilitada (`true`) para carregar o catálogo inicial, promoções, cupons, pedidos em múltiplos estados, contas demo e habilitar os controles de demonstração na tela.

### 2. Acessos Locais

- **Portal Web (Loja + Área do Cliente + Checkout + Painel Admin)**: [http://localhost:8080/](http://localhost:8080/)
- **Documentação Interativa OpenAPI / Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Console H2 em Memória**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL padrão configurada em `application.yml`)

---

## Credenciais, Cupons e Cartões para Demonstração

### Contas Semeadas (`seed-demo-ativo=true`)

| Perfil | E-mail | Senha | Recursos Disponíveis |
| :--- | :--- | :--- | :--- |
| **CLIENTE** | `cliente.demo@ecommerce.fatec.edu.br` | `ClienteFatec@2026` | Compras, múltiplos endereços, histórico, checkout e simulação demo |
| **ADMIN** | `admin@ecommerce.fatec.edu.br` | `AdminFatec@2026` | Catálogo, combos, estoque, promoções, cupons, expedição de pedidos, auditoria, operadores e relatórios |

> **Atenção:** Caso a aplicação seja iniciada com `ecommerce.modulo3.seed-demo-ativo=false`, os cartões de acesso rápido no login e os botões de simulação controlada de pagamento não são exibidos, e o endpoint `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento` bloqueia execuções (HTTP `403`). Operadores comerciais, de estoque e de promoções podem ser provisionados pelo `ADMIN` na aba **Auditoria e Operadores**.

### Cupons e Promoções Pré-cadastrados (Módulo 2)

- **Cupons de teste no carrinho**:
  - `BEMVINDO10` — 10% de desconto.
  - `FATEC50` — R$ 50,00 de desconto fixo (meta mínima de carrinho configurada).
- **Campanhas vigentes exibidas na vitrine**:
  - Leve X e Pague Y, Desconto Percentual Primeira Compra (15% OFF), Meta de Carrinho (R$ 50 OFF acima de R$ 300) e Combo Cruzado Teclado + Mouse (`DEMO-TEC-PRO-PRETO` + `DEMO-MOU-PREC-PRETO`).

### Simulação de Cartão Fictício no Checkout

- **Aprovação imediata (`PAGO`)**: qualquer número que **não** termine em `0000` (ex.: `4111111111111111`).
- **Recusa imediata (`PAGAMENTO_RECUSADO`)**: número terminado em `0000` (ex.: `4000000000000000`).
- **PIX / Boleto**: cria o pedido em `AGUARDANDO_PAGAMENTO`; na tela de confirmação (com modo demo ativo), clique em **Simular Confirmação de Pagamento (PAGO)** e clique novamente para validar a resposta de **reexecução idempotente**.

---

## Executando a Suíte Completa de Testes (JUnit 5 + MockMvc)

```powershell
.\mvnw.cmd clean test
```

Principais suítes adicionadas para o **Portal Web e Extensões REST (`modulo4`)**:
- `src/test/java/br/edu/fatec/ecommerce/modulo4/PortalWebEEndpointsPublicosTest.java`
- `src/test/java/br/edu/fatec/ecommerce/modulo4/MesclagemCarrinhoESimulacaoDemoTest.java`
- `src/test/java/br/edu/fatec/ecommerce/modulo4/SimulacaoPagamentoDemoAtivoTest.java`
- `src/test/java/br/edu/fatec/ecommerce/modulo4/CatalogoAdminListagemEEndpointsTest.java`

---

## Estrutura de Front-end Servido pelo Spring Boot

```text
src/main/resources/static/
├── index.html                  # Shell SPA (Vitrine, Conta, Checkout, Admin RBAC, Drawer e Modais)
├── css/
│   ├── tokens.css              # Design tokens, tipografia e cores de status de pedido
│   ├── layout.css              # Grid responsivo, barra fixa, drawer lateral e breakpoints
│   └── components.css          # Cards de vitrine/combo, chips de variação, timeline e tabelas
└── js/
    ├── store.js                # Observer reativo + sessão X-Sessao-Carrinho + utilidades XSS/BRL
    ├── api-client.js           # Facade fetch() com Basic Auth / X-Sessao-Carrinho e handler 401 sem popup
    ├── main.js                 # Bootstrapper, navegação multi-visão, toasts e modais
    ├── catalogo.js             # Banners vigentes, árvore Composite, variações Flyweight e ComboKits
    ├── carrinho.js             # Drawer lateral, barra de frete grátis, cupons e memória detalhada
    ├── auth-cliente.js         # Login sem popup, cadastro, acesso rápido demo e endereços (Null Object)
    ├── checkout-pedidos.js     # Timeline, snapshot imutável, cotação de frete, checkout e demo webhook
    └── admin.js                # Painel dinâmico RBAC (Catálogo, Estoque, Promoções, Expedição, Auditoria, Relatórios)
```
