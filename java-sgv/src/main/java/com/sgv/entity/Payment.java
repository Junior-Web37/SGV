package com.sgv.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "sale_id")
    private Sale sale;

    private Double amount;
    private String method;
    // ─── AT — Detalhes de pagamento electrónico ───────────────────────────────
    /** Referência do terminal bancário / POS (para pagamentos por cartão) */
    private String terminalRef;
    /** Tipo de cartão: VISA, MASTERCARD, MB, etc. */
    private String cardType;
    // ─────────────────────────────────────────────────────────────────────────
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    // ─── AT getters/setters ───────────────────────────────────────────────────
    public String getTerminalRef() { return terminalRef; }
    public void setTerminalRef(String terminalRef) { this.terminalRef = terminalRef; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    // ─────────────────────────────────────────────────────────────────────────
}
