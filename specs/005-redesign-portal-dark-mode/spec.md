# Feature Specification: Redesign Profissional do Portal Web e Modo Escuro

**Feature Directory**: `specs/005-redesign-portal-dark-mode`  
**Created**: 2026-11-16  
**Status**: Draft  
**Input**: User description: "Melhoria visual profissional do Portal Web da FATEC Store (src/main/resources/static/) mantendo 100% da arquitetura HTML5/CSS3/ES Modules sem build externo e sem quebrar fluxos e contratos existentes: (1) elevação estética moderna de vitrine, hero/banners promocionais, cards de produto/combo, barra de categorias, drawer de carrinho, checkout, área do cliente e painel administrativo com melhor hierarquia tipográfica, espaçamento, microinterações, estados vazios/carregamento e responsividade; (2) implementação completa de Modo Claro / Modo Escuro (Dark Mode) usando tokens semânticos em css/tokens.css ([data-theme=\"dark\"] e fallback prefers-color-scheme), botão de alternância acessível no cabeçalho com persistência em localStorage e contraste WCAG AA adequado em tabelas, modais, formulários, alertas e badges de status."

## Clarifications

### Session 2026-11-16

- Q: Qual direção estética e estrutura de destaque inicial (seção hero) o portal deve adotar na Vitrine Pública para os modos claro e escuro? → A: Tech Store Contemporânea — superfícies neutras suaves (grafite/ardósia no escuro e fundo claro limpo), acentos em azul-índigo no claro e azul-ciano luminoso no escuro, com hero editorial compacto integrado ao acesso rápido ao catálogo e promoções vigentes.
- Q: Como deve funcionar o controle de alternância de tema no cabeçalho quando o usuário interage com ele? → A: Alternância direta em 1 clique (Claro ↔ Escuro); enquanto não houver escolha manual salva, o portal acompanha automaticamente as mudanças de tema do dispositivo e, após o primeiro clique, fixa e persiste a escolha manual do usuário.
- Q: Como os cartões de produto e combo devem apresentar sua área visual superior na vitrine considerando que o catálogo não fornece fotos externas? → A: Topo visual com ilustração vetorial temática por tipo/categoria combinada a selos de tipo (Produto Simples / Combo) e indicador de estoque em destaque, adaptado aos modos claro e escuro.
- Q: Quando o visitante aplica uma busca por texto ou filtra uma categoria na Vitrine, o que deve acontecer com a seção hero editorial? → A: Recolhimento inteligente durante filtros — exibe o hero editorial na vitrine completa (sem filtros) e o oculta automaticamente enquanto houver busca textual ou categoria selecionada, reexibindo-o ao limpar os filtros.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Alternância de Tema Claro e Modo Escuro com Persistência (Priority: P1)

Como visitante, cliente ou administrador da loja, quero alternar facilmente entre **Modo Claro** e **Modo Escuro** pelo cabeçalho principal (além de o portal respeitar automaticamente a preferência inicial do meu dispositivo quando ainda não escolhi manualmente), para navegar com conforto visual e leitura nítida em qualquer ambiente de iluminação.

**Why this priority**: O suporte a tema claro/escuro impacta a fundação visual de todo o portal (cores, superfícies, contraste de leitura, alertas e emblemas de status) e precisa guiar o redesenho de todos os componentes desde o início.

**Independent Test**: Pode ser testado abrindo o portal em um navegador novo configurado em tema escuro (ou claro), alternando o botão de tema no cabeçalho, recarregando a página e percorrendo Vitrine, Gaveta do Carrinho, Modais, Checkout e Painel Administrativo para verificar persistência e legibilidade integral.

**Acceptance Scenarios**:

1. **Given** um visitante acessando o portal pela primeira vez sem escolha manual prévia, **When** o sistema do dispositivo estiver configurado em modo escuro, **Then** o portal abre automaticamente em modo escuro com texto, cartões, formulários e destaques perfeitamente legíveis.
2. **Given** qualquer usuário com o portal aberto em qualquer visão (Vitrine, Minha Conta, Checkout ou Painel Administrativo), **When** aciona o controle de alternância de tema no cabeçalho, **Then** toda a interface (incluindo modais e gavetas abertas) transita instantaneamente entre claro e escuro sem recarregar a página e sem perder dados preenchidos ou itens do carrinho.
3. **Given** que o usuário selecionou manualmente o modo escuro (ou claro), **When** atualiza a página ou retorna ao site em uma nova abertura do navegador, **Then** a preferência escolhida manualmente é restaurada antes que a interface seja exibida.
4. **Given** alertas informativos, erros de validação, botões desabilitados e emblemas de status de pedido ou estoque exibidos no modo escuro, **When** o usuário os visualiza, **Then** todos mantêm contraste nítido e diferenciação semântica imediata entre aprovado, pendente, em preparação, enviado, entregue, cancelado e recusado.

