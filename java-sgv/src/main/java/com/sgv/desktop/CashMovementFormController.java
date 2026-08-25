package com.sgv.desktop;

import com.sgv.service.CashSessionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;

@Component
public class CashMovementFormController extends BaseFormController {

    private static final Logger log = LoggerFactory.getLogger(CashMovementFormController.class);

    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> reasonCombo;
    @FXML private Label cashSessionHintLabel;
    @FXML private TextField amountField;
    @FXML private TextField descriptionField;

    private final CashSessionService cashSessionService;
    private final com.sgv.service.SystemLogService systemLogService;
    private Runnable onSuccess;

    public CashMovementFormController(CashSessionService cashSessionService, com.sgv.service.SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
        this.cashSessionService = cashSessionService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.hardenComboBox(typeCombo);
        UiUtils.hardenComboBox(reasonCombo);
        typeCombo.setItems(FXCollections.observableArrayList("ENTRADA", "SAÍDA"));
        reasonCombo.setItems(FXCollections.observableArrayList("SANGRIA", "REFORCO", "OUTRO"));
        UiUtils.attachSafe(saveButton, this::doSave, null, "CASH_MOV_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "CASH_MOV_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);

        UiUtils.applyNumericFormatter(amountField);

        typeCombo.valueProperty().addListener((obs, o, n) -> {
            updateReasonOptions();
            validateRealTime();
        });
        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        descriptionField.textProperty().addListener((obs, o, n) -> validateRealTime());
        reasonCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(() -> {
            updateReasonOptions();
            validateRealTime();
        });
    }

    private void updateReasonOptions() {
        if (reasonCombo == null || typeCombo == null) return;
        if ("SAÍDA".equals(typeCombo.getValue())) {
            reasonCombo.setItems(FXCollections.observableArrayList("SANGRIA", "OUTRO"));
        } else if ("ENTRADA".equals(typeCombo.getValue())) {
            reasonCombo.setItems(FXCollections.observableArrayList("REFORCO", "OUTRO"));
        } else {
            reasonCombo.setItems(FXCollections.observableArrayList("SANGRIA", "REFORCO", "OUTRO"));
        }
        if (reasonCombo.getItems().size() > 0 && !reasonCombo.getItems().contains(reasonCombo.getValue())) {
            reasonCombo.setValue(reasonCombo.getItems().get(0));
        }
        if (cashSessionHintLabel != null) {
            cashSessionHintLabel.setText("Movimento vinculado ao turno aberto");
        }
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;
        if (typeCombo.getValue() == null) {
            errors.append("Tipo é obrigatório. ");
            valid = false;
        }
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) {
            errors.append("Descrição é obrigatória. ");
            valid = false;
        }
        String amt = amountField.getText();
        if (amt == null || amt.trim().isEmpty()) {
            errors.append("Valor é obrigatório. ");
            valid = false;
        } else {
            try {
                java.math.BigDecimal val = new java.math.BigDecimal(amt.trim().replace(",", "."));
                if (val.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    errors.append("Valor deve ser maior que 0. ");
                    valid = false;
                }
            } catch (Exception e) {
                errors.append("Valor inválido. ");
                valid = false;
            }
        }
        formValidProperty.set(valid);
        if (!valid) {
            showError(errors.toString().trim());
        } else {
            hideError();
        }
    }

    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (typeCombo.getValue() == null) { showError("Tipo é obrigatório."); return; }
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) { showError("Descrição é obrigatória."); return; }
        if (amountField.getText() == null || amountField.getText().trim().isEmpty()) { showError("Valor é obrigatório."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                BigDecimal amount = new BigDecimal(amountField.getText().trim().replace(",", "."));
                String type = typeCombo.getValue().contains("ENTRADA") ? "IN" : "OUT";
                String reason = reasonCombo.getValue() != null ? reasonCombo.getValue() : "";
                log.info("Attempting to register movement from UI: user={} type={} amount={} reason={}",
                        currentUser != null ? currentUser.getUsername() : "null", type, amount, reason);
                cashSessionService.registerMovement(currentUser, type, amount, descriptionField.getText().trim(), reason);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "MOVIMENTO_CAIXA_GRAVADO", "Movimento de caixa (" + typeCombo.getValue() + "): " + amountField.getText() + " MT - " + descriptionField.getText()); if (onSuccess != null) onSuccess.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("CASH_MOV_FAILED", "Erro em movimento de caixa: " + (ex != null ? ex.getMessage() : ""), ex);
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            showError(msg);
            hideSaveSpinner();
        });
        UiUtils.runTask(saveTask);
    }
}
