# Research & Design Decisions: Redesign Profissional do Portal Web e Modo Escuro (`005`)

**Feature**: `specs/005-redesign-portal-dark-mode/spec.md`  
**Date**: 2026-11-16  
**Status**: Concluído (Phase 0)

---

## 1. Gerenciamento de Tema Claro / Escuro sem Flash Visual (FOUC)

- **Decision**: Utilizar o atributo `data-theme="light" | "dark"` no elemento raiz (`<html>`), combinado com a propriedade CSS `color-scheme: light | dark`, uma chave dedicada em `localStorage` (`fatec_store_tema_v1`), um script síncrono mínimo no `<head>` de `src/main/resources/static/index.html` e sincronização reativa centralizada em `src/main/resources/static/js/store.js` + `js/main.js`.
- **Rationale**:
  - Como `index.html` carrega os scripts ES Modules ao final do `<body>` (`<script type="module" src="/js/main.js">`), depender apenas do módulo JS para aplicar `data-theme` causaria um flash de tela clara antes da execução do módulo quando o dispositivo ou o usuário prefere o modo escuro (`FR-003`).
  - O micro-script inline no `<head>` resolve em menos de 1 ms: lê `localStorage.getItem('fatec_store_tema_v1')`; se for `'light'` ou `'dark'`, aplica imediatamente `document.documentElement.dataset.theme`; caso contrário, consulta `window.matchMedia('(prefers-color-scheme: dark)').matches` (`FR-002`).
  - Em `js/store.js`, quando não há valor manual salvo (`origemTema === 'sistema'`), um listener `matchMedia('(prefers-color-scheme: dark)').addEventListener('change', ...)` atualiza o tema ao vivo caso o sistema operacional mude; ao acionar o botão no cabeçalho (`FR-001`), grava a escolha manual em `localStorage` (`origemTema = 'manual'`) e alterna diretamente `light ↔ dark` em 1 clique.
- **Alternatives considered**:
  - *Apenas `@media (prefers-color-scheme: dark)` sem `data-theme`*: impediria alternância manual pelo usuário no cabeçalho.
  - *Ciclo de 3 estados (Sistema → Claro → Escuro)*: descartado na sessão de esclarecimentos por adicionar cliques extras e complexidade desnecessária ao botão do cabeçalho.

---

## 2. Sistema de Tokens Semânticos e Garantia de Contraste WCAG AA

- **Decision**: Adotar a direção estética **Tech Store Contemporânea** em `src/main/resources/static/css/tokens.css`, definindo dois conjuntos completos de tokens semânticos (`:root` / `:root[data-theme="light"]` e `:root[data-theme="dark"]`) e eliminando todas as cores hexadecimais/RGBA hardcoded encontradas na auditoria de `layout.css` (linhas 37, 38, 49, 226) e `components.css` (badges neutros, alertas, linhas de memória de cálculo e trilha de timeline).
- **Rationale**:
  - Na auditoria técnica da base atual (`tokens.css` de 68 linhas), `layout.css` possuía fundo translúcido branco fixo no `.site-header` e `components.css` possuía bordas/textos hardcoded em claro para `.alert-*`, `.badge-neutral`, `.timeline-step` e `.breakdown-*`.
  - Paleta planejada e contraste verificado:
    - **Modo Claro (`data-theme="light"`)**:
      - Fundo base `--color-bg: #f3f6fb`, cartão `--color-surface: #ffffff`, superfície secundária `--color-surface-alt: #f8fafc`;
      - Texto principal `--color-text: #0f172a` (~16:1 sobre `#ffffff`), secundário `--color-text-muted: #475569` (~6.7:1), sutil `--color-text-subtle: #526075` (~5.5:1);
      - Acento de marca azul-índigo `--color-primary: #2563eb` (~5.17:1 com texto branco `--color-primary-contrast: #ffffff`), hover `#1d4ed8`.
    - **Modo Escuro (`data-theme="dark"`)**:
      - Fundo base `--color-bg: #0b1120` (ardósia profundo), cartão `--color-surface: #111827` (grafite elevado), superfície secundária `--color-surface-alt: #1e293b`, borda `--color-border: #273449`;
      - Texto principal `--color-text: #f1f5f9` (~15.4:1 sobre `#111827`), secundário `--color-text-muted: #cbd5e1` (~11.1:1), sutil `--color-text-subtle: #94a3b8` (~5.1:1 sobre `#1e293b` e ~6.4:1 sobre `#111827`, acima de 4,5:1);
      - Acento de marca azul-ciano luminoso `--color-primary: #38bdf8` (~9.4:1 sobre `#111827` para links/ícones/foco) pareado com `--color-primary-contrast: #082f49` para texto escuro sobre botões primários (~9.2:1 AAA), evitando falha de contraste de texto branco sobre ciano claro;
      - **Emblemas de Status (`StatusPedido` x 8) e Alertas**: fundos escuros sutis tintados (`#1e293b`, `#451a03`, `#064e3b`, `#0c4a6e`, `#2e1065`, `#052e16`, `#450a0a`) com textos pastéis luminosos (`#cbd5e1`, `#fcd34d`, `#6ee7b7`, `#7dd3fc`, `#c4b5fd`, `#86efac`, `#fda4af`) garantindo > 6,5:1 de contraste em todos os estados.
