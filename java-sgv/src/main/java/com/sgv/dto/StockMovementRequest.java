package com.sgv.dto;

public class StockMovementRequest {
    @jakarta.validation.constraints.NotNull
    private Long productId;
    @jakarta.validation.constraints.NotNull
    private Long branchId;
    @jakarta.validation.constraints.NotBlank
    private String type;
    private String subtype;
    @jakarta.validation.constraints.NotNull
    private Double qty;
    private String reference;
    private String notes;
    private Double unitCostPrice;
    private Double unitSalePrice;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubtype() { return subtype; }
    public void setSubtype(String subtype) { this.subtype = subtype; }
    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Double getUnitCostPrice() { return unitCostPrice; }
    public void setUnitCostPrice(Double unitCostPrice) { this.unitCostPrice = unitCostPrice; }
    public Double getUnitSalePrice() { return unitSalePrice; }
    public void setUnitSalePrice(Double unitSalePrice) { this.unitSalePrice = unitSalePrice; }
}
