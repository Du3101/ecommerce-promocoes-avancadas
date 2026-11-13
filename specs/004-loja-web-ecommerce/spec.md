# Feature Specification: Portal Web Completo de E-commerce e Painel Administrativo

**Feature Branch**: `004-loja-web-ecommerce`

**Created**: 2026-11-12

**Status**: Draft

**Input**: User description: "Criar a interface web completa do Sistema de E-commerce com Promoções Avançadas integrada aos Módulos 1, 2 e 3, servida diretamente pelo endereço raiz da aplicação (sem depender de servidor de interface separado), oferecendo: Loja Virtual do Cliente (árvore recursiva de categorias, vitrine de produtos com variações e combos/kits com estoque e economia, banners de promoções ativas, busca e filtro), Carrinho lateral interativo (visitante anônimo e cliente autenticado, cupons e memória de cálculo transparente), Área do Cliente (cadastro, login/logout integrados na página sem caixas de diálogo nativas do navegador, múltiplos endereços com tratamento de endereço ausente, histórico de pedidos com linha do tempo e snapshot imutável, cancelamento), Checkout guiado (seleção de endereço, cotação de frete em tempo real e frete grátis, PIX, Cartão aprovado/recusado e Boleto, além de simulação interativa de confirmação do pagamento para demonstrar mudança de estado e idempotência), Painel Administrativo dinâmico por perfil (catálogo, estoque, promoções/cupons, expedição de pedidos com listagem geral, auditoria e relatórios) e documentação unificada na raiz do projeto."

## Clarifications

### Session 2026-11-12

- Q: O que deve acontecer quando um visitante anônimo adiciona itens ao carrinho e, ao fazer login, descobre-se que sua conta de cliente já possui itens salvos de uma sessão anterior? → A: Mesclar aditivamente: transferir e somar os itens do carrinho visitante ao carrinho da conta do cliente, limpando a sessão anônima em seguida.
- Q: Quem deve ter permissão para acionar o botão de simulação de confirmação de pagamento na tela após um pedido ser criado com pagamento pendente por PIX ou Boleto? → A: Permitir ao cliente autenticado (dono do pedido) acionar a simulação na própria tela do pedido exclusivamente quando o modo de demonstração estiver habilitado (e bloquear em operação real ou para outros usuários).
- Q: Como a tela de login deve lidar com as contas pré-cadastradas de demonstração (`Cliente Demonstração` e `Administrador`) para facilitar testes e a apresentação? → A: Exibir cartões/botões discretos de "Acesso Rápido (Demonstração)" apenas quando o modo de demonstração estiver ativo, preenchendo e autenticando via fluxo real no servidor.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Vitrine da Loja, Árvore de Categorias, Promoções Ativas e Seleção de Variações/Combos (Priority: P1)

Um visitante acessa o endereço principal da loja virtual e visualiza imediatamente uma página de e-commerce completa: barra de navegação superior, banners destacando as campanhas promocionais ativas no momento, menu hierárquico recursivo de categorias e subcategorias, campo de busca por nome ou código identificador e grade de produtos e combos/kits comerciais. Ao navegar para uma categoria pai, todos os itens da categoria e de suas subcategorias descendentes são exibidos. Nos cards e detalhes dos itens, o comprador alterna entre variações disponíveis (como cor, tamanho ou especificação), visualiza o saldo físico disponível em estoque e identifica facilmente quando uma oferta é um Combo/Kit composto (vendo seus componentes e a economia obtida em relação aos itens individuais).

**Why this priority**: É a porta de entrada visual do sistema para clientes e para a banca avaliadora; sem vitrine navegável conectada ao catálogo, estoque e promoções reais, o sistema não é percebido como uma loja virtual utilizável.

**Independent Test**: Pode ser testada abrindo o endereço inicial da loja com a massa de demonstração carregada, clicando na categoria raiz para confirmar a listagem recursiva dos produtos das subcategorias netas, filtrando por termo de busca, inspecionando um Combo/Kit com cálculo de gargalo de estoque e visualizando os banners das promoções vigentes.

**Acceptance Scenarios**:

