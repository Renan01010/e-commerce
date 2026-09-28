# Tasks: Gerenciamento de Categorias

**Input**: Design documents from `specs/009-category-management/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/category-management-api.md`, `quickstart.md`

**Tests**: Obrigatórios por FR-018. Usar JUnit 5, Mockito, MockMvc e Testcontainers/PostgreSQL já disponíveis; adicionar testes antes da implementação correspondente.

**Limites**: Evoluir somente o Product Service e o contrato OpenAPI existente. Não criar microserviço, módulo Maven, tabela de categorias, CRUD duplicado ou frontend administrativo. Não alterar `V1__create_catalog_schema.sql`. Não implementar produtos/checkout/pedidos/pagamento/estoque.

**Migration de slug**: `V2__AddCategorySlug` deve fazer preflight de todas as categorias, inclusive inativas, antes de DDL/DML. Não gerar sufixos. Para colisões históricas, o mapa é um objeto JSON com UUIDs de categoria como chaves e slugs canônicos como valores, por exemplo `{"550e8400-e29b-41d4-a716-446655440001":"audio-profissional"}`. Deve conter todos os IDs de cada grupo colidente. `CATEGORY_SLUG_OVERRIDES_JSON` é ligado a `spring.flyway.placeholders.categorySlugOverrides`; mapa ausente/incompleto/inválido ou ainda conflitante aborta sem alteração parcial e informa ID, nome e slug candidato.

## Phase 1: Setup

**Purpose**: Reutilizar o módulo Product Service e infraestrutura Maven/PostgreSQL/Flyway existentes.

Nenhuma tarefa de inicialização de projeto ou dependência nova é necessária. `backend/product-service` já está no reactor Maven e já possui Category, Flyway, Springdoc e dependências de teste.

## Phase 2: Foundational

**Purpose**: Estabelecer a regra única de slug, o campo persistido e a migration segura; bloqueia as três histórias.

- [ ] T001 [P] Add unit tests for canonical slug format, transliteration, separator normalization, empty result and 120-character maximum in `backend/product-service/src/test/java/com/techstore/product/domain/CategorySlugTest.java`
- [ ] T002 [P] Add PostgreSQL/Testcontainers migration tests for collision diagnostics, explicit UUID-to-slug map, rollback, inactive slug reservation and preservation of IDs/FKs in `backend/product-service/src/test/java/com/techstore/product/migration/CategorySlugMigrationTest.java`
- [ ] T003 Implement the shared `CategorySlug` canonicalization used by create, update and migration in `backend/product-service/src/main/java/com/techstore/product/domain/CategorySlug.java`
- [ ] T004 Add the required slug to the existing category domain/entity conversion and response model, preserving existing fields and identity, in `backend/product-service/src/main/java/com/techstore/product/domain/Category.java`, `backend/product-service/src/main/java/com/techstore/product/adapter/persistence/CategoryEntity.java` and `backend/product-service/src/main/java/com/techstore/product/adapter/http/ApiModels.java`
- [ ] T005 [P] Bind optional external `CATEGORY_SLUG_OVERRIDES_JSON` to Flyway placeholder `categorySlugOverrides` in `backend/product-service/src/main/resources/application.yml` and pass the optional variable through the existing `product-service` service in `docker-compose.yml`; document the JSON as an object `{ "<categoryId UUID>": "<canonical slug>" }`, with one entry for every ID in each collision group
- [ ] T006 Implement `V2__AddCategorySlug` in `backend/product-service/src/main/java/db/migration/V2__AddCategorySlug.java`: use the shared canonicalizer, precompute/validate every slug before writes, abort with all collision IDs/names/candidates when mapping is absent or invalid, persist explicit mappings without suffixes, add NOT NULL/unique constraints only after complete validation, preserve inactive rows and all IDs/FKs, and leave `V1__create_catalog_schema.sql` unchanged
- [ ] T007 Add existing-name and global-slug uniqueness query methods (including inactive records) to `backend/product-service/src/main/java/com/techstore/product/application/port/CategoryRepository.java`, `backend/product-service/src/main/java/com/techstore/product/adapter/persistence/CategoryJpaRepository.java` and `backend/product-service/src/main/java/com/techstore/product/adapter/persistence/CategoryPersistenceAdapter.java`

**Checkpoint**: A single canonicalizer is shared; schema evolution is additive; migration detects all conflicts and performs no partial writes until the complete active/inactive slug set is valid.

## Phase 3: User Story 1 - Criar categoria identificável por slug (Priority: P1)

**Goal**: Administrators can create categories with a generated or explicit slug while retaining existing validation and ADMIN authorization.

**Independent Test**: With an ADMIN JWT, create one category without slug and one with a valid explicit slug; verify canonical response and persistence, then repeat a name/slug and verify `409` without an extra row. Verify invalid values return `400` and non-admin requests return `401`/`403`.

### Tests for User Story 1

