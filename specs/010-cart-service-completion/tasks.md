# Tasks: Cart Service Completion

**Input**: Design documents from `specs/010-cart-service-completion/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/cart-api.yaml`, `quickstart.md`

**Tests**: Required by the specification for every new business rule and persistence/API change. Test tasks precede their implementation tasks.

**Organization**: Tasks are grouped by the four user stories in `spec.md`; shared domain/schema/port work is foundational.

## Format: `[ID] [P?] [Story] Description`

- `[P]`: parallelizable across independent files after prerequisites.
- `[US#]`: user-story mapping from `spec.md`; setup/foundation/polish tasks omit this label.
- Every task names its target file(s).

## Phase 1: Setup

**Purpose**: Capture the existing public behavior before extending the response contract.

- [x] T001 [P] Add regression assertions for the existing `/api/cart` routes and successful statuses in `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartOpenApiContractTest.java`.

## Phase 2: Foundational

**Purpose**: Establish shared domain, migration, port, configuration, and persistence support required before any user story.

- [x] T002 [P] Add tests for known/unknown price invariants in `backend/cart-service/src/test/java/com/techstore/cart/domain/CartItemTest.java` and quantity/stock boundaries in `backend/cart-service/src/test/java/com/techstore/cart/domain/CartItemRulesTest.java`.
- [x] T003 [P] Add a Testcontainers migration test in `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartPriceSnapshotMigrationTest.java` proving V2 is additive, V1 remains unchanged, and legacy rows begin PENDING with null `unit_price` (execution is skipped locally because Docker is unavailable).
- [x] T004 Implement the pure domain types and rules in `backend/cart-service/src/main/java/com/techstore/cart/domain/UnitPriceSnapshot.java`, `backend/cart-service/src/main/java/com/techstore/cart/domain/CartItem.java`, and `backend/cart-service/src/main/java/com/techstore/cart/domain/CartItemRules.java`; keep PENDING as persistence/bootstrap state, not a served domain price.
- [x] T005 Add `backend/cart-service/src/main/resources/db/migration/V2__add_cart_item_price_snapshot.sql` with nullable `unit_price NUMERIC(12,2)`, PENDING/KNOWN/UNKNOWN state, and constraints; do not edit V1 or use a price default.
- [x] T006 Map price and snapshot state in `backend/cart-service/src/main/java/com/techstore/cart/adapter/persistence/CartItemEntity.java` and add conditional legacy-resolution queries in `backend/cart-service/src/main/java/com/techstore/cart/adapter/persistence/CartItemJpaRepository.java`.
- [x] T007 Evolve `backend/cart-service/src/main/java/com/techstore/cart/application/port/out/CartStorePort.java` for price-bearing writes, atomic limit outcomes, and pending legacy line resolution; preserve remove/clear contracts.
- [x] T008 Extend `backend/cart-service/src/main/java/com/techstore/cart/application/port/out/ProductCatalogPort.java` and map existing `ProductResponse.quantity` in `backend/cart-service/src/main/java/com/techstore/cart/adapter/product/ProductCatalogHttpAdapter.java` as current available stock; do not alter Product Service.
- [x] T009 Bind `techstore.cart.max-item-quantity` with default 99 in `backend/cart-service/src/main/resources/application.yml`, wire it through `backend/cart-service/src/main/java/com/techstore/cart/adapter/config/CartApplicationConfiguration.java`, and expose the effective value in `CartResponse`.
- [x] T010 Implement snapshot persistence and conditional PostgreSQL upsert/update guards in `backend/cart-service/src/main/java/com/techstore/cart/adapter/persistence/CartPersistenceAdapter.java`; the resulting quantity must not exceed configured max or the stock observed by the operation. (Database runtime assertions require Docker.)

**Checkpoint**: V1-compatible schema evolution and shared domain/port/persistence contracts are ready; JWT and owner resolution are untouched.

## Phase 3: User Story 1 - Add items within quantity and stock limits (P1)

**Goal**: Add or consolidate only valid active-product quantities and persist the operation's price snapshot.

**Independent Test**: Add an active product within max and stock; verify snapshot and quantity. Verify invalid quantity, max, stock, inactive product, or catalog failure leaves the cart unchanged.

### Tests first

