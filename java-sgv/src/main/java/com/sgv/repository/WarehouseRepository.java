package com.sgv.repository;

import com.sgv.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);
    List<Warehouse> findAllByOrderByNameAsc();

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Warehouse w WHERE w.isActive = true ORDER BY w.name ASC")
    List<Warehouse> findByIsActiveTrueOrderByNameAsc();
}
