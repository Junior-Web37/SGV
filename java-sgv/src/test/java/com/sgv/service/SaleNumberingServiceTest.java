package com.sgv.service;

import com.sgv.entity.SeriesCounter;
import com.sgv.repository.SaleRepository;
import com.sgv.repository.SeriesCounterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleNumberingServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SeriesCounterRepository seriesCounterRepository;

    @Test
    void nextDocumentNumber_noExisting_returnsOne() {
        when(seriesCounterRepository.findForUpdate(anyLong(), anyString(), anyString())).thenReturn(Optional.empty());
        when(saleRepository.findMaxDocumentNumberBySeriesAndDocumentTypeAndBranchIdAndYear(
                anyString(), anyString(), anyLong(), anyInt())).thenReturn(null);

        SaleNumberingService service = new SaleNumberingService(saleRepository, seriesCounterRepository);
        assertEquals(1L, service.nextDocumentNumber(1L, "FT", "FATURA"));
    }

    @Test
    void nextDocumentNumber_existingIncrements() {
        when(seriesCounterRepository.findForUpdate(anyLong(), anyString(), anyString())).thenReturn(Optional.empty());
        when(saleRepository.findMaxDocumentNumberBySeriesAndDocumentTypeAndBranchIdAndYear(
                anyString(), anyString(), anyLong(), anyInt())).thenReturn(42L);

        SaleNumberingService service = new SaleNumberingService(saleRepository, seriesCounterRepository);
        assertEquals(43L, service.nextDocumentNumber(1L, "FT", "FATURA"));
    }

    @Test
    void nextDocumentNumber_atomicCounterIncrements() {
        SeriesCounter counter = new SeriesCounter(1L, "FT", "FATURA", 50L);
        when(seriesCounterRepository.findForUpdate(anyLong(), anyString(), anyString())).thenReturn(Optional.of(counter));

        SaleNumberingService service = new SaleNumberingService(saleRepository, seriesCounterRepository);
        assertEquals(51L, service.nextDocumentNumber(1L, "FT", "FATURA"));
    }

    @Test
    void nextHashControl_noExisting_returnsOne() {
        when(saleRepository.findMaxHashControlByBranchId(2L)).thenReturn(null);

        SaleNumberingService service = new SaleNumberingService(saleRepository, seriesCounterRepository);
        assertEquals(1L, service.nextHashControl(2L));
    }

    @Test
    void nextHashControl_existingIncrements() {
        when(saleRepository.findMaxHashControlByBranchId(2L)).thenReturn(99L);

        SaleNumberingService service = new SaleNumberingService(saleRepository, seriesCounterRepository);
        assertEquals(100L, service.nextHashControl(2L));
    }
}
