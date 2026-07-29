package com.sgv.desktop;

import com.sgv.entity.Supplier;
import com.sgv.repository.SupplierRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SupplierFormController extends BaseFormController {

    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private TextField contactField;
    @FXML private TextField addressField;
    @FXML private CheckBox activeCheckbox;
    @FXML private Button deleteButton;

    private final SupplierRepository supplierRepository;
    private Supplier editingSupplier;
    private volatile boolean duplicatePending = false;

    public SupplierFormController(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "SUPPLIER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "SUPPLIER_CANCEL");

        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
            UiUtils.attachSafe(deleteButton, this::doDelete, null, "SUPPLIER_DELETE");
        }

        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        activeCheckbox.setSelected(true);
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
                boolean dup = supplierRepository.findAll().stream()
                    .anyMatch(s -> s.getName() != null && s.getName().equalsIgnoreCase(finalName)
                        && (editingSupplier == null || !s.getId().equals(editingSupplier.getId())));
                javafx.application.Platform.runLater(() -> {
                    if (duplicatePending) {
                        duplicatePending = false;
                        if (dup) {
                            showError("Já existe um fornecedor com este nome.");
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

    public void setSupplier(Supplier supplier) {
        this.editingSupplier = supplier;
        if (supplier != null) {
            if (deleteButton != null) {
                deleteButton.setVisible(true);
                deleteButton.setManaged(true);
            }
            nameField.setText(supplier.getName());
            nuitField.setText(supplier.getNuit());
            contactField.setText(supplier.getContact());
            addressField.setText(supplier.getAddress());
            activeCheckbox.setSelected(supplier.getActive() != null && supplier.getActive());
        }
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void doDelete() {
        if (editingSupplier == null || editingSupplier.getId() == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja apagar este fornecedor?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.setTitle("Confirmar Eliminação");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        javafx.concurrent.Task<Void> deleteTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                supplierRepository.deleteById(editingSupplier.getId());
                return null;
            }
        };
        deleteTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            doCancel();
        });
        deleteTask.setOnFailed(e -> {
            showError("Erro ao apagar: " + deleteTask.getException().getMessage());
        });
        new Thread(deleteTask).start();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                Supplier s = editingSupplier != null ? editingSupplier : new Supplier();
                s.setName(nameField.getText().trim());
                s.setNuit(nuitField.getText() != null ? nuitField.getText().trim() : null);
                s.setContact(contactField.getText() != null ? contactField.getText().trim() : null);
                s.setAddress(addressField.getText() != null ? addressField.getText().trim() : null);
                s.setActive(activeCheckbox.isSelected());
                supplierRepository.save(s);
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
