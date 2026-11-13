# Interface Contracts: Módulo 3 — API REST & Portas de Integração

**Feature**: `003-checkout-pedidos-pagamentos`  
**Date**: 2026-11-11  
**Integrante 3**: Henrique Sandrini  
**Format**: REST JSON API (OpenAPI 3.0 / Swagger) + Consumo de Portas dos Módulos 1 e 2  

---

## 1. Endpoints REST do Módulo 3

### 1.1. Orquestração de Checkout (`/api/v1/checkout`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Retorno |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/checkout` | `CLIENTE` | Executa o fechamento da compra via `CheckoutFacade`, valida cliente/endereço no Módulo 1, lê o carrinho no Módulo 2, reserva estoque, calcula frete e cria o pedido com snapshot imutável. | `201 Created`, `400 Bad Request`, `404 Not Found`, `409 Conflict` (Estoque insuficiente) |

#### Exemplo de Payload — `POST /api/v1/checkout`
```json
{
  "metodoPagamento": "PIX",
  "modalidadeFrete": "CORREIOS_SEDEX",
  "chaveIdempotenciaCliente": "CHK-CLIENTE-991823"
}
```

#### Exemplo de Resposta — `POST /api/v1/checkout` (`201 Created`)
```json
{
  "pedidoId": "7e3b8f10-912c-482f-b472-a1b2c3d4e5f6",
  "numeroPedido": "PED-202611-0001",
  "status": "AGUARDANDO_PAGAMENTO",
  "metodoPagamento": "PIX",
  "dadosPagamento": {
    "chavePixCopiaECola": "00020126580014br.gov.bcb.pix0136fatec-ecommerce-pix-mock5204000053039865405520.005802BR5913FATEC_STORE6009SAO_PAULO62070503***6304E2D1",
    "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
    "expiraEm": "2026-11-11T18:30:00Z"
  },
  "snapshotFinanceiro": {
    "subtotalBruto": 600.00,
    "totalDescontos": 110.00,
    "valorFrete": 30.00,
    "freteGratis": false,
    "valorTotalLiquido": 520.00
  },
  "entrega": {
    "modalidade": "CORREIOS_SEDEX",
    "prazoDiasUteis": 2,
    "enderecoDestino": "Av. Paulista, 1000 - Bela Vista, São Paulo/SP - CEP: 01310-100"
  },
  "itens": [
    {
      "sku": "DEMO-TEC-PRO-PRETO",
      "nome": "Teclado Mecânico Gamer FATEC Pro",
      "quantidade": 1,
      "precoUnitarioTabela": 300.00,
      "descontoUnitario": 50.00,
      "subtotalLiquido": 250.00
    },
    {
      "sku": "DEMO-MOU-PREC-PRETO",
      "nome": "Mouse Gamer Óptico FATEC Precision",
      "quantidade": 2,
      "precoUnitarioTabela": 150.00,
      "descontoUnitario": 30.00,
      "subtotalLiquido": 240.00
    }
  ],
  "criadoEm": "2026-11-11T17:45:00Z"
}
```

---

### 1.2. Consulta e Gestão de Pedidos (`/api/v1/pedidos`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Retorno |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/pedidos/me` | `CLIENTE` | Lista todos os pedidos pertencentes ao cliente autenticado ordenados por data decrescente. | `200 OK` |
| `GET` | `/api/v1/pedidos/{id}` | `CLIENTE`, `ADMIN` | Consulta detalhada do pedido e do seu histórico/estado atual. Clientes só podem consultar os próprios pedidos. | `200 OK`, `403 Forbidden`, `404 Not Found` |
| `DELETE` | `/api/v1/pedidos/{id}/cancelamento` | `CLIENTE`, `ADMIN` | Solicita cancelamento do pedido. Valida o estado atual (`State`), aciona o `Mediator`, libera estoque no Módulo 1 e solicita estorno. | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `PATCH` | `/api/v1/admin/pedidos/{id}/despachar` | `ADMIN`, `OPERADOR_COMERCIAL` | Transita o pedido de `EM_PREPARACAO` para `ENVIADO`, associando o código de rastreamento logístico e emitindo evento. | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `PATCH` | `/api/v1/admin/pedidos/{id}/entregar` | `ADMIN`, `OPERADOR_COMERCIAL` | Registra a entrega final do pedido transitando o estado para `ENTREGUE`. | `200 OK`, `400 Bad Request`, `404 Not Found` |

---

### 1.3. Webhook de Confirmação de Pagamento com Idempotência (`/api/v1/pagamentos`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Retorno |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/pagamentos/confirmacao` | Público / Gateway | Recebe notificações de liquidação financeira. Possui **idempotência estrita**: requisições repetidas respondem com sucesso sem duplicar baixa de estoque ou eventos. | `200 OK`, `400 Bad Request`, `404 Not Found` |

