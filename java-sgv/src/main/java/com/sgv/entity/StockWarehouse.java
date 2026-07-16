package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Stock por armazém. Paralelo a StockBranch, mas para o armazém central.
 * Compras a fornecedores incrementam este stock. Transferências decrementam.
 */
@Entity
@Table(name = "stock_warehouse", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"warehouse_id", "product_id"})
})
public class StockWarehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private BigDecimal stockCurrent = BigDecimal.ZERO;
    private BigDecimal stockMin = BigDecimal.ZERO;
    private BigDecimal stockMax = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Double getStockCurrent() { return stockCurrent != null ? stockCurrent.doubleValue() : null; }
    public void setStockCurrent(Double stockCurrent) { this.stockCurrent = stockCurrent != null ? BigDecimal.valueOf(stockCurrent) : null; }
    public BigDecimal getStockCurrentAmount() { return stockCurrent; }
    public void setStockCurrentAmount(BigDecimal stockCurrent) { this.stockCurrent = stockCurrent; }
    public Double getStockMin() { return stockMin != null ? stockMin.doubleValue() : null; }
    public void setStockMin(Double stockMin) { this.stockMin = stockMin != null ? BigDecimal.valueOf(stockMin) : null; }
    public BigDecimal getStockMinAmount() { return stockMin; }
    public void setStockMinAmount(BigDecimal stockMin) { this.stockMin = stockMin; }
    public Double getStockMax() { return stockMax != null ? stockMax.doubleValue() : null; }
    public void setStockMax(Double stockMax) { this.stockMax = stockMax != null ? BigDecimal.valueOf(stockMax) : null; }
    public BigDecimal getStockMaxAmount() { return stockMax; }
    public void setStockMaxAmount(BigDecimal stockMax) { this.stockMax = stockMax; }

    @Override
    public String toString() {
        String p = product != null ? product.getName() : "Produto";
        String w = warehouse != null ? warehouse.getName() : "Armazém";
        return p + " @ " + w;
    }
}