- [ ] T008 [P] [US1] Extend category service tests for generated/provided slug, duplicate existing name, duplicate slug including inactive rows, invalid values and no partial save in `backend/product-service/src/test/java/com/techstore/product/application/service/CategoryServiceTest.java`
- [ ] T009 [US1] Extend catalog API integration tests for POST `201`, slug in CategoryResponse, omitted-slug generation, explicit-slug normalization, duplicate `409`, validation `400` and ADMIN authorization in `backend/product-service/src/test/java/com/techstore/product/CatalogApiIntegrationTest.java`

### Implementation for User Story 1

- [ ] T010 [US1] Update category creation to generate slug when omitted, normalize explicit slug through `CategorySlug`, validate duplicate names/slugs globally and preserve existing parent/audit rules in `backend/product-service/src/main/java/com/techstore/product/application/service/CategoryService.java`
- [ ] T011 [US1] Extend `CreateCategoryRequest` with optional slug, expose slug in `CategoryResponse`, and map the request through the existing POST `/api/categories` without accepting actor identity in `backend/product-service/src/main/java/com/techstore/product/adapter/http/ApiModels.java` and `backend/product-service/src/main/java/com/techstore/product/adapter/http/CategoryController.java`

**Checkpoint**: Existing category POST creates a persisted canonical unique slug; old request bodies without slug remain compatible.

## Phase 4: User Story 2 - Listar e pesquisar categorias (Priority: P1)

**Goal**: Preserve public active-only category reads while allowing ADMIN to search and include inactive categories.

**Independent Test**: Seed active/inactive categories whose name, description and slug can be matched; verify public search never returns inactive rows, ADMIN can use `includeInactive=true`, and unauthenticated/non-admin callers receive `401`/`403` without inactive data.

### Tests for User Story 2

- [ ] T012 [P] [US2] Extend category service tests for case-insensitive partial search across name/description/slug, active-only default, include-inactive result and empty search behavior in `backend/product-service/src/test/java/com/techstore/product/application/service/CategoryServiceTest.java`
- [ ] T013 [US2] Extend API integration tests for public search, active-only visibility, ADMIN includeInactive list/detail, `401`/`403`, not-found behavior and empty results in `backend/product-service/src/test/java/com/techstore/product/CatalogApiIntegrationTest.java`

### Implementation for User Story 2

- [ ] T014 [US2] Add repository queries for active search and administrative search over active/inactive categories, matching partial case-insensitive name, description or slug, in `backend/product-service/src/main/java/com/techstore/product/adapter/persistence/CategoryJpaRepository.java`, `backend/product-service/src/main/java/com/techstore/product/adapter/persistence/CategoryPersistenceAdapter.java` and `backend/product-service/src/main/java/com/techstore/product/application/port/CategoryRepository.java`
- [ ] T015 [US2] Implement search and include-inactive service behavior while preserving existing displayOrder/name sorting and public active-only defaults in `backend/product-service/src/main/java/com/techstore/product/application/service/CategoryService.java`
- [ ] T016 [US2] Extend existing GET `/api/categories` and GET `/api/categories/{id}` with optional `search`/`includeInactive`; enforce ADMIN when includeInactive is true while keeping ordinary GET public in `backend/product-service/src/main/java/com/techstore/product/adapter/http/CategoryController.java`

**Checkpoint**: Public callers see/search active categories only; only ADMIN can discover or retrieve inactive categories.

## Phase 5: User Story 3 - Editar, ativar e desativar categorias (Priority: P1)

**Goal**: Administrators can update slug and existing editable fields, retain the slug when omitted, and reactivate soft-deactivated categories.

**Independent Test**: Update category fields with and without slug, verify slug preservation and conflict behavior, deactivate a category without products, reactivate it with bodyless PATCH, then verify the public listing; confirm existing product-linked category deactivation still returns `409`.

### Tests for User Story 3

- [ ] T017 [P] [US3] Extend category service tests for slug preservation on rename, explicit slug change, slug/name conflicts, activation audit and deactivation with/without products in `backend/product-service/src/test/java/com/techstore/product/application/service/CategoryServiceTest.java`
- [ ] T018 [US3] Extend API integration tests for PUT slug updates, omitted-slug preservation, `409` no partial update, DELETE `204`/`409`, bodyless PATCH activation `200`, public visibility transition and ADMIN authorization in `backend/product-service/src/test/java/com/techstore/product/CatalogApiIntegrationTest.java`

### Implementation for User Story 3

- [ ] T019 [US3] Update category fields and slug with uniqueness checks; add explicit activate transition with audit timestamp/actor; preserve current rule that categories containing products cannot be deactivated in `backend/product-service/src/main/java/com/techstore/product/domain/Category.java` and `backend/product-service/src/main/java/com/techstore/product/application/service/CategoryService.java`
- [ ] T020 [US3] Extend `UpdateCategoryRequest` with optional slug and add ADMIN-only `PATCH /api/categories/{id}/activation` with no request body; retain current DELETE soft-deactivation behavior in `backend/product-service/src/main/java/com/techstore/product/adapter/http/ApiModels.java` and `backend/product-service/src/main/java/com/techstore/product/adapter/http/CategoryController.java`

