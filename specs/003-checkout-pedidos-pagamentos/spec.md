# Feature Specification: Módulo 3 — Checkout, Ciclo de Vida de Pedidos, Pagamentos Idempotentes, Frete, Notificações e Relatórios Gerenciais

**Feature Branch**: `003-checkout-pedidos-pagamentos`  
**Created**: 2026-11-11  
**Status**: Draft  
**Integrante 3**: Henrique Sandrini  
**Input**: "Módulo 3 (Integrante 3 — Henrique Sandrini): Checkout, Ciclo de Vida de Pedidos, Pagamentos Idempotentes, Frete, Eventos e Notificações, Cancelamento com Liberação de Estoque e Relatórios Gerenciais. O módulo deve contemplar: (1) Processo de checkout orquestrado com Facade e Template Method; (2) Snapshot comercial imutável no Pedido com itens, preços, descontos, frete e endereço de entrega; (3) Máquina de estados do ciclo de vida do pedido com padrão State; (4) Pagamentos simulados (Cartão de Crédito, PIX, Boleto) com garantia de idempotência estrita; (5) Cálculo de frete e prazos com múltiplas transportadoras e frete grátis via Adapter; (6) Cancelamento e estorno com liberação atômica de estoque via Mediator e Template Method; (7) Barramento de eventos de domínio com Observer e notificações desacopladas; (8) Relatórios gerenciais de faturamento, ticket médio e descontos por campanha."

---

## Clarifications

### Session 2026-11-11

- Q: Como o Módulo 3 consome os dados do Módulo 1 e Módulo 2 sem quebrar o desacoplamento arquitetural? → A: O Módulo 3 consome estritamente as interfaces Java expostas: `PortaConsultaCatalogoCliente` e `PortaComandoReservaEstoque` (Módulo 1) e `PortaConsultaCarrinhoPromocoes` (Módulo 2), sem importar nem acoplar entidades JPA internas de outros módulos.
- Q: Como é garantida a idempotência das confirmações de pagamento? → A: Toda confirmação de pagamento enviada por webhook ou chamada externa exige um identificador único (`chaveOperacaoPedido` ou `idTransacaoGateway`). O sistema registra a transação na tabela de idempotência; chamadas subsequentes com a mesma chave retornam sucesso imediatamente sem reprocessar baixa de estoque, sem emitir novos eventos e sem alterar estados adicionais.
- Q: Como o cancelamento de um pedido lida com a devolução do estoque? → A: Ao cancelar um pedido em estado elegível (`CRIADO` ou `AGUARDANDO_PAGAMENTO` com reserva ativa, ou `PAGO` antes do despacho), o fluxo orquestrado pelo `Mediator` aciona o gateway para estorno e invoca `PortaComandoReservaEstoque.liberarReservaPorCancelamentoOuRecusa(...)` no Módulo 1, garantindo consistência transacional.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Orquestração do Checkout e Snapshot Comercial Imutável (Priority: P1) 🎯 MVP

Um cliente autenticado com itens em seu carrinho decide finalizar a compra. O orquestrador de checkout (`CheckoutFacade`) valida o cliente e seu endereço selecionado de entrega no Módulo 1, obtém o resumo do carrinho com os preços e descontos promocionais congelados no Módulo 2, calcula o valor do frete e solicita a reserva atômica dos itens no estoque (Módulo 1). Em seguida, cria a entidade `Pedido` contendo um **snapshot comercial imutável** (valores dos produtos, descontos discriminados, custo do frete, endereço e forma de pagamento), esvazia o carrinho no Módulo 2 e transita o pedido para o estado inicial `AGUARDANDO_PAGAMENTO`.

**Why this priority**: É a espinha dorsal do e-commerce transacional. Sem o checkout orquestrado e a geração do pedido com snapshot imutável, o cliente não consegue converter sua intenção de compra em uma transação comercial auditável e imune a alterações futuras no catálogo.

**Independent Test**: Pode ser testada submetendo uma requisição `POST /api/v1/checkout` para um cliente com itens no carrinho e endereço selecionado. O sistema retorna o pedido criado com ID único, número legível, subtotal, frete, total líquido, linhas de itens detalhadas com cópia dos preços vigentes e status `AGUARDANDO_PAGAMENTO`, esvaziando o carrinho.

**Acceptance Scenarios**:

