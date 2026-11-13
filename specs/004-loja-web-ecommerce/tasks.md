---

description: "Actionable, dependency-ordered implementation tasks for 004-loja-web-ecommerce"
---

# Tasks: Portal Web Completo de E-commerce e Painel Administrativo

**Input**: Design documents from `/specs/004-loja-web-ecommerce/`  
**Prerequisites**: [plan.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/plan.md), [spec.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/spec.md), [research.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/research.md), [data-model.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/data-model.md), [modulo4-web-and-api-extensions.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/contracts/modulo4-web-and-api-extensions.md), [quickstart.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/quickstart.md)

**Tests**: Cobertura automatizada JUnit 5 + MockMvc explicitamente requerida pela especificação (`FR-011`, `SC-005`), plano (`plan.md`) e Princípio VI da Constituição.

**Organization**: Tarefas organizadas por fase fundacional e por História de Usuário (`US1` a `US6`), garantindo entrega incremental com `US1` como MVP independente e cumprimento rigoroso de Javadoc explícito em 100% dos métodos Java (Princípio III).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode ser executada em paralelo (arquivos distintos, sem dependência pendente)
- **[Story]**: História de usuário associada (`[US1]`..`[US6]`); omitida em Setup, Foundational e Polish

---

## Phase 1: Setup (Estrutura Base HTML5 & CSS3 do Portal Web)

**Purpose**: Criar a casca HTML5 semântica e os arquivos modulares de estilo servidos estaticamente pelo Spring Boot em `http://localhost:8080/`.

- [X] T001 Create semantic HTML5 SPA shell with top navbar (Vitrine, Minha Conta, Checkout, conditional Painel Admin, cart badge trigger, auth button), `#view-vitrine`, `#view-conta`, `#view-checkout`, `#view-admin`, `#drawer-carrinho`, `#modal-auth`, `#modal-detalhes`, and `#toast-container` loading `<script type="module" src="/js/main.js">` in `src/main/resources/static/index.html`
- [X] T002 [P] Create CSS design tokens (brand palette, typography, spacing, border radius, elevation, stock indicators, and semantic status badge colors for `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`, `PAGAMENTO_RECUSADO`) in `src/main/resources/static/css/tokens.css`
- [X] T003 [P] Create responsive page layout stylesheet (sticky navbar, 2-column storefront grid with recursive category sidebar, slide-over side cart drawer, responsive tablet/mobile breakpoints) in `src/main/resources/static/css/layout.css`
- [X] T004 [P] Create reusable UI component stylesheet (promo hero banners, product and ComboKit cards, attribute variation selector pills, itemized price calculation breakdown table, order state timeline, RBAC admin tabs, modals, tables, and toast alerts) in `src/main/resources/static/css/components.css`

---

## Phase 2: Foundational (Segurança HTTP Sem Popup, Config Pública & Core JS)

**Purpose**: Infraestrutura obrigatória que desbloqueia todas as histórias de usuário do portal sem alterar tabelas do banco (`V1`–`V3` mantidas intactas).

**⚠️ CRITICAL**: Nenhuma história de usuário web (`US1`–`US5`) deve ser considerada integrada antes da conclusão desta fase.

