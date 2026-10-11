package com.stockpilot.service;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.model.*;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.OrderRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        WarehouseRepository warehouseRepository,
                        InventoryRepository inventoryRepository,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getOrdersByStatus(String status) {
        if (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("ALL")) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatusIgnoreCase(status.trim());
    }

    public Order getOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with number: " + orderNumber));
    }

    public Order createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        Order order = new Order();
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerName(request.getCustomerName() != null ? request.getCustomerName() : "Guest");
        order.setStatus("CONFIRMED");

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemDto itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));
            Warehouse warehouse = warehouseRepository.findById(itemDto.getWarehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + itemDto.getWarehouseId()));

            Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                    .orElseThrow(() -> new IllegalStateException("Inventory record missing for product " + product.getSku()));

            if (inventory.getAvailableStock() < itemDto.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product " + product.getName() + " in warehouse " + warehouse.getName());
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}

            // Deduct available, add to reserved
            inventory.setAvailableStock(inventory.getAvailableStock() - itemDto.getQuantity());
            inventory.setReservedStock(inventory.getReservedStock() + itemDto.getQuantity());
            inventoryRepository.save(inventory);

            inventoryService.recordMovement(
                    product, warehouse, itemDto.getQuantity(), "RESERVE", order.getOrderNumber(), "Stock reserved for order"
            );

            OrderItem orderItem = new OrderItem(order, product, warehouse, itemDto.getQuantity(), product.getUnitPrice());
            order.addItem(orderItem);

            total = total.add(product.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }

        order.setTotalAmount(total);

        if ("FAIL_SIMULATION".equalsIgnoreCase(request.getCustomerName())) {
            throw new RuntimeException("Simulated order processing failure after inventory reservation");
        }

        return orderRepository.save(order);
    }

    public Order cancelOrder(String orderNumber) {
        Order order = getOrder(orderNumber);
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Order is already cancelled");
        }

        // Release stock for each item
        for (OrderItem item : order.getItems()) {
            inventoryService.releaseStockOnCancellation(
                    item.getProduct().getId(),
                    item.getWarehouse().getId(),
                    item.getQuantity(),
                    order.getOrderNumber()
            );
        }

        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(String orderNumber, String newStatus) {
        Order order = getOrder(orderNumber);
        order.setStatus(newStatus.toUpperCase());
        return orderRepository.save(order);
    }
}
