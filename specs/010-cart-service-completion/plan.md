# Implementation Plan: Cart Service Completion

**Branch**: `004-cart` | **Date**: 2026-10-01 | **Spec**: [spec.md](spec.md)

**Input**: `specs/010-cart-service-completion/spec.md`

## Summary

Evoluir o carrinho existente para validar máximo configurável e estoque, persistir snapshot do preço, calcular subtotal/total e representar explicitamente linhas legadas sem preço. O trabalho preserva rotas, autenticação, ownership e CRUD. A V1 permanece imutável; uma V2 aditiva é seguida por inicialização idempotente de snapshots legados via portas existentes.

## Technical Context

**Language/Version**: Java 21; frontend TypeScript as already configured.

**Primary Dependencies**: Spring Boot 3.3, Spring Security/JWT, Spring Data JPA, JDBC, Flyway, RestClient, Springdoc; React, Zustand and Vitest for the existing cart consumer.

**Storage**: PostgreSQL `cart_items`; retain V1 and add V2 with price and snapshot-state fields. Product price precision is `NUMERIC(12,2)`.

**Testing**: JUnit 5, Mockito, Spring MVC tests, Testcontainers PostgreSQL; frontend Vitest and Testing Library.

**Target Platform**: Existing Railway Docker deployment for Cart Service; frontend consumes unchanged Gateway paths.

**Project Type**: Existing Java microservice plus its existing frontend cart view.

**Performance Goals**: No new latency or throughput target is specified. Preserve current Product Service connect/read timeouts; legacy initialization must finish before Cart readiness.

**Constraints**: No changes to Product Service, User Service, API Gateway, JWT, route paths or V1 migration; no stock reservation. Max defaults to 99 and remains configurable. Legacy inactive/missing product lines remain with unknown price; never substitute zero/default/current inactive price.

**Scale/Scope**: Existing per-user cart lines only; no order, checkout, payment, event or new microservice scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Plan response |
|---|---|---|
| Domain-driven microservices | PASS | Cart owns its table; Product Service remains the catalog/stock source through its existing API. No cross-service database access. |
| Hexagonal architecture | PASS | New rules remain in domain/application; startup backfill uses `CartStorePort` and `ProductCatalogPort`; HTTP, JDBC and Flyway remain adapters. |
| Clean code/SOLID | PASS | Extend current services/ports; add only the quantity policy and one-time legacy initializer required by the spec. |
| Test-first quality | PASS | Add focused unit, adapter, migration, HTTP and concurrency tests before accepting implementation. |
| API/contract discipline | PASS | Keep endpoint paths and successful statuses; document additive response fields and new conflict responses. |
| Security by default | PASS | Leave JWT, `SecurityConfig`, owner resolution and user isolation unchanged. |
| Observability | PASS | Preserve correlation IDs; initialization failure is visible and prevents readiness rather than becoming UNKNOWN. |
| Frontend architecture | PASS | Extend existing cart types/store/components; backend remains authoritative for price and totals. |
| Infrastructure/reproducibility | PASS | Externalize the max setting; existing Java 21 Docker build packages Flyway resources. |
| Spec-driven development | PASS | Follow this plan with tasks, implementation, test gates and convergence review. |

**Gate result**: PASS. The temporary dependency on Product Service exists only to resolve PENDING legacy snapshots and is covered by startup/readiness validation.

## Project Structure

### Documentation (this feature)

```text
specs/010-cart-service-completion/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Next phase (/speckit.tasks)
```

### Source Code (repository root)

```text
backend/cart-service/src/main/java/com/techstore/cart/
├── domain/                         # CartItem, price snapshot and quantity/stock rules
├── application/
│   ├── port/out/                   # CartStorePort, ProductCatalogPort
│   └── service/                    # Existing use cases plus legacy snapshot initializer
└── adapter/
  ├── config/                     # Existing bean wiring and max setting
  ├── http/                       # Existing routes, response DTOs and exception mapping
  ├── persistence/                # Entity, repository and CartPersistenceAdapter
  ├── product/                    # Existing Product Service HTTP adapter
  └── security/                   # Unchanged JWT and owner isolation
backend/cart-service/src/main/resources/db/migration/
└── V2__add_cart_item_price_snapshot.sql
backend/cart-service/src/test/java/com/techstore/cart/
├── domain/
├── application/service/
└── adapter/{http,persistence,product}/
frontend/src/{types,store,components/cart,pages}/
```

**Structure Decision**: Preserve the existing application/domain/adapter boundaries and current frontend cart modules. No endpoint, service or project layer is recreated. The UI is included because the specification requires customers to see stable line prices, subtotals, totals and unknown-price states.

## Research Decisions

