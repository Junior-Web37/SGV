package com.sgv.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockMovementTest {

    @Test
    void amountAccessorsRoundTrip() {
        StockMovement movement = new StockMovement();
        movement.setQtyAmount(new BigDecimal("2.50"));
        movement.setStockBeforeAmount(new BigDecimal("10.00"));
        movement.setStockAfterAmount(new BigDecimal("12.50"));

        assertEquals(new BigDecimal("2.50"), movement.getQtyAmount());
        assertEquals(new BigDecimal("10.00"), movement.getStockBeforeAmount());
        assertEquals(new BigDecimal("12.50"), movement.getStockAfterAmount());
        assertEquals(2.50, movement.getQty(), 0.0001);
    }
}
