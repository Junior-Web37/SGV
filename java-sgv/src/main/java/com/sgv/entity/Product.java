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

    /** Produto activo (pode ser vendido) ou inactivo (venda proibida) */
    @Column(nullable = false)
    private Boolean isActive = true;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

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
    
    /** Margem de lucro em percentagem (calculada: ((priceSale - priceCost) / priceCost) * 100) */
    private Double profitMargin = 0.0;
    
    private Double taxRate = 0.0;      // IVA definido por produto (pode herdar da categoria)
    private Double iceRate = 0.0;      // ICE definido por produto (pode herdar da categoria)
    
    /** Taxa IVA padrão do sistema (usado se taxRate = 0) */
    private Double defaultTaxRate = 16.0;
    /** Taxa ICE padrão do sistema (usado se iceRate = 0) */
    private Double defaultIceRate = 0.0;
    
    private String expiryDate;
    private String description;        // Descrição curta do produto
    private String supplierReference;
    private String entryDate;
    private String exitDate;
    private String location;
    
    /** Stock máximo permitido (para alertas) */
    private Double stockMax = 0.0;
    /** Stock mínimo (para alertas de reposição) */
    private Double stockMin = 0.0;
    
    /** Comissão do vendedor para este produto (em %) */
    private Double commissionPercent = 0.0;
    
    /** Observações adicionais */
    private String observations;
    
    private LocalDateTime lastMovementAt;

    // ─── getters e setters ──────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Boolean getActive() { return isActive != null && isActive; }
    public void setActive(Boolean active) { isActive = active; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    
    public Boolean getService() { return isService; }
    public void setService(Boolean service) { isService = service; }
    
    public MetricUnit getUnit() { return unit; }
    public void setUnit(MetricUnit unit) { this.unit = unit; }
    
    public MetricUnit getUnitBulk() { return unitBulk; }
    public void setUnitBulk(MetricUnit unitBulk) { this.unitBulk = unitBulk; }
    
    public Double getPriceCost() { return priceCost; }
    public void setPriceCost(Double priceCost) { this.priceCost = priceCost; recalculateProfitMargin(); }
    
    public Double getPriceSale() { return priceSale; }
    public void setPriceSale(Double priceSale) { this.priceSale = priceSale; recalculateProfitMargin(); }
    
    public Double getPriceSaleBulk() { return priceSaleBulk; }
    public void setPriceSaleBulk(Double priceSaleBulk) { this.priceSaleBulk = priceSaleBulk; }
    
    public Double getBulkQuantity() { return bulkQuantity; }
    public void setBulkQuantity(Double bulkQuantity) { this.bulkQuantity = bulkQuantity; }
    
    public Double getConversionFactor() { return conversionFactor; }
    public void setConversionFactor(Double conversionFactor) { this.conversionFactor = conversionFactor; }
    
    public Double getProfitMargin() { return profitMargin; }
    public void setProfitMargin(Double profitMargin) { this.profitMargin = profitMargin; }
    
    /**
     * Calcula e define a margem de lucro automaticamente.
     * Fórmula: ((priceSale - priceCost) / priceCost) * 100
     */
    public void recalculateProfitMargin() {
        if (priceCost != null && priceCost > 0 && priceSale != null) {
            this.profitMargin = ((priceSale - priceCost) / priceCost) * 100.0;
        } else {
            this.profitMargin = 0.0;
        }
    }
    
    /**
     * Define o preço de venda com base na margem de lucro desejada.
     * Fórmula: priceSale = priceCost * (1 + profitMargin/100)
     */
    public void calculatePriceFromMargin() {
        if (priceCost != null && priceCost > 0 && profitMargin != null) {
            this.priceSale = priceCost * (1 + profitMargin / 100.0);
        }
    }
    
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    
    public Double getIceRate() { return iceRate; }
    public void setIceRate(Double iceRate) { this.iceRate = iceRate; }
    
    public Double getDefaultTaxRate() { return defaultTaxRate != null ? defaultTaxRate : 16.0; }
    public void setDefaultTaxRate(Double defaultTaxRate) { this.defaultTaxRate = defaultTaxRate; }
    
    public Double getDefaultIceRate() { return defaultIceRate != null ? defaultIceRate : 0.0; }
    public void setDefaultIceRate(Double defaultIceRate) { this.defaultIceRate = defaultIceRate; }
    
    /**
     * Retorna a taxa IVA efectiva: do produto ou o padrão do sistema.
     */
    public Double getEffectiveTaxRate() {
        if (taxRate != null && taxRate > 0) return taxRate;
        return getDefaultTaxRate();
    }
    
    /**
     * Retorna a taxa ICE efectiva: do produto ou o padrão do sistema.
     */
    public Double getEffectiveIceRate() {
        if (iceRate != null && iceRate > 0) return iceRate;
        return getDefaultIceRate();
    }
    
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSupplierReference() { return supplierReference; }
    public void setSupplierReference(String supplierReference) { this.supplierReference = supplierReference; }
    
    public String getEntryDate() { return entryDate; }
    public void setEntryDate(String entryDate) { this.entryDate = entryDate; }
    
    public String getExitDate() { return exitDate; }
    public void setExitDate(String exitDate) { this.exitDate = exitDate; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public Double getStockMax() { return stockMax != null ? stockMax : 0.0; }
    public void setStockMax(Double stockMax) { this.stockMax = stockMax; }
    
    public Double getStockMin() { return stockMin != null ? stockMin : 0.0; }
    public void setStockMin(Double stockMin) { this.stockMin = stockMin; }
    
    public Double getCommissionPercent() { return commissionPercent != null ? commissionPercent : 0.0; }
    public void setCommissionPercent(Double commissionPercent) { this.commissionPercent = commissionPercent; }
    
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    
    public LocalDateTime getLastMovementAt() { return lastMovementAt; }
    public void setLastMovementAt(LocalDateTime lastMovementAt) { this.lastMovementAt = lastMovementAt; }
    
    // ─── Utilitários ───────────────────────────────────────────────────────
    
    /**
     * Gera código automático: 3 primeiras letras do nome (maiúsculas) + ID ou timestamp.
     * Ex: "Água Mineral" → "AGU-001" ou "AGU-1234567890"
     */
    public static String generateCode(String name, Long existingId) {
        if (name == null || name.isBlank()) return "PROD-" + System.currentTimeMillis();
        
        // Remove acentos e pega as 3 primeiras letras
        String cleaned = name.toUpperCase()
                .replaceAll("[ÀÁÂÃÄÅ]", "A")
                .replaceAll("[ÈÉÊË]", "E")
                .replaceAll("[ÌÍÎÏ]", "I")
                .replaceAll("[ÒÓÔÕÖ]", "O")
                .replaceAll("[ÙÚÛÜ]", "U")
                .replaceAll("[Ç]", "C")
                .replaceAll("[Ñ]", "N")
                .replaceAll("[^A-Z0-9]", "");
        
        String prefix = cleaned.length() >= 3 ? cleaned.substring(0, 3) : cleaned;
        if (prefix.length() < 3) prefix = String.format("%-3s", prefix).replace(' ', 'X');
        
        if (existingId != null && existingId > 0) {
            return prefix + "-" + String.format("%04d", existingId % 10000);
        }
        return prefix + "-" + (System.currentTimeMillis() % 100000);
    }
    
    /**
     * Verifica se o produto pode ser vendido.
     * Produtos inactivos (isActive = false) não podem ser vendidos.
     */
    public boolean canBeSold() {
        return Boolean.TRUE.equals(isActive);
    }

    @Override
    public String toString() {
        if (name == null) return "Produto";
        String status = Boolean.TRUE.equals(isActive) ? "" : " [INACTIVO]";
        return code != null ? code + " — " + name + status : name + status;
    }
}
