package com.sgv.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentReceiptDto(
    Long paymentId,
    Long saleId,
    String invoiceNumber,
    String customerName,
    BigDecimal amount,
    String method,
    String reference,
    LocalDateTime paymentDate
) {}
