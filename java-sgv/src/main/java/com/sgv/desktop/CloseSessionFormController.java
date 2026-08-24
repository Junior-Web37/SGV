package com.sgv.desktop;

import com.sgv.entity.AppConfig;
import com.sgv.entity.CashMovement;
import com.sgv.entity.CashSession;
import com.sgv.repository.CashMovementRepository;
import com.sgv.service.AppConfigService;
import com.sgv.service.CashSessionService;
import com.sgv.service.ThermalPrintService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CloseSessionFormController extends BaseFormController {

    @FXML private Label expectedValueLabel;
    @FXML private Label differenceLabel;
    @FXML private TextField reportedValueField;
    @FXML private TextArea notesArea;

    private final CashSessionService cashSessionService;
    private final CashMovementRepository cashMovementRepository;
    private final ThermalPrintService thermalPrintService;
    private final AppConfigService appConfigService;
    private final com.sgv.service.SystemLogService systemLogService;
    private Runnable onSuccess;
    private BigDecimal expectedValue;

    private static final Logger log = LoggerFactory.getLogger(CloseSessionFormController.class);

    public CloseSessionFormController(CashSessionService cashSessionService,
                                      CashMovementRepository cashMovementRepository,
                                      ThermalPrintService thermalPrintService,
                                      AppConfigService appConfigService,
                                      com.sgv.service.SystemLogService systemLogService) {
        this.cashSessionService = cashSessionService;
        this.cashMovementRepository = cashMovementRepository;
        this.thermalPrintService = thermalPrintService;
        this.appConfigService = appConfigService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "CLOSE_SESSION_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "CLOSE_SESSION_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);

        UiUtils.applyNumericFormatter(reportedValueField);

        reportedValueField.textProperty().addListener((obs, o, n) -> {
            validateRealTime();
            updateDifference();
        });
        javafx.application.Platform.runLater(() -> {
            validateRealTime();
            updateDifference();
        });
    }

    private void updateDifference() {
        if (differenceLabel == null || expectedValue == null) return;
        String text = reportedValueField.getText();
        if (text == null || text.trim().isEmpty()) {
            differenceLabel.setText("0.00 MT");
            differenceLabel.setStyle("-fx-text-fill:#475569;");
            return;
        }
        try {
            BigDecimal reported = new BigDecimal(text.trim().replace(",", "."));
            BigDecimal diff = reported.subtract(expectedValue);
            String prefix = diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
            differenceLabel.setText(prefix + String.format("%.2f MT", diff));
            differenceLabel.setStyle(diff.compareTo(BigDecimal.ZERO) >= 0 ? "-fx-text-fill:#10B981;" : "-fx-text-fill:#EF4444;");
        } catch (NumberFormatException e) {
            differenceLabel.setText("0.00 MT");
            differenceLabel.setStyle("-fx-text-fill:#475569;");
        }
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

        javafx.concurrent.Task<File> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected File call() throws Exception {
                BigDecimal reportedValue = new BigDecimal(reportedValueField.getText().trim().replace(",", "."));
                log.info("Closing session for user={} reportedValue={}", currentUser != null ? currentUser.getUsername() : "null", reportedValue);
                CashSession closedSession = cashSessionService.closeSession(currentUser, reportedValue, notesArea.getText().trim());

                // Gerar Fita Z (Relatório térmico de fecho de caixa)
                try {
                    List<CashMovement> movements = cashMovementRepository.findBySessionOrderByCreatedAtDesc(closedSession);
                    AppConfig config = appConfigService.get();
                    return thermalPrintService.printCashSessionReport(closedSession, movements, config);
                } catch (Exception ex) {
                    log.warn("Falha ao gerar relatório de Fita Z: {}", ex.getMessage());
                    return null;
                }
            }
        };
        saveTask.setOnSucceeded(e -> {
            systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CAIXA_FECHADO", "Fecho de caixa concluído com emissão de Fita Z");
            File fitaZ = saveTask.getValue();
            if (fitaZ != null) {
                Platform.runLater(() -> DocumentPreviewDialog.show(fitaZ, "FITA_Z_FECHO_CAIXA"));
            }
            if (onSuccess != null) onSuccess.run();
            doCancel();
        });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("CLOSE_CASH_FAILED", "Erro ao fechar caixa: " + msg, ex);
            showError(msg);
            hideSaveSpinner();
        });
        UiUtils.runTask(saveTask);
    }
}
