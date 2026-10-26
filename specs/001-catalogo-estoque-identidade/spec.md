# Feature Specification: Módulo 1 — Identidade, Catálogo, Composição de Produtos, Estoque e Auditoria

**Feature Branch**: `001-catalogo-estoque-identidade`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Módulo 1 (Integrante 1): Identidade, Catálogo, Composição de Produtos, Estoque e Auditoria. O módulo deve permitir: (1) cadastro e autenticação de usuários com perfis distintos (Cliente, Administrador e operadores) sem armazenar senhas em texto puro, permitindo ao cliente cadastrar múltiplos endereços e escolher qual usar na entrega; (2) catálogo de produtos com nome, descrição, preço, marca, peso, dimensões e variações (como tamanho, cor, capacidade), além de combos/kits formados por múltiplos produtos; (3) organização de produtos em categorias e subcategorias hierárquicas recursivas; (4) controle rigoroso de estoque com reserva atômica no momento do fechamento, liberação de reserva em caso de cancelamento/recusa de pagamento e proteção contra compra simultânea da última unidade por dois clientes; (5) trilha de auditoria registrando usuário, data/hora e detalhes de operações administrativas críticas (alteração de preço, estoque, campanhas e cancelamentos)."

## Clarifications

### Session 2026-10-26

- Q: Como devem ser criadas as contas de perfis administrativos e operacionais (`ADMIN`, `OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE`, `RESPONSAVEL_PROMOCOES`) no sistema? → A: O cadastro público cria exclusivamente contas `CLIENTE`; contas administrativas e operacionais são criadas ou promovidas apenas por um `ADMIN` autenticado (com um `ADMIN` inicial pré-cadastrado na inicialização do sistema).
- Q: Como deve ser definido o cálculo de preço de um `ComboKit` em relação aos produtos que o compõem? → A: O `ComboKit` suporta tanto o cálculo recursivo pela soma dos preços atuais dos itens componentes (com desconto opcional do kit) quanto um preço fixo dedicado definido pelo operador comercial, enquanto peso, dimensões e disponibilidade máxima em estoque são sempre agregados recursivamente a partir dos itens filhos.
- Q: O que deve acontecer com uma reserva de estoque (`ReservaEstoque`) caso o cliente inicie o checkout, mas abandone o pagamento sem concluí-lo nem cancelá-lo explicitamente? → A: Além da liberação imediata por pagamento recusado ou cancelamento do pedido, cada reserva possui um prazo de validade configurável (TTL padrão de 30 minutos), permitindo liberar automaticamente reservas expiradas e devolver o saldo ao estoque disponível.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cadastro, Autenticação, Autorização por Perfil e Gestão de Endereços (Priority: P1)

Um novo visitante cria uma conta de cliente informando seus dados cadastrais (nome completo, e-mail, documento e telefone) e definindo uma senha segura. Após autenticar-se, o cliente gerencia um ou mais endereços de entrega em sua conta e define qual endereço será utilizado em uma compra. Paralelamente, usuários internos (Administrador Geral, Operador Comercial, Responsável pelo Estoque e Responsável pelas Promoções) autenticam-se com permissões restritas exclusivamente às funções de seu cargo, enquanto clientes possuem acesso restrito apenas aos seus próprios dados e endereços.

**Why this priority**: É a base de segurança, privacidade e identificação de todo o e-commerce. Sem contas autenticadas, controle de acesso por função e endereços válidos, nenhuma operação segura de administração de catálogo, cálculo de entrega ou fechamento de pedido pode ocorrer.

**Independent Test**: Pode ser testada independentemente criando contas de cliente e de operadores, autenticando cada perfil, cadastrando múltiplos endereços para um cliente, selecionando o endereço de entrega ativo e verificando que um cliente não consegue acessar dados de outro cliente nem executar ações administrativas.

**Acceptance Scenarios**:

