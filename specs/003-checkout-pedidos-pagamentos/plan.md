# Implementation Plan: Módulo 3 — Checkout, Ciclo de Vida de Pedidos, Pagamentos Idempotentes, Frete, Notificações e Relatórios Gerenciais

**Branch**: `003-checkout-pedidos-pagamentos` | **Date**: 2026-11-11 | **Spec**: [spec.md](./spec.md)  
**Integrante 3**: Henrique Sandrini  

---

## Summary

Este plano técnico estrutura a implementação do **Módulo 3 (Integrante 3 — Henrique Sandrini)** do Sistema de E-commerce com Promoções Avançadas: **Orquestração de Checkout com Facade**, **Snapshot Comercial Imutável no Pedido**, **Máquina de Estados do Ciclo de Vida do Pedido com State**, **Pagamentos Simulados com Idempotência Estrita via Adapter**, **Cálculo de Frete e Transportadoras com Frete Grátis**, **Fluxos Invariantes de Checkout e Cancelamento com Template Method**, **Coordenação de Compensação com Mediator**, **Barramento de Eventos de Domínio com Observer** e **Relatórios Gerenciais Analíticos**.

A arquitetura aplica de maneira autêntica e justificada os 7 padrões GoF exigidos para o Integrante 3 na [Constituição do Projeto](../../.specify/memory/constitution.md): `Facade`, `State`, `Adapter` (com Camada Anticorrupção), `Abstract Factory` (combinada a `Service Locator`), `Template Method` (com `Hook Method`), `Observer` e `Mediator`, além de consumir estritamente as portas Java expostas pelos Integrantes 1 e 2 (`PortaConsultaCatalogoCliente`, `PortaComandoReservaEstoque` e `PortaConsultaCarrinhoPromocoes`).

---

## Technical Context

**Language/Version**: Java 21 (LTS)  
**Primary Dependencies**: Spring Boot 3.4.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`), Springdoc OpenAPI 2, Flyway.  
**Storage**: PostgreSQL / H2 em modo de compatibilidade PostgreSQL (para testes automatizados rápidos sem dependência externa).  
**Testing**: JUnit 5, MockMvc, testes unitários para cada padrão de projeto e testes integrados de API REST.  
**Target Platform**: JVM em ambiente Linux / Windows Server / Container Docker.  
**Constraints**:
- 100% dos métodos documentados com comentários e Javadoc explícito indicando o papel funcional e no Design Pattern (Constituição, Princípio III).
- Ausência de estruturas condicionais monolíticas (`if/else` ou `switch`) onde cabe polimorfismo (Princípio I).
- Idempotência estrita garantida no processamento de notificações de pagamento.
- Isolamento modular absoluto: consumo exclusivo pelas portas Java desacopladas.

---

## Constitution Check

| Princípio da Constituição | Status | Evidência no Plano / Design |
| :--- | :--- | :--- |
| **I. OCP e Design Patterns** | ✅ PASS | Adição de novos estados de pedido via `EstadoPedido` (`State`), novas formas de pagamento e transportadoras via `Adapter` e `Abstract Factory` sem alterar o núcleo transacional. |
| **II. Catálogo de Padrões Integrante 3** | ✅ PASS | Todos os 7 padrões obrigatórios mapeados: `Facade`, `State`, `Adapter` (ACL), `Abstract Factory`/`Service Locator`, `Template Method`/`Hook Method`, `Observer`, `Mediator`. |
| **III. Código Explícito e Comentários** | ✅ PASS | Regra inegociável: comentários funcionais e papel no Design Pattern em absolutamente todos os métodos e classes do pacote `modulo3`. |
| **IV. Integridade Transacional e Idempotência** | ✅ PASS | Snapshot comercial imutável no `Pedido`, reserva atômica de estoque com liberação garantida no cancelamento, e tabela `tb_registro_idempotencia_pagamento` para repetições idênticas sem efeitos colaterais duplos. |
| **V. Camadas e Segurança** | ✅ PASS | Pacote `br.edu.fatec.ecommerce.modulo3` segregado em `checkout`, `pedido`, `pagamento`, `frete`, `eventos`, `notificacao` e `relatorios`. Rastreabilidade RBAC com `CLIENTE` e `ADMIN`. |
| **VI. Testes Automatizados** | ✅ PASS | Suíte JUnit 5 cobrindo: checkout ponta a ponta, máquina de estados com transições ilegais bloqueadas, recebimento duplicado de confirmação de pagamento (idempotência), frete grátis e cancelamento com liberação de estoque. |
| **Divisão de Trabalho em 3 Integrantes** | ✅ PASS | Módulo 3 totalmente desacoplado, consumindo `PortaConsultaCatalogoCliente` e `PortaComandoReservaEstoque` (Módulo 1) e `PortaConsultaCarrinhoPromocoes` (Módulo 2). |

---

## Project Structure

### Documentation (this feature)

```text
specs/003-checkout-pedidos-pagamentos/
├── plan.md              # Este plano técnico (/speckit-plan)
├── research.md          # Defesa acadêmica dos 7 Design Patterns GoF (/speckit-plan)
├── data-model.md        # Entidades, Value Objects, State Machine e Migração Flyway V3 (/speckit-plan)
├── quickstart.md        # Guia de testes, execução e roteiro de demonstração (/speckit-plan)
├── contracts/           # Contratos REST JSON e Portas Java consumidas (/speckit-plan)
│   └── modulo3-api-and-integration-ports.md
└── checklists/
    └── requirements.md  # Checklist de qualidade da especificação
