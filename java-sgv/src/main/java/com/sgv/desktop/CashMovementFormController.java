package com.sgv.desktop;

import com.sgv.entity.User;
import com.sgv.service.CashSessionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CashMovementFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField amountField;
    @FXML private TextField descriptionField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final CashSessionService cashSessionService;
    private User currentUser;
    private Runnable onSuccess;

    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public CashMovementFormController(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
    }

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("🟢 ENTRADA", "🔴 SAÍDA"));

        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // UX: Transição de entrada fluida (Fade-in)
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // UX/MVVM: Data-Binding
        saveButton.disableProperty().bind(formValidProperty.not());

        // Listeners
        typeCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        descriptionField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void validateRealTime() {
        boolean valid = true;

        if (typeCombo.getValue() == null) valid = false;

        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) valid = false;

        String amt = amountField.getText();
        if (amt == null || amt.trim().isEmpty()) {
            valid = false;
        } else {
            try {
                double val = Double.parseDouble(amt.trim().replace(",", "."));
                if (val <= 0) valid = false;
            } catch (NumberFormatException e) {
                valid = false;
            }
        }

        formValidProperty.set(valid);
        if (valid) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    private void doSave() {
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
                BigDecimal amount = new BigDecimal(amountField.getText().trim().replace(",", "."));
                String type = typeCombo.getValue().contains("ENTRADA") ? "IN" : "OUT";
                cashSessionService.registerMovement(currentUser, type, amount, descriptionField.getText().trim());
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSuccess != null) onSuccess.run();
            doCancel();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            showError(ex.getMessage());
            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
