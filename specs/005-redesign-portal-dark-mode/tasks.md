---

description: "Task list for Redesign Profissional do Portal Web e Modo Escuro"
---

# Tasks: Redesign Profissional do Portal Web e Modo Escuro (`005`)

**Input**: Design documents from `/specs/005-redesign-portal-dark-mode/`  
**Prerequisites**: [plan.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/plan.md), [spec.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/spec.md), [research.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/research.md), [data-model.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/data-model.md), [ui-theme-and-components.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/contracts/ui-theme-and-components.md), [quickstart.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/quickstart.md)

**Tests**: Preservação integral dos 98 testes automatizados existentes (`.\mvnw.cmd clean test`), extensão de assertivas MockMvc para entrega do tema/hero e validação E2E real no navegador conforme [quickstart.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/quickstart.md).

**Organization**: Tarefas agrupadas por História de Usuário (P1 → P2 → P3) para entregas incrementais independentemente testáveis, precedidas por Setup e Fundação de tokens CSS semânticos.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode executar em paralelo (arquivos distintos, sem dependência de tarefa incompleta)
- **[Story]**: História correspondente em `spec.md` (`[US1]` a `[US5]`)

---

## Phase 1: Setup (Auditoria & Linha de Base)

**Purpose**: Inventariar seletores funcionais preservados e cores hardcoded existentes antes de alterar o sistema visual

- [X] T001 Auditar todas as cores fixas herdadas e validar o inventário completo de IDs/seletores preservados (`#btn-*`, `#view-*`, `#drawer-*`, `#modal-*`, `#checkout-*`, `#admin-*` e atributos `data-*`) em `src/main/resources/static/index.html`, `src/main/resources/static/css/layout.css` e `src/main/resources/static/css/components.css` conforme `contracts/ui-theme-and-components.md` (§4)

---

## Phase 2: Foundational (Tokens Semânticos Claro/Escuro & Base CSS)

**Purpose**: Construir a fundação completa de Design Tokens (*Tech Store Contemporânea*, WCAG AA) e remover cores fixas herdadas em todas as folhas de estilo

**⚠️ CRITICAL**: Nenhuma User Story visual deve ser iniciada antes desta fundação semântica estar consolidada

- [X] T002 Expandir `src/main/resources/static/css/tokens.css` definindo `:root` / `:root[data-theme="light"]` (`color-scheme: light`) e `:root[data-theme="dark"]` (`color-scheme: dark`) com paleta completa de superfícies (`--color-bg`, `--color-surface`, `--color-surface-alt`, `--color-surface-elevated`), sobreposições (`--color-header-bg`, `--color-backdrop`, `--color-topbar-bg`, `--color-topbar-fg`), textos (`>= 4,5:1`), acento de marca (`--color-primary: #2563eb` / `--color-primary-contrast: #ffffff` no claro; `#38bdf8` / `#082f49` no escuro), hero/banners, mídia/skeletons, `--color-focus-ring`, feedback semântico e os 8 pares `--status-{criado,aguardando,pago,preparacao,enviado,entregue,cancelado,recusado}-{bg,fg}` conforme `data-model.md` (§2) e `research.md` (§2)
- [X] T003 [P] Refatorar `src/main/resources/static/css/layout.css` substituindo cores fixas (`.topbar-info`, `.site-header`, `.drawer-backdrop`) por variáveis semânticas de `tokens.css`, refinando escala tipográfica/espaçamentos e adicionando regras globais de `:focus-visible` e `@media (prefers-reduced-motion: reduce)`
- [X] T004 [P] Refatorar `src/main/resources/static/css/components.css` eliminando todas as cores hexadecimais/rgba fixas em `.badge-neutral`, `.alert-*`, `.timeline-step`, `.breakdown-*`, banners e modais em favor de tokens semânticos de `src/main/resources/static/css/tokens.css`

**Checkpoint**: Fundação de tokens semânticos e contraste claro/escuro pronta para implementar as histórias de usuário

---

## Phase 3: User Story 1 - Alternância de Tema Claro e Modo Escuro com Persistência (Priority: P1) 🎯 MVP

**Goal**: Entregar alternância direta em 1 clique (**Modo Claro ↔ Modo Escuro**) no cabeçalho, com detecção automática inicial do dispositivo, acompanhamento dinâmico antes da escolha manual e persistência local sem flash inicial (FOUC).

**Independent Test**: Abrir `http://localhost:8080/` em sessão limpa emulando `prefers-color-scheme: dark`, confirmar abertura direta em `data-theme="dark"` sem flash claro, abrir drawer/modal, clicar em `#btn-alternar-tema` e recarregar verificando persistência em `localStorage['fatec_store_tema_v1']`.

