# Implementation Plan: Módulo 1 — Identidade, Catálogo, Composição de Produtos, Estoque e Auditoria

**Branch**: `001-catalogo-estoque-identidade` | **Date**: 2026-10-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-catalogo-estoque-identidade/spec.md`

## Summary

Este plano técnico estrutura a implementação da **frente do Integrante 1 (Módulo 1)** do Sistema de E-commerce com Promoções Avançadas: **Identidade e Controle de Acesso por Perfil**, **Gestão de Clientes e Múltiplos Endereços**, **Catálogo de Produtos com Variações e Categorias Hierárquicas Recursivas**, **Composição de Produtos em Combos/Kits**, **Controle Concorrente de Estoque com Reserva Atômica/Idempotente** e **Trilha de Auditoria Imutável**. A arquitetura é construída em **Java 21 + Spring Boot 3.4**, aplicando de forma explícita e documentada os padrões `Composite` (Composição Recursiva em categorias e combos), `Flyweight`, `Builder`, `Static Factory Method`, `Factory Method`, `Proxy`, `Singleton` e `Null Object` / `Missing Object`, além de expor portas de integração desacopladas (`PortaConsultaCatalogoCliente` e `PortaComandoReservaEstoque`) para os Módulos 2 e 3.

## Technical Context

**Language/Version**: Java 21 (LTS)

**Primary Dependencies**: Spring Boot 3.4.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`), Springdoc OpenAPI 2 (`springdoc-openapi-starter-webmvc-ui`), Flyway (`flyway-core`, `flyway-database-postgresql`), BCrypt (`PasswordEncoder`).

**Storage**: PostgreSQL 15+ (produção/execução persistente) e H2 Database em modo de compatibilidade PostgreSQL (execução autônoma de testes e desenvolvimento rápido).

**Testing**: JUnit 5 (Jupiter), Spring Boot Test, Spring Security Test, MockMvc e testes de concorrência multithread (`ExecutorService` + `CountDownLatch`) para disputa da última unidade em estoque.

**Target Platform**: JVM / Servidor Multiplataforma (Windows, Linux, macOS).

**Project Type**: Web Service / API REST modular (Monólito Modular orientado a Domínio e Design Patterns).

**Performance Goals**: Zero condição de corrida (*overselling*) em 50+ requisições simultâneas sobre o mesmo SKU (`SC-001`); consultas recursivas à árvore de categorias e cálculo de Combos em $< 100\text{ ms}$ para catálogos de médio porte.

**Constraints**: Todos os métodos (públicos, protegidos e privados) DEVEM ser explícitos e comentados via Javadoc/comentários internos explicando responsabilidade comercial, papel no Design Pattern e parâmetros/retorno (Constituição, Princípio III); proibição de condicionais extensas (`if/else`/`switch`) onde cabe polimorfismo (Princípio I); senhas exclusivamente em hash BCrypt (Princípio V).

**Scale/Scope**: Suporte a múltiplos perfis de usuário (`CLIENTE`, `ADMIN`, `OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE`, `RESPONSAVEL_PROMOCOES`), árvores de categorias e combos de até 10 níveis de profundidade, compartilhamento em memória de atributos intrínsecos via `Flyweight` e reservas atômicas com expiração configurável (TTL padrão de 30 minutos).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio da Constituição | Status Pré-Pesquisa | Status Pós-Design (Phase 1) | Evidência no Plano / Design |
| :--- | :--- | :--- | :--- |
| **I. Arquitetura Orientada a Design Patterns e Extensibilidade (OCP)** | ✅ PASS | ✅ PASS | Hierarquia polimórfica para itens de catálogo (`ItemCatalogoComponent`), criação (`CriadorItemCatalogo`), categorias (`ComponenteCategoria`) e endereços (`OpcaoEnderecoEntrega`), eliminando `if/else` por tipo e documentando as 3 perguntas de projeto em [research.md](./research.md). |
| **II. Catálogo Obrigatório de Padrões (Escopo Integrante 1)** | ✅ PASS | ✅ PASS | Todos os 8 padrões do Integrante 1 (`Composite`, `Flyweight`, `Builder`, `Static Factory Method`, `Factory Method`, `Proxy`, `Singleton`, `Null Object`) estão mapeados em classes reais do domínio em [data-model.md](./data-model.md). |
| **III. Código Explícito e Documentação Obrigatória em Todos os Métodos** | ✅ PASS | ✅ PASS | Regra obrigatória incorporada na estrutura de pacotes e critérios de conclusão de todas as classes (Javadoc + papel do padrão em 100% dos métodos). |
| **IV. Integridade Transacional, Idempotência e Concorrência de Estoque** | ✅ PASS | ✅ PASS | Reserva atômica com lock no banco + ordenação determinística de SKUs em Combos + chave de idempotência `chaveOperacaoPedido` e estados `ATIVA`/`CONFIRMADA`/`LIBERADA`/`EXPIRADA`. |
| **V. Separação Arquitetural em Camadas, Segurança e Auditoria** | ✅ PASS | ✅ PASS | Separação estrita em `dominio`, `aplicacao`, `infraestrutura`, `api` e `integracao`; senhas em BCrypt; RBAC por perfil; auditoria automática via `CatalogoAdminAuditoriaProxy` e `EstoqueAdminSegurancaProxy`. |
| **VI. Testes Automatizados (JUnit) e Documentação UML** | ✅ PASS | ✅ PASS | Diagrama de classes e diagrama de estados da reserva gerados em [data-model.md](./data-model.md); cenários de teste de concorrência, composição recursiva, `Flyweight`, `Null Object` e `Proxy` definidos. |
| **Divisão em 3 Integrantes sem Conflito de Código** | ✅ PASS | ✅ PASS | O Módulo 1 fica isolado no pacote `br.edu.fatec.ecommerce.modulo1` e expõe portas limpas (`PortaConsultaCatalogoCliente`, `PortaComandoReservaEstoque`) para os Integrantes 2 (`modulo2`) e 3 (`modulo3`). |

