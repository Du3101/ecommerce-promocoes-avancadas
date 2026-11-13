# Quickstart e Guia de Validação E2E — Portal Web E-commerce (004-loja-web-ecommerce)

**Feature**: `004-loja-web-ecommerce` | **URL do Portal**: `http://localhost:8080/`

---

## 1. Pré-requisitos e Execução Local

* **Java 21+** instalado (`java -version`).
* **Nenhum servidor Node.js, npm ou bundler necessário**: toda a camada web estática é servida diretamente pelo Spring Boot.

### Subir a Aplicação (Banco H2 + Dados de Demonstração Automáticos)
```powershell
.\mvnw.cmd spring-boot:run
```

### URLs Principais
* **Loja Virtual & Painel Administrativo**: `http://localhost:8080/`
* **Swagger UI (API REST + Authorize)**: `http://localhost:8080/swagger-ui.html`
* **Console H2 (Inspeção de Banco)**: `http://localhost:8080/h2-console`

### Credenciais de Demonstração Pré-carregadas
* **Cliente Demo (`CLIENTE`)**: `cliente.demo@ecommerce.fatec.edu.br` / `ClienteFatec@2026`
* **Administrador (`ADMIN`)**: `admin@ecommerce.fatec.edu.br` / `AdminFatec@2026`
* **Cupons Pré-carregados (Módulo 2)**: `BEMVINDO10`, `FATEC50`

---

## 2. Verificação Automatizada (JUnit 5 + MockMvc)

Execute toda a suíte integrada (Módulos 1, 2, 3 e extensões web/REST do portal):
```powershell
.\mvnw.cmd clean test
```

Validações específicas esperadas na suíte da Feature `004`:
1. `GET /` retorna `200 OK` servindo o HTML do portal sem exigir login.
2. `GET /api/v1/promocoes/ativas` e `GET /api/v1/loja/configuracao` respondem `200 OK` publicamente (com `modoDemonstracaoAtivo = false` sob perfil `test`).
3. Rotas protegidas (ex.: `GET /api/v1/clientes/me`, `GET /api/v1/admin/pedidos`) retornam `401` JSON **sem** cabeçalho `WWW-Authenticate` quando não autenticadas.
4. Carrinho anônimo com cabeçalho `X-Sessao-Carrinho` adiciona itens, realiza login do cliente e mescla quantidades aditivamente via `POST /api/v1/carrinho/mesclar`, limpando a sessão anônima.
5. `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento` respeita titularidade (`CLIENTE` dono ou `ADMIN`), verifica idempotência na segunda chamada (`idempotenteReexecutado = true`) e rejeita chamadas com `403` quando o modo demonstração está desabilitado.
6. `GET /api/v1/admin/pedidos` retorna todos os pedidos em ordem decrescente para `ADMIN`/`OPERADOR_COMERCIAL` e bloqueia perfil `CLIENTE` com `403`.

---

## 3. Roteiro de Validação Ponta a Ponta no Navegador (`SC-001` a `SC-005`)

### Etapa A — Vitrine Pública, Promoções e Carrinho Visitante (`SC-001`, `SC-002`)
1. Abra `http://localhost:8080/` em janela anônima (sem popup nativo do navegador).
2. Confira o carregamento automático dos banners de promoções ativas (`Leve 3 e Pague 2`, `R$ 50 OFF Acima de R$ 300`, `Combo Casado Teclado + Mouse`, etc.) e da árvore recursiva de categorias na lateral.
3. Filtre por categoria ou pesquise por SKU/nome, altere quantidade/variação e observe o estoque disponível exibido nos cards (produtos simples e `ComboKit`).
4. Adicione 1+ itens ao carrinho enquanto **visitante** e abra o drawer lateral; aplique o cupom `BEMVINDO10` e valide a memória de cálculo (subtotal bruto, promoções aplicadas, descontos e total líquido).

### Etapa B — Login Rápido Demo & Mesclagem Aditiva de Carrinho (`FR-004`, `FR-006`)
1. Clique em **Entrar** no topo para abrir o modal de autenticação.
2. Confira a exibição discreta dos cards **Acesso Rápido (Demonstração)** (exibidos porque `modoDemonstracaoAtivo = true`).
3. Clique em **Entrar como Cliente Demo** (ou digite email/senha manualmente).
4. Confirme que os itens e quantidades adicionados como visitante foram **somados** ao carrinho do cliente logado e nenhum popup nativo de Basic Auth apareceu.

### Etapa C — Área do Cliente, Null Object de Endereço e Checkout Completo (`SC-003`, `SC-004`)
1. Acesse **Minha Conta**: teste criar um novo endereço ou desmarcar/observar o tratamento amigável quando não há endereço selecionado (`possuiEnderecoValido = false`) e selecione o endereço de entrega ativo.
2. Acesse o **Checkout**, simule o frete (`CORREIOS_PAC`, `CORREIOS_SEDEX`, `TRANSPORTADORA_EXPRESSA`) e verifique o cálculo em tempo real e o selo **Frete Grátis** (aplicado apenas a `CORREIOS_PAC` quando `>= R$ 250,00` e `<= 5000g`).
3. Finalize um pedido com **PIX** (status inicial `AGUARDANDO_PAGAMENTO`).
4. Na tela de detalhes/confirmação do pedido, clique no botão de demonstração **Simular Confirmação de Pagamento (Sucesso)** -> confirme a transição para **`PAGO`**. Clique uma segunda vez -> valide o retorno idempotente (`idempotenteReexecutado = true`).
5. Teste criar outro pedido com **Cartão de Crédito Recusado** (número fictício final `0000`, ex.: `4000000000000000`, observando `PAGAMENTO_RECUSADO`) e teste o **Cancelamento** de um pedido elegível, verificando a linha do tempo de estados e o snapshot comercial imutável.

### Etapa D — Painel Administrativo RBAC & Expedição (`SC-005`)
1. Faça logout e entre como **Administrador Demo** (`admin@ecommerce.fatec.edu.br` / `AdminFatec@2026`).
2. Abra o **Painel Administrativo** no menu superior e valide a presença de todas as abas (`Pedidos & Expedição`, `Catálogo & Preços`, `Estoque & Reservas`, `Promoções & Cupons`, `Operadores & Auditoria`, `Relatórios Gerenciais`).
3. Na aba **Pedidos & Expedição**, visualize o pedido recém-pago no topo da lista (`criadoEm DESC`), avance-o para **Em Preparação** (`EM_PREPARACAO` via `preparar`), **Despachado** (`despachar` com código de rastreio) e **Entregue** (`entregar`).
4. Crie ou teste um ajuste de estoque/campanha/cupom, consulte a trilha de auditoria e recarregue os relatórios gerenciais de vendas, ticket médio, mais vendidos e cancelamentos.
5. *(Opcional RBAC)* Cadastre um operador `RESPONSAVEL_ESTOQUE` na aba `Operadores & Auditoria`, faça login com ele e valide que apenas a aba `Estoque & Reservas` aparece no painel administrativo.
