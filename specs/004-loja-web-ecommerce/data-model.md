# Modelo de Dados, Estado da Aplicação e DTOs — Portal Web E-commerce

**Feature**: `004-loja-web-ecommerce` | **Data**: 2026-11-12 | **Spec**: [spec.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/004-loja-web-ecommerce/spec.md)

## 1. Impacto no Banco de Dados (Migrations Flyway)

**Nenhuma nova migration (`V4__...`) é necessária.** Todas as tabelas relacionais requeridas já existem nas migrations vigentes:
- `V1`: usuários, perfis, clientes, endereços, categorias hierárquicas, produtos, atributos Flyweight, combos/kits, saldos/reservas de estoque e trilha de auditoria;
- `V2`: carrinhos (`cliente_id`, `sessao_id`, `cupom_codigo`), itens de carrinho, campanhas promocionais e cupons;
- `V3`: pedidos, snapshots financeiros, itens congelados, transações de pagamento idempotentes e eventos de entrega.

As extensões do back-end operam exclusivamente por **projeções de leitura**, **regras transacionais de aplicação sobre agregados já persistidos** (mesclagem de carrinho) e **orquestração segura de caso de uso já existente** (simulação de confirmação de pagamento).

---

## 2. Novos DTOs e Projeções de API (Back-end Java 21)

### 2.1 `CampanhaPublicaResponseDTO` (Módulo 2 — Leitura Pública)
Record imutável exposto por `GET /api/v1/promocoes/ativas` representando apenas metadados comerciais seguros para banners e selos da vitrine (sem dados internos de auditoria administrativa):

| Campo | Tipo | Descrição / Regra |
|-------|------|-------------------|
| `id` | `UUID` | Identificador da campanha |
| `nome` | `String` | Título curto exibido no banner da vitrine |
| `descricao` | `String` | Descrição explicativa das condições da oferta |
| `tipoMecanica` | `TipoMecanicaPromocao` | Tipo da regra (`LEVE_X_PAGUE_Y`, `PERCENTUAL_CONDICIONAL`, `VALOR_FIXO_META_CARRINHO`, `COMBO_CRUZADO_CASADO`) |
| `descricaoMecanica` | `String` | Rótulo amigável de `tipoMecanica.getDescricaoExibicao()` para selo do banner |
| `prioridade` | `int` | Ordem de prioridade descendente usada nos banners |
| `cumulativa` | `boolean` | Indica se a promoção combina com outras campanhas/cupons |
| `dataInicio` | `Instant` | Início da vigência da campanha |
| `dataFim` | `Instant` | Término da vigência (opcional/`null` se indeterminada) |

**Filtro de exibição pública**: retorna apenas campanhas cujo `ativa == true` e cuja janela temporal (`dataInicio`/`dataFim`) esteja vigente em `Instant.now()`.

### 2.2 `ConfiguracaoLojaPublicaDTO` (Compartilhado — Configuração Pública da Vitrine)
Record imutável exposto por `GET /api/v1/loja/configuracao` para parametrizar comportamentos condicionais da interface web sem vazar credenciais ou tokens:

| Campo | Tipo | Origem / Regra |
|-------|------|----------------|
| `modoDemonstracaoAtivo` | `boolean` | `true` apenas quando `ecommerce.modulo3.seed-demo-ativo` estiver ativo (alinhado com `SimulacaoPagamentoDemoController`; em perfil `test`/produção = `false`) |
| `freteGratisValorMinimo` | `BigDecimal` | Valor mínimo configurado em `ecommerce.modulo3.frete-gratis.valor-minimo` (padrão `250.00`) |
| `freteGratisPesoMaximoGramas` | `int` | Peso limite configurado em `ecommerce.modulo3.frete-gratis.peso-maximo-gramas` (padrão `5000`) |

### 2.3 `SimulacaoPagamentoDemoRequestDTO` (Módulo 3 — Simulação Controlada em Demo)
Payload de entrada de `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento`:

| Campo | Tipo | Validação | Descrição |
|-------|------|-----------|-----------|
| `sucesso` | `Boolean` | `@NotNull` | `true` simula aprovação do pagamento (`PAGO`); `false` simula recusa (`PAGAMENTO_RECUSADO`) |
| `motivoRecusa` | `String` | Opcional | Mensagem explicativa gravada quando `sucesso = false` (padrão amigável se omitida) |

**Conversão interna no servidor**: o controlador/serviço constrói internamente `ConfirmacaoPagamentoRequestDTO` com:
- `chaveOperacaoPedido = pedido.getNumeroPedido()`
- `idTransacaoGateway = "DEMO-SIM-" + pedido.getNumeroPedido()`
- `sucesso = requisicao.sucesso()`
- `valorPago = Boolean.TRUE.equals(requisicao.sucesso()) ? pedido.getFinanceiro().getValorTotalLiquido() : null`
- `motivoRecusa = requisicao.motivoRecusa()`
e delega ao `ServicoProcessamentoPagamento.processarNotificacaoGateway(...)`, preservando integralmente a chave idempotente `WEBHOOK:<numeroPedido>` e retornando `ConfirmacaoPagamentoResponseDTO` (`idempotenteReexecutado = true` na segunda execução).

### 2.4 DTOs Reutilizados sem Alteração Estrutural
- **`CarrinhoResponse` + `MemoriaCalculoPrecoVO`** (Módulo 2): retorno tanto das operações normais do carrinho quanto da nova ação `POST /api/v1/carrinho/mesclar`.
- **`PedidoResponseDTO`** (Módulo 3): retorno de `GET /api/v1/admin/pedidos` com todos os snapshots comerciais imutáveis, status atual e descrição amigável, itens e evento de entrega.

---

