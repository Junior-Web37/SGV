package com.sgv.desktop;

import com.sgv.entity.Category;
import com.sgv.service.CategoryService;
import com.sgv.service.SystemLogService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class CategoryFormController extends BaseFormController {

    @FXML private TextField nameField;

    private final CategoryService categoryService;
    private final SystemLogService systemLogService;
    private Category editingCategory;

    public CategoryFormController(CategoryService categoryService, SystemLogService systemLogService) {
        this.categoryService = categoryService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "CATEGORY_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "CATEGORY_CANCEL");
        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);
        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        String name = nameField.getText();
        boolean valid = true;
        String errorMsg = null;

        if (name == null || name.isBlank()) {
            errorMsg = "Nome é obrigatório.";
            valid = false;
        } else if (name.trim().length() < 3) {
            errorMsg = "Nome muito curto (mín. 3 caracteres).";
            valid = false;
        }

        formValidProperty.set(valid);

        if (!valid) {
            showError(errorMsg);
        } else {
            hideError();
        }
    }

    public void setCategory(Category category) {
        this.editingCategory = category;
        if (category != null) nameField.setText(category.getName());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (nameField.getText() == null || nameField.getText().isBlank()) { showError("Nome é obrigatório."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                Category cat = editingCategory != null ? editingCategory : new Category();
                String trimmedName = nameField.getText().trim();
                categoryService.findByName(trimmedName)
                        .filter(existing -> editingCategory == null || !existing.getId().equals(editingCategory.getId()))
                        .ifPresent(existing -> { throw new RuntimeException("Já existe uma categoria com este nome."); });
                cat.setName(trimmedName);
                categoryService.saveCategory(cat);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CATEGORIA_GRAVADA", "Categoria gravada com sucesso: " + nameField.getText()); if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("CATEGORY_SAVE_FAILED", "Erro ao salvar categoria: " + msg, ex);
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        UiUtils.runTask(saveTask);
    }
}