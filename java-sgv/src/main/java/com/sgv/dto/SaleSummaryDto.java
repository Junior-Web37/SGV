package com.sgv.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleSummaryDto(
    Long id,
    String documentType,
    String series,
    Long documentNumber,
    Integer documentYear,
    String customerName,
    String customerNuit,
    String paymentMethod,
    BigDecimal subtotal,
    BigDecimal totalTax,
    BigDecimal total,
    BigDecimal paidAmount,
    BigDecimal changeAmount,
    String state,
    LocalDateTime createdAt,
    List<SaleItemDto> items
) {}