1. **Given** a loja inicializada com categorias hierárquicas, produtos com variações, combos e campanhas promocionais ativas, **When** um visitante acessa o endereço principal da loja, **Then** a vitrine exibe os banners de promoções vigentes, a árvore recursiva de categorias e os cards de produtos e combos sem exigir login prévio.
2. **Given** a árvore de categorias exibida na navegação, **When** o visitante seleciona uma categoria pai (como `Eletrônicos`), **Then** a vitrine passa a listar tanto itens vinculados diretamente a ela quanto os itens pertencentes a todas as suas subcategorias descendentes.
3. **Given** um item do tipo Combo/Kit na vitrine, **When** o visitante visualiza seu card ou abre seus detalhes, **Then** a interface exibe o preço vigente, a indicação dos componentes do pacote, a disponibilidade máxima calculada a partir do componente limitante e desabilita a compra quando o estoque disponível é zero.

---

### User Story 2 - Carrinho Lateral Interativo, Cupons e Transparência na Formação de Preço (Priority: P1)

De qualquer tela da loja, o comprador (visitante anônimo identificado por uma sessão persistida no navegador ou cliente autenticado) adiciona produtos ou combos ao carrinho lateral deslizante. A cada inclusão, alteração de quantidade, remoção ou aplicação/remoção de cupom promocional, o carrinho exibe imediatamente os itens, o subtotal bruto, a lista discriminada de cada promoção ativa aplicada e seu respectivo abatimento, o desconto de cupom (ou mensagem clara caso o cupom seja inválido, expirado, esgotado ou incompatível) e o total líquido final respeitando o piso mínimo zero.

**Why this priority**: Concentra a demonstração visual do motor de promoções avançadas e da transparência financeira exigida pelo negócio antes do fechamento da compra.

**Independent Test**: Pode ser testada adicionando os itens participantes de um combo promocional como visitante anônimo, atualizando quantidades para disparar campanhas ativas, aplicando um cupom válido (e depois um inválido para validar a mensagem de rejeição) e observando o recálculo instantâneo da memória de cálculo.

**Acceptance Scenarios**:

1. **Given** um visitante anônimo navegando na vitrine, **When** adiciona dois produtos elegíveis a uma promoção cruzada e ajusta a quantidade no carrinho lateral, **Then** a sessão de carrinho anônimo persiste no navegador, os itens são exibidos com atualização automática e o demonstrativo detalha o subtotal bruto, cada desconto aplicado e o total líquido.
2. **Given** um carrinho com itens elegíveis, **When** o comprador digita um código de cupom promocional válido, **Then** o cupom é aplicado e discriminado na memória de cálculo; caso o código seja inexistente, expirado, esgotado ou incompatível com a campanha exclusiva vigente, a interface exibe uma mensagem clara explicando a recusa sem perder os itens do carrinho.
3. **Given** descontos acumulados que superam o subtotal dos itens, **When** a interface exibe o resumo financeiro do carrinho, **Then** o valor líquido apresentado é mantido em `R$ 0,00` (nunca negativo).

---

### User Story 3 - Autenticação Integrada na Página, Cadastro, Endereços e Histórico de Pedidos do Cliente (Priority: P1)

Um visitante pode criar sua conta de cliente ou autenticar-se diretamente por meio de formulários próprios integrados na página do portal (com credenciais inválidas exibindo aviso elegante na própria tela, sem provocar caixas de diálogo nativas de autenticação do navegador). Quando autenticado como cliente, ele acessa sua "Área do Cliente" para cadastrar múltiplos endereços de entrega, definir qual endereço é o padrão atual (visualizando uma indicação neutra e amigável caso ainda não tenha nenhum endereço selecionado) e acompanhar todos os seus pedidos com linha do tempo de estados, detalhamento dos valores históricos congelados no instante da compra e opção de cancelamento quando permitido pelo estado atual.

**Why this priority**: Viabiliza a jornada completa de identificação do comprador, o fechamento autenticado da compra e a comprovação visual da máquina de estados e imutabilidade comercial dos pedidos.

**Independent Test**: Pode ser testada realizando login pela tela do site (tanto com credencial errada para validar ausência de popup nativo quanto com cliente válido), gerenciando endereços na conta do cliente e consultando os 4 pedidos de demonstração com diferentes status e snapshots congelados.

**Acceptance Scenarios**:

1. **Given** um usuário não autenticado na loja virtual, **When** informa e-mail ou senha incorretos no modal/página de login, **Then** a interface exibe uma mensagem amigável de erro no próprio formulário sem acionar caixa de diálogo nativa de usuário/senha do navegador.
2. **Given** um cliente recém-cadastrado que ainda não possui endereços ou não selecionou nenhum como padrão, **When** abre sua área de endereços, **Then** a interface exibe o estado explícito de "Nenhum endereço selecionado para entrega" sem gerar falhas e orienta o cadastro e seleção de um endereço válido.
3. **Given** um cliente autenticado na aba de pedidos, **When** visualiza um pedido existente e solicita seu cancelamento, **Then** o pedido em estado permitido (como `AGUARDANDO_PAGAMENTO` ou `PAGO`) é cancelado com atualização imediata de estado na tela, enquanto pedidos em estado bloqueado (como `ENVIADO` ou `ENTREGUE`) exibem a justificativa clara de transição não permitida.

---

### User Story 4 - Checkout Guiado, Frete em Tempo Real e Simulação Interativa de Confirmação de Pagamento (Priority: P1)

Com o carrinho preenchido e autenticado como cliente, o comprador avança para o fluxo guiado de Checkout. Na tela de checkout, ele confirma o endereço selecionado (ou cadastra/seleciona um caso esteja ausente), compara as cotações em tempo real das transportadoras disponíveis (`PAC`, `SEDEX` e `Transportadora Expressa`, com destaque automático quando atingir elegibilidade para Frete Grátis) e escolhe a forma de pagamento (`PIX`, `Cartão de Crédito` fictício — com instrução clara de números que simulam aprovação ou recusa para demonstração — ou `Boleto`). Ao confirmar o pedido utilizando pagamento assíncrono (`PIX` ou `Boleto`), a tela exibe os dados gerados (código copia-e-cola / linha digitável) junto a um botão interativo para simular o recebimento da confirmação do gateway de pagamento, permitindo observar ao vivo a mudança do pedido para `PAGO` e comprovar a idempotência caso o clique seja repetido.

**Why this priority**: Conecta ponta a ponta todos os três módulos (catálogo/estoque, carrinho/promoções e pedido/frete/pagamento) em uma experiência coesa e demonstrável em poucos segundos durante a apresentação do trabalho.

**Independent Test**: Pode ser testada autenticando-se como cliente com endereço selecionado e itens no carrinho, cotando frete ao vivo, fechando uma compra em `PIX`, acionando duas vezes o botão de confirmação de pagamento na tela resultante e verificando na primeira chamada a transição para `PAGO` e na segunda a sinalização explícita de processamento idempotente sem duplicar efeitos.

**Acceptance Scenarios**:

1. **Given** um cliente autenticado com endereço válido e carrinho não vazio na tela de checkout, **When** seleciona seu endereço e solicita a cotação de frete, **Then** a interface exibe prazos e valores de todas as modalidades simuladas e destaca automaticamente o frete gratuito quando o pedido atende às regras vigentes.
2. **Given** um checkout concluído via `PIX` ou `Boleto` com pedido em `AGUARDANDO_PAGAMENTO`, **When** o usuário clica no botão de simular confirmação do gateway de pagamento uma primeira vez e depois uma segunda vez, **Then** a primeira ação atualiza visualmente o pedido para `PAGO` com baixa definitiva de estoque e a segunda ação informa que a confirmação já havia sido processada de forma idempotente.
3. **Given** um checkout via `Cartão de Crédito` com dados configurados para simulação de recusa, **When** o cliente finaliza a tentativa de compra, **Then** a interface informa o motivo da recusa de pagamento, exibe o pedido como `PAGAMENTO_RECUSADO` e confirma a liberação imediata do estoque reservado.

---

### User Story 5 - Painel Administrativo Dinâmico por Perfil RBAC (Priority: P2)

Quando o usuário autenticado possui perfil administrativo ou operacional (`ADMIN`, `OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE` ou `RESPONSAVEL_PROMOCOES`), a barra superior libera o acesso ao "Painel Administrativo". As abas e ações disponíveis no painel se adaptam estritamente ao perfil do usuário autenticado: gestão de catálogo (criar categorias/subcategorias, produtos com variações, combos e alterar preços/status), gestão de estoque (consultar saldos, executar ajustes manuais com justificativa e acionar expiração de reservas vencidas), gestão comercial de promoções e cupons (listar, criar e ativar/inativar campanhas e cupons), expedição de pedidos (listar todos os pedidos da loja, preparar, despachar informando transportadora/rastreio e marcar como entregue), auditoria administrativa (consultar trilha imutável com filtros e valores antes/depois) e relatórios gerenciais (vendas consolidadas, ticket médio, produtos mais vendidos e cancelamentos).

