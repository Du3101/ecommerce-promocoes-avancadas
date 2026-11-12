# Passagem de sessão — Módulo 3 (retomar daqui)

**Última atualização**: 2026-11-12 · **Branch**: `003-checkout-pedidos-pagamentos`
**Estado do build**: `mvnw compile` OK. A suíte completa de testes **não** foi rodada depois das mudanças abaixo
(só o `IntegracaoModulo2Test`, que passou com 4/4 antes das alterações de portas).

> Leia este arquivo inteiro antes de continuar. Depois de terminar cada bloco, atualize as seções
> "Feito" / "Pendente" e marque as tasks em [tasks.md](./tasks.md).

---

## 1. Pedido do usuário (Henrique)

1. Implementar todas as tasks pendentes do Módulo 3 ([tasks.md](./tasks.md)).
2. **Decisão aprovada**: estender as portas dos Módulos 1 e 2 (em vez de contornar dentro do Módulo 3) e
   ratear o desconto por item no próprio Módulo 3.
3. Manter **sempre atualizado e detalhado** o guia de estudo/apresentação, que fica **fora do repositório**:
   `C:\Users\Henriqueee sigma\Documents\ProgramingProjects\modulo3-apresentacao-henrique.md`
   (é a partir dele que o Henrique vai estudar e apresentar para a banca). **Ainda não foi atualizado** com
   nada desta sessão — ver seção 5.

## 2. Bugs encontrados no commit `26d2c4a` (Antigravity)

- **Aplicação inteira não subia**: `endereco_uf CHAR(2)` na V3 vs `VARCHAR` na entidade → Hibernate
  `ddl-auto: validate` falhava e TODOS os testes dos Módulos 1 e 2 quebravam. **Corrigido** (V3 virou `VARCHAR(2)`
  e os `@Embeddable` ganharam `@Column` explícitos). Como a V3 nunca foi para a `main`, ela está sendo editada
  diretamente (não criar V4 enquanto a branch não for mergeada).
- `tasks.md` só marcava T001–T006, mas o código tinha mais coisas (DTOs, State, adapters, frete, eventos).

## 3. Feito nesta sessão

### Integração com outros módulos (decisão aprovada)
| Onde | O quê |
|---|---|
| `modulo1/integracao/PortaConsultaCatalogoCliente` | + `resolverClienteIdPorEmailUsuario(email)` e `consultarEnderecoEntregaSelecionado(clienteId)` → `Optional<VisaoEnderecoEntregaDTO>` (novo record) |
| `modulo1/integracao/PortaComandoReservaEstoque` | + `estornarBaixaDefinitivaPorCancelamento(chave, motivo)` — necessário porque `liberarReserva...` lança exceção para reserva `CONFIRMADA`, então cancelar pedido **PAGO** não devolvia estoque |
| `modulo1/estoque` | novo estado `ESTORNADA_POS_BAIXA`; `ReservaEstoque.estornarBaixaDefinitiva()` (idempotente); `ServicoReservaEstoqueConcorrente.estornarBaixaDefinitivaPorCancelamento(...)` (lock ordenado + movimentação de entrada) |
| `modulo1/integracao/PortaRegistroAuditoria` (nova) + `AdaptadorAuditoriaModulo1` | auditoria de cancelamento de pedido (Princípio V). Usa o tipo já existente `CANCELAMENTO_PEDIDO_ADMIN` e a nova factory `RegistroAuditoria.registrarCancelamentoPedido(...)` |
| `modulo2/integracao/PortaConsultaCarrinhoPromocoes` | + `resolverIdentificadorCarrinhoPorEmail(email)` (o carrinho é indexado por UUID **derivado do e-mail**, diferente do `Cliente.id` real do Módulo 1) |
| `modulo2/integracao/ResumoCarrinhoCheckoutDTO` | + campo `itens` (`List<ItemResumoCarrinhoDTO>`: sku, nome, tipoItem, precoUnitario, quantidade, peso) e `estaVazio()` |

