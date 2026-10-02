# Feature Specification: Cart Service Completion

**Feature Branch**: `004-cart`

**Created**: 2026-10-01

**Status**: Draft

**Input**: User description: "Cart Service — Regras de negócio, estoque e cálculo financeiro"

## User Scenarios & Testing

### User Story 1 - Add items within quantity and stock limits (Priority: P1)

An authenticated customer adds a product to their cart and receives confirmation only when the resulting quantity is within the configured limit and current available stock.

**Why this priority**: Prevents the cart from representing quantities the customer cannot request and establishes the rules required by all subsequent cart operations.

**Independent Test**: Add an active product with available stock and a valid quantity; verify the cart line and its price snapshot. Repeat with zero, excessive, and unavailable quantities and verify no invalid mutation occurs.

**Acceptance Scenarios**:

1. **Given** an active product with enough available stock, **When** the customer adds a positive quantity within the configured maximum, **Then** the cart contains the resulting quantity and the current unit price snapshot.
2. **Given** an inactive or missing product, **When** the customer adds it, **Then** the cart remains unchanged and the response identifies that the product cannot be added.
3. **Given** available stock lower than the requested resulting quantity, **When** the customer adds the item, **Then** the cart remains unchanged and the response reports the stock limitation.
4. **Given** a quantity that is zero or negative, **When** the customer submits it, **Then** the request is rejected and the cart remains unchanged.
5. **Given** a requested quantity above the configured maximum, **When** the customer submits it, **Then** the request is rejected and the cart remains unchanged.

### User Story 2 - Change an existing cart quantity (Priority: P1)

An authenticated customer replaces the quantity of an item already in their cart; the new quantity is validated against the current product availability and the configured maximum.

**Why this priority**: Quantity changes must obey the same rules as additions to prevent invalid cart state.

**Independent Test**: Change an owned line to a valid quantity and verify the quantity, price snapshot, subtotal, and cart total; attempt quantities outside stock or configured bounds and verify no mutation.

**Acceptance Scenarios**:

1. **Given** an existing item and enough current stock, **When** the customer sets a valid quantity, **Then** the line quantity is replaced and its unit price snapshot is refreshed from the current catalog price.
2. **Given** an existing item with insufficient current stock for the requested quantity, **When** the customer changes its quantity, **Then** the operation is rejected without changing the line.
3. **Given** a product that is no longer active, **When** the customer changes its quantity, **Then** the operation is rejected and the existing line remains available for review or removal.
4. **Given** an item not owned by the authenticated customer, **When** the customer tries to change it, **Then** the service reports it as not found.

### User Story 3 - Review stable item and cart prices (Priority: P1)

An authenticated customer views their cart and sees the unit prices captured when each item was last added or its quantity was changed, with a subtotal for each priced line and an overall total when every line has a known price.

**Why this priority**: The customer needs a consistent financial view that does not silently change when catalog prices change later.

**Independent Test**: Add items at known prices, change catalog prices, read the cart, and verify the stored snapshots and arithmetic remain correct.

**Acceptance Scenarios**:

1. **Given** an item already in the cart, **When** its catalog price changes without a cart mutation, **Then** the displayed snapshot, line subtotal, and cart total remain unchanged.
2. **Given** multiple cart lines with price snapshots, **When** the customer reads the cart, **Then** each line subtotal equals its snapshot unit price multiplied by quantity and the total equals the sum of line subtotals.
3. **Given** an empty cart, **When** the customer reads it, **Then** the response contains no lines and a total of zero.
4. **Given** a customer adds an item or changes its quantity after a catalog price change, **When** the operation succeeds, **Then** the item's snapshot uses the current catalog price for that operation.
5. **Given** a legacy line whose product is inactive or no longer exists, **When** the customer reads the cart, **Then** the line remains present, its price and subtotal are explicitly unavailable, and the cart total is explicitly unavailable.
6. **Given** a cart with an unknown-price legacy line, **When** the customer removes every unknown-price line, **Then** the total is calculated from the remaining known-price lines.

### User Story 4 - Preserve existing cart operations and ownership (Priority: P2)

An authenticated customer continues to read, add, update, remove, and clear only their own cart using the existing behavior, with the new financial fields included where applicable.

**Why this priority**: The feature evolves the current cart rather than replacing its working CRUD and security behavior.

**Independent Test**: Exercise all existing cart operations with authenticated customers and verify status/ownership behavior is preserved while response models include financial values.

**Acceptance Scenarios**:

1. **Given** two authenticated customers, **When** each reads or mutates a cart, **Then** neither can access or change the other's items.
2. **Given** an item is removed or a cart is cleared, **When** the operation succeeds, **Then** the existing success behavior remains and financial totals reflect the remaining cart state.
3. **Given** a request without a valid token, **When** it invokes a protected cart operation, **Then** the service rejects it as unauthenticated.

### Edge Cases

- Adding a product already present in the cart must consolidate the line rather than create a duplicate.
- The quantity after a repeated add is the current quantity plus the requested increment and must remain within the configured maximum and available stock. If the sum exceeds the configured maximum, the entire add operation is rejected and the cart remains unchanged.
- A product can become inactive or have its stock or price changed after it was added; reads retain the price snapshot, while add/update operations validate current catalog state.
- Existing lines without a price snapshot are initialized from the current catalog price only when the product is active; inactive or missing products remain without a known price.
- A line with an unknown price is retained until the owner removes it; while one or more such lines remain, the cart total is unavailable rather than estimated.
- A catalog timeout or unavailable response must not partially mutate the cart.
- Removing an inactive product and clearing an empty cart must remain possible.
- Financial calculations must use the catalog's currency and monetary precision consistently.
- Concurrent additions must not create duplicate lines or bypass the quantity and stock rules.

