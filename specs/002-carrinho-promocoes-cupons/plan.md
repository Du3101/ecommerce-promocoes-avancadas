# Implementation Plan: Módulo 2 — Carrinho, Motor de Promoções Avançadas, Elegibilidade, Cupons e Formação de Preço

**Branch**: `002-carrinho-promocoes-cupons` | **Date**: 2026-11-03 | **Spec**: [spec.md](./spec.md)  
**Integrante 2**: Eduardo Juncker Toras  

---

## Summary

Este plano técnico estrutura a implementação do **Módulo 2 (Integrante 2 — Eduardo Juncker Toras)** do Sistema de E-commerce com Promoções Avançadas: **Carrinho de Compras**, **Motor de Promoções Avançadas com no mínimo 4 naturezas**, **Árvore Recursiva de Condições de Elegibilidade**, **Validação de Cupons e Resolução Determinística de Conflitos**, **Pipeline de Formação de Preço com Piso Zero**, **Objetos Nulos** e **Portas de Integração Desacopladas com os Módulos 1 e 3**.

A arquitetura aplica de maneira autêntica e justificada os padrões exigidos na Constituição: `Composite`, `Chain of Responsibility`, `Decorator`, `Visitor` (com `Double Dispatch`), `Comparable`, `Dynamic Factory`, `Null Object` e `Operações Matemáticas com BigDecimal`.

---

## Technical Context

