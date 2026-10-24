# Sistema de E-commerce com Promoções Avançadas Constitution

## Core Principles

### I. Arquitetura Orientada a Design Patterns e Extensibilidade (Aberto-Fechado)
O sistema DEVE ser projetado para evolução contínua das regras de negócio (novas campanhas promocionais, regras de preço, formas de pagamento, modalidades de frete e canais de notificação) sem transformar o código em sequências extensas de estruturas condicionais (`if/else` ou `switch`).
- Toda variação de regra comercial ou integração DEVE ser isolada por meio de polimorfismo e Design Patterns adequados, respeitando o Princípio Aberto-Fechado (OCP).
- A introdução de uma 5ª ou 6ª modalidade de promoção, novo provedor de pagamento, nova transportadora ou novo canal de notificação DEVE ocorrer pela adição de novas classes coesas, com impacto mínimo ou nulo sobre as classes existentes.
- Para cada Design Pattern aplicado, o código e a documentação DEVEM responder explicitamente: (1) Qual problema de projeto existia, (2) Por que este padrão foi escolhido e (3) O que ficou mais simples de modificar depois de sua utilização.

### II. Catálogo Obrigatório de Padrões e Metodologias
O projeto DEVE aplicar de maneira autêntica e justificada — resolvendo problemas reais do domínio de e-commerce e nunca criando classes artificiais apenas para exibir nomes de padrões — todas as metodologias e Design Patterns exigidos:
- **Padrões Criacionais, Fábricas e Resolução de Dependências**:
  - `Static Factory Method` (Objetos Static Factory): criação semântica e validada de entidades, Value Objects e resultados de domínio.
  - `Factory Method` e `Abstract Factory`: criação polimórfica de famílias de objetos relacionados (ex.: estratégias de pagamento, calculadoras de frete, canais de notificação e tipos de promoção).
  - `Dynamic Factory`: instanciação dinâmica baseada em metadados/configurações de campanhas, tipos de desconto e provedores registrados sem condicionais rígidas.
  - `Builder`: construção passo a passo e fluente de agregados complexos (Produtos com variações, Pedidos imutáveis, Campanhas promocionais compostas e Relatórios).
  - `Singleton`: gerenciamento controlado de instâncias únicas de registro/configuração quando aplicável ao domínio/infraestrutura.
  - `Dependency / Dynamic Injection` e `Service Locator`: injeção dinâmica de dependências e localização desacoplada de processadores, estratégias e adaptadores em tempo de execução.
- **Padrões Estruturais, Composição e Fronteira**:
  - `Composite` (Composição Recursiva): representação uniforme de estruturas em árvore, incluindo hierarquia recursiva de Categorias/Subcategorias, composição de Produtos em Combos/Kits e árvores de Condições de Elegibilidade (`AND`, `OR`, `NOT`).
  - `Decorator` e `Proxy` (Proxys e Decorators): empilhamento dinâmico de etapas na formação de preço/aplicação de descontos (`Decorator`) e interceptação transparente para controle de acesso, auditoria, cache ou validação (`Proxy`).
  - `Adapter` e `Anti-Corruption Layer` (Camada Anticorrupção): tradução e isolamento de interfaces heterogêneas de serviços externos simulados (gateways de pagamento como Cartão, PIX, Boleto e transportadoras externas), impedindo que modelos externos contaminem o domínio central.
  - `Facade`: interface simplificada e coesa para orquestrar subsistemas complexos (especialmente o fluxo de Checkout, coordenando catálogo, estoque, promoções, frete, pedido e pagamento).
  - `Flyweight`: compartilhamento eficiente de estado intrínseco imutável (ex.: metadados compartilhados de atributos de variação como tamanhos, cores, especificações técnicas ou regras estáticas de catálogo).
