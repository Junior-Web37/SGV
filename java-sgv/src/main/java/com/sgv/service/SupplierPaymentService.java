package com.sgv.service;

import com.sgv.entity.Purchase;
import com.sgv.entity.SupplierPayment;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.SupplierPaymentRepository;
import com.sgv.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupplierPaymentService {

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;

    public SupplierPaymentService(SupplierPaymentRepository supplierPaymentRepository,
                                  PurchaseRepository purchaseRepository,
                                  SupplierRepository supplierRepository) {
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
    }

    public List<SupplierPayment> findRecentPayments(int maxResults) {
        if (maxResults <= 0) maxResults = 50;
        return supplierPaymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(maxResults)
                .toList();
    }

    public List<Purchase> findPendingPurchasesBySupplier(Long supplierId) {
        if (supplierId == null) return List.of();
        return purchaseRepository.findPendingBySupplierId(supplierId);
    }

    @Transactional
    public SupplierPayment savePayment(SupplierPayment payment) {
        if (payment == null) throw new IllegalArgumentException("Pagamento é obrigatório.");
        BigDecimal amount = payment.getAmountValue();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero.");
        }
        if (payment.getSupplier() == null || payment.getSupplier().getId() == null) {
            throw new IllegalArgumentException("Fornecedor associado é obrigatório.");
        }

        var supplier = supplierRepository.findById(payment.getSupplier().getId())
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado."));
        payment.setSupplier(supplier);

        Purchase purchase = payment.getPurchase();
        if (purchase != null && purchase.getId() != null) {
            purchase = purchaseRepository.findById(purchase.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Factura de compra associada não encontrada."));
            payment.setPurchase(purchase);

            BigDecimal purchaseTotal = purchase.getTotalAmount() != null ? purchase.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal previousPaid = purchase.getPaidAmountValue() != null ? purchase.getPaidAmountValue() : BigDecimal.ZERO;
            BigDecimal pendingBalance = purchaseTotal.subtract(previousPaid);

            if (pendingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Esta factura de compra já se encontra 100% quitada.");
            }

            if (amount.compareTo(pendingBalance) > 0) {
                throw new IllegalArgumentException(String.format("Valor a pagar (%.2f MT) excede o saldo pendente da factura (%.2f MT).", amount, pendingBalance));
            }

            BigDecimal nextPaid = previousPaid.add(amount);
            purchase.setPaidAmountValue(nextPaid);
            if (nextPaid.compareTo(purchaseTotal) >= 0) {
                purchase.setState("PAID");
            } else {
                purchase.setState("PAGO_PARCIAL");
            }
            purchaseRepository.save(purchase);
        }

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        return supplierPaymentRepository.save(payment);
    }
}
