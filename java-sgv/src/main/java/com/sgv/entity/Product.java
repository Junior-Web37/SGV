package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    private Boolean isService = false;
    @ManyToOne
    @JoinColumn(name = "unit_id")
    private MetricUnit unit;

    @ManyToOne
    @JoinColumn(name = "unit_bulk_id")
    private MetricUnit unitBulk;
    private Double priceCost = 0.0;
    private Double priceSale = 0.0;
    private Double priceSaleBulk = 0.0;
    private Double bulkQuantity = 0.0;
    private Double conversionFactor = 1.0;
    private Double profitMargin = 0.0;
    private Double taxRate = 0.0;
    private Double iceRate = 0.0;
    private String expiryDate;
    private String description;
    private String supplier;
    private String supplierReference;
    private String entryDate;
    private String exitDate;
    private String location;
    private LocalDateTime lastMovementAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Boolean getService() { return isService; }
    public void setService(Boolean service) { isService = service; }
    public MetricUnit getUnit() { return unit; }
    public void setUnit(MetricUnit unit) { this.unit = unit; }
    public MetricUnit getUnitBulk() { return unitBulk; }
    public void setUnitBulk(MetricUnit unitBulk) { this.unitBulk = unitBulk; }
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
    public LocalDateTime getLastMovementAt() { return lastMovementAt; }
    public void setLastMovementAt(LocalDateTime lastMovementAt) { this.lastMovementAt = lastMovementAt; }

    @Override
    public String toString() {
        if (name == null) return "Produto";
        return code != null ? code + " — " + name : name;
    }
}