1. **Given** um visitante não cadastrado com dados válidos (nome, e-mail único, CPF válido e senha), **When** solicita a criação de conta de cliente, **Then** a conta é criada com perfil `CLIENTE`, a senha é armazenada de forma irreversivelmente protegida (nunca em texto puro) e o cliente pode se autenticar imediatamente.
2. **Given** um cliente autenticado, **When** cadastra três endereços distintos (ex.: Residencial, Trabalho e Casa dos Pais) e marca um deles como endereço selecionado para entrega, **Then** o sistema persiste todos os endereços vinculados ao cliente e retorna o endereço selecionado para uso no cálculo de entrega e fechamento do pedido.
3. **Given** um cliente autenticado que ainda não cadastrou ou não selecionou nenhum endereço, **When** o sistema consulta seu endereço padrão de entrega, **Then** o sistema retorna uma representação segura de endereço ausente/não informado (sem gerar falhas inesperadas), sinalizando que um endereço válido precisará ser informado antes da conclusão da compra.
4. **Given** um usuário autenticado com perfil `CLIENTE` ou `RESPONSAVEL_ESTOQUE`, **When** tenta executar uma operação fora de seu escopo (ex.: um cliente tentando alterar o preço de um produto, ou o responsável pelo estoque tentando encerrar uma campanha promocional), **Then** a operação é negada imediatamente com mensagem clara de falta de permissão.

---

### User Story 2 - Catálogo de Produtos, Variações e Categorias Hierárquicas Recursivas (Priority: P1)

Administradores e operadores comerciais estruturam o catálogo criando uma árvore de categorias e subcategorias de profundidade arbitrária (ex.: `Eletrônicos > Dispositivos Móveis > Smartphones`) e cadastram produtos contendo nome, descrição, preço base, marca, imagens, peso, dimensões e características específicas. Para produtos com opções diferenciadas (como uma camiseta com tamanhos P/M/G e cores Preta/Branca, ou um smartphone com 128GB/256GB), o operador registra variações comercializáveis que compartilham os atributos descritivos comuns sem duplicação desnecessária. Clientes navegam e pesquisam produtos filtrando por categoria (incluindo automaticamente os produtos das subcategorias filhas).

**Why this priority**: O catálogo estruturado em categorias e variações é o coração comercial da loja virtual, sendo indispensável tanto para a vitrine de clientes quanto para a aplicação futura de promoções por categoria, produto ou peso/dimensão no frete.

**Independent Test**: Pode ser testada independentemente cadastrando uma hierarquia de categorias de 3 níveis, registrando produtos com múltiplas variações, alterando preços e consultando o catálogo como cliente por categoria pai para validar que todos os produtos das subcategorias descendentes são listados.

**Acceptance Scenarios**:

1. **Given** um administrador autenticado, **When** cria a categoria `Vestuário`, a subcategoria `Camisetas` dentro de `Vestuário` e a subcategoria `Esportivas` dentro de `Camisetas`, **Then** a estrutura em árvore é mantida de forma recursiva e consultas à categoria `Vestuário` abrangem produtos vinculados diretamente a ela e a qualquer uma de suas subcategorias descendentes.
2. **Given** um produto base cadastrado com nome, descrição, preço base, marca, peso e dimensões, **When** o operador adiciona variações (ex.: Tamanho `M` / Cor `Azul` e Tamanho `G` / Cor `Preta`) com seus respectivos códigos identificadores (SKUs) e eventuais acréscimos de preço, **Then** cada variação torna-se um item individualmente selecionável e controlável no estoque, reutilizando as especificações compartilhadas de tamanho/cor.
3. **Given** a necessidade de cadastrar um novo tipo de produto com características específicas adicionais (ex.: livros com ISBN e número de páginas, ou alimentos com data de perecibilidade), **When** o operador registra o produto no catálogo, **Then** o sistema permite a inclusão dessas características sem exigir reestruturação dos produtos já existentes.

---

### User Story 3 - Composição de Produtos em Combos e Kits Comerciais (Priority: P2)

Um operador comercial cria um "Combo" ou "Kit" promocional (ex.: *Kit Home Office* composto por 1 Teclado Mecânico + 1 Mouse Sem Fio + 1 Suporte de Notebook, ou até um *Mega Kit* que inclui um subkit e um produto avulso). O cliente visualiza e adiciona o combo ao carrinho como um item comercial único, enquanto o sistema calcula automaticamente seu peso/dimensões agregados e valida a disponibilidade de todos os seus produtos componentes.

**Why this priority**: Permite comercializar produtos compostos de maneira transparente para o cliente e fornece a base estrutural para promoções de venda casada e pacotes comerciais exigidos pelo negócio.

