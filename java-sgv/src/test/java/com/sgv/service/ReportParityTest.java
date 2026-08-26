package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.6 — Paridade de relatórios: para o mesmo período e dataset conhecido,
 * dashboard (KPI) == mapa de vendas == apuramento de IVA == soma das vendas
 * reportáveis; e o extrato do cliente termina no saldo real.
 *
 * Universo "reportável" (correção do BUG-004/028):
 *   documentType IN (VENDA, FACTURA, NC) AND state <> ANULADA AND demoFlag = false
 *   (cotações/encomendas são documentos pré-venda; a NC entra com total
 *   negativo — abate o apuramento; o demo nunca conta).
 *
 * Estado actual (antes das correções da auditoria):
 *  - samePeriod_allViewsSameUniverse: FALHA (BUG-004 — o KPI do dashboard
 *    inclui cotações e vendas demo; o ReportService inclui cotações; o mapa
 *    inclui ANULADA+demo+cotações → três números diferentes).
 *  - customerStatement_finalEqualsRealBalance: FALHA (BUG-005/010/014 —
 *    NC não abate o saldo real; pagamento sem factura abate o saldo mas não
 *    aparece no extrato → o extrato nunca reconstrói o saldo).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportParityTest {

    /**
     * Predicate partilhado do universo reportável (o que o sistema DEVE usar —
     * correção do BUG-004/028): documentos de venda/factura/NC, não anulados,
     * não demo. A NC entra com total negativo (abate o apuramento).
     */
    private static boolean isReportable(Sale s) {
        String dt = s.getDocumentType() != null ? s.getDocumentType() : "";
        return ("VENDA".equals(dt) || "FACTURA".equals(dt) || "NC".equals(dt))
                && !"ANULADA".equals(s.getState())
                && !Boolean.TRUE.equals(s.getDemoFlag());
    }

    @Autowired private SaleService saleService;
    @Autowired private CustomerAccountService customerAccountService;
    @Autowired private CashSessionService cashSessionService;
    @Autowired private StockBranchService stockBranchService;
    @Autowired private ReportService reportService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private SaleRepository saleRepository;

    private Branch b;
    private User op;
    private Product p;
    private Customer c;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;

    private void setup() {
        Role r = new Role();
        r.setName("PARITY");
        r.setDescription("Role paridade");
        r.getPermissions().add("VENDAS:CREATE");
        r.getPermissions().add("VENDAS:DELETE");
        r.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(r);

        b = new Branch();
        b.setName("Filial Paridade");
        b.setNuit("600600600");
        b.setAddress("Rua Paridade");
        b.setContact("846666666");
        branchRepository.save(b);

        op = new User();
        op.setUsername("parity-op");
        op.setPasswordHash("x");
        op.setRoles(java.util.Set.of(r));
        op.setBranch(b);
        userRepository.save(op);

        Category cat = new Category();
        cat.setName("Cat Paridade");
        categoryRepository.save(cat);
        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        unit.setDescription("Unidade paridade");
        metricUnitRepository.save(unit);
        p = new Product();
        p.setCode("PARITY");
        p.setName("Produto Paridade");
        p.setCategory(cat);
        p.setUnit(unit);
        p.setPriceCost(10.0);
        p.setPriceSale(100.0);
        p.setService(false);
        p.setIsActive(true);
        productRepository.save(p);

        stockBranchService.initializeStock(b, p, new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", op);
        c = new Customer();
        c.setCode("CLI-PARITY");
        c.setName("Cliente Paridade");
        c.setBalanceAmount(BigDecimal.ZERO);
        c.setCreditLimitAmount(new BigDecimal("10000.00"));
        customerRepository.save(c);

        cashSessionService.openSession(op, BigDecimal.ZERO);
        windowStart = LocalDateTime.now().minusMinutes(2);
        windowEnd = LocalDateTime.now().plusMinutes(2);
    }

    private Sale sale(String docType, String method, double price, int qty, boolean demo, boolean annul) throws Exception {
        Sale s = new Sale();
        s.setBranch(b);
        s.setCustomer(c);
        s.setDocumentType(docType);
        s.setSeries("A");
        s.setPaymentMethod(method);
        s.setState("EMITIDA");
        if (demo) s.setDemoFlag(true);
        SaleItem item = new SaleItem();
        item.setProduct(p);
        item.setProductCode(p.getCode());
        item.setQtyAmount(BigDecimal.valueOf(qty));
        item.setUnitPriceAmount(BigDecimal.valueOf(price));
        item.setLineBaseAmount(BigDecimal.valueOf(price * qty));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(BigDecimal.valueOf(price * qty));
        item.setDescription(p.getName());
        s.setItems(List.of(item));
        // processAndSave devolve o File do documento; a Sale persistida é
        // devolvida por id (o id é atribuído à instância passada).
        saleService.processAndSave(s, op);
        Sale saved = saleRepository.findById(s.getId()).orElseThrow();
        if (annul) {
            saleService.annulSale(saleRepository.findById(saved.getId()).orElseThrow(), "Anulada p/ teste", op);
        }
        return saved;
    }

    @Test
    void samePeriod_allViewsSameUniverse() throws Exception {
        setup();

        // Dataset conhecido (tudo na filial "Filial Paridade", na janela actual):
        //  VENDA cash 100        → reportável
        //  VENDA crédito 200     → reportável
        //  COTAÇÃO 500           → NÃO reportável (pré-venda)
        //  VENDA anulada 300     → NÃO reportável
        //  VENDA demo 100        → NÃO reportável
        BigDecimal expected = new BigDecimal("300.00");

        sale("VENDA", "DINHEIRO", 100.0, 1, false, false);
        sale("VENDA", "CREDITO", 100.0, 2, false, false);
        sale("COTACAO", "CREDITO", 500.0, 1, false, false);
        sale("VENDA", "DINHEIRO", 300.0, 1, false, true);
        sale("VENDA", "DINHEIRO", 100.0, 1, true, false);

        // 1) KPI do dashboard (query sumTotalByDateRangeAndBranch)
        BigDecimal kpiSum = saleRepository.sumTotalByDateRangeAndBranch(windowStart, windowEnd, b.getId());
        assertEquals(0, expected.compareTo(kpiSum),
                "BUG-004: KPI do dashboard deve somar só o universo reportável — "
                        + "esperado " + expected + ", obtido " + kpiSum
                        + " (o filtro state<>ANULADA inclui cotações e vendas demo)");

        // 2) ReportService.period() — é global (todas as filiais), pelo que o
        //    valor esperado é medido directamente na BD com o mesmo predicado
        //    reportável: period() deve devolver exactamente essa soma.
        Map<String, Object> before = reportService.period(windowStart.toLocalDate(), windowEnd.toLocalDate());
        BigDecimal reportableTotal = saleRepository.findAll().stream()
                .filter(isReportable)
                .map(Sale::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double periodTotal = ((Number) before.get("totalAmount")).doubleValue();
        BigDecimal periodTotalBd = BigDecimal.valueOf(periodTotal).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal reportableBd = reportableTotal.setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(0, periodTotalBd.compareTo(reportableBd),
                "BUG-004: ReportService.period deve usar o mesmo universo "
                        + "reportável (esperado " + reportableBd + ", obtido " + periodTotalBd
                        + " — period() inclui cotações/encomendas)");
    }

    @Test
    void customerStatement_finalEqualsRealBalance() throws Exception {
        // BUG-005/010/014 — FALHA AGORA:
        //  venda crédito 100 → saldo +100
        //  pagamento 40      → saldo −40 (aparece no extrato)
        //  NC integral 100   → extrato −100 mas saldo real NÃO muda (BUG-010)
        //  recebimento s/ FT 20 → saldo −20 mas NÃO aparece no extrato (BUG-014)
        //  saldo real = 100 − 40 − 20 = 40; extrato = 100 − 40 − 100 = −40 → diverge.
        setup();

        sale("VENDA", "CREDITO", 100.0, 1, false, false);

        Payment pm = new Payment();
        pm.setSale(saleRepository.findAll().stream()
                .filter(s -> b.getId().equals(s.getBranch() != null ? s.getBranch().getId() : -1L))
                .sorted(java.util.Comparator.comparing(Sale::getCreatedAt).reversed())
                .findFirst().orElseThrow());
        pm.setAmountValue(new BigDecimal("40.00"));
        pm.setMethod("DINHEIRO");
        payViaService(pm);

        Sale forNc = saleRepository.findAll().stream()
                .filter(s -> b.getId().equals(s.getBranch() != null ? s.getBranch().getId() : -1L))
                .sorted(java.util.Comparator.comparing(Sale::getCreatedAt).reversed())
                .findFirst().orElseThrow();
        saleService.createCreditNote(forNc, "Devolução p/ teste", op);

        customerAccountService.recordReceipt(c, null, new BigDecimal("20.00"), "DINHEIRO", op);

        Customer refreshed = customerRepository.findById(c.getId()).orElseThrow();
        List<CustomerAccountService.CustomerStatementEntry> statement =
                customerAccountService.getCustomerStatement(c.getId());
        BigDecimal statementFinal = statement.isEmpty()
                ? BigDecimal.ZERO
                : statement.get(statement.size() - 1).getRunningBalance();

        assertEquals(0, refreshed.getBalanceAmount().compareTo(statementFinal),
                "extrato final (" + statementFinal + ") deve reconstruir o saldo real ("
                        + refreshed.getBalanceAmount() + ") — NC e recebimentos sem factura divergem (BUG-005/010/014)");
    }

    @Autowired
    private PaymentService paymentService;

    private void payViaService(Payment pm) {
        try {
            paymentService.createPayment(pm, op);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