- [X] T005 Create `EntradaAutenticacaoRestSemPopup` implementing `AuthenticationEntryPoint` returning HTTP `401 Unauthorized` (`application/json;charset=UTF-8`) with JSON fields `{timestamp, status: 401, codigo: "NAO_AUTENTICADO", mensagem}` and strictly omitting `WWW-Authenticate` response header on `/api/**` calls, with complete Javadoc per Constitution Principle III in `src/main/java/br/edu/fatec/ecommerce/compartilhado/seguranca/EntradaAutenticacaoRestSemPopup.java`
- [X] T006 Update `ConfiguracaoSegurancaHttp` to permit static paths (`/`, `/index.html`, `/css/**`, `/js/**`, `/img/**`, `/favicon.ico`), public read endpoints (`GET /api/v1/promocoes/ativas`, `GET /api/v1/loja/configuracao`), require role `CLIENTE` on `POST /api/v1/carrinho/mesclar`, and attach `EntradaAutenticacaoRestSemPopup` on both `httpBasic(...)` and `exceptionHandling(...)` while preserving Swagger UI `/swagger-ui/**` and all existing RBAC rules (with Javadoc) in `src/main/java/br/edu/fatec/ecommerce/compartilhado/seguranca/ConfiguracaoSegurancaHttp.java` (depends on T005)
- [X] T007 [P] Create `ConfiguracaoLojaPublicaDTO(boolean modoDemonstracaoAtivo, BigDecimal freteGratisValorMinimo, int freteGratisPesoMaximoGramas)` and `LojaConfiguracaoPublicaController` (`GET /api/v1/loja/configuracao`, `permitAll`) deriving `modoDemonstracaoAtivo` directly from `ecommerce.modulo3.seed-demo-ativo` (matching `SimulacaoPagamentoDemoController`) and free-shipping limits without exposing `webhook-token` or credentials, with 100% Javadocs in `src/main/java/br/edu/fatec/ecommerce/compartilhado/loja/api/ConfiguracaoLojaPublicaDTO.java` and `src/main/java/br/edu/fatec/ecommerce/compartilhado/loja/api/LojaConfiguracaoPublicaController.java`
- [X] T008 [P] Add `@Order(3)` and complete Javadocs (functional goal, Design Pattern role, parameters/exceptions) on constructor, `run`, `inicializarCampanhasDemonstracao`, and `inicializarCuponsDemonstracao` in `src/main/java/br/edu/fatec/ecommerce/modulo2/integracao/InicializadorDemonstracaoModulo2Runner.java`
- [X] T009 [P] Create Observer state store managing `EstadoLoja` (`configuracao`, persistent `sessaoCarrinhoId` UUID v4 in `localStorage` key `ecommerce_sessao_carrinho`, `autenticacao` in `sessionStorage` key `ecommerce_auth`, `catalogo`, `promocoesAtivas`, `carrinho`, `clientePerfil`, `pedidosCliente`, `visaoAtiva`) with subscription hooks in `src/main/resources/static/js/store.js`
- [X] T010 Create HTTP Facade module encapsulating `fetch()`, automatically sending `Authorization: Basic <tokenBasicBase64>` (when authenticated) and `X-Sessao-Carrinho` header, parsing `{timestamp, status, codigo, mensagem}` errors cleanly, and triggering in-page login handler on `401` without triggering native browser prompts in `src/main/resources/static/js/api-client.js` (depends on T009)
- [X] T011 Create application entry bootstrapper and view switcher loading `GET /api/v1/loja/configuracao`, coordinating navigation across `#view-vitrine`, `#view-conta`, `#view-checkout`, `#view-admin`, rendering toast alerts, and initializing UI modules in `src/main/resources/static/js/main.js` (depends on T001, T009, T010)

**Checkpoint**: Aplicação sobe com entrega estática liberada em `/`, `401` JSON sem `WWW-Authenticate` ativo e camada core JS operacional.

---

## Phase 3: User Story 1 - Vitrine da Loja, Árvore de Categorias, Promoções Ativas e Seleção de Variações/Combos (Priority: P1) 🎯 MVP

**Goal**: Permitir que qualquer visitante navegue pela loja virtual completa em `http://localhost:8080/`, visualizando banners de campanhas promocionais ativas, árvore recursiva de categorias e subcategorias, busca textual e cards/detalhes de produtos com variações e Combos/Kits com saldo físico/gargalo em tempo real.

**Independent Test**: Subir com `seed-demo-ativo=true` (ou testar via MockMvc `PortalWebEEndpointsPublicosTest`), abrir `/` sem login, selecionar uma categoria pai na árvore recursiva, buscar por termo/SKU, alternar variações de um produto e verificar no card de `ComboKit` seus componentes, economia e limite de estoque.

### Implementation for User Story 1

- [X] T012 [P] [US1] Create `CampanhaPublicaResponseDTO(UUID id, String nome, String descricao, TipoMecanicaPromocao tipoMecanica, String descricaoMecanica, int prioridade, boolean cumulativa, Instant dataInicio, Instant dataFim)` with Static Factory Method `de(CampanhaPromocional)` and complete Javadoc in `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/api/CampanhaPublicaResponseDTO.java`
- [X] T013 [US1] Create `PromocoesPublicoController` exposing `GET /api/v1/promocoes/ativas` querying `CampanhaPromocionalJpaRepository.listarCampanhasAtivasPorPrioridade()`, filtering active campaigns whose temporal window (`dataInicio`..`dataFim`, allowing null open end) covers `Instant.now()`, and mapping safely via `CampanhaPublicaResponseDTO::de` with 100% Javadocs in `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/api/PromocoesPublicoController.java` (depends on T006, T012)
- [X] T014 [US1] Implement `catalogo.js` consuming `GET /api/v1/promocoes/ativas`, `GET /api/v1/catalogo/categorias/arvore`, `GET /api/v1/catalogo/itens?categoriaId=`, `GET /api/v1/catalogo/itens/{sku}`, and `GET /api/v1/estoque/{sku}` (on-demand refresh) to render active promo banners, recursive nested Composite category tree, search filter, sibling variation selector grouping items by base name prefix and `atributosFlyweight` chips, ComboKit component breakdown and calculated savings via `skusFisicosDecompostos` + `percentualDescontoKit` + local `itensMapaPorSku` lookup, direct `disponibilidadeMaximaEstoque` badges (disabling CTA when 0), and inline SVG icons (`escapeHtml` sanitized) in `src/main/resources/static/js/catalogo.js` (depends on T010, T013)
- [X] T015 [US1] Wire `catalogo.js` initialization, search bar listeners, and vitrine view bindings into `src/main/resources/static/js/main.js` and verify vitrine DOM hooks in `src/main/resources/static/index.html` (depends on T011, T014)

### Tests for User Story 1

