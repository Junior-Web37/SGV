package com.sgv.repository;

import com.sgv.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {
    List<ProductBatch> findByProductIdOrderByExpiryDateAsc(Long productId);
    List<ProductBatch> findByStatusOrderByExpiryDateAsc(String status);

    @Query("SELECT pb FROM ProductBatch pb WHERE pb.expiryDate <= :targetDate AND pb.status = 'ACTIVE' ORDER BY pb.expiryDate ASC")
    List<ProductBatch> findNearExpiryBatches(@Param("targetDate") LocalDate targetDate);
}
