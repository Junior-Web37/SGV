package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.PurchaseOrderRepository;
import com.sgv.repository.SupplierRepository;
import com.sgv.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final PurchaseService purchaseService;
    private final SystemLogService systemLogService;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                SupplierRepository supplierRepository,
                                WarehouseRepository warehouseRepository,
                                ProductRepository productRepository,
                                PurchaseService purchaseService,
                                SystemLogService systemLogService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.purchaseService = purchaseService;
        this.systemLogService = systemLogService;
    }

    public List<PurchaseOrder> findAll() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<PurchaseOrder> findById(Long id) {
        if (id == null) return Optional.empty();
        return purchaseOrderRepository.findById(id);
    }

    public List<PurchaseOrder> findBySupplier(Long supplierId) {
        if (supplierId == null) return List.of();
        return purchaseOrderRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
    }

    public List<PurchaseOrder> findByStatus(String status) {
        if (status == null || status.isBlank()) return List.of();
        return purchaseOrderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public PurchaseOrder createOrder(PurchaseOrder order, User user) {
        if (order == null) throw new IllegalArgumentException("Encomenda não pode ser nula.");
        if (order.getSupplier() == null || order.getSupplier().getId() == null) {
            throw new IllegalArgumentException("Fornecedor é obrigatório.");
        }
        Supplier managedSupplier = supplierRepository.findById(order.getSupplier().getId())
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado."));
        order.setSupplier(managedSupplier);

        if (order.getTargetWarehouse() != null && order.getTargetWarehouse().getId() != null) {
            Warehouse wh = warehouseRepository.findById(order.getTargetWarehouse().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Armazém não encontrado."));
            order.setTargetWarehouse(wh);
        }

        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber("ENC-" + System.currentTimeMillis());
        }

        order.setCreatedBy(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PENDING");

        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (PurchaseOrderItem item : order.getItems()) {
                item.setOrder(order);
                if (item.getProduct() != null && item.getProduct().getId() != null) {
                    Product p = productRepository.findById(item.getProduct().getId()).orElse(item.getProduct());
                    item.setProduct(p);
                }
                BigDecimal lineSub = item.getQuantity().multiply(item.getUnitCost());
                item.setSubtotal(lineSub);
                sub = sub.add(lineSub);
                if (item.getProduct() != null) {
                    double rate = item.getProduct().getEffectiveTaxRate();
                    tax = tax.add(lineSub.multiply(BigDecimal.valueOf(rate / 100.0)));
                }
            }
        }

        order.setSubtotal(sub);
        order.setTotalTax(tax);
        order.setTotal(sub.add(tax));

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        systemLogService.logUserAction(
                user != null ? user.getUsername() : "Sistema",
                "PURCHASE_ORDER_CREATED",
                "Ordem de Encomenda nº " + saved.getOrderNumber() + " criada para " + managedSupplier.getName());
        return saved;
    }

    @Transactional
    public PurchaseOrder approveOrder(Long orderId, User user) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Encomenda não encontrada: " + orderId));
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Apenas encomendas no estado PENDENTE podem ser aprovadas.");
        }
        order.setStatus("APPROVED");
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        systemLogService.logUserAction(
                user != null ? user.getUsername() : "Sistema",
                "PURCHASE_ORDER_APPROVED",
                "Ordem de Encomenda nº " + saved.getOrderNumber() + " aprovada.");
        return saved;
    }

    @Transactional
    public PurchaseOrder cancelOrder(Long orderId, User user) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Encomenda não encontrada: " + orderId));
        if ("RECEIVED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("Não é possível cancelar uma encomenda que já deu entrada em stock.");
        }
        order.setStatus("CANCELLED");
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        systemLogService.logUserAction(
                user != null ? user.getUsername() : "Sistema",
                "PURCHASE_ORDER_CANCELLED",
                "Ordem de Encomenda nº " + saved.getOrderNumber() + " cancelada.");
        return saved;
    }

    @Transactional
    public Purchase convertToPurchase(Long orderId, Warehouse targetWarehouse, String invoiceNumber, User user) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Encomenda não encontrada: " + orderId));
        if ("RECEIVED".equalsIgnoreCase(order.getStatus()) || "CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("A encomenda já foi recebida ou cancelada.");
        }

        Purchase purchase = new Purchase();
        purchase.setSupplier(order.getSupplier());
        purchase.setTargetWarehouse(targetWarehouse != null ? targetWarehouse : order.getTargetWarehouse());
        purchase.setInvoiceNumber(invoiceNumber != null && !invoiceNumber.isBlank() ? invoiceNumber : order.getOrderNumber());
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setNotes("Recebimento da Ordem de Encomenda " + order.getOrderNumber() + (order.getNotes() != null ? " - " + order.getNotes() : ""));
        purchase.setUser(user);
        if (user != null) purchase.setBranch(user.getBranch());

        List<PurchaseItem> pItems = new ArrayList<>();
        if (order.getItems() != null) {
            for (PurchaseOrderItem oi : order.getItems()) {
                PurchaseItem pi = new PurchaseItem();
                pi.setProduct(oi.getProduct());
                pi.setQuantity(oi.getQuantity().doubleValue());
                pi.setCostPrice(oi.getUnitCost().doubleValue());
                pi.setSubtotal(oi.getSubtotal().doubleValue());
                pItems.add(pi);
            }
        }
        purchase.setItems(pItems);

        Purchase savedPurchase = purchaseService.savePurchase(purchase, user, false, null);

        order.setStatus("RECEIVED");
        purchaseOrderRepository.save(order);

        systemLogService.logUserAction(
                user != null ? user.getUsername() : "Sistema",
                "PURCHASE_ORDER_CONVERTED",
                "Ordem de Encomenda " + order.getOrderNumber() + " convertida em Factura de Compra " + savedPurchase.getInvoiceNumber());

        return savedPurchase;
    }
}
