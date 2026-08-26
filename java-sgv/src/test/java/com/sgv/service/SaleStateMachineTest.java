package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.model.SaleState;
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
 * §19.5 — Máquina de estados da venda:
 *   venda a crédito → EMITIDA; pagamento parcial → PAGO_PARCIAL;
 *   pagamento total → PAGO; e PAGO_PARCIAL é um estado real do enum.
 *
 * Estado actual (antes das correções da auditoria):
 *  - creditSale_createdAsEmittedNotPago: FALHA (BUG-009 —
 *    applyDocumentState marca VENDA/FACTURA como PAGO independentemente do
 *    método: a fatura em dívida nasce "PAGO").
 *  - partialPayment_setsPagoParcial: PASSA (a string "PAGO_PARCIAL" já é
 *    escrita pelo PaymentService — embora seja uma string solta fora do enum).
 *  - fullPayment_setsPago: PASSA.
 *  - pagoParcialIsARealEnumState: FALHA (BUG-009 — SaleState não tem
 *    PAGO_PARCIAL; fromString("PAGO_PARCIAL") normaliza para EMITIDA,
 *    escondendo o estado em qualquer código que use o enum).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SaleStateMachineTest {

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

    private Branch b;
    private User op;
    private Product p;
    private Customer c;

    private void setup(String tag) {
        Role r = new Role();
        r.setName("SM-" + tag.toUpperCase());
        r.setDescription("Role estados");
        r.getPermissions().add("VENDAS:CREATE");
        r.getPermissions().add("CAIXA:VIEW");
        roleRepository.save(r);

        b = new Branch();
        b.setName("Filial SM " + tag);
        b.setNuit("500500" + tag.length());
        b.setAddress("Rua SM " + tag);
        b.setContact("845555555");
        branchRepository.save(b);

        op = new User();
        op.setUsername("sm-" + tag.toLowerCase().replace(" ", "-") + "-op");
        op.setPasswordHash("x");
        op.setRoles(java.util.Set.of(r));
        op.setBranch(b);
        userRepository.save(op);

        Category cat = new Category();
        cat.setName("Cat SM " + tag);
        categoryRepository.save(cat);
        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        unit.setDescription("Unidade SM " + tag);
        metricUnitRepository.save(unit);
        p = new Product();
        p.setCode("SM" + tag.toUpperCase());
        p.setName("Produto SM " + tag);
        p.setCategory(cat);
        p.setUnit(unit);
        p.setPriceCost(10.0);
        p.setPriceSale(100.0);
        p.setService(false);
        p.setIsActive(true);
        productRepository.save(p);

        stockBranchService.initializeStock(b, p, new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", op);
        c = new Customer();
        c.setCode("CLI-SM-" + tag.toUpperCase());
        c.setName("Cliente SM " + tag);
        c.setBalanceAmount(BigDecimal.ZERO);
        c.setCreditLimitAmount(new BigDecimal("1000.00"));
        customerRepository.save(c);
    }

    private Sale creditSale() throws Exception {
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
        item.setUnitPriceAmount(new BigDecimal("100.00"));
        item.setLineBaseAmount(new BigDecimal("100.00"));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(new BigDecimal("100.00"));
        item.setDescription(p.getName());
        s.setItems(List.of(item));
        return saleService.processAndSave(s, op);
    }

    private void pay(String amount) {
        Payment pm = new Payment();
        pm.setSale(saleRepository.findAll().stream()
                .filter(s -> c.getId().equals(s.getCustomer() != null ? s.getCustomer().getId() : -1L))
                .filter(s -> s.getDocumentNumber() != null)
                .sorted(java.util.Comparator.comparing(Sale::getDocumentNumber).reversed())
                .findFirst().orElseThrow());
        pm.setAmountValue(new BigDecimal(amount));
        pm.setMethod("DINHEIRO");
        try {
            paymentService.createPayment(pm, op);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void creditSale_createdAsEmittedNotPago() throws Exception {
        // BUG-009 — FALHA AGORA: a fatura a crédito nasce "PAGO".
        setup("Cred");
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale sale = creditSale();

        assertEquals("EMITIDA", sale.getState(),
                "venda a crédito deve nascer EMITIDA (em dívida), não PAGO");
    }

    @Test
    void partialPayment_setsPagoParcial_thenFullSetsPago() throws Exception {
        setup("Parc");
        cashSessionService.openSession(op, BigDecimal.ZERO);

        Sale sale = creditSale();
        pay("40.00");
        Sale partial = saleRepository.findById(sale.getId()).orElseThrow();
        assertEquals("PAGO_PARCIAL", partial.getState(),
                "após pagamento parcial o estado deve ser PAGO_PARCIAL");

        pay("60.00");
        Sale full = saleRepository.findById(sale.getId()).orElseThrow();
        assertEquals("PAGO", full.getState(),
                "após liquidação total o estado deve ser PAGO");
    }

    @Test
    void pagoParcialIsARealEnumState() {
        // BUG-009 — FALHA AGORA: PAGO_PARCIAL não existe no enum SaleState;
        // fromString("PAGO_PARCIAL") devolve EMITIDA (estado órfão normalizado).
        assertDoesNotThrow(() -> SaleState.valueOf("PAGO_PARCIAL"),
                "SaleState deve ter PAGO_PARCIAL como estado de primeira classe");
        assertNotEquals(SaleState.EMITIDA, SaleState.fromString("PAGO_PARCIAL"),
                "fromString(\"PAGO_PARCIAL\") não pode normalizar para EMITIDA — escondia o estado parcial");
    }
}
