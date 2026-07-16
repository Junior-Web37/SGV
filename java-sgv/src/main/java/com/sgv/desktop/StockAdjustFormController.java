package com.sgv.desktop;

import com.sgv.entity.StockBranch;
import com.sgv.entity.User;
import com.sgv.service.StockBranchService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class StockAdjustFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private Label productLabel;
    @FXML private Label branchLabel;
    @FXML private TextField currentStockField;
    @FXML private TextField minStockField;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final StockBranchService stockBranchService;
    private Runnable onSave;
    private StockBranch editingStock;
    private User currentUser;

    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public StockAdjustFormController(StockBranchService stockBranchService) {
        this.stockBranchService = stockBranchService;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        saveButton.disableProperty().bind(formValidProperty.not());

        javafx.beans.value.ChangeListener<String> listener = (obs, o, n) -> validateRealTime();
        currentStockField.textProperty().addListener(listener);
        minStockField.textProperty().addListener(listener);
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void validateRealTime() {
        boolean valid = true;
        try { Double.parseDouble(currentStockField.getText().replace(",", ".")); } catch (Exception e) { valid = false; }
        try { Double.parseDouble(minStockField.getText().replace(",", ".")); } catch (Exception e) { valid = false; }
        formValidProperty.set(valid);
        if (valid) { errorLabel.setVisible(false); errorLabel.setManaged(false); }
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void setStock(StockBranch stock) {
        this.editingStock = stock;
        if (stock != null) {
            productLabel.setText(stock.getProduct() != null ? stock.getProduct().getName() : "N/A");
            branchLabel.setText(stock.getBranch() != null ? stock.getBranch().getName() : "N/A");
            currentStockField.setText(stock.getStockCurrentAmount() != null ? stock.getStockCurrentAmount().toPlainString() : "0.0");
            minStockField.setText(stock.getStockMinAmount() != null ? stock.getStockMinAmount().toPlainString() : "0.0");
        }
        validateRealTime();
    }

    private void doSave() {
        if (!formValidProperty.get() || editingStock == null) return;

        saveButton.setVisible(false);
        saveSpinner.setVisible(true);
        saveSpinner.setManaged(true);
        cancelButton.setDisable(true);

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                if (currentUser == null) {
                    throw new IllegalStateException("Utilizador não autenticado.");
                }
                BigDecimal current = new BigDecimal(currentStockField.getText().replace(",", "."));
                BigDecimal min = new BigDecimal(minStockField.getText().replace(",", "."));
                stockBranchService.adjustStock(editingStock, current, min, "AJUSTE_MANUAL", currentUser);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            closeStage();
        });

        saveTask.setOnFailed(e -> {
            errorLabel.setText("Erro ao salvar: " + saveTask.getException().getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }

    private void doCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
