# Research & Technical Decisions: Módulo 1 — Identidade, Catálogo, Composição de Produtos, Estoque e Auditoria

**Feature**: `001-catalogo-estoque-identidade`  
**Date**: 2026-10-26  
**Status**: Complete (All technical unknowns resolved)

---

## R1. Stack Tecnológica, Build e Persistência

- **Decision**: Utilizar **Java 21**, **Spring Boot 3.4.x** gerenciado via **Maven Wrapper (`mvnw`)**, **Spring Data JPA (Hibernate 6)**, **Spring Security 6** (com hash `BCryptPasswordEncoder` e controle de acesso baseado em papéis), **Bean Validation (`jakarta.validation`)**, **Springdoc OpenAPI 2 (`swagger-ui`)**, **Flyway** para versionamento de schema SQL, **PostgreSQL** como banco relacional principal e **H2 Database** (em modo de compatibilidade PostgreSQL) para execução autônoma da suíte de testes automatizados **JUnit 5**.
- **Rationale**: Atende integralmente às restrições da Constituição (Princípio V), permite que os 3 integrantes rodem o projeto e os testes imediatamente após clonar o repositório (sem depender de instalação manual do Maven graças ao `mvnw` nem de infraestrutura externa para rodar `mvnw test`), e gera documentação interativa automática da API REST via Swagger.
- **Alternatives considered**:
  - *Gradle*: descartado em favor do Maven Wrapper (`mvnw`) por maior padronização acadêmica na FATEC e simplicidade declarativa do `pom.xml`.
  - *Senhas com SHA-256 simples*: descartado porque hashes rápidos são vulneráveis a força bruta; `BCrypt` inclui *salt* automático e fator de custo adaptativo.

---

## R2. Controle de Concorrência em Reserva de Estoque e Prevenção de Overselling

- **Decision**: Adotar controle de concorrência híbrido no banco de dados combinando:
  1. **Atualização Condicional Atômica + Bloqueio Pessimista/Otimista JPA** (`@Version` em `ItemEstoqueEntity` e `@Lock(LockModeType.PESSIMISTIC_WRITE)` na operação de reserva de checkout por SKU);
  2. **Ordenação Determinística de SKUs** (ordem alfabética crescente de SKU) antes de adquirir locks ao reservar **Combos/Kits** com múltiplos itens componentes, eliminando qualquer possibilidade de *deadlock* entre transações concorrentes;
  3. **Chave de Idempotência (`chaveOperacao` / `pedidoRef`)** única na tabela `reservas_estoque` com controle de estados (`ATIVA`, `CONFIRMADA`, `LIBERADA`, `EXPIRADA`) e prazo de validade (`expiraEm`, padrão de 30 minutos).
- **Rationale**: Garante matematicamente que duas requisições simultâneas disputando a última unidade física nunca aprovem duas reservas (atendendo a `FR-010` e `SC-001`), preserva atomicidade tudo-ou-nada para Combos/Kits e impede que retentativas de confirmação ou cancelamento dupliquem baixas ou devoluções de estoque (`FR-011`).
- **Alternatives considered**:
  - *Sincronização apenas em memória (`synchronized` no Java)*: descartada por não proteger a integridade no nível transacional do banco de dados nem escalar para múltiplas instâncias.
  - *Verificação simples `if (estoque.getDisponivel() >= qtd)` sem lock*: descartada por sofrer condição de corrida clássica (*Time-Of-Check to Time-Of-Use* — TOCTOU) quando dois clientes clicam em finalizar compra simultaneamente.

---

## R3. Aplicação Autêntica dos Design Patterns do Integrante 1

Para cumprir os Princípios I, II e III da Constituição, cada padrão exigido para o Integrante 1 resolve um problema concreto do domínio e responde às três perguntas fundamentais (problema, justificativa e ganho de evolutividade):

### 1. Padrão `Composite` (Composição Recursiva)
- **Decision**: Aplicar `Composite` em duas estruturas hierárquicas reais do módulo:
  1. **Hierarquia de Categorias (`ComponenteCategoria`)**: interface comum implementada por nós compostos (`CategoriaComp`) e folhas, permitindo adicionar subcategorias em qualquer profundidade, validar ausência de ciclos e coletar recursivamente todos os produtos/SKUs de uma categoria e suas descendentes (`listarTodosProdutosRecursivo()`).
  2. **Composição de Ofertas do Catálogo (`ItemCatalogoComponent`)**: interface comum implementada por `ProdutoVariacaoLeaf` (item individual vendável) e `ComboKitComposite` (pacote formado por uma lista de `ItemComponenteKit`, cada um referenciando outro `ItemCatalogoComponent` e sua quantidade). O `ComboKitComposite` calcula recursivamente:
     - Peso físico total e volume/dimensões agregadas;
     - Disponibilidade máxima em estoque ($\min(\lfloor \text{estoqueDisponivel}_i / \text{quantidadeNoKit}_i \rfloor)$);
     - Preço comercial (calculado recursivamente pela soma dos componentes com desconto opcional do kit OU preço fixo dedicado, conforme clarificado em `FR-008`).
