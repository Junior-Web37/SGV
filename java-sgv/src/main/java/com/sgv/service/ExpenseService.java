package com.sgv.service;

import com.sgv.entity.Expense;
import com.sgv.entity.User;
import com.sgv.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CashSessionService cashSessionService;
    private final SystemLogService systemLogService;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CashSessionService cashSessionService,
                          SystemLogService systemLogService) {
        this.expenseRepository = expenseRepository;
        this.cashSessionService = cashSessionService;
        this.systemLogService = systemLogService;
    }

    @Transactional
    public Expense createOrUpdateExpense(Expense expense, User currentUser, boolean markAsPaid) {
        if (expense == null) throw new IllegalArgumentException("Despesa não pode ser nula.");
        if (expense.getCreatedAt() == null) expense.setCreatedAt(LocalDateTime.now());
        if (currentUser != null) {
            expense.setUser(currentUser);
            if (expense.getBranch() == null) expense.setBranch(currentUser.getBranch());
        }
        Expense saved = expenseRepository.save(expense);

        if (markAsPaid && currentUser != null && cashSessionService != null && cashSessionService.hasOpenSession(currentUser)) {
            try {
                BigDecimal amt = saved.getAmountValue() != null ? saved.getAmountValue() : BigDecimal.ZERO;
                String desc = (saved.getDescription() != null ? saved.getDescription() : "Despesa") +
                        (saved.getCategory() != null ? " [" + saved.getCategory() + "]" : "");
                cashSessionService.registerMovement(currentUser, "OUT", amt, desc, "DESPESA", com.sgv.entity.OperationKind.OTHER, false);
            } catch (Exception ex) {
                systemLogService.logError("EXPENSE_PAYMENT_REGISTER_FAILED", "Falha ao registar movimento de caixa para despesa id=" + saved.getId(), ex);
            }
        }

        return saved;
    }

    @Transactional
    public void annulExpense(Long expenseId, User currentUser) {
        if (expenseId == null) throw new IllegalArgumentException("ID da despesa é obrigatório.");
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada: " + expenseId));
        if ("CANCELLED".equalsIgnoreCase(expense.getState())) {
            throw new IllegalStateException("Esta despesa já se encontra anulada.");
        }

        if ("PAID".equalsIgnoreCase(expense.getState()) && currentUser != null && cashSessionService != null && cashSessionService.hasOpenSession(currentUser)) {
            try {
                BigDecimal amt = expense.getAmountValue() != null ? expense.getAmountValue() : BigDecimal.ZERO;
                String desc = "Estorno de Despesa: " + (expense.getDescription() != null ? expense.getDescription() : "");
                cashSessionService.registerMovement(currentUser, "IN", amt, desc, "ESTORNO_DESPESA", com.sgv.entity.OperationKind.OTHER, false);
            } catch (Exception ex) {
                systemLogService.logError("EXPENSE_ANNUL_REVERSAL_FAILED", "Falha ao estornar movimento de caixa para despesa id=" + expenseId, ex);
            }
        }

        expense.setState("CANCELLED");
        expenseRepository.save(expense);
        systemLogService.logUserAction(
                currentUser != null ? currentUser.getUsername() : "Sistema",
                "EXPENSE_ANNULLED",
                "Despesa #" + expense.getId() + " (" + expense.getDescription() + ") anulada com sucesso.");
    }
}
