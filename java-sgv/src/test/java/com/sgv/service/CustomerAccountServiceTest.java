package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.repository.CustomerAccountEntryRepository;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.PaymentAllocationRepository;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerAccountServiceTest {

    @Test
    void shouldSettleSaleAndReduceCustomerBalanceWhenReceiptCoversFullAmount() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        SystemLogService systemLogService = mock(SystemLogService.class);

        Customer customer = new Customer();
        customer.setId(7L);
        customer.setBalanceAmount(BigDecimal.valueOf(100));

        Sale sale = new Sale();
        sale.setId(11L);
        sale.setTotalAmount(BigDecimal.valueOf(80));
        sale.setState("PENDENTE");

        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.findById(11L)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentAllocationRepository paymentAllocationRepository = mock(PaymentAllocationRepository.class);
        CustomerAccountEntryRepository entryRepository = mock(CustomerAccountEntryRepository.class);
        // Ledger real sobre o customerRepository mockado: o saldo é escrito
        // pelo CustomerAccountLedger (ponto único de escrita — BUG-005/010).
        CustomerAccountLedger ledger = new CustomerAccountLedger(entryRepository, customerRepository);
        CustomerAccountService service = new CustomerAccountService(customerRepository, saleRepository, paymentRepository, paymentAllocationRepository, systemLogService, null, entryRepository, ledger);

        service.recordReceipt(customer, sale, BigDecimal.valueOf(80), "Dinheiro", null);

        assertEquals(BigDecimal.valueOf(20), customer.getBalanceAmount());
        assertEquals("PAGO", sale.getState());
        verify(paymentRepository).save(any(Payment.class));
        verify(entryRepository).save(any());
    }
}
