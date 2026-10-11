package com.stockpilot.controller;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.model.Order;
import com.stockpilot.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrder(orderNumber));
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Order created = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception ex) {
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("success", false);
            errorPayload.put("status", "error");
            errorPayload.put("message", ex.getMessage());
            return ResponseEntity.ok(errorPayload);
        }
    }

    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.cancelOrder(orderNumber));
    }

    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<Order> updateStatus(@PathVariable String orderNumber, @RequestBody Map<String, String> statusBody) {
        String newStatus = statusBody.get("status");
        return ResponseEntity.ok(orderService.updateOrderStatus(orderNumber, newStatus));
    }
}