### Módulo 3
- **State** reescrito com Javadoc em todos os métodos; nova transição `aguardarPagamento` (antes o `Pedido`
  mudava para AGUARDANDO_PAGAMENTO sem passar pelo State) e consulta polimórfica `pagamentoJaCapturado()`
  (o Mediator usa isso em vez de `switch(status)`). `PedidoEnviadoState.cancelar` lança mensagem específica de devolução.
- **Pedido** reescrito: **Builder** (`Pedido.builder()...construir()` com validação), novos campos
  `emailCliente`, `nomeCliente`, `cupomUtilizado`, `promocoesAplicadas`; `modalidadeFrete` virou enum
  `ModalidadeFrete`; `pertenceAoCliente(uuid)`; Javadoc em todos os getters.
- **Migração V3**: + colunas `email_cliente`, `nome_cliente`, `cupom_utilizado`, `promocoes_aplicadas`.

## 4. Pendente — desenho já decidido (seguir este plano)

Ordem sugerida: 4.1 → 4.2 → 4.3 → 4.4 → 4.5 → 4.6 → 4.7 → 4.8 → testes → 4.9.
Regras do projeto: Javadoc em **todo** método (objetivo, papel no pattern, params/retorno/exceções) e
comentários numerados nos passos de algoritmo (ver constituição em `.specify/memory/constitution.md`).

### 4.1 Observer (T034/T035)
- Converter eventos para `record` implementando `EventoDominio` (`eventoId, ocorridoEm, pedidoId, numeroPedido,
  emailCliente, ...` + `resumo()`), com factory `de(Pedido, ...)`. Criar `PagamentoRecusadoEvent`.
- `ObservadorEventoDominio { String nomeCanal(); boolean interessadoEm(EventoDominio); void notificar(EventoDominio); }`
- `BarramentoEventosDominio` (@Component): lista `CopyOnWriteArrayList` com todos os observadores injetados +
  `inscrever/cancelarInscricao` (attach/detach clássico); `publicar` síncrono com try/catch por observador
  (um listener com erro não derruba os outros). Síncrono de propósito: testes `@Transactional` não disparariam afterCommit.
- Listeners: `NotificacaoEmailListener` (todos os eventos), `NotificacaoSmsListener` (pagamento confirmado e envio),
  `AuditoriaPedidoListener` (log estruturado).

### 4.2 Frete (T025–T028)
- Tirar a regra de frete grátis de dentro do `CorreiosPacAdapter` → nova `PoliticaFreteGratis` (R$ 250 e 5 kg),
  aplicada pelo `ServicoCalculoFrete` só na modalidade econômica (flag `economica` em `ModalidadeFrete`).
- Considerar o CEP: enum `RegiaoEntrega.deCep(cep)` pelo 1º dígito (fator de preço + dias extras).
- Adapters como ACL de verdade: cada um traduz a resposta de uma "API externa simulada" (ex.: Correios devolve
  valor em String com vírgula e peso em kg; transportadora devolve centavos) para `CotacaoFrete`.
- `FreteController` `POST /api/v1/frete/calcular` (público), resposta conforme contrato.

### 4.3 Pagamento — Abstract Factory (T020) e cartão
- `FabricaProvedorPagamento` (família: `GatewayPagamentoPort` + `FormatadorInstrucoesPagamento` +
  `TipoLiquidacao ONLINE|ASSINCRONA`). Fábricas: PIX, Cartão, Boleto. `GatewayPagamentoServiceLocator` vira
  `Map<MetodoPagamento, Fabrica>`.
- `CartaoCreditoGatewayAdapter`: cartão final `0000` deve **retornar** `RespostaPagamentoDTO.recusa(...)` (hoje lança exceção).
- Adicionar `expiraEm` ao `RespostaPagamentoDTO` (PIX/boleto). Adicionar `CANCELADA` em `StatusTransacaoPagamento`.

