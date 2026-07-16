package com.sgv.dto;

public class PaymentRequest {
    @jakarta.validation.constraints.NotNull
    private Long saleId;
    @jakarta.validation.constraints.NotNull
    private Double amount;
    @jakarta.validation.constraints.NotBlank
    private String method;
    // ─── AT — Detalhes de pagamento electrónico ───────────────────────────────
    /** Referência do terminal bancário / POS */
    private String terminalRef;
    /** Tipo de cartão: VISA, MASTERCARD, MB, etc. */
    private String cardType;
    // ─────────────────────────────────────────────────────────────────────────

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    // ─── AT getters/setters ───────────────────────────────────────────────────
    public String getTerminalRef() { return terminalRef; }
    public void setTerminalRef(String terminalRef) { this.terminalRef = terminalRef; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    // ─────────────────────────────────────────────────────────────────────────
}