- **Padrões Comportamentais, Polimorfismo Avançado e Domínio**:
  - `Observer` (Padrão Observer): publicação e reação desacoplada a eventos de domínio (criação de pedido, confirmação de pagamento, envio, cancelamento), permitindo que estoque, notificações, relatórios e auditoria reajam sem acoplamento direto na classe emissora.
  - `Chain of Responsibility` (Cadeia de Responsabilidade): encadeamento de validadores de checkout, verificação de condições de elegibilidade, validação de cupons e resolução de conflitos entre promoções.
  - `State`: modelagem explícita do ciclo de vida do Pedido (`CRIADO`, `AGUARDANDO_PAGAMENTO`, `PAGO`, `EM_PREPARACAO`, `ENVIADO`, `ENTREGUE`, `CANCELADO`, `PAGAMENTO_RECUSADO`), garantindo apenas transições válidas e comportamentos específicos por estado.
  - `Template Method`, `Hook Method` e `Hook Template`: definição do esqueleto algorítmico invariante (ex.: processamento de checkout, cálculo base de campanha, fluxo de pagamento/cancelamento) com métodos gancho (`hook methods`) explícitos para customização pelas subclasses.
  - `Visitor` e `Double Dispatch`: execução de operações de cálculo de desconto, inspeção de elegibilidade, precificação e geração de relatórios sobre diferentes elementos da estrutura de carrinho/catálogo/pedido sem poluir as classes visitadas.
  - `Mediator`: coordenação centralizada da comunicação entre componentes interdependentes (ex.: interação entre itens do carrinho, motor de promoções, cupons e recalculador de frete).
  - `Null Object` e `Missing Object`: objetos polimórficos que representam a ausência segura de cupom, promoção, desconto, frete ou registro (`NoOp` / comportamento neutro), eliminando checagens defensivas de `null` espalhadas pelo código.
  - `Comparable`: ordenação determinística de promoções por prioridade, desempate pelo maior benefício financeiro ao cliente e ordenação de itens/ofertas.
  - `Operações Matemáticas`: encapsulamento explícito de aritmética monetária e percentual (uso estrito de `BigDecimal`, políticas de arredondamento, piso zero para evitar totais negativos e objetos de operação matemática combináveis).

### III. Código Explícito e Documentação Obrigatória em Todos os Métodos (INEGOCIÁVEL)
Todos os métodos, classes e interfaces do sistema DEVEM ser escritos de forma explícita e integralmente documentados via comentários no código-fonte:
- **Explicitude**: Todo método DEVE ter responsabilidade única, assinatura clara, visibilidade explícita e fluxo compreensível. É proibido ocultar regras de negócio críticas em anotações mágicas ou expressões obscuras sem método explícito nomeado.
- **Comentários Obrigatórios em Todos os Métodos**: Absolutamente TODOS os métodos (públicos, protegidos e privados) DEVEM possuir comentários explicativos (Javadoc e comentários internos quando houver passos de algoritmo) descrevendo:
  1. O objetivo funcional/comercial do método;
  2. O papel arquitetural que ele desempenha no Design Pattern correspondente (ex.: *"Método Hook do Template Method de Checkout"*, *"Concrete Decorator de desconto percentual"*, *"Handler da Cadeia de Responsabilidade de validação de cupom"*, *"Segundo despacho do Double Dispatch com Visitor"*);
  3. O significado dos parâmetros de entrada, o valor retornado e as exceções ou estados de retorno possíveis.

### IV. Integridade Transacional, Idempotência e Imutabilidade Comercial
As regras críticas de negócio do e-commerce DEVEM garantir consistência financeira e operacional em qualquer cenário de falha ou concorrência:
- **Snapshot Comercial Imutável no Pedido**: Ao concluir o checkout, o Pedido DEVE registrar de forma imutável os preços praticados, promoções aplicadas, valores de desconto concedidos por campanha, cupom utilizado, endereço de entrega e custo de frete vigentes no instante da compra. Alterações futuras no catálogo ou nas campanhas NÃO DEVEM alterar pedidos históricos.
- **Transparência na Formação do Preço**: Tanto o Carrinho quanto o Pedido DEVEM expor de forma clara e auditável a memória de cálculo do valor final: subtotal bruto dos itens, lista discriminada de descontos (promoções e cupons), valor do frete (ou indicação de frete grátis) e valor total líquido.
- **Reserva de Estoque e Controle de Concorrência**: A simples adição ao carrinho NÃO garante reserva. O estoque DEVE ser verificado e reservado atomicamente no fluxo de fechamento/checkout (com proteção contra condição de corrida para impedir que dois clientes comprem simultaneamente a última unidade). Caso o pagamento seja recusado ou o pedido seja cancelado em estado permitido, a reserva DEVE ser integralmente liberada.
- **Idempotência de Pagamentos**: O processamento de confirmações ou recusas de pagamento enviadas por serviços externos DEVE ser estritamente idempotente. O recebimento repetido da mesma confirmação de pagamento NÃO DEVE produzir dupla baixa de estoque, duplo estorno, duplicação de eventos ou transições repetidas no pedido.

