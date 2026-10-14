# StockPilot

## Description

StockPilot is an offline-capable inventory and order fulfillment management platform built with Java 17, Spring Boot, and H2 database. It features multi-warehouse stock tracking, atomic order reservations, cancellation restorations, and audit movement logging.

## Repository Structure

```text
demo-_challenge/
├── challenge.json                         Runtime configuration, preview port, and test command
├── pom.xml                                Maven build specification and Spring Boot dependencies
├── README.md                              Application overview and challenge instructions
├── AI.md                                  Architecture documentation, repository structure, and bug analysis
├── start.sh                               Startup script launching the Spring Boot application server
├── tests/
│   └── run_tests.sh                       Automated test runner executing JUnit 5 test suite
└── src/
    ├── main/
    │   ├── java/com/stockpilot/
    │   │   ├── StockPilotApplication.java Spring Boot application entry point
    │   │   ├── config/
    │   │   │   └── DataInitializer.java   Database fixture seeding initial products, warehouses, and stock
    │   │   ├── controller/
    │   │   │   ├── InventoryController.java REST API controller for inventory lookup and adjustments
    │   │   │   └── OrderController.java   REST API controller for order creation and cancellation
    │   │   ├── dto/
    │   │   │   ├── CreateOrderRequest.java Request payload DTO for order placement
    │   │   │   └── StockAdjustmentRequest.java Request payload DTO for manual stock adjustment
    │   │   ├── model/
    │   │   │   ├── Inventory.java         Entity tracking available and reserved stock per warehouse
    │   │   │   ├── InventoryMovement.java Entity logging stock adjustments and audit history
    │   │   │   ├── Order.java             Entity representing customer orders and lifecycle status
    │   │   │   ├── OrderItem.java         Entity representing individual order line items
    │   │   │   ├── Product.java           Entity representing catalog products
    │   │   │   └── Warehouse.java         Entity representing regional storage facilities
    │   │   ├── repository/
    │   │   │   ├── InventoryMovementRepository.java Spring Data JPA repository for stock movement logs
    │   │   │   ├── InventoryRepository.java Spring Data JPA repository for warehouse inventory records
    │   │   │   ├── OrderRepository.java   Spring Data JPA repository for customer orders
    │   │   │   ├── ProductRepository.java Spring Data JPA repository for catalog products
    │   │   │   └── WarehouseRepository.java Spring Data JPA repository for warehouses
    │   │   └── service/
    │   │       ├── InventoryService.java  Service handling inventory queries and cancellation returns
    │   │       └── OrderService.java      Service handling order placement, transactions, and reservation
    │   └── resources/
    │       ├── application.properties     Spring Boot configuration and database settings
    │       └── static/
    │           ├── app.js                 Frontend state polling and user interface logic
    │           ├── index.html             Dashboard view for inventory and order management
    │           └── style.css              Dark-themed styles and dashboard UI layout
    └── test/
        ├── java/com/stockpilot/
        │   ├── TestRunner.java            CLI test runner emitting JSON test results
        │   └── test/
        │       ├── StockPilotChallengeTests.java JUnit 5 integration tests covering challenge defects
        │       └── StockPilotExecutionListener.java Custom test execution listener generating JSON output
        └── resources/
            ├── logback-test.xml           Logback test logging configuration
            └── META-INF/services/org.junit.platform.launcher.TestExecutionListener TestExecutionListener SPI registration
```

## Bugs and Bug Locations

### 1. Stock is checked but not reserved atomically

- **Bug location:** `src/main/java/com/stockpilot/service/OrderService.java`
- **How to observe it:** Send concurrent order placement requests for the same product and warehouse where requested quantity exceeds available stock.
- **Failure:** The availability check and stock reservation execute non-atomically without synchronization or pessimistic locking, allowing concurrent orders to both pass the check and oversell inventory.
- **Expected:** Stock validation and deduction occur atomically so that only requests with sufficient stock succeed, preventing overselling.

### 2. Incorrect stock adjustment during cancellation

- **Bug location:** `src/main/java/com/stockpilot/service/InventoryService.java`
- **How to observe it:** Place an order to reserve inventory and then cancel it via `POST /api/orders/{orderNumber}/cancel`.
- **Failure:** Cancellation logic subtracts the quantity from available stock instead of adding it back, reducing available stock further upon cancellation.
- **Expected:** Cancelling an order decrements reserved stock and increments available stock by the cancelled quantity.

### 3. Missing or incorrect query condition in stock lookup

- **Bug location:** `src/main/java/com/stockpilot/repository/InventoryRepository.java`
- **How to observe it:** Call `/api/inventory/lookup` with a product ID and warehouse ID for a product stored in multiple warehouses.
- **Failure:** The repository query filters only by product ID and omits the warehouse filter, returning an arbitrary inventory record from a different warehouse.
- **Expected:** The query filters strictly by both product ID and warehouse ID, returning the inventory record for the requested warehouse.

### 4. Incorrect HTTP status or response mapping on order creation failure

- **Bug location:** `src/main/java/com/stockpilot/controller/OrderController.java`
- **How to observe it:** Send a `POST /api/orders` request with an invalid payload such as an empty item list.
- **Failure:** The controller catches the validation exception and returns `HTTP 200 OK` with an error message in the response body.
- **Expected:** Invalid requests and order processing errors return `HTTP 400 Bad Request`.

### 5. Transaction boundary is missing or incorrectly placed

- **Bug location:** `src/main/java/com/stockpilot/service/OrderService.java`
- **How to observe it:** Submit an order request that triggers a downstream processing exception after inventory deduction.
- **Failure:** The order creation method lacks `@Transactional`, causing inventory deductions to remain committed even when the overall order placement fails.
- **Expected:** The order placement method runs within a transaction so any downstream failure rolls back all inventory modifications.

## Expected Behaviour After Fixing All Bugs

- Concurrent order requests cannot oversell warehouse stock beyond available inventory.
- Cancelling an order restores available stock and clears reserved stock.
- Inventory lookups by product and warehouse return the exact warehouse stock record requested.
- Invalid order requests return `HTTP 400 Bad Request` instead of `HTTP 200 OK`.
- Unhandled order processing exceptions roll back all database modifications atomically.
- Running `./tests/run_tests.sh` executes all 5 integration tests successfully with exit code 0.
