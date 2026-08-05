package com.sgv.service;

import com.sgv.entity.Expense;
import com.sgv.entity.User;
import com.sgv.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (expense == null) throw new IllegalArgumentException("Expense is null");
        if (expense.getCreatedAt() == null) expense.setCreatedAt(LocalDateTime.now());
        if (currentUser != null) {
            expense.setUser(currentUser);
            if (expense.getBranch() == null) expense.setBranch(currentUser.getBranch());
        }
        Expense saved = expenseRepository.save(expense);

        if (markAsPaid) {
            try {
                java.math.BigDecimal amt = saved.getAmountValue() != null ? saved.getAmountValue() : java.math.BigDecimal.ZERO;
                String desc = saved.getDescription() != null ? saved.getDescription() : "Despesa";
                cashSessionService.registerMovement(currentUser, "OUT", amt, desc, "DESPESA", com.sgv.entity.OperationKind.OTHER, true);
            } catch (Exception ex) {
                systemLogService.logError("EXPENSE_PAYMENT_REGISTER_FAILED", "Falha ao registar movimento de caixa para despesa id=" + saved.getId(), ex);
                throw ex;
            }
        }

        return saved;
    }
}