1. **Given** um cliente autenticado com endereço selecionado e um carrinho contendo itens promocionais totalizando R$ 490,00 líquidos, **When** executa o checkout selecionando frete expresso de R$ 30,00 e método de pagamento `PIX`, **Then** o pedido é criado com valor final de R$ 520,00, o estoque é reservado no Módulo 1, o carrinho é esvaziado no Módulo 2 e o status é `AGUARDANDO_PAGAMENTO`.
2. **Given** um cliente que tenta fechar o checkout sem ter nenhum endereço de entrega selecionado (retornando `EnderecoAusenteNullObject` do Módulo 1), **When** a solicitação é processada, **Then** o checkout é rejeitado com status HTTP 400 informando a ausência de endereço de entrega válido.
3. **Given** um carrinho cujos itens excedem o estoque disponível no momento da reserva (falha na `PortaComandoReservaEstoque`), **When** o checkout é submetido, **Then** o pedido não é persistido, nenhuma cobrança é iniciada e o erro de estoque insuficiente é retornado de forma amigável ao cliente.

---

### User Story 2 - Máquina de Estados do Ciclo de Vida do Pedido com Padrão State (Priority: P1)

O ciclo de vida do pedido é gerenciado por uma máquina de estados orientada a objetos (`EstadoPedido`). Os estados válidos são:
- `CRIADO`: Pedido registrado aguardando inicialização do pagamento.
- `AGUARDANDO_PAGAMENTO`: Aguardando confirmação do gateway/webhook (PIX, Boleto ou análise de cartão).
- `PAGO`: Pagamento confirmado com sucesso, estoque com baixa definitiva confirmada.
- `EM_PREPARACAO`: Pedido sendo separado e embalado no centro de distribuição.
- `ENVIADO`: Pedido despachado para a transportadora com código de rastreio.
- `ENTREGUE`: Pedido entregue ao destinatário final (estado terminal com sucesso).
- `CANCELADO`: Pedido cancelado (com liberação de estoque e estorno financeiro se aplicável).
- `PAGAMENTO_RECUSADO`: Pagamento recusado pelo gateway com liberação imediata de estoque.

Tentativas de realizar transições inválidas (ex.: tentar pagar um pedido `CANCELADO` ou cancelar um pedido `ENVIADO`) são bloqueadas imediatamente pelo próprio objeto de estado, lançando `TransicaoEstadoInvalidaException`.

**Why this priority**: Garante conformidade com o Princípio II e IV da Constituição, impedindo inconsistências graves no ciclo de vida operacional da compra.

**Independent Test**: Pode ser testada instanciando um pedido e executando transições válidas e inválidas via testes automatizados e endpoints de transição administrativa, verificando que transições ilegais retornam erro HTTP 400 sem alterar o banco de dados.

**Acceptance Scenarios**:

1. **Given** um pedido no estado `AGUARDANDO_PAGAMENTO`, **When** recebe o evento de confirmação de pagamento, **Then** transita com sucesso para `PAGO`.
2. **Given** um pedido no estado `PAGO`, **When** a expedição inicia a separação, **Then** transita para `EM_PREPARACAO`, e posteriormente para `ENVIADO`.
3. **Given** um pedido que já se encontra no estado `ENVIADO` ou `ENTREGUE`, **When** há uma tentativa de cancelamento direto, **Then** a operação é bloqueada lançando `TransicaoEstadoInvalidaException`.

---

### User Story 3 - Processamento de Pagamento Simulado com Idempotência Estrita (Priority: P1)

O sistema suporta pagamentos simulados via **PIX**, **Cartão de Crédito** e **Boleto Bancário**. As integrações utilizam o padrão **Adapter** como camada anticorrupção para simular o comportamento de gateways externos e o padrão **Template Method** para garantir a ordem invariante de validação, cobrança e confirmação.
Além disso, toda confirmação de pagamento processada pelo webhook (`POST /api/v1/pagamentos/confirmacao`) DEVE ser estritamente **idempotente**: a submissão repetida da mesma chave de operação ou identificador de transação retorna a resposta de confirmação sem duplicar baixa de estoque, sem disparar eventos repetidos e sem alterar o status do pedido novamente.

**Why this priority**: Requisito mandatório do Princípio IV e VI da Constituição (garantia de consistência financeira e proteção contra webhooks duplicados em ambiente distribuído).

