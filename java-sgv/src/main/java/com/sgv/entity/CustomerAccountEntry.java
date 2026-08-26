package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lançamento imutável do livro de conta corrente do cliente.
 *
 * <p>Correcção do BUG-005/BUG-010: o saldo {@link Customer#getBalanceAmount()}
 * deixou de ser uma caixa preta — cada variação passa a ser registada aqui com
 * valor SINALIZADO (positivo aumenta a dívida do cliente, negativo diminui).
 * A invariante Σ(lançamentos) == saldo é garantida estruturalmente porque o
 * único ponto de escrita do saldo é o {@link com.sgv.service.CustomerAccountLedger},
 * que grava cliente e lançamento na mesma transação.</p>
 */
@Entity
@Table(name = "customer_account_entries", indexes = {
        @Index(name = "idx_customer_account_entries_customer", columnList = "customer_id, created_at")
})
public class CustomerAccountEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Venda/factura que originou o lançamento (NULL para recebimentos sem documento). */
    @Column(name = "sale_id")
    private Long saleId;

    /** Pagamento/recebimento que originou o lançamento (NULL para vendas a crédito e anulações). */
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * CREDITO_SALE | PAYMENT | RECEIPT_WITHOUT_SALE | RECONCILIATION | CREDIT_NOTE | ANNULMENT | ADJUSTMENT
     */
    @Column(name = "entry_type", nullable = false, length = 40)
    private String entryType;

    /** Valor sinalizado: positivo aumenta a dívida, negativo diminui. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Referência curta do documento (ex.: "FT A/12", "NC B/3"). */
    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getSaleId() { return saleId; }
    public void setSaleId(Long saleId) { this.saleId = saleId; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