### V. Separação Arquitetural em Camadas, Segurança e Auditoria
A aplicação DEVE ser implementada em **Java** com **Spring Boot**, utilizando **Spring Data JPA**, **Spring Security**, **API REST documentada via OpenAPI/Swagger** e banco de dados relacional (**PostgreSQL** ou **MySQL**):
- **Separação Estrita de Responsabilidades**: O código DEVE manter fronteiras claras entre: (1) Domínio (entidades, agregados, políticas, padrões de negócio), (2) Aplicação (casos de uso, orquestração), (3) Persistência (repositórios JPA, mapeamentos, migrações), (4) Interface HTTP (Controllers REST, DTOs de entrada/saída, tratamento de erros) e (5) Integrações Externas (adaptadores simulados de pagamento, frete e notificação). É proibida a criação de classes monolíticas que acumulem responsabilidades de múltiplos módulos.
- **Segurança e Privacidade**: Senhas NUNCA devem ser armazenadas em texto puro (uso obrigatório de algoritmo de hash seguro como BCrypt). Dados de pagamento utilizados em testes e demonstrações DEVEM ser estritamente fictícios. A autorização DEVE segregar rigorosamente ações de Clientes (acesso apenas aos próprios dados, endereços, carrinho e pedidos) e perfis Administrativos (gestão de catálogo, preços, estoque, promoções, pedidos e relatórios).
- **Auditoria Obrigatória**: Operações administrativas e comerciais sensíveis (alteração de preço, criação/alteração de campanhas promocionais, ajustes de estoque e cancelamento de pedidos) DEVEM gerar registros de auditoria identificando o usuário responsável, a operação realizada, os dados relevantes e o instante exato (`timestamp`).

### VI. Testes Automatizados de Comportamento e Documentação Arquitetural (UML)
A qualidade do sistema é medida pela verificabilidade dos seus comportamentos de negócio e pela clareza da sua documentação técnica:
- **Testes Automatizados (JUnit)**: O projeto DEVE conter suíte de testes automatizados cobrindo obrigatoriamente:
  1. Pelo menos 4 tipos de promoções de naturezas distintas funcionando isoladamente;
  2. Cenários com múltiplas promoções simultâneas (acumulação, exclusividade e resolução de conflitos por prioridade/maior desconto);
  3. Tentativa de compra com estoque insuficiente e proteção contra compra simultânea da última unidade;
  4. Rejeição de cupons inexistentes, expirados, esgotados ou incompatíveis com as condições de elegibilidade;
  5. Pagamento recusado com consequente liberação de reserva de estoque;
  6. Tentativa de transição inválida no ciclo de vida do pedido (padrão `State`);
  7. Recebimento duplicado da mesma confirmação de pagamento comprovando a idempotência.
- **Documentação e Diagramas UML**: O repositório DEVE manter documentação contendo descrição do sistema, modelo de domínio, casos de uso, **Diagrama de Classes UML**, **Diagrama de Estados do Ciclo de Vida do Pedido**, **Diagramas de Sequência** (checkout, aplicação de promoções e confirmação de pagamento) e o quadro justificativo de cada Design Pattern com suas classes participantes.

## Divisão de Trabalho em 3 Integrantes e Módulos do Domínio

Para garantir coesão arquitetural, paralelismo no desenvolvimento e evidência clara de autoria no histórico do Git, o projeto é estruturado em **3 frentes complementares** (uma por integrante), onde todos os membros compreendem a arquitetura global e aplicam a diretriz de métodos explícitos e comentados:

### 1. Integrante 1 — Identidade, Catálogo, Composição de Produtos, Estoque e Auditoria
- **Escopo de Domínio**:
  - Usuários, Autenticação e Autorização baseada em papéis (`CLIENTE`, `ADMIN`, perfis operacionais), Clientes e múltiplos Endereços de entrega;
  - Catálogo de Produtos, Variações (tamanho, cor, capacidade, etc.), Hierarquia de Categorias e Subcategorias, e composição de Combos/Kits;
  - Controle de Estoque, reserva atômica contra concorrência, confirmação de baixa e liberação de reserva;
  - Registro e consulta de Trilha de Auditoria de operações administrativas.
- **Padrões e Metodologias Foco**:
  - `Composite` / Composição Recursiva (árvore de categorias/subcategorias e produtos simples vs. combos/kits);
  - `Flyweight` (compartilhamento de atributos intrínsecos de variações e metadados de catálogo);
  - `Builder` (construção fluente e validada de Produtos, Variações, Clientes e Endereços);
  - `Static Factory Method` e `Factory Method` (criação semântica de itens de catálogo, reservas de estoque e entradas de auditoria);
  - `Proxy` (interceptação de operações administrativas para auditoria e controle de acesso);
  - `Singleton` (gerenciador/registro de configurações de catálogo e cache de metadados);
  - `Null Object` / `Missing Object` (representação segura de categoria raiz, variação padrão ou endereço não selecionado).

### 2. Integrante 2 — Carrinho, Motor de Promoções Avançadas, Elegibilidade, Cupons e Preço
- **Escopo de Domínio**:
  - Carrinho de Compras (inclusão, alteração, remoção e recálculo automático a cada mutação);
  - Motor de Promoções flexível (mínimo de 4 naturezas distintas: ex. leve X pague Y, percentual na 1ª compra ou por categoria, valor fixo acima de meta do carrinho, desconto em combo A+B, brinde ou frete grátis promocional);
  - Condições de Elegibilidade combináveis (primeira compra, valor mínimo, região, categoria, produto, período de vigência, perfil do cliente);
  - Resolução de Conflitos entre Promoções (regras de acumulação, exclusividade mútua, prioridade explícita e seleção do maior desconto);
  - Gestão e validação de Cupons Promocionais (código, validade, limite de uso, compatibilidade);
  - Pipeline de Formação de Preço transparente (subtotal, descontos detalhados, frete e total).
- **Padrões e Metodologias Foco**:
  - `Composite` / Composição Recursiva (composição lógica de múltiplas condições de elegibilidade);
  - `Chain of Responsibility` (pipeline de verificação de elegibilidade, validação de cupons e resolução de conflitos entre campanhas);
  - `Decorator` (encadeamento transparente de etapas de cálculo de preço, promoções e cupons sobre o valor base);
  - `Visitor` e `Double Dispatch` (avaliação e cálculo polimórfico de descontos sobre diferentes tipos de itens, categorias e combos do carrinho);
  - `Comparable` (ordenação determinística de campanhas por prioridade e benefício calculado);
  - `Operações Matemáticas` (objetos de valor monetário e estratégias de cálculo percentual/fixo/progressivo com `BigDecimal`);
  - `Dynamic Factory` e `Dynamic Injection` (construção e injeção dinâmica de regras promocionais configuradas pelos administradores);
  - `Null Object` / `Missing Object` (Promoção Nula e Cupom Nulo quando nenhum benefício se aplica).

### 3. Integrante 3 — Checkout, Ciclo de Vida de Pedidos, Pagamentos, Frete, Eventos e Relatórios
- **Escopo de Domínio**:
  - Processo de Checkout (detecção de mudanças de preço/promoção/estoque desde a adição ao carrinho e conversão consistente em pedido);
  - Agregado de Pedido com Snapshot Comercial Imutável e Máquina de Estados do Ciclo de Vida;
  - Integração de Pagamentos simulados (Cartão de Crédito fictício, PIX, Boleto) com garantia explícita de Idempotência em confirmações repetidas;
  - Cálculo de Entrega e Frete (múltiplas transportadoras/modalidades simuladas por região, peso, valor ou regra de frete grátis);
  - Cancelamento de Pedidos (validação de estado permitido, liberação de estoque, estorno simulado e histórico) e Devolução parcial/total;
  - Barramento de Eventos do Sistema, Notificações desacopladas ao cliente e Relatórios Gerenciais (total de vendas, ticket médio, produtos mais vendidos, descontos concedidos por campanha e cancelamentos).
