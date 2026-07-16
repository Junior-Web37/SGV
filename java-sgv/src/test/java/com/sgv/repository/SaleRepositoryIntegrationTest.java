package com.sgv.repository;

import com.sgv.BaseIntegrationTest;
import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SaleRepository.
 * Uses real MariaDB via Testcontainers.
 */
class SaleRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SaleRepository saleRepository;

    @Test
    void save_and_find_by_id() {
        Sale sale = new Sale();
        sale.setSeries("A");
        sale.setDocumentType("FA");
        sale.setDocumentNumber(1L);
        sale.setDocumentYear(LocalDateTime.now().getYear());
        sale.setState("EMITIDA");
        sale.setTotal(100.0);
        sale.setSubtotal(100.0);
        sale.setCurrency("MZN");
        sale.setCustomerName("Consumidor Final");
        sale.setCustomerNuit("999999999");
        sale.setCreatedAt(LocalDateTime.now());

        Sale saved = saleRepository.save(sale);

        assertThat(saved.getId()).isNotNull();
        assertThat(saleRepository.findById(saved.getId())).isPresent();
        assertThat(saleRepository.findById(saved.getId()).get().getState()).isEqualTo("EMITIDA");
    }

    @Test
    void findByState_filtersCorrectly() {
        Sale emitted = new Sale();
        emitted.setSeries("A"); emitted.setDocumentType("FA");
        emitted.setDocumentNumber(1L); emitted.setDocumentYear(LocalDateTime.now().getYear());
        emitted.setState("EMITIDA"); emitted.setTotal(10.0); emitted.setSubtotal(10.0);
        emitted.setCurrency("MZN"); emitted.setCustomerName("X"); emitted.setCustomerNuit("999999999");
        saleRepository.save(emitted);

        Sale cancelled = new Sale();
        cancelled.setSeries("A"); cancelled.setDocumentType("FA");
        cancelled.setDocumentNumber(2L); cancelled.setDocumentYear(LocalDateTime.now().getYear());
        cancelled.setState("ANULADA"); cancelled.setTotal(20.0); cancelled.setSubtotal(20.0);
        cancelled.setCurrency("MZN"); cancelled.setCustomerName("Y"); cancelled.setCustomerNuit("999999999");
        saleRepository.save(cancelled);

        assertThat(saleRepository.findByState("EMITIDA")).hasSizeGreaterThanOrEqualTo(1);
        assertThat(saleRepository.findByState("ANULADA")).hasSizeGreaterThanOrEqualTo(1);
        assertThat(saleRepository.findByState("PENDENTE")).isEmpty();
    }

    @Test
    void sale_items_are_persisted_cascade() {
        Sale sale = new Sale();
        sale.setSeries("A"); sale.setDocumentType("FA");
        sale.setDocumentNumber(1L); sale.setDocumentYear(LocalDateTime.now().getYear());
        sale.setState("EMITIDA"); sale.setTotal(200.0); sale.setSubtotal(200.0);
        sale.setCurrency("MZN"); sale.setCustomerName("X"); sale.setCustomerNuit("999999999");

        SaleItem item1 = new SaleItem();
        item1.setQty(2.0); item1.setUnitPrice(50.0);
        item1.setLineBase(100.0); item1.setLineTotal(117.0);
        item1.setLineTax(17.0);
        item1.setSale(sale);

        SaleItem item2 = new SaleItem();
        item2.setQty(1.0); item2.setUnitPrice(100.0);
        item2.setLineBase(100.0); item2.setLineTotal(117.0);
        item2.setLineTax(17.0);
        item2.setSale(sale);

        sale.getItems().add(item1);
        sale.getItems().add(item2);

        Sale saved = saleRepository.save(sale);

        Sale reloaded = saleRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getItems()).hasSize(2);
        assertThat(reloaded.getItems())
            .extracting(item -> item.getQty())
            .containsExactlyInAnyOrder(2.0, 1.0);
    }

    @Test
    void countByDateRange_returnsCorrectCount() {
        long before = saleRepository.countByDateRange(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1)
        );

        Sale s = new Sale();
        s.setSeries("A"); s.setDocumentType("FA");
        s.setDocumentNumber(9999L); s.setDocumentYear(LocalDateTime.now().getYear());
        s.setState("EMITIDA"); s.setTotal(50.0); s.setSubtotal(50.0);
        s.setCurrency("MZN"); s.setCustomerName("Test"); s.setCustomerNuit("999999999");
        saleRepository.save(s);

        long after = saleRepository.countByDateRange(
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(1)
        );

        assertThat(after).isEqualTo(before + 1);
    }
}
