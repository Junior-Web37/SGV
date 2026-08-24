package com.sgv.config;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.StockBranchService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
    private final MetricUnitRepository metricUnitRepository;
    private final ProductRecipeRepository productRecipeRepository;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           BranchRepository branchRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           CustomerRepository customerRepository,
                           StockBranchService stockBranchService,
                           SaleRepository saleRepository,
                           MetricUnitRepository metricUnitRepository,
                           ProductRecipeRepository productRecipeRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRepository = branchRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.stockBranchService = stockBranchService;
        this.saleRepository = saleRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.productRecipeRepository = productRecipeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Iniciando verificação de dados padrão para o mercado moçambicano...");
        ensureRole("ADMIN", "Administrador do sistema");
        ensureRole("GESTOR", "Gestor / Gerente");
        ensureRole("CAIXA", "Operador de Caixa");
        ensureRole("CLIENTE", "Cliente (corrente/diverso)");
        ensureDefaultUnits();
        ensureDefaultBranch();
        ensureDefaultUsers();
        ensureDefaultCategories();
        ensureDefaultProductsAndRecipes();
        ensureDefaultCustomer();
        ensureDefaultSales();
        logger.info("Verificação de dados padrão concluída com sucesso.");
    }

    private void ensureDefaultUnits() {
        if (metricUnitRepository.count() == 0) {
            createUnit("UN", "Unidade");
            createUnit("KG", "Quilograma");
            createUnit("G", "Grama");
            createUnit("L", "Litro");
            createUnit("ML", "Mililitro");
            createUnit("CX", "Caixa");
            createUnit("SAC", "Saco");
            createUnit("PCT", "Pacote");
            createUnit("M", "Metro");
        }
    }

    private void createUnit(String abbr, String desc) {
        if (metricUnitRepository.findByAbbreviation(abbr).isEmpty()) {
            MetricUnit u = new MetricUnit();
            u.setAbbreviation(abbr);
            u.setDescription(desc);
            metricUnitRepository.save(u);
        }
    }

    private void ensureDefaultUsers() {
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            r.setDescription("Administrador do sistema");
            return roleRepository.save(r);
        });

        ensureUser("admin", "admin", "Administrador Principal", adminRole);
    }

    private void ensureUser(String username, String rawPassword, String fullName, Role role) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
            }
            if (user.getFullName() == null || user.getFullName().isBlank()) {
                user.setFullName(fullName);
            }
            user.setActive(true);
            user.setCanViewStats(true);
            if (!user.getRoles().contains(role)) user.getRoles().add(role);
            Branch branch = branchRepository.findAll().stream()
                    .filter(b -> "Sede Maputo".equalsIgnoreCase(b.getName()) || "Matriz SGV".equalsIgnoreCase(b.getName()))
                    .findFirst().orElse(null);
            if (branch != null && user.getBranch() == null) {
                user.setBranch(branch);
            }
            userRepository.save(user);
        } else {
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setFullName(fullName);
            user.setActive(true);
            user.setCanViewStats(true);
            user.getRoles().add(role);
            Branch branch = branchRepository.findAll().stream().findFirst().orElse(null);
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

        Set<String> updatedPerms = defaultPermissionsForRole(name);
        if (role.getPermissions() == null || role.getPermissions().isEmpty() || !role.getPermissions().containsAll(updatedPerms)) {
            if (role.getPermissions() != null) {
                role.getPermissions().addAll(updatedPerms);
            } else {
                role.setPermissions(updatedPerms);
            }
            roleRepository.save(role);
        }
    }

    private Set<String> defaultPermissionsForRole(String roleName) {
        String normalized = roleName != null ? roleName.toUpperCase() : "";
        Set<String> permissions = new HashSet<>();
        switch (normalized) {
            case "ADMIN" -> permissions.addAll(Set.of(
                "RESUMO:VIEW", "VENDAS:VIEW", "VENDAS:CREATE", "VENDAS:DELETE",
                "PRODUTOS:VIEW", "PRODUTOS:CREATE", "PRODUTOS:DELETE",
                "CLIENTES:VIEW", "CLIENTES:CREATE", "CLIENTES:DELETE",
                "STOCK:VIEW", "STOCK:CREATE", "ARMAZENS:VIEW", "ARMAZENS:CREATE", "ARMAZENS:DELETE",
                "TRANSFERENCIAS:VIEW", "TRANSFERENCIAS:CREATE",
                "CATALOGOS:VIEW", "CATALOGOS:CREATE", "CATALOGOS:DELETE",
                "CAIXA:VIEW", "FINANCEIRO:VIEW", "COMPRAS:VIEW", "COMPRAS:CREATE",
                "PRODUCAO:VIEW", "RELATORIOS:VIEW", "SISTEMA:VIEW", "SISTEMA:CREATE", "SISTEMA:DELETE"
            ));
            case "GESTOR" -> permissions.addAll(Set.of(
                "RESUMO:VIEW", "VENDAS:VIEW", "VENDAS:CREATE",
                "PRODUTOS:VIEW", "PRODUTOS:CREATE", "PRODUTOS:DELETE",
                "CLIENTES:VIEW", "CLIENTES:CREATE", "CLIENTES:DELETE",
                "STOCK:VIEW", "STOCK:CREATE", "ARMAZENS:VIEW", "ARMAZENS:CREATE",
                "TRANSFERENCIAS:VIEW", "TRANSFERENCIAS:CREATE",
                "CATALOGOS:VIEW", "CATALOGOS:CREATE", "CATALOGOS:DELETE",
                "CAIXA:VIEW", "FINANCEIRO:VIEW", "COMPRAS:VIEW", "COMPRAS:CREATE",
                "PRODUCAO:VIEW", "RELATORIOS:VIEW", "SISTEMA:VIEW"
            ));
            case "CAIXA" -> permissions.addAll(Set.of(
                "RESUMO:VIEW", "VENDAS:VIEW", "VENDAS:CREATE", "CLIENTES:VIEW", "CAIXA:VIEW"
            ));
            default -> permissions.add("RESUMO:VIEW");
        }
        return permissions;
    }

    private void ensureDefaultBranch() {
        if (branchRepository.count() == 0) {
            Branch branch = new Branch();
            branch.setName("Sede Maputo");
            branch.setNuit("400123456");
            branch.setAddress("Av. 24 de Julho, Maputo");
            branch.setContact("+258 84 123 4567");
            branch.setHead(true);
            branchRepository.save(branch);
            logger.info("Criada filial padrão: Sede Maputo");
        }
    }

    private void ensureDefaultCategories() {
        ensureCat("Mercearia & Bebidas");
        ensureCat("Padaria & Pastelaria");
        ensureCat("Matérias-Primas & Insumos");
    }

    private Category ensureCat(String name) {
        return categoryRepository.findByName(name).orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            return categoryRepository.save(c);
        });
    }

    private void ensureDefaultProductsAndRecipes() {
        Branch branch = branchRepository.findAll().stream().findFirst().orElse(null);
        MetricUnit un = metricUnitRepository.findByAbbreviation("UN").orElse(null);
        MetricUnit kg = metricUnitRepository.findByAbbreviation("KG").orElse(null);

        Category catMercearia = ensureCat("Mercearia & Bebidas");
        Category catPadaria = ensureCat("Padaria & Pastelaria");
        Category catInsumos = ensureCat("Matérias-Primas & Insumos");

        // 1. Água Mineral
        if (productRepository.findByCode("P001").isEmpty()) {
            Product p1 = new Product();
            p1.setCode("P001");
            p1.setName("Água Mineral 500ml");
            p1.setCategory(catMercearia);
            p1.setUnit(un);
            p1.setService(false);
            p1.setPriceCost(18.0);
            p1.setPriceSale(30.0);
            p1.setProfitMargin(66.6);
            p1.setTaxRate(16.0);
            productRepository.save(p1);
            if (branch != null) stockBranchService.initializeStock(branch, p1, BigDecimal.valueOf(100), BigDecimal.valueOf(10), BigDecimal.valueOf(500), "INIT", null);
        }

        // 2. Refrigerante
        if (productRepository.findByCode("P002").isEmpty()) {
            Product p2 = new Product();
            p2.setCode("P002");
            p2.setName("Refrigerante 2L");
            p2.setCategory(catMercearia);
            p2.setUnit(un);
            p2.setService(false);
            p2.setPriceCost(45.0);
            p2.setPriceSale(70.0);
            p2.setProfitMargin(55.5);
            p2.setTaxRate(16.0);
            productRepository.save(p2);
            if (branch != null) stockBranchService.initializeStock(branch, p2, BigDecimal.valueOf(50), BigDecimal.valueOf(5), BigDecimal.valueOf(200), "INIT", null);
        }

        // 3. Matérias-Primas para Padaria
        Product matFarinha = productRepository.findByCode("MAT01").orElseGet(() -> {
            Product m = new Product();
            m.setCode("MAT01");
            m.setName("Farinha de Trigo Especial");
            m.setCategory(catInsumos);
            m.setUnit(kg);
            m.setPriceCost(48.0);
            m.setPriceSale(60.0);
            m.setTaxRate(0.0); // Isento Art 9 CIVA
            Product saved = productRepository.save(m);
            if (branch != null) stockBranchService.initializeStock(branch, saved, BigDecimal.valueOf(200), BigDecimal.valueOf(20), BigDecimal.valueOf(1000), "INIT", null);
            return saved;
        });

        Product matFermento = productRepository.findByCode("MAT02").orElseGet(() -> {
            Product m = new Product();
            m.setCode("MAT02");
            m.setName("Fermento Biológico");
            m.setCategory(catInsumos);
            m.setUnit(kg);
            m.setPriceCost(120.0);
            m.setPriceSale(150.0);
            m.setTaxRate(16.0);
            Product saved = productRepository.save(m);
            if (branch != null) stockBranchService.initializeStock(branch, saved, BigDecimal.valueOf(20), BigDecimal.valueOf(2), BigDecimal.valueOf(100), "INIT", null);
            return saved;
        });

        // 4. Produto Acabado de Padaria: Pão Francês 50g
        Product pao = productRepository.findByCode("PAO01").orElseGet(() -> {
            Product p = new Product();
            p.setCode("PAO01");
            p.setName("Pão Francês 50g");
            p.setCategory(catPadaria);
            p.setUnit(un);
            p.setPriceCost(4.5);
            p.setPriceSale(10.0);
            p.setProfitMargin(122.2);
            p.setTaxRate(0.0); // Isento Artigo 9º CIVA (Bens de 1ª Necessidade)
            Product saved = productRepository.save(p);
            if (branch != null) stockBranchService.initializeStock(branch, saved, BigDecimal.valueOf(50), BigDecimal.valueOf(10), BigDecimal.valueOf(300), "INIT", null);
            return saved;
        });

        // 5. Ficha Técnica / Receita (BOM): 1 Pão = 0.035kg Farinha + 0.001kg Fermento
        if (productRecipeRepository.findByParentProductId(pao.getId()).isEmpty()) {
            ProductRecipe r1 = new ProductRecipe();
            r1.setParentProduct(pao);
            r1.setIngredientProduct(matFarinha);
            r1.setQuantityRequired(new BigDecimal("0.035"));
            r1.setUnit("KG");
            productRecipeRepository.save(r1);

            ProductRecipe r2 = new ProductRecipe();
            r2.setParentProduct(pao);
            r2.setIngredientProduct(matFermento);
            r2.setQuantityRequired(new BigDecimal("0.001"));
            r2.setUnit("KG");
            productRecipeRepository.save(r2);
            logger.info("Criada Ficha Técnica (BOM) padrão para Pão Francês 50g");
        }
    }

    private void ensureDefaultCustomer() {
        if (customerRepository.count() == 0) {
            Customer corporate = new Customer();
            corporate.setCode("CLI-0001");
            corporate.setName("Empresa Comercial de Maputo, Lda");
            corporate.setNuit("400987654");
            corporate.setType("GROSSO");
            corporate.setCreditLimit(50000.0);
            corporate.setBalance(0.0);
            corporate.setContact("+258 84 999 8888");
            corporate.setAddress("Av. Eduardo Mondlane, Maputo");
            customerRepository.save(corporate);
            logger.info("Criado cliente padrão: Empresa Comercial de Maputo, Lda");
        }
    }

    private void ensureDefaultSales() {
        if (saleRepository.count() > 0) {
            return;
        }

        Branch branch = branchRepository.findAll().stream().findFirst().orElse(null);
        Product product = productRepository.findByCode("P001").orElse(null);
        if (branch == null || product == null) return;

        Sale sale = new Sale();
        sale.setSeries("A");
        sale.setDocumentType("VENDA");
        sale.setDocumentNumber(1L);
        sale.setDocumentYear(LocalDateTime.now().getYear());
        sale.setBranch(branch);
        sale.setCustomerName("Consumidor Final");
        sale.setCustomerNuit("999999999");
        sale.setPaymentMethod("DINHEIRO");
        sale.setState("PAGO");
        sale.setSubtotal(product.getPriceSale());
        sale.setTotalTax(product.getPriceSale() * (product.getTaxRate() / 100.0));
        sale.setTotalIce(0.0);
        sale.setTotalDiscount(0.0);
        sale.setTotal(sale.getSubtotal() + sale.getTotalTax());
        sale.setPaidAmount(sale.getTotal());
        sale.setChangeAmount(0.0);

        SaleItem item = new SaleItem();
        item.setSale(sale);
        item.setProduct(product);
        item.setProductCode(product.getCode());
        item.setUnit(product.getUnit() != null ? product.getUnit().getAbbreviation() : "UN");
        item.setDescription(product.getName());
        item.setQty(1.0);
        item.setUnitPrice(product.getPriceSale());
        item.setDiscount(0.0);
        item.setTaxRate(product.getTaxRate());
        item.setLineBase(product.getPriceSale());
        item.setLineTax(sale.getTotalTax());
        item.setLineTotal(sale.getTotal());
        sale.getItems().add(item);

        saleRepository.save(sale);
        logger.info("Criada primeira venda padrão de demonstração.");
    }
}
