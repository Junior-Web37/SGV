package com.sgv.dto;

import java.math.BigDecimal;

public record ProductCatalogDto(
    Long id,
    String code,
    String name,
    String categoryName,
    BigDecimal priceCost,
    BigDecimal priceSale,
    BigDecimal priceSaleBulk,
    BigDecimal profitMargin,
    BigDecimal taxRate,
    String unit,
    boolean isActive,
    boolean isService
) {}