---

### User Story 2 - Vitrine Pública Moderna, Hero Promocional e Catálogo Refinado (Priority: P1)

Como visitante ou comprador, quero encontrar uma vitrine com aparência comercial moderna e profissional — com faixa de destaque, banners de promoções vigentes bem organizados, navegação lateral de categorias clara e cartões de produto e combo com excelente hierarquia visual — para descobrir ofertas e escolher produtos com confiança e rapidez.

**Why this priority**: A vitrine é a porta de entrada e o principal cartão de visitas da loja diante do público e de avaliadores; elevar sua estética e escaneabilidade maximiza a percepção de qualidade já no primeiro segundo.

**Independent Test**: Pode ser testado carregando a página inicial como visitante em desktop e smartphone, filtrando categorias, realizando busca, trocando variações de um produto no cartão, abrindo os detalhes de um combo e conferindo estados de carregamento e busca vazia.

**Acceptance Scenarios**:

1. **Given** a página inicial carregada, **When** o visitante visualiza a dobra inicial, **Then** identifica claramente a barra de benefícios de frete, o cabeçalho fixo equilibrado, uma seção editorial/promocional com as campanhas ativas e a grade de produtos sem poluição visual.
2. **Given** um produto com múltiplas variações ou um combo promocional na vitrine, **When** o visitante visualiza o cartão ou abre a janela de detalhes, **Then** o preço vigente, eventual economia do combo, seletores de variação e indicador de estoque disponível aparecem organizados com destaque claro e feedback visual suave ao interagir.
3. **Given** que o visitante filtra uma categoria sem itens ou pesquisa um termo inexistente, **When** a grade atualiza, **Then** é exibido um estado vazio bem estruturado, com orientação clara e botão em destaque para restaurar todos os itens.

---

### User Story 3 - Experiência Refinada de Carrinho, Autenticação, Conta e Checkout (Priority: P2)

Como cliente, quero que a gaveta do carrinho, os diálogos de login/cadastro, minha área de endereços e pedidos e o fluxo de fechamento de compra tenham acabamento limpo, espaçamento generoso e informações financeiras fáceis de conferir, reduzindo atrito até a confirmação do pedido.

**Why this priority**: São as etapas críticas de conversão onde o cliente analisa descontos, frete grátis, endereços, métodos de pagamento e acompanha a linha do tempo do pedido; clareza visual evita dúvidas e erros.

**Independent Test**: Pode ser testado adicionando itens ao carrinho, aplicando cupom, abrindo o diálogo de entrada/cadastro, navegando em Minha Conta e finalizando uma compra com simulação de pagamento no modo claro e no modo escuro.

**Acceptance Scenarios**:

1. **Given** itens adicionados ao carrinho, **When** o comprador abre a gaveta lateral, **Then** visualiza controles táteis de quantidade, barra de progresso expressiva para frete grátis, feedback destacado de cupom e memória de cálculo alinhada com leitura imediata do desconto e do total líquido.
2. **Given** um cliente autenticado na área da conta ou no fechamento da compra, **When** consulta endereços, cotações de frete e opções de pagamento, **Then** o cartão selecionado fica visualmente inequívoco, o resumo financeiro acompanha a tela com clareza e o painel pós-compra apresenta a linha do tempo de estados de forma nítida nos dois temas.

---

### User Story 4 - Painel Administrativo Claro, Organizado e Produtivo (Priority: P2)

Como administrador ou operador comercial, de estoque ou promoções, quero que as abas, formulários de cadastro e tabelas gerenciais do painel administrativo tenham alinhamento tabular nítido, ações rápidas fáceis de distinguir e leitura confortável mesmo durante longas sessões de operação.

**Why this priority**: O painel administrativo concentra tabelas densas (catálogo completo, reservas de estoque, campanhas, cupons, fila de expedição, auditoria e relatórios gerenciais) que exigem legibilidade impecável em ambos os temas.

**Independent Test**: Pode ser testado autenticando-se como administrador e percorrendo todas as abas gerenciais em desktop e tablet/mobile nos temas claro e escuro.

**Acceptance Scenarios**:

1. **Given** um operador autenticado no painel administrativo, **When** alterna entre as abas disponíveis para o seu perfil, **Then** as abas exibem estado ativo inequívoco e cada tabela apresenta cabeçalhos nítidos, espaçamento equilibrado, colunas numéricas/monetárias alinhadas e emblemas de status legíveis em claro e escuro.
2. **Given** formulários de criação ou edição rápida dentro das abas administrativas (como ajuste de preço, alteração de status, registro de código de rastreio e criação de campanhas), **When** o operador interage com os campos e botões, **Then** ações primárias, secundárias e destrutivas têm hierarquia visual clara sem aparência amontoada.