**Language/Version**: Java 21 (LTS)  
**Primary Dependencies**: Spring Boot 3.4.x (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`), Springdoc OpenAPI 2, Flyway.  
**Storage**: PostgreSQL / H2 em modo de compatibilidade PostgreSQL (para testes automatizados rápidos).  
**Testing**: JUnit 5, MockMvc, testes unitários para cada padrão e testes de integração de API REST.  
**Constraints**: 100% dos métodos documentados com comentários e Javadoc; zero `if/else` extensivo onde cabe polimorfismo; arredondamento bancário `HALF_EVEN` e piso zero; integração com Módulo 1 exclusivamente via `PortaConsultaCatalogoCliente`.

---

## Constitution Check

| Princípio da Constituição | Status | Evidência no Plano / Design |
| :--- | :--- | :--- |
| **I. OCP e Design Patterns** | ✅ PASS | Adição de novas mecânicas de promoção via `EstrategiaCalculoPromocao` (`Dynamic Factory`) e novas condições via `CondicaoElegibilidadeComponent` (`Composite`) sem alterar o motor. |
| **II. Catálogo de Padrões Integrante 2** | ✅ PASS | Todos os 8 padrões mapeados: `Composite`, `Chain of Responsibility`, `Decorator`, `Visitor`/`Double Dispatch`, `Comparable`, `Dynamic Factory`, `Null Object`, `Operações Matemáticas`. |
| **III. Código Explícito e Comentários** | ✅ PASS | Regra inegociável: comentários funcionais e papel no Design Pattern em cada método e classe do pacote `modulo2`. |
| **IV. Transparência na Formação de Preço** | ✅ PASS | Memória de cálculo detalhada gerada pelo `Decorator` com piso zero garantido. |
| **V. Camadas e Segurança** | ✅ PASS | Pacote `br.edu.fatec.ecommerce.modulo2` segregado em `dominio`, `aplicacao`, `infraestrutura`, `api` e `integracao`. RBAC com `RESPONSAVEL_PROMOCOES`. |
| **VI. Testes Automatizados** | ✅ PASS | Suíte JUnit 5 cobrindo as 4 naturezas de promoção, conflitos de exclusividade, cupons expirados e piso zero. |
| **Divisão de Trabalho em 3 Integrantes** | ✅ PASS | Módulo 2 completamente autocontido, consumindo `PortaConsultaCatalogoCliente` (Módulo 1) e expondo `PortaConsultaCarrinhoPromocoes` (Módulo 3). |

---

## Project Structure

```text
src/main/java/br/edu/fatec/ecommerce/modulo2/
├── carrinho/
│   ├── dominio/
│   │   ├── CarrinhoCompras.java
│   │   ├── ItemCarrinho.java
│   │   ├── ItemCarrinhoProduto.java
│   │   ├── ItemCarrinhoComboKit.java
│   │   ├── ElementoCarrinhoComponent.java
│   │   └── visitor/
│   │       ├── ElementoCarrinhoVisitor.java
│   │       └── CalculadoraDescontoVisitor.java
│   ├── aplicacao/
│   │   └── ServicoCarrinhoCompras.java
│   ├── infraestrutura/
│   │   └── CarrinhoComprasJpaRepository.java
│   └── api/
│       └── CarrinhoComprasController.java
├── promocoes/
│   ├── dominio/
│   │   ├── CampanhaPromocional.java
│   │   ├── TipoMecanicaPromocao.java
│   │   ├── ParametrosMecanicaPromocao.java
│   │   ├── fabrica/
│   │   │   ├── EstrategiaCalculoPromocao.java
│   │   │   └── FabricaDinamicaEstrategiaPromocao.java
│   │   ├── estrategias/
│   │   │   ├── PromocaoLeveXPagueY.java
│   │   │   ├── PromocaoPercentualCondicional.java
│   │   │   ├── PromocaoValorFixoMetaCarrinho.java
│   │   │   ├── PromocaoComboCruzadoCasado.java
│   │   │   └── PromocaoNula.java
│   │   ├── composite/
│   │   │   ├── CondicaoElegibilidadeComponent.java
│   │   │   ├── ContextoElegibilidade.java
│   │   │   ├── CondicaoValorMinimoCarrinho.java
│   │   │   ├── CondicaoPrimeiraCompraCliente.java
│   │   │   ├── CondicaoCategoriaProduto.java
│   │   │   ├── CondicaoItemEspecificoSku.java
│   │   │   ├── CondicaoRegiaoEntrega.java
│   │   │   ├── CondicaoAndComposite.java
│   │   │   ├── CondicaoOrComposite.java
│   │   │   └── CondicaoNotComposite.java
│   │   └── cadeia/
│   │       ├── ValidadorPromocaoHandler.java
│   │       ├── ValidadorCarrinhoNaoVazio.java
│   │       ├── ValidadorElegibilidadeCampanha.java
│   │       └── ValidadorConflitoExclusividade.java
│   ├── aplicacao/
│   │   ├── MotorPromocoesAvancadas.java
│   │   └── ServicoAdminPromocoes.java
│   ├── infraestrutura/
│   │   └── CampanhaPromocionalJpaRepository.java
│   └── api/
│       └── PromocoesAdminController.java
├── cupons/
│   ├── dominio/
│   │   ├── CupomPromocional.java
│   │   ├── TipoDescontoCupom.java
│   │   └── CupomNulo.java
│   ├── aplicacao/
│   │   └── ServicoCuponsPromocionais.java
│   ├── infraestrutura/
│   │   └── CupomPromocionalJpaRepository.java
│   └── api/
│       └── CuponsAdminController.java
├── precificacao/
│   ├── dominio/
│   │   ├── PrecoCalculadoComponent.java
│   │   ├── PrecoBaseCarrinho.java
│   │   ├── DescontoDecoratorAbstrato.java
│   │   ├── DescontoPromocaoDecorator.java
│   │   ├── DescontoCupomDecorator.java
│   │   ├── MemoriaCalculoPrecoVO.java
│   │   ├── ParcelaDescontoVO.java
│   │   └── OperacoesMonetarias.java
│   └── aplicacao/
│       └── PipelineFormacaoPrecoService.java
└── integracao/
    ├── PortaConsultaCarrinhoPromocoes.java
    ├── ResumoCarrinhoCheckoutDTO.java
    └── AdaptadorIntegracaoModulo2.java
```