- **Padrões e Metodologias Foco**:
  - `Facade` (orquestração limpa do Checkout integrando validação de catálogo, reserva de estoque, motor de preço, frete e criação do pedido);
  - `State` (controle estrito das transições válidas e ações permitidas em cada estado do Pedido);
  - `Adapter` e `Anti-Corruption Layer` (integração desacoplada com gateways externos de pagamento e transportadoras de frete);
  - `Abstract Factory` e `Service Locator` (resolução de famílias de provedores de pagamento, entrega e canais de notificação);
  - `Template Method`, `Hook Method` e `Hook Template` (fluxo padronizado de processamento de checkout, pagamento e cancelamento com ganchos de extensão);
  - `Observer` (disparo de eventos de domínio na confirmação de pagamento, envio e cancelamento para múltiplos ouvintes independentes);
  - `Mediator` (coordenação das interações entre pedido, pagamento, estoque, frete e notificações durante o checkout e pós-venda).

## Fluxo de Desenvolvimento Incremental e Qualidade

### 1. Etapas de Evolução Executável
O sistema DEVE permanecer compilável, testável e executável ao final de cada incremento:
- **Incremento 1 — Fundação e Catálogo**: Autenticação/autorização, clientes, endereços, catálogo de produtos com variações, categorias recursivas, combos e carrinho básico.
- **Incremento 2 — Estoque, Checkout e Pedidos**: Reserva concorrente de estoque, fluxo de checkout (`Facade`), pedido com snapshot imutável e ciclo de vida (`State`).
- **Incremento 3 — Motor de Promoções e Formação de Preço**: Condições de elegibilidade (`Composite`/`Chain of Responsibility`), pelo menos 4 promoções distintas, resolução de conflitos (`Comparable`), cupons e composição do preço (`Decorator`/`Visitor`/`Double Dispatch`).
- **Incremento 4 — Pagamentos, Entrega, Eventos e Gestão**: Meios de pagamento idempotentes (`Adapter`/`Anti-Corruption`/`Template Method`), cálculo de frete e frete grátis, cancelamento/devolução, eventos e notificações (`Observer`/`Mediator`), auditoria, relatórios gerenciais e diagramas UML finais.

### 2. Preparação para Alteração de Requisito na Apresentação
- Cada ponto de variação da arquitetura (nova promoção, nova condição de elegibilidade, novo meio de pagamento, nova transportadora, novo canal de notificação ou novo estado/regra de pedido) DEVE possuir um contrato claro (interface ou classe abstrata documentada).
- Na documentação e nos comentários das classes base, DEVE constar um guia rápido de extensibilidade indicando exatamente quais classes criar ou registrar caso um novo requisito seja solicitado durante a defesa do trabalho.

## Governance

Esta Constituição estabelece as regras arquiteturais, técnicas e acadêmicas inegociáveis do projeto **Sistema de E-commerce com Promoções Avançadas** e prevalece sobre quaisquer práticas informais.

1. **Procedimento de Emenda**: Qualquer proposta de alteração nesta Constituição DEVE ser documentada com justificativa técnica, validada contra os requisitos da disciplina e aprovada pelos 3 integrantes da equipe antes de sua aplicação.
2. **Política de Versionamento**: A versão desta Constituição segue o padrão SemVer (`MAJOR.MINOR.PATCH`):
   - `MAJOR`: Remoção ou alteração incompatível de princípios fundamentais, padrões obrigatórios ou divisão de módulos.
   - `MINOR`: Inclusão de novos princípios, novos padrões de projeto ou detalhamento estrutural de governança.
   - `PATCH`: Ajustes textuais, correções ortográficas ou esclarecimentos que não alterem as exigências existentes.
3. **Verificação de Conformidade (Quality Gates)**:
   - Todo commit e Pull Request DEVE manter a aplicação executável e todos os testes JUnit passando.
   - O histórico de commits do Git DEVE refletir a contribuição real e contínua dos 3 integrantes da equipe.
   - Nenhuma classe ou método pode ser integrado sem comentários explícitos explicando sua lógica e seu papel no Design Pattern adotado.

**Version**: 1.0.0 | **Ratified**: 2026-10-24 | **Last Amended**: 2026-10-24
