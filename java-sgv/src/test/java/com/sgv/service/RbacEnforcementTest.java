package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.8 — RBAC a nível de serviço (a segurança NÃO pode depender da UI):
 * papel CAIXA (sem COMPRAS:CREATE, sem TRANSFERENCIAS:CREATE, sem
 * FINANCEIRO:CREATE, sem STOCK:CREATE) tem de ser recusado pelos services de
 * escrita — hoje nenhuma destas operações verifica permissões.
 *
 * Estado actual (antes das correções da auditoria): TODOS FALHAM
 *  - BUG-017: TransferService.create sem RBAC.
 *  - BUG-019: PurchaseService.savePurchase sem RBAC.
 *  - BUG-020: SupplierPaymentService.savePayment sem RBAC.
 *  - BUG-024: StockBranchService.initializeStock sem RBAC.
 * Após a correção (requirePermission em cada método) passam.
 */
@SpringBootTest
@ActiveProfiles("test")
class RbacEnforcementTest {

    @Autowired private TransferService transferService;
    @Autowired private PurchaseService purchaseService;
    @Autowired private SupplierPaymentService supplierPaymentService;
    @Autowired private StockBranchService stockBranchService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private SupplierRepository supplierRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    /** Papel CAIXA com as permissões default do DataInitializer (nem mais, nem menos). */
    private Role caixaRole;
    private User caixa;
    private Branch b;
    private Product p;

    @BeforeEach
    void setup() {
        tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(s -> {
            caixaRole = new Role();
            caixaRole.setName("RBAC-CAIXA");
            caixaRole.setDescription("Papel caixa (permissões default)");
            caixaRole.getPermissions().addAll(Set.of(
                    "RESUMO:VIEW", "VENDAS:VIEW", "VENDAS:CREATE",
                    "CLIENTES:VIEW", "CAIXA:VIEW"));
            roleRepository.save(caixaRole);

            b = new Branch();
            b.setName("Filial RBAC");
            b.setNuit("700700700");
            b.setAddress("Rua RBAC");
            b.setContact("847777777");
            branchRepository.save(b);

            caixa = new User();
            caixa.setUsername("rbac-caixa");
            caixa.setPasswordHash("x");
            caixa.setRoles(Set.of(caixaRole));
            caixa.setBranch(b);
            userRepository.save(caixa);

            Category c = new Category();
            c.setName("Cat RBAC");
            categoryRepository.save(c);
            MetricUnit u = new MetricUnit();
            u.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
            u.setDescription("Unidade RBAC");
            metricUnitRepository.save(u);
            p = new Product();
            p.setCode("RBAC");
            p.setName("Produto RBAC");
            p.setCategory(c);
            p.setUnit(u);
            p.setPriceCost(10.0);
            p.setPriceSale(50.0);
            p.setService(false);
            p.setIsActive(true);
            productRepository.save(p);
        });
    }

    @Test
    void caixaCannotCreateBranchTransfer() {
        // BUG-017 — TransferService.create: zero requirePermission.
        Branch to = tx.execute(s -> {
            Branch t = new Branch();
            t.setName("Filial RBAC Dest");
            t.setNuit("700700701");
            t.setAddress("Rua RBAC Dest");
            t.setContact("847777778");
            return branchRepository.save(t);
        });

        assertThrows(IllegalStateException.class, () -> {
            Transfer t = new Transfer();
            t.setSourceBranch(b);
            t.setDestinationBranch(to);
            t.setRequestedBy(caixa);
            TransferItem item = new TransferItem();
            item.setProduct(p);
            item.setQuantityAmount(BigDecimal.ONE);
            transferService.create(t, List.of(item));
        }, "BUG-017: papel CAIXA não pode criar transferências entre filiais — "
                + "TransferService.create não verifica permissões");
    }

    @Test
    void caixaCannotSavePurchase() {
        // BUG-019 — PurchaseService.savePurchase: zero requirePermission.
        // (armazém criado para o fluxo SER válido se houvesse permissão —
        //  isto isola a falta de RBAC de qualquer outro motivo de falha)
        Warehouse w = tx.execute(s -> {
            Warehouse wh = new Warehouse();
            wh.setCode("RBAC-WH");
            wh.setName("Armazém RBAC");
            return warehouseRepository.save(wh);
        });
        Supplier sup = tx.execute(s -> {
            Supplier sp = new Supplier();
            sp.setName("Fornecedor RBAC");
            sp.setNuit("800800800");
            sp.setAddress("Rua Fornecedor");
            sp.setContact("848888888");
            return supplierRepository.save(sp);
        });

        assertThrows(IllegalStateException.class, () -> {
            Purchase purchase = new Purchase();
            purchase.setSupplier(sup);
            purchase.setTargetWarehouse(w);
            purchase.setBranch(b);
            purchase.setUser(caixa);
            purchase.setInvoiceNumber("RBAC-INV-1");
            PurchaseItem item = new PurchaseItem();
            item.setProduct(p);
            item.setQuantity(2.0);
            item.setCostPrice(20.0);
            item.setSubtotal(40.0);
            purchase.setItems(List.of(item));
            purchaseService.savePurchase(purchase, caixa, false, null);
        }, "BUG-019: papel CAIXA não pode gravar compras — PurchaseService "
                + "não verifica permissões (a compra foi aceite sem RBAC)");
    }

    @Test
    void caixaCannotPaySupplier() {
        // BUG-020 — SupplierPaymentService.savePayment: zero requirePermission.
        Supplier sup = tx.execute(s -> {
            Supplier sp = new Supplier();
            sp.setName("Fornecedor RBAC Pag");
            sp.setNuit("800800801");
            sp.setAddress("Rua Fornecedor Pag");
            sp.setContact("848888889");
            return supplierRepository.save(sp);
        });
        Purchase purchase = tx.execute(s -> {
            Purchase pc = new Purchase();
            pc.setSupplier(sup);
            pc.setBranch(b);
            pc.setUser(caixa);
            pc.setInvoiceNumber("RBAC-INV-2");
            pc.setTotalAmount(new BigDecimal("100.00"));
            pc.setSubtotalAmount(new BigDecimal("100.00"));
            pc.setPaidAmountValue(BigDecimal.ZERO);
            pc.setState("RECEIVED");
            return purchaseRepository.save(pc);
        });

        assertThrows(IllegalStateException.class, () -> {
            SupplierPayment payment = new SupplierPayment();
            payment.setSupplier(sup);
            payment.setPurchase(purchase);
            payment.setAmountValue(new BigDecimal("50.00"));
            payment.setMethod("DINHEIRO");
            supplierPaymentService.savePayment(payment, caixa);
        }, "BUG-020: papel CAIXA não pode pagar fornecedores — "
                + "SupplierPaymentService não verifica permissões");
    }

    @Test
    void caixaCannotInitializeStock() {
        // BUG-024 — StockBranchService.initializeStock: zero requirePermission
        //  (qualquer utilizador autenticado sobrepõe o stock de uma filial).
        assertThrows(IllegalStateException.class, () ->
                stockBranchService.initializeStock(
                        b, p, new BigDecimal("9999"), BigDecimal.ZERO, BigDecimal.ZERO, "RBAC-TEST", caixa),
                "BUG-024: papel CAIXA não pode inicializar stock — "
                        + "StockBranchService.initializeStock não verifica permissões");
    }
}
