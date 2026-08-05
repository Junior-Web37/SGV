package com.sgv.desktop;

import com.sgv.entity.Supplier;
import com.sgv.service.SupplierService;
import com.sgv.service.SystemLogService;
import com.sgv.util.NuitValidator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;

import javafx.concurrent.Task;
import java.util.Optional;

@Component
public class SupplierFormController extends BaseFormController {

    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private TextField contactField;
    @FXML private TextField addressField;
    @FXML private CheckBox activeCheckbox;
    @FXML private Button deleteButton;

    private final SupplierService supplierService;
    private final SystemLogService systemLogService;
    private Supplier editingSupplier;
    private Task<Boolean> duplicateCheckTask;

    public SupplierFormController(SupplierService supplierService, SystemLogService systemLogService) {
        this.supplierService = supplierService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "SUPPLIER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "SUPPLIER_CANCEL");

        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
            UiUtils.attachSafe(deleteButton, this::doDelete, systemLogService, "SUPPLIER_DELETE");
        }

        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        activeCheckbox.setSelected(true);
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            errors.append("Nome é obrigatório. ");
            valid = false;
        } else if (name.trim().length() < 3) {
            errors.append("Nome muito curto (mín. 3 caracteres). ");
            valid = false;
        }

        if (nuitField.getText() != null && !nuitField.getText().isBlank()) {
            String nuit = nuitField.getText().replaceAll("\\D", "");
            if (!nuit.isEmpty()) {
                if (nuit.length() != 9) {
                    errors.append("NUIT deve ter 9 dígitos. ");
                    valid = false;
                } else if (!NuitValidator.isValid(nuit)) {
                    errors.append("NUIT inválido — dígito de controlo incorrecto. ");
                    valid = false;
                }
            }
        }

        if (contactField.getText() != null && !contactField.getText().isBlank()) {
            String contact = contactField.getText().trim();
            if (contact.length() < 7) {
                errors.append("Contacto muito curto (mín. 7 caracteres). ");
                valid = false;
            } else {
                boolean hasDigit = false;
                boolean hasLetter = false;
                for (char c : contact.toCharArray()) {
                    if (Character.isDigit(c)) hasDigit = true;
                    else if (Character.isLetter(c)) hasLetter = true;
                }
                if (hasLetter && !contact.contains("@")) {
                    errors.append("Contacto inválido — use telefone ou email. ");
                    valid = false;
                }
            }
        }

        formValidProperty.set(valid);

        if (!valid) {
            showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios.");
        } else {
            hideError();
        }

        if (valid && name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            if (duplicateCheckTask != null) duplicateCheckTask.cancel(false);
            duplicateCheckTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return supplierService.findByName(trimmedName)
                            .filter(s -> editingSupplier == null || !editingSupplier.getId().equals(s.getId()))
                            .isPresent();
                }
            };
            duplicateCheckTask.setOnSucceeded(e -> {
                if (duplicateCheckTask.getValue()) {
                    formValidProperty.set(false);
                    showError("Nome de fornecedor já existe.");
                }
            });
            new Thread(duplicateCheckTask).start();
        }
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
                supplierService.deleteById(editingSupplier.getId());
                return null;
            }
        };
        deleteTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            doCancel();
        });
        deleteTask.setOnFailed(e -> {
            Throwable ex = deleteTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("SUPPLIER_DELETE_FAILED", "Erro ao apagar fornecedor: " + msg, ex);
            showError("Erro ao apagar: " + msg);
        });
        new Thread(deleteTask).start();
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
                Supplier s = editingSupplier != null ? editingSupplier : new Supplier();
                String trimmedName = nameField.getText().trim();
                supplierService.findByName(trimmedName)
                        .filter(existing -> editingSupplier == null || !existing.getId().equals(editingSupplier.getId()))
                        .ifPresent(existing -> { throw new RuntimeException("Nome de fornecedor '" + trimmedName + "' já existe."); });
                s.setName(trimmedName);
                s.setNuit(nuitField.getText() != null ? nuitField.getText().trim() : null);
                s.setContact(contactField.getText() != null ? contactField.getText().trim() : null);
                s.setAddress(addressField.getText() != null ? addressField.getText().trim() : null);
                s.setActive(activeCheckbox.isSelected());
                supplierService.saveSupplier(s);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("SUPPLIER_SAVE_FAILED", "Erro ao salvar fornecedor: " + msg, ex);
            showError("Erro ao guardar: " + msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
