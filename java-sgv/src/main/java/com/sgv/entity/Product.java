package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    
    @Column(name = "price_cost", precision = 19, scale = 4)
    private BigDecimal priceCost = BigDecimal.ZERO;
    @Column(name = "price_sale", precision = 19, scale = 4)
    private BigDecimal priceSale = BigDecimal.ZERO;
    @Column(name = "price_sale_bulk", precision = 19, scale = 4)
    private BigDecimal priceSaleBulk = BigDecimal.ZERO;
    private Double bulkQuantity = 0.0;
    private Double conversionFactor = 1.0;
    
    /** Margem de lucro em percentagem (calculada: ((priceSale - priceCost) / priceCost) * 100) */
    private Double profitMargin = 0.0;
    @Transient
    private BigDecimal profitMarginAmount = BigDecimal.ZERO;
    
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
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    /** @deprecated Usar {@link #getIsActive()} com Boolean.TRUE.equals() */
    @Deprecated
    public Boolean getActive() { return isActive != null && isActive; }
    public void setActive(Boolean active) { isActive = active; }
    
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
    
    public Double getPriceCost() { return priceCost != null ? priceCost.doubleValue() : 0.0; }
    public void setPriceCost(Double priceCost) { this.priceCost = priceCost != null ? BigDecimal.valueOf(priceCost) : BigDecimal.ZERO; recalculateProfitMargin(); }
    public BigDecimal getPriceCostAmount() { return priceCost; }
    public void setPriceCostAmount(BigDecimal priceCost) { this.priceCost = priceCost; recalculateProfitMargin(); }
    
    public Double getPriceSale() { return priceSale != null ? priceSale.doubleValue() : 0.0; }
    public void setPriceSale(Double priceSale) { this.priceSale = priceSale != null ? BigDecimal.valueOf(priceSale) : BigDecimal.ZERO; recalculateProfitMargin(); }
    public BigDecimal getPriceSaleAmount() { return priceSale; }
    public void setPriceSaleAmount(BigDecimal priceSale) { this.priceSale = priceSale; recalculateProfitMargin(); }
    
    public Double getPriceSaleBulk() { return priceSaleBulk != null ? priceSaleBulk.doubleValue() : 0.0; }
    public void setPriceSaleBulk(Double priceSaleBulk) { this.priceSaleBulk = priceSaleBulk != null ? BigDecimal.valueOf(priceSaleBulk) : BigDecimal.ZERO; }
    public BigDecimal getPriceSaleBulkAmount() { return priceSaleBulk; }
    public void setPriceSaleBulkAmount(BigDecimal priceSaleBulk) { this.priceSaleBulk = priceSaleBulk; }
    
    public Double getBulkQuantity() { return bulkQuantity; }
    public void setBulkQuantity(Double bulkQuantity) { this.bulkQuantity = bulkQuantity; }
    
    public Double getConversionFactor() { return conversionFactor; }
    public void setConversionFactor(Double conversionFactor) { this.conversionFactor = conversionFactor; }
    
    public Double getProfitMargin() { return profitMargin; }
    public void setProfitMargin(Double profitMargin) {
        this.profitMargin = profitMargin != null ? profitMargin : 0.0;
        this.profitMarginAmount = BigDecimal.valueOf(this.profitMargin)
                .setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal getProfitMarginAmount() {
        return profitMarginAmount != null ? profitMarginAmount : BigDecimal.ZERO;
    }

    public void setProfitMarginAmount(BigDecimal profitMarginAmount) {
        this.profitMarginAmount = profitMarginAmount != null
                ? profitMarginAmount.setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.profitMargin = this.profitMarginAmount.doubleValue();
    }
    
    /**
     * Calcula e define a margem de lucro automaticamente.
     * Fórmula: ((priceSale - priceCost) / priceCost) * 100
     */
    public void recalculateProfitMargin() {
        if (priceCost != null && priceCost.compareTo(BigDecimal.ZERO) > 0 && priceSale != null) {
            BigDecimal delta = priceSale.subtract(priceCost);
            BigDecimal percentage = delta.divide(priceCost, 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            setProfitMarginAmount(percentage.setScale(4, RoundingMode.HALF_UP));
        } else {
            setProfitMarginAmount(BigDecimal.ZERO);
        }
    }
    
    /**
     * Define o preço de venda com base na margem de lucro desejada.
     * Fórmula: priceSale = priceCost * (1 + profitMargin/100)
     */
    public void calculatePriceFromMargin() {
        BigDecimal margin = profitMarginAmount != null && profitMarginAmount.compareTo(BigDecimal.ZERO) != 0
                ? profitMarginAmount
                : (profitMargin != null ? BigDecimal.valueOf(profitMargin) : BigDecimal.ZERO);

        if (priceCost != null && priceCost.compareTo(BigDecimal.ZERO) > 0 && margin != null) {
            BigDecimal percentFactor = margin.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            this.priceSale = priceCost.multiply(BigDecimal.ONE.add(percentFactor))
                    .setScale(4, RoundingMode.HALF_UP);
            setProfitMarginAmount(margin.setScale(4, RoundingMode.HALF_UP));
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
     * Gera o prefixo do código: 3 primeiras letras do nome (maiúsculas, sem acentos).
     * Ex: "Água Mineral" → "AGU", "Arroz" → "ARR", "Milho" → "MIL"
     */
    public static String generatePrefix(String name) {
        if (name == null || name.isBlank()) return "PRO";
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
        return prefix;
    }

    /**
     * Gera código sequencial: prefixo + número de 5 dígitos.
     * Ex: "Água Mineral" (produto #1) → "AGU-00001"
     */
    public static String generateCode(String prefix, long seqNumber) {
        return prefix + "-" + String.format("%05d", seqNumber);
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
