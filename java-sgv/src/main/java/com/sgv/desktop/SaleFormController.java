package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.BranchService;
import com.sgv.service.CategoryService;
import com.sgv.service.CustomerService;
import com.sgv.service.ProductService;
import com.sgv.service.StockBranchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.File;
import com.sgv.service.CashSessionService;
import com.sgv.service.SaleDocumentService;
import com.sgv.service.SystemLogService;
import com.sgv.service.ThermalPrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SaleFormController extends BaseFormController {

    private static final Logger log = LoggerFactory.getLogger(SaleFormController.class);

    @FXML private ComboBox<String> documentTypeCombo;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private ToggleButton retailModeToggle;
    @FXML private ToggleButton wholesaleModeToggle;
    @FXML private CheckBox diverseCustomerCheck;
    @FXML private ComboBox<Customer> customerCombo;

    @FXML private ComboBox<Category> productCategoryCombo;
    @FXML private ComboBox<Product> productSearchCombo;
    @FXML private TextField quantityField;
    @FXML private Button addItemButton;

    @FXML private TableView<SaleItem> itemsTable;
    @FXML private TableColumn<SaleItem, String> codeColumn;
    @FXML private TableColumn<SaleItem, String> productColumn;
    @FXML private TableColumn<SaleItem, String> quantityColumn;
    @FXML private TableColumn<SaleItem, String> priceColumn;
    @FXML private TableColumn<SaleItem, String> totalColumn;
    @FXML private TableColumn<SaleItem, Void> actionColumn;

    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private ComboBox<String> currencyCombo;

    @FXML private Label subtotalLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label headerTotalLabel;

    @FXML private VBox cashDrawerPane;
    @FXML private TextField receivedAmountField;
    @FXML private Label changeAmountLabel;
    @FXML private Button btnExactAmount;
    @FXML private Button btnPlus50;
    @FXML private Button btnPlus100;
    @FXML private Button btnPlus500;
    @FXML private Button btnPlus1000;
    @FXML private Button btnHoldCart;
    @FXML private Button btnRecallCart;

    @FXML private Button num0;
    @FXML private Button num1;
    @FXML private Button num2;
    @FXML private Button num3;
    @FXML private Button num4;
    @FXML private Button num5;
    @FXML private Button num6;
    @FXML private Button num7;
    @FXML private Button num8;
    @FXML private Button num9;
    @FXML private Button numClear;
    @FXML private Button numDot;

    private static final java.util.List<SaleItem> heldCartItems = new java.util.ArrayList<>();
    private static String heldCustomerName = null;

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockBranchService stockBranchService;
    private final SaleDocumentService saleDocumentService;
    private final ThermalPrintService thermalPrintService;
    private final SystemLogService systemLogService;
    private final CashSessionService cashSessionService;
    private final com.sgv.service.AppConfigService appConfigService;
    private final com.sgv.service.SaleService saleService;
    private final ProductService productService;
    private final BranchService branchService;
    private final CustomerService customerService;
    private final CategoryService categoryService;
    private final com.sgv.repository.ProductBarcodeRepository productBarcodeRepository;

    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private static final long ALL_CATEGORY_ID = Long.MIN_VALUE;
    private static final Category ALL_CATEGORIES = createAllCategories();
    private final Map<Long, Double> productPopularity = new HashMap<>();
    private final int POPULAR_PRODUCTS_LIMIT = 40;

    private Sale sale;
    private User currentUser;
    
    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty cashSessionValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);
    
    // Toggle group for pricing mode
    private final ToggleGroup modeGroup = new ToggleGroup();

    public SaleFormController(SaleRepository saleRepository,
                              SaleItemRepository saleItemRepository,
                              StockBranchService stockBranchService,
                              SaleDocumentService saleDocumentService,
                              ThermalPrintService thermalPrintService,
                              SystemLogService systemLogService,
                              CashSessionService cashSessionService,
                              com.sgv.service.SaleService saleService,
                              com.sgv.service.AppConfigService appConfigService,
                              com.sgv.repository.ProductBarcodeRepository productBarcodeRepository,
                              ProductService productService,
                              BranchService branchService,
                              CustomerService customerService,
                              CategoryService categoryService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockBranchService = stockBranchService;
        this.saleDocumentService = saleDocumentService;
        this.thermalPrintService = thermalPrintService;
        this.systemLogService = systemLogService;
        this.cashSessionService = cashSessionService;
        this.saleService = saleService;
        this.appConfigService = appConfigService;
        this.productBarcodeRepository = productBarcodeRepository;
        this.productService = productService;
        this.branchService = branchService;
        this.customerService = customerService;
        this.categoryService = categoryService;
    }

    /**
     * Define o utilizador autenticado — necessário para validar a sessão de
     * caixa antes de gravar a venda.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        refreshCashSessionAvailability();
    }

    /**
     * Devolve o formato AT-padrão (Moçambique, Decreto 7/2024) para o tipo de
     * documento:
     *  - "thermal-80mm" para Talão de Venda (TV) / Recibo (RC) / VENDA
     *  - "a4"           para Factura (FA) / Cotação / NC / ND / FACTURA
     *  - "a5"           nunca é o padrão, só via override
     */
    public static String atDefaultFormat(String documentType) {
        if (documentType == null) return "thermal-80mm";
        switch (documentType.toUpperCase()) {
            case "VENDA":
            case "RECIBO":
            case "RC":
            case "TV":
                return "thermal-80mm";
            case "COTACAO":
            case "COTACAO_ABERTA":
            case "FACTURA":
            case "FA":
            case "NC":
            case "ND":
            case "ENCOMENDA":
            case "ENCOMENDA_ABERTA":
                return "a4";
            default:
                return "thermal-80mm";
        }
    }

    @FXML
    public void initialize() {
        sale = null;
        currentUser = null;
        onSave = null;
        lastSelectedProduct = null;
        isRefreshingProducts = false;
        productPopularity.clear();
        cashSessionValidProperty.set(false);
        if (itemsTable != null) itemsTable.getItems().clear();
        categories.clear();
        initCommonFields();
        UiUtils.hardenComboBox(documentTypeCombo);
        UiUtils.hardenComboBox(branchCombo);
        UiUtils.hardenComboBox(customerCombo);
        UiUtils.hardenComboBox(productCategoryCombo);
        UiUtils.hardenComboBox(productSearchCombo);
        UiUtils.hardenComboBox(paymentMethodCombo);
        UiUtils.hardenComboBox(currencyCombo);
        
        UiUtils.applyNumericFormatter(quantityField);
        
        documentTypeCombo.setItems(FXCollections.observableArrayList(
            com.sgv.model.DocumentType.VENDA.name(),
            com.sgv.model.DocumentType.FACTURA.name(),
            com.sgv.model.DocumentType.COTACAO.name(),
            com.sgv.model.DocumentType.ENCOMENDA.name()
        ));
        documentTypeCombo.setValue(com.sgv.model.DocumentType.VENDA.name());
        documentTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFactura = com.sgv.model.DocumentType.FACTURA.name().equals(newVal);
            if (isFactura) {
                diverseCustomerCheck.setSelected(false);
                diverseCustomerCheck.setDisable(true);
                if (customerCombo.getItems().isEmpty()) {
                    customerCombo.setItems(FXCollections.observableArrayList(customerService.findAll()));
                }
            } else {
                diverseCustomerCheck.setDisable(false);
            }
        });
        
        branchCombo.setItems(FXCollections.observableArrayList(branchService.findAll()));
        if(!branchCombo.getItems().isEmpty()) branchCombo.getSelectionModel().selectFirst();
        branchCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            refreshCashSessionAvailability();
            reloadBranchStockCache();
            refreshPopularProducts();
            refreshProductSearchResults();
        });
        
        customerCombo.setItems(FXCollections.observableArrayList(customerService.findAll()));
        
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
            com.sgv.model.PaymentMethod.DINHEIRO.name(),
            com.sgv.model.PaymentMethod.MPESA.name(),
            com.sgv.model.PaymentMethod.EMOLA.name(),
            com.sgv.model.PaymentMethod.MKESH.name(),
            com.sgv.model.PaymentMethod.POS.name(),
            com.sgv.model.PaymentMethod.DEBITO.name(),
            com.sgv.model.PaymentMethod.TRANSFERENCIA.name(),
            com.sgv.model.PaymentMethod.CREDITO.name(),
            com.sgv.model.PaymentMethod.CHEQUE.name()
        ));
        paymentMethodCombo.setValue(com.sgv.model.PaymentMethod.DINHEIRO.name());
        
        currencyCombo.setItems(FXCollections.observableArrayList("MZN", "USD", "EUR", "ZAR"));
        currencyCombo.setValue("MZN");

        categories.setAll(categoryService.findAll().stream()
                .sorted(Comparator.comparing(Category::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList()));
        categories.add(0, ALL_CATEGORIES);
        productCategoryCombo.setItems(categories);
        productCategoryCombo.setValue(ALL_CATEGORIES);
        productCategoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshProductSearchResults());

        refreshPopularProducts();

        // Pricing Mode Toggle Logic
        retailModeToggle.setToggleGroup(modeGroup);
        wholesaleModeToggle.setToggleGroup(modeGroup);
        retailModeToggle.setSelected(true);
        
        modeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true); // Previne que nenhum fique selecionado
            } else {
                updateStylesForToggles();
                recalculateCartPrices();
            }
        });
        updateStylesForToggles();

        // Customer Logic
        diverseCustomerCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            customerCombo.setDisable(newVal);
            if(newVal) {
                customerCombo.setValue(null);
            }
        });
        customerCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && ("GROSSO".equalsIgnoreCase(newVal.getType()) || "ATACADO".equalsIgnoreCase(newVal.getType()))) {
                wholesaleModeToggle.setSelected(true);
            }
            validateRealTime();
        });

        // Table Columns
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("qty"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        setupActionColumn();

        // Product Search Logic
        setupProductSearch();
        refreshProductSearchResults();
        
        UiUtils.attachSafe(addItemButton, () -> handleProductSelection(resolveSelectedProduct()), systemLogService, "SALE_ADD_ITEM");
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "SALE_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "SALE_CANCEL");

        // Fix #6: Somente VENDA/FACTURA/RECIBO exigem sessão de caixa.
        // Cotações e Encomendas são documentos comerciais que não movimentam caixa.
        documentTypeCombo.valueProperty().addListener((obs, o, n) -> updateSaveButtonBinding());
        updateSaveButtonBinding();

        // Setup real-time listeners for styles & validation
        setupRealTimeValidation();
        
        // Setup Cash Drawer & Quick Cash Buttons
        setupCashDrawerAndShortcuts();

        // Auto-foco no campo de pesquisa ao abrir (pronto para venda ou scan)
        Platform.runLater(() -> {
            productSearchCombo.requestFocus();
            refreshCashSessionAvailability();
            setupGlobalShortcuts();
        });
    }
    
    private static Category createAllCategories() {
        Category all = new Category();
        all.setId(ALL_CATEGORY_ID);
        all.setName("Todos");
        return all;
    }

    private void setupRealTimeValidation() {
        // Observers for real-time validation
        branchCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateRealTime());
        customerCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateRealTime());
        diverseCustomerCheck.selectedProperty().addListener((obs, oldVal, newVal) -> validateRealTime());
        documentTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> validateRealTime());
        
        // Trigger initial validation
        Platform.runLater(this::validateRealTime);
    }
    
    @Override
    protected void validateRealTime() {
        boolean isValid = true;
        StringBuilder errors = new StringBuilder();
        
        // Validar Filial
        if (branchCombo.getValue() == null) {
            branchCombo.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-color: #FEE2E2;");
            isValid = false;
        } else {
            branchCombo.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 4; -fx-background-color: #F8FAFC;");
        }
        
        // Validar Cliente
        if (!diverseCustomerCheck.isSelected() && customerCombo.getValue() == null) {
            customerCombo.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2; -fx-border-radius: 4; -fx-background-color: #FEE2E2;");
            isValid = false;
        } else {
            customerCombo.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 4; -fx-background-color: #F8FAFC;");
        }
        
        // Validar Carrinho
        if (itemsTable.getItems() == null || itemsTable.getItems().isEmpty()) {
            isValid = false;
        }
        
        // Validações específicas por tipo de documento
        String dtVal = documentTypeCombo.getValue();
        if (com.sgv.model.DocumentType.FACTURA.name().equals(dtVal)) {
            if (diverseCustomerCheck.isSelected()) {
                errors.append("Factura (FA) requer um cliente com NUIT. Desactive 'Cliente Diverso'. ");
                isValid = false;
            } else {
                Customer c = customerCombo.getValue();
                if (c != null) {
                    String nuit = c.getNuit();
                    if (nuit == null || nuit.isBlank()) {
                        errors.append("Cliente não tem NUIT preenchido. ");
                        isValid = false;
                    } else if (!com.sgv.util.NuitValidator.isValid(nuit)) {
                        errors.append("NUIT do cliente inválido. ");
                        isValid = false;
                    }
                }
            }
        }

        // Validação de Venda a Crédito e Limite de Crédito
        if (paymentMethodCombo != null && "CREDITO".equalsIgnoreCase(paymentMethodCombo.getValue())) {
            if (diverseCustomerCheck.isSelected()) {
                errors.append("Venda a crédito requer um cliente registado. ");
                isValid = false;
            } else {
                Customer c = customerCombo.getValue();
                if (c != null) {
                    BigDecimal limit = c.getCreditLimitAmount();
                    if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
                        double currentTotal = calculateCurrentTotal();
                        BigDecimal curBal = c.getBalanceAmount() != null ? c.getBalanceAmount() : BigDecimal.ZERO;
                        if (curBal.add(BigDecimal.valueOf(currentTotal)).compareTo(limit) > 0) {
                            errors.append(String.format("Limite de crédito excedido (Máx: %,.2f MT | Dívida atual: %,.2f MT). ", limit.doubleValue(), curBal.doubleValue()));
                            isValid = false;
                        }
                    }
                }
            }
        }
        
        // Atualizar estado reativo (MVVM Data Binding)
        formValidProperty.set(isValid);
        
        // Mostrar/ocultar erro
        if (isValid) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else if (errors.length() > 0) {
            showError(errors.toString().trim());
        } else {
            showError("Preencha filial, cliente e adicione pelo menos um produto ao carrinho.");
        }
    }
    
    private void updateStylesForToggles() {
        if (retailModeToggle.isSelected()) {
            retailModeToggle.setStyle("-fx-padding:6 16; -fx-background-color:#2563EB; -fx-text-fill:#ffffff; -fx-font-weight:700; -fx-background-radius:3 0 0 3; -fx-cursor:hand;");
            wholesaleModeToggle.setStyle("-fx-padding:6 16; -fx-background-color:#F8FAFC; -fx-text-fill:#94A3B8; -fx-font-weight:600; -fx-background-radius:0 3 3 0; -fx-border-color:#E2E8F0; -fx-border-width:1 1 1 0; -fx-cursor:hand;");
        } else {
            wholesaleModeToggle.setStyle("-fx-padding:6 16; -fx-background-color:#2563EB; -fx-text-fill:#ffffff; -fx-font-weight:700; -fx-background-radius:0 3 3 0; -fx-cursor:hand;");
            retailModeToggle.setStyle("-fx-padding:6 16; -fx-background-color:#F8FAFC; -fx-text-fill:#94A3B8; -fx-font-weight:600; -fx-background-radius:3 0 0 3; -fx-border-color:#E2E8F0; -fx-border-width:1 0 1 1; -fx-cursor:hand;");
        }
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("X");
            {
                btn.setStyle("-fx-background-color:transparent; -fx-text-fill:#EF4444; -fx-font-weight:900; -fx-cursor:hand;");
                btn.setOnAction(UiUtils.safeOnAction(() -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    itemsTable.getItems().remove(item);
                    updateTotals();
                }, systemLogService, "SALE_REMOVE_ITEM"));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    public void setSale(Sale s) {
        this.sale = s;
        if (s != null && s.getId() != null) {
            // Edição de documento existente
            documentTypeCombo.setValue(s.getDocumentType() != null ? s.getDocumentType() : "VENDA");
            branchCombo.setValue(s.getBranch());
            paymentMethodCombo.setValue(s.getPaymentMethod());
            currencyCombo.setValue(s.getCurrency() != null ? s.getCurrency() : "MZN");
            
            if (s.getCustomer() != null) {
                diverseCustomerCheck.setSelected(false);
                customerCombo.setValue(s.getCustomer());
            } else if (s.getCustomerName() != null && !s.getCustomerName().isBlank()) {
                diverseCustomerCheck.setSelected(true);
            } else {
                diverseCustomerCheck.setSelected(true);
            }
            
            itemsTable.setItems(FXCollections.observableArrayList(s.getItems() != null ? s.getItems() : java.util.List.of()));
            branchCombo.setDisable(true);
            documentTypeCombo.setDisable(true);
        } else if (s != null && s.getId() == null && s.getItems() != null && !s.getItems().isEmpty()) {
            // Novo documento pré-preenchido (ex: Cotação convertida em Fatura)
            this.sale = s;
            documentTypeCombo.setValue(s.getDocumentType() != null ? s.getDocumentType() : "FACTURA");
            if (s.getBranch() != null) branchCombo.setValue(s.getBranch());
            if (s.getPaymentMethod() != null) paymentMethodCombo.setValue(s.getPaymentMethod());
            currencyCombo.setValue(s.getCurrency() != null ? s.getCurrency() : "MZN");
            
            // Pré-selecionar cliente se disponível
            if (s.getCustomer() != null) {
                diverseCustomerCheck.setSelected(false);
                customerCombo.setValue(s.getCustomer());
            } else {
                diverseCustomerCheck.setSelected(true);
            }
            
            itemsTable.setItems(FXCollections.observableArrayList(s.getItems()));
        } else {
            this.sale = new Sale();
            itemsTable.setItems(FXCollections.observableArrayList());
        }
        updateTotals();
    }

    public void setDocumentType(String type) {
        if (type != null && documentTypeCombo != null) {
            documentTypeCombo.setValue(type);
        }
    }

    public void setOnSave(Runnable callback) {
        this.onSave = callback;
    }

    private ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;
    private final ObservableList<Product> comboDisplayList = FXCollections.observableArrayList();
    private final Map<Long, BigDecimal> branchStockCache = new HashMap<>();
    private boolean isRefreshingProducts = false;
    private Product lastSelectedProduct = null;
    private volatile boolean enterProcessingGuard = false;

    private void setupProductSearch() {
        allProducts.setAll(productService.findAllActive());
        filteredProducts = new FilteredList<>(allProducts, p -> true);
        reloadBranchStockCache();
        
        productSearchCombo.setItems(comboDisplayList);

        productSearchCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof Product p) {
                lastSelectedProduct = p;
            }
        });
        
        productSearchCombo.setConverter(new StringConverter<Product>() {
            @Override
            public String toString(Product p) {
                return p == null ? "" : productDisplayText(p);
            }
            @Override
            public Product fromString(String string) {
                return findBestMatchingProduct(string);
            }
        });

        productSearchCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(productDisplayText(p));
                if (!isProductAvailableInBranch(p, branchCombo != null ? branchCombo.getValue() : null)) {
                    setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                } else {
                    setStyle("");
                }
            }
        });
        productSearchCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText("");
                    return;
                }
                setText(productDisplayText(p));
            }
        });
        
        UiUtils.setupDebounce(productSearchCombo.getEditor(), () -> {
            if (isRefreshingProducts) return;
            if (productSearchCombo.isShowing()) return;
            String currentText = productSearchCombo.getEditor().getText() != null
                    ? productSearchCombo.getEditor().getText().trim() : "";
            Product selected = productSearchCombo.getSelectionModel().getSelectedItem();
            if (selected != null && productDisplayText(selected).equals(currentText)) {
                return;
            }
            if (selected != null && !productDisplayText(selected).equals(currentText)) {
                if (currentText.length() < 60) {
                    productSearchCombo.getSelectionModel().clearSelection();
                } else {
                    return;
                }
            }
            refreshProductSearchResults();
            if (!productSearchCombo.isShowing() && productSearchCombo.isFocused()) {
                productSearchCombo.show();
            }
        }, 180);

        productSearchCombo.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (enterProcessingGuard) { e.consume(); return; }
                enterProcessingGuard = true;
                try {
                    String text = productSearchCombo.getEditor().getText().trim();
                    if (tryAddByBarcode(text)) {
                        e.consume();
                        return;
                    }
                    handleProductSelection(resolveSelectedProduct());
                    e.consume();
                } finally {
                    enterProcessingGuard = false;
                }
            }
        });

        // Deteção de barcode por caracteres rápidos (scanner USB emit characters in ~50ms)
        setupBarcodeAutoDetection();

        // Interceta ENTER no popup do ComboBox (dropdown aberto)
        productSearchCombo.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin instanceof javafx.scene.control.skin.ComboBoxListViewSkin) {
                javafx.scene.control.skin.ComboBoxListViewSkin<?> skin =
                    (javafx.scene.control.skin.ComboBoxListViewSkin<?>) newSkin;
                javafx.scene.control.ListView<?> listView =
                    (javafx.scene.control.ListView<?>) skin.getPopupContent();
                listView.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                    if (ev.getCode() == KeyCode.ENTER) {
                        if (enterProcessingGuard) { ev.consume(); return; }
                        enterProcessingGuard = true;
                        try {
                            String text = productSearchCombo.getEditor().getText().trim();
                            if (tryAddByBarcode(text)) {
                                ev.consume();
                                return;
                            }
                            Product selected = (Product) listView.getSelectionModel().getSelectedItem();
                            if (selected != null) {
                                handleProductSelection(selected);
                                ev.consume();
                            }
                        } finally {
                            enterProcessingGuard = false;
                        }
                    }
                });
            }
        });

        quantityField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleProductSelection(resolveSelectedProduct());
            }
        });
    }

    private Product resolveSelectedProduct() {
        // Layer 1: ComboBox value property (persists across list changes)
        Product val = productSearchCombo.getValue();
        if (val != null) return val;

        // Layer 2: Selection model
        Product selected = productSearchCombo.getSelectionModel().getSelectedItem();
        if (selected != null) return selected;

        // Layer 3: Last tracked selection (valueProperty listener)
        if (lastSelectedProduct != null) {
            String editorText = productSearchCombo.getEditor() != null ? productSearchCombo.getEditor().getText().trim() : "";
            if (!editorText.isBlank() && productDisplayText(lastSelectedProduct).equals(editorText)) {
                return lastSelectedProduct;
            }
        }

        // Layer 4: Text search in allProducts (handles exact matches only)
        String text = productSearchCombo.getEditor() != null ? productSearchCombo.getEditor().getText().trim() : "";
        if (!text.isBlank()) {
            // Layer 5: Extract code from display text "CODE - NAME (PRICE MT)"
            String extractedCode = extractCodeFromDisplayText(text);
            String searchTarget = extractedCode != null ? extractedCode : text;

            selected = allProducts.stream()
                    .filter(p -> p != null)
                    .filter(p -> (p.getCode() != null && p.getCode().equalsIgnoreCase(searchTarget)) ||
                                 (p.getName() != null && p.getName().equalsIgnoreCase(searchTarget)))
                    .findFirst()
                    .orElse(null);
            if (selected != null) return selected;
        }

        log.debug("resolveSelectedProduct: null — editor='{}', value={}, selected={}, lastTracked={}",
                text,
                productSearchCombo.getValue(),
                productSearchCombo.getSelectionModel().getSelectedItem(),
                lastSelectedProduct);
        return null;
    }

    private String extractCodeFromDisplayText(String text) {
        if (text == null) return null;
        // Format: "CODE - NAME (PRICE MT)" or "CODE - NAME (PRICE MT) - Fora de estoque (X)"
        int dashIdx = text.indexOf(" - ");
        if (dashIdx > 0) {
            return text.substring(0, dashIdx).trim();
        }
        return null;
    }

    private int compareProductsForSearch(Product a, Product b) {
        if (a == null || b == null) return a == b ? 0 : a == null ? 1 : -1;
        Branch branch = branchCombo != null ? branchCombo.getValue() : null;
        int aAvail = isProductAvailableInBranch(a, branch) ? 1 : 0;
        int bAvail = isProductAvailableInBranch(b, branch) ? 1 : 0;
        int cmp = Integer.compare(bAvail, aAvail);
        if (cmp != 0) return cmp;
        double aScore = productPopularity.getOrDefault(a.getId(), 0.0);
        double bScore = productPopularity.getOrDefault(b.getId(), 0.0);
        cmp = Double.compare(bScore, aScore);
        if (cmp != 0) return cmp;
        return Comparator.comparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Product::getCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .compare(a, b);
    }

    private String productDisplayText(Product p) {
        if (p == null) return "";
        String price = p.getPriceSale() != null ? String.format("%.2f", p.getPriceSale()) : "0.00";
        String availableText = "";
        Branch branch = branchCombo != null ? branchCombo.getValue() : null;
        if (!isProductAvailableInBranch(p, branch)) {
            Optional<StockBranch> stock = getStockForBranch(p, branch);
            BigDecimal available = stock.map(sb -> sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO).orElse(BigDecimal.ZERO);
            availableText = " - Fora de estoque (" + String.format("%.2f", available.doubleValue()) + ")";
        }
        return p.getCode() + " - " + p.getName() + " (" + price + " MT)" + availableText;
    }

    private void refreshProductSearchResults() {
        if (isRefreshingProducts) return;
        isRefreshingProducts = true;
        try {
            String search = productSearchCombo.getEditor() != null ? productSearchCombo.getEditor().getText() : null;
            Category category = productCategoryCombo != null ? productCategoryCombo.getValue() : null;
            Branch branch = branchCombo != null ? branchCombo.getValue() : null;

            boolean emptySearch = search == null || search.isBlank();
            List<Long> topIds = allProducts.stream()
                    .filter(p -> categoryMatches(p, category))
                    .sorted(Comparator.comparingInt((Product p) -> isProductAvailableInBranch(p, branch) ? 0 : 1)
                            .thenComparingDouble((Product p) -> productPopularity.getOrDefault(p.getId(), 0.0)).reversed()
                            .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .limit(POPULAR_PRODUCTS_LIMIT)
                    .map(Product::getId)
                    .collect(Collectors.toList());

            filteredProducts.setPredicate(p -> {
                if (!categoryMatches(p, category)) return false;
                if (emptySearch) {
                    return topIds.contains(p.getId());
                }
                return productMatchesSearch(p, search);
            });

            List<Product> snapshot = filteredProducts.stream()
                    .sorted(this::compareProductsForSearch)
                    .collect(Collectors.toList());

            Product previousSelection = productSearchCombo.getValue();

            if (productSearchCombo.isShowing()) {
                productSearchCombo.hide();
            }
            comboDisplayList.setAll(snapshot);

            if (previousSelection != null) {
                boolean stillInList = snapshot.stream()
                        .anyMatch(p -> p.getId().equals(previousSelection.getId()));
                if (stillInList) {
                    productSearchCombo.getSelectionModel().select(previousSelection);
                }
            }

            if (!emptySearch && !productSearchCombo.isShowing() && productSearchCombo.isFocused()) {
                productSearchCombo.show();
            }
        } finally {
            isRefreshingProducts = false;
        }
    }

    private boolean categoryMatches(Product p, Category category) {
        if (category == null || category.getId() == null || category.getId().equals(ALL_CATEGORY_ID)) {
            return true;
        }
        return p.getCategory() != null && Objects.equals(p.getCategory().getId(), category.getId());
    }

    private boolean productMatchesSearch(Product p, String search) {
        if (search == null || search.isBlank()) return true;
        String lower = search.toLowerCase();
        return (p.getCode() != null && p.getCode().toLowerCase().contains(lower)) ||
               (p.getName() != null && p.getName().toLowerCase().contains(lower)) ||
               (p.getCategory() != null && p.getCategory().getName() != null && p.getCategory().getName().toLowerCase().contains(lower));
    }

    private boolean branchMatchesStock(Product p, Branch branch) {
        return isProductAvailableInBranch(p, branch);
    }

    private void reloadBranchStockCache() {
        branchStockCache.clear();
        Branch branch = branchCombo != null ? branchCombo.getValue() : null;
        if (branch == null || branch.getId() == null || allProducts.isEmpty()) return;
        List<Long> ids = allProducts.stream().map(Product::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) return;
        for (StockBranch sb : stockBranchService.loadStockForBranchAndProducts(branch.getId(), ids)) {
            if (sb.getProduct() != null && sb.getProduct().getId() != null) {
                branchStockCache.put(sb.getProduct().getId(),
                        sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO);
            }
        }
    }

    private BigDecimal cachedStock(Product p) {
        if (p == null || p.getId() == null) return BigDecimal.ZERO;
        return branchStockCache.getOrDefault(p.getId(), BigDecimal.ZERO);
    }

    private boolean isProductAvailableInBranch(Product p, Branch branch) {
        if (branch == null || p == null) return true;
        if (Boolean.TRUE.equals(p.getService())) return true;
        return cachedStock(p).compareTo(BigDecimal.ZERO) > 0;
    }

    private Optional<StockBranch> getStockForBranch(Product p, Branch branch) {
        if (p == null || p.getId() == null || branch == null || branch.getId() == null) {
            return Optional.empty();
        }
        BigDecimal current = cachedStock(p);
        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        StockBranch sb = new StockBranch();
        sb.setProduct(p);
        sb.setBranch(branch);
        sb.setStockCurrentAmount(current);
        return Optional.of(sb);
    }

    private boolean validateProductQuantityForBranch(Product product, double requestedQty) {
        Branch branch = branchCombo != null ? branchCombo.getValue() : null;
        if (product == null || requestedQty <= 0 || branch == null) return true;
        if (Boolean.TRUE.equals(product.getService())) return true;

        BigDecimal available = stockBranchService.getCurrentStock(branch, product);
        BigDecimal existingQty = BigDecimal.valueOf(itemsTable.getItems().stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getId() != null && i.getProduct().getId().equals(product.getId()))
                .mapToDouble(i -> i.getQty() != null ? i.getQty() : 0.0)
                .sum());

        if (available.compareTo(existingQty.add(BigDecimal.valueOf(requestedQty))) < 0) {
            showError("Estoque insuficiente para " + product.getCode() + " na filial " + branch.getName() + ". Disponível: " + String.format("%.2f", available.doubleValue()));
            return false;
        }
        return true;
    }

    private boolean validateSaleStock() {
        Branch branch = branchCombo != null ? branchCombo.getValue() : null;
        if (branch == null) return true;
        for (SaleItem item : itemsTable.getItems()) {
            Product product = item.getProduct();
            if (product == null || Boolean.TRUE.equals(product.getService())) continue;
            Optional<StockBranch> stock = getStockForBranch(product, branch);
            BigDecimal available = stock.map(sb -> sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO).orElse(BigDecimal.ZERO);
            BigDecimal qty = BigDecimal.valueOf(item.getQty() != null ? item.getQty() : 0.0);
            if (available.compareTo(qty) < 0) {
                showError("Estoque insuficiente para " + product.getCode() + " na filial " + branch.getName() + ". Disponível: " + String.format("%.2f", available.doubleValue()));
                return false;
            }
        }
        return true;
    }

    private int searchMatchPriority(Product p, String text, String lower) {
        if (p == null) return Integer.MAX_VALUE;
        if (p.getCode() != null && p.getCode().equalsIgnoreCase(text)) return 1;
        if (p.getName() != null && p.getName().equalsIgnoreCase(text)) return 2;
        if (p.getCode() != null && p.getCode().toLowerCase().startsWith(lower)) return 3;
        if (p.getName() != null && p.getName().toLowerCase().startsWith(lower)) return 4;
        if (p.getName() != null && p.getName().toLowerCase().contains(lower)) return 5;
        if (p.getCode() != null && p.getCode().toLowerCase().contains(lower)) return 6;
        if (p.getCategory() != null && p.getCategory().getName() != null && p.getCategory().getName().toLowerCase().contains(lower)) return 7;
        return 100;
    }

    private Product findBestMatchingProduct(String text) {
        if (text == null || text.isBlank()) return null;

        // Extract code from display text "CODE - NAME (PRICE MT)"
        String extractedCode = extractCodeFromDisplayText(text);
        String searchTarget = extractedCode != null ? extractedCode : text.trim();

        return allProducts.stream()
                .filter(p -> p != null)
                .filter(p -> (p.getCode() != null && p.getCode().equalsIgnoreCase(searchTarget)) ||
                             (p.getName() != null && p.getName().equalsIgnoreCase(searchTarget)))
                .findFirst()
                .orElse(null);
    }

    private void refreshPopularProducts() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(90).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();
        Branch branch = branchCombo != null ? branchCombo.getValue() : null;

        productPopularity.clear();
        if (true) {
            Long branchId = branch != null ? branch.getId() : null;
            try {
                List<Object[]> rows = saleItemRepository.findTopSellingProductsToday(
                        from, to, branchId, org.springframework.data.domain.PageRequest.of(0, 80));
                Map<String, Product> byCode = allProducts.stream()
                        .filter(p -> p.getCode() != null)
                        .collect(Collectors.toMap(Product::getCode, p -> p, (a, b) -> a));
                for (Object[] row : rows) {
                    if (row == null || row.length < 3) continue;
                    String code = row[0] != null ? row[0].toString() : null;
                    Number qty = row[2] instanceof Number n ? n : null;
                    Product p = code != null ? byCode.get(code) : null;
                    if (p != null && p.getId() != null && qty != null) {
                        productPopularity.merge(p.getId(), qty.doubleValue(), Double::sum);
                    }
                }
            } catch (Exception ex) {
                log.debug("Popularidade de produtos ignorada: {}", ex.getMessage());
            }
            return;
        }
        List<SaleItem> items = saleItemRepository.findBySaleDateRange(from, to);
        for (SaleItem item : items) {
            if (item.getProduct() == null || item.getProduct().getId() == null) continue;
            if (item.getSale() == null) continue;
            Sale sale = item.getSale();
            if (sale.getState() == null) continue;
            if (Boolean.TRUE.equals(sale.getDemoFlag())) continue;
            if ("ANULADA".equalsIgnoreCase(sale.getState())) continue;
            if (!"EMITIDA".equalsIgnoreCase(sale.getState()) && !"PAGO".equalsIgnoreCase(sale.getState())) continue;

            if (branch != null && sale.getBranch() != null && !Objects.equals(branch.getId(), sale.getBranch().getId())) {
                continue;
            }

            String docType = sale.getDocumentType();
            if (docType != null) {
                String upperType = docType.toUpperCase();
                if (upperType.startsWith("COTACAO") || upperType.startsWith("ENCOMENDA")) {
                    continue;
                }
            }

            double qty = item.getQty() != null ? item.getQty() : 0.0;
            productPopularity.merge(item.getProduct().getId(), qty, Double::sum);
        }
    }

    /**
     * Tenta encontrar um produto pelo barcode e adicioná-lo automaticamente ao carrinho.
     * Retorna true se o barcode foi reconhecido e o produto adicionado, false caso contrário.
     */
    private boolean tryAddByBarcode(String text) {
        if (text == null || text.isBlank() || text.length() < 3) return false;
        try {
            // 1. Pesquisa na tabela product_barcodes
            com.sgv.entity.ProductBarcode pb = productBarcodeRepository.findByBarcode(text)
                    .orElse(productBarcodeRepository.findByBarcodeIgnoreCase(text).orElse(null));

            if (pb != null && pb.getProduct() != null) {
                Product product = pb.getProduct();
                double qty = pb.getQuantity() != null ? pb.getQuantity() : 1.0;
                // Usar quantidade do field se foi digitada manualmente
                try {
                    String qtyText = quantityField.getText().trim();
                    if (!qtyText.isBlank()) {
                        qty = Double.parseDouble(qtyText.replace(",", "."));
                    }
                } catch (NumberFormatException ignored) {}
                handleProductSelectionWithQty(product, qty);
                // Limpar campo de pesquisa após adicionar
                productSearchCombo.getEditor().clear();
                productSearchCombo.setValue(null);
                productSearchCombo.getEditor().requestFocus();
                return true;
            }

            // 2. Fallback: pesquisar pelo código do produto diretamente
            Product byCode = productService.findByBarcodeOrCode(text).orElse(null);
            if (byCode != null) {
                double qty = 1.0;
                try {
                    String qtyText = quantityField.getText().trim();
                    if (!qtyText.isBlank()) {
                        qty = Double.parseDouble(qtyText.replace(",", "."));
                    }
                } catch (NumberFormatException ignored) {}
                handleProductSelectionWithQty(byCode, qty);
                productSearchCombo.getEditor().clear();
                productSearchCombo.setValue(null);
                productSearchCombo.getEditor().requestFocus();
                return true;
            }
        } catch (Exception e) {
            log.warn("[BARCODE] Erro ao pesquisar barcode '{}': {}", text, e.getMessage());
            showError("Erro ao pesquisar código de barras: " + e.getMessage());
            return true;
        }
        return false;
    }

    // Estado para deteção de scanner de barcode (digitação rápida)
    private long lastKeyTime = 0;
    private int rapidKeyCount = 0;
    private static final int BARCODE_MIN_LENGTH = 4;
    private static final long BARCODE_MAX_GAP_MS = 60; // scanners digitam < 60ms entre chars

    /**
     * Configura deteção automática de barcode por velocidade de digitação.
     * Scanners USB digitam todo o barcode em < 60ms por caracter, seguido de ENTER.
     * Se a velocidade for de scanner, adiciona automaticamente.
     */
    private void setupBarcodeAutoDetection() {
        productSearchCombo.getEditor().setOnKeyReleased(e -> {
            long now = System.currentTimeMillis();
            long gap = now - lastKeyTime;
            lastKeyTime = now;

            if (e.getCode() == KeyCode.ENTER) {
                // ENTER emitido pelo scanner — já tratado pelo setOnKeyPressed, ignorar aqui
                rapidKeyCount = 0;
                return;
            }

            if (gap < BARCODE_MAX_GAP_MS) {
                rapidKeyCount++;
            } else {
                rapidKeyCount = 1; // Reiniciar contagem se o gap foi lento (digitação humana)
            }

            // Se acumulou caracteres suficientes em velocidade de scanner, aguardar ENTER
            // A deteção final acontece no listener de ENTER do setOnKeyPressed
        });
    }

    /**
     * Versão do handleProductSelection que aceita quantidade diretamente (para barcode).
     */
    private void handleProductSelectionWithQty(Product product, double qty) {
        String originalText = quantityField.getText();
        quantityField.setText(String.valueOf(qty));
        handleProductSelection(product);
        quantityField.setText(originalText);
    }

    private void handleProductSelection(Product product) {
        errorLabel.setVisible(false);
        
        if (product == null) {
            showError("Produto não encontrado.");
            return;
        }
        
        // ─── VERIFICAÇÃO: Produto inactivo não pode ser vendido ───────────────
        if (!product.canBeSold()) {
            showError("Produto \"" + product.getCode() + " - " + product.getName() + "\" está INACTIVO e não pode ser vendido.\n\nActive o produto primeiro no cadastro.");
            productSearchCombo.setValue(null);
            productSearchCombo.getEditor().clear();
            return;
        }
        
        double qty = 1.0;
        try {
            qty = Double.parseDouble(quantityField.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            qty = 1.0;
        }
        // Fix #10: Bloquear quantidade zero ou negativa
        if (qty <= 0) {
            showError("A quantidade deve ser superior a zero.");
            quantityField.requestFocus();
            return;
        }
        
        boolean isWholesale = wholesaleModeToggle.isSelected();
        if (isWholesale) {
            if (product.getPriceSaleBulk() == null || product.getPriceSaleBulk() <= 0) {
                showError("Produto \"" + product.getName() + "\" não tem preço de grosso configurado.");
                return;
            }
            double bulkMin = product.getBulkQuantity() != null ? product.getBulkQuantity() : 0.0;
            if (bulkMin > 0 && qty < bulkMin) {
                showError("Preço de grosso só se aplica a partir de " + (int)bulkMin + " unidades.");
                return;
            }
        }
        double unitPrice = isWholesale ? product.getPriceSaleBulk() : product.getPriceSale();
        if (unitPrice <= 0) {
            showError("Erro: Preço de venda inválido (0,00 MT). Configure o preço no cadastro do produto.");
            return;
        }
                
        if (!validateProductQuantityForBranch(product, qty)) {
            return;
        }

        // Verificamos se já existe no carrinho para somar quantidade
        boolean found = false;
        for (SaleItem item : itemsTable.getItems()) {
            if (item.getProduct() != null && item.getProduct().getId().equals(product.getId())) {
                item.setQty(item.getQty() + qty);
                
                double lineBase = item.getQty() * item.getUnitPrice();
                double lineIce = lineBase * (item.getIceRate() != null ? item.getIceRate() / 100.0 : 0.0);
                double lineTax = (lineBase + lineIce) * (item.getTaxRate() != null ? item.getTaxRate() / 100.0 : 0.0);
                
                item.setLineBase(lineBase);
                item.setLineIce(lineIce);
                item.setLineTax(lineTax);
                item.setLineTotal(lineBase + lineIce + lineTax);
                
                found = true;
                break;
            }
        }
        
        if (!found) {
            SaleItem item = new SaleItem();
            item.setProduct(product);
            item.setProductCode(product.getCode());
            String desc = product.getDescription();
            item.setDescription(desc != null && !desc.isBlank() ? desc : product.getName());
            item.setQty(qty);
            item.setUnitPrice(unitPrice);
            item.setUnit(product.getUnit() != null ? product.getUnit().getAbbreviation() : "un");

            // Usa a taxa efetiva: do produto ou o padrão do sistema
            double taxRate = product.getEffectiveTaxRate();
            double iceRate = product.getEffectiveIceRate();
            item.setTaxRate(taxRate);
            item.setIceRate(iceRate);

            double lineBase = qty * unitPrice;
            double lineIce = lineBase * (iceRate / 100.0);
            double lineTax = (lineBase + lineIce) * (taxRate / 100.0);

            item.setLineBase(lineBase);
            item.setLineIce(lineIce);
            item.setLineTax(lineTax);
            item.setLineTotal(lineBase + lineIce + lineTax);
            item.setSale(sale);
            itemsTable.getItems().add(item);
        }
        
        itemsTable.refresh();
        updateTotals();
        
        productSearchCombo.setValue(null);
        productSearchCombo.getEditor().clear();
        quantityField.setText("1");
        javafx.application.Platform.runLater(() -> productSearchCombo.requestFocus());
    }
    
    private void recalculateCartPrices() {
        boolean isWholesale = wholesaleModeToggle.isSelected();
        boolean hasErrors = false;
        for (SaleItem item : itemsTable.getItems()) {
            Product p = item.getProduct();
            if (p != null) {
                if (isWholesale) {
                    if (p.getPriceSaleBulk() == null || p.getPriceSaleBulk() <= 0) {
                        showError("Produto \"" + p.getName() + "\" não tem preço de grosso. Remova-o ou mude para retalho.");
                        hasErrors = true;
                        continue;
                    }
                    double bulkMin = p.getBulkQuantity() != null ? p.getBulkQuantity() : 0.0;
                    if (bulkMin > 0 && item.getQty() < bulkMin) {
                        showError("Produto \"" + p.getName() + "\" precisa de " + (int)bulkMin + " unidades p/ preço de grosso.");
                        hasErrors = true;
                        continue;
                    }
                }
                double newPrice = isWholesale ? p.getPriceSaleBulk() : p.getPriceSale();
                if (newPrice <= 0) {
                    showError("Preço inválido (0,00 MT) para \"" + p.getName() + "\". Configure o preço no cadastro.");
                    hasErrors = true;
                    continue;
                }
                item.setUnitPrice(newPrice);
                
                // Usa a taxa efetiva: do produto ou o padrão do sistema
                double taxRate = p.getEffectiveTaxRate();
                double iceRate = p.getEffectiveIceRate();
                item.setTaxRate(taxRate);
                item.setIceRate(iceRate);
                
                double lineBase = item.getQty() * newPrice;
                double lineIce = lineBase * (iceRate / 100.0);
                double lineTax = (lineBase + lineIce) * (taxRate / 100.0);
                
                item.setLineBase(lineBase);
                item.setLineIce(lineIce);
                item.setLineTax(lineTax);
                item.setLineTotal(lineBase + lineIce + lineTax);
            }
        }
        itemsTable.refresh();
        updateTotals();
        if (hasErrors) {
            retailModeToggle.setSelected(true);
        }
    }

    private double calculateCurrentTotal() {
        if (itemsTable == null || itemsTable.getItems() == null) return 0.0;
        return itemsTable.getItems().stream()
                .mapToDouble(i -> (i.getLineBase() != null ? i.getLineBase() : 0.0)
                        + (i.getLineIce() != null ? i.getLineIce() : 0.0)
                        + (i.getLineTax() != null ? i.getLineTax() : 0.0))
                .sum();
    }

    private void setupNumpad() {
        Button[] numButtons = {num0, num1, num2, num3, num4, num5, num6, num7, num8, num9};
        for (int i = 0; i < numButtons.length; i++) {
            final String digit = String.valueOf(i);
            if (numButtons[i] != null) {
                numButtons[i].setOnAction(e -> appendToActiveInput(digit));
                UiUtils.applyPressFeedback(numButtons[i]);
            }
        }
        if (numDot != null) {
            numDot.setOnAction(e -> appendToActiveInput("."));
            UiUtils.applyPressFeedback(numDot);
        }
        if (numClear != null) {
            UiUtils.applyPressFeedback(numClear);
            numClear.setOnAction(e -> {
                if (receivedAmountField != null && receivedAmountField.isFocused()) {
                    receivedAmountField.setText("");
                } else if (quantityField != null && quantityField.isFocused()) {
                    quantityField.setText("1");
                } else if (receivedAmountField != null) {
                    receivedAmountField.setText("");
                }
            });
        }
    }

    private void appendToActiveInput(String s) {
        TextField target = (quantityField != null && quantityField.isFocused()) ? quantityField : receivedAmountField;
        if (target != null) {
            String current = target.getText() != null ? target.getText() : "";
            if (s.equals(".") && current.contains(".")) return;
            target.setText(current + s);
            target.positionCaret(target.getText().length());
        }
    }

    private void setupCashDrawerAndShortcuts() {
        if (receivedAmountField != null) {
            UiUtils.applyNumericFormatter(receivedAmountField);
            receivedAmountField.textProperty().addListener((obs, o, n) -> updateChangeCalculation(calculateCurrentTotal()));
        }

        if (btnExactAmount != null) {
            UiUtils.applyPressFeedback(btnExactAmount);
            btnExactAmount.setOnAction(e -> {
                double tot = calculateCurrentTotal();
                if (receivedAmountField != null) {
                    receivedAmountField.setText(String.format(java.util.Locale.US, "%.2f", tot));
                }
            });
        }
        if (btnPlus50 != null) { UiUtils.applyPressFeedback(btnPlus50); btnPlus50.setOnAction(e -> addCashToReceived(50.0)); }
        if (btnPlus100 != null) { UiUtils.applyPressFeedback(btnPlus100); btnPlus100.setOnAction(e -> addCashToReceived(100.0)); }
        if (btnPlus500 != null) { UiUtils.applyPressFeedback(btnPlus500); btnPlus500.setOnAction(e -> addCashToReceived(500.0)); }
        if (btnPlus1000 != null) { UiUtils.applyPressFeedback(btnPlus1000); btnPlus1000.setOnAction(e -> addCashToReceived(1000.0)); }

        if (btnHoldCart != null) {
            UiUtils.applyPressFeedback(btnHoldCart);
            btnHoldCart.setOnAction(e -> holdCurrentCart());
        }
        if (btnRecallCart != null) {
            UiUtils.applyPressFeedback(btnRecallCart);
            btnRecallCart.setOnAction(e -> recallHeldCart());
        }
        if (addItemButton != null) UiUtils.applyPressFeedback(addItemButton);
        if (saveButton != null) UiUtils.applyPressFeedback(saveButton);
        if (cancelButton != null) UiUtils.applyPressFeedback(cancelButton);

        setupNumpad();

        paymentMethodCombo.valueProperty().addListener((obs, o, n) -> {
            boolean isCash = "DINHEIRO".equalsIgnoreCase(n) || "NUMERARIO".equalsIgnoreCase(n);
            if (cashDrawerPane != null) {
                cashDrawerPane.setOpacity(isCash ? 1.0 : 0.6);
            }
            if ("CREDITO".equalsIgnoreCase(n)) {
                if (diverseCustomerCheck != null && diverseCustomerCheck.isSelected()) {
                    diverseCustomerCheck.setSelected(false);
                }
            }
            validateRealTime();
        });
    }

    private void addCashToReceived(double amount) {
        double current = 0.0;
        if (receivedAmountField != null && receivedAmountField.getText() != null && !receivedAmountField.getText().isBlank()) {
            try {
                current = Double.parseDouble(receivedAmountField.getText().trim().replace(",", "."));
            } catch (Exception ignored) {}
        }
        double next = current + amount;
        if (receivedAmountField != null) {
            receivedAmountField.setText(String.format(java.util.Locale.US, "%.2f", next));
        }
    }

    private void updateChangeCalculation(double total) {
        if (changeAmountLabel == null) return;
        String text = (receivedAmountField != null) ? receivedAmountField.getText() : null;
        if (text == null || text.isBlank()) {
            changeAmountLabel.setText("0.00 MT");
            changeAmountLabel.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#64748B;");
            return;
        }
        try {
            double received = Double.parseDouble(text.trim().replace(",", "."));
            double diff = received - total;
            if (diff >= 0) {
                changeAmountLabel.setText(String.format(java.util.Locale.US, "%.2f MT", diff));
                changeAmountLabel.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#10B981;");
            } else {
                changeAmountLabel.setText(String.format(java.util.Locale.US, "Faltam %.2f MT", Math.abs(diff)));
                changeAmountLabel.setStyle("-fx-font-size:12px; -fx-font-weight:900; -fx-text-fill:#EF4444;");
            }
        } catch (Exception e) {
            changeAmountLabel.setText("0.00 MT");
            changeAmountLabel.setStyle("-fx-font-size:15px; -fx-font-weight:900; -fx-text-fill:#64748B;");
        }
    }

    private void holdCurrentCart() {
        if (itemsTable == null || itemsTable.getItems().isEmpty()) {
            showError("Carrinho está vazio para suspender.");
            return;
        }
        heldCartItems.clear();
        heldCartItems.addAll(itemsTable.getItems());
        heldCustomerName = (!diverseCustomerCheck.isSelected() && customerCombo.getValue() != null)
                ? customerCombo.getValue().getName() : "Cliente Diverso";
        itemsTable.getItems().clear();
        updateTotals();
        if (btnRecallCart != null) {
            btnRecallCart.setText("▶ Recuperar (" + heldCartItems.size() + ")");
            btnRecallCart.setStyle("-fx-background-color:#F59E0B; -fx-text-fill:#ffffff; -fx-font-size:11px; -fx-font-weight:800; -fx-background-radius:4; -fx-cursor:hand; -fx-padding:5 10;");
        }
        showError("Venda suspensa com sucesso (" + heldCartItems.size() + " itens guardados em espera).");
    }

    private void recallHeldCart() {
        if (heldCartItems.isEmpty()) {
            showError("Não há nenhuma venda em espera.");
            return;
        }
        itemsTable.setItems(FXCollections.observableArrayList(heldCartItems));
        heldCartItems.clear();
        if (btnRecallCart != null) {
            btnRecallCart.setText("▶ Recuperar (F7)");
            btnRecallCart.setStyle("-fx-background-color:rgba(255,255,255,0.15); -fx-text-fill:#FCD34D; -fx-font-size:11px; -fx-font-weight:700; -fx-background-radius:4; -fx-cursor:hand; -fx-padding:5 10;");
        }
        updateTotals();
        hideError();
    }

    private void setupGlobalShortcuts() {
        if (rootPane != null && rootPane.getScene() != null) {
            rootPane.getScene().setOnKeyPressed(event -> {
                KeyCode code = event.getCode();
                if (code == KeyCode.F2) {
                    if (productSearchCombo != null) { productSearchCombo.requestFocus(); }
                    event.consume();
                } else if (code == KeyCode.F4) {
                    if (quantityField != null) { quantityField.requestFocus(); quantityField.selectAll(); }
                    event.consume();
                } else if (code == KeyCode.F6) {
                    holdCurrentCart();
                    event.consume();
                } else if (code == KeyCode.F7) {
                    recallHeldCart();
                    event.consume();
                } else if (code == KeyCode.F8) {
                    if (diverseCustomerCheck != null) { diverseCustomerCheck.setSelected(!diverseCustomerCheck.isSelected()); }
                    event.consume();
                } else if (code == KeyCode.F10) {
                    if (formValidProperty.get()) { doSave(); }
                    event.consume();
                } else if (code == KeyCode.ESCAPE) {
                    doCancel();
                    event.consume();
                }
            });
        }
    }

    private void updateTotals() {
        double subtotal = itemsTable.getItems().stream()
                .mapToDouble(i -> i.getLineBase() != null ? i.getLineBase() : 0.0)
                .sum();

        double totalIce = itemsTable.getItems().stream()
                .mapToDouble(i -> i.getLineIce() != null ? i.getLineIce() : 0.0)
                .sum();

        double tax = itemsTable.getItems().stream()
                .mapToDouble(i -> i.getLineTax() != null ? i.getLineTax() : 0.0)
                .sum();

        double total = subtotal + totalIce + tax;

        subtotalLabel.setText(String.format("%.2f", subtotal));
        taxLabel.setText(String.format("%.2f", tax));
        totalLabel.setText(String.format("%.2f", total));

        String curr = currencyCombo.getValue() != null ? currencyCombo.getValue() : "MZN";
        if (headerTotalLabel != null) {
            headerTotalLabel.setText(String.format("Total: %.2f %s", total, curr));
        }
        updateChangeCalculation(total);

        // Assegurar que a validação corre sempre que os totais mudam (adição/remoção de produtos)
        validateRealTime();
    }

    private boolean validate() {
        errorLabel.setVisible(false);
        
        if (branchCombo.getValue() == null) {
            showError("Filial é obrigatória");
            return false;
        }
        if (!diverseCustomerCheck.isSelected() && customerCombo.getValue() == null) {
            showError("Selecione um cliente ou ative o 'Cliente Diverso'");
            return false;
        }
        if (itemsTable.getItems().isEmpty()) {
            showError("Adicione ao menos um produto no carrinho");
            return false;
        }
        String dtVal = documentTypeCombo.getValue();
        if (com.sgv.model.DocumentType.FACTURA.name().equals(dtVal)) {
            if (diverseCustomerCheck.isSelected()) {
                showError("Factura (FA) requer um cliente com NUIT. Desative 'Cliente Diverso' e selecione o cliente.");
                return false;
            }
            Customer c = customerCombo.getValue();
            String nuit = c != null ? c.getNuit() : null;
            if (nuit == null || nuit.isBlank()) {
                showError("Cliente selecionado para Factura (FA) não tem NUIT preenchido. Edite o cadastro do cliente.");
                return false;
            }
            if (!com.sgv.util.NuitValidator.isValid(nuit)) {
                showError("NUIT do cliente inválido: '" + nuit + "'. Verifique o número no cadastro do cliente.");
                return false;
            }
        }
        return true;
    }

    private void refreshCashSessionAvailability() {
        if (currentUser == null) {
            cashSessionValidProperty.set(false);
            showError("Não é possível vender sem uma sessão de utilizador válida. Faça login novamente.");
            return;
        }

        String sessionCheck = cashSessionService.requireOpenToday(currentUser);
        boolean valid = CashSessionService.OK.equals(sessionCheck);
        cashSessionValidProperty.set(valid);
        if (!valid) {
            showError(sessionCheck);
            return;
        }

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;

        // Fix #6: Bloquear sessão de caixa apenas para vendas fiscais
        com.sgv.model.DocumentType dtCheck = com.sgv.model.DocumentType.fromString(documentTypeCombo.getValue());
        boolean requiresCashSession = dtCheck == com.sgv.model.DocumentType.VENDA
                || dtCheck == com.sgv.model.DocumentType.FACTURA
                || dtCheck == com.sgv.model.DocumentType.RECIBO;
        if (requiresCashSession) {
            refreshCashSessionAvailability();
            if (!cashSessionValidProperty.get()) {
                systemLogService.logSystem("CAIXA_FECHADO", "Venda bloqueada — sessão de caixa inválida.");
                return;
            }
        }

        // Mostrar spinner e ocultar o botão
        showSaveSpinner();

        // Prepare sale object for background processing and delegate to SaleService
        sale.setDocumentType(com.sgv.model.DocumentType.fromString(documentTypeCombo.getValue()).name());
        sale.setBranch(branchCombo.getValue());

                if (diverseCustomerCheck.isSelected()) {
                    sale.setCustomer(null);
                    sale.setCustomerName("Cliente Diverso");
                    String finalNuit = appConfigService != null ? appConfigService.get().getConsumerFinalNuit() : "999999999";
                    sale.setCustomerNuit(finalNuit);
                    sale.setCustomerAddress(null);
                } else {
            Customer c = customerCombo.getValue();
            sale.setCustomer(c);
            sale.setCustomerName(c.getName());
            sale.setCustomerNuit(c.getNuit());
            sale.setCustomerAddress(c.getAddress());
        }

        sale.setPaymentMethod(com.sgv.model.PaymentMethod.fromString(paymentMethodCombo.getValue()).name());
        sale.setCurrency(currencyCombo.getValue());

        // Calcular e definir valor pago e troco
        double totalVal = calculateCurrentTotal();
        double receivedVal = totalVal;
        if (receivedAmountField != null && receivedAmountField.getText() != null && !receivedAmountField.getText().isBlank()) {
            try {
                receivedVal = Double.parseDouble(receivedAmountField.getText().trim().replace(",", "."));
            } catch (Exception ignored) {}
        }
        double changeVal = Math.max(0.0, receivedVal - totalVal);
        sale.setPaidAmount(receivedVal);
        sale.setChangeAmount(changeVal);

        // Defensive copy of items for background thread
        sale.setItems(new java.util.ArrayList<>(itemsTable.getItems()));

        com.sgv.model.DocumentType dt = com.sgv.model.DocumentType.fromString(sale.getDocumentType());
        if (dt == com.sgv.model.DocumentType.COTACAO || dt == com.sgv.model.DocumentType.ENCOMENDA) {
            sale.setState(dt == com.sgv.model.DocumentType.COTACAO
                ? com.sgv.model.SaleState.COTACAO_ABERTA.name()
                : com.sgv.model.SaleState.ENCOMENDA_ABERTA.name());

            // Fix #1: Aviso de stock insuficiente para cotações (não bloqueante)
            Branch b = branchCombo.getValue();
            if (b != null) {
                StringBuilder stockWarnings = new StringBuilder();
                for (SaleItem cartItem : itemsTable.getItems()) {
                    if (cartItem.getProduct() == null) continue;
                    if (Boolean.TRUE.equals(cartItem.getProduct().getService())) continue;
                    try {
                        java.util.Optional<com.sgv.entity.StockBranch> sbOpt =
                                stockBranchService.findByProductIdAndBranchId(cartItem.getProduct().getId(), b.getId());
                        java.math.BigDecimal avail = sbOpt.map(s -> s.getStockCurrentAmount() != null
                                ? s.getStockCurrentAmount() : java.math.BigDecimal.ZERO)
                                .orElse(java.math.BigDecimal.ZERO);
                        java.math.BigDecimal needed = java.math.BigDecimal.valueOf(cartItem.getQty() != null ? cartItem.getQty() : 0);
                        if (avail.compareTo(needed) < 0) {
                            stockWarnings.append("\n• ").append(cartItem.getProductCode())
                                .append(" (disponível: ").append(avail.toPlainString())
                                .append(", pedido: ").append(needed.toPlainString()).append(")");
                        }
                    } catch (Exception ignored) {}
                }
                if (stockWarnings.length() > 0) {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.WARNING);
                        alert.setTitle("Aviso de Stock");
                        alert.setHeaderText("Stock insuficiente no momento da cotação");
                        alert.setContentText("Os seguintes produtos podem não ter stock suficiente quando a cotação for convertida em venda:" + stockWarnings);
                        alert.showAndWait();
                    });
                }
            }
        } else if (dt == com.sgv.model.DocumentType.VENDA || dt == com.sgv.model.DocumentType.FACTURA) {
            sale.setState(com.sgv.model.SaleState.PAGO.name());
        } else if (dt == com.sgv.model.DocumentType.RECIBO) {
            sale.setState(com.sgv.model.SaleState.EMITIDA.name());
        } else {
            sale.setState(com.sgv.model.SaleState.EMITIDA.name());
        }

        sale.setCreatedAt(LocalDateTime.now());

        javafx.concurrent.Task<File> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected File call() throws Exception {
                return saleService.processAndSave(sale, currentUser);
            }
        };

        saveTask.setOnSucceeded(e -> {
            File pdf = saveTask.getValue();
            try {
                systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "VENDA_CRIADA", "Venda/Documento " + sale.getDocumentType() + " gravado com sucesso.");
            } catch (Exception ex) {
                log.error("Erro ao registar acção de venda", ex);
            }

            javafx.application.Platform.runLater(() -> {
                try {
                    if (pdf != null) {
                        javafx.scene.control.Label successLabel = new javafx.scene.control.Label("Operação concluída com sucesso!");
                        successLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center;");
                        successLabel.setMaxWidth(Double.MAX_VALUE);
                        rootPane.getChildren().add(0, successLabel);

                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                        delay.setOnFinished(ev -> rootPane.getChildren().remove(successLabel));
                        delay.play();

                        // Passar todos os 4 geradores de formato (80mm, 58mm, A4, A5)
                        com.sgv.desktop.DocumentPreviewDialog.show(
                            pdf,
                            sale.getDocumentType(),
                            atDefaultFormat(sale.getDocumentType()),
                            sale,
                            s -> { try { return thermalPrintService.printReceipt(s); } catch (Exception ex) { log.error("Thermal 80mm gen failed", ex); return null; } },
                            s -> { try { return thermalPrintService.printReceipt58mm(s); } catch (Exception ex) { log.error("Thermal 58mm gen failed", ex); return null; } },
                            s -> { try { return saleDocumentService.generateDocument(s); } catch (Exception ex) { log.error("A4 gen failed", ex); return null; } },
                            s -> { try { return saleDocumentService.generateDocumentA5(s); } catch (Exception ex) { log.error("A5 gen failed", ex); return null; } }
                        );
                    }
                    // Fix #8: Chamar onSave antes do resetForm para evitar NPE de timing
                    if (onSave != null) onSave.run();
                } catch (Exception ex) {
                    log.error("Erro ao mostrar preview do PDF", ex);
                    systemLogService.logError("PDF_OPEN_FAILED", "Falha ao preparar visualização: " + ex.getMessage(), ex);
                } finally {
                    resetForm();
                }
            });
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("SALE_SAVE_FAILED", "Erro ao salvar a operação (" + documentTypeCombo.getValue() + "): " + ex.getMessage(), ex);
            showError("Erro ao salvar a operação: " + ex.getMessage());
            log.error("Erro ao salvar venda", ex);
            
            hideSaveSpinner();
        });

        // Executa a tarefa numa nova Thread para não travar a UI
        new Thread(saveTask).start();
    }

    private void resetForm() {
        this.sale = new Sale();
        itemsTable.getItems().clear();
        quantityField.clear();
        productSearchCombo.setValue(null);
        
        hideSaveSpinner();
        hideError();
        
        updateTotals();
    }

    /**
     * Fix #6: Liga o botão Guardar ao cashSession apenas para tipos fiscais (VENDA/FACTURA/RECIBO).
     * Cotações e Encomendas podem ser emitidas sem sessão de caixa aberta.
     */
    private void updateSaveButtonBinding() {
        saveButton.disableProperty().unbind();
        String type = documentTypeCombo.getValue();
        boolean isFiscalSale = type == null
                || "VENDA".equalsIgnoreCase(type)
                || "FACTURA".equalsIgnoreCase(type)
                || "RECIBO".equalsIgnoreCase(type);
        if (isFiscalSale) {
            saveButton.disableProperty().bind(formValidProperty.not().or(cashSessionValidProperty.not()));
        } else {
            saveButton.disableProperty().bind(formValidProperty.not());
        }
    }
}
