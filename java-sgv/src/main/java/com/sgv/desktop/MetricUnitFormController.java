package com.sgv.desktop;

import com.sgv.entity.MetricUnit;
import com.sgv.repository.MetricUnitRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class MetricUnitFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private TextField abbreviationField;
    @FXML private TextField descriptionField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final MetricUnitRepository metricUnitRepository;
    private Runnable onSave;
    private MetricUnit editingUnit;

    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public MetricUnitFormController(MetricUnitRepository metricUnitRepository) {
        this.metricUnitRepository = metricUnitRepository;
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        saveButton.disableProperty().bind(formValidProperty.not());

        javafx.beans.value.ChangeListener<String> listener = (obs, o, n) -> validateRealTime();
        abbreviationField.textProperty().addListener(listener);
        descriptionField.textProperty().addListener(listener);
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void validateRealTime() {
        boolean valid = (abbreviationField.getText() != null && !abbreviationField.getText().isBlank())
                     && (descriptionField.getText() != null && !descriptionField.getText().isBlank());
        formValidProperty.set(valid);
        if (valid) { errorLabel.setVisible(false); errorLabel.setManaged(false); }
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void setMetricUnit(MetricUnit unit) {
        this.editingUnit = unit;
        if (unit != null) {
            abbreviationField.setText(unit.getAbbreviation());
            descriptionField.setText(unit.getDescription());
        }
        validateRealTime();
    }

    private void doSave() {
        if (!formValidProperty.get()) return;

        saveButton.setVisible(false);
        saveSpinner.setVisible(true);
        saveSpinner.setManaged(true);
        cancelButton.setDisable(true);

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                MetricUnit unit = editingUnit != null ? editingUnit : new MetricUnit();
                unit.setAbbreviation(abbreviationField.getText());
                unit.setDescription(descriptionField.getText());
                metricUnitRepository.save(unit);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            closeStage();
        });

        saveTask.setOnFailed(e -> {
            errorLabel.setText("Erro ao salvar: " + saveTask.getException().getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }

    private void doCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
