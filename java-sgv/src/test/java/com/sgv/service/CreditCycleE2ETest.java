package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.3 — E2E crédito completo: venda → parcial → total → dívida 0;
 * NC sobre crédito → saldo abate; anulação → caixa OUT existe.
 *
 * Estado actual (antes das correções da auditoria):
 *  - creditFullCycle_debtReachesZero: PASSA.
 *  - annulCashSale_withOpenSession_registersRefund: PASSA (o estorno funciona
 *    quando o anulador tem turno aberto).
 *  - annulCashSale_withoutOpenSession_mustFailAtomically: FALHA (BUG-011 —
 *    estorno engolido em `catch (Exception ignored)`: a anulação completa com
 *    stock devolvido e caixa não debitada).
 *  - creditNoteOnCreditSale_decreasesCustomerBalance: FALHA (BUG-010 — a NC
 *    devolve stock mas nunca abate `customers.balance`).
 */
// Não é @Transactional de propósito: annulCashSale_withoutOpenSession_mustFailAtomically
// captura a exception da anulação e continua (verifica rollback/estorno pós-falha).
// Numa classe @Transactional essa exception marcava a transação partilhada como
// rollback-only e o commit final do framework lançava UnexpectedRollbackException,
// escondendo o resultado real do teste (problema agravado após a correção do BUG-011,
// que faz annulSale lançar em vez de engolir o erro). Cada chamada de
// service/repositório commita sozinha; os códigos de entidade são únicos por teste
// (tags "Ciclo"/"AnulOK"/"AnulSil"/"NC").
@SpringBootTest
@ActiveProfiles("test")
class CreditCycleE2ETest {

    @Autowired private SaleService saleService;
    @Autowired private PaymentService paymentService;
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
    @Autowired private StockBranchRepository stockBranchRepository;
    @Autowired private CashMovementRepository cashMovementRepository;

    private Branch b;
    private User op;
    private User operatorNoSession;
    private Product p;
    private Customer c;