- [x] T011 [P] [US1] Add add-rule cases to `backend/cart-service/src/test/java/com/techstore/cart/application/service/AddCartItemServiceTest.java` for new line, repeated add, max 99, stock exact/insufficient, inactive product, and no write on failure.
- [x] T012 [P] [US1] Add POST status/response cases to `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartItemControllerTest.java` for existing 200/201 behavior and new 409 business conflicts.
- [x] T013 [P] [US1] Add max-quantity and HTTP 409 message cases to `frontend/src/store/__tests__/cartStore.test.ts` and `frontend/src/services/__tests__/apiClient.test.ts`.

### Implementation

- [x] T014 [US1] Add `backend/cart-service/src/main/java/com/techstore/cart/application/exception/CartQuantityLimitExceededException.java` and `backend/cart-service/src/main/java/com/techstore/cart/application/exception/InsufficientProductStockException.java`; map both to 409 with actionable messages in `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartExceptionHandler.java`.
- [x] T015 [US1] Evolve `backend/cart-service/src/main/java/com/techstore/cart/application/service/AddCartItemService.java` to check active product, resulting line quantity, current stock, and current price before calling the atomic store write; reject the whole repeated add if the result exceeds 99.
- [x] T016 [US1] Extend POST response mapping and OpenAPI annotations in `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartApiModels.java` and `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartItemController.java`; keep path and successful statuses unchanged.
- [x] T017 [US1] Enforce the 1..99 client-side guard and preserve server 409 feedback in `frontend/src/store/cartStore.ts` and `frontend/src/services/apiClient.ts`; backend remains authoritative until the configured maximum is included in CartResponse by US3.

**Checkpoint**: Add and repeated-add flows enforce maximum/stock atomically, preserve 200/201 successes, and return the persisted price snapshot.

## Phase 4: User Story 2 - Change an existing cart quantity (P1)

**Goal**: Replace an owned line quantity only when the requested quantity fits the configured maximum and current stock; refresh its price snapshot.

**Independent Test**: Update a line within bounds and verify quantity/snapshot; over-limit, insufficient-stock, missing, inactive, or unauthenticated requests do not mutate it.

### Tests first

- [x] T018 [P] [US2] Add quantity replacement cases to `backend/cart-service/src/test/java/com/techstore/cart/application/service/SetCartItemQuantityServiceTest.java` for max, stock, refreshed price, missing/inactive line, and unchanged state on failure.
- [x] T019 [P] [US2] Add PUT success/409/404 response cases to `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartItemControllerTest.java`.
- [x] T020 [P] [US2] Add quantity-control boundary and pending/error behavior tests to `frontend/src/components/cart/__tests__/CartItem.test.tsx`.

### Implementation

- [x] T021 [US2] Evolve `backend/cart-service/src/main/java/com/techstore/cart/application/service/SetCartItemQuantityService.java` to validate requested quantity and stock and persist the current catalog price in the same line update.
- [x] T022 [US2] Update PUT response documentation in `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartItemController.java` for 409 while preserving the path and 200 success response.
- [x] T023 [US2] Update `frontend/src/components/cart/CartItem.tsx` so quantity controls stop at the configured client default 99 and display the confirmed response values without optimistic price changes.

**Checkpoint**: Update behavior obeys the same rules as add; active authenticated ownership and existing successful endpoint contract remain.

## Phase 5: User Story 3 - Review stable item and cart prices (P1)

**Goal**: Persist stable snapshots, initialize legacy rows according to their state, and return/display subtotal and total or explicit unavailability.

**Independent Test**: Change catalog price without cart mutation and verify saved values stay fixed; active legacy rows become KNOWN, missing/inactive rows remain UNKNOWN, and totals recover after removing every UNKNOWN line.

### Tests first

