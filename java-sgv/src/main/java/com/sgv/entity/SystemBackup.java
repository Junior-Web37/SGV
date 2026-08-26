package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registo de backups do sistema na base de dados.
 */
@Entity
@Table(name = "system_backups")
public class SystemBackup {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false, length = 512)
    private String filePath;
    
    private Long fileSize = 0L;
    
    @Column(nullable = false)
    private String backupType = "MANUAL";
    
    @Column(nullable = false)
    private String status = "COMPLETED";
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private String createdBy;
    
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** SHA-256 do ficheiro de backup (correção do BUG-001 — validação de integridade no restore). */
    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getBackupType() { return backupType; }
    public void setBackupType(String backupType) { this.backupType = backupType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    // Utilitário para formato do tamanho
    public String getFormattedSize() {
        if (fileSize == null || fileSize == 0) return "0 B";
        long kb = fileSize / 1024;
        if (kb < 1024) return kb + " KB";
        return String.format("%.1f MB", kb / 1024.0);
    }
}
