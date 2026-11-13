# Contratos HTTP REST e Fronteira Web Modular — Portal E-commerce (004-loja-web-ecommerce)

**Feature**: `004-loja-web-ecommerce` | **Data**: 2026-11-12 | **Base URL**: `http://localhost:8080`

Este documento define os novos contratos HTTP/estáticos, pequenas extensões de leitura/orquestração nos Módulos 2 e 3, ajustes em `ConfiguracaoSegurancaHttp` e as fronteiras entre os ES Modules JavaScript do portal web e a API REST existente dos Módulos 1, 2 e 3.

---

## 1. Contrato de Entrega Estática e Segurança HTTP (`ConfiguracaoSegurancaHttp`)

### 1.1 Recursos Estáticos Liberados (`permitAll`)
| Caminho | Origem em `classpath:/static/` | Observação |
|---------|-------------------------------|------------|
| `GET /`, `GET /index.html` | `src/main/resources/static/index.html` | Entrada única do portal SPA modular |
| `GET /css/**` | `src/main/resources/static/css/*.css` | Folhas de estilo (`tokens.css`, `layout.css`, `components.css`) |
| `GET /js/**` | `src/main/resources/static/js/*.js` | ES Modules puros servidos com MIME `text/javascript` |
| `GET /img/**` | `src/main/resources/static/img/*` | Imagens opcionais (vitrine utiliza ícones SVG inline) |
| `GET /favicon.ico` | Opcional / estático | Evita 401 no console do navegador para visitantes anônimos |

### 1.2 `AuthenticationEntryPoint` Customizado Sem Popup Nativo (`FR-006`)
Quando qualquer requisição a `/api/**` exige autenticação e falha ou não envia `Authorization`:
- **Status HTTP**: `401 Unauthorized`
- **Content-Type**: `application/json;charset=UTF-8`
- **Header `WWW-Authenticate`**: **AUSENTE (não enviado)** — impede popup modal nativo de Basic Auth no navegador durante chamadas `fetch()`.
- **Corpo JSON** (idêntico ao padrão de `ManipuladorGlobalExcecoesRest`):
```json
{
  "timestamp": "2026-11-12T21:45:00Z",
  "status": 401,
  "codigo": "NAO_AUTENTICADO",
  "mensagem": "Autenticação obrigatória para acessar este recurso."
}
```
> **Compatibilidade Swagger UI**: `/swagger-ui/**` e `/v3/api-docs/**` permanecem públicos; o botão **Authorize** continua montando o cabeçalho `Authorization: Basic <base64>` via JavaScript do Swagger UI.

---

## 2. Novos Endpoints e Extensões de API

### 2.1 `GET /api/v1/promocoes/ativas` (Módulo 2 — Público)
Lista campanhas promocionais ativas e vigentes no instante atual, ordenadas por prioridade descendente, para banners e selos da vitrine (`FR-002`).

- **Autenticação**: Pública (`permitAll`)
- **Respostas**:
  - `200 OK`:
```json
[
  {
    "id": "b7c0e91a-1234-4a56-9876-111122223333",
    "nome": "Leve 3 e Pague 2",
    "descricao": "Na compra de 3 itens elegíveis, o item de menor valor sai grátis!",
    "tipoMecanica": "LEVE_X_PAGUE_Y",
    "descricaoMecanica": "Leve X e Pague Y",
    "prioridade": 100,
    "cumulativa": false,
    "dataInicio": "2026-11-01T00:00:00Z",
    "dataFim": "2027-11-01T00:00:00Z"
  }
]
```

### 2.2 `GET /api/v1/loja/configuracao` (Compartilhado — Público)
Expõe parâmetros públicos não sensíveis da loja para governar recursos condicionais da UI (`Q3`, política pública de frete grátis exibida no topo/carrinho).

- **Autenticação**: Pública (`permitAll`)
- **Respostas**:
  - `200 OK`:
```json
{
  "modoDemonstracaoAtivo": true,
  "freteGratisValorMinimo": 250.00,
  "freteGratisPesoMaximoGramas": 5000
}
```
- **Regra de segurança**: nunca retorna tokens, senhas ou propriedades internas (`ecommerce.modulo3.webhook-token` nunca aparece aqui). Quando `seed-demo-ativo` estiver `false` (ex.: perfil `test` ou produção), `modoDemonstracaoAtivo` retorna `false`.

