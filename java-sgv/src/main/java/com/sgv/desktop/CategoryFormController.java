package com.sgv.desktop;

import com.sgv.entity.Category;
import com.sgv.repository.CategoryRepository;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class CategoryFormController extends BaseFormController {

    @FXML private TextField nameField;

    private final CategoryRepository categoryRepository;
    private Category editingCategory;
    private volatile boolean duplicatePending = false;

    public CategoryFormController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "CATEGORY_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "CATEGORY_CANCEL");
        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        String name = nameField.getText();
        boolean valid = true;

        if (name == null || name.isBlank()) {
            valid = false;
        } else if (name.trim().length() < 3) {
            showError("Nome muito curto (mín. 3 caracteres).");
            valid = false;
        }

        if (valid) {
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
            return;
        }

        formValidProperty.set(false);
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
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                Category cat = editingCategory != null ? editingCategory : new Category();
                cat.setName(nameField.getText().trim());
                categoryRepository.save(cat);
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
