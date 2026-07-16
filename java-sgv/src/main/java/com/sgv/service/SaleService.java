package com.sgv.service;

import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import com.sgv.entity.User;
import com.sgv.entity.Role;
import com.sgv.repository.SaleRepository;
import com.sgv.repository.SaleItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockBranchService stockBranchService;
    private final SaleNumberingService saleNumberingService;
    private final SaleDocumentService saleDocumentService;
    private final ThermalPrintService thermalPrintService;

    public SaleService(SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository,
                       StockBranchService stockBranchService,
                       SaleNumberingService saleNumberingService,
                       SaleDocumentService saleDocumentService,
                       ThermalPrintService thermalPrintService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockBranchService = stockBranchService;
        this.saleNumberingService = saleNumberingService;
        this.saleDocumentService = saleDocumentService;
        this.thermalPrintService = thermalPrintService;
    }

    @Transactional
    public File processAndSave(Sale sale, User currentUser) throws Exception {
        if (sale == null) throw new IllegalArgumentException("Sale is null");
        requirePermission(currentUser, "VENDAS", "CREATE");

        // Ensure series/year/number
        if (sale.getSeries() == null) sale.setSeries("A");
        if (sale.getDocumentYear() == null) sale.setDocumentYear(LocalDateTime.now().getYear());
        if (sale.getDocumentNumber() == null) {
            Long branchId = sale.getBranch() != null ? sale.getBranch().getId() : null;
            sale.setDocumentNumber(saleNumberingService.nextDocumentNumber(branchId, sale.getSeries(), sale.getDocumentType()));
        }

        // Totals
        java.math.BigDecimal baseTotal = sale.getItems().stream()
                .map(i -> i.getLineBaseAmount() != null ? i.getLineBaseAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalIce = sale.getItems().stream()
                .map(i -> i.getLineIceAmount() != null ? i.getLineIceAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalTax = sale.getItems().stream()
                .map(i -> i.getLineTaxAmount() != null ? i.getLineTaxAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        sale.setSubtotalAmount(baseTotal);
        sale.setTotalIceAmount(totalIce);
        sale.setTotalTaxAmount(totalTax);
        sale.setTotalAmount(baseTotal.add(totalIce).add(totalTax));

        // Stock validation
        if (sale.getBranch() != null) {
            for (SaleItem item : sale.getItems()) {
                if (item.getProduct() == null) continue;
                if (Boolean.TRUE.equals(item.getProduct().getService())) continue;
                java.math.BigDecimal avail = stockBranchService.getCurrentStock(sale.getBranch(), item.getProduct());
                java.math.BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : java.math.BigDecimal.ZERO;
                if (avail.compareTo(qty) < 0) {
                    throw new IllegalStateException("Estoque insuficiente para " + item.getProductCode());
                }
            }
        }

        // Persist sale (cascade will persist items)
        Sale saved = saleRepository.save(sale);

        // Decrement stock for fiscal sales
        com.sgv.model.DocumentType dt = com.sgv.model.DocumentType.fromString(sale.getDocumentType());
        if (dt == com.sgv.model.DocumentType.VENDA || dt == com.sgv.model.DocumentType.FACTURA || dt == com.sgv.model.DocumentType.RECIBO) {
            for (SaleItem item : saved.getItems()) {
                if (item.getProduct() == null) continue;
                java.math.BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : java.math.BigDecimal.ZERO;
                if (qty.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;
                stockBranchService.decreaseStock(
                        saved.getBranch(),
                        item.getProduct(),
                        qty,
                        "VENDA-" + saved.getDocumentNumber() + "/" + saved.getSeries(),
                        "VENDA",
                        currentUser
                );
            }
        }

        // Generate PDF or thermal
        String fmt = com.sgv.desktop.SaleFormController.atDefaultFormat(sale.getDocumentType());
        if ("thermal-80mm".equals(fmt)) {
            return thermalPrintService.printReceipt(saved);
        }
        return saleDocumentService.generateDocument(saved);
    }

    private void requirePermission(User user, String page, String action) {
        if (user == null) {
            throw new IllegalStateException("Utilizador não autenticado.");
        }
        boolean hasPermission = user.hasPermission(page, action);
        if (!hasPermission) {
            throw new IllegalStateException("Não tem permissão para " + page + ":" + action + ".");
        }
    }
}
