# Pesquisa Técnica e Decisões Arquiteturais — Portal Web E-commerce (004-loja-web-ecommerce)

**Feature**: `004-loja-web-ecommerce` | **Data**: 2026-11-12 | **Branch**: `004-loja-web-ecommerce`

## 1. Visão Geral das Descobertas no Repositório

Antes da definição dos contratos e da estrutura do portal web, foi realizada auditoria direta no código-fonte dos Módulos 1, 2 e 3 e nos testes existentes:

1. **Segurança HTTP Basic (`ConfiguracaoSegurancaHttp.java`)**: Atualmente utiliza `.httpBasic(Customizer.withDefaults())`. O `BasicAuthenticationEntryPoint` padrão do Spring Security envia o cabeçalho `WWW-Authenticate: Basic realm="Realm"` em respostas `401 Unauthorized`, o que faz navegadores exibirem um popup modal nativo ao receber `401` em chamadas `fetch()`.
2. **Identificador de Sessão do Carrinho (`CarrinhoComprasController.java`)**: O método privado `resolverSessao(...)` lê o query parameter `@RequestParam(required = false) String sessaoId` e `request.getSession(false)`, mas ainda **não lê** o cabeçalho HTTP `X-Sessao-Carrinho` especificado na funcionalidade (`FR-004`). Também não existe endpoint de mesclagem do carrinho anônimo para o carrinho do cliente após o login (`Q1`).
3. **Promoções Ativas para Vitrine (`PromocoesAdminController.java` & `CampanhaPromocionalJpaRepository.java`)**: As campanhas ativas já têm consulta ordenada pronta no repositório (`listarCampanhasAtivasPorPrioridade()`), porém o único endpoint REST existente está protegido sob `/api/v1/admin/promocoes` (`ADMIN`, `RESPONSAVEL_PROMOCOES`), bloqueando visitantes e clientes na vitrine pública.
4. **Simulação de Pagamento Pendente (`WebhookPagamentoController.java`)**: O webhook `POST /api/v1/pagamentos/confirmacao` exige o token interno `X-Gateway-Token` (`gateway-simulado-fatec`). Enviar esse token no JavaScript público violaria boas práticas de segurança e o requisito esclarecido em `Q2`.
5. **Listagem Geral de Pedidos no Painel Admin (`PedidoAdminController.java` & `PedidoJpaRepository.java`)**: O controlador administrativo possui apenas as mutações de expedição (`PATCH /{id}/preparar`, `PATCH /{id}/despachar`, `PATCH /{id}/entregar`). Não há `GET /api/v1/admin/pedidos` para popular a fila de pedidos do painel administrativo.
6. **Contrato do Teste `SwaggerESeedDemonstracaoTest.java`**: A suíte existente asserta explicitamente `openApiSpec.getInfo().getTitle().contains("Módulo 1")`. Qualquer atualização no título em `ConfiguracaoOpenApiSwagger.java` precisa preservar exatamente a substring `"Módulo 1"` para não quebrar testes existentes.
7. **Dívidas Pontuais de Documentação e Ordem de Carga**:
   - `InicializadorDemonstracaoModulo2Runner.java` não declara `@Order(...)` (enquanto Módulo 1 usa `@Order(2)` e Admin usa `@Order(1)`) e possui construtor/métodos sem Javadoc completo (violação latente do Princípio III da Constituição).
   - `specs/003-checkout-pedidos-pagamentos/quickstart.md` lista credenciais divergentes (`Admin@Fatec2026` e `cliente.teste@fatec.edu.br / Cliente@Fatec2026`) em vez das credenciais reais dos runners (`AdminFatec@2026` e `cliente.demo@ecommerce.fatec.edu.br / ClienteFatec@2026`).
   - Ainda não existe um `README.md` unificado na raiz do repositório.

---

## 2. Decisões Técnicas e Alternativas Consideradas

### Decisão 1: Stack Front-end Estático Puro (HTML5 + CSS3 + JavaScript ES Modules) no Spring Boot
- **Decisão**: Servir o portal web completo a partir de `src/main/resources/static/` (`index.html`, `css/` modular e `js/` organizado em ES Modules nativos `<script type="module">`) diretamente pelo servidor Spring Boot em `http://localhost:8080/`, sem Node.js, sem etapa de build e sem configuração de CORS.
- **Justificativa**: Atende exatamente às restrições da especificação (`FR-001`, Zero dependências externas no ambiente do avaliador/aluno), reduz atrito na execução via `.\mvnw.cmd spring-boot:run` ou `java -jar`, evita problemas de CORS com headers customizados (`Authorization`, `X-Sessao-Carrinho`) e mantém organização limpa equivalente à componentização moderna por meio de ES Modules.
- **Alternativas consideradas**:
  - *SPA React/Vite em diretório separado*: Rejeitada por introduzir Node.js/npm obrigatório, CORS e dois servidores para avaliação acadêmica.
  - *Templates Thymeleaf (SSR)*: Rejeitada porque tornaria interações reativas do carrinho lateral, cálculo em tempo real de frete e tabs RBAC dependentes de recarregamento de página, além de menos fiel ao consumo direto da API REST existente.

