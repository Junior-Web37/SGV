package com.sgv.desktop;

import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.service.PaymentService;
import com.sgv.service.SystemLogService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentFormController extends BaseFormController {

    @FXML private Label totalSaleLabel;
    @FXML private Label paidSaleLabel;
    @FXML private Label pendingSaleLabel;

    @FXML private ComboBox<Sale> saleCombo;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private DatePicker paymentDatePicker;
    @FXML private TextField referenceField;

    private final PaymentService paymentService;
    private final SystemLogService systemLogService;
    private final com.sgv.service.ThermalPrintService thermalPrintService;
    private final com.sgv.service.SaleDocumentService saleDocumentService;
    private Payment payment;
    private BigDecimal currentPendingBalance = BigDecimal.ZERO;

    public PaymentFormController(PaymentService paymentService,
                                 SystemLogService systemLogService,
                                 com.sgv.service.ThermalPrintService thermalPrintService,
                                 com.sgv.service.SaleDocumentService saleDocumentService) {
        this.paymentService = paymentService;
        this.systemLogService = systemLogService;
        this.thermalPrintService = thermalPrintService;
        this.saleDocumentService = saleDocumentService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        paymentMethodCombo.getItems().addAll("Numerário", "M-Pesa", "e-Mola", "mKesh", "Cartão (POS)", "Transferência Bancária", "Cheque");
        paymentMethodCombo.setValue("Numerário");
        if (paymentDatePicker != null) paymentDatePicker.setValue(LocalDate.now());

        // Carregar apenas facturas e vendas pendentes com saldo em dívida
        List<Sale> pendingSales = paymentService.findPendingSales();
        saleCombo.setItems(FXCollections.observableArrayList(pendingSales));
        saleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Sale s) {
                if (s == null) return "";
                String doc = (s.getDocumentType() != null ? s.getDocumentType() : "DOC") + " "
                        + (s.getSeries() != null ? s.getSeries() : "A") + "/" + (s.getDocumentNumber() != null ? s.getDocumentNumber() : s.getId());
                String cust = s.getCustomerName() != null ? s.getCustomerName() : (s.getCustomer() != null ? s.getCustomer().getName() : "Cliente");
                BigDecimal tot = s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal paid = s.getPaidAmountValue() != null ? s.getPaidAmountValue() : BigDecimal.ZERO;
                BigDecimal pend = tot.subtract(paid);
                return String.format("%s - %s | Total: %.2f MT (Pendente: %.2f MT)", doc, cust, tot, pend);
            }

            @Override
            public Sale fromString(String string) { return null; }
        });

        saleCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateSaleSummary(newVal);
            validateRealTime();
        });

        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "PAYMENT_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "PAYMENT_CANCEL");
        UiUtils.applyNumericFormatter(amountField);

        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        paymentMethodCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void updateSaleSummary(Sale s) {
        if (s == null) {
            currentPendingBalance = BigDecimal.ZERO;
            if (totalSaleLabel != null) totalSaleLabel.setText("0.00 MT");
            if (paidSaleLabel != null) paidSaleLabel.setText("0.00 MT");
            if (pendingSaleLabel != null) pendingSaleLabel.setText("0.00 MT");
            amountField.setText("0.00");
            return;
        }

        BigDecimal tot = s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = s.getPaidAmountValue() != null ? s.getPaidAmountValue() : BigDecimal.ZERO;
        currentPendingBalance = tot.subtract(paid);

        if (totalSaleLabel != null) totalSaleLabel.setText(String.format("%.2f MT", tot));
        if (paidSaleLabel != null) paidSaleLabel.setText(String.format("%.2f MT", paid));
        if (pendingSaleLabel != null) pendingSaleLabel.setText(String.format("%.2f MT", currentPendingBalance));
        amountField.setText(String.format(java.util.Locale.US, "%.2f", currentPendingBalance));
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (saleCombo.getValue() == null) {
            errors.append("Seleccione uma factura pendente. ");
            valid = false;
        }

        String amtStr = amountField.getText();
        if (amtStr == null || amtStr.isBlank()) {
            errors.append("Valor a receber é obrigatório. ");
            valid = false;
        } else {
            try {
                BigDecimal amt = new BigDecimal(amtStr.trim().replace(",", "."));
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.append("Valor deve ser maior que zero. ");
                    valid = false;
                } else if (currentPendingBalance.compareTo(BigDecimal.ZERO) > 0 && amt.compareTo(currentPendingBalance) > 0) {
                    errors.append(String.format("Valor (%.2f MT) excede o saldo pendente (%.2f MT). ", amt, currentPendingBalance));
                    valid = false;
                }
            } catch (Exception e) {
                errors.append("Valor inválido. ");
                valid = false;
            }
        }

        if (paymentMethodCombo.getValue() == null) {
            errors.append("Método de pagamento é obrigatório. ");
            valid = false;
        }

        formValidProperty.set(valid);
        if (!valid) showError(errors.toString().trim());
        else hideError();
    }

    public void setPayment(Payment p) {
        this.payment = p;
        if (p != null && p.getId() != null) {
            saleCombo.setValue(p.getSale());
            amountField.setText(String.valueOf(p.getAmount()));
            paymentMethodCombo.setValue(p.getMethod());
            saveButton.setDisable(true);
            showError("Recibos já emitidos não podem ser alterados.");
        } else {
            this.payment = new Payment();
        }
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (saleCombo.getValue() == null) { showError("Seleccione uma factura."); return; }
        if (amountField.getText() == null || amountField.getText().isBlank()) { showError("Valor é obrigatório."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<File> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected File call() throws Exception {
                payment.setSale(saleCombo.getValue());
                BigDecimal amt = new BigDecimal(amountField.getText().trim().replace(",", "."));
                payment.setAmountValue(amt);
                payment.setMethod(paymentMethodCombo.getValue());
                payment.setTerminalRef(referenceField != null ? referenceField.getText() : null);
                if (paymentDatePicker != null && paymentDatePicker.getValue() != null) {
                    payment.setCreatedAt(paymentDatePicker.getValue().atStartOfDay());
                } else if (payment.getId() == null) {
                    payment.setCreatedAt(LocalDateTime.now());
                }
                return paymentService.createPayment(payment, currentUser);
            }
        };
        saveTask.setOnSucceeded(e -> {
            File pdf = saveTask.getValue();
            if (pdf != null) {
                Sale s = saleCombo.getValue();
                javafx.application.Platform.runLater(() -> DocumentPreviewDialog.show(
                    pdf,
                    "RECIBO",
                    DocumentPreviewDialog.FMT_THERMAL_80MM,
                    s,
                    sale -> { try { return thermalPrintService.printReceipt(sale); } catch (Exception ex) { return null; } },
                    sale -> { try { return thermalPrintService.printReceipt58mm(sale); } catch (Exception ex) { return null; } },
                    sale -> { try { return saleDocumentService.generateDocument(sale); } catch (Exception ex) { return null; } },
                    sale -> { try { return saleDocumentService.generateDocumentA5(sale); } catch (Exception ex) { return null; } }
                ));
            }
            if (onSave != null) onSave.run();
            doCancel();
        });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("PAYMENT_SAVE_FAILED", "Erro ao salvar recebimento: " + msg, ex);
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
