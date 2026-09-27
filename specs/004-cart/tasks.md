---
description: "Tarefas de implementação da feature Carrinho de Compras"
---

# Tasks: Carrinho de Compras

**Input**: Documentos de design em `specs/004-cart/`.

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contrato OpenAPI](contracts/cart-service-api.openapi.yaml).

**Testes**: Incluídos conforme a Constituição TechStore: regras de negócio e casos de uso requerem testes; persistência, APIs e integração entre serviços recebem testes de integração apropriados.

**Organização**: As tarefas são agrupadas por história para permitir implementação incremental. Cada linha de tarefa segue `- [ ] Tnnn [P?] [USn?] descrição com caminho`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Criar o módulo e conectar a infraestrutura local do novo serviço.

- [X] T001 [P] Registrar `cart-service` como módulo Maven em `backend/pom.xml`.
- [X] T002 [P] Criar `backend/cart-service/pom.xml` com as dependências alinhadas aos serviços existentes: Web, Data JPA, Resource Server, Validation, Actuator, Flyway, PostgreSQL, springdoc e dependências de teste.
- [X] T003 [P] Criar `backend/cart-service/src/main/resources/application.yml` com porta, datasource externalizado, Flyway, validação de propriedades JSON desconhecidas, JWT, Product Service, timeouts finitos externalizados, Actuator e springdoc.
- [X] T004 [P] Criar `backend/cart-service/Dockerfile` para build Maven do módulo e runtime Java 21, expondo a porta configurada do serviço.
- [X] T005 [P] Criar `docker/postgres/init-cart-database.sh` para validar identificadores e criar idempotentemente o database lógico `techstore_cart_db` usando o padrão de inicialização PostgreSQL existente.
- [X] T006 [P] Documentar `CART_POSTGRES_DB` e `CART_SERVICE_PORT` em `.env.example`, sem adicionar valores secretos reais.
- [X] T007 [P] Adicionar inicializador do database e serviço `cart-service` ao `docker-compose.yml`, usando database/URL próprios e dependência do Product Service.
- [X] T008 [P] Adicionar a rota `CART_SERVICE_URL` para `/api/cart/**` em `backend/api-gateway/src/main/resources/application.yml`, preservando o prefixo `/api`.
- [X] T009 Criar a classe de inicialização Spring Boot `backend/cart-service/src/main/java/com/techstore/cart/CartServiceApplication.java`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implementar modelo, portas e adapters comuns que bloqueiam todas as histórias.

**Checkpoint**: Concluir esta fase antes das operações públicas por história.

