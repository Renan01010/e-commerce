# Tasks: Carrinho no Frontend

**Input**: Design documents from `specs/008-frontend-cart/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/cart-gateway-consumption.md`, `quickstart.md`

**Tests**: Automated tests are required by CS-008 and use the existing Vitest/React Testing Library setup. Write focused tests before the corresponding implementation tasks.

**Visual reference constraint**: The requester-provided screenshot is exclusively a visual guide for `/cart` composition, spacing, cards, borders, typography, hierarchy and overall feel. Do not implement screenshot-only features or fields. In particular, do not add totals, subtotals, shipping, checkout, reviews, recommendations, favorites, guarantees or data absent from `CartResponse`. Only display `productId`, `quantity`, `available` and, when non-null, `name`, `price`, `brand` and `imageUrl`. Price is informational, never aggregated. For `product: null`, show a neutral unavailable state and allow removal only. The header badge sums all line quantities, including unavailable lines.

## Phase 1: Setup

**Purpose**: Reuse the initialized React application and existing dependencies; no framework setup or package installation is needed.

No project initialization tasks are required. Keep the existing Vite scripts and dependency set in `frontend/package.json` unchanged.

## Phase 2: Foundational

**Purpose**: Define the existing API contract and shared in-memory state required by all user stories. Complete this phase before page/header work.

- [X] T001 [P] Add cart API contract tests for GET/POST/PUT/DELETE paths, payloads, 200/201 handling and bodyless 204 responses in `frontend/src/services/__tests__/apiClient.test.ts`
- [X] T002 Define `CartResponse`, `CartItem` and nullable `ProductSummary` types matching 004-cart in `frontend/src/types/cart.ts`
- [X] T003 Implement `cartApi` methods on the existing Axios client without changing catalog error behavior in `frontend/src/services/apiClient.ts`
- [X] T004 [P] Add store tests for per-session loading deduplication, response replacement, mutation failures, session reset and stale GET responses in `frontend/src/store/__tests__/cartStore.test.ts`
- [X] T005 Implement the in-memory Zustand cart store, confirmed mutation synchronization, operation feedback and derived quantity sum in `frontend/src/store/cartStore.ts`

**Checkpoint**: API methods match only the five existing endpoints, types allow `product: null`, and shared state cannot leak across sessions or be overwritten by stale reads.

## Phase 3: User Story 1 - Adicionar produto ao carrinho (Priority: P1)

**Goal**: Add a selected product quantity from the existing product detail page and update shared cart state from the API response.

**Independent Test**: With an authenticated session and an active catalog product, select a positive integer quantity, submit once, and verify the POST payload, success feedback and badge-ready cart response; failures leave confirmed cart state unchanged.

### Tests for User Story 1

- [X] T006 [P] [US1] Update product detail tests for quantity selection, valid POST payload without `userId`, loading/duplicate-click prevention and API error feedback in `frontend/src/pages/__tests__/ProductDetailPage.test.tsx`

### Implementation for User Story 1

- [X] T007 [US1] Enable quantity selection and add-to-cart feedback using the cart store, while leaving product activity validation to the Cart Service, in `frontend/src/pages/ProductDetailPage.tsx`
- [X] T008 [US1] Add accessible quantity-control and add-operation styles consistent with the existing product detail visual system in `frontend/src/styles.css`

**Checkpoint**: Product detail can add an active product with an integer quantity; an active product with catalog stock zero is not blocked by a new frontend inventory rule.

## Phase 4: User Story 2 - Consultar e manter o próprio carrinho (Priority: P1)

**Goal**: Render the cart contents and support quantity replacement, item removal and confirmed cart clearing using the existing contract.

**Independent Test**: Seed mocked GET data for available and unavailable lines; verify rendered content, positive PUT quantities, item DELETE after 204, clear confirmation/cancellation and empty state after successful clear.

### Tests for User Story 2

