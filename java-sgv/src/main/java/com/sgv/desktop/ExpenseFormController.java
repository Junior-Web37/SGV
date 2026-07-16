package com.sgv.desktop;

import com.sgv.entity.Expense;
import com.sgv.entity.User;
import com.sgv.repository.ExpenseRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import com.sgv.service.SystemLogService;

@Component
public class ExpenseFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField amountField;
    @FXML private TextField documentNumberField;
    @FXML private DatePicker expenseDatePicker; // UI only - not stored separately in entity
    @FXML private DatePicker dueDateDatePicker;
    @FXML private CheckBox isPaidCheckBox;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final ExpenseRepository expenseRepository;
    private final SystemLogService systemLogService;
    private Expense editingExpense;
    private User currentUser;
    private Runnable onSave;
    
    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public ExpenseFormController(ExpenseRepository expenseRepository, SystemLogService systemLogService) {
        this.expenseRepository = expenseRepository;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        categoryCombo.setItems(FXCollections.observableArrayList(
            "RENDA", "SALÁRIOS", "UTILITIES", "FORNECIMENTOS", "TRANSPORTE",
            "MARKETING", "MANUTENÇÃO", "IMPOSTOS", "OUTROS"
        ));
        categoryCombo.setValue("OUTROS");
        expenseDatePicker.setValue(java.time.LocalDate.now());

        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        saveButton.setOnAction(e -> save());
        cancelButton.setOnAction(e -> close());
        
        // UX: Transição de entrada fluida (Fade-in)
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // UX/MVVM: Data-Binding do botão de salvar
        saveButton.disableProperty().bind(formValidProperty.not());

        // Setup real-time listeners
        setupRealTimeValidation();
    }
    
    private void setupRealTimeValidation() {
        descriptionField.textProperty().addListener((obs, o, n) -> validateRealTime());
        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        categoryCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        
        javafx.application.Platform.runLater(this::validateRealTime);
    }
    
    private void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;
        
        String desc = descriptionField.getText();
        if (desc == null || desc.isBlank()) {
            valid = false;
        } else if (desc.trim().length() < 3) {
            errors.append("Descrição muito curta (mín. 3 caracteres). ");
            valid = false;
        }
        
        String amt = amountField.getText();
        if (amt == null || amt.isBlank()) {
            valid = false;
        } else {
            try {
                double amount = Double.parseDouble(amt.replace(",", ".").trim());
                if (amount <= 0) {
                    errors.append("Valor deve ser maior que 0. ");
                    valid = false;
                }
            } catch (NumberFormatException ex) {
                errors.append("Valor inválido. ");
                valid = false;
            }
        }
        
        if (categoryCombo.getValue() == null) valid = false;
        
        formValidProperty.set(valid);
        if (!valid && errors.length() > 0) {
            showError(errors.toString().trim());
        } else {
            hideError();
        }
    }

    private void save() {
        if (!formValidProperty.get()) return;

        saveButton.setVisible(false);
        saveSpinner.setVisible(true);
        saveSpinner.setManaged(true);
        cancelButton.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                String desc = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
                double amount = Double.parseDouble(amountField.getText().replace(",", ".").trim());

                Expense expense = editingExpense != null ? editingExpense : new Expense();
                expense.setDescription(desc);
                expense.setCategory(categoryCombo.getValue());
                expense.setAmount(amount);
                // documentNumberField stored in notes field if entity doesn't have specific column
                String docNum = documentNumberField.getText() != null ? documentNumberField.getText().trim() : "";
                expense.setNotes(docNum.isEmpty() ? null : "Doc: " + docNum);
                expense.setUser(currentUser);
                if (currentUser != null) expense.setBranch(currentUser.getBranch());
                if (dueDateDatePicker.getValue() != null)
                    expense.setDueDate(dueDateDatePicker.getValue());

                boolean paid = isPaidCheckBox.isSelected();
                expense.setState(paid ? "PAID" : "PENDING");
                if (paid && expense.getPaidAt() == null)
                    expense.setPaidAt(LocalDateTime.now());

                expenseRepository.save(expense);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("EXPENSE_SAVE_FAILED", "Erro ao salvar despesa: " + ex.getMessage(), ex);
            showError("Erro ao salvar despesa: " + ex.getMessage());
            
            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }

    public void setExpense(Expense expense) {
        this.editingExpense = expense;
        if (expense != null) {
            descriptionField.setText(expense.getDescription() != null ? expense.getDescription() : "");
            categoryCombo.setValue(expense.getCategory() != null ? expense.getCategory() : "OUTROS");
            amountField.setText(expense.getAmount() != null ? String.format("%.2f", expense.getAmount()) : "");
            // restore doc number from notes if present
            if (expense.getNotes() != null && expense.getNotes().startsWith("Doc: ")) {
                documentNumberField.setText(expense.getNotes().substring(5));
            }
            if (expense.getDueDate() != null) dueDateDatePicker.setValue(expense.getDueDate());
            isPaidCheckBox.setSelected("PAID".equals(expense.getState()));
        }
        validateRealTime();
    }

    public void setCurrentUser(User user) { this.currentUser = user; }
    public void setOnSave(Runnable onSave) { this.onSave = onSave; }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 0;");
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
