package com.sgv.service;

import com.sgv.entity.SeriesCounter;
import com.sgv.repository.SaleRepository;
import com.sgv.repository.SeriesCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class SaleNumberingService {

    private final SaleRepository saleRepository;
    private final SeriesCounterRepository seriesCounterRepository;

    public SaleNumberingService(SaleRepository saleRepository,
                                SeriesCounterRepository seriesCounterRepository) {
        this.saleRepository = saleRepository;
        this.seriesCounterRepository = seriesCounterRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public synchronized long nextDocumentNumber(Long branchId, String series, String documentType) {
        String cleanSeries = (series != null && !series.isBlank()) ? series.trim().toUpperCase() : "A";
        String cleanType = (documentType != null && !documentType.isBlank()) ? documentType.trim().toUpperCase() : "VENDA";
        int year = LocalDate.now().getYear();

        var counterOpt = seriesCounterRepository.findForUpdate(branchId, cleanSeries, cleanType);
        SeriesCounter counter;
        if (counterOpt.isPresent()) {
            counter = counterOpt.get();
            long next = counter.getCurrentNumber() + 1L;
            counter.setCurrentNumber(next);
            counter.setUpdatedAt(LocalDateTime.now());
            seriesCounterRepository.save(counter);
            return next;
        } else {
            Long lastNumber = saleRepository.findMaxDocumentNumberBySeriesAndDocumentTypeAndBranchIdAndYear(cleanSeries, cleanType, branchId, year);
            long initialNumber = (lastNumber == null ? 1L : lastNumber + 1L);
            counter = new SeriesCounter(branchId, cleanSeries, cleanType, initialNumber);
            seriesCounterRepository.save(counter);
            return initialNumber;
        }
    }

    /**
     * Retorna o próximo número sequencial controlado por filial para o hashHash AT.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public synchronized long nextHashControl(Long branchId) {
        Long last = saleRepository.findMaxHashControlByBranchId(branchId);
        return (last == null ? 1L : last + 1L);
    }
}