See [research.md](research.md). The central decision is a nullable `unit_price` plus persisted snapshot state so an inactive legacy line cannot be mistaken for a line whose backfill has not run. V2 is additive; a Cart-owned initializer resolves PENDING rows through existing ports after Flyway and before readiness.

## Planned Changes by File

### Cart Service and API

| Arquivo | Motivo | Dependências | Impacto | Testes necessários |
|---|---|---|---|---|
| `backend/cart-service/src/main/java/com/techstore/cart/domain/CartItem.java` | Representar preço snapshot e estado known/unknown sem inventar preço. | `UnitPriceSnapshot`; regra de quantidade. | Evolui a linha existente; mantém product ID e quantidade positiva. | `CartItemTest`: estados válidos, preço known/unknown e quantidade mínima. |
| `backend/cart-service/src/main/java/com/techstore/cart/domain/UnitPriceSnapshot.java` (novo) | Expressar preço conhecido ou desconhecido sem confundir unknown com zero. | BigDecimal e precisão do catálogo. | Value object puro, sem Spring/JPA. | Invariantes known/null e valor não negativo. |
| `backend/cart-service/src/main/java/com/techstore/cart/domain/CartItemRules.java` (novo) | Centralizar teto configurado, soma da adição e comparação com estoque. | Max da configuração; estoque do catálogo. | Regras no domínio; não reserva nem decrementa estoque. | Max 99, excesso por soma, estoque exato/insuficiente e set. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/port/out/CartStorePort.java` | Aceitar snapshot/limites nas escritas e expor linhas PENDING e resolução condicional do legado. | `CartItem` e estado de snapshot. | Contrato de saída evolui; remove/update/clear preservam semântica. | Mocks e testes de serviços atualizados. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/port/out/ProductCatalogPort.java` | Expor quantidade em estoque já incluída na resposta recebida. | `ProductResponse.quantity`; API atual. | Nenhuma mudança externa. | Adapter e serviços com estoque variado. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/model/CartView.java` | Transportar snapshot, subtotal, estado por linha, total e máximo efetivo. | `UnitPriceSnapshot`; regra configurada; cálculo decimal. | Mantém informações atuais e acrescenta valores financeiros e o limite que a UI deve respeitar. | Carrinho vazio, conhecido, unknown e configuração de max. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/service/AddCartItemService.java` | Validar produto ativo, máximo acumulado e estoque; persistir preço atual como snapshot. | Rules, catálogo e store. | Preserva consolidação e 200/201; nova rejeição 409 sem escrita. | Novo, repetido, limite, estoque, produto ausente e catálogo indisponível. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/service/SetCartItemQuantityService.java` | Validar quantidade absoluta contra máximo/estoque e atualizar snapshot. | Rules, catálogo e store. | Preserva rota/status de sucesso e ausência 404. | Limites, estoque, preço atualizado e nenhuma mutação em erro. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/service/CartViewAssembler.java` | Calcular valores do snapshot persistido; nunca usar preço live como fallback. | Catálogo para disponibilidade/dados descritivos; CartView. | Disponibilidade do produto e do preço são estados independentes. | Preço estável, subtotal null, total null e produto inativo. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/service/GetCartService.java` | Entregar visão financeira agregada mantendo leitura/ownership. | Store e assembler. | GET preservado; vazio totaliza zero; unknown torna total null. | Vazio, known e unknown. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/service/InitializeLegacyPriceSnapshotsService.java` (novo) | Resolver PENDING: ativo→KNOWN/preço atual; 404/inativo→UNKNOWN/null; falha transitória aborta sem classificar como unknown. | Store, ProductCatalogPort; update condicional/idempotente. | Preserva linhas e evita preço fallback. | Ativo, ausente/inativo, erro transitório, repetição e idempotência. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/config/CartApplicationConfiguration.java` | Injetar max e acionar initializer pós-Flyway antes de readiness. | `application.yml` e initializer. | Dependência temporária do catálogo durante backfill. | Binding/wiring e erro transitório. |
| `backend/cart-service/src/main/resources/application.yml` | Configurar `techstore.cart.max-item-quantity`, default 99, externalizado como `MAX_CART_ITEM_QUANTITY`. | Binding Spring existente. | Configuração Cart-only; JWT, URL e timeouts intactos. | Default, override e valor não positivo. |
| `backend/cart-service/src/main/resources/db/migration/V2__add_cart_item_price_snapshot.sql` (novo) | Adicionar `unit_price NUMERIC(12,2)` nullable e estado PENDING/KNOWN/UNKNOWN com constraints coerentes. | V1 imutável; precisão do catálogo. | Legado inicia PENDING; sem default monetário ou FK novo. | Testcontainers V1→V2, constraints e checksum V1 estável. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/persistence/CartItemEntity.java` | Mapear preço e estado para o domínio. | Migration V2 e UnitPriceSnapshot. | `ddl-auto: validate` reflete schema novo. | Round-trip JPA known/unknown. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/persistence/CartItemJpaRepository.java` | Consultar PENDING e finalizar snapshot condicionalmente; proteger updates. | Entity e V2. | Retry não sobrescreve estado resolvido. | Updates condicionais e transacionais. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/persistence/CartPersistenceAdapter.java` | Gravar snapshot em insert/upsert/set e condicionar atomicamente soma ao max/estoque observado. | Port, SQL PostgreSQL, max e stock. | Evita corrida local acima dos limites; remove/clear sem mudança. | Persistência e concorrência Testcontainers. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/product/ProductCatalogHttpAdapter.java` | Mapear `quantity` como estoque e manter 404 como ausente/inativo. | `ProductResponse.quantity` já desserializado. | Nenhuma mudança no Product Service. | Quantidade, ativo, 404, timeout e 5xx. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/product/ProductResponse.java` | Nenhuma alteração prevista: já desserializa `quantity` e `isActive`. | Contrato atual. | Mantido. | Adapter confirma mapeamento. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartApiModels.java` | Acrescentar snapshot, subtotal, flags de preço, total nullable e máximo efetivo. | CartView e contrato. | Campos antigos/rotas preservados; `product.price` usa snapshot known e null quando unknown. | Serialização MVC, max retornado e OpenAPI. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartItemController.java` | Documentar conflitos; manter POST/PUT, caminhos e statuses de sucesso. | Exceptions e DTO. | Sem endpoint novo; 409 somente para max/estoque. | 200/201 existentes, 400/404/409 e campos financeiros. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartQueryController.java` | Documentar GET ampliado e serializar total/estados. | DTO e CartView. | GET/401/503 preservados; unknown explícito. | GET known/unknown e contrato. |
| `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartExceptionHandler.java` | Mapear limite/estoque para 409 com mensagem clara. | Exceptions novas; ErrorResponse atual. | Status anteriores preservados; JWT inalterado. | 409 e regressão dos status existentes. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/exception/CartQuantityLimitExceededException.java` (novo) | Distinguir teto violado de request malformado. | Rules e handler. | Novo erro de negócio. | Mapeamento 409. |
| `backend/cart-service/src/main/java/com/techstore/cart/application/exception/InsufficientProductStockException.java` (novo) | Distinguir estoque insuficiente. | ProductCatalogPort e handler. | Sem reserva/decremento. | Mapeamento 409 e nenhuma escrita. |

