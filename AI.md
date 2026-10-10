# StockPilot — Inventory Management System (Java + Spring Boot Edition)

## Description
StockPilot is an enterprise-grade, offline-capable inventory and order fulfillment backend service paired with an interactive real-time management dashboard. It runs as a self-contained Java 17 Spring Boot service backed by an in-memory H2 database on port 8080 (preview compatible with container reverse proxies and port 3000 mapping). The platform manages multi-warehouse inventory levels, customer order reservation workflows, cancellation restorations, and audit movement tracking.

## Repository Structure
```
demo-_challenge/
├── pom.xml                                Maven build specification, dependencies, and Surefire test configuration
├── challenge.json                         Challenge runtime, port, start, and test runner configuration
├── README.md                              Candidate-facing application overview and bug reproduction guide
├── AI.md                                  Challenge architecture, bug locations, and benchmark specification
├── start.sh                               Container start script with automatic file watcher and JVM hot-reload
├── tests/
│   └── run_tests.sh                       Automated test runner recompiling code and executing test suite
└── src/
    ├── main/
    │   ├── java/com/stockpilot/
    │   │   ├── StockPilotApplication.java Main Spring Boot application entrypoint
    │   │   ├── config/
    │   │   │   └── DataInitializer.java   Deterministic in-memory catalog and warehouse stock fixture
    │   │   ├── controller/
    │   │   │   ├── InventoryController.java REST API controller for inventory lookup, adjustment, and movements
    │   │   │   └── OrderController.java   REST API controller for orders (Bug 4: HTTP status mapping)
    │   │   ├── dto/
    │   │   │   ├── CreateOrderRequest.java DTO for customer order placement
    │   │   │   └── StockAdjustmentRequest.java DTO for manual warehouse stock adjustments
    │   │   ├── model/
    │   │   │   ├── Product.java           JPA entity for catalog products
    │   │   │   ├── Warehouse.java         JPA entity for regional storage facilities
    │   │   │   ├── Inventory.java         JPA entity for warehouse stock balances
    │   │   │   ├── InventoryMovement.java JPA entity for audit movement logs
    │   │   │   ├── Order.java             JPA entity for customer orders
    │   │   │   └── OrderItem.java         JPA entity for order line items
    │   │   ├── repository/
    │   │   │   ├── ProductRepository.java Spring Data JPA repository for products
    │   │   │   ├── WarehouseRepository.java Spring Data JPA repository for warehouses
    │   │   │   ├── InventoryRepository.java Spring Data JPA repository (Bug 3: stock lookup query condition)
    │   │   │   ├── InventoryMovementRepository.java Repository for movement audit ledger
    │   │   │   └── OrderRepository.java   Spring Data JPA repository for orders
    │   │   └── service/
    │   │       ├── InventoryService.java  Service for stock adjustments (Bug 2: cancellation restoration)
    │   │       └── OrderService.java      Service for orders (Bug 1: atomic check, Bug 5: transaction boundary)
    │   └── resources/
    │       ├── application.properties     Server port, H2 datasource, and logging levels
    │       └── static/
    │           ├── index.html             Glassmorphic dashboard shell
    │           ├── style.css              Dark theme CSS variables, glassmorphism, and responsive tables
    │           └── app.js                 Client coordinator with live background state polling
    └── test/
        ├── java/com/stockpilot/
        │   ├── TestRunner.java            CLI test launcher invoking JUnit Platform
        │   └── test/
        │       ├── StockPilotChallengeTests.java Automated JUnit 5 challenge test suite
        │       └── StockPilotExecutionListener.java Custom listener emitting strict JSON telemetry
        └── resources/
            ├── logback-test.xml           Silences standard test logs so only JSON outputs to stdout
            └── META-INF/services/org.junit.platform.launcher.TestExecutionListener TestExecutionListener SPI
```

## Bugs and Bug Locations
These are the five behavioral bug surfaces covered by the challenge. The named locations identify the owning implementation areas for debugging and review.

### 1. Stock is checked but not reserved atomically
- **Bug location**: `src/main/java/com/stockpilot/service/OrderService.java`, `createOrder`
- **How to observe it**: Run concurrent order placement requests for the same product exceeding available capacity.
- **Failure**: The availability check and deduction execute non-atomically without synchronization or pessimistic locking. Multiple concurrent orders simultaneously pass `availableStock >= requested`, decrementing the inventory and overselling beyond stock capacity.
- **Expected**: Stock check and reservation occur atomically so that only requests with sufficient available stock succeed while subsequent concurrent requests fail cleanly.

### 2. Incorrect stock adjustment during cancellation
- **Bug location**: `src/main/java/com/stockpilot/service/InventoryService.java`, `releaseStockOnCancellation`
- **How to observe it**: Create an order, observe stock reservation, and then trigger `POST /api/orders/{orderNumber}/cancel`.
- **Failure**: The cancellation logic decrements `availableStock` instead of restoring it (`Math.max(0, inventory.getAvailableStock() - quantity)`), reducing available stock further on cancellation.
- **Expected**: Cancelling an order reduces `reservedStock` by the cancelled quantity and increments `availableStock` back by the same quantity.

### 3. Missing or incorrect query condition in stock lookup
- **Bug location**: `src/main/java/com/stockpilot/repository/InventoryRepository.java`, `findByProductIdAndWarehouseIdBuggy` (invoked via `InventoryService.getInventory`)
- **How to observe it**: Query `/api/inventory/lookup?productId={id}&warehouseId={id}` for a product stored in multiple warehouses.
- **Failure**: The query condition does not filter by warehouse ID (`(i.warehouse.id != :warehouseId OR :warehouseId IS NULL)`), returning the inventory record of a different warehouse.
- **Expected**: The query filters strictly by both `i.product.id = :productId AND i.warehouse.id = :warehouseId`, returning the exact warehouse record requested.

### 4. Incorrect HTTP status or response mapping on order creation failure
- **Bug location**: `src/main/java/com/stockpilot/controller/OrderController.java`, `createOrder`
- **How to observe it**: Submit a `POST /api/orders` request with an invalid payload (e.g. empty item list `{"customerName": "Bob", "items": []}`).
- **Failure**: The controller catches the exception and returns an `HTTP 200 OK` status with an error response body (`{"success": false, "status": "error"}`), misleading client applications and API proxies.
- **Expected**: Failed validation and bad requests return an appropriate client error status (`HTTP 400 Bad Request`).

### 5. Transaction boundary is missing or incorrectly placed
- **Bug location**: `src/main/java/com/stockpilot/service/OrderService.java`, `createOrder`
- **How to observe it**: Place an order that encounters a simulated downstream processing exception after inventory deduction.
- **Failure**: `@Transactional` is missing on `createOrder`. When order creation throws an exception after stock deduction, the partial database mutations remain committed and unrolled, resulting in lost stock.
- **Expected**: Adding `@Transactional` to `createOrder` ensures the entire operation is atomic; any unhandled runtime exception causes all preceding inventory modifications to roll back completely.

## Expected Behaviour After Fixing All Bugs
- Concurrent orders cannot oversell available warehouse inventory.
- Cancelling an order accurately restores available stock and resets reserved units.
- Stock lookups by product and warehouse return the inventory specific to the requested warehouse.
- Invalid order submissions return `HTTP 400 Bad Request`.
- If order processing fails mid-transaction, database modifications roll back without leaving orphan reservations.
- Running `./tests/run_tests.sh` (or `./mvnw test -B -q`) passes all 5 tests, prints strict JSON telemetry to stdout, and exits with code 0.
