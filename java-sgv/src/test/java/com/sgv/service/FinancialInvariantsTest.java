package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.1 — Invariantes financeiras (teste de BD por operação).
 *
 * Invariantes cobertas:
 *  1. Sessão de caixa fechada: initialValue + Σ(IN) − Σ(OUT) == systemValue.
 *  2. Σ(payments de uma venda) == sales.paidAmount.
 *  3. stock_branch.current == Σ(stock_movements.qty) para (filial, artigo).
 *  4. customers.balance == extrato da conta corrente (último running balance).
 *
 * Estado actual (antes das correções da auditoria):
 *  - 1..3: PASSA (comportamento correcto no fluxo normal).
 *  - 4:    FALHA quando a operação não está representada no extrato
 *          (pagamento sem factura — BUG-014/BUG-005).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialInvariantsTest {

    @Autowired private SaleService saleService;
    @Autowired private PaymentService paymentService;
    @Autowired private CustomerAccountService customerAccountService;
    @Autowired private CashSessionService cashSessionService;
    @Autowired private StockBranchService stockBranchService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private StockBranchRepository stockBranchRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    private Role role(String name, String... perms) {
        Role r = new Role();
        r.setName(name);
        r.setDescription("Role de teste");
        for (String p : perms) r.getPermissions().add(p);
        return roleRepository.save(r);
    }

    private User user(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("x");
        u.setRoles(java.util.Set.of(role));
        return userRepository.save(u);
    }

    private Branch branch(String name) {
        Branch b = new Branch();
        b.setName(name);
        b.setNuit("100100100");
        b.setAddress("Rua Teste");
        b.setContact("840000000");
        return branchRepository.save(b);
    }

    private Product product(String code, String name, double cost, double price) {
        Category c = new Category();
        c.setName("Cat " + code);
        categoryRepository.save(c);
        MetricUnit u = new MetricUnit();
        u.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        u.setDescription("Unidade invariante");
        metricUnitRepository.save(u);
        Product p = new Product();
        p.setCode(code);
        p.setName(name);
        p.setCategory(c);
        p.setUnit(u);
        p.setPriceCost(cost);
        p.setPriceSale(price);
        p.setService(false);
        p.setIsActive(true);
        return productRepository.save(p);
    }

    private Customer customer(String code, String name, BigDecimal limit) {
        Customer c = new Customer();
        c.setCode(code);
        c.setName(name);
        c.setBalanceAmount(BigDecimal.ZERO);
        if (limit != null) c.setCreditLimitAmount(limit);
        return customerRepository.save(c);
    }

    private Sale saleWith(Branch b, Customer c, String docType, String method, double unitPrice, int qty) {
        Product p = productRepository.findAll().stream()
                .filter(x -> x.getCode().equals("INV-" + b.getId()))
                .findFirst().orElseThrow();
        Sale s = new Sale();
        s.setBranch(b);
        s.setCustomer(c);
        s.setDocumentType(docType);
        s.setSeries("A");
        s.setPaymentMethod(method);
        s.setState("EMITIDA");
        SaleItem item = new SaleItem();
        item.setProduct(p);
        item.setProductCode(p.getCode());
        item.setQtyAmount(BigDecimal.valueOf(qty));
        item.setUnitPriceAmount(BigDecimal.valueOf(unitPrice));
        item.setLineBaseAmount(BigDecimal.valueOf(unitPrice * qty));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(BigDecimal.valueOf(unitPrice * qty));
        item.setDescription(p.getName());
        s.setItems(List.of(item));
        return s;
    }

    @Test
    void closedSession_systemValueEqualsInitialPlusInMinusOut() {
        Role r = role("INV-CASH", "CAIXA:VIEW");
        User u = user("inv-cash-op", r);
        branchRepository.save(branch("Filial Inv Caixa"));
        u.setBranch(branchRepository.findByNameIgnoreCase("Filial Inv Caixa").orElseThrow());
        userRepository.save(u);

        cashSessionService.openSession(u, new BigDecimal("100.00"));
        cashSessionService.registerMovement(u, "IN", new BigDecimal("50.00"), "entrada de teste", "REFORCO");
        cashSessionService.registerMovement(u, "OUT", new BigDecimal("20.00"), "saída de teste", "SANGRIA");

        CashSession closed = cashSessionService.closeSession(u, new BigDecimal("130.00"), "fecho invariante");

        // invariante: inicial + IN − OUT == systemValue
        assertEquals(0, new BigDecimal("130.00").compareTo(closed.getSystemValue()),
                "systemValue deve ser inicial(100) + IN(50) − OUT(20) = 130");
    }

    @Test
    void payments_sumMatchesPaidAmount() throws Exception {
        Role r = role("INV-PAY", "VENDAS:CREATE", "CAIXA:VIEW");
        User u = user("inv-pay-op", r);
        Branch b = branch("Filial Inv Pag");
        u.setBranch(b);
        userRepository.save(u);
        Product p = product("INV-" + b.getId(), "Produto Inv Pag", 5.0, 100.0);
        stockBranchService.initializeStock(b, p, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", u);
        Customer c = customer("CLI-INV-PAY", "Cliente Inv Pag", BigDecimal.ZERO);
        cashSessionService.openSession(u, BigDecimal.ZERO);

        Sale sale = saleWith(b, c, "VENDA", "CREDITO", 100.0, 1);
        saleService.processAndSave(sale, u);

        Payment p1 = new Payment();
        p1.setSale(sale);
        p1.setAmountValue(new BigDecimal("40.00"));
        p1.setMethod("DINHEIRO");
        paymentService.createPayment(p1, u);

        Payment p2 = new Payment();
        p2.setSale(saleRepository.findById(sale.getId()).orElseThrow());
        p2.setAmountValue(new BigDecimal("60.00"));
        p2.setMethod("DINHEIRO");
        paymentService.createPayment(p2, u);

        Sale saved = saleRepository.findById(sale.getId()).orElseThrow();
        BigDecimal sumPayments = paymentRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 1000))
                .getContent().stream()
                .filter(x -> x.getSale() != null && x.getSale().getId() != null && x.getSale().getId().equals(saved.getId()))
                .map(Payment::getAmountValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, new BigDecimal("100.00").compareTo(saved.getPaidAmountValue()),
                "paidAmount deve ser a soma dos pagamentos (40+60)");
        assertEquals(0, sumPayments.compareTo(saved.getPaidAmountValue()),
                "Σ(payments) deve ser igual a sales.paidAmount");
        assertEquals("PAGO", saved.getState());
    }

    @Test
    void stock_currentEqualsSumOfMovements() throws Exception {
        Role r = role("INV-STOCK", "VENDAS:CREATE", "CAIXA:VIEW");
        User u = user("inv-stock-op", r);
        Branch b = branch("Filial Inv Stock");
        u.setBranch(b);
        userRepository.save(u);
        Product p = product("INV-" + b.getId(), "Produto Inv Stock", 5.0, 20.0);
        stockBranchService.initializeStock(b, p, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", u);
        Customer c = customer("CLI-INV-STOCK", "Cliente Inv Stock", BigDecimal.ZERO);
        cashSessionService.openSession(u, BigDecimal.ZERO);

        Sale sale = saleWith(b, c, "VENDA", "DINHEIRO", 20.0, 3);
        saleService.processAndSave(sale, u);

        StockBranch sb = stockBranchRepository.findByProductAndBranch(p, b).orElseThrow();
        List<StockMovement> movements = stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(p.getId()).stream()
                .filter(m -> m.getBranch() != null && b.getId().equals(m.getBranch().getId()))
                .toList();
        BigDecimal sumMovements = movements.stream()
                .map(StockMovement::getQtyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, new BigDecimal("7.00").compareTo(sb.getStockCurrentAmount()),
                "stock actual deve ser 10 (init) − 3 (venda) = 7");
        assertEquals(0, sumMovements.compareTo(sb.getStockCurrentAmount()),
                "stock_branch.current deve ser igual a Σ(stock_movements.qty) — kardex reconciliado");
    }

    @Test
    void customer_balanceReconstructedFromStatement_onHappyPath() throws Exception {
        Role r = role("INV-CC", "VENDAS:CREATE", "CAIXA:VIEW");
        User u = user("inv-cc-op", r);
        Branch b = branch("Filial Inv CC");
        u.setBranch(b);
        userRepository.save(u);
        Product p = product("INV-" + b.getId(), "Produto Inv CC", 5.0, 100.0);
        stockBranchService.initializeStock(b, p, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", u);
        Customer c = customer("CLI-INV-CC", "Cliente Inv CC", new BigDecimal("1000.00"));
        cashSessionService.openSession(u, BigDecimal.ZERO);

        Sale sale = saleWith(b, c, "VENDA", "CREDITO", 100.0, 1);
        saleService.processAndSave(sale, u);

        Payment pm = new Payment();
        pm.setSale(saleRepository.findById(sale.getId()).orElseThrow());
        pm.setAmountValue(new BigDecimal("40.00"));
        pm.setMethod("DINHEIRO");
        paymentService.createPayment(pm, u);

        Customer refreshed = customerRepository.findById(c.getId()).orElseThrow();
        List<CustomerAccountService.CustomerStatementEntry> statement =
                customerAccountService.getCustomerStatement(c.getId());
        assertFalse(statement.isEmpty(), "extrato não pode estar vazio");
        BigDecimal statementFinal = statement.get(statement.size() - 1).getRunningBalance();

        // caminho feliz (venda crédito + pagamento com factura): extrato == saldo
        assertEquals(0, refreshed.getBalanceAmount().compareTo(statementFinal),
                "extrato final deve reconstruir customers.balance no caminho feliz");
        assertEquals(0, new BigDecimal("60.00").compareTo(refreshed.getBalanceAmount()));
    }

    @Test
    void customer_receiptWithoutSale_statementStillMatchesBalance() {
        // BUG-014/BUG-005 — FALHA AGORA:
        // recordReceipt sem factura abate o saldo real mas o extrato (derivado só de
        // sales) não mostra o pagamento → saldo real ≠ extrato.
        // Depois do ledger de conta corrente (A5/BUG-005) deve passar.
        Role r = role("INV-CC2", "VENDAS:CREATE", "CAIXA:VIEW");
        User u = user("inv-cc2-op", r);
        Customer c = customer("CLI-INV-CC2", "Cliente Inv CC2", BigDecimal.ZERO);

        customerAccountService.recordReceipt(c, null, new BigDecimal("25.00"), "DINHEIRO", u);

        Customer refreshed = customerRepository.findById(c.getId()).orElseThrow();
        List<CustomerAccountService.CustomerStatementEntry> statement =
                customerAccountService.getCustomerStatement(c.getId());
        BigDecimal statementFinal = statement.isEmpty()
                ? BigDecimal.ZERO
                : statement.get(statement.size() - 1).getRunningBalance();

        assertEquals(0, refreshed.getBalanceAmount().compareTo(statementFinal),
                "pagamento sem factura deve aparecer no extrato (saldo "
                        + refreshed.getBalanceAmount() + " vs extrato " + statementFinal + ")");
    }
}