## Requirements

### Functional Requirements

- **FR-001**: The service MUST preserve the existing rule that item quantity is greater than zero.
- **FR-002**: The service MUST enforce a configurable maximum quantity per product line, with an initial default of 99.
- **FR-003**: The service MUST validate current product availability before adding a product or changing its quantity; it MUST NOT reserve or decrement inventory.
- **FR-004**: A rejected quantity or stock validation MUST leave the cart unchanged.
- **FR-005**: The service MUST capture a unit-price snapshot when a product is added and refresh that snapshot when the item's quantity is changed through a successful cart mutation.
- **FR-006**: A catalog price change without a subsequent cart mutation MUST NOT alter the item's saved unit-price snapshot.
- **FR-007**: The service MUST expose a line subtotal calculated from the saved unit-price snapshot and line quantity.
- **FR-008**: The service MUST expose a cart total calculated as the sum of its line subtotals; an empty cart MUST have a total of zero.
- **FR-009**: Existing cart response fields and successful CRUD semantics MUST remain available while responses are extended with price snapshot, subtotal, and total information.
- **FR-010**: The service MUST retain JWT authentication and per-user cart isolation for all existing cart operations.
- **FR-011**: The service MUST return a clear failure when the catalog cannot be reached and MUST NOT persist a mutation whose required catalog validation did not succeed.
- **FR-012**: The service MUST preserve add consolidation, item removal, and cart clearing behavior.
- **FR-013**: The service MUST NOT implement checkout, order creation, payment, permanent stock reservation/decrement, delivery, messaging, or changes to Product Service, User Service, API Gateway, or authentication.
- **FR-014**: When adding to an existing line would make its resulting quantity exceed the configured maximum, the service MUST reject the entire addition and leave the cart unchanged.
- **FR-015**: The service MUST add the price-snapshot schema through a new forward migration and MUST NOT modify the existing V1 migration.
- **FR-016**: During legacy snapshot initialization, the service MUST save the current catalog price for each existing line whose product is active; it MUST preserve lines whose product is inactive or missing and leave their price unknown.
- **FR-017**: For a line with unknown price, the service MUST clearly report that its unit price and subtotal are unavailable; while any such line exists, the cart total MUST be reported as unavailable.
- **FR-018**: The service MUST NOT automatically remove a line with unknown price. The owner MUST be able to remove it through the existing item-removal operation; after all unknown-price lines are removed, the total MUST be calculated from known-price lines.
- **FR-019**: Every newly added item and every successful quantity change MUST persist a known unit-price snapshot.
- **FR-020**: The service MUST NOT substitute zero, a default amount, or the current price of an inactive or missing product for an unknown legacy price.

### Key Entities

- **Cart owner**: The authenticated customer who owns a cart; cart data is private to that owner.
- **Cart line**: A product reference, positive quantity, and unit-price snapshot belonging to one cart owner. The snapshot may be unknown only for a legacy line whose product is inactive or missing.
- **Product availability**: Current active status and available quantity reported by the catalog when a cart mutation is attempted.
- **Line subtotal**: The cart line's saved unit-price snapshot multiplied by its quantity.
- **Cart total**: The sum of all line subtotals for one owner's cart.

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of add and quantity-change attempts with quantity below 1 or above the configured maximum are rejected without changing cart data.
- **SC-002**: 100% of add and quantity-change attempts that exceed current reported product availability are rejected without changing cart data.
- **SC-003**: After a catalog price change without a cart mutation, 100% of cart reads retain the previously saved unit price and corresponding subtotal.
- **SC-004**: For every returned cart whose lines all have known prices, each subtotal equals saved unit price multiplied by quantity, and the cart total equals the sum of all line subtotals.
- **SC-005**: All existing authenticated cart operations continue to enforce owner isolation and retain their established success behavior.
- **SC-006**: A customer can distinguish successful cart mutations from invalid quantity, unavailable product/stock, unauthenticated access, and catalog-unavailable outcomes through the service response.
- **SC-007**: Every active legacy product line receives its current catalog price as its initial persisted snapshot during the migration process; inactive or missing product lines remain present with unknown price and subtotal.
- **SC-008**: Whenever at least one line has unknown price, every cart read reports the line's price/subtotal and the cart total as unavailable; after removal of all such lines, the total is again the sum of known subtotals.

## Assumptions

- The Product Service remains the source of current product active status, available quantity, and price; this feature does not change that service.
- The configured maximum applies to the resulting quantity of one product line, regardless of how many add operations created that quantity.
- An add or quantity update uses the current catalog price as the new snapshot; a read-only operation never refreshes it.
- Stock is checked when adding or changing quantity, but is not reserved or decremented. Availability may change after the check.
- Legacy price initialization is an idempotent post-schema migration step owned by Cart Service: active products use the current catalog price once; inactive or missing products remain with unknown price until removed.
- A null legacy price is the only representation of an unknown amount; the service never uses zero or a default price for this state.
- V1 remains immutable; schema evolution is performed by a later migration.
- Existing product currency and monetary precision remain authoritative; no currency conversion is introduced.
- The existing authentication, per-user ownership, removal, clearing, and API behavior remain in force except for the additional financial response data and new validations.