- [X] T010 [P] Escrever testes de invariantes de quantidade em `backend/cart-service/src/test/java/com/techstore/cart/domain/CartItemTest.java`, cobrindo quantidade positiva e rejeição de zero/negativa.
- [X] T011 Implementar `backend/cart-service/src/main/java/com/techstore/cart/domain/CartItem.java` como modelo de domínio sem dependências Spring, preservando `quantity` inteiro maior que zero.
- [X] T012 Criar `CartStorePort` e `ProductCatalogPort` em `backend/cart-service/src/main/java/com/techstore/cart/application/port/out/`, cobrindo leitura por proprietário, upsert aditivo atômico que informa se a linha foi criada, substituição, remoção, limpeza e consulta REST de produto ativo.
- [X] T013 Criar exceções de aplicação para item ausente, identidade inválida e indisponibilidade do catálogo em `backend/cart-service/src/main/java/com/techstore/cart/application/exception/`.
- [X] T014 [P] Escrever testes HTTP do adapter de catálogo em `backend/cart-service/src/test/java/com/techstore/cart/adapter/product/ProductCatalogHttpAdapterTest.java`, cobrindo produto ativo inclusive com estoque zero, 404, 5xx e timeout.
- [X] T015 Implementar `ProductCatalogHttpAdapter` e configuração `RestClient` em `backend/cart-service/src/main/java/com/techstore/cart/adapter/product/`, chamando diretamente pela rede privada `PRODUCT_SERVICE_URL/api/products/{id}` (sem Gateway e com o prefixo interno `/api`), aplicando timeouts configuráveis, mapeando 404 para produto indisponível e falhas de rede/5xx para 503; não encaminhar o JWT do cliente.
- [X] T016 [P] Escrever testes PostgreSQL/Flyway em `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartPersistenceAdapterTest.java` para isolamento por proprietário, chave composta e constraint positiva de quantidade.
- [X] T017 Criar `backend/cart-service/src/main/resources/db/migration/V1__create_cart_items.sql` com `owner_user_id UUID` (“Não nulo; parte da chave composta; valor do sub validado.”), `product_id UUID` (“Não nulo; parte da chave composta; não possui FK entre serviços.”), chave `(owner_user_id, product_id)` e `quantity INTEGER` (“Não nulo e maior que zero (CHECK).”).
- [X] T018 Implementar entidade/chave JPA, repositório e adapter em `backend/cart-service/src/main/java/com/techstore/cart/adapter/persistence/`; usar upsert PostgreSQL atômico para somar quantidades sem duplicar linhas nem perder incrementos.
- [X] T019 [P] Escrever testes de autenticação em `backend/cart-service/src/test/java/com/techstore/cart/adapter/security/CartSecurityIntegrationTest.java`, cobrindo token ausente, inválido/expirado e `sub` que não seja UUID.
- [X] T020 Implementar Resource Server HS256 e resolução de proprietário em `backend/cart-service/src/main/java/com/techstore/cart/adapter/security/`, validando assinatura/expiração e derivando UUID somente de `sub`; não aceitar `userId` do cliente.
- [X] T021 [P] Escrever testes de mapeamento de erros em `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartExceptionHandlerTest.java` para validação, 404, 401 e indisponibilidade 503.
- [X] T022 Implementar handler e `ErrorResponse` comum em `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/`, sem registrar Authorization, JWT ou secrets.
- [X] T023 [P] Escrever testes de montagem de resposta em `backend/cart-service/src/test/java/com/techstore/cart/application/service/CartViewAssemblerTest.java`, cobrindo resumo atual, linha de produto inativo indisponível e falha do Product Service.
- [X] T024 Implementar `backend/cart-service/src/main/java/com/techstore/cart/application/service/CartViewAssembler.java` e `backend/cart-service/src/main/java/com/techstore/cart/application/model/CartView.java`, produzindo resultado de aplicação com `available=true` somente conforme o produto estar ativo (sem usar quantidade em estoque); o adapter HTTP mapeia o resultado para `CartApiModels` e produto ausente/inativo resulta em `available=false` com `product=null`.
- [X] T025 Criar DTOs de resposta (`CartResponse`, `CartItemResponse`, `ProductSummaryResponse`) em `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartApiModels.java`, conforme `contracts/cart-service-api.openapi.yaml`.

---

## Phase 3: User Story 1 - Consultar o próprio carrinho (Priority: P1)

**Goal**: Consultar exclusivamente as linhas do usuário autenticado e enriquecer a resposta com os dados atuais do catálogo.

**Independent Test**: Preparar linhas iniciais por fixture de persistência para um usuário e deixar outro sem linhas; com dois JWTs válidos, consultar os dois carrinhos e confirmar isolamento, lista vazia, resumo de produto ativo e produto inativo identificável como indisponível. Não usar POST de US2 para preparar o estado.

### Tests for User Story 1

- [X] T026 [P] [US1] Escrever `backend/cart-service/src/test/java/com/techstore/cart/application/service/GetCartServiceTest.java` para carrinho vazio, isolamento por proprietário, resumo atual, produto inativo e falha do catálogo sem escrita.
- [X] T027 [P] [US1] Escrever `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartQueryControllerTest.java` para `GET /api/cart`, resposta `200`, lista vazia, autenticação obrigatória e formato OpenAPI.

### Implementation for User Story 1

- [X] T028 [US1] Implementar `GetCartService` em `backend/cart-service/src/main/java/com/techstore/cart/application/service/GetCartService.java`, lendo somente pelo UUID do usuário e retornando `items: []` sem materializar carrinho vazio.
- [X] T029 [US1] Implementar `GET /api/cart` em `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartQueryController.java`, usando a identidade resolvida do JWT e mapeando o `CartView` da application para `CartApiModels.CartResponse`.

**Checkpoint**: A consulta é utilizável por usuário autenticado e não revela linhas de outro proprietário.

---

## Phase 4: User Story 2 - Adicionar e manter produtos (Priority: P1)

**Goal**: Adicionar, consolidar, atualizar e remover itens próprios, validando produto ativo e quantidade.

**Independent Test**: Com estado inicial preparado por fixture, adicionar produto ativo, repetir adição e confirmar nas respostas e no estado persistido uma única linha com a soma exata; substituir quantidade e confirmar o novo valor; remover e confirmar ausência da linha e resposta `204`. Produto inválido/inativo, quantidade inválida ou catálogo indisponível não deixa mutação parcial. A consulta após as mutações pertence à validação E2E com US1.

### Tests for User Story 2

