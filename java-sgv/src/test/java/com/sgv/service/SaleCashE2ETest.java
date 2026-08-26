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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.2 — E2E venda em dinheiro: após a venda, assert em
 * sales, sale_items, stock_branch, stock_movements, cash_movements,
 * e audit_logs.
 *
 * Estado actual (antes das correções da auditoria):
 *  - cashSale_writesAllArtifacts: PASSA (fluxo base sólido — BUG-003 a parte
 *    do método DINHEIRO está correcto; o bug afecta os meios electrónicos).
 *  - cashSale_auditLogExists: FALHA (BUG-049 — a venda não tem log próprio no
 *    service; só o controller audita, e mesmo assim sem antes/depois).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaleCashE2ETest {

    @Autowired private SaleService saleService;
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
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private CashMovementRepository cashMovementRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    void cashSale_writesAllArtifacts() throws Exception {
        Role r = new Role();
        r.setName("E2E-CASH");
        r.setDescription("Role E2E cash");
        r.getPermissions().add("VENDAS:CREATE");
        r.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(r);

        Branch b = new Branch();
        b.setName("Filial E2E Cash");
        b.setNuit("200200200");
        b.setAddress("Rua E2E");
        b.setContact("842222222");
        branchRepository.save(b);

        User u = new User();
        u.setUsername("e2e-cash-op");
        u.setPasswordHash("x");
        u.setRoles(java.util.Set.of(r));
        u.setBranch(b);
        userRepository.save(u);

        Category c = new Category();
        c.setName("Cat E2E Cash");
        categoryRepository.save(c);
        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        unit.setDescription("Unidade E2E");
        metricUnitRepository.save(unit);
        Product p = new Product();
        p.setCode("E2ECASH");
        p.setName("Produto E2E Cash");
        p.setCategory(c);
        p.setUnit(unit);
        p.setPriceCost(10.0);
        p.setPriceSale(36.26);
        p.setService(false);
        p.setIsActive(true);
        productRepository.save(p);

        stockBranchService.initializeStock(b, p, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", u);
        cashSessionService.openSession(u, BigDecimal.ZERO);

        Customer cust = new Customer();
        cust.setCode("CLI-E2E-CASH");
        cust.setName("Cliente E2E Cash");
        cust.setBalanceAmount(BigDecimal.ZERO);
        customerRepository.save(cust);

        Sale sale = new Sale();
        sale.setBranch(b);
        sale.setCustomer(cust);
        sale.setDocumentType("VENDA");
        sale.setSeries("A");
        sale.setPaymentMethod("DINHEIRO");
        sale.setState("EMITIDA");
        SaleItem item = new SaleItem();
        item.setProduct(p);
        item.setProductCode(p.getCode());
        item.setQtyAmount(new BigDecimal("2"));
        item.setUnitPriceAmount(new BigDecimal("36.26"));
        item.setLineBaseAmount(new BigDecimal("72.52"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(new BigDecimal("72.52"));
        item.setDescription(p.getName());
        sale.setItems(List.of(item));

        File document = saleService.processAndSave(sale, u);

        // 1) Documento gerado
        assertNotNull(document, "processAndSave deve gerar o documento");
        assertTrue(document.exists() && document.length() > 0, "documento em disco deve existir com bytes");

        // 2) sales
        Sale saved = saleRepository.findById(sale.getId()).orElseThrow();
        assertNotNull(saved.getDocumentNumber(), "número de documento atribuído");
        assertTrue(saved.getDocumentNumber() > 0);
        assertNotNull(saved.getDocumentYear());
        assertEquals("A", saved.getSeries());
        assertEquals(0, new BigDecimal("72.52").compareTo(saved.getTotalAmount()));
        assertEquals(0, new BigDecimal("72.52").compareTo(saved.getPaidAmountValue()));
        assertEquals("PAGO", saved.getState());
        assertNotNull(saved.getHashHash(), "hashHash AT deve ser gerado");
        assertNotNull(saved.getSignatureHash(), "signatureHash (cadeia) deve ser gerado");
        assertNotNull(saved.getHashControl(), "hashControl sequencial deve ser atribuído");

        // 3) sale_items
        assertEquals(1, saved.getItems().size());
        SaleItem savedItem = saved.getItems().get(0);
        assertEquals(0, new BigDecimal("2").compareTo(savedItem.getQtyAmount()));
        assertEquals(0, new BigDecimal("72.52").compareTo(savedItem.getLineTotalAmount()));

        // 4) stock_branch
        StockBranch sb = stockBranchRepository.findByProductAndBranch(p, b).orElseThrow();
        assertEquals(0, new BigDecimal("8.00").compareTo(sb.getStockCurrentAmount()),
                "stock deve descer de 10 para 8");

        // 5) stock_movements (kardex)
        List<StockMovement> movements = stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(p.getId()).stream()
                .filter(m -> b.getId().equals(m.getBranch() != null ? m.getBranch().getId() : -1L))
                .toList();
        StockMovement salida = movements.stream()
                .filter(m -> "SAIDA".equals(m.getType()) && "VENDA".equals(m.getSubtype()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Movimento SAIDA/VENDA não existe no kardex"));
        assertEquals(0, new BigDecimal("-2").compareTo(salida.getQtyAmount()));
        assertEquals(0, new BigDecimal("8.00").compareTo(salida.getStockAfterAmount()));

        // 6) cash_movements
        CashSession session = cashSessionService.getOpenSession(u).orElseThrow();
        List<CashMovement> cashMovements = cashMovementRepository.findBySessionOrderByCreatedAtAsc(session);
        CashMovement in = cashMovements.stream()
                .filter(m -> "IN".equals(m.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Movimento de caixa IN da venda não existe"));
        assertEquals(0, new BigDecimal("72.52").compareTo(in.getAmount()),
                "entrada de caixa deve ser igual ao total da venda");
        assertEquals(OperationKind.SALE, in.getOperationKind());
    }

    @Test
    void cashSale_auditLogExists() throws Exception {
        // BUG-049 — FALHA AGORA: o service de venda não escreve registo de
        // auditoria próprio (sem antes/depois). Depois do reforço de auditoria
        // (Sprint 3) deve passar.
        Role r = new Role();
        r.setName("E2E-CASH-AUD");
        r.setDescription("Role E2E cash auditoria");
        r.getPermissions().add("VENDAS:CREATE");
        r.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(r);

        Branch b = new Branch();
        b.setName("Filial E2E Cash Aud");
        b.setNuit("200200201");
        b.setAddress("Rua E2E Aud");
        b.setContact("842222223");
        branchRepository.save(b);

        User u = new User();
        u.setUsername("e2e-cash-aud-op");
        u.setPasswordHash("x");
        u.setRoles(java.util.Set.of(r));
        u.setBranch(b);
        userRepository.save(u);

        Category c = new Category();
        c.setName("Cat E2E Cash Aud");
        categoryRepository.save(c);
        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        unit.setDescription("Unidade E2E Aud");
        metricUnitRepository.save(unit);
        Product p = new Product();
        p.setCode("E2ECASHAUD");
        p.setName("Produto E2E Cash Aud");
        p.setCategory(c);
        p.setUnit(unit);
        p.setPriceCost(10.0);
        p.setPriceSale(20.0);
        p.setService(false);
        p.setIsActive(true);
        productRepository.save(p);

        stockBranchService.initializeStock(b, p, new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", u);
        cashSessionService.openSession(u, BigDecimal.ZERO);

        Customer cust = new Customer();
        cust.setCode("CLI-E2E-CASH-AUD");
        cust.setName("Cliente E2E Cash Aud");
        cust.setBalanceAmount(BigDecimal.ZERO);
        customerRepository.save(cust);

        Sale sale = new Sale();
        sale.setBranch(b);
        sale.setCustomer(cust);
        sale.setDocumentType("VENDA");
        sale.setSeries("A");
        sale.setPaymentMethod("DINHEIRO");
        sale.setState("EMITIDA");
        SaleItem item = new SaleItem();
        item.setProduct(p);
        item.setProductCode(p.getCode());
        item.setQtyAmount(BigDecimal.ONE);
        item.setUnitPriceAmount(new BigDecimal("20.00"));
        item.setLineBaseAmount(new BigDecimal("20.00"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(new BigDecimal("20.00"));
        item.setDescription(p.getName());
        sale.setItems(List.of(item));

        saleService.processAndSave(sale, u);
        Sale saved = saleRepository.findById(sale.getId()).orElseThrow();

        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        boolean hasSaleAudit = logs.stream().anyMatch(l ->
                l.getAction() != null
                        && l.getAction().toUpperCase().contains("VENDA")
                        && (l.getEntity() == null || String.valueOf(saved.getId()).equals(String.valueOf(l.getEntityId()))));
        assertTrue(hasSaleAudit,
                "Venda criada deve deixar registo de auditoria com a entidade (BUG-049) — nada encontrado em audit_logs");
    }
}