### 4.4 Checkout — Template Method + Facade (T009–T011)
- `ProcessadorCheckoutTemplate.processarCheckout(ctx)` **final**: checkout repetido (idempotência) →
  validarClienteEEndereco → obterResumoCarrinho (vazio = 400) → validarPrecosVigentes (preço do carrinho ≠ catálogo
  = 409; regra da constituição p/ Integrante 3) → calcularFrete → reservarEstoque → criarPedido (Builder, rateio de
  desconto) → processarCobranca → **tratarResultadoCobranca (abstrato)** → esvaziarCarrinho (hook
  `deveEsvaziarCarrinho`, falso se recusado) → publicar eventos → resposta.
- Subclasses: `ProcessadorCheckoutPagamentoOnline` (cartão: aprova → confirma via serviço de pagamento; recusa →
  Mediator) e `ProcessadorCheckoutPagamentoAssincrono` (PIX/boleto → `aguardarConfirmacaoPagamento`).
- `CheckoutFacade.executarCheckout(email, request)` `@Transactional`: escolhe o processador pelo `TipoLiquidacao` da fábrica.
- Chave da reserva: `"CHK-" + clienteId + "-" + chaveIdempotenciaCliente` (limitar a chave do cliente a 60
  caracteres) ou `"CHK-" + numeroPedido`. Se já existir pedido com essa chave → devolve o mesmo pedido.
- Rateio: desconto total proporcional ao subtotal bruto de cada linha, desconto unitário = parcela/quantidade
  (HALF_EVEN). Pequenas diferenças de centavos entre soma das linhas e o total ficam documentadas; o total oficial é o do Módulo 2.
- Número do pedido: `PED-AAAAMM-XXXXXX` (aleatório, checar unicidade).
- Cartão recusado: pedido persiste como `PAGAMENTO_RECUSADO`, reserva liberada, carrinho **não** é esvaziado, HTTP 201.
- Reescrever `CheckoutResponseDTO` como record aninhado igual ao contrato (`dadosPagamento`, `snapshotFinanceiro`,
  `entrega`, `itens`), com mapeador compartilhado com `PedidoResponseDTO`.

### 4.5 Webhook idempotente (T021–T023)
- `ServicoIdempotenciaPagamento.executar(chave, Supplier<T>, Class<T>)` → `ResultadoIdempotente<T>(resposta, reexecutado)`,
  payload serializado em JSON na tabela `registro_idempotencia_pagamento`.
- Chave: `"WEBHOOK:" + chaveOperacaoPedido` (= numeroPedido). Fluxo: lê registro → `@Lock(PESSIMISTIC_WRITE)`
  no pedido (`buscarPorNumeroComLock`) → relê registro → processa → grava registro.
- Aprovado: State `confirmarPagamento` → transação `confirmar` → `confirmarBaixaDefinitiva` (Módulo 1) →
  `confirmarConsumoDeCupom` (Módulo 2, se houver cupom) → `PagamentoConfirmadoEvent`. Valor pago diferente do total → 400.
- Recusado: Mediator `coordenarRecusaPagamento`.
- Cartão aprovado no checkout também grava o registro `WEBHOOK:<numero>` para webhooks tardios responderem 200.
- Webhook protegido por header `X-Gateway-Token` (property `ecommerce.modulo3.webhook-token`).

### 4.6 Cancelamento — Mediator + Template Method (T030–T032)
- `CoordenadorCancelamentoMediator`: `coordenarCancelamento(pedido, motivo)` (lê `pagamentoJaCapturado()` **antes**,
  executa `pedido.cancelar` primeiro para validar no State; se pago → estorno no gateway + `estornarBaixaDefinitivaPorCancelamento`;
  senão → `liberarReservaPorCancelamentoOuRecusa`; transação → ESTORNADA/CANCELADA; evento) e `coordenarRecusaPagamento`.