- [X] T030 [P] [US2] Escrever `backend/cart-service/src/test/java/com/techstore/cart/application/service/AddCartItemServiceTest.java` para produto ativo (inclusive com estoque zero), soma repetida, produto inválido/inativo, quantidade não positiva e falha do catálogo sem persistência.
- [X] T031 [P] [US2] Escrever `backend/cart-service/src/test/java/com/techstore/cart/application/service/SetCartItemQuantityServiceTest.java` para substituição de quantidade, produto ativo com estoque zero, linha/produto ausente ou inativo e rejeição sem alteração.
- [X] T032 [P] [US2] Escrever `backend/cart-service/src/test/java/com/techstore/cart/application/service/RemoveCartItemServiceTest.java` para remoção própria, item ausente e preservação das linhas de outro usuário.
- [X] T033 [P] [US2] Escrever `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartConcurrentAddIntegrationTest.java` com Testcontainers, verificando uma única linha e soma exata sob adições concorrentes do mesmo usuário/produto.
- [X] T034 [P] [US2] Escrever `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartItemControllerTest.java` para `POST` 201/200, `PUT` 200, `DELETE` 204, validação, 404, 401 e 503 conforme OpenAPI.

### Implementation for User Story 2

- [X] T035 [P] [US2] Implementar `AddCartItemService` em `backend/cart-service/src/main/java/com/techstore/cart/application/service/AddCartItemService.java`, validar produto ativo antes do upsert e somar quantidade atomicamente.
- [X] T036 [P] [US2] Implementar `SetCartItemQuantityService` em `backend/cart-service/src/main/java/com/techstore/cart/application/service/SetCartItemQuantityService.java`, exigir item do proprietário, validar produto ativo e substituir por inteiro positivo.
- [X] T037 [P] [US2] Implementar `RemoveCartItemService` em `backend/cart-service/src/main/java/com/techstore/cart/application/service/RemoveCartItemService.java`, removendo apenas `(owner_user_id, product_id)` do usuário autenticado e retornando 404 se não existir.
- [X] T038 [US2] Criar requests validados e endpoints `POST /api/cart/items` e `PUT/DELETE /api/cart/items/{productId}` em `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartRequests.java` e `CartItemController.java`; rejeitar `userId`, exigir `quantity` inteira maior que zero e retornar o carrinho atualizado com 201 para nova linha ou 200 para consolidação/atualização.

**Checkpoint**: As cinco operações previstas entre consulta e manutenção preservam unicidade, autorização, validação e sem gravação parcial quando a validação remota falha.

---

## Phase 5: User Story 3 - Limpar o carrinho (Priority: P2)

**Goal**: Remover todas as linhas do proprietário de forma idempotente e sem tocar em carrinhos alheios.

**Independent Test**: Preparar por fixture várias linhas para um usuário e uma linha para outro; limpar o primeiro e confirmar no estado persistido que somente suas linhas foram removidas, com `204` também para a limpeza repetida. A consulta após limpeza é validada no E2E com US1; criar as linhas pela API depende de US2.

### Tests for User Story 3

- [X] T039 [P] [US3] Escrever `backend/cart-service/src/test/java/com/techstore/cart/application/service/ClearCartServiceTest.java` para remoção exclusiva por usuário e repetição idempotente.
- [X] T040 [P] [US3] Escrever `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartControllerTest.java` para `DELETE /api/cart` com token válido, ausente/inválido, vazio e múltiplas linhas.

### Implementation for User Story 3

- [X] T041 [US3] Implementar `ClearCartService` em `backend/cart-service/src/main/java/com/techstore/cart/application/service/ClearCartService.java`, excluindo todas as linhas pelo `owner_user_id` sem exigir carrinho materializado.
- [X] T042 [US3] Implementar `DELETE /api/cart` em `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartController.java`, exigindo JWT válido e retornando `204 No Content` inclusive para carrinho vazio.

**Checkpoint**: Limpeza repetível remove somente os itens do usuário autenticado.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificar integrações, documentação e critérios transversais antes do gate de implementação convergente.