**Why this priority**: Fecha o ciclo completo de backoffice da loja virtual, dando interface gráfica para todas as operações protegidas e auditadas pelos proxies e serviços dos três módulos.

**Independent Test**: Pode ser testada autenticando-se como administrador, acessando cada aba do painel administrativo para criar um produto ou ajustar estoque, avançando um pedido pago na fila de expedição e consultando imediatamente o registro gerado na trilha de auditoria e nos relatórios consolidados.

**Acceptance Scenarios**:

1. **Given** um usuário autenticado com perfil `ADMIN`, **When** acessa o Painel Administrativo, **Then** todas as seções gerenciais (Catálogo, Estoque, Promoções/Cupons, Expedição de Pedidos, Auditoria e Relatórios) ficam acessíveis e operacionais.
2. **Given** um usuário autenticado com perfil `CLIENTE`, **When** navega pela interface da loja, **Then** o acesso ao Painel Administrativo permanece oculto e qualquer operação restrita é bloqueada.
3. **Given** pedidos pagos aguardando expedição no painel, **When** o administrador ou operador comercial visualiza a fila geral de pedidos, coloca um pedido em preparação, informa o código de rastreio para despachar e depois marca como entregue, **Then** cada transição atualiza a listagem e respeita integralmente as regras da máquina de estados.

---

### User Story 6 - Extensões de Consulta para a Interface e Documentação Unificada do Sistema (Priority: P2)

Para que a vitrine exiba as promoções ativas sem exigir perfil administrativo e o painel de expedição visualize toda a fila de pedidos sem depender de memorizar identificadores, o sistema passa a disponibilizar consultas dedicadas adequadas a cada fronteira de acesso. Adicionalmente, o repositório passa a contar com uma documentação inicial unificada apresentando a arquitetura completa dos três módulos, credenciais de acesso para testes, comandos de execução e roteiro consolidado de demonstração.

**Why this priority**: Preenche as duas lacunas pontuais identificadas entre a camada de interface web e os contratos existentes dos Módulos 2 e 3, além de entregar o guia único do projeto completo.

**Independent Test**: Pode ser testada consultando as promoções ativas sem autenticação (recebendo apenas campanhas vigentes ativas), consultando a listagem geral de pedidos autenticado como administrador (e verificando bloqueio para cliente comum) e abrindo o guia principal na raiz do repositório para conferir instruções atualizadas.

**Acceptance Scenarios**:

1. **Given** campanhas promocionais cadastradas (algumas ativas vigentes e outras inativas ou expiradas), **When** a loja consulta a lista pública de promoções ativas para exibir seus banners, **Then** o sistema retorna exclusivamente as campanhas ativas dentro do período de vigência, sem exigir autenticação administrativa.
2. **Given** múltiplos pedidos de diferentes clientes persistidos no sistema, **When** um usuário com perfil `ADMIN` ou `OPERADOR_COMERCIAL` consulta a lista geral de pedidos no painel de expedição, **Then** o sistema retorna todos os pedidos ordenados do mais recente ao mais antigo, enquanto usuários sem permissão recebem recusa de acesso.

---

### Edge Cases

