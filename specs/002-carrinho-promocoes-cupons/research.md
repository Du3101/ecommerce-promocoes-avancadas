# Research & Design Patterns Defense: Módulo 2 — Carrinho, Motor de Promoções Avançadas, Elegibilidade, Cupons e Formação de Preço

**Feature**: `002-carrinho-promocoes-cupons`  
**Date**: 2026-11-03  
**Integrante 2**: Eduardo Juncker Toras  
**Status**: Completed  

---

## 1. Contexto Acadêmico e Decisões de Projeto

O **Módulo 2** é responsável pelo motor promocional, carrinho de compras e formação de preço do e-commerce. A Constituição do Projeto estabelece o **Princípio Aberto-Fechado (OCP)** como mandatário: o motor de promoções deve ser capaz de receber uma nova modalidade de promoção (ex.: "Desconto Progressivo por Quantidade" ou "Brinde Físico Promocional") ou uma nova regra de elegibilidade sem alterar as classes existentes e sem adicionar cadeias condicionais (`if/else` ou `switch`) nos serviços centrais.

Para cada um dos padrões exigidos para o Integrante 2, documentamos as respostas às três perguntas obrigatórias da banca avaliadora:
1. **Qual problema de projeto existia?**
2. **Por que este padrão foi escolhido?**
3. **O que ficou mais simples de modificar ou estender?**

---

## 2. Catálogo dos Padrões de Projeto do Módulo 2

### 2.1. `Composite` (Composição Recursiva) — Árvore de Condições de Elegibilidade

- **Problema de Projeto**: Campanhas promocionais exigem regras de elegibilidade altamente flexíveis e aninhadas (ex.: "Cliente em primeira compra E carrinho acima de R$ 200", ou "(Categoria Periféricos OU Categoria Games) E Região Sudeste"). Se essas regras fossem implementadas com flags booleanas na tabela de promoções ou códigos procedurais, o sistema ficaria limitado a poucas combinações fixas e exigiria alterações no banco e no código a cada nova campanha.
- **Por que foi escolhido**: O padrão `Composite` permite tratar condições atômicas (folhas) e conjuntos lógicos de condições (compostos `AND`, `OR`, `NOT`) sob uma mesma interface unificada `CondicaoElegibilidadeComponent`. O método `avaliar(ContextoElegibilidade contexto)` é invocado recursivamente por toda a árvore até chegar às folhas.
- **O que ficou mais simples de estender**: Novas condições atômicas (ex.: `CondicaoAniversarianteDoMes`, `CondicaoDiaDaSemana`) ou novas operações lógicas (ex.: `CondicaoXorComposite`) são criadas simplesmente implementando `CondicaoElegibilidadeComponent` sem alterar nenhuma classe do motor de promoções ou da persistência.

### 2.2. `Chain of Responsibility` (Cadeia de Responsabilidade) — Pipeline de Validação de Carrinho, Elegibilidade e Cupons

- **Problema de Projeto**: Antes de aplicar um desconto ou fechar um cupom, é necessário executar uma série de validações sequenciais: (1) verificar se o carrinho não está vazio, (2) verificar se o cupom existe, está ativo e não expirou, (3) verificar se o limite de utilizações globais e por cliente foi atingido, (4) avaliar a árvore de elegibilidade da campanha e (5) verificar regras de exclusividade mútua entre promoções. Misturar todas essas verificações em um único método de serviço geraria um método gigante e frágil.
- **Por que foi escolhido**: O padrão `Chain of Responsibility` organiza as validações em elos independentes e desacoplados (`ValidadorCarrinhoNaoVazio`, `ValidadorVigenciaCupom`, `ValidadorLimiteUsoCupom`, `ValidadorElegibilidadeCampanha`, `ValidadorConflitoExclusividade`). Cada elo decide se interrompe a requisição com erro de negócio ou repassa a requisição para o próximo elo.
- **O que ficou mais simples de estender**: É possível incluir uma nova etapa de validação (ex.: `ValidadorFraudeScore`, `ValidadorEstoqueMinimoCampanha`) ou reordenar a cadeia sem alterar os outros validadores nem o serviço consumidor.

### 2.3. `Decorator` — Encadeamento Transparente da Formação de Preço

- **Problema de Projeto**: O cálculo do valor a pagar pelo cliente deve começar no subtotal dos produtos e passar por sucessivas camadas de modificação: promoções ativas, descontos de cupons e, futuramente, frete (Módulo 3). Se uma única classe fizesse todas as contas, não haveria memória de cálculo transparente e seria difícil isolar o impacto de cada benefício financeiro.
- **Por que foi escolhido**: O padrão `Decorator` envolve o componente base `PrecoCalculadoComponent` (`PrecoBaseCarrinho`) com decoradores concretos (`DescontoPromocaoDecorator`, `DescontoCupomDecorator`). Cada camada calcula o seu desconto sobre o valor resultante da camada anterior, preserva o subtotal original, gera o discriminativo da parcela de desconto e delega o total ao próximo elo.
- **O que ficou mais simples de estender**: Adicionar uma nova camada de cálculo (ex.: `CashbackDecorator`, `TaxaEmbalagemPresenteDecorator`) exige apenas um novo Decorator, sem tocar na lógica de cálculo das promoções ou do carrinho base.

### 2.4. `Visitor` e `Double Dispatch` — Avaliação Polimórfica de Desconto sobre Itens do Carrinho