    private void setup(String tag) {
        Role r = new Role();
        r.setName("CC-" + tag.toUpperCase());
        r.setDescription("Role ciclo crédito");
        r.getPermissions().add("VENDAS:CREATE");
        r.getPermissions().add("VENDAS:DELETE");
        r.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(r);

        b = new Branch();
        b.setName("Filial CC " + tag);
        b.setNuit("300300" + tag.length());
        b.setAddress("Rua CC " + tag);
        b.setContact("843333333");
        branchRepository.save(b);

        op = new User();
        op.setUsername("cc-" + tag.toLowerCase().replace(" ", "-") + "-op");
        op.setPasswordHash("x");
        op.setRoles(java.util.Set.of(r));
        op.setBranch(b);
        userRepository.save(op);

        operatorNoSession = new User();
        operatorNoSession.setUsername("cc-" + tag.toLowerCase().replace(" ", "-") + "-op2");
        operatorNoSession.setPasswordHash("x");
        operatorNoSession.setRoles(java.util.Set.of(r));
        operatorNoSession.setBranch(b);
        userRepository.save(operatorNoSession);

        Category cat = new Category();
        cat.setName("Cat CC " + tag);
        categoryRepository.save(cat);
        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        unit.setDescription("Unidade CC " + tag);
        metricUnitRepository.save(unit);
        p = new Product();
        p.setCode("CC" + tag.toUpperCase());
        p.setName("Produto CC " + tag);
        p.setCategory(cat);
        p.setUnit(unit);
        p.setPriceCost(10.0);
        p.setPriceSale(100.0);
        p.setService(false);
        p.setIsActive(true);
        productRepository.save(p);

        stockBranchService.initializeStock(b, p, new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", op);
        c = new Customer();
        c.setCode("CLI-CC-" + tag.toUpperCase());
        c.setName("Cliente CC " + tag);
        c.setBalanceAmount(BigDecimal.ZERO);
        c.setCreditLimitAmount(new BigDecimal("1000.00"));
        customerRepository.save(c);
    }

    private Sale creditSale(double unitPrice, int qty) throws Exception {
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
        item.setQtyAmount(BigDecimal.valueOf(qty));
        item.setUnitPriceAmount(BigDecimal.valueOf(unitPrice));
        item.setLineBaseAmount(BigDecimal.valueOf(unitPrice * qty));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(BigDecimal.valueOf(unitPrice * qty));
        item.setDescription(p.getName());
        s.setItems(List.of(item));
        // processAndSave devolve o File do documento; a Sale persistida é
        // devolvida por id (o id é atribuído à instância passada).
        saleService.processAndSave(s, op);
        return saleRepository.findById(s.getId()).orElseThrow();
    }

    private void pay(Sale sale, String amount) throws Exception {
        Payment pm = new Payment();
        pm.setSale(saleRepository.findById(sale.getId()).orElseThrow());
        pm.setAmountValue(new BigDecimal(amount));
        pm.setMethod("DINHEIRO");
        paymentService.createPayment(pm, op);
    }

    @Test
    void creditFullCycle_debtReachesZero() throws Exception {
        setup("Ciclo");
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale sale = creditSale(100.0, 1);
        assertEquals(0, new BigDecimal("100.00").compareTo(
                customerRepository.findById(c.getId()).orElseThrow().getBalanceAmount()),
                "venda a crédito deve debitar a conta corrente");

        pay(sale, "40.00");
        Sale afterPartial = saleRepository.findById(sale.getId()).orElseThrow();
        assertEquals("PAGO_PARCIAL", afterPartial.getState());
        assertEquals(0, new BigDecimal("60.00").compareTo(
                customerRepository.findById(c.getId()).orElseThrow().getBalanceAmount()));

        pay(afterPartial, "60.00");
        Sale afterFull = saleRepository.findById(sale.getId()).orElseThrow();
        assertEquals("PAGO", afterFull.getState());
        assertEquals(0, new BigDecimal("0.00").compareTo(
                customerRepository.findById(c.getId()).orElseThrow().getBalanceAmount()),
                "dívida deve chegar a 0 após liquidação total");
        assertTrue(saleRepository.findPendingSales().stream()
                        .noneMatch(s -> s.getId().equals(afterFull.getId())),
                "venda liquidada não deve aparecer nas pendências");
    }

    @Test
    void annulCashSale_withOpenSession_registersRefund() throws Exception {
        setup("AnulOK");
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale s = new Sale();
        s.setBranch(b);
        s.setCustomer(c);
        s.setDocumentType("VENDA");
        s.setSeries("A");
        s.setPaymentMethod("DINHEIRO");
        s.setState("EMITIDA");
        SaleItem item = new SaleItem();
        item.setProduct(p);
        item.setProductCode(p.getCode());
        item.setQtyAmount(BigDecimal.ONE);
        item.setUnitPriceAmount(new BigDecimal("100.00"));
        item.setLineBaseAmount(new BigDecimal("100.00"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(new BigDecimal("100.00"));
        item.setDescription(p.getName());
        s.setItems(List.of(item));
        saleService.processAndSave(s, op);

        saleService.annulSale(saleRepository.findById(s.getId()).orElseThrow(), "Devolução ao cliente", op);

        Sale annulled = saleRepository.findById(s.getId()).orElseThrow();
        assertEquals("ANULADA", annulled.getState());

        CashSession session = cashSessionService.getOpenSession(op).orElseThrow();
        CashMovement refund = cashMovementRepository.findBySessionOrderByCreatedAtAsc(session).stream()
                .filter(m -> "OUT".equals(m.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Estorno OUT não existe no turno do operador"));
        assertEquals(0, new BigDecimal("100.00").compareTo(refund.getAmount()));
        assertEquals(OperationKind.REFUND, refund.getOperationKind());
    }

    @Test
    void annulCashSale_withoutOpenSession_mustFailAtomically() throws Exception {
        // BUG-011 — FALHA AGORA: anulação por utilizador sem turno aberto
        // completa-se (ANULADA + stock devolvido) com o estorno de caixa
        // engolido em silêncio. Comportamento correcto: a anulação deve
        // falhar atomicamente (rollback) — ou deixar estorno — nunca deixar
        // o caixa sem o OUT correspondente.
        setup("AnulSil");
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale s = new Sale();
        s.setBranch(b);
        s.setCustomer(c);
        s.setDocumentType("VENDA");
        s.setSeries("A");
        s.setPaymentMethod("DINHEIRO");
        s.setState("EMITIDA");
        SaleItem item = new SaleItem();
        item.setProduct(p);
        item.setProductCode(p.getCode());
        item.setQtyAmount(BigDecimal.ONE);
        item.setUnitPriceAmount(new BigDecimal("100.00"));
        item.setLineBaseAmount(new BigDecimal("100.00"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(new BigDecimal("100.00"));
        item.setDescription(p.getName());
        s.setItems(List.of(item));
        saleService.processAndSave(s, op);

        // Anulador SEM turno aberto (operadorNoSession)
        boolean annulmentFailed = false;
        try {
            saleService.annulSale(saleRepository.findById(s.getId()).orElseThrow(), "Devolução", operatorNoSession);
        } catch (RuntimeException e) {
            annulmentFailed = true;
        }

        Sale after = saleRepository.findById(s.getId()).orElseThrow();
        boolean refundExists = cashMovementRepository.findAll().stream()
                .anyMatch(m -> "OUT".equals(m.getType())
                        && m.getDescription() != null
                        && m.getDescription().contains("Estorno"));

        if (annulmentFailed) {
            // rollback total: venda mantém-se viva
            assertNotEquals("ANULADA", after.getState(),
                    "se a anulação falhou, a venda não pode ficar ANULADA (rollback)");
        } else {
            assertTrue(refundExists,
                    "BUG-011: anulação completou sem estorno de caixa — o caixa ficou com o dinheiro da venda");
        }
    }

    @Test
    void creditNoteOnCreditSale_decreasesCustomerBalance() throws Exception {
        // BUG-010 — FALHA AGORA: NC sobre venda a crédito devolve stock mas
        // nunca abate a dívida (customers.balance continua integral).
        setup("NC");
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale sale = creditSale(100.0, 1);
        BigDecimal balanceBefore = customerRepository.findById(c.getId()).orElseThrow().getBalanceAmount();
        assertEquals(0, new BigDecimal("100.00").compareTo(balanceBefore));

        StockBranch before = stockBranchRepository.findByProductAndBranch(p, b).orElseThrow();
        BigDecimal stockBefore = before.getStockCurrentAmount();

        saleService.createCreditNote(saleRepository.findById(sale.getId()).orElseThrow(),
                "Produto defeituoso", op);

        // stock devolvido (parte que funciona)
        StockBranch stockAfter = stockBranchRepository.findByProductAndBranch(p, b).orElseThrow();
        assertEquals(0, stockBefore.add(BigDecimal.ONE).compareTo(stockAfter.getStockCurrentAmount()),
                "NC devolve 1 unidade ao stock (" + stockBefore + " → " + stockAfter.getStockCurrentAmount() + ")");

        // dívida abatida (parte que FALHA hoje)
        BigDecimal balanceAfter = customerRepository.findById(c.getId()).orElseThrow().getBalanceAmount();
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceAfter),
                "BUG-010: NC sobre venda a crédito deve abater a dívida (saldo " + balanceAfter + ")");
    }
}
