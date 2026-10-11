package com.stockpilot.service;

import com.stockpilot.dto.StockAdjustmentRequest;
import com.stockpilot.model.Inventory;
import com.stockpilot.model.InventoryMovement;
import com.stockpilot.model.Product;
import com.stockpilot.model.Warehouse;
import com.stockpilot.repository.InventoryMovementRepository;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryMovementRepository movementRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository,
                            WarehouseRepository warehouseRepository,
                            InventoryMovementRepository movementRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.movementRepository = movementRepository;
    }

    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    public Inventory getInventory(Long productId, Long warehouseId) {
        List<Inventory> list = inventoryRepository.findByProductIdAndWarehouseIdBuggy(productId, warehouseId);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Inventory not found for product ID " + productId + " and warehouse ID " + warehouseId);
        }
        return list.get(0);
    }

    @Transactional
    public Inventory adjustStock(StockAdjustmentRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> new Inventory(null, product, warehouse, 0, 0));

        int newStock = inventory.getAvailableStock() + request.getQuantity();
        if (newStock < 0) {
            throw new IllegalStateException("Available stock cannot be negative");
        }
        inventory.setAvailableStock(newStock);
        Inventory saved = inventoryRepository.save(inventory);

        movementRepository.save(new InventoryMovement(
                product,
                warehouse,
                request.getQuantity(),
                request.getQuantity() >= 0 ? "IN" : "OUT",
                null,
                request.getReason() != null ? request.getReason() : "Manual adjustment"
        ));

        return saved;
    }

    @Transactional
    public void releaseStockOnCancellation(Long productId, Long warehouseId, int quantity, String orderNumber) {
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product ID " + productId + " and warehouse ID " + warehouseId));

        inventory.setReservedStock(Math.max(0, inventory.getReservedStock() - quantity));
        inventory.setAvailableStock(Math.max(0, inventory.getAvailableStock() - quantity));

        inventoryRepository.save(inventory);

        movementRepository.save(new InventoryMovement(
                inventory.getProduct(),
                inventory.getWarehouse(),
                quantity,
                "CANCEL_RESTORE",
                orderNumber,
                "Stock released from cancelled order " + orderNumber
        ));
    }

    @Transactional
    public void recordMovement(Product product, Warehouse warehouse, int qty, String type, String orderNumber, String notes) {
        movementRepository.save(new InventoryMovement(product, warehouse, qty, type, orderNumber, notes));
    }

    public List<InventoryMovement> getAllMovements() {
        return movementRepository.findAllByOrderByTimestampDesc();
    }
}