**Checkpoint**: Update and activation never mutate on conflict; inactive rows keep their unique slug and product/category references.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Keep public contract/docs aligned and validate migration, security and package readiness.

- [ ] T021 [P] Update the existing Category OpenAPI paths, query parameters, slug request/response schemas, bodyless activation and 400/401/403/404/409 responses in `specs/001-foundation-product-catalog/contracts/product-service-api.openapi.yaml`
- [ ] T022 [P] Extend Springdoc annotations and integration assertions for `/api/categories` search/includeInactive and bodyless activation in `backend/product-service/src/main/java/com/techstore/product/adapter/http/CategoryController.java` and `backend/product-service/src/test/java/com/techstore/product/CatalogApiIntegrationTest.java`
- [ ] T023 Verify the common Product Service error envelope maps slug/name uniqueness to 409, invalid slug/query to 400, missing category to 404, and unauthorized management access to 401/403 in `backend/product-service/src/test/java/com/techstore/product/CatalogApiIntegrationTest.java`
- [ ] T024 Run the focused migration/API tests and then the complete reactor test command from the repository root: `mvn -f backend/pom.xml -pl product-service -am test`
- [ ] T025 Run the package gate after tests pass: `mvn -f backend/pom.xml -pl product-service -am package -DskipTests`
- [ ] T026 Validate the operational slug-collision procedure and API scenarios documented in `specs/009-category-management/quickstart.md`; record that V1 remains untouched and the explicit map is removed after successful migration

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No initialization is required; the Product Service, PostgreSQL database, Flyway, Springdoc, test dependencies and Gateway route already exist.
- **Foundational (Phase 2)**: T001-T007 define canonical slug semantics, persistence and migration; blocks all user stories.
- **US1 (Phase 3)**: Depends on Foundation; provides category create with slug.
- **US2 (Phase 4)**: Depends on Foundation and category slug persistence; can proceed independently of US1 using fixtures.
- **US3 (Phase 5)**: Depends on Foundation; can proceed independently of US2 using service/API fixtures, but integrated visibility verification uses US2.
- **Polish (Phase 6)**: Depends on all stories and migration work.

### User Story Dependencies

- **US1 (P1)**: Foundation only; independently testable with a Product Service database and ADMIN JWT.
- **US2 (P1)**: Foundation only; independently testable with seeded active/inactive categories and public/ADMIN principals.
- **US3 (P1)**: Foundation only; independently testable with a seeded category and a product-linked category fixture for the 409 rule.

### Parallel Opportunities

- T001 slug unit tests and T002 migration integration tests touch separate files and can be authored in parallel.
- After T003, T004 domain/entity mapping and T005 external placeholder config touch separate source/config files and can proceed in parallel; T006 waits for slug helper and placeholder key.
- T008 service tests and T009 API tests are separate files; do not edit the same `CatalogApiIntegrationTest.java` tasks concurrently across user stories.
- T012 service tests and API integration tests T013 use separate files; repository/service implementation follows their contract.
- T017 service tests and T018 integration tests are separate files; service implementation follows both.
- T021 OpenAPI YAML and the Java/controller test work in T022 are separate paths, but coordinate the shared API schema names.
- The single Product Service migration is a blocking foundation; do not parallelize V1 edits (V1 must remain unchanged) or category schema ownership.

## Parallel Example: Foundation Tests

```text
T001 canonical slug unit tests -> backend/product-service/src/test/java/com/techstore/product/domain/CategorySlugTest.java
T002 Flyway/Testcontainers migration scenarios -> backend/product-service/src/test/java/com/techstore/product/migration/CategorySlugMigrationTest.java
```

## Parallel Example: Create Story

```text
T008 CategoryService create/slug tests -> backend/product-service/src/test/java/com/techstore/product/application/service/CategoryServiceTest.java
T009 POST API validation and auth tests -> backend/product-service/src/test/java/com/techstore/product/CatalogApiIntegrationTest.java
```

## Implementation Strategy

### MVP First

1. Complete T001-T007, especially slug validation and collision-safe V2 migration; do not proceed to stories if migration rollback/map tests fail.
2. Complete US1 (T008-T011) so ADMIN can create categories with unique slugs using the existing route.
3. Complete US2 (T012-T016) to safely search/manage inactive categories without changing the public active-only catalog.
4. Complete US3 (T017-T020) for edit, deactivation and bodyless reactivation.
5. Complete OpenAPI, complete reactor tests, package and migration quickstart gates.

### Incremental Delivery

- Foundation establishes one slug rule and an additive, fail-closed migration.
- US1 creates categories with generated or explicit unique slug.
- US2 supports public search and ADMIN visibility of inactive rows.
- US3 provides safe update and lifecycle operations.
- Polish keeps the existing catalog API contract/documentation aligned and validates deployment procedure.
