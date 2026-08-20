package com.sgv.desktop;

import com.sgv.entity.Warehouse;
import com.sgv.service.SystemLogService;
import com.sgv.service.WarehouseService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WarehouseFormController extends BaseFormController {

    private static final Logger log = LoggerFactory.getLogger(WarehouseFormController.class);

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private TextField addressField;
    @FXML private TextField contactField;
    @FXML private TextArea  notesArea;
    @FXML private CheckBox   activeCheck;

    private final WarehouseService warehouseService;
    private final SystemLogService systemLogService;
    private Warehouse editing;
    private Runnable onSaved;
    private Task<Boolean> duplicateCheckTask;

    public WarehouseFormController(WarehouseService warehouseService, SystemLogService systemLogService) {
        this.warehouseService = warehouseService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "WAREHOUSE_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "WAREHOUSE_CANCEL");
        activeCheck.setSelected(true);
        codeField.textProperty().addListener((obs, o, n) -> validateRealTime());
        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();

        if (code.isBlank()) { errors.append("Código é obrigatório. "); valid = false; }
        if (name.isBlank()) { errors.append("Nome é obrigatório. "); valid = false; }
        else if (name.length() < 3) { errors.append("Nome muito curto (mín. 3 caracteres). "); valid = false; }

        formValidProperty.set(valid);
        if (!valid) { showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios."); return; }
        else hideError();

        if (!code.isBlank() && (editing == null || editing.getId() == null)) {
            if (duplicateCheckTask != null) duplicateCheckTask.cancel(false);
            duplicateCheckTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return warehouseService.findByCode(code).isPresent();
                }
            };
            duplicateCheckTask.setOnSucceeded(e -> {
                if (duplicateCheckTask.getValue()) {
                    formValidProperty.set(false);
                    showError("Código '" + code + "' já existe noutro armazém.");
                }
            });
            new Thread(duplicateCheckTask).start();
        }
    }

    public void setWarehouse(Warehouse w) {
        this.editing = w;
        if (w != null && w.getId() != null) {
            codeField.setText(w.getCode());
            nameField.setText(w.getName());
            nuitField.setText(w.getNuit());
            addressField.setText(w.getAddress());
            contactField.setText(w.getContact());
            notesArea.setText(w.getNotes());
            activeCheck.setSelected(w.isActive());
            codeField.setDisable(true);
        } else {
            this.editing = null;
            codeField.setDisable(false);
            codeField.clear();
            nameField.clear();
            nuitField.clear();
            addressField.clear();
            contactField.clear();
            notesArea.clear();
            activeCheck.setSelected(true);
        }
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (codeField.getText() == null || codeField.getText().isBlank()) { showError("Código é obrigatório."); return; }
        if (nameField.getText() == null || nameField.getText().isBlank()) { showError("Nome é obrigatório."); return; }

        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                String code = codeField.getText().trim();
                Warehouse w = (editing != null) ? editing : new Warehouse();
                if (editing == null && warehouseService.findByCode(code).isPresent()) {
                    throw new IllegalArgumentException("Código '" + code + "' já existe noutro armazém.");
                }
                w.setCode(code);
                w.setName(nameField.getText().trim());
                w.setNuit(blankToNull(nuitField.getText()));
                w.setAddress(blankToNull(addressField.getText()));
                w.setContact(blankToNull(contactField.getText()));
                w.setNotes(blankToNull(notesArea.getText()));
                w.setActive(activeCheck.isSelected());
                warehouseService.save(w);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "ARMAZEM_GRAVADO", "Armazém gravado com sucesso: " + nameField.getText());
            if (onSaved != null) onSaved.run();
            doCancel();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("WAREHOUSE_SAVE_FAILED", "Erro ao salvar armazém: " + msg, ex);
            showError("Erro: " + msg);
            hideSaveSpinner();
        });

        new Thread(saveTask).start();
    }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
