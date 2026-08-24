package com.sgv.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemainingDoubleToDecimalTest {

    private static void assertAmount(String expected, BigDecimal actual) {
        assertTrue(actual.compareTo(new BigDecimal(expected)) == 0,
                "esperado " + expected + " mas foi " + actual);
    }

    @Test
    void productQuantityAndFactorsPersistAsExactDecimals() {
        Product p = new Product();
        p.setBulkQuantity(0.1);
        p.setConversionFactor(1.5);
        p.setStockMin(2.5);
        p.setStockMax(100.75);
        p.setCommissionPercent(0.05);

        assertAmount("0.1000", p.getBulkQuantityAmount());
        assertAmount("1.5000", p.getConversionFactorAmount());
        assertAmount("2.5000", p.getStockMinAmount());
        assertAmount("100.7500", p.getStockMaxAmount());
        assertAmount("0.0500", p.getCommissionPercentAmount());
        assertEquals(0.1, p.getBulkQuantity(), 0.0);
        assertEquals(100.75, p.getStockMax(), 0.0);
    }

    @Test
    void productRatesRemainReadableAsDoubleAndExactAsDecimal() {
        Product p = new Product();
        p.setTaxRate(16.0);
        p.setIceRate(0.5);
        p.setDefaultTaxRate(16.0);
        p.setDefaultIceRate(0.0);

        assertEquals(16.0, p.getTaxRate(), 0.0);
        assertAmount("16.0000", p.getTaxRateAmount());
        assertEquals(0.5, p.getIceRate(), 0.0);
        assertAmount("0.5000", p.getIceRateAmount());
        assertEquals(16.0, p.getEffectiveTaxRate(), 0.0);
    }

    @Test
    void productionOrderQuantityIsExactDecimal() {
        ProductionOrder order = new ProductionOrder();
        order.setQuantity(12.3456);
        assertAmount("12.3456", order.getQuantityAmount());
        assertEquals(12.3456, order.getQuantity(), 0.0);
    }

    @Test
    void appConfigPercentagesAndExchangeRateAreExactDecimals() {
        AppConfig config = new AppConfig();
        config.setDefaultTaxRate(16.0);
        config.setDefaultIceRate(0.0);
        config.setStockMinAlertPercent(20.0);
        config.setMaxDiscountPercent(10.0);
        config.setExchangeRate(74.0);

        assertAmount("16.0000", config.getDefaultTaxRateAmount());
        assertAmount("0.0000", config.getDefaultIceRateAmount());
        assertAmount("20.0000", config.getStockMinAlertPercentAmount());
        assertAmount("10.0000", config.getMaxDiscountPercentAmount());
        assertAmount("74.0000", config.getExchangeRateAmount());
        assertEquals(74.0, config.getExchangeRate(), 0.0);
    }

    @Test
    void stockBoundaryAtHundredthsDoesNotIntroduceFloatArtifact() {
        Product p = new Product();
        p.setBulkQuantity(0.1);
        p.setStockMin(0.29);
        p.setStockMax(9.99);
        assertAmount("0.2900", p.getStockMinAmount());
        assertAmount("9.9900", p.getStockMaxAmount());
    }
}
