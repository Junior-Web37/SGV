package com.sgv.repository;

import com.sgv.entity.StockBranch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;

public interface StockBranchRepository extends JpaRepository<StockBranch, Long> {
	java.util.Optional<StockBranch> findByProductIdAndBranchId(Long productId, Long branchId);
	java.util.Optional<StockBranch> findByProductAndBranch(com.sgv.entity.Product product, com.sgv.entity.Branch branch);
	java.util.List<StockBranch> findByStockCurrentLessThanEqual(BigDecimal threshold);
	java.util.List<StockBranch> findByBranchId(Long branchId);

	@org.springframework.data.jpa.repository.Query("SELECT sb FROM StockBranch sb WHERE sb.branch.id = :branchId AND sb.product.id IN :productIds")
	java.util.List<StockBranch> findByBranchIdAndProductIdIn(@org.springframework.data.repository.query.Param("branchId") Long branchId, @org.springframework.data.repository.query.Param("productIds") java.util.List<Long> productIds);

	@org.springframework.data.jpa.repository.Query("SELECT COUNT(sb) FROM StockBranch sb JOIN sb.product p WHERE (:branchId IS NULL OR sb.branch.id = :branchId) AND (p.isService = false OR p.isService IS NULL) AND sb.stockCurrent > 0.0 AND sb.stockCurrent <= COALESCE(sb.stockMin, 0.0)")
	long countLowStockByBranch(@org.springframework.data.repository.query.Param("branchId") Long branchId);

	@org.springframework.data.jpa.repository.Query("SELECT COUNT(sb) FROM StockBranch sb JOIN sb.product p WHERE (:branchId IS NULL OR sb.branch.id = :branchId) AND (p.isService = false OR p.isService IS NULL) AND (sb.stockCurrent IS NULL OR sb.stockCurrent <= 0.0)")
	long countZeroStockByBranch(@org.springframework.data.repository.query.Param("branchId") Long branchId);

	@org.springframework.data.jpa.repository.Query("SELECT sb FROM StockBranch sb JOIN FETCH sb.product p LEFT JOIN FETCH sb.branch b WHERE (p.isService = false OR p.isService IS NULL) AND (sb.stockCurrent IS NULL OR sb.stockCurrent <= COALESCE(sb.stockMin, 0.0)) AND (:branchId IS NULL OR b.id = :branchId) ORDER BY sb.stockCurrent ASC")
	java.util.List<StockBranch> findStockAlerts(@org.springframework.data.repository.query.Param("branchId") Long branchId);

	@org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.isActive = true AND (p.isService = false OR p.isService IS NULL) AND NOT EXISTS (SELECT 1 FROM StockBranch sb WHERE sb.product = p AND (:branchId IS NULL OR sb.branch.id = :branchId))")
	java.util.List<com.sgv.entity.Product> findPhysicalProductsWithoutStockRow(@org.springframework.data.repository.query.Param("branchId") Long branchId);
}
