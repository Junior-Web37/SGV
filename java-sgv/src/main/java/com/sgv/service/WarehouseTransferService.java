package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de transferências armazém → filial.
 * Estados:
 *   PENDING     — criada, ainda não enviada
 *   IN_TRANSIT  — aprovada/enviada, ainda não recebida
 *   COMPLETED   — recebida na filial, stock actualizado
 *   CANCELLED   — cancelada
 *
 * Movimento atómico:
 *   No COMPLETED: decrementa stock do armazém E incrementa stock da filial
 *   no mesmo @Transactional.
 */
@Service
public class WarehouseTransferService {

    private final WarehouseTransferRepository transferRepository;
    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final StockWarehouseRepository stockWarehouseRepository;
    private final WarehouseService warehouseService;
    private final StockBranchService stockBranchService;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SaleNumberingService saleNumberingService;

    public WarehouseTransferService(WarehouseTransferRepository transferRepository,
                                    WarehouseRepository warehouseRepository,
                                    BranchRepository branchRepository,
                                    StockWarehouseRepository stockWarehouseRepository,
                                    WarehouseService warehouseService,
                                    StockBranchService stockBranchService,
                                    ProductRepository productRepository,
                                    StockMovementRepository stockMovementRepository,
                                    SaleNumberingService saleNumberingService) {
        this.transferRepository = transferRepository;
        this.warehouseRepository = warehouseRepository;
        this.branchRepository = branchRepository;
        this.stockWarehouseRepository = stockWarehouseRepository;
        this.warehouseService = warehouseService;
        this.stockBranchService = stockBranchService;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.saleNumberingService = saleNumberingService;
    }

    public List<WarehouseTransfer> listAll() { return transferRepository.findAllByOrderByCreatedAtDesc(); }
    public List<WarehouseTransfer> listByStatus(String s) { return transferRepository.findByStatusOrderByCreatedAtDesc(s); }

    @Transactional
    public synchronized WarehouseTransfer create(WarehouseTransfer draft, User user) {
        requirePermission(user, "TRANSFERENCIAS", "CREATE");
        if (draft.getWarehouse() == null || draft.getWarehouse().getId() == null)
            throw new IllegalArgumentException("Armazém de origem é obrigatório");
        if (draft.getBranch() == null || draft.getBranch().getId() == null)
            throw new IllegalArgumentException("Filial de destino é obrigatória");
        if (draft.getItems() == null || draft.getItems().isEmpty())
            throw new IllegalArgumentException("Adicione pelo menos um item");

        Warehouse wh = warehouseRepository.findById(draft.getWarehouse().getId())
                .orElseThrow(() -> new IllegalArgumentException("Armazém não encontrado"));
        Branch br = branchRepository.findById(draft.getBranch().getId())
                .orElseThrow(() -> new IllegalArgumentException("Filial não encontrada"));

        // Validar stock antes de criar
        for (WarehouseTransferItem item : draft.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantidade inválida em " + item.getProduct().getName());
            }
            StockWarehouse sw = stockWarehouseRepository
                    .findByWarehouseIdAndProductId(wh.getId(), item.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Sem stock no armazém para " + item.getProduct().getName()));
            java.math.BigDecimal available = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal required = java.math.BigDecimal.valueOf(item.getQuantity());
            if (available.compareTo(required) < 0) {
                throw new IllegalStateException(
                    "Stock insuficiente para " + item.getProduct().getName() +
                    " (tem " + available + ", precisa " + item.getQuantity() + ")");
            }
        }

        int year = LocalDateTime.now().getYear();
        Long nextNum = transferRepository.findMaxDocumentNumberBySeriesAndYear("TWA", year);
        if (nextNum == null) nextNum = 1L;

        draft.setDocumentNumber(nextNum);
        draft.setDocumentYear(year);
        draft.setSeries("TWA");
        draft.setStatus("PENDING");
        draft.setWarehouse(wh);
        draft.setBranch(br);
        draft.setRequestedBy(user);
        draft.setCreatedAt(LocalDateTime.now());

        // Associar cada item à transferência
        for (WarehouseTransferItem item : draft.getItems()) {
            item.setTransfer(draft);
            if (item.getProduct() != null && item.getProduct().getId() != null) {
                item.setProduct(productRepository.findById(item.getProduct().getId()).orElse(item.getProduct()));
            }
        }
        return transferRepository.save(draft);
    }

