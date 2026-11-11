# Research & Design Patterns Defense: Módulo 3 — Checkout, Ciclo de Vida de Pedidos, Pagamentos Idempotentes, Frete, Notificações e Relatórios Gerenciais

**Feature**: `003-checkout-pedidos-pagamentos`  
**Date**: 2026-11-11  
**Integrante 3**: Henrique Sandrini  
**Status**: Completed  

---

## 1. Contexto Acadêmico e Decisões de Projeto

O **Módulo 3** é responsável pela consolidação transacional final do e-commerce: o orquestrador de Checkout, o ciclo de vida e snapshot comercial imutável do Pedido, os pagamentos simulados com idempotência estrita, o subsistema de frete logístico com regras de frete grátis, o cancelamento com liberação atômica de estoque, o barramento de eventos desacoplado e a emissão de relatórios gerenciais analíticos.

Em conformidade com a Constituição do Projeto (Princípios I, II, III, IV, V e VI), todas as decisões arquiteturais foram fundamentadas em padrões de projeto do GoF que eliminam código procedural e estruturas condicionais rígidas (`if/else` ou `switch`), garantindo extrema extensibilidade, testabilidade automatizada e clareza documental.

Abaixo, documentamos formalmente para a banca avaliadora as respostas às três perguntas obrigatórias para cada um dos 7 padrões exigidos no Módulo 3:
1. **Qual problema de projeto existia?**
2. **Por que este padrão foi escolhido?**
3. **O que ficou mais simples de modificar ou estender?**

---

## 2. Catálogo dos 7 Padrões de Projeto do Módulo 3

### 2.1. `Facade` (Fachada) — `CheckoutFacade`

- **Problema de Projeto**: O fechamento de um pedido (checkout) exige coordenar múltiplos subsistemas de domínios distintos:
  1. Consultar e validar cliente ativo e endereço selecionado de entrega (Módulo 1);
  2. Consultar o resumo do carrinho com itens e descontos promocionais calculados e congelados (Módulo 2);
  3. Solicitar a reserva atômica de estoque contra concorrência e chave de idempotência (Módulo 1);
  4. Cotar ou validar a modalidade de frete selecionada;
  5. Processar a autorização ou registro inicial do meio de pagamento;
  6. Persistir o snapshot comercial imutável no banco;
  7. Esvaziar o carrinho do cliente (Módulo 2);
  8. Publicar eventos de domínio para notificação.
  Se o controller REST ou um serviço comum fizesse todas essas chamadas diretamente, haveria acoplamento excessivo, fragilidade transacional e forte quebra de coesão.
- **Por que foi escolhido**: O padrão `Facade` fornece uma interface unificada, simples e de alto nível (`executarCheckout(CheckoutCommandDTO comando)`), escondendo dos clientes HTTP toda a complexidade e coordenação dos subsistemas subjacentes e centralizando o isolamento transacional.
- **O que ficou mais simples de estender**: Adicionar uma nova etapa no fechamento de compra (por exemplo, análise preventiva antifraude, validação de regras de compliance ou reserva de brinde comemorativo) exige alterar apenas a orquestração interna da Facade, sem que nenhum controller ou cliente externo da API sofra impacto.

---

### 2.2. `State` (Estado) — Máquina de Estados do Ciclo de Vida do Pedido (`EstadoPedido`)

- **Problema de Projeto**: O agregado `Pedido` transita por 8 estados distintos (`CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`, `PAGAMENTO_RECUSADO`). Cada estado possui regras estritas de transição (ex.: pedidos no estado `ENVIADO` ou `ENTREGUE` não podem ser cancelados diretamente; pedidos `CANCELADO` não podem receber confirmação de pagamento). Tratar essas regras através de condicionais `switch(status)` ou múltiplos `if/else` espalhados tornaria o código propenso a erros graves de concorrência e difícil de testar.
- **Por que foi escolhido**: O padrão `State` encapsula o comportamento e as transições válidas de cada estado em classes polimórficas dedicadas (`PedidoCriadoState`, `PedidoAguardandoPagamentoState`, `PedidoPagoState`, `PedidoEmPreparacaoState`, `PedidoEnviadoState`, `PedidoEntregueState`, `PedidoCanceladoState`, `PedidoPagamentoRecusadoState`), todas implementando a interface `EstadoPedido`. Cada estado decide se a operação solicitada é legal ou lança `TransicaoEstadoInvalidaException`.
- **O que ficou mais simples de estender**: Caso a banca solicite adicionar um novo estado operacional (ex.: `DEVOLUCAO_SOLICITADA` ou `EM_ANALISE_RISCO`), basta criar uma nova classe implementando `EstadoPedido` e conectar as transições permitidas a partir dos estados adjacentes, sem modificar o código interno de dezenas de outros métodos da entidade `Pedido`.