### Decisão 2: `AuthenticationEntryPoint` Customizado Sem `WWW-Authenticate` para Rotas `/api/**`
- **Decisão**: Criar e registrar um `AuthenticationEntryPoint` explícito em `ConfiguracaoSegurancaHttp` (aplicado tanto a `httpBasic(...)` quanto a `exceptionHandling(...)`) que detecta requisições `/api/**` e devolve HTTP `401 Unauthorized` com corpo JSON padronizado compatível com `ManipuladorGlobalExcecoesRest`:
  ```json
  {
    "timestamp": "...",
    "status": 401,
    "codigo": "NAO_AUTENTICADO",
    "mensagem": "Autenticação obrigatória para acessar este recurso."
  }
  ```
  **omitindo deliberadamente** o cabeçalho `WWW-Authenticate` (`FR-006`).
- **Justificativa**: Suprime 100% o popup nativo do navegador quando a UI ou o visitante consulta endpoints protegidos com `fetch()`, mantendo intacto o funcionamento do Swagger UI (cujo botão **Authorize** gera o header `Authorization: Basic` puramente via JavaScript usando o `SecurityScheme` OpenAPI).
- **Alternativas consideradas**:
  - *Session Cookies / Form Login (`JSESSIONID`)*: Rejeitada para preservar statelessness e compatibilidade total com `POST /api/v1/auth/login`, HTTP Basic e o contrato dos testes dos Módulos 1, 2 e 3.
  - *Header `X-Requested-With: XMLHttpRequest` condicional*: Menos determinístico do que checar prefixo `/api/` (ou responder sempre JSON sem `WWW-Authenticate` na `SecurityFilterChain`, já que a única rota browser humana é estática e permitida via `permitAll()`).

### Decisão 3: Suporte Nativo a `X-Sessao-Carrinho` e Endpoint de Mesclagem Aditiva no Login (`Q1`)
- **Decisão**:
  1. Atualizar `CarrinhoComprasController.resolverSessao(...)` para ler primeiro o cabeçalho HTTP `X-Sessao-Carrinho`, mantendo `@RequestParam(required = false) String sessaoId` e `HttpSession` como fallbacks compatíveis com os Módulos 2 e 3.
  2. Adicionar `POST /api/v1/carrinho/mesclar` (protegido para papel `CLIENTE`) e `ServicoCarrinhoCompras.mesclarCarrinhoVisitante(UUID clienteId, String sessaoId)` que lê o carrinho anônimo associado à sessão informada via `X-Sessao-Carrinho` (ou param `sessaoId`), soma as quantidades dos SKUs coincidentes no carrinho do cliente (revalidando itens ativos via `PortaConsultaCatalogoCliente`), transfere o cupom do visitante apenas se o cliente ainda não possuir cupom aplicado e se o cupom continuar válido após a soma, e remove o carrinho anônimo.
- **Justificativa**: Mantém o isolamento de subdomínios entre Módulo 1 (`AutenticacaoEAdminUsuariosController` não precisa acoplar ao repositório do Módulo 2) e garante a experiência contínua pedida em `Q1` com uma chamada explícita feita pelo módulo `auth-cliente.js` imediatamente após autenticação bem-sucedida quando existir carrinho anônimo local.
- **Alternativas consideradas**:
  - *Acoplar `AutenticacaoEAdminUsuariosController` diretamente ao `ServicoCarrinhoCompras`*: Rejeitada para evitar acoplamento inverso do Módulo 1 para o Módulo 2.

### Decisão 4: Leitura Pública de Promoções Ativas e Configuração da Loja (`Q3`)
- **Decisão**: Criar dois endpoints públicos de leitura somente-leitura liberados em `ConfiguracaoSegurancaHttp`:
  - `GET /api/v1/promocoes/ativas` (Módulo 2): reutiliza `CampanhaPromocionalJpaRepository.listarCampanhasAtivasPorPrioridade()`, filtrando opcionalmente por janela temporal vigente e projetando apenas campos seguros de exibição (`CampanhaPublicaResponseDTO`).
  - `GET /api/v1/loja/configuracao` (Módulo Compartilhado): retorna `ConfiguracaoLojaPublicaDTO` com `modoDemonstracaoAtivo` (alinhado diretamente a `ecommerce.modulo3.seed-demo-ativo`, garantindo consistência com `SimulacaoPagamentoDemoController`) e parâmetros informativos públicos de frete grátis (`valorMinimo` e `pesoMaximoGramas` já configurados no `application.yml`).