- **Sessão expirada ou credencial inválida em chamadas em segundo plano**: Quando o usuário tenta executar ação protegida sem estar autenticado ou com sessão inválida, a interface intercepta a resposta de não autorizado e abre/destaca a tela de login na própria página, nunca exibindo caixa de diálogo nativa do navegador.
- **Visitante anônimo que faz login antes do checkout**: Se o comprador adicionou itens ao carrinho como visitante anônimo e realiza login em uma conta que já possui itens salvos, os itens da sessão visitante são transferidos e somados aditivamente ao carrinho da conta (limpando a sessão anônima em seguida), mantendo um eventual cupom aplicado na conta ou reaplicando o cupom da sessão visitante quando a conta não possuir cupom prévio.
- **Tentativa de checkout sem endereço selecionado ou sem estoque**: Se o cliente tentar avançar ao checkout sem endereço de entrega selecionado ou com quantidade no carrinho superior ao saldo atual de estoque, a interface impede a submissão com aviso claro apontando diretamente para o cadastro de endereço ou ajuste de quantidade.
- **Falha de transição de estado na expedição ou cancelamento**: Ações inválidas no ciclo de vida do pedido exibem o motivo retornado pelo domínio em alerta visual dentro da interface sem quebrar o estado exibido na tela.
- **Acesso cruzado por perfil operacional restrito**: Se um operador autenticado possui apenas perfil comercial ou apenas estoque, o painel administrativo destaca suas abas autorizadas e trata recusas de permissão com aviso claro quando uma ação estiver fora do escopo de seu cargo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 (Ponto Único de Acesso Web)**: O sistema DEVE disponibilizar a interface web completa da loja virtual diretamente no endereço raiz da aplicação, integrando-se ao mesmo servidor dos serviços existentes sem exigir execução de servidor de interface separado.
- **FR-002 (Vitrine, Busca e Árvore Recursiva de Categorias)**: A vitrine pública DEVE exibir o menu hierárquico recursivo de categorias e subcategorias, campo de busca por nome ou código identificador, seleção de variações por produto e exibição destacada de Combos/Kits com indicação dos itens componentes, peso/preço vigentes e estoque disponível calculado pelo gargalo.
- **FR-003 (Banners Públicos de Promoções Vigentes)**: O sistema DEVE expor uma consulta pública retornando as campanhas promocionais ativas e vigentes para que a loja apresente banners promocionais dinâmicos a qualquer visitante sem expor regras restritas de criação ou edição administrativa.
- **FR-004 (Carrinho Lateral com Suporte a Visitante e Cliente Autenticado)**: A loja DEVE disponibilizar um carrinho lateral interativo acessível a partir de qualquer visão, persistindo o identificador de sessão anônima no navegador para visitantes, mesclando aditivamente os itens do visitante ao carrinho da conta autenticada no momento do login (limpando a sessão anônima em seguida) e permitindo adicionar, alterar quantidades, remover itens, limpar carrinho e aplicar/remover cupons promocionais.
- **FR-005 (Transparência Financeira no Carrinho e Checkout)**: O carrinho e o resumo de checkout DEVEM apresentar integralmente a memória de cálculo recebida do motor de promoções: subtotal bruto dos itens, relação discriminada de descontos de campanhas e cupons, frete calculado (ou destaque de frete grátis) e total líquido final com piso zero.
- **FR-006 (Autenticação e Cadastro Integrados sem Popup Nativo)**: A interface DEVE oferecer telas/modais próprios de login, logout e auto-cadastro de cliente, exibindo atalhos discretos de "Acesso Rápido (Demonstração)" apenas quando o modo de demonstração da loja estiver ativo (submetendo a autenticação real ao servidor e ocultando tais atalhos fora do modo de demonstração), e o sistema DEVE responder às falhas de autenticação das chamadas de interface de forma estruturada sem provocar caixas de diálogo nativas de credenciais do navegador.
- **FR-007 (Área do Cliente: Múltiplos Endereços e Histórico de Pedidos)**: Clientes autenticados DEVEM conseguir cadastrar, listar, selecionar como padrão e remover seus endereços de entrega (exibindo tratamento neutro amigável quando nenhum endereço estiver selecionado), além de consultar seus pedidos com linha do tempo de estados, snapshot comercial congelado e cancelamento em estados permitidos.
- **FR-008 (Checkout Guiado, Frete ao Vivo e Simulação de Pagamento)**: A tela de checkout DEVE guiar o cliente pela confirmação do endereço selecionado, cotação ao vivo das modalidades de entrega (`PAC`, `SEDEX`, `Expressa`), seleção do método de pagamento (`PIX`, `Cartão de Crédito` fictício com cenários de aprovação/recusa e `Boleto`) e, exclusivamente quando o modo de demonstração estiver habilitado, disponibilizar ao cliente autenticado dono do pedido (ou ao administrador) ação interativa segura na tela do pedido para simular a notificação de pagamento sem expor segredos internos de integração, exibindo feedback claro tanto na primeira confirmação quanto em reenvios idempotentes.
- **FR-009 (Listagem Administrativa de Pedidos)**: O sistema DEVE fornecer uma consulta administrativa restrita a perfis autorizados listando todos os pedidos da loja em ordem cronológica decrescente para alimentar a fila de expedição no painel administrativo.
- **FR-010 (Painel Administrativo Adaptado por Perfil RBAC)**: Usuários autenticados com perfis administrativos ou operacionais DEVEM visualizar o Painel Administrativo segmentado por abas (Catálogo e Combos, Estoque, Promoções e Cupons, Expedição de Pedidos, Trilha de Auditoria e Relatórios Gerenciais), respeitando as permissões de cada perfil e bloqueando acesso de clientes comuns.
- **FR-011 (Documentação Unificada e Conformidade de Código)**: Qualquer novo componente adicionado ao servidor de aplicação DEVE seguir integralmente a regra de métodos explícitos e documentados (objetivo funcional, papel arquitetural e parâmetros/retorno) com cobertura de testes automatizados, e o repositório DEVE disponibilizar uma documentação principal unificada na raiz reunindo arquitetura, credenciais corrigidas e roteiro de demonstração dos três módulos integrados.

