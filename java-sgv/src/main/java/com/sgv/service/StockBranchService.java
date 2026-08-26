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
     * Lista as matérias-primas insuficientes para produzir {@code producedQty}
     * unidades de {@code finishedProduct} na {@code branch} (vazio = há
     * stock suficiente para tudo).
     */
    public List<String> listIngredientShortages(Branch branch, Product finishedProduct, BigDecimal producedQty) {
        List<String> shortages = new java.util.ArrayList<>();
        if (branch == null || finishedProduct == null || producedQty == null || producedQty.compareTo(BigDecimal.ZERO) <= 0) {
            return shortages;
        }
        List<ProductRecipe> recipes = productRecipeRepository.findByParentProductId(finishedProduct.getId());
        if (recipes == null || recipes.isEmpty()) return shortages;

        for (ProductRecipe recipe : recipes) {
            Product ingredient = recipe.getIngredientProduct();
            if (ingredient == null) continue;
            BigDecimal requiredPerUnit = recipe.getQuantityRequired() != null ? recipe.getQuantityRequired() : BigDecimal.ONE;
            BigDecimal totalConsumption = requiredPerUnit.multiply(producedQty);
            BigDecimal available = stockBranchRepository.findByProductAndBranch(ingredient, branch)
                    .map(sb -> sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO)
                    .orElse(BigDecimal.ZERO);
            if (available.compareTo(totalConsumption) < 0) {
                shortages.add(ingredient.getName() + " (necessário " + totalConsumption.stripTrailingZeros().toPlainString()
                        + ", disponível " + available.stripTrailingZeros().toPlainString() + ")");
            }
        }
        return shortages;
    }

    /**
     * Abate de matérias-primas e ingredientes com base na Ficha Técnica (BOM / Receitas).
     *
     * <p>Correção do BUG-002: antes, este método engolia TODAS as excepções
     * (catch vazio) e permitia stock negativo → produção gravada sem consumo,
     * ou com MP negativa, em silêncio. Agora:
     * <ol>
     *   <li>valida a disponibilidade de TODAS as MP antes de qualquer escrita;</li>
     *   <li>se faltar alguma, lança {@link IllegalStateException} com a lista
     *       de faltas — a transacção completa é revertida;</li>
     *   <li>o consumo nunca deixa {@code stock_current < 0} (verificação
     *       defensiva na escrita).</li>
     * </ol>
     */
    @Transactional
    public void consumeIngredientsForProduction(Branch branch, Product finishedProduct, BigDecimal producedQty, String reference, User user) {
        if (branch == null || finishedProduct == null || producedQty == null || producedQty.compareTo(BigDecimal.ZERO) <= 0) return;
        List<ProductRecipe> recipes = productRecipeRepository.findByParentProductId(finishedProduct.getId());
        if (recipes == null || recipes.isEmpty()) return;

        // 1) Validação prévia — nenhuma escrita acontece se faltar MP
        List<String> shortages = listIngredientShortages(branch, finishedProduct, producedQty);
        if (!shortages.isEmpty()) {
            throw new IllegalStateException("Matérias-primas insuficientes para produzir "
                    + producedQty.stripTrailingZeros().toPlainString() + " de " + finishedProduct.getName()
                    + " na filial " + branch.getName() + ": " + String.join("; ", shortages)
                    + ". A produção não foi gravada.");
        }

        // 2) Consumo — sem catch: qualquer falha (lock, constraint) propaga e
        // reverte a transacção inteira (nunca mais "log and continue").
        for (ProductRecipe recipe : recipes) {
            Product ingredient = recipe.getIngredientProduct();
            if (ingredient == null) continue;
            BigDecimal requiredPerUnit = recipe.getQuantityRequired() != null ? recipe.getQuantityRequired() : BigDecimal.ONE;
            BigDecimal totalConsumption = requiredPerUnit.multiply(producedQty);

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
            if (after.compareTo(BigDecimal.ZERO) < 0) {
                // Defesa em profundidade (ex.: consumo entre a validação e a
                // escrita por outra transacção) — nunca gravar negativo.
                throw new IllegalStateException("Stock de " + ingredient.getName() + " tornou-se insuficiente "
                        + "(" + before + " − " + totalConsumption + " < 0). Produção bloqueada; nada foi gravado.");
            }
            sb.setStockCurrentAmount(after);
            StockBranch saved = stockBranchRepository.save(sb);
            saveMovement(saved, totalConsumption.negate(), before, after, "SAIDA", "PROD_CONSUMO", reference, user,
                    "Consumo de matéria-prima para produção de " + finishedProduct.getName());
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
