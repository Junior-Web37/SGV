package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaleServiceCreditFlowTest {

    @Autowired private SaleService saleService;
    @Autowired private StockBranchService stockBranchService;
    @Autowired private CashSessionService cashSessionService;
    @Autowired private StockBranchRepository stockBranchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private SaleRepository saleRepository;

    @Test
    void creditSale_decrementsStockAndIncreasesCustomerBalance() throws Exception {
        Role role = new Role();
        role.setName("SALE_TEST");
        role.setDescription("Role para teste de venda");
        role.getPermissions().add("VENDAS:CREATE");
        role.getPermissions().add("STOCK:CREATE");
        roleRepository.save(role);

        User user = new User();
        user.setUsername("sale-flow-user");
        user.setPasswordHash("x");
        user.getRoles().add(role);
        userRepository.save(user);

        Branch branch = new Branch();
        branch.setName("Filial Fluxo");
        branch.setNuit("111222333");
        branch.setAddress("Av. Fluxo");
        branch.setContact("841111111");
        branchRepository.save(branch);

        Category category = new Category();
        category.setName("Categoria Fluxo");
        categoryRepository.save(category);

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("UN");
        unit.setDescription("Unidade");
        metricUnitRepository.save(unit);

        Product product = new Product();
        product.setCode("F001");
        product.setName("Produto Fluxo");
        product.setCategory(category);
        product.setUnit(unit);
        product.setPriceCost(10.0);
        product.setPriceSale(20.0);
        product.setService(false);
        product.setIsActive(true);
        productRepository.save(product);

        stockBranchService.initializeStock(branch, product, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", user);

        Customer customer = new Customer();
        customer.setCode("CLI-FLOW");
        customer.setName("Cliente Fluxo");
        customer.setBalanceAmount(BigDecimal.ZERO);
        customerRepository.save(customer);

        Sale sale = new Sale();
        sale.setBranch(branch);
        sale.setCustomer(customer);
        sale.setDocumentType("VENDA");
        sale.setSeries("A");
        sale.setPaymentMethod("CREDITO");
        sale.setState("EMITIDA");

        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setProductCode(product.getCode());
        item.setQtyAmount(new BigDecimal("2"));
        item.setUnitPriceAmount(new BigDecimal("20.00"));
        item.setLineBaseAmount(new BigDecimal("40.00"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(new BigDecimal("0.00"));
        item.setLineTotalAmount(new BigDecimal("40.00"));
        item.setDescription(product.getName());
        sale.setItems(List.of(item));

        saleService.processAndSave(sale, user);

        StockBranch updatedStock = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseThrow(() -> new AssertionError("Stock da filial não foi criado"));
        assertEquals(0, new BigDecimal("8.00").compareTo(updatedStock.getStockCurrentAmount()));

        Customer refreshedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("40.00").compareTo(refreshedCustomer.getBalanceAmount()));
    }

    @Test
    void creditSale_accumulatesOnExistingCustomerBalance() throws Exception {
        Role role = new Role();
        role.setName("SALE_TEST_2");
        role.setDescription("Role para teste de acumulação de crédito");
        role.getPermissions().add("VENDAS:CREATE");
        role.getPermissions().add("STOCK:CREATE");
        roleRepository.save(role);

        User user = new User();
        user.setUsername("sale-flow-user-2");
        user.setPasswordHash("x");
        user.getRoles().add(role);
        userRepository.save(user);

        Branch branch = new Branch();
        branch.setName("Filial Fluxo 2");
        branch.setNuit("222333444");
        branch.setAddress("Av. Fluxo 2");
        branch.setContact("841111112");
        branchRepository.save(branch);

        Category category = new Category();
        category.setName("Categoria Fluxo 2");
        categoryRepository.save(category);

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("UN");
        unit.setDescription("Unidade");
        metricUnitRepository.save(unit);

        Product product = new Product();
        product.setCode("F002");
        product.setName("Produto Fluxo 2");
        product.setCategory(category);
        product.setUnit(unit);
        product.setPriceCost(10.0);
        product.setPriceSale(20.0);
        product.setService(false);
        product.setIsActive(true);
        productRepository.save(product);

        stockBranchService.initializeStock(branch, product, new BigDecimal("15"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", user);

        Customer customer = new Customer();
        customer.setCode("CLI-FLOW-2");
        customer.setName("Cliente Fluxo 2");
        customer.setBalanceAmount(new BigDecimal("12.50"));
        customerRepository.save(customer);

        Sale sale = new Sale();
        sale.setBranch(branch);
        sale.setCustomer(customer);
        sale.setDocumentType("VENDA");
        sale.setSeries("A");
        sale.setPaymentMethod("CREDITO");
        sale.setState("EMITIDA");

        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setProductCode(product.getCode());
        item.setQtyAmount(new BigDecimal("2"));
        item.setUnitPriceAmount(new BigDecimal("20.00"));
        item.setLineBaseAmount(new BigDecimal("40.00"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(new BigDecimal("0.00"));
        item.setLineTotalAmount(new BigDecimal("40.00"));
        item.setDescription(product.getName());
        sale.setItems(List.of(item));

        saleService.processAndSave(sale, user);

        StockBranch updatedStock = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseThrow(() -> new AssertionError("Stock da filial não foi criado"));
        assertEquals(0, new BigDecimal("13.00").compareTo(updatedStock.getStockCurrentAmount()));

        Customer refreshedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("52.50").compareTo(refreshedCustomer.getBalanceAmount()));
    }

    @Test
    void cashSale_setsPaidAmountToTotalAndZeroChange() throws Exception {
        Role role = new Role();
        role.setName("SALE_TEST_3");
        role.setDescription("Role para teste de pagamento à vista");
        role.getPermissions().add("VENDAS:CREATE");
        role.getPermissions().add("STOCK:CREATE");
        role.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(role);

        Branch branch = new Branch();
        branch.setName("Filial Fluxo 3");
        branch.setNuit("333444555");
        branch.setAddress("Av. Fluxo 3");
        branch.setContact("841111113");
        branchRepository.save(branch);

        User user = new User();
        user.setUsername("sale-flow-user-3");
        user.setPasswordHash("x");
        user.getRoles().add(role);
        user.setBranch(branch);
        userRepository.save(user);

        Category category = new Category();
        category.setName("Categoria Fluxo 3");
        categoryRepository.save(category);

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("UN");
        unit.setDescription("Unidade");
        metricUnitRepository.save(unit);

        Product product = new Product();
        product.setCode("F003");
        product.setName("Produto Fluxo 3");
        product.setCategory(category);
        product.setUnit(unit);
        product.setPriceCost(10.0);
        product.setPriceSale(20.0);
        product.setService(false);
        product.setIsActive(true);
        productRepository.save(product);

        stockBranchService.initializeStock(branch, product, new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", user);

        Customer customer = new Customer();
        customer.setCode("CLI-FLOW-3");
        customer.setName("Cliente Fluxo 3");
        customer.setBalanceAmount(BigDecimal.ZERO);
        customerRepository.save(customer);

        cashSessionService.openSession(user, BigDecimal.ZERO);

        Sale sale = new Sale();
        sale.setBranch(branch);
        sale.setCustomer(customer);
        sale.setDocumentType("VENDA");
        sale.setSeries("A");
        sale.setPaymentMethod("DINHEIRO");
        sale.setState("EMITIDA");

        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setProductCode(product.getCode());
        item.setQtyAmount(new BigDecimal("1"));
        item.setUnitPriceAmount(new BigDecimal("20.00"));
        item.setLineBaseAmount(new BigDecimal("20.00"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(new BigDecimal("0.00"));
        item.setLineTotalAmount(new BigDecimal("20.00"));
        item.setDescription(product.getName());
        sale.setItems(List.of(item));

        File generatedDocument = saleService.processAndSave(sale, user);
        assertNotNull(generatedDocument);

        Sale savedSale = saleRepository.findById(sale.getId()).orElse(null);
        assertNotNull(savedSale);
        assertEquals(0, new BigDecimal("20.00").compareTo(savedSale.getPaidAmountValue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(savedSale.getChangeAmountValue()));
    }
}
