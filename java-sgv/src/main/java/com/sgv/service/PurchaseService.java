package com.sgv.service;

import com.sgv.entity.Purchase;
import com.sgv.entity.PurchaseItem;
import com.sgv.entity.Warehouse;
import com.sgv.entity.Product;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
                    Product p = item.getProduct();
                    double oldCost = p.getPriceCost() != null ? p.getPriceCost() : 0.0;
                    double newCost = item.getCostPrice() != null ? item.getCostPrice() : 0.0;
                    double newQty = item.getQuantity();

                    // Obter stock antes da nova entrada
                    BigDecimal curStockBd = warehouseService.getCurrentStockAmount(targetWarehouse.getId(), p.getId());
                    double currentStock = curStockBd != null ? curStockBd.doubleValue() : 0.0;

                    warehouseService.addStock(
                            targetWarehouse.getId(),
                            p.getId(),
                            newQty,
                            saved.getInvoiceNumber() != null ? saved.getInvoiceNumber() : "COMPRA",
                            currentUser,
                            newCost);

                    // Recálculo do Preço Médio Ponderado (PMP / WAC - Padrão Contabilístico Moçambicano)
                    if (newCost > 0) {
                        if (currentStock <= 0 || oldCost <= 0) {
                            p.setPriceCost(newCost);
                        } else {
                            double weightedCost = ((currentStock * oldCost) + (newQty * newCost)) / (currentStock + newQty);
                            p.setPriceCost(Math.round(weightedCost * 10000.0) / 10000.0);
                        }
                        productRepository.save(p);
                    }
                }
            }
        }

        return saved;
    }

    @Transactional
    public void annulPurchase(Long purchaseId, com.sgv.entity.User currentUser) {
        if (purchaseId == null) throw new IllegalArgumentException("ID da compra é obrigatório.");
        Purchase p = purchaseRepository.findByIdWithItems(purchaseId);
        if (p == null) throw new IllegalArgumentException("Factura de compra não encontrada.");
        if ("CANCELLED".equalsIgnoreCase(p.getState())) {
            throw new IllegalStateException("Esta factura de compra já se encontra anulada.");
        }
        if (p.getPaidAmountValue() != null && p.getPaidAmountValue().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(String.format("Não é possível anular compra com pagamentos registados (Pago: %.2f MT). Anule primeiro os pagamentos associados.", p.getPaidAmountValue()));
        }

        if ("RECEIVED".equalsIgnoreCase(p.getState())) {
            Warehouse wh = p.getTargetWarehouse();
            if (wh != null) {
                for (PurchaseItem item : p.getItems()) {
                    if (item.getProduct() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                        warehouseService.removeStock(
                                wh.getId(),
                                item.getProduct().getId(),
                                item.getQuantity(),
                                "ANULACAO-COMPRA-" + (p.getInvoiceNumber() != null ? p.getInvoiceNumber() : p.getId()),
                                currentUser);
                    }
                }
            }
        }

        p.setState("CANCELLED");
        purchaseRepository.save(p);
        systemLogService.logUserAction(
                currentUser != null ? currentUser.getUsername() : "Sistema",
                "PURCHASE_ANNULLED",
                "Factura de compra " + (p.getInvoiceNumber() != null ? p.getInvoiceNumber() : p.getId()) + " anulada com sucesso.");
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
