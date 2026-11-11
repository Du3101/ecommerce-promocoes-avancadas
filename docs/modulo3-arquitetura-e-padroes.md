# Documentação Arquitetural e Defesa de Padrões de Projeto — Módulo 3

**Disciplina**: Engenharia de Software / Padrões de Projeto (FATEC)  
**Projeto**: Sistema de E-commerce com Promoções Avançadas  
**Módulo 3 (Integrante 3)**: Checkout, Ciclo de Vida de Pedidos, Pagamentos Idempotentes, Frete, Notificações e Relatórios Gerenciais (`003-checkout-pedidos-pagamentos`)  
**Autor**: Henrique Sandrini  

---

## 1. Visão Geral da Arquitetura do Módulo 3

O **Módulo 3** constitui o núcleo transacional de finalização de compras, ciclo de vida de pedidos, pagamentos simulados com idempotência estrita, logística de entrega e inteligência de relatórios gerenciais, estruturado em seis subdomínios coesos:

1. **`checkout`**: Orquestrador central do fluxo de fechamento de compra via `CheckoutFacade` (`Facade`) e `ProcessadorCheckoutTemplate` (`Template Method` + `Hook Method`), consumindo o resumo do carrinho consolidado pelo Módulo 2 (`PortaConsultaCarrinhoPromocoes`), validando clientes e endereços no Módulo 1 (`PortaConsultaCatalogoCliente`), efetuando a reserva atômica de estoque (`PortaComandoReservaEstoque`) e persistindo o snapshot comercial imutável.
2. **`pedido`**: Agregado raiz `Pedido` e suas linhas `ItemPedidoSnapshot`, com controle estrito de ciclo de vida através de máquina de estados orientada a objetos (`EstadoPedido`, com estados concretos `PedidoCriadoState`, `PedidoAguardandoPagamentoState`, `PedidoPagoState`, `PedidoEmPreparacaoState`, `PedidoEnviadoState`, `PedidoEntregueState`, `PedidoCanceladoState` e `PedidoPagamentoRecusadoState`) implementando o padrão `State`.
3. **`pagamento`**: Camada anticorrupção (`Anti-Corruption Layer`) e adaptadores (`Adapter`) para gateways simulados de pagamento (`PixGatewayAdapter`, `CartaoCreditoGatewayAdapter`, `BoletoGatewayAdapter`), fábrica dinâmica com injeção de dependências (`Abstract Factory` / `Service Locator`) e controle de transações idempotentes com tabela de chaves de idempotência (`RegistroIdempotenciaPagamento`).
4. **`frete`**: Subsistema de cálculo logístico e prazos de entrega com múltiplas modalidades simuladas (Sedex, PAC e Transportadora Privada Expressa) integradas via `CalculadoraFreteAdapter` (`Adapter`), aplicando regras de frete grátis baseadas em peso e subtotal líquido.
5. **`eventos` & `notificacao`**: Barramento reativo de eventos de domínio desacoplado via padrão `Observer` (`PedidoCriadoEvent`, `PagamentoConfirmadoEvent`, `PedidoCanceladoEvent`, `PedidoEnviadoEvent`), com múltiplos ouvintes independentes (`NotificacaoEmailListener`, `NotificacaoSmsListener`, `AuditoriaPedidoListener`).
6. **`relatorios`**: Serviços de inteligência gerencial e consultas analíticas consolidadas (faturamento total líquido, volume de pedidos, ticket médio, produtos e variações mais vendidos e impacto financeiro de descontos concedidos por campanha promocional).

---

## 2. Catálogo dos 7 Padrões de Projeto Aplicados (Defesa Acadêmica)

Para cada padrão aplicado no Módulo 3, respondemos com rigor acadêmico às três perguntas obrigatórias da banca avaliadora:
1. **Qual problema de projeto existia?**
2. **Por que este padrão foi escolhido?**
3. **O que ficou mais simples de modificar ou estender?**

