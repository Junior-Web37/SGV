package com.sgv.repository;

import com.sgv.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("SELECT si FROM SaleItem si JOIN FETCH si.sale WHERE si.sale.createdAt >= :start AND si.sale.createdAt <= :end")
    List<SaleItem> findBySaleDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
