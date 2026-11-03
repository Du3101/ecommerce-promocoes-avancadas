# Feature Specification: Módulo 2 — Carrinho, Motor de Promoções Avançadas, Elegibilidade, Cupons e Formação de Preço

**Feature Branch**: `002-carrinho-promocoes-cupons`  
**Created**: 2026-11-03  
**Status**: Draft  
**Integrante 2**: Eduardo Juncker Toras  
**Input**: "Módulo 2 (Integrante 2 — Eduardo Juncker Toras): Carrinho de Compras, Motor de Promoções Avançadas, Elegibilidade, Cupons e Formação de Preço. O módulo deve contemplar: (1) Carrinho de compras do cliente com adição, alteração, remoção de itens e recálculo automático; (2) Motor de promoções com no mínimo 4 naturezas distintas (Leve X Pague Y, percentual condicional, desconto fixo por meta de carrinho e desconto em combo cruzado); (3) Árvore recursiva de condições de elegibilidade (Composite com AND, OR, NOT e predicados de cliente, valor e categoria); (4) Validação de cupons e resolução determinística de conflitos entre promoções acumulativas e exclusivas (Comparable e Chain of Responsibility); (5) Pipeline transparente de formação de preço (Decorator, Visitor e Double Dispatch com operações BigDecimal e piso zero); (6) Objetos nulos para ausência de desconto/cupom (Null Object); (7) Integração desacoplada com o Módulo 1 (PortaConsultaCatalogoCliente) e exposição de porta para o Módulo 3 (Checkout)."

---

## Clarifications

### Session 2026-11-03

