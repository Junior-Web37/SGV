package com.sgv.desktop;

import com.sgv.entity.MetricUnit;
import com.sgv.service.MetricUnitService;
import com.sgv.service.SystemLogService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class MetricUnitFormController extends BaseFormController {

    @FXML private TextField abbreviationField;
    @FXML private TextField descriptionField;

    private final MetricUnitService metricUnitService;
    private final SystemLogService systemLogService;
    private MetricUnit editingUnit;
    private Task<Boolean> duplicateCheckTask;

    public MetricUnitFormController(MetricUnitService metricUnitService, SystemLogService systemLogService) {
        this.metricUnitService = metricUnitService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "METRIC_UNIT_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "METRIC_UNIT_CANCEL");

        javafx.beans.value.ChangeListener<String> listener = (obs, o, n) -> validateRealTime();
        abbreviationField.textProperty().addListener(listener);
        descriptionField.textProperty().addListener(listener);
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        String abbr = abbreviationField.getText();
        if (abbr == null || abbr.isBlank()) {
            valid = false;
        } else if (abbr.trim().length() < 1) {
            errors.append("Abreviatura é obrigatória. ");
            valid = false;
        }

        if (descriptionField.getText() == null || descriptionField.getText().isBlank()) {
            errors.append("Descrição é obrigatória. ");
            valid = false;
        }

        formValidProperty.set(valid);
        if (!valid && errors.length() > 0) { showError(errors.toString().trim()); return; }
        else hideError();

        if (valid && abbr != null && !abbr.isBlank()) {
            String trimmedAbbr = abbr.trim();
            if (duplicateCheckTask != null) duplicateCheckTask.cancel(false);
            duplicateCheckTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return metricUnitService.existsByAbbreviation(trimmedAbbr, editingUnit != null ? editingUnit.getId() : null);
                }
            };
            duplicateCheckTask.setOnSucceeded(e -> {
                if (duplicateCheckTask.getValue()) {
                    formValidProperty.set(false);
                    showError("Abreviatura '" + trimmedAbbr + "' já existe.");
                }
            });
            new Thread(duplicateCheckTask).start();
        }
    }

    public void setMetricUnit(MetricUnit unit) {
        this.editingUnit = unit;
        if (unit != null) {
            abbreviationField.setText(unit.getAbbreviation());
            descriptionField.setText(unit.getDescription());
        }
        validateRealTime();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (abbreviationField.getText() == null || abbreviationField.getText().isBlank()) { showError("Abreviatura é obrigatória."); return; }
        if (descriptionField.getText() == null || descriptionField.getText().isBlank()) { showError("Descrição é obrigatória."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                MetricUnit unit = editingUnit != null ? editingUnit : new MetricUnit();
                String trimmedAbbr = abbreviationField.getText().trim();
                if (metricUnitService.existsByAbbreviation(trimmedAbbr, editingUnit != null ? editingUnit.getId() : null)) {
                    throw new IllegalArgumentException("Abreviatura '" + trimmedAbbr + "' já existe.");
                }
                unit.setAbbreviation(trimmedAbbr);
                unit.setDescription(descriptionField.getText().trim());
                metricUnitService.saveMetricUnit(unit);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "UNIDADE_GRAVADA", "Unidade de medida gravada: " + abbreviationField.getText()); if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("METRIC_UNIT_SAVE_FAILED", "Erro ao salvar unidade: " + msg, ex);
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}