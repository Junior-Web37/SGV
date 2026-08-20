package com.sgv.desktop;

import com.sgv.entity.StockBranch;
import com.sgv.service.StockBranchService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class StockAdjustFormController extends BaseFormController {

    @FXML private Label productLabel;
    @FXML private Label branchLabel;
    @FXML private Label currentStockInfoLabel;
    @FXML private TextField currentStockField;
    @FXML private TextField minStockField;
    @FXML private TextField maxStockField;
    @FXML private TextArea reasonArea;

    private final StockBranchService stockBranchService;
    private final com.sgv.service.SystemLogService systemLogService;
    private StockBranch editingStock;
    private BigDecimal originalStock;

    public StockAdjustFormController(StockBranchService stockBranchService, com.sgv.service.SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
        this.stockBranchService = stockBranchService;
    }

    @FXML
    public void initialize() {
        editingStock = null;
        originalStock = BigDecimal.ZERO;
        currentUser = null;
        onSave = null;
        if (productLabel != null) productLabel.setText("-");
        if (branchLabel != null) branchLabel.setText("-");
        if (currentStockInfoLabel != null) currentStockInfoLabel.setText("-");
        if (currentStockField != null) currentStockField.setText("");
        if (minStockField != null) minStockField.setText("");
        if (maxStockField != null) maxStockField.setText("");
        if (reasonArea != null) reasonArea.setText("");
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "STOCK_ADJUST_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "STOCK_ADJUST_CANCEL");

        UiUtils.applyNumericFormatter(currentStockField);
        UiUtils.applyNumericFormatter(minStockField);
        UiUtils.applyNumericFormatter(maxStockField);

        currentStockField.textProperty().addListener((obs, o, n) -> validateRealTime());
        minStockField.textProperty().addListener((obs, o, n) -> validateRealTime());
        maxStockField.textProperty().addListener((obs, o, n) -> validateRealTime());
        reasonArea.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        String current = currentStockField.getText();
        if (current == null || current.isBlank()) {
            errors.append("Stock actual é obrigatório. ");
            valid = false;
        } else {
            try {
                BigDecimal val = new BigDecimal(current.replace(",", "."));
                if (val.compareTo(BigDecimal.ZERO) < 0) {
                    errors.append("Stock actual não pode ser negativo. ");
                    valid = false;
                }
            } catch (Exception e) {
                errors.append("Stock actual inválido. ");
                valid = false;
            }
        }

        String min = minStockField.getText();
        if (min == null || min.isBlank()) {
            errors.append("Stock mínimo é obrigatório. ");
            valid = false;
        } else {
            try {
                new BigDecimal(min.replace(",", "."));
            } catch (Exception e) {
                errors.append("Stock mínimo inválido. ");
                valid = false;
            }
        }

        String max = maxStockField.getText();
        if (max == null || max.isBlank()) {
            errors.append("Stock máximo é obrigatório. ");
            valid = false;
        } else {
            try {
                new BigDecimal(max.replace(",", "."));
            } catch (Exception e) {
                errors.append("Stock máximo inválido. ");
                valid = false;
            }
        }

        if (valid) {
            try {
                BigDecimal minVal = new BigDecimal(minStockField.getText().replace(",", "."));
                BigDecimal maxVal = new BigDecimal(maxStockField.getText().replace(",", "."));
                if (minVal.compareTo(maxVal) > 0) {
                    errors.append("Stock mínimo não pode ser maior que o stock máximo. ");
                    valid = false;
                }
            } catch (Exception e) {
                errors.append("Valores de stock inválidos. ");
                valid = false;
            }
        }

        String motivo = reasonArea.getText();
        if (motivo == null || motivo.trim().isEmpty()) {
            errors.append("Motivo é obrigatório. ");
            valid = false;
        }

        formValidProperty.set(valid);
        if (!valid) {
            showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios.");
        } else {
            hideError();
        }
    }

    public void setStock(StockBranch stock) {
        this.editingStock = stock;
        if (stock != null) {
            productLabel.setText(stock.getProduct() != null ? stock.getProduct().getName() : "N/A");
            branchLabel.setText(stock.getBranch() != null ? stock.getBranch().getName() : "N/A");
            originalStock = stock.getStockCurrentAmount() != null ? stock.getStockCurrentAmount() : BigDecimal.ZERO;
            currentStockInfoLabel.setText(originalStock.toPlainString() + " unidades");
            currentStockField.setText(originalStock.toPlainString());
            minStockField.setText(stock.getStockMinAmount() != null ? stock.getStockMinAmount().toPlainString() : "0.0");
            maxStockField.setText(stock.getStockMaxAmount() != null ? stock.getStockMaxAmount().toPlainString() : "0.0");
        }
        validateRealTime();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get() || editingStock == null) return;
        if (reasonArea.getText() == null || reasonArea.getText().trim().isEmpty()) { showError("Motivo é obrigatório."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                if (currentUser == null) throw new IllegalStateException("Utilizador não autenticado.");
                BigDecimal current = new BigDecimal(currentStockField.getText().replace(",", "."));
                BigDecimal min = new BigDecimal(minStockField.getText().replace(",", "."));
                BigDecimal max = new BigDecimal(maxStockField.getText().replace(",", "."));
                String reason = reasonArea.getText().trim();
                String reference = "PERDA_DANO - " + reason;
                stockBranchService.adjustStock(editingStock, current, min, max, reference, currentUser);
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "AJUSTE_STOCK_GRAVADO", "Ajuste de stock gravado para: " + (editingStock != null && editingStock.getProduct() != null ? editingStock.getProduct().getName() : "Artigo")); if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> { Throwable ex = saveTask.getException(); systemLogService.logError("STOCK_ADJUST_FAILED", "Erro ao ajustar stock: " + (ex != null ? ex.getMessage() : ""), ex);
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