- `ServicoCancelamentoPedido` abstrato (Template) com subclasses Cliente (só o dono) e Administrador; hook de
  auditoria via `PortaRegistroAuditoria`. Endpoint `DELETE /api/v1/pedidos/{id}/cancelamento`.

### 4.7 Consulta e transições admin (T015/T016)
- `ServicoConsultaPedido` (cliente só vê os próprios → 403). Endpoints `GET /api/v1/pedidos/me`,
  `GET /api/v1/pedidos/{id}`, `PATCH /api/v1/admin/pedidos/{id}/preparar` (extra, necessário para sair de PAGO),
  `/despachar` (gera rastreio se ausente, publica `PedidoEnviadoEvent`), `/entregar`.
- Exceção de acesso negado do Módulo 3 → 403 e de conflito → 409 no `ManipuladorGlobalExcecoesRest`.

### 4.8 Relatórios (T037–T039) e segurança
- Records `RelatorioVendasConsolidadasDTO` (+ descontos por cupom), `RelatorioTicketMedioDTO`, `ItemMaisVendidoDTO`,
  `RelatorioCancelamentosDTO` (pedido pela spec). Queries já existem em `PedidoJpaRepository`/`ItemPedidoSnapshotJpaRepository`.
- `ConfiguracaoSegurancaHttp`: checkout e `/pedidos/me` = CLIENTE; `/pedidos/**` = CLIENTE/ADMIN;
  `/admin/pedidos/**` = ADMIN/OPERADOR_COMERCIAL; `/admin/relatorios/**` = ADMIN; `/pagamentos/confirmacao` e
  `/frete/calcular` = permitAll (antes do `anyRequest`).

### 4.9 Testes, runner e Swagger (T007, T012, T017, T024, T029, T033, T036, T040–T042)
- Seed do Módulo 1 (`InicializadorDadosDemonstracaoModulo1Runner.executarCargaDemonstracaoIdempotente()`) cria
  catálogo e estoque (`DEMO-TEC-PRO-PRETO` 10 un, `DEMO-MOU-PREC-PRETO` 5 un, `DEMO-KIT-GAMER-01`), mas **nenhum
  cliente**. Testes criam cliente com `ServicoIdentidadeECliente.cadastrarClientePublico(...)` +
  `adicionarEnderecoAoCliente` + `selecionarEnderecoDoCliente`, carrinho via `ServicoCarrinhoCompras` /
  `POST /api/v1/carrinho/itens` com `@WithMockUser(username = email)`. Ver `IntegracaoModulo2Test` como modelo
  (`@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional`).
- Runner de demo (`ecommerce.modulo3.seed-demo-ativo`) deve passar pelo fluxo real (checkout + webhook + transições).
- Swagger: tags Checkout, Pedidos, Pagamentos, Frete, Relatórios em `ConfiguracaoOpenApiSwagger`.
- Javadoc faltando (~120 métodos): `CheckoutResponseDTO`, `RequisicaoCobrancaDTO`, `RespostaPagamentoDTO`,
  `CheckoutRequestDTO`, `CotacaoFrete`, eventos.
- Final: `.\mvnw.cmd clean test` com 100% nos 3 módulos.

## 5. Atualizar o guia de apresentação (pendente)
No arquivo `modulo3-apresentacao-henrique.md`, incluir (em linguagem de estudo, com o "porquê"):
o bug de schema e a correção; as extensões de portas dos Módulos 1/2 e por que foram necessárias (dois IDs de
cliente, endereço incompleto, devolução pós-baixa, auditoria); o Builder do Pedido; a tabela de transições do State
(incluindo `pagamentoJaCapturado` e `preparar`); como cada pattern foi realmente implementado (classes reais);
diagrama dos fluxos de checkout, webhook idempotente e cancelamento; perguntas prováveis da banca; roteiro de demo
com payloads reais do Swagger.
