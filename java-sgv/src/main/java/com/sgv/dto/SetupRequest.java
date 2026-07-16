package com.sgv.dto;

import com.sgv.entity.AppConfig;

/**
 * DTO para o wizard de configuração inicial.
 * Apenas os campos essenciais para a primeira execução.
 */
public class SetupRequest {
    private String companyName;
    private String companyNuit;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    private String softwareCertNumber;
    private String licenseNumber;
    private String defaultSeries;
    private Long initialDocumentNumber;

    public static AppConfig apply(AppConfig cfg, SetupRequest req) {
        if (req.companyName != null) cfg.setCompanyName(req.companyName);
        if (req.companyNuit != null) cfg.setCompanyNuit(req.companyNuit);
        if (req.companyAddress != null) cfg.setCompanyAddress(req.companyAddress);
        if (req.companyPhone != null) cfg.setCompanyPhone(req.companyPhone);
        if (req.companyEmail != null) cfg.setCompanyEmail(req.companyEmail);
        if (req.softwareCertNumber != null) cfg.setSoftwareCertNumber(req.softwareCertNumber);
        if (req.licenseNumber != null) cfg.setLicenseNumber(req.licenseNumber);
        if (req.defaultSeries != null) cfg.setDefaultSeries(req.defaultSeries);
        if (req.initialDocumentNumber != null) cfg.setInitialDocumentNumber(req.initialDocumentNumber);
        cfg.setSetupCompleted(true);
        return cfg;
    }

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
    public String getSoftwareCertNumber() { return softwareCertNumber; }
    public void setSoftwareCertNumber(String softwareCertNumber) { this.softwareCertNumber = softwareCertNumber; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getDefaultSeries() { return defaultSeries; }
    public void setDefaultSeries(String defaultSeries) { this.defaultSeries = defaultSeries; }
    public Long getInitialDocumentNumber() { return initialDocumentNumber; }
    public void setInitialDocumentNumber(Long initialDocumentNumber) { this.initialDocumentNumber = initialDocumentNumber; }
}