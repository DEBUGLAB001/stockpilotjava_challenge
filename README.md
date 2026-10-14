# StockPilot — Inventory Management System

StockPilot is an enterprise-grade retail inventory and warehouse fulfillment service built for inventory managers and order fulfillment teams. It allows users to track product stock across regional warehouses, reserve inventory during order placement, release stock on cancellations, inspect inventory movements, and search and filter orders in real time.

---

## 1. Application Overview

### Core Functionality
- **Product & Warehouse Inventory Management**: Maintain product catalogs and monitor stock distribution across regional storage facilities.
- **Order Placement & Stock Reservation**: Synchronously verify and reserve warehouse stock when customer orders are placed.
- **Stock Release on Cancellation**: Release reserved stock and restore available quantities back to warehouses upon order cancellation.
- **Inventory Movement Ledger**: Audit log tracking inbound restocks (`IN`), outbound dispatches (`OUT`), order reservations (`RESERVE`), and cancellation restorations (`CANCEL_RESTORE`).
- **Order Search & Status Filtering**: Query and filter orders by their fulfillment status (`CONFIRMED`, `SHIPPED`, `CANCELLED`).
- **Interactive Web Dashboard**: Modern, glassmorphic UI for live monitoring of warehouse inventory, fast stock adjustments, and order workflows.

### Technology Stack
- **Framework**: Java 17, Spring Boot 3 (Spring Web, Spring Data JPA, Bean Validation)
- **Database**: In-memory H2 Database (`jdbc:h2:mem:stockpilotdb`)
- **Build Tool**: Maven (with embedded Maven wrapper `mvnw`)
- **Frontend / Dashboard**: HTML5, Vanilla CSS (CSS custom properties, glassmorphism, responsive layout, dark theme), Vanilla JavaScript

---

## 2. Debugging Challenge

QA engineers and warehouse managers have flagged several critical issues in the StockPilot platform. Your goal is to investigate the codebase, reproduce each bug, and implement the necessary fixes so that all automated test suites pass.

### Reported Issues & Tasks:

#### Issue 1: Stock is Checked But Not Reserved Atomically
- **User Symptom**: Under high traffic, concurrent orders placed for the same product in a warehouse pass the available stock check simultaneously, causing available stock to oversell beyond total capacity.

#### Issue 2: Incorrect Stock Adjustment During Order Cancellation
- **User Symptom**: When an order is cancelled, customers and warehouse staff notice that available stock decreases further instead of returning to available inventory, corrupting stock totals.

#### Issue 3: Missing Query Condition in Stock Lookup
- **User Symptom**: When looking up inventory for a product in a specific warehouse, the system returns stock numbers from a completely different warehouse.

#### Issue 4: Failed Order Requests Return Misleading HTTP 200 Responses
- **User Symptom**: When an invalid order request is submitted (e.g. empty item list or missing details), client applications receive an `HTTP 200 OK` status with an error body instead of an appropriate client error code, breaking client-side error handling.

#### Issue 5: Missing Transaction Boundary Leaves Inventory Committed on Failure
- **User Symptom**: If an unexpected error occurs during order creation after inventory has been deducted, the order record is not saved, but the deducted warehouse stock remains committed in the database, resulting in phantom stock deductions.

---

## 3. Expected Behavior After Fixing Bugs

After resolving the issues:

- Concurrent orders cannot oversell warehouse stock; only orders with sufficient available stock succeed.
- Cancelling an order restores the available stock count and resets reserved stock properly.
- Warehouse inventory lookups return the exact inventory matching both the requested product and warehouse.
- Submitting invalid order requests returns `HTTP 400 Bad Request`.
- If an order fails mid-process, database changes roll back completely, leaving available and reserved stock intact.
