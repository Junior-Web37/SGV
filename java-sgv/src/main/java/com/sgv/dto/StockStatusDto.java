package com.sgv.dto;

import java.math.BigDecimal;

public record StockStatusDto(
    Long productId,
    String productCode,
    String productName,
    Long branchId,
    String branchName,
    BigDecimal currentStock,
    BigDecimal minStock,
    BigDecimal maxStock,
    boolean isLowStock
) {}
