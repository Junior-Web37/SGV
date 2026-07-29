package com.sgv.desktop;

import com.sgv.entity.Customer;
import com.sgv.repository.CustomerRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Optional;
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

    private final CustomerRepository customerRepository;
    private final SystemLogService systemLogService;
    private Customer customer;
    private boolean codeAlreadyExists = false;

    public CustomerFormController(CustomerRepository customerRepository, SystemLogService systemLogService) {
        this.customerRepository = customerRepository;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        typeCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "PESSOA_FISICA", "PESSOA_JURIDICA", "EMPRESA"));
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "CUSTOMER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "CUSTOMER_CANCEL");

        UiUtils.applyNumericFormatter(creditLimitField);
        UiUtils.applyNumericFormatter(defaultDiscountField);

        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
            UiUtils.attachSafe(deleteButton, this::doDelete, systemLogService, "CUSTOMER_DELETE");
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

        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void checkDuplicateCode(String code) {
        new Thread(() -> {
            Optional<Customer> existing = customerRepository.findAll().stream()
                    .filter(c -> code.equalsIgnoreCase(c.getCode()))
                    .filter(c -> customer == null || customer.getId() == null || !c.getId().equals(customer.getId()))
                    .findFirst();
            codeAlreadyExists = existing.isPresent();
            javafx.application.Platform.runLater(this::validateRealTime);
        }).start();
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (codeField.getText() == null || codeField.getText().isBlank()) {
            valid = false;
        } else if (codeAlreadyExists) {
            errors.append("Código já existe. ");
            valid = false;
        }

        String name = nameField.getText();
        if (name == null || name.isBlank()) {
            valid = false;
        } else if (name.trim().length() < 3) {
            errors.append("Nome muito curto (mín. 3 caracteres). ");
            valid = false;
        }

        if (creditLimitField.getText() != null && !creditLimitField.getText().isBlank()) {
            try {
                java.math.BigDecimal limit = new java.math.BigDecimal(creditLimitField.getText().replace(",", "."));
                if (limit.compareTo(java.math.BigDecimal.ZERO) < 0) {
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
                java.math.BigDecimal disc = new java.math.BigDecimal(defaultDiscountField.getText().replace(",", "."));
                if (disc.compareTo(java.math.BigDecimal.ZERO) < 0 || disc.compareTo(new java.math.BigDecimal("100")) > 0) {
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
            if (!nuit.isEmpty() && nuit.length() != 9) {
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
            typeCombo.setValue(c.getType());
            creditLimitField.setText(c.getCreditLimit() != null ? String.valueOf(c.getCreditLimit()) : "");
            defaultDiscountField.setText(c.getDefaultDiscount() != null ? String.valueOf(c.getDefaultDiscount()) : "");
            addressField.setText(c.getAddress() != null ? c.getAddress() : "");
            contactField.setText(c.getContact() != null ? c.getContact() : "");
            codeField.setDisable(true);
            codeAlreadyExists = false;
        } else {
            this.customer = new Customer();
            if (c == null || c.getCode() == null || c.getCode().isBlank()) {
                codeField.setText("CLI-" + System.currentTimeMillis() % 100000);
            }
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
                customerRepository.deleteById(customer.getId());
                return null;
            }
        };
        deleteTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });
        deleteTask.setOnFailed(e -> {
            Throwable ex = deleteTask.getException();
            systemLogService.logError("CUSTOMER_DELETE_FAILED", "Erro ao apagar cliente: " + ex.getMessage(), ex);
            showError("Erro ao apagar: " + ex.getMessage());
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
            protected Void call() throws Exception {
                customer.setCode(codeField.getText().trim());
                customer.setName(nameField.getText().trim());
                customer.setNuit(nuitField.getText() != null ? nuitField.getText().trim() : "");
                customer.setType(typeCombo.getValue() != null ? typeCombo.getValue() : "PESSOA_FISICA");
                customer.setCreditLimitAmount(parseBigDecimalOrZero(creditLimitField.getText()));
                customer.setDefaultDiscountAmount(parseBigDecimalOrZero(defaultDiscountField.getText()));
                customer.setAddress(addressField.getText() != null ? addressField.getText().trim() : "");
                customer.setContact(contactField.getText() != null ? contactField.getText().trim() : "");
                if (customer.getCreatedAt() == null) {
                    customer.setCreatedAt(java.time.LocalDateTime.now());
                }

                customerRepository.save(customer);
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
            String msg = ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("CUSTOMER_SAVE_FAILED", "Erro ao salvar cliente: " + msg, ex);
            if (msg.contains("ConstraintViolationException") || msg.contains("DataIntegrityViolationException")) {
                showError("Código duplicado. Este cliente já existe.");
            } else {
                showError("Erro ao guardar: " + msg);
            }
            hideSaveSpinner();
        });

        new Thread(saveTask).start();
    }

    private java.math.BigDecimal parseBigDecimalOrZero(String text) {
        if (text == null || text.isBlank()) return java.math.BigDecimal.ZERO;
        try {
            return new java.math.BigDecimal(text.trim().replace(",", "."));
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }
}
