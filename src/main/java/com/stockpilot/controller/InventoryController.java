package com.stockpilot.controller;

import com.stockpilot.dto.StockAdjustmentRequest;
import com.stockpilot.model.Inventory;
import com.stockpilot.model.InventoryMovement;
import com.stockpilot.model.Product;
import com.stockpilot.model.Warehouse;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryController(InventoryService inventoryService,
                               ProductRepository productRepository,
                               WarehouseRepository warehouseRepository) {
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventories());
    }

    @GetMapping("/lookup")
    public ResponseEntity<Inventory> getInventory(@RequestParam Long productId, @RequestParam Long warehouseId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId, warehouseId));
    }

    @PostMapping("/adjust")
    public ResponseEntity<Inventory> adjustStock(@RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(request));
    }

    @GetMapping("/movements")
    public ResponseEntity<List<InventoryMovement>> getMovements() {
        return ResponseEntity.ok(inventoryService.getAllMovements());
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> getWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }
}
