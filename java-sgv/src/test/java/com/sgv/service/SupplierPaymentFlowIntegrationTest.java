package com.sgv.service;

import com.sgv.entity.Purchase;
import com.sgv.entity.Supplier;
import com.sgv.entity.SupplierPayment;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.SupplierPaymentRepository;
import com.sgv.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste de integração do fluxo de pagamentos a fornecedores:
 * registo de pagamento, atualização de paid_amount/estado da compra e cálculo do saldo em aberto.
 * Usa H2 in-memory (perfil "test"), contexto Spring real, dados rolados para trás por transação.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SupplierPaymentFlowIntegrationTest {

    @Autowired private SupplierPaymentService supplierPaymentService;
    @Autowired private SupplierPaymentRepository supplierPaymentRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private SupplierRepository supplierRepository;

    @Test
    void pagamentosAcumulamECompraFicaPaga() {
        Supplier s = new Supplier();
        s.setName("Fornecedor Fluxo");
        s.setNuit("123456789");
        supplierRepository.save(s);

        Purchase p = new Purchase();
        p.setInvoiceNumber("FAC-9000");
        p.setSupplier(s);
        p.setTotal(500.0);
        p.setState("RECEIVED");
        purchaseRepository.save(p);

        SupplierPayment pay1 = new SupplierPayment();
        pay1.setSupplier(s);
        pay1.setPurchase(p);
        pay1.setAmount(200.0);
        pay1.setMethod("Transferência Bancária");
        supplierPaymentService.savePayment(pay1);

        Purchase reloaded1 = purchaseRepository.findById(p.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("200").compareTo(reloaded1.getPaidAmountValue()),
                "Após o 1º pagamento o valor pago deve ser 200");
        assertEquals("RECEIVED", reloaded1.getState(), "Compra parcialmente paga mantém estado RECEIVED");

        SupplierPayment pay2 = new SupplierPayment();
        pay2.setSupplier(s);
        pay2.setPurchase(p);
        pay2.setAmount(300.0);
        supplierPaymentService.savePayment(pay2);

        Purchase reloaded2 = purchaseRepository.findById(p.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500").compareTo(reloaded2.getPaidAmountValue()),
                "Após o 2º pagamento o valor pago deve ser 500");
        assertEquals("PAID", reloaded2.getState(), "Compra totalmente paga deve ficar PAID");

        assertEquals(2, supplierPaymentRepository.findBySupplierId(s.getId()).size(),
                "Devem existir 2 pagamentos registados para o fornecedor");
    }

    @Test
    void saldoEmAbertoRefleteTotalMenosPago() {
        Supplier s = new Supplier();
        s.setName("Fornecedor Saldo");
        s.setNuit("987654321");
        supplierRepository.save(s);

        Purchase p = new Purchase();
        p.setInvoiceNumber("FAC-9001");
        p.setSupplier(s);
        p.setTotal(1000.0);
        p.setState("RECEIVED");
        purchaseRepository.save(p);

        SupplierPayment pay = new SupplierPayment();
        pay.setSupplier(s);
        pay.setPurchase(p);
        pay.setAmount(400.0);
        supplierPaymentService.savePayment(pay);

        List<Object[]> balances = purchaseRepository.findOutstandingBalanceBySupplier();
        BigDecimal bal = balances.stream()
                .filter(r -> r[0] != null && r[0].equals(s.getId()))
                .map(r -> (BigDecimal) r[1])
                .findFirst()
                .orElseThrow(() -> new AssertionError("Saldo do fornecedor não encontrado"));
        assertEquals(0, new BigDecimal("600").compareTo(bal),
                "Saldo em aberto deve ser total - pago (1000 - 400 = 600)");
    }
}
