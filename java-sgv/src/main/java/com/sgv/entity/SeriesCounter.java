package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "series_counters", uniqueConstraints = {
    @UniqueConstraint(name = "uk_series_counter_branch_series_type", columnNames = {"branch_id", "series", "document_type"})
})
public class SeriesCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(nullable = false, length = 10)
    private String series = "A";

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType = "VENDA";

    @Column(name = "current_number", nullable = false)
    private Long currentNumber = 0L;

    @Column(name = "last_hash_control", nullable = false)
    private Long lastHashControl = 0L;

    @Column(name = "last_signature_hash", length = 255)
    private String lastSignatureHash;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public SeriesCounter() {}

    public SeriesCounter(Long branchId, String series, String documentType, Long currentNumber) {
        this.branchId = branchId;
        this.series = series;
        this.documentType = documentType;
        this.currentNumber = currentNumber;
        this.lastHashControl = currentNumber;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public Long getCurrentNumber() { return currentNumber; }
    public void setCurrentNumber(Long currentNumber) { this.currentNumber = currentNumber; }

    public Long getLastHashControl() { return lastHashControl; }
    public void setLastHashControl(Long lastHashControl) { this.lastHashControl = lastHashControl; }

    public String getLastSignatureHash() { return lastSignatureHash; }
    public void setLastSignatureHash(String lastSignatureHash) { this.lastSignatureHash = lastSignatureHash; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
