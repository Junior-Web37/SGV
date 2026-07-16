package com.sgv.repository;

import com.sgv.entity.StockBranch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockBranchRepository extends JpaRepository<StockBranch, Long> {
	java.util.Optional<StockBranch> findByProductIdAndBranchId(Long productId, Long branchId);
	java.util.Optional<StockBranch> findByProductAndBranch(com.sgv.entity.Product product, com.sgv.entity.Branch branch);
	java.util.List<StockBranch> findByStockCurrentLessThanEqual(Double threshold);
	java.util.List<StockBranch> findByBranchId(Long branchId);
}