#### Exemplo de Payload — `POST /api/v1/pagamentos/confirmacao`
```json
{
  "chaveOperacaoPedido": "PED-202611-0001",
  "idTransacaoGateway": "GW-PIX-98827361",
  "sucesso": true,
  "valorPago": 520.00,
  "motivoRecusa": null
}
```

#### Exemplo de Resposta — `POST /api/v1/pagamentos/confirmacao` (`200 OK`)
```json
{
  "statusProcessamento": "PROCESSADO_SUCESSO",
  "pedidoId": "7e3b8f10-912c-482f-b472-a1b2c3d4e5f6",
  "numeroPedido": "PED-202611-0001",
  "novoStatusPedido": "PAGO",
  "mensagem": "Pagamento confirmado com sucesso e baixa definitiva de estoque consolidada.",
  "idempotenteReexecutado": false
}
```

---

### 1.4. Simulação e Cotação de Frete (`/api/v1/frete`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Retorno |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/frete/calcular` | Público / `CLIENTE` | Calcula opções de frete disponíveis (PAC, SEDEX e Transportadora Expressa) com base no CEP de destino, peso e subtotal, aplicando regra de Frete Grátis. | `200 OK`, `400 Bad Request` |

#### Exemplo de Payload — `POST /api/v1/frete/calcular`
```json
{
  "cepDestino": "01310100",
  "pesoTotalGramas": 1700,
  "subtotalLiquido": 490.00
}
```

#### Exemplo de Resposta — `POST /api/v1/frete/calcular` (`200 OK`)
```json
{
  "cepDestino": "01310-100",
  "pesoTotalGramas": 1700,
  "freteGratisDisponivel": true,
  "opcoes": [
    {
      "modalidade": "CORREIOS_PAC",
      "nome": "Correios Econômico (PAC)",
      "valorOriginal": 22.50,
      "valorCobrado": 0.00,
      "prazoDiasUteis": 6,
      "freteGratisAplicado": true
    },
    {
      "modalidade": "CORREIOS_SEDEX",
      "nome": "Correios Expresso (SEDEX)",
      "valorOriginal": 35.00,
      "valorCobrado": 35.00,
      "prazoDiasUteis": 2,
      "freteGratisAplicado": false
    },
    {
      "modalidade": "TRANSPORTADORA_EXPRESSA",
      "nome": "Transportadora Privada Expressa",
      "valorOriginal": 45.00,
      "valorCobrado": 45.00,
      "prazoDiasUteis": 1,
      "freteGratisAplicado": false
    }
  ]
}
```

---

### 1.5. Relatórios Gerenciais Analíticos (`/api/v1/admin/relatorios`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Retorno |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/relatorios/vendas-consolidadas` | `ADMIN` | Total faturado bruto, total de descontos promocionais concedidos, total líquido e volume de pedidos concluídos. | `200 OK`, `403 Forbidden` |
| `GET` | `/api/v1/admin/relatorios/ticket-medio` | `ADMIN` | Valor médio gasto por cliente e indicador de ticket médio geral da loja. | `200 OK`, `403 Forbidden` |
| `GET` | `/api/v1/admin/relatorios/produtos-mais-vendidos` | `ADMIN` | Lista ordenada dos SKUs com maior volume de unidades faturadas e receita gerada. | `200 OK`, `403 Forbidden` |

---

## 2. Contratos Internos Java (Portas Consumidas pelo Módulo 3)

O Módulo 3 atua como consumidor estrito das portas disponibilizadas:

### Consumo do Módulo 1:
```java
// Consulta de Cliente e Endereço Selecionado
br.edu.fatec.ecommerce.modulo1.integracao.PortaConsultaCatalogoCliente
    -> VisaoClienteElegibilidadeDTO consultarClienteParaCompra(UUID clienteId);

// Comandos Transacionais de Estoque
br.edu.fatec.ecommerce.modulo1.integracao.PortaComandoReservaEstoque
    -> ResumoReservaEstoqueDTO reservarItensParaCheckout(String chave, UUID clienteId, Map<String, Integer> skus);
    -> ResumoReservaEstoqueDTO confirmarBaixaDefinitiva(String chave);
    -> ResumoReservaEstoqueDTO liberarReservaPorCancelamentoOuRecusa(String chave, String motivo);
```

### Consumo do Módulo 2:
```java
// Consulta de Carrinho Consolidado e Promoções
br.edu.fatec.ecommerce.modulo2.integracao.PortaConsultaCarrinhoPromocoes
    -> ResumoCarrinhoCheckoutDTO obterResumoCarrinhoParaCheckout(UUID clienteId, String sessaoId);
    -> void esvaziarCarrinhoAposCheckout(UUID clienteId, String sessaoId);
    -> void confirmarConsumoDeCupom(String codigoCupom);
```