### Frontend consumidor

| Arquivo | Motivo | Dependências | Impacto | Testes necessários |
|---|---|---|---|---|
| `frontend/src/types/cart.ts` | Tipar snapshot, subtotal, flags e total nullable. | Contrato atualizado. | URL/métodos atuais permanecem. | TypeScript build e fixtures. |
| `frontend/src/store/cartStore.ts` | Guardar total/status/máximo efetivo com itens em GET/mutações; reset em sessão/clear. | CartResponse ampliada. | Estado segue resposta confirmada do backend; não duplica configuração de max. | Carga, mutação, reset, unknown e limites configurados. |
| `frontend/src/components/cart/CartItem.tsx` | Exibir snapshot/subtotal ou aviso de preço unknown; preservar remoção e limitar controle pelo max retornado. | Tipos e disponibilidade atual. | Preço live deixa de ser fonte financeira; item active/unknown ainda pode atualizar. | Known, unknown, indisponível e controles no max efetivo. |
| `frontend/src/pages/CartPage.tsx` | Exibir total conhecido ou aviso explícito de indisponibilidade. | Store e response. | Substitui expectativa atual de ausência de total. | Total, unknown e recuperação após remoção. |
| `frontend/src/styles.css` | Estilizar subtotal, total e avisos na área do carrinho. | Novos elementos. | Mudança local à área cart. | Testes de componente e inspeção responsiva. |
| `frontend/src/services/apiClient.ts` | Preservar mensagem HTTP 409 para max/estoque. | ErrorResponse. | Feedback existente pela store continua. | 409 com mensagem do servidor. |

### Testes a evoluir/adicionar

