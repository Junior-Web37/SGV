package com.sgv.desktop;

import com.sgv.entity.Branch;
import com.sgv.service.BranchService;
import com.sgv.service.SystemLogService;
import com.sgv.util.NuitValidator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class BranchFormController extends BaseFormController {

    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private TextField addressField;
    @FXML private TextField contactField;
    @FXML private CheckBox isHeadCheckbox;
    @FXML private TextField softwareCertField;
    @FXML private TextField licenseField;

    private final BranchService branchService;
    private final SystemLogService systemLogService;
    private Branch branch;
    private Task<Boolean> duplicateCheckTask;

    public BranchFormController(BranchService branchService, SystemLogService systemLogService) {
        this.branchService = branchService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "BRANCH_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "BRANCH_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);

        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        nuitField.textProperty().addListener((obs, o, n) -> validateRealTime());
        contactField.textProperty().addListener((obs, o, n) -> validateRealTime());
        addressField.textProperty().addListener((obs, o, n) -> validateRealTime());

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
            String digits = nuitField.getText().replaceAll("\\D", "");
            if (digits.length() != 9) {
                errors.append("NUIT deve ter 9 dígitos. ");
                valid = false;
            } else if (!NuitValidator.isValid(digits)) {
                errors.append("NUIT inválido — dígito de controlo incorrecto (Módulo 11). ");
                valid = false;
            }
        }

        formValidProperty.set(valid);
        if (!valid) {
            showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios.");
            return;
        } else {
            hideError();
        }

        if (name != null && !name.isBlank()) {
            String trimmedName = name.trim();
            if (duplicateCheckTask != null) duplicateCheckTask.cancel(false);
            duplicateCheckTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return branchService.findByName(trimmedName)
                            .filter(b -> branch == null || branch.getId() == null || !branch.getId().equals(b.getId()))
                            .isPresent();
                }
            };
            duplicateCheckTask.setOnSucceeded(e -> {
                if (Boolean.TRUE.equals(duplicateCheckTask.getValue())) {
                    formValidProperty.set(false);
                    showError("Nome de filial '" + trimmedName + "' já existe.");
                }
            });
            Thread dupThread = new Thread(duplicateCheckTask);
            dupThread.setDaemon(true);
            dupThread.start();
        }
    }

    public void setBranch(Branch b) {
        this.branch = (b != null && b.getId() != null) ? b : new Branch();
        if (b != null && b.getId() != null) {
            nameField.setText(b.getName());
            nuitField.setText(b.getNuit());
            addressField.setText(b.getAddress());
            contactField.setText(b.getContact());
            isHeadCheckbox.setSelected(b.isHead());
            softwareCertField.setText(b.getSoftwareCertNumber() != null ? b.getSoftwareCertNumber() : "");
            licenseField.setText(b.getLicenseNumber() != null ? b.getLicenseNumber() : "");
        } else {
            isHeadCheckbox.setSelected(false);
        }
        validateRealTime();
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
                String trimmedName = nameField.getText().trim();
                branchService.findByName(trimmedName)
                        .filter(b -> branch == null || branch.getId() == null || !branch.getId().equals(b.getId()))
                        .ifPresent(b -> { throw new RuntimeException("Nome de filial '" + trimmedName + "' já existe."); });
                branch.setName(trimmedName);
                branch.setNuit(nuitField.getText() != null ? nuitField.getText().trim() : null);
                branch.setAddress(addressField.getText() != null ? addressField.getText().trim() : null);
                branch.setContact(contactField.getText() != null ? contactField.getText().trim() : null);
                branch.setHead(isHeadCheckbox.isSelected());
                branch.setSoftwareCertNumber(softwareCertField.getText() != null ? softwareCertField.getText().trim() : "");
                branch.setLicenseNumber(licenseField.getText() != null ? licenseField.getText().trim() : "");
                branchService.saveBranch(branch);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> {
            systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "FILIAL_GRAVADA", "Filial gravada com sucesso: " + nameField.getText());
            if (onSave != null) onSave.run();
            doCancel();
        });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("BRANCH_SAVE_FAILED", "Erro ao salvar filial: " + msg, ex);
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        Thread saveThread = new Thread(saveTask);
        saveThread.setDaemon(true);
        saveThread.start();
    }
}
