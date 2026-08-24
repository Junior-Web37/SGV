package com.sgv.service;

import com.sgv.config.CacheConfig;
import com.sgv.entity.*;
import com.sgv.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard statistics service with Redis/Caffeine caching.
 * Expensive aggregation queries are cached to avoid hitting the DB on every poll.
 *
 * Cache eviction: any write to sales, stock, expenses, or cash sessions evicts
 * the relevant cache entry automatically.
 */
@Service
public class DashboardService {

    private final SaleRepository saleRepository;
    private final StockBranchService stockBranchService;
    private final ExpenseRepository expenseRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final SaleItemRepository saleItemRepository;

    public DashboardService(SaleRepository saleRepository,
                           StockBranchService stockBranchService,
                           ExpenseRepository expenseRepository,
                           ProductRepository productRepository,
                           CustomerRepository customerRepository,
                           BranchRepository branchRepository,
                           SaleItemRepository saleItemRepository) {
        this.saleRepository = saleRepository;
        this.stockBranchService = stockBranchService;
        this.expenseRepository = expenseRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.saleItemRepository = saleItemRepository;
    }

    // ---- Cached queries ----

    @Cacheable(value = CacheConfig.DASHBOARD_STATS_CACHE,
               key = "'stats:' + #branchId + ':' + #from + ':' + #to")
    public DashboardStats getStats(Long branchId, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        long saleCount = saleRepository.countByDateRangeAndState(fromDt, toDt, "EMITIDA");
        BigDecimal saleTotalBD = saleRepository.sumTotalByDateRangeAndState(fromDt, toDt, "EMITIDA");
        double saleTotal = saleTotalBD != null ? saleTotalBD.doubleValue() : 0.0;
        double expenseTotal = expenseRepository.sumAmountByDateRangeAndStates(
            fromDt.toLocalDate(), toDt.toLocalDate(),
            List.of("PAID", "PENDING"));
        long customerCount = customerRepository.count();
        long productCount = productRepository.count();

        return new DashboardStats(saleCount, saleTotal, expenseTotal, customerCount, productCount);
    }

    @Cacheable(value = CacheConfig.LOW_STOCK_CACHE,
               key = "'lowstock:' + #branchId")
    public List<StockBranch> getLowStock(Long branchId) {
        List<StockBranch> all = stockBranchService.findAll();
        return all.stream()
            .filter(sb -> sb.getProduct() == null || !Boolean.TRUE.equals(sb.getProduct().getService()))
            .filter(sb -> {
                BigDecimal current = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO;
                BigDecimal min = sb.getStockMinAmount() != null ? sb.getStockMinAmount() : BigDecimal.ZERO;
                return current.compareTo(min) <= 0;
            })
            .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.PRODUCTS_CACHE,
               key = "'all:' + #activeOnly")
    public List<Product> getAllProducts(boolean activeOnly) {
        if (activeOnly) {
            return productRepository.findAllActive();
        }
        return productRepository.findAll();
    }

    @Cacheable(value = CacheConfig.BRANCHES_CACHE, key = "'all'")
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Cacheable(value = CacheConfig.SALE_SUMMARY_CACHE,
               key = "'monthly:' + #branchId + ':' + #year")
    public Map<String, Double> getMonthlySaleSummary(Long branchId, int year) {
        Map<String, Double> monthly = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthly.put(String.format("%02d", m), 0.0);
        }
        List<Object[]> results = saleRepository.sumTotalByMonthAndYear(year);
        for (Object[] row : results) {
            int month = ((Number) row[0]).intValue();
            double total = ((Number) row[1]).doubleValue();
            monthly.put(String.format("%02d", month), total);
        }
        return monthly;
    }

    // ---- Cache eviction (call on write operations) ----

    @CacheEvict(value = CacheConfig.DASHBOARD_STATS_CACHE, allEntries = true)
    public void evictStatsCache() {}

    @CacheEvict(value = CacheConfig.LOW_STOCK_CACHE, allEntries = true)
    public void evictLowStockCache() {}

    @CacheEvict(value = CacheConfig.SALE_SUMMARY_CACHE, allEntries = true)
    public void evictSaleSummaryCache() {}

    // ---- DTO ----
    public record DashboardStats(
        long saleCount,
        double saleTotal,
        double expenseTotal,
        long customerCount,
        long productCount
    ) {}
}