- [x] T024 [P] [US3] Add backfill unit tests in `backend/cart-service/src/test/java/com/techstore/cart/application/service/InitializeLegacyPriceSnapshotsServiceTest.java` for active, 404/inactive, transient catalog failure, idempotent retry, and line preservation.
- [x] T025 [P] [US3] Add persisted financial-view cases to `backend/cart-service/src/test/java/com/techstore/cart/application/service/CartViewAssemblerTest.java` and `backend/cart-service/src/test/java/com/techstore/cart/application/service/GetCartServiceTest.java` for empty, known, mixed, and all-known carts.
- [x] T026 [P] [US3] Add legacy startup/migration integration cases in `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartLegacyPriceSnapshotIntegrationTest.java` for PENDING→KNOWN/UNKNOWN and restart idempotency (runtime skipped without Docker).
- [x] T027 [P] [US3] Add GET unknown-price response assertions in `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartQueryControllerTest.java` and schema assertions in `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartOpenApiContractTest.java`.
- [x] T028 [P] [US3] Add state/store/UI cases in `frontend/src/store/__tests__/cartStore.test.ts`, `frontend/src/components/cart/__tests__/CartItem.test.tsx`, and `frontend/src/pages/__tests__/CartPage.test.tsx` for known totals, unknown-price warning, and total recovery after removal.

### Implementation

- [x] T029 [US3] Implement `backend/cart-service/src/main/java/com/techstore/cart/application/service/InitializeLegacyPriceSnapshotsService.java`: active catalog result stores current price as initial legacy snapshot; 404/inactive stores UNKNOWN/null; timeout/5xx leaves PENDING and fails startup readiness; never delete lines.
- [x] T030 [US3] Invoke the initializer after Flyway through `backend/cart-service/src/main/java/com/techstore/cart/adapter/config/CartApplicationConfiguration.java`; process distinct product IDs, use conditional idempotent store updates, and do not accept traffic until initialization succeeds. (Postgres startup integration runtime skipped without Docker.)
- [x] T031 [US3] Extend `backend/cart-service/src/main/java/com/techstore/cart/application/model/CartView.java` and `backend/cart-service/src/main/java/com/techstore/cart/application/service/CartViewAssembler.java` to calculate `subtotal = unit_price × quantity`; return null subtotal/total when any line is UNKNOWN and total zero for an empty cart.
- [x] T032 [US3] Ensure `backend/cart-service/src/main/java/com/techstore/cart/application/service/GetCartService.java` returns the financial aggregate while ordinary reads never replace a known or unknown snapshot with live catalog price.
- [x] T033 [US3] Complete financial fields/unknown markers in `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartApiModels.java` and `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartQueryController.java`; update `specs/010-cart-service-completion/contracts/cart-api.yaml` without adding or renaming paths.
- [x] T034 [US3] Update `frontend/src/types/cart.ts` and `frontend/src/store/cartStore.ts` to retain `maxItemQuantity`, `total`, and `totalAvailable` from every confirmed GET/mutation response and reset them on clear/session change.
- [x] T035 [US3] Update `frontend/src/components/cart/CartItem.tsx` and `frontend/src/pages/CartPage.tsx` to use the returned max, show snapshot price/subtotal, explain unknown-price lines, show unavailable total, and keep removal available.
- [x] T036 [US3] Add focused styles for financial totals and unknown-price status in `frontend/src/styles.css`, preserving current responsive cart layout.

**Checkpoint**: All customer-visible financial values come from persisted snapshots; legacy unknown prices are explicit, never fabricated, and never auto-removed.

## Phase 6: User Story 4 - Preserve existing cart operations and ownership (P2)

**Goal**: Prove the existing authenticated CRUD and per-owner isolation survive the additive response/schema changes.

**Independent Test**: Existing GET/add/update/remove/clear paths keep their success behavior; requests without valid JWT fail, and one owner cannot affect another owner's cart.

- [x] T037 [P] [US4] Extend `backend/cart-service/src/test/java/com/techstore/cart/adapter/security/CartSecurityIntegrationTest.java` and `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartPersistenceAdapterTest.java` to retain JWT rejection and cross-owner isolation coverage (JWT passed; database isolation Testcontainers skipped without Docker).
- [x] T038 [P] [US4] Extend `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartControllerTest.java` and `backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartItemControllerTest.java` to verify clear/remove statuses and removal of UNKNOWN lines without quantity mutation (HTTP removal coverage passes; persisted UNKNOWN removal is in Testcontainers integration and skipped without Docker).
- [x] T039 [P] [US4] Extend `frontend/src/pages/__tests__/CartPage.test.tsx` to verify an unavailable/unknown-price line remains removable and the total becomes calculable after removal.
- [x] T040 [US4] Review `backend/cart-service/src/main/java/com/techstore/cart/adapter/security/SecurityConfig.java`, `backend/cart-service/src/main/java/com/techstore/cart/adapter/security/CartOwnerResolver.java`, `backend/cart-service/src/main/java/com/techstore/cart/adapter/http/CartController.java`, `backend/cart-service/src/main/java/com/techstore/cart/application/service/RemoveCartItemService.java`, and `backend/cart-service/src/main/java/com/techstore/cart/application/service/ClearCartService.java`; confirm no security/CRUD reimplementation is needed and record the regression result in `specs/010-cart-service-completion/quickstart.md` (JWT checks passed; security configuration and routes unchanged).

