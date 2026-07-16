package com.sgv.desktop;

import com.sgv.entity.User;
import com.sgv.service.CashSessionService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CloseSessionFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private Label expectedValueLabel;
    @FXML private TextField reportedValueField;
    @FXML private TextArea notesArea;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final CashSessionService cashSessionService;
    private User currentUser;
    private Runnable onSuccess;
    private BigDecimal expectedValue;

    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public CloseSessionFormController(CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
    }

    @FXML
    public void initialize() {
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
        reportedValueField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void validateRealTime() {
        boolean valid = true;
        String text = reportedValueField.getText();
        if (text == null || text.trim().isEmpty()) {
            valid = false;
        } else {
            try {
                new BigDecimal(text.trim().replace(",", "."));
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

    public void setExpectedValue(BigDecimal value) {
        this.expectedValue = value;
        expectedValueLabel.setText(String.format("%.2f MT", value));
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
                BigDecimal reportedValue = new BigDecimal(reportedValueField.getText().trim().replace(",", "."));
                cashSessionService.closeSession(currentUser, reportedValue, notesArea.getText().trim());
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