---

### User Story 5 - Responsividade, Acessibilidade e Preservação Integral de Fluxos (Priority: P3)

Como usuário em qualquer dispositivo (computador, tablet ou celular) ou navegando por teclado, quero um portal consistente, sem quebras de layout ou perda de funcionalidades existentes após a modernização visual.

**Why this priority**: Garante que a evolução estética eleve a apresentação pública sem introduzir regressões de usabilidade, acessibilidade ou quebra em telas menores.

**Independent Test**: Pode ser testado percorrendo todas as visões em larguras de tela desktop e smartphone (a partir de 360px), navegando via teclado e validando os fluxos funcionais completos da loja.

**Acceptance Scenarios**:

1. **Given** o portal acessado em telas pequenas (smartphones a partir de 360px de largura), **When** o usuário navega pela Vitrine, Gaveta do Carrinho, Checkout e Painel Administrativo, **Then** cabeçalho, filtros, cartões e formulários se reorganizam sem cortes ou rolagem horizontal na página, mantendo tabelas extensas contidas em sua própria área rolável.
2. **Given** um usuário navegando pelo teclado ou com preferência de movimento reduzido, **When** percorre botões, seletores de variação, abas e campos de formulário, **Then** todos os elementos interativos exibem contorno de foco visível nos temas claro e escuro, com transições discretas e respeitosas.

---

### Edge Cases

- **Primeira visita sem preferência salva vs. escolha explícita anterior**: na ausência de preferência salva pelo usuário, deve prevalecer a preferência de aparência do sistema operacional; após o usuário clicar no alternador de tema, sua escolha explícita passa a prevalecer independentemente do tema do sistema.
- **Alternância durante interação ativa**: mudar o tema com a gaveta do carrinho aberta, um modal de login/detalhes visível ou formulários parcialmente preenchidos não deve fechar janelas, limpar campos nem reiniciar dados carregados.
- **Conteúdos extensos nos cartões e tabelas**: títulos longos de produtos/kits, múltiplos atributos de variação, códigos promocionais ou códigos de rastreamento não devem deformar cartões adjacentes nem estourar a largura da tela.
- **Estados vazios e transições rápidas de carregamento**: carregamentos assíncronos da vitrine, promoções e tabelas devem exibir placeholders visuais proporcionais e estáveis no tema ativo, evitando flashes bruscos de fundo claro durante carregamento no modo escuro.
- **Indicadores semânticos no modo escuro**: textos claros sobre fundos coloridos suaves (avisos, descontos, frete grátis, status `PAGO`, `AGUARDANDO_PAGAMENTO`, `ENVIADO`, `ENTREGUE`, `PAGAMENTO_RECUSADO`) devem manter contraste suficiente para leitura sem fadiga visual.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O cabeçalho global do portal DEVE disponibilizar um controle acessível de alternância direta em 1 clique (**Modo Claro ↔ Modo Escuro**), com rótulo/estado explícito para leitores de tela, aplicando a mudança imediatamente em qualquer visão sem recarregar a página.
- **FR-002**: Enquanto não houver preferência manual registrada anteriormente, o sistema DEVE detectar e acompanhar automaticamente as preferências de aparência (clara ou escura) do dispositivo do usuário, adotando o Modo Claro caso nenhuma preferência escura seja detectada; após acionamento manual do controle, a escolha explícita passa a prevalecer sobre o tema do sistema.
- **FR-003**: A preferência de tema selecionada manualmente pelo usuário DEVE ser persistida localmente e aplicada de forma imediata na inicialização para evitar piscadas visuais ao atualizar ou reabrir o portal.
- **FR-004**: Toda a paleta semântica da interface — fundos de página, superfícies elevadas, textos principais/secundários, bordas, sombras, campos de formulário, botões, modais, gaveta do carrinho, alertas e todos os emblemas semânticos de status de pedido e estoque — DEVE adaptar-se de forma coerente entre Modo Claro e Modo Escuro com legibilidade mínima equivalente ao padrão de contraste WCAG AA.
- **FR-005**: A Vitrine Pública DEVE seguir a direção visual **Tech Store Contemporânea** (superfícies neutras em ardósia/grafite no tema escuro e tons claros limpos no tema claro, com acento de marca em azul-índigo no claro e azul-ciano luminoso no escuro), contemplando faixa superior informativa refinada, cabeçalho fixo organizado, hero editorial compacto conectado ao catálogo/campanhas vigentes (exibido na vitrine completa e recolhido automaticamente enquanto houver busca ou categoria filtrada, reexibindo ao limpar filtros), barra lateral de categorias hierárquicas com estado ativo claro e seção de banners de promoções visualmente destacada.
- **FR-006**: Os cartões de produto, variações e combos/kits DEVEM apresentar área visual superior estilizada com ilustração vetorial temática por tipo/categoria e selos de classificação e disponibilidade em ambos os temas, além de destaque claro para preço vigente e economia de combos, seletores táteis de atributos com estado selecionado evidente e microinterações discretas em estados de hover, ativo e foco.
- **FR-007**: Todos os estados de carregamento e estados vazios (vitrine sem resultados, carrinho vazio, cliente sem endereços ou pedidos, painel administrativo sem registros filtrados) DEVEM possuir apresentação visual intencional e harmonizada em ambos os temas.
- **FR-008**: A gaveta lateral do carrinho DEVE receber acabamento visual aprimorado para itens, controles de quantidade, barra de progresso de frete grátis, seção de cupom e memória de cálculo, garantindo fácil leitura dos descontos e do total líquido.
- **FR-009**: Os fluxos de Autenticação (modais de entrada, cadastro e cartões de demonstração), Minha Conta (perfil, cartões de endereços e cartões de histórico de pedidos) e Checkout (etapas numeradas de endereço, frete, pagamento, resumo financeiro fixo e cartão pós-compra com linha do tempo) DEVEM exibir acabamento visual coeso e moderno nos dois temas.
- **FR-010**: O Painel Administrativo DEVE apresentar abas estilizadas, formulários agrupados com bom respiro visual e tabelas operacionais com alinhamento consistente, cabeçalhos legíveis e hierarquia clara entre ações de edição, transição e remoção/inativação.
- **FR-011**: O portal atualizado DEVE permanecer responsivo em resoluções desktop, tablet e smartphone (a partir de 360px de largura), mantendo navegação acessível por teclado com anéis de foco visíveis e respeitando preferências de movimento reduzido.
- **FR-012**: A evolução visual e o suporte ao tema escuro DEVEM preservar integralmente todos os comportamentos funcionais, controles interativos, fluxos de compra, simulação demonstrativa, controles RBAC e contratos de comunicação já existentes no sistema.

