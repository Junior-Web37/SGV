package com.sgv.desktop;

import com.sgv.entity.StockBranch;
import com.sgv.entity.User;
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
    private StockBranch editingStock;
    private BigDecimal originalStock;

    public StockAdjustFormController(StockBranchService stockBranchService) {
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
        boolean valid = true;
        try {
            String t = currentStockField.getText();
            if (t == null || t.isBlank()) {
                valid = false;
            } else {
                BigDecimal val = new BigDecimal(t.replace(",", "."));
                if (val.compareTo(BigDecimal.ZERO) < 0) valid = false;
            }
        } catch (Exception e) { valid = false; }

        try {
            String t = minStockField.getText();
            if (t == null || t.isBlank()) { valid = false; }
            else { Double.parseDouble(t.replace(",", ".")); }
        } catch (Exception e) { valid = false; }

        try {
            String t = maxStockField.getText();
            if (t == null || t.isBlank()) { valid = false; }
            else { Double.parseDouble(t.replace(",", ".")); }
        } catch (Exception e) { valid = false; }

        if (valid) {
            try {
                BigDecimal min = new BigDecimal(minStockField.getText().replace(",", "."));
                BigDecimal max = new BigDecimal(maxStockField.getText().replace(",", "."));
                if (min.compareTo(max) > 0) valid = false;
            } catch (Exception e) { valid = false; }
        }

        String motivo = reasonArea.getText();
        if (motivo == null || motivo.trim().isEmpty()) valid = false;

        formValidProperty.set(valid);
        if (valid) hideError();
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
        saveTask.setOnSucceeded(e -> { if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            showError("Erro ao salvar: " + saveTask.getException().getMessage());
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
