# Implementation Plan: Redesign Profissional do Portal Web e Modo Escuro

**Branch**: `005-redesign-portal-dark-mode` | **Date**: 2026-11-16 | **Spec**: [spec.md](file:///c:/Coisas%20Importantes/Escola/FATEC/Trabalho%20Benio/specs/005-redesign-portal-dark-mode/spec.md)

**Input**: Feature specification from `/specs/005-redesign-portal-dark-mode/spec.md`

## Summary

Elevar o padrão visual e a apresentação pública do Portal Web da **FATEC Store** (`src/main/resources/static/`) adotando a direção estética **Tech Store Contemporânea** com suporte completo e persistente a **Modo Claro / Modo Escuro (Dark Mode)** em 1 clique e detecção automática inicial do sistema operacional. A arquitetura mantém 100% da entrega estática nativa pelo Spring Boot 3.4.5 (HTML5 + CSS3 com Design Tokens semânticos + ES Modules puros, sem Node.js/build externo), preserva integralmente todos os seletores funcionais e contratos REST dos Módulos 1 a 4 e garante contraste WCAG AA em todos os componentes, tabelas, alertas e emblemas de status.

## Technical Context

**Language/Version**: HTML5, CSS3 (Custom Properties, Grid, Flexbox, `color-scheme`), JavaScript ES2022+ (ES Modules nativos) no front-end; Java 21 + Spring Boot 3.4.5 no servidor hospedeiro estático  
**Primary Dependencies**: Zero dependências externas adicionais no front-end; Spring Boot Web/Security/Data JPA/H2/Flyway existentes no back-end  
**Storage**: `window.localStorage` (`fatec_store_tema_v1` para preferência de tema e `X-Sessao-Carrinho` para carrinho visitante) + H2/Flyway no servidor (inalterado)  
**Testing**: JUnit 5 + MockMvc (`.\mvnw.cmd clean test`, incluindo `PortalWebEEndpointsPublicosTest`), validação sintática JS (`node --check`) e automação headless em navegador real via Microsoft Edge CDP (`http://localhost:8080/`, light/dark + mobile 375px + fluxos E2E)  
**Target Platform**: Navegadores modernos desktop e mobile (`>= 360px` de largura) servidos diretamente pelo Spring Boot em `localhost:8080`  
**Project Type**: Aplicação web monolítica modular com front-end estático integrado em `src/main/resources/static/`  
**Performance Goals**: Resolução inicial de tema síncrona em `<head>` antes da primeira pintura (zero FOUC), alternância visual instantânea (< 100 ms) sem recarregar a página e ausência de requests externos de imagens/fontes bloqueantes  
**Constraints**: Zero etapa externa de compilação/empacotamento (sem Vite/Webpack/Tailwind CLI); zero alteração de contratos REST ou regras de domínio Java; paridade WCAG AA (>= 4,5:1 texto normal e >= 3:1 controles/elementos grandes) nos dois temas; preservação total dos seletores/IDs funcionais e compatibilidade com `prefers-reduced-motion`  
**Scale/Scope**: 1 shell SPA (`index.html`), 3 folhas CSS (`tokens.css`, `layout.css`, `components.css`), até 8 módulos JS (`store.js`, `main.js`, `catalogo.js`, `carrinho.js`, `auth-cliente.js`, `checkout-pedidos.js`, `admin.js`, `api-client.js`) cobrindo 4 visões principais, 2 drawers/modais globais, 5 perfis RBAC e 8 status de pedido

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio da Constituição | Aderência e Impacto desta Feature (`005`) | Status |
|---|---|---|
| **I. OCP & Design Patterns** | O sistema de tema utiliza tokens semânticos polimórficos por `[data-theme]` em CSS e eventos desacoplados via *Observer* (`store.js`), permitindo adicionar novos temas sem condicionais espalhadas no layout. | `PASS` |
| **II. Catálogo de Padrões** | Preserva *Observer* (`store.js`), *Facade* (`api-client.js`), *Table-Driven Dispatch* (`admin.js`) e todos os padrões GoF dos Módulos 1–3 sem criar classes artificiais. | `PASS` |
| **III. Código Explícito & Documentação** | Nenhum código Java novo de negócio é exigido; eventuais ajustes em testes Java manterão Javadoc completo, e todas as novas funções JS (ilustrações, tema, hero) terão JSDoc completo em português descrevendo objetivo, papel arquitetural, parâmetros e retorno. | `PASS` |
| **IV. Integridade, Idempotência e Imutabilidade** | Fluxos de carrinho, snapshot imutável, reserva atômica e simulação idempotente de pagamento não são alterados e seguem integralmente cobertos por testes. | `PASS` |
| **V. Camadas, Segurança e Auditoria** | Separação intacta; `ConfiguracaoSegurancaHttp` e `EntradaAutenticacaoRestSemPopup` não sofrem alteração; preferência de tema armazena exclusivamente `'light'`/`'dark'` sem dados sensíveis. | `PASS` |
| **VI. Testes & Documentação** | Mantém e revalida toda a suíte JUnit 5/MockMvc (`.\mvnw.cmd clean test`) acrescida de verificação E2E real em ambos os temas, responsividade e ausência de regressões visuais/funcionais. | `PASS` |

**Post-Design Gate Re-check (após Phase 1)**: Todos os artefatos (`research.md`, `data-model.md`, `contracts/ui-theme-and-components.md`, `quickstart.md`) confirmam 100% de conformidade sem necessidade de exceções em *Complexity Tracking*.

## Project Structure

### Documentation (this feature)

```text
specs/005-redesign-portal-dark-mode/
├── spec.md                                 # Especificação funcional e esclarecimentos (/speckit-specify + /speckit-clarify)
├── plan.md                                 # Este plano técnico (/speckit-plan)
├── research.md                             # Decisões arquiteturais, contraste WCAG AA e estratégia anti-FOUC
├── data-model.md                           # Modelo de estado do tema, hero e inventário completo de tokens semânticos
├── quickstart.md                           # Roteiro executável de validação claro/escuro/responsividade
├── checklists/
│   └── requirements.md                     # Checklist de qualidade da especificação (16/16 aprovados)
├── contracts/
│   └── ui-theme-and-components.md          # Contrato DOM, tema, acessibilidade e retrocompatibilidade
└── tasks.md                                # Lista de tarefas acionáveis (gerada posteriormente por /speckit-tasks)
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/br/edu/fatec/ecommerce/        # Sem alterações funcionais necessárias (hospedagem estática e REST preservados)
│   └── resources/static/
│       ├── index.html                      # Script síncrono anti-FOUC em <head>, botão #btn-alternar-tema e seção #vitrine-hero
│       ├── css/
│       │   ├── tokens.css                  # Sistema completo de Design Tokens para :root[data-theme="light"] e [data-theme="dark"]
│       │   ├── layout.css                  # Grid/sticky header refinados, hero editorial, responsividade e movimento reduzido
│       │   └── components.css              # Cards ilustrados, skeletons, alertas, badges de status e tabelas com tokens puros
│       └── js/
│           ├── store.js                    # Estado de tema, persistência localStorage ('fatec_store_tema_v1') e sync matchMedia
│           ├── main.js                     # Binding do #btn-alternar-tema, atalhos do hero e atualização acessível (aria-pressed/ícone)
│           ├── catalogo.js                 # Controle de visibilidade do #vitrine-hero em filtros, skeletons e SVGs em .product-card-media
│           ├── carrinho.js                 # Refinamento visual de drawer, barra de frete grátis e memória de cálculo (se necessário)
│           ├── auth-cliente.js             # Refinamento visual de perfis, endereços e avisos em ambos os temas (se necessário)
│           ├── checkout-pedidos.js         # Refinamento visual da timeline de estados, cartões de entrega e confirmação pós-compra
│           ├── admin.js                    # Alinhamento tabular numérico/monetário e refinamento de abas/ações RBAC
│           └── api-client.js               # Inalterado
└── test/
    └── java/br/edu/fatec/ecommerce/modulo4/
        └── PortalWebEEndpointsPublicosTest.java  # Opcional: estender assertivas de presença de tokens/tema/hero entregues em / e /css/tokens.css
```

**Structure Decision**: Toda a evolução visual e de tema concentra-se na camada estática já servida pelo Spring Boot em `src/main/resources/static/`, respeitando a organização modular existente (`tokens.css` → `layout.css` → `components.css` e ES Modules por domínio) sem criar diretórios paralelos nem alterar a estrutura Java.

## Complexity Tracking

Nenhuma violação à Constituição identificada; tabela omitida por estar em total conformidade.