**Independent Test**: Pode ser testada independentemente criando dois produtos simples com variações, agrupando-os em um Kit com preço próprio, consultando os detalhes do Kit (peso total, itens integrantes e disponibilidade) e verificando que o Kit aparece indisponível caso qualquer um de seus componentes fique sem estoque.

**Acceptance Scenarios**:

1. **Given** produtos existentes no catálogo, **When** um operador comercial cria um Combo/Kit definindo os itens componentes (e suas respectivas quantidades no kit) e o preço comercial do conjunto, **Then** o Combo passa a ser listado no catálogo e tratado de forma uniforme aos produtos individuais nas consultas de preço, peso total e disponibilidade.
2. **Given** um Combo composto por 1 unidade do Produto A e 2 unidades do Produto B, **When** um cliente consulta a disponibilidade máxima de compra desse Combo tendo 10 unidades de A e 5 unidades de B em estoque, **Then** o sistema informa que há no máximo 2 unidades completas do Combo disponíveis para compra.

---

### User Story 4 - Controle Rigoroso de Estoque, Reserva Atômica e Prevenção de Concorrência (Priority: P1)

O sistema controla a quantidade física disponível e a quantidade reservada de cada produto/variação. Quando clientes adicionam itens ao carrinho, o estoque é apenas consultado de forma informativa. No momento em que o fechamento da compra (checkout) é iniciado, o sistema reserva atomicamente as unidades solicitadas. Se dois clientes tentarem fechar a compra da última unidade disponível ao mesmo tempo, apenas um obtém a reserva e o outro recebe imediatamente a informação de saldo insuficiente. Caso o pagamento seja confirmado, a reserva é convertida em baixa definitiva; caso o pagamento seja recusado ou o pedido seja cancelado, as unidades reservadas retornam imediatamente ao estoque disponível.

**Why this priority**: Impede a venda de produtos sem cobertura física (overselling) em cenários de concorrência real e garante que reservas de pedidos não pagos ou cancelados não retenham mercadorias indevidamente.

**Independent Test**: Pode ser testada independentemente definindo o estoque de um produto para 1 unidade, disparando duas solicitações simultâneas de reserva para dois clientes distintos, verificando que exatamente uma reserva é aprovada e a outra é rejeitada por falta de estoque, e em seguida cancelando a reserva aprovada para comprovar o retorno imediato do saldo para 1 unidade disponível.

**Acceptance Scenarios**:

1. **Given** uma variação de produto com apenas 1 unidade disponível em estoque e dois clientes com essa variação em seus carrinhos, **When** ambos solicitam o fechamento da compra simultaneamente, **Then** o sistema garante que apenas a primeira reserva seja efetivada, reduzindo o estoque disponível para 0 e rejeitando a segunda tentativa com mensagem explícita de estoque insuficiente.
2. **Given** uma reserva de estoque ativa vinculada a um pedido em processamento, **When** o pagamento do pedido é recusado ou o pedido é cancelado, **Then** o sistema libera integralmente as unidades reservadas, restaurando o saldo disponível para novas compras e mantendo o histórico da movimentação.
3. **Given** uma reserva de estoque ativa para um Combo/Kit composto por múltiplos produtos, **When** um dos itens componentes não possui saldo suficiente no instante da reserva, **Then** nenhuma unidade parcial dos demais componentes permanece bloqueada (operação atômica do tipo tudo-ou-nada).
4. **Given** uma confirmação de baixa ou liberação de reserva já processada anteriormente para o mesmo identificador de pedido/operação, **When** a solicitação é recebida novamente em duplicidade, **Then** o sistema reconhece a operação como já processada e não duplica a baixa nem a devolução de saldo.

---

### User Story 5 - Trilha de Auditoria de Operações Administrativas Críticas (Priority: P2)

Sempre que um administrador ou operador realiza uma operação crítica — como alterar o preço de um produto ou combo, ajustar manualmente o saldo de estoque, desativar um item do catálogo, alterar parâmetros de campanhas ou executar cancelamentos — o sistema registra automaticamente um evento de auditoria imutável contendo quem realizou a ação, quando ocorreu, qual recurso foi afetado e os valores antes e depois da mudança. Administradores podem consultar e filtrar essa trilha de auditoria.

**Why this priority**: Garante rastreabilidade, governança e conformidade sobre mudanças comerciais sensíveis que impactam diretamente o faturamento e os pedidos da loja.

