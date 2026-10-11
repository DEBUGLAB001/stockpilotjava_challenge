package com.stockpilot.repository;

import com.stockpilot.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND (i.warehouse.id != :warehouseId OR :warehouseId IS NULL)")
    List<Inventory> findByProductIdAndWarehouseIdBuggy(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);

    Optional<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByWarehouseId(Long warehouseId);
}
