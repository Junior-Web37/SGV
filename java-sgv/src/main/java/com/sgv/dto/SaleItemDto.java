package com.sgv.dto;

import java.math.BigDecimal;

public record SaleItemDto(
    Long productId,
    String productCode,
    String description,
    String unit,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineBase,
    BigDecimal taxRate,
    BigDecimal lineTax,
    BigDecimal lineTotal,
    String taxExemptionReason
) {}
