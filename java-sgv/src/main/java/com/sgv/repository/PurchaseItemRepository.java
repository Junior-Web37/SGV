package com.sgv.repository;

import com.sgv.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
    List<PurchaseItem> findByPurchaseId(Long purchaseId);
}
