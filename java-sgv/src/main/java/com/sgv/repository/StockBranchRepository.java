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

	@org.springframework.data.jpa.repository.Query("SELECT COUNT(sb) FROM StockBranch sb WHERE (:branchId IS NULL OR sb.branch.id = :branchId) AND sb.stockCurrent <= sb.stockMin AND sb.stockCurrent > 0.0")
	long countLowStockByBranch(@org.springframework.data.repository.query.Param("branchId") Long branchId);

	@org.springframework.data.jpa.repository.Query("SELECT COUNT(sb) FROM StockBranch sb WHERE (:branchId IS NULL OR sb.branch.id = :branchId) AND sb.stockCurrent <= 0.0")
	long countZeroStockByBranch(@org.springframework.data.repository.query.Param("branchId") Long branchId);
}