## 3. Regras de Estado da Mesclagem de Carrinho (`Q1`)

```
[Visitante Anônimo (X-Sessao-Carrinho = S)]
   ├─ itens: { SKU_A: 1, SKU_B: 2 }
   └─ cupom: "BEMVINDO10" (opcional)
            │
            │  POST /api/v1/auth/login (200 OK)
            ▼
[POST /api/v1/carrinho/mesclar] (Cliente autenticado + X-Sessao-Carrinho: S)
   ├─ 1. Localiza carrinho de sessão S; se inexistente ou vazio (+ sem cupom), retorna carrinho do cliente C.
   ├─ 2. Para cada item do carrinho S:
   │      • Valida SKU ativo no catálogo do Módulo 1 (PortaConsultaCatalogoCliente).
   │      • Soma quantidade ao carrinho do cliente C via adicionarOuIncrementarItem(...).
   ├─ 3. Se carrinho C não possui cupom E carrinho S possui cupom:
   │      • Revalida vigência e valor mínimo do cupom com novo subtotal de C; se válido, aplica ao carrinho C.
   ├─ 4. Limpa itens e exclui o carrinho anônimo S para evitar duplicação futura.
   └─ 5. Retorna CarrinhoResponse atualizado de C (com memória de cálculo recalculada).
```

---

## 4. Modelo de Estado no Front-end (`EstadoLoja` — JavaScript Modular)

O módulo central `js/store.js` implementa um **Store Reativo (Padrão Observer)** mantendo estado único previsível e assinaturas desacopladas para cada painel da interface:

| Chave de Estado | Tipo / Estrutura | Persistência | Responsável pela Atualização |
|-----------------|------------------|--------------|------------------------------|
| `configuracao` | `{ modoDemonstracaoAtivo, freteGratisValorMinimo, freteGratisPesoMaximoGramas }` | Em memória (boot) | `main.js` via `GET /api/v1/loja/configuracao` |
| `sessaoCarrinhoId` | `string` (UUID v4 gerado no browser) | `localStorage` (`ecommerce_sessao_carrinho`) | `api-client.js` |
| `autenticacao` | `{ autenticado, usuarioId, email, perfis[], tokenBasicBase64 } \| null` | `sessionStorage` (`ecommerce_auth`) | `auth-cliente.js` (limpo no logout ou `401`) |
| `catalogo` | `{ arvoreCategorias[], itens[], itensMapaPorSku, categoriaSelecionadaId, termoBusca, estoquePorSku }` | Em memória | `catalogo.js` |
| `promocoesAtivas` | `CampanhaPublicaResponseDTO[]` | Em memória | `catalogo.js` |
| `carrinho` | `{ carrinhoId, quantidadeTotalItens, pesoTotalGramas, itens[], cupomCodigo, memoriaCalculo }` | Em memória (sincronizado com servidor) | `carrinho.js` |
| `clientePerfil` | `{ clienteId, nomeCompleto, email, enderecos, enderecoSelecionado: { possuiEnderecoValido, cep, uf, resumoFormatado } }` | Em memória (quando perfil `CLIENTE`) | `auth-cliente.js` / `checkout-pedidos.js` |
| `pedidosCliente` | `PedidoResponseDTO[]` | Em memória | `checkout-pedidos.js` |
| `visaoAtiva` | `'vitrine' \| 'conta' \| 'checkout' \| 'admin'` | URL hash / memória | `main.js` |

### Tratamento Explícito do Null Object de Endereço na Interface (`FR-005`)
Quando `clientePerfil.enderecoSelecionado.possuiEnderecoValido === false` (estado correspondente ao `EnderecoAusenteNullObject` do Módulo 1), a interface **não** falha nem exibe campos `undefined`: apresenta um card de aviso orientativo em português exibindo `enderecoSelecionado.resumoFormatado` e botão direto para selecionar ou cadastrar um endereço antes de liberar a confirmação do checkout.

### Mapeamento Declarativo RBAC no Painel Administrativo (`FR-009` & Princípio I)
Em vez de cadeias `if/else` acopladas ao HTML, `admin.js` utiliza uma tabela declarativa (`MAPA_ABAS_RBAC`) associando cada aba administrativa aos perfis autorizados e à função renderizadora:

| Aba Admin | Perfis Autorizados | API Consumida |
|-----------|--------------------|---------------|
| `expedicao` ("Pedidos & Expedição") | `ADMIN`, `OPERADOR_COMERCIAL` | `GET /api/v1/admin/pedidos`, `PATCH .../preparar\|despachar\|entregar` |
| `catalogo-admin` ("Catálogo & Preços") | `ADMIN`, `OPERADOR_COMERCIAL` | `POST /api/v1/admin/catalogo/{categorias\|produtos\|combos}`, `PATCH .../preco\|status` |
| `estoque-admin` ("Estoque & Reservas") | `ADMIN`, `RESPONSAVEL_ESTOQUE` | `GET /api/v1/estoque/{sku}`, `POST /api/v1/admin/estoque/{sku}/ajustes`, `POST .../expirar-vencidas` |
| `promocoes-admin` ("Promoções & Cupons") | `ADMIN`, `RESPONSAVEL_PROMOCOES` | `GET/POST/PATCH /api/v1/admin/promocoes/**`, `GET/POST /api/v1/admin/cupons` |
| `operadores-admin` ("Operadores & Auditoria") | `ADMIN` | `POST /api/v1/admin/usuarios/operadores`, `GET /api/v1/admin/auditoria` |
| `relatorios-admin` ("Relatórios Gerenciais") | `ADMIN` | `GET /api/v1/admin/relatorios/{vendas-consolidadas\|ticket-medio\|produtos-mais-vendidos\|cancelamentos}` |
