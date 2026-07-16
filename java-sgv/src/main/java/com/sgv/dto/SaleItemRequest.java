package com.sgv.dto;

public class SaleItemRequest {
    private Long productId;
    private String description;
    private String unit;
    @jakarta.validation.constraints.NotNull
    private Double qty;
    @jakarta.validation.constraints.NotNull
    private Double unitPrice;
    private Double taxRate;
    private Double discount;
    private Double iceRate;
    // ─── AT — Facturação Electrónica ─────────────────────────────────────────
    /** Tipo de imposto: IVA, IS, IES, NS, IEX, IEC */
    private String taxType;
    /**
     * Motivo de inexistência de IVA quando taxRate = 0.
     * Valores: M01, M02, M09, M10-M19
     */
    private String motivoInexistTax;
    // ─────────────────────────────────────────────────────────────────────────

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }
    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public Double getIceRate() { return iceRate; }
    public void setIceRate(Double iceRate) { this.iceRate = iceRate; }
    // ─── AT getters/setters ───────────────────────────────────────────────────
    public String getTaxType() { return taxType; }
    public void setTaxType(String taxType) { this.taxType = taxType; }
    public String getMotivoInexistTax() { return motivoInexistTax; }
    public void setMotivoInexistTax(String motivoInexistTax) { this.motivoInexistTax = motivoInexistTax; }
    // ─────────────────────────────────────────────────────────────────────────
}