- Q: Como o Carrinho obtém informações sobre itens do catálogo e perfil do cliente sem violar o desacoplamento arquitetural? → A: O Módulo 2 consome estritamente a interface Java [`PortaConsultaCatalogoCliente`](file:///c:/Users/Edu/ecommerce-promocoes-avancadas/src/main/java/br/edu/fatec/ecommerce/modulo1/integracao/PortaConsultaCatalogoCliente.java) exposta pelo Módulo 1, sem dependência direta de entidades JPA ou tabelas de banco do Módulo 1.
- Q: O que ocorre quando múltiplas promoções exclusivas são elegíveis simultaneamente? → A: A resolução de conflitos aplica a ordenação determinística de `Comparable<Promocao>`: prevalece a promoção com maior prioridade numérica configurada; em caso de empate de prioridade, prevalece a que proporcionar o maior valor absoluto de desconto financeiro para o cliente. Se ambas forem acumulativas, aplicam-se em sequência respeitando o piso zero.
- Q: Como evitar que cupons e promoções reduzam o valor total do carrinho abaixo de zero? → A: A camada de operações monetárias (`OperacoesMonetarias`) garante que qualquer desconto aplicado sobre o subtotal tenha piso zero (`Math.max(0, subtotal - descontos)`), impedindo saldos finais negativos.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gestão do Carrinho de Compras e Recálculo Automático por Mutação (Priority: P1)

Um cliente autenticado ou visitante adiciona itens comercializáveis (produtos com variação específica ou Combos/Kits) ao seu carrinho de compras informando o SKU e a quantidade desejada. O cliente pode alterar quantidades, remover itens ou esvaziar o carrinho. A cada mutação (adição, alteração ou exclusão), o carrinho recalcula automaticamente seu subtotal bruto, quantidade total de itens, peso total agregado e reavalia imediatamente a incidência de promoções e cupons aplicáveis.

**Why this priority**: É a porta de entrada para qualquer jornada de compra. Sem o carrinho persistente e seu recálculo automático e transparente a cada mutação, o cliente não consegue selecionar produtos nem visualizar os benefícios promocionais.

**Independent Test**: Pode ser testada criando um carrinho, adicionando dois produtos e um combo, alterando a quantidade de um dos itens e verificando se os subtotais bruto e recalculado, bem como a lista de itens e o peso total, são computados corretamente a cada passo.

**Acceptance Scenarios**:

1. **Given** um cliente autenticado com um carrinho vazio, **When** adiciona 2 unidades do SKU `DEMO-TEC-PRO-PRETO` (R$ 300,00 cada), **Then** o carrinho armazena o item, registra o subtotal bruto de R$ 600,00 e o peso de 1700g, recalculando o estado geral.
2. **Given** um carrinho contendo 2 unidades de um produto, **When** o cliente altera a quantidade para 4 unidades, **Then** a linha do item é atualizada e o subtotal do carrinho passa a refletir exatamente o valor de 4 unidades.
3. **Given** um carrinho com itens cadastrados, **When** o cliente remove um dos itens ou solicita a limpeza completa, **Then** o item é descartado, o subtotal é reajustado e eventuais promoções que dependiam dele têm sua aplicação automaticamente revogada.
4. **Given** uma tentativa de adicionar um SKU inexistente ou inativo no catálogo consultado via `PortaConsultaCatalogoCliente`, **When** a solicitação é processada, **Then** a inclusão é rejeitada com mensagem de erro descritiva sem corromper o estado do carrinho.

---

### User Story 2 - Motor de Promoções com no Mínimo 4 Naturezas Distintas (Priority: P1)

O sistema de promoções permite criar, configurar e aplicar regras de desconto de pelo menos quatro naturezas comerciais completamente diferentes:
1. **Leve X Pague Y**: Na compra de $X$ unidades de produtos de uma categoria ou SKU, o cliente paga apenas $Y$ unidades (sendo as unidades excedentes bonificadas com 100% de desconto sobre as de menor valor);
2. **Desconto Percentual Condicional**: Concessão de percentual de desconto (ex.: 15%) caso o cliente esteja em sua primeira compra ou caso os produtos pertençam a uma categoria promocional;
3. **Desconto em Valor Fixo por Meta de Carrinho**: Abatimento fixo (ex.: R$ 50,00) caso o subtotal bruto dos itens elegíveis atinja ou supere um valor mínimo (ex.: R$ 300,00);
4. **Desconto em Combo Cruzado / Venda Casada**: Concessão de desconto percentual ou fixo sobre itens específicos quando comprados em conjunto (ex.: Compre Teclado + Mouse e obtenha 20% de desconto no conjunto).

**Why this priority**: É o requisito acadêmico e comercial central do trabalho: demonstrar flexibilidade algorítmica para suportar diferentes mecânicas de promoção sem estruturas condicionais engessadas.

**Independent Test**: Pode ser testada configurando individualmente cada uma das quatro modalidades de promoção, submetendo carrinhos com produtos que atendam aos critérios de cada modalidade e comprovando a exatidão matemática do benefício concedido em cada caso.

**Acceptance Scenarios**:

1. **Given** uma promoção ativa "Leve 3 Pague 2 em Periféricos", **When** o cliente adiciona 3 mouses de R$ 150,00 ao carrinho, **Then** o sistema cobra 2 unidades (R$ 300,00) e concede R$ 150,00 de desconto discriminado sob a rubrica da promoção.
2. **Given** uma promoção de 15% de desconto para primeira compra de novos clientes, **When** um cliente elegível (confirmado via `PortaConsultaCatalogoCliente`) fecha um carrinho de R$ 400,00, **Then** é aplicado um desconto de R$ 60,00.
3. **Given** uma promoção de R$ 50,00 OFF para compras acima de R$ 300,00, **When** o carrinho atinge R$ 350,00, **Then** o desconto de R$ 50,00 é deduzido do total; caso o carrinho tenha R$ 250,00, nenhum desconto é concedido.
4. **Given** uma promoção de venda casada (Combo Teclado + Mouse com 20% OFF no par), **When** o cliente tem ambos os produtos no carrinho, **Then** o desconto de 20% é calculado sobre a soma dos pares casados.

---

### User Story 3 - Composição Recursiva de Condições de Elegibilidade com Padrão Composite (Priority: P1)

Cada campanha promocional pode exigir regras de elegibilidade complexas combinando múltiplos predicados através de operadores lógicos booleanos recursivos (`AND`, `OR`, `NOT`). As condições atômicas incluem: valor mínimo do carrinho, primeira compra do cliente, pertinência a categorias/subcategorias (verificada recursivamente no catálogo), SKU de produto específico e UF de entrega do cliente. A árvore de condições avalia uniformemente a elegibilidade através do padrão `Composite`.

**Why this priority**: Evita `if/else` complexos e espalhados pelo código, permitindo que regras de negócio ricas (ex.: "(Cliente Primeira Compra AND Valor > 200) OR (Categoria Eletrônicos AND Região SP)") sejam montadas dinamicamente e avaliadas com elegância polimórfica.

**Independent Test**: Pode ser testada montando uma árvore com `CondicaoAndComposite` contendo `CondicaoValorMinimo` e `CondicaoPrimeiraCompra`, além de uma `CondicaoOrComposite` aninhada com `CondicaoCategoria`, e verificando que a promoção só é ativada quando a expressão lógica completa resulta em verdadeiro.

**Acceptance Scenarios**:

1. **Given** uma promoção cuja condição é um `CondicaoAndComposite` entre `ValorMinimo(200)` e `PrimeiraCompraCliente()`, **When** um cliente em sua primeira compra possui R$ 250,00 no carrinho, **Then** a condição retorna `true` e a promoção é considerada elegível.
2. **Given** a mesma condição anterior, **When** um cliente recorrente (não primeira compra) possui R$ 300,00 no carrinho, **Then** a condição retorna `false` e a promoção é rejeitada.
3. **Given** uma condição com `CondicaoNotComposite` negando uma determinada UF (ex.: frete ou promoção não válida para RJ), **When** o cliente possui endereço de entrega em SP, **Then** a negação é avaliada como verdadeira e a promoção permanece elegível.

---

### User Story 4 - Resolução Determinística de Conflitos, Cupons e Ordenação com Comparable e Chain of Responsibility (Priority: P1)

Quando o cliente é elegível a múltiplas promoções simultâneas ou insere um cupom de desconto, o sistema precisa arbitrar conflitos e determinar quais benefícios serão efetivamente aplicados:
1. **Regras de Acumulação e Exclusividade**: Promoções e cupons podem ser configurados como *Acumulativos* ou *Exclusivos*;
2. **Ordenação Determinística (`Comparable`)**: Promoções implementam `Comparable<Promocao>`, sendo ordenadas prioritariamente pelo nível de prioridade cadastrado e, em caso de empate, pelo maior benefício financeiro proporcionado ao cliente;
3. **Cadeia de Responsabilidade (`Chain of Responsibility`)**: Encadeia validadores de integridade do carrinho, elegibilidade temporal e de uso de cupons, exclusividade mútua e limite de abatimento;
4. **Validação de Cupons Promocionais**: Cupons possuem código alfanumérico, prazo de validade, quantidade máxima de utilizações globais e por cliente, valor mínimo de carrinho e vínculo opcional a campanhas exclusivas.

**Why this priority**: Garante justiça financeira e previsibilidade: o cliente sempre recebe o melhor desconto permitido pelas regras comerciais sem gerar prejuízos para o lojista decorrentes de acumulação indevida de descontos.

**Independent Test**: Pode ser testada submetendo um carrinho elegível a duas promoções exclusivas distintas com prioridades diferentes, confirmando que apenas a de maior prioridade/maior desconto é aplicada, e testando cupons válidos, vencidos e esgotados.

**Acceptance Scenarios**:

1. **Given** duas promoções ativas marcadas como exclusivas (uma concede R$ 40,00 e outra R$ 60,00), **When** ambas são elegíveis para o carrinho, **Then** o sistema aplica deterministicamente a promoção de R$ 60,00 e descarta a de R$ 40,00.
2. **Given** uma promoção exclusiva já aplicada, **When** o cliente insere um cupom que não permite acumulação com outras promoções, **Then** a Cadeia de Responsabilidade compara os benefícios e aplica apenas o mais vantajoso, informando a regra de exclusividade ao cliente.
3. **Given** um cupom promocional com limite de 100 usos já esgotado ou com data de validade expirada, **When** o cliente tenta aplicá-lo, **Then** o cupom é rejeitado com mensagem explicativa e nenhum desconto é computado.

---

### User Story 5 - Pipeline de Formação de Preço Transparente (Decorator, Visitor e Operações BigDecimal) (Priority: P2)

A precificação final do carrinho é montada através de um pipeline transparente:
1. **Padrão Decorator**: Camadas sucessivas decoram o subtotal base (`PrecoBaseCarrinho`, `DescontoPromocaoDecorator`, `DescontoCupomDecorator`), mantendo uma memória de cálculo auditável com valor bruto, cada parcela de desconto discriminada e valor líquido final;
2. **Padrão Visitor e Double Dispatch**: A avaliação e cálculo de descontos sobre itens do carrinho utilizam o padrão `Visitor` (`ElementoCarrinhoVisitor`) e duplo despacho (`item.aceitar(visitor)`), permitindo que itens unitários e combos sejam visitados de acordo com sua semântica sem uso de `instanceof`;
3. **Padrão Null Object**: A ausência de desconto ou cupom aplicável é representada por instâncias de `PromocaoNula` e `CupomNulo`, evitando cheques defensivos de `null`;
4. **Aritmética Segura com Piso Zero**: Cálculos utilizam rigorosamente `BigDecimal` com arredondamento padronizado (`RoundingMode.HALF_EVEN`), e o valor líquido final é travado em um piso de `BigDecimal.ZERO` (nunca negativo).

**Why this priority**: Fornece clareza contábil e matemática inquestionável para o cliente e para a contabilidade da loja, preparando o snapshot comercial imutável exigido no checkout pelo Módulo 3.

**Independent Test**: Pode ser testada inspecionando o objeto de resultado da formação de preço, validando cada camada do Decorator, conferindo o arredondamento de centavos e garantindo que descontos que superem o subtotal resultem exatamente em zero.

**Acceptance Scenarios**:

1. **Given** um subtotal bruto de R$ 200,00, uma promoção que concede R$ 30,00 e um cupom que concede R$ 20,00, **When** o pipeline de formação de preço executa, **Then** o resultado expõe: subtotal bruto R$ 200,00, desconto de promoção R$ 30,00, desconto de cupom R$ 20,00 e total líquido R$ 150,00.
2. **Given** uma situação excepcional em que a soma dos descontos totalize R$ 250,00 para um carrinho de R$ 200,00, **When** o pipeline calcula o total líquido, **Then** o valor final retornado é exatamente R$ 0,00 (piso zero) sem valores negativos.
3. **Given** um carrinho sem nenhuma promoção ou cupom elegível, **When** a precificação é calculada, **Then** o sistema manipula `PromocaoNula` e `CupomNulo`, retornando desconto zero sem lançar `NullPointerException`.

---

### User Story 6 - Gestão de Campanhas e Cupons via API REST e Porta de Integração para Checkout (Priority: P2)

Usuários com perfil `RESPONSAVEL_PROMOCOES` ou `ADMIN` gerenciam o ciclo de vida de campanhas promocionais e cupons via API REST (criação com parâmetros dinâmicos via `Dynamic Factory`, listagem, ativação, desativação e consulta de métricas de uso). Além disso, o Módulo 2 disponibiliza a interface Java `PortaConsultaCarrinhoPromocoes` para que o **Módulo 3 (Checkout/Pedidos/Frete)** obtenha o snapshot consolidado dos itens e do preço promocional para fechar o pedido.

**Why this priority**: Fecha o ciclo de governança operacional e garante a integração desacoplada exigida pela Constituição com o Módulo 3.

**Independent Test**: Pode ser testada autenticando-se com perfil `RESPONSAVEL_PROMOCOES`, cadastrando uma nova campanha via endpoint REST, ativando-a e, em seguida, consumindo a porta Java para obter o resumo do carrinho com o desconto aplicado.

**Acceptance Scenarios**:

1. **Given** um operador com perfil `RESPONSAVEL_PROMOCOES`, **When** envia requisição REST para cadastrar uma nova campanha promocional com vigência e condições de elegibilidade, **Then** a campanha é persistida e registrada na fábrica dinâmica de promoções.
2. **Given** um usuário com perfil `CLIENTE`, **When** tenta invocar endpoints administrativos de cadastro de campanhas ou cupons, **Then** a requisição é rejeitada com status HTTP 403 Forbidden.
3. **Given** o Módulo 3 iniciando o checkout de um cliente, **When** consome `PortaConsultaCarrinhoPromocoes.obterResumoCarrinhoParaCheckout(clienteId)`, **Then** recebe um DTO imutável com a lista de SKUs, quantidades, subtotal bruto, total de descontos aplicados e total líquido.

---

### Edge Cases

- **Carrinho vazio**: O recálculo de um carrinho sem itens deve retornar subtotal `0.00`, descontos `0.00` e total `0.00`, sem disparar avaliações desnecessárias no motor de promoções.
- **Divisão com dízima periódica em rateio de descontos**: Quando um desconto em valor fixo (ex.: R$ 10,00) for rateado entre 3 itens, os centavos residuais decorrentes de arredondamento devem ser balanceados para que a soma dos descontos individuais seja exatamente igual a R$ 10,00.
- **Concorrência em uso de cupom limitado**: Se restar apenas 1 uso disponível para um cupom e dois clientes tentarem aplicá-lo simultaneamente no checkout, a validação deve garantir que apenas um consiga consumir a última cota.
- **Item do carrinho removido ou desativado no catálogo**: Se um item presente no carrinho for inativado no catálogo pelo Módulo 1, o recálculo do carrinho deve marcar o item como indisponível e orientar sua remoção.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE permitir a criação e manutenção de carrinhos de compras persistentes para clientes autenticados e sessões anônimas temporárias.
- **FR-002**: O sistema DEVE validar a existência, preço e status ativo de qualquer SKU adicionado ao carrinho através da [`PortaConsultaCatalogoCliente`](file:///c:/Users/Edu/ecommerce-promocoes-avancadas/src/main/java/br/edu/fatec/ecommerce/modulo1/integracao/PortaConsultaCatalogoCliente.java).
- **FR-003**: O sistema DEVE recalcular automaticamente o subtotal bruto, total de itens e peso acumulado a cada operação de adição, atualização de quantidade ou remoção de itens.
- **FR-004**: O sistema DEVE disponibilizar no mínimo quatro modalidades de promoção distintas: (1) Leve X Pague Y, (2) Desconto Percentual Condicional, (3) Desconto em Valor Fixo por Meta de Valor, e (4) Desconto em Combo Cruzado / Venda Casada.
- **FR-005**: O sistema DEVE permitir a instanciação dinâmica de novas regras e estratégias promocionais através do padrão `Dynamic Factory` a partir de tipos registrados sem condicionais fixas no código central.
- **FR-006**: O sistema DEVE implementar o padrão `Composite` para modelar árvores recursivas de condições de elegibilidade promocional com operadores lógicos `AND`, `OR` e `NOT`.
- **FR-007**: As condições atômicas de elegibilidade DEVEM contemplar: valor mínimo do carrinho, primeira compra do cliente, categoria/subcategoria do produto (recursiva via Módulo 1), SKU específico e UF/região de entrega.
- **FR-008**: O sistema DEVE implementar a interface `Comparable<Promocao>` para ordenar deterministicamente as campanhas ativas por nível de prioridade e, em caso de empate, pelo maior benefício financeiro.
- **FR-009**: O sistema DEVE implementar o padrão `Chain of Responsibility` para processar a validação e resolução de conflitos entre promoções acumulativas e exclusivas.
- **FR-010**: O sistema DEVE permitir a criação, consulta, aplicação e controle de cotas de Cupons Promocionais com validação de código, período de vigência e compatibilidade.
- **FR-011**: O sistema DEVE implementar o padrão `Decorator` para estruturar a formação do preço final em camadas aditivas/subtrativas independentes e auditáveis sobre o valor base.
- **FR-012**: O sistema DEVE implementar o padrão `Visitor` com duplo despacho (`Double Dispatch`) para percorrer polimorficamente itens individuais e combos no cálculo de descontos sem acoplamento a tipos concretos.
- **FR-013**: O sistema DEVE implementar o padrão `Null Object` através das classes `PromocaoNula` e `CupomNulo` para representar a ausência segura de descontos sem cheques de `null`.
- **FR-014**: Todos os cálculos monetários DEVEM utilizar `BigDecimal` com arredondamento padronizado (`RoundingMode.HALF_EVEN`), garantindo piso zero para evitar totais negativos.
- **FR-015**: O sistema DEVE expor API REST documentada via OpenAPI/Swagger para operações de carrinho e para administração de campanhas/cupons pelo perfil `RESPONSAVEL_PROMOCOES` e `ADMIN`.
- **FR-016**: O sistema DEVE expor a interface de integração Java `PortaConsultaCarrinhoPromocoes` para que o Módulo 3 consulte o resumo financeiro consolidado do carrinho no checkout.

---

### Key Entities

- **CarrinhoCompras**: Agregado raiz que reúne os itens selecionados pelo cliente, o cupom aplicado, a memória de cálculo de formação de preço e o instante da última mutação.
- **ItemCarrinho**: Linha de item do carrinho contendo o SKU do produto ou combo, quantidade desejada, preço unitário vigente e peso unitário obtidos do catálogo.
- **CampanhaPromocional**: Configuração de uma regra de promoção com identificador, nome, descrição, tipo de promoção, parâmetros de cálculo, vigência, prioridade, indicador de exclusividade/acumulação e raiz da árvore de elegibilidade (`CondicaoElegibilidadeComponent`).
- **CondicaoElegibilidadeComponent**: Interface do padrão Composite que declara o método polimórfico de avaliação de elegibilidade frente ao contexto do carrinho e cliente.
- **CupomPromocional**: Entidade que representa um cupom de desconto com código alfanumérico único, modalidade de desconto, limite de utilizações, quantidade utilizada e vigência.
- **MemoriaCalculoPreco**: Objeto de valor gerado pelo Decorator que discrimina o subtotal bruto dos itens, a lista de abatimentos concedidos com o nome da promoção/cupom de origem e o total líquido com piso zero.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Todas as quatro modalidades distintas de promoção executam com 100% de precisão matemática comprovada por testes unitários e de integração JUnit 5.
- **SC-002**: A resolução de conflitos entre múltiplas promoções simultâneas produz resultado determinístico invariante em 100% dos cenários de teste, aplicando sempre a regra de maior benefício financeiro em empates.
- **SC-003**: A avaliação completa do carrinho (recálculo de itens, verificação de elegibilidade e pipeline de preço) executa em menos de 50ms para carrinhos com até 50 itens e 20 campanhas ativas.
- **SC-004**: O total líquido final do carrinho nunca atinge valor inferior a R$ 0,00 (`BigDecimal.ZERO`) em nenhum cenário de teste, mesmo com descontos acumulados superiores ao subtotal bruto.
- **SC-005**: 100% dos métodos e classes públicas e privadas contêm comentários explicativos e Javadoc declarando o papel funcional e o papel no Design Pattern adotado.
- **SC-006**: Cobertura de testes automatizados com taxa de sucesso de 100% (`BUILD SUCCESS`) integrada ao pipeline de compilação do Maven.

---

## Assumptions

- O catálogo de produtos, variações, combos e controle de estoque continuam sob responsabilidade exclusiva do Módulo 1 e são consultados via `PortaConsultaCatalogoCliente`.
- O cálculo de frete, cobrança de pagamentos e transições de ciclo de vida do pedido serão implementados pelo Integrante 3 (Módulo 3) consumindo os dados consolidados do Módulo 2.
- A autenticação e os perfis de segurança (`CLIENTE`, `RESPONSAVEL_PROMOCOES`, `ADMIN`) reutilizam a configuração do Spring Security já implementada no Módulo 1.