- [X] T043 [P] Adicionar teste de rota Gateway em `backend/api-gateway/src/test/java/com/techstore/gateway/CartServiceRouteTest.java`, conferindo path `/api/cart/**`, método, body, Authorization e `X-Correlation-ID` encaminhados sem remover prefixo.
- [X] T044 [P] Publicar documentação springdoc e validar endpoints, schemas e bearer security em `backend/cart-service/src/main/java/com/techstore/cart/adapter/config/OpenApiConfiguration.java` e `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartOpenApiContractTest.java`, mantendo compatibilidade com `specs/004-cart/contracts/cart-service-api.openapi.yaml`.
- [X] T045 [P] Verificar health/metrics e logs sanitizados em `backend/cart-service/src/test/java/com/techstore/cart/adapter/config/CartObservabilityTest.java`, sem expor JWT, Authorization ou secrets.
- [ ] T046 Revisar e executar os cenários integrados de `specs/004-cart/quickstart.md`; atualizar o guia somente se os comandos/configuração implementados divergirem do design aprovado.
- [ ] T047 Executar `mvn -f backend/pom.xml -pl cart-service -am verify` e registrar em `specs/004-cart/quickstart.md` o resultado de build, testes e cenários de aceite SC-001 a SC-005.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: inicia sem dependências externas; T009 conclui o bootstrap depois da estrutura do módulo.
- **Foundational (Phase 2)**: depende do Setup e bloqueia as três histórias.
- **User Stories (Phases 3-5)**: implementações e testes isolados podem começar após Foundation; testes isolados preparam estado inicial por fixture/repositório, sem depender de outra história. A validação ponta a ponta de US2 requer US1 para consultar o resultado; a validação ponta a ponta de US3 requer US1 para consultar e US2 para criar os itens pela API.
- **Polish (Phase 6)**: depende das histórias desejadas e termina com quickstart e Maven verify.

### Dependency Graph

```text
Setup T001-T009
  -> Foundation T010-T025
        -> US1 T026-T029 (P1; fixture para linhas iniciais)
        -> US2 T030-T038 (P1; testes isolados, sem dependência de US1)
        -> US3 T039-T042 (P2; fixture para linhas iniciais)
      -> Validação E2E: US1 + US2 -> US3
      -> Polish T043-T047 (após as histórias selecionadas)
```

### Parallel Opportunities

- Setup: T001-T008 alteram arquivos distintos; T009 pode ser implementada após T002 e em paralelo às configurações externas.
- Foundation: T010/T011 não são paralelos por causa do ciclo teste-antes-implementação; testes dos adapters T014, T016, T019, T021 e T023 podem ser preparados em paralelo após as portas/bootstrap necessários, pois ficam em arquivos separados.
- US1: T026 e T027 são testes em arquivos distintos; executar juntos. A implementação T028 precede T029.
- US2: T030-T034 são testes em arquivos separados; T035-T037 também podem ser implementados em paralelo após os testes, pois são casos de uso em arquivos distintos. T038 integra os três.
- US3: T039 e T040 são testes independentes; implementar T041 antes de T042.
- Polish: T043-T045 atuam em módulos/arquivos distintos e podem ser feitos em paralelo após configuração das rotas e das histórias.

## Parallel Example: User Story 2

```text
Em paralelo, após Foundation:
T030 AddCartItemServiceTest.java
T031 SetCartItemQuantityServiceTest.java
T032 RemoveCartItemServiceTest.java
T033 CartConcurrentAddIntegrationTest.java
T034 CartItemControllerTest.java

Depois dos testes, em paralelo:
T035 AddCartItemService.java
T036 SetCartItemQuantityService.java
T037 RemoveCartItemService.java

Por último, integrar em CartRequests.java e CartItemController.java (T038).
```

## Implementation Strategy

### MVP

1. Concluir Setup e Foundation.
2. Implementar US1 para consulta autenticada e verificar isolamento.
3. Implementar US2 para que o cliente possa criar/manter itens e validar a jornada P1 completa.
4. Executar validação independente das duas histórias P1; US3 pode ser entregue depois como incremento P2.

### Incremental Delivery

1. Setup + Foundation: serviço compila, migra banco e valida JWT/Product Service.
2. US1: carrinho vazio e linhas próprias são consultáveis.
3. US2: adicionar, consolidar, atualizar e remover itens; testar concorrência e falhas do catálogo.
4. US3: limpar carrinho idempotentemente.
5. Polish: Gateway, OpenAPI, observabilidade, quickstart e `mvn verify`.

## Notes

- `[P]` indica arquivos distintos sem dependência de implementação incompleta; tarefas sem `[P]` devem seguir a ordem indicada.
- `[US1]`, `[US2]` e `[US3]` referenciam as histórias da [spec](spec.md).
- O Cart Service não persiste ProductSummary; indisponibilidade do Product Service durante validação/leitura não deve ser tratada como produto inexistente.
- Não incluir checkout, pedido, pagamento, estoque, Kafka, notificações ou acesso a databases de outros serviços.