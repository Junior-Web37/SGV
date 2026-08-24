package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final TransferItemRepository transferItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockBranchService stockBranchService;
    private final UserRepository userRepository;

    public TransferService(TransferRepository transferRepository,
                           TransferItemRepository transferItemRepository,
                           StockMovementRepository stockMovementRepository,
                           StockBranchService stockBranchService,
                           UserRepository userRepository) {
        this.transferRepository = transferRepository;
        this.transferItemRepository = transferItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockBranchService = stockBranchService;
        this.userRepository = userRepository;
    }

    public List<Transfer> findAll() {
        return transferRepository.findAll();
    }

    public Optional<Transfer> findById(Long id) {
        return transferRepository.findById(id);
    }

    public List<Transfer> findByDateRangeAndStatus(LocalDateTime start, LocalDateTime end, String status) {
        return transferRepository.findByDateRangeAndStatus(start, end, status);
    }

    public List<Transfer> findByStatusAndBranch(String status, Long branchId) {
        return transferRepository.findByStatusAndBranch(status, branchId);
    }

    @Transactional
    public Transfer create(Transfer transfer, List<TransferItem> items) {
        if (transfer.getSeries() == null) transfer.setSeries("TR");
        if (transfer.getDocumentYear() == null) transfer.setDocumentYear(LocalDateTime.now().getYear());
        if (transfer.getDocumentNumber() == null && transfer.getSourceBranch() != null) {
            Long maxNum = transferRepository.findMaxDocumentNumberBySeriesAndYearAndBranch(
                    transfer.getSeries(), transfer.getDocumentYear(), transfer.getSourceBranch().getId());
            transfer.setDocumentNumber(maxNum + 1);
        }
        transfer.setStatus("PENDING");
        Transfer saved = transferRepository.save(transfer);
        for (TransferItem item : items) {
            item.setTransfer(saved);
            transferItemRepository.save(item);
        }
        return saved;
    }

    @Transactional
    public Transfer approve(Long transferId, User processedBy) {
        Transfer t = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
        if (!"PENDING".equals(t.getStatus())) {
            throw new IllegalStateException("Only PENDING transfers can be approved.");
        }
        t.setStatus("APPROVED");
        t.setProcessedBy(processedBy);
        t.setProcessedAt(LocalDateTime.now());
        return transferRepository.save(t);
    }

    @Transactional
    public Transfer reject(Long transferId, User processedBy, String reason) {
        Transfer t = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
        if (!"PENDING".equals(t.getStatus())) {
            throw new IllegalStateException("Only PENDING transfers can be rejected.");
        }
        t.setStatus("REJECTED");
        t.setProcessedBy(processedBy);
        t.setProcessedAt(LocalDateTime.now());
        t.setCancelReason(reason);
        return transferRepository.save(t);
    }

    @Transactional
    public Transfer complete(Long transferId, User processedBy) {
        Transfer t = transferRepository.findByIdWithItems(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
        if (!"APPROVED".equals(t.getStatus())) {
            throw new IllegalStateException("Only APPROVED transfers can be completed.");
        }
        // Record stock movements: deduct from source, add to destination
        for (TransferItem item : t.getItems()) {
            Product product = item.getProduct();
            Branch source = t.getSourceBranch();
            Branch dest = t.getDestinationBranch();

            stockBranchService.decreaseStock(
                    source,
                    product,
                    java.math.BigDecimal.valueOf(item.getQuantity()),
                    "TR-" + t.getDocumentNumber() + "/" + t.getSeries(),
                    "TRANSFER_OUT",
                    processedBy
            );
            stockBranchService.increaseStock(
                    dest,
                    product,
                    java.math.BigDecimal.valueOf(item.getQuantity()),
                    "TR-" + t.getDocumentNumber() + "/" + t.getSeries(),
                    "TRANSFER_IN",
                    processedBy
            );
        }
        t.setStatus("COMPLETED");
        t.setProcessedBy(processedBy);
        t.setProcessedAt(LocalDateTime.now());
        return transferRepository.save(t);
    }

    @Transactional
    public Transfer cancel(Long transferId, User requestedBy, String reason) {
        Transfer t = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transferId));
        if ("COMPLETED".equals(t.getStatus())) {
            throw new IllegalStateException("Completed transfers cannot be cancelled.");
        }
        if ("CANCELLED".equals(t.getStatus())) {
            throw new IllegalStateException("Transfer is already cancelled.");
        }
        t.setStatus("CANCELLED");
        t.setCancelReason(reason);
        if (t.getRequestedBy() == null) t.setRequestedBy(requestedBy);
        return transferRepository.save(t);
    }
}
