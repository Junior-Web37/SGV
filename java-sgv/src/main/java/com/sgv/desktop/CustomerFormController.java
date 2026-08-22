package com.sgv.desktop;

import com.sgv.entity.Customer;
import com.sgv.repository.CustomerRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.math.BigDecimal;
import com.sgv.service.SystemLogService;

@Component
public class CustomerFormController extends BaseFormController {

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField creditLimitField;
    @FXML private TextField defaultDiscountField;
    @FXML private TextField addressField;
    @FXML private TextField contactField;
    @FXML private Button deleteButton;

    private final com.sgv.service.CustomerService customerService;
    private final SystemLogService systemLogService;
    private Customer customer;
    private boolean codeAlreadyExists = false;

    public CustomerFormController(com.sgv.service.CustomerService customerService, SystemLogService systemLogService) {
        this.customerService = customerService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        typeCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "RETALHO", "GROSSO", "EMPRESA", "ISENTO"));
        typeCombo.setValue("RETALHO");
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "CUSTOMER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "CUSTOMER_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);

        UiUtils.applyNumericFormatter(creditLimitField);
        UiUtils.applyNumericFormatter(defaultDiscountField);

        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
            UiUtils.attachSafe(deleteButton, this::doDelete, systemLogService, "CUSTOMER_DELETE");
            UiUtils.applyHoverElevation(deleteButton);
            UiUtils.applyPressFeedback(deleteButton);
        }

        setupRealTimeValidation();
    }

    private void setupRealTimeValidation() {
        codeField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) {
                checkDuplicateCode(n);
            } else {
                codeAlreadyExists = false;
                validateRealTime();
            }
        });

        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        creditLimitField.textProperty().addListener((obs, o, n) -> validateRealTime());
        defaultDiscountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        nuitField.textProperty().addListener((obs, o, n) -> validateRealTime());
        contactField.textProperty().addListener((obs, o, n) -> validateRealTime());
        typeCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());

        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void checkDuplicateCode(String code) {
        UiUtils.runAsync(() -> {
            try {
                String finalCode = code;
                Long excludeId = customer != null ? customer.getId() : null;
                codeAlreadyExists = customerService.existsByCode(finalCode, excludeId);
            } catch (Exception e) {
                codeAlreadyExists = false;
                systemLogService.logError("CUSTOMER_DUPL_CHECK", "Erro ao verificar código", e);
            }
            javafx.application.Platform.runLater(this::validateRealTime);
        });
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (codeField.getText() == null || codeField.getText().isBlank()) {
            errors.append("Código é obrigatório. ");
            valid = false;
        } else if (codeAlreadyExists) {
            errors.append("Código já existe. ");
            valid = false;
        }

        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            errors.append("Nome é obrigatório. ");
            valid = false;
        } else if (name.trim().length() < 3) {
            errors.append("Nome muito curto (mín. 3 caracteres). ");
            valid = false;
        }

        if (creditLimitField.getText() != null && !creditLimitField.getText().isBlank()) {
            try {
                BigDecimal limit = new BigDecimal(creditLimitField.getText().replace(",", "."));
                if (limit.compareTo(BigDecimal.ZERO) < 0) {
                    errors.append("Limite de crédito não pode ser negativo. ");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errors.append("Limite de crédito inválido. ");
                valid = false;
            }
        }

        if (defaultDiscountField.getText() != null && !defaultDiscountField.getText().isBlank()) {
            try {
                BigDecimal disc = new BigDecimal(defaultDiscountField.getText().replace(",", "."));
                if (disc.compareTo(BigDecimal.ZERO) < 0 || disc.compareTo(new BigDecimal("100")) > 0) {
                    errors.append("Desconto deve estar entre 0 e 100%. ");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errors.append("Desconto inválido. ");
                valid = false;
            }
        }

        if (nuitField.getText() != null && !nuitField.getText().isBlank()) {
            String nuit = nuitField.getText().replaceAll("\\D", "");
            if (!nuit.isEmpty()) {
                if (nuit.length() != 9) {
                    errors.append("NUIT deve ter 9 dígitos. ");
                    valid = false;
                } else if (!com.sgv.util.NuitValidator.isValid(nuit)) {
                    errors.append("NUIT inválido — dígito de controlo incorrecto. ");
                    valid = false;
                }
            }
        }

        if (contactField.getText() != null && !contactField.getText().isBlank()) {
            String contact = contactField.getText().trim();
            boolean hasDigit = false;
            boolean hasLetter = false;
            for (char c : contact.toCharArray()) {
                if (Character.isDigit(c)) hasDigit = true;
                else if (Character.isLetter(c)) hasLetter = true;
            }
            if (contact.length() < 7) {
                errors.append("Contacto muito curto (mín. 7 caracteres). ");
                valid = false;
            } else if (hasLetter && !contact.contains("@")) {
                errors.append("Contacto inválido — use telefone ou email. ");
                valid = false;
            }
        }

        formValidProperty.set(valid);

        if (!valid) {
            showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios.");
        } else {
            hideError();
        }
    }

    public void setCustomer(Customer c) {
        this.customer = c;
        if (c != null && c.getId() != null) {
            if (deleteButton != null) {
                deleteButton.setVisible(true);
                deleteButton.setManaged(true);
            }
            codeField.setText(c.getCode());
            nameField.setText(c.getName());
            nuitField.setText(c.getNuit() != null ? c.getNuit() : "");
            
            String t = c.getType();
            if (t == null) t = "RETALHO";
            else if ("PESSOA_FISICA".equalsIgnoreCase(t) || "VAREJO".equalsIgnoreCase(t)) t = "RETALHO";
            else if ("PESSOA_JURIDICA".equalsIgnoreCase(t) || "B2B".equalsIgnoreCase(t)) t = "EMPRESA";
            else if ("ATACADO".equalsIgnoreCase(t)) t = "GROSSO";
            typeCombo.setValue(t);

            creditLimitField.setText(c.getCreditLimitAmount() != null
                    ? c.getCreditLimitAmount().stripTrailingZeros().toPlainString() : "");
            defaultDiscountField.setText(c.getDefaultDiscountAmount() != null
                    ? c.getDefaultDiscountAmount().stripTrailingZeros().toPlainString() : "");
            addressField.setText(c.getAddress() != null ? c.getAddress() : "");
            contactField.setText(c.getContact() != null ? c.getContact() : "");
            codeField.setDisable(true);
            codeAlreadyExists = false;
        } else {
            this.customer = new Customer();
            codeField.setText(customerService.generateNewCode());
            codeField.setDisable(true);
            typeCombo.setValue("RETALHO");
        }
        validateRealTime();
    }

    private void doDelete() {
        if (customer == null || customer.getId() == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja apagar este cliente?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.setTitle("Confirmar Eliminação");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        javafx.concurrent.Task<Void> deleteTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                customerService.deleteById(customer.getId());
                return null;
            }
        };
        deleteTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CLIENTE_ELIMINADO", "Cliente eliminado: " + customer.getName());
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });
        deleteTask.setOnFailed(e -> {
            Throwable ex = deleteTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("CUSTOMER_DELETE_FAILED", "Erro ao apagar cliente: " + msg, ex);
            showError("Erro ao apagar: " + msg);
        });
        UiUtils.runTask(deleteTask);
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;

        String code = codeField.getText();
        if (code == null || code.isBlank()) { showError("Código é obrigatório."); return; }
        if (nameField.getText() == null || nameField.getText().isBlank()) { showError("Nome é obrigatório."); return; }

        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                String trimmedCode = code.trim();
                if (customerService.existsByCode(trimmedCode, customer != null ? customer.getId() : null)) {
                    throw new RuntimeException("Código '" + trimmedCode + "' já existe.");
                }

                customer.setCode(trimmedCode);
                customer.setName(nameField.getText().trim());
                customer.setNuit(nuitField.getText() != null ? nuitField.getText().trim() : "");
                customer.setType(typeCombo.getValue() != null ? typeCombo.getValue() : "RETALHO");
                customer.setCreditLimitAmount(parseBigDecimalOrZero(creditLimitField.getText()));
                customer.setDefaultDiscountAmount(parseBigDecimalOrZero(defaultDiscountField.getText()));
                customer.setAddress(addressField.getText() != null ? addressField.getText().trim() : "");
                customer.setContact(contactField.getText() != null ? contactField.getText().trim() : "");

                customerService.saveCustomer(customer);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CLIENTE_GRAVADO", "Cliente gravado: " + nameField.getText());
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("CUSTOMER_SAVE_FAILED", "Erro ao salvar cliente: " + msg, ex);
            if (msg.contains("ConstraintViolationException") || msg.contains("DataIntegrityViolationException")) {
                showError("Código duplicado. Este cliente já existe.");
            } else {
                showError("Erro ao guardar: " + msg);
            }
            hideSaveSpinner();
        });

        UiUtils.runTask(saveTask);
    }

    private BigDecimal parseBigDecimalOrZero(String text) {
        if (text == null || text.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.trim().replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}