### Key Entities

- **Preferência de Tema Visual**: Representa a escolha explícita do usuário (Modo Claro ou Modo Escuro) ou a herança automática das preferências de aparência do dispositivo quando não houver escolha manual salva.
- **Sistema de Tokens Semânticos**: Conjunto unificado de definições visuais para superfícies, elevação, tipografia, bordas, marca, feedback e estados comerciais utilizado uniformemente por todas as visões nos modos claro e escuro.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos textos, rótulos, valores monetários, alertas, formulários, tabelas e emblemas de status permanecem legíveis em ambos os temas, atingindo contraste mínimo de 4,5:1 para texto regular e 3:1 para elementos grandes e controles de interface (nível WCAG AA).
- **SC-002**: A alternância entre Modo Claro e Modo Escuro ocorre em um único clique em qualquer tela com resposta visual imediata (abaixo de 1 segundo) e 100% de persistência após recarregamento completo da página.
- **SC-003**: 100% dos fluxos de usuário existentes (vitrine, filtros, carrinho visitante/autenticado, cupons, login/cadastro, endereços, checkout PIX/Cartão/Boleto, simulação demo e todas as abas administrativas) permanecem funcionais sem regressões após o redesign.
- **SC-004**: Todas as visões principais (Vitrine, Gaveta do Carrinho, Minha Conta, Checkout e Painel Admin) exibem zero rolagem horizontal indesejada na página em dispositivos móveis a partir de 360px–375px e em desktops comuns (1280px+), tanto no tema claro quanto no escuro.
- **SC-005**: Visitantes e avaliadores identificam na dobra inicial da página, em menos de 5 segundos de inspeção visual, a identidade da loja, as promoções ativas, o campo de busca/categorias e a transição para compra ou alternância de tema.

## Assumptions

- O portal continua integrado diretamente à aplicação web atual sem exigir etapas externas de compilação de interface ou introdução de bibliotecas/frameworks externos pesados.
- Nenhuma regra de negócio do servidor, esquema de banco de dados, modelo de autorização ou contrato de endpoints REST precisa ser alterado para entregar a evolução visual e o modo escuro.
- A visualização do catálogo e dos banners utiliza recursos gráficos estilizados, ícones vetoriais limpos e placeholders visuais consistentes em ambos os temas sem depender de carregamento externo frágil de imagens de terceiros.
- O público-alvo utiliza versões modernas de navegadores desktop e móveis com suporte padrão a troca dinâmica de tema visual e armazenamento local de preferências.
