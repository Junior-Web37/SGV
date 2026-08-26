package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SaleServiceTest {

    @Test
    void createCreditNoteCreatesNegativeQuantityDocument() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        SaleItemRepository saleItemRepository = mock(SaleItemRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        SaleNumberingService saleNumberingService = mock(SaleNumberingService.class);
        SaleDocumentService saleDocumentService = mock(SaleDocumentService.class);
        ThermalPrintService thermalPrintService = mock(ThermalPrintService.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        FiscalService fiscalService = mock(FiscalService.class);
        TrainingModeService trainingModeService = mock(TrainingModeService.class);
        AppConfigService appConfigService = mock(AppConfigService.class);

        when(trainingModeService.isTrainingMode()).thenReturn(false);
        when(saleRepository.findByIdWithItems(10L)).thenReturn(buildSale());
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(buildBranch()));
        when(productRepository.findByIdIn(any())).thenReturn(List.of(buildProduct()));
        when(stockBranchService.increaseStock(any(), any(), any(), anyString(), anyString(), any())).thenReturn(new StockBranch());
        CashSessionService cashSessionService = mock(CashSessionService.class);
        when(cashSessionService.registerMovement(any(), anyString(), any(), anyString(), anyString())).thenReturn(new CashMovement());
        // BUG-010: a NC sobre venda a crédito tem de abater a dívida do cliente
        // (lançamento CREDIT_NOTE no livro de conta corrente).
        Customer cust = buildCustomer();
        when(customerRepository.findById(5L)).thenReturn(Optional.of(cust));
        CustomerAccountEntryRepository entryRepository = mock(CustomerAccountEntryRepository.class);
        CustomerAccountLedger ledger = new CustomerAccountLedger(entryRepository, customerRepository);

        SaleService saleService = new SaleService(
                saleRepository,
                saleItemRepository,
                stockBranchService,
                saleNumberingService,
                saleDocumentService,
                thermalPrintService,
                productRepository,
                branchRepository,
                customerRepository,
                fiscalService,
                trainingModeService,
                appConfigService,
                cashSessionService,
                ledger);

        Sale result = saleService.createCreditNote(buildSale(), "Devolução", buildUser());

        assertNotNull(result);
        assertEquals("NC", result.getDocumentType());
        assertFalse(result.getItems().isEmpty());
        assertTrue(result.getItems().get(0).getQtyAmount().compareTo(BigDecimal.ZERO) < 0);
        verify(stockBranchService, atLeastOnce()).increaseStock(any(), any(), any(), anyString(), anyString(), any());
        // Dívida 10 − 10 (NC) = 0
        assertEquals(BigDecimal.ZERO, cust.getBalanceAmount());
        verify(entryRepository).save(any());
    }

    @Test
    void annulSaleRestoresStockAndCustomerBalance() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        SaleItemRepository saleItemRepository = mock(SaleItemRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        SaleNumberingService saleNumberingService = mock(SaleNumberingService.class);
        SaleDocumentService saleDocumentService = mock(SaleDocumentService.class);
        ThermalPrintService thermalPrintService = mock(ThermalPrintService.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        FiscalService fiscalService = mock(FiscalService.class);
        TrainingModeService trainingModeService = mock(TrainingModeService.class);
        AppConfigService appConfigService = mock(AppConfigService.class);

        when(trainingModeService.isTrainingMode()).thenReturn(false);
        when(saleRepository.findByIdWithItems(10L)).thenReturn(buildSale());
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(buildCustomer()));
        when(stockBranchService.increaseStock(any(), any(), any(), anyString(), anyString(), any())).thenReturn(new StockBranch());
        CashSessionService cashSessionService = mock(CashSessionService.class);
        when(cashSessionService.registerMovement(any(), anyString(), any(), anyString(), anyString())).thenReturn(new CashMovement());
        CustomerAccountEntryRepository entryRepository = mock(CustomerAccountEntryRepository.class);
        CustomerAccountLedger ledger = new CustomerAccountLedger(entryRepository, customerRepository);

        SaleService saleService = new SaleService(
                saleRepository,
                saleItemRepository,
                stockBranchService,
                saleNumberingService,
                saleDocumentService,
                thermalPrintService,
                productRepository,
                branchRepository,
                customerRepository,
                fiscalService,
                trainingModeService,
                appConfigService,
                cashSessionService,
                ledger);

        Sale result = saleService.annulSale(buildSale(), "Motivo", buildUser());

        assertNotNull(result);
        assertEquals("ANULADA", result.getState());
        verify(stockBranchService, atLeastOnce()).increaseStock(any(), any(), any(), anyString(), anyString(), any());
        verify(customerRepository).save(any(Customer.class));
    }

    private Sale buildSale() {
        Sale sale = new Sale();
        sale.setId(10L);
        sale.setState("PAGO");
        sale.setPaymentMethod("CREDITO");
        sale.setTotalAmount(BigDecimal.TEN);
        sale.setBranch(new Branch());
        sale.getBranch().setId(1L);
        Customer customer = buildCustomer();
        sale.setCustomer(customer);
        sale.setItems(List.of(buildItem()));
        sale.setCreatedAt(LocalDateTime.now());
        return sale;
    }

    private SaleItem buildItem() {
        SaleItem item = new SaleItem();
        Product product = new Product();
        product.setId(3L);
        product.setService(false);
        item.setProduct(product);
        item.setQtyAmount(BigDecimal.ONE);
        item.setQty(1.0);
        item.setLineTotalAmount(BigDecimal.TEN);
        item.setLineBaseAmount(BigDecimal.TEN);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineIceAmount(BigDecimal.ZERO);
        return item;
    }

    private Branch buildBranch() {
        Branch branch = new Branch();
        branch.setId(1L);
        return branch;
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(3L);
        product.setService(false);
        return product;
    }

    private Customer buildCustomer() {
        Customer customer = new Customer();
        customer.setId(5L);
        customer.setBalanceAmount(BigDecimal.TEN);
        return customer;
    }

    private User buildUser() {
        User user = new User();
        user.setUsername("test");
        Role role = new Role();
        role.setName("ADMIN");
        role.setPermissions(Set.of("VENDAS:CREATE", "VENDAS:DELETE"));
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }
}
