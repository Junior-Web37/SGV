package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.CategoryRepository;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.ExpenseRepository;
import com.sgv.repository.MetricUnitRepository;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.ProductionOrderRepository;
import com.sgv.repository.UserRepository;
import com.sgv.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardCrudServiceTest {

    @Test
    void deleteCustomerDelegatesToRepository() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProductionOrderRepository productionOrderRepository = mock(ProductionOrderRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        MetricUnitRepository metricUnitRepository = mock(MetricUnitRepository.class);

        CustomerAccountService customerAccountService = mock(CustomerAccountService.class);
        SaleService saleService = mock(SaleService.class);

        DashboardCrudService service = new DashboardCrudService(
            customerRepository,
            purchaseRepository,
            expenseRepository,
            paymentRepository,
            userRepository,
            productionOrderRepository,
            warehouseRepository,
            productRepository,
            stockBranchService,
            categoryRepository,
            metricUnitRepository,
            customerAccountService,
            saleService);

        service.deleteCustomer(7L);

        verify(customerRepository).deleteById(7L);
    }

    @Test
    void deleteCategoryDelegatesToRepository() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProductionOrderRepository productionOrderRepository = mock(ProductionOrderRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        MetricUnitRepository metricUnitRepository = mock(MetricUnitRepository.class);
        CustomerAccountService customerAccountService = mock(CustomerAccountService.class);
        SaleService saleService = mock(SaleService.class);

        DashboardCrudService service = new DashboardCrudService(
                customerRepository,
                purchaseRepository,
                expenseRepository,
                paymentRepository,
                userRepository,
                productionOrderRepository,
                warehouseRepository,
                productRepository,
                stockBranchService,
                categoryRepository,
                metricUnitRepository,
                customerAccountService,
                saleService);

        service.deleteCategory(13L);

        verify(categoryRepository).deleteById(13L);
    }

    @Test
    void recordReceiptDelegatesToCustomerAccountService() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProductionOrderRepository productionOrderRepository = mock(ProductionOrderRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        MetricUnitRepository metricUnitRepository = mock(MetricUnitRepository.class);
        CustomerAccountService customerAccountService = mock(CustomerAccountService.class);
        SaleService saleService = mock(SaleService.class);

        DashboardCrudService service = new DashboardCrudService(
                customerRepository,
                purchaseRepository,
                expenseRepository,
                paymentRepository,
                userRepository,
                productionOrderRepository,
                warehouseRepository,
                productRepository,
                stockBranchService,
                categoryRepository,
                metricUnitRepository,
                customerAccountService,
                saleService);

        Customer customer = new Customer();
        Sale sale = new Sale();
        User user = new User();
        Payment payment = new Payment();
        BigDecimal amount = BigDecimal.valueOf(150.0);

        when(customerAccountService.recordReceipt(customer, sale, amount, "Dinheiro", user)).thenReturn(payment);

        Payment result = service.recordReceipt(customer, sale, amount, "Dinheiro", user);

        verify(customerAccountService).recordReceipt(customer, sale, amount, "Dinheiro", user);
        assert result == payment;
    }

    @Test
    void reconcileCustomerCreditsDelegatesToCustomerAccountService() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ProductionOrderRepository productionOrderRepository = mock(ProductionOrderRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        MetricUnitRepository metricUnitRepository = mock(MetricUnitRepository.class);
        CustomerAccountService customerAccountService = mock(CustomerAccountService.class);
        SaleService saleService = mock(SaleService.class);

        DashboardCrudService service = new DashboardCrudService(
                customerRepository,
                purchaseRepository,
                expenseRepository,
                paymentRepository,
                userRepository,
                productionOrderRepository,
                warehouseRepository,
                productRepository,
                stockBranchService,
                categoryRepository,
                metricUnitRepository,
                customerAccountService,
                saleService);

        Customer customer = new Customer();
        User user = new User();
        ReconciliationResult result = new ReconciliationResult(java.util.Collections.emptyList(), BigDecimal.ZERO);
        when(customerAccountService.reconcileCustomerCredits(customer, BigDecimal.valueOf(200.0), user)).thenReturn(result);

        ReconciliationResult actual = service.reconcileCustomerCredits(customer, BigDecimal.valueOf(200.0), user);

        verify(customerAccountService).reconcileCustomerCredits(customer, BigDecimal.valueOf(200.0), user);
        assert actual == result;
    }
}
