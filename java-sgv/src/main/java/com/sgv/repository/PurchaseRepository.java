package com.sgv.repository;

import com.sgv.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    @Query("SELECT p FROM Purchase p WHERE " +
           "LOWER(p.invoiceNumber) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Purchase> searchByInvoice(@Param("q") String query);

    List<Purchase> findAllByOrderByCreatedAtDesc();
}