- [X] T016 [P] [US1] Create `PortalWebEEndpointsPublicosTest` using MockMvc verifying: (1) `GET /` returns `200 OK` HTML without auth, (2) `GET /api/v1/loja/configuracao` returns `200 OK` with `modoDemonstracaoAtivo=false` under `test` profile and no secrets, (3) `GET /api/v1/promocoes/ativas` excludes inactive and out-of-window campaigns and returns active ones ordered by descending priority (isolating or asserting against created test campaign IDs/names since `InicializadorDemonstracaoModulo2Runner` seeds demo campaigns under `test`), and (4) unauthenticated requests to `/api/v1/clientes/me` and `/api/v1/admin/pedidos` return `401` JSON without `WWW-Authenticate` header in `src/test/java/br/edu/fatec/ecommerce/modulo4/PortalWebEEndpointsPublicosTest.java` (depends on T006, T007, T013)

**Checkpoint**: User Story 1 (MVP) opera isoladamente via navegador e testes MockMvc sem precisar de autenticação prévia.

---

## Phase 4: User Story 2 - Carrinho Lateral Interativo, Cupons e Transparência na Formação de Preço (Priority: P1)

**Goal**: Permitir que visitantes anônimos (via `X-Sessao-Carrinho`) e clientes autenticados gerenciem o carrinho lateral deslizante em qualquer tela, aplicando/removendo cupons promocionais e inspecionando a memória detalhada de descontos com piso R$ 0,00.

**Independent Test**: Adicionar itens como visitante anônimo na vitrine, abrir o drawer lateral, ajustar quantidades para acionar promoções ativas, aplicar cupom `BEMVINDO10` e cupom inválido para verificar extrato discriminado e mensagens claras de erro.

### Implementation for User Story 2

- [X] T017 [US2] Update `CarrinhoComprasController.resolverSessao(...)` to prioritize HTTP header `X-Sessao-Carrinho` before `?sessaoId=` query parameter and existing `HttpSession` (falling back to `"sessao-anonima-padrao"`), keeping full backward compatibility and adding complete Javadocs per Principle III in `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/api/CarrinhoComprasController.java`
- [X] T018 [US2] Implement interactive slide-over cart drawer module consuming `GET /api/v1/carrinho`, `POST /api/v1/carrinho/itens`, `PUT /api/v1/carrinho/itens/{sku}`, `DELETE /api/v1/carrinho/itens/{sku}`, `DELETE /api/v1/carrinho`, `POST /api/v1/carrinho/cupom`, and `DELETE /api/v1/carrinho/cupom` rendering item rows, free-shipping progress bar (`configuracao.freteGratisValorMinimo` & `freteGratisPesoMaximoGramas`), itemized promotion rebates, coupon rejection notices, and R$ 0,00 floor total in `src/main/resources/static/js/carrinho.js` (depends on T010, T017)
- [X] T019 [US2] Connect vitrine "Adicionar ao Carrinho" button handlers in `src/main/resources/static/js/catalogo.js` and navbar cart badge/drawer open-close controls in `src/main/resources/static/js/main.js` to trigger immediate drawer synchronization (depends on T015, T018)

### Tests for User Story 2

- [X] T020 [P] [US2] Create `MesclagemCarrinhoESimulacaoDemoTest` initial test block validating visitor cart persistence via HTTP header `X-Sessao-Carrinho` (`POST /api/v1/carrinho/itens`, `PUT /api/v1/carrinho/itens/{sku}`, `POST /api/v1/carrinho/cupom` valid/invalid, and `GET /api/v1/carrinho` calculation memory) in `src/test/java/br/edu/fatec/ecommerce/modulo4/MesclagemCarrinhoESimulacaoDemoTest.java` (depends on T017)

**Checkpoint**: Visitante anônimo consegue montar carrinho com `X-Sessao-Carrinho`, testar cupons e visualizar toda a memória de cálculo.

---

## Phase 5: User Story 3 - Autenticação Integrada na Página, Cadastro, Merge de Carrinho e Área do Cliente (Priority: P1)

**Goal**: Eliminar qualquer caixa de diálogo nativa do navegador na autenticação, suportar cadastro de cliente e Acesso Rápido Demo condicional (`Q3`), mesclar aditivamente o carrinho visitante ao logar (`Q1`), gerenciar múltiplos endereços com tratamento explícito do Null Object (`possuiEnderecoValido === false`) e consultar/cancelar pedidos do cliente.

**Independent Test**: Testar login com senha incorreta sem popup nativo, adicionar itens como visitante, logar como cliente que já tem itens salvos confirmando soma aditiva das quantidades, inspecionar tratamento neutro quando `possuiEnderecoValido === false`, cadastrar/selecionar endereço e testar cancelamento permitido/bloqueado no histórico de pedidos.

### Implementation for User Story 3