**Independent Test**: Pode ser testada enviando duas requisições idênticas de confirmação de pagamento com a mesma `chaveOperacaoPedido`. A primeira aprova o pedido e confirma a baixa no Módulo 1; a segunda responde com sucesso indicando processamento prévio sem executar nenhuma operação colateral no estoque ou no pedido.

**Acceptance Scenarios**:

1. **Given** um pedido aguardando pagamento com chave `PED-123456`, **When** o webhook envia a aprovação de pagamento pela primeira vez, **Then** o pedido transita para `PAGO`, o Módulo 1 é acionado para confirmar baixa definitiva de estoque, o cupom é consumido definitivamente no Módulo 2 e o evento `PagamentoConfirmadoEvent` é publicado.
2. **Given** a confirmação de pagamento do pedido `PED-123456` já processada, **When** o webhook reenvia a mesma mensagem com a mesma chave, **Then** o sistema retorna `200 OK` informando confirmação já registrada sem reenviar comando de estoque nem duplicar notificações.
3. **Given** um pagamento via cartão de crédito com dados simulados inválidos ou recusa de saldo, **When** o processamento é executado, **Then** o pedido transita para `PAGAMENTO_RECUSADO` e a reserva de estoque é liberada imediatamente via `PortaComandoReservaEstoque`.

---

### User Story 4 - Cálculo Logístico de Frete, Transportadoras e Política de Frete Grátis (Priority: P2)

O cliente pode cotar opções de frete antes ou durante o checkout (`POST /api/v1/frete/calcular`). O subsistema de frete oferece no mínimo 3 modalidades simuladas através de adaptadores (`Adapter`):
- **Correios PAC Simulado**: Menor custo, prazo maior de entrega.
- **Correios SEDEX Simulado**: Custo intermediário, prazo rápido.
- **Transportadora Privada Expressa**: Entrega expressa com seguro de carga.

O cálculo leva em conta o peso total agregado dos itens (calculado pelo carrinho no Módulo 2) e o CEP de destino. O sistema aplica a regra de **Frete Grátis** caso o subtotal líquido de produtos atinja a meta mínima configurada (ex.: R$ 250,00) e o peso não ultrapasse o limite regulamentado (ex.: 5 kg), zerando o valor da modalidade econômica.

**Why this priority**: Proporciona transparência na precificação final do frete e flexibilidade na escolha da transportadora pelo cliente.

**Independent Test**: Pode ser testada enviando simulações de frete para CEPs de diferentes regiões do Brasil com pesos variados, verificando os prazos e valores retornados, bem como a aplicação correta da isenção de frete para carrinhos qualificados.

**Acceptance Scenarios**:

1. **Given** um carrinho com peso de 1.200g e CEP de São Paulo (`01310-100`), **When** calcula o frete, **Then** são retornadas as 3 opções (PAC, SEDEX e Transportadora) com seus respectivos valores e prazos em dias úteis.
2. **Given** um carrinho com total líquido de R$ 350,00 (acima da meta de R$ 250,00) e peso de 2 kg, **When** calcula o frete, **Then** a opção econômica é exibida com valor R$ 0,00 sob a indicação de benefício de Frete Grátis.

---

### User Story 5 - Cancelamento de Pedido, Estorno e Liberação Automática de Estoque (Priority: P2)

Um cliente ou administrador pode solicitar o cancelamento de um pedido (`DELETE /api/v1/pedidos/{id}/cancelamento`).
A solicitação é processada pelo `ServicoCancelamentoPedido` utilizando o padrão `Template Method` e `Mediator`:
1. Valida se o estado atual do pedido permite cancelamento (`CRIADO`, `AGUARDANDO_PAGAMENTO` ou `PAGO` antes do envio).
2. Se o pedido já tiver sido pago, aciona o `Adapter` do gateway para solicitar estorno financeiro simulado.
3. Aciona `PortaComandoReservaEstoque.liberarReservaPorCancelamentoOuRecusa(...)` no Módulo 1 para devolver os itens ao saldo disponível do catálogo.
4. Atualiza o estado do pedido para `CANCELADO` registrando o motivo e timestamp.
5. Publica o evento `PedidoCanceladoEvent` para notificar o cliente e registrar auditoria.

**Why this priority**: Requisito crítico de consistência transacional e governança pós-venda da loja.

**Independent Test**: Cancelar um pedido aguardando pagamento e verificar que o status passa para `CANCELADO` e que o estoque correspondente é liberado imediatamente no Módulo 1.

**Acceptance Scenarios**:

