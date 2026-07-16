package com.sgv.desktop;

import com.sgv.entity.Category;
import com.sgv.entity.Product;
import com.sgv.repository.CategoryRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.MetricUnitRepository;
import com.sgv.entity.MetricUnit;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Optional;
import com.sgv.service.SystemLogService;

@Component
public class ProductFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField priceCostField;
    @FXML private TextField priceSaleField;
    @FXML private TextField priceSaleBulkField;
    @FXML private TextField bulkQuantityField;
    @FXML private ComboBox<MetricUnit> unitField;
    @FXML private ComboBox<MetricUnit> unitBulkField;
    @FXML private TextField taxRateField;
    @FXML private TextField iceRateField;
    @FXML private TextField profitMarginField;
    @FXML private CheckBox isServiceCheckbox;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final SystemLogService systemLogService;

    private Product product;
    private Runnable onSave;
    private boolean codeAlreadyExists = false;

    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public ProductFormController(ProductRepository productRepository,
                                 CategoryRepository categoryRepository,
                                 MetricUnitRepository metricUnitRepository,
                                 SystemLogService systemLogService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        categoryCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                categoryRepository.findAll()));
        unitField.setItems(javafx.collections.FXCollections.observableArrayList(
                metricUnitRepository.findAll()));
        unitBulkField.setItems(javafx.collections.FXCollections.observableArrayList(
                metricUnitRepository.findAll()));

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

        // UX/MVVM: Data-Binding do botão de salvar
        saveButton.disableProperty().bind(formValidProperty.not());

        // Setup real-time listeners for styles & validation
        setupRealTimeValidation();
    }

    private void setupRealTimeValidation() {
        // Check duplicate code asynchronously when user types
        codeField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) {
                checkDuplicateCode(n);
            } else {
                codeAlreadyExists = false;
                validateRealTime();
            }
        });

        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        categoryCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        priceSaleField.textProperty().addListener((obs, o, n) -> validateRealTime());
        priceCostField.textProperty().addListener((obs, o, n) -> validateRealTime());

        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void checkDuplicateCode(String code) {
        // Run in background to avoid blocking UI
        new Thread(() -> {
            Optional<Product> existing = productRepository.findAll().stream()
                    .filter(p -> code.equalsIgnoreCase(p.getCode()))
                    .filter(p -> product == null || product.getId() == null || !p.getId().equals(product.getId()))
                    .findFirst();
            codeAlreadyExists = existing.isPresent();
            javafx.application.Platform.runLater(this::validateRealTime);
        }).start();
    }

    private void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        // 1. Código: obrigatório + não duplicado
        if (codeField.getText() == null || codeField.getText().isBlank()) {
            valid = false;
        } else if (codeAlreadyExists) {
            errors.append("Código já existe. ");
            valid = false;
        }

        // 2. Nome: obrigatório
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            valid = false;
        }

        // 3. Categoria: obrigatória
        if (categoryCombo.getValue() == null) {
            valid = false;
        }

        // 4. Preço de venda: obrigatório + >= 0
        if (priceSaleField.getText() == null || priceSaleField.getText().isBlank()) {
            valid = false;
        } else {
            try {
                double price = Double.parseDouble(priceSaleField.getText());
                if (price < 0) {
                    errors.append("Preço de venda não pode ser negativo. ");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errors.append("Preço de venda inválido. ");
                valid = false;
            }
        }

        // 5. Preço de custo: opcional mas se preenchido >= 0
        if (priceCostField.getText() != null && !priceCostField.getText().isBlank()) {
            try {
                double cost = Double.parseDouble(priceCostField.getText());
                if (cost < 0) {
                    errors.append("Preço de custo não pode ser negativo. ");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errors.append("Preço de custo inválido. ");
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

    public void setProduct(Product p) {
        this.product = p;
        if (p != null && p.getId() != null) {
            codeField.setText(p.getCode());
            nameField.setText(p.getName());
            categoryCombo.setValue(p.getCategory());
            priceCostField.setText(String.valueOf(p.getPriceCost() != null ? p.getPriceCost() : 0.0));
            priceSaleField.setText(String.valueOf(p.getPriceSale() != null ? p.getPriceSale() : 0.0));
            priceSaleBulkField.setText(String.valueOf(p.getPriceSaleBulk() != null ? p.getPriceSaleBulk() : 0.0));
            bulkQuantityField.setText(String.valueOf(p.getBulkQuantity() != null ? p.getBulkQuantity() : 0));
            unitField.setValue(p.getUnit());
            unitBulkField.setValue(p.getUnitBulk());
            taxRateField.setText(String.valueOf(p.getTaxRate() != null ? p.getTaxRate() : 0.0));
            iceRateField.setText(String.valueOf(p.getIceRate() != null ? p.getIceRate() : 0.0));
            profitMarginField.setText(String.valueOf(p.getProfitMargin() != null ? p.getProfitMargin() : 0.0));
            isServiceCheckbox.setSelected(p.getService() != null && p.getService());
            codeField.setDisable(true); // não alterar código ao editar
            codeAlreadyExists = false; // edição não tem duplicado
        } else {
            this.product = new Product();
        }
        validateRealTime();
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

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                product.setCode(codeField.getText().trim());
                product.setName(nameField.getText().trim());
                product.setCategory(categoryCombo.getValue());
                product.setPriceCost(parseDoubleOrZero(priceCostField.getText()));
                product.setPriceSale(parseDoubleOrZero(priceSaleField.getText()));
                product.setPriceSaleBulk(parseDoubleOrZero(priceSaleBulkField.getText()));
                product.setBulkQuantity(parseDoubleOrZeroToInt(bulkQuantityField.getText()));
                product.setUnit(unitField.getValue());
                product.setUnitBulk(unitBulkField.getValue());
                product.setTaxRate(parseDoubleOrZero(taxRateField.getText()));
                product.setIceRate(parseDoubleOrZero(iceRateField.getText()));
                product.setProfitMargin(parseDoubleOrZero(profitMarginField.getText()));
                product.setService(isServiceCheckbox.isSelected());

                productRepository.save(product);
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
            systemLogService.logError("PRODUCT_SAVE_FAILED", "Erro ao salvar produto: " + msg, ex);
            
            if (msg.contains("ConstraintViolationException") || msg.contains("DataIntegrityViolationException")) {
                showError("Código duplicado. Este produto já existe.");
            } else {
                showError("Erro ao guardar: " + msg);
            }

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

    private double parseDoubleOrZero(String text) {
        try {
            return text == null || text.isBlank() ? 0.0 : Double.parseDouble(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Double parseDoubleOrZeroToInt(String text) {
        try {
            return text == null || text.isBlank() ? 0.0 : Double.parseDouble(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
