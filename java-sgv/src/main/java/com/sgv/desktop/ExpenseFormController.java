package com.sgv.desktop;

import com.sgv.entity.Expense;
import com.sgv.repository.ExpenseRepository;
import com.sgv.service.SystemLogService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class ExpenseFormController extends BaseFormController {

    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField amountField;
    @FXML private TextField documentNumberField;
    @FXML private DatePicker expenseDatePicker;
    @FXML private DatePicker dueDateDatePicker;
    @FXML private CheckBox isPaidCheckBox;

    private final com.sgv.service.ExpenseService expenseService;
    private final SystemLogService systemLogService;
    private Expense editingExpense;

    public ExpenseFormController(com.sgv.service.ExpenseService expenseService, SystemLogService systemLogService) {
        this.expenseService = expenseService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        categoryCombo.setItems(FXCollections.observableArrayList(
            "RENDA", "SALÁRIOS", "UTILITIES", "FORNECIMENTOS", "TRANSPORTE",
            "MARKETING", "MANUTENÇÃO", "IMPOSTOS", "OUTROS"
        ));
        categoryCombo.setValue("OUTROS");
        expenseDatePicker.setValue(java.time.LocalDate.now());

        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "EXPENSE_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "EXPENSE_CANCEL");
        
        UiUtils.applyNumericFormatter(amountField);

        descriptionField.textProperty().addListener((obs, o, n) -> validateRealTime());
        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        categoryCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        String desc = descriptionField.getText();
        if (desc == null || desc.isBlank()) { errors.append("Descrição é obrigatória. "); valid = false; }
        else if (desc.trim().length() < 3) { errors.append("Descrição muito curta (mín. 3 caracteres). "); valid = false; }

        String amt = amountField.getText();
        if (amt == null || amt.isBlank()) { errors.append("Valor é obrigatório. "); valid = false; }
        else {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(amt.replace(",", ".").trim());
                if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) { errors.append("Valor deve ser maior que 0. "); valid = false; }
            } catch (Exception ex) { errors.append("Valor inválido. "); valid = false; }
        }
        if (categoryCombo.getValue() == null) { errors.append("Categoria é obrigatória. "); valid = false; }

        formValidProperty.set(valid);
        if (!valid) showError(errors.toString().trim());
        else hideError();
    }

    public void setExpense(Expense expense) {
        this.editingExpense = expense;
        if (expense != null) {
            descriptionField.setText(expense.getDescription() != null ? expense.getDescription() : "");
            categoryCombo.setValue(expense.getCategory() != null ? expense.getCategory() : "OUTROS");
            amountField.setText(expense.getAmount() != null ? String.format("%.2f", expense.getAmount()) : "");
            if (expense.getNotes() != null && expense.getNotes().startsWith("Doc: "))
                documentNumberField.setText(expense.getNotes().substring(5));
            if (expense.getDueDate() != null) dueDateDatePicker.setValue(expense.getDueDate());
            isPaidCheckBox.setSelected("PAID".equals(expense.getState()));
        }
        validateRealTime();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (descriptionField.getText() == null || descriptionField.getText().isBlank()) { showError("Descrição é obrigatória."); return; }
        if (amountField.getText() == null || amountField.getText().isBlank()) { showError("Valor é obrigatório."); return; }
        if (categoryCombo.getValue() == null) { showError("Categoria é obrigatória."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                java.math.BigDecimal amount = new java.math.BigDecimal(amountField.getText().replace(",", ".").trim());
                Expense expense = editingExpense != null ? editingExpense : new Expense();
                expense.setDescription(descriptionField.getText().trim());
                expense.setCategory(categoryCombo.getValue());
                expense.setAmount(amount.doubleValue());
                String docNum = documentNumberField.getText() != null ? documentNumberField.getText().trim() : "";
                expense.setNotes(docNum.isEmpty() ? null : "Doc: " + docNum);
                if (dueDateDatePicker.getValue() != null) expense.setDueDate(dueDateDatePicker.getValue());
                if (expenseDatePicker != null && expenseDatePicker.getValue() != null) {
                    expense.setCreatedAt(expenseDatePicker.getValue().atStartOfDay());
                } else if (expense.getId() == null) {
                    expense.setCreatedAt(LocalDateTime.now());
                }
                boolean paid = isPaidCheckBox.isSelected();
                expense.setState(paid ? "PAID" : "PENDING");
                if (paid && expense.getPaidAt() == null) expense.setPaidAt(LocalDateTime.now());

                expenseService.createOrUpdateExpense(expense, currentUser, paid);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "DESPESA_GRAVADA", "Despesa gravada: " + descriptionField.getText() + " (" + amountField.getText() + " MT)"); if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("EXPENSE_SAVE_FAILED", "Erro ao salvar despesa: " + msg, ex);
            showError("Erro ao salvar despesa: " + msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