### Implementation for User Story 1

- [X] T005 [US1] Adicionar script síncrono mínimo anti-FOUC no `<head>` (resolvendo `localStorage['fatec_store_tema_v1']` se `'light'|'dark'`, senão `matchMedia('(prefers-color-scheme: dark)')`, fallback `'light'`, aplicando em `document.documentElement.dataset.theme`) e incluir o botão `#btn-alternar-tema` (`type="button"`, `aria-pressed="false"`, ícone SVG sol/lua acessível) em `.header-actions` de `src/main/resources/static/index.html` conforme `contracts/ui-theme-and-components.md` (§1.1–§1.2)
- [X] T006 [US1] Implementar o estado reativo de tema (`tema`: `'light' | 'dark'`, `origem`: `'manual' | 'sistema'`), funções explícitas `obterTemaAtual()`, `alternarTema()` (1 clique `light ↔ dark` gravando `'fatec_store_tema_v1'`) e listener ao vivo de `matchMedia('(prefers-color-scheme: dark)')` quando `origem === 'sistema'` emitindo evento `'TEMA'` em `src/main/resources/static/js/store.js` (com JSDoc completo conforme Princípio III)
- [X] T007 [US1] Conectar `#btn-alternar-tema` no orquestrador `src/main/resources/static/js/main.js` sincronizando `aria-pressed`, `aria-label`, `title` e ícone sol/lua via `assinarEstado` (`TEMA`), e estilizar o controle de tema no cabeçalho em `src/main/resources/static/css/layout.css` (depende de T005, T006)

**Checkpoint**: Alternância de tema claro/escuro funcional em 1 clique com persistência, sincronização inicial de sistema e contraste base em toda a aplicação

---

## Phase 4: User Story 2 - Vitrine Pública Moderna, Hero Promocional e Catálogo Refinado (Priority: P1)

**Goal**: Modernizar a Vitrine Pública com seção hero editorial compacta (recolhida automaticamente durante busca/filtro), banners promocionais destacados, navegação lateral de categorias refinada, cards de produto/combo com ilustrações vetoriais SVG temáticas, skeletons e estados vazios.

**Independent Test**: Acessar a vitrine como visitante em claro e escuro, confirmar `#vitrine-hero` visível na tela inicial, filtrar por categoria/busca confirmando recolhimento automático de `#vitrine-hero`, limpar filtros vendo o hero retornar, inspecionar ilustrações SVG nos cards simples e combos, e validar estado vazio de busca.

### Implementation for User Story 2

- [X] T008 [US2] Inserir a seção hero editorial compacta `#vitrine-hero` dentro de `#view-vitrine > .container` em `src/main/resources/static/index.html` com título comercial, selo de ofertas, destaque dinâmico e botões de atalho para catálogo/promoções preservando todos os IDs e estrutura das seções seguintes conforme `contracts/ui-theme-and-components.md` (§2)
- [X] T009 [US2] Estilizar `.vitrine-hero`, `.promo-banner-card`, `.category-sidebar`, `.product-card`, `.product-card-media` (topo ilustrado), `.variation-chip`, `.combo-components-box`, `.skeleton-card` e `.empty-state-card` com microinterações discretas em `src/main/resources/static/css/layout.css` e `src/main/resources/static/css/components.css`
- [X] T010 [US2] Implementar `obterIlustracaoCardHtml(item)` (classificação determinística `combo | teclado | mouse | audio | monitor | hardware` via SVG inline), controle reativo `atualizarVisibilidadeHero()` (`hidden = Boolean(estado.categoriaSelecionadaId || estado.termoBusca)`), placeholders de carregamento aprimorados e atalhos no hero em `src/main/resources/static/js/catalogo.js` e `src/main/resources/static/js/main.js` (com JSDoc completo em português conforme Princípio III), preservando todos os seletores `[data-sku-card]`, `[data-add-sku]`, `[data-detalhe-sku]` e `[data-variacao-sku]` (depende de T008, T009)

**Checkpoint**: Vitrine pública e tema escuro completos, entregando um incremento visual completo de alto impacto para visitantes

---

## Phase 5: User Story 3 - Experiência Refinada de Carrinho, Autenticação, Conta e Checkout (Priority: P2)

**Goal**: Elevar o acabamento visual, legibilidade de valores/descontos e contraste claro/escuro da gaveta do carrinho, modais de login/cadastro, área do cliente e fluxo de checkout/linha do tempo.

**Independent Test**: Realizar jornada completa visitante → carrinho com cupom `BEMVINDO10` → login cliente demo → Minha Conta (endereços e pedidos) → Checkout PIX + simulação demo e timeline alternando Claro ↔ Escuro sem quebras nem falhas de contraste.

### Implementation for User Story 3

