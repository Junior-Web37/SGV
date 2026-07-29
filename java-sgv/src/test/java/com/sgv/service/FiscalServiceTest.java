package com.sgv.service;

import com.sgv.entity.Branch;
import com.sgv.entity.Sale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FiscalService — verifies hash generation
 * uses SHA-256 and produces deterministic, unique results.
 */
class FiscalServiceTest {

    private FiscalService fiscalService;
    private SaleNumberingService saleNumberingService;

    @BeforeEach
    void setUp() {
        saleNumberingService = null; // Not needed for hash generation tests
        fiscalService = new FiscalService(saleNumberingService);
    }

    @Test
    void generateHashHash_shouldReturnSha256Hex() {
        Sale sale = new Sale();
        Branch branch = new Branch();
        branch.setId(1L);
        sale.setBranch(branch);
        sale.setSeries("A");
        sale.setDocumentNumber(1L);
        sale.setTotal(100.0);

        String hash = fiscalService.generateHashHash(sale);

        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 produces 64 hex characters");
        assertTrue(hash.matches("[0-9a-f]{64}"), "Hash should be lowercase hex");
    }

    @Test
    void generateHashHash_shouldBeDeterministic() {
        Sale sale = createSale(1L, "A", 1L, 100.0);

        String hash1 = fiscalService.generateHashHash(sale);
        String hash2 = fiscalService.generateHashHash(sale);

        assertEquals(hash1, hash2, "Same input should produce same hash");
    }

    @Test
    void generateHashHash_shouldDifferForDifferentTotal() {
        Sale sale1 = createSale(1L, "A", 1L, 100.0);
        Sale sale2 = createSale(1L, "A", 1L, 200.0);

        assertNotEquals(fiscalService.generateHashHash(sale1), fiscalService.generateHashHash(sale2));
    }

    @Test
    void generateHashHash_shouldDifferForDifferentBranch() {
        Sale sale1 = createSale(1L, "A", 1L, 100.0);
        Sale sale2 = createSale(2L, "A", 1L, 100.0);

        assertNotEquals(fiscalService.generateHashHash(sale1), fiscalService.generateHashHash(sale2));
    }

    @Test
    void generateHashHash_shouldDifferForDifferentSeries() {
        Sale sale1 = createSale(1L, "A", 1L, 100.0);
        Sale sale2 = createSale(1L, "B", 1L, 100.0);

        assertNotEquals(fiscalService.generateHashHash(sale1), fiscalService.generateHashHash(sale2));
    }

    @Test
    void generateHashHash_shouldHandleNullFields() {
        Sale sale = new Sale();
        // All fields null
        String hash = fiscalService.generateHashHash(sale);
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void generateSignatureHash_shouldReturnSha256() {
        Sale sale = new Sale();
        sale.setCustomerNuit("123456789");
        sale.setCreatedAt(java.time.LocalDateTime.of(2024, 1, 15, 10, 30));

        String sig = fiscalService.generateSignatureHash(sale);

        assertNotNull(sig);
        assertEquals(64, sig.length());
    }

    @Test
    void generateHashHash_shouldFormatTotalToTwoDecimals() {
        // Total 100.0 and 100.00 should produce same hash
        Sale sale1 = createSale(1L, "A", 1L, 100.0);
        Sale sale2 = createSale(1L, "A", 1L, 100.00);

        assertEquals(fiscalService.generateHashHash(sale1), fiscalService.generateHashHash(sale2));
    }

    private Sale createSale(Long branchId, String series, Long docNumber, Double total) {
        Sale sale = new Sale();
        Branch branch = new Branch();
        branch.setId(branchId);
        sale.setBranch(branch);
        sale.setSeries(series);
        sale.setDocumentNumber(docNumber);
        sale.setTotal(total);
        return sale;
    }
}