```

### Source Code (Módulo 3)

```text
src/main/java/br/edu/fatec/ecommerce/modulo3/
├── checkout/
│   ├── api/
│   │   ├── CheckoutController.java
│   │   ├── dto/
│   │   │   ├── CheckoutRequestDTO.java
│   │   │   └── CheckoutResponseDTO.java
│   ├── aplicacao/
│   │   ├── CheckoutFacade.java
│   │   ├── ProcessadorCheckoutTemplate.java
│   │   ├── ProcessadorCheckoutPix.java
│   │   ├── ProcessadorCheckoutCartao.java
│   │   └── ProcessadorCheckoutBoleto.java
│   └── dominio/
│       ├── ValidadorCheckout.java
│       └── SnapshotFinanceiroVO.java
├── pedido/
│   ├── api/
│   │   ├── PedidoClienteController.java
│   │   └── PedidoAdminController.java
│   ├── aplicacao/
│   │   ├── ServicoConsultaPedido.java
│   │   └── ServicoCancelamentoPedido.java
│   ├── dominio/
│   │   ├── Pedido.java
│   │   ├── ItemPedidoSnapshot.java
│   │   ├── EnderecoEntregaSnapshot.java
│   │   ├── SnapshotFinanceiro.java
│   │   ├── StatusPedido.java
│   │   ├── TransicaoEstadoInvalidaException.java
│   │   └── estado/
│   │       ├── EstadoPedido.java
│   │       ├── PedidoCriadoState.java
│   │       ├── PedidoAguardandoPagamentoState.java
│   │       ├── PedidoPagoState.java
│   │       ├── PedidoEmPreparacaoState.java
│   │       ├── PedidoEnviadoState.java
│   │       ├── PedidoEntregueState.java
│   │       ├── PedidoCanceladoState.java
│   │       └── PedidoPagamentoRecusadoState.java
│   └── infraestrutura/
│       ├── PedidoJpaRepository.java
│       └── ItemPedidoSnapshotJpaRepository.java
├── pagamento/
│   ├── api/
│   │   └── WebhookPagamentoController.java
│   ├── aplicacao/
│   │   ├── ServicoProcessamentoPagamento.java
│   │   └── ServicoIdempotenciaPagamento.java
│   ├── dominio/
│   │   ├── MetodoPagamento.java
│   │   ├── StatusTransacaoPagamento.java
│   │   ├── TransacaoPagamento.java
│   │   └── RegistroIdempotenciaPagamento.java
│   ├── adaptadores/
│   │   ├── GatewayPagamentoPort.java
│   │   ├── PixGatewayAdapter.java
│   │   ├── CartaoCreditoGatewayAdapter.java
│   │   ├── BoletoGatewayAdapter.java
│   │   └── dto/
│   │       ├── RequisicaoCobrancaDTO.java
│   │       └── RespostaPagamentoDTO.java
│   ├── fabrica/
│   │   ├── FabricaGatewayPagamento.java
│   │   └── GatewayPagamentoServiceLocator.java
│   └── infraestrutura/
│       ├── TransacaoPagamentoJpaRepository.java
│       └── RegistroIdempotenciaJpaRepository.java
├── frete/
│   ├── api/
│   │   └── FreteController.java
│   ├── aplicacao/
│   │   └── ServicoCalculoFrete.java
│   ├── dominio/
│   │   ├── CotacaoFrete.java
│   │   ├── ModalidadeFrete.java
│   │   └── PoliticaFreteGratis.java
│   └── adaptadores/
│       ├── CalculadoraFretePort.java
│       ├── CorreiosPacAdapter.java
│       ├── CorreiosSedexAdapter.java
│       └── TransportadoraExpressaAdapter.java
├── mediador/
│   └── CoordenadorCancelamentoMediator.java
├── eventos/
│   ├── BarramentoEventosDominio.java
│   ├── EventoDominio.java
│   ├── PedidoCriadoEvent.java
│   ├── PagamentoConfirmadoEvent.java
│   ├── PedidoCanceladoEvent.java
│   └── PedidoEnviadoEvent.java
├── notificacao/
│   ├── NotificacaoEmailListener.java
│   └── NotificacaoSmsListener.java
└── relatorios/
    ├── api/
    │   └── RelatoriosAdminController.java
    ├── aplicacao/
    │   └── ServicoRelatoriosGerenciais.java
    └── dto/
        ├── RelatorioVendasConsolidadasDTO.java
        ├── RelatorioTicketMedioDTO.java
        └── ItemMaisVendidoDTO.java
```

### Database Migrations

```text
src/main/resources/db/migration/
├── V1__modulo1_identidade_catalogo_estoque_auditoria.sql
├── V2__modulo2_carrinho_promocoes_cupons.sql
└── V3__modulo3_checkout_pedidos_pagamentos_frete.sql
```

### Test Suite

```text
src/test/java/br/edu/fatec/ecommerce/modulo3/
├── checkout/
│   └── CheckoutFacadeIntegrationTest.java
├── pedido/
│   ├── MaquinaEstadosPedidoTest.java
│   └── CancelamentoPedidoMediatorTest.java
├── pagamento/
│   ├── PagamentoIdempotenciaIntegrationTest.java
│   └── GatewayPagamentoAdaptersTest.java
├── frete/
│   └── CalculoFreteEPoliticaFreteGratisTest.java
├── eventos/
│   └── BarramentoEventosObserverTest.java
└── relatorios/
    └── RelatoriosGerenciaisAdminTest.java
```
