package com.sgv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class ExpenseRequest {
    
    @NotBlank(message = "Descrição é obrigatória")
    private String description;
    
    @NotBlank(message = "Categoria é obrigatória")
    private String category; // RENT, SALARY, UTILITIES, SUPPLIES, TRANSPORT, MARKETING, MAINTENANCE, TAXES, OTHER
    
    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private Double amount;
    
    private LocalDate dueDate;
    private Boolean paid = false;
    private String notes;
    private Long branchId;
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
}
