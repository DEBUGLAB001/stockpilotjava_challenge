package com.stockpilot.dto;

import java.util.List;

public class CreateOrderRequest {
    private String customerName;
    private List<OrderItemDto> items;

    public static class OrderItemDto {
        private Long productId;
        private Long warehouseId;
        private Integer quantity;

        public OrderItemDto() {}

        public OrderItemDto(Long productId, Long warehouseId, Integer quantity) {
            this.productId = productId;
            this.warehouseId = warehouseId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public CreateOrderRequest() {}

    public CreateOrderRequest(String customerName, List<OrderItemDto> items) {
        this.customerName = customerName;
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
