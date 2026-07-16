package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Configuração global da aplicação (uma única linha com id=1).
 * Usada pelo wizard de configuração inicial e para flags da AT.
 */
@Entity
@Table(name = "app_config")
public class AppConfig {

    @Id
    private Long id = 1L;

    // ─── Dados da Empresa (obrigatórios para conformidade AT) ──────────────
    private String companyName;
    private String companyNuit;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    private String companyWebsite;

    // ─── Certificação AT ──────────────────────────────────────────────────────
    private String softwareCertNumber;
    private String licenseNumber;

    // ─── Configurações operacionais ───────────────────────────────────────────
    /** Série inicial (ex: "A") */
    private String defaultSeries = "A";
    /** Número inicial da série (ex: 1) */
    private Long initialDocumentNumber = 1L;
    /** Moeda padrão */
    private String defaultCurrency = "MZN";
    /** NUIT para consumidor final */
    private String consumerFinalNuit = "999999999";

    // ─── Flags de funcionalidade ───────────────────────────────────────────────
    private Boolean demoMode = false;
    private Boolean autoBackupEnabled = true;
    private Boolean offlineModeEnabled = true;
    private Boolean duplicateDetectionEnabled = true;
    /** Janela em segundos para detecção de duplicação */
    private Integer duplicateWindowSeconds = 60;

    // ─── Canais de envio de recibo ─────────────────────────────────────────────
    private String whatsappApiKey;
    private String whatsappPhoneNumber;
    private String smsApiKey;
    private String smsSender;

    // ─── Impressora térmica ────────────────────────────────────────────────────
    private String thermalPrinterName;
    private Integer thermalPrinterWidth = 80;

    // ─── Auditoria ─────────────────────────────────────────────────────────────
    private Boolean setupCompleted = false;
    private LocalDateTime setupCompletedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getCompanyNuit() { return companyNuit; }
    public void setCompanyNuit(String companyNuit) { this.companyNuit = companyNuit; }
    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }
    public String getCompanyWebsite() { return companyWebsite; }
    public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }
    public String getSoftwareCertNumber() { return softwareCertNumber; }
    public void setSoftwareCertNumber(String softwareCertNumber) { this.softwareCertNumber = softwareCertNumber; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getDefaultSeries() { return defaultSeries; }
    public void setDefaultSeries(String defaultSeries) { this.defaultSeries = defaultSeries; }
    public Long getInitialDocumentNumber() { return initialDocumentNumber; }
    public void setInitialDocumentNumber(Long initialDocumentNumber) { this.initialDocumentNumber = initialDocumentNumber; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public String getConsumerFinalNuit() { return consumerFinalNuit; }
    public void setConsumerFinalNuit(String consumerFinalNuit) { this.consumerFinalNuit = consumerFinalNuit; }
    public Boolean getDemoMode() { return demoMode; }
    public void setDemoMode(Boolean demoMode) { this.demoMode = demoMode; }
    public Boolean getAutoBackupEnabled() { return autoBackupEnabled; }
    public void setAutoBackupEnabled(Boolean autoBackupEnabled) { this.autoBackupEnabled = autoBackupEnabled; }
    public Boolean getOfflineModeEnabled() { return offlineModeEnabled; }
    public void setOfflineModeEnabled(Boolean offlineModeEnabled) { this.offlineModeEnabled = offlineModeEnabled; }
    public Boolean getDuplicateDetectionEnabled() { return duplicateDetectionEnabled; }
    public void setDuplicateDetectionEnabled(Boolean duplicateDetectionEnabled) { this.duplicateDetectionEnabled = duplicateDetectionEnabled; }
    public Integer getDuplicateWindowSeconds() { return duplicateWindowSeconds; }
    public void setDuplicateWindowSeconds(Integer duplicateWindowSeconds) { this.duplicateWindowSeconds = duplicateWindowSeconds; }
    public String getWhatsappApiKey() { return whatsappApiKey; }
    public void setWhatsappApiKey(String whatsappApiKey) { this.whatsappApiKey = whatsappApiKey; }
    public String getWhatsappPhoneNumber() { return whatsappPhoneNumber; }
    public void setWhatsappPhoneNumber(String whatsappPhoneNumber) { this.whatsappPhoneNumber = whatsappPhoneNumber; }
    public String getSmsApiKey() { return smsApiKey; }
    public void setSmsApiKey(String smsApiKey) { this.smsApiKey = smsApiKey; }
    public String getSmsSender() { return smsSender; }
    public void setSmsSender(String smsSender) { this.smsSender = smsSender; }
    public String getThermalPrinterName() { return thermalPrinterName; }
    public void setThermalPrinterName(String thermalPrinterName) { this.thermalPrinterName = thermalPrinterName; }
    public Integer getThermalPrinterWidth() { return thermalPrinterWidth; }
    public void setThermalPrinterWidth(Integer thermalPrinterWidth) { this.thermalPrinterWidth = thermalPrinterWidth; }
    public Boolean getSetupCompleted() { return setupCompleted; }
    public void setSetupCompleted(Boolean setupCompleted) { this.setupCompleted = setupCompleted; }
    public LocalDateTime getSetupCompletedAt() { return setupCompletedAt; }
    public void setSetupCompletedAt(LocalDateTime setupCompletedAt) { this.setupCompletedAt = setupCompletedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}