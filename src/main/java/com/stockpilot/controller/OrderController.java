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

    /*
     * INTENTIONAL BUG 4:
     * Incorrect HTTP status or response mapping:
     * When order creation fails with an exception (e.g., out of stock or bad request),
     * instead of returning HTTP 400 Bad Request or 409 Conflict, it returns HTTP 200 OK
     * with a misleading success response body or wraps failure in 200 OK!
     * 
     * CANDIDATE FIX:
     * If request fails or has error, return proper status code:
     * e.g. ResponseEntity.badRequest().body(...) or HttpStatus.BAD_REQUEST / HttpStatus.INTERNAL_SERVER_ERROR
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Order created = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception ex) {
            // BUG 4: Returning HTTP 200 OK on failure with error message, misleading clients/callers!
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("success", false);
            errorPayload.put("status", "error");
            errorPayload.put("message", ex.getMessage());
            return ResponseEntity.ok(errorPayload); // BUG! Should be ResponseEntity.badRequest().body(...)
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
