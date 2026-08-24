package com.sgv.repository;

import com.sgv.entity.StockWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StockWarehouseRepository extends JpaRepository<StockWarehouse, Long> {
    Optional<StockWarehouse> findByWarehouseIdAndProductId(Long warehouseId, Long productId);
    List<StockWarehouse> findByWarehouseId(Long warehouseId);
}
