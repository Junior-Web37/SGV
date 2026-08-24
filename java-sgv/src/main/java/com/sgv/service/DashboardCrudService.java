package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardCrudService {

    private final CustomerRepository customerRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockBranchService stockBranchService;
    private final CategoryRepository categoryRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final CustomerAccountService customerAccountService;
    private final SaleService saleService;

    public DashboardCrudService(CustomerRepository customerRepository,
                                PurchaseRepository purchaseRepository,
                                ExpenseRepository expenseRepository,
                                PaymentRepository paymentRepository,
                                UserRepository userRepository,
                                ProductionOrderRepository productionOrderRepository,
                                WarehouseRepository warehouseRepository,
                                ProductRepository productRepository,
                                StockBranchService stockBranchService,
                                CategoryRepository categoryRepository,
                                MetricUnitRepository metricUnitRepository,
                                CustomerAccountService customerAccountService,
                                SaleService saleService) {
        this.customerRepository = customerRepository;
        this.purchaseRepository = purchaseRepository;
        this.expenseRepository = expenseRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.stockBranchService = stockBranchService;
        this.categoryRepository = categoryRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.customerAccountService = customerAccountService;
        this.saleService = saleService;
    }

    @Transactional
    public void deleteCustomer(Long id) { customerRepository.deleteById(id); }

    @Transactional
    public void deletePurchase(Long id) { purchaseRepository.deleteById(id); }

    @Transactional
    public void deleteExpense(Long id) { expenseRepository.deleteById(id); }

    @Transactional
    public void deletePayment(Long id) { paymentRepository.deleteById(id); }

    @Transactional
    public void toggleUser(User user) {
        if (user == null) throw new IllegalArgumentException("User is null");
        user.setActive(!Boolean.TRUE.equals(user.isActive()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteProductionOrder(Long id) { productionOrderRepository.deleteById(id); }

    @Transactional
    public void completeProductionOrder(ProductionOrder order) {
        if (order == null) throw new IllegalArgumentException("Order is null");
        order.setState("COMPLETED");
        order.setCompletedAt(java.time.LocalDateTime.now());
        productionOrderRepository.save(order);
    }

    @Transactional
    public void deleteWarehouse(Long id) { warehouseRepository.deleteById(id); }

    @Transactional
    public void deleteProduct(Long id) { productRepository.deleteById(id); }

    @Transactional
    public void deleteCategory(Long id) { categoryRepository.deleteById(id); }

    @Transactional
    public void deleteMetricUnit(Long id) { metricUnitRepository.deleteById(id); }

    @Transactional
    public void deleteStockBranch(StockBranch stock) {
        if (stock == null) throw new IllegalArgumentException("Stock is null");
        stockBranchService.deleteStock(stock, null);
    }

    @Transactional
    public Payment recordReceipt(Customer customer, Sale sale, java.math.BigDecimal amount, String method, User currentUser) {
        if (customerAccountService == null) throw new IllegalStateException("CustomerAccountService not available");
        return customerAccountService.recordReceipt(customer, sale, amount, method, currentUser);
    }

    @Transactional
    public ReconciliationResult reconcileCustomerCredits(Customer customer, java.math.BigDecimal amount, User currentUser) {
        if (customerAccountService == null) throw new IllegalStateException("CustomerAccountService not available");
        return customerAccountService.reconcileCustomerCredits(customer, amount, currentUser);
    }
}