### 2.1. `Facade` — `CheckoutFacade`
- **Problema**: O fechamento de um pedido envolve múltiplos subsistemas heterogêneos: catálogo, carrinho congelado, reserva atômica de estoque, cálculo de frete, pagamento e criação de pedido. Expor esses serviços diretamente aos controllers REST geraria alto acoplamento e duplicaria lógica orquestradora.
- **Escolha**: Centraliza a orquestração em uma interface simplificada e coesa (`executarCheckout(...)`), escondendo a complexidade interna dos subsistemas e garantindo integridade transacional.
- **Extensibilidade**: Incluir etapas adicionais no checkout (ex.: verificação antifraude ou consulta de score de crédito) altera apenas a implementação da Facade, sem impacto nos clientes HTTP ou nos outros módulos.

### 2.2. `State` — Ciclo de Vida do Pedido (`EstadoPedido`)
- **Problema**: O pedido transita por 8 estados distintos no seu ciclo de vida. Cada estado restringe rigorosamente operações permitidas (ex.: não é permitido cancelar pedido já despachado, nem confirmar pagamento de pedido cancelado). Controlar isso com `switch(status)` ou múltiplos `if/else` espalhados causaria fragilidade e risco de inconsistência.
- **Escolha**: Cada estado é uma classe polimórfica que implementa `EstadoPedido`, encapsulando as regras de transição permitidas e rejeitando operações ilegais com exceções de domínio explícitas (`TransicaoEstadoInvalidaException`).
- **Extensibilidade**: Adicionar um novo estado (ex.: `DEVOLUCAO_SOLICITADA`) requer criar uma nova classe e ajustar somente os estados adjacentes que transitam para ela.

### 2.3. `Adapter` e `Anti-Corruption Layer` (ACL) — `GatewayPagamentoAdapter` e `CalculadoraFreteAdapter`
- **Problema**: Provedores externos simulados de pagamento (PIX, Cartão, Boleto) e logística (Correios, Transportadora) possuem contratos, payloads e padrões proprietários que não devem poluir o domínio interno do e-commerce.
- **Escolha**: Os adaptadores implementam as interfaces internas (`GatewayPagamentoPort` e `CalculadoraFretePort`), traduzindo estruturas proprietárias para os objetos e Value Objects do sistema.
- **Extensibilidade**: Trocar ou adicionar uma nova transportadora (ex.: Jadlog) exige apenas implementar um novo Adapter, sem alterar entidades de pedido nem fluxos de checkout.

### 2.4. `Abstract Factory` e `Service Locator` — Resolução de Famílias de Provedores
- **Problema**: Cada meio de pagamento e provedor logístico requer uma família de objetos relacionados (adaptador de comunicação, validador de payload e formatador de recibo).
- **Escolha**: A Abstract Factory e o Service Locator baseados no Spring resolvem dinamicamente a família adequada de serviços em tempo de execução com base no enum ou identificador selecionado pelo cliente.
- **Extensibilidade**: Criar um novo método de pagamento (ex.: Carteira Digital) requer apenas adicionar sua fábrica registrada no Spring.

### 2.5. `Template Method` e `Hook Method` — `ProcessadorCheckoutTemplate` e `FluxoCancelamentoTemplate`
- **Problema**: O esqueleto da execução de checkout e cancelamento possui passos algorítmicos invariantes inegociáveis (validar -> reservar estoque -> cobrar -> snapshot -> notificar), mas pagamentos diferentes exigem customizações (ex.: Cartão cobra online; Boleto apenas gera linha digitável).
- **Escolha**: O método template define o fluxo mestre imutável (`final`), delegando etapas variáveis para métodos abstratos ou métodos gancho (`hook methods`) nas subclasses.
- **Extensibilidade**: Uma nova modalidade de pagamento reutiliza toda a lógica de estoque e persistência, implementando apenas os hooks de autorização.

