package com.sgv.desktop;

import com.sgv.entity.User;
import com.sgv.service.CashSessionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CashMovementFormController extends BaseFormController {

    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField amountField;
    @FXML private TextField descriptionField;

    private final CashSessionService cashSessionService;
    private Runnable onSuccess;

    public CashMovementFormController(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        typeCombo.setItems(FXCollections.observableArrayList("ENTRADA", "SAÍDA"));
        UiUtils.attachSafe(saveButton, this::doSave, null, "CASH_MOV_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "CASH_MOV_CANCEL");

        UiUtils.applyNumericFormatter(amountField);

        typeCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        descriptionField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        boolean valid = true;
        if (typeCombo.getValue() == null) valid = false;
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) valid = false;
        String amt = amountField.getText();
        if (amt == null || amt.trim().isEmpty()) valid = false;
        else {
            try { 
                java.math.BigDecimal val = new java.math.BigDecimal(amt.trim().replace(",", ".")); 
                if (val.compareTo(java.math.BigDecimal.ZERO) <= 0) valid = false; 
            }
            catch (Exception e) { valid = false; }
        }
        formValidProperty.set(valid);
        if (valid) hideError();
    }

    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                BigDecimal amount = new BigDecimal(amountField.getText().trim().replace(",", "."));
                String type = typeCombo.getValue().contains("ENTRADA") ? "IN" : "OUT";
                cashSessionService.registerMovement(currentUser, type, amount, descriptionField.getText().trim());
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { if (onSuccess != null) onSuccess.run(); doCancel(); });
        saveTask.setOnFailed(e -> { showError(((javafx.concurrent.Task<?>)e.getSource()).getException().getMessage()); hideSaveSpinner(); });
        new Thread(saveTask).start();
    }
}
