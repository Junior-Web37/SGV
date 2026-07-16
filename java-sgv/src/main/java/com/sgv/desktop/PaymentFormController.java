package com.sgv.desktop;

import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import com.sgv.service.SaleDocumentService;
import com.sgv.service.SystemLogService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private ComboBox<Sale> saleCombo;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private DatePicker paymentDatePicker;
    @FXML private TextField referenceField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final SaleDocumentService saleDocumentService;
    private final SystemLogService systemLogService;
    private Payment payment;
    private Runnable onSave;
    
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public PaymentFormController(PaymentRepository paymentRepository, SaleRepository saleRepository, SaleDocumentService saleDocumentService, SystemLogService systemLogService) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.saleDocumentService = saleDocumentService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        paymentMethodCombo.getItems().addAll("Numerário", "M-Pesa", "e-Mola", "Cartão (POS)", "Transferência Bancária", "Cheque");
        paymentMethodCombo.setValue("Numerário");
        if (paymentDatePicker != null) paymentDatePicker.setValue(java.time.LocalDate.now());

        // Carregar vendas recentes que não estejam "CANCELADAS"
        java.util.List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> !"CANCELLED".equalsIgnoreCase(s.getState()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(50)
                .toList();
        
        saleCombo.setItems(javafx.collections.FXCollections.observableArrayList(sales));
        
        saleCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getTotal() != null) {
                amountField.setText(String.format("%.2f", newVal.getTotal()).replace(",", "."));
            }
        });

        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        // UX: Transição de entrada fluida (Fade-in)
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // UX/MVVM: Data-Binding
        saveButton.disableProperty().bind(formValidProperty.not());

        // Listeners
        saleCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        amountField.textProperty().addListener((obs, o, n) -> validateRealTime());
        paymentMethodCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }
    
    private void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (saleCombo.getValue() == null) {
            errors.append("Selecione uma venda. ");
            valid = false;
        }

        String amtStr = amountField.getText();
        if (amtStr == null || amtStr.isBlank()) {
            errors.append("Valor é obrigatório. ");
            valid = false;
        } else {
            try {
                double amt = Double.parseDouble(amtStr.replace(",", "."));
                if (amt <= 0) {
                    errors.append("Valor deve ser maior que 0. ");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errors.append("Valor inválido. ");
                valid = false;
            }
        }

        if (paymentMethodCombo.getValue() == null) {
            errors.append("Método de pagamento é obrigatório. ");
            valid = false;
        }

        formValidProperty.set(valid);
        if (!valid && errors.length() > 0) {
            showError(errors.toString().trim());
        } else {
            hideError();
        }
    }

    public void setPayment(Payment p) {
        this.payment = p;
        if (p != null && p.getId() != null) {
            saleCombo.setValue(p.getSale());
            amountField.setText(String.valueOf(p.getAmount()));
            paymentMethodCombo.setValue(p.getMethod());
            // Prevenir edição de pagamentos antigos por questões de auditoria
            saveButton.setDisable(true);
            showError("Pagamentos emitidos não podem ser alterados.");
        } else {
            this.payment = new Payment();
        }
    }

    public void setOnSave(Runnable callback) {
        this.onSave = callback;
    }

    private void doSave() {
        if (!formValidProperty.get()) return;

        saveButton.setVisible(false);
        saveSpinner.setVisible(true);
        saveSpinner.setManaged(true);
        cancelButton.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        javafx.concurrent.Task<java.io.File> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected java.io.File call() throws Exception {
                payment.setSale(saleCombo.getValue());
                payment.setAmount(Double.parseDouble(amountField.getText().replace(",", ".")));
                payment.setMethod(paymentMethodCombo.getValue());
                
                if (payment.getId() == null) {
                    payment.setCreatedAt(LocalDateTime.now());
                }

                paymentRepository.save(payment);
                
                // Atualizar estado da Venda para PAGO
                if (payment.getSale() != null) {
                    Sale s = payment.getSale();
                    s.setState(com.sgv.model.SaleState.PAGO.name());
                    s.setPaidAmount((s.getPaidAmount() != null ? s.getPaidAmount() : 0) + payment.getAmount());
                    saleRepository.save(s);
                    
                    // Gerar o Recibo (usando um clone temporário para não afetar o tipo no BD)
                    String origType = s.getDocumentType();
                    try {
                        s.setDocumentType(com.sgv.model.DocumentType.RECIBO.name());
                        return saleDocumentService.generateDocument(s);
                    } finally {
                        s.setDocumentType(origType);
                    }
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
                        javafx.scene.control.Label successLabel = new javafx.scene.control.Label("Pagamento efetuado com sucesso!");
                        successLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center;");
                        successLabel.setMaxWidth(Double.MAX_VALUE);
                        rootPane.getChildren().add(0, successLabel);
                        
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                        delay.setOnFinished(ev -> rootPane.getChildren().remove(successLabel));
                        delay.play();
                        
                        com.sgv.desktop.DocumentPreviewDialog.show(pdf, com.sgv.model.DocumentType.RECIBO.name());
                    });
                } catch (Exception ex) {
                    systemLogService.logError("PDF_OPEN_FAILED", "Falha ao preparar visualização do recibo.", ex);
                }
            }
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("PAYMENT_SAVE_FAILED", "Erro ao salvar pagamento: " + ex.getMessage(), ex);
            showError("Erro ao salvar: " + ex.getMessage());
            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 0;");
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }


    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