- [X] T009 [P] [US2] Add cart page tests for available/unavailable item rendering, product fields, positive quantity controls, removal, clear confirmation/cancel and empty state in `frontend/src/pages/__tests__/CartPage.test.tsx`
- [X] T010 [P] [US2] Add cart item component tests verifying null-safe unavailable presentation, remove-only actions and accessible quantity controls in `frontend/src/components/cart/__tests__/CartItem.test.tsx`

### Implementation for User Story 2

- [X] T011 [US2] Implement cart item rows using only `productId`, `quantity`, `available` and nullable product summary in `frontend/src/components/cart/CartItem.tsx`
- [X] T012 [US2] Implement `/cart` content, loading the shared state and dispatching PUT/item DELETE/cart DELETE through the store in `frontend/src/pages/CartPage.tsx`
- [X] T013 [US2] Add an accessible clear-cart confirmation whose cancel action makes no request and whose confirm action calls the existing clear operation in `frontend/src/components/cart/ClearCartConfirmation.tsx`
- [X] T014 [US2] Style the cart to follow the screenshot's visual language: broad item list on the left, compact count/actions panel on the right, restrained light cards, existing typography/borders/spacing, and vertically stacked mobile layout; omit screenshot-only recommendation cards and all unsupported data in `frontend/src/styles.css`

**Checkpoint**: `/cart` supports available lines and remove-only unavailable lines, with no financial aggregate and no unsupported product attributes.

## Phase 5: User Story 3 - Acompanhar e acessar o carrinho (Priority: P1)

**Goal**: Make the header cart control navigable and show the real total number of units from shared state.

**Independent Test**: With mocked cart lines of different quantities, including an unavailable line, load a store route and verify the badge equals the sum of quantities and opens `/cart` without a redundant GET after the state is loaded.

### Tests for User Story 3

- [X] T015 [P] [US3] Add application routing/header tests for `/cart` navigation and badge quantity sum including unavailable lines in `frontend/src/__tests__/App.test.tsx`

### Implementation for User Story 3

- [X] T016 [US3] Register `/cart` inside the existing store layout and replace the disabled header placeholder with a cart link and count badge derived from shared state in `frontend/src/App.tsx`
- [X] T017 [US3] Add responsive header badge/link states and preserve existing header behavior at narrow widths in `frontend/src/styles.css`

**Checkpoint**: The cart is reachable from the global header and its badge counts units, not rows, without becoming a hard-coded zero.

## Phase 6: User Story 4 - Entender estados vazios, carregamento e falha (Priority: P2)

**Goal**: Keep the cart understandable and recoverable without authentication or when requests fail.

**Independent Test**: Exercise `/cart` with no valid session, delayed GET, empty response, 401, 404, 503 and network failure; verify inline login guidance, retry/error states and preservation of the last confirmed data.

### Tests for User Story 4

- [X] T018 [P] [US4] Add tests for unauthenticated inline login guidance, loading, retryable GET errors, 401 session invalidation and mutation failure state in `frontend/src/pages/__tests__/CartPage.test.tsx`
- [X] T019 [P] [US4] Add tests for cart API error classification without changing catalog-facing error messages in `frontend/src/services/__tests__/apiClient.test.ts`

### Implementation for User Story 4

- [X] T020 [US4] Implement accessible empty, loading and retryable error views, inline `/login` link, 401 handling and preservation of confirmed state in `frontend/src/pages/CartPage.tsx`
- [X] T021 [US4] Add cart-specific API error messages for 401, 404, 503 and network failures without changing catalog messages in `frontend/src/services/apiClient.ts`
- [X] T022 [US4] Verify cart state is cleared on session expiration, invalidation or replacement while keeping `/cart` open for login guidance in `frontend/src/store/cartStore.ts`

**Checkpoint**: Auth and request failures never look like an empty successful cart, and old-session rows are not displayed.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validate the complete frontend journey, accessibility, responsive layout and strict contract boundaries.

