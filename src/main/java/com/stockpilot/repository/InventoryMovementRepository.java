package com.stockpilot.repository;

import com.stockpilot.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findAllByOrderByTimestampDesc();
    List<InventoryMovement> findByProductId(Long productId);
    List<InventoryMovement> findByWarehouseId(Long warehouseId);
}
