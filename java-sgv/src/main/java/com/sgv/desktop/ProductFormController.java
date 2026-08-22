package com.sgv.desktop;

import com.sgv.entity.Category;
import com.sgv.entity.Product;
import com.sgv.entity.ProductBarcode;
import com.sgv.entity.Supplier;
import com.sgv.repository.CategoryRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.MetricUnitRepository;
import com.sgv.repository.SupplierRepository;
import com.sgv.entity.MetricUnit;
import com.sgv.service.AppConfigService;
import com.sgv.service.ProductService;
import com.sgv.service.SystemLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import java.util.Optional;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ProductFormController extends BaseFormController {

    private static final Logger log = LoggerFactory.getLogger(ProductFormController.class);

    // ─── Campos do FXML ─────────────────────────────────────────────────────
    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private CheckBox activeCheck;
    @FXML private CheckBox isServiceCheckbox;
    
    @FXML private TextField priceCostField;
    @FXML private TextField priceSaleField;
    @FXML private TextField profitMarginField;
    @FXML private TextField taxRateField;
    @FXML private Label taxRateHint;
    
    @FXML private ComboBox<MetricUnit> unitField;
    @FXML private ComboBox<MetricUnit> unitBulkField;
    @FXML private TextField priceSaleBulkField;
    @FXML private TextField bulkQuantityField;
    @FXML private VBox wholesaleSection;
    
    @FXML private TextField stockMinField;
    @FXML private TextField stockMaxField;
    @FXML private VBox stockSection;
    
    @FXML private TextField barcodeField;
    @FXML private TextField barcodeQtyField;
    @FXML private TableView<ProductBarcode> barcodeTable;
    @FXML private TableColumn<ProductBarcode, String> barcodeCodeColumn;
    @FXML private TableColumn<ProductBarcode, Double> barcodeQtyColumn;
    @FXML private Button addBarcodeButton;
    @FXML private Button removeBarcodeButton;
    @FXML private VBox barcodeSection;
    
    @FXML private TextArea descriptionArea;
    @FXML private TextField conversionFactorField;
    @FXML private Button deleteButton;

    // ─── Repositórios e Serviços ────────────────────────────────────────────
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final AppConfigService appConfigService;
    private final SystemLogService systemLogService;
    private final com.sgv.repository.SaleItemRepository saleItemRepository;
    private final ProductService productService;

    // ─── Estado ─────────────────────────────────────────────────────────────
    private final ObservableList<ProductBarcode> barcodeList = FXCollections.observableArrayList();
    private Product product;
    private boolean codeAlreadyExists = false;
    private boolean nameAlreadyExists = false;
    private boolean updatingMargin = false;
    private long nextSeqNumber = 1;
    
    // ─── Valores padrão do sistema ─────────────────────────────────────────
    private double defaultTaxRate = 16.0;
    private double defaultIceRate = 0.0;

    // ─── Construtor ──────────────────────────────────────────────────────────
    public ProductFormController(ProductRepository productRepository,
                                 CategoryRepository categoryRepository,
                                 SupplierRepository supplierRepository,
                                 MetricUnitRepository metricUnitRepository,
                                 AppConfigService appConfigService,
                                 SystemLogService systemLogService,
                                 com.sgv.repository.SaleItemRepository saleItemRepository,
                                 ProductService productService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.appConfigService = appConfigService;
        this.systemLogService = systemLogService;
        this.saleItemRepository = saleItemRepository;
        this.productService = productService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.hardenComboBox(categoryCombo);
        UiUtils.hardenComboBox(supplierCombo);
        UiUtils.hardenComboBox(unitField);
        UiUtils.hardenComboBox(unitBulkField);
        loadDefaultRates();
        
        categoryCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                categoryRepository.findAll()));
        var suppliers = javafx.collections.FXCollections.observableArrayList(
                supplierRepository.findAll());
        supplierCombo.setItems(suppliers);
        if (!suppliers.isEmpty()) {
            supplierCombo.getSelectionModel().selectFirst();
        }
        
        unitField.setItems(javafx.collections.FXCollections.observableArrayList(
                metricUnitRepository.findAll()));
        unitBulkField.setItems(javafx.collections.FXCollections.observableArrayList(
                metricUnitRepository.findAll()));

        taxRateField.setText(String.valueOf(defaultTaxRate));
        
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "PRODUCT_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "PRODUCT_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);

        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
            UiUtils.attachSafe(deleteButton, this::doDelete, systemLogService, "PRODUCT_DELETE");
            UiUtils.applyHoverElevation(deleteButton);
            UiUtils.applyPressFeedback(deleteButton);
        }

        setupBarcodeTable();
        if (addBarcodeButton != null) { UiUtils.applyHoverElevation(addBarcodeButton); UiUtils.applyPressFeedback(addBarcodeButton); }
        if (removeBarcodeButton != null) { UiUtils.applyHoverElevation(removeBarcodeButton); UiUtils.applyPressFeedback(removeBarcodeButton); }
        setupRealTimeValidation();
        setupMarginCalculation();
        setupServiceToggle();
        applyServiceToggleState();
    }

    private void setupServiceToggle() {
        isServiceCheckbox.selectedProperty().addListener((obs, o, n) -> {
            boolean isService = Boolean.TRUE.equals(n);
            unitField.setDisable(isService);
            unitBulkField.setDisable(isService);
            priceSaleBulkField.setDisable(isService);
            bulkQuantityField.setDisable(isService);
            stockMinField.setDisable(isService);
            stockMaxField.setDisable(isService);
            updateStylesForService(isService);
            validateRealTime();
        });
    }

    private void applyServiceToggleState() {
        boolean isService = isServiceCheckbox.isSelected();
        unitField.setDisable(isService);
        unitBulkField.setDisable(isService);
        priceSaleBulkField.setDisable(isService);
        bulkQuantityField.setDisable(isService);
        stockMinField.setDisable(isService);
        stockMaxField.setDisable(isService);
        wholesaleSection.setVisible(!isService);
        wholesaleSection.setManaged(!isService);
        stockSection.setVisible(!isService);
        stockSection.setManaged(!isService);
        updateStylesForService(isService);
    }

    private void updateStylesForService(boolean isService) {
        String normal = "-fx-opacity: 1.0;";
        String dimmed = "-fx-opacity: 0.5;";
        unitField.setStyle(isService ? dimmed : normal);
        unitBulkField.setStyle(isService ? dimmed : normal);
        priceSaleBulkField.setStyle(isService ? dimmed : normal);
        bulkQuantityField.setStyle(isService ? dimmed : normal);
        stockMinField.setStyle(isService ? dimmed : normal);
        stockMaxField.setStyle(isService ? dimmed : normal);
    }
    
    /**
     * Carrega as taxas padrão de IVA/ICE da configuração do sistema.
     */
    private void loadDefaultRates() {
        try {
            var config = appConfigService.get();
            defaultTaxRate = config.getDefaultTaxRate() != null ? config.getDefaultTaxRate() : 16.0;
            defaultIceRate = config.getDefaultIceRate() != null ? config.getDefaultIceRate() : 0.0;
        } catch (Exception e) {
            // Usa valores padrão se falhar
            defaultTaxRate = 16.0;
            defaultIceRate = 0.0;
        }
    }

    private void setupBarcodeTable() {
        barcodeCodeColumn.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        barcodeQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        barcodeTable.setItems(barcodeList);
        barcodeQtyField.setText("1.0");

        addBarcodeButton.setOnAction(e -> addBarcode());
        removeBarcodeButton.setOnAction(e -> removeBarcode());
    }

    private void addBarcode() {
        if (product == null) return;
        String code = barcodeField.getText();
        if (code == null || code.isBlank()) return;
        double qty;
        try {
            qty = Double.parseDouble(barcodeQtyField.getText().trim().replace(",", "."));
        } catch (Exception e) {
            qty = 1.0;
        }

        String trimmedCode = code.trim();
        boolean alreadyInList = barcodeList.stream()
                .anyMatch(pb -> pb.getBarcode() != null && pb.getBarcode().equalsIgnoreCase(trimmedCode));
        if (alreadyInList) {
            showError("Código de barras já registado para este produto.");
            return;
        }

        hideError();
        addBarcodeButton.setDisable(true);
        String finalCode = trimmedCode;
        double finalQty = qty;

        // Verifica na BD se o código já existe
        javafx.concurrent.Task<Boolean> checkTask = new javafx.concurrent.Task<>() {
            @Override
            protected Boolean call() {
                return productService.barcodeExists(finalCode);
            }
        };
        checkTask.setOnSucceeded(ev -> {
            addBarcodeButton.setDisable(false);
            if (checkTask.getValue()) {
                showError("Código de barras já existe no sistema.");
                return;
            }
            ProductBarcode pb = new ProductBarcode();
            pb.setProduct(product);
            pb.setBarcode(finalCode);
            pb.setQuantity(finalQty);

            if (product.getId() != null) {
                javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
                    @Override
                    protected Void call() {
                        productService.saveBarcode(pb);
                        return null;
                    }
                };
                saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "PRODUTO_GRAVADO", "Artigo gravado com sucesso: " + nameField.getText());
                    loadBarcodes();
                    barcodeField.clear();
                    hideError();
                });
                saveTask.setOnFailed(e -> {
                    log.warn("Erro ao adicionar código de barras", saveTask.getException());
                    showError("Erro ao adicionar código de barras.");
                });
                UiUtils.runTask(saveTask);
            } else {
                barcodeList.add(pb);
                barcodeField.clear();
                hideError();
            }
        });
        checkTask.setOnFailed(ev -> {
            addBarcodeButton.setDisable(false);
            log.warn("Erro ao verificar código de barras", checkTask.getException());
            showError("Erro ao verificar código de barras.");
        });
        UiUtils.runTask(checkTask);
    }

    private void removeBarcode() {
        ProductBarcode selected = barcodeTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.getId() != null) {
            // Já existe na BD — apaga
            Long selectedId = selected.getId();
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() {
                    productService.deleteBarcodeById(selectedId);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                loadBarcodes();
                hideError();
            });
            task.setOnFailed(e -> {
                log.warn("Erro ao remover código de barras", task.getException());
                showError("Erro ao remover código de barras.");
            });
            UiUtils.runTask(task);
        } else {
            // Apenas em memória — remove da lista
            barcodeList.remove(selected);
        }
    }

    private void loadBarcodes() {
        if (product != null && product.getId() != null) {
            barcodeList.setAll(productService.findBarcodesByProduct(product));
        }
    }

    /**
     * Salva códigos de barras pendentes (adicionados antes do produto existir na BD).
     */
    private void flushPendingBarcodes() {
        if (product == null || product.getId() == null) return;
        for (ProductBarcode pb : barcodeList) {
            if (pb.getId() == null) {
                pb.setProduct(product);
                productService.saveBarcode(pb);
            }
        }
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

        nameField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) {
                checkDuplicateName(n);
            } else {
                nameAlreadyExists = false;
                validateRealTime();
            }
        });
        
        categoryCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        priceSaleField.textProperty().addListener((obs, o, n) -> validateRealTime());
        priceCostField.textProperty().addListener((obs, o, n) -> validateRealTime());
        unitField.valueProperty().addListener((obs, o, n) -> validateRealTime());
        taxRateField.textProperty().addListener((obs, o, n) -> validateRealTime());
        stockMinField.textProperty().addListener((obs, o, n) -> validateRealTime());
        stockMaxField.textProperty().addListener((obs, o, n) -> validateRealTime());
        isServiceCheckbox.selectedProperty().addListener((obs, o, n) -> validateRealTime());

        UiUtils.applyNumericFormatter(priceCostField);
        UiUtils.applyNumericFormatter(priceSaleField);
        UiUtils.applyNumericFormatter(priceSaleBulkField);
        UiUtils.applyNumericFormatter(taxRateField);
        UiUtils.applyNumericFormatter(bulkQuantityField);
        UiUtils.applyNumericFormatter(stockMinField);
        UiUtils.applyNumericFormatter(stockMaxField);
        UiUtils.applyNumericFormatter(barcodeQtyField);
        UiUtils.applyNumericFormatter(conversionFactorField);

        javafx.application.Platform.runLater(this::validateRealTime);
    }
    
    /**
     * Configura os listeners para calcular a margem de lucro automaticamente
     * quando o preço de custo ou preço de venda mudar.
     */
    private void setupMarginCalculation() {
        // Quando preço de custo muda → recalcula margem
        priceCostField.textProperty().addListener((obs, o, n) -> {
            if (!updatingMargin) {
                calculateMarginFromPrices();
            }
        });
        
        // Quando preço de venda muda → recalcula margem
        priceSaleField.textProperty().addListener((obs, o, n) -> {
            if (!updatingMargin) {
                calculateMarginFromPrices();
            }
        });
    }
    
    /**
     * Calcula a margem de lucro: ((preço venda - preço custo) / preço custo) * 100
     */
    private void calculateMarginFromPrices() {
        try {
            java.math.BigDecimal cost = parseBigDecimalOrZero(priceCostField.getText());
            java.math.BigDecimal sale = parseBigDecimalOrZero(priceSaleField.getText());
            
            if (cost.compareTo(java.math.BigDecimal.ZERO) > 0 && sale.compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal margin = sale.subtract(cost).divide(cost, 4, java.math.RoundingMode.HALF_UP).multiply(new java.math.BigDecimal("100"));
                margin = margin.setScale(2, java.math.RoundingMode.HALF_UP);
                
                updatingMargin = true;
                profitMarginField.setText(margin.toPlainString());
                updatingMargin = false;
                
                profitMarginField.setStyle(
                    "-fx-padding:8 12; -fx-background-radius:3; " +
                    "-fx-border-color:#10B981; -fx-border-radius:3; " +
                    "-fx-font-size:13px; -fx-background-color:#D1FAE5; -fx-text-fill:#065F46;");
            } else {
                updatingMargin = true;
                profitMarginField.setText("0.00");
                updatingMargin = false;
                profitMarginField.setStyle("");
            }
        } catch (Exception e) {
            updatingMargin = true;
            profitMarginField.setText("0.00");
            updatingMargin = false;
        }
        validateRealTime();
    }

    private void checkDuplicateCode(String code) {
        UiUtils.runAsync(() -> {
            try {
                Optional<Product> existing = productService.findByCode(code.trim().toUpperCase());
                if (existing.isPresent() && (product == null || product.getId() == null || !existing.get().getId().equals(product.getId()))) {
                    codeAlreadyExists = true;
                } else {
                    codeAlreadyExists = false;
                }
            } catch (Exception e) {
                log.warn("Erro ao verificar código duplicado: {}", e.getMessage());
                codeAlreadyExists = false;
            }
            javafx.application.Platform.runLater(this::validateRealTime);
        });
    }

    private void checkDuplicateName(String name) {
        UiUtils.runAsync(() -> {
            try {
                Optional<Product> existing = productService.findByNameIgnoreCase(name.trim());
                if (existing.isPresent() && (product == null || product.getId() == null || !existing.get().getId().equals(product.getId()))) {
                    nameAlreadyExists = true;
                } else {
                    nameAlreadyExists = false;
                }
            } catch (Exception e) {
                log.warn("Erro ao verificar nome duplicado: {}", e.getMessage());
                nameAlreadyExists = false;
            }
            javafx.application.Platform.runLater(this::validateRealTime);
        });
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;
        boolean isService = isServiceCheckbox.isSelected();

        if (codeField.getText() == null || codeField.getText().isBlank()) {
            errors.append("Código é obrigatório. ");
            valid = false;
        } else if (codeAlreadyExists) {
            errors.append("Código já existe. ");
            valid = false;
        }

        if (nameField.getText() == null || nameField.getText().isBlank()) {
            errors.append("Nome é obrigatório. ");
            valid = false;
        } else if (nameAlreadyExists) {
            errors.append("Nome já existe. ");
            valid = false;
        }

        if (categoryCombo.getValue() == null) {
            errors.append("Categoria é obrigatória. ");
            valid = false;
        }

        if (priceSaleField.getText() == null || priceSaleField.getText().isBlank()) {
            errors.append("Preço de venda é obrigatório. ");
            valid = false;
        } else {
            java.math.BigDecimal val = parseBigDecimalOrZero(priceSaleField.getText());
            if (val.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                errors.append("Preço de venda deve ser maior que zero. ");
                valid = false;
            }
        }

        if (!isService && (unitField.getValue() == null)) {
            errors.append("Unidade base é obrigatória. ");
            valid = false;
        }

        if (priceCostField.getText() != null && !priceCostField.getText().isBlank()) {
            java.math.BigDecimal cost = parseBigDecimalOrZero(priceCostField.getText());
            if (cost.compareTo(java.math.BigDecimal.ZERO) < 0) {
                errors.append("Preço de custo não pode ser negativo. ");
                valid = false;
            }
        }

        if (!isService) {
            java.math.BigDecimal cost = parseBigDecimalOrZero(priceCostField.getText());
            java.math.BigDecimal sale = parseBigDecimalOrZero(priceSaleField.getText());
            if (cost.compareTo(java.math.BigDecimal.ZERO) > 0 && sale.compareTo(java.math.BigDecimal.ZERO) > 0
                    && sale.compareTo(cost) < 0) {
                errors.append("Preço de venda abaixo do preço de custo (margem negativa). ");
                valid = false;
            }
        }

        if (taxRateField.getText() != null && !taxRateField.getText().isBlank()) {
            java.math.BigDecimal tax = parseBigDecimalOrZero(taxRateField.getText());
            if (tax.compareTo(java.math.BigDecimal.ZERO) < 0 || tax.compareTo(new java.math.BigDecimal("100")) > 0) {
                errors.append("Taxa IVA deve estar entre 0% e 100%. ");
                valid = false;
            }
        }

        if (!isService) {
            java.math.BigDecimal min = parseBigDecimalOrZero(stockMinField.getText());
            java.math.BigDecimal max = parseBigDecimalOrZero(stockMaxField.getText());
            if (min.compareTo(java.math.BigDecimal.ZERO) >= 0 && max.compareTo(java.math.BigDecimal.ZERO) >= 0
                    && min.compareTo(max) > 0) {
                errors.append("Stock mínimo não pode ser maior que o stock máximo. ");
                valid = false;
            }
        }

        formValidProperty.set(valid);

        if (!valid) {
            showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios.");
        } else {
            hideError();
        }
    }

    public void setProduct(Product p) {
        this.product = p;
        if (p != null && p.getId() != null) {
            if (deleteButton != null) {
                deleteButton.setVisible(true);
                deleteButton.setManaged(true);
            }
            // Modo edição
            codeField.setText(p.getCode());
            nameField.setText(p.getName());
            categoryCombo.setValue(p.getCategory());
            supplierCombo.setValue(p.getSupplier());
            activeCheck.setSelected(Boolean.TRUE.equals(p.getIsActive()));
            isServiceCheckbox.setSelected(Boolean.TRUE.equals(p.getService()));
            applyServiceToggleState();

            priceCostField.setText(p.getPriceCost() != null ? java.math.BigDecimal.valueOf(p.getPriceCost()).toPlainString() : "0");
            priceSaleField.setText(p.getPriceSale() != null ? java.math.BigDecimal.valueOf(p.getPriceSale()).toPlainString() : "0");
            priceSaleBulkField.setText(p.getPriceSaleBulk() != null ? java.math.BigDecimal.valueOf(p.getPriceSaleBulk()).toPlainString() : "0");
            bulkQuantityField.setText(p.getBulkQuantity() != null ? java.math.BigDecimal.valueOf(p.getBulkQuantity()).toPlainString() : "0");
            
            // Calcula margem automaticamente
            calculateMarginFromPrices();
            
            taxRateField.setText(p.getTaxRate() != null ? java.math.BigDecimal.valueOf(p.getTaxRate()).toPlainString() : String.valueOf(defaultTaxRate));

            unitField.setValue(p.getUnit());
            unitBulkField.setValue(p.getUnitBulk());
            
            conversionFactorField.setText(p.getConversionFactor() != null ? java.math.BigDecimal.valueOf(p.getConversionFactor()).toPlainString() : "1.0");
            
            stockMinField.setText(p.getStockMin() != null ? java.math.BigDecimal.valueOf(p.getStockMin()).toPlainString() : "0");
            stockMaxField.setText(p.getStockMax() != null ? java.math.BigDecimal.valueOf(p.getStockMax()).toPlainString() : "0");
            
            descriptionArea.setText(p.getDescription() != null ? p.getDescription() : "");
            
            codeField.setDisable(true); // Não alterar código ao editar
            codeAlreadyExists = false;
            loadBarcodes();
        } else {
            // Modo novo produto
            this.product = new Product();
            product.setActive(true);
            activeCheck.setSelected(true);
            isServiceCheckbox.setSelected(false);
            applyServiceToggleState();
            codeField.clear();
            codeField.setDisable(false);
            nameField.clear();
            categoryCombo.setValue(null);
            supplierCombo.getSelectionModel().selectFirst();
            unitField.setValue(null);
            unitBulkField.setValue(null);
            priceCostField.clear();
            priceSaleField.clear();
            priceSaleBulkField.clear();
            profitMarginField.setText("0.00");
            stockMinField.setText("0");
            stockMaxField.setText("0");
            bulkQuantityField.setText("1");
            conversionFactorField.setText("1.0");
            barcodeList.clear();
            descriptionArea.setText("");
            
            // Define valores padrão
            taxRateField.setText(String.valueOf(defaultTaxRate));
            
            // Busca próximo número sequencial para o código
            try {
                nextSeqNumber = productService.findMaxId() + 1;
            } catch (Exception e) {
                nextSeqNumber = 1;
            }
            
            // Gera código automaticamente quando o nome é digitado
            nameField.textProperty().addListener((obs, o, n) -> {
                if (n != null && !n.isBlank()) {
                    String currentCode = codeField.getText();
                    if (currentCode == null || currentCode.isBlank()) {
                        String prefix = com.sgv.entity.Product.generatePrefix(n);
                        codeField.setText(com.sgv.entity.Product.generateCode(prefix, nextSeqNumber));
                    }
                }
            });
        }
        validateRealTime();
    }
    
    // generateAutoCode removido — usar Product.generateCode() estático

    private void doDelete() {
        if (product == null || product.getId() == null) return;

        javafx.concurrent.Task<Long> checkTask = new javafx.concurrent.Task<>() {
            @Override
            protected Long call() {
                return saleItemRepository.countByProductId(product.getId());
            }
        };
        checkTask.setOnSucceeded(e -> {
            long count = checkTask.getValue();
            if (count > 0) {
                javafx.application.Platform.runLater(() -> {
                    Alert warn = new Alert(Alert.AlertType.WARNING,
                        "Não é possível apagar este produto.\n\nO produto possui " + count + " registro(s) em vendas. " +
                        "Em vez disso, desative o produto na edição.");
                    warn.setTitle("Produto não pode ser apagado");
                    warn.setHeaderText(null);
                    warn.showAndWait();
                });
                return;
            }
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Tem certeza que deseja apagar este produto?", ButtonType.YES, ButtonType.NO);
                alert.setHeaderText(null);
                alert.setTitle("Confirmar Eliminação");
                java.util.Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.YES) return;

                javafx.concurrent.Task<Void> deleteTask = new javafx.concurrent.Task<>() {
                    @Override
                    protected Void call() {
                        productService.deleteById(product.getId());
                        return null;
                    }
                };
                deleteTask.setOnSucceeded(ev -> {
                    if (onSave != null) onSave.run();
                    Stage stage = (Stage) saveButton.getScene().getWindow();
                    stage.close();
                });
                deleteTask.setOnFailed(ev -> {
                    Throwable ex = deleteTask.getException();
                    systemLogService.logError("PRODUCT_DELETE_FAILED", "Erro ao apagar produto: " + ex.getMessage(), ex);
                    showError("Erro ao apagar: " + ex.getMessage());
                });
                UiUtils.runTask(deleteTask);
            });
        });
        UiUtils.runTask(checkTask);
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;

        validateRealTime();
        if (!formValidProperty.get()) return;

        String name = nameField.getText();
        if (name == null || name.isBlank()) { showError("Nome é obrigatório."); return; }

        if (categoryCombo.getValue() == null) { showError("Categoria é obrigatória."); return; }

        boolean isService = isServiceCheckbox.isSelected();
        if (!isService && unitField.getValue() == null) { showError("Unidade base é obrigatória."); return; }

        if (priceSaleField.getText() == null || priceSaleField.getText().isBlank()) { showError("Preço de venda é obrigatório."); return; }

        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                String code = codeField.getText();
                if (code == null || code.isBlank()) {
                    String prefix = com.sgv.entity.Product.generatePrefix(name);
                    code = com.sgv.entity.Product.generateCode(prefix, nextSeqNumber);
                }

                if (productService.findByCode(code.trim().toUpperCase()).isPresent()
                        && (product == null || product.getId() == null || !productService.findByCode(code.trim().toUpperCase()).get().getId().equals(product.getId()))) {
                    throw new IllegalArgumentException("Código '" + code + "' já existe.");
                }

                product.setCode(code.trim().toUpperCase());
                product.setName(name.trim());
                product.setCategory(categoryCombo.getValue());
                product.setSupplier(supplierCombo.getValue());
                product.setActive(activeCheck.isSelected());
                product.setService(isService);
                
                product.setPriceCost(parseBigDecimalOrZero(priceCostField.getText()).doubleValue());
                product.setPriceSale(parseBigDecimalOrZero(priceSaleField.getText()).doubleValue());
                product.setPriceSaleBulk(parseBigDecimalOrZero(priceSaleBulkField.getText()).doubleValue());
                product.setBulkQuantity(parseBigDecimalOrZero(bulkQuantityField.getText()).doubleValue());
                
                product.setTaxRate(parseBigDecimalOrZero(taxRateField.getText()).doubleValue());
                product.setIceRate(defaultIceRate);
                
                product.setDefaultTaxRate(defaultTaxRate);
                product.setDefaultIceRate(defaultIceRate);
                
                product.setUnit(unitField.getValue());
                product.setUnitBulk(unitBulkField.getValue());
                
                product.setConversionFactor(parseBigDecimalOrZero(conversionFactorField.getText()).doubleValue());
                
                product.setStockMin(parseBigDecimalOrZero(stockMinField.getText()).doubleValue());
                product.setStockMax(parseBigDecimalOrZero(stockMaxField.getText()).doubleValue());
                
                product.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");

                productService.saveProduct(product, barcodeList);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "PRODUTO_GRAVADO", "Artigo gravado com sucesso: " + nameField.getText());
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("PRODUCT_SAVE_FAILED", "Erro ao salvar produto: " + msg, ex);
            
            if (msg.contains("ConstraintViolationException") || msg.contains("DataIntegrityViolationException")) {
                showError("Código duplicado. Este produto já existe.");
            } else {
                showError("Erro ao guardar: " + msg);
            }
            hideSaveSpinner();
        });

        UiUtils.runTask(saveTask);
    }

    private BigDecimal parseBigDecimalOrZero(String text) {
        if (text == null || text.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.trim().replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
