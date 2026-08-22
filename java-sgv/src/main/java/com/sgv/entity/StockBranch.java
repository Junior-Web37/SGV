package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_branch", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "branch_id"})
})
public class StockBranch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private BigDecimal stockCurrent = BigDecimal.ZERO;
    private BigDecimal stockMin = BigDecimal.ZERO;
    private BigDecimal stockMax = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
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
        String prod = product != null ? product.getName() : "Produto";
        String br = branch != null ? branch.getName() : "Filial";
        return prod + " @ " + br;
    }
}
