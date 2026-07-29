package com.sgv.desktop;

import com.sgv.entity.MetricUnit;
import com.sgv.repository.MetricUnitRepository;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class MetricUnitFormController extends BaseFormController {

    @FXML private TextField abbreviationField;
    @FXML private TextField descriptionField;

    private final MetricUnitRepository metricUnitRepository;
    private MetricUnit editingUnit;

    public MetricUnitFormController(MetricUnitRepository metricUnitRepository) {
        this.metricUnitRepository = metricUnitRepository;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "METRIC_UNIT_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "METRIC_UNIT_CANCEL");

        javafx.beans.value.ChangeListener<String> listener = (obs, o, n) -> validateRealTime();
        abbreviationField.textProperty().addListener(listener);
        descriptionField.textProperty().addListener(listener);
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        boolean valid = (abbreviationField.getText() != null && !abbreviationField.getText().isBlank())
                     && (descriptionField.getText() != null && !descriptionField.getText().isBlank());
        formValidProperty.set(valid);
        if (valid) hideError();
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
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                MetricUnit unit = editingUnit != null ? editingUnit : new MetricUnit();
                unit.setAbbreviation(abbreviationField.getText());
                unit.setDescription(descriptionField.getText());
                metricUnitRepository.save(unit);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            showError("Erro ao salvar: " + saveTask.getException().getMessage());
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
