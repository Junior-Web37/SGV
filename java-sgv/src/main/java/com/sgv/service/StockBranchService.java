package com.sgv.service;

import com.sgv.entity.Branch;
import com.sgv.entity.Product;
import com.sgv.entity.StockBranch;
import com.sgv.entity.StockMovement;
import com.sgv.entity.User;
import com.sgv.repository.StockBranchRepository;
import com.sgv.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class StockBranchService {

    private final StockBranchRepository stockBranchRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockBranchService(StockBranchRepository stockBranchRepository,
                              StockMovementRepository stockMovementRepository) {
        this.stockBranchRepository = stockBranchRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public boolean isAdmin(User user) {
        if (user == null || user.getRoles() == null) return false;
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() != null && role.getName().toUpperCase().contains("ADMIN"));
    }

    @Transactional
    public StockBranch adjustStock(StockBranch stockBranch,
                                   BigDecimal currentStock,
                                   BigDecimal minStock,
                                   String reference,
                                   User user) {
        requirePermission(user, "STOCK", "CREATE");
        if (!isAdmin(user)) {
            throw new IllegalStateException("Apenas administradores podem corrigir stock da loja.");
        }
        if (stockBranch == null) {
            throw new IllegalArgumentException("Stock branch is required.");
        }
        if (stockBranch.getProduct() == null || stockBranch.getBranch() == null) {
            throw new IllegalArgumentException("Stock branch must have a product and branch.");
        }
        BigDecimal before = stockBranch.getStockCurrentAmount() != null ? stockBranch.getStockCurrentAmount() : BigDecimal.ZERO;
        BigDecimal after = currentStock != null ? currentStock : before;

        stockBranch.setStockCurrentAmount(after);
        if (minStock != null) {
            stockBranch.setStockMinAmount(minStock);
        }

        StockBranch saved = stockBranchRepository.save(stockBranch);

        if (after.compareTo(before) != 0) {
            saveMovement(saved, after.subtract(before), before, after, "AJUSTE_MANUAL", "CORRECAO_LOJA", reference, user,
                    "Correção manual de stock da filial.");
        }

        return saved;
    }

    @Transactional
    public StockBranch increaseStock(Branch branch,
                                     Product product,
                                     BigDecimal quantity,
                                     String reference,
                                     String subtype,
                                     User user) {
        if (branch == null || branch.getId() == null) {
            throw new IllegalArgumentException("Branch is required.");
        }
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        StockBranch stockBranch = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseGet(() -> {
                    StockBranch sb = new StockBranch();
                    sb.setProduct(product);
                    sb.setBranch(branch);
                    sb.setStockCurrentAmount(BigDecimal.ZERO);
                    sb.setStockMinAmount(BigDecimal.ZERO);
                    return sb;
                });

        BigDecimal before = stockBranch.getStockCurrentAmount() != null ? stockBranch.getStockCurrentAmount() : BigDecimal.ZERO;
        BigDecimal after = before.add(quantity);
        stockBranch.setStockCurrentAmount(after);
        StockBranch saved = stockBranchRepository.save(stockBranch);
        saveMovement(saved, quantity, before, after, "ENTRADA", subtype, reference, user,
                "Entrada de stock na filial.");
        return saved;
    }

    @Transactional
    public StockBranch decreaseStock(Branch branch,
                                     Product product,
                                     BigDecimal quantity,
                                     String reference,
                                     String subtype,
                                     User user) {
        if (branch == null || branch.getId() == null) {
            throw new IllegalArgumentException("Branch is required.");
        }
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }

        StockBranch stockBranch = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseThrow(() -> new IllegalStateException("Stock da filial não encontrado para o produto: " + product.getCode()));

        BigDecimal before = stockBranch.getStockCurrentAmount() != null ? stockBranch.getStockCurrentAmount() : BigDecimal.ZERO;
        BigDecimal after = before.subtract(quantity);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Stock insuficiente na filial para o produto: " + product.getCode());
        }

        stockBranch.setStockCurrentAmount(after);
        StockBranch saved = stockBranchRepository.save(stockBranch);
        saveMovement(saved, quantity.negate(), before, after, "SAIDA", subtype, reference, user,
                "Saída de stock da filial.");
        return saved;
    }

    private void saveMovement(StockBranch stockBranch,
                              BigDecimal qty,
                              BigDecimal before,
                              BigDecimal after,
                              String type,
                              String subtype,
                              String reference,
                              User user,
                              String notes) {
        StockMovement mov = new StockMovement();
        mov.setProduct(stockBranch.getProduct());
        mov.setBranch(stockBranch.getBranch());
        mov.setType(type);
        mov.setSubtype(subtype);
        mov.setQtyAmount(qty);
        mov.setStockBeforeAmount(before);
        mov.setStockAfterAmount(after);
        mov.setReference(reference != null ? reference : type);
        mov.setUser(user);
        mov.setNotes(notes);
        stockMovementRepository.save(mov);
    }

    public java.math.BigDecimal getCurrentStock(Branch branch, Product product) {
        if (branch == null || branch.getId() == null) {
            throw new IllegalArgumentException("Branch is required.");
        }
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        return stockBranchRepository.findByProductAndBranch(product, branch)
                .map(sb -> sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);
    }

    private void requirePermission(User user, String page, String action) {
        if (user == null || !user.hasPermission(page, action)) {
            throw new IllegalStateException("Não tem permissão para " + page + ":" + action + ".");
        }
    }

    public java.util.Optional<StockBranch> findByProductIdAndBranchId(Long productId, Long branchId) {
        return stockBranchRepository.findByProductIdAndBranchId(productId, branchId);
    }

    public java.util.List<StockBranch> findAll() {
        return stockBranchRepository.findAll();
    }

    @Transactional
    public StockBranch initializeStock(Branch branch,
                                       Product product,
                                       BigDecimal currentStock,
                                       BigDecimal minStock,
                                       String reference,
                                       User user) {
        if (branch == null || branch.getId() == null) {
            throw new IllegalArgumentException("Branch is required.");
        }
        if (product == null || product.getId() == null) {
            throw new IllegalArgumentException("Product is required.");
        }
        if (currentStock != null && currentStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current stock cannot be negative.");
        }

        StockBranch stockBranch = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseGet(() -> {
                    StockBranch sb = new StockBranch();
                    sb.setProduct(product);
                    sb.setBranch(branch);
                    sb.setStockCurrentAmount(BigDecimal.ZERO);
                    sb.setStockMinAmount(BigDecimal.ZERO);
                    return sb;
                });

        BigDecimal before = stockBranch.getStockCurrentAmount() != null ? stockBranch.getStockCurrentAmount() : BigDecimal.ZERO;
        BigDecimal after = currentStock != null ? currentStock : before;
        stockBranch.setStockCurrentAmount(after);
        if (minStock != null) {
            stockBranch.setStockMinAmount(minStock);
        }

        StockBranch saved = stockBranchRepository.save(stockBranch);
        if (after.compareTo(before) != 0) {
            saveMovement(saved, after.subtract(before), before, after, "AJUSTE_MANUAL", "INITIALIZE", reference, user,
                    "Inicialização de stock da filial.");
        }
        return saved;
    }

    @Transactional
    public void deleteStock(StockBranch stockBranch, User user) {
        requirePermission(user, "STOCK", "DELETE");
        if (!isAdmin(user)) {
            throw new IllegalStateException("Apenas administradores podem eliminar stock da loja.");
        }
        if (stockBranch == null || stockBranch.getId() == null) {
            throw new IllegalArgumentException("StockBranch is required.");
        }
        StockBranch existing = stockBranchRepository.findById(stockBranch.getId())
                .orElseThrow(() -> new IllegalStateException("StockBranch não encontrado."));
        BigDecimal before = existing.getStockCurrentAmount() != null ? existing.getStockCurrentAmount() : BigDecimal.ZERO;
        stockBranchRepository.delete(existing);
        if (before.compareTo(BigDecimal.ZERO) != 0) {
            saveMovement(existing, before.negate(), before, BigDecimal.ZERO, "AJUSTE_MANUAL", "REMOVER_STOCK", "REMOVE_STOCK", user,
                    "Remoção manual de stock da filial.");
        }
    }
}