- [X] T021 [US3] Implement `ServicoCarrinhoCompras.mesclarCarrinhoVisitante(UUID clienteId, String sessaoId)` verifying `clienteId != null`, looking up visitor cart by trimmed `sessaoId`, summing quantities of matching SKUs into customer cart (revalidating active catalog items via `PortaConsultaCatalogoCliente`), transferring visitor coupon only if customer cart has none and `cupom.isDisponivelParaUso(...)` passes on the merged subtotal, emptying/deleting the anonymous cart, and saving customer cart with 100% Javadocs in `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/aplicacao/ServicoCarrinhoCompras.java`
- [X] T022 [US3] Expose `POST /api/v1/carrinho/mesclar` accepting `X-Sessao-Carrinho` header or `?sessaoId=` param for authenticated `CLIENTE` users and returning updated `CarrinhoResponse` via `construirResposta(...)`, with complete Javadocs per Principle III in `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/api/CarrinhoComprasController.java` (depends on T006, T017, T021)
- [X] T023 [US3] Implement `auth-cliente.js` supporting in-page login (`POST /api/v1/auth/login`), customer sign-up (`POST /api/v1/auth/clientes/cadastro`), logout, conditional "Acesso Rápido (Demonstração)" quick-fill & real server sign-in cards (`Cliente Demo` and `Administrador`) visible strictly when `store.configuracao.modoDemonstracaoAtivo === true`, automatic `POST /api/v1/carrinho/mesclar` immediately after `CLIENTE` login, and customer profile/address CRUD (`GET /api/v1/clientes/me`, `POST /enderecos`, `PUT /enderecos/{id}/selecionar`, `DELETE /enderecos/{id}`) rendering a friendly guidance banner without undefined fields when `enderecoSelecionado.possuiEnderecoValido === false` in `src/main/resources/static/js/auth-cliente.js` (depends on T010, T018, T022)
- [X] T024 [US3] Implement customer order history panel inside `checkout-pedidos.js` (`GET /api/v1/pedidos/me`, `GET /api/v1/pedidos/{id}`) showing visual state timeline, frozen commercial snapshot (`snapshotFinanceiro`, `entrega`, `itens`, `promocoesAplicadas`, `cupomUtilizado`), and order cancellation (`DELETE /api/v1/pedidos/{id}/cancelamento`) with domain error reporting, wiring navigation in `src/main/resources/static/js/main.js` and `src/main/resources/static/js/auth-cliente.js` in `src/main/resources/static/js/checkout-pedidos.js` (depends on T010, T023)

### Tests for User Story 3

- [X] T025 [P] [US3] Extend `MesclagemCarrinhoESimulacaoDemoTest` verifying `POST /api/v1/carrinho/mesclar` additively merges visitor SKU quantities into pre-existing authenticated customer cart, transfers valid visitor coupon when customer lacks one, clears visitor cart session, and returns `401`/`403` for anonymous or non-`CLIENTE` users in `src/test/java/br/edu/fatec/ecommerce/modulo4/MesclagemCarrinhoESimulacaoDemoTest.java` (depends on T022)

**Checkpoint**: Visitante anônimo faz login sem popup, herda carrinho mesclado, gerencia endereços (inclusive estado Null Object) e acompanha/cancela seus pedidos.

---

## Phase 6: User Story 4 - Checkout Guiado, Frete em Tempo Real e Simulação Controlada de Pagamento (Priority: P1)

**Goal**: Guiar o cliente autenticado na escolha de endereço, cotação ao vivo de frete (`CORREIOS_PAC`, `CORREIOS_SEDEX`, `TRANSPORTADORA_EXPRESSA` + Frete Grátis no `CORREIOS_PAC`), seleção de pagamento (`PIX`, Cartão aprovado/recusado final `0000`, `BOLETO`), geração de pedido imutável e simulação segura da confirmação de pagamento exclusivamente em modo demonstração comprovando transição para `PAGO` e idempotência (`Q2`).

**Independent Test**: Com cliente autenticado, endereço selecionado e itens no carrinho, cotar frete, finalizar pedido em `PIX`, acionar botão de confirmação de pagamento na tela do pedido duas vezes (1ª transita para `PAGO`; 2ª indica `idempotenteReexecutado = true`), e testar cartão recusado liberando estoque.

### Implementation for User Story 4

