package com.sgv.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductMarginPrecisionTest {

    @Test
    void shouldRecalculateProfitMarginWithExactPercentage() {
        Product product = new Product();
        product.setPriceCostAmount(new BigDecimal("10.0000"));
        product.setPriceSaleAmount(new BigDecimal("12.5000"));

        product.recalculateProfitMargin();

        assertEquals(25.0, product.getProfitMargin(), 0.0001);
    }

    @Test
    void shouldRoundCalculatedPriceFromMarginToBusinessScale() {
        Product product = new Product();
        product.setPriceCostAmount(new BigDecimal("10.0000"));
        product.setProfitMargin(33.3333);

        product.calculatePriceFromMargin();

        BigDecimal expected = new BigDecimal("13.3333");
        assertEquals(expected, product.getPriceSaleAmount().setScale(4, RoundingMode.HALF_UP));
    }
}