- **Rationale**: Elimina condicionais `if (item instanceof Combo)` espalhadas pelo catálogo, carrinho e cálculo de frete/estoque, tratando produtos simples e kits aninhados de forma 100% uniforme.
- **Alternatives considered**: Tabela plana relacionando apenas produtos avulsos sem interface polimórfica recursiva (rejeitada por impedir kits contendo subkits e exigir tratamento condicional duplicado).

### 2. Padrão `Flyweight` (`AtributoVariacaoFlyweight` + `FabricaAtributoVariacaoFlyweight`)
- **Decision**: Isolar os metadados intrínsecos e imutáveis de atributos de variação (ex.: `TipoAtributo = COR, Valor = "Preto", CodigoHex = "#000000"`; `TipoAtributo = TAMANHO, Valor = "G", DescricaoPadrao = "Grande"`; `TipoAtributo = CAPACIDADE, Valor = "256GB"`) em objetos `AtributoVariacaoFlyweight` gerenciados por um pool central (`FabricaAtributoVariacaoFlyweight`). Cada `VariacaoProduto` armazena apenas seu estado extrínseco exclusivo (SKU, ajuste de preço, referência de estoque) e aponta para as instâncias compartilhadas do `Flyweight`.
- **Rationale**: Em um catálogo com milhares de variações de roupas e eletrônicos, evita a proliferação de objetos duplicados idênticos para os mesmos tamanhos, cores e especificações técnicas.
- **Alternatives considered**: Instanciar novas strings e objetos de especificação duplicados dentro de cada linha de variação em memória (rejeitado por desperdício de memória e ausência de padronização).

### 3. Padrão `Builder` (`ProdutoBuilder`, `ComboKitBuilder`, `ClienteBuilder`, `EnderecoBuilder`)
- **Decision**: Implementar Builders explícitos e fluentes para a construção passo a passo de agregados com múltiplos atributos obrigatórios e opcionais (`Produto`, `ComboKitComposite`, `Cliente` e `Endereco`), concentrando todas as validações de consistência estrutural no método explícito `construir()`.
- **Rationale**: Evita construtores telescópicos com 10+ parâmetros propensos a inversão acidental de argumentos (como altura, largura, profundidade, peso, marca, descrição) e garante que nenhum objeto de domínio nasça em estado inválido.
- **Alternatives considered**: Construtores com muitos argumentos ou uso de setters soltos que permitem objetos parcialmente inicializados (rejeitado por violar encapsulamento e segurança de estado).

### 4. Padrão `Static Factory Method` (Objetos Static Factory)
- **Decision**: Adotar métodos fábrica estáticos com nomes expressivos de domínio em Value Objects e entidades, tais como:
  - `Usuario.criarContaClientePublico(...)` vs. `Usuario.criarContaOperadorInterno(...)` (reforçando a regra de segurança de `FR-001` e `FR-002`);
  - `ReservaEstoque.criarReservaAtivaComExpiracao(...)`;
  - `RegistroAuditoria.registrarMudancaDePreco(...)` e `RegistroAuditoria.registrarAjusteDeEstoque(...)`;
  - `DimensoesFisicas.emCentimetrosEGramas(...)`.
- **Rationale**: Torna a intenção de negócio explícita no ponto de chamada (muito mais legível do que múltiplos construtores `new Usuario(...)` sobrecarregados) e permite controlar o retorno de instâncias cacheadas ou especializadas.
- **Alternatives considered**: Construtores públicos sobrecarregados sem semântica explícita no nome.

### 5. Padrão `Factory Method` (`CriadorItemCatalogo` e subclasses)
- **Decision**: Definir uma hierarquia abstrata `CriadorItemCatalogo` com o método fábrica `criarItemCatalogo(DadosCadastroItemCommand)` implementado por criadores concretos especializados (`CriadorProdutoFisicoPadrao`, `CriadorProdutoComVariacoes`, `CriadorComboKitPromocional`).
- **Rationale**: Permite adicionar futuramente novos tipos de produtos ao catálogo (como produtos perecíveis, livros ou assinaturas, conforme exigido no enunciado) criando apenas uma nova subclasse criadora, sem modificar o código de orquestração existente.
- **Alternatives considered**: Um único método gigante com `switch (tipoProduto)` para instanciar e configurar cada tipo de item (rejeitado por violar o Princípio Aberto-Fechado).

