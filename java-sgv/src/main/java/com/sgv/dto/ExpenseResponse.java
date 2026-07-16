package com.sgv.dto;

import com.sgv.entity.Expense;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExpenseResponse {
    
    private Long id;
    private String description;
    private String category;
    private Double amount;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private String state;
    private String notes;
    private Long branchId;
    private String branchName;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
    
    public static ExpenseResponse fromEntity(Expense expense) {
        ExpenseResponse dto = new ExpenseResponse();
        dto.setId(expense.getId());
        dto.setDescription(expense.getDescription());
        dto.setCategory(expense.getCategory());
        dto.setAmount(expense.getAmount());
        dto.setDueDate(expense.getDueDate());
        dto.setPaidAt(expense.getPaidAt());
        dto.setState(expense.getState());
        dto.setNotes(expense.getNotes());
        dto.setCreatedAt(expense.getCreatedAt());
        
        if (expense.getBranch() != null) {
            dto.setBranchId(expense.getBranch().getId());
            dto.setBranchName(expense.getBranch().getName());
        }
        
        if (expense.getUser() != null) {
            dto.setUserId(expense.getUser().getId());
            dto.setUserName(expense.getUser().getFullName());
        }
        
        return dto;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