**Independent Test**: Pode ser testada independentemente realizando uma alteração de preço de um produto e um ajuste manual de estoque com um usuário administrador e, em seguida, consultando o histórico de auditoria para verificar os registros com usuário responsável, data/hora, tipo de operação e valores anterior/novo.

**Acceptance Scenarios**:

1. **Given** um administrador autenticado que altera o preço de um produto de R$ 100,00 para R$ 85,00, **When** a alteração é salva, **Then** o sistema gera automaticamente um registro de auditoria imutável contendo o identificador do usuário responsável, perfil, data e hora exatas, entidade afetada, valor anterior (`100.00`) e novo valor (`85.00`).
2. **Given** registros de auditoria acumulados no sistema, **When** um administrador consulta a trilha de auditoria filtrando por período, usuário responsável ou tipo de operação, **Then** o sistema lista os registros correspondentes em ordem cronológica sem permitir que registros históricos sejam editados ou apagados.

---

### Edge Cases

- **Concorrência na última unidade**: O que acontece quando dois ou mais clientes tentam reservar simultaneamente a última unidade de um produto simples ou de um item integrante de um Combo/Kit? Apenas uma transação obtém a reserva; as demais falham de forma limpa sem deixar saldo negativo.
- **Alteração de preço com produto já em carrinhos ativos**: Se um administrador altera o preço de um produto que já está no carrinho de um cliente, o catálogo passa a exibir o novo preço imediatamente e disponibiliza a informação atualizada para que o carrinho e o checkout detectem a mudança antes do fechamento.
- **Exclusão ou desativação de categoria/produto**: Tentativas de remover uma categoria que possui subcategorias ou produtos vinculados, ou um produto que já compõe pedidos históricos ou combos ativos, devem ser impedidas ou convertidas em inativação lógica para preservar a integridade de pedidos antigos e combos.
- **Ciclos em estruturas recursivas**: O que acontece se um administrador tentar definir uma categoria como subcategoria de si mesma (ou de uma de suas descendentes), ou incluir um Combo A dentro de um Combo B que já contém o Combo A? O sistema deve detectar e bloquear referências circulares no momento do cadastro/atualização.
- **Valores inválidos de catálogo e estoque**: Tentativas de cadastrar preços negativos ou iguais a zero, pesos/dimensões negativos, quantidades de reserva menores ou iguais a zero, ou ajustes de estoque que deixem o saldo físico disponível menor que zero devem ser rejeitadas imediatamente.
- **Cliente sem endereço cadastrado ou selecionado**: Consultas ao endereço padrão de entrega de um cliente recém-cadastrado não devem causar erros inesperados, retornando um objeto neutro que representa "Endereço Não Informado" até que o cliente selecione um endereço válido.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 (Cadastro e Segurança de Credenciais)**: O sistema DEVE permitir o cadastro público de clientes contendo nome completo, e-mail único, documento (CPF) válido, telefone e senha, atribuindo obrigatoriamente o perfil `CLIENTE` no auto-cadastro e armazenando a senha exclusivamente por meio de resumo criptográfico irreversível (nunca em texto puro).
- **FR-002 (Autenticação, Gestão de Operadores e Controle de Acesso por Perfil)**: O sistema DEVE autenticar usuários e segregar permissões entre os perfis `CLIENTE`, `ADMIN`, `OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE` e `RESPONSAVEL_PROMOCOES`, disponibilizando um `ADMIN` inicial pré-cadastrado e permitindo exclusivamente ao perfil `ADMIN` criar contas ou conceder perfis administrativos/operacionais, enquanto clientes acessam exclusivamente seus próprios dados e endereços.
- **FR-003 (Múltiplos Endereços por Cliente)**: O sistema DEVE permitir que cada cliente cadastre, edite, liste, remova e selecione um entre múltiplos endereços de entrega (contendo CEP, logradouro, número, complemento opcional, bairro, cidade, estado/UF e identificador amigável como "Casa" ou "Trabalho").
- **FR-004 (Tratamento Seguro de Ausência de Endereço/Categoria)**: O sistema DEVE representar a ausência de endereço selecionado ou de categoria pai por meio de objetos de domínio com comportamento neutro e explícito, evitando falhas por referências nulas durante consultas de perfil e catálogo.
- **FR-005 (Catálogo de Produtos e Atributos)**: O sistema DEVE manter um catálogo de produtos contendo código identificador, nome, descrição, preço base positivo, marca, URLs/referências de imagens, peso, dimensões (altura, largura, profundidade), status (ativo/inativo), categoria associada e atributos específicos extensíveis por tipo de produto.
- **FR-006 (Variações de Produto e Compartilhamento de Atributos)**: O sistema DEVE permitir que um produto possua múltiplas variações comercializáveis (ex.: combinações de tamanho, cor, voltagem ou capacidade de armazenamento), cada uma com código SKU próprio e preço efetivo, compartilhando eficientemente metadados imutáveis repetitivos (como tabelas de tamanhos, cores e especificações técnicas padronizadas).
- **FR-007 (Hierarquia Recursiva de Categorias e Subcategorias)**: O sistema DEVE organizar categorias em estrutura hierárquica recursiva (árvore de categorias e subcategorias sem limite fixo de níveis), impedindo ciclos hierárquicos e permitindo consultar produtos pertencentes a uma categoria e a todas as suas subcategorias descendentes de forma unificada.
- **FR-008 (Composição Recursiva de Combos e Kits)**: O sistema DEVE permitir agrupar dois ou mais produtos/variações (ou outros subkits, sem referências circulares) em Combos/Kits comercializáveis, expondo uma interface uniforme entre produtos simples e combos em que o peso total, as dimensões agregadas e a disponibilidade máxima em estoque são sempre calculados recursivamente a partir dos itens filhos, e o preço do Combo pode ser configurado pelo operador comercial tanto pelo cálculo recursivo da soma dos componentes (com desconto opcional do pacote) quanto por um valor fixo dedicado.
- **FR-009 (Controle de Saldo de Estoque)**: O sistema DEVE controlar, para cada item estocável (produto simples ou variação), a quantidade física disponível, a quantidade reservada em compras em andamento e o histórico de movimentações (entrada, ajuste manual, reserva, liberação de reserva e baixa definitiva).
- **FR-010 (Reserva Atômica de Estoque no Fechamento)**: O sistema DEVE realizar a reserva de estoque de forma atômica no momento do fechamento da compra (checkout), garantindo que dois clientes nunca consigam reservar simultaneamente a última unidade disponível de um item (ou componente de um Combo/Kit).
- **FR-011 (Liberação, Expiração e Confirmação Idempotente de Reserva)**: O sistema DEVE converter a reserva de estoque em baixa definitiva quando o pagamento for aprovado e DEVE liberar integralmente a reserva (restaurando o saldo disponível) quando o pagamento for recusado, quando o pedido for cancelado ou quando o prazo de validade configurável da reserva (TTL padrão de 30 minutos) expirar, tratando chamadas repetidas para a mesma reserva de forma idempotente.
- **FR-012 (Trilha de Auditoria Imutável)**: O sistema DEVE registrar automaticamente em trilha de auditoria imutável todas as operações administrativas críticas (alterações de preço, ajustes de estoque, ativação/inativação de itens ou categorias, alterações de campanhas e cancelamentos), gravando usuário responsável, perfil, data/hora exata, operação realizada, identificador do recurso e estado anterior/novo.
- **FR-013 (Contrato de Integração com Carrinho, Promoções e Checkout)**: O módulo DEVE fornecer consultas consistentes e atualizadas de produtos, categorias ascendentes, preços vigentes, pesos/dimensões, endereços de cliente e operações de reserva/baixa/liberação de estoque para que os módulos dos Integrantes 2 (Carrinho/Promoções) e 3 (Checkout/Pedidos/Frete) operem sem acoplamento direto às estruturas internas de armazenamento.

