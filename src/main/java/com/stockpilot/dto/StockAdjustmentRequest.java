package com.stockpilot.dto;

public class StockAdjustmentRequest {
    private Long productId;
    private Long warehouseId;
    private Integer quantity;
    private String reason;

    public StockAdjustmentRequest() {}

    public StockAdjustmentRequest(Long productId, Long warehouseId, Integer quantity, String reason) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
