package com.sgv.repository;

import com.sgv.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
    List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(String status);
    Optional<PurchaseOrder> findByOrderNumber(String orderNumber);
}