- [X] T011 [US3] Aprimorar estilos do drawer `.cart-drawer`, `.cart-item-row`, controles `.qty-stepper`, barra `.free-shipping-bar`, feedback `.coupon-feedback` e resumo `.price-breakdown` garantindo destaque claro de descontos e total líquido nos dois temas em `src/main/resources/static/css/components.css`
- [X] T012 [US3] Aprimorar estilos dos modais (`.modal-dialog`, `.modal-tabs`, `.quick-demo-box`, `.demo-account-card`), cartões de Minha Conta (`.address-card`, `.order-card`, `#conta-operacional-aviso`), `.checkout-grid`, `.payment-option`, `.confirmation-card` e 8 estados da `.order-timeline` em `src/main/resources/static/css/components.css` (depende de T011)
- [X] T013 [US3] Revisar ajustes visuais/classes auxiliares sem alterar lógica funcional nem seletores em `src/main/resources/static/js/carrinho.js`, `src/main/resources/static/js/auth-cliente.js` e `src/main/resources/static/js/checkout-pedidos.js` garantindo preservação de `#btn-conta-entrar`, payload `sucesso` na simulação demo e zero estilos de cor inline (depende de T011, T012)

**Checkpoint**: Toda a jornada de compra do cliente final está refinada e legível em ambos os temas

---

## Phase 6: User Story 4 - Painel Administrativo Claro, Organizado e Produtivo (Priority: P2)

**Goal**: Refinar abas RBAC, formulários de criação e tabelas operacionais densas garantindo leitura tabular nítida, alinhamento monetário e distinção clara de ações em Modo Claro e Modo Escuro.

**Independent Test**: Autenticar como `ADMIN`, navegar por todas as abas (`Catálogo`, `Estoque`, `Promoções`, `Expedição`, `Auditoria`, `Relatórios`) nos dois temas confirmando legibilidade de cabeçalhos, números tabulares, inputs inline e emblemas de status.

### Implementation for User Story 4

- [X] T014 [US4] Aprimorar estilos de `.admin-tabs`, `.admin-tab-btn`, `.admin-panel-body`, `.data-table` (`font-variant-numeric: tabular-nums`, cabeçalhos tintados, linhas com hover sutil, contenção `.table-responsive`) e hierarquia de ações/inputs inline em `src/main/resources/static/css/components.css`
- [X] T015 [US4] Revisar alinhamento de colunas numéricas/monetárias, cartões de métricas nos relatórios e estados vazios em `src/main/resources/static/js/admin.js` preservando chamadas `GET /api/v1/admin/catalogo/itens`, `POST /api/v1/admin/estoque/reservas/expirar-vencidas` e dispatch RBAC (depende de T014)

**Checkpoint**: Painel administrativo completo, produtivo e acessível nos dois temas

---

## Phase 7: User Story 5 - Responsividade, Acessibilidade e Preservação Integral de Fluxos (Priority: P3)

**Goal**: Garantir layout responsivo livre de rolagem horizontal na página a partir de `360px`, navegação acessível por teclado e cobertura automatizada validando os novos hooks de interface entregues pelo Spring Boot.

**Independent Test**: Medir `document.documentElement.scrollWidth <= window.innerWidth` em `360x800`, `375x812` e `1366x900` em Vitrine, Checkout e Admin, testar foco via Tab e executar a suíte `.\mvnw.cmd clean test`.

### Implementation for User Story 5

- [X] T016 [US5] Ajustar breakpoints responsivos (`<= 1024px`, `<= 768px`, `<= 480px`, suporte até `360px`) em `src/main/resources/static/css/layout.css` e `src/main/resources/static/css/components.css` garantindo encaixe limpo de `#btn-alternar-tema`, busca, barra lateral de categorias, `.vitrine-hero`, checkout e `.table-responsive` com `0px` de rolagem horizontal na página
- [X] T017 [P] [US5] Estender `src/test/java/br/edu/fatec/ecommerce/modulo4/PortalWebEEndpointsPublicosTest.java` (com Javadoc completo conforme Princípio III) para validar que `GET /index.html` contém `#btn-alternar-tema`, `#vitrine-hero` e o bootstrap anti-FOUC `fatec_store_tema_v1`, e que `GET /css/tokens.css` entrega os tokens `[data-theme="dark"]`

**Checkpoint**: Responsividade, acessibilidade e contrato de entrega estática garantidos e cobertos por testes automatizados

---

## Phase 8: Polish & Cross-Cutting Verification

**Purpose**: Verificação final de higiene, conformidade Java/JS, suíte Maven e bateria E2E real no navegador