### 2.3 Extensão do Carrinho: Header `X-Sessao-Carrinho` e `POST /api/v1/carrinho/mesclar` (Módulo 2)
#### A. Resolução de Sessão em `/api/v1/carrinho/**`
O método `resolverSessao(...)` em `CarrinhoComprasController` passa a seguir estritamente a precedência:
1. Se usuário autenticado possuir `clienteId` -> usa `clienteId` (ignora `sessaoId` nas operações normais).
2. Se não autenticado -> usa header HTTP `X-Sessao-Carrinho` (se não vazio); caso ausente, fallback para query param `?sessaoId=`, depois `HttpSession` existente e por fim `"sessao-anonima-padrao"`.

#### B. `POST /api/v1/carrinho/mesclar` (`Q1`)
Mescla aditivamente os itens (e opcionalmente cupom válido) de um carrinho de visitante anônimo para o carrinho do cliente recém-autenticado.

- **Autenticação**: Obrigatória com perfil `CLIENTE` (`hasRole("CLIENTE")`)
- **Parâmetros**: Header `X-Sessao-Carrinho: <uuid-sessao>` (ou query param `?sessaoId=<uuid-sessao>`)
- **Respostas**:
  - `200 OK`: `CarrinhoResponse` completo do cliente já mesclado com `memoriaCalculo` recalculada. (Se a sessão informada não existir ou estiver vazia, retorna normalmente o carrinho atual do cliente — operação idempotente).
  - `401 Unauthorized`: JSON sem `WWW-Authenticate` se não autenticado.
  - `403 Forbidden`: Se autenticado sem perfil `CLIENTE` (ex.: apenas `ADMIN`).

### 2.4 `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento` (Módulo 3 — Modo Demo `Q2`)
Permite que o dono autenticado do pedido (ou `ADMIN`), exclusivamente quando o modo demonstração está habilitado no servidor, simule a confirmação ou recusa do pagamento sem que o navegador precise conhecer ou enviar `X-Gateway-Token`.

- **Autenticação**: Obrigatória (`CLIENTE` dono do pedido ou `ADMIN`)
- **Pré-condição de ambiente**: `ecommerce.modulo3.seed-demo-ativo=true`
- **Path Variable**: `id` (`UUID` do pedido)
- **Request Body**:
```json
{
  "sucesso": true,
  "motivoRecusa": null
}
```
- **Respostas**:
  - `200 OK` (primeira confirmação):
```json
{
  "statusProcessamento": "PROCESSADO_SUCESSO",
  "pedidoId": "...",
  "numeroPedido": "PED-2026-0001",
  "novoStatusPedido": "PAGO",
  "mensagem": "Pagamento confirmado com sucesso.",
  "idempotenteReexecutado": false
}
```
  - `200 OK` (repetição / teste de idempotência): mesmo payload com `"idempotenteReexecutado": true`.
  - `403 Forbidden`: Quando `modoDemonstracaoAtivo == false` ou quando o usuário `CLIENTE` autenticado tenta simular pedido de outro cliente.
  - `404 Not Found`: Pedido inexistente.
  - `400 Bad Request` / `409 Conflict`: Estado atual do pedido não permite transição de pagamento.

### 2.5 `GET /api/v1/admin/pedidos` (Módulo 3 — Administrativo `FR-009`)
Lista todos os pedidos da loja em ordem cronológica decrescente (`criadoEm DESC`) para a aba de expedição e monitoramento administrativo.

- **Autenticação**: Obrigatória (`ADMIN` ou `OPERADOR_COMERCIAL`)
- **Query Param opcional**: `?status=AGUARDANDO_PAGAMENTO|PAGO|EM_PREPARACAO|ENVIADO|ENTREGUE|CANCELADO|PAGAMENTO_RECUSADO`
- **Respostas**:
  - `200 OK`: `List<PedidoResponseDTO>` ordenada do pedido mais recente para o mais antigo.
  - `401 Unauthorized` / `403 Forbidden`: Conforme perfil RBAC.

---

## 3. Mapa de Integração dos Módulos JavaScript (Front-end -> API Existente)

