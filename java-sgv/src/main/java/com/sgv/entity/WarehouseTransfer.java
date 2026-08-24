package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Transferência de stock: armazém central → filial.
 * Status: PENDING (criada), IN_TRANSIT (aprovada/enviada), COMPLETED (recebida), CANCELLED.
 */
@Entity
@Table(name = "warehouse_transfers")
public class WarehouseTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentNumber;
    private Integer documentYear;
    private String series = "TWA";

    @Column(nullable = false)
    private String status = "PENDING";

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy;

    private String notes;
    private String cancelReason;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<WarehouseTransferItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(Long documentNumber) { this.documentNumber = documentNumber; }
    public Integer getDocumentYear() { return documentYear; }
    public void setDocumentYear(Integer documentYear) { this.documentYear = documentYear; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public List<WarehouseTransferItem> getItems() { return items; }
    public void setItems(List<WarehouseTransferItem> items) { this.items = items; }
}
