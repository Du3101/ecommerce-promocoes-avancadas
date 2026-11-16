# Data Model & Visual Tokens: Redesign Portal Web e Modo Escuro (`005`)

**Feature**: `specs/005-redesign-portal-dark-mode/spec.md`  
**Scope**: Estado de tema no front-end e catálogo semântico de Design Tokens CSS (sem alterações de schema relacional/JPA no servidor).

---

## 1. Entidades de Estado no Front-end (`store.js` & `<head>`)

### 1.1 `PreferenciaTema` (Estado de Aparência)

| Campo | Tipo | Origem / Persistência | Descrição |
|---|---|---|---|
| `tema` | `'light' \| 'dark'` | DOM `<html data-theme="...">` | Tema visual atualmente aplicado em toda a interface |
| `origem` | `'manual' \| 'sistema'` | Derivado de `localStorage['fatec_store_tema_v1']` | Indica se o usuário escolheu manualmente ou segue o dispositivo |
| `chavePersistencia` | `string` (`'fatec_store_tema_v1'`) | `window.localStorage` | Grava `'light'` ou `'dark'` apenas após clique explícito no alternador |

**Regras de transição**:
1. **Inicialização (`<head>` inline + bootstrap)**:
   - Se `localStorage['fatec_store_tema_v1']` for `'light'` ou `'dark'` → aplica esse valor (`origem = 'manual'`).
   - Caso contrário → consulta `window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'` (`origem = 'sistema'`).
2. **Mudança de tema do sistema operacional (`matchMedia` event `'change'`)**:
   - Se `origem === 'sistema'` (sem escolha manual gravada) → atualiza `data-theme` e notifica o store (`TEMA`).
   - Se `origem === 'manual'` → ignora a mudança do sistema mantendo a preferência explícita do usuário.
3. **Clique no botão `#btn-alternar-tema`**:
   - Calcula `proximoTema = temaAtual === 'dark' ? 'light' : 'dark'`.
   - Persiste `localStorage.setItem('fatec_store_tema_v1', proximoTema)`, atualiza `<html data-theme>` e publica evento `'TEMA'` no `store.js`.

### 1.2 `EstadoVisualHeroVitrine` (Estado Derivado da Vitrine)

| Campo | Tipo | Derivação | Regra |
|---|---|---|---|
| `heroVisivel` | `boolean` | `!estado.categoriaSelecionadaId && !estado.termoBusca` | Exibido na vitrine completa; oculto (`hidden = true`) durante busca textual ou filtragem de categoria (`FR-005`) |

---

## 2. Catálogo Semântico de Design Tokens (`css/tokens.css`)

| Categoria | Tokens CSS Principais | Modo Claro (`data-theme="light"`) | Modo Escuro (`data-theme="dark"`) |
|---|---|---|---|
| **Base & Superfícies** | `--color-bg`, `--color-surface`, `--color-surface-alt`, `--color-surface-elevated` | `#f3f6fb`, `#ffffff`, `#f8fafc`, `#ffffff` | `#0b1120`, `#111827`, `#1e293b`, `#172033` |
| **Cabeçalho & Sobreposições** | `--color-header-bg`, `--color-backdrop`, `--color-topbar-bg`, `--color-topbar-fg` | `rgba(255,255,255,0.92)`, `rgba(15,23,42,0.48)`, gradiente `#0f172a → #1e3a8a`, `#f8fafc` | `rgba(11,17,32,0.92)`, `rgba(2,6,23,0.72)`, gradiente `#020617 → #0f172a`, `#e2e8f0` |
| **Bordas & Anéis de Foco** | `--color-border`, `--color-border-strong`, `--color-focus-ring` | `#e2e8f0`, `#cbd5e1`, `rgba(37, 99, 235, 0.25)` | `#273449`, `#3b4c68`, `rgba(56, 189, 248, 0.35)` |
| **Tipografia & Contraste** | `--color-text`, `--color-text-muted`, `--color-text-subtle` | `#0f172a`, `#475569`, `#526075` | `#f1f5f9`, `#cbd5e1`, `#94a3b8` |
| **Marca & Acento** | `--color-primary`, `--color-primary-hover`, `--color-primary-contrast`, `--color-primary-soft` | `#2563eb`, `#1d4ed8`, `#ffffff`, `#eff6ff` | `#38bdf8`, `#7dd3fc`, `#082f49`, `rgba(56, 189, 248, 0.14)` |
| **Heróis & Promoções** | `--color-hero-bg`, `--color-hero-border`, `--color-banner-gradient`, `--color-banner-fg` | Gradiente suave `#eff6ff → #e0e7ff`, `#c7d2fe`, `#1d4ed8 → #4338ca`, `#ffffff` | Gradiente `#0f172a → #162447`, `#1e3a5f`, `#0369a1 → #3730a3`, `#f8fafc` |
| **Feedback Semântico** | `--color-success*`, `--color-warning*`, `--color-danger*`, `--color-info*`, `--color-purple*`, `--color-neutral*` | Fundos pastéis claros + tintas escuras saturadas | Fundos escuros translúcidos/tintados + textos luminosos (>= 6,5:1) |
| **8 Estados `StatusPedido`** | `--status-{criado,aguardando,pago,preparacao,enviado,entregue,cancelado,recusado}-{bg,fg}` | Mantém paridade visual de cores distintas no claro | Adapta bg escuro + fg claro com distinção inequívoca |
| **Mídia de Cards & Skeletons** | `--color-media-bg`, `--color-media-combo-bg`, `--color-skeleton-base`, `--color-skeleton-shine` | `#eef2ff` / `#f5f3ff`, `#e2e8f0`, `#f1f5f9` | `#162033` / `#241b42`, `#1e293b`, `#2b3950` |
| **Sombras & Profundidade** | `--shadow-sm`, `--shadow-md`, `--shadow-lg` | Sombras sutis azul-ardósia `0.05..0.14` | Sombras mais profundas `0.32..0.55` + bordas definidas |

---

## 3. Taxonomia de Ilustrações Vetoriais (`CatalogoIlustracaoTipo`)

Derivada deterministicamente no cliente a partir de `item.eComboKit`, `item.nomeExibicao` e `item.sku`:

| Categoria Visual | Regra de Classificação | Estilo Temático |
|---|---|---|
| `combo` | `item.eComboKit === true` | Ilustração de pacote/setup múltiplo com acento roxo/ciano |
| `teclado` | Nome ou SKU contém `TEC`, `TECLADO` ou `KEY` | Silhueta vetorial estilizada de teclado mecânico |
| `mouse` | Nome ou SKU contém `MOU`, `MOUSE` | Silhueta vetorial ergonômica de mouse gamer/precisão |
| `audio` | Nome ou SKU contém `HEAD`, `FONE`, `AUDIO` | Silhueta de headset com arco e conchas acústicas |
| `monitor` | Nome ou SKU contém `MON`, `TELA`, `DISPLAY` | Silhueta de monitor widescreen com base |
| `hardware` | Demais SKUs/categorias | Ícone geométrico tech minimalista (chip/periférico) |
