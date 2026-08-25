package com.sgv.service;

import com.sgv.entity.Branch;
import com.sgv.entity.Product;
import com.sgv.entity.ProductRecipe;
import com.sgv.entity.StockBranch;
import com.sgv.entity.StockMovement;
import com.sgv.entity.User;
import com.sgv.repository.ProductRecipeRepository;
import com.sgv.repository.StockBranchRepository;
import com.sgv.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StockBranchService {

    private final StockBranchRepository stockBranchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRecipeRepository productRecipeRepository;

    public StockBranchService(StockBranchRepository stockBranchRepository,
                              StockMovementRepository stockMovementRepository,
                              ProductRecipeRepository productRecipeRepository) {
        this.stockBranchRepository = stockBranchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productRecipeRepository = productRecipeRepository;
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
                                   BigDecimal maxStock,
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
        if (maxStock != null) {
            stockBranch.setStockMaxAmount(maxStock);
        }

        StockBranch saved = stockBranchRepository.save(stockBranch);

        if (after.compareTo(before) != 0) {
            saveMovement(saved, after.subtract(before), before, after, "AJUSTE_MANUAL", "PERDA_DANO", reference, user,
                    "Perda/Dano registado na filial.");
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

    /**
     * Abate automático de matérias-primas e ingredientes com base na Ficha Técnica (BOM / Receitas).
     */
    @Transactional
    public void consumeIngredientsForProduction(Branch branch, Product finishedProduct, BigDecimal producedQty, String reference, User user) {
        if (branch == null || finishedProduct == null || producedQty == null || producedQty.compareTo(BigDecimal.ZERO) <= 0) return;
        List<ProductRecipe> recipes = productRecipeRepository.findByParentProductId(finishedProduct.getId());
        if (recipes == null || recipes.isEmpty()) return;

        for (ProductRecipe recipe : recipes) {
            Product ingredient = recipe.getIngredientProduct();
            if (ingredient == null) continue;
            BigDecimal requiredPerUnit = recipe.getQuantityRequired() != null ? recipe.getQuantityRequired() : BigDecimal.ONE;
            BigDecimal totalConsumption = requiredPerUnit.multiply(producedQty);

            try {
                StockBranch sb = stockBranchRepository.findByProductAndBranch(ingredient, branch)
                        .orElseGet(() -> {
                            StockBranch newSb = new StockBranch();
                            newSb.setProduct(ingredient);
                            newSb.setBranch(branch);
                            newSb.setStockCurrentAmount(BigDecimal.ZERO);
                            newSb.setStockMinAmount(BigDecimal.ZERO);
                            return newSb;
                        });
                BigDecimal before = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO;
                BigDecimal after = before.subtract(totalConsumption);
                sb.setStockCurrentAmount(after);
                StockBranch saved = stockBranchRepository.save(sb);
                saveMovement(saved, totalConsumption.negate(), before, after, "SAIDA", "PROD_CONSUMO", reference, user,
                        "Consumo de matéria-prima para produção de " + finishedProduct.getName());
            } catch (Exception e) {
                // Log and continue
            }
        }
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

    /**
     * Batch-load StockBranch records for a branch and list of product IDs (single query).
     */
    public java.util.List<StockBranch> loadStockForBranchAndProducts(Long branchId, java.util.List<Long> productIds) {
        if (branchId == null || productIds == null || productIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return stockBranchRepository.findByBranchIdAndProductIdIn(branchId, productIds);
    }

    /**
     * Direct save of StockBranch — used by SaleService for batch stock decrement.
     */
    @org.springframework.transaction.annotation.Transactional
    public StockBranch saveStockBranch(StockBranch sb) {
        return stockBranchRepository.save(sb);
    }

    /**
     * Record a stock movement — used by SaleService for batch stock decrement.
     */
    @org.springframework.transaction.annotation.Transactional
    public void saveStockMovement(StockBranch stockBranch,
                                  BigDecimal qty,
                                  BigDecimal before,
                                  BigDecimal after,
                                  String type,
                                  String subtype,
                                  String reference,
                                  User user) {
        saveMovement(stockBranch, qty, before, after, type, subtype, reference, user,
                type.equals("SAIDA") ? "Saída de stock da filial." : "Entrada de stock na filial.");
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

    /**
     * Alertas de reposição: artigos físicos esgotados, abaixo do mínimo,
     * ou sem ficha de stock. Serviços nunca entram.
     */
    public java.util.List<StockAlert> listStockAlerts(Long branchId) {
        java.util.List<StockAlert> alerts = new java.util.ArrayList<>();
        for (StockBranch sb : stockBranchRepository.findStockAlerts(branchId)) {
            if (sb.getProduct() == null || Boolean.TRUE.equals(sb.getProduct().getService())) continue;
            BigDecimal cur = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO;
            BigDecimal min = sb.getStockMinAmount() != null ? sb.getStockMinAmount() : BigDecimal.ZERO;
            String kind = cur.compareTo(BigDecimal.ZERO) <= 0 ? "ESGOTADO" : "ABAIXO_MINIMO";
            alerts.add(new StockAlert(sb.getProduct(), sb.getBranch(), cur, min, kind));
        }
        for (Product p : stockBranchRepository.findPhysicalProductsWithoutStockRow(branchId)) {
            if (p == null || Boolean.TRUE.equals(p.getService())) continue;
            alerts.add(new StockAlert(p, null, BigDecimal.ZERO, BigDecimal.ZERO, "SEM_FICHA"));
        }
        return alerts;
    }

    public record StockAlert(Product product, Branch branch, BigDecimal current, BigDecimal min, String kind) {
        public String label() {
            String name = product != null
                    ? ((product.getCode() != null ? product.getCode() + " — " : "") + (product.getName() != null ? product.getName() : "Artigo"))
                    : "Artigo";
            return switch (kind) {
                case "ESGOTADO" -> name + " — Esgotado (0)";
                case "SEM_FICHA" -> name + " — Sem stock (sem ficha)";
                default -> name + " — Abaixo do mínimo (" + current + " / mín " + min + ")";
            };
        }
    }

    @Transactional
    public StockBranch initializeStock(Branch branch,
                                       Product product,
                                       BigDecimal currentStock,
                                       BigDecimal minStock,
                                       BigDecimal maxStock,
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
                    sb.setStockMaxAmount(BigDecimal.ZERO);
                    return sb;
                });

        BigDecimal before = stockBranch.getStockCurrentAmount() != null ? stockBranch.getStockCurrentAmount() : BigDecimal.ZERO;
        BigDecimal after = currentStock != null ? currentStock : before;
        stockBranch.setStockCurrentAmount(after);
        if (minStock != null) {
            stockBranch.setStockMinAmount(minStock);
        }
        if (maxStock != null) {
            stockBranch.setStockMaxAmount(maxStock);
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
