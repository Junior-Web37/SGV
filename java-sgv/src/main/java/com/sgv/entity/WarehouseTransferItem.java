package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "warehouse_transfer_items")
public class WarehouseTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "warehouse_transfer_id")
    private WarehouseTransfer transfer;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal quantityReceived = BigDecimal.ZERO;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WarehouseTransfer getTransfer() { return transfer; }
    public void setTransfer(WarehouseTransfer transfer) { this.transfer = transfer; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Double getQuantity() { return quantity != null ? quantity.doubleValue() : null; }
    public void setQuantity(Double quantity) { this.quantity = quantity != null ? BigDecimal.valueOf(quantity) : null; }
    public BigDecimal getQuantityAmount() { return quantity; }
    public void setQuantityAmount(BigDecimal quantity) { this.quantity = quantity; }
    public Double getQuantityReceived() { return quantityReceived != null ? quantityReceived.doubleValue() : null; }
    public void setQuantityReceived(Double quantityReceived) { this.quantityReceived = quantityReceived != null ? BigDecimal.valueOf(quantityReceived) : null; }
    public BigDecimal getQuantityReceivedAmount() { return quantityReceived; }
    public void setQuantityReceivedAmount(BigDecimal quantityReceived) { this.quantityReceived = quantityReceived; }
}
