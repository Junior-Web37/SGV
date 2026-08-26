package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.4 — Concorrência: 2 (ou 4) threads a competir nos 4 cenários críticos.
 *
 * Nota: esta classe NÃO é @Transactional — os workers correm em transacções
 * próprias (o @Transactional do service) que COMMIT, pelo que o setup é feito
 * num TransactionTemplate próprio e os códigos de todos os registos são únicos
 * (prefixo CONC-) para não poluir as restantes classes de teste.
 *
 * Estado actual (antes das correções da auditoria):
 *  - twoCreditSales_sameCustomer_limitMustHold: FALHA (BUG-033 — sem lock no
 *    cliente: as 2 vendas leem o mesmo saldo, ambas passam no limite e
 *    comprometem o limite de crédito).
 *  - twoPayments_sameInvoice_cannotOverpay: FALHA (BUG-012/033 — sem lock na
 *    factura: os 2 pagamentos passam ambos na validação → pago em dobro).
 *  - twoSales_sameItemOnlyOneSucceeds: PASSA (BUG-033 a parte do stock:
 *    @Version em stock_branch existe — o 2º falha; só a UX do erro é má).
 *  - twoTransfers_sameBranch_numberUnique: FALHA (BUG-017 — numeração MAX+1
 *    sem lock e sem constraint único na tabela transfers).
 *
 * Os 3 testes FALHA são probabilísticos por natureza de race; a barreira
 * (CountDownLatch) maximiza a janela de colisão. Após a correção (locks +
 * constraint) passam sempre.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired private SaleService saleService;
    @Autowired private PaymentService paymentService;
    @Autowired private TransferService transferService;
    @Autowired private StockBranchService stockBranchService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private StockBranchRepository stockBranchRepository;
    @Autowired private TransferRepository transferRepository;
    @Autowired private PlatformTransactionManager txManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setupTx() {
        tx = new TransactionTemplate(txManager);
    }

    private void inTx(Runnable r) {
        tx.executeWithoutResult(s -> r.run());
    }

    private Branch branch(String name) {
        Branch b = new Branch();
        b.setName(name);
        b.setNuit("400400400");
        b.setAddress("Rua Conc");
        b.setContact("844444444");
        return branchRepository.save(b);
    }

    private Product product(String code, double price) {
        Category c = new Category();
        c.setName("Cat " + code);
        categoryRepository.save(c);
        MetricUnit u = new MetricUnit();
        u.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        u.setDescription("Unidade " + code);
        metricUnitRepository.save(u);
        Product p = new Product();
        p.setCode(code);
        p.setName("Produto " + code);
        p.setCategory(c);
        p.setUnit(u);
        p.setPriceCost(price / 2);
        p.setPriceSale(price);
        p.setService(false);
        p.setIsActive(true);
        return productRepository.save(p);
    }

    private Role role(String name, String... perms) {
        Role r = new Role();
        r.setName(name);
        r.setDescription("Role conc");
        for (String p : perms) r.getPermissions().add(p);
        return roleRepository.save(r);
    }

    private User user(String username, Role r, Branch b) {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash("x");
        u.setRoles(Set.of(r));
        if (b != null) u.setBranch(b);
        return userRepository.save(u);
    }

    /** Corre N tarefas em threads com barreira comum; devolve os erros. */
    private List<Throwable> race(int n, Runnable task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            for (int i = 0; i < n; i++) {
                pool.submit(() -> {
                    try {
                        start.await(30, TimeUnit.SECONDS);
                        task.run();
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(120, TimeUnit.SECONDS), "workers não terminaram a tempo");
        } finally {
            pool.shutdownNow();
        }
        return errors;
    }

    @Test
    void twoCreditSales_sameCustomer_limitMustHold() throws Exception {
        // BUG-033 — sem lock no cliente: 2 vendas de crédito simultâneas com o
        // mesmo cliente ultrapassam o limite e perdem actualizações.
        Branch b = branch("Filial CONC Limite");
        Product p = product("CONC-LIMIT", 600.0);
        inTx(() -> stockBranchService.initializeStock(b, p, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", null));
        Customer c = new Customer();
        c.setCode("CLI-CONC-LIMIT");
        c.setName("Cliente CONC Limite");
        c.setBalanceAmount(BigDecimal.ZERO);
        c.setCreditLimitAmount(new BigDecimal("1000.00"));
        inTx(() -> customerRepository.save(c));
        Role r = role("CONC-CREDIT", "VENDAS:CREATE");
        User u = user("conc-credit-op", r, b);

        List<Throwable> errors = race(2, () -> {
            Sale s = new Sale();
            s.setBranch(b);
            s.setCustomer(c);
            s.setDocumentType("VENDA");
            s.setSeries("A");
            s.setPaymentMethod("CREDITO");
            s.setState("EMITIDA");
            SaleItem item = new SaleItem();
            item.setProduct(p);
            item.setProductCode(p.getCode());
            item.setQtyAmount(BigDecimal.ONE);
            item.setUnitPriceAmount(new BigDecimal("600.00"));
            item.setLineBaseAmount(new BigDecimal("600.00"));
            item.setLineIceAmount(BigDecimal.ZERO);
            item.setLineTaxAmount(BigDecimal.ZERO);
            item.setLineTotalAmount(new BigDecimal("600.00"));
            item.setDescription(p.getName());
            s.setItems(List.of(item));
            try {
                saleService.processAndSave(s, u);
            } catch (Exception ignored) {
                // falha esperada quando o limite for correctamente imposto
            }
        });

        Customer after = customerRepository.findById(c.getId()).orElseThrow();
        // o PDF (dentro de processAndSave) torna a janela de race larga:
        // sem lock, as 2 vendas leem o saldo 0 antes de qualquer commit.
        assertTrue(after.getBalanceAmount().compareTo(new BigDecimal("1000.00")) <= 0,
                "BUG-033: limite de crédito comprometido por 2 vendas simultâneas — saldo "
                        + after.getBalanceAmount() + " > limite 1000 (erros nas threads: " + errors.size() + ")");
    }

    @Test
    void twoPayments_sameInvoice_cannotOverpay() throws Exception {
        // BUG-012/033 — sem lock na factura: 2 pagamentos simultâneos do
        // valor total passam ambos na validação (pago em dobro).
        Branch b = branch("Filial CONC Pag");
        Product p = product("CONC-PAY", 1000.0);
        inTx(() -> stockBranchService.initializeStock(b, p, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", null));
        Customer c = new Customer();
        c.setCode("CLI-CONC-PAY");
        c.setName("Cliente CONC Pag");
        c.setBalanceAmount(BigDecimal.ZERO);
        c.setCreditLimitAmount(new BigDecimal("10000.00"));
        inTx(() -> customerRepository.save(c));
        Role r = role("CONC-PAY-ROLE", "VENDAS:CREATE");
        User u = user("conc-pay-op", r, b);

        Sale sale = tx.execute(status -> {
            Sale s = new Sale();
            s.setBranch(b);
            s.setCustomer(c);
            s.setDocumentType("VENDA");
            s.setSeries("A");
            s.setPaymentMethod("CREDITO");
            s.setState("EMITIDA");
            SaleItem item = new SaleItem();
            item.setProduct(p);
            item.setProductCode(p.getCode());
            item.setQtyAmount(BigDecimal.ONE);
            item.setUnitPriceAmount(new BigDecimal("1000.00"));
            item.setLineBaseAmount(new BigDecimal("1000.00"));
            item.setLineIceAmount(BigDecimal.ZERO);
            item.setLineTaxAmount(BigDecimal.ZERO);
            item.setLineTotalAmount(new BigDecimal("1000.00"));
            item.setDescription(p.getName());
            s.setItems(List.of(item));
            try {
                return saleService.persistSaleTransaction(s, u);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertNotNull(sale);

        List<Throwable> errors = race(2, () -> {
            Payment pm = new Payment();
            pm.setSale(saleRepository.findById(sale.getId()).orElseThrow());
            pm.setAmountValue(new BigDecimal("1000.00"));
            pm.setMethod("DINHEIRO");
            try {
                paymentService.createPayment(pm, u);
            } catch (Exception ignored) {
                // falha esperada quando o lock impedir o segundo pagamento
            }
        });

        Sale after = saleRepository.findById(sale.getId()).orElseThrow();
        assertTrue(after.getPaidAmountValue().compareTo(new BigDecimal("1000.00")) <= 0,
                "BUG-012/033: factura paga em dobro — paidAmount " + after.getPaidAmountValue()
                        + " (erros nas threads: " + errors.size() + ")");
        assertTrue(after.getPaidAmountValue().compareTo(new BigDecimal("0.00")) > 0,
                "pelo menos um pagamento deve ter sido aceite");
    }

    @Test
    void twoSales_sameItemOnlyOneSucceeds() throws Exception {
        // Guarda o comportamento CORRECTO do @Version em stock_branch:
        // com 1 unidade em stock, 2 vendas simultâneas → exactamente 1
        // sucesso, stock final 0 (nunca negativo).
        Branch b = branch("Filial CONC Stock");
        Product p = product("CONC-STOCK", 50.0);
        inTx(() -> stockBranchService.initializeStock(b, p, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, "INIT", null));
        Customer c = new Customer();
        c.setCode("CLI-CONC-STOCK");
        c.setName("Cliente CONC Stock");
        c.setBalanceAmount(BigDecimal.ZERO);
        inTx(() -> customerRepository.save(c));
        Role r = role("CONC-STOCK-ROLE", "VENDAS:CREATE");
        User u = user("conc-stock-op", r, b);

        List<Throwable> errors = race(2, () -> {
            Sale s = new Sale();
            s.setBranch(b);
            s.setCustomer(c);
            s.setDocumentType("VENDA");
            s.setSeries("A");
            s.setPaymentMethod("CREDITO");
            s.setState("EMITIDA");
            SaleItem item = new SaleItem();
            item.setProduct(p);
            item.setProductCode(p.getCode());
            item.setQtyAmount(BigDecimal.ONE);
            item.setUnitPriceAmount(new BigDecimal("50.00"));
            item.setLineBaseAmount(new BigDecimal("50.00"));
            item.setLineIceAmount(BigDecimal.ZERO);
            item.setLineTaxAmount(BigDecimal.ZERO);
            item.setLineTotalAmount(new BigDecimal("50.00"));
            item.setDescription(p.getName());
            s.setItems(List.of(item));
            try {
                saleService.processAndSave(s, u);
            } catch (Exception ignored) {
                // o 2º vendedor deve falhar (conflito optimista / stock)
            }
        });

        StockBranch sb = stockBranchRepository.findByProductAndBranch(p, b).orElseThrow();
        long salesCount = saleRepository.findAllByCustomerId(c.getId()).stream()
                .filter(s -> "VENDA".equals(s.getDocumentType()) && !Boolean.TRUE.equals(s.getDemoFlag()))
                .count();
        assertEquals(0, BigDecimal.ZERO.compareTo(sb.getStockCurrentAmount()),
                "stock nunca pode ficar negativo: " + sb.getStockCurrentAmount());
        assertEquals(1, salesCount,
                "exatamente 1 das 2 vendas simultâneas deve ter sucesso (erros: " + errors.size() + ")");
    }

    @Test
    void twoTransfers_sameBranch_numberUnique() throws Exception {
        // BUG-017 — numeração MAX+1 sem lock e sem constraint único:
        // 2 transferências simultâneas da mesma filial podem ficar com o
        // mesmo número de documento.
        Branch from = branch("Filial CONC TR From");
        Branch to = branch("Filial CONC TR To");
        Product p = product("CONC-TR", 10.0);
        Role r = role("CONC-TR-ROLE", "TRANSFERENCIAS:CREATE");
        User u = user("conc-tr-op", r, from);

        race(2, () -> {
            Transfer t = new Transfer();
            t.setSourceBranch(from);
            t.setDestinationBranch(to);
            t.setRequestedBy(u);
            TransferItem item = new TransferItem();
            item.setProduct(p);
            item.setQuantityAmount(BigDecimal.valueOf(2));
            transferService.create(t, List.of(item));
        });

        List<Transfer> created = transferRepository.findAll().stream()
                .filter(t -> from.getId().equals(t.getSourceBranch() != null ? t.getSourceBranch().getId() : -1L))
                .toList();
        long distinctNumbers = created.stream().map(Transfer::getDocumentNumber).distinct().count();
        assertTrue(distinctNumbers == created.size() && created.size() >= 2,
                "BUG-017: números de transferência duplicados sob concorrência — "
                        + created.size() + " transferências, " + distinctNumbers + " números distintos");
    }
}
