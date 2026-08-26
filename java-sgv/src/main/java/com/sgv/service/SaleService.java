package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.*;
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
    private final CashSessionService cashSessionService;
    private final CustomerAccountLedger customerAccountLedger;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

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
                       AppConfigService appConfigService,
                       CashSessionService cashSessionService,
                       CustomerAccountLedger customerAccountLedger) {
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
        this.cashSessionService = cashSessionService;
        this.customerAccountLedger = customerAccountLedger;
    }

    @Transactional
    public Sale persistSaleTransaction(Sale sale, User currentUser) throws Exception {
        if (sale == null) throw new IllegalArgumentException("Sale is null");
        if (trainingModeService != null && trainingModeService.isTrainingMode()) {
            throw new IllegalStateException("Operação bloqueada: modo treino está activo.");
        }
        requirePermission(currentUser, "VENDAS", "CREATE");

        Sale persistentSale = normalizeSaleForPersistence(sale, currentUser);
        applyDocumentState(persistentSale);

        // Ensure createdAt
        if (persistentSale.getCreatedAt() == null) {
            persistentSale.setCreatedAt(LocalDateTime.now());
        }

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

        validateSaleForPersistence(persistentSale);
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

            // Validate stock (serviços não movimentam existências físicas)
            for (SaleItem item : persistentSale.getItems()) {
                if (item.getProduct() == null) continue;
                if (isServiceItem(item.getProduct())) continue;
                if (!Boolean.TRUE.equals(item.getProduct().getIsActive())) {
                    throw new IllegalStateException("Produto inactivo não pode ser vendido: " + item.getProduct().getCode());
                }
                StockBranch sb = stockMap.get(item.getProduct().getId());
                java.math.BigDecimal avail = sb != null && sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : java.math.BigDecimal.ZERO;
                if (avail.compareTo(qty) < 0) {
                    throw new IllegalStateException("Stock insuficiente para " + item.getProductCode());
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

        // Abater stock ANTES do movimento de caixa — serviços não entram aqui.
        com.sgv.model.DocumentType dt = com.sgv.model.DocumentType.fromString(persistentSale.getDocumentType());
        if (dt == com.sgv.model.DocumentType.VENDA || dt == com.sgv.model.DocumentType.FACTURA || dt == com.sgv.model.DocumentType.RECIBO) {
            if (persistentSale.getBranch() != null && stockBranchService != null) {
                List<Long> soldProductIds = saved.getItems().stream()
                        .filter(i -> i.getProduct() != null
                                && !isServiceItem(i.getProduct())
                                && i.getQtyAmount() != null
                                && i.getQtyAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                        .map(i -> i.getProduct().getId())
                        .distinct()
                        .collect(java.util.stream.Collectors.toList());

                java.util.Map<Long, StockBranch> stockMap2 = new java.util.HashMap<>();
                if (!soldProductIds.isEmpty()) {
                    stockBranchService.loadStockForBranchAndProducts(persistentSale.getBranch().getId(), soldProductIds)
                            .forEach(sb -> stockMap2.put(sb.getProduct().getId(), sb));
                }

                for (SaleItem item : saved.getItems()) {
                    if (item.getProduct() == null) continue;
                    if (isServiceItem(item.getProduct())) continue;
                    java.math.BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : java.math.BigDecimal.ZERO;
                    if (qty.compareTo(java.math.BigDecimal.ZERO) <= 0) continue;

                    StockBranch sb = stockMap2.get(item.getProduct().getId());
                    if (sb == null) {
                        throw new IllegalStateException("Stock insuficiente para " + item.getProduct().getCode()
                                + " na filial " + persistentSale.getBranch().getName() + " (sem ficha de stock).");
                    }
                    java.math.BigDecimal before = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal after = before.subtract(qty);
                    if (after.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        throw new IllegalStateException("Stock insuficiente na filial para o produto: " + item.getProduct().getCode());
                    }
                    sb.setStockCurrentAmount(after);
                    stockBranchService.saveStockBranch(sb);

                    stockBranchService.saveStockMovement(
                            sb, qty.negate(), before, after, "SAIDA", "VENDA",
                            "VENDA-" + saved.getDocumentNumber() + "/" + saved.getSeries(),
                            currentUser
                    );
                }
            }
        }

        // Caixa só depois do stock — se o abate falhar, não fica movimento órfão no log.
        com.sgv.model.DocumentType paymentDocumentType = com.sgv.model.DocumentType.fromString(persistentSale.getDocumentType());
        if (paymentDocumentType == com.sgv.model.DocumentType.VENDA || paymentDocumentType == com.sgv.model.DocumentType.FACTURA || paymentDocumentType == com.sgv.model.DocumentType.RECIBO) {
            // Correção do BUG-003: só o numerário (DINHEIRO) move a gaveta.
            // M-Pesa/POS/transferência/cheque não são entradas de caixa física
            // (deixam de criar sobra/quebra falsa no fecho/fita Z).
            if (com.sgv.model.PaymentMethod.movesCashDrawer(persistentSale.getPaymentMethod())) {
                try {
                    java.math.BigDecimal saleTotal = saved.getTotalAmount() != null ? saved.getTotalAmount() : java.math.BigDecimal.ZERO;
                    String saleReference = saved.getSeries() != null && saved.getDocumentNumber() != null
                            ? "Venda " + saved.getDocumentNumber() + "/" + saved.getSeries()
                            : "Venda";
                    String reason = saved.getPaymentMethod() != null ? saved.getPaymentMethod() : "VENDA";
                    cashSessionService.registerMovement(currentUser, "IN", saleTotal, saleReference, reason, com.sgv.entity.OperationKind.SALE, false);
                } catch (Exception ex) {
                    throw new IllegalStateException("Falha ao registar movimento de caixa para esta venda: " + ex.getMessage(), ex);
                }
            }
        }

        if ("CREDITO".equals(persistentSale.getPaymentMethod()) && persistentSale.getCustomer() != null && customerRepository != null) {
            Customer cust = customerRepository.findById(persistentSale.getCustomer().getId()).orElse(null);
            if (cust != null) {
                java.math.BigDecimal saleTotal = persistentSale.getTotalAmount() != null ? persistentSale.getTotalAmount() : java.math.BigDecimal.ZERO;
                // Correção do BUG-005/010: o saldo e o lançamento são escritos
                // atomicamente no livro de conta corrente — o extrato deixa de
                // ser derivado das vendas e passa a reflectir o saldo real.
                customerAccountLedger.recordEntry(cust.getId(), saved.getId(), null,
                        CustomerAccountLedger.TYPE_CREDITO_SALE, saleTotal,
                        docRef(saved), "Factura a crédito");
            }
        }
        return saved;
    }

    /**
     * Orquestrador de venda de alta disponibilidade:
     * A transação do banco é executada e finalizada rapidamente via persistSaleTransaction;
     * A geração de ficheiro PDF/I/O é realizada fora do lock transacional.
     */
    public File processAndSave(Sale sale, User currentUser) throws Exception {
        Sale saved = persistSaleTransaction(sale, currentUser);

        // Geração de documento/impressão fora da transação do banco
        String fmt = com.sgv.desktop.SaleFormController.atDefaultFormat(saved.getDocumentType());
        if ("thermal-80mm".equals(fmt)) {
            return thermalPrintService.printReceipt(saved);
        }
        return saleDocumentService.generateDocument(saved);
    }

    @Transactional
    public Sale createCreditNote(Sale originalSale, String reason, User currentUser) {
        if (originalSale == null || originalSale.getId() == null) {
            throw new IllegalArgumentException("Venda original é obrigatória para emitir uma nota de crédito.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalStateException("O motivo da nota de crédito é obrigatório.");
        }
        requirePermission(currentUser, "VENDAS", "CREATE");

        Sale baseSale = saleRepository.findByIdWithItems(originalSale.getId());
        if (baseSale == null) {
            baseSale = originalSale;
        }

        Sale creditNote = new Sale();
        creditNote.setBranch(baseSale.getBranch());
        creditNote.setCustomer(baseSale.getCustomer());
        creditNote.setCustomerName(baseSale.getCustomerName());
        creditNote.setCustomerNuit(baseSale.getCustomerNuit());
        creditNote.setCustomerAddress(baseSale.getCustomerAddress());
        creditNote.setPaymentMethod(baseSale.getPaymentMethod());
        creditNote.setOriginSale(baseSale);
        creditNote.setDocumentType(com.sgv.model.DocumentType.NC.name());
        creditNote.setSeries(baseSale.getSeries());
        creditNote.setDocumentYear(LocalDateTime.now().getYear());
        creditNote.setCreatedAt(LocalDateTime.now());
        creditNote.setAnnulReason(reason);

        List<SaleItem> creditItems = new ArrayList<>();
        if (baseSale.getItems() != null) {
            for (SaleItem item : baseSale.getItems()) {
                if (item == null) continue;
                SaleItem creditItem = new SaleItem();
                creditItem.setSale(creditNote);
                creditItem.setProduct(item.getProduct());
                creditItem.setProductCode(item.getProductCode());
                creditItem.setUnit(item.getUnit());
                creditItem.setDescription(item.getDescription());

                BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : BigDecimal.ZERO;
                BigDecimal negativeQty = qty.negate();
                BigDecimal lineBase = item.getLineBaseAmount() != null ? item.getLineBaseAmount() : BigDecimal.ZERO;
                BigDecimal lineTax = item.getLineTaxAmount() != null ? item.getLineTaxAmount() : BigDecimal.ZERO;
                BigDecimal lineIce = item.getLineIceAmount() != null ? item.getLineIceAmount() : BigDecimal.ZERO;
                BigDecimal lineDiscount = item.getLineDiscountAmount() != null ? item.getLineDiscountAmount() : BigDecimal.ZERO;
                BigDecimal lineTotal = item.getLineTotalAmount() != null ? item.getLineTotalAmount() : BigDecimal.ZERO;

                creditItem.setQtyAmount(negativeQty);
                creditItem.setQty(negativeQty.doubleValue());
                creditItem.setUnitPriceAmount(item.getUnitPriceAmount());
                creditItem.setUnitPrice(item.getUnitPriceAmount() != null ? item.getUnitPriceAmount().doubleValue() : null);
                creditItem.setLineBaseAmount(lineBase.negate());
                creditItem.setLineTaxAmount(lineTax.negate());
                creditItem.setLineIceAmount(lineIce.negate());
                creditItem.setLineDiscountAmount(lineDiscount.negate());
                creditItem.setLineTotalAmount(lineTotal.negate());
                creditItems.add(creditItem);
            }
        }
        creditNote.setItems(creditItems);

        BigDecimal subtotal = creditItems.stream()
                .map(SaleItem::getLineBaseAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTax = creditItems.stream()
                .map(SaleItem::getLineTaxAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIce = creditItems.stream()
                .map(SaleItem::getLineIceAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        creditNote.setSubtotalAmount(subtotal);
        creditNote.setTotalTaxAmount(totalTax);
        creditNote.setTotalIceAmount(totalIce);
        creditNote.setTotalAmount(subtotal.add(totalIce).add(totalTax));
        creditNote.setPaidAmountValue(BigDecimal.ZERO);
        creditNote.setChangeAmountValue(BigDecimal.ZERO);

        normalizeSaleForPersistence(creditNote, currentUser);
        applyDocumentState(creditNote);
        if (creditNote.getDocumentNumber() == null) {
            Long branchId = creditNote.getBranch() != null ? creditNote.getBranch().getId() : null;
            creditNote.setDocumentNumber(saleNumberingService.nextDocumentNumber(branchId, creditNote.getSeries(), creditNote.getDocumentType()));
        }

        Sale saved = saleRepository.save(creditNote);

        if (saved.getItems() != null) {
            for (SaleItem item : saved.getItems()) {
                if (item.getProduct() == null) continue;
                if (Boolean.TRUE.equals(item.getProduct().getService())) continue;
                BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : BigDecimal.ZERO;
                if (qty.compareTo(BigDecimal.ZERO) >= 0) continue;
                stockBranchService.increaseStock(
                        saved.getBranch(),
                        item.getProduct(),
                        qty.abs(),
                        "NC-" + saved.getDocumentNumber() + "/" + saved.getSeries(),
                        "DEVOLUCAO",
                        currentUser);
            }
        }

        // =====================================================================
        // Correção do BUG-010: a nota de crédito tem de REVERTER os efeitos da
        // venda original, não apenas o stock:
        //  - venda a CRÉDITO → baixa da dívida do cliente (lançamento CREDIT_NOTE
        //    no livro de conta corrente — sem isto o extrato e a dívida nunca
        //    reflectiam a devolução);
        //  - venda em CAIXA  → estorno físico (OUT) na gaveta do operador, SEM
        //    catch engolido: se não houver turno aberto, a NC não é emitida
        //    (rollback total), porque uma devolução sem estorno deixa a gaveta
        //    com dinheiro a mais.
        // =====================================================================
        BigDecimal ncValue = saved.getTotalAmount() != null ? saved.getTotalAmount().abs() : BigDecimal.ZERO;
        if (ncValue.signum() > 0) {
            boolean wasCreditSale = "CREDITO".equalsIgnoreCase(baseSale.getPaymentMethod())
                    && baseSale.getCustomer() != null && baseSale.getCustomer().getId() != null;
            boolean wasCashSale = com.sgv.model.PaymentMethod.movesCashDrawer(baseSale.getPaymentMethod());
            if (wasCreditSale) {
                Customer cust = customerRepository.findById(baseSale.getCustomer().getId()).orElse(null);
                if (cust != null) {
                    customerAccountLedger.recordEntry(cust.getId(), baseSale.getId(), null,
                            CustomerAccountLedger.TYPE_CREDIT_NOTE, ncValue.negate(),
                            docRef(saved),
                            "Nota de crédito sobre a venda original " + docRef(baseSale));
                }
            }
            if (wasCashSale && cashSessionService != null && currentUser != null) {
                String ref = "NC " + docRef(saved) + " (estorno de " + docRef(baseSale) + ")";
                cashSessionService.registerMovement(currentUser, "OUT", ncValue, ref, "DEVOLUCAO",
                        com.sgv.entity.OperationKind.REFUND, true);
            }
        }

        return saved;
    }

    /** Referência curta de um documento: "série/número" (ou id quando ainda sem número). */
    private static String docRef(Sale s) {
        String series = s.getSeries() != null ? s.getSeries() : "A";
        Object number = s.getDocumentNumber() != null ? s.getDocumentNumber() : s.getId();
        return series + "/" + number;
    }

    @Transactional
    public Sale annulSale(Sale sale, String reason, User currentUser) {
        if (sale == null) throw new IllegalArgumentException("Sale is null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalStateException("O motivo da anulação é obrigatório por lei.");
        }
        requirePermission(currentUser, "VENDAS", "DELETE");

        Sale persistentSale = saleRepository.findByIdWithItems(sale.getId());
        if (persistentSale == null) {
            persistentSale = sale;
        }

        if ("ANULADA".equalsIgnoreCase(persistentSale.getState())) {
            throw new IllegalStateException("Venda já Anulada: Este documento já se encontra anulado.");
        }

        persistentSale.setState("ANULADA");
        persistentSale.setAnnulReason(reason);
        persistentSale.setAnnulDate(LocalDateTime.now());
        persistentSale.setAnnulInProgress(true);
        Sale saved = saleRepository.save(persistentSale);

        if (saved.getItems() != null) {
            for (var item : saved.getItems()) {
                if (item.getProduct() == null) continue;
                if (Boolean.TRUE.equals(item.getProduct().getService())) continue;
                BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : BigDecimal.ZERO;
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
                var increased = stockBranchService.increaseStock(
                    saved.getBranch(), item.getProduct(), qty,
                    "ANULACAO-" + saved.getDocumentNumber() + "/" + saved.getSeries(),
                    "ANULACAO_VENDA", currentUser);
            }
            if (entityManager != null) entityManager.flush();
        }

        if ("CREDITO".equalsIgnoreCase(saved.getPaymentMethod()) && saved.getCustomer() != null) {
            Customer cust = customerRepository.findById(saved.getCustomer().getId()).orElse(null);
            if (cust != null) {
                BigDecimal totalAmount = saved.getTotalAmount() != null ? saved.getTotalAmount() : BigDecimal.ZERO;
                // Correção do BUG-005/010: a anulação abre LANÇAMENTO no livro
                // (o extrato reflecte a baixa da dívida; antes só o saldo mudava).
                customerAccountLedger.recordEntry(cust.getId(), saved.getId(), null,
                        CustomerAccountLedger.TYPE_ANNULMENT, totalAmount.negate(),
                        docRef(saved), "Anulação de venda a crédito");
                if (entityManager != null) entityManager.flush();
            }
        } else {
            // Estorno de tesouraria / saída de caixa para vendas a pronto que
            // movimentaram caixa. Simetria do BUG-003: só estorna para a gaveta
            // se a venda original era numerário (um M-Pesa/POS que nunca entrou
            // na gaveta não pode sair dela).
            com.sgv.model.DocumentType docType = com.sgv.model.DocumentType.fromString(saved.getDocumentType());
            boolean fiscalDoc = docType == com.sgv.model.DocumentType.VENDA
                    || docType == com.sgv.model.DocumentType.FACTURA
                    || docType == com.sgv.model.DocumentType.RECIBO;
            if (fiscalDoc && com.sgv.model.PaymentMethod.movesCashDrawer(saved.getPaymentMethod())) {
                if (cashSessionService != null && currentUser != null) {
                    try {
                        BigDecimal saleTotal = saved.getTotalAmount() != null ? saved.getTotalAmount() : BigDecimal.ZERO;
                        String ref = "Estorno " + (saved.getDocumentType() != null ? saved.getDocumentType() : "DOC")
                                + " #" + (saved.getDocumentNumber() != null ? saved.getDocumentNumber() : saved.getId())
                                + "/" + (saved.getSeries() != null ? saved.getSeries() : "A");
                        cashSessionService.registerMovement(currentUser, "OUT", saleTotal, ref, "ANULACAO_VENDA", com.sgv.entity.OperationKind.REFUND, false);
                    } catch (Exception ignored) {
                        // Se o operador não tiver sessão aberta no momento da anulação, prossegue sem travar a anulação fiscal
                    }
                }
            }
        }

        return saved;
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

    private void applyDocumentState(Sale sale) {
        if (sale == null || sale.getDocumentType() == null) return;
        com.sgv.model.DocumentType dt = com.sgv.model.DocumentType.fromString(sale.getDocumentType());
        if (dt == null) return;

        switch (dt) {
            case COTACAO -> sale.setState(com.sgv.model.SaleState.COTACAO_ABERTA.name());
            case ENCOMENDA -> sale.setState(com.sgv.model.SaleState.ENCOMENDA_ABERTA.name());
            case VENDA, FACTURA -> sale.setState(com.sgv.model.SaleState.PAGO.name());
            case RECIBO -> sale.setState(com.sgv.model.SaleState.EMITIDA.name());
            default -> sale.setState(com.sgv.model.SaleState.EMITIDA.name());
        }
    }

    private void validateSaleForPersistence(Sale sale) {
        if (sale == null) throw new IllegalArgumentException("Sale is null");
        if (sale.getBranch() == null || sale.getBranch().getId() == null) {
            throw new IllegalStateException("Filial é obrigatória para gravar a venda.");
        }
        if (sale.getDocumentType() == null) {
            throw new IllegalStateException("Tipo de documento é obrigatório.");
        }
        if (com.sgv.model.DocumentType.FACTURA.name().equals(sale.getDocumentType())) {
            if (sale.getCustomer() == null || sale.getCustomer().getId() == null) {
                throw new IllegalStateException("Factura requer um cliente válido.");
            }
            String nuit = sale.getCustomerNuit();
            if (nuit == null || nuit.isBlank()) {
                throw new IllegalStateException("Cliente para factura deve ter NUIT.");
            }
            if (!com.sgv.util.NuitValidator.isValid(nuit)) {
                throw new IllegalStateException("NUIT do cliente inválido.");
            }
        }
        if ("CREDITO".equals(sale.getPaymentMethod())) {
            if (sale.getCustomer() == null || sale.getCustomer().getId() == null) {
                throw new IllegalStateException("Venda a crédito requer um cliente registado.");
            }
            if (customerRepository != null) {
                Customer cust = customerRepository.findById(sale.getCustomer().getId()).orElse(null);
                if (cust != null) {
                    BigDecimal limit = cust.getCreditLimitAmount();
                    if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal curBal = cust.getBalanceAmount() != null ? cust.getBalanceAmount() : BigDecimal.ZERO;
                        BigDecimal saleTot = sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO;
                        if (curBal.add(saleTot).compareTo(limit) > 0) {
                            throw new IllegalStateException(String.format(
                                "Limite de crédito excedido para '%s'. Limite Máx: %,.2f MT | Dívida Atual: %,.2f MT | Total Venda: %,.2f MT",
                                cust.getName(), limit, curBal, saleTot
                            ));
                        }
                    }
                }
            }
        }
        if (sale.getItems() == null || sale.getItems().isEmpty()) {
            throw new IllegalStateException("A venda deve conter ao menos um item.");
        }
    }

    private static boolean isServiceItem(Product product) {
        return product != null && Boolean.TRUE.equals(product.getService());
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