### Key Entities

- **Usuário (`Usuario`)**: Representa qualquer pessoa autenticável na plataforma, possuindo credenciais protegidas, status de conta e conjunto de perfis de acesso (`CLIENTE`, `ADMIN`, `OPERADOR_COMERCIAL`, `RESPONSAVEL_ESTOQUE`, `RESPONSAVEL_PROMOCOES`).
- **Cliente (`Cliente`)**: Perfil comercial vinculado a um usuário comprador, contendo nome completo, CPF, telefone, data de cadastro, indicador de primeira compra e lista de endereços de entrega com referência ao endereço selecionado.
- **Endereço (`Endereco` / `EnderecoAusente`)**: Localização física de entrega do cliente (CEP, logradouro, número, complemento, bairro, cidade, UF/região) utilizada para elegibilidade regional de promoções e cálculo de frete.
- **Categoria (`Categoria` / `ComponenteCategoria`)**: Nó da árvore hierárquica de classificação do catálogo, podendo conter subcategorias filhas e produtos associados em estrutura recursiva.
- **Item de Catálogo (`ItemCatalogo` — `Produto`, `VariacaoProduto`, `ComboKit`)**: Abstração uniforme para qualquer oferta comercializável na loja, seja um produto individual/variação com SKU próprio ou um Combo/Kit composto recursivamente por outros itens e quantidades.
- **Atributo Compartilhado de Variação (`AtributoVariacao`)**: Especificação intrínseca imutável e compartilhável (ex.: definição de Cor, Tamanho, Marca ou Especificação Técnica) reutilizada por múltiplas variações do catálogo.
- **Estoque e Reserva de Estoque (`ItemEstoque`, `ReservaEstoque`)**: Registro do saldo disponível e reservado de um SKU, acompanhado das reservas vinculadas a fechamentos de compra com controle de validade (`expiraEm`, TTL padrão de 30 minutos) e estado (`ATIVA`, `CONFIRMADA`, `LIBERADA`, `EXPIRADA`).
- **Registro de Auditoria (`RegistroAuditoria`)**: Evento histórico imutável contendo autor, perfil, data/hora (`timestamp`), tipo de ação administrativa, entidade alvo e resumo das alterações realizadas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001 (Zero Venda sem Estoque em Concorrência)**: Em cenários com 50 ou mais solicitações simultâneas disputando as últimas unidades de um mesmo produto ou Combo/Kit, 100% das reservas aprovadas respeitam rigorosamente o limite físico disponível, com zero ocorrências de saldo negativo ou dupla alocação da última unidade.
- **SC-002 (Rastreabilidade Total de Operações Críticas)**: 100% das alterações administrativas de preço, ajustes de estoque e mudanças de status de catálogo geram registro completo na trilha de auditoria no mesmo instante da operação, identificando usuário, data/hora e valores anterior e novo.
- **SC-003 (Segurança de Credenciais e Segregação de Acesso)**: 100% das senhas cadastradas são armazenadas de forma criptograficamente irreversível (zero senhas legíveis em armazenamento ou logs) e 100% das tentativas de acesso cruzado entre clientes ou execução de ações administrativas por perfis não autorizados são bloqueadas.
- **SC-004 (Consistência em Hierarquias e Combos)**: Consultas de catálogo por categoria pai retornam 100% dos produtos ativos vinculados a qualquer nível de subcategoria descendente, e a disponibilidade calculada para qualquer Combo/Kit reflete com 100% de exatidão o gargalo de estoque de seus itens componentes.
- **SC-005 (Recuperação Integral de Estoque Reservado)**: 100% das reservas associadas a pagamentos recusados ou pedidos cancelados têm suas unidades devolvidas ao saldo disponível imediatamente após o evento de cancelamento/recusa, sem retenção residual de estoque.

## Assumptions

- **Moeda e Precisão Monetária**: Todos os preços do catálogo são expressos em Reais (`BRL`, `R$`) com duas casas decimais e arredondamento comercial padrão, não sendo permitido preço negativo ou zero para itens vendáveis avulsos.
- **Identificação Fiscal do Cliente**: Cada conta de cliente é associada a um CPF e a um endereço de e-mail únicos no sistema.
- **Escopo da Reserva de Estoque**: A reserva de estoque ocorre exclusivamente no início do processo de fechamento do pedido (checkout) e é identificada por uma chave única de operação/pedido fornecida pelo fluxo de checkout, garantindo idempotência caso o comando de confirmação ou liberação seja reenviado.
- **Integração entre os 3 Módulos da Equipe**: Este Módulo 1 fornece a fundação de identidade, catálogo, categorias, combos, estoque e auditoria consumida pelo Módulo 2 (Carrinho, Promoções, Elegibilidade e Cupons) e pelo Módulo 3 (Checkout, Pedidos, Pagamentos, Frete, Eventos e Relatórios).