## Project Structure

### Documentation (this feature)

```text
specs/001-catalogo-estoque-identidade/
├── plan.md              # Este arquivo (Plano de Implementação do Módulo 1)
├── research.md          # Decisões técnicas e justificativas dos Design Patterns (Phase 0)
├── data-model.md        # Modelo de domínio, UML e máquina de estados da reserva (Phase 1)
├── quickstart.md        # Guia de execução e roteiro de validação fim-a-fim (Phase 1)
├── contracts/           # Contratos REST e Portas de Integração com Módulos 2 e 3 (Phase 1)
│   └── modulo1-api-and-integration-ports.md
├── checklists/
│   └── requirements.md  # Checklist de qualidade da especificação
└── tasks.md             # Lista de tarefas ordenadas (gerada pelo comando /speckit-tasks)
```

### Source Code (repository root)

```text
pom.xml
mvnw
mvnw.cmd
src/
├── main/
│   ├── java/
│   │   └── br/edu/fatec/ecommerce/
│   │       ├── EcommercePromocoesApplication.java
│   │       ├── compartilhado/                        # Utilitários base, segurança global e tratamento de exceções REST
│   │       ├── modulo1/                              # ESCOPO EXCLUSIVO DO INTEGRANTE 1
│   │       │   ├── configuracao/
│   │       │   │   └── ConfiguracaoModuloCatalogoEstoqueSingleton.java   # [Singleton]
│   │       │   ├── identidade/
│   │       │   │   ├── dominio/
│   │       │   │   │   ├── Usuario.java                                  # [Static Factory Method]
│   │       │   │   │   ├── PerfilUsuario.java
│   │       │   │   │   ├── Cliente.java
│   │       │   │   │   ├── ClienteBuilder.java                           # [Builder]
│   │       │   │   │   ├── OpcaoEnderecoEntrega.java
│   │       │   │   │   ├── Endereco.java
│   │       │   │   │   ├── EnderecoBuilder.java                          # [Builder]
│   │       │   │   │   └── EnderecoAusenteNullObject.java                # [Null Object / Missing Object]
│   │       │   │   ├── aplicacao/
│   │       │   │   ├── infraestrutura/
│   │       │   │   └── api/
│   │       │   ├── catalogo/
│   │       │   │   ├── dominio/
│   │       │   │   │   ├── categoria/
│   │       │   │   │   │   ├── ComponenteCategoria.java                  # [Composite - Component]
│   │       │   │   │   │   ├── CategoriaComp.java                        # [Composite - Composite/Leaf]
│   │       │   │   │   │   └── CategoriaRaizNullObject.java              # [Null Object]
│   │       │   │   │   ├── item/
│   │       │   │   │   │   ├── ItemCatalogoComponent.java                # [Composite - Component]
│   │       │   │   │   │   ├── Produto.java
│   │       │   │   │   │   ├── ProdutoBuilder.java                       # [Builder]
│   │       │   │   │   │   ├── ProdutoVariacaoLeaf.java                  # [Composite - Leaf]
│   │       │   │   │   │   ├── ComboKitComposite.java                    # [Composite - Composite]
│   │       │   │   │   │   ├── ComboKitBuilder.java                      # [Builder]
│   │       │   │   │   │   ├── ItemComponenteKit.java
│   │       │   │   │   │   └── DimensoesFisicas.java                     # [Static Factory Method]
│   │       │   │   │   ├── flyweight/
│   │       │   │   │   │   ├── AtributoVariacaoFlyweight.java            # [Flyweight]
│   │       │   │   │   │   ├── FabricaAtributoVariacaoFlyweight.java     # [Flyweight Factory]
│   │       │   │   │   │   └── TipoAtributoVariacao.java
│   │       │   │   │   └── fabrica/
│   │       │   │   │       ├── CriadorItemCatalogo.java                  # [Factory Method - Creator]
│   │       │   │   │       ├── CriadorProdutoPadrao.java                 # [Factory Method - ConcreteCreator]
│   │       │   │   │       └── CriadorComboKitPromocional.java           # [Factory Method - ConcreteCreator]
│   │       │   │   ├── aplicacao/
│   │       │   │   │   ├── ServicoAdminCatalogoPort.java                 # [Proxy - Subject]
│   │       │   │   │   ├── ServicoAdminCatalogoReal.java                 # [Proxy - RealSubject]
│   │       │   │   │   └── CatalogoAdminAuditoriaProxy.java              # [Proxy - Protection & Audit Proxy]
│   │       │   │   ├── infraestrutura/
│   │       │   │   └── api/
│   │       │   ├── estoque/
│   │       │   │   ├── dominio/
│   │       │   │   │   ├── ItemEstoque.java
│   │       │   │   │   ├── ReservaEstoque.java                           # [Static Factory Method]
│   │       │   │   │   ├── ItemReservaEstoque.java
│   │       │   │   │   ├── EstadoReservaEstoque.java
│   │       │   │   │   └── MovimentacaoEstoque.java                      # [Static Factory Method]
│   │       │   │   ├── aplicacao/
│   │       │   │   │   ├── ServicoAdminEstoquePort.java                  # [Proxy - Subject]
│   │       │   │   │   ├── ServicoAdminEstoqueReal.java                  # [Proxy - RealSubject]
│   │       │   │   │   ├── EstoqueAdminSegurancaProxy.java               # [Proxy - Protection & Audit Proxy]
│   │       │   │   │   └── ServicoReservaEstoqueConcorrente.java
│   │       │   │   ├── infraestrutura/
│   │       │   │   └── api/
│   │       │   ├── auditoria/
│   │       │   │   ├── dominio/
│   │       │   │   │   ├── RegistroAuditoria.java                        # [Static Factory Method]
│   │       │   │   │   └── TipoOperacaoAuditoria.java
│   │       │   │   ├── aplicacao/
│   │       │   │   ├── infraestrutura/
│   │       │   │   └── api/
│   │       │   └── integracao/
│   │       │       ├── PortaConsultaCatalogoCliente.java                 # Contrato para Módulos 2 e 3
│   │       │       ├── PortaComandoReservaEstoque.java                   # Contrato para Módulo 3
│   │       │       └── AdaptadorIntegracaoModulo1Service.java
│   │       ├── modulo2/                              # Reservado ao Integrante 2 (Carrinho, Promoções, Cupons, Preço)
│   │       └── modulo3/                              # Reservado ao Integrante 3 (Checkout, Pedidos, Pagamentos, Frete)
│   └── resources/
│       ├── application.yml
│       ├── application-test.yml
│       └── db/migration/
│           └── V1__modulo1_identidade_catalogo_estoque_auditoria.sql
└── test/
    └── java/
        └── br/edu/fatec/ecommerce/modulo1/
            ├── identidade/
            ├── catalogo/
            ├── estoque/
            └── auditoria/
```

