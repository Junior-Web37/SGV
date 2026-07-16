package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false)
    private String type;
    private String subtype;
    private BigDecimal qty = BigDecimal.ZERO;
    private BigDecimal stockBefore;
    private BigDecimal stockAfter;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String reference;
    private Double unitCostPrice = 0.0;
    private Double unitSalePrice = 0.0;
    private String notes;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubtype() { return subtype; }
    public void setSubtype(String subtype) { this.subtype = subtype; }
    public Double getQty() { return qty != null ? qty.doubleValue() : null; }
    public void setQty(Double qty) { this.qty = qty != null ? BigDecimal.valueOf(qty) : null; }
    public BigDecimal getQtyAmount() { return qty; }
    public void setQtyAmount(BigDecimal qty) { this.qty = qty; }
    public Double getStockBefore() { return stockBefore != null ? stockBefore.doubleValue() : null; }
    public void setStockBefore(Double stockBefore) { this.stockBefore = stockBefore != null ? BigDecimal.valueOf(stockBefore) : null; }
    public BigDecimal getStockBeforeAmount() { return stockBefore; }
    public void setStockBeforeAmount(BigDecimal stockBefore) { this.stockBefore = stockBefore; }
    public Double getStockAfter() { return stockAfter != null ? stockAfter.doubleValue() : null; }
    public void setStockAfter(Double stockAfter) { this.stockAfter = stockAfter != null ? BigDecimal.valueOf(stockAfter) : null; }
    public BigDecimal getStockAfterAmount() { return stockAfter; }
    public void setStockAfterAmount(BigDecimal stockAfter) { this.stockAfter = stockAfter; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Double getUnitCostPrice() { return unitCostPrice; }
    public void setUnitCostPrice(Double unitCostPrice) { this.unitCostPrice = unitCostPrice; }
    public Double getUnitSalePrice() { return unitSalePrice; }
    public void setUnitSalePrice(Double unitSalePrice) { this.unitSalePrice = unitSalePrice; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
