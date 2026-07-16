package com.sgv.desktop;

import com.sgv.entity.Category;
import com.sgv.repository.CategoryRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class CategoryFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private TextField nameField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final CategoryRepository categoryRepository;
    private Runnable onSave;
    private Category editingCategory;
    private String originalName;

    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);
    private volatile boolean duplicatePending = false;

    public CategoryFormController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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

        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void validateRealTime() {
        String name = nameField.getText();
        boolean valid = true;
        StringBuilder errors = new StringBuilder();

        if (name == null || name.isBlank()) {
            valid = false;
        } else if (name.trim().length() < 3) {
            errors.append("Nome muito curto (mín. 3 caracteres). ");
            valid = false;
        }

        if (valid) {
            // Check for duplicate asynchronously
            duplicatePending = true;
            String finalName = name.trim();
            new Thread(() -> {
                boolean dup = categoryRepository.findAll().stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(finalName)
                        && (editingCategory == null || !c.getId().equals(editingCategory.getId())));
                javafx.application.Platform.runLater(() -> {
                    if (duplicatePending) {
                        duplicatePending = false;
                        if (dup) {
                            showError("Já existe uma categoria com este nome.");
                            formValidProperty.set(false);
                        } else {
                            hideError();
                            formValidProperty.set(true);
                        }
                    }
                });
            }).start();
            return; // Keep disabled until duplicate check resolves
        }

        formValidProperty.set(false);
        if (errors.length() > 0) hideError();
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

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void setCategory(Category category) {
        this.editingCategory = category;
        if (category != null) {
            this.originalName = category.getName();
            nameField.setText(category.getName());
        }
        javafx.application.Platform.runLater(this::validateRealTime);
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
                Category cat = editingCategory != null ? editingCategory : new Category();
                cat.setName(nameField.getText().trim());
                categoryRepository.save(cat);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            closeStage();
        });

        saveTask.setOnFailed(e -> {
            showError("Erro ao salvar: " + saveTask.getException().getMessage());
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