1. **Given** um pedido no estado `AGUARDANDO_PAGAMENTO`, **When** o cliente solicita cancelamento, **Then** o pedido passa para `CANCELADO` e a reserva de estoque é liberada sem cobrança.
2. **Given** um pedido no estado `PAGO`, **When** o administrador solicita o cancelamento antes do envio, **Then** o estorno é simulado com sucesso, o estoque é devolvido e o status passa para `CANCELADO`.
3. **Given** um pedido já com status `ENVIADO`, **When** há solicitação de cancelamento direto via API, **Then** a operação é rejeitada informando que pedidos em transporte exigem processo de devolução pós-entrega.

---

### User Story 6 - Barramento de Eventos de Domínio e Notificações Multicanal (Priority: P3)

O sistema conta com um barramento de eventos desacoplado aplicando o padrão **Observer**. Sempre que ocorrem marcos críticos no ciclo de vida do pedido:
- `PedidoCriadoEvent`: Notifica o cliente com resumo da compra e instruções de pagamento.
- `PagamentoConfirmadoEvent`: Notifica o cliente sobre o sucesso do pagamento e aciona a expedição.
- `PedidoEnviadoEvent`: Envia código de rastreio da transportadora.
- `PedidoCanceladoEvent`: Notifica o cliente sobre o cancelamento e eventuais dados de estorno.

Múltiplos listeners reagem de forma desacoplada: `NotificacaoEmailListener`, `NotificacaoSmsListener` e `AuditoriaPedidoListener`.

**Why this priority**: Promove o Princípio Aberto-Fechado (OCP), permitindo adicionar novos canais de comunicação sem alterar a lógica do pedido.

**Independent Test**: Disparar a transição de um pedido e verificar através dos logs e dos componentes mockados que todos os listeners registrados foram notificados com os dados corretos do evento.

---

### User Story 7 - Relatórios Gerenciais e Inteligência de Vendas (Priority: P3)

Perfis com papel `ADMIN` têm acesso a endpoints analíticos gerenciais (`/api/v1/admin/relatorios`):
- **Vendas Consolidadas**: Total de pedidos faturados, faturamento bruto, total de descontos promocionais concedidos e faturamento líquido.
- **Ticket Médio**: Valor médio gasto por cliente em compras concluídas.
- **Top Produtos Mais Vendidos**: Lista dos SKUs mais comercializados por quantidade e receita.
- **Cancelamentos**: Taxa de cancelamentos e motivos mais frequentes.

**Why this priority**: Fornece visibilidade executiva e demonstra a integridade analítica das tabelas de pedidos e itens.

**Independent Test**: Executar uma série de pedidos e consultar os endpoints de relatório, verificando a consistência aritmética dos números agregados.

---

## Edge Cases

