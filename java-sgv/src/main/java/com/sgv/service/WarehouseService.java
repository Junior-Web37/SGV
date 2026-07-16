package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lógica de armazém:
 *  - Entrada de stock (compra a fornecedor)
 *  - Saída de stock (transferência para filial)
 *  - Listagens e ajustes
 */
@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockWarehouseRepository stockWarehouseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            StockWarehouseRepository stockWarehouseRepository,
                            StockMovementRepository stockMovementRepository,
                            ProductRepository productRepository) {
        this.warehouseRepository = warehouseRepository;
        this.stockWarehouseRepository = stockWarehouseRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
    }

    public List<Warehouse> listAll() { return warehouseRepository.findAllByOrderByNameAsc(); }
    public List<Warehouse> listActive() { return warehouseRepository.findByIsActiveTrueOrderByNameAsc(); }
    public Optional<Warehouse> findById(Long id) { return warehouseRepository.findById(id); }
    public Warehouse save(Warehouse w) { return warehouseRepository.save(w); }

    public List<StockWarehouse> stockByWarehouse(Long warehouseId) {
        return stockWarehouseRepository.findByWarehouseId(warehouseId);
    }

    public Optional<StockWarehouse> getStock(Long warehouseId, Long productId) {
        return stockWarehouseRepository.findByWarehouseIdAndProductId(warehouseId, productId);
    }

    /**
     * Adiciona stock ao armazém (entrada — compra de fornecedor).
     * Cria o StockWarehouse se não existir; regista StockMovement tipo "ENTRADA_ARMAZEM".
     */
    @Transactional
    public StockWarehouse addStock(Long warehouseId, Long productId, double qty, String reference, User user) {
        if (qty <= 0) throw new IllegalArgumentException("Quantidade deve ser > 0");
        Warehouse wh = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Armazém não encontrado"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));

        StockWarehouse sw = stockWarehouseRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> {
                    StockWarehouse n = new StockWarehouse();
                    n.setWarehouse(wh);
                    n.setProduct(product);
                    n.setStockCurrentAmount(java.math.BigDecimal.ZERO);
                    return n;
                });
        java.math.BigDecimal quantity = java.math.BigDecimal.valueOf(qty);
        java.math.BigDecimal before = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
        sw.setStockCurrentAmount(before.add(quantity));
        sw = stockWarehouseRepository.save(sw);

        // Regista movimento
        StockMovement mov = new StockMovement();
        mov.setType("ENTRADA_ARMAZEM");
        mov.setSubtype("COMPRA");
        mov.setQtyAmount(quantity);
        mov.setStockBeforeAmount(before);
        mov.setStockAfterAmount(sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO);
        mov.setProduct(product);
        mov.setUnitCostPrice(product.getPriceCost());
        mov.setUnitSalePrice(product.getPriceSale());
        mov.setNotes("Entrada de stock");
        // O campo "branch" no StockMovement fica null porque não é movimento de filial
        mov.setReference(reference);
        mov.setUser(user);
        stockMovementRepository.save(mov);

        return sw;
    }

    /**
     * Remove stock do armazém (saída para transferência).
     * Lança excepção se stock insuficiente.
     */
    @Transactional
    public StockWarehouse removeStock(Long warehouseId, Long productId, double qty, String reference, User user) {
        if (qty <= 0) throw new IllegalArgumentException("Quantidade deve ser > 0");
        StockWarehouse sw = stockWarehouseRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new IllegalStateException("Produto não tem stock no armazém"));
        java.math.BigDecimal quantity = java.math.BigDecimal.valueOf(qty);
        java.math.BigDecimal before = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
        if (before.compareTo(quantity) < 0) {
            throw new IllegalStateException(
                "Stock insuficiente no armazém: tem " + before + ", precisa " + qty);
        }
        sw.setStockCurrentAmount(before.subtract(quantity));
        sw = stockWarehouseRepository.save(sw);

        Product product = sw.getProduct();
        product.setLastMovementAt(java.time.LocalDateTime.now());
        productRepository.save(product);

        StockMovement mov = new StockMovement();
        mov.setType("SAIDA_ARMAZEM");
        mov.setSubtype("TRANSFERENCIA");
        mov.setQtyAmount(quantity.negate());
        mov.setStockBeforeAmount(before);
        mov.setStockAfterAmount(sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO);
        mov.setProduct(product);
        mov.setUnitCostPrice(product.getPriceCost());
        mov.setUnitSalePrice(product.getPriceSale());
        mov.setNotes("Saída de stock");
        mov.setReference(reference);
        mov.setUser(user);
        stockMovementRepository.save(mov);

        return sw;
    }
}