- [X] T026 [P] [US4] Create `SimulacaoPagamentoDemoRequestDTO(@NotNull Boolean sucesso, String motivoRecusa)` with Javadoc in `src/main/java/br/edu/fatec/ecommerce/modulo3/pagamento/api/dto/SimulacaoPagamentoDemoRequestDTO.java`
- [X] T027 [US4] Create `SimulacaoPagamentoDemoController` exposing `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento` guarded by `@Value("${ecommerce.modulo3.seed-demo-ativo:false}")` (throwing `AcessoNegadoModulo3Exception` when disabled), validating order ownership (`Pedido.getClienteId()` matches authenticated customer OR caller holds `ADMIN`) via `ServicoConsultaPedido`, building internal `ConfirmacaoPagamentoRequestDTO` (`chaveOperacaoPedido = pedido.getNumeroPedido()`, `idTransacaoGateway = "DEMO-SIM-" + pedido.getNumeroPedido()`) without exposing `X-Gateway-Token`, and delegating to `ServicoProcessamentoPagamento.processarNotificacaoGateway(...)` with 100% Javadocs in `src/main/java/br/edu/fatec/ecommerce/modulo3/pagamento/api/SimulacaoPagamentoDemoController.java` (depends on T026)
- [X] T028 [US4] Implement full guided Checkout flow inside `src/main/resources/static/js/checkout-pedidos.js`: validate selected address (`possuiEnderecoValido` gate redirecting smoothly to Minha Conta if false), live `POST /api/v1/frete/calcular` (`{cepDestino, pesoTotalGramas, subtotalLiquido}`) option cards rendering `CORREIOS_PAC` (with Free Shipping highlight when eligible), `CORREIOS_SEDEX`, and `TRANSPORTADORA_EXPRESSA`, payment selector (`PIX`, `CARTAO_CREDITO` sending `numeroCartaoFicticio` — with subtle helper toggle showing approved card vs final `0000` e.g. `4000000000000000` refusal — plus `titularCartao`, `BOLETO`), `POST /api/v1/checkout` (passing `metodoPagamento`, `modalidadeFrete`, optional `chaveIdempotenciaCliente`, `numeroCartaoFicticio`, `titularCartao`) clearing cart drawer upon success, and post-checkout order confirmation panel showing PIX/Boleto code plus conditional "Simular Confirmação de Pagamento" controls (shown only when `modoDemonstracaoAtivo === true`) rendering `idempotenteReexecutado` badge on repeat clicks (depends on T018, T023, T024, T027)

### Tests for User Story 4

- [X] T029 [P] [US4] Extend `MesclagemCarrinhoESimulacaoDemoTest` covering `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento`: (1) rejects `403` when `ecommerce.modulo3.seed-demo-ativo=false`, (2) rejects `403` when invoked by another `CLIENTE`, (3) succeeds transitioning `AGUARDANDO_PAGAMENTO` to `PAGO` with `idempotenteReexecutado=false` on first call for owner/`ADMIN` when enabled, and (4) returns `200 OK` with `idempotenteReexecutado=true` on duplicate call in `src/test/java/br/edu/fatec/ecommerce/modulo4/MesclagemCarrinhoESimulacaoDemoTest.java` (depends on T027)

**Checkpoint**: Fluxo E2E completo de compra — da vitrine até confirmação idempotente de pagamento — funcional no navegador sem `X-Gateway-Token` exposto no JS.

---

## Phase 7: User Story 5 - Painel Administrativo Dinâmico por Perfil RBAC e Expedição (Priority: P2)

**Goal**: Exibir dinamicamente o Painel Administrativo somente para perfis autorizados (`ADMIN`, `OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE`, `RESPONSAVEL_PROMOCOES`), segmentando abas por `MAPA_ABAS_RBAC` e entregando operação visual de expedição (listagem geral ordenada), catálogo/preços, estoque, promoções/cupons, operadores/auditoria e relatórios gerenciais.

**Independent Test**: Autenticar como `ADMIN` (ou operador específico), conferir ocultamento automático para `CLIENTE`/visitante, listar pedidos ordenados no painel de expedição, executar transições `preparar -> despachar (com rastreio) -> entregar`, gerenciar catálogo/estoque/promoções e inspecionar trilha de auditoria e relatórios.

### Implementation for User Story 5

- [X] T030 [P] [US5] Add `findAllByOrderByCriadoEmDesc()` and `findByStatusOrderByCriadoEmDesc(StatusPedido status)` query methods with complete Javadocs in `src/main/java/br/edu/fatec/ecommerce/modulo3/pedido/infraestrutura/PedidoJpaRepository.java`
- [X] T031 [US5] Add `listarTodosPedidos(StatusPedido statusOpcional)` retrieving chronological descending orders (`findAllByOrderByCriadoEmDesc` or `findByStatusOrderByCriadoEmDesc`), mapping associated `TransacaoPagamento` via `paraDto(...)`, with complete Javadocs per Principle III in `src/main/java/br/edu/fatec/ecommerce/modulo3/pedido/aplicacao/ServicoConsultaPedido.java` (depends on T030)
- [X] T032 [US5] Add `GET /api/v1/admin/pedidos` endpoint (optional `@RequestParam(required = false) StatusPedido status`, protected under existing `/api/v1/admin/pedidos/**` RBAC for `ADMIN` and `OPERADOR_COMERCIAL`) returning `List<PedidoResponseDTO>` with complete Javadoc in `src/main/java/br/edu/fatec/ecommerce/modulo3/pedido/api/PedidoAdminController.java` (depends on T031)
- [X] T033 [US5] Implement `admin.js` using declarative role-mapping table `MAPA_ABAS_RBAC` dynamically rendering only authorized tabs: (1) Pedidos & Expedição (`GET /api/v1/admin/pedidos`, `PATCH .../preparar|despachar|entregar`), (2) Catálogo & Preços (`POST /api/v1/admin/catalogo/{categorias,produtos,combos}`, `PATCH .../itens/{sku}/{preco,status}`), (3) Estoque & Reservas (`GET /api/v1/estoque/{sku}`, `POST /api/v1/admin/estoque/{sku}/ajustes`, `POST .../expirar-vencidas`), (4) Promoções & Cupons (`GET/POST/PATCH /api/v1/admin/promocoes/**`, `GET/POST /api/v1/admin/cupons`), (5) Operadores & Auditoria (`POST /api/v1/admin/usuarios/operadores`, `GET /api/v1/admin/auditoria`), and (6) Relatórios Gerenciais (`GET /api/v1/admin/relatorios/{vendas-consolidadas,ticket-medio,produtos-mais-vendidos,cancelamentos}`) in `src/main/resources/static/js/admin.js` (depends on T010, T032)
- [X] T034 [US5] Integrate RBAC navbar admin toggle and tab activation lifecycle into `src/main/resources/static/js/main.js` and `src/main/resources/static/index.html`, ensuring `#view-admin` stays hidden and unaccessible for unauthenticated visitors and pure `CLIENTE` sessions (depends on T011, T033)

