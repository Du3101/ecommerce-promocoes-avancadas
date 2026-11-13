# Passagem de sessão — Módulo 3

**Última atualização**: 2026-11-12 · **Branch**: `003-checkout-pedidos-pagamentos`
**Estado**: implementação **concluída** (T001–T042). `.\mvnw.cmd clean test` → **86 testes, 0 falhas** (Módulos 1, 2 e 3).
A aplicação sobe com `.\mvnw.cmd spring-boot:run` e o runner de demonstração cria 4 pedidos em estados diferentes.

## Onde está cada coisa

| O quê | Onde |
|---|---|
| Arquitetura, padrões, diagramas UML (estados, classes, sequência), endpoints | [docs/modulo3-arquitetura-e-padroes.md](../../docs/modulo3-arquitetura-e-padroes.md) |
| Tarefas e desvios em relação ao plano | [tasks.md](./tasks.md) (seção final "Registro de Execução") |
| Guia de estudo/apresentação do Henrique (**fora do repositório**) | `C:\Users\Henriqueee sigma\Documents\ProgramingProjects\modulo3-apresentacao-henrique.md` |

## Decisões que valem para quem continuar

- O Módulo 3 só usa os Módulos 1 e 2 por portas Java. As portas foram **estendidas** com aprovação do usuário (ver tasks.md).
- Carrinho de cliente logado = `Cliente.id` real (regra única em `AdaptadorIntegracaoModulo2.resolverIdentificadorCarrinhoPorEmail`).
- A V3 foi editada diretamente porque a branch ainda não foi para a `main`. **Depois do merge, qualquer mudança de schema vai numa V4.**
- Javadoc em todos os métodos do Módulo 3 (verificado por script). Métodos antigos do Módulo 2 sem Javadoc (ex.: `InicializadorDemonstracaoModulo2Runner`) são do Integrante 2.

## Possíveis próximos passos (não pedidos)

- Abrir o PR da branch para a `main` (avisar os Integrantes 1 e 2 sobre as mudanças nas portas e o bug corrigido no carrinho).
- Fluxo de devolução pós-entrega (hoje, pedido ENVIADO só é bloqueado, com orientação).
- Assinatura HMAC no webhook no lugar do token compartilhado.
