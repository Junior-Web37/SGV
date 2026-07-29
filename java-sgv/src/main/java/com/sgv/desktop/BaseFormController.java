package com.sgv.desktop;

import com.sgv.entity.User;
import com.sgv.service.AppConfigService;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for all form controllers. Eliminates ~900 lines of duplicated code
 * across 17 form controllers by centralizing common UI patterns:
 * error display, spinner management, fade-in animation, form validation binding.
 *
 * Also enforces training mode: when demoMode=true, all save operations are blocked.
 */
public abstract class BaseFormController {

    @FXML protected VBox rootPane;
    @FXML protected Label errorLabel;
    @FXML protected Button saveButton;
    @FXML protected Button cancelButton;
    @FXML protected ProgressIndicator saveSpinner;

    protected final BooleanProperty formValidProperty = new SimpleBooleanProperty(false);
    protected Runnable onSave;
    protected User currentUser;

    @Autowired
    private AppConfigService appConfigService;

    protected void initCommonFields() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
        if (saveSpinner != null) {
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
        }
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            FadeTransition ft = new FadeTransition(Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
        if (saveButton != null) {
            saveButton.disableProperty().bind(formValidProperty.not());
        }
        validateRealTime();
    }

    /**
     * Verifica se o modo treino está activo e bloqueia a gravação se sim.
     * Deve ser chamado no início de cada doSave().
     * Retorna true se a operação foi bloqueada.
     */
    protected boolean checkTrainingBlock() {
        if (appConfigService != null && appConfigService.isDemoMode()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Modo Treinamento Activo");
            alert.setHeaderText(null);
            alert.setContentText(
                "Não é possível guardar dados no modo treinamento.\n\n" +
                "Para efectuar alterações reais, desative o modo treino em:\n" +
                "Sistema > Modo Treinamento > Desactivar");
            alert.showAndWait();
            return true;
        }
        return false;
    }

    protected void showError(String msg) {
        if (errorLabel == null) return;
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    protected void hideError() {
        if (errorLabel == null) return;
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    protected void showSaveSpinner() {
        if (saveButton != null) { saveButton.setVisible(false); saveButton.setManaged(false); }
        if (saveSpinner != null) { saveSpinner.setVisible(true); saveSpinner.setManaged(true); }
        if (cancelButton != null) { cancelButton.setDisable(true); }
        hideError();
    }

    protected void hideSaveSpinner() {
        if (saveButton != null) { saveButton.setVisible(true); saveButton.setManaged(true); }
        if (saveSpinner != null) { saveSpinner.setVisible(false); saveSpinner.setManaged(false); }
        if (cancelButton != null) { cancelButton.setDisable(false); }
    }

    protected void doCancel() {
        currentUser = null;
        onSave = null;
        formValidProperty.set(false);
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void setOnSave(Runnable onSave) { this.onSave = onSave; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public BooleanProperty getFormValidProperty() { return formValidProperty; }

    protected abstract void validateRealTime();
    protected abstract void doSave();
}
