package com.sgv.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransferItemTest {

    @Test
    void shouldRoundTripBigDecimalAmountValues() {
        TransferItem item = new TransferItem();
        item.setQuantityAmount(new BigDecimal("3.50"));
        item.setQuantityReceivedAmount(new BigDecimal("1.25"));

        assertEquals(new BigDecimal("3.50"), item.getQuantityAmount());
        assertEquals(new BigDecimal("1.25"), item.getQuantityReceivedAmount());
        assertEquals(3.5d, item.getQuantity());
        assertEquals(1.25d, item.getQuantityReceived());
    }
}
