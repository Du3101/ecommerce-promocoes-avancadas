# Quickstart & Validation Guide: Módulo 2 — Carrinho, Motor de Promoções Avançadas, Elegibilidade, Cupons e Formação de Preço

**Feature**: `002-carrinho-promocoes-cupons`  
**Date**: 2026-11-03  
**Integrante 2**: Eduardo Juncker Toras  
**References**: [spec.md](./spec.md) | [data-model.md](./data-model.md) | [research.md](./research.md)  

---

## 1. Comandos de Compilação e Testes Automatizados

### Executar todos os testes do projeto (Módulo 1 + Módulo 2)
```powershell
.\mvnw.cmd clean test
```

### Executar exclusivamente os testes de unidade e integração do Módulo 2
```powershell
.\mvnw.cmd test -Dtest="br.edu.fatec.ecommerce.modulo2.**.*Test"
```

---

## 2. Roteiro de Demonstração dos Padrões (Apresentação / Defesa Acadêmica)

### Cenário 1: Composição Recursiva de Elegibilidade (`Composite`)
- Demonstrar uma campanha promocional cuja regra seja `CondicaoAndComposite` combinando valor mínimo com primeira compra do cliente.
- Testar com cliente em 1ª compra (promoção ativada) e cliente recorrente (promoção ignorada).

### Cenário 2: Motor de Promoções com 4 Naturezas Distintas (`Dynamic Factory`)
- Validar `Leve 3 Pague 2`, `Desconto Percentual`, `Desconto Fixo por Meta` e `Combo Cruzado Teclado + Mouse`.
- Explicar como a `FabricaDinamicaEstrategiaPromocao` instancia as estratégias sem `switch/case`.

### Cenário 3: Resolução Determinística de Conflitos (`Comparable` + `Chain of Responsibility`)
- Cadastrar duas promoções exclusivas simultaneamente elegíveis com prioridades distintas.
- Mostrar que o sistema seleciona a de maior prioridade (ou maior desconto financeiro no desempate).

### Cenário 4: Pipeline de Formação de Preço (`Decorator` + `Visitor` + Piso Zero)
- Exibir a resposta REST com o discriminativo completo da memória de cálculo.
- Forçar desconto cumulativo superior ao subtotal e demonstrar que o piso zero crava em R$ 0,00 sem saldos negativos.

