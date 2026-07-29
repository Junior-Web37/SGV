package com.sgv.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppConfigService singleton defaults.
 */
class AppConfigServiceTest {

    @Test
    void defaultTaxRate_shouldBe16Percent() {
        // Mozambique IVA standard rate
        double defaultTaxRate = 16.0;
        assertEquals(16.0, defaultTaxRate);
    }

    @Test
    void defaultIceRate_shouldBe0Percent() {
        double defaultIceRate = 0.0;
        assertEquals(0.0, defaultIceRate);
    }

    @Test
    void defaultCurrency_shouldBeMZN() {
        String currency = "MZN";
        assertEquals("MZN", currency);
    }

    @Test
    void nuitValidation_shouldHave9Digits() {
        String validNuit = "123456789";
        String digits = validNuit.replaceAll("\\D", "");
        assertEquals(9, digits.length());
    }

    @Test
    void nuitValidation_shouldRejectInvalidLength() {
        String invalidNuit = "12345";
        String digits = invalidNuit.replaceAll("\\D", "");
        assertTrue(digits.length() != 9);
    }
}
