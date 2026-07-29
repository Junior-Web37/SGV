package com.sgv.desktop;

import com.sgv.entity.Branch;
import com.sgv.entity.Product;
import com.sgv.entity.User;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.service.StockBranchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class StockNewFormController extends BaseFormController {

    @FXML private ComboBox<Product> productCombo;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private TextField currentStockField;
    @FXML private TextField minStockField;
    @FXML private TextField maxStockField;
    @FXML private TextArea referenceArea;

    private final StockBranchService stockBranchService;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    public StockNewFormController(StockBranchService stockBranchService,
                                  ProductRepository productRepository,
                                  BranchRepository branchRepository) {
        this.stockBranchService = stockBranchService;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
    }

    @FXML
    public void initialize() {
        currentUser = null;
        onSave = null;
        initCommonFields();

        productCombo.setItems(FXCollections.observableArrayList(productRepository.findAllActive()));
        productCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Product p) { return p != null ? p.getCode() + " - " + p.getName() : ""; }
            public Product fromString(String s) {
                return productCombo.getItems().stream().filter(p -> (p.getCode() + " - " + p.getName()).equals(s)).findFirst().orElse(null);
            }
        });

        branchCombo.setItems(FXCollections.observableArrayList(branchRepository.findAll()));
        branchCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Branch b) { return b != null ? b.getName() : ""; }
            public Branch fromString(String s) {
                return branchCombo.getItems().stream().filter(b -> b.getName().equals(s)).findFirst().orElse(null);
            }
        });

        UiUtils.attachSafe(saveButton, this::doSave, null, "STOCK_NEW_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "STOCK_NEW_CANCEL");

        UiUtils.applyNumericFormatter(currentStockField);

        productCombo.valueProperty().addListener((obs, o, n) -> {
            validateRealTime();
            if (n != null) {
                if (n.getStockMin() != null && n.getStockMin() > 0) {
                    minStockField.setText(String.valueOf(n.getStockMin()));
                }
                if (n.getStockMax() != null && n.getStockMax() > 0) {
                    maxStockField.setText(String.valueOf(n.getStockMax()));
                }
            }
        });
        branchCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        currentStockField.textProperty().addListener((obs, o, n) -> validateRealTime());
        minStockField.textProperty().addListener((obs, o, n) -> validateRealTime());
        maxStockField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        boolean valid = true;
        if (productCombo.getValue() == null) valid = false;
        if (branchCombo.getValue() == null) valid = false;
        try {
            String t = currentStockField.getText();
            if (t == null || t.isBlank()) valid = false;
            else new BigDecimal(t.replace(",", "."));
        } catch (Exception e) { valid = false; }
        if (valid) {
            try {
                String minText = minStockField.getText();
                String maxText = maxStockField.getText();
                if (minText != null && !minText.isBlank() && maxText != null && !maxText.isBlank()) {
                    BigDecimal min = new BigDecimal(minText.replace(",", "."));
                    BigDecimal max = new BigDecimal(maxText.replace(",", "."));
                    if (min.compareTo(max) > 0) valid = false;
                }
            } catch (Exception e) { valid = false; }
        }
        formValidProperty.set(valid);
        if (valid) hideError();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                if (currentUser == null) throw new IllegalStateException("Utilizador não autenticado.");
                Product product = productCombo.getValue();
                Branch branch = branchCombo.getValue();
                BigDecimal current = new BigDecimal(currentStockField.getText().replace(",", "."));
                BigDecimal min = minStockField.getText() != null && !minStockField.getText().isBlank()
                        ? new BigDecimal(minStockField.getText().replace(",", ".")) : BigDecimal.ZERO;
                BigDecimal max = maxStockField.getText() != null && !maxStockField.getText().isBlank()
                        ? new BigDecimal(maxStockField.getText().replace(",", ".")) : BigDecimal.ZERO;
                String ref = referenceArea.getText() != null ? referenceArea.getText().trim() : "";
                if (ref.isEmpty()) ref = "INITIAL_STOCK";
                stockBranchService.initializeStock(branch, product, current, min, max, ref, currentUser);
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
