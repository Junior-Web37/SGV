package com.sgv.repository;

import com.sgv.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    Page<StockMovement> findByProductId(Long productId, Pageable pageable);

    List<StockMovement> findByTypeInOrderByCreatedAtDesc(java.util.Collection<String> types);

    List<StockMovement> findTop50ByTypeInOrderByCreatedAtDesc(java.util.Collection<String> types);

    List<StockMovement> findTop50ByWarehouseIdAndTypeInOrderByCreatedAtDesc(Long warehouseId, java.util.Collection<String> types);
}
