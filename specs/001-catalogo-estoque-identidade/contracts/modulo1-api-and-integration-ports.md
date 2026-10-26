# Interface Contracts: Módulo 1 — API REST & Portas de Integração entre Módulos

**Feature**: `001-catalogo-estoque-identidade`  
**Date**: 2026-10-26  
**Format**: REST JSON API (OpenAPI 3.0 / Swagger) + Portas Internas Java para Módulos 2 e 3

---

## 1. Endpoints REST — Identidade, Clientes e Endereços (`/api/v1/auth` e `/api/v1/clientes`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Sucesso / Erro |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/clientes/cadastro` | Público | Auto-cadastro exclusivo de conta `CLIENTE` com hash BCrypt (`FR-001`). | `201 Created`, `400 Bad Request`, `409 Conflict` (e-mail/CPF duplicado) |
| `POST` | `/api/v1/auth/login` | Público | Autentica credenciais e retorna dados da sessão/token e perfis do usuário. | `200 OK`, `401 Unauthorized` |
| `POST` | `/api/v1/admin/usuarios/operadores` | `ADMIN` | Cria conta administrativa/operacional (`OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE`, `RESPONSAVEL_PROMOCOES`, `ADMIN`) (`FR-002`). | `201 Created`, `403 Forbidden`, `409 Conflict` |
| `GET` | `/api/v1/clientes/me` | `CLIENTE` | Retorna dados do próprio cliente e seu endereço selecionado (ou resumo do `EnderecoAusenteNullObject`). | `200 OK`, `401 Unauthorized` |
| `POST` | `/api/v1/clientes/me/enderecos` | `CLIENTE` | Cadastra novo endereço para o cliente autenticado (`FR-003`). | `201 Created`, `400 Bad Request` |
| `PUT` | `/api/v1/clientes/me/enderecos/{enderecoId}/selecionar` | `CLIENTE` | Define qual endereço do cliente será utilizado para a entrega atual (`FR-003`). | `200 OK`, `404 Not Found` |
| `DELETE` | `/api/v1/clientes/me/enderecos/{enderecoId}` | `CLIENTE` | Remove um endereço do cliente; se era o selecionado, o perfil volta a retornar `EnderecoAusenteNullObject`. | `204 No Content`, `404 Not Found` |

### Exemplo de Payload — `POST /api/v1/auth/clientes/cadastro`
```json
{
  "nomeCompleto": "Maria Aparecida Silva",
  "email": "maria.silva@exemplo.com",
  "cpf": "12345678909",
  "telefone": "19999998888",
  "senha": "SenhaForte@2026"
}
```

### Exemplo de Resposta — `GET /api/v1/clientes/me` (quando ainda não selecionou endereço — `Null Object`)
```json
{
  "clienteId": "b8c4e1a0-5b2e-4f1a-9a1b-02a9d22c1111",
  "nomeCompleto": "Maria Aparecida Silva",
  "email": "maria.silva@exemplo.com",
  "primeiraCompraConcluida": false,
  "enderecoSelecionado": {
    "possuiEnderecoValido": false,
    "cep": "",
    "uf": "",
    "resumoFormatado": "Nenhum endereço de entrega selecionado"
  }
}
```

---

## 2. Endpoints REST — Categorias, Catálogo de Produtos, Variações e Combos (`/api/v1/catalogo`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Sucesso / Erro |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/catalogo/categorias/arvore` | Público | Retorna a árvore recursiva completa de categorias e subcategorias (`Composite`) (`FR-007`). | `200 OK` |
| `POST` | `/api/v1/admin/catalogo/categorias` | `ADMIN`, `OPERADOR_COMERCIAL` | Cria categoria raiz ou subcategoria filha (validando ausência de ciclos e gravando auditoria via `Proxy`). | `201 Created`, `400 Bad Request`, `403 Forbidden` |
| `GET` | `/api/v1/catalogo/itens` | Público | Lista itens comercializáveis (`ProdutoVariacaoLeaf` e `ComboKitComposite`), filtrando opcionalmente por `categoriaId` (incluindo todas as subcategorias descendentes recursivamente). | `200 OK` |
| `GET` | `/api/v1/catalogo/itens/{sku}` | Público | Consulta detalhes uniformes de um item simples ou Combo/Kit (preço vigente, peso total, dimensões agregadas, atributos `Flyweight` e disponibilidade máxima). | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/admin/catalogo/produtos` | `ADMIN`, `OPERADOR_COMERCIAL` | Cadastra produto com variações usando `ProdutoBuilder`, `CriadorItemCatalogo` (`Factory Method`) e `FabricaAtributoVariacaoFlyweight` (`Flyweight`). | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `PATCH` | `/api/v1/admin/catalogo/itens/{sku}/preco` | `ADMIN`, `OPERADOR_COMERCIAL` | Altera preço base/adicional de produto ou preço de combo, gerando registro automático na trilha de auditoria via `CatalogoAdminAuditoriaProxy`. | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `PATCH` | `/api/v1/admin/catalogo/itens/{sku}/status` | `ADMIN`, `OPERADOR_COMERCIAL` | Ativa ou inativa logicamente um item ou combo do catálogo, gerando auditoria `ATIVACAO_INATIVACAO_ITEM` via `CatalogoAdminAuditoriaProxy`. | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/admin/catalogo/combos` | `ADMIN`, `OPERADOR_COMERCIAL` | Cria um `ComboKitComposite` (`Composite`) com modo de preço recursivo ou fixo (`FR-008`). | `201 Created`, `400 Bad Request`, `409 Conflict` |