### Tests for User Story 5

- [X] T035 [P] [US5] Extend `MesclagemCarrinhoESimulacaoDemoTest` verifying `GET /api/v1/admin/pedidos` returns all persisted orders sorted newest first (`criadoEm DESC`), respects optional `?status=` filter for `ADMIN`/`OPERADOR_COMERCIAL`, and returns `403 Forbidden` for `CLIENTE` in `src/test/java/br/edu/fatec/ecommerce/modulo4/MesclagemCarrinhoESimulacaoDemoTest.java` (depends on T032)

**Checkpoint**: Painel Administrativo completo e segregado por RBAC operacional em conjunto com a fila de expedição geral.

---

## Phase 8: User Story 6 - Documentação Unificada na Raiz, Swagger Unificado e Correções Documentais (Priority: P2)

**Goal**: Consolidar a documentação técnica e de uso do sistema completo na raiz do repositório, alinhar a descrição OpenAPI/Swagger preservando contratos existentes e corrigir credenciais divergentes de demonstração.

**Independent Test**: Verificar título e descrição do Swagger UI em `/swagger-ui.html` sem regressão em `SwaggerESeedDemonstracaoTest`, conferir instruções completas no `README.md` raiz e validar credenciais reais no quickstart do Módulo 3.

### Implementation for User Story 6

- [X] T036 [P] [US6] Update `ConfiguracaoOpenApiSwagger` metadata and tags documenting Módulos 1, 2, 3 and Portal Web while strictly preserving substring `"Módulo 1"` in `Info.title(...)` (satisfying `SwaggerESeedDemonstracaoTest`) and updating helper method Javadocs in `src/main/java/br/edu/fatec/ecommerce/compartilhado/config/ConfiguracaoOpenApiSwagger.java`
- [X] T037 [P] [US6] Correct demo credentials from `Admin@Fatec2026` / `cliente.teste@fatec.edu.br` / `Cliente@Fatec2026` to actual runner credentials (`AdminFatec@2026` and `cliente.demo@ecommerce.fatec.edu.br` / `ClienteFatec@2026`) in `specs/003-checkout-pedidos-pagamentos/quickstart.md`
- [X] T038 [P] [US6] Create unified repository documentation covering architectural overview (Módulos 1, 2, 3 + Portal Web), GoF Design Patterns catalog, execution profiles (`H2`/`PostgreSQL`), URLs (`http://localhost:8080/`, `/swagger-ui.html`, `/h2-console`), demo credentials, coupons (`BEMVINDO10`, `FATEC50`), test commands, and consolidated E2E walkthrough in `README.md`

**Checkpoint**: Repositório unificado, autoexplicativo e consistente entre código-fonte, runners de demonstração, Swagger UI e documentação escrita.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Auditoria constitucional final, endurecimento de UI, suíte completa automatizada e validação ponta a ponta ao vivo.

