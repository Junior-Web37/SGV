package com.sgv.desktop;

import com.sgv.entity.Category;
import com.sgv.entity.Product;
import com.sgv.entity.ProductBarcode;
import com.sgv.entity.Supplier;
import com.sgv.repository.CategoryRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.MetricUnitRepository;
import com.sgv.repository.SupplierRepository;
import com.sgv.repository.ProductBarcodeRepository;
import com.sgv.entity.MetricUnit;
import com.sgv.service.AppConfigService;
import com.sgv.service.SystemLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ProductFormController extends BaseFormController {

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
    @FXML private TextField iceRateField;
    
    @FXML private ComboBox<MetricUnit> unitField;
    @FXML private ComboBox<MetricUnit> unitBulkField;
    @FXML private TextField priceSaleBulkField;
    @FXML private TextField bulkQuantityField;
    
    @FXML private TextField stockMinField;
    @FXML private TextField stockMaxField;
    
    @FXML private TextField barcodeField;
    @FXML private TextField barcodeQtyField;
    @FXML private TableView<ProductBarcode> barcodeTable;
    @FXML private TableColumn<ProductBarcode, String> barcodeCodeColumn;
    @FXML private TableColumn<ProductBarcode, Double> barcodeQtyColumn;
    @FXML private Button addBarcodeButton;
    @FXML private Button removeBarcodeButton;
    
    @FXML private TextArea observationsField;
    @FXML private TextArea descriptionArea;
    @FXML private Button deleteButton;

    // ─── Repositórios e Serviços ────────────────────────────────────────────
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final AppConfigService appConfigService;
    private final SystemLogService systemLogService;
    private final com.sgv.repository.SaleItemRepository saleItemRepository;
    private final ProductBarcodeRepository productBarcodeRepository;

    // ─── Estado ─────────────────────────────────────────────────────────────
    private final ObservableList<ProductBarcode> barcodeList = FXCollections.observableArrayList();
    private Product product;
    private boolean codeAlreadyExists = false;
    private boolean updatingMargin = false;
    
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
                                 ProductBarcodeRepository productBarcodeRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.appConfigService = appConfigService;
        this.systemLogService = systemLogService;
        this.saleItemRepository = saleItemRepository;
        this.productBarcodeRepository = productBarcodeRepository;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        loadDefaultRates();
        
        categoryCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                categoryRepository.findAll()));
        supplierCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                supplierRepository.findAll()));
        supplierCombo.getItems().add(0, null);
        supplierCombo.getSelectionModel().selectFirst();
        
        unitField.setItems(javafx.collections.FXCollections.observableArrayList(
                metricUnitRepository.findAll()));
        unitBulkField.setItems(javafx.collections.FXCollections.observableArrayList(
                metricUnitRepository.findAll()));

        taxRateHint.setText("Padrão do sistema: " + defaultTaxRate + "%");
        
        taxRateField.setText(String.valueOf(defaultTaxRate));
        iceRateField.setText("0");
        
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "PRODUCT_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "PRODUCT_CANCEL");

        if (deleteButton != null) {
            deleteButton.setVisible(false);
            deleteButton.setManaged(false);
            UiUtils.attachSafe(deleteButton, this::doDelete, systemLogService, "PRODUCT_DELETE");
        }

        setupBarcodeTable();
        setupRealTimeValidation();
        setupMarginCalculation();
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
        if (product == null || product.getId() == null) return;
        String code = barcodeField.getText();
        if (code == null || code.isBlank()) return;
        double qty;
        try {
            qty = Double.parseDouble(barcodeQtyField.getText().trim().replace(",", "."));
        } catch (Exception e) {
            qty = 1.0;
        }
        ProductBarcode pb = new ProductBarcode();
        pb.setProduct(product);
        pb.setBarcode(code.trim());
        pb.setQuantity(qty);
        productBarcodeRepository.save(pb);
        loadBarcodes();
        barcodeField.clear();
    }

    private void removeBarcode() {
        ProductBarcode selected = barcodeTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == null) return;
        productBarcodeRepository.deleteById(selected.getId());
        loadBarcodes();
    }

    private void loadBarcodes() {
        if (product != null && product.getId() != null) {
            barcodeList.setAll(productBarcodeRepository.findByProductOrderByBarcode(product));
        } else {
            barcodeList.clear();
        }
    }

    private void setupRealTimeValidation() {
        // Verifica código duplicado
        codeField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) {
                checkDuplicateCode(n);
            } else {
                codeAlreadyExists = false;
                validateRealTime();
            }
        });

        // Valida nome
        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        
        // Valida categoria
        categoryCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        
        // Valida preço de venda
        priceSaleField.textProperty().addListener((obs, o, n) -> validateRealTime());

        UiUtils.applyNumericFormatter(priceCostField);
        UiUtils.applyNumericFormatter(priceSaleField);
        UiUtils.applyNumericFormatter(priceSaleBulkField);
        UiUtils.applyNumericFormatter(taxRateField);
        UiUtils.applyNumericFormatter(iceRateField);
        UiUtils.applyNumericFormatter(bulkQuantityField);
        UiUtils.applyNumericFormatter(stockMinField);
        UiUtils.applyNumericFormatter(stockMaxField);
        UiUtils.applyNumericFormatter(barcodeQtyField);

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
        new Thread(() -> {
            Optional<Product> existing = productRepository.findByCode(code.trim().toUpperCase());
            if (existing.isPresent() && (product == null || product.getId() == null || !existing.get().getId().equals(product.getId()))) {
                codeAlreadyExists = true;
            } else {
                codeAlreadyExists = false;
            }
            javafx.application.Platform.runLater(this::validateRealTime);
        }).start();
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (codeField.getText() == null || codeField.getText().isBlank()) {
            valid = false;
        } else if (codeAlreadyExists) {
            errors.append("Código já existe. ");
            valid = false;
        }

        if (nameField.getText() == null || nameField.getText().isBlank()) {
            valid = false;
        }

        if (categoryCombo.getValue() == null) {
            valid = false;
        }

        if (priceSaleField.getText() == null || priceSaleField.getText().isBlank()) {
            valid = false;
        } else {
            java.math.BigDecimal val = parseBigDecimalOrZero(priceSaleField.getText());
            if (val.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                errors.append("Preço de venda deve ser maior que zero. ");
                valid = false;
            }
        }

        if (priceCostField.getText() != null && !priceCostField.getText().isBlank()) {
            java.math.BigDecimal cost = parseBigDecimalOrZero(priceCostField.getText());
            if (cost.compareTo(java.math.BigDecimal.ZERO) < 0) {
                errors.append("Preço de custo não pode ser negativo. ");
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
            
            priceCostField.setText(p.getPriceCost() != null ? java.math.BigDecimal.valueOf(p.getPriceCost()).toPlainString() : "0");
            priceSaleField.setText(p.getPriceSale() != null ? java.math.BigDecimal.valueOf(p.getPriceSale()).toPlainString() : "0");
            priceSaleBulkField.setText(p.getPriceSaleBulk() != null ? java.math.BigDecimal.valueOf(p.getPriceSaleBulk()).toPlainString() : "0");
            bulkQuantityField.setText(p.getBulkQuantity() != null ? java.math.BigDecimal.valueOf(p.getBulkQuantity()).toPlainString() : "0");
            
            // Calcula margem automaticamente
            calculateMarginFromPrices();
            
            taxRateField.setText(p.getTaxRate() != null ? java.math.BigDecimal.valueOf(p.getTaxRate()).toPlainString() : String.valueOf(defaultTaxRate));
            iceRateField.setText(p.getIceRate() != null ? java.math.BigDecimal.valueOf(p.getIceRate()).toPlainString() : "0");
            
            unitField.setValue(p.getUnit());
            unitBulkField.setValue(p.getUnitBulk());
            
            stockMinField.setText(p.getStockMin() != null ? java.math.BigDecimal.valueOf(p.getStockMin()).toPlainString() : "0");
            stockMaxField.setText(p.getStockMax() != null ? java.math.BigDecimal.valueOf(p.getStockMax()).toPlainString() : "0");
            
            observationsField.setText(p.getObservations());
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
            stockMinField.setText("0");
            stockMaxField.setText("0");
            bulkQuantityField.setText("1");
            observationsField.setText("");
            descriptionArea.setText("");
            
            // Define valores padrão
            taxRateField.setText(String.valueOf(defaultTaxRate));
            iceRateField.setText(String.valueOf(defaultIceRate));
            
            // Gera código automaticamente a partir do nome
            codeField.textProperty().addListener((obs, o, n) -> {
                if (n != null && !n.isBlank() && (o == null || o.isBlank())) {
                    // Código será gerado ao salvar
                }
            });
        }
        validateRealTime();
    }
    
    /**
     * Gera código automático: 3 iniciais do nome + ID ou timestamp.
     */
    private String generateAutoCode(String name) {
        if (name == null || name.isBlank()) {
            return "PRD-" + (System.currentTimeMillis() % 100000);
        }
        
        // Remove acentos e pega as 3 primeiras letras
        String cleaned = name.toUpperCase()
                .replaceAll("[ÀÁÂÃÄÅ]", "A")
                .replaceAll("[ÈÉÊË]", "E")
                .replaceAll("[ÌÍÎÏ]", "I")
                .replaceAll("[ÒÓÔÕÖ]", "O")
                .replaceAll("[ÙÚÛÜ]", "U")
                .replaceAll("[Ç]", "C")
                .replaceAll("[Ñ]", "N")
                .replaceAll("[^A-Z0-9]", "");
        
        String prefix = cleaned.length() >= 3 ? cleaned.substring(0, 3) : cleaned;
        if (prefix.length() < 3) {
            prefix = String.format("%-3s", prefix).replace(' ', 'X');
        }
        
        // Se já tem ID, usa-o; senão usa timestamp
        if (product != null && product.getId() != null) {
            return prefix + "-" + String.format("%04d", product.getId() % 10000);
        }
        return prefix + "-" + (System.currentTimeMillis() % 100000);
    }

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
                        productRepository.deleteById(product.getId());
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
                new Thread(deleteTask).start();
            });
        });
        new Thread(checkTask).start();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;

        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                String code = codeField.getText();
                if (code == null || code.isBlank()) {
                    code = generateAutoCode(nameField.getText());
                }
                product.setCode(code.trim().toUpperCase());
                product.setName(nameField.getText().trim());
                product.setCategory(categoryCombo.getValue());
                product.setSupplier(supplierCombo.getValue());
                product.setActive(activeCheck.isSelected());
                product.setService(isServiceCheckbox.isSelected());
                
                product.setPriceCost(parseBigDecimalOrZero(priceCostField.getText()).doubleValue());
                product.setPriceSale(parseBigDecimalOrZero(priceSaleField.getText()).doubleValue());
                product.setPriceSaleBulk(parseBigDecimalOrZero(priceSaleBulkField.getText()).doubleValue());
                product.setBulkQuantity(parseBigDecimalOrZero(bulkQuantityField.getText()).doubleValue());
                
                product.recalculateProfitMargin();
                
                product.setTaxRate(parseBigDecimalOrZero(taxRateField.getText()).doubleValue());
                product.setIceRate(parseBigDecimalOrZero(iceRateField.getText()).doubleValue());
                
                product.setDefaultTaxRate(defaultTaxRate);
                product.setDefaultIceRate(defaultIceRate);
                
                product.setUnit(unitField.getValue());
                product.setUnitBulk(unitBulkField.getValue());
                
                product.setStockMin(parseBigDecimalOrZero(stockMinField.getText()).doubleValue());
                product.setStockMax(parseBigDecimalOrZero(stockMaxField.getText()).doubleValue());
                
                product.setObservations(observationsField.getText());
                product.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");

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
            String msg = ex != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("PRODUCT_SAVE_FAILED", "Erro ao salvar produto: " + msg, ex);
            
            if (msg.contains("ConstraintViolationException") || msg.contains("DataIntegrityViolationException")) {
                showError("Código duplicado. Este produto já existe.");
            } else {
                showError("Erro ao guardar: " + msg);
            }
            hideSaveSpinner();
        });

        new Thread(saveTask).start();
    }

    private java.math.BigDecimal parseBigDecimalOrZero(String text) {
        if (text == null || text.isBlank()) return java.math.BigDecimal.ZERO;
        try {
            return new java.math.BigDecimal(text.trim().replace(",", "."));
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }
    
    private String formatDouble(Double value) {
        if (value == null) return "0";
        return String.format("%.2f", value).replace(",", ".");
    }
}
