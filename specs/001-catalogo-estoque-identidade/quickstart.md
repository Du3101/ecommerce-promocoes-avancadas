# Quickstart & Validation Guide: Módulo 1 — Identidade, Catálogo, Composição de Produtos, Estoque e Auditoria

**Feature**: `001-catalogo-estoque-identidade`  
**Date**: 2026-10-26  
**References**: [spec.md](./spec.md) | [data-model.md](./data-model.md) | [contracts/modulo1-api-and-integration-ports.md](./contracts/modulo1-api-and-integration-ports.md)

---

## 1. Pré-requisitos de Ambiente

- **Java JDK 21** configurado no `JAVA_HOME` / `PATH`.
- **Maven Wrapper (`./mvnw` / `.\mvnw.cmd`)** incluído na raiz do projeto (não exige instalação manual do Maven).
- **Banco de Dados**:
  - Para execução dos testes automatizados e perfil de desenvolvimento rápido (`dev`/`test`): **H2 em memória (modo PostgreSQL)** embarcado — zero configuração externa necessária.
  - Para execução com banco persistente (`prod`/`local-pg`): **PostgreSQL 15+** (opcionalmente via `docker compose up -d postgres`).

---

## 2. Comandos de Execução e Testes Automatizados

### Executar toda a suíte de testes JUnit 5 do Módulo 1
```powershell
.\mvnw.cmd clean test
```
**Resultado esperado**: Todos os testes unitários e de integração passam (`BUILD SUCCESS`), validando:
- Hash BCrypt e segregação de perfis (`CLIENTE` vs. `ADMIN` / operadores);
- `Null Object` para endereço ausente e categoria raiz;
- Árvore recursiva de categorias (`Composite`) e detecção de ciclos;
- Compartilhamento de instâncias `Flyweight` de atributos de variação;
- Cálculo recursivo de preço, peso e disponibilidade de `ComboKitComposite` (`Composite`);
- Disputa concorrente (`ExecutorService` / `CountDownLatch`) pela última unidade em estoque com exatamente 1 reserva aprovada;
- Idempotência na confirmação/liberação de reserva e expiração por TTL;
- Gravação automática de `RegistroAuditoria` via `Proxy`.

### Iniciar a aplicação Spring Boot localmente (com Banco em Memória + Dados de Demonstração)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
.\mvnw.cmd spring-boot:run
```
- **API Base URL**: `http://localhost:8080/api/v1`
- **Documentação Interativa OpenAPI / Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Botão `Authorize 🔒` (HTTP Basic Auth no topo do Swagger UI)**:
  - **Usuário**: `admin@ecommerce.fatec.edu.br`
  - **Senha**: `AdminFatec@2026`
- **Dados de Demonstração Pré-Carregados Automaticamente no Startup**:
  - **Árvore de Categorias (`Composite`)**: `Eletrônicos > Periféricos > Teclados e Mouses`
  - **Produtos com Variações (`Flyweight` + `Builder`)**:
    - `DEMO-TEC-PRO-PRETO` (*Teclado Mecânico Gamer FATEC Pro* — `R$ 300,00`, `850g`, estoque: `10`)
    - `DEMO-MOU-PREC-PRETO` (*Mouse Gamer Óptico FATEC Precision* — `R$ 150,00`, `120g`, estoque: `5`)
  - **Combo/Kit Promocional (`ComboKitComposite`)**:
    - `DEMO-KIT-GAMER-01` (*Kit Gamer FATEC Completo: 1 Teclado + 2 Mouses* — `10%` de desconto sobre a soma recursiva = `R$ 540,00`, `1090g`, disponibilidade máxima limitada pelo gargalo = `2 kits`)

---

## 3. Cenários de Validação Ponta a Ponta (Roteiro de Apresentação)

### Cenário 1: Cadastro de Cliente, `Null Object` de Endereço e Seleção de Entrega (`US1`)
1. Enviar `POST /api/v1/auth/clientes/cadastro` conforme payload em [modulo1-api-and-integration-ports.md](./contracts/modulo1-api-and-integration-ports.md#1-endpoints-rest--identidade-clientes-e-endereços-apiv1auth-e-apiv1clientes).
2. Consultar `GET /api/v1/clientes/me` autenticado como o novo cliente:
   - Verificar que `enderecoSelecionado.possuiEnderecoValido` é `false` (`EnderecoAusenteNullObject` em ação, sem `null`).
3. Cadastrar dois endereços (`POST /api/v1/clientes/me/enderecos`) e selecionar um (`PUT /api/v1/clientes/me/enderecos/{id}/selecionar`).
4. Consultar `GET /api/v1/clientes/me` e confirmar que o endereço selecionado é retornado com CEP e UF.

### Cenário 2: Hierarquia Recursiva de Categorias (`Composite`) e Variações (`Flyweight` + `Builder`) (`US2`)
1. Autenticar como `ADMIN` (ou `OPERADOR_COMERCIAL`) e criar a árvore de categorias `Eletrônicos` → `Periféricos` → `Teclados` via `POST /api/v1/admin/catalogo/categorias`.
2. Cadastrar produto com variações (`Cor: Preto`, `Cor: Branco`, `Switch: Mecânico`) via `POST /api/v1/admin/catalogo/produtos`.
3. Consultar `GET /api/v1/catalogo/itens?categoriaId={idEletronicos}` e verificar que os produtos da subcategoria neta `Teclados` são listados recursivamente.

### Cenário 3: Criação de `ComboKit` (`Composite`) e Gargalo de Estoque (`US3`)
1. Ajustar estoque (`POST /api/v1/admin/estoque/{sku}/ajustes`) do item A para `10` unidades e do item B para `5` unidades.
2. Criar um `ComboKit` exigindo `1x Item A + 2x Item B` com `modoPreco = SOMA_RECURSIVA_COMPONENTES` e `10%` de desconto (`POST /api/v1/admin/catalogo/combos`).
3. Consultar `GET /api/v1/catalogo/itens/{skuCombo}`:
   - Verificar que a disponibilidade máxima informada para o Combo é exatamente `2` kits ($\lfloor 5 / 2 \rfloor$), o peso é a soma ponderada dos componentes e o preço aplica os 10% sobre a soma recursiva.

### Cenário 4: Concorrência na Última Unidade, Liberação Idempotente e Auditoria (`Proxy`) (`US4` e `US5`)
1. Definir saldo de um SKU para `1` unidade e disparar duas reservas para o mesmo SKU:
   - A 1ª retorna `201 Created` (`estado = ATIVA`);
   - A 2ª retorna `409 Conflict` (estoque insuficiente).
2. Chamar `POST /api/v1/estoque/reservas/{chaveOperacao}/liberar` duas vezes seguidas:
   - Ambas retornam `200 OK` (idempotência), e o saldo disponível volta exatamente para `1` unidade (sem duplicar devolução).
3. Consultar `GET /api/v1/admin/auditoria` como `ADMIN` e comprovar que todas as alterações de preço, categoria e estoque realizadas nos passos anteriores foram registradas automaticamente pelo `Proxy` com autor, data/hora e valores anterior/novo.

