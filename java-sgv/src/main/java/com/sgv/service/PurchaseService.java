package com.sgv.service;

import com.sgv.entity.Purchase;
import com.sgv.entity.PurchaseItem;
import com.sgv.entity.Purchase;
import com.sgv.entity.Warehouse;
import com.sgv.entity.Product;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseService warehouseService;
    private final SystemLogService systemLogService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           ProductRepository productRepository,
                           WarehouseRepository warehouseRepository,
                           WarehouseService warehouseService,
                           SystemLogService systemLogService) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseService = warehouseService;
        this.systemLogService = systemLogService;
    }

    @Transactional
    public Purchase savePurchase(Purchase purchase, com.sgv.entity.User currentUser, boolean isEdit, Long previousPurchaseId) {
        if (purchase == null) throw new IllegalArgumentException("Purchase is null");

        double sub = 0.0;
        double tax = 0.0;
        List<PurchaseItem> items = purchase.getItems();
        if (items == null) {
            items = new ArrayList<>();
            purchase.setItems(items);
        }
        List<PurchaseItem> savedItems = new ArrayList<>();

        for (PurchaseItem item : items) {
            if (item.getProduct() != null && item.getProduct().getId() != null) {
                Product managedProduct = productRepository.findById(item.getProduct().getId()).orElse(item.getProduct());
                item.setProduct(managedProduct);
            }
            double lineSub = item.getSubtotal() != null ? item.getSubtotal() : 0.0;
            sub += lineSub;
            if (item.getProduct() != null) {
                double taxRate = item.getProduct().getEffectiveTaxRate();
                tax += lineSub * (taxRate / 100.0);
            }
            item.setPurchase(purchase);
            savedItems.add(item);
        }

        purchase.setSubtotal(sub);
        purchase.setTotalTax(tax);
        purchase.setTotal(sub + tax);

        Warehouse targetWarehouse = purchase.getTargetWarehouse();
        if (targetWarehouse == null) {
            targetWarehouse = warehouseRepository.findByIsActiveTrueOrderByNameAsc().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Nenhum armazém activo encontrado."));
            purchase.setTargetWarehouse(targetWarehouse);
        } else {
            Warehouse managedWarehouse = warehouseRepository.findById(targetWarehouse.getId())
                    .orElseThrow(() -> new IllegalStateException("Armazém selecionado não existe."));
            purchase.setTargetWarehouse(managedWarehouse);
            targetWarehouse = managedWarehouse;
        }

        // Reverse previous stock on edit
        if (isEdit && previousPurchaseId != null) {
            try {
                Purchase fullPrevious = purchaseRepository.findByIdWithItems(previousPurchaseId);
                if (fullPrevious != null && "RECEIVED".equals(fullPrevious.getState())) {
                    Warehouse prevWarehouse = fullPrevious.getTargetWarehouse() != null ? fullPrevious.getTargetWarehouse() : targetWarehouse;
                    for (PurchaseItem oldItem : fullPrevious.getItems()) {
                        if (oldItem.getProduct() != null && oldItem.getQuantity() != null && oldItem.getQuantity() > 0) {
                            warehouseService.removeStock(
                                    prevWarehouse.getId(),
                                    oldItem.getProduct().getId(),
                                    oldItem.getQuantity(),
                                    "REVERSAO-EDICAO-" + (fullPrevious.getInvoiceNumber() != null ? fullPrevious.getInvoiceNumber() : ""),
                                    currentUser);
                        }
                    }
                }
            } catch (Exception ex) {
                systemLogService.logError("PURCHASE_REVERSE_FAILED", "Falha ao reverter stock anterior para compra id=" + previousPurchaseId, ex);
                throw ex;
            }
        }

        purchase.getItems().clear();
        purchase.getItems().addAll(savedItems);

        Purchase saved = purchaseRepository.save(purchase);

        if ("RECEIVED".equals(saved.getState())) {
            for (PurchaseItem item : savedItems) {
                if (item.getProduct() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    warehouseService.addStock(
                            targetWarehouse.getId(),
                            item.getProduct().getId(),
                            item.getQuantity(),
                            saved.getInvoiceNumber() != null ? saved.getInvoiceNumber() : "COMPRA",
                            currentUser,
                            item.getCostPrice());

                    // Atualização automática de custo do produto
                    if (item.getCostPrice() != null && item.getCostPrice() > 0) {
                        Product p = item.getProduct();
                        double oldCost = p.getPriceCost() != null ? p.getPriceCost() : 0.0;
                        double newCost = item.getCostPrice();
                        if (oldCost <= 0.0) {
                            p.setPriceCost(newCost);
                        } else {
                            p.setPriceCost((oldCost + newCost) / 2.0);
                        }
                        productRepository.save(p);
                    }
                }
            }
        }

        return saved;
    }

    public boolean isDuplicateInvoiceForSupplier(String invoiceNumber, Long supplierId, Long excludePurchaseId) {
        if (invoiceNumber == null || invoiceNumber.isBlank() || supplierId == null) {
            return false;
        }
        java.util.Optional<Purchase> existing = purchaseRepository.findByInvoiceNumberIgnoreCase(invoiceNumber.trim());
        if (existing.isEmpty()) {
            return false;
        }
        Purchase p = existing.get();
        if (p.getSupplier() == null || p.getSupplier().getId() == null) {
            return false;
        }
        if (!supplierId.equals(p.getSupplier().getId())) {
            return false;
        }
        return excludePurchaseId == null || !excludePurchaseId.equals(p.getId());
    }
}
