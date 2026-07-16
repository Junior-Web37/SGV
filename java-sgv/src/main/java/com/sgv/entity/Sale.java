package com.sgv.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"branch_id", "series", "documentNumber", "documentType", "documentYear"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentNumber;
    private Integer documentYear;
    private String series;
    private String documentType;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String customerName;
    private String customerNuit;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal totalTax = BigDecimal.ZERO;
    private BigDecimal totalIce = BigDecimal.ZERO;
    private BigDecimal totalDiscount = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private String currency = "MZN";
    private BigDecimal exchangeRate = BigDecimal.ONE;
    private String paymentMethod;
    private String state = "EMITIDA";
    private String signatureHash;
    private String qrCode;
    private Integer reprintCount = 0;
    private Boolean pendingSync = false;
    private String customerAddress;

    @ManyToOne
    @JoinColumn(name = "origin_sale_id")
    private Sale originSale;

    private String annulReason;
    private LocalDateTime annulDate;
    private Boolean offlineFlag = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    // ─── AT (Autoridade Tributária) — Facturação Electrónica ──────────────────
    /** Hash MD5 do documento fiscal (obrigatório AT) */
    private String hashHash;
    /** Taxa de retenção na fonte (ex: 5.0 para 5%) */
    private BigDecimal withholdingTaxRate = BigDecimal.ZERO;
    /** Montante de retenção na fonte */
    private BigDecimal withholdingTax = BigDecimal.ZERO;
    /** Montante total pago pelo cliente */
    private BigDecimal paidAmount = BigDecimal.ZERO;
    /** Troco devolvido ao cliente */
    private BigDecimal changeAmount = BigDecimal.ZERO;
    /** Número sequencial controlado para o hash (AT) */
    private Long hashControl;
    /** NH-17: Documento gerado em modo demonstração (não conta para relatórios oficiais) */
    private Boolean demoFlag = false;
    // ─────────────────────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(Long documentNumber) { this.documentNumber = documentNumber; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerNuit() { return customerNuit; }
    public void setCustomerNuit(String customerNuit) { this.customerNuit = customerNuit; }
    public Double getSubtotal() { return subtotal != null ? subtotal.doubleValue() : null; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal != null ? BigDecimal.valueOf(subtotal) : null; }
    public BigDecimal getSubtotalAmount() { return subtotal; }
    public void setSubtotalAmount(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Double getTotalTax() { return totalTax != null ? totalTax.doubleValue() : null; }
    public void setTotalTax(Double totalTax) { this.totalTax = totalTax != null ? BigDecimal.valueOf(totalTax) : null; }
    public BigDecimal getTotalTaxAmount() { return totalTax; }
    public void setTotalTaxAmount(BigDecimal totalTax) { this.totalTax = totalTax; }

    public Double getTotalIce() { return totalIce != null ? totalIce.doubleValue() : null; }
    public void setTotalIce(Double totalIce) { this.totalIce = totalIce != null ? BigDecimal.valueOf(totalIce) : null; }
    public BigDecimal getTotalIceAmount() { return totalIce; }
    public void setTotalIceAmount(BigDecimal totalIce) { this.totalIce = totalIce; }

    public Double getTotalDiscount() { return totalDiscount != null ? totalDiscount.doubleValue() : null; }
    public void setTotalDiscount(Double totalDiscount) { this.totalDiscount = totalDiscount != null ? BigDecimal.valueOf(totalDiscount) : null; }
    public BigDecimal getTotalDiscountAmount() { return totalDiscount; }
    public void setTotalDiscountAmount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }

    public Double getTotal() { return total != null ? total.doubleValue() : null; }
    public void setTotal(Double total) { this.total = total != null ? BigDecimal.valueOf(total) : null; }
    public BigDecimal getTotalAmount() { return total; }
    public void setTotalAmount(BigDecimal total) { this.total = total; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Double getExchangeRate() { return exchangeRate != null ? exchangeRate.doubleValue() : null; }
    public void setExchangeRate(Double exchangeRate) { this.exchangeRate = exchangeRate != null ? BigDecimal.valueOf(exchangeRate) : null; }
    public BigDecimal getExchangeRateAmount() { return exchangeRate; }
    public void setExchangeRateAmount(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }
    public Integer getDocumentYear() { return documentYear; }
    public void setDocumentYear(Integer documentYear) { this.documentYear = documentYear; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public Integer getReprintCount() { return reprintCount; }
    public void setReprintCount(Integer reprintCount) { this.reprintCount = reprintCount; }
    public Boolean getPendingSync() { return pendingSync; }
    public void setPendingSync(Boolean pendingSync) { this.pendingSync = pendingSync; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getSignatureHash() { return signatureHash; }
    public void setSignatureHash(String signatureHash) { this.signatureHash = signatureHash; }
    public Sale getOriginSale() { return originSale; }
    public void setOriginSale(Sale originSale) { this.originSale = originSale; }
    public String getAnnulReason() { return annulReason; }
    public void setAnnulReason(String annulReason) { this.annulReason = annulReason; }
    public LocalDateTime getAnnulDate() { return annulDate; }
    public void setAnnulDate(LocalDateTime annulDate) { this.annulDate = annulDate; }
    public Boolean getOfflineFlag() { return offlineFlag; }
    public void setOfflineFlag(Boolean offlineFlag) { this.offlineFlag = offlineFlag; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }
    // ─── AT getters/setters ───────────────────────────────────────────────────
    public String getHashHash() { return hashHash; }
    public void setHashHash(String hashHash) { this.hashHash = hashHash; }
    public Double getWithholdingTaxRate() { return withholdingTaxRate != null ? withholdingTaxRate.doubleValue() : null; }
    public void setWithholdingTaxRate(Double withholdingTaxRate) { this.withholdingTaxRate = withholdingTaxRate != null ? BigDecimal.valueOf(withholdingTaxRate) : null; }
    public BigDecimal getWithholdingTaxRateAmount() { return withholdingTaxRate; }
    public void setWithholdingTaxRateAmount(BigDecimal withholdingTaxRate) { this.withholdingTaxRate = withholdingTaxRate; }

    public Double getWithholdingTax() { return withholdingTax != null ? withholdingTax.doubleValue() : null; }
    public void setWithholdingTax(Double withholdingTax) { this.withholdingTax = withholdingTax != null ? BigDecimal.valueOf(withholdingTax) : null; }
    public BigDecimal getWithholdingTaxAmount() { return withholdingTax; }
    public void setWithholdingTaxAmount(BigDecimal withholdingTax) { this.withholdingTax = withholdingTax; }

    public Double getPaidAmount() { return paidAmount != null ? paidAmount.doubleValue() : null; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount != null ? BigDecimal.valueOf(paidAmount) : null; }
    public BigDecimal getPaidAmountValue() { return paidAmount; }
    public void setPaidAmountValue(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public Double getChangeAmount() { return changeAmount != null ? changeAmount.doubleValue() : null; }
    public void setChangeAmount(Double changeAmount) { this.changeAmount = changeAmount != null ? BigDecimal.valueOf(changeAmount) : null; }
    public BigDecimal getChangeAmountValue() { return changeAmount; }
    public void setChangeAmountValue(BigDecimal changeAmount) { this.changeAmount = changeAmount; }
    public Long getHashControl() { return hashControl; }
    public void setHashControl(Long hashControl) { this.hashControl = hashControl; }
    public Boolean getDemoFlag() { return demoFlag; }
    public void setDemoFlag(Boolean demoFlag) { this.demoFlag = demoFlag; }
    // ─────────────────────────────────────────────────────────────────────────

    @PreRemove
    public void preRemove() {
        if ("EMITIDA".equals(state) || "ANULADA".equals(state)) {
            throw new IllegalStateException("Não é possível remover um documento fiscal já emitido ou anulado.");
        }
    }

    @PreUpdate
    public void preUpdate() {
        // @PreUpdate corre DEPOIS de setState("ANULADA") no SaleController.annul(),
        // logo this.state já é "ANULADA" quando esta validação corre — a excepção não é lançada na anulação.
        if ("EMITIDA".equals(state)) {
            throw new IllegalStateException(
                "Um documento fiscal EMITIDO não pode ser modificado directamente na Base de Dados. Apenas pode ser Anulado.");
        }
    }

    @Override
    public String toString() {
        String cust = customerName != null ? customerName : (customer != null ? customer.getName() : "Cliente");
        return documentType + " #" + documentNumber + " - " + cust + (total != null ? " (" + String.format("%.2f", total) + " MT)" : "");
    }
}
