package com.sgv.service;

import com.sgv.dto.ExpenseRequest;
import com.sgv.dto.ExpenseResponse;
import com.sgv.entity.Branch;
import com.sgv.entity.Expense;
import com.sgv.entity.User;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.ExpenseRepository;
import com.sgv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AuditLogService auditLogService;

    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(ExpenseResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getExpensesByBranch(Long branchId) {
        return expenseRepository.findByBranchIdOrderByCreatedAtDesc(branchId)
            .stream()
            .map(ExpenseResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public List<ExpenseResponse> getExpensesByState(String state) {
        return expenseRepository.findByStateOrderByDueDateAsc(state)
            .stream()
            .map(ExpenseResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Despesa não encontrada: " + id));
        return ExpenseResponse.fromEntity(expense);
    }

    public List<ExpenseResponse> searchExpenses(String query) {
        return expenseRepository.searchByDescriptionOrCategory(query)
            .stream()
            .map(ExpenseResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {
        User actor = userRepository.findById(userId).orElse(null);
        requirePermission(actor, "FINANCEIRO", "VIEW");
        Expense expense = new Expense();
        updateExpenseFromRequest(expense, request);

        if (userId != null) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado: " + userId));
            expense.setUser(user);
            
            if (request.getBranchId() != null) {
                Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Filial não encontrada: " + request.getBranchId()));
                expense.setBranch(branch);
            } else if (user.getBranch() != null) {
                expense.setBranch(user.getBranch());
            }
        }

        Expense saved = expenseRepository.save(expense);
        
        auditLogService.log(userId, "CREATE_EXPENSE", "expenses", saved.getId(),
            "Nova despesa: " + saved.getDescription() + " - " + saved.getAmount());

        return ExpenseResponse.fromEntity(saved);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long userId) {
        User actor = userRepository.findById(userId).orElse(null);
        requirePermission(actor, "FINANCEIRO", "VIEW");
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Despesa não encontrada: " + id));

        updateExpenseFromRequest(expense, request);
        
        // Se estava pendente e agora está paga
        if ("PENDING".equals(expense.getState()) && Boolean.TRUE.equals(request.getPaid())) {
            expense.setState("PAID");
            expense.setPaidAt(LocalDateTime.now());
        }

        Expense saved = expenseRepository.save(expense);
        
        auditLogService.log(userId, "UPDATE_EXPENSE", "expenses", saved.getId(),
            "Atualizada despesa: " + saved.getDescription());

        return ExpenseResponse.fromEntity(saved);
    }

    @Transactional
    public void markAsPaid(Long id, Long userId) {
        User actor = userRepository.findById(userId).orElse(null);
        requirePermission(actor, "FINANCEIRO", "VIEW");
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Despesa não encontrada: " + id));

        expense.setState("PAID");
        expense.setPaidAt(LocalDateTime.now());
        expenseRepository.save(expense);

        auditLogService.log(userId, "MARK_PAID_EXPENSE", "expenses", id,
            "Despesa marcada como paga: " + expense.getDescription());
    }

    @Transactional
    public void deleteExpense(Long id, Long userId) {
        User actor = userRepository.findById(userId).orElse(null);
        requirePermission(actor, "FINANCEIRO", "VIEW");
        Expense expense = expenseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Despesa não encontrada: " + id));

        expenseRepository.delete(expense);

        auditLogService.log(userId, "DELETE_EXPENSE", "expenses", id,
            "Despesa eliminada: " + expense.getDescription());
    }

    private void requirePermission(User user, String page, String action) {
        if (user == null || !user.hasPermission(page, action)) {
            throw new IllegalStateException("Não tem permissão para " + page + ":" + action + ".");
        }
    }

    private void updateExpenseFromRequest(Expense expense, ExpenseRequest request) {
        expense.setDescription(request.getDescription());
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setDueDate(request.getDueDate());
        expense.setNotes(request.getNotes());
        
        if (Boolean.TRUE.equals(request.getPaid())) {
            expense.setState("PAID");
            expense.setPaidAt(LocalDateTime.now());
        } else {
            expense.setState("PENDING");
        }
    }

    public Double getTotalPendingExpenses() {
        return expenseRepository.findByStateOrderByDueDateAsc("PENDING")
            .stream()
            .mapToDouble(Expense::getAmount)
            .sum();
    }

    public Double getTotalPaidExpensesInPeriod(LocalDateTime start, LocalDateTime end) {
        return expenseRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .filter(e -> "PAID".equals(e.getState()))
            .filter(e -> e.getPaidAt() != null)
            .filter(e -> !e.getPaidAt().isBefore(start) && !e.getPaidAt().isAfter(end))
            .mapToDouble(Expense::getAmount)
            .sum();
    }
}
