package com.sgv.desktop;

import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import com.sgv.service.SaleDocumentService;
import com.sgv.service.SystemLogService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class PaymentFormController extends BaseFormController {

    @FXML private ComboBox<Sale> saleCombo;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private DatePicker paymentDatePicker;
    @FXML private TextField referenceField;

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final SaleDocumentService saleDocumentService;
    private final SystemLogService systemLogService;
    private Payment payment;

    public PaymentFormController(PaymentRepository paymentRepository, SaleRepository saleRepository,
                                  SaleDocumentService saleDocumentService, SystemLogService systemLogService) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.saleDocumentService = saleDocumentService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        paymentMethodCombo.getItems().addAll("Numerário", "M-Pesa", "e-Mola", "Cartão (POS)", "Transferência Bancária", "Cheque");
        paymentMethodCombo.setValue("Numerário");
        if (paymentDatePicker != null) paymentDatePicker.setValue(java.time.LocalDate.now());

        java.util.List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> !"CANCELLED".equalsIgnoreCase(s.getState()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(50).toList();
        saleCombo.setItems(javafx.collections.FXCollections.observableArrayList(sales));
        saleCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getTotal() != null)
                amountField.setText(String.format("%.2f", newVal.getTotal()).replace(",", "."));
        });

        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "PAYMENT_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "PAYMENT_CANCEL");
        
        UiUtils.applyNumericFormatter(amountField);

        saleCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        paymentMethodCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (saleCombo.getValue() == null) { errors.append("Selecione uma venda. "); valid = false; }
        String amtStr = amountField.getText();
        if (amtStr == null || amtStr.isBlank()) { errors.append("Valor é obrigatório. "); valid = false; }
        else {
            try { 
                java.math.BigDecimal amt = new java.math.BigDecimal(amtStr.replace(",", ".")); 
                if (amt.compareTo(java.math.BigDecimal.ZERO) <= 0) { errors.append("Valor deve ser maior que 0. "); valid = false; } 
            }
            catch (Exception e) { errors.append("Valor inválido. "); valid = false; }
        }
        if (paymentMethodCombo.getValue() == null) { errors.append("Método de pagamento é obrigatório. "); valid = false; }

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
            showError("Pagamentos emitidos não podem ser alterados.");
        } else {
            this.payment = new Payment();
        }
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (saleCombo.getValue() == null) { showError("Selecione uma venda."); return; }
        if (amountField.getText() == null || amountField.getText().isBlank()) { showError("Valor é obrigatório."); return; }
        if (paymentMethodCombo.getValue() == null) { showError("Método de pagamento é obrigatório."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<java.io.File> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected java.io.File call() {
                payment.setSale(saleCombo.getValue());
                java.math.BigDecimal amt = new java.math.BigDecimal(amountField.getText().replace(",", "."));
                payment.setAmount(amt.doubleValue());
                payment.setMethod(paymentMethodCombo.getValue());
                if (paymentDatePicker != null && paymentDatePicker.getValue() != null) {
                    payment.setCreatedAt(paymentDatePicker.getValue().atStartOfDay());
                } else if (payment.getId() == null) {
                    payment.setCreatedAt(LocalDateTime.now());
                }
                paymentRepository.save(payment);

                if (payment.getSale() != null) {
                    Sale s = payment.getSale();
                    s.setState(com.sgv.model.SaleState.PAGO.name());
                    s.setPaidAmount((s.getPaidAmount() != null ? s.getPaidAmount() : 0) + payment.getAmount());
                    saleRepository.save(s);
                    String origType = s.getDocumentType();
                    try {
                        s.setDocumentType(com.sgv.model.DocumentType.RECIBO.name());
                        return saleDocumentService.generateDocument(s);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    } finally { s.setDocumentType(origType); }
                }
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> {
            java.io.File pdf = saveTask.getValue();
            if (pdf != null) {
                try {
                    systemLogService.logUserAction("Sistema", "PAGAMENTO_CRIADO", "Pagamento gravado com sucesso.");
                    javafx.application.Platform.runLater(() -> {
                        com.sgv.desktop.DocumentPreviewDialog.show(pdf, com.sgv.model.DocumentType.RECIBO.name());
                    });
                } catch (Exception ex) { systemLogService.logError("PDF_OPEN_FAILED", "Falha ao preparar visualização do recibo.", ex); }
            }
            if (onSave != null) onSave.run();
            doCancel();
        });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("PAYMENT_SAVE_FAILED", "Erro ao salvar pagamento: " + msg, ex);
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