    /**
     * Aprova/envia a transferência. Não move stock ainda — só marca como IN_TRANSIT.
     */
    @Transactional
    public WarehouseTransfer approve(Long id, User user) {
        requirePermission(user, "TRANSFERENCIAS", "VIEW");
        WarehouseTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada"));
        if (!"PENDING".equals(t.getStatus())) {
            throw new IllegalStateException("Só transferências PENDING podem ser aprovadas (actual: " + t.getStatus() + ")");
        }
        t.setStatus("IN_TRANSIT");
        t.setProcessedBy(user);
        t.setProcessedAt(LocalDateTime.now());
        return transferRepository.save(t);
    }

    /**
     * Confirma a recepção na filial. Move o stock:
     *   - Decrementa StockWarehouse (saída do armazém)
     *   - Incrementa StockBranch   (entrada na loja)
     * Tudo em @Transactional — se falhar, nada fica gravado.
     */
    @Transactional
    public WarehouseTransfer complete(Long id, User user) {
        requirePermission(user, "TRANSFERENCIAS", "VIEW");
        WarehouseTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada"));
        if (!"PENDING".equals(t.getStatus()) && !"IN_TRANSIT".equals(t.getStatus())) {
            throw new IllegalStateException("Transferência já finalizada (status: " + t.getStatus() + ")");
        }

        // Re-validar stock disponível
        for (WarehouseTransferItem item : t.getItems()) {
            StockWarehouse sw = stockWarehouseRepository
                    .findByWarehouseIdAndProductId(t.getWarehouse().getId(), item.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException(
                        "Sem stock no armazém para " + item.getProduct().getName()));
            java.math.BigDecimal available = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal required = java.math.BigDecimal.valueOf(item.getQuantity());
            if (available.compareTo(required) < 0) {
                throw new IllegalStateException(
                    "Stock insuficiente no armazém para " + item.getProduct().getName());
            }
        }

        String reference = "TWA-" + t.getSeries() + "-" + t.getDocumentNumber();

        // 1. Decrementar stock do armazém (cada item)
        for (WarehouseTransferItem item : t.getItems()) {
            warehouseService.removeStock(
                t.getWarehouse().getId(),
                item.getProduct().getId(),
                item.getQuantity(),
                reference,
                user
            );

            // 2. Incrementar stock da filial
            stockBranchService.increaseStock(
                    t.getBranch(),
                    item.getProduct(),
                    java.math.BigDecimal.valueOf(item.getQuantity()),
                    reference,
                    "TRANSFERENCIA_ARMAZEM",
                    user
            );
        }

        t.setStatus("COMPLETED");
        t.setProcessedBy(user);
        t.setProcessedAt(LocalDateTime.now());
        return transferRepository.save(t);
    }

    @Transactional
    public WarehouseTransfer cancel(Long id, String reason, User user) {
        requirePermission(user, "TRANSFERENCIAS", "VIEW");
        WarehouseTransfer t = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transferência não encontrada"));
        if ("COMPLETED".equals(t.getStatus())) {
            throw new IllegalStateException("Não é possível cancelar uma transferência já recebida");
        }
        t.setStatus("CANCELLED");
        t.setCancelReason(reason);
        t.setProcessedBy(user);
        t.setProcessedAt(LocalDateTime.now());
        return transferRepository.save(t);
    }

    private void requirePermission(User user, String page, String action) {
        if (user == null || !user.hasPermission(page, action)) {
            throw new IllegalStateException("Não tem permissão para " + page + ":" + action + ".");
        }
    }
}
