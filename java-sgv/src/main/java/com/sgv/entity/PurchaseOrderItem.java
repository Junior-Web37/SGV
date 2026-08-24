package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_cost", precision = 19, scale = 4, nullable = false)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseOrder getOrder() { return order; }
    public void setOrder(PurchaseOrder order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public BigDecimal getQuantity() { return quantity != null ? quantity : BigDecimal.ZERO; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost != null ? unitCost : BigDecimal.ZERO; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getSubtotal() { return subtotal != null ? subtotal : BigDecimal.ZERO; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
