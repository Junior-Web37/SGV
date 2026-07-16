package com.sgv.desktop;

import com.sgv.entity.Branch;
import com.sgv.repository.BranchRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import com.sgv.service.SystemLogService;

@Component
public class BranchFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private TextField addressField;
    @FXML private TextField contactField;
    @FXML private CheckBox isHeadCheckbox;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final BranchRepository branchRepository;
    private final SystemLogService systemLogService;
    private Branch branch;
    private Runnable onSave;
    
    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public BranchFormController(BranchRepository branchRepository, SystemLogService systemLogService) {
        this.branchRepository = branchRepository;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setText("");
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

        // UX/MVVM: Data-Binding do botão de salvar
        saveButton.disableProperty().bind(formValidProperty.not());

        // Setup real-time listeners
        setupRealTimeValidation();
    }
    
    private void setupRealTimeValidation() {
        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        nuitField.textProperty().addListener((obs, o, n) -> validateRealTime());
        
        javafx.application.Platform.runLater(this::validateRealTime);
    }
    
    private void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;
        
        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            valid = false;
        } else if (name.trim().length() < 3) {
            errors.append("Nome muito curto (mín. 3 caracteres). ");
            valid = false;
        }
        
        String nuit = nuitField.getText();
        if (nuit == null || nuit.isBlank()) {
            valid = false;
        } else {
            String digits = nuit.replaceAll("\\D", "");
            if (digits.length() != 9) {
                errors.append("NUIT deve ter 9 dígitos. ");
                valid = false;
            }
        }
        
        formValidProperty.set(valid);
        if (!valid && errors.length() > 0) {
            showError(errors.toString().trim());
        } else {
            hideError();
        }
    }

    public void setBranch(Branch b) {
        this.branch = b;
        if (b != null && b.getId() != null) {
            nameField.setText(b.getName());
            nuitField.setText(b.getNuit());
            addressField.setText(b.getAddress());
            contactField.setText(b.getContact());
            isHeadCheckbox.setSelected(b.isHead());
        } else {
            this.branch = new Branch();
            isHeadCheckbox.setSelected(false);
        }
        validateRealTime();
    }

    public void setOnSave(Runnable callback) {
        this.onSave = callback;
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
                branch.setName(nameField.getText());
                branch.setNuit(nuitField.getText());
                branch.setAddress(addressField.getText());
                branch.setContact(contactField.getText());
                branch.setHead(isHeadCheckbox.isSelected());

                branchRepository.save(branch);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("BRANCH_SAVE_FAILED", "Erro ao salvar filial: " + ex.getMessage(), ex);
            errorLabel.setText("Erro ao salvar: " + ex.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            
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
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 0;");
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
