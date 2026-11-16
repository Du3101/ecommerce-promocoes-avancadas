# Contratos de Interface Web e Tema (`005-redesign-portal-dark-mode`)

**Feature**: `specs/005-redesign-portal-dark-mode/spec.md`  
**Scope**: Contratos DOM, acessibilidade, persistência local e garantia de compatibilidade com módulos JS e endpoints REST existentes.

---

## 1. Contrato do Alternador e Estado de Tema (`FR-001`..`FR-004`)

### 1.1 Atributo Raiz e Persistência
- **Elemento raiz**: `document.documentElement` (`<html>`) recebe obrigatoriamente `data-theme="light"` ou `data-theme="dark"` antes da primeira renderização visível (via script inline síncrono em `<head>`) e atualizado reativamente a cada alternância.
- **Chave `localStorage`**: `fatec_store_tema_v1` (aceita exclusivamente `'light'` ou `'dark'`; valores inválidos são ignorados e tratados como preferência de sistema).
- **Resolução inicial**:
  1. Valor válido em `localStorage.getItem('fatec_store_tema_v1')`;
  2. Se ausente, `window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'`;
  3. Fallback padrão: `'light'`.

### 1.2 Controle no Cabeçalho (`index.html` + `js/main.js`)
- **ID reservado**: `#btn-alternar-tema` dentro de `.header-actions` em `src/main/resources/static/index.html`.
- **Acessibilidade obrigatória**:
  - `type="button"`
  - `aria-pressed="true" | "false"` (`true` quando `tema === 'dark'`)
  - `aria-label` e `title` descritivos atualizados dinamicamente (ex.: `"Ativar modo escuro"` no modo claro; `"Ativar modo claro"` no modo escuro)
  - Ícone SVG sol/lua dinâmico (`aria-hidden="true"`) + rótulo curto responsivo ocultável em larguras muito estreitas sem comprometer acessibilidade.

---

## 2. Contrato do Hero Editorial da Vitrine (`FR-005`)

- **ID reservado**: `#vitrine-hero` dentro de `#view-vitrine > .container`, posicionado logo acima de `.promo-banners-section` / `.vitrine-layout`.
- **Comportamento obrigatório de exibição**:
  - Exibido (`hero.hidden = false`) quando `estado.categoriaSelecionadaId === null && !estado.termoBusca`.
  - Ocultado (`hero.hidden = true`) automaticamente ao selecionar qualquer categoria ou submeter busca textual na vitrine; restaurado ao acionar `#btn-limpar-categoria`, `#btn-vitrine-limpar-filtros` ou limpar busca vazia.
- **Ações no Hero**:
  - Atalho primário para rolar/focar a grade de produtos (`#grade-produtos`) ou abrir as campanhas vigentes (`#lista-banners-promocoes`) sem introduzir novos endpoints.

---

## 3. Contrato Visual de Cartões, Estados de Carregamento e Vazios (`FR-006`, `FR-007`)

- **Área visual dos cards**: `.product-card-media` inserida como topo interno de `<article class="product-card" data-sku-card="...">`, contendo:
  - Ilustração SVG temática categorizada (`aria-hidden="true"`);
  - Mantém inalterados os blocos de badges (`.product-card-top`), título (`.product-title`), seletores de variação (`[data-grupo-key][data-variacao-sku]`), preço, e botões (`[data-add-sku]`, `[data-detalhe-sku]`).
- **Skeletons e Estados Vazios**:
  - Carregamentos assíncronos usam `.skeleton-card` estilizados via tokens (`--color-skeleton-base` e `--color-skeleton-shine`), com suporte a `@media (prefers-reduced-motion: reduce)`.
  - Estados vazios em Vitrine, Drawer do Carrinho, Minha Conta e Painel Admin utilizam `.empty-state-card` com contraste alinhado nos dois temas.

---

## 4. Contrato de Retrocompatibilidade (Zero Quebra)

1. **Endpoints REST**: nenhuma alteração em URLs, métodos HTTP, cabeçalhos (`Authorization`, `X-Sessao-Carrinho`) ou payloads JSON da API Spring Boot.
2. **Seletores preservados**: permanecem intactos todos os IDs e atributos `data-*` existentes em `index.html`, `catalogo.js`, `carrinho.js`, `auth-cliente.js`, `checkout-pedidos.js` e `admin.js` validados pelos testes automatizados e E2E da feature `004`.
3. **Zero Build Externo**: todos os recursos continuam entregues diretamente de `src/main/resources/static/` pelo Spring Boot sem dependência de Node.js/npm/Vite/Webpack em tempo de build ou execução.
