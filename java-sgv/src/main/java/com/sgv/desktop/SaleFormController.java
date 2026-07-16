package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.StockBranchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.util.StringConverter;
import javafx.stage.Stage;
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

@Component
public class SaleFormController {

    @FXML private VBox rootPane;
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
    @FXML private Label errorLabel;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private ProgressIndicator saveSpinner;

    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SaleItemRepository saleItemRepository;
    private final StockBranchService stockBranchService;
    private final SaleDocumentService saleDocumentService;
    private final ThermalPrintService thermalPrintService;
    private final SystemLogService systemLogService;
    private final CashSessionService cashSessionService;
    private final com.sgv.service.AppConfigService appConfigService;
    private final com.sgv.service.SaleService saleService;

    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private static final long ALL_CATEGORY_ID = Long.MIN_VALUE;
    private static final Category ALL_CATEGORIES = createAllCategories();
    private final Map<Long, Double> productPopularity = new HashMap<>();
    private final int POPULAR_PRODUCTS_LIMIT = 40;

    private Sale sale;
    private User currentUser;
    private Runnable onSave;
    
    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);
    private final javafx.beans.property.BooleanProperty cashSessionValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);
    
    // Toggle group for pricing mode
    private final ToggleGroup modeGroup = new ToggleGroup();

    public SaleFormController(SaleRepository saleRepository,
                              BranchRepository branchRepository,
                              CustomerRepository customerRepository,
                              ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              SaleItemRepository saleItemRepository,
                              StockBranchService stockBranchService,
                              SaleDocumentService saleDocumentService,
                              ThermalPrintService thermalPrintService,
                              SystemLogService systemLogService,
                              CashSessionService cashSessionService,
                              com.sgv.service.SaleService saleService,
                              com.sgv.service.AppConfigService appConfigService) {
        this.saleRepository = saleRepository;
        this.branchRepository = branchRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockBranchService = stockBranchService;
        this.saleDocumentService = saleDocumentService;
        this.thermalPrintService = thermalPrintService;
        this.systemLogService = systemLogService;
        this.cashSessionService = cashSessionService;
        this.saleService = saleService;
        this.appConfigService = appConfigService;
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
        documentTypeCombo.setItems(FXCollections.observableArrayList(
            com.sgv.model.DocumentType.VENDA.name(),
            com.sgv.model.DocumentType.COTACAO.name(),
            com.sgv.model.DocumentType.ENCOMENDA.name()
        ));
        documentTypeCombo.setValue(com.sgv.model.DocumentType.VENDA.name());
        
        branchCombo.setItems(FXCollections.observableArrayList(branchRepository.findAll()));
        if(!branchCombo.getItems().isEmpty()) branchCombo.getSelectionModel().selectFirst();
        branchCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            refreshCashSessionAvailability();
            refreshPopularProducts();
            refreshProductSearchResults();
        });
        
        customerCombo.setItems(FXCollections.observableArrayList(customerRepository.findAll()));
        
        paymentMethodCombo.setItems(FXCollections.observableArrayList(
            com.sgv.model.PaymentMethod.DINHEIRO.name(),
            com.sgv.model.PaymentMethod.DEBITO.name(),
            com.sgv.model.PaymentMethod.CREDITO.name(),
            com.sgv.model.PaymentMethod.CHEQUE.name(),
            com.sgv.model.PaymentMethod.TRANSFERENCIA.name(),
            com.sgv.model.PaymentMethod.MULTICAIXA.name()
        ));
        paymentMethodCombo.setValue(com.sgv.model.PaymentMethod.DINHEIRO.name());
        
        currencyCombo.setItems(FXCollections.observableArrayList("AKZ", "USD", "EUR", "ZAR"));
        currencyCombo.setValue("AKZ");

        categories.setAll(categoryRepository.findAll().stream()
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
        
        addItemButton.setOnAction(e -> handleProductSelection(productSearchCombo.getValue()));
        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setText("");

        // UX: Transição de entrada fluida (Fade-in)
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // UX/MVVM: Data-Binding do botão de salvar
        saveButton.disableProperty().bind(formValidProperty.not().or(cashSessionValidProperty.not()));

        // Setup real-time listeners for styles & validation
        setupRealTimeValidation();
        
        // Auto-foco no campo de pesquisa ao abrir (pronto para venda ou scan)
        Platform.runLater(() -> {
            productSearchCombo.requestFocus();
            refreshCashSessionAvailability();
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
        
        // Trigger initial validation
        Platform.runLater(this::validateRealTime);
    }
    
    private void validateRealTime() {
        boolean isValid = true;
        
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
        
        // Atualizar estado reativo (MVVM Data Binding)
        formValidProperty.set(isValid);
        
        // Ocultar mensagem de erro geral se válido
        if (isValid) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
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
                btn.setOnAction(event -> {
                    SaleItem item = getTableView().getItems().get(getIndex());
                    itemsTable.getItems().remove(item);
                    updateTotals();
                });
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

    private void setupProductSearch() {
        allProducts.setAll(productRepository.findAllActive());
        filteredProducts = new FilteredList<>(allProducts, p -> true);
        SortedList<Product> sortedProducts = new SortedList<>(filteredProducts, this::compareProductsForSearch);
        productSearchCombo.setItems(sortedProducts);
        
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
        
        productSearchCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            String currentText = newVal != null ? newVal.trim() : "";
            Product selected = productSearchCombo.getSelectionModel().getSelectedItem();
            if (selected != null && !productDisplayText(selected).equals(currentText)) {
                productSearchCombo.getSelectionModel().clearSelection();
            }
            refreshProductSearchResults();
            if (!productSearchCombo.isShowing() && productSearchCombo.isFocused()) {
                productSearchCombo.show();
            }
        });

        productSearchCombo.getEditor().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String text = productSearchCombo.getEditor().getText().trim();
                Product selected = findBestMatchingProduct(text);
                if (selected == null) {
                    selected = productSearchCombo.getValue();
                }
                handleProductSelection(selected);
            }
        });
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
        if (!emptySearch && !productSearchCombo.isShowing() && productSearchCombo.isFocused()) {
            productSearchCombo.show();
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

    private boolean isProductAvailableInBranch(Product p, Branch branch) {
        if (branch == null || p == null) return true;
        if (Boolean.TRUE.equals(p.getService())) return true;
        Optional<StockBranch> stock = getStockForBranch(p, branch);
        return stock.map(sb -> sb.getStockCurrentAmount() != null && sb.getStockCurrentAmount().compareTo(BigDecimal.ZERO) > 0).orElse(false);
    }

    private Optional<StockBranch> getStockForBranch(Product p, Branch branch) {
        if (p == null || p.getId() == null || branch == null || branch.getId() == null) {
            return Optional.empty();
        }
        java.math.BigDecimal current = stockBranchService.getCurrentStock(branch, p);
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
        String lower = text.toLowerCase();

        return productSearchCombo.getItems().stream()
                .filter(p -> p != null)
                .filter(p -> searchMatchPriority(p, text, lower) < 100)
                .sorted(Comparator.comparingInt((Product p) -> searchMatchPriority(p, text, lower))
                        .thenComparingDouble((Product p) -> productPopularity.getOrDefault(p.getId(), 0.0)).reversed()
                        .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .findFirst()
                .orElse(null);
    }

    private void refreshPopularProducts() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(90).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();
        Branch branch = branchCombo != null ? branchCombo.getValue() : null;

        List<SaleItem> items = saleItemRepository.findBySaleDateRange(from, to);
        productPopularity.clear();
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

    private void handleProductSelection(Product product) {
        errorLabel.setVisible(false);
        
        if (product == null) {
            showError("Produto não encontrado.");
            return;
        }
        double qty = 1.0;
        try {
            qty = Double.parseDouble(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            qty = 1.0;
        }
        
        boolean isWholesale = wholesaleModeToggle.isSelected();
        double unitPrice = isWholesale ? 
                (product.getPriceSaleBulk() != null ? product.getPriceSaleBulk() : 0.0) : 
                (product.getPriceSale() != null ? product.getPriceSale() : 0.0);
                
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
            item.setDescription(product.getName());
            item.setQty(qty);
            item.setUnitPrice(unitPrice);

            double taxRate = product.getTaxRate() != null ? product.getTaxRate() : 0.0;
            double iceRate = product.getIceRate() != null ? product.getIceRate() : 0.0;
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
        for (SaleItem item : itemsTable.getItems()) {
            Product p = item.getProduct();
            if (p != null) {
                double newPrice = isWholesale ? 
                    (p.getPriceSaleBulk() != null ? p.getPriceSaleBulk() : 0.0) : 
                    (p.getPriceSale() != null ? p.getPriceSale() : 0.0);
                item.setUnitPrice(newPrice);
                
                double taxRate = p.getTaxRate() != null ? p.getTaxRate() : 0.0;
                double iceRate = p.getIceRate() != null ? p.getIceRate() : 0.0;
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
        return true;
    }
    
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
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

    private void doSave() {
        if (!validate()) return;

        // ── Bloqueio por sessão de caixa ─────────────────────────────────────
        // A venda só pode ser registada quando o turno de caixa estiver aberto
        // e ainda não tenha sido fechado no dia atual.
        refreshCashSessionAvailability();
        if (!cashSessionValidProperty.get()) {
            systemLogService.logSystem("CAIXA_FECHADO", "Venda bloqueada — sessão de caixa inválida.");
            return;
        }

        // Mostrar spinner e ocultar o botão
        saveButton.setVisible(false);
        saveSpinner.setVisible(true);
        saveSpinner.setManaged(true);
        cancelButton.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

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

        // Defensive copy of items for background thread
        sale.setItems(new java.util.ArrayList<>(itemsTable.getItems()));

        com.sgv.model.DocumentType dt = com.sgv.model.DocumentType.fromString(sale.getDocumentType());
        if (dt == com.sgv.model.DocumentType.COTACAO) {
            sale.setState(com.sgv.model.SaleState.COTACAO_ABERTA.name());
        } else if (dt == com.sgv.model.DocumentType.ENCOMENDA) {
            sale.setState(com.sgv.model.SaleState.ENCOMENDA_ABERTA.name());
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
            if (pdf != null) {
                try {
                    systemLogService.logUserAction("Sistema", "VENDA_CRIADA", "Venda/Documento gravado com sucesso.");
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Label successLabel = new javafx.scene.control.Label("Operação concluída com sucesso!");
                        successLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10px; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center;");
                        successLabel.setMaxWidth(Double.MAX_VALUE);
                        rootPane.getChildren().add(0, successLabel);
                        
                        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                        delay.setOnFinished(ev -> rootPane.getChildren().remove(successLabel));
                        delay.play();
                        
                        com.sgv.desktop.DocumentPreviewDialog.show(
                            pdf,
                            sale.getDocumentType(),
                            atDefaultFormat(sale.getDocumentType())
                        );
                        if (onSave != null) onSave.run();
                        resetForm();
                    });
                } catch (Exception ex) {
                    systemLogService.logError("PDF_OPEN_FAILED", "Falha ao preparar visualização.", ex);
                }
            }
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("SALE_SAVE_FAILED", "Erro ao salvar a operação (" + documentTypeCombo.getValue() + "): " + ex.getMessage(), ex);
            showError("Erro ao salvar a operação: " + ex.getMessage());
            ex.printStackTrace();
            
            // Reverter UI em caso de erro
            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        // Executa a tarefa numa nova Thread para não travar a UI
        new Thread(saveTask).start();
    }

    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void resetForm() {
        this.sale = new Sale();
        itemsTable.getItems().clear();
        quantityField.clear();
        productSearchCombo.setValue(null);
        
        saveButton.setVisible(true);
        saveSpinner.setVisible(false);
        saveSpinner.setManaged(false);
        cancelButton.setDisable(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        updateTotals();
    }
}