---

### 2.3. `Adapter` e `Anti-Corruption Layer` (Camada Anticorrupção) — Gateways de Pagamento e Transportadoras

- **Problema de Projeto**: Provedores externos simulados de pagamento (Mercado Pago, Cielo, Banco Central PIX) e transportadoras de logística (Correios PAC/SEDEX, Transportadora Expressa) utilizam contratos, formatos de dados, nomes de atributos e códigos de resposta proprietários. Permitir que esses modelos externos entrem diretamente no domínio central corromperia a linguagem ubíqua e criaria dependência direta de APIs externas.
- **Por que foi escolhido**: O padrão `Adapter` atua como uma Camada Anticorrupção (ACL), implementando as interfaces do domínio (`GatewayPagamentoPort` e `CalculadoraFretePort`) e convertendo os dados externos para os objetos de valor do nosso sistema (`RespostaPagamentoDTO`, `CotacaoFreteDTO`), mantendo o núcleo do e-commerce imune a alterações em fornecedores externos.
- **O que ficou mais simples de estender**: Trocar um gateway de pagamento ou adicionar uma nova transportadora (ex.: Loggi ou Jadlog) envolve apenas escrever um novo adaptador que implemente a porta interna, sem alterar nenhuma linha das regras de checkout, pedido ou banco de dados.

---

### 2.4. `Abstract Factory` e `Service Locator` — Resolução Dinâmica de Famílias de Provedores

- **Problema de Projeto**: O sistema suporta múltiplas modalidades de pagamento (PIX, Cartão de Crédito, Boleto) e logística de entrega (PAC, SEDEX, Expressa). Cada modalidade requer um conjunto de comportamentos específicos (adaptador de cobrança, gerador de recibo, validador de payload). Instanciar essas famílias usando cadeias de `if (tipo == CARTAO) new ...` violaria o Princípio Aberto-Fechado (OCP).
- **Por que foi escolhido**: O padrão `Abstract Factory` define contratos abstratos para criação de famílias de objetos relacionados. Combinado com o padrão `Service Locator` (apoiado no Spring ApplicationContext), o sistema resolve e injeta dinamicamente em tempo de execução a fábrica e o processador apropriados a partir do tipo selecionado no comando do cliente.
- **O que ficou mais simples de estender**: Registrar uma nova modalidade de pagamento (ex.: Carteira Digital PicPay ou Criptomoeda) exige apenas adicionar uma nova classe anotada com `@Component` implementando a interface de fábrica/processador, sendo descoberta automaticamente pelo Service Locator sem nenhuma alteração no código existente.

---

### 2.5. `Template Method` e `Hook Method` — Fluxos Invariantes de Checkout e Cancelamento

- **Problema de Projeto**: Os fluxos de fechamento de compra (checkout) e cancelamento de pedido possuem uma sequência de passos algorítmicos invariantes obrigatórios que não podem ser alterados ou omitidos:
  - *No Checkout*: (1) Pré-validações de cliente e endereço -> (2) Reserva atômica de estoque -> (3) Processamento do meio de pagamento -> (4) Criação do snapshot do pedido -> (5) Esvaziamento do carrinho -> (6) Disparo de notificações.
  - *No Cancelamento*: (1) Verificação de estado cancelável -> (2) Estorno financeiro se aplicável -> (3) Devolução de estoque no Módulo 1 -> (4) Mudança de estado para `CANCELADO` -> (5) Notificação ao cliente.
  Entretanto, cada modalidade de pagamento tem particularidades na etapa de cobrança/estorno (ex.: Cartão liquida online com token; Boleto gera linha digitável; PIX gera QR Code e chave copia-e-cola).
- **Por que foi escolhido**: O padrão `Template Method` define o algoritmo fixo em um método mestre `final` na classe base abstrata (`ProcessadorCheckoutTemplate` e `FluxoCancelamentoTemplate`), enquanto os passos que variam são delegados a métodos abstratos ou métodos gancho (**Hook Methods**) implementados pelas subclasses especializadas.
- **O que ficou mais simples de estender**: Novas modalidades de pagamento reutilizam 100% da lógica crítica de consistência transacional, estoque e criação de pedidos, precisando implementar apenas o método gancho específico de comunicação com o provedor financeiro.

