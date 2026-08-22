package com.sgv.desktop;

import com.sgv.entity.User;
import com.sgv.service.CashSessionService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OpenSessionFormController extends BaseFormController {

    @FXML private Label operatorLabel;
    @FXML private Label branchLabel;
    @FXML private TextField initialValueField;

    private final CashSessionService cashSessionService;
    private final com.sgv.service.SystemLogService systemLogService;
    private Runnable onSuccess;

    private static final Logger log = LoggerFactory.getLogger(OpenSessionFormController.class);

    public OpenSessionFormController(CashSessionService cashSessionService, com.sgv.service.SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
        this.cashSessionService = cashSessionService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "OPEN_SESSION_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "OPEN_SESSION_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);

        UiUtils.applyNumericFormatter(initialValueField);

        initialValueField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
        updateUserLabels();
    }

    @Override
    public void setCurrentUser(User user) {
        super.setCurrentUser(user);
        updateUserLabels();
    }

    private void updateUserLabels() {
        if (operatorLabel != null) {
            operatorLabel.setText(currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "-");
        }
        if (branchLabel != null) {
            branchLabel.setText(currentUser != null && currentUser.getBranch() != null && currentUser.getBranch().getName() != null
                    ? currentUser.getBranch().getName() : "-");
        }
    }

    @Override
    protected void validateRealTime() {
        String text = initialValueField.getText();
        boolean valid = true;
        String errorMsg = null;
        if (text != null && !text.trim().isEmpty()) {
            try {
                double val = Double.parseDouble(text.trim().replace(",", "."));
                if (val < 0) {
                    errorMsg = "Valor inicial não pode ser negativo.";
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errorMsg = "Valor inicial inválido.";
                valid = false;
            }
        }
        formValidProperty.set(valid);
        if (valid) hideError();
        else showError(errorMsg);
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
                BigDecimal initialValue = BigDecimal.ZERO;
                String text = initialValueField.getText();
                if (text != null && !text.trim().isEmpty()) initialValue = new BigDecimal(text.trim().replace(",", "."));
                log.info("Opening session for user={} initialValue={}", currentUser != null ? currentUser.getUsername() : "null", initialValue);
                cashSessionService.openSession(currentUser, initialValue);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CAIXA_ABERTO", "Turno de caixa aberto com Fundo de Maneio: " + initialValueField.getText() + " MT"); if (onSuccess != null) onSuccess.run(); doCancel(); });
        saveTask.setOnFailed(e -> { Throwable ex = saveTask.getException(); systemLogService.logError("OPEN_CASH_FAILED", "Erro ao abrir caixa: " + (ex != null ? ex.getMessage() : ""), ex);
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            showError(msg);
            hideSaveSpinner();
        });
        UiUtils.runTask(saveTask);
    }
}
