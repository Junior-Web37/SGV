package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "target_warehouse_id")
    private Warehouse targetWarehouse;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    private LocalDateTime purchaseDate = LocalDateTime.now();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal totalTax = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String state = "RECEIVED"; // RECEIVED, PENDING, CANCELLED
    private String notes;
    @Column(precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public Warehouse getTargetWarehouse() { return targetWarehouse; }
    public void setTargetWarehouse(Warehouse targetWarehouse) { this.targetWarehouse = targetWarehouse; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }
    public Double getSubtotal() { return subtotal != null ? subtotal.doubleValue() : null; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal != null ? BigDecimal.valueOf(subtotal) : null; }
    public BigDecimal getSubtotalAmount() { return subtotal; }
    public void setSubtotalAmount(BigDecimal subtotal) { this.subtotal = subtotal; }
    public Double getTotalTax() { return totalTax != null ? totalTax.doubleValue() : null; }
    public void setTotalTax(Double totalTax) { this.totalTax = totalTax != null ? BigDecimal.valueOf(totalTax) : null; }
    public BigDecimal getTotalTaxAmount() { return totalTax; }
    public void setTotalTaxAmount(BigDecimal totalTax) { this.totalTax = totalTax; }
    public Double getTotal() { return total != null ? total.doubleValue() : null; }
    public void setTotal(Double total) { this.total = total != null ? BigDecimal.valueOf(total) : null; }
    public BigDecimal getTotalAmount() { return total; }
    public void setTotalAmount(BigDecimal total) { this.total = total; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Double getPaidAmount() { return paidAmount != null ? paidAmount.doubleValue() : null; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount != null ? BigDecimal.valueOf(paidAmount) : null; }
    public BigDecimal getPaidAmountValue() { return paidAmount; }
    public void setPaidAmountValue(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) { this.items = items; }

    @Override
    public String toString() {
        return invoiceNumber != null ? "Compra #" + invoiceNumber : "Compra #" + id;
    }
}