| Módulo JS | Papel Arquitetural | Endpoints Existentes + Novos Consumidos |
|-----------|--------------------|------------------------------------------|
| `js/api-client.js` | **Facade / HTTP Proxy**: encapsula `fetch`, injeta `Authorization: Basic` + `X-Sessao-Carrinho`, trata `401` sem popup e normaliza payloads de erro `{codigo, mensagem}` | Todos `/api/v1/**` |
| `js/store.js` | **Observer Store**: estado global reativo + persistência de `sessaoCarrinhoId` (`localStorage`) e sessão autenticada (`sessionStorage`) | N/A (estado local) |
| `js/auth-cliente.js` | **Gestão de Identidade, Acesso Rápido Demo & Conta do Cliente**: login real, auto-merge pós-login, cadastro de cliente, troca de vista e CRUD de endereços com tratamento de `possuiEnderecoValido === false` | `GET /api/v1/loja/configuracao`, `POST /api/v1/auth/clientes/cadastro`, `POST /api/v1/auth/login`, `POST /api/v1/carrinho/mesclar`, `GET /api/v1/clientes/me`, `POST /api/v1/clientes/me/enderecos`, `PUT /api/v1/clientes/me/enderecos/{id}/selecionar`, `DELETE /api/v1/clientes/me/enderecos/{id}` |
| `js/catalogo.js` | **Vitrine, Árvore Recursiva e Banners**: renderiza árvore recursiva de categorias (`Composite`), busca textual, banners de promoções ativas, seletor de variações/atributos e estoque por SKU em tempo real | `GET /api/v1/promocoes/ativas`, `GET /api/v1/catalogo/categorias/arvore`, `GET /api/v1/catalogo/itens?categoriaId=`, `GET /api/v1/catalogo/itens/{sku}`, `GET /api/v1/estoque/{sku}` |
| `js/carrinho.js` | **Drawer Interativo do Carrinho e Extrato**: adicionar/alterar/remover SKU (anônimo ou logado), aplicar/remover cupom, barra de frete grátis e memória detalhada de descontos | `GET /api/v1/carrinho`, `POST /api/v1/carrinho/itens`, `PUT /api/v1/carrinho/itens/{sku}`, `DELETE /api/v1/carrinho/itens/{sku}`, `DELETE /api/v1/carrinho`, `POST /api/v1/carrinho/cupom`, `DELETE /api/v1/carrinho/cupom` |
| `js/checkout-pedidos.js` | **Checkout Guiado, Timeline e Pedidos**: cálculo de frete (`CORREIOS_PAC` elegível a frete grátis, `CORREIOS_SEDEX`, `TRANSPORTADORA_EXPRESSA`), seleção de pagamento (`PIX`, `CARTAO_CREDITO` com `numeroCartaoFicticio` — final `0000` recusado —, `titularCartao` e `chaveIdempotenciaCliente` opcional, `BOLETO`), fechamento de pedido, timeline visual de estados, snapshot comercial, botão de simulação de pagamento (`idempotenteReexecutado`) e cancelamento | `POST /api/v1/frete/calcular`, `POST /api/v1/checkout`, `GET /api/v1/pedidos/me`, `GET /api/v1/pedidos/{id}`, `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento`, `DELETE /api/v1/pedidos/{id}/cancelamento` |
| `js/admin.js` | **Painel RBAC Dinâmico (`MAPA_ABAS_RBAC`)**: mostra apenas abas permitidas ao perfil autenticado, opera expedição, catálogo, estoque, promoções/cupons, operadores, auditoria e relatórios | `GET /api/v1/admin/pedidos`, `PATCH /api/v1/admin/pedidos/{id}/{preparar\|despachar\|entregar}`, `POST /api/v1/admin/catalogo/{categorias\|produtos\|combos}`, `PATCH /api/v1/admin/catalogo/itens/{sku}/{preco\|status}`, `POST /api/v1/admin/estoque/{sku}/ajustes`, `POST /api/v1/admin/estoque/reservas/expirar-vencidas`, `GET/POST/PATCH /api/v1/admin/promocoes/**`, `GET/POST /api/v1/admin/cupons`, `POST /api/v1/admin/usuarios/operadores`, `GET /api/v1/admin/auditoria`, `GET /api/v1/admin/relatorios/*` |
| `js/main.js` | **Bootstrap & Roteamento Modular**: carrega configuração pública, inicializa módulos em paralelo, coordena navegação topo (`Vitrine`, `Minha Conta`, `Checkout`, `Painel Admin`) e toasts | Orquestração dos módulos |
