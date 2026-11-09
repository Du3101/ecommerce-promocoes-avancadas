---

description: "Task list for Módulo 2 — Carrinho, Motor de Promoções Avançadas, Elegibilidade, Cupons e Formação de Preço"
---

# Tasks: Módulo 2 — Carrinho, Motor de Promoções Avançadas, Elegibilidade, Cupons e Formação de Preço

**Input**: Design documents from `/specs/002-carrinho-promocoes-cupons/`  
**Prerequisites**: [plan.md](./plan.md) (required), [spec.md](./spec.md) (required for user stories), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/modulo2-api-and-integration-ports.md](./contracts/modulo2-api-and-integration-ports.md)  
**Integrante 2**: Eduardo Juncker Toras  
**Tests**: Incluídos obrigatoriamente conforme o Princípio VI da [Constituição](../../.specify/memory/constitution.md) (testes automatizados em JUnit 5 cobrindo as 4 naturezas de promoções, árvores Composite, resolução de conflitos, Decorator de preço e cupons).  

**Regra Inegociável de Código (Princípio III da Constituição)**: Em **todas as tarefas abaixo**, absolutamente **todos os métodos** (públicos, protegidos e privados) DEVEM ser explícitos e possuir comentários (Javadoc e comentários internos nos passos do algoritmo) explicando: (1) objetivo funcional/comercial, (2) papel no Design Pattern correspondente e (3) parâmetros, retorno e exceções.

**Organization**: As tarefas estão agrupadas por User Story para permitir implementação, teste e demonstração independentes de cada entrega.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode ser executada em paralelo (arquivos diferentes, sem dependências pendentes)
- **[Story]**: Rastreabilidade direta para a User Story (`[US1]` a `[US6]`)
- Caminhos exatos de arquivo incluídos em todas as tarefas

---

## Phase 1: Setup & Database Migration (Módulo 2)

**Purpose**: Estruturar a tabela e persistência do Módulo 2 integrada ao Flyway e Spring Boot