| Arquivo | Motivo/dependências | Testes necessários/impacto |
|---|---|---|
| `backend/cart-service/src/test/java/com/techstore/cart/domain/CartItemTest.java` | Novos estados e invariantes. | Known/unknown, quantidade mínima e valor real zero distinto de unknown. |
| `backend/cart-service/src/test/java/com/techstore/cart/domain/CartItemRulesTest.java` (novo) | Política de domínio. | Max exato, soma acima, estoque exato/insuficiente e set. |
| `backend/cart-service/src/test/java/com/techstore/cart/application/service/AddCartItemServiceTest.java` | Add e consolidação. | Produto active/inactive, max, estoque, falha sem persistência. |
| `backend/cart-service/src/test/java/com/techstore/cart/application/service/SetCartItemQuantityServiceTest.java` | Atualização. | Máximo/estoque, snapshot atualizado e erro sem mutação. |
| `backend/cart-service/src/test/java/com/techstore/cart/application/service/CartViewAssemblerTest.java` | Cálculos e disponibilidade. | Preço estável, subtotal/total known e unknown. |
| `backend/cart-service/src/test/java/com/techstore/cart/application/service/InitializeLegacyPriceSnapshotsServiceTest.java` (novo) | Backfill pelas portas. | Ativo→KNOWN; 404→UNKNOWN; erro mantém PENDING; retry idempotente. |
| `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartPersistenceAdapterTest.java` | V2 e SQL condicional. | Estados, sem sentinel price, limite e migration. |
| `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartConcurrentAddIntegrationTest.java` | Estender corrida existente. | Uma linha; concorrência não ultrapassa max/estoque observado. |
| `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartItemControllerTest.java` | Compatibilidade e DTO. | Sucessos 200/201, 409 e campos financeiros. |
| `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartQueryControllerTest.java` | GET financeiro. | Total null quando há UNKNOWN. |
| `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartExceptionHandlerTest.java` | Erros novos. | 409 claro; status antigos intactos. |
| `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartOpenApiContractTest.java` | Contrato Springdoc. | Sem remoção/renomeação de paths; response/errors atualizados. |
| `frontend/src/pages/__tests__/CartPage.test.tsx` | Remover expectativa de “sem total”. | Total conhecido/unknown e recuperação após remoção. |
| `frontend/src/components/cart/__tests__/CartItem.test.tsx` | Financeiro por linha. | Subtotal, preço unknown, remoção e limite. |
| `frontend/src/store/__tests__/cartStore.test.ts` | Estado de response ampliada. | GET/mutação/reset atualizam itens e total juntos. |
| `frontend/src/services/__tests__/apiClient.test.ts` | Erro 409. | Mensagem do backend chega à interface. |

### Arquivos sem alteração prevista

- `backend/cart-service/src/main/java/com/techstore/cart/adapter/security/SecurityConfig.java`, `CartOwnerResolver.java` e `CartController.java`: JWT, ownership e clear preservados.
- `RemoveCartItemService.java` e `ClearCartService.java`: operações mantidas; remoção suporta linha sem preço.
- `backend/cart-service/Dockerfile`: sem mudança; `COPY backend/` inclui a migration. Apenas construir/inspecionar a imagem.
- Product Service, User Service e API Gateway: nenhum arquivo alterado.
- Rotas existentes: não criar nem renomear endpoints.

## Sequência e gates

1. Domínio, ports e testes unitários de snapshot/quantidade/estoque.
2. V2 additive e persistência; verificar V1 intacta.
3. Inicializador PENDING: active→KNOWN, 404/inactive→UNKNOWN, erro transitório impede readiness.
4. Evoluir add/set/get, DTOs e exceptions; preservar paths/sucessos e adicionar 409 de negócio.
5. Atualizar types/store/UI e testes do consumidor.
6. Executar testes backend/frontend, package e build da imagem Docker.
7. Railway com escrita antiga suspensa; liberar tráfego somente após migration, backfill e readiness.

## Railway e rollback

- Manter `TECHSTORE_JWT_SECRET`, `SPRING_DATASOURCE_*` e `PRODUCT_SERVICE_URL`; configurar `MAX_CART_ITEM_QUANTITY` ou usar default 99.
- Product Service deve estar acessível antes do initializer. Timeout/5xx não pode ser tratado como produto ausente.
- Não permitir mutações por instância antiga entre V2 e conclusão do initializer: janela controlada de escrita suspensa durante migration/backfill; reabrir após `/actuator/health/readiness` saudável.
- Verificar V2 em `flyway_schema_history`, V1/checksum intacta, nenhum PENDING após readiness, UNKNOWN com `unit_price IS NULL` e linhas preservadas.
- Testar GET/add/update/remove/clear, snapshot estável, total indisponível, limite, estoque, JWT e isolamento.
- Deploy do frontend somente após contrato backend ativo; não implantar Product, User ou Gateway.
- Após mutações na versão nova, preferir roll-forward: versão antiga não atualiza snapshot ao mudar quantidade. V2 não deve ser removida.

## Critérios de conclusão

- FR-001..FR-020 e SC-001..SC-008 cobertos por testes.
- Nenhum preço artificial, remoção automática ou mudança de endpoint/JWT/serviço externo.
- V1 intacta; V2, initializer, concorrência, API e UI validados.
- Testes backend/frontend e build Docker aprovados.
- Resultado do deploy e smoke tests Railway registrado; teste local não substitui validação de produção.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | No constitution violation identified. | Not applicable. |