- [X] T023 [P] Add responsive/accessibility assertions for keyboard access, focus, labels, image alternatives and no horizontal overflow in `frontend/src/pages/__tests__/CartPage.test.tsx`
- [X] T024 Run the frontend test suite and production build from `frontend/` using `npm test` and `npm run build`
- [ ] T025 Validate the manual flows and expected outcomes documented in `specs/008-frontend-cart/quickstart.md`
- [X] T026 Review `frontend/src/pages/CartPage.tsx`, `frontend/src/components/cart/CartItem.tsx` and `frontend/src/styles.css` against the screenshot as visual-only reference; confirm no totals, recommendations, reviews, shipping, checkout or unsupported fields were added

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No initialization required; existing React/Vite dependencies and test scripts are reused.
- **Foundational (Phase 2)**: T001-T005 establish the shared API, types and cart store; blocks all user stories.
- **US1 (Phase 3)**: Depends on Foundation; enables add-to-cart from product detail.
- **US2 (Phase 4)**: Depends on Foundation; renders and maintains cart. It can be developed independently of US1 with mocked initial cart data, while end-to-end add-to-cart observation also uses US1.
- **US3 (Phase 5)**: Depends on Foundation and App/layout integration; may proceed alongside US1/US2 once shared store contract is stable, though its tasks are ordered after page work for straightforward integration.
- **US4 (Phase 6)**: Depends on cart API/store and CartPage; completes login guidance and failure handling.
- **Polish (Phase 7)**: Depends on all desired stories.

### User Story Dependencies

- **US1 (P1)**: Foundation only; independently testable from Product Detail with mocked API.
- **US2 (P1)**: Foundation only; independently testable with mocked GET state; shares layout/styles with US3 during integration.
- **US3 (P1)**: Foundation only; badge and route can be tested with mocked cart state.
- **US4 (P2)**: Depends on the cart page and store behaviors from US2/US3; error/login views are independently testable within that page.

### Parallel Opportunities

- T001 and T004 can be authored in parallel because they target separate test files; both precede their respective API/store implementations.
- T006 can be prepared while foundational API/store work is underway; it depends on their agreed public methods to run.
- T009 and T010 are separate test files and can be authored in parallel.
- T015 can be authored independently of CartPage tests; App integration still consumes the shared store contract.
- T018 and T019 are separate test files and can be authored in parallel after initial page/API surfaces exist.
- Visual CSS should be coordinated with CartPage structure; do not parallelize multiple tasks that edit `frontend/src/styles.css` concurrently.

## Parallel Example: Foundation Tests

```text
T001 cart API contract tests -> frontend/src/services/__tests__/apiClient.test.ts
T004 cart store behavior tests -> frontend/src/store/__tests__/cartStore.test.ts
```

## Parallel Example: Cart Presentation Tests

```text
T009 cart page journey tests -> frontend/src/pages/__tests__/CartPage.test.tsx
T010 item accessibility/null-state tests -> frontend/src/components/__tests__/CartItem.test.tsx
```

## Implementation Strategy

### MVP First

1. Complete T001-T005 for types, API and shared state.
2. Complete US1 (T006-T008) to add products from details and update the shared cart response.
3. Complete US2 (T009-T014) to browse and maintain lines in `/cart` using the visual reference within the contract boundary.
4. Complete US3 (T015-T017) so the cart and real badge are available across store routes.
5. Complete US4 and polish; validate error/auth states, responsive behavior and contract exclusions.

### Incremental Delivery

- Foundation provides typed, tested Cart Service access and session-bound shared state.
- US1 connects product detail to add-to-cart.
- US2 delivers the complete cart view and item operations.
- US3 makes the cart discoverable in the header and keeps the quantity badge synchronized.
- US4 hardens unauthenticated, loading, empty and failure experiences.
- Final validation confirms desktop/tablet/mobile layout and that screenshot-only functionality/data were not added.