### 2.6. `Observer` — Barramento de Eventos de Domínio (`BarramentoEventosDominio`)
- **Problema**: A confirmação de pagamento ou cancelamento de pedido deve disparar múltiplos efeitos colaterais (e-mail, SMS, auditoria, estoque). Acoplar todos esses disparos dentro do método de transição do pedido violaria o princípio da responsabilidade única.
- **Escolha**: O `Pedido` ou o orquestrador publica eventos de domínio (`PedidoCriadoEvent`, `PagamentoConfirmadoEvent`, `PedidoCanceladoEvent`). Múltiplos ouvintes independentes reagem de forma desacoplada.
- **Extensibilidade**: Incluir um novo canal de notificação (ex.: WhatsApp ou Push Notification) requer apenas criar um novo ouvinte anotado com `@EventListener`.

### 2.7. `Mediator` — Coordenador de Interações entre Subsistemas
- **Problema**: Durante o fluxo de cancelamento ou estorno, estoque, gateway de pagamento, ciclo de vida do pedido e notificações precisam se comunicar em cadeia, gerando dependências circulares caso interagissem diretamente.
- **Escolha**: O `Mediator` centraliza e coordena a interação entre esses subsistemas, orquestrando as chamadas de compensação financeira e liberação de estoque de maneira transparente.
- **Extensibilidade**: Ajustes na ordem das operações de cancelamento e compensação ficam concentrados em uma única classe mediadora.

---

## 3. Contratos de Integração com os Módulos 1 e 2

O Módulo 3 atua como consumidor de fronteira das portas Java expostas:

```text
[Módulo 1: Identidade, Catálogo e Estoque]
   ├── PortaConsultaCatalogoCliente (validação de cliente e endereço ativo)
   └── PortaComandoReservaEstoque (reserva atômica, confirmação de baixa e liberação)
                               ▲
                               │
                [Módulo 3: Checkout e Pedidos]
                               │
                               ▼
[Módulo 2: Carrinho, Promoções e Cupons]
   └── PortaConsultaCarrinhoPromocoes (resumo com preços congelados, esvaziamento e consumo de cupom)
```

---

## 4. Estrutura de Pacotes do Módulo 3

```text
src/main/java/br/edu/fatec/ecommerce/modulo3/
├── checkout/
│   ├── api/                  # CheckoutController (REST POST /api/v1/checkout)
│   ├── aplicacao/            # CheckoutFacade, ProcessadorCheckoutTemplate, CheckoutCommandDTO
│   └── dominio/              # ValidadorCheckout, SnapshotCheckoutVO
├── pedido/
│   ├── api/                  # PedidoConsultaController, PedidoCancelamentoController
│   ├── aplicacao/            # ServicoPedido, ServicoCancelamentoPedido
│   ├── dominio/
│   │   ├── Pedido.java       # Agregado raiz com snapshot imutável
│   │   ├── ItemPedido.java   # Linhas com preços congelados por item
│   │   └── estado/           # EstadoPedido (Interface State) + 8 Estados Concretos
│   └── infraestrutura/       # PedidoJpaRepository
├── pagamento/
│   ├── api/                  # WebhookPagamentoController (POST /api/v1/pagamentos/confirmacao)
│   ├── aplicacao/            # ServicoProcessamentoPagamento, ServicoIdempotenciaPagamento
│   ├── dominio/              # MetodoPagamento (Enum), TransacaoPagamento, RegistroIdempotencia
│   └── adaptadores/          # GatewayPagamentoAdapter, PixAdapter, CartaoAdapter, BoletoAdapter
├── frete/
│   ├── api/                  # FreteConsultaController (POST /api/v1/frete/calcular)
│   ├── aplicacao/            # ServicoCalculoFrete, PoliticaFreteGratis
│   └── adaptadores/          # CalculadoraFreteAdapter, CorreiosSimuladoAdapter, TransportadoraAdapter
├── eventos/                  # PedidoCriadoEvent, PagamentoConfirmadoEvent, PedidoCanceladoEvent
├── notificacao/              # NotificacaoEmailListener, NotificacaoSmsListener
└── relatorios/
    ├── api/                  # RelatoriosAdminController (GET /api/v1/admin/relatorios/...)
    └── aplicacao/            # ServicoRelatoriosGerenciais
```
