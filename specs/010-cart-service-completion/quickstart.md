# Quickstart: Cart Service Completion

## Prerequisites

- Java 21 and Maven 3.9+.
- Docker available for PostgreSQL Testcontainers and image build.
- PostgreSQL and Product Service reachable for local end-to-end/startup-backfill validation.
- Valid `TECHSTORE_JWT_SECRET` and a user token whose `sub` is a UUID.
- `MAX_CART_ITEM_QUANTITY` omitted (default 99) or set to a positive test value.

## Local validation

From repository root:

```powershell
mvn -f backend/pom.xml -pl cart-service -am test
mvn -f backend/pom.xml -pl cart-service -am package
npm --prefix frontend test -- --run
npm --prefix frontend run build
```

Build the existing Cart image from repository root:

```powershell
docker build -f backend/cart-service/Dockerfile -t techstore-cart:local .
```

## Database and legacy initialization scenarios

Use PostgreSQL Testcontainers with a database containing the existing V1 schema and representative legacy rows. Verify:

1. V2 is applied while V1 remains unchanged.
2. Active legacy products become KNOWN with their current catalog price.
3. Inactive/missing legacy products remain in `cart_items`, become UNKNOWN, and have `unit_price IS NULL`.
4. Catalog timeout/5xx does not convert a row to UNKNOWN and prevents readiness until retry.
5. Re-running initialization does not replace KNOWN/UNKNOWN decisions or delete lines.

## Cart behavior scenarios

Use the existing authenticated endpoints and an isolated owner:

- GET empty cart: `items=[]`, total zero and available.
- Add/update within max and stock: success status remains the current `201`/`200`; returned line includes persisted snapshot and subtotal.
- Verify `maxItemQuantity` in each cart response matches the effective server configuration and drives the frontend quantity control.
- Add duplicate whose resulting quantity is 100 with max 99: `409`, no quantity or price mutation.
- Add/update above current stock: `409`, no cart mutation; no stock is reserved or decremented.
- Change catalog price and GET without cart mutation: snapshot/subtotal/total stay unchanged.
- Read a cart containing a legacy UNKNOWN line: line remains, price/subtotal unavailable, cart total null/unavailable.
- Remove that line through the existing DELETE route: remaining known-line total is calculable again.
- Verify a second JWT owner cannot read or mutate the first owner's rows; unauthenticated behavior remains unchanged.
- Run concurrent adds for one owner/product at the max and stock boundaries; final persisted quantity does not exceed either bound and no duplicate row appears.

## Railway release validation

1. Confirm Cart's existing database and Product Service private URL, JWT secret, and `MAX_CART_ITEM_QUANTITY` configuration. Do not change Product, User, Gateway, routes, or JWT.
2. Ensure Product Service is healthy and available for lookup before deploying Cart.
3. Suspend Cart writes/old-version instances during the expand migration and initializer; deploy only the Cart Service.
4. Wait for `/actuator/health/readiness` to become healthy. If the catalog is transiently unavailable, do not release traffic; retry/restart after the dependency recovers.
5. Confirm `flyway_schema_history` records V2 and V1 checksum is unchanged; verify no PENDING rows remain after successful startup. UNKNOWN rows must have null `unit_price` and remain persisted.
6. Run authenticated GET/add/update/remove/clear checks, including an unknown-price legacy line, max 99, insufficient stock, stable price after catalog price change, and cross-owner isolation.
7. Confirm API responses contain correct per-line subtotals and total null/available semantics. Deploy the frontend only after the Cart contract is active; verify its cart UI and mobile layout.
8. Record Railway deployment IDs, migration state, test token owner (never token value), observed statuses, and results. This manual production check is not satisfied by local tests alone.

## Local implementation result

Validated on 2026-10-01 in this workspace:

- Backend command: `mvn -f backend/pom.xml -pl cart-service clean test -Dnet.bytebuddy.experimental=true`.
- Backend result: 76 tests, 0 failures, 0 errors, 12 skipped. The skipped tests are Testcontainers/PostgreSQL integration and concurrency cases because Docker is not installed/available in this environment.
- JWT/security regression: 3 `CartSecurityIntegrationTest` cases passed; cart HTTP/OpenAPI tests passed. Database-backed cross-owner isolation remains among the Docker-skipped tests.
- Frontend command: `npm --prefix frontend test`.
- Frontend result: 15 test files and 64 tests passed.
- Frontend build: `npm --prefix frontend run build` passed.
- Docker image build was not run because the Docker CLI/daemon is unavailable.
- Railway migration, startup initializer, readiness, and production smoke checks were not run; no Railway deployment context/CLI is available in this workspace.
- Local backend tests ran on Java 26 with Byte Buddy's experimental compatibility flag because the project target/container is Java 21. Production deployment must use the existing Java 21 Docker image.
