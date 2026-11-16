# Quickstart de Validação: Redesign Portal Web & Modo Escuro (`005`)

**Objetivo**: Validar que a elevação visual, o Modo Claro/Escuro e a responsividade funcionam integralmente em `http://localhost:8080/` sem regressão nos fluxos existentes ou nos testes automatizados.

---

## 1. Pré-requisitos e Execução

1. Rodar a suíte automatizada completa antes e depois das alterações:
   ```powershell
   .\mvnw.cmd clean test
   ```
   **Esperado**: `Tests run: 98` (ou mais, se novos checks forem adicionados), `Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
2. Subir o servidor Spring Boot completo (com portal estático servido em `src/main/resources/static/`):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. Acessar [http://localhost:8080/](http://localhost:8080/).

---

## 2. Cenários de Validação Ponta a Ponta

### Cenário A — Detecção de Tema do Sistema + Alternância 1-Clique e Persistência (`US1`)
1. Abrir uma janela anônima com emulação de tema escuro do sistema (`prefers-color-scheme: dark`) e sem chave `fatec_store_tema_v1` no `localStorage`:
   - **Esperado**: o site abre diretamente no Modo Escuro sem flash claro inicial; `document.documentElement.dataset.theme === "dark"` e `#btn-alternar-tema` possui `aria-pressed="true"`.
2. Clicar no botão `#btn-alternar-tema` no cabeçalho enquanto um modal ou o drawer do carrinho estiver aberto:
   - **Esperado**: transição instantânea para `data-theme="light"`, `localStorage.fatec_store_tema_v1 === "light"`, janela aberta permanece intacta.
3. Recarregar a página (`F5`):
   - **Esperado**: o tema selecionado manualmente (`light`) permanece ativo imediatamente ao carregar.

### Cenário B — Vitrine Moderna, Hero com Recolhimento e Cards Ilustrados (`US2`)
1. Na home sem filtros, verificar na dobra inicial: barra superior, cabeçalho equilibrado, `#vitrine-hero`, banners de promoções ativas e cards com `.product-card-media` exibindo ilustração vetorial temática + badges de tipo/estoque.
2. Selecionar uma categoria na árvore lateral ou buscar `"Teclado"` no campo de busca:
   - **Esperado**: `#vitrine-hero` é ocultado (`hidden`) trazendo os resultados para o topo; ao clicar em `Ver todas` / limpar busca, `#vitrine-hero` volta a ficar visível.
3. Alternar entre Modo Claro e Modo Escuro inspecionando banners, chips de variação, caixas de composição de combo e estados vazios (ex.: busca por termo inexistente `"XYZ999"`).

### Cenário C — Carrinho, Conta, Checkout e Painel Admin nos Dois Temas (`US3`, `US4`)
1. Percorrer no Modo Escuro e no Modo Claro:
   - Drawer do carrinho com cupom `BEMVINDO10`, barra de frete grátis (`R$ 250,00` / `5,00 kg`) e memória de cálculo;
   - Modal de login (`cliente.demo@ecommerce.fatec.edu.br` / `ClienteFatec@2026`), Minha Conta (endereços e histórico) e Checkout (PIX com simulação demo e timeline de estados);
   - Login Admin (`admin@ecommerce.fatec.edu.br` / `AdminFatec@2026`) percorrendo todas as abas (**Catálogo**, **Estoque**, **Promoções**, **Expedição**, **Auditoria**, **Relatórios**) validando contraste das tabelas, inputs e todos os badges de status.

### Cenário D — Responsividade e Acessibilidade (`US5`)
1. Redimensionar viewport para `360x800`, `375x812` (mobile) e `1366x900` (desktop):
   - **Esperado**: `document.documentElement.scrollWidth <= window.innerWidth` (`0px` de overflow horizontal) em Vitrine, Checkout e Painel Admin (tabelas roláveis dentro de `.table-responsive`).
2. Navegar via teclado (`Tab` / `Shift+Tab` / `Enter`) confirmando contornos de foco nítidos em ambos os temas e zero erros de console no DevTools.
