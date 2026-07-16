package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentNumber;
    private Integer documentYear;
    private String series;
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED

    @ManyToOne
    @JoinColumn(name = "source_branch_id")
    private Branch sourceBranch;

    @ManyToOne
    @JoinColumn(name = "destination_branch_id")
    private Branch destinationBranch;

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

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransferItem> items = new ArrayList<>();

    // Getters and setters
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
    public Branch getSourceBranch() { return sourceBranch; }
    public void setSourceBranch(Branch sourceBranch) { this.sourceBranch = sourceBranch; }
    public Branch getDestinationBranch() { return destinationBranch; }
    public void setDestinationBranch(Branch destinationBranch) { this.destinationBranch = destinationBranch; }
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
    public List<TransferItem> getItems() { return items; }
    public void setItems(List<TransferItem> items) { this.items = items; }
}