### Exemplo de Payload — `POST /api/v1/admin/catalogo/combos`
```json
{
  "skuCombo": "KIT-GAMER-PRO",
  "nomeCombo": "Kit Gamer Teclado Mecânico + Mouse Óptico",
  "descricao": "Conjunto completo para escritório e jogos",
  "categoriaId": "a1b2c3d4-0000-4000-8000-000000000001",
  "modoPreco": "SOMA_RECURSIVA_COMPONENTES",
  "percentualDescontoKit": 10.00,
  "precoFixoOpcional": null,
  "componentes": [
    { "componenteSku": "TEC-MEC-RGB-PT", "quantidadePorKit": 1 },
    { "componenteSku": "MOU-OPT-16000DPI", "quantidadePorKit": 1 }
  ]
}
```

---

## 3. Endpoints REST — Estoque, Reservas e Auditoria (`/api/v1/estoque` e `/api/v1/admin/auditoria`)

| Método | Rota | Perfis Autorizados | Descrição | Status de Sucesso / Erro |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/estoque/{sku}` | Público / Autenticado | Consulta quantidade disponível e reservada de um SKU ou disponibilidade máxima calculada de um Combo. | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/admin/estoque/{sku}/ajustes` | `ADMIN`, `RESPONSAVEL_ESTOQUE` | Realiza entrada ou ajuste manual de saldo físico com auditoria automática via `EstoqueAdminSegurancaProxy`. | `200 OK`, `400 Bad Request`, `403 Forbidden` |
| `POST` | `/api/v1/estoque/reservas` | `CLIENTE`, `ADMIN` (ou via Checkout) | Reserva atomicamente SKUs (ou componentes de Combos) para fechamento de compra (`FR-010`). | `201 Created`, `409 Conflict` (estoque insuficiente) |
| `POST` | `/api/v1/estoque/reservas/{chaveOperacao}/confirmar` | Autenticado / Sistema | Converte reserva `ATIVA` em `CONFIRMADA` (baixa definitiva idempotente) (`FR-011`). | `200 OK`, `409 Conflict` |
| `POST` | `/api/v1/estoque/reservas/{chaveOperacao}/liberar` | Autenticado / Sistema | Libera reserva `ATIVA` para `LIBERADA` devolvendo o saldo ao estoque disponível de forma idempotente (`FR-011`). | `200 OK`, `409 Conflict` |
| `POST` | `/api/v1/admin/estoque/reservas/expirar-vencidas` | `ADMIN`, `RESPONSAVEL_ESTOQUE` | Varre e libera reservas ativas cujo `expiraEm` já venceu (`FR-011`). | `200 OK` |
| `GET` | `/api/v1/admin/auditoria` | `ADMIN` | Consulta trilha de auditoria imutável filtrando por usuário, tipo de operação e período (`FR-012`). | `200 OK`, `403 Forbidden` |

---

## 4. Contratos de Integração Interna (Java Ports) para os Integrantes 2 e 3 (`FR-013`)

Para que o **Módulo 2 (Carrinho, Promoções, Elegibilidade e Cupons)** e o **Módulo 3 (Checkout, Pedidos, Pagamentos, Frete e Relatórios)** se conectem ao Módulo 1 sem acoplamento às entidades JPA:

```java
package br.edu.fatec.ecommerce.modulo1.integracao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Porta de leitura fornecida pelo Módulo 1 (Integrante 1) para consumo
 * desacoplado pelos Módulos 2 (Carrinho/Promoções) e 3 (Checkout/Frete).
 */
public interface PortaConsultaCatalogoCliente {

    /**
     * Consulta os dados consolidados e polimórficos de um item do catálogo
     * (seja ProdutoVariacaoLeaf ou ComboKitComposite).
     */
    VisaoItemCatalogoDTO consultarItemPorSku(String sku);

    /**
     * Verifica se determinado SKU pertence à categoria alvo ou a qualquer
     * uma de suas subcategorias descendentes (via padrão Composite).
     */
    boolean itemPertenceACategoriaOuSubcategoria(String sku, UUID categoriaAlvoId);

    /**
     * Retorna os dados de elegibilidade do cliente e seu endereço selecionado
     * (ou representação segura via EnderecoAusenteNullObject).
     */
    VisaoClienteElegibilidadeDTO consultarClienteParaCompra(UUID clienteId);
}

/**
 * Porta de comando transacional fornecida pelo Módulo 1 (Integrante 1)
 * para o Módulo 3 (Checkout, Pagamentos e Cancelamento de Pedidos).
 */
public interface PortaComandoReservaEstoque {

    /**
     * Reserva atomicamente os itens solicitados (decompondo Combos/Kits em SKUs
     * físicos e aplicando lock contra condição de corrida na última unidade).
     * Operação idempotente por chaveOperacaoPedido.
     */
    ResumoReservaEstoqueDTO reservarItensParaCheckout(
        String chaveOperacaoPedido,
        UUID clienteId,
        Map<String, Integer> quantidadePorSku
    );

    /**
     * Converte a reserva em baixa definitiva de forma idempotente após confirmação do pagamento.
     */
    ResumoReservaEstoqueDTO confirmarBaixaDefinitiva(String chaveOperacaoPedido);

    /**
     * Libera integralmente o estoque reservado de forma idempotente após pagamento recusado
     * ou cancelamento do pedido.
     */
    ResumoReservaEstoqueDTO liberarReservaPorCancelamentoOuRecusa(
        String chaveOperacaoPedido,
        String motivo
    );
}
```