- **Problema de Projeto**: O carrinho pode conter diferentes tipos de itens vendáveis: produtos simples com variação (`ItemCarrinhoProduto`) e combos/kits compostos (`ItemCarrinhoComboKit`). Certas promoções concedem desconto apenas sobre itens unitários (ex.: Leve 3 Pague 2), enquanto outras concedem desconto sobre o kit completo ou sobre itens específicos dentro do kit. Fazer `if (item instanceof ItemCarrinhoProduto)` no motor de promoções violaria o OCP e espalharia `instanceof` por todo o sistema.
- **Por que foi escolhido**: O padrão `Visitor` com duplo despacho (`Double Dispatch`) desacopla as operações de cálculo da estrutura do carrinho. Cada elemento implementa `void aceitar(ElementoCarrinhoVisitor visitor)` chamando de volta `visitor.visitarProduto(this)` ou `visitor.visitarComboKit(this)`. O compilador Java despacha dinamicamente para o método correto em tempo de execução sem nenhum casting manual.
- **O que ficou mais simples de estender**: Novas operações de inspeção ou precificação sobre os itens do carrinho (ex.: gerador de relatório fiscal, calculadora de pontos de fidelidade) são criadas adicionando novos Visitors, sem alterar uma linha das classes de itens do carrinho.

### 2.5. `Comparable` — Ordenação Determinística e Resolução de Conflitos entre Promoções

- **Problema de Projeto**: Quando um cliente se qualifica para múltiplas promoções simultâneas que não são cumulativas (exclusivas entre si), o sistema deve decidir de forma 100% determinística e justa qual promoção aplicar. Regras aleatórias ou dependentes da ordem em que o banco retornou os registros gerariam inconsistências e reclamações dos clientes.
- **Por que foi escolhido**: As promoções implementam a interface nativa Java `Comparable<Promocao>`. O método `compareTo(Promocao outra)` compara primeiro a prioridade numérica configurada pelo lojista (maior prioridade primeiro); em caso de empate, compara o valor financeiro do desconto simulado para aquele carrinho, favorecendo sempre o maior desconto ao cliente.
- **O que ficou mais simples de estender**: A lógica de desempate fica centralizada na própria entidade ou em um `Comparator<Promocao>` especializado, garantindo que listas de promoções sejam ordenadas com `Collections.sort()` ou `TreeSet` de forma idêntica em qualquer camada do sistema.

### 2.6. `Dynamic Factory` e `Dynamic Injection` — Criação Dinâmica de Estratégias Promocionais

- **Problema de Projeto**: As regras de cálculo de desconto variam radicalmente: percentual, fixo, Leve X Pague Y, desconto progressivo, combo cruzado. Se a aplicação utilizasse um `switch(tipoPromocao)` com 10 `case`s para instanciar a estratégia correspondente, violaria o OCP a cada nova promoção.
- **Por que foi escolhido**: A `FabricaDinamicaEstrategiaPromocao` mantém um registro dinâmico (`Map<TipoMecanicaPromocao, ProvedorEstrategiaPromocao>`) alimentado automaticamente pelo mecanismo de injeção de dependências do Spring (`List<EstrategiaCalculoPromocao>`). Cada estratégia declara qual tipo atende, e a fábrica a localiza em $O(1)$ sem condicionais.
- **O que ficou mais simples de estender**: Para criar uma 5ª ou 6ª mecânica de promoção (ex.: `DescontoProgressivoPorQtd`), basta criar uma nova classe anotada com `@Component` implementando `EstrategiaCalculoPromocao`. Ela é descoberta e injetada automaticamente sem mexer na fábrica nem nos controllers.

### 2.7. `Null Object` — Representação Segura de Ausência de Benefício (`PromocaoNula` e `CupomNulo`)

- **Problema de Projeto**: Quando um carrinho não possui cupom aplicado ou nenhuma promoção foi elegível, o uso de `null` causaria verificações constantes de `if (carrinho.getCupom() != null)` e riscos de `NullPointerException` durante o cálculo de totais e renderização de respostas.
- **Por que foi escolhido**: O padrão `Null Object` introduz `PromocaoNula` (implementa `EstrategiaCalculoPromocao` retornando `BigDecimal.ZERO`) e `CupomNulo` (implementa `Cupom` com código vazio e `isValido() == false`).
- **O que ficou mais simples de estender**: Os decoradores e formatadores de preço invocam `.calcularDesconto()` e `.obterCodigo()` sem qualquer verificação condicional de nulo.

### 2.8. `Operações Matemáticas` com `BigDecimal` e Piso Zero

- **Problema de Projeto**: Operações financeiras com números de ponto flutuante (`double` ou `float`) geram erros de arredondamento inaceitáveis no comércio eletrônico (ex.: `0.1 + 0.2 = 0.30000000000000004`). Além disso, promoções e cupons agressivos poderiam subtrair um valor superior ao subtotal, deixando o total a pagar negativo.
- **Por que foi escolhido**: Criamos um Value Object utilitário `OperacoesMonetarias` encapsulando operações aritméticas com `BigDecimal`, escala de 2 casas decimais e política de arredondamento bancário padronizada (`RoundingMode.HALF_EVEN`). Todas as subtrações de desconto aplicam a função de piso zero `subtotal.subtract(desconto).max(BigDecimal.ZERO)`.
- **O que ficou mais simples de estender**: Qualquer nova estratégia de desconto utiliza os métodos auxiliares `calcularPercentual(valor, taxa)`, `aplicarDescontoComPisoZero(base, desconto)` e `ratearValorProporcionalmente(...)`, garantindo que os centavos nunca sejam perdidos nem criados artificialmente.

