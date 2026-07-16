package com.sgv.service;

import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SaleNumberingService {

    private final SaleRepository saleRepository;

    public SaleNumberingService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public long nextDocumentNumber(Long branchId, String series, String documentType) {
        int year = LocalDate.now().getYear();
        Long lastNumber = saleRepository.findMaxDocumentNumberBySeriesAndDocumentTypeAndBranchIdAndYear(series, documentType, branchId, year);
        return (lastNumber == null ? 1L : lastNumber + 1L);
    }

    /**
     * Retorna o próximo número sequencial controlado por filial para o hashHash AT.
     * Este número é único por filial e incrementa com cada documento.
     */
    public long nextHashControl(Long branchId) {
        Long last = saleRepository.findMaxHashControlByBranchId(branchId);
        return (last == null ? 1L : last + 1L);
    }
}
