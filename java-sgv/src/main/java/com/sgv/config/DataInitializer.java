package com.sgv.config;

import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.RoleRepository;
import com.sgv.repository.UserRepository;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.CategoryRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.CustomerRepository;
import com.sgv.service.StockBranchService;
import com.sgv.repository.SaleRepository;
import com.sgv.entity.Branch;
import com.sgv.entity.Category;
import com.sgv.entity.Product;
import com.sgv.entity.Customer;
import com.sgv.entity.StockBranch;
import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import com.sgv.entity.Payment;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final StockBranchService stockBranchService;
    private final SaleRepository saleRepository;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           BranchRepository branchRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           CustomerRepository customerRepository,
                           StockBranchService stockBranchService,
                           SaleRepository saleRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.stockBranchService = stockBranchService;
        this.saleRepository = saleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting default data initialization");
        ensureRole("ADMIN", "Administrador do sistema");
        ensureRole("GESTOR", "Gestor / Gerente");
        ensureRole("CAIXA", "Operador de Caixa");
        ensureRole("CLIENTE", "Cliente (corrente/diverso)");
        ensureDefaultBranch();
        ensureDefaultUsers();
        ensureDefaultCategory();
        ensureDefaultProduct();
        ensureDefaultCustomer();
        ensureDefaultSales();
        logger.info("Default data initialization complete");
    }

    private void ensureDefaultUsers() {
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            r.setDescription("Administrador do sistema");
            return roleRepository.save(r);
        });

        ensureUser("admin", "admin", "Administrador", adminRole);
    }

    private void ensureUser(String username, String rawPassword, String fullName, Role role) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setActive(true);
            user.setCanViewStats(true);
            user.getRoles().add(role);
            Branch branch = branchRepository.findAll().stream()
                    .filter(b -> "Matriz SGV".equalsIgnoreCase(b.getName()))
                    .findFirst().orElse(null);
            if (branch != null) {
                user.setBranch(branch);
            }
            userRepository.save(user);
        }
    }

    private void ensureRole(String name, String desc) {
        Role role = roleRepository.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDescription(desc);
            return r;
        });

        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            role.setPermissions(defaultPermissionsForRole(name));
            roleRepository.save(role);
        }
    }

    private Set<String> defaultPermissionsForRole(String roleName) {
        String normalized = roleName != null ? roleName.toUpperCase() : "";
        Set<String> permissions = new HashSet<>();
        switch (normalized) {
            case "ADMIN" -> {
                permissions.addAll(Set.of(
                    "RESUMO:VIEW",
                    "VENDAS:VIEW",
                    "VENDAS:CREATE",
                    "VENDAS:DELETE",
                    "PRODUTOS:VIEW",
                    "PRODUTOS:CREATE",
                    "PRODUTOS:DELETE",
                    "CLIENTES:VIEW",
                    "CLIENTES:CREATE",
                    "CLIENTES:DELETE",
                    "STOCK:VIEW",
                    "STOCK:CREATE",
                    "ARMAZENS:VIEW",
                    "TRANSFERENCIAS:VIEW",
                    "CATALOGOS:VIEW",
                    "CATALOGOS:CREATE",
                    "CATALOGOS:DELETE",
                    "CAIXA:VIEW",
                    "FINANCEIRO:VIEW",
                    "COMPRAS:VIEW",
                    "PRODUCAO:VIEW",
                    "RELATORIOS:VIEW",
                    "SISTEMA:VIEW"
                ));
            }
            case "GESTOR" -> {
                permissions.addAll(Set.of(
                    "RESUMO:VIEW",
                    "VENDAS:VIEW",
                    "VENDAS:CREATE",
                    "PRODUTOS:VIEW",
                    "PRODUTOS:CREATE",
                    "PRODUTOS:DELETE",
                    "CLIENTES:VIEW",
                    "CLIENTES:CREATE",
                    "CLIENTES:DELETE",
                    "STOCK:VIEW",
                    "STOCK:CREATE",
                    "ARMAZENS:VIEW",
                    "TRANSFERENCIAS:VIEW",
                    "CATALOGOS:VIEW",
                    "CATALOGOS:CREATE",
                    "CATALOGOS:DELETE",
                    "CAIXA:VIEW",
                    "FINANCEIRO:VIEW",
                    "COMPRAS:VIEW",
                    "PRODUCAO:VIEW",
                    "RELATORIOS:VIEW"
                ));
            }
            case "CAIXA" -> {
                permissions.addAll(Set.of(
                    "RESUMO:VIEW",
                    "VENDAS:VIEW",
                    "VENDAS:CREATE",
                    "CLIENTES:VIEW",
                    "CAIXA:VIEW"
                ));
            }
            default -> {
                permissions.add("RESUMO:VIEW");
            }
        }
        return permissions;
    }

    private void ensureDefaultBranch() {
        boolean exists = branchRepository.findAll().stream().anyMatch(b -> "Matriz SGV".equalsIgnoreCase(b.getName()));
        if (!exists) {
            Branch branch = new Branch();
            branch.setName("Matriz SGV");
            branch.setNuit("123456789");
            branch.setAddress("Av. Julius Nyerere, Maputo");
            branch.setContact("+258 84 000 0000");
            branch.setHead(true);
            branchRepository.save(branch);
            logger.info("Created default branch: Matriz SGV");
        }
    }

    private void ensureDefaultCategory() {
        boolean exists = categoryRepository.findAll().stream().anyMatch(c -> "Produtos Gerais".equalsIgnoreCase(c.getName()));
        if (!exists) {
            Category category = new Category();
            category.setName("Produtos Gerais");
            categoryRepository.save(category);
        }
    }

    private void ensureDefaultProduct() {
        boolean exists = productRepository.findAll().stream().anyMatch(p -> "P001".equalsIgnoreCase(p.getCode()));
        if (!exists) {
            Category category = categoryRepository.findAll().stream().filter(c -> "Produtos Gerais".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            Branch branch = branchRepository.findAll().stream().filter(b -> "Matriz SGV".equalsIgnoreCase(b.getName())).findFirst().orElse(null);
            Product product = new Product();
            product.setCode("P001");
            product.setName("Água Mineral 500ml");
            product.setCategory(category);
            product.setService(false);
            product.setPriceCost(20.0);
            product.setPriceSale(30.0);
            product.setProfitMargin(50.0);
            product.setTaxRate(17.0);
            product.setIceRate(0.0);
            productRepository.save(product);

            if (branch != null) {
                stockBranchService.initializeStock(branch, product, BigDecimal.valueOf(100.0), BigDecimal.valueOf(10.0), "DATA_INIT", null);
            }
            logger.info("Created default product: P001 - Água Mineral 500ml");
        }

        boolean exists2 = productRepository.findAll().stream().anyMatch(p -> "P002".equalsIgnoreCase(p.getCode()));
        if (!exists2) {
            Category category = categoryRepository.findAll().stream().filter(c -> "Produtos Gerais".equalsIgnoreCase(c.getName())).findFirst().orElse(null);
            Branch branch = branchRepository.findAll().stream().filter(b -> "Matriz SGV".equalsIgnoreCase(b.getName())).findFirst().orElse(null);
            Product product = new Product();
            product.setCode("P002");
            product.setName("Refrigerante 2L");
            product.setCategory(category);
            product.setService(false);
            product.setPriceCost(40.0);
            product.setPriceSale(60.0);
            product.setProfitMargin(50.0);
            product.setTaxRate(17.0);
            product.setIceRate(0.0);
            productRepository.save(product);

            if (branch != null) {
                stockBranchService.initializeStock(branch, product, BigDecimal.valueOf(50.0), BigDecimal.valueOf(5.0), "DATA_INIT", null);
            }
            logger.info("Created default product: P002 - Refrigerante 2L");
        }
    }

    private void ensureDefaultCustomer() {
        boolean exists = customerRepository.findAll().stream().anyMatch(c -> "Cliente Demo".equalsIgnoreCase(c.getName()));
        if (!exists) {
            Customer customer = new Customer();
            customer.setCode("C001");
            customer.setName("Cliente Demo");
            customer.setNuit("999999999");
            customer.setType("B2C");
            customer.setCreditLimit(0.0);
            customer.setBalance(0.0);
            customerRepository.save(customer);
            logger.info("Created default customer: Cliente Demo");
        }

        boolean exists2 = customerRepository.findAll().stream().anyMatch(c -> "Cliente Empresa".equalsIgnoreCase(c.getName()));
        if (!exists2) {
            Customer corporate = new Customer();
            corporate.setCode("C002");
            corporate.setName("Cliente Empresa");
            corporate.setNuit("111222333");
            corporate.setType("B2B");
            corporate.setCreditLimit(10000.0);
            corporate.setBalance(0.0);
            customerRepository.save(corporate);
            logger.info("Created default customer: Cliente Empresa");
        }
    }

    private void ensureDefaultSales() {
        if (saleRepository.count() > 0) {
            return;
        }

        Branch branch = branchRepository.findAll().stream()
                .filter(b -> "Matriz SGV".equalsIgnoreCase(b.getName()))
                .findFirst().orElse(null);
        Customer customer = customerRepository.findAll().stream()
                .filter(c -> "Cliente Demo".equalsIgnoreCase(c.getName()))
                .findFirst().orElse(null);
        Product product = productRepository.findAll().stream()
                .filter(p -> "P001".equalsIgnoreCase(p.getCode()))
                .findFirst().orElse(null);

        if (branch == null || customer == null || product == null) {
            logger.warn("Skipping demo sale creation because required entities are missing");
            return;
        }

        Sale sale = new Sale();
        sale.setSeries("A");
        sale.setDocumentType("F");
        sale.setDocumentNumber(1L);
        sale.setDocumentYear(LocalDateTime.now().getYear());
        sale.setBranch(branch);
        sale.setCustomer(customer);
        sale.setCustomerName(customer.getName());
        sale.setCustomerNuit(customer.getNuit());
        sale.setPaymentMethod("DINHEIRO");
        sale.setState("EMITIDA");
        sale.setSubtotal(product.getPriceSale());
        sale.setTotalTax(product.getPriceSale() * (product.getTaxRate() / 100.0));
        sale.setTotalIce(product.getPriceSale() * (product.getIceRate() / 100.0));
        sale.setTotalDiscount(0.0);
        sale.setTotal(sale.getSubtotal() + sale.getTotalTax() + sale.getTotalIce());

        SaleItem item = new SaleItem();
        item.setSale(sale);
        item.setProduct(product);
        item.setProductCode(product.getCode());
        item.setUnit(product.getUnit() != null ? product.getUnit().getAbbreviation() : "");
        item.setDescription(product.getName());
        item.setQty(1.0);
        item.setUnitPrice(product.getPriceSale());
        item.setDiscount(0.0);
        item.setTaxRate(product.getTaxRate());
        item.setIceRate(product.getIceRate());
        item.setLineBase(product.getPriceSale());
        item.setLineTax(sale.getTotalTax());
        item.setLineIce(sale.getTotalIce());
        item.setLineTotal(sale.getTotal());
        sale.getItems().add(item);

        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setAmount(sale.getTotal());
        payment.setMethod("DINHEIRO");
        sale.getPayments().add(payment);

        saleRepository.save(sale);
        logger.info("Created default sale: F-1 for Cliente Demo");

        Customer corporate = customerRepository.findAll().stream()
                .filter(c -> "Cliente Empresa".equalsIgnoreCase(c.getName()))
                .findFirst().orElse(null);
        Product product2 = productRepository.findAll().stream()
                .filter(p -> "P002".equalsIgnoreCase(p.getCode()))
                .findFirst().orElse(null);

        if (corporate != null && product2 != null) {
            Sale sale2 = new Sale();
            sale2.setSeries("A");
            sale2.setDocumentType("F");
            sale2.setDocumentNumber(2L);
            sale2.setDocumentYear(LocalDateTime.now().getYear());
            sale2.setBranch(branch);
            sale2.setCustomer(corporate);
            sale2.setCustomerName(corporate.getName());
            sale2.setCustomerNuit(corporate.getNuit());
            sale2.setPaymentMethod("DINHEIRO");
            sale2.setState("EMITIDA");
            sale2.setSubtotal(product2.getPriceSale() * 2);
            sale2.setTotalTax(sale2.getSubtotal() * (product2.getTaxRate() / 100.0));
            sale2.setTotalIce(sale2.getSubtotal() * (product2.getIceRate() / 100.0));
            sale2.setTotalDiscount(0.0);
            sale2.setTotal(sale2.getSubtotal() + sale2.getTotalTax() + sale2.getTotalIce());

            SaleItem item2 = new SaleItem();
            item2.setSale(sale2);
            item2.setProduct(product2);
            item2.setProductCode(product2.getCode());
            item2.setUnit(product2.getUnit() != null ? product2.getUnit().getAbbreviation() : "");
            item2.setDescription(product2.getName());
            item2.setQty(2.0);
            item2.setUnitPrice(product2.getPriceSale());
            item2.setDiscount(0.0);
            item2.setTaxRate(product2.getTaxRate());
            item2.setIceRate(product2.getIceRate());
            item2.setLineBase(product2.getPriceSale() * 2);
            item2.setLineTax(sale2.getTotalTax());
            item2.setLineIce(sale2.getTotalIce());
            item2.setLineTotal(sale2.getTotal());
            sale2.getItems().add(item2);

            Payment payment2 = new Payment();
            payment2.setSale(sale2);
            payment2.setAmount(sale2.getTotal());
            payment2.setMethod("DINHEIRO");
            sale2.getPayments().add(payment2);

            saleRepository.save(sale2);
            logger.info("Created default sale: F-2 for Cliente Empresa");
        }
    }
}
