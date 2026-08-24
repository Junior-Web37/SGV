package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_sessions")
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "initial_value", precision = 15, scale = 2)
    private BigDecimal initialValue = BigDecimal.ZERO;

    @Column(name = "reported_value", precision = 15, scale = 2)
    private BigDecimal reportedValue;

    @Column(name = "system_value", precision = 15, scale = 2)
    private BigDecimal systemValue;

    @Column(nullable = false, length = 20)
    private String state = "OPEN"; // OPEN, CLOSED

    private String notes;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public BigDecimal getInitialValue() { return initialValue; }
    public void setInitialValue(BigDecimal initialValue) { this.initialValue = initialValue; }
    public BigDecimal getInitialValueAmount() { return initialValue; }
    public void setInitialValueAmount(BigDecimal initialValue) { this.initialValue = initialValue; }

    public BigDecimal getReportedValue() { return reportedValue; }
    public void setReportedValue(BigDecimal reportedValue) { this.reportedValue = reportedValue; }
    public BigDecimal getReportedValueAmount() { return reportedValue; }
    public void setReportedValueAmount(BigDecimal reportedValue) { this.reportedValue = reportedValue; }

    public BigDecimal getSystemValue() { return systemValue; }
    public void setSystemValue(BigDecimal systemValue) { this.systemValue = systemValue; }
    public BigDecimal getSystemValueAmount() { return systemValue; }
    public void setSystemValueAmount(BigDecimal systemValue) { this.systemValue = systemValue; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