- [X] T039 Audit all Java classes added or edited in feature `004` (`EntradaAutenticacaoRestSemPopup`, `ConfiguracaoSegurancaHttp`, `LojaConfiguracaoPublicaController`, `ConfiguracaoOpenApiSwagger`, `PromocoesPublicoController`, `CarrinhoComprasController`, `ServicoCarrinhoCompras`, `InicializadorDemonstracaoModulo2Runner`, `SimulacaoPagamentoDemoController`, `PedidoAdminController`, `ServicoConsultaPedido`, `PedidoJpaRepository`) confirming explicit single-responsibility methods and complete Javadocs per Constitution Principle III
- [X] T040 [P] Audit static frontend ES Modules (`src/main/resources/static/js/*.js`) and CSS (`src/main/resources/static/css/*.css`) verifying zero occurrences of `X-Gateway-Token`, XSS escaping on all user/domain strings injected in DOM templates, responsive behavior on narrow viewports, and zero console warnings/errors
- [X] T041 Run full integrated test suite via `.\mvnw.cmd clean test` validating 100% pass across all Módulo 1, Módulo 2, Módulo 3, and Módulo 4 (`PortalWebEEndpointsPublicosTest` + `MesclagemCarrinhoESimulacaoDemoTest`) test cases (depends on T001–T040)
- [X] T042 Validate live server boot (`.\mvnw.cmd spring-boot:run`) and execute end-to-end browser verification scenarios from `specs/004-loja-web-ecommerce/quickstart.md` confirming vitrine, anonymous-to-authenticated cart merge, Null Object address handling, freight/checkout, demo payment confirmation idempotency, and RBAC admin tabs (depends on T041)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Sem dependências; pode iniciar imediatamente (`T001` seguido de `T002`–`T004` em paralelo).
- **Phase 2 (Foundational)**: `T005`→`T006` bloqueiam rotas estáticas/públicas e segurança sem popup; `T009`→`T010`→`T011` bloqueiam todos os módulos JS de UI; `T007` e `T008` podem rodar em paralelo dentro da fase.
- **Phase 3 (US1 — MVP)**: Depende de Phase 2 (`T006`, `T010`, `T011`). Entrega vitrine pública navegável + promoções ativas de forma 100% independente.
- **Phase 4 (US2 — Carrinho)**: Depende de Phase 2 (`T010`) e integra botões de compra após `T015` (US1).
- **Phase 5 (US3 — Conta & Merge)**: Depende de `T006` + `T017` (leitura de `X-Sessao-Carrinho` para o `POST /api/v1/carrinho/mesclar`) e integra-se ao drawer de `T018` (US2).
- **Phase 6 (US4 — Checkout & Demo Payment)**: Depende de `T023`/`T024` (US3: endereço + histórico) e `T018` (US2: carrinho).
- **Phase 7 (US5 — Admin RBAC)**: Back-end (`T030`–`T032`) depende apenas de Phase 2 e corre em paralelo com US1–US4; front-end (`T033`–`T034`) pluga no `main.js` e `auth-cliente.js`.
- **Phase 8 (US6 — Docs)**: `T036`–`T038` independem dos módulos de UI e podem executar em paralelo após Phase 2 / US1–US5 definidos.
- **Phase 9 (Polish)**: Executa após conclusão de todas as histórias (`T039`–`T042`).

### User Story Dependencies

- **US1 (P1)**: Independente após Foundational → **Escopo MVP recomendado**.
- **US2 (P1)**: Independente no back-end; integra botões da vitrine (`US1`) na UI.
- **US3 (P1)**: Reutiliza `X-Sessao-Carrinho` (`US2`) e drawer do carrinho para mesclagem aditiva pós-login.
- **US4 (P1)**: Consome carrinho (`US2`) e endereço/pedidos (`US3`) para concluir checkout + simulação demo.
- **US5 (P2)**: Independente das telas de cliente após Foundational; compartilha autenticação integrada (`US3`).
- **US6 (P2)**: Documentação unificada e OpenAPI podem ser produzidos paralelamente assim que os contratos estiverem estáveis.

---

## Parallel Execution Examples

### Paralelismo na Fase 1 & Fase 2 (CSS + Back-end Auxiliar + Store JS)
```text
T002 (css/tokens.css) || T003 (css/layout.css) || T004 (css/components.css)
T007 (LojaConfiguracaoPublicaController) || T008 (InicializadorDemonstracaoModulo2Runner Javadocs/@Order) || T009 (js/store.js)
```

### Paralelismo entre User Stories (após Foundational concluída)
```text
Dev Back-end:  T012+T013 (US1 Promoções públicas) -> T021+T022 (US3 Merge) -> T026+T027 (US4 Simulação Demo) -> T030-T032 (US5 Admin Pedidos)
Dev Front-end: T014+T015 (US1 Vitrine) -> T018+T019 (US2 Carrinho) -> T023+T024 (US3 Auth/Conta) -> T028 (US4 Checkout) -> T033+T034 (US5 Admin)
Dev QA/Docs:   T016 (Teste US1) || T020/T025/T029/T035 (Suíte MockMvc Módulo 4) || T036+T037+T038 (US6 Swagger/Quickstart/README)
```

---

## Implementation Strategy

### MVP First (Phase 1 + Phase 2 + Phase 3 / US1)
1. Completar Setup (`T001`–`T004`) e Foundational (`T005`–`T011`).
2. Completar **User Story 1 (`T012`–`T016`)**.
3. **STOP & VALIDATE**: Abrir `http://localhost:8080/`, navegar pelas categorias recursivas, visualizar banners das campanhas ativas e validar `PortalWebEEndpointsPublicosTest`.

