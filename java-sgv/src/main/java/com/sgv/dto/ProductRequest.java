package com.sgv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private Long categoryId;
    private Boolean isService;
    private String unit;
    private String unitBulk;
    private Double priceCost;
    private Double priceSale;
    private Double priceSaleBulk;
    private Double bulkQuantity;
    private Double conversionFactor;
    private Double profitMargin;
    private Double taxRate;
    private Double iceRate;
    private String expiryDate;
    private String description;
    private String supplier;
    private String supplierReference;
    private String entryDate;
    private String exitDate;
    private String location;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Boolean getIsService() { return isService; }
    public void setIsService(Boolean isService) { this.isService = isService; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getUnitBulk() { return unitBulk; }
    public void setUnitBulk(String unitBulk) { this.unitBulk = unitBulk; }
    public Double getPriceCost() { return priceCost; }
    public void setPriceCost(Double priceCost) { this.priceCost = priceCost; }
    public Double getPriceSale() { return priceSale; }
    public void setPriceSale(Double priceSale) { this.priceSale = priceSale; }
    public Double getPriceSaleBulk() { return priceSaleBulk; }
    public void setPriceSaleBulk(Double priceSaleBulk) { this.priceSaleBulk = priceSaleBulk; }
    public Double getBulkQuantity() { return bulkQuantity; }
    public void setBulkQuantity(Double bulkQuantity) { this.bulkQuantity = bulkQuantity; }
    public Double getConversionFactor() { return conversionFactor; }
    public void setConversionFactor(Double conversionFactor) { this.conversionFactor = conversionFactor; }
    public Double getProfitMargin() { return profitMargin; }
    public void setProfitMargin(Double profitMargin) { this.profitMargin = profitMargin; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public Double getIceRate() { return iceRate; }
    public void setIceRate(Double iceRate) { this.iceRate = iceRate; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getSupplierReference() { return supplierReference; }
    public void setSupplierReference(String supplierReference) { this.supplierReference = supplierReference; }
    public String getEntryDate() { return entryDate; }
    public void setEntryDate(String entryDate) { this.entryDate = entryDate; }
    public String getExitDate() { return exitDate; }
    public void setExitDate(String exitDate) { this.exitDate = exitDate; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
