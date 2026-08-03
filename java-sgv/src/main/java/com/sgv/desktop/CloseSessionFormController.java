package com.sgv.desktop;

import com.sgv.service.CashSessionService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CloseSessionFormController extends BaseFormController {

    @FXML private Label expectedValueLabel;
    @FXML private TextField reportedValueField;
    @FXML private TextArea notesArea;

    private final CashSessionService cashSessionService;
    private Runnable onSuccess;
    private BigDecimal expectedValue;

    public CloseSessionFormController(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "CLOSE_SESSION_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "CLOSE_SESSION_CANCEL");

        UiUtils.applyNumericFormatter(reportedValueField);

        reportedValueField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;
        String text = reportedValueField.getText();
        if (text == null || text.trim().isEmpty()) {
            errors.append("Valor reportado é obrigatório. ");
            valid = false;
        } else {
            try {
                new BigDecimal(text.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                errors.append("Valor reportado inválido. ");
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

    public void setExpectedValue(BigDecimal value) {
        this.expectedValue = value;
        expectedValueLabel.setText(String.format("%.2f MT", value));
    }

    public void setOnSuccess(Runnable onSuccess) { this.onSuccess = onSuccess; }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (reportedValueField.getText() == null || reportedValueField.getText().trim().isEmpty()) { showError("Valor reportado é obrigatório."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                BigDecimal reportedValue = new BigDecimal(reportedValueField.getText().trim().replace(",", "."));
                cashSessionService.closeSession(currentUser, reportedValue, notesArea.getText().trim());
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { if (onSuccess != null) onSuccess.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            showError(msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
