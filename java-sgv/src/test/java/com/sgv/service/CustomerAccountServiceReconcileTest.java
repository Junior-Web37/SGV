package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.PaymentAllocationRepository;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerAccountServiceReconcileTest {

    @Test
    void reconcileAppliesToOldestPendingSales() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentAllocationRepository paymentAllocationRepository = mock(PaymentAllocationRepository.class);
        SystemLogService systemLogService = mock(SystemLogService.class);

        CustomerAccountService svc = new CustomerAccountService(customerRepository, saleRepository, paymentRepository, paymentAllocationRepository, systemLogService);

        Customer c = new Customer();
        c.setId(42L);

        Sale s1 = new Sale(); s1.setId(1L); s1.setTotalAmount(BigDecimal.valueOf(100)); s1.setPaidAmountValue(BigDecimal.ZERO); s1.setCreatedAt(java.time.LocalDateTime.now().minusDays(3));
        Sale s2 = new Sale(); s2.setId(2L); s2.setTotalAmount(BigDecimal.valueOf(50)); s2.setPaidAmountValue(BigDecimal.ZERO); s2.setCreatedAt(java.time.LocalDateTime.now().minusDays(1));

        when(saleRepository.findPendingByCustomerId(42L)).thenReturn(List.of(s1, s2));
        when(customerRepository.findById(42L)).thenReturn(Optional.of(c));

        ReconciliationResult result = svc.reconcileCustomerCredits(c, BigDecimal.valueOf(120), new User());

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getRemaining());
        verify(paymentRepository, atLeast(1)).save(any());
        verify(paymentAllocationRepository, atLeast(1)).save(any());
        verify(saleRepository, atLeast(2)).save(any());
    }
}