- **Justificativa**: Alimenta os banners da vitrine (`FR-002`) e controla dinamicamente a exibição dos cartões "Acesso Rápido (Demonstração)" na modal de login e do botão de simulação de pagamento (`Q2`/`Q3`), garantindo que em perfil `test` ou produção (`seed-demo-ativo: false`) nenhum recurso de demonstração fique exposto.
- **Alternativas consideradas**:
  - *Hardcode de banners e flag de demo no JavaScript*: Rejeitada por violar `Q3`, duplicar regras de negócio e dessincronizar quando o admin ativa/desativa campanhas pelo painel.

### Decisão 5: Endpoint Autenticado de Simulação de Pagamento em Modo Demonstração (`Q2`)
- **Decisão**: Expor `POST /api/v1/pedidos/{id}/simular-confirmacao-pagamento` no Módulo 3 (somente autenticado: cliente dono do pedido ou `ADMIN`), condicionado à flag `ecommerce.modulo3.seed-demo-ativo=true`. Caso `seed-demo-ativo` esteja `false`, lança `AcessoNegadoModulo3Exception` (`403 Forbidden`). Caso habilitado, valida titularidade do pedido (mesma regra de `PedidoClienteController`/`ServicoConsultaPedido`), monta internamente um `ConfirmacaoPagamentoRequestDTO` com `idTransacaoGateway = "DEMO-SIM-" + pedido.getNumeroPedido()` (preservando a chave idempotente `WEBHOOK:<numeroPedido>` de `ServicoProcessamentoPagamento`) e delega ao `ServicoProcessamentoPagamento.processarNotificacaoGateway(...)`.
- **Justificativa**: Mantém o `WebhookPagamentoController` (`POST /api/v1/pagamentos/confirmacao` com `X-Gateway-Token`) protegido contra chamadas públicas sem token e evita vazar `gateway-simulado-fatec` para o navegador, permitindo testar na tela do cliente tanto a aprovação (`PAGO`), a recusa (`PAGAMENTO_RECUSADO`) quanto cliques repetidos mostrando o selo de idempotência (`idempotenteReexecutado = true`).
- **Alternativas consideradas**:
  - *Expor `X-Gateway-Token` no front-end*: Explicitamente rejeitada em `Q2` por razões de segurança.

### Decisão 6: Listagem Administrativa de Pedidos no Módulo 3 (`FR-009` & `FR-010`)
- **Decisão**: Adicionar `findAllByOrderByCriadoEmDesc()` e `findByStatusOrderByCriadoEmDesc(StatusPedido)` em `PedidoJpaRepository`, método `listarTodosPedidos(StatusPedido statusOpcional)` em `ServicoConsultaPedido` e endpoint `GET /api/v1/admin/pedidos` (parâmetro opcional `?status=`) em `PedidoAdminController` (já protegido em `ConfiguracaoSegurancaHttp` para `ADMIN` e `OPERADOR_COMERCIAL`).
- **Justificativa**: Permite que a aba de Expedição do Painel Administrativo liste todos os pedidos da loja (do mais recente ao mais antigo) com filtro rápido por estado (`AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`, `PAGAMENTO_RECUSADO`), reutilizando integralmente `PedidoResponseDTO`.
- **Alternativas consideradas**:
  - *Consulta pedido a pedido por UUID digitado manualmente*: Rejeitada por inviabilizar a operação visual de loja real no painel administrativo (`US5`).

### Decisão 7: Organização CSS/JS Sem Frameworks e Conformidade Constitucional (Princípios I e III)
- **Decisão**: Estruturar o front-end com tokens de design em `css/tokens.css`, layout responsivo (`grid`/`flexbox`) em `css/layout.css`, componentes reutilizáveis (cards, drawer de carrinho, modais, toasts, timeline de pedido, tabelas admin) em `css/components.css`, e 6+ módulos ES dedicados em `js/` coordenados por um store reativo simples (**Observer**) e um cliente HTTP unificado (**Facade**). Todo código Java novo ou modificado cumprirá 100% do Princípio III (Javadoc completo em métodos `public`, `protected` e `private` descrevendo objetivo funcional, papel no Design Pattern, parâmetros e retornos), incluindo a regularização pendente do `InicializadorDemonstracaoModulo2Runner`.
- **Justificativa**: Preserva rigorosamente a arquitetura limpa dos três módulos, mantém cobertura automatizada em JUnit 5/MockMvc e garante clareza didática e profissional do projeto FATEC.