## Phase 7: Polish and Cross-Cutting Validation

- [x] T041 Extend `backend/cart-service/src/test/java/com/techstore/cart/adapter/persistence/CartConcurrentAddIntegrationTest.java` to race additions at maximum/stock boundaries and prove one line never exceeds either bound (compiled; runtime skipped because Docker is unavailable).
- [x] T042 Run backend module and dependent-module tests with `mvn -f backend/pom.xml -pl cart-service -am test`; resolve failures attributable to this feature and record results in `specs/010-cart-service-completion/quickstart.md` (76 tests, 0 failures/errors, 12 Testcontainers skips).
- [x] T043 Run `npm --prefix frontend test -- --run` and `npm --prefix frontend run build`; record results in `specs/010-cart-service-completion/quickstart.md` (15 files/64 tests passed; build passed).
- [ ] T044 BLOCKED: Build `backend/cart-service/Dockerfile` from repository root and verify the packaged JAR contains V2; Docker CLI/daemon is unavailable here.
- [ ] T045 BLOCKED: Execute the controlled Railway rollout and smoke checklist in `specs/010-cart-service-completion/quickstart.md`; Railway deployment access/CLI is unavailable, so no production migration or smoke result is claimed.

## Dependencies & Execution Order

### Phase dependencies

- Setup (Phase 1) precedes Foundational (Phase 2).
- Foundational (Phase 2) blocks all stories: domain model, V2, ports, catalog stock mapping, max config, and conditional persistence must exist first.
- US1 and US2 can be implemented in parallel after Phase 2 if shared port/domain contracts are frozen; their service/controller/test files are mostly separate.
- US3 depends on snapshot writes from US1/US2 and adds legacy initialization, financial views, and frontend display.
- US4 regression validation follows the functional stories; it does not change JWT or existing clear/remove use cases.
- Polish and Railway gates follow all stories. Do not deploy intermediate states that leave PENDING rows serving or allow old-version writes after V2.

### User story dependency graph

```text
Setup → Foundational → ┬→ US1 (add/max/stock/snapshot) ─┐
                       └→ US2 (quantity update) ────────┴→ US3 (legacy/financial view)
                                                               ↓
                                                             US4
                                                               ↓
                                                     Polish/Railway validation
```

### Parallel opportunities

- T002 and T003 are independent test files during initial setup.
- Within US1, T011, T012, and T013 can be authored in parallel before implementation.
- Within US2, T018, T019, and T020 are separate test files and can run in parallel.
- Within US3, T024–T028 are independent test surfaces before service/API/UI implementation; backend implementation and frontend consumer work can proceed in parallel after the response contract is fixed.
- US1 and US2 can be staffed in parallel after Phase 2; avoid concurrent edits to the shared DTO/port files.

## Parallel Example: User Story 1

```text
Task T011: add application tests in backend/cart-service/src/test/java/com/techstore/cart/application/service/AddCartItemServiceTest.java
Task T012: add POST contract tests in backend/cart-service/src/test/java/com/techstore/cart/adapter/http/CartItemControllerTest.java
Task T013: add client limit/error tests in frontend/src/store/__tests__/cartStore.test.ts and frontend/src/services/__tests__/apiClient.test.ts
```

## Implementation Strategy

### MVP first

1. Complete setup and shared domain/schema/persistence foundation.
2. Deliver US1: additions obey max/stock and persist snapshots.
3. Validate US1 independently before US2/US3. This is a functional MVP only; do not deploy to Railway before US2, US3 legacy backfill, and the compatibility gate are complete.

### Incremental delivery

1. Add/update rules can be developed after the shared ports and DB guards are stable.
2. Financial reads and legacy state follow once new mutations always write known snapshots.
3. Preserve/ownership regression suite runs before release.
4. Railway rollout is a release gate, not implied by local test success.
