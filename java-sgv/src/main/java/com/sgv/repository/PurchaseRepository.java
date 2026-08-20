package com.sgv.repository;

import com.sgv.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByBranchIdOrderByCreatedAtDesc(Long branchId);
    List<Purchase> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

    @Query("SELECT p FROM Purchase p WHERE p.supplier.id = :supplierId AND p.state <> 'CANCELLED' AND (COALESCE(p.total, 0) - COALESCE(p.paidAmount, 0)) > 0.01 ORDER BY p.createdAt DESC")
    List<Purchase> findPendingBySupplierId(@Param("supplierId") Long supplierId);

    Optional<Purchase> findByInvoiceNumberIgnoreCase(String invoiceNumber);

    @Query("SELECT p FROM Purchase p WHERE " +
           "LOWER(p.invoiceNumber) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Purchase> searchByInvoice(@Param("q") String query);

    List<Purchase> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Purchase p LEFT JOIN FETCH p.items WHERE p.id = :id")
    Purchase findByIdWithItems(@Param("id") Long id);

    /**
     * Saldo em aberto por fornecedor: total das compras não canceladas menos o valor já pago.
     * Retorna linhas [supplierId, saldo].
     */
    @Query("SELECT p.supplier.id, COALESCE(SUM(p.total), 0) - COALESCE(SUM(p.paidAmount), 0) " +
           "FROM Purchase p WHERE p.state <> 'CANCELLED' AND p.supplier IS NOT NULL " +
           "GROUP BY p.supplier.id")
    List<Object[]> findOutstandingBalanceBySupplier();
}
