package com.sgv.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_items")
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private String productCode;
    private String unit;
    private String description;
    private BigDecimal qty = BigDecimal.ONE;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal iceRate = BigDecimal.ZERO;
    private BigDecimal lineBase = BigDecimal.ZERO;
    private BigDecimal lineDiscount = BigDecimal.ZERO;
    private BigDecimal lineIce = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal lineTax = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;

    // ─── AT — Facturação Electrónica ─────────────────────────────────────────
    /**
     * Tipo de imposto do item.
     * Valores AT: IVA, IS, IES, NS, IEX, IEC
     * (nível item, para documentos onde cada item pode ter tipo diferente)
     */
    private String taxType = "IVA";
    /**
     * Motivo de inexistência de imposto quando taxRate = 0.
     * Valores AT: M01 (Art.10º), M02 (Art.9º), M09 (Isenção), M10-M19 (outros)
     */
    private String motivoInexistTax;
    // ─────────────────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getQty() { return qty != null ? qty.doubleValue() : null; }
    public void setQty(Double qty) { this.qty = qty != null ? BigDecimal.valueOf(qty) : null; }
    public BigDecimal getQtyAmount() { return qty; }
    public void setQtyAmount(BigDecimal qty) { this.qty = qty; }

    public Double getUnitPrice() { return unitPrice != null ? unitPrice.doubleValue() : null; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice != null ? BigDecimal.valueOf(unitPrice) : null; }
    public BigDecimal getUnitPriceAmount() { return unitPrice; }
    public void setUnitPriceAmount(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Double getDiscount() { return discount != null ? discount.doubleValue() : null; }
    public void setDiscount(Double discount) { this.discount = discount != null ? BigDecimal.valueOf(discount) : null; }
    public BigDecimal getDiscountAmount() { return discount; }
    public void setDiscountAmount(BigDecimal discount) { this.discount = discount; }

    public Double getIceRate() { return iceRate != null ? iceRate.doubleValue() : null; }
    public void setIceRate(Double iceRate) { this.iceRate = iceRate != null ? BigDecimal.valueOf(iceRate) : null; }
    public BigDecimal getIceRateAmount() { return iceRate; }
    public void setIceRateAmount(BigDecimal iceRate) { this.iceRate = iceRate; }

    public Double getLineBase() { return lineBase != null ? lineBase.doubleValue() : null; }
    public void setLineBase(Double lineBase) { this.lineBase = lineBase != null ? BigDecimal.valueOf(lineBase) : null; }
    public BigDecimal getLineBaseAmount() { return lineBase; }
    public void setLineBaseAmount(BigDecimal lineBase) { this.lineBase = lineBase; }

    public Double getLineDiscount() { return lineDiscount != null ? lineDiscount.doubleValue() : null; }
    public void setLineDiscount(Double lineDiscount) { this.lineDiscount = lineDiscount != null ? BigDecimal.valueOf(lineDiscount) : null; }
    public BigDecimal getLineDiscountAmount() { return lineDiscount; }
    public void setLineDiscountAmount(BigDecimal lineDiscount) { this.lineDiscount = lineDiscount; }

    public Double getLineIce() { return lineIce != null ? lineIce.doubleValue() : null; }
    public void setLineIce(Double lineIce) { this.lineIce = lineIce != null ? BigDecimal.valueOf(lineIce) : null; }
    public BigDecimal getLineIceAmount() { return lineIce; }
    public void setLineIceAmount(BigDecimal lineIce) { this.lineIce = lineIce; }

    public Double getTaxRate() { return taxRate != null ? taxRate.doubleValue() : null; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate != null ? BigDecimal.valueOf(taxRate) : null; }
    public BigDecimal getTaxRateAmount() { return taxRate; }
    public void setTaxRateAmount(BigDecimal taxRate) { this.taxRate = taxRate; }

    public Double getLineTax() { return lineTax != null ? lineTax.doubleValue() : null; }
    public void setLineTax(Double lineTax) { this.lineTax = lineTax != null ? BigDecimal.valueOf(lineTax) : null; }
    public BigDecimal getLineTaxAmount() { return lineTax; }
    public void setLineTaxAmount(BigDecimal lineTax) { this.lineTax = lineTax; }

    public Double getLineTotal() { return lineTotal != null ? lineTotal.doubleValue() : null; }
    public void setLineTotal(Double lineTotal) { this.lineTotal = lineTotal != null ? BigDecimal.valueOf(lineTotal) : null; }
    public BigDecimal getLineTotalAmount() { return lineTotal; }
    public void setLineTotalAmount(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    // ─── AT getters/setters ───────────────────────────────────────────────────
    public String getTaxType() { return taxType; }
    public void setTaxType(String taxType) { this.taxType = taxType; }
    public String getMotivoInexistTax() { return motivoInexistTax; }
    public void setMotivoInexistTax(String motivoInexistTax) { this.motivoInexistTax = motivoInexistTax; }
    // ─────────────────────────────────────────────────────────────────────────

    public String getProductName() {
        return product != null ? product.getName() : description;
    }

    public Double getTotal() {
        return lineTotal != null ? lineTotal.doubleValue() : null;
    }

    public Double getQuantity() {
        return qty != null ? qty.doubleValue() : null;
    }

    @PreRemove
    public void preRemove() {
        if (sale != null && ("EMITIDA".equals(sale.getState()) || "ANULADA".equals(sale.getState()))) {
            throw new IllegalStateException("Não é possível remover um item de um documento fiscal já emitido ou anulado.");
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (sale != null && ("EMITIDA".equals(sale.getState()) || "ANULADA".equals(sale.getState()))) {
            throw new IllegalStateException("Não é possível atualizar um item de um documento fiscal já emitido ou anulado.");
        }
    }
}
