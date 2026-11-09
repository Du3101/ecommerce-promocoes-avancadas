# Interface Contracts: Módulo 2 — API REST & Portas de Integração

**Feature**: `002-carrinho-promocoes-cupons`  
**Date**: 2026-11-03  
**Integrante 2**: Eduardo Juncker Toras  
**Format**: REST JSON API (OpenAPI 3.0) + Porta Interna Java para o Módulo 3  

---

## 1. Endpoints REST — Carrinho de Compras (`/api/v1/carrinho`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Sucesso / Erro |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/carrinho` | Público / Cliente | Retorna o carrinho atual com todos os itens, memória de cálculo transparente e descontos. | `200 OK` |
| `POST` | `/api/v1/carrinho/itens` | Público / Cliente | Adiciona um item ao carrinho por SKU e quantidade (valida catálogo via Módulo 1). | `201 Created`, `400 Bad Request`, `404 Not Found` |
| `PUT` | `/api/v1/carrinho/itens/{sku}` | Público / Cliente | Atualiza a quantidade desejada de um SKU no carrinho. | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/v1/carrinho/itens/{sku}` | Público / Cliente | Remove um SKU do carrinho e recalcula o total. | `200 OK`, `404 Not Found` |
| `DELETE` | `/api/v1/carrinho` | Público / Cliente | Esvazia completamente o carrinho. | `204 No Content` |
| `POST` | `/api/v1/carrinho/cupom` | Público / Cliente | Aplica um cupom promocional via código (valida vigência, limite e elegibilidade). | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/v1/carrinho/cupom` | Público / Cliente | Remove o cupom atualmente aplicado ao carrinho. | `200 OK` |

### Exemplo de Resposta — `GET /api/v1/carrinho` (com Decorator e Memória de Cálculo)

```json
{
  "carrinhoId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "clienteId": "b8c4e1a0-5b2e-4f1a-9a1b-02a9d22c1111",
  "quantidadeTotalItens": 3,
  "pesoTotalGramas": 1700,
  "itens": [
    {
      "sku": "DEMO-TEC-PRO-PRETO",
      "nome": "Teclado Mecânico Gamer FATEC Pro",
      "tipoItem": "PRODUTO",
      "precoUnitario": 300.00,
      "quantidade": 1,
      "subtotal": 300.00,
      "pesoGramas": 850
    },
    {
      "sku": "DEMO-MOU-PREC-PRETO",
      "nome": "Mouse Gamer Óptico FATEC Precision",
      "tipoItem": "PRODUTO",
      "precoUnitario": 150.00,
      "quantidade": 2,
      "subtotal": 300.00,
      "pesoGramas": 240
    }
  ],
  "cupomAplicado": {
    "codigo": "BEMVINDO10",
    "tipoDesconto": "PERCENTUAL",
    "valorDesconto": 10.00,
    "ativo": true
  },
  "memoriaCalculo": {
    "subtotalBruto": 600.00,
    "totalDescontos": 110.00,
    "totalLiquido": 490.00,
    "descontosDiscriminados": [
      {
        "origem": "PROMOCAO",
        "nomeBeneficio": "Promoção Combo Gamer Teclado + Mouse",
        "valorAbatido": 50.00
      },
      {
        "origem": "CUPOM",
        "nomeBeneficio": "Cupom BEMVINDO10 (10% sobre saldo elegível)",
        "valorAbatido": 60.00
      }
    ]
  }
}
```

---

## 2. Endpoints REST — Gestão Administrativa de Campanhas e Cupons (`/api/v1/admin/promocoes` e `/api/v1/admin/cupons`)

| Método | Rota | Perfis Autorizados | Descrição | Status |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/promocoes` | `ADMIN`, `RESPONSAVEL_PROMOCOES` | Lista todas as campanhas promocionais cadastradas. | `200 OK` |
| `POST` | `/api/v1/admin/promocoes` | `ADMIN`, `RESPONSAVEL_PROMOCOES` | Cria uma nova campanha com regras de mecânica e árvore de elegibilidade. | `201 Created`, `400 Bad Request` |
| `PATCH` | `/api/v1/admin/promocoes/{id}/status` | `ADMIN`, `RESPONSAVEL_PROMOCOES` | Ativa ou desativa uma campanha promocional. | `200 OK`, `404 Not Found` |
| `GET` | `/api/v1/admin/cupons` | `ADMIN`, `RESPONSAVEL_PROMOCOES` | Lista cupons promocionais cadastrados e seus contadores de uso. | `200 OK` |
| `POST` | `/api/v1/admin/cupons` | `ADMIN`, `RESPONSAVEL_PROMOCOES` | Cria um novo cupom de desconto com cotas e vigência. | `201 Created`, `400 Bad Request`, `409 Conflict` |

---

## 3. Contrato de Integração Interna (Java Port) para o Módulo 3 (Checkout/Pedidos)

O **Módulo 3 (Integrante 3)** não acessa diretamente as tabelas do Módulo 2. Ele consome a porta desacoplada:

```java
package br.edu.fatec.ecommerce.modulo2.integracao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Porta de consulta do Carrinho e Promoções fornecida pelo Módulo 2
 * (Eduardo Juncker Toras) para o Módulo 3 (Checkout, Pedidos e Pagamentos).
 */
public interface PortaConsultaCarrinhoPromocoes {

    /**
     * Obtém o resumo consolidado do carrinho de um cliente ou sessão,
     * incluindo itens físicos, peso total e o snapshot comercial de formação de preço.
     */
    ResumoCarrinhoCheckoutDTO obterResumoCarrinhoParaCheckout(UUID clienteId, String sessaoId);

    /**
     * Limpa o carrinho após a conclusão bem-sucedida do checkout no Módulo 3.
     */
    void esvaziarCarrinhoAposCheckout(UUID clienteId, String sessaoId);

    /**
     * Notifica a confirmação definitiva de uso do cupom para incrementar o contador de uso.
     */
    void confirmarConsumoDeCupom(String codigoCupom);
}
```

