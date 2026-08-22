package com.sgv.repository;

import com.sgv.entity.SaleItem;
import org.springframework.data.domain.Pageable;
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

    long countByProductId(Long productId);

    @Query("SELECT si.productCode, si.description, SUM(si.qty), SUM(si.lineTotal) " +
           "FROM SaleItem si JOIN si.sale s " +
           "WHERE s.createdAt >= :start AND s.createdAt <= :end " +
           "AND s.state <> 'ANULADA' AND s.state <> 'CANCELLED' " +
           "AND (:branchId IS NULL OR s.branch.id = :branchId) " +
           "GROUP BY si.productCode, si.description " +
           "ORDER BY SUM(si.qty) DESC")
    List<Object[]> findTopSellingProductsToday(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("branchId") Long branchId,
                                               Pageable pageable);
}

