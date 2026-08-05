package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaleServiceAnnulmentTest {

    @Autowired private SaleService saleService;
    @Autowired private StockBranchService stockBranchService;
    @Autowired private StockBranchRepository stockBranchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private CashSessionService cashSessionService;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    @Test
    void annulCreditSale_restoresStockAndCustomerBalance() throws Exception {
        Role role = new Role();
        role.setName("SALE_ANNUL_TEST");
        role.setDescription("Role para anulação de venda");
        role.getPermissions().add("VENDAS:CREATE");
        role.getPermissions().add("VENDAS:DELETE");
        role.getPermissions().add("STOCK:CREATE");
        roleRepository.save(role);

        User user = new User();
        user.setUsername("sale-annul-user");
        user.setPasswordHash("x");
        user.getRoles().add(role);
        userRepository.save(user);

        Branch branch = new Branch();
        branch.setName("Filial Anulação");
        branch.setNuit("555666777");
        branch.setAddress("Av. Anulação");
        branch.setContact("841111114");
        branchRepository.save(branch);

        Category category = new Category();
        category.setName("Categoria Anulação");
        categoryRepository.save(category);

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("UN");
        unit.setDescription("Unidade");
        metricUnitRepository.save(unit);

        Product product = new Product();
        product.setCode("F-ANNUL");
        product.setName("Produto Anulação");
        product.setCategory(category);
        product.setUnit(unit);
        product.setPriceCost(10.0);
        product.setPriceSale(20.0);
        product.setService(false);
        product.setIsActive(true);
        productRepository.save(product);

        stockBranchService.initializeStock(branch, product, new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", user);

        Customer customer = new Customer();
        customer.setCode("CLI-ANNUL");
        customer.setName("Cliente Anulação");
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
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(new BigDecimal("40.00"));
        item.setDescription(product.getName());
        sale.setItems(List.of(item));

        saleService.processAndSave(sale, user);

        StockBranch stockBeforeAnnul = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseThrow(() -> new AssertionError("Stock da filial não foi criado"));
        assertEquals(0, new BigDecimal("3.00").compareTo(stockBeforeAnnul.getStockCurrentAmount()));

        Customer customerBeforeAnnul = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("52.50").compareTo(customerBeforeAnnul.getBalanceAmount()));

        Sale persistedSale = saleRepository.findAll().stream()
                .filter(s -> s.getCustomer() != null && s.getCustomer().getId().equals(customer.getId()))
                .findFirst()
                .orElseThrow();

        saleService.annulSale(persistedSale, "Erro de emissão", user);
        entityManager.clear();

        StockBranch stockAfterAnnul = stockBranchRepository.findByProductAndBranch(product, branch)
            .orElseThrow(() -> new AssertionError("Stock da filial não foi criado"));
        assertEquals(0, new BigDecimal("5.00").compareTo(stockAfterAnnul.getStockCurrentAmount()));

        Customer customerAfterAnnul = customerRepository.findById(customer.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("12.50").compareTo(customerAfterAnnul.getBalanceAmount()));

        Sale annulledSale = saleRepository.findById(persistedSale.getId()).orElseThrow();
        assertEquals("ANULADA", annulledSale.getState());
        assertEquals("Erro de emissão", annulledSale.getAnnulReason());
    }

    @Test
    void annulledSale_cannotBeUpdatedOrDeleted() throws Exception {
        Role role = new Role();
        role.setName("SALE_ANNUL_PROTECT_TEST");
        role.setDescription("Role para proteção de documento anulado");
        role.getPermissions().add("VENDAS:CREATE");
        role.getPermissions().add("VENDAS:DELETE");
        role.getPermissions().add("STOCK:CREATE");
        role.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(role);

        User user = new User();
        user.setUsername("sale-annul-protect-user");
        user.setPasswordHash("x");
        user.getRoles().add(role);
        userRepository.save(user);

        Branch branch = new Branch();
        branch.setName("Filial Proteção");
        branch.setNuit("666777888");
        branch.setAddress("Av. Proteção");
        branch.setContact("841111115");
        branchRepository.save(branch);

        Category category = new Category();
        category.setName("Categoria Proteção");
        categoryRepository.save(category);

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("UN");
        unit.setDescription("Unidade");
        metricUnitRepository.save(unit);

        Product product = new Product();
        product.setCode("F-PROTECT");
        product.setName("Produto Proteção");
        product.setCategory(category);
        product.setUnit(unit);
        product.setPriceCost(10.0);
        product.setPriceSale(20.0);
        product.setService(false);
        product.setIsActive(true);
        productRepository.save(product);

        stockBranchService.initializeStock(branch, product, new BigDecimal("7"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", user);

        cashSessionService.openSession(user, BigDecimal.ZERO);

        Customer customer = new Customer();
        customer.setCode("CLI-PROTECT");
        customer.setName("Cliente Proteção");
        customer.setBalanceAmount(BigDecimal.ZERO);
        customerRepository.save(customer);

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
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(new BigDecimal("20.00"));
        item.setDescription(product.getName());
        sale.setItems(List.of(item));

        saleService.processAndSave(sale, user);

        Sale persistedSale = saleRepository.findAll().stream()
                .filter(s -> s.getCustomer() != null && s.getCustomer().getId().equals(customer.getId()))
                .findFirst()
                .orElseThrow();

        saleService.annulSale(persistedSale, "Motivo fiscal", user);
        entityManager.clear();

        Sale lockedSale = saleRepository.findById(persistedSale.getId()).orElseThrow();

        InvalidDataAccessApiUsageException updateException = assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            lockedSale.setCustomerName("Tentativa de alteração");
            saleRepository.saveAndFlush(lockedSale);
        });
        assertNotNull(updateException.getCause());
        assertTrue(updateException.getCause() instanceof IllegalStateException);
        assertEquals("Um documento fiscal ANULADO não pode ser modificado. Crie um novo documento.", updateException.getCause().getMessage());

        InvalidDataAccessApiUsageException deleteException = assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            saleRepository.delete(lockedSale);
            saleRepository.flush();
        });
        assertNotNull(deleteException.getCause());
        assertTrue(deleteException.getCause() instanceof IllegalStateException);
        assertEquals("Não é possível remover um documento fiscal anulado.", deleteException.getCause().getMessage());
    }
}