### Incremental Delivery
1. **Incremento 1 (MVP)**: Setup + Foundational + US1 (`Vitrine pública real`).
2. **Incremento 2 (Compra Anônima)**: US2 (`Carrinho lateral + Cupons + Memória de cálculo`).
3. **Incremento 3 (Conta & Checkout E2E)**: US3 (`Login sem popup + Merge aditivo + Endereços Null Object + Pedidos`) + US4 (`Frete ao vivo + Checkout + Simulação idempotente`).
4. **Incremento 4 (Backoffice & Entrega Final)**: US5 (`Painel Admin RBAC + Expedição`) + US6 (`README.md raiz + Swagger + Quickstart fix`) + Phase 9 (`.\mvnw.cmd clean test` e validação E2E ao vivo).

---

## Notes

- Todas as tarefas seguem estritamente o formato `[ ] TXXX [P?] [US?] Descrição com caminho de arquivo`.
- Nenhuma migration SQL (`V4`) é gerada neste ciclo; toda evolução utiliza tabelas e agregados existentes (`V1`–`V3`).
- Em `T005`–`T008`, `T012`–`T013`, `T017`, `T021`–`T022`, `T026`–`T027`, `T030`–`T032` e `T036`, todo método Java novo ou alterado DEVE conter Javadoc conforme o Princípio III da Constituição.

---

## Phase 10: Convergence

- [X] T043 [US5] Fix reserve expiration endpoint URL from `/api/v1/estoque/reservas/expirar-vencidas` to `/api/v1/admin/estoque/reservas/expirar-vencidas` in `src/main/resources/static/js/admin.js` per `spec.md` (`FR-010`), `contracts/modulo4-web-and-api-extensions.md` (§3), and `tasks.md` (`T033`) (contradicts)
- [X] T044 [US5] Add administrative catalog listing endpoint `GET /api/v1/admin/catalogo/itens` (backed by a new Javadoc-documented method in `src/main/java/br/edu/fatec/ecommerce/modulo1/catalogo/aplicacao/ServicoConsultaCatalogo.java` returning active and inactive SKUs/combos) in `src/main/java/br/edu/fatec/ecommerce/modulo1/catalogo/api/CatalogoAdminController.java`, consume it inside `renderizarAbaCatalogo` in `src/main/resources/static/js/admin.js` so inactivated items remain visible for reactivation, and cover with MockMvc tests per `spec.md` (`US5`, `FR-010`) and `tasks.md` (`T033`) (partial)
- [X] T045 [US3] Preserve or restore the original login/signup prompt inside `#conta-nao-autenticada` (`#btn-conta-entrar`) instead of permanently overwriting `innerHTML` when rendering non-`CLIENTE` operational sessions in `renderizarAreaConta` inside `src/main/resources/static/js/auth-cliente.js` per `spec.md` (`US3`, `FR-006`) and `tasks.md` (`T023`) (partial)
- [X] T046 [US4] Align demo payment simulation request DTO field name to `sucesso` (`@NotNull Boolean sucesso`) across `src/main/java/br/edu/fatec/ecommerce/modulo3/pagamento/api/dto/SimulacaoPagamentoDemoRequestDTO.java`, `src/main/java/br/edu/fatec/ecommerce/modulo3/pagamento/api/SimulacaoPagamentoDemoController.java`, `src/main/resources/static/js/checkout-pedidos.js`, and `src/test/java/br/edu/fatec/ecommerce/modulo4/` integration tests (keeping complete Javadocs per Constitution Principle III) per `contracts/modulo4-web-and-api-extensions.md` (§2.4), `data-model.md` (§2.3), and `tasks.md` (`T026`) (contradicts)
- [X] T047 [US4] Restrict demo gateway simulation buttons (`#btn-simular-webhook-pago`, `#btn-simular-webhook-recusa`) inside `renderizarConfirmacaoCheckout` to `statusAtual === 'AGUARDANDO_PAGAMENTO'` and hide/clear stale `#checkout-confirmacao-resultado` / `ultimoPedidoCheckout` upon logout or session change in `src/main/resources/static/js/checkout-pedidos.js` per `spec.md` (`US4`, `FR-008`, `Assumptions`) and `tasks.md` (`T028`) (contradicts)
- [X] T048 [P] [US6] Align OpenAPI tag name `"Configuração Pública da Loja"` between `src/main/java/br/edu/fatec/ecommerce/compartilhado/loja/api/LojaConfiguracaoPublicaController.java` and `src/main/java/br/edu/fatec/ecommerce/compartilhado/config/ConfiguracaoOpenApiSwagger.java`, and add Módulo 1 & Módulo 2 OpenAPI tag metadata in `ConfiguracaoOpenApiSwagger` (renaming `criarTagsModulo3()` and updating Javadocs per Constitution Principle III) per `tasks.md` (`T036`) (partial)
- [X] T049 [P] [US2] Align frontend fallback default `freteGratisPesoMaximoGramas` from `15000` to `5000` in `src/main/resources/static/js/store.js` and `src/main/resources/static/js/carrinho.js` per `specs/004-loja-web-ecommerce/data-model.md` (§2.2), `src/main/resources/application.yml`, and `LojaConfiguracaoPublicaController` (contradicts)
