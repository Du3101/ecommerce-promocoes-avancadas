# Quickstart & Validation Guide: Módulo 3 — Checkout, Ciclo de Vida de Pedidos, Pagamentos Idempotentes, Frete, Notificações e Relatórios Gerenciais

**Feature**: `003-checkout-pedidos-pagamentos`  
**Date**: 2026-11-11  
**Integrante 3**: Henrique Sandrini  
**References**: [spec.md](./spec.md) | [data-model.md](./data-model.md) | [research.md](./research.md) | [contracts/modulo3-api-and-integration-ports.md](./contracts/modulo3-api-and-integration-ports.md)  

---

## 1. Comandos de Compilação e Testes Automatizados

### Executar a suíte completa de testes (Módulo 1, Módulo 2 e Módulo 3)
```powershell
.\mvnw.cmd clean test
```

### Executar exclusivamente os testes de unidade e integração do Módulo 3
```powershell
.\mvnw.cmd test -Dtest="br.edu.fatec.ecommerce.modulo3.**.*Test"
```

---

## 2. Inicialização e Acesso à Documentação Interativa OpenAPI

Suba a aplicação com o perfil de desenvolvimento:
```powershell
.\mvnw.cmd spring-boot:run
```

Acesse a interface interativa do Swagger UI:
* **URL**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Credenciais de Teste**:
  - `admin@ecommerce.fatec.edu.br` / `Admin@Fatec2026` (`ADMIN`)
  - `cliente.teste@fatec.edu.br` / `Cliente@Fatec2026` (`CLIENTE`)

---

## 3. Roteiro Prático de Demonstração dos Padrões GoF (Defesa Acadêmica)

### Cenário 1: Orquestração de Checkout e Snapshot Comercial Imutável (`Facade` + `Template Method`)
1. Realize login como cliente e adicione itens ao carrinho.
2. Execute `POST /api/v1/checkout` informando `metodoPagamento = "PIX"`.
3. Verifique que o pedido foi gerado com snapshot imutável, o estoque foi reservado no Módulo 1, o carrinho foi esvaziado no Módulo 2 e o status inicial é `AGUARDANDO_PAGAMENTO`.

### Cenário 2: Máquina de Estados do Pedido (`State`)
1. Consulte o pedido criado via `GET /api/v1/pedidos/{id}`.
2. Simule a confirmação do pagamento (status muda para `PAGO`).
3. Despache o pedido via `PATCH /api/v1/admin/pedidos/{id}/despachar` com código de rastreio (status muda para `ENVIADO`).
4. Tente cancelar o pedido enviado via `DELETE /api/v1/pedidos/{id}/cancelamento`.
5. Observe que a transição ilegal é bloqueada lançando `TransicaoEstadoInvalidaException` com status HTTP 400.

### Cenário 3: Idempotência Estrita no Webhook de Pagamento (`Adapter`)
1. Envie `POST /api/v1/pagamentos/confirmacao` com a `chaveOperacaoPedido`. O pedido transita para `PAGO` e o estoque baixa no Módulo 1.
2. Reenvie a **mesma requisição idêntica**.
3. O sistema responde com `200 OK` informando execução idempotente, **sem duplicar a baixa de estoque** e **sem emitir eventos repetidos**.

### Cenário 4: Cancelamento com Liberação Automática de Estoque (`Mediator` + `Template Method`)
1. Crie um novo pedido no estado `AGUARDANDO_PAGAMENTO`.
2. Solicite o cancelamento via `DELETE /api/v1/pedidos/{id}/cancelamento`.
3. O mediador orquestra o estorno e aciona `PortaComandoReservaEstoque.liberarReservaPorCancelamentoOuRecusa(...)`, devolvendo imediatamente as unidades ao saldo disponível do catálogo.

### Cenário 5: Barramento de Eventos e Notificações (`Observer`)
1. Observe os logs do console durante o checkout e confirmação de pagamento.
2. Veja os listeners `NotificacaoEmailListener` e `NotificacaoSmsListener` reagindo de forma assíncrona e desacoplada aos eventos `PedidoCriadoEvent` e `PagamentoConfirmadoEvent`.

### Cenário 6: Relatórios Gerenciais Analíticos
1. Autentique-se como `ADMIN`.
2. Acesse `GET /api/v1/admin/relatorios/vendas-consolidadas` e `GET /api/v1/admin/relatorios/ticket-medio`.
3. Visualize os indicadores financeiros consolidados com total faturado, descontos concedidos e ticket médio.