- **Alternatives considered**:
  - *Inversão automática via `filter: invert(1)`*: gera distorção de cores em botões, ícones e estados semânticos além de degradação de nitidez.
  - *Manter acento azul escuro no modo escuro*: reduz visibilidade em superfícies `#111827` e perde a sofisticação visual tech esperada.

---

## 3. Hero Editorial Compacto com Recolhimento Inteligente em Filtros

- **Decision**: Introduzir um bloco `#vitrine-hero` compacto no topo de `#view-vitrine` (em `index.html`), estilizado com composição editorial em dois blocos (proposta de valor + resumo ao vivo de campanhas/frete grátis e atalhos para catálogo/ofertas), controlado reativamente em `js/catalogo.js`: visível na vitrine completa e recolhido (`hidden = true`) sempre que `estado.categoriaSelecionadaId` ou `estado.termoBusca` estiverem ativos (`FR-005`).
- **Rationale**:
  - Atende `SC-005` dando presença de loja profissional logo na dobra inicial sem empurrar a grade de produtos para fora da tela.
  - Quando o usuário pesquisa ou filtra uma categoria, ocultar o hero coloca imediatamente os produtos filtrados no topo visível, tanto no desktop quanto em celulares `360px–375px`.
- **Alternatives considered**:
  - *Hero fixo permanente*: obriga rolagem repetida a cada pesquisa ou troca de categoria.

---

## 4. Ilustrações Vetoriais Temáticas nos Cartões de Produto e Combo

- **Decision**: Adicionar função pura determinística `obterIlustracaoCardHtml(item)` em `js/catalogo.js` rendendo SVGs inline nítidos dentro de `.product-card-media`, categorizados por tipo (`eComboKit`) e palavras-chave do nome/SKU (teclado, mouse, áudio/headset, monitor, hardware genérico), usando `currentColor` e variáveis CSS de acento/roxo sem requisições externas.
- **Rationale**:
  - Os DTOs de `/api/v1/catalogo/itens` não possuem URLs de foto e a arquitetura evita dependências externas frágeis (`Assumptions`).
  - SVGs estilizados com variáveis CSS se adaptam instantaneamente aos temas claro e escuro com nitidez vetorial em telas HiDPI/Retina e carregamento instantâneo.
- **Alternatives considered**:
  - *Cartões sem área visual ou dependências externas de stock images*: aparência genérica ou risco de imagem quebrada offline.

---

## 5. Preservação Estrita de Contratos, Seletores e Build Zero-Node

- **Decision**: Manter intactos todos os IDs HTML (`#btn-*`, `#view-*`, `#drawer-*`, `#modal-*`, `#checkout-*`, `#admin-*`) e atributos `data-*` usados pelos módulos JS e pelos testes E2E/MockMvc, realizando melhorias inteiramente dentro de `src/main/resources/static/` sem alterações em `src/main/java/` nem adição de ferramentas Node/build externo.
- **Rationale**:
  - Garante zero regressão nos 98 testes automatizados existentes e preservação completa da arquitetura sem build pipeline do projeto.
  - O arquivo `PortalWebEEndpointsPublicosTest.java` valida entrega de `/`, `/index.html`, `/css/tokens.css` e `/js/main.js`; todas as referências permanecem nos mesmos caminhos estáticos servidos pelo Spring Boot.
