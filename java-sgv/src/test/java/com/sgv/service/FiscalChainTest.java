package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.9 — Fiscal: cadeia de hashes verificável por série, QR Code presente
 * no documento, e recibo com série/numeração própria.
 *
 * Estado actual (antes das correções da auditoria):
 *  - sameSeries_chainVerifiable: FALHA (BUG-016 — o "previous hash" é a
 *    venda mais recente da filial por createdAt, de TODAS as séries: com
 *    séries entrelaçadas a cadeia da série A usa o hash de B como anterior).
 *  - hashControl_sequential: PASSA (contador sequencial por filial — mantém).
 *  - qrCode_generatedOnDocument: FALHA (BUG-016 — `sale.qrCode` nunca é
 *    populado; nenhum documento sai com QR).
 *  - receipt_hasOwnSeriesNumbering: FALHA (BUG-012 — o "recibo" muda o
 *    documentType da FT em memória; não existe recibo com série/numeração
 *    própria na BD).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FiscalChainTest {

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

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            try (Formatter fmt = new Formatter()) {
                for (byte b : digest) fmt.format("%02x", b);
                return fmt.out().toString();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Recomputa o signatureHash exactamente como o FiscalService, mas com o previous indicado. */
    private static String recomputeSignatureHash(Sale s, String previousHash) {
        String invoiceDate = s.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String systemEntryDate = s.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String invoiceNo = s.getDocumentType() + " " + s.getSeries() + "/" + s.getDocumentNumber();
        String grossTotal = String.format(Locale.US, "%.2f", s.getTotal());
        String chainingInput = invoiceDate + ";" + systemEntryDate + ";" + invoiceNo + ";" + grossTotal + ";" + previousHash;
        return sha256Hex(chainingInput);
    }

    private Branch b;
    private User op;
    private Product p;
    private Customer c;

    private void setup() {
        Role r = new Role();
        r.setName("FISCAL");
        r.setDescription("Role fiscal");
        r.getPermissions().add("VENDAS:CREATE");
        r.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(r);

        b = new Branch();
        b.setName("Filial Fiscal");
        b.setNuit("900900900");
        b.setAddress("Rua Fiscal");
        b.setContact("849999999");
        branchRepository.save(b);

        op = new User();
        op.setUsername("fiscal-op");
        op.setPasswordHash("x");
        op.setRoles(java.util.Set.of(r));
        op.setBranch(b);
        userRepository.save(op);

        Category cat = new Category();
        cat.setName("Cat Fiscal");
        categoryRepository.save(cat);
        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        unit.setDescription("Unidade fiscal");
        metricUnitRepository.save(unit);
        p = new Product();
        p.setCode("FISCAL");
        p.setName("Produto Fiscal");
        p.setCategory(cat);
        p.setUnit(unit);
        p.setPriceCost(10.0);
        p.setPriceSale(100.0);
        p.setService(false);
        p.setIsActive(true);
        productRepository.save(p);

        stockBranchService.initializeStock(b, p, new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", op);
        c = new Customer();
        c.setCode("CLI-FISCAL");
        c.setName("Cliente Fiscal");
        c.setNuit("999999999"); // consumidor final — sempre válido no NuitValidator
        c.setBalanceAmount(BigDecimal.ZERO);
        c.setCreditLimitAmount(new BigDecimal("100000.00"));
        customerRepository.save(c);
    }

    private Sale sell(String series, String method, double price) throws Exception {
        Sale s = new Sale();
        s.setBranch(b);
        s.setCustomer(c);
        s.setCustomerNuit("999999999"); // FACTURA valida o NUIT na venda
        s.setDocumentType(series.startsWith("A") ? "VENDA" : "FACTURA");
        s.setSeries(series);
        s.setPaymentMethod(method);
        s.setState("EMITIDA");
        SaleItem item = new SaleItem();
        item.setProduct(p);
        item.setProductCode(p.getCode());
        item.setQtyAmount(BigDecimal.ONE);
        item.setUnitPriceAmount(BigDecimal.valueOf(price));
        item.setLineBaseAmount(BigDecimal.valueOf(price));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(BigDecimal.valueOf(price));
        item.setDescription(p.getName());
        s.setItems(List.of(item));
        return saleService.processAndSave(s, op);
    }

    @Test
    void sameSeries_chainVerifiable_acrossInterleavedSeries() throws Exception {
        // BUG-016 — FALHA AGORA.
        setup();
        cashSessionService.openSession(op, BigDecimal.ZERO);

        // Séries A e B entrelaçadas na mesma filial (datas distintas p/
        // ordenação determinística por createdAt):
        Sale a1 = sell("A", "DINHEIRO", 100.0);
        Thread.sleep(1100);
        Sale b1 = sell("B", "DINHEIRO", 200.0);
        Thread.sleep(1100);
        Sale a2 = sell("A", "DINHEIRO", 300.0);

        a1 = saleRepository.findById(a1.getId()).orElseThrow();
        b1 = saleRepository.findById(b1.getId()).orElseThrow();
        a2 = saleRepository.findById(a2.getId()).orElseThrow();

        assertNotNull(a1.getSignatureHash(), "A1 precisa de signatureHash");
        assertNotNull(b1.getSignatureHash(), "B1 precisa de signatureHash");
        assertNotNull(a2.getSignatureHash(), "A2 precisa de signatureHash");
        assertNotEquals(a1.getSignatureHash(), a2.getSignatureHash(),
                "hashes da mesma série têm de ser distintos");

        // A cadeia da série A tem de usar o hash A1 como anterior — não o
        // documento mais recente da filial (que é o B1).
        String expectedA2 = recomputeSignatureHash(a2, a1.getSignatureHash());
        assertEquals(expectedA2, a2.getSignatureHash(),
                "BUG-016: o previous hash da série A deve ser o A1 — a cadeia "
                        + "usa a venda mais recente de TODAS as séries (aqui, o B1)");

        // A série B, por sua vez, deve encadear a partir de B1 (ou vazio) —
        // verificação simétrica: recomputar B1 com previous = A1 (que é o
        // comportamento actual) deve continuar a bater, e A2 com B1 NÃO.
        String actualB1 = recomputeSignatureHash(b1, a1.getSignatureHash());
        assertEquals(actualB1, b1.getSignatureHash(),
                "B1 (o 2º documento da filial) encadeia a partir de A1 — comportamento base");
    }

    @Test
    void hashControl_sequentialPerBranch() throws Exception {
        // Comportamento CORRECTO a manter (contador atómico por filial).
        setup();
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale s1 = sell("A", "DINHEIRO", 100.0);
        Thread.sleep(1100);
        Sale s2 = sell("A", "DINHEIRO", 100.0);
        Thread.sleep(1100);
        Sale s3 = sell("A", "DINHEIRO", 100.0);

        s1 = saleRepository.findById(s1.getId()).orElseThrow();
        s2 = saleRepository.findById(s2.getId()).orElseThrow();
        s3 = saleRepository.findById(s3.getId()).orElseThrow();

        assertNotNull(s1.getHashControl());
        assertNotNull(s2.getHashControl());
        assertNotNull(s3.getHashControl());
        assertTrue(s1.getHashControl() < s2.getHashControl() && s2.getHashControl() < s3.getHashControl(),
                "hashControl deve ser estritamente sequencial por filial: "
                        + s1.getHashControl() + ", " + s2.getHashControl() + ", " + s3.getHashControl());
        assertTrue(s1.getDocumentNumber() < s2.getDocumentNumber()
                        && s2.getDocumentNumber() < s3.getDocumentNumber(),
                "numeração da série A deve ser sequencial");
    }

    @Test
    void qrCode_generatedOnDocument() throws Exception {
        // BUG-016 — FALHA AGORA: sale.qrCode nunca é populado (setQrCode tem
        // zero chamadores em src/main) → nenhum documento sai com QR.
        setup();
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale s = sell("A", "DINHEIRO", 100.0);
        s = saleRepository.findById(s.getId()).orElseThrow();

        assertNotNull(s.getQrCode(),
                "BUG-016: o documento deve ter QR Code gerado (facturação electrónica MZ)");
    }

    @Test
    void receipt_hasOwnSeriesNumbering() throws Exception {
        // BUG-012 — FALHA AGORA: o "recibo" é a própria FT com documentType
        // trocado em memória; não existe recibo com série/numeração própria.
        setup();
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale credit = sell("A", "CREDITO", 100.0);
        Payment pm = new Payment();
        pm.setSale(saleRepository.findById(credit.getId()).orElseThrow());
        pm.setAmountValue(new BigDecimal("100.00"));
        pm.setMethod("DINHEIRO");
        paymentService.createPayment(pm, op);

        boolean receiptExists = saleRepository.findAll().stream()
                .anyMatch(s -> "RECIBO".equals(s.getDocumentType())
                        && b.getId().equals(s.getBranch() != null ? s.getBranch().getId() : -1L)
                        && s.getDocumentNumber() != null && s.getDocumentNumber() > 0);
        assertTrue(receiptExists,
                "BUG-012: pagamento deve gerar recibo como documento próprio "
                        + "com série/numeração da série RECIBO — nada gravado em sales");
    }
}