- **Timeout ou falha temporária no gateway de pagamento**: O pedido permanece em `AGUARDANDO_PAGAMENTO` até o limite do TTL da reserva de estoque; expirado o tempo sem confirmação, a reserva é liberada por expiração.
- **Confirmação de pagamento recebida em duplicidade**: O mecanismo de idempotência identifica a chave existente e responde com `200 OK` sem executar ações duplicadas.
- **Cliente sem endereço cadastrado ou selecionado**: O checkout é abortado logo no passo 1 do template, antes de criar reserva ou transação financeira.
- **Tentativa de cancelamento de pedido em trânsito**: A máquina de estados bloqueia a transição e instrui sobre o fluxo de devolução.
- **Concorrência entre cancelamento do cliente e aprovação do gateway**: A transação do banco com isolamento e controle de estado serializa os eventos; se for aprovado primeiro, o cancelamento trata como pedido pago (com estorno); se cancelado primeiro, o webhook subsequente rejeita a confirmação.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE fornecer o endpoint `POST /api/v1/checkout` orquestrado via `CheckoutFacade` para fechamento de compra de clientes autenticados.
- **FR-002**: O sistema DEVE validar o endereço selecionado do cliente através da `PortaConsultaCatalogoCliente` (Módulo 1) antes de prosseguir com o pedido.
- **FR-003**: O sistema DEVE consumir o resumo do carrinho via `PortaConsultaCarrinhoPromocoes` (Módulo 2), esvaziar o carrinho após a criação do pedido e confirmar o consumo do cupom após o pagamento.
- **FR-004**: O sistema DEVE realizar a reserva atômica de estoque através da `PortaComandoReservaEstoque` (Módulo 1) com chave única idempotente de pedido.
- **FR-005**: O agregado `Pedido` DEVE persistir um snapshot comercial imutável contendo cópia dos preços dos itens, descontos concedidos, frete, endereço de entrega e método de pagamento.
- **FR-006**: O ciclo de vida do pedido DEVE ser controlado pelo padrão `State`, restringindo transições aos estados válidos (`CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`, `PAGAMENTO_RECUSADO`).
- **FR-007**: Qualquer tentativa de transição ilegal de estado DEVE lançar `TransicaoEstadoInvalidaException` e retornar HTTP 400 Bad Request.
- **FR-008**: O sistema DEVE fornecer o endpoint `POST /api/v1/pagamentos/confirmacao` com suporte estrito a idempotência através da tabela `tb_registro_idempotencia_pagamento`.
- **FR-009**: O sistema DEVE suportar pagamentos simulados para PIX, Cartão de Crédito e Boleto através do padrão `Adapter` e `Anti-Corruption Layer`.
- **FR-010**: O processamento do checkout e do cancelamento DEVE seguir o esqueleto algorítmico do padrão `Template Method` e `Hook Method`.
- **FR-011**: O sistema DEVE fornecer o endpoint `POST /api/v1/frete/calcular` retornando cotações de pelo menos 3 modalidades (PAC, SEDEX, Transportadora Expressa) com prazo e valor.
- **FR-012**: O sistema DEVE aplicar automaticamente a política de Frete Grátis na modalidade econômica para pedidos que atendam aos requisitos de valor mínimo e peso.
- **FR-013**: O cancelamento de pedidos DEVE liberar automaticamente a reserva de estoque no Módulo 1 via `PortaComandoReservaEstoque.liberarReservaPorCancelamentoOuRecusa`.
- **FR-014**: O sistema DEVE publicar eventos de domínio desacoplados via padrão `Observer` para criação de pedido, confirmação de pagamento e cancelamento.
- **FR-015**: O sistema DEVE disponibilizar endpoints analíticos para o perfil `ADMIN` em `/api/v1/admin/relatorios` para faturamento consolidado, ticket médio e produtos mais vendidos.

---

### Key Entities

- **Pedido**: Agregado raiz da compra contendo número identificador, clienteId, endereçoSnapshot, snapshotFinanceiro (subtotal, desconto, frete, total), metodoPagamento, dataCriacao e referencia ao `EstadoPedido`.
- **ItemPedidoSnapshot**: Linha imutável do pedido registrando SKU, nome do produto/combo, quantidade, preço unitário de tabela, desconto unitário rateado e subtotal líquido.
- **TransacaoPagamento**: Registro da tentativa ou liquidação financeira contendo identificador da transação, chaveIdempotencia, método, valor, status e timestamp.
- **CotacaoFrete**: Objeto de valor contendo identificador da transportadora, nome da modalidade, valor calculado, prazo estimado em dias úteis e indicador de frete grátis.
- **RegistroIdempotenciaPagamento**: Tabela de bloqueio de concorrência e idempotência armazenando a chave única da requisição, payload de resposta e data de processamento.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O endpoint de checkout processa e cria o pedido com reserva de estoque em menos de 800ms em ambiente de teste.
- **SC-002**: 100% das requisições duplicadas de confirmação de pagamento são tratadas com idempotência, sem causar duplicidade de estoque ou múltiplos eventos.
- **SC-003**: 100% das transições ilegais de estado do pedido são bloqueadas com exceções explícitas de domínio.
- **SC-004**: 100% dos métodos de todas as classes do Módulo 3 possuem Javadoc explícito documentando objetivo, papel no Design Pattern, parâmetros e exceções (Constituição, Princípio III).
- **SC-005**: A suíte de testes automatizados JUnit 5 cobre todos os fluxos críticos (checkout, máquina de estados, idempotência, frete grátis, cancelamento e liberação de estoque) sem falhas.

---

## Assumptions

- O ambiente de testes utiliza banco H2 em memória e PostgreSQL para produção via migração Flyway `V3__modulo3_checkout_pedidos_pagamentos_frete.sql`.
- Os gateways externos de pagamento e provedores de frete são simulados com fidelidade através de adaptadores e geradores de dados determinísticos.
- Os usuários já estão autenticados pelo Spring Security implementado no Módulo 1, fornecendo o `clienteId` seguro no token/sessão.
