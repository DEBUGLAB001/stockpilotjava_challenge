package com.stockpilot.config;

import com.stockpilot.model.Inventory;
import com.stockpilot.model.Product;
import com.stockpilot.model.Warehouse;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.WarehouseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;

    public DataInitializer(ProductRepository productRepository,
                           WarehouseRepository warehouseRepository,
                           InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) return;

        // Products
        Product p1 = productRepository.save(new Product(null, "LAPTOP-X1", "Pro Gaming Laptop 16-inch", "Electronics", new BigDecimal("1299.99")));
        Product p2 = productRepository.save(new Product(null, "PHONE-Z10", "Ultra 5G Smartphone 256GB", "Mobile", new BigDecimal("799.50")));
        Product p3 = productRepository.save(new Product(null, "HEADSET-H5", "Noise Cancelling Headphones", "Audio", new BigDecimal("199.99")));
        Product p4 = productRepository.save(new Product(null, "KEYBOARD-M1", "Mechanical RGB Keyboard", "Accessories", new BigDecimal("89.00")));

        // Warehouses
        Warehouse w1 = warehouseRepository.save(new Warehouse(null, "WH-NYC-01", "New York Central Warehouse", "New York, NY"));
        Warehouse w2 = warehouseRepository.save(new Warehouse(null, "WH-CHI-02", "Chicago Midwest Depot", "Chicago, IL"));
        Warehouse w3 = warehouseRepository.save(new Warehouse(null, "WH-LAX-03", "Los Angeles Pacific Hub", "Los Angeles, CA"));

        // Inventories
        // Note: For Bug 3 demo/testing:
        // LAPTOP-X1: NYC has 10 units, CHI has 50 units.
        // A lookup for LAPTOP-X1 in CHI should return 50, but buggy query returns first match (NYC = 10)!
        inventoryRepository.save(new Inventory(null, p1, w1, 10, 0));
        inventoryRepository.save(new Inventory(null, p1, w2, 50, 0));
        inventoryRepository.save(new Inventory(null, p1, w3, 25, 0));

        inventoryRepository.save(new Inventory(null, p2, w1, 30, 0));
        inventoryRepository.save(new Inventory(null, p2, w2, 40, 0));

        inventoryRepository.save(new Inventory(null, p3, w1, 15, 0));
        inventoryRepository.save(new Inventory(null, p3, w3, 60, 0));

        inventoryRepository.save(new Inventory(null, p4, w2, 80, 0));
        inventoryRepository.save(new Inventory(null, p4, w3, 45, 0));
    }
}
