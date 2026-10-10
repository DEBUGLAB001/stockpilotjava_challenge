package com.stockpilot.test;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.model.Inventory;
import com.stockpilot.model.Order;
import com.stockpilot.model.Product;
import com.stockpilot.model.Warehouse;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.InventoryService;
import com.stockpilot.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false, addFilters = false)
public class StockPilotChallengeTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private MockMvc mockMvc;

    /**
     * Bug 1: Stock is checked but not reserved atomically
     * Expected behavior when fixed:
     * When multiple concurrent orders attempt to reserve stock beyond available capacity,
     * total reserved + available must remain consistent and exactly equal total stock without overselling.
     * Bug behavior: Concurrent requests both pass available check and reserve stock, overselling inventory.
     */
    @Test
    @DisplayName("Bug 1: Stock is checked but not reserved atomically")
    public void testBug1_OversellConcurrentOrders() throws Exception {
        // Setup isolated product and warehouse with 5 available stock
        Product p = productRepository.save(new Product(null, "RACE-" + System.nanoTime(), "Race Item", "Test", new BigDecimal("10.00")));
        Warehouse w = warehouseRepository.save(new Warehouse(null, "WH-RACE-" + System.nanoTime(), "Race WH", "Test"));
        Inventory inv = inventoryRepository.save(new Inventory(null, p, w, 5, 0));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    CreateOrderRequest req = new CreateOrderRequest("Customer-" + id,
                            List.of(new CreateOrderRequest.OrderItemDto(p.getId(), w.getId(), 4)));
                    orderService.createOrder(req);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        int successCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(10, TimeUnit.SECONDS)) {
                successCount++;
            }
        }
        executor.shutdown();

        Inventory updatedInv = inventoryRepository.findByProductIdAndWarehouseId(p.getId(), w.getId()).orElseThrow();

        // If oversold, both orders succeeded (4 + 4 = 8 > 5), or available stock went negative / reserved went above 5
        // When bug is present, successCount == 2. In fixed code, only 1 order should succeed (successCount == 1)
        assertEquals(1, successCount, "Only 1 order of 4 units should succeed when only 5 units are available. Concurrent orders oversold inventory!");
        assertTrue(updatedInv.getAvailableStock() >= 0, "Available stock should not be negative");
    }

    /**
     * Bug 2: Incorrect stock adjustment during cancellation
     * Expected behavior when fixed:
     * When an order is cancelled, available stock must increase back by the order quantity,
     * and reserved stock must decrease by the quantity.
     * Bug behavior: Buggy code decrements availableStock instead of restoring it!
     */
    @Test
    @DisplayName("Bug 2: Incorrect stock adjustment during cancellation")
    public void testBug2_CancelledOrdersDoNotRestoreAvailableStockCorrectly() {
        Product p = productRepository.save(new Product(null, "CANCEL-" + System.nanoTime(), "Cancel Item", "Test", new BigDecimal("25.00")));
        Warehouse w = warehouseRepository.save(new Warehouse(null, "WH-CNL-" + System.nanoTime(), "Cancel WH", "Test"));
        Inventory inv = inventoryRepository.save(new Inventory(null, p, w, 20, 0));

        // Place order of 5 units
        CreateOrderRequest req = new CreateOrderRequest("Alice",
                List.of(new CreateOrderRequest.OrderItemDto(p.getId(), w.getId(), 5)));
        Order order = orderService.createOrder(req);

        // Inventory should now have available 15, reserved 5
        Inventory postOrder = inventoryRepository.findByProductIdAndWarehouseId(p.getId(), w.getId()).orElseThrow();
        assertEquals(15, postOrder.getAvailableStock().intValue());
        assertEquals(5, postOrder.getReservedStock().intValue());

        // Cancel order
        orderService.cancelOrder(order.getOrderNumber());

        Inventory postCancel = inventoryRepository.findByProductIdAndWarehouseId(p.getId(), w.getId()).orElseThrow();

        // EXPECTED AFTER FIX: availableStock restored to 20, reservedStock to 0
        // BUGGY OBSERVED: availableStock was decremented to 10 instead of restored to 20!
        assertEquals(20, postCancel.getAvailableStock().intValue(), "Cancelled orders should restore available stock back to original count (20)");
        assertEquals(0, postCancel.getReservedStock().intValue(), "Reserved stock should be 0 after cancellation");
    }

    /**
     * Bug 3: Missing or incorrect query condition
     * Expected behavior when fixed:
     * findByProductIdAndWarehouseId must filter by BOTH productId and warehouseId.
     * Bug behavior: The query only filters by productId, returning the wrong warehouse's inventory!
     */
    @Test
    @DisplayName("Bug 3: Missing or incorrect query condition in stock lookup")
    public void testBug3_StockLookupReturnsWrongWarehouseInventory() {
        Product p = productRepository.save(new Product(null, "QUERY-" + System.nanoTime(), "Query Item", "Test", new BigDecimal("50.00")));
        Warehouse w1 = warehouseRepository.save(new Warehouse(null, "WH-1-" + System.nanoTime(), "WH 1", "City 1"));
        Warehouse w2 = warehouseRepository.save(new Warehouse(null, "WH-2-" + System.nanoTime(), "WH 2", "City 2"));

        // WH1 has 10 units, WH2 has 88 units
        inventoryRepository.save(new Inventory(null, p, w1, 10, 0));
        inventoryRepository.save(new Inventory(null, p, w2, 88, 0));

        // Lookup inventory specifically for warehouse w2 via lookup API / service
        Inventory foundW2 = inventoryService.getInventory(p.getId(), w2.getId());

        // BUGGY OBSERVED: returns w1 (availableStock == 10) because query lacks warehouseId!
        // EXPECTED: returns w2 (availableStock == 88)
        assertEquals(w2.getId(), foundW2.getWarehouse().getId(), "Query must return inventory belonging to warehouse " + w2.getId());
        assertEquals(88, foundW2.getAvailableStock().intValue(), "Stock lookup returned the wrong warehouse's inventory!");
    }

    /**
     * Bug 4: Incorrect HTTP status or response mapping
     * Expected behavior when fixed:
     * Failed requests (e.g. invalid order, missing items, or insufficient stock) must return HTTP 4xx (400 Bad Request).
     * Bug behavior: Controller catches exception and returns HTTP 200 OK with error payload!
     */
    @Test
    @DisplayName("Bug 4: Incorrect HTTP status or response mapping on order creation failure")
    public void testBug4_FailedRequestsReturnMisleadingResponses() throws Exception {
        // Send an invalid order request with no items
        String invalidJson = "{\"customerName\":\"Bob\",\"items\":[]}";

        // BUGGY OBSERVED: returns status 200 OK with misleading failure payload
        // EXPECTED: returns status 400 Bad Request
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * Bug 5: Transaction boundary is missing or incorrectly placed
     * Expected behavior when fixed:
     * If an order fails unexpectedly during processing, all inventory mutations must roll back.
     * Bug behavior: Lack of @Transactional on createOrder leaves reserved inventory changes committed when creation fails.
     */
    @Test
    @DisplayName("Bug 5: Transaction boundary is missing or incorrectly placed")
    public void testBug5_MissingTransactionBoundaryLeavesInventoryCommittedOnFailure() {
        Product p = productRepository.save(new Product(null, "TX-" + System.nanoTime(), "Tx Item", "Test", new BigDecimal("15.00")));
        Warehouse w = warehouseRepository.save(new Warehouse(null, "WH-TX-" + System.nanoTime(), "Tx WH", "City"));
        inventoryRepository.save(new Inventory(null, p, w, 30, 0));

        // Use trigger "FAIL_SIMULATION" which throws exception after deducting inventory
        CreateOrderRequest req = new CreateOrderRequest("FAIL_SIMULATION",
                List.of(new CreateOrderRequest.OrderItemDto(p.getId(), w.getId(), 10)));

        assertThrows(Exception.class, () -> {
            orderService.createOrder(req);
        });

        // Verify inventory after failure
        Inventory invAfterFail = inventoryRepository.findByProductIdAndWarehouseId(p.getId(), w.getId()).orElseThrow();

        // EXPECTED: Transaction rolled back -> availableStock remains 30, reservedStock remains 0
        // BUGGY OBSERVED: availableStock was permanently reduced to 20 and reservedStock set to 10 even though order failed!
        assertEquals(30, invAfterFail.getAvailableStock().intValue(),
                "Inventory changes must be rolled back when order creation fails! Observed uncommitted/dirty state committed.");
        assertEquals(0, invAfterFail.getReservedStock().intValue(),
                "Reserved stock changes must be rolled back when order creation fails!");
    }
}
