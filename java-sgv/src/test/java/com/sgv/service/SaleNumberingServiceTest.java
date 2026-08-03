package com.sgv.service;

import com.sgv.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleNumberingServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Test
    void nextDocumentNumber_noExisting_returnsOne() {
        SaleNumberingService service = new SaleNumberingService(saleRepository);
        when(saleRepository.findMaxDocumentNumberBySeriesAndDocumentTypeAndBranchIdAndYear(
                "FT", "FATURA", 1L, java.time.LocalDate.now().getYear())).thenReturn(null);

        assertEquals(1L, service.nextDocumentNumber(1L, "FT", "FATURA"));
    }

    @Test
    void nextDocumentNumber_existingIncrements() {
        SaleNumberingService service = new SaleNumberingService(saleRepository);
        when(saleRepository.findMaxDocumentNumberBySeriesAndDocumentTypeAndBranchIdAndYear(
                "FT", "FATURA", 1L, java.time.LocalDate.now().getYear())).thenReturn(42L);

        assertEquals(43L, service.nextDocumentNumber(1L, "FT", "FATURA"));
    }

    @Test
    void nextHashControl_noExisting_returnsOne() {
        SaleNumberingService service = new SaleNumberingService(saleRepository);
        when(saleRepository.findMaxHashControlByBranchId(2L)).thenReturn(null);

        assertEquals(1L, service.nextHashControl(2L));
    }

    @Test
    void nextHashControl_existingIncrements() {
        SaleNumberingService service = new SaleNumberingService(saleRepository);
        when(saleRepository.findMaxHashControlByBranchId(2L)).thenReturn(99L);

        assertEquals(100L, service.nextHashControl(2L));
    }
}
