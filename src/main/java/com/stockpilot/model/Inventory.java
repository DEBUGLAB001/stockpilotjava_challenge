package com.stockpilot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inventories", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "warehouse_id"})
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(nullable = false)
    private Integer availableStock;

    @Column(nullable = false)
    private Integer reservedStock;

    public Inventory() {
    }

    public Inventory(Long id, Product product, Warehouse warehouse, Integer availableStock, Integer reservedStock) {
        this.id = id;
        this.product = product;
        this.warehouse = warehouse;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(Integer reservedStock) {
        this.reservedStock = reservedStock;
    }
}
