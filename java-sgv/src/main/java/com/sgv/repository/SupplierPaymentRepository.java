package com.sgv.repository;

import com.sgv.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    List<SupplierPayment> findByPurchaseId(Long purchaseId);
    List<SupplierPayment> findBySupplierId(Long supplierId);
    List<SupplierPayment> findAllByOrderByCreatedAtDesc();
    List<SupplierPayment> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