### Key Entities

- **Sessão da Loja (`SessaoLoja`)**: Estado mantido no navegador do usuário contemplando o identificador de sessão anônima de carrinho, a identidade do usuário autenticado, seus perfis de acesso e o estado atual do painel exibido.
- **Vitrine de Item (`ItemVitrine`)**: Representação visual unificada na loja para produtos com suas variações selecionáveis e combos/kits com seus componentes, preços vigentes e saldos disponíveis.
- **Resumo Promocional Público (`PromocaoPublica`)**: Visão pública resumida de campanha promocional ativa e vigente contendo nome comercial, descrição, modalidade e período de validade para exibição em banners na loja.
- **Fila de Expedição (`PedidoAdministrativo`)**: Visão consolidada dos pedidos da loja ordenada por data de criação para acompanhamento e avanço de estados no painel administrativo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Jornada Completa de Compra pelo Navegador)**: Um avaliador consegue acessar o endereço principal da loja, navegar por categorias e promoções, adicionar itens ao carrinho, autenticar-se, simular frete, concluir um checkout e confirmar pagamento em menos de 3 minutos sem usar ferramentas externas de requisição manual.
- **SC-002 (Ausência de Interferência de Autenticação do Navegador)**: 100% das tentativas de login inválido ou acessos não autenticados a recursos protegidos via interface web exibem tratamento visual próprio na página, com zero ocorrências de popup nativo de credenciais do navegador.
- **SC-003 (Fidelidade Total aos Contratos dos 3 Módulos)**: 100% das operações realizadas na loja virtual (catálogo, variações, combos, estoque, promoções, cupons, checkout, frete, webhook idempotente, expedição, auditoria e relatórios) utilizam os fluxos reais integrados dos três módulos sem dados simulados no cliente web.
- **SC-004 (Segregação Visual e Segurança por Perfil)**: 100% das ações restritas do painel administrativo permanecem ocultas para visitantes e clientes comuns, e tentativas diretas aos novos recursos protegidos sem perfil compatível são rejeitadas pela camada de segurança.
- **SC-005 (Estabilidade e Conformidade Integral da Suíte)**: 100% da suíte automatizada existente nos três módulos mais os novos cenários de verificação da interface web e novos endpoints passam integralmente em única execução automatizada.

## Assumptions

- **Stack de Interface Web**: Como o requisito exige que o portal funcione diretamente no endereço raiz do servidor existente sem servidor separado de front-end, assume-se uma interface web estática servida diretamente pela aplicação principal, mantendo o código do servidor limpo e sem dependências externas de construção.
- **Transferência do Carrinho Visitante no Login**: Ao realizar autenticação na interface enquanto possui itens no carrinho anônimo, a interface somará aditivamente as quantidades de cada item da sessão visitante ao carrinho da conta do cliente autenticado e limpará os itens da sessão anônima em seguida.
- **Atalhos de Demonstração na Tela de Login**: Para agilizar apresentações acadêmicas, a tela de login exibirá cartões/botões discretos de "Acesso Rápido (Demonstração)" para as contas pré-cadastradas (`Administrador` e `Cliente Demonstração`) somente quando o modo de demonstração estiver ativo, realizando a autenticação real no servidor e permanecendo completamente ocultos quando o modo de demonstração estiver desativado.
- **Simulação Controlada de Pagamento na Tela**: A ação de "Simular Confirmação de Pagamento" exibida após um pedido `PIX` ou `Boleto` fica habilitada apenas quando o modo de demonstração da loja estiver ativo, exige autenticação válida do cliente dono do pedido (ou administrador) e não expõe credenciais internas de integração ao navegador, permitindo testar ao vivo a transição para `PAGO` e a idempotência sem comprometer a operação real.
