package com.sgv.dto;

import java.util.List;

public class SaleRequest {
    @jakarta.validation.constraints.NotNull
    private Long branchId;
    private Long customerId;
    private String customerName;
    private String customerNuit;
    private String customerAddress;
    private String paymentMethod;
    private String documentType;
    private String series;
    private String documentDateTime;
    private Long originSaleId;
    private Boolean offlineFlag = false;
    // ─── AT — Retenção na fonte ───────────────────────────────────────────────
    private Double withholdingTaxRate;
    // ─────────────────────────────────────────────────────────────────────────
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Size(min = 1)
    private List<SaleItemRequest> items;

    /** Lista de pagamentos para a venda */
    private List<PaymentRequest> payments;

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerNuit() { return customerNuit; }
    public void setCustomerNuit(String customerNuit) { this.customerNuit = customerNuit; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getDocumentDateTime() { return documentDateTime; }
    public void setDocumentDateTime(String documentDateTime) { this.documentDateTime = documentDateTime; }
    public Long getOriginSaleId() { return originSaleId; }
    public void setOriginSaleId(Long originSaleId) { this.originSaleId = originSaleId; }
    public Boolean getOfflineFlag() { return offlineFlag; }
    public void setOfflineFlag(Boolean offlineFlag) { this.offlineFlag = offlineFlag; }
    // ─── AT getters/setters ───────────────────────────────────────────────────
    public Double getWithholdingTaxRate() { return withholdingTaxRate; }
    public void setWithholdingTaxRate(Double withholdingTaxRate) { this.withholdingTaxRate = withholdingTaxRate; }
    // ─────────────────────────────────────────────────────────────────────────
    public List<SaleItemRequest> getItems() { return items; }
    public void setItems(List<SaleItemRequest> items) { this.items = items; }

    public List<PaymentRequest> getPayments() { return payments; }
    public void setPayments(List<PaymentRequest> payments) { this.payments = payments; }
}
