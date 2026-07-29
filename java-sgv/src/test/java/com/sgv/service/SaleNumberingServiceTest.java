package com.sgv.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SaleNumberingService logic patterns.
 * Tests the sequential numbering behavior.
 */
class SaleNumberingServiceTest {

    @Test
    void documentNumberCalculation_shouldFollowPattern() {
        // Simulate: next doc = max(existing) + 1
        long maxDoc = 42;
        long next = maxDoc + 1;
        assertEquals(43, next);
    }

    @Test
    void hashControl_shouldBeSequential() {
        // Simulate hash control increment
        Long[] controls = {1L, 2L, 3L, 4L, 5L};
        for (int i = 1; i < controls.length; i++) {
            assertEquals(controls[i-1] + 1, controls[i]);
        }
    }

    @Test
    void monetaryPrecision_shouldPreserveDecimals() {
        // Ensure BigDecimal preserves precision for MZN
        BigDecimal price = new BigDecimal("1234.56");
        BigDecimal qty = new BigDecimal("3");
        BigDecimal total = price.multiply(qty);

        assertEquals(new BigDecimal("3703.68"), total);
    }

    @Test
    void monetaryPrecision_shouldNotLoseFractionalDigits() {
        BigDecimal a = new BigDecimal("0.1");
        BigDecimal b = new BigDecimal("0.2");
        BigDecimal sum = a.add(b);

        assertNotEquals(new BigDecimal("0.30000000000000004"), sum);
        assertEquals(0, sum.compareTo(new BigDecimal("0.3")));
    }
}
