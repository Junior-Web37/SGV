package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_items")
public class PurchaseItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private BigDecimal quantity = BigDecimal.ONE;
    private BigDecimal costPrice = BigDecimal.ZERO;
    private BigDecimal subtotal = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Purchase getPurchase() { return purchase; }
    public void setPurchase(Purchase purchase) { this.purchase = purchase; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Double getQuantity() { return quantity != null ? quantity.doubleValue() : null; }
    public void setQuantity(Double quantity) { this.quantity = quantity != null ? BigDecimal.valueOf(quantity) : null; }
    public BigDecimal getQuantityAmount() { return quantity; }
    public void setQuantityAmount(BigDecimal quantity) { this.quantity = quantity; }
    public Double getCostPrice() { return costPrice != null ? costPrice.doubleValue() : null; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice != null ? BigDecimal.valueOf(costPrice) : null; }
    public BigDecimal getCostPriceAmount() { return costPrice; }
    public void setCostPriceAmount(BigDecimal costPrice) { this.costPrice = costPrice; }
    public Double getSubtotal() { return subtotal != null ? subtotal.doubleValue() : null; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal != null ? BigDecimal.valueOf(subtotal) : null; }
    public BigDecimal getSubtotalAmount() { return subtotal; }
    public void setSubtotalAmount(BigDecimal subtotal) { this.subtotal = subtotal; }
}
