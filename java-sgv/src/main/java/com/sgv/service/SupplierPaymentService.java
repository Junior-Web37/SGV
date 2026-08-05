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

    @Transactional
    public SupplierPayment savePayment(SupplierPayment payment) {
        if (payment == null) throw new IllegalArgumentException("Pagamento é obrigatório.");
        if (payment.getAmountValue() == null || payment.getAmountValue().compareTo(BigDecimal.ZERO) <= 0) {
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
                    .orElseThrow(() -> new IllegalArgumentException("Compra associada não encontrada."));
            payment.setPurchase(purchase);
            if (payment.getSupplier() == null) {
                payment.setSupplier(purchase.getSupplier());
            }
        }

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        SupplierPayment saved = supplierPaymentRepository.save(payment);

        if (purchase != null) {
            BigDecimal previousPaid = purchase.getPaidAmountValue() != null ? purchase.getPaidAmountValue() : BigDecimal.ZERO;
            BigDecimal nextPaid = previousPaid.add(payment.getAmountValue());
            purchase.setPaidAmountValue(nextPaid);
            if (purchase.getTotalAmount() != null && nextPaid.compareTo(purchase.getTotalAmount()) >= 0) {
                purchase.setState("PAID");
            }
            purchaseRepository.save(purchase);
        }

        return saved;
    }
}