---

### 2.6. `Observer` (Observador) — Barramento Reativo de Eventos de Domínio

- **Problema de Projeto**: Quando um pedido transita para `PAGO` ou é `CANCELADO`, diversos módulos satélites precisam agir: envio de confirmação por e-mail ao cliente, disparo de alerta por SMS, atualização de registros contábeis de auditoria e recálculo de dashboards de vendas. Chamar todos esses serviços dentro do método `confirmarPagamento()` da entidade `Pedido` acoplaria o agregado a múltiplos serviços de infraestrutura e violaria o SRP.
- **Por que foi escolhido**: O padrão `Observer` implementa uma comunicação baseada em publicação e assinatura (Publish/Subscribe). O agregado `Pedido` publica eventos de domínio imutáveis (`PedidoCriadoEvent`, `PagamentoConfirmadoEvent`, `PedidoCanceladoEvent`, `PedidoEnviadoEvent`) em um barramento leve (`BarramentoEventosDominio` / `ApplicationEventPublisher`). Os observadores (`NotificacaoEmailListener`, `NotificacaoSmsListener`, `AuditoriaPedidoListener`) recebem as notificações de forma assíncrona e completamente desacoplada.
- **O que ficou mais simples de estender**: Adicionar um novo canal de comunicação (ex.: notificação Push no aplicativo móvel ou integração com um ERP) requer apenas criar um novo ouvinte anotado com `@EventListener`, sem tocar em nenhuma linha de código da entidade `Pedido` ou dos fluxos transacionais.

---

### 2.7. `Mediator` (Mediador) — Coordenação de Compensações e Cancelamento

- **Problema de Projeto**: No cancelamento ou falha de pagamento de um pedido, ocorre uma teia de dependências cruzadas entre quatro subsistemas: a máquina de estados do Pedido, a liberação de reserva de estoque no Módulo 1, o estorno financeiro no gateway de pagamento e as notificações ao cliente. Se esses componentes conversassem diretamente entre si, teríamos acoplamento bidirecional e referências circulares (`Pedido` chama `Estoque`, que chama `Notificacao`, que chama `Pagamento`).
- **Por que foi escolhido**: O padrão `Mediator` (`CoordenadorCancelamentoMediator`) centraliza o fluxo de compensação. Os subsistemas participantes conhecem apenas o mediador, e o mediador sabe exatamente em qual ordem executar o estorno, liberar a reserva no Módulo 1, transitar o pedido e disparar as notificações, mantendo as partes totalmente desacopladas entre si.
- **O que ficou mais simples de estender**: A lógica de compensação e recuperação de falhas fica isolada em um único componente, simplificando testes de integração, simulação de falhas parciais e auditoria transacional.

---

## 3. Resumo das Decisões de Tecnologia e Bibliotecas

| Componente / Área | Escolha Tecnológica | Justificativa Técnica |
| :--- | :--- | :--- |
| **Linguagem & Runtime** | Java 21 (LTS) | Padrão do projeto; recursos modernos de records, pattern matching e suporte LTS. |
| **Framework Base** | Spring Boot 3.4.x | Ecossistema maduro com Spring Data JPA, Spring Security, Validation e Springdoc OpenAPI. |
| **Banco de Dados** | PostgreSQL (Produção) / H2 (Testes) | Compatibilidade total com Flyway; testes em memória rápidos sem dependência externa. |
| **Migrações SQL** | Flyway (`V3__modulo3_...`) | Controle versionado do esquema de banco, garantindo repetibilidade e histórico imutável. |
| **Idempotência** | Tabela relacional `tb_registro_idempotencia_pagamento` | Chave única natural (`chave_operacao_pedido`), impedindo concorrência e repetição via constraint de banco. |
| **Documentação REST** | Springdoc OpenAPI 2 (Swagger) | Exposição interativa dos endpoints de checkout, pedidos, pagamentos, frete e relatórios em `/swagger-ui.html`. |
| **Testes Automatizados** | JUnit 5 + Mockito + MockMvc | Suíte abrangente de testes unitários de cada padrão GoF e testes de integração de ponta a ponta. |
