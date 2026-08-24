package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description;
    private String category; // RENT, SALARY, UTILITIES, SUPPLIES, OTHER
    @Column(precision = 19, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private String state = "PENDING"; // PENDING, PAID, OVERDUE, CANCELLED
    private String notes;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getAmount() { return amount != null ? amount.doubleValue() : 0.0; }
    public void setAmount(Double amount) { this.amount = amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO; }
    public BigDecimal getAmountValue() { return amount; }
    public void setAmountValue(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
