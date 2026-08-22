package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 100)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "target_warehouse_id")
    private Warehouse targetWarehouse;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(nullable = false, length = 30)
    private String status = "PENDING"; // PENDING, APPROVED, RECEIVED, CANCELLED

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_tax", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 1024)
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public Warehouse getTargetWarehouse() { return targetWarehouse; }
    public void setTargetWarehouse(Warehouse targetWarehouse) { this.targetWarehouse = targetWarehouse; }

    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSubtotal() { return subtotal != null ? subtotal : BigDecimal.ZERO; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTotalTax() { return totalTax != null ? totalTax : BigDecimal.ZERO; }
    public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }

    public BigDecimal getTotal() { return total != null ? total : BigDecimal.ZERO; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items != null ? items : new ArrayList<>(); }
}