### 6. Padrão `Proxy` (`CatalogoAdminAuditoriaProxy` e `EstoqueAdminSegurancaProxy`)
- **Decision**: Implementar Proxies explícitos que envolvem os serviços reais de administração de catálogo (`ServicoAdminCatalogoReal`) e de estoque (`ServicoAdminEstoqueReal`). Antes de delegar a execução ao objeto real, o Proxy valida o contexto de segurança/perfil do operador; após a execução bem-sucedida, o Proxy captura automaticamente o estado anterior e o estado posterior e persiste o `RegistroAuditoria` imutável.
- **Rationale**: Desacopla completamente a lógica de negócio de catálogo/estoque da responsabilidade transversal de verificação de acesso e gravação de trilha de auditoria (`FR-012`), mantendo cada classe com responsabilidade única.
- **Alternatives considered**: Espalhar chamadas manuais `auditoriaRepository.save(...)` e checagens de permissão dentro de todos os métodos de negócio de produto, variação, categoria e estoque (rejeitado por gerar duplicação e risco de esquecimento).

### 7. Padrão `Singleton` (`ConfiguracaoModuloCatalogoEstoqueSingleton`)
- **Decision**: Implementar um `Singleton` explícito e *thread-safe* para manter as políticas globais do módulo (TTL padrão de reserva de estoque = 30 minutos, limite máximo de profundidade de árvore de categorias/combos = 10 níveis, moeda padrão = `BRL`) e prover acesso centralizado ao registro de `Flyweights` de atributos de variação.
- **Rationale**: Garante um ponto único e consistente de consulta das diretrizes operacionais do módulo em toda a aplicação, demonstrando a implementação explícita e segura do padrão `Singleton`.
- **Alternatives considered**: Constantes mágicas espalhadas em várias classes utilitárias sem ponto central de configuração.

### 8. Padrão `Null Object` / `Missing Object` (`EnderecoAusenteNullObject`, `CategoriaRaizNullObject`)
- **Decision**: Implementar objetos nulos polimórficos:
  - `EnderecoAusenteNullObject` (implementa `OpcaoEnderecoEntrega`): retornado quando o cliente ainda não cadastrou ou não selecionou endereço de entrega (`FR-004`), informando `possuiEnderecoValido() == false` e resumo `"Nenhum endereço de entrega selecionado"`;
  - `CategoriaRaizNullObject` (implementa `ComponenteCategoria`): representa o pai de categorias de primeiro nível, eliminando `if (categoria.getPai() != null)` ao montar caminhos hierárquicos (`getCaminhoCompleto()`).
- **Rationale**: Elimina `NullPointerException` e condicionais defensivas repetitivas ao navegar pela árvore de categorias ou consultar o endereço ativo do cliente no carrinho/checkout.
- **Alternatives considered**: Retornar `null` e obrigar todos os consumidores (Módulos 1, 2 e 3) a testar `!= null` antes de cada chamada.

---

## R4. Contrato de Integração Desacoplada com os Integrantes 2 e 3

- **Decision**: Expor duas interfaces de contrato explícitas no pacote `br.edu.fatec.ecommerce.modulo1.integracao`:
  1. `PortaConsultaCatalogoCliente` (consumida pelos Módulos 2 e 3): consulta dados consolidados de qualquer `ItemCatalogoComponent` por código/SKU (preço vigente, peso total, dimensões, árvore de códigos de categorias ascendentes para elegibilidade de promoções e disponibilidade em estoque) e dados do cliente/endereço selecionado (incluindo se é primeira compra e UF/região de entrega).
  2. `PortaComandoReservaEstoque` (consumida pelo Módulo 3 no Checkout/Pagamento/Cancelamento): operações transacionais `reservarItensParaCheckout(...)`, `confirmarBaixaDefinitiva(...)`, `liberarReserva(...)` e `expirarReservasVencidas()`.
- **Rationale**: Permite que os 3 integrantes desenvolvam e testem seus módulos em paralelo com contratos claros, sem acoplamento às tabelas ou entidades JPA internas do Módulo 1 (`FR-013`).
- **Alternatives considered**: Permitir que os Módulos 2 e 3 acessem diretamente os repositórios JPA do Módulo 1 (rejeitado por violar o encapsulamento modular e a Constituição).