- [X] T001 Criar migração SQL Flyway `V2__modulo2_carrinho_promocoes_cupons.sql` com tabelas `tb_carrinho`, `tb_item_carrinho`, `tb_campanha_promocional` e `tb_cupom_promocional` em `src/main/resources/db/migration/V2__modulo2_carrinho_promocoes_cupons.sql`
- [X] T002 [P] Implementar Value Object aritmético `OperacoesMonetarias` encapsulando operações seguras de `BigDecimal`, escala 2, arredondamento `RoundingMode.HALF_EVEN`, piso zero e rateio proporcional em `src/main/java/br/edu/fatec/ecommerce/modulo2/precificacao/dominio/OperacoesMonetarias.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelos base, repositórios JPA e objetos nulos que bloqueiam as User Stories

- [X] T003 [P] Implementar enum `TipoMecanicaPromocao` (`LEVE_X_PAGUE_Y`, `PERCENTUAL_CONDICIONAL`, `VALOR_FIXO_META_CARRINHO`, `COMBO_CRUZADO_CASADO`) e `TipoDescontoCupom` (`PERCENTUAL`, `VALOR_FIXO`) em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/TipoMecanicaPromocao.java`
- [X] T004 [P] Implementar classes do padrão `[Null Object]` `PromocaoNula` e `CupomNulo` com retorno neutro de desconto zero em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/estrategias/PromocaoNula.java` e `src/main/java/br/edu/fatec/ecommerce/modulo2/cupons/dominio/CupomNulo.java`
- [X] T005 Implementar repositórios JPA `CarrinhoComprasJpaRepository`, `CampanhaPromocionalJpaRepository` e `CupomPromocionalJpaRepository` em `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/infraestrutura/CarrinhoComprasJpaRepository.java`

**Checkpoint**: Fundação pronta — implementação das User Stories liberada.

---

## Phase 3: User Story 1 - Gestão do Carrinho e Recálculo Automático por Mutação (Priority: P1) 🎯 MVP

**Goal**: Permitir adicionar itens simples e combos ao carrinho, alterar quantidade, remover e recalcular automaticamente subtotal, peso e itens com validação no catálogo do Módulo 1.

### Tests for User Story 1

- [X] T006 [P] [US1] Criar testes unitários e de integração JUnit 5 para o carrinho (adição de produto/combo validando via mock/porta do Módulo 1, alteração de quantidade, remoção, peso total e recálculo) em `src/test/java/br/edu/fatec/ecommerce/modulo2/carrinho/CarrinhoComprasTest.java`

### Implementation for User Story 1

- [X] T007 [P] [US1] Implementar interface `ElementoCarrinhoComponent` e classes concretas `ItemCarrinho`, `ItemCarrinhoProduto` e `ItemCarrinhoComboKit` com suporte ao padrão `[Visitor]` em `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/dominio/ItemCarrinho.java`
- [X] T008 [US1] Implementar agregado raiz `CarrinhoCompras` com recálculo automático de itens, subtotal bruto e peso acumulado em `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/dominio/CarrinhoCompras.java`
- [X] T009 [US1] Implementar `ServicoCarrinhoCompras` consumindo `PortaConsultaCatalogoCliente` para validar existência e status ativo dos SKUs em `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/aplicacao/ServicoCarrinhoCompras.java`
- [X] T010 [P] [US1] Implementar controller REST `CarrinhoComprasController` (`GET /api/v1/carrinho`, `POST /api/v1/carrinho/itens`, `PUT /api/v1/carrinho/itens/{sku}`, `DELETE /api/v1/carrinho/itens/{sku}`, `DELETE /api/v1/carrinho`) em `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/api/CarrinhoComprasController.java`

**Checkpoint**: User Story 1 completa e testável de forma independente.

---

## Phase 4: User Story 2 & 3 - Motor de Promoções, 4 Naturezas Distintas e Árvore de Elegibilidade Composite (Priority: P1)

**Goal**: Criar o motor flexível com árvore recursiva de condições (`[Composite]`) e as 4 naturezas de promoções (`[Dynamic Factory]`).

### Tests for User Story 2 & 3

- [X] T011 [P] [US2/US3] Criar testes JUnit 5 para a árvore `[Composite]` de elegibilidade (`AND`, `OR`, `NOT`, valor mínimo, 1ª compra, categoria recursiva) e para as 4 estratégias de promoção em `src/test/java/br/edu/fatec/ecommerce/modulo2/promocoes/MotorPromocoesElegibilidadeTest.java`

### Implementation for User Story 2 & 3

- [X] T012 [P] [US3] Implementar interface `CondicaoElegibilidadeComponent` e nós compostos `[Composite]` `CondicaoAndComposite`, `CondicaoOrComposite` e `CondicaoNotComposite` em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/composite/CondicaoAndComposite.java`
- [X] T013 [P] [US3] Implementar folhas do `[Composite]` `CondicaoValorMinimoCarrinho`, `CondicaoPrimeiraCompraCliente`, `CondicaoCategoriaProduto`, `CondicaoItemEspecificoSku` e `CondicaoRegiaoEntrega` em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/composite/CondicaoValorMinimoCarrinho.java`
- [X] T014 [US2] Implementar interface `EstrategiaCalculoPromocao` e as 4 estratégias concretas de promoção: `PromocaoLeveXPagueY`, `PromocaoPercentualCondicional`, `PromocaoValorFixoMetaCarrinho` e `PromocaoComboCruzadoCasado` em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/estrategias/PromocaoLeveXPagueY.java`
- [X] T015 [US2] Implementar fábrica dinâmica `[Dynamic Factory]` `FabricaDinamicaEstrategiaPromocao` com injeção automática de estratégias do Spring (`List<EstrategiaCalculoPromocao>`) em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/fabrica/FabricaDinamicaEstrategiaPromocao.java`

**Checkpoint**: User Stories 2 e 3 completas e testáveis.

---

## Phase 5: User Story 4 - Resolução Determinística de Conflitos, Cupons e Comparable / Chain of Responsibility (Priority: P1)

**Goal**: Implementar resolução determinística de conflitos entre promoções exclusivas/cumulativas (`[Comparable]`), validação de cupons e pipeline `[Chain of Responsibility]`.

### Tests for User Story 4

- [X] T016 [P] [US4] Criar testes JUnit 5 para resolução de conflitos (`Comparable` por prioridade e maior desconto), desempate determinístico e validação de cupons válidos, esgotados e vencidos em `src/test/java/br/edu/fatec/ecommerce/modulo2/promocoes/ResolucaoConflitosECuponsTest.java`

### Implementation for User Story 4

- [X] T017 [US4] Implementar entidade `CampanhaPromocional` com `Comparable<CampanhaPromocional>` (prioridade configurada e maior benefício financeiro no desempate) em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/CampanhaPromocional.java`
- [X] T018 [P] [US4] Implementar entidade `CupomPromocional` (código alfanumérico, vigência, limites de uso globais e por cliente) em `src/main/java/br/edu/fatec/ecommerce/modulo2/cupons/dominio/CupomPromocional.java`
- [X] T019 [US4] Implementar elos da cadeia `[Chain of Responsibility]` `ValidadorPromocaoHandler`, `ValidadorCarrinhoNaoVazio`, `ValidadorElegibilidadeCampanha` e `ValidadorConflitoExclusividade` em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/dominio/cadeia/ValidadorPromocaoHandler.java`
- [X] T020 [US4] Implementar `ServicoCuponsPromocionais` (aplicação, validação e registro de consumo de cupons) em `src/main/java/br/edu/fatec/ecommerce/modulo2/cupons/aplicacao/ServicoCuponsPromocionais.java`

**Checkpoint**: User Story 4 completa e testável.

---

## Phase 6: User Story 5 - Pipeline de Formação de Preço Decorator, Visitor e Double Dispatch com Piso Zero (Priority: P2)

**Goal**: Estruturar a memória de cálculo do preço final com `[Decorator]` auditável, `[Visitor]` com duplo despacho e piso zero.

### Tests for User Story 5

- [X] T021 [P] [US5] Criar testes JUnit 5 para a formação de preço (Decorator encadeado, memória de cálculo discriminada, Visitor/Double Dispatch sobre produto e combo, e garantia de piso zero `BigDecimal.ZERO`) em `src/test/java/br/edu/fatec/ecommerce/modulo2/precificacao/PipelineFormacaoPrecoTest.java`

### Implementation for User Story 5

- [X] T022 [P] [US5] Implementar interface `ElementoCarrinhoVisitor` e classe concreta `CalculadoraDescontoVisitor` com duplo despacho em `src/main/java/br/edu/fatec/ecommerce/modulo2/carrinho/dominio/visitor/CalculadoraDescontoVisitor.java`
- [X] T023 [US5] Implementar interface `PrecoCalculadoComponent`, classe base `PrecoBaseCarrinho` e decoradores `[Decorator]` `DescontoDecoratorAbstrato`, `DescontoPromocaoDecorator` e `DescontoCupomDecorator` em `src/main/java/br/edu/fatec/ecommerce/modulo2/precificacao/dominio/DescontoPromocaoDecorator.java`
- [X] T024 [US5] Implementar `PipelineFormacaoPrecoService` orquestrando o cálculo do carrinho com memória de cálculo detalhada em `src/main/java/br/edu/fatec/ecommerce/modulo2/precificacao/aplicacao/PipelineFormacaoPrecoService.java`

**Checkpoint**: User Story 5 completa e testável.

---

## Phase 7: User Story 6 - Gestão Administrativa de Campanhas/Cupons e Porta para Checkout (Priority: P2)

**Goal**: Endpoints REST de administração com RBAC (`RESPONSAVEL_PROMOCOES`/`ADMIN`) e porta Java de integração com o Módulo 3.

### Tests for User Story 6

- [X] T025 [P] [US6] Criar testes de integração para os endpoints administrativos de campanhas e cupons e para o consumo da porta `PortaConsultaCarrinhoPromocoes` em `src/test/java/br/edu/fatec/ecommerce/modulo2/integracao/IntegracaoModulo2Test.java`

### Implementation for User Story 6

- [X] T026 [P] [US6] Implementar controller REST `PromocoesAdminController` (`GET /api/v1/admin/promocoes`, `POST /api/v1/admin/promocoes`, `PATCH /api/v1/admin/promocoes/{id}/status`) com proteção RBAC em `src/main/java/br/edu/fatec/ecommerce/modulo2/promocoes/api/PromocoesAdminController.java`
- [X] T027 [P] [US6] Implementar controller REST `CuponsAdminController` (`GET /api/v1/admin/cupons`, `POST /api/v1/admin/cupons`) com proteção RBAC em `src/main/java/br/edu/fatec/ecommerce/modulo2/cupons/api/CuponsAdminController.java`
- [X] T028 [US6] Implementar interface `PortaConsultaCarrinhoPromocoes`, DTO `ResumoCarrinhoCheckoutDTO` e adaptador `AdaptadorIntegracaoModulo2` para integração desacoplada com o Módulo 3 em `src/main/java/br/edu/fatec/ecommerce/modulo2/integracao/AdaptadorIntegracaoModulo2.java`

**Checkpoint**: User Story 6 completa e integração pronta.

---

## Phase 8: Polish, Seed de Demonstração e Documentação

**Purpose**: Documentação acadêmica completa dos padrões do Módulo 2 e validação final da suíte Maven

- [X] T029 Implementar seed de dados de demonstração do Módulo 2 (carrinho com itens, 4 promoções ativas e cupons de teste pré-carregados) em `src/main/java/br/edu/fatec/ecommerce/modulo2/integracao/InicializadorDemonstracaoModulo2Runner.java`
- [X] T030 Criar documentação arquitetural e defesa dos padrões do Módulo 2 em `docs/modulo2-arquitetura-e-padroes.md`
- [X] T031 Executar suíte completa de testes automatizados com `./mvnw.cmd clean test` garantindo 100% de sucesso e zero quebras no Módulo 1
