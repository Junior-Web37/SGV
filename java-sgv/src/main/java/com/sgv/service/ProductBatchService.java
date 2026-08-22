package com.sgv.service;

import com.sgv.entity.ProductBatch;
import com.sgv.entity.User;
import com.sgv.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductBatchService {

    private final ProductBatchRepository productBatchRepository;
    private final SystemLogService systemLogService;

    public ProductBatchService(ProductBatchRepository productBatchRepository,
                               SystemLogService systemLogService) {
        this.productBatchRepository = productBatchRepository;
        this.systemLogService = systemLogService;
    }

    public List<ProductBatch> findAll() {
        return productBatchRepository.findAll();
    }

    public Optional<ProductBatch> findById(Long id) {
        if (id == null) return Optional.empty();
        return productBatchRepository.findById(id);
    }

    public List<ProductBatch> findByProductId(Long productId) {
        if (productId == null) return List.of();
        return productBatchRepository.findByProductIdOrderByExpiryDateAsc(productId);
    }

    public List<ProductBatch> findNearExpiry(int daysThreshold) {
        LocalDate targetDate = LocalDate.now().plusDays(daysThreshold <= 0 ? 30 : daysThreshold);
        return productBatchRepository.findNearExpiryBatches(targetDate);
    }

    public List<ProductBatch> findExpired() {
        return productBatchRepository.findByStatusOrderByExpiryDateAsc("EXPIRED");
    }

    @Transactional
    public ProductBatch registerBatch(ProductBatch batch, User user) {
        if (batch == null) throw new IllegalArgumentException("Lote não pode ser nulo.");
        if (batch.getProduct() == null || batch.getProduct().getId() == null) {
            throw new IllegalArgumentException("Artigo associado ao lote é obrigatório.");
        }
        if (batch.getBatchNumber() == null || batch.getBatchNumber().isBlank()) {
            throw new IllegalArgumentException("Número do lote é obrigatório.");
        }
        if (batch.getExpiryDate() == null) {
            throw new IllegalArgumentException("Data de validade do lote é obrigatória.");
        }
        if (batch.getCreatedAt() == null) {
            batch.setCreatedAt(LocalDateTime.now());
        }

        if (batch.getExpiryDate().isBefore(LocalDate.now())) {
            batch.setStatus("EXPIRED");
        } else if (batch.getExpiryDate().isBefore(LocalDate.now().plusDays(30))) {
            batch.setStatus("NEAR_EXPIRY");
        } else {
            batch.setStatus("ACTIVE");
        }

        ProductBatch saved = productBatchRepository.save(batch);
        systemLogService.logUserAction(
                user != null ? user.getUsername() : "Sistema",
                "BATCH_REGISTERED",
                "Lote nº " + saved.getBatchNumber() + " registado para o artigo " + (saved.getProduct() != null ? saved.getProduct().getName() : "") + " (Validade: " + saved.getExpiryDate() + ")");
        return saved;
    }

    /**
     * Abate de stock por Lotes utilizando a regra FEFO (First Expired, First Out).
     */
    @Transactional
    public void deductFromBatchesFefo(Long productId, BigDecimal quantityToDeduct, User user) {
        if (productId == null || quantityToDeduct == null || quantityToDeduct.compareTo(BigDecimal.ZERO) <= 0) return;

        List<ProductBatch> activeBatches = productBatchRepository.findByProductIdOrderByExpiryDateAsc(productId);
        BigDecimal remaining = quantityToDeduct;

        for (ProductBatch batch : activeBatches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal batchQty = batch.getQuantity();
            if (batchQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            if (batchQty.compareTo(remaining) <= 0) {
                remaining = remaining.subtract(batchQty);
                batch.setQuantity(BigDecimal.ZERO);
                batch.setStatus("DEPLETED");
            } else {
                batch.setQuantity(batchQty.subtract(remaining));
                remaining = BigDecimal.ZERO;
            }
            productBatchRepository.save(batch);
        }
    }
}
