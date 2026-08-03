package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockBranchService stockBranchService;
    private final SaleNumberingService saleNumberingService;
    private final SaleDocumentService saleDocumentService;
    private final ThermalPrintService thermalPrintService;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final FiscalService fiscalService;
    private final TrainingModeService trainingModeService;
    private final AppConfigService appConfigService;

    @Autowired
    public SaleService(SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository,
                       StockBranchService stockBranchService,
                       SaleNumberingService saleNumberingService,
                       SaleDocumentService saleDocumentService,
                       ThermalPrintService thermalPrintService,
                       ProductRepository productRepository,
                       BranchRepository branchRepository,
                       CustomerRepository customerRepository,
                       FiscalService fiscalService,
                       TrainingModeService trainingModeService,
                       AppConfigService appConfigService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockBranchService = stockBranchService;
        this.saleNumberingService = saleNumberingService;
        this.saleDocumentService = saleDocumentService;
        this.thermalPrintService = thermalPrintService;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.customerRepository = customerRepository;
        this.fiscalService = fiscalService;
        this.trainingModeService = trainingModeService;
        this.appConfigService = appConfigService;
    }

    @Transactional
    public File processAndSave(Sale sale, User currentUser) throws Exception {
        if (sale == null) throw new IllegalArgumentException("Sale is null");
        if (trainingModeService != null && trainingModeService.isTrainingMode()) {
            throw new IllegalStateException("Operação bloqueada: modo treino está activo.");
        }
        requirePermission(currentUser, "VENDAS", "CREATE");

        Sale persistentSale = normalizeSaleForPersistence(sale, currentUser);

        // Ensure series/year/number
        if (persistentSale.getSeries() == null) {
            String series = appConfigService != null ? appConfigService.get().getDefaultSeries() : "A";
            persistentSale.setSeries(series != null ? series : "A");
        }
        if (persistentSale.getDocumentYear() == null) persistentSale.setDocumentYear(LocalDateTime.now().getYear());
        if (persistentSale.getDocumentNumber() == null) {
            Long branchId = persistentSale.getBranch() != null ? persistentSale.getBranch().getId() : null;
            persistentSale.setDocumentNumber(saleNumberingService.nextDocumentNumber(branchId, persistentSale.getSeries(), persistentSale.getDocumentType()));
        }

        // Totals
        java.math.BigDecimal baseTotal = persistentSale.getItems().stream()
                .map(i -> i.getLineBaseAmount() != null ? i.getLineBaseAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalIce = persistentSale.getItems().stream()
                .map(i -> i.getLineIceAmount() != null ? i.getLineIceAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalTax = persistentSale.getItems().stream()
                .map(i -> i.getLineTaxAmount() != null ? i.getLineTaxAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        persistentSale.setSubtotalAmount(baseTotal);
        persistentSale.setTotalIceAmount(totalIce);
        persistentSale.setTotalTaxAmount(totalTax);
        persistentSale.setTotalAmount(baseTotal.add(totalIce).add(totalTax));

        // C3: Set paidAmount / changeAmount
        if ("CREDITO".equals(persistentSale.getPaymentMethod())) {
            persistentSale.setPaidAmountValue(java.math.BigDecimal.ZERO);
        } else {
            persistentSale.setPaidAmountValue(persistentSale.getTotalAmount());
            persistentSale.setChangeAmountValue(java.math.BigDecimal.ZERO);
        }

        // C2: Generate fiscal hashes
        if (fiscalService != null) {
            persistentSale.setHashHash(fiscalService.generateHashHash(persistentSale));
            persistentSale.setSignatureHash(fiscalService.generateSignatureHash(persistentSale));
            if (persistentSale.getBranch() != null) {
                persistentSale.setHashControl(fiscalService.assignHashControlForBranch(persistentSale.getBranch().getId()));
            }
        }

        // Stock validation + decrement — batch-load StockBranch records
        if (persistentSale.getBranch() != null) {
            List<Long> productIdsForStock = persistentSale.getItems().stream()
                    .filter(i -> i.getProduct() != null)
                    .map(i -> i.getProduct().getId())
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            java.util.Map<Long, StockBranch> stockMap = new java.util.HashMap<>();
            if (stockBranchService != null && !productIdsForStock.isEmpty()) {
                // Single query to load all StockBranch records for this branch + products
                stockBranchService.loadStockForBranchAndProducts(persistentSale.getBranch().getId(), productIdsForStock)
                        .forEach(sb -> stockMap.put(sb.getProduct().getId(), sb));
            }

            // Validate stock
            for (SaleItem item : persistentSale.getItems()) {
                if (item.getProduct() == null) continue;
                if (Boolean.TRUE.equals(item.getProduct().getService())) continue;
                if (!Boolean.TRUE.equals(item.getProduct().getIsActive())) {
                    throw new IllegalStateException("Produto inactivo não pode ser vendido: " + item.getProduct().getCode());
                }
                StockBranch sb = stockMap.get(item.getProduct().getId());
                java.math.BigDecimal avail = sb != null && sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : java.math.BigDecimal.ZERO;
                if (avail.compareTo(qty) < 0) {
                    throw new IllegalStateException("Estoque insuficiente para " + item.getProductCode());
                }
            }
        }

        // Persist sale (cascade will persist items)
        Sale saved = saleRepository.save(persistentSale);

        // Se esta venda originou de uma cotação, atualizar o estado da cotação original
        if (saved.getOriginSale() != null) {
            Sale origin = saleRepository.findById(saved.getOriginSale().getId()).orElse(null);
            if (origin != null && "COTACAO_ABERTA".equals(origin.getState())) {
                origin.setState("COTACAO_PAGA");
                saleRepository.save(origin);
            }
        }

        // C4: Update customer balance for credit sales
        if ("CREDITO".equals(persistentSale.getPaymentMethod()) && persistentSale.getCustomer() != null && customerRepository != null) {
            Customer cust = customerRepository.findById(persistentSale.getCustomer().getId()).orElse(null);
            if (cust != null) {
                java.math.BigDecimal currentBalance = cust.getBalanceAmount();
                java.math.BigDecimal saleTotal = persistentSale.getTotalAmount() != null ? persistentSale.getTotalAmount() : java.math.BigDecimal.ZERO;
                cust.setBalanceAmount(currentBalance.add(saleTotal));
                customerRepository.save(cust);
            }
        }

        // Decrement stock for fiscal sales — batch operation
        com.sgv.model.DocumentType dt = com.sgv.model.DocumentType.fromString(persistentSale.getDocumentType());
        if (dt == com.sgv.model.DocumentType.VENDA || dt == com.sgv.model.DocumentType.FACTURA || dt == com.sgv.model.DocumentType.RECIBO) {
            if (persistentSale.getBranch() != null && stockBranchService != null) {
                List<Long> soldProductIds = saved.getItems().stream()
                        .filter(i -> i.getProduct() != null && i.getQtyAmount() != null && i.getQtyAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                        .map(i -> i.getProduct().getId())
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());

                // Re-fetch StockBranch records (they may have changed after save)
                java.util.Map<Long, StockBranch> stockMap2 = new java.util.HashMap<>();
                if (!soldProductIds.isEmpty()) {
                    stockBranchService.loadStockForBranchAndProducts(persistentSale.getBranch().getId(), soldProductIds)
                            .forEach(sb -> stockMap2.put(sb.getProduct().getId(), sb));
                }

                for (SaleItem item : saved.getItems()) {
                    if (item.getProduct() == null) continue;
                    java.math.BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : java.math.BigDecimal.ZERO;
                    if (qty.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;

                    StockBranch sb = stockMap2.get(item.getProduct().getId());
                    if (sb == null) {
                        throw new IllegalStateException("Stock da filial não encontrado para o produto: " + item.getProduct().getCode());
                    }
                    java.math.BigDecimal before = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal after = before.subtract(qty);
                    if (after.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        throw new IllegalStateException("Stock insuficiente na filial para o produto: " + item.getProduct().getCode());
                    }
                    sb.setStockCurrentAmount(after);
                    stockBranchService.saveStockBranch(sb);

                    // Record movement
                    stockBranchService.saveStockMovement(
                            sb, qty.negate(), before, after, "SAIDA", "VENDA",
                            "VENDA-" + saved.getDocumentNumber() + "/" + saved.getSeries(),
                            currentUser
                    );
                }
            }
        }

        // Generate PDF or thermal
        String fmt = com.sgv.desktop.SaleFormController.atDefaultFormat(persistentSale.getDocumentType());
        if ("thermal-80mm".equals(fmt)) {
            return thermalPrintService.printReceipt(saved);
        }
        return saleDocumentService.generateDocument(saved);
    }

    Sale normalizeSaleForPersistence(Sale sale, User currentUser) {
        if (sale == null) throw new IllegalArgumentException("Sale is null");
        if (branchRepository != null && sale.getBranch() != null && sale.getBranch().getId() != null) {
            Branch managedBranch = branchRepository.findById(sale.getBranch().getId()).orElse(sale.getBranch());
            sale.setBranch(managedBranch);
        }
        if (customerRepository != null && sale.getCustomer() != null && sale.getCustomer().getId() != null) {
            Customer managedCustomer = customerRepository.findById(sale.getCustomer().getId()).orElse(sale.getCustomer());
            sale.setCustomer(managedCustomer);
        }
        if (sale.getItems() == null) {
            sale.setItems(new ArrayList<>());
            return sale;
        }

        // Batch-load all products at once instead of N+1 individual findById
        List<Long> productIds = sale.getItems().stream()
                .filter(item -> item != null && item.getProduct() != null && item.getProduct().getId() != null)
                .map(item -> item.getProduct().getId())
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        Map<Long, Product> managedProducts = new java.util.HashMap<>();
        if (productRepository != null && !productIds.isEmpty()) {
            productRepository.findByIdIn(productIds).forEach(p -> managedProducts.put(p.getId(), p));
        }

        List<SaleItem> normalizedItems = new ArrayList<>();
        for (SaleItem item : sale.getItems()) {
            if (item == null) continue;
            item.setSale(sale);
            if (item.getProduct() != null && item.getProduct().getId() != null) {
                Product managedProduct = managedProducts.get(item.getProduct().getId());
                if (managedProduct != null) {
                    item.setProduct(managedProduct);
                }
                if (item.getProductCode() == null || item.getProductCode().isBlank()) {
                    item.setProductCode(item.getProduct() != null ? item.getProduct().getCode() : null);
                }
                if (item.getDescription() == null || item.getDescription().isBlank()) {
                    Product prod = item.getProduct();
                    if (prod != null) {
                        String desc = prod.getDescription();
                        item.setDescription(desc != null && !desc.isBlank() ? desc : prod.getName());
                    } else {
                        item.setDescription(null);
                    }
                }
                if ((item.getUnit() == null || item.getUnit().isBlank()) && item.getProduct() != null && item.getProduct().getUnit() != null) {
                    item.setUnit(item.getProduct().getUnit().getAbbreviation());
                }
                if (item.getUnitPriceAmount() == null && item.getProduct() != null) {
                    Double priceSale = item.getProduct().getPriceSale();
                    item.setUnitPriceAmount(priceSale != null ? BigDecimal.valueOf(priceSale) : BigDecimal.ZERO);
                }
            }
            if (item.getQtyAmount() == null && item.getQty() != null) {
                item.setQtyAmount(BigDecimal.valueOf(item.getQty()));
            } else if (item.getQtyAmount() == null) {
                item.setQtyAmount(BigDecimal.ONE);
            }
            if (item.getLineTotalAmount() == null && item.getLineTotal() != null) {
                item.setLineTotalAmount(BigDecimal.valueOf(item.getLineTotal()));
            }
            normalizedItems.add(item);
        }
        sale.setItems(normalizedItems);
        if (sale.getCreatedAt() == null) {
            sale.setCreatedAt(LocalDateTime.now());
        }
        return sale;
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
