# Data Model: Cart Service Completion

## Cart line (persisted)

| Field | Type/meaning | Validation/state |
|---|---|---|
| `owner_user_id` | UUID owner derived from JWT `sub` | Existing PK component; never accepted from request body. |
| `product_id` | UUID reference to Product Service | Existing PK component; no cross-service FK. |
| `quantity` | Positive integer | At least 1; at most configured max (default 99); also no greater than stock observed for add/set. |
| `unit_price` | Nullable `NUMERIC(12,2)` snapshot | Non-null for KNOWN; null for PENDING/UNKNOWN. Never a zero/default sentinel. |
| `price_snapshot_status` | `PENDING`, `KNOWN`, `UNKNOWN` | PENDING only while initialization is incomplete; KNOWN requires a price; UNKNOWN requires null price. |

The primary key remains `(owner_user_id, product_id)`. No timestamp or FK is introduced. Product Service remains the owner of product data and inventory.

## Snapshot state transitions

```text
Existing row after V2: PENDING, unit_price = null
  ├── catalog returns active product ──> KNOWN, unit_price = current catalog price
  ├── catalog returns 404/inactive ────> UNKNOWN, unit_price = null
  └── timeout/5xx ─────────────────────> remain PENDING; initializer/readiness fails

New add / successful quantity update ──> KNOWN, unit_price = price returned for that operation
KNOWN ── read/catalog price changes ────> KNOWN, same persisted unit_price
UNKNOWN ── ordinary read ──────────────> UNKNOWN, no live-price fallback
UNKNOWN ── successful add/update ──────> KNOWN, current operation price
```

PENDING must not be served as a completed cart state. Initialization updates are conditional on PENDING, making retries idempotent and avoiding overwriting a resolved price.

## Product catalog summary (external, not persisted)

The existing response already includes product ID, active status, current price, and `quantity`. Extend the Cart-side port model to include available stock. Product Service is not changed. Catalog data used for a cart mutation is a point-in-time observation; the cart does not reserve or decrement inventory.

## Cart line view

| Field | Meaning |
|---|---|
| `productId`, `quantity`, `available`, `product` | Existing response data and semantics, except product price is sourced from the saved snapshot when known. Product availability remains separate from price availability. |
| `unitPriceSnapshot` | Decimal snapshot, or null if price is unknown. |
| `priceAvailable` | True only for KNOWN. |
| `subtotal` | Snapshot × quantity when KNOWN; null when UNKNOWN. |

For UNKNOWN, do not remove the line. `available` describes product status; `priceAvailable` describes financial data status, so a reactivated product may be available while retaining unknown price until a successful mutation captures a new snapshot.

## Cart response

| Field | Meaning |
|---|---|
| `items` | Existing per-owner lines. |
| `maxItemQuantity` | Effective configured maximum used by the current Cart Service instance and its UI consumer. |
| `total` | Sum of all known line subtotals; null if any line price is UNKNOWN; zero for empty cart. |
| `totalAvailable` | False exactly when at least one line has unknown price. |

The response uses the existing routes and authorization. Remove and clear operations preserve current semantics. Removing all UNKNOWN lines makes `totalAvailable=true` and restores a calculable total.

## Database migration

V2 adds `unit_price` and `price_snapshot_status` with consistency checks. It does not modify V1, alter the existing composite key, or introduce a default price. Legacy rows begin PENDING; the Cart startup initializer resolves them through `ProductCatalogPort` before readiness.
