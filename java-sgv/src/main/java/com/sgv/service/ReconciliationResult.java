package com.sgv.service;

import com.sgv.entity.PaymentAllocation;

import java.math.BigDecimal;
import java.util.List;

public class ReconciliationResult {
    private final List<PaymentAllocation> allocations;
    private final BigDecimal remaining;

    public ReconciliationResult(List<PaymentAllocation> allocations, BigDecimal remaining) {
        this.allocations = allocations;
        this.remaining = remaining;
    }

    public List<PaymentAllocation> getAllocations() { return allocations; }
    public BigDecimal getRemaining() { return remaining; }
}
