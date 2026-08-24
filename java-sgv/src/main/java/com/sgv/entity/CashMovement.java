package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.sgv.entity.OperationKind;

@Entity
@Table(name = "cash_movements")
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CashSession session;

    @Column(nullable = false, length = 20)
    private String type; // IN, OUT

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(length = 50)
    private String reason; // e.g. SANGRIA, REFORCO, OUTRO

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_kind", length = 20)
    private OperationKind operationKind;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CashSession getSession() { return session; }
    public void setSession(CashSession session) { this.session = session; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAmountValue() { return amount; }
    public void setAmountValue(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public OperationKind getOperationKind() { return operationKind; }
    public void setOperationKind(OperationKind operationKind) { this.operationKind = operationKind; }
}