- [X] T018 [P] Validar sintaxe de todos os 8 módulos JavaScript em `src/main/resources/static/js/` (`node --check`) e auditar ausência de cores claras fixas residuais e conformidade de JSDoc/Javadoc nas alterações realizadas
- [X] T019 Executar a suíte completa de testes automatizados (`.\mvnw.cmd clean test`) confirmando 100% de aprovação (`0` falhas, `0` erros, `0` ignorados)
- [X] T020 Executar validação E2E em navegador real (Microsoft Edge headless via CDP sobre `.\mvnw.cmd spring-boot:run` em `http://localhost:8080/`) cobrindo os Cenários A–D de `specs/005-redesign-portal-dark-mode/quickstart.md` (tema automático escuro do sistema, alternância 1-clique + persistência, recolhimento de `#vitrine-hero` em filtros, ilustrações SVG dos cards, fluxos completos de compra/admin em claro e escuro, verificação programática da razão de contraste WCAG AA via `getComputedStyle`/luminância em botões, textos, alertas e badges nos dois temas, e `0px` de overflow em `360x800`, `375x812` e `1366x900`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1 — T001)**: Sem dependências — inicia imediatamente.
- **Foundational (Phase 2 — T002–T004)**: Depende de `T001` — **BLOQUEIA** todas as histórias de usuário; `T003` e `T004` podem rodar em paralelo após `T002`.
- **User Story 1 (Phase 3 — T005–T007)**: Depende da Phase 2; entrega o sistema completo de Modo Claro/Escuro (MVP de tema).
- **User Story 2 (Phase 4 — T008–T010)**: Depende da Phase 2 (e sequencia edições compartilhadas em `index.html`, `layout.css` e `main.js` após Phase 3).
- **User Story 3 (Phase 5 — T011–T013)** e **User Story 4 (Phase 6 — T014–T015)**: Dependem da Phase 2 (`tokens.css`) e sequenciam edições no mesmo arquivo `components.css`; após o CSS estar consolidado, revisões JS em arquivos distintos (`T013` vs `T015`) seguem sem conflito.
- **User Story 5 (Phase 7 — T016–T017)**: Depende dos ajustes visuais de US1–US4; `T017` pode rodar em paralelo assim que `T002`, `T005` e `T008` estiverem prontos.
- **Polish (Phase 8 — T018–T020)**: Depende da conclusão de todas as fases anteriores.

### User Story Dependencies

- **US1 (P1)**: Independente após Foundational.
- **US2 (P1)**: Independente após Foundational; adapta-se automaticamente aos tokens de tema definidos em `T002`.
- **US3 (P2)** & **US4 (P2)**: Independentes entre si em comportamento após Foundational.
- **US5 (P3)**: Valida o conjunto integrado em larguras móveis (`>= 360px`) e testes automatizados.

---

## Parallel Example: Foundational & Story Tasks

```text
# Após T002 (tokens.css) estar pronto, refatorar layout e componentes em paralelo (arquivos distintos):
Task: T003 [P] Refatorar src/main/resources/static/css/layout.css com variáveis semânticas
Task: T004 [P] Refatorar src/main/resources/static/css/components.css removendo hex/rgba fixos

# Após T005 e T008 alterarem index.html e tokens.css, rodar extensão MockMvc em paralelo à revisão JS:
Task: T017 [P] [US5] Estender PortalWebEEndpointsPublicosTest.java com assertivas de tema/hero
Task: T018 [P] Validar sintaxe JS com node --check nos 8 módulos
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Concluir Phase 1 (`T001`) + Phase 2 (`T002`–`T004`).
2. Implementar Phase 3 (`US1` — Modo Claro/Escuro 1-clique anti-FOUC).
3. Implementar Phase 4 (`US2` — Hero editorial recolhível, cards ilustrados e vitrine moderna).
4. Validar visualmente no navegador em ambos os temas antes de seguir para `US3`–`US5`.

### Incremental Delivery

1. Fundação de tokens semânticos → base livre de cores fixas.
2. US1 → Tema Claro/Escuro funcional em todo o portal.
3. US2 → Vitrine moderna e apresentável ao público.
4. US3 & US4 → Carrinho, Conta, Checkout e Painel Administrativo alinhados nos dois temas.
5. US5 & Polish → Responsividade a partir de `360px`, testes MockMvc (`.\mvnw.cmd clean test`) e E2E real completo.

---

## Notes

- Todos os arquivos alterados concentram-se em `src/main/resources/static/` e no teste de contrato estático em `PortalWebEEndpointsPublicosTest.java`.
- Preservar integralmente IDs HTML, atributos `data-*`, contratos REST e fluxos validados na feature `004-loja-web-ecommerce`.
- Garantir JSDoc em todas as novas funções JS e Javadoc conforme o Princípio III no arquivo de teste Java alterado.