**Structure Decision**: Adotamos um **Monólito Modular em Spring Boot** na raiz do repositório, segregando cada integrante em seu próprio pacote raiz (`modulo1`, `modulo2`, `modulo3`) e numerando as migrações SQL por módulo (`V1__modulo1_...`). Essa decisão permite que os 3 integrantes trabalhem simultaneamente no mesmo repositório Git sem conflitos de merge em arquivos de domínio/serviço, comunicando-se exclusivamente pelas interfaces do pacote `integracao`.

---

## Quadro Justificativo dos Design Patterns do Módulo 1 (Critério de Defesa)

| Padrão / Metodologia | Classes Participantes no Módulo 1 | 1. Qual problema existia? | 2. Por que este padrão foi escolhido? | 3. O que ficou mais simples de modificar? |
| :--- | :--- | :--- | :--- | :--- |
| **Composite (Composição Recursiva)** | `ComponenteCategoria`, `CategoriaComp`; `ItemCatalogoComponent`, `ProdutoVariacaoLeaf`, `ComboKitComposite` | Categorias possuem subcategorias em profundidade variável, e Combos/Kits podem agrupar produtos simples ou outros subkits, o que geraria `if/else` complexos para calcular preço, peso e estoque. | Permite tratar objetos individuais (`Leaf`) e composições recursivas (`Composite`) através da mesma interface polimórfica. | Adicionar novos níveis de subcategorias, aninhar kits dentro de kits ou criar novas políticas de cálculo agregado sem tocar no código do catálogo ou do carrinho. |
| **Flyweight** | `AtributoVariacaoFlyweight`, `FabricaAtributoVariacaoFlyweight`, `ProdutoVariacaoLeaf` | Milhares de variações de roupas/eletrônicos repetem os mesmos atributos imutáveis (Tamanho P/M/G, Cor Preta/Branca, 128GB/256GB), desperdiçando memória e despadronizando metadados. | Separa o estado intrínseco compartilhável (`AtributoVariacaoFlyweight`) do estado extrínseco exclusivo da variação (SKU, saldo e adicional de preço). | Incluir novos tipos de atributos padronizados (ex.: Voltagem, Material, Idioma) reutilizados instantaneamente por todo o catálogo. |
| **Builder** | `ProdutoBuilder`, `ComboKitBuilder`, `ClienteBuilder`, `EnderecoBuilder` | Entidades como `Produto` e `ComboKitComposite` possuem dezenas de parâmetros (dimensões, peso, marca, variações, modo de preço), tornando construtores longos ilegíveis e propensos a erro. | Constrói objetos complexos passo a passo com métodos fluentes autoexplicativos e valida todas as invariantes no método final `construir()`. | Adicionar novos atributos opcionais ao produto ou endereço sem quebrar chamadas de construtores existentes. |
| **Static Factory Method** | `Usuario`, `ReservaEstoque`, `RegistroAuditoria`, `DimensoesFisicas`, `MovimentacaoEstoque` | Construtores tradicionais não expressam a regra de negócio da criação (ex.: diferenciar criação de cliente público vs. operador interno, ou reserva ativa vs. ajuste de estoque). | Fornece métodos estáticos com nomes explícitos de domínio que documentam a intenção e validam pré-condições específicas de cada fábrica. | Criar novas formas especializadas de instanciar entidades ou reaproveitar instâncias sem mudar assinaturas de construtores. |
| **Factory Method** | `CriadorItemCatalogo`, `CriadorProdutoPadrao`, `CriadorComboKitPromocional` | A lógica de criação e preparação de um produto físico com variações difere da criação de um Combo/Kit ou de novos tipos futuros de produtos. | Delega a instanciação e validação específica para subclasses criadoras especializadas que implementam o contrato `criarItemCatalogo()`. | Acrescentar novos tipos de produtos no futuro (ex.: `CriadorProdutoPerecivel`, `CriadorLivro`) sem alterar os serviços existentes. |
| **Proxy** | `ServicoAdminCatalogoPort`, `ServicoAdminCatalogoReal`, `CatalogoAdminAuditoriaProxy`; `ServicoAdminEstoquePort`, `ServicoAdminEstoqueReal`, `EstoqueAdminSegurancaProxy` | Operações administrativas exigem validação estrita de perfil e gravação obrigatória na trilha de auditoria antes/depois da execução, o que poluiria a lógica de negócio. | O Proxy envolve o serviço real implementando a mesma interface, interceptando chamadas para checar permissões e persistir o `RegistroAuditoria` de forma transparente. | Incluir novas regras de auditoria, bloqueio de horário ou cache de leitura sem alterar uma única linha de `ServicoAdminCatalogoReal` ou `ServicoAdminEstoqueReal`. |
| **Singleton** | `ConfiguracaoModuloCatalogoEstoqueSingleton` | Parâmetros operacionais globais do módulo (TTL de expiração de reserva, profundidade máxima de árvores recursivas, moeda) precisam de um ponto único e consistente de acesso. | Garante a existência de uma única instância controlada e *thread-safe* compartilhada em toda a aplicação. | Alterar políticas globais (como ajustar o TTL de reserva durante testes ou apresentação) em um único ponto centralizado. |
| **Null Object / Missing Object** | `OpcaoEnderecoEntrega`, `EnderecoAusenteNullObject`; `CategoriaRaizNullObject` | Clientes recém-cadastrados não têm endereço selecionado e categorias de 1º nível não têm categoria pai, o que espalharia `if (x != null)` e risco de `NullPointerException`. | Substitui a referência `null` por um objeto polimórfico que implementa a mesma interface com comportamento neutro e seguro. | Evoluir a formatação de endereço ou navegação de categorias sem precisar adicionar verificações de nulidade nos Módulos 1, 2 e 3. |

## Complexity Tracking

> **Nenhuma violação à Constituição foi introduzida.** Todos os componentes e padrões previstos atendem diretamente aos Princípios I a VI de [constitution.md](../../.specify/memory/constitution.md).
