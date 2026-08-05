package com.sgv.desktop;

import com.sgv.entity.Purchase;
import com.sgv.entity.Supplier;
import com.sgv.entity.SupplierPayment;
import com.sgv.repository.PurchaseRepository;
import com.sgv.service.SupplierPaymentService;
import com.sgv.service.SupplierService;
import com.sgv.service.SystemLogService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SupplierPaymentFormController extends BaseFormController {

    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private ComboBox<Purchase> purchaseCombo;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> methodCombo;
    @FXML private DatePicker paymentDatePicker;
    @FXML private TextField referenceField;

    private final SupplierService supplierService;
    private final PurchaseRepository purchaseRepository;
    private final SupplierPaymentService supplierPaymentService;
    private final SystemLogService systemLogService;
    private SupplierPayment payment;

    public SupplierPaymentFormController(SupplierService supplierService,
                                         PurchaseRepository purchaseRepository,
                                         SupplierPaymentService supplierPaymentService,
                                         SystemLogService systemLogService) {
        this.supplierService = supplierService;
        this.purchaseRepository = purchaseRepository;
        this.supplierPaymentService = supplierPaymentService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();

        amountField.textProperty().addListener((obs, oldVal, newVal) -> validateRealTime());
        supplierCombo.valueProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null) refreshPurchaseCombo(newVal); validateRealTime(); });
        purchaseCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateRealTime());
        methodCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateRealTime());

        supplierCombo.setItems(FXCollections.observableArrayList(supplierService.findAll()));
        supplierCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Supplier s) { return s != null ? s.getName() : ""; }
            @Override public Supplier fromString(String string) { return supplierCombo.getItems().stream().filter(s -> s.getName() != null && s.getName().equals(string)).findFirst().orElse(null); }
        });
        purchaseCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Purchase p) { return p != null ? (p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "Compra #" + p.getId()) : ""; }
            @Override public Purchase fromString(String s) { return purchaseCombo.getItems().stream().filter(p -> toString(p).equals(s)).findFirst().orElse(null); }
        });

        methodCombo.getItems().addAll("Dinheiro", "Transferência Bancária", "Cheque", "POS", "Outro");
        methodCombo.setValue("Dinheiro");
        paymentDatePicker.setValue(LocalDate.now());
        amountField.setText("0.00");

        UiUtils.applyNumericFormatter(amountField);
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "SUPPLIER_PAYMENT_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "SUPPLIER_PAYMENT_CANCEL");

        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void refreshPurchaseCombo(Supplier supplier) {
        if (supplier == null) {
            purchaseCombo.setItems(FXCollections.observableArrayList());
            return;
        }
        List<Purchase> purchases = purchaseRepository.findBySupplierIdOrderByCreatedAtDesc(supplier.getId());
        purchaseCombo.setItems(FXCollections.observableArrayList(purchases));
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (supplierCombo.getValue() == null) {
            errors.append("Selecione um fornecedor. ");
            valid = false;
        }

        String amountText = amountField.getText();
        if (amountText == null || amountText.isBlank()) {
            errors.append("Valor é obrigatório. ");
            valid = false;
        } else {
            try {
                double value = Double.parseDouble(amountText.replace(",", "."));
                if (value <= 0) {
                    errors.append("Valor deve ser maior que zero. ");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errors.append("Valor inválido. ");
                valid = false;
            }
        }

        if (methodCombo.getValue() == null || methodCombo.getValue().isBlank()) {
            errors.append("Método de pagamento é obrigatório. ");
            valid = false;
        }

        formValidProperty.set(valid);
        if (!valid) showError(errors.toString().trim()); else hideError();
    }

    public void setSupplierPayment(SupplierPayment payment) {
        this.payment = payment != null ? payment : new SupplierPayment();
        if (payment != null && payment.getId() != null) {
            supplierCombo.setValue(payment.getSupplier());
            purchaseCombo.setValue(payment.getPurchase());
            amountField.setText(String.valueOf(payment.getAmount()));
            methodCombo.setValue(payment.getMethod());
            if (payment.getCreatedAt() != null) paymentDatePicker.setValue(payment.getCreatedAt().toLocalDate());
            referenceField.setText(payment.getReference());
            saveButton.setDisable(true);
            showError("Pagamentos já gravados não podem ser alterados.");
        }
    }

    public void setSupplier(Supplier supplier) {
        if (supplier != null) {
            supplierCombo.setValue(supplier);
            refreshPurchaseCombo(supplier);
        }
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;

        SupplierPayment p = payment != null ? payment : new SupplierPayment();
        p.setSupplier(supplierCombo.getValue());
        p.setPurchase(purchaseCombo.getValue());
        p.setAmount(Double.parseDouble(amountField.getText().replace(",", ".")));
        p.setMethod(methodCombo.getValue());
        p.setReference(referenceField.getText() != null ? referenceField.getText().trim() : null);
        if (paymentDatePicker.getValue() != null) {
            p.setCreatedAt(paymentDatePicker.getValue().atStartOfDay());
        }

        showSaveSpinner();
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                supplierPaymentService.savePayment(p);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            doCancel();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("SUPPLIER_PAYMENT_SAVE_FAILED", "Erro ao salvar pagamento a fornecedor: " + msg, ex);
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        new Thread(task).start();
    }
}
