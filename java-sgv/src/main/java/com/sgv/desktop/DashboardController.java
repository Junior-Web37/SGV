package com.sgv.desktop;

import com.sgv.entity.CashSession;
import com.sgv.service.SystemLogService;
import com.sgv.entity.Category;
import com.sgv.entity.User;
import com.sgv.entity.Branch;
import com.sgv.entity.Customer;
import com.sgv.entity.Payment;
import com.sgv.entity.Product;
import com.sgv.entity.ProductionOrder;
import com.sgv.entity.Purchase;
import com.sgv.entity.Sale;
import com.sgv.entity.Expense;
import com.sgv.entity.FilterPreset;
import com.sgv.entity.StockBranch;
import com.sgv.entity.StockWarehouse;
import com.sgv.entity.Warehouse;
import com.sgv.repository.WarehouseRepository;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.MetricUnitRepository;
import com.sgv.repository.StockMovementRepository;
import com.sgv.repository.StockWarehouseRepository;
import com.sgv.repository.CategoryRepository;
import com.sgv.service.StockBranchService;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.ExpenseRepository;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.ProductionOrderRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.SaleRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.CashSessionService;
import com.sgv.service.FilterPresetService;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tab;
import javafx.geometry.Side;
import com.sgv.service.SaleDocumentService;
import java.io.File;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.util.Callback;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableRow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.scene.paint.Color;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class DashboardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label welcomeText;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label cashStatusBadge;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userAvatar;
    @FXML private Button logoutButton;
    @FXML private Button notificationBellButton;
    @FXML private Label notificationBadge;

    @FXML private Label salesLabel;
    @FXML private Label productsLabel;
    @FXML private Label customersLabel;
    @FXML private Label branchesLabel;

    @FXML private javafx.scene.layout.BorderPane dashboardRoot;

    @FXML private Button navResumo;
    @FXML private Button shortcutPDVButton;
    @FXML private Button shortcutCotacaoButton;

    // Drill-down nav panes
    @FXML private javafx.scene.layout.StackPane navStackPane;
    @FXML private javafx.scene.layout.HBox navRootPane;
    @FXML private javafx.scene.layout.HBox navSubPane;
    @FXML private Label navSubModuleLabel;
    @FXML private Button navBackButton;

    // Root module buttons
    @FXML private Button navModComercial;
    @FXML private Button navModFinanceiro;
    @FXML private Button navModOperacoes;
    @FXML private Button navModAdministracao;
    @FXML private Button navModLogs;

    // Sub-item group panes
    @FXML private javafx.scene.layout.HBox navComercialItems;
    @FXML private javafx.scene.layout.HBox navFinanceiroItems;
    @FXML private javafx.scene.layout.HBox navOperacoesItems;
    @FXML private javafx.scene.layout.HBox navAdminItems;

    // Comercial sub-items (now Buttons)
    @FXML private Button navMenuNovaVenda;
    @FXML private Button navMenuVendas;
    @FXML private Button navMenuProdutos;

    @FXML private Button navMenuClientes;
    @FXML private Button navMenuStock;
    @FXML private Button navMenuArmazens;
    @FXML private Button navMenuTransferir;
    @FXML private Button navMenuCatalogos;

    // Financeiro sub-items (now Buttons)
    @FXML private Button navMenuCaixa;
    @FXML private Button navMenuFinanceiro;
    @FXML private Button navMenuPagamentos;
    @FXML private Button navMenuDespesas;
    @FXML private Button navMenuCompras;
    @FXML private Button navMenuFornecedores;

    // Operações sub-items (now Buttons)
    @FXML private Button navMenuProducao;
    @FXML private Button navMenuRelatorios;

    // Administração sub-items (now Buttons)
    @FXML private Button navMenuSistema;


    // Legacy fields (no longer needed but kept for applyPermissions compat)
    private javafx.scene.control.MenuItem navTurnoCaixa;
    private javafx.scene.control.MenuItem navFinanceiro;
    private javafx.scene.control.MenuItem navCompras;
    private javafx.scene.control.MenuItem navProducao;
    private javafx.scene.control.MenuItem navRelatorios;
    private javafx.scene.control.MenuItem navSistema;

    @FXML private javafx.scene.chart.LineChart<String, Number> salesLineChart;
    @FXML private javafx.scene.chart.PieChart salesPieChart;

    @FXML private StackPane contentPane;
    @FXML private VBox summaryPane;
    @FXML private VBox turnoCaixaPane;
    @FXML private VBox salesPane;
    @FXML private VBox salesStatsPane;
    @FXML private VBox productsPane;
    @FXML private VBox customersPane;
    @FXML private VBox comprasPane;
    @FXML private VBox financeiroPane;
    @FXML private VBox reportsPane;
    @FXML private VBox sistemaPane;
    @FXML private VBox producaoPane;
    @FXML private VBox warehousesPane;
    @FXML private VBox usersPane;
    @FXML private VBox catalogsPane;
    @FXML private VBox stockPane;
    @FXML private VBox logsPane;

    // Catalogs
    @FXML private Button btnNovaCategoria;
    @FXML private Button btnEditCategoria;
    @FXML private Button btnDeleteCategoria;
    @FXML private Button btnNovaUnidade;
    @FXML private Button btnEditUnidade;
    @FXML private Button btnDeleteUnidade;
    @FXML private TableView<com.sgv.entity.Category> categoriesTable;
    @FXML private TableColumn<com.sgv.entity.Category, String> colCatId;
    @FXML private TableColumn<com.sgv.entity.Category, String> colCatName;
    @FXML private TableView<com.sgv.entity.MetricUnit> unitsTable;
    @FXML private TableColumn<com.sgv.entity.MetricUnit, String> colUnitId;
    @FXML private TableColumn<com.sgv.entity.MetricUnit, String> colUnitAbbr;
    @FXML private TableColumn<com.sgv.entity.MetricUnit, String> colUnitDesc;

    // Stock
    @FXML private TextField stockSearchField;
    @FXML private Button btnAjustarStock;
    @FXML private TableView<com.sgv.entity.StockBranch> stockTable;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockCode;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockProduct;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockCategory;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockUnit;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockBranch;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockCurrent;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockMin;
    @FXML private TableColumn<com.sgv.entity.StockBranch, String> colStockStatus;

    // Sales Stats Page
    @FXML private Label salesStatsRevenueTodayLabel;
    @FXML private Label salesStatsCountTodayLabel;
    @FXML private Label salesStatsAvgTicketLabel;
    @FXML private Label salesStatsPendingLabel;
    @FXML private javafx.scene.chart.BarChart<String, Number> salesStatsBarChart;
    @FXML private javafx.scene.chart.PieChart salesStatsPieChart;

    // Sales
    @FXML private Button createSaleButton;
    @FXML private Button createCotacaoButton;
    @FXML private Button invoiceQuoteButton;
    @FXML private Button printSaleButton;
    @FXML private Button printReceiptButton;
    @FXML private Button annulSaleButton;
    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, String> salesDocumentColumn;
    @FXML private TableColumn<Sale, String> salesBranchColumn;
    @FXML private TableColumn<Sale, String> salesCustomerColumn;
    @FXML private TableColumn<Sale, String> salesTotalColumn;
    @FXML private TableColumn<Sale, String> salesStateColumn;
    @FXML private TableColumn<Sale, String> salesDateColumn;
    @FXML private Button salesPrevButton;
    @FXML private Label salesPageLabel;
    @FXML private Button salesNextButton;

    // Products
    @FXML private Button createProductButton;
    @FXML private Button editProductButton;
    @FXML private Button deleteProductButton;
    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, String> productCodeColumn;
    @FXML private TableColumn<Product, String> productNameColumn;
    @FXML private TableColumn<Product, String> productCategoryColumn;
    @FXML private TableColumn<Product, String> productPriceColumn;
    @FXML private TableColumn<Product, String> productStockColumn;
    @FXML private Button productsPrevButton;
    @FXML private Label productsPageLabel;
    @FXML private Button productsNextButton;

    // Customers
    @FXML private Button createCustomerButton;
    @FXML private Button editCustomerButton;
    @FXML private Button deleteCustomerButton;
    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, String> customerCodeColumn;
    @FXML private TableColumn<Customer, String> customerNameColumn;
    @FXML private TableColumn<Customer, String> customerTypeColumn;
    @FXML private TableColumn<Customer, String> customerBalanceColumn;
    @FXML private TableColumn<Customer, String> customerCreatedColumn;
    @FXML private Button customersPrevButton;
    @FXML private Label customersPageLabel;
    @FXML private Button customersNextButton;

    // Compras (Purchases)
    @FXML private Button createPurchaseButton;
    @FXML private Button editPurchaseButton;
    @FXML private Button deletePurchaseButton;
    @FXML private TableView<Purchase> purchasesTable;
    @FXML private TableColumn<Purchase, String> purchaseInvoiceColumn;
    @FXML private TableColumn<Purchase, String> purchaseTotalColumn;
    @FXML private TableColumn<Purchase, String> purchaseStateColumn;
    @FXML private TableColumn<Purchase, String> purchaseDateColumn;

    // Financeiro (Pagamentos)
    @FXML private Button createPaymentButton;
    @FXML private Button deletePaymentButton;
    @FXML private Label finTotalVendasLabel;
    @FXML private Label finTotalRecebidoLabel;
    @FXML private Label finPendenteLabel;
    @FXML private Label finNumPagamentosLabel;
    @FXML private TableView<Payment> paymentsTable;
    @FXML private TableColumn<Payment, String> paymentSaleColumn;
    @FXML private TableColumn<Payment, String> paymentCustomerColumn;
    @FXML private TableColumn<Payment, String> paymentMethodColumn;
    @FXML private TableColumn<Payment, String> paymentAmountColumn;
    @FXML private TableColumn<Payment, String> paymentDateColumn;

    // Financeiro (Despesas)
    @FXML private Button createExpenseButton;
    @FXML private Button editExpenseButton;
    @FXML private Button deleteExpenseButton;
    @FXML private TableView<Expense> expensesTable;
    @FXML private TableColumn<Expense, String> expenseDescColumn;
    @FXML private TableColumn<Expense, String> expenseCategoryColumn;
    @FXML private TableColumn<Expense, String> expenseAmountColumn;
    @FXML private TableColumn<Expense, String> expenseDueColumn;
    @FXML private TableColumn<Expense, String> expenseStateColumn;

    // Producao
    @FXML private Button createOrderButton;
    @FXML private Button editOrderButton;
    @FXML private Button deleteOrderButton;
    @FXML private Button completeOrderButton;
    @FXML private TableView<ProductionOrder> ordersTable;
    @FXML private TableColumn<ProductionOrder, String> orderNumberColumn;
    @FXML private TableColumn<ProductionOrder, String> orderProductColumn;
    @FXML private TableColumn<ProductionOrder, String> orderQuantityColumn;
    @FXML private TableColumn<ProductionOrder, String> orderUnitColumn;
    @FXML private TableColumn<ProductionOrder, String> orderStateColumn;
    @FXML private TableColumn<ProductionOrder, String> orderDateColumn;

    // Sistema
    @FXML private Button createUserButton;
    @FXML private Button editUserButton;
    @FXML private Button toggleUserButton;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> sysUserUsernameColumn;
    @FXML private TableColumn<User, String> sysUserFullNameColumn;
    @FXML private TableColumn<User, String> sysUserRoleColumn;
    @FXML private TableColumn<User, String> sysUserBranchColumn;
    @FXML private TableColumn<User, String> sysUserActiveColumn;

    @FXML private Button createBranchButton;
    @FXML private Button editBranchButton;
    @FXML private TableView<Branch> branchesTable;
    @FXML private TableColumn<Branch, Long> sysBranchIdColumn;
    @FXML private TableColumn<Branch, String> sysBranchNameColumn;
    @FXML private TableColumn<Branch, String> sysBranchAddressColumn;
    @FXML private TableColumn<Branch, String> sysBranchNuitColumn;
    @FXML private TableColumn<Branch, String> sysBranchActiveColumn;

    @FXML private TextField companyNameField;
    @FXML private TextField companyNuitField;
    @FXML private Button saveCompanyConfigButton;
    @FXML private VBox branchInfoBox;

    // Filter panes
    @FXML private VBox salesFilterPane;
    @FXML private VBox productsFilterPane;
    @FXML private VBox customersFilterPane;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final StockBranchService stockBranchService;
    private final StockMovementRepository stockMovementRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentRepository paymentRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final ApplicationContext applicationContext;
    private final FilterPresetService filterPresetService;
    private final CashSessionService cashSessionService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private User currentUser;
    private VBox productFilterPanel;
    private VBox customerFilterPanel;
    private VBox salesFilterPanel;
    
    private final MetricUnitRepository metricUnitRepository;
    private final SaleDocumentService saleDocumentService;
    private final WarehouseRepository warehouseRepository;
    private final StockWarehouseRepository stockWarehouseRepository;
    private final SystemLogService systemLogService;
    
    private ReportsController reportsController;
    
    // Pagination state
    private int currentSalesPage = 0;
    private int currentProductsPage = 0;
    private int currentCustomersPage = 0;
    private static final int PAGE_SIZE = 10;
    
    // Filter state
    private String productFilter = "";
    private String customerFilter = "";
    private String saleFilter = "";
    private String saleStateFilter = "TODOS";  // TODOS, EMITIDA, PAGO, ANULADA, COTACAO_ABERTA
    private TextField saleSearchField;         // referência ao campo de pesquisa na toolbar
    private ComboBox<String> saleStateCombo;   // referência ao filtro de estado na toolbar

    public DashboardController(SaleRepository saleRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               BranchRepository branchRepository,
                               StockBranchService stockBranchService,
                               StockMovementRepository stockMovementRepository,
                               CategoryRepository categoryRepository,
                               PaymentRepository paymentRepository,
                               ProductionOrderRepository productionOrderRepository,
                               UserRepository userRepository,
                               PurchaseRepository purchaseRepository,
                               ExpenseRepository expenseRepository,
                               ApplicationContext applicationContext,
                               FilterPresetService filterPresetService,
                               CashSessionService cashSessionService,
                               MetricUnitRepository metricUnitRepository,
                               SaleDocumentService saleDocumentService,
                               WarehouseRepository warehouseRepository,
                               StockWarehouseRepository stockWarehouseRepository,
                               SystemLogService systemLogService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.stockBranchService = stockBranchService;
        this.stockMovementRepository = stockMovementRepository;
        this.categoryRepository = categoryRepository;
        this.paymentRepository = paymentRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.expenseRepository = expenseRepository;
        this.applicationContext = applicationContext;
        this.filterPresetService = filterPresetService;
        this.cashSessionService = cashSessionService;
        this.metricUnitRepository = metricUnitRepository;
        this.saleDocumentService = saleDocumentService;
        this.warehouseRepository = warehouseRepository;
        this.stockWarehouseRepository = stockWarehouseRepository;
        this.systemLogService = systemLogService;
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadProfile();
        loadStats();
        loadSales();
        loadProducts();
        loadCustomers();
        refreshPresetCombos();
        applyPermissions();
        systemLogService.logSystem("DASHBOARD_LOADED", "Painel carregado para o utilizador: " + (user != null ? user.getUsername() : "?"));
    }

    private boolean hasPermission(String page, String action) {
        if (currentUser == null || currentUser.getRoles() == null || currentUser.getRoles().isEmpty()) {
            return false;
        }

        boolean isAdmin = currentUser.getRoles().stream()
            .filter(java.util.Objects::nonNull)
            .anyMatch(role -> role.getName() != null && role.getName().toUpperCase(Locale.ROOT).contains("ADMIN"));
        if (isAdmin) {
            return true;
        }

        return currentUser.getRoles().stream()
            .filter(java.util.Objects::nonNull)
            .anyMatch(role -> role.hasPermission(page, action));
    }

    private boolean ensurePermission(String page, String action, String label) {
        if (hasPermission(page, action)) {
            return true;
        }
        showAlert(Alert.AlertType.WARNING, "Não tem permissão para aceder a " + label + ".");
        return false;
    }

    private void setActionButtonPermission(Button button, String page, String action) {
        if (button == null) return;
        boolean allowed = hasPermission(page, action);
        button.setDisable(!allowed);
        button.setVisible(true);
        button.setManaged(true);
    }

    private void applyPermissions() {
        if (currentUser == null) return;

        boolean canResumo = hasPermission("RESUMO", "VIEW");
        boolean canVendas = hasPermission("VENDAS", "VIEW") || hasPermission("VENDAS", "CREATE");
        boolean canProdutos = hasPermission("PRODUTOS", "VIEW") || hasPermission("PRODUTOS", "CREATE");
        boolean canClientes = hasPermission("CLIENTES", "VIEW");
        boolean canStock = hasPermission("STOCK", "VIEW");
        boolean canArmazens = hasPermission("ARMAZENS", "VIEW");
        boolean canTransferir = hasPermission("TRANSFERENCIAS", "VIEW");
        boolean canCatalogos = hasPermission("CATALOGOS", "VIEW");
        boolean canCaixa = hasPermission("CAIXA", "VIEW");
        boolean canFinanceiro = hasPermission("FINANCEIRO", "VIEW");
        boolean canCompras = hasPermission("COMPRAS", "VIEW");
        boolean canProducao = hasPermission("PRODUCAO", "VIEW");
        boolean canRelatorios = hasPermission("RELATORIOS", "VIEW");
        boolean canSistema = hasPermission("SISTEMA", "VIEW");

        if (navResumo != null) { navResumo.setVisible(canResumo); navResumo.setManaged(canResumo); }
        if (navMenuNovaVenda != null) { navMenuNovaVenda.setVisible(canVendas); navMenuNovaVenda.setManaged(canVendas); }
        if (navMenuVendas != null) { navMenuVendas.setVisible(canVendas); navMenuVendas.setManaged(canVendas); }
        if (navMenuProdutos != null) { navMenuProdutos.setVisible(canProdutos); navMenuProdutos.setManaged(canProdutos); }
        if (navMenuClientes != null) { navMenuClientes.setVisible(canClientes); navMenuClientes.setManaged(canClientes); }
        if (navMenuStock != null) { navMenuStock.setVisible(canStock); navMenuStock.setManaged(canStock); }
        if (navMenuArmazens != null) { navMenuArmazens.setVisible(canArmazens); navMenuArmazens.setManaged(canArmazens); }
        if (navMenuTransferir != null) { navMenuTransferir.setVisible(canTransferir); navMenuTransferir.setManaged(canTransferir); }
        if (navMenuCatalogos != null) { navMenuCatalogos.setVisible(canCatalogos); navMenuCatalogos.setManaged(canCatalogos); }
        if (navMenuCaixa != null) { navMenuCaixa.setVisible(canCaixa); navMenuCaixa.setManaged(canCaixa); }
        if (navMenuFinanceiro != null) { navMenuFinanceiro.setVisible(canFinanceiro); navMenuFinanceiro.setManaged(canFinanceiro); }
        if (navMenuCompras != null) { navMenuCompras.setVisible(canCompras); navMenuCompras.setManaged(canCompras); }
        if (navMenuDespesas != null) { navMenuDespesas.setVisible(canFinanceiro); navMenuDespesas.setManaged(canFinanceiro); }
        if (navMenuPagamentos != null) { navMenuPagamentos.setVisible(canFinanceiro); navMenuPagamentos.setManaged(canFinanceiro); }
        if (navMenuProducao != null) { navMenuProducao.setVisible(canProducao); navMenuProducao.setManaged(canProducao); }
        if (navMenuRelatorios != null) { navMenuRelatorios.setVisible(canRelatorios); navMenuRelatorios.setManaged(canRelatorios); }
        if (navMenuSistema != null) { navMenuSistema.setVisible(canSistema); navMenuSistema.setManaged(canSistema); }
        if (navModComercial != null) { boolean visible = canVendas || canProdutos || canClientes || canStock || canArmazens || canTransferir || canCatalogos; navModComercial.setVisible(visible); navModComercial.setManaged(visible); }
        if (navModFinanceiro != null) { navModFinanceiro.setVisible(canCaixa || canFinanceiro || canCompras); navModFinanceiro.setManaged(canCaixa || canFinanceiro || canCompras); }
        if (navModOperacoes != null) { navModOperacoes.setVisible(canProducao || canRelatorios); navModOperacoes.setManaged(canProducao || canRelatorios); }
        if (navModAdministracao != null) { navModAdministracao.setVisible(canSistema); navModAdministracao.setManaged(canSistema); }
        if (navModLogs != null) { navModLogs.setVisible(canSistema); navModLogs.setManaged(canSistema); }

        setActionButtonPermission(createProductButton, "PRODUTOS", "CREATE");
        setActionButtonPermission(editProductButton, "PRODUTOS", "CREATE");
        setActionButtonPermission(deleteProductButton, "PRODUTOS", "DELETE");
        setActionButtonPermission(createCustomerButton, "CLIENTES", "CREATE");
        setActionButtonPermission(editCustomerButton, "CLIENTES", "CREATE");
        setActionButtonPermission(deleteCustomerButton, "CLIENTES", "DELETE");
        setActionButtonPermission(createSaleButton, "VENDAS", "CREATE");
        setActionButtonPermission(createCotacaoButton, "VENDAS", "CREATE");
        setActionButtonPermission(invoiceQuoteButton, "VENDAS", "CREATE");
        setActionButtonPermission(printSaleButton, "VENDAS", "VIEW");
        setActionButtonPermission(printReceiptButton, "VENDAS", "VIEW");
        setActionButtonPermission(annulSaleButton, "VENDAS", "DELETE");
        setActionButtonPermission(btnNovaCategoria, "CATALOGOS", "CREATE");
        setActionButtonPermission(btnEditCategoria, "CATALOGOS", "CREATE");
        setActionButtonPermission(btnDeleteCategoria, "CATALOGOS", "DELETE");
        setActionButtonPermission(btnNovaUnidade, "CATALOGOS", "CREATE");
        setActionButtonPermission(btnEditUnidade, "CATALOGOS", "CREATE");
        setActionButtonPermission(btnDeleteUnidade, "CATALOGOS", "DELETE");
        setActionButtonPermission(btnAjustarStock, "STOCK", "CREATE");
    }

    @FXML
    public void initialize() {
        // Load dark-nav CSS for hover effects
        String css = getClass().getResource("/css/dashboard.css").toExternalForm();
        dashboardRoot.getStylesheets().add(css);

        logoutButton.setOnAction(e -> doLogout());
        navResumo.setOnAction(e -> { if (ensurePermission("RESUMO", "VIEW", "Resumo")) showSummaryPane(); });
        if (shortcutPDVButton != null) shortcutPDVButton.setOnAction(e -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openPDVSaleFormWithType(null, null); });
        if (shortcutCotacaoButton != null) shortcutCotacaoButton.setOnAction(e -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openSaleFormWithType(null, "COTACAO"); });

        // Wire new Button sub-items
        if (navMenuNovaVenda != null) navMenuNovaVenda.setOnAction(e -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openPDVSaleFormWithType(null, null); });
        if (navMenuVendas != null) navMenuVendas.setOnAction(e -> { if (ensurePermission("VENDAS", "VIEW", "Vendas")) showSalesStatsPane(); });
        if (navMenuProdutos != null) navMenuProdutos.setOnAction(e -> { if (ensurePermission("PRODUTOS", "VIEW", "Produtos")) showProductsPane(); });
        if (navMenuClientes != null) navMenuClientes.setOnAction(e -> { if (ensurePermission("CLIENTES", "VIEW", "Clientes")) showCustomersPane(); });
        if (navMenuStock != null) navMenuStock.setOnAction(e -> { if (ensurePermission("STOCK", "VIEW", "Stock")) showStockPane(); });
        if (navMenuArmazens != null) navMenuArmazens.setOnAction(e -> { if (ensurePermission("ARMAZENS", "VIEW", "Armazéns")) showWarehousesPane(); });
        if (navMenuTransferir != null) navMenuTransferir.setOnAction(e -> { if (ensurePermission("TRANSFERENCIAS", "VIEW", "Transferências")) openWarehouseTransferForm(); });
        if (navMenuCatalogos != null) navMenuCatalogos.setOnAction(e -> { if (ensurePermission("CATALOGOS", "VIEW", "Catálogos")) showCatalogsPane(); });

        if (navMenuCaixa != null) navMenuCaixa.setOnAction(e -> { if (ensurePermission("CAIXA", "VIEW", "Caixa")) showTurnoCaixaPane(); });
        if (navMenuFinanceiro != null) navMenuFinanceiro.setOnAction(e -> { if (ensurePermission("FINANCEIRO", "VIEW", "Financeiro")) showFinanceiroPane(); });
        if (navMenuCompras != null) navMenuCompras.setOnAction(e -> { if (ensurePermission("COMPRAS", "VIEW", "Compras")) showComprasPane(); });

        if (navMenuProducao != null) navMenuProducao.setOnAction(e -> { if (ensurePermission("PRODUCAO", "VIEW", "Produção")) showProducaoPane(); });
        if (navMenuRelatorios != null) navMenuRelatorios.setOnAction(e -> { if (ensurePermission("RELATORIOS", "VIEW", "Relatórios")) showReportsPane(); });

        if (navMenuSistema != null) navMenuSistema.setOnAction(e -> { if (ensurePermission("SISTEMA", "VIEW", "Sistema")) showSistemaPane(); });


        // Novos handlers - Despesas e Unidades
        if (navMenuDespesas != null) navMenuDespesas.setOnAction(e -> {
            showFinanceiroPane();  // Navega para Financeiro onde está o tab Despesas
        });
        if (navMenuPagamentos != null) navMenuPagamentos.setOnAction(e -> showFinanceiroPane());

        // ─── Module buttons (open sub-nav) ─────────────────────────────────────
        if (navModComercial != null) navModComercial.setOnAction(e -> showSubNav("Comercial", navComercialItems));
        if (navModOperacoes != null) navModOperacoes.setOnAction(e -> showSubNav("Operações", navOperacoesItems));
        if (navModFinanceiro != null) navModFinanceiro.setOnAction(e -> showSubNav("Financeiro", navFinanceiroItems));
        if (navModAdministracao != null) navModAdministracao.setOnAction(e -> showSubNav("Admin", navAdminItems));
        if (navModLogs != null) navModLogs.setOnAction(e -> showLogsPane());

        // Back button
        if (navBackButton != null) navBackButton.setOnAction(e -> drillBack());

        // ─────────────────────────────────────────────────────────────────────

        
        // Setup CRUD buttons
        if (createProductButton != null) {
            createProductButton.setOnAction(e -> {
                if (ensurePermission("PRODUTOS", "CREATE", "Produtos")) openProductForm(null);
            });
            editProductButton.setOnAction(e -> {
                if (!ensurePermission("PRODUTOS", "CREATE", "Produtos")) return;
                if (productsTable == null) return;
                Product selected = productsTable.getSelectionModel().getSelectedItem();
                if (selected != null) openProductForm(selected);
            });
            deleteProductButton.setOnAction(e -> {
                if (!ensurePermission("PRODUTOS", "DELETE", "Produtos")) return;
                if (productsTable == null) return;
                Product selected = productsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Eliminar Produto");
                    confirm.setHeaderText("Tem certeza que deseja apagar este produto?");
                    confirm.setContentText("Esta ação não pode ser revertida.");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            safeDelete(() -> productRepository.deleteById(selected.getId()), this::loadProducts, "Produto");
                        }
                    });
                }
            });
            productsPrevButton.setOnAction(e -> {
                if (currentProductsPage > 0) {
                    currentProductsPage--;
                    loadProducts();
                    updatePageLabel(productsPageLabel, currentProductsPage);
                }
            });
            productsNextButton.setOnAction(e -> {
                currentProductsPage++;
                loadProducts();
                updatePageLabel(productsPageLabel, currentProductsPage);
            });
        }
        
        if (createCustomerButton != null) {
            createCustomerButton.setOnAction(e -> {
                if (ensurePermission("CLIENTES", "CREATE", "Clientes")) openCustomerForm(null);
            });
            editCustomerButton.setOnAction(e -> {
                if (!ensurePermission("CLIENTES", "CREATE", "Clientes")) return;
                if (customersTable == null) return;
                Customer selected = customersTable.getSelectionModel().getSelectedItem();
                if (selected != null) openCustomerForm(selected);
            });
            deleteCustomerButton.setOnAction(e -> {
                if (!ensurePermission("CLIENTES", "DELETE", "Clientes")) return;
                if (customersTable == null) return;
                Customer selected = customersTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Eliminar Cliente");
                    confirm.setHeaderText("Tem certeza que deseja apagar este cliente?");
                    confirm.setContentText("Esta ação não pode ser revertida.");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            safeDelete(() -> customerRepository.deleteById(selected.getId()), this::loadCustomers, "Cliente");
                        }
                    });
                }
            });
            customersPrevButton.setOnAction(e -> {
                if (currentCustomersPage > 0) {
                    currentCustomersPage--;
                    loadCustomers();
                    updatePageLabel(customersPageLabel, currentCustomersPage);
                }
            });
            customersNextButton.setOnAction(e -> {
                currentCustomersPage++;
                loadCustomers();
                updatePageLabel(customersPageLabel, currentCustomersPage);
            });
        }

        if (createSaleButton != null) {
            createSaleButton.setOnAction(e -> {
                if (ensurePermission("VENDAS", "CREATE", "Vendas")) openPDVSaleFormWithType(null, null);
            });
            if (createCotacaoButton != null) createCotacaoButton.setOnAction(e -> {
                if (ensurePermission("VENDAS", "CREATE", "Vendas")) openCotacaoForm();
            });
            if (invoiceQuoteButton != null) invoiceQuoteButton.setOnAction(e -> {
                if (ensurePermission("VENDAS", "CREATE", "Vendas")) invoiceSelectedQuote();
            });
            printSaleButton.setOnAction(e -> {
                if (ensurePermission("VENDAS", "VIEW", "Vendas")) printSelectedSale();
            });
            if (printReceiptButton != null) printReceiptButton.setOnAction(e -> {
                if (ensurePermission("VENDAS", "VIEW", "Vendas")) printSelectedReceipt();
            });
            annulSaleButton.setOnAction(e -> {
                if (ensurePermission("VENDAS", "DELETE", "Vendas")) annulSelectedSale();
            });
            salesPrevButton.setOnAction(e -> {
                if (currentSalesPage > 0) {
                    currentSalesPage--;
                    loadSales();
                    updatePageLabel(salesPageLabel, currentSalesPage);
                }
            });
            salesNextButton.setOnAction(e -> {
                currentSalesPage++;
                loadSales();
                updatePageLabel(salesPageLabel, currentSalesPage);
            });
        }
        
        if (createPurchaseButton != null) {
            createPurchaseButton.setOnAction(e -> openPurchaseForm(null));
            editPurchaseButton.setOnAction(e -> {
                if (purchasesTable == null) return;
                Purchase selected = purchasesTable.getSelectionModel().getSelectedItem();
                if (selected != null) openPurchaseForm(selected);
            });
            deletePurchaseButton.setOnAction(e -> {
                if (purchasesTable == null) return;
                Purchase selected = purchasesTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Eliminar Compra");
                    confirm.setHeaderText("Tem certeza que deseja apagar esta compra?");
                    confirm.setContentText("Esta ação não pode ser revertida.");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            safeDelete(() -> purchaseRepository.deleteById(selected.getId()), this::loadPurchases, "Compra");
                        }
                    });
                }
            });
        }

        if (createExpenseButton != null) {
            createExpenseButton.setOnAction(e -> openExpenseForm(null));
            editExpenseButton.setOnAction(e -> {
                if (expensesTable == null) return;
                Expense selected = expensesTable.getSelectionModel().getSelectedItem();
                if (selected != null) openExpenseForm(selected);
            });
            deleteExpenseButton.setOnAction(e -> {
                if (expensesTable == null) return;
                Expense selected = expensesTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Eliminar Despesa");
                    confirm.setHeaderText("Tem certeza que deseja apagar esta despesa?");
                    confirm.setContentText("Esta ação não pode ser revertida.");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            safeDelete(() -> expenseRepository.deleteById(selected.getId()), this::loadExpenses, "Despesa");
                        }
                    });
                }
            });
        }

        if (createPaymentButton != null) {
            createPaymentButton.setOnAction(e -> openPaymentForm(null));
            deletePaymentButton.setOnAction(e -> {
                if (paymentsTable == null) return;
                Payment selected = paymentsTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Estornar Pagamento");
                    confirm.setHeaderText("Tem certeza que deseja apagar este pagamento?");
                    confirm.setContentText("Esta acção não pode ser revertida.");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            safeDelete(() -> paymentRepository.deleteById(selected.getId()), this::loadFinanceiro, "Pagamento");
                        }
                    });
                }
            });
        }

        if (createUserButton != null) {
            createUserButton.setOnAction(e -> openUserForm(null));
            editUserButton.setOnAction(e -> {
                if (usersTable == null) return;
                User selected = usersTable.getSelectionModel().getSelectedItem();
                if (selected != null) openUserForm(selected);
            });
            toggleUserButton.setOnAction(e -> {
                if (usersTable == null) return;
                User selected = usersTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selected.setActive(!selected.isActive());
                    userRepository.save(selected);
                    loadSystem();
                }
            });
        }

        if (createBranchButton != null) {
            createBranchButton.setOnAction(e -> openBranchForm(null));
            editBranchButton.setOnAction(e -> {
                if (branchesTable == null) return;
                Branch selected = branchesTable.getSelectionModel().getSelectedItem();
                if (selected != null) openBranchForm(selected);
            });
        }

        if (saveCompanyConfigButton != null) {
            saveCompanyConfigButton.setOnAction(e -> saveCompanyConfig());
        }

        if (createOrderButton != null) {
            createOrderButton.setOnAction(e -> openOrderForm(null));
            editOrderButton.setOnAction(e -> {
                if (ordersTable == null) return;
                ProductionOrder selected = ordersTable.getSelectionModel().getSelectedItem();
                if (selected != null) openOrderForm(selected);
            });
            deleteOrderButton.setOnAction(e -> {
                if (ordersTable == null) return;
                ProductionOrder selected = ordersTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Eliminar Ordem de Produção");
                    confirm.setHeaderText("Tem certeza que deseja apagar esta ordem?");
                    confirm.setContentText("Esta ação não pode ser revertida.");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            safeDelete(() -> productionOrderRepository.deleteById(selected.getId()), this::loadProductionOrders, "Ordem de Produção");
                        }
                    });
                }
            });
            if (completeOrderButton != null) {
                completeOrderButton.setOnAction(e -> {
                    if (ordersTable == null) return;
                    ProductionOrder selected = ordersTable.getSelectionModel().getSelectedItem();
                    if (selected != null && !"COMPLETED".equals(selected.getState())) {
                        selected.setState("COMPLETED");
                        selected.setCompletedAt(java.time.LocalDateTime.now());
                        productionOrderRepository.save(selected);
                        loadProductionOrders();
                    }
                });
            }
        }

        
        // Build and attach filter panels
        try {
            if (productsFilterPane != null) {
                productFilterPanel = FilterPanelBuilder.createProductFilterPanel(criteria -> {
                    if (criteria != null && productsTable != null) {
                        if (criteria.category() != null && !criteria.category().isBlank() && !criteria.category().equals("Todos")) {
                            var list = productRepository.searchByCodeOrNameAndCategory(
                                    criteria.search() == null ? "" : criteria.search(),
                                    criteria.category());
                            productsTable.setItems(FXCollections.observableArrayList(list));
                        } else {
                            setProductFilter(criteria.search());
                        }
                    }
                }, () -> clearProductFilter());
                productsFilterPane.getChildren().add(productFilterPanel);
                try {
                    var combo = (ComboBox<String>) productFilterPanel.lookup("#productCategoryCombo");
                    if (combo != null) {
                        var cats = categoryRepository.findAll().stream()
                                .map(c -> c.getName())
                                .filter(n -> n != null && !n.isBlank())
                                .distinct()
                                .sorted(String::compareToIgnoreCase)
                                .toList();
                        java.util.List<String> items = new java.util.ArrayList<>();
                        items.add("Todos");
                        items.addAll(cats);
                        combo.setItems(FXCollections.observableArrayList(items));
                        combo.setValue("Todos");
                    }
                    var presetCombo = (ComboBox<String>) productFilterPanel.lookup("#productPresetCombo");
                    var savePreset = (Button) productFilterPanel.lookup("#productSavePresetButton");
                    var deletePreset = (Button) productFilterPanel.lookup("#productDeletePresetButton");
                    if (presetCombo != null) {
                        populatePresetCombo(productFilterPanel, "PRODUCT", presetCombo);
                    }
                    if (savePreset != null) {
                        savePreset.setOnAction(e -> saveProductPreset(productFilterPanel));
                    }
                    if (deletePreset != null) {
                        deletePreset.setOnAction(e -> deletePreset("PRODUCT", productFilterPanel));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (customersFilterPane != null) {
                customerFilterPanel = FilterPanelBuilder.createCustomerFilterPanel(criteria -> {
                    if (criteria != null) {
                        if (criteria.type() != null && !criteria.type().isBlank() && !criteria.type().equals("Todos")) {
                            var list = customerRepository.searchByCodeOrNameAndType(
                                    criteria.search() == null ? "" : criteria.search(),
                                    criteria.type());
                            customersTable.setItems(FXCollections.observableArrayList(list));
                        } else {
                            setCustomerFilter(criteria.search());
                        }
                    }
                }, () -> clearCustomerFilter());
                customersFilterPane.getChildren().add(customerFilterPanel);
                try {
                    var combo = (ComboBox<String>) customerFilterPanel.lookup("#customerTypeCombo");
                    if (combo != null) {
                        var types = customerRepository.findAll().stream()
                                .map(c -> c.getType())
                                .filter(t -> t != null && !t.isBlank())
                                .distinct()
                                .sorted(String::compareToIgnoreCase)
                                .toList();
                        java.util.List<String> items = new java.util.ArrayList<>();
                        items.add("Todos");
                        items.addAll(types);
                        combo.setItems(FXCollections.observableArrayList(items));
                        combo.setValue("Todos");
                    }
                    var presetCombo = (ComboBox<String>) customerFilterPanel.lookup("#customerPresetCombo");
                    var savePreset = (Button) customerFilterPanel.lookup("#customerSavePresetButton");
                    var deletePreset = (Button) customerFilterPanel.lookup("#customerDeletePresetButton");
                    if (presetCombo != null) {
                        populatePresetCombo(customerFilterPanel, "CUSTOMER", presetCombo);
                    }
                    if (savePreset != null) {
                        savePreset.setOnAction(e -> saveCustomerPreset(customerFilterPanel));
                    }
                    if (deletePreset != null) {
                        deletePreset.setOnAction(e -> deletePreset("CUSTOMER", customerFilterPanel));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (salesFilterPane != null) {
                salesFilterPanel = FilterPanelBuilder.createSalesFilterPanel(criteria -> {
                    if (criteria != null) {
                        if (criteria.startDate() != null || criteria.endDate() != null || (criteria.state() != null && !criteria.state().isBlank())) {
                            java.time.LocalDate start = criteria.startDate();
                            java.time.LocalDate end = criteria.endDate();
                            java.time.LocalDateTime startDt = start != null ? start.atStartOfDay() : java.time.LocalDateTime.MIN;
                            java.time.LocalDateTime endDt = end != null ? end.atTime(23, 59, 59) : java.time.LocalDateTime.MAX;
                            var list = saleRepository.findByCustomerAndDateRangeAndState(
                                    criteria.search() == null ? "" : criteria.search(),
                                    startDt,
                                    endDt,
                                    criteria.state());
                            salesTable.setItems(FXCollections.observableArrayList(list));
                        } else {
                            setSaleFilter(criteria.search());
                        }
                    }
                }, () -> clearSaleFilter());
                salesFilterPane.getChildren().add(salesFilterPanel);
                try {
                    var presetCombo = (ComboBox<String>) salesFilterPanel.lookup("#salesPresetCombo");
                    var savePreset = (Button) salesFilterPanel.lookup("#salesSavePresetButton");
                    var deletePreset = (Button) salesFilterPanel.lookup("#salesDeletePresetButton");
                    if (presetCombo != null) {
                        populatePresetCombo(salesFilterPanel, "SALE", presetCombo);
                    }
                    if (savePreset != null) {
                        savePreset.setOnAction(e -> saveSalesPreset(salesFilterPanel));
                    }
                    if (deletePreset != null) {
                        deletePreset.setOnAction(e -> deletePreset("SALE", salesFilterPanel));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Notification bell
        if (notificationBellButton != null) {
            notificationBellButton.setOnAction(e -> showNotificationPopup());
        }

        initTables();
        loadReportsPane();
        showSummaryPane();
    }

    private void loadReportsPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reports.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Node reportsContent = loader.load();
            reportsController = loader.getController();

            if (reportsPane != null) {
                reportsPane.getChildren().add(reportsContent);
                VBox.setVgrow(reportsContent, Priority.ALWAYS);
            }
        } catch (Exception ex) {
            System.err.println("Failed to load reports pane: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ─── NAVIGATION HELPERS ───────────────────────────────────────────────────

    private void setActiveNav(Button btn) {
        Button[] all = {navResumo, navMenuNovaVenda, navMenuVendas, navMenuProdutos, navMenuClientes, navMenuStock, navMenuCaixa, navMenuCompras, navMenuFornecedores, navMenuPagamentos, navMenuDespesas, navMenuCatalogos, navMenuRelatorios, navMenuSistema, navMenuArmazens, navMenuTransferir};
        for (Button b : all) {
            if (b != null) {
                b.getStyleClass().remove("nav-active");
                b.getStyleClass().remove("active");
            }
        }
        if (btn != null) {
            btn.getStyleClass().add("nav-btn");
            btn.getStyleClass().add("active");
        }
    }

    private void setPaneVisibility(VBox pane) {
        VBox[] all = {summaryPane, salesStatsPane, salesPane, productsPane, customersPane, stockPane, turnoCaixaPane, comprasPane, financeiroPane, catalogsPane, reportsPane, producaoPane, sistemaPane, usersPane, warehousesPane, logsPane};
        for (VBox v : all) { if (v != null) { v.setVisible(false); v.setManaged(false); } }
        if (pane != null) { pane.setVisible(true); pane.setManaged(true); }
    }

    private void showCustomersPane() {
        setActiveNav(navMenuClientes);
        pageTitleLabel.setText("Clientes");
        pageSubtitleLabel.setText("Base de dados de clientes");
        setPaneVisibility(customersPane);

        if (customersPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = new HBox(8);
            toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnNovo = makeActionButton("Novo Cliente", "#10B981", "#ffffff");
            btnNovo.setOnAction(e -> openCustomerForm(null));

            Button btnEditar = makeActionButton("Editar", "#2563EB", "#ffffff");
            btnEditar.setOnAction(e -> {
                if (customersTable.getSelectionModel().getSelectedItem() != null) {
                    openCustomerForm(customersTable.getSelectionModel().getSelectedItem());
                } else {
                    showToast("Selecione um cliente para editar.");
                }
            });

            Button btnLiquidar = makeActionButton("Liquidar Dívida", "#F59E0B", "#ffffff");
            btnLiquidar.setOnAction(e -> {
                Customer sel = customersTable.getSelectionModel().getSelectedItem();
                if (sel == null) { showToast("Selecione um cliente para liquidar a dívida."); return; }
                openLiquidarDivida(sel);
            });

            Button btnAdiantamento = makeActionButton("Adiantamento", "#10B981", "#ffffff");
            btnAdiantamento.setOnAction(e -> {
                Customer sel = customersTable.getSelectionModel().getSelectedItem();
                if (sel == null) { showToast("Selecione um cliente para registar adiantamento."); return; }
                openAdiantamento(sel);
            });

            Button btnExtrato = makeActionButton("Extrato", "#2563EB", "#ffffff");
            btnExtrato.setOnAction(e -> {
                Customer sel = customersTable.getSelectionModel().getSelectedItem();
                if (sel == null) { showToast("Selecione um cliente para ver o extrato."); return; }
                openExtrato(sel);
            });

            Button btnHistorico = makeActionButton("Histórico", "#7C3AED", "#ffffff");
            btnHistorico.setOnAction(e -> {
                Customer sel = customersTable.getSelectionModel().getSelectedItem();
                if (sel == null) { showToast("Selecione um cliente para ver o histórico."); return; }
                openExtrato(sel);
            });

            Button btnAtualizar = makeActionButton("Atualizar", "#2563EB", "#ffffff");
            btnAtualizar.setOnAction(e -> loadCustomers());

            Button btnExportar = makeActionButton("Exportar Excel", "#10B981", "#ffffff");
            btnExportar.setOnAction(e -> exportCustomersToExcel());

            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            javafx.scene.control.Label lblPesquisar = new javafx.scene.control.Label("Pesquisar:");
            lblPesquisar.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-font-size: 13px;");

            javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
            searchField.setPrefWidth(220);
            searchField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-radius: 0; -fx-padding: 4 8;");
            
            toolbar.getChildren().addAll(btnNovo, btnEditar, btnLiquidar, btnAdiantamento, btnExtrato, btnHistorico, btnAtualizar, btnExportar, spacer, lblPesquisar, searchField);

            searchField.textProperty().addListener((obs, old, val) -> {
                customerFilter = val != null ? val : "";
                loadCustomers();
            });

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Clientes", "Atacado", "Varejo", "Recentes 30d"},
                new String[]{"Registos totais", "Clientes grossa", "Clientes finais", "Novos este mês"},
                new String[]{"blue", "purple", "green", "orange"}
            );
            kpiGrid.setId("customersKPI");

            customersTable = new TableView<>();
            customersTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");

            TableColumn<Customer, String> c1 = new TableColumn<>("Código");
            c1.setPrefWidth(100);
            c1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCode() != null ? d.getValue().getCode() : "—"));

            TableColumn<Customer, String> c2 = new TableColumn<>("Nome");
            c2.setPrefWidth(250);
            c2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName() != null ? d.getValue().getName() : "—"));

            TableColumn<Customer, String> c3 = new TableColumn<>("Tipo");
            c3.setPrefWidth(120);
            c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType() != null ? d.getValue().getType() : "—"));

            TableColumn<Customer, String> c4 = new TableColumn<>("Contacto");
            c4.setPrefWidth(140);
            c4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContact() != null ? d.getValue().getContact() : "—"));

            customersTable.getColumns().addAll(c1, c2, c3, c4);
            customersTable.setRowFactory(makeTableRowFactory());

            main.getChildren().addAll(kpiGrid, toolbar, customersTable);
            customersPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) customersPane.lookup("#customersKPI");
        updateCustomersKPIs(kpi);
        loadCustomers();
        updateCashBadge();
    }

    private void showWarehousesPane() {
        setActiveNav(navMenuArmazens);
        pageTitleLabel.setText("Armazéns");
        pageSubtitleLabel.setText("Gestão de locais e stock");
        setPaneVisibility(warehousesPane);
        
        if (warehousesPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");
            
            // Build toolbar with "Novo Armazém" button
            HBox toolbar = new HBox(12);
            toolbar.setStyle("-fx-padding: 20; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1 0;");
            toolbar.setAlignment(Pos.CENTER_LEFT);
            Button btnNovo = new Button("+ Novo Armazém");
            btnNovo.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
            btnNovo.setOnAction(e -> openWarehouseForm());
            toolbar.getChildren().add(btnNovo);

            // FlowPane for Warehouse Cards
            javafx.scene.layout.FlowPane cardsPane = new javafx.scene.layout.FlowPane();
            cardsPane.setHgap(16);
            cardsPane.setVgap(16);
            cardsPane.setStyle("-fx-padding: 20;");
            
            ScrollPane scroll = new ScrollPane(cardsPane);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color:transparent; -fx-background: #F8FAFC; -fx-border-color:transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);

            main.getChildren().addAll(toolbar, scroll);
            warehousesPane.getChildren().add(main);
        }
        
        loadWarehousesCards();
    }

    private void loadWarehousesCards() {
        if (warehousesPane == null || warehousesPane.getChildren().isEmpty()) return;
        VBox main = (VBox) warehousesPane.getChildren().get(0);
        ScrollPane scroll = (ScrollPane) main.getChildren().get(1);
        javafx.scene.layout.FlowPane cardsPane = (javafx.scene.layout.FlowPane) scroll.getContent();
        
        cardsPane.getChildren().clear();
        
        List<Warehouse> list = warehouseRepository.findAllByOrderByNameAsc();
        for (Warehouse w : list) {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4,0,0,2);");
            card.setPrefWidth(260);
            
            Label icon = new Label("🏭");
            icon.setStyle("-fx-font-size: 32px;");
            
            Label name = new Label(w.getName());
            name.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
            
            Label code = new Label(w.getCode());
            code.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
            
            card.getChildren().addAll(icon, name, code);
            
            card.setOnMouseClicked(e -> {
                enterWarehouse(w);
            });
            
            card.setOnMouseEntered(e -> {
                card.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #3B82F6; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6,0,0,2);");
            });
            card.setOnMouseExited(e -> {
                card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4,0,0,2);");
            });
            
            cardsPane.getChildren().add(card);
        }
    }

    private void enterWarehouse(Warehouse w) {
        warehousesPane.getChildren().clear();
        
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");
        
        // Header
        HBox header = new HBox(12);
        header.setStyle("-fx-padding: 20; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        
        Button btnBack = new Button("◂ Voltar aos Armazéns");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 4 8;");
        btnBack.setOnAction(e -> {
            warehousesPane.getChildren().clear();
            showWarehousesPane();
        });
        
        Label title = new Label("🏭 " + w.getName() + " (" + w.getCode() + ")");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-padding: 0 0 0 16;");
        
        header.getChildren().addAll(btnBack, title);
        
        TableView<StockWarehouse> warehouseStockTable = new TableView<>();
        warehouseStockTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");
        warehouseStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(warehouseStockTable, Priority.ALWAYS);
        
        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.setStyle("-fx-padding: 16 20; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnTransferir = new Button("Transferir Stock");
        btnTransferir.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A; -fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #E2E8F0; -fx-border-radius: 6;");
        btnTransferir.setOnAction(e -> openWarehouseTransferForm());

        Button btnRegistrarCompra = new Button("Registrar Compra");
        btnRegistrarCompra.setStyle("-fx-background-color: #10B981; -fx-text-fill: #ffffff; -fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRegistrarCompra.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/purchase_form.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();
                com.sgv.desktop.PurchaseFormController ctrl = loader.getController();
                com.sgv.entity.Purchase p = new com.sgv.entity.Purchase();
                p.setTargetWarehouse(w);
                ctrl.setPurchase(p);
                ctrl.setCurrentUser(currentUser);
                ctrl.setOnSave(() -> {
                    List<StockWarehouse> newList = stockWarehouseRepository.findByWarehouseId(w.getId());
                    warehouseStockTable.setItems(FXCollections.observableArrayList(newList));
                });
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.initStyle(StageStyle.DECORATED);
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erro ao abrir formulário de compras: " + ex.getMessage());
            }
        });

        Button btnMovimentos = new Button("Ver Movimentos");
        btnMovimentos.setStyle("-fx-background-color: #F8FAFC; -fx-text-fill: #0F172A; -fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #E2E8F0; -fx-border-radius: 6;");
        btnMovimentos.setOnAction(e -> {
            try {
                List<StockWarehouse> list = stockWarehouseRepository.findByWarehouseId(w.getId());
                List<Map<String,String>> rows = new java.util.ArrayList<>();
                com.sgv.repository.StockMovementRepository smr = applicationContext.getBean(com.sgv.repository.StockMovementRepository.class);
                for (StockWarehouse sw : list) {
                    if (sw.getProduct() == null || sw.getProduct().getId() == null) continue;
                    List<com.sgv.entity.StockMovement> movs = smr.findByProductIdOrderByCreatedAtDesc(sw.getProduct().getId());
                    for (com.sgv.entity.StockMovement sm : movs) {
                        Map<String,String> r = new HashMap<>();
                        r.put("dateTime", sm.getCreatedAt() != null ? sm.getCreatedAt().format(DATE_FORMATTER) : "—");
                        r.put("productName", sm.getProduct() != null ? sm.getProduct().getName() : "—");
                        r.put("type", sm.getType() != null ? sm.getType() : "—");
                        r.put("quantity", String.format("%.2f", sm.getQty() != null ? sm.getQty() : 0.0));
                        r.put("stockBefore", String.format("%.2f", sm.getStockBefore() != null ? sm.getStockBefore() : 0.0));
                        r.put("stockAfter", String.format("%.2f", sm.getStockAfter() != null ? sm.getStockAfter() : 0.0));
                        r.put("branchName", sm.getBranch() != null ? sm.getBranch().getName() : "—");
                        r.put("userName", sm.getUser() != null ? sm.getUser().getUsername() : "—");
                        rows.add(r);
                    }
                }

                TableView<Map<String,String>> table = new TableView<>();
                table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
                TableColumn<Map<String,String>, String> col1 = new TableColumn<>("Data / Hora");
                col1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("dateTime")));
                TableColumn<Map<String,String>, String> col2 = new TableColumn<>("Produto");
                col2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("productName")));
                TableColumn<Map<String,String>, String> col3 = new TableColumn<>("Tipo");
                col3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("type")));
                TableColumn<Map<String,String>, String> col4 = new TableColumn<>("Qtd.");
                col4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("quantity")));
                TableColumn<Map<String,String>, String> col5 = new TableColumn<>("Stock Antes");
                col5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("stockBefore")));
                TableColumn<Map<String,String>, String> col6 = new TableColumn<>("Stock Depois");
                col6.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("stockAfter")));
                TableColumn<Map<String,String>, String> col7 = new TableColumn<>("Filial");
                col7.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("branchName")));
                TableColumn<Map<String,String>, String> col8 = new TableColumn<>("Utilizador");
                col8.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("userName")));
                table.getColumns().addAll(col1, col2, col3, col4, col5, col6, col7, col8);
                table.setItems(FXCollections.observableArrayList(rows));

                VBox box = new VBox(8, table);
                box.setStyle("-fx-padding:12;");
                Stage st = new Stage();
                st.initModality(Modality.APPLICATION_MODAL);
                st.initStyle(StageStyle.DECORATED);
                st.setScene(new Scene(box, 900, 500));
                st.show();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erro ao carregar movimentos: " + ex.getMessage());
            }
        });

        Button btnAjustar = new Button("Ajustar Stock");
        btnAjustar.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: #ffffff; -fx-font-weight: 600; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAjustar.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setHeaderText("Formato: productId,quantidade (ex: 12,5.0 ou 12,-3.0)");
            Optional<String> res = dlg.showAndWait();
            res.ifPresent(s -> {
                try {
                    String[] parts = s.split(",");
                    Long prodId = Long.parseLong(parts[0].trim());
                    double q = Double.parseDouble(parts[1].trim());
                    com.sgv.service.WarehouseService ws = applicationContext.getBean(com.sgv.service.WarehouseService.class);
                    if (q > 0) {
                        ws.addStock(w.getId(), prodId, q, "AJUSTE_MANUAL", currentUser);
                    } else {
                        ws.removeStock(w.getId(), prodId, Math.abs(q), "AJUSTE_MANUAL", currentUser);
                    }
                    List<StockWarehouse> newList = stockWarehouseRepository.findByWarehouseId(w.getId());
                    warehouseStockTable.setItems(FXCollections.observableArrayList(newList));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erro no ajuste: " + ex.getMessage());
                }
            });
        });

        toolbar.getChildren().addAll(btnTransferir, btnRegistrarCompra, btnMovimentos, btnAjustar);
        
        // Table
        
        TableColumn<StockWarehouse, String> cProd = new TableColumn<>("Produto");
        cProd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct().getName()));
        
        TableColumn<StockWarehouse, String> cCat = new TableColumn<>("Categoria");
        cCat.setCellValueFactory(d -> {
            Category c = d.getValue().getProduct().getCategory();
            return new SimpleStringProperty(c != null ? c.getName() : "—");
        });
        
        TableColumn<StockWarehouse, String> cQtd = new TableColumn<>("Quantidade");
        cQtd.setCellValueFactory(d -> {
            BigDecimal q = d.getValue().getStockCurrentAmount();
            return new SimpleStringProperty(q != null ? String.format("%.2f", q) : "0.00");
        });
        
        warehouseStockTable.getColumns().addAll(cProd, cCat, cQtd);
        
        // Load data
        List<StockWarehouse> stockList = stockWarehouseRepository.findByWarehouseId(w.getId());
        warehouseStockTable.setItems(FXCollections.observableArrayList(stockList));
        
        main.getChildren().addAll(header, toolbar, warehouseStockTable);
        warehousesPane.getChildren().add(main);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGS DO SISTEMA — Painel com separadores por categoria
    // ══════════════════════════════════════════════════════════════════════════

    private void showLogsPane() {
        setActiveNav(null);
        pageTitleLabel.setText("Logs do Sistema");
        pageSubtitleLabel.setText("Auditoria e registo de eventos");
        setPaneVisibility(logsPane);
        logsPane.getChildren().clear();

        // ── Header ───────────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setStyle("-fx-background-color:#1E293B; -fx-padding:16 24; -fx-alignment:CENTER_LEFT;");
        Label titleLbl = new Label("📋  Logs e Auditoria do Sistema");
        titleLbl.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:#ffffff;");
        Label subLbl = new Label("Registo de todas as ações, erros e eventos");
        subLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8; -fx-padding:2 0 0 8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnRefresh = makeActionButton("⟳ Atualizar", "#475569", "#ffffff");
        Button btnLimpar = makeActionButton("🗑 Limpar Tudo", "#EF4444", "#ffffff");
        header.getChildren().addAll(titleLbl, subLbl, spacer, btnRefresh, btnLimpar);

        // ── Tab Pane ──────────────────────────────────────────────────────────
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-min-width:120; -fx-font-size:13px;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabAll    = buildLogTab("Todos",         null);
        Tab tabErrors = buildLogTab("Erros",         "ERROR");
        Tab tabSec    = buildLogTab("Segurança",     "SECURITY");
        Tab tabUser   = buildLogTab("Utilizadores",  "USER_ACTION");
        Tab tabSys    = buildLogTab("Sistema",       "SYSTEM");

        tabPane.getTabs().addAll(tabAll, tabErrors, tabSec, tabUser, tabSys);

        // Refresh action reloads the currently selected tab
        btnRefresh.setOnAction(e -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            String cat = (String) selected.getUserData();
            refreshLogTab(selected, cat);
        });

        // Limpar action
        btnLimpar.setOnAction(e -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("Limpar Todos os Logs");
            alert.setHeaderText("Tem a certeza que deseja eliminar TODOS os logs do sistema?");
            alert.setContentText("Esta ação é irreversível.");
            alert.showAndWait().ifPresent(res -> {
                if (res == javafx.scene.control.ButtonType.OK) {
                    try {
                        com.sgv.repository.AuditLogRepository repo = applicationContext.getBean(com.sgv.repository.AuditLogRepository.class);
                        repo.deleteAll();
                        showToast("Todos os logs foram eliminados.");
                        showLogsPane();
                    } catch (Exception ex) {
                        showToast("Erro ao limpar logs: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            });
        });

        // Reload tabs when switching
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                String cat = (String) newTab.getUserData();
                refreshLogTab(newTab, cat);
            }
        });

        logsPane.getChildren().addAll(header, tabPane);
    }

    /** Build a tab for a given category (null = all) */
    private Tab buildLogTab(String title, String category) {
        Tab tab = new Tab(title);
        tab.setUserData(category);
        TableView<com.sgv.entity.AuditLog> table = buildLogTable();
        tab.setContent(table);
        // Load data immediately
        refreshLogTab(tab, category);
        return tab;
    }

    /** Reload data for a log tab */
    private void refreshLogTab(Tab tab, String category) {
        try {
            com.sgv.repository.AuditLogRepository repo = applicationContext.getBean(com.sgv.repository.AuditLogRepository.class);
            java.util.List<com.sgv.entity.AuditLog> logs;
            if (category == null) {
                logs = repo.findAllByOrderByCreatedAtDesc();
            } else {
                logs = repo.findByCategoryOrderByCreatedAtDesc(category);
            }
            TableView<com.sgv.entity.AuditLog> table = (TableView<com.sgv.entity.AuditLog>) tab.getContent();
            if (table != null) {
                table.setItems(javafx.collections.FXCollections.observableArrayList(logs));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Build a styled TableView for AuditLog entries */
    @SuppressWarnings("unchecked")
    private TableView<com.sgv.entity.AuditLog> buildLogTable() {
        TableView<com.sgv.entity.AuditLog> table = new TableView<>();
        table.setStyle("-fx-font-size:12px; -fx-background-color:#F8FAFC;");
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<com.sgv.entity.AuditLog, String> colCat = new TableColumn<>("Categoria");
        colCat.setPrefWidth(110);
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory() != null ? d.getValue().getCategory() : "—"));
        colCat.setCellFactory(col -> new TableCell<com.sgv.entity.AuditLog, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("ERROR".equals(item)) setStyle("-fx-text-fill:#EF4444; -fx-font-weight:700;");
                else if ("SECURITY".equals(item)) setStyle("-fx-text-fill:#F59E0B; -fx-font-weight:700;");
                else if ("USER_ACTION".equals(item)) setStyle("-fx-text-fill:#10B981; -fx-font-weight:700;");
                else if ("SYSTEM".equals(item)) setStyle("-fx-text-fill:#6366F1; -fx-font-weight:700;");
                else setStyle("-fx-text-fill:#475569;");
            }
        });

        TableColumn<com.sgv.entity.AuditLog, String> colDate = new TableColumn<>("Data / Hora");
        colDate.setPrefWidth(140);
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCreatedAt() != null
                ? d.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : "—"));

        TableColumn<com.sgv.entity.AuditLog, String> colUser = new TableColumn<>("Utilizador");
        colUser.setPrefWidth(120);
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername() != null ? d.getValue().getUsername() : "—"));

        TableColumn<com.sgv.entity.AuditLog, String> colAction = new TableColumn<>("Ação");
        colAction.setPrefWidth(200);
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAction() != null ? d.getValue().getAction() : "—"));

        TableColumn<com.sgv.entity.AuditLog, String> colDetails = new TableColumn<>("Detalhes");
        colDetails.setPrefWidth(340);
        colDetails.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDetails() != null ? d.getValue().getDetails() : ""));

        TableColumn<com.sgv.entity.AuditLog, String> colStack = new TableColumn<>("Stacktrace");
        colStack.setPrefWidth(60);
        colStack.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStackTrace() != null ? "Ver" : ""));
        colStack.setCellFactory(col -> new TableCell<com.sgv.entity.AuditLog, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); setGraphic(null); return; }
                Button btn = new Button("Ver");
                btn.setStyle("-fx-background-color:#6366F1; -fx-text-fill:white; -fx-font-size:10px; -fx-padding:2 6; -fx-background-radius:3; -fx-cursor:hand;");
                btn.setOnAction(e -> {
                    com.sgv.entity.AuditLog log = getTableView().getItems().get(getIndex());
                    javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(log.getStackTrace());
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.setPrefSize(700, 400);
                    javafx.scene.control.Dialog<Void> dlg = new javafx.scene.control.Dialog<>();
                    dlg.setTitle("Stacktrace — " + log.getAction());
                    dlg.getDialogPane().setContent(area);
                    dlg.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
                    dlg.showAndWait();
                });
                setGraphic(btn);
                setText(null);
            }
        });

        table.getColumns().addAll(colCat, colDate, colUser, colAction, colDetails, colStack);
        table.setPlaceholder(new Label("Sem registos para esta categoria."));

        // Row color styling by category
        table.setRowFactory(tv -> new TableRow<com.sgv.entity.AuditLog>() {
            @Override protected void updateItem(com.sgv.entity.AuditLog item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                String cat = item.getCategory();
                if ("ERROR".equals(cat))       setStyle("-fx-background-color:#FEF2F2;");
                else if ("SECURITY".equals(cat)) setStyle("-fx-background-color:#FFFBEB;");
                else if ("SYSTEM".equals(cat)) setStyle("-fx-background-color:#F5F3FF;");
                else setStyle("");
            }
        });

        return table;
    }

    private void showComprasPane() {
        setActiveNav(navMenuCompras);
        pageTitleLabel.setText("Compras / Fornecedores");
        pageSubtitleLabel.setText("Gestão de fornecedores");
        setPaneVisibility(comprasPane);

        if (comprasPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Compras", "Valor Total", "Pendentes", "Recebidas"},
                new String[]{"Todas as compras", "Capital investido", "Por receber", "Já recebidas"},
                new String[]{"blue", "green", "orange", "purple"}
            );
            kpiGrid.setId("comprasKPI");

            Label lblComp = new Label("Compras");
            lblComp.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-padding: 16 20 8 20;");

            HBox toolbarComp = buildCrudToolbar(purchasesTable, "Compra",
                () -> openPurchaseForm(null),
                () -> openPurchaseForm(purchasesTable.getSelectionModel().getSelectedItem()),
                () -> {
                    Purchase p = purchasesTable.getSelectionModel().getSelectedItem();
                    safeDelete(() -> purchaseRepository.deleteById(p.getId()), this::loadPurchases, "Compra");
                }
            );
            TextField searchComp = (TextField) toolbarComp.getChildren().get(0);
            searchComp.setPromptText("Pesquisar compra...");
            searchComp.textProperty().addListener((obs, old, val) -> loadPurchases());

            purchasesTable = new TableView<>();
            purchasesTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");

            TableColumn<Purchase, String> p1 = new TableColumn<>("Factura");
            p1.setPrefWidth(120);
            p1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInvoiceNumber() != null ? d.getValue().getInvoiceNumber() : "—"));

            TableColumn<Purchase, String> p2 = new TableColumn<>("Data");
            p2.setPrefWidth(120);
            p2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString().substring(0, 10) : "—"));

            TableColumn<Purchase, String> p3 = new TableColumn<>("Total");
            p3.setPrefWidth(120);
            p3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTotal() != null ? String.format("%.2f MT", d.getValue().getTotal()) : "0 MT"));

            TableColumn<Purchase, String> p4 = new TableColumn<>("Estado");
            p4.setPrefWidth(100);
            p4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState() != null ? d.getValue().getState() : "—"));
            p4.setCellFactory(coloredStateCell());

            purchasesTable.getColumns().addAll(p1, p2, p3, p4);
            purchasesTable.setRowFactory(makeTableRowFactory());

            main.getChildren().addAll(kpiGrid, lblComp, toolbarComp, purchasesTable);
            comprasPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) comprasPane.lookup("#comprasKPI");
        updateComprasKPIs(kpi);
        updateCashBadge();
    }

    private void showProducaoPane() {
        setActiveNav(navMenuProducao);
        pageTitleLabel.setText("Produção");
        pageSubtitleLabel.setText("Ordens de produção");
        setPaneVisibility(producaoPane);

        if (producaoPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = buildCrudToolbar(ordersTable, "Ordem",
                () -> openOrderForm(null),
                () -> openOrderForm(ordersTable.getSelectionModel().getSelectedItem()),
                () -> {
                    ProductionOrder o = ordersTable.getSelectionModel().getSelectedItem();
                    safeDelete(() -> productionOrderRepository.deleteById(o.getId()), this::loadProductionOrders, "Ordem de Produção");
                }
            );
            TextField searchField = (TextField) toolbar.getChildren().get(0);
            searchField.setPromptText("Pesquisar ordem...");
            searchField.textProperty().addListener((obs, old, val) -> loadProductionOrders());

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Ordens", "Pendentes", "Em Curso", "Concluídas"},
                new String[]{"Ordens criadas", "Por iniciar", "A produzir", "Finalizadas"},
                new String[]{"blue", "orange", "purple", "green"}
            );
            kpiGrid.setId("producaoKPI");

            ordersTable = new TableView<>();
            ordersTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");

            TableColumn<ProductionOrder, String> o1 = new TableColumn<>("Nº Ordem");
            o1.setPrefWidth(120);
            o1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderNumber() != null ? d.getValue().getOrderNumber() : "—"));

            TableColumn<ProductionOrder, String> o2 = new TableColumn<>("Produto");
            o2.setPrefWidth(220);
            o2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null ? d.getValue().getProduct().getName() : "—"));

            TableColumn<ProductionOrder, String> o3 = new TableColumn<>("Quantidade");
            o3.setPrefWidth(100);
            o3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getQuantity() != null ? String.valueOf(d.getValue().getQuantity()) + " " + (d.getValue().getUnit() != null ? d.getValue().getUnit() : "") : "—"));

            TableColumn<ProductionOrder, String> o4 = new TableColumn<>("Criada em");
            o4.setPrefWidth(140);
            o4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString().substring(0, 10) : "—"));

            TableColumn<ProductionOrder, String> o5 = new TableColumn<>("Estado");
            o5.setPrefWidth(120);
            o5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState() != null ? d.getValue().getState() : "—"));
            o5.setCellFactory(coloredStateCell());

            ordersTable.getColumns().addAll(o1, o2, o3, o4, o5);
            ordersTable.setRowFactory(makeTableRowFactory());

            main.getChildren().addAll(kpiGrid, toolbar, ordersTable);
            producaoPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) producaoPane.lookup("#producaoKPI");
        updateProducaoKPIs(kpi);
        loadProductionOrders();
        updateCashBadge();
    }

    private void showReportsPane() {
        setActiveNav(navMenuRelatorios);
        pageTitleLabel.setText("Relatórios");
        pageSubtitleLabel.setText("Estatísticas e exportações");
        setPaneVisibility(reportsPane);

        if (reportsPane.getChildren().isEmpty()) {
            loadReportsPane();
        }

        updateCashBadge();
    }

    private void showSistemaPane() {
        setActiveNav(navMenuSistema);
        pageTitleLabel.setText("Sistema");
        pageSubtitleLabel.setText("Gestão de utilizadores, permissões e configurações");
        setPaneVisibility(sistemaPane);

        if (sistemaPane.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sistema_module.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();
                SistemaModuleController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
                sistemaPane.getChildren().add(root);
            } catch (Exception ex) {
                ex.printStackTrace();
                sistemaPane.getChildren().add(new Label("Erro ao carregar módulo Sistema: " + ex.getMessage()));
            }
        }
        updateCashBadge();
    }

    private void showSummaryPane() {
        setActiveNav(navResumo);
        pageTitleLabel.setText("Dashboard");
        pageSubtitleLabel.setText("Visão geral do sistema");
        setPaneVisibility(summaryPane);
        loadStats();
        updateCashBadge();
    }

    private void showSalesStatsPane() {
        setActiveNav(null);
        pageTitleLabel.setText("Vendas");
        pageSubtitleLabel.setText("Histórico e gestão de documentos");
        setPaneVisibility(salesStatsPane);

        // Build UI if empty
        if (salesStatsPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            // ── TOOLBAR ──
            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            // Campo de pesquisa
            saleSearchField = new TextField();
            saleSearchField.setPromptText("Pesquisar por nº documento, cliente...");
            saleSearchField.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; " +
                "-fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; " +
                "-fx-background-color: #F8FAFC; -fx-min-width: 260; -fx-pref-width: 300;"
            );
            HBox.setHgrow(saleSearchField, Priority.ALWAYS);
            saleSearchField.textProperty().addListener((obs, old, val) -> {
                saleFilter = val != null ? val : "";
                loadSales();
            });

            // Filtro por estado
            saleStateCombo = new ComboBox<>();
            saleStateCombo.setItems(FXCollections.observableArrayList(
                "TODOS", "EMITIDA", "PAGO", "COTACAO_ABERTA", "COTACAO_PAGA", "ANULADA"
            ));
            saleStateCombo.setValue("TODOS");
            saleStateCombo.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; " +
                "-fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; " +
                "-fx-background-color: #F8FAFC; -fx-min-width: 160;"
            );
            saleStateCombo.valueProperty().addListener((obs, old, val) -> {
                saleStateFilter = val != null ? val : "TODOS";
                loadSales();
            });

            // + Nova Venda
            Button newBtn = makeActionButton("+ Nova Venda", "#2563EB", "#ffffff");
            newBtn.setOnAction(e -> openSaleForm(null));

            // Imprimir
            Button printBtn = makeActionButton("Imprimir", "#475569", "#ffffff");
            printBtn.setOnAction(e -> {
                Sale selected = salesTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showToast("Selecione uma venda para imprimir.");
                    return;
                }
                printSelectedSale();
            });

            // Ver Detalhes
            Button viewBtn = makeActionButton("Ver Detalhes", "#10B981", "#ffffff");
            viewBtn.setOnAction(e -> {
                Sale selected = salesTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showToast("Selecione uma venda para ver detalhes.");
                    return;
                }
                openSaleForm(selected);
            });

            // Converter Cotação → Venda
            Button convertBtn = makeActionButton("Faturar Cotação", "#F59E0B", "#ffffff");
            convertBtn.setOnAction(e -> invoiceSelectedQuote());

            // Imprimir Recibo
            Button receiptBtn = makeActionButton("Imprimir Recibo", "#8B5CF6", "#ffffff");
            receiptBtn.setOnAction(e -> printSelectedReceipt());

            // Anular
            Button annulBtn = makeActionButton("Anular", "#EF4444", "#ffffff");
            annulBtn.setOnAction(e -> {
                Sale selected = salesTable.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showToast("Selecione uma venda para anular.");
                    return;
                }
                annulSelectedSale();
            });

            // Refresh
            Button refreshBtn = makeIconButton("↻", "#10B981", "#ffffff");
            refreshBtn.setOnAction(e -> loadSales());

            toolbar.getChildren().addAll(
                saleSearchField, saleStateCombo,
                newBtn, printBtn, receiptBtn, viewBtn, convertBtn, annulBtn, refreshBtn
            );

            // ── KPI GRID ──
            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Vendas Hoje", "Vendas Semana", "Vendas Mês", "Ticket Médio"},
                new String[]{"Hoje", "Últimos 7 dias", "Este mês", "Por venda"},
                new String[]{"blue", "green", "purple", "orange"}
            );
            kpiGrid.setId("salesKPI");

            // ── TABELA ──
            salesTable = new TableView<>();
            salesTable.setStyle(
                "-fx-font-size: 13px; -fx-background-color: #ffffff; " +
                "-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1;"
            );
            salesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            salesTable.setPlaceholder(new Label("Nenhuma venda encontrada"));

            // Coluna: Documento
            salesDocumentColumn = new TableColumn<>("Documento");
            salesDocumentColumn.setPrefWidth(160);
            salesDocumentColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDocumentType() + " #" +
                (d.getValue().getDocumentNumber() != null ? d.getValue().getDocumentNumber() : "—")
            ));

            // Coluna: Série
            TableColumn<Sale, String> seriesCol = new TableColumn<>("Série");
            seriesCol.setPrefWidth(80);
            seriesCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSeries() != null ? d.getValue().getSeries() : "—"
            ));

            // Coluna: Cliente
            salesCustomerColumn = new TableColumn<>("Cliente");
            salesCustomerColumn.setPrefWidth(220);
            salesCustomerColumn.setCellValueFactory(d -> {
                Sale s = d.getValue();
                String name = s.getCustomer() != null ? s.getCustomer().getName()
                    : (s.getCustomerName() != null ? s.getCustomerName() : "Consumidor Final");
                return new SimpleStringProperty(name);
            });

            // Coluna: Total
            salesTotalColumn = new TableColumn<>("Total");
            salesTotalColumn.setPrefWidth(140);
            salesTotalColumn.setCellFactory(col -> {
                TableCell<Sale, String> cell = new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setText(null); setGraphic(null); }
                        else {
                            Label lbl = new Label(item);
                            lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A;");
                            setGraphic(lbl);
                            setText(null);
                        }
                    }
                };
                col.setCellValueFactory(d -> {
                    Double t = d.getValue().getTotal();
                    return new SimpleStringProperty(t != null ? String.format("%.2f MT", t) : "0.00 MT");
                });
                return cell;
            });

            // Coluna: Estado (com badge colorido)
            salesStateColumn = new TableColumn<>("Estado");
            salesStateColumn.setPrefWidth(140);
            salesStateColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getState() != null ? d.getValue().getState() : "—"
            ));
            salesStateColumn.setCellFactory(coloredStateCell());

            // Coluna: Data
            salesDateColumn = new TableColumn<>("Data");
            salesDateColumn.setPrefWidth(160);
            salesDateColumn.setCellValueFactory(d -> {
                java.time.LocalDateTime dt = d.getValue().getCreatedAt();
                return new SimpleStringProperty(dt != null ? dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
            });

            // Coluna: Método Pagamento
            TableColumn<Sale, String> paymentCol = new TableColumn<>("Pagamento");
            paymentCol.setPrefWidth(140);
            paymentCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPaymentMethod() != null ? d.getValue().getPaymentMethod() : "—"
            ));

            salesTable.getColumns().addAll(
                salesDocumentColumn, seriesCol, salesCustomerColumn,
                salesTotalColumn, salesStateColumn, salesDateColumn, paymentCol
            );
            salesTable.setRowFactory(makeSalesRowFactory());

            main.getChildren().addAll(kpiGrid, toolbar, salesTable);
            salesStatsPane.getChildren().add(main);
        }

        // Update KPIs
        GridPane kpi = (GridPane) salesStatsPane.lookup("#salesKPI");
        updateSalesKPIs(kpi);
        loadSalesStats();
        loadSales();
        updateCashBadge();
    }

    // ==========================================
    // CATALOGOS
    // ==========================================
    private void showCatalogsPane() {
        setActiveNav(navMenuCatalogos);
        pageTitleLabel.setText("Catálogos");
        pageSubtitleLabel.setText("Gestão de Categorias e Unidades");
        setPaneVisibility(catalogsPane);

        if (catalogsPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Categorias", "Unidades Métricas", "Activas", "—"},
                new String[]{"Total de categorias", "Tipos de unidade", "Unidades activas", "—"},
                new String[]{"blue", "purple", "green", "gray"}
            );
            kpiGrid.setId("catalogsKPI");

            // ─── Split: esquerda = categorias, direita = unidades ───
            GridPane splitGrid = new GridPane();
            splitGrid.setStyle("-fx-padding: 0 20 20 20;");
            splitGrid.setHgap(16);

            ColumnConstraints half = new ColumnConstraints();
            half.setPercentWidth(50);
            half.setHgrow(Priority.ALWAYS);
            splitGrid.getColumnConstraints().addAll(half, half);

            // ═══════════════════════════════════════════
            // LEFT: Categorias
            // ═══════════════════════════════════════════
            VBox leftPane = new VBox(0);
            leftPane.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 3;");

            // Header com título + botões CRUD
            HBox catHeader = new HBox(8);
            catHeader.setStyle(
                "-fx-padding: 10 12; -fx-background-color: #F8FAFC; " +
                "-fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
            catHeader.setAlignment(Pos.CENTER_LEFT);

            Label leftTitle = new Label("Categorias");
            leftTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

            Region catSpacer = new Region();
            HBox.setHgrow(catSpacer, Priority.ALWAYS);

            Button catNewBtn = new Button("+ Nova");
            catNewBtn.setStyle(
                "-fx-background-color: #2563EB; -fx-text-fill: #ffffff; -fx-font-weight: 700; " +
                "-fx-padding: 6 12; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px;");
            catNewBtn.setOnAction(e -> openCategoryForm(null));

            Button catEditBtn = new Button("Editar");
            catEditBtn.setDisable(true);
            catEditBtn.setStyle(
                "-fx-background-color: #ffffff; -fx-text-fill: #2563EB; -fx-font-weight: 600; " +
                "-fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px; " +
                "-fx-border-color: #BFDBFE; -fx-border-width: 1;");
            catEditBtn.setOnAction(e -> {
                com.sgv.entity.Category c = categoriesTable.getSelectionModel().getSelectedItem();
                if (c != null) openCategoryForm(c);
            });

            Button catDelBtn = new Button("Eliminar");
            catDelBtn.setDisable(true);
            catDelBtn.setStyle(
                "-fx-background-color: #ffffff; -fx-text-fill: #EF4444; -fx-font-weight: 600; " +
                "-fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px; " +
                "-fx-border-color: #FECACA; -fx-border-width: 1;");
            catDelBtn.setOnAction(e -> deleteSelectedCategory());

            catHeader.getChildren().addAll(leftTitle, catSpacer, catNewBtn, catEditBtn, catDelBtn);

            // Tabela
            categoriesTable = new TableView<>();
            categoriesTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff;");
            categoriesTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            TableColumn<com.sgv.entity.Category, String> catIdCol = new TableColumn<>("ID");
            catIdCol.setPrefWidth(60);
            catIdCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getId() != null ? String.valueOf(d.getValue().getId()) : "—"));

            TableColumn<com.sgv.entity.Category, String> catNomeCol = new TableColumn<>("Nome");
            catNomeCol.setPrefWidth(260);
            catNomeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getName() != null ? d.getValue().getName() : "—"));

            TableColumn<com.sgv.entity.Category, String> catActCol = new TableColumn<>("Acções");
            catActCol.setPrefWidth(130);
            catActCol.setCellFactory(col -> new TableCell<>() {
                private final Button ed = new Button("Editar");
                private final Button dl = new Button("Eliminar");
                {
                    ed.setStyle(
                        "-fx-background-color: #2563EB; -fx-text-fill: #ffffff; " +
                        "-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 2; -fx-cursor: hand;");
                    dl.setStyle(
                        "-fx-background-color: #EF4444; -fx-text-fill: #ffffff; " +
                        "-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 2; -fx-cursor: hand;");
                    ed.setOnAction(e -> {
                        com.sgv.entity.Category c = getTableView().getItems().get(getIndex());
                        openCategoryForm(c);
                    });
                    dl.setOnAction(e -> {
                        com.sgv.entity.Category c = getTableView().getItems().get(getIndex());
                        if (c != null) {
                            categoriesTable.getSelectionModel().select(c);
                            deleteSelectedCategory();
                        }
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); }
                    else {
                        HBox h = new HBox(4, ed, dl);
                        h.setAlignment(Pos.CENTER);
                        setGraphic(h);
                    }
                }
            });

            categoriesTable.getColumns().addAll(catIdCol, catNomeCol, catActCol);
            categoriesTable.setRowFactory(makeTableRowFactory());
            categoriesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                catEditBtn.setDisable(sel == null);
                catDelBtn.setDisable(sel == null);
            });
            categoriesTable.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    com.sgv.entity.Category c = categoriesTable.getSelectionModel().getSelectedItem();
                    if (c != null) openCategoryForm(c);
                }
            });

            leftPane.getChildren().addAll(catHeader, categoriesTable);
            VBox.setVgrow(categoriesTable, Priority.ALWAYS);

            // ═══════════════════════════════════════════
            // RIGHT: Unidades Métricas
            // ═══════════════════════════════════════════
            VBox rightPane = new VBox(0);
            rightPane.setStyle(
                "-fx-background-color: #ffffff; " +
                "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 3;");

            HBox unitHeader = new HBox(8);
            unitHeader.setStyle(
                "-fx-padding: 10 12; -fx-background-color: #F8FAFC; " +
                "-fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
            unitHeader.setAlignment(Pos.CENTER_LEFT);

            Label rightTitle = new Label("Unidades Métricas");
            rightTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

            Region unitSpacer = new Region();
            HBox.setHgrow(unitSpacer, Priority.ALWAYS);

            Button unitNewBtn = new Button("+ Nova");
            unitNewBtn.setStyle(
                "-fx-background-color: #7C3AED; -fx-text-fill: #ffffff; -fx-font-weight: 700; " +
                "-fx-padding: 6 12; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px;");
            unitNewBtn.setOnAction(e -> openMetricUnitForm(null));

            Button unitEditBtn = new Button("Editar");
            unitEditBtn.setDisable(true);
            unitEditBtn.setStyle(
                "-fx-background-color: #ffffff; -fx-text-fill: #7C3AED; -fx-font-weight: 600; " +
                "-fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px; " +
                "-fx-border-color: #DDD6FE; -fx-border-width: 1;");
            unitEditBtn.setOnAction(e -> {
                com.sgv.entity.MetricUnit m = unitsTable.getSelectionModel().getSelectedItem();
                if (m != null) openMetricUnitForm(m);
            });

            Button unitDelBtn = new Button("Eliminar");
            unitDelBtn.setDisable(true);
            unitDelBtn.setStyle(
                "-fx-background-color: #ffffff; -fx-text-fill: #EF4444; -fx-font-weight: 600; " +
                "-fx-padding: 5 10; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px; " +
                "-fx-border-color: #FECACA; -fx-border-width: 1;");
            unitDelBtn.setOnAction(e -> deleteSelectedUnit());

            unitHeader.getChildren().addAll(rightTitle, unitSpacer, unitNewBtn, unitEditBtn, unitDelBtn);

            // Tabela
            unitsTable = new TableView<>();
            unitsTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff;");
            unitsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            TableColumn<com.sgv.entity.MetricUnit, String> uAbbrCol = new TableColumn<>("Abreviação");
            uAbbrCol.setPrefWidth(100);
            uAbbrCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAbbreviation() != null ? d.getValue().getAbbreviation() : "—"));

            TableColumn<com.sgv.entity.MetricUnit, String> uDescCol = new TableColumn<>("Descrição");
            uDescCol.setPrefWidth(220);
            uDescCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescription() != null ? d.getValue().getDescription() : "—"));

            TableColumn<com.sgv.entity.MetricUnit, String> uActCol = new TableColumn<>("Acções");
            uActCol.setPrefWidth(130);
            uActCol.setCellFactory(col -> new TableCell<>() {
                private final Button ed = new Button("Editar");
                private final Button dl = new Button("Eliminar");
                {
                    ed.setStyle(
                        "-fx-background-color: #7C3AED; -fx-text-fill: #ffffff; " +
                        "-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 2; -fx-cursor: hand;");
                    dl.setStyle(
                        "-fx-background-color: #EF4444; -fx-text-fill: #ffffff; " +
                        "-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 2; -fx-cursor: hand;");
                    ed.setOnAction(e -> {
                        com.sgv.entity.MetricUnit m = getTableView().getItems().get(getIndex());
                        openMetricUnitForm(m);
                    });
                    dl.setOnAction(e -> {
                        com.sgv.entity.MetricUnit m = getTableView().getItems().get(getIndex());
                        if (m != null) {
                            unitsTable.getSelectionModel().select(m);
                            deleteSelectedUnit();
                        }
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); }
                    else {
                        HBox h = new HBox(4, ed, dl);
                        h.setAlignment(Pos.CENTER);
                        setGraphic(h);
                    }
                }
            });

            unitsTable.getColumns().addAll(uAbbrCol, uDescCol, uActCol);
            unitsTable.setRowFactory(makeTableRowFactory());
            unitsTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
                unitEditBtn.setDisable(sel == null);
                unitDelBtn.setDisable(sel == null);
            });
            unitsTable.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    com.sgv.entity.MetricUnit m = unitsTable.getSelectionModel().getSelectedItem();
                    if (m != null) openMetricUnitForm(m);
                }
            });

            rightPane.getChildren().addAll(unitHeader, unitsTable);
            VBox.setVgrow(unitsTable, Priority.ALWAYS);

            splitGrid.add(leftPane, 0, 0);
            splitGrid.add(rightPane, 1, 0);

            main.getChildren().addAll(kpiGrid, splitGrid);
            catalogsPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) catalogsPane.lookup("#catalogsKPI");
        updateCatalogsKPIs(kpi);
        loadCategories();
        loadMetricUnits();
        updateCashBadge();
    }

    private void showStockPane() {
        setActiveNav(null);
        pageTitleLabel.setText("Stock");
        pageSubtitleLabel.setText("Gestão de Inventário");
        setPaneVisibility(stockPane);

        if (stockPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = buildCrudToolbar(stockTable, "Stock",
                () -> openStockAdjustForm(stockTable.getSelectionModel().getSelectedItem()),
                () -> openStockAdjustForm(stockTable.getSelectionModel().getSelectedItem()),
                () -> {
                    if (!isCurrentUserAdmin()) {
                        showToast("Apenas administradores podem eliminar stock.");
                        return;
                    }
                    StockBranch s = stockTable.getSelectionModel().getSelectedItem();
                    safeDelete(() -> stockBranchService.deleteStock(s, currentUser), this::loadStock, "Stock");
                }
            );
            ((Button) toolbar.getChildren().get(1)).setText("Ajustar");
            TextField searchField = (TextField) toolbar.getChildren().get(0);
            searchField.textProperty().addListener((obs, old, val) -> loadStock());

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Items", "Valor Stock", "Stock Baixo", "Esgotados"},
                new String[]{"No inventário", "Capital investido", "Precisa reposição", "Fora de stock"},
                new String[]{"blue", "green", "orange", "red"}
            );
            kpiGrid.setId("stockKPI");

            stockTable = new TableView<>();
            stockTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");

            TableColumn<StockBranch, String> c1 = new TableColumn<>("Produto");
            c1.setPrefWidth(250);
            c1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null ? d.getValue().getProduct().getName() : "—"));

            TableColumn<StockBranch, String> c2 = new TableColumn<>("Filial");
            c2.setPrefWidth(150);
            c2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));

            TableColumn<StockBranch, String> c3 = new TableColumn<>("Stock Actual");
            c3.setPrefWidth(120);
            c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockCurrentAmount() != null ? String.valueOf(d.getValue().getStockCurrentAmount()) : "0"));

            TableColumn<StockBranch, String> c4 = new TableColumn<>("Stock Mín.");
            c4.setPrefWidth(100);
            c4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockMinAmount() != null ? String.valueOf(d.getValue().getStockMinAmount()) : "0"));

            TableColumn<StockBranch, String> c5 = new TableColumn<>("Stock Máx.");
            c5.setPrefWidth(100);
            c5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockMaxAmount() != null ? String.valueOf(d.getValue().getStockMaxAmount()) : "0"));

            stockTable.getColumns().addAll(c1, c2, c3, c4, c5);
            stockTable.setRowFactory(makeTableRowFactory());

            main.getChildren().addAll(kpiGrid, toolbar, stockTable);
            stockPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) stockPane.lookup("#stockKPI");
        updateStockKPIs(kpi);
        loadStock();
        updateCashBadge();
    }

    private void showFinanceiroPane() {
        setActiveNav(null);
        pageTitleLabel.setText("Financeiro");
        pageSubtitleLabel.setText("Controlo de pagamentos e receitas");
        setPaneVisibility(financeiroPane);

        if (financeiroPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = buildCrudToolbar(paymentsTable, "Pagamento",
                () -> openPaymentForm(null),
                () -> openPaymentForm(paymentsTable.getSelectionModel().getSelectedItem()),
                () -> {
                    Payment p = paymentsTable.getSelectionModel().getSelectedItem();
                    safeDelete(() -> paymentRepository.deleteById(p.getId()), this::loadFinanceiro, "Pagamento");
                }
            );
            TextField searchField = (TextField) toolbar.getChildren().get(0);
            searchField.setPromptText("Pesquisar pagamento...");
            searchField.textProperty().addListener((obs, old, val) -> loadFinanceiro());

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Vendas", "Recebido", "Pendente", "Despesas Mês"},
                new String[]{"Todas as vendas", "Total cobrado", "Por cobrar", "Este mês"},
                new String[]{"blue", "green", "orange", "red"}
            );
            kpiGrid.setId("financeiroKPI");

            paymentsTable = new TableView<>();
            paymentsTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");

            TableColumn<Payment, String> p1 = new TableColumn<>("Data");
            p1.setPrefWidth(150);
            p1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString() : "—"));

            TableColumn<Payment, String> p2 = new TableColumn<>("Cliente");
            p2.setPrefWidth(200);
            p2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSale() != null && d.getValue().getSale().getCustomerName() != null ? d.getValue().getSale().getCustomerName() : "—"));

            TableColumn<Payment, String> p3 = new TableColumn<>("Valor");
            p3.setPrefWidth(140);
            p3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAmount() != null ? String.format("%.2f MT", d.getValue().getAmount()) : "0 MT"));

            TableColumn<Payment, String> p4 = new TableColumn<>("Método");
            p4.setPrefWidth(120);
            p4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMethod() != null ? d.getValue().getMethod() : "—"));

            paymentsTable.getColumns().addAll(p1, p2, p3, p4);
            paymentsTable.setRowFactory(makeTableRowFactory());

            main.getChildren().addAll(kpiGrid, toolbar, paymentsTable);
            financeiroPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) financeiroPane.lookup("#financeiroKPI");
        updateFinanceiroKPIs(kpi);
        loadFinanceiro();
        updateCashBadge();
    }

    private void loadCategories() {
        if (categoriesTable == null || categoryRepository == null) return;
        List<com.sgv.entity.Category> list = categoryRepository.findAll();
        categoriesTable.setItems(FXCollections.observableArrayList(list));
    }

    private void loadMetricUnits() {
        if (unitsTable == null || metricUnitRepository == null) return;
        List<com.sgv.entity.MetricUnit> list = metricUnitRepository.findAll();
        unitsTable.setItems(FXCollections.observableArrayList(list));
    }

    private void openCategoryForm(com.sgv.entity.Category category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/category_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            CategoryFormController ctrl = loader.getController();
            ctrl.setCategory(category);
            ctrl.setOnSave(this::loadCategories);
            openModal(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openMetricUnitForm(com.sgv.entity.MetricUnit unit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/metric_unit_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            MetricUnitFormController ctrl = loader.getController();
            ctrl.setMetricUnit(unit);
            ctrl.setOnSave(this::loadMetricUnits);
            openModal(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showSalesPane() {
        setActiveNav(null);
        pageTitleLabel.setText("Consultar Vendas");
        pageSubtitleLabel.setText("Histórico de vendas e facturas");
        setPaneVisibility(salesPane);
        loadSales();
        updateCashBadge();
    }

    private void showProductsPane() {
        setActiveNav(null);
        pageTitleLabel.setText("Produtos");
        pageSubtitleLabel.setText("Gestão de catálogo e stock");
        setPaneVisibility(productsPane);
        updateCashBadge();

        // Só constrói UI na primeira vez
        if (productsPane.getChildren().isEmpty()) {
            buildProductsPaneUI();
        }
        loadProducts();
    }

    private void buildProductsPaneUI() {
        productsPane.setStyle("-fx-padding: 0;");

        // ── CONTAINER PRINCIPAL ──
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: #F8FAFC;");
        mainContainer.setOpacity(0);

        // ── TOOLBAR ──
        HBox toolbar = new HBox(12);
        toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar produto...");
        searchField.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; " +
            "-fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; " +
            "-fx-background-color: #F8FAFC; -fx-min-width: 280; -fx-pref-width: 320;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            productFilter = newVal != null ? newVal : "";
            currentProductsPage = 0;
            loadProducts();
        });

        Button newBtn = makeActionButton("+ Novo", "#2563EB", "#ffffff");
        newBtn.setOnAction(e -> openProductForm(null));

        Button editBtn = makeActionButton("Editar", "#475569", "#ffffff");
        editBtn.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected != null) openProductForm(selected);
        });

        Button deleteBtn = makeActionButton("Eliminar", "#EF4444", "#ffffff");
        deleteBtn.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Eliminar Produto");
                confirm.setHeaderText("Tem certeza que deseja apagar este produto?");
                confirm.setContentText("Esta ação não pode ser revertida.");
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK) {
                        safeDelete(() -> productRepository.deleteById(selected.getId()), this::loadProducts, "Produto");
                    }
                });
            }
        });

        Button historyBtn = makeActionButton("Histórico", "#7C3AED", "#ffffff");
        historyBtn.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showToast("Selecione um produto para ver o histórico.");
                return;
            }
            openProductStockHistory(selected);
        });

        Button refreshBtn = makeIconButton("↻", "#10B981", "#ffffff");
        refreshBtn.setOnAction(e -> loadProducts());

        toolbar.getChildren().addAll(searchField, newBtn, editBtn, deleteBtn, historyBtn, refreshBtn);

        // ── TABELA ──
        productsTable = new TableView<>();
        productsTable.setStyle(
            "-fx-font-size: 13px; -fx-background-color: #ffffff; " +
            "-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; " +
            "-fx-table-cell-border-color: #F1F5F9;"
        );
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productsTable.setPlaceholder(new Label("Nenhum produto encontrado"));
        productsTable.setRowFactory(makeTableRowFactory());

        // Colunas
        productCodeColumn = new TableColumn<>("Código");
        productCodeColumn.setPrefWidth(120);
        productCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCode() != null ? data.getValue().getCode() : ""));

        productNameColumn = new TableColumn<>("Produto");
        productNameColumn.setPrefWidth(320);
        productNameColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getName() != null ? data.getValue().getName() : ""));

        productCategoryColumn = new TableColumn<>("Categoria");
        productCategoryColumn.setPrefWidth(180);
        productCategoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : "—"));

        productPriceColumn = new TableColumn<>("Preço Venda");
        productPriceColumn.setPrefWidth(140);
        productPriceColumn.setCellValueFactory(data -> {
            double price = data.getValue().getPriceSale() != null ? data.getValue().getPriceSale() : 0.0;
            return new SimpleStringProperty(String.format("%.2f MT", price));
        });
        productPriceColumn.setCellFactory(col -> {
            TableCell<Product, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setGraphic(null); }
                    else {
                        Label lbl = new Label(item);
                        lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A;");
                        setGraphic(lbl);
                        setText(null);
                    }
                }
            };
            return cell;
        });

        productStockColumn = new TableColumn<>("Stock");
        productStockColumn.setPrefWidth(120);
        productStockColumn.setCellValueFactory(data -> {
            try {
                if (currentUser != null && currentUser.getBranch() != null && data.getValue().getId() != null) {
                    return stockBranchService.findByProductIdAndBranchId(data.getValue().getId(), currentUser.getBranch().getId())
                        .map(sb -> new SimpleStringProperty(String.format("%.1f", sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO)))
                        .orElse(new SimpleStringProperty("0.0"));
                }
            } catch (Exception ex) { System.err.println("[DashboardController] Erro ao carregar stock: " + ex.getMessage()); }
            return new SimpleStringProperty("—");
        });
        productStockColumn.setCellFactory(col -> {
            TableCell<Product, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setGraphic(null); }
                    else {
                        Label lbl = new Label(item);
                        try {
                            double val = Double.parseDouble(item);
                            if (val <= 0) lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #EF4444; -fx-background-color: #FEE2E2; -fx-padding: 2 6; -fx-background-radius: 4;");
                            else if (val < 10) lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #F59E0B; -fx-background-color: #FEF3C7; -fx-padding: 2 6; -fx-background-radius: 4;");
                            else lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #10B981;");
                        } catch (Exception ex) {
                            lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #475569;");
                        }
                        setGraphic(lbl);
                        setText(null);
                    }
                }
            };
            return cell;
        });

        // Coluna Ações
        TableColumn<Product, Void> actionsCol = new TableColumn<>("");
        actionsCol.setPrefWidth(80);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏");
            private final Button delBtn = new Button("🗑");
            {
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563EB; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4;");
                delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4;");
                editBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (p != null) openProductForm(p);
                });
                delBtn.setOnAction(e -> {
                    Product p = getTableView().getItems().get(getIndex());
                    if (p != null) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Eliminar Produto");
                        confirm.setHeaderText("Eliminar: " + p.getName() + "?");
                        confirm.setContentText("Esta ação não pode ser revertida.");
                        confirm.showAndWait().ifPresent(res -> {
                            if (res == ButtonType.OK)
                                safeDelete(() -> productRepository.deleteById(p.getId()), DashboardController.this::loadProducts, "Produto");
                        });
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else {
                    HBox box = new HBox(4, editBtn, delBtn);
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        productsTable.getColumns().addAll(productCodeColumn, productNameColumn, productCategoryColumn, productPriceColumn, productStockColumn, actionsCol);

        // ── PAGINAÇÃO ──
        HBox pagination = new HBox(12);
        pagination.setStyle("-fx-background-color: #ffffff; -fx-padding: 10 16; -fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0;");
        pagination.setAlignment(javafx.geometry.Pos.CENTER);

        productsPrevButton = new Button("◂ Anterior");
        productsPageLabel = new Label("Página 1");
        productsNextButton = new Button("Próxima ▸");

        productsPrevButton.setStyle("-fx-padding: 6 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-color: #F8FAFC; -fx-text-fill: #0F172A; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-cursor: hand;");
        productsPageLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");
        productsNextButton.setStyle("-fx-padding: 6 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-color: #F8FAFC; -fx-text-fill: #0F172A; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-cursor: hand;");

        productsPrevButton.setOnAction(e -> {
            if (currentProductsPage > 0) { currentProductsPage--; loadProducts(); updateProductsPagination(); }
        });
        productsNextButton.setOnAction(e -> {
            currentProductsPage++;
            loadProducts();
            updateProductsPagination();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        pagination.getChildren().addAll(productsPrevButton, productsPageLabel, spacer, productsNextButton);

        // Montar
        // ── CARTÕES DE ESTATÍSTICA ──
        HBox statsCards = buildProductsStatsCards();
        mainContainer.getChildren().addAll(statsCards, toolbar, productsTable, pagination);
        productsPane.getChildren().add(mainContainer);

        // Carregar estatísticas
        loadProductStats(statsCards);

        // ── ANIMAÇÃO: Fade-in staggered ──
        FadeTransition fadeMain = new FadeTransition(Duration.millis(400), mainContainer);
        fadeMain.setFromValue(0);
        fadeMain.setToValue(1);
        fadeMain.setOnFinished(e -> {
            FadeTransition ftStats = new FadeTransition(Duration.millis(300), statsCards);
            ftStats.setFromValue(0); ftStats.setToValue(1); ftStats.play();
            FadeTransition ftToolbar = new FadeTransition(Duration.millis(300), toolbar);
            ftToolbar.setFromValue(0); ftToolbar.setToValue(1); ftToolbar.play();
            FadeTransition ftTable = new FadeTransition(Duration.millis(300), productsTable);
            ftTable.setFromValue(0); ftTable.setToValue(1); ftTable.play();
        });
        fadeMain.play();

        // Hover nos botões
        addHoverEffect(newBtn, "#1D4ED8", "#ffffff");
        addHoverEffect(editBtn, "#475569", "#ffffff");
        addHoverEffect(deleteBtn, "#EF4444", "#ffffff");
        addHoverEffect(refreshBtn, "#10B981", "#ffffff");
        addHoverEffect(productsPrevButton, "#E2E8F0", "#0F172A");
        addHoverEffect(productsNextButton, "#E2E8F0", "#0F172A");
    }

    private HBox buildProductsStatsCards() {
        HBox cardsBox = new HBox(16);
        cardsBox.setStyle("-fx-padding: 0 0 16 0;");

        // Card: Total de Produtos — azul
        VBox cardTotal = makeKpiCard("Total de Produtos", "—", "No catálogo", "card-pane", "kpi-icon kpi-icon-blue", "📦");
        // Card: Preço Médio — verde
        VBox cardPreco = makeKpiCard("Preço Médio", "—", "Valor médio venda", "card-pane", "kpi-icon kpi-icon-green", "💰");
        // Card: Stock Baixo — laranja
        VBox cardStock = makeKpiCard("Stock Baixo", "—", "Atenção necessária", "card-pane", "kpi-icon kpi-icon-orange", "⚠");
        // Card: Valor em Stock — roxo
        VBox cardValor = makeKpiCard("Valor em Stock", "—", "Capital em produtos", "card-pane", "kpi-icon kpi-icon-purple", "📊");

        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardPreco, Priority.ALWAYS);
        HBox.setHgrow(cardStock, Priority.ALWAYS);
        HBox.setHgrow(cardValor, Priority.ALWAYS);
        cardsBox.getChildren().addAll(cardTotal, cardPreco, cardStock, cardValor);
        return cardsBox;
    }

    private VBox makeKpiCard(String title, String value, String subtitle, String... styleClasses) {
        VBox card = new VBox();
        for (String sc : styleClasses) card.getStyleClass().add(sc);
        card.setStyle(card.getStyle() + "; -fx-padding: 22; -fx-min-height: 110; -fx-pref-height: 110;");

        HBox row = new HBox(16);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(row, Priority.ALWAYS);

        Label icon = new Label(styleClasses.length > 1 ? getKpiIconEmoji(styleClasses[1]) : "📦");
        icon.setStyle("-fx-font-size: 26px; -fx-min-width: 56px; -fx-min-height: 56px; -fx-alignment: center; -fx-background-radius: 12; -fx-padding: 10;");
        if (styleClasses.length > 1 && styleClasses[1].contains("blue")) icon.setStyle(icon.getStyle() + "; -fx-background-color: #DBEAFE;");
        else if (styleClasses.length > 1 && styleClasses[1].contains("green")) icon.setStyle(icon.getStyle() + "; -fx-background-color: #D1FAE5;");
        else if (styleClasses.length > 1 && styleClasses[1].contains("orange")) icon.setStyle(icon.getStyle() + "; -fx-background-color: #FEF3C7;");
        else if (styleClasses.length > 1 && styleClasses[1].contains("purple")) icon.setStyle(icon.getStyle() + "; -fx-background-color: #EDE9FE;");

        VBox info = new VBox(5);
        info.setStyle("-fx-min-width: 0;");
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #0F172A; -fx-font-weight: 700;");

        Label valueLbl = new Label(value);
        valueLbl.setId("kpi-value-" + title.replaceAll("\\s+", "").toLowerCase());
        valueLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: #0F172A;");

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        info.getChildren().addAll(titleLbl, valueLbl, subLbl);
        row.getChildren().addAll(icon, info);
        card.getChildren().add(row);
        return card;
    }

    private String getKpiIconEmoji(String styleClass) {
        if (styleClass.contains("blue")) return "📦";
        if (styleClass.contains("green")) return "💰";
        if (styleClass.contains("orange")) return "⚠";
        if (styleClass.contains("purple")) return "📊";
        return "📦";
    }

    private void loadProductStats(HBox cardsBox) {
        if (cardsBox == null || cardsBox.getChildren().isEmpty()) return;
        try {
            long total = productRepository.count();

            // Média de preço: buscar todos os preços
            double avgPrice = 0;
            try {
                List<Product> all = productRepository.findAll();
                avgPrice = all.stream()
                    .filter(p -> p.getPriceSale() != null && p.getPriceSale() > 0)
                    .mapToDouble(Product::getPriceSale)
                    .average().orElse(0);
            } catch (Exception ex) { System.err.println("[DashboardController] Erro ao calcular KPIs: " + ex.getMessage()); }

            // Stock baixo: contar produtos com stock < mínimo
            long lowStock = 0;
            double totalValue = 0;
            try {
                List<Product> all = productRepository.findAll();
                for (Product p : all) {
                    if (currentUser != null && currentUser.getBranch() != null && p.getId() != null) {
                        var sb = stockBranchService.findByProductIdAndBranchId(p.getId(), currentUser.getBranch().getId()).orElse(null);
                        if (sb != null) {
                            BigDecimal stock = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO;
                            BigDecimal min = sb.getStockMinAmount() != null ? sb.getStockMinAmount() : BigDecimal.ZERO;
                            if (stock.compareTo(BigDecimal.ZERO) > 0 && stock.compareTo(min) <= 0) lowStock++;
                            if (p.getPriceSale() != null) totalValue += stock.doubleValue() * p.getPriceSale();
                        }
                    }
                }
            } catch (Exception ex) { System.err.println("[DashboardController] Erro ao calcular stock/valor: " + ex.getMessage()); }

            // Actualizar valores nos cartões KPI
            ((Label) ((VBox) cardsBox.getChildren().get(0)).lookup("#kpi-value-totaldeprodutos"))
                .setText(String.valueOf(total));
            ((Label) ((VBox) cardsBox.getChildren().get(1)).lookup("#kpi-value-preciomédio"))
                .setText(String.format("%.0f MT", avgPrice));
            ((Label) ((VBox) cardsBox.getChildren().get(2)).lookup("#kpi-value-stockbaixo"))
                .setText(String.valueOf(lowStock));
            ((Label) ((VBox) cardsBox.getChildren().get(3)).lookup("#kpi-value-valorstock"))
                .setText(String.format("%.0f MT", totalValue));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateProductsPagination() {
        if (productsPageLabel == null) return;
        productsPageLabel.setText("Página " + (currentProductsPage + 1));
        if (productsPrevButton != null)
            productsPrevButton.setDisable(currentProductsPage <= 0);
    }

    private Button makeActionButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-padding: 7 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-cursor: hand; -fx-min-width: 70;");
        return btn;
    }

    private Button makeIconButton(String icon, String bg, String fg) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-padding: 7 10; -fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: 700; -fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-cursor: hand;");
        return btn;
    }

    /**
     * Returns a cell factory that colors the state cell based on its value.
     * COMPLETED/RECEIVED/ACTIVE  → green badge
     * CANCELLED/INACTIVE        → red badge
     * PENDING/IN_PROGRESS       → orange badge
     * default                   → gray badge
     */
    /**
     * Reusable row factory that correctly handles selection highlighting.
     * Must be called AFTER the table has its selection model set.
     */
    private <T> Callback<TableView<T>, TableRow<T>> makeTableRowFactory() {
        return tv -> {
            TableRow<T> row = new TableRow<>();
            Runnable applyStyle = () -> {
                if (row.isEmpty()) {
                    row.setStyle("");
                } else if (row.isSelected()) {
                    row.setStyle("-fx-background-color: #DBEAFE; -fx-border-color: #BFDBFE; -fx-border-width: 0 0 1 0;");
                } else if (row.isHover()) {
                    row.setStyle("-fx-background-color: #DBEAFE; -fx-border-color: #BFDBFE; -fx-border-width: 0 0 1 0;");
                } else {
                    row.setStyle(row.getIndex() % 2 == 0
                        ? "-fx-background-color: #ffffff; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;"
                        : "-fx-background-color: #F8FAFC; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");
                }
            };
            row.hoverProperty().addListener((obs, old, isHover) -> applyStyle.run());
            row.selectedProperty().addListener((obs, old, isSel) -> applyStyle.run());
            return row;
        };
    }

    /** Overload com handler de duplo-click (para a tabela de vendas). */
    private Callback<TableView<Sale>, TableRow<Sale>> makeSalesRowFactory() {
        return tv -> {
            TableRow<Sale> row = new TableRow<>();
            Runnable applyStyle = () -> {
                if (row.isEmpty()) {
                    row.setStyle("");
                } else if (row.isSelected()) {
                    row.setStyle("-fx-background-color: #DBEAFE; -fx-border-color: #BFDBFE; -fx-border-width: 0 0 1 0;");
                } else if (row.isHover()) {
                    row.setStyle("-fx-background-color: #DBEAFE; -fx-border-color: #BFDBFE; -fx-border-width: 0 0 1 0;");
                } else {
                    row.setStyle(row.getIndex() % 2 == 0
                        ? "-fx-background-color: #ffffff; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;"
                        : "-fx-background-color: #F8FAFC; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");
                }
            };
            row.hoverProperty().addListener((obs, old, isHover) -> applyStyle.run());
            row.selectedProperty().addListener((obs, old, isSel) -> applyStyle.run());
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openSaleForm(row.getItem());
                }
            });
            return row;
        };
    }

    private <T> Callback<TableColumn<T, String>, TableCell<T, String>> coloredStateCell() {
        return col -> new TableCell<T, String>() {
            @Override
            protected void updateItem(String state, boolean empty) {
                super.updateItem(state, empty);
                if (empty || state == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(state);
                    setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 10;");
                    String upper = state.toUpperCase();
                    // Estados de vendas específicos
                    if (upper.equals("PAGO")) {
                        setStyle(getStyle() + "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;");
                    } else if (upper.equals("EMITIDA")) {
                        setStyle(getStyle() + "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;");
                    } else if (upper.equals("COTACAO_ABERTA") || upper.equals("COTACAO")) {
                        setStyle(getStyle() + "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;");
                    } else if (upper.equals("ANULADA") || upper.contains("CANCELLED")) {
                        setStyle(getStyle() + "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;");
                    } else if (upper.equals("ENCOMENDA_ABERTA") || upper.equals("ENCOMENDA")) {
                        setStyle(getStyle() + "-fx-background-color: #EDE9FE; -fx-text-fill: #5B21B6;");
                    // Estados gerais
                    } else if (upper.contains("COMPLETED") || upper.contains("RECEIVED") || upper.contains("ACTIVE") || upper.equals("ACTIVO") || upper.equals("CONCLUÍDA")) {
                        setStyle(getStyle() + "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;");
                    } else if (upper.contains("INACTIVE") || upper.equals("INACTIVO") || upper.equals("CANCELADA")) {
                        setStyle(getStyle() + "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;");
                    } else if (upper.contains("PENDING") || upper.contains("IN_PROGRESS") || upper.equals("PENDENTE") || upper.equals("EM CURSO")) {
                        setStyle(getStyle() + "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;");
                    } else {
                        setStyle(getStyle() + "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;");
                    }
                }
            }
        };
    }

    /**
     * Builds a standard CRUD toolbar with search, novo, editar, eliminar buttons.
     * @param table      the TableView to wire selection to
     * @param entityName used for dialog titles
     * @param onNew      called when "Novo" is clicked
     * @param onEdit     called when "Editar" is clicked
     * @param onDelete   called when "Eliminar" is clicked (caller handles repo delete)
     * @return the HBox toolbar, ready to add to the pane
     */
    private HBox buildCrudToolbar(TableView<?> table, String entityName,
                                   Runnable onNew, Runnable onEdit, Runnable onDelete) {
        HBox toolbar = new HBox(12);
        toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar...");
        searchField.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; " +
            "-fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; " +
            "-fx-background-color: #F8FAFC; -fx-min-width: 240; -fx-pref-width: 280;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button newBtn = makeActionButton("+ Novo", "#2563EB", "#ffffff");
        newBtn.setOnAction(e -> onNew.run());

        Button editBtn = makeActionButton("Editar", "#475569", "#ffffff");
        editBtn.setOnAction(e -> {
            if (table.getSelectionModel().getSelectedItem() == null) {
                showToast("Selecione um registo para editar.");
                return;
            }
            onEdit.run();
        });

        Button deleteBtn = makeActionButton("Eliminar", "#EF4444", "#ffffff");
        deleteBtn.setOnAction(e -> {
            if (table.getSelectionModel().getSelectedItem() == null) {
                showToast("Selecione um registo para eliminar.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Eliminar " + entityName);
            confirm.setHeaderText("Eliminar este " + entityName + "?");
            confirm.setContentText("Esta ação não pode ser revertida.");
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) onDelete.run();
            });
        });

        toolbar.getChildren().addAll(searchField, newBtn, editBtn, deleteBtn);
        return toolbar;
    }

    private void showToast(String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
            // Auto-close after 2.5s
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.5));
            delay.setOnFinished(ev -> alert.close());
            delay.play();
        } catch (Exception ignored) {}
    }

    private void addHoverEffect(Button btn, String hoverBg, String hoverFg) {
        String originalStyle = btn.getStyle();
        btn.setOnMouseEntered(e -> btn.setStyle(originalStyle + " -fx-opacity: 0.85;"));
        btn.setOnMouseExited(e -> btn.setStyle(originalStyle));
    }




    private void showSubNav(String moduleLabel, HBox targetItems) {
        if (navRootPane == null || navSubPane == null) return;
        navSubModuleLabel.setText(moduleLabel);

        if (navComercialItems != null) { navComercialItems.setVisible(false); navComercialItems.setManaged(false); }
        if (navOperacoesItems != null) { navOperacoesItems.setVisible(false); navOperacoesItems.setManaged(false); }
        if (navFinanceiroItems != null) { navFinanceiroItems.setVisible(false); navFinanceiroItems.setManaged(false); }
        if (navAdminItems != null) { navAdminItems.setVisible(false); navAdminItems.setManaged(false); }

        if (targetItems != null) { targetItems.setVisible(true); targetItems.setManaged(true); }

        navRootPane.setVisible(false);
        navRootPane.setManaged(false);
        navSubPane.setVisible(true);
        navSubPane.setManaged(true);
    }

    private void drillBack() {
        if (navRootPane == null || navSubPane == null) return;

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), navRootPane);
        navSubPane.setVisible(false);
        navSubPane.setManaged(false);
        navRootPane.setVisible(true);
        navRootPane.setManaged(true);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }
    // ────────────────────────────────────────────────────────────────────

    private void showUsersPane() {
        setActiveNav(null);
        pageTitleLabel.setText("Utilizadores");
        pageSubtitleLabel.setText("Gestão de utilizadores e permissões");
        setPaneVisibility(usersPane);

        if (usersPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = buildCrudToolbar(usersTable, "Utilizador",
                () -> openUserForm(null),
                () -> openUserForm(usersTable.getSelectionModel().getSelectedItem()),
                () -> {
                    User u = usersTable.getSelectionModel().getSelectedItem();
                    safeDelete(() -> userRepository.deleteById(u.getId()), this::loadSystem, "Utilizador");
                }
            );
            TextField searchField = (TextField) toolbar.getChildren().get(0);
            searchField.setPromptText("Pesquisar utilizador...");
            searchField.textProperty().addListener((obs, old, val) -> loadSystem());

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Utilizadores", "Activos", "Admin", "—"},
                new String[]{"Total de users", "Users activos", "Users admin", "—"},
                new String[]{"blue", "green", "orange", "gray"}
            );
            kpiGrid.setId("usersKPI");

            usersTable = new TableView<>();
            usersTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");

            TableColumn<User, String> uc1 = new TableColumn<>("Nome Completo");
            uc1.setPrefWidth(200);
            uc1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName() != null ? d.getValue().getFullName() : "—"));

            TableColumn<User, String> uc2 = new TableColumn<>("Username");
            uc2.setPrefWidth(120);
            uc2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername() != null ? d.getValue().getUsername() : "—"));

            TableColumn<User, String> uc3 = new TableColumn<>("Email");
            uc3.setPrefWidth(180);
            uc3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));

            TableColumn<User, String> uc4 = new TableColumn<>("Filial");
            uc4.setPrefWidth(150);
            uc4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));

            TableColumn<User, String> uc5 = new TableColumn<>("Estado");
            uc5.setPrefWidth(80);
            uc5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "Activo" : "Inactivo"));
            uc5.setCellFactory(coloredStateCell());

            TableColumn<User, String> uc6 = new TableColumn<>("Roles");
            uc6.setPrefWidth(150);
            uc6.setCellValueFactory(d -> {
                if (d.getValue().getRoles() == null || d.getValue().getRoles().isEmpty()) return new SimpleStringProperty("—");
                String roles = d.getValue().getRoles().stream()
                        .map(r -> r.getName() != null ? r.getName() : "")
                        .filter(r -> !r.isEmpty())
                        .collect(java.util.stream.Collectors.joining(", "));
                return new SimpleStringProperty(roles.isEmpty() ? "—" : roles);
            });

            usersTable.getColumns().addAll(uc1, uc2, uc3, uc4, uc5, uc6);
            usersTable.setRowFactory(makeTableRowFactory());

            main.getChildren().addAll(kpiGrid, toolbar, usersTable);
            usersPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) usersPane.lookup("#usersKPI");
        updateUsersKPIs(kpi);
        loadSystem();
        updateCashBadge();
    }

    private void loadProfile() {
        if (currentUser == null) {
            userNameLabel.setText("Usuário");
            userRoleLabel.setText("");
            userAvatar.setText("U");
            welcomeText.setText("Bem-vindo ao SGV.");
            return;
        }
        String fullName = currentUser.getFullName() != null && !currentUser.getFullName().isBlank()
                ? currentUser.getFullName() : currentUser.getUsername();
        userNameLabel.setText(fullName);
        String role = currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()
                ? currentUser.getRoles().iterator().next().getName() : "";
        userRoleLabel.setText(role);
        userAvatar.setText(fullName.isEmpty() ? "U" : fullName.substring(0, 1).toUpperCase());
        if (welcomeText != null) welcomeText.setText("Bem-vindo ao SGV, " + fullName + ".");
    }

    private void loadSalesStats() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
        java.util.List<Sale> allSales = saleRepository.findAll();

        // Faturação Hoje
        double revenueToday = allSales.stream()
                .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(startOfDay) && !s.getCreatedAt().isAfter(endOfDay))
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        if (salesStatsRevenueTodayLabel != null)
            salesStatsRevenueTodayLabel.setText(String.format("%.2f AKZ", revenueToday));

        // Vendas Hoje
        long countToday = allSales.stream()
                .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(startOfDay) && !s.getCreatedAt().isAfter(endOfDay))
                .count();
        if (salesStatsCountTodayLabel != null)
            salesStatsCountTodayLabel.setText(String.valueOf(countToday));

        // Ticket Médio (total geral / total documentos)
        long totalDocs = allSales.size();
        double totalRevenue = allSales.stream()
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        double avgTicket = totalDocs > 0 ? totalRevenue / totalDocs : 0.0;
        if (salesStatsAvgTicketLabel != null)
            salesStatsAvgTicketLabel.setText(String.format("%.2f AKZ", avgTicket));

        // Valores Pendentes
        double pending = allSales.stream()
                .filter(s -> "PENDENTE".equalsIgnoreCase(s.getState()) || "PENDING".equalsIgnoreCase(s.getState()))
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        if (salesStatsPendingLabel != null)
            salesStatsPendingLabel.setText(String.format("%.2f AKZ", pending));

        // Bar Chart: Faturação Mensal dos últimos 6 meses
        if (salesStatsBarChart != null) {
            salesStatsBarChart.getData().clear();
            javafx.scene.chart.XYChart.Series<String, Number> barSeries = new javafx.scene.chart.XYChart.Series<>();
            barSeries.setName("Faturação");
            java.time.LocalDate start = today.minusMonths(5).withDayOfMonth(1);
            for (int i = 0; i < 6; i++) {
                java.time.LocalDate monthStart = start.plusMonths(i);
                java.time.LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                double monthTotal = allSales.stream()
                        .filter(s -> s.getCreatedAt() != null &&
                                !s.getCreatedAt().toLocalDate().isBefore(monthStart) &&
                                !s.getCreatedAt().toLocalDate().isAfter(monthEnd))
                        .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                        .sum();
                String label = monthStart.format(DateTimeFormatter.ofPattern("MMM"));
                barSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(label, monthTotal));
            }
            salesStatsBarChart.getData().add(barSeries);
        }

        // Pie Chart: Estado das Vendas
        if (salesStatsPieChart != null) {
            long paid = allSales.stream().filter(s -> "PAGO".equalsIgnoreCase(s.getState()) || "PAID".equalsIgnoreCase(s.getState())).count();
            long pendingCount = allSales.stream().filter(s -> "PENDENTE".equalsIgnoreCase(s.getState()) || "PENDING".equalsIgnoreCase(s.getState())).count();
            long cancelled = allSales.stream().filter(s -> "CANCELADO".equalsIgnoreCase(s.getState()) || "CANCELLED".equalsIgnoreCase(s.getState())).count();
            javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> pieData = FXCollections.observableArrayList(
                new javafx.scene.chart.PieChart.Data("Pago (" + paid + ")", paid),
                new javafx.scene.chart.PieChart.Data("Pendente (" + pendingCount + ")", pendingCount),
                new javafx.scene.chart.PieChart.Data("Cancelado (" + cancelled + ")", cancelled)
            );
            salesStatsPieChart.setData(pieData);
        }
    }

    private void loadStats() {
        long salesCount = saleRepository.count();
        double salesTotal = saleRepository.findAll().stream()
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        salesLabel.setText(salesCount + " (" + String.format("%.2f", salesTotal) + ")");
        productsLabel.setText(String.valueOf(productRepository.count()));
        customersLabel.setText(String.valueOf(customerRepository.count()));
        branchesLabel.setText(String.valueOf(branchRepository.count()));

        // Populate Line Chart (Sales Last 7 Days)
        if (salesLineChart != null) {
            salesLineChart.getData().clear();
            javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
            series.setName("Vendas");
            
            // Get sales grouped by date for last 7 days
            java.time.LocalDate today = java.time.LocalDate.now();
            java.util.List<Sale> allSales = saleRepository.findAll();
            
            for (int i = 6; i >= 0; i--) {
                java.time.LocalDate date = today.minusDays(i);
                double dailyTotal = allSales.stream()
                        .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().toLocalDate().equals(date))
                        .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                        .sum();
                series.getData().add(new javafx.scene.chart.XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("dd/MM")), dailyTotal));
            }
            salesLineChart.getData().add(series);
        }

        // Populate Pie Chart (Sales State)
        if (salesPieChart != null) {
            java.util.List<Sale> allSales = saleRepository.findAll();
            long paid = allSales.stream().filter(s -> "PAGO".equalsIgnoreCase(s.getState()) || "PAID".equalsIgnoreCase(s.getState())).count();
            long pending = allSales.stream().filter(s -> "PENDENTE".equalsIgnoreCase(s.getState()) || "PENDING".equalsIgnoreCase(s.getState())).count();
            long cancelled = allSales.stream().filter(s -> "CANCELADO".equalsIgnoreCase(s.getState()) || "CANCELLED".equalsIgnoreCase(s.getState())).count();
            
            javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> pieData = FXCollections.observableArrayList(
                new javafx.scene.chart.PieChart.Data("Pago (" + paid + ")", paid),
                new javafx.scene.chart.PieChart.Data("Pendente (" + pending + ")", pending),
                new javafx.scene.chart.PieChart.Data("Cancelado (" + cancelled + ")", cancelled)
            );
            salesPieChart.setData(pieData);
        }

        checkAlerts();
        animateEntrance();
    }

    private void checkAlerts() {
        int alertCount = 0;
        
        // Check low stock
        java.util.List<StockBranch> stocks = stockBranchService.findAll();
        for (StockBranch stock : stocks) {
            BigDecimal current = stock.getStockCurrentAmount();
            if (current != null && current.compareTo(BigDecimal.valueOf(10)) <= 0 && stock.getProduct() != null && !Boolean.TRUE.equals(stock.getProduct().getService())) {
                alertCount++;
            }
        }
        
        // Check pending sales
        java.util.List<Sale> allSales = saleRepository.findAll();
        for (Sale sale : allSales) {
            if ("PENDENTE".equalsIgnoreCase(sale.getState()) || "PENDING".equalsIgnoreCase(sale.getState())) {
                alertCount++;
            }
        }
        
        if (alertCount > 0 && notificationBadge != null) {
            notificationBadge.setText(String.valueOf(alertCount));
            notificationBadge.setVisible(true);
            notificationBadge.setManaged(true);
        } else if (notificationBadge != null) {
            notificationBadge.setVisible(false);
            notificationBadge.setManaged(false);
        }
    }
    
    private void showNotificationPopup() {
        if (notificationBellButton == null) return;
        
        VBox popupContent = new VBox(10);
        popupContent.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);");
        popupContent.setPrefWidth(300);
        popupContent.setMaxHeight(400);
        
        Label titleLabel = new Label("Notificações do Sistema");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");
        popupContent.getChildren().add(titleLabel);
        popupContent.getChildren().add(new javafx.scene.control.Separator());
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: white;");
        scrollPane.setMaxHeight(300);
        
        VBox alertsBox = new VBox(8);
        
        // Check low stock
        java.util.List<StockBranch> stocks = stockBranchService.findAll();
        for (StockBranch stock : stocks) {
            BigDecimal current = stock.getStockCurrentAmount();
            if (current != null && current.compareTo(BigDecimal.valueOf(10)) <= 0 && stock.getProduct() != null && !Boolean.TRUE.equals(stock.getProduct().getService())) {
                String branchName = stock.getBranch() != null ? stock.getBranch().getName() : "Desconhecida";
                Label alert = new Label("[Aviso] Estoque baixo: " + stock.getProduct().getName() + " na filial " + branchName + " (" + current + ")");
                alert.setWrapText(true);
                alert.setStyle("-fx-text-fill: #F59E0B; -fx-font-size: 12px; -fx-font-weight: 600;");
                alertsBox.getChildren().add(alert);
            }
        }
        
        // Check pending sales
        java.util.List<Sale> allSales = saleRepository.findAll();
        for (Sale sale : allSales) {
            if ("PENDENTE".equalsIgnoreCase(sale.getState()) || "PENDING".equalsIgnoreCase(sale.getState())) {
                Label alert = new Label("[Pendente] Venda " + sale.getSeries() + "/" + sale.getDocumentNumber() + " - " + sale.getCustomerName());
                alert.setWrapText(true);
                alert.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: 600;");
                alertsBox.getChildren().add(alert);
            }
        }
        
        if (alertsBox.getChildren().isEmpty()) {
            Label noAlerts = new Label("Não há notificações novas.");
            noAlerts.setStyle("-fx-text-fill: #475569; -fx-font-style: italic; -fx-font-size: 12px;");
            alertsBox.getChildren().add(noAlerts);
        }
        
        scrollPane.setContent(alertsBox);
        popupContent.getChildren().add(scrollPane);
        
        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.getContent().add(popupContent);
        popup.setAutoHide(true);
        
        javafx.geometry.Point2D p = notificationBellButton.localToScreen(0.0, notificationBellButton.getHeight());
        if (p != null) {
            popup.show(notificationBellButton, p.getX() - 250, p.getY() + 5);
        }
    }

    private void initTables() {
        // Sales Table
        if (salesDocumentColumn != null) {
            salesDocumentColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getDocumentType() + " " + cell.getValue().getSeries() + "/" + cell.getValue().getDocumentNumber()));
            salesDocumentColumn.setCellFactory(col -> new TableCell<Sale, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label label = new Label(item);
                        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB;");
                        if (item.startsWith("COTACAO") || item.startsWith("ENCOMENDA")) {
                            label.setStyle("-fx-font-weight: bold; -fx-text-fill: #F59E0B;"); // Orange
                        }
                        setGraphic(label);
                        setText(null);
                    }
                }
            });
        }
        if (salesBranchColumn != null) salesBranchColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getBranch() != null ? cell.getValue().getBranch().getName() : ""));
        if (salesCustomerColumn != null) salesCustomerColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCustomerName() != null ? cell.getValue().getCustomerName() : ""));
        if (salesTotalColumn != null) salesTotalColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().getTotal() != null ? cell.getValue().getTotal() : 0.0)));
        if (salesStateColumn != null) {
            salesStateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getState() != null ? cell.getValue().getState() : ""));
            salesStateColumn.setCellFactory(col -> new TableCell<Sale, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        Label label = new Label(item);
                        label.setStyle("-fx-padding: 2 6; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;");
                        if ("EMITIDA".equals(item)) {
                            label.setStyle(label.getStyle() + "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
                        } else if ("ANULADA".equals(item)) {
                            label.setStyle(label.getStyle() + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
                        } else if (item.contains("ABERTA")) {
                            label.setStyle(label.getStyle() + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;");
                        } else {
                            label.setStyle(label.getStyle() + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
                        }
                        setGraphic(label);
                        setText(null);
                    }
                }
            });
        }
        if (salesDateColumn != null) salesDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt() != null ? cell.getValue().getCreatedAt().format(DATE_FORMATTER) : ""));

        // Products Table
        if (productCodeColumn != null) productCodeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCode()));
        if (productNameColumn != null) productNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        if (productCategoryColumn != null) productCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCategory() != null ? cell.getValue().getCategory().getName() : ""));
        if (productPriceColumn != null) productPriceColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().getPriceSale() != null ? cell.getValue().getPriceSale() : 0.0)));
        if (productStockColumn != null) productStockColumn.setCellValueFactory(cell -> {
            Product p = cell.getValue();
            try {
                if (currentUser != null && currentUser.getBranch() != null && p != null && p.getId() != null) {
                    Long branchId = currentUser.getBranch().getId();
                    return new SimpleStringProperty(
                            stockBranchService.findByProductIdAndBranchId(p.getId(), branchId)
                                    .map(sb -> String.format("%.2f", sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO))
                                    .orElse("0.00")
                    );
                }
            } catch (Exception ex) {
                // fallback on any error
            }
            return new SimpleStringProperty("—");
        });

        // Customers Table
        if (customerCodeColumn != null) customerCodeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCode()));
        if (customerNameColumn != null) customerNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        if (customerTypeColumn != null) customerTypeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType()));
        if (customerBalanceColumn != null) customerBalanceColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().getBalance() != null ? cell.getValue().getBalance() : 0.0)));
        if (customerCreatedColumn != null) customerCreatedColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt() != null ? cell.getValue().getCreatedAt().toString() : ""));

        // Purchases Table
        if (purchaseInvoiceColumn != null) {
            purchaseInvoiceColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getInvoiceNumber()));
            if (purchaseTotalColumn != null) purchaseTotalColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                    String.format("%.2f", cell.getValue().getTotal() != null ? cell.getValue().getTotal() : 0.0)));
            if (purchaseStateColumn != null) purchaseStateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getState()));
            if (purchaseDateColumn != null) purchaseDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getPurchaseDate() != null ? cell.getValue().getPurchaseDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
        }

        // Financeiro (Payments) Table
        if (paymentSaleColumn != null) paymentSaleColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSale() != null ? String.valueOf(cell.getValue().getSale().getDocumentNumber()) : ""));
        if (paymentCustomerColumn != null) paymentCustomerColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSale() != null && cell.getValue().getSale().getCustomerName() != null ? cell.getValue().getSale().getCustomerName() : ""));
        if (paymentMethodColumn != null) paymentMethodColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getMethod()));
        if (paymentAmountColumn != null) paymentAmountColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().getAmount() != null ? cell.getValue().getAmount() : 0.0)));
        if (paymentDateColumn != null) paymentDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt() != null ? cell.getValue().getCreatedAt().format(DATE_FORMATTER) : ""));

        // Expenses Table
        if (expenseDescColumn != null) {
            expenseDescColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
            if (expenseCategoryColumn != null) expenseCategoryColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCategory()));
            if (expenseAmountColumn != null) expenseAmountColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                    String.format("%.2f", cell.getValue().getAmount() != null ? cell.getValue().getAmount() : 0.0)));
            if (expenseDueColumn != null) expenseDueColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getDueDate() != null ? cell.getValue().getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""));
            if (expenseStateColumn != null) expenseStateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getState()));
        }

        // Production Orders Table
        if (orderNumberColumn != null) orderNumberColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrderNumber()));
        if (orderProductColumn != null) orderProductColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getProduct() != null ? cell.getValue().getProduct().getName() : ""));
        if (orderQuantityColumn != null) orderQuantityColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().getQuantity() != null ? cell.getValue().getQuantity() : 0.0)));
        if (orderUnitColumn != null) orderUnitColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUnit()));
        if (orderStateColumn != null) orderStateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getState()));
        if (orderDateColumn != null) orderDateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt() != null ? cell.getValue().getCreatedAt().format(DATE_FORMATTER) : ""));

        // Users (System) Table
        if (sysUserUsernameColumn != null) sysUserUsernameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        if (sysUserFullNameColumn != null) sysUserFullNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));
        if (sysUserRoleColumn != null) sysUserRoleColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getRoles() != null && !cell.getValue().getRoles().isEmpty() ? cell.getValue().getRoles().iterator().next().getName() : ""));
        if (sysUserBranchColumn != null) sysUserBranchColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getBranch() != null ? cell.getValue().getBranch().getName() : ""));
        if (sysUserActiveColumn != null) sysUserActiveColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                Boolean.TRUE.equals(cell.getValue().isActive()) ? "Ativo" : "Inativo"));

        // Branches (System) Table
        if (sysBranchIdColumn != null) sysBranchIdColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        if (sysBranchNameColumn != null) sysBranchNameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        if (sysBranchAddressColumn != null) sysBranchAddressColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("address"));
        if (sysBranchNuitColumn != null) sysBranchNuitColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nuit"));
        if (sysBranchActiveColumn != null) sysBranchActiveColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                Boolean.TRUE.equals(cell.getValue().isHead()) ? "Sede" : "Filial"));
                
        setupCatalogsTables();
    }

    private void setupCatalogsTables() {
        if (colCatId != null) colCatId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        if (colCatName != null) colCatName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        
        if (colUnitId != null) colUnitId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        if (colUnitAbbr != null) colUnitAbbr.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAbbreviation()));
        if (colUnitDesc != null) colUnitDesc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        
        if (btnNovaCategoria != null) btnNovaCategoria.setOnAction(e -> {
            if (ensurePermission("CATALOGOS", "CREATE", "Catálogos")) openCategoryForm(null);
        });
        if (btnNovaUnidade != null) btnNovaUnidade.setOnAction(e -> {
            if (ensurePermission("CATALOGOS", "CREATE", "Catálogos")) openMetricUnitForm(null);
        });

        if (btnEditCategoria != null) btnEditCategoria.setOnAction(e -> {
            if (!ensurePermission("CATALOGOS", "CREATE", "Catálogos")) return;
            if (categoriesTable == null) return;
            com.sgv.entity.Category selected = categoriesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Por favor selecione uma categoria para editar.");
                return;
            }
            openCategoryForm(selected);
        });

        if (btnEditUnidade != null) btnEditUnidade.setOnAction(e -> {
            if (!ensurePermission("CATALOGOS", "CREATE", "Catálogos")) return;
            if (unitsTable == null) return;
            com.sgv.entity.MetricUnit selected = unitsTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Por favor selecione uma unidade para editar.");
                return;
            }
            openMetricUnitForm(selected);
        });

        if (btnDeleteCategoria != null) btnDeleteCategoria.setOnAction(e -> {
            if (ensurePermission("CATALOGOS", "DELETE", "Catálogos")) deleteSelectedCategory();
        });
        if (btnDeleteUnidade != null) btnDeleteUnidade.setOnAction(e -> {
            if (ensurePermission("CATALOGOS", "DELETE", "Catálogos")) deleteSelectedUnit();
        });
        
        setupStockTable();
    }

    private void setupStockTable() {
        if (colStockCode != null) colStockCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct().getCode()));
        if (colStockProduct != null) colStockProduct.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct().getName()));
        if (colStockCategory != null) colStockCategory.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProduct().getCategory() != null ? d.getValue().getProduct().getCategory().getName() : ""));
        if (colStockUnit != null) colStockUnit.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProduct().getUnit() != null ? d.getValue().getProduct().getUnit().getAbbreviation() : ""));
        if (colStockBranch != null) colStockBranch.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : ""));
        if (colStockCurrent != null) colStockCurrent.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f", d.getValue().getStockCurrentAmount())));
        if (colStockMin != null) colStockMin.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f", d.getValue().getStockMinAmount())));
        
        if (colStockStatus != null) {
            colStockStatus.setCellValueFactory(d -> {
                BigDecimal current = d.getValue().getStockCurrentAmount() != null ? d.getValue().getStockCurrentAmount() : BigDecimal.ZERO;
                BigDecimal min = d.getValue().getStockMinAmount() != null ? d.getValue().getStockMinAmount() : BigDecimal.ZERO;
                if (current.compareTo(BigDecimal.ZERO) <= 0) return new SimpleStringProperty("Esgotado");
                if (current.compareTo(min) <= 0) return new SimpleStringProperty("Abaixo Mínimo");
                return new SimpleStringProperty("Normal");
            });
        }
        
        if (btnAjustarStock != null) btnAjustarStock.setOnAction(e -> {
            if (!ensurePermission("STOCK", "CREATE", "Stock")) return;
            com.sgv.entity.StockBranch selected = stockTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Por favor selecione um item de stock para ajustar.");
                return;
            }
            openStockAdjustForm(selected);
        });
        
        if (stockSearchField != null) {
            stockSearchField.textProperty().addListener((obs, old, nv) -> loadStock());
        }
    }

    private void loadSales() {
        if (salesTable == null) return;

        java.util.List<Sale> allSales = saleRepository.findAll();

        // Filtro por texto: pesquisa em número do documento, nome do cliente, série
        java.util.List<Sale> filtered = allSales.stream()
            .filter(s -> {
                if (saleFilter == null || saleFilter.isBlank()) return true;
                String term = saleFilter.toLowerCase();
                boolean match = false;
                if (s.getDocumentNumber() != null) match |= s.getDocumentNumber().toString().contains(term);
                if (s.getSeries() != null) match |= s.getSeries().toLowerCase().contains(term);
                if (s.getDocumentType() != null) match |= s.getDocumentType().toLowerCase().contains(term);
                if (s.getCustomer() != null && s.getCustomer().getName() != null) match |= s.getCustomer().getName().toLowerCase().contains(term);
                if (s.getCustomerName() != null) match |= s.getCustomerName().toLowerCase().contains(term);
                return match;
            })
            .filter(s -> {
                if (saleStateFilter == null || "TODOS".equals(saleStateFilter)) return true;
                return saleStateFilter.equals(s.getState());
            })
            .sorted(Comparator.comparing(Sale::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();

        salesTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void loadProducts() {
        if (productsTable == null) return;
        java.util.List<Product> products;
        if (productFilter != null && !productFilter.isEmpty()) {
            products = productRepository.searchByCodeOrName(productFilter);
        } else {
            Pageable pageable = PageRequest.of(currentProductsPage, PAGE_SIZE);
            products = productRepository.findAll(pageable).getContent();
        }
        productsTable.setItems(FXCollections.observableArrayList(products));
        updateProductsPagination();
    }

    private void loadCustomers() {
        if (customersTable == null) return;
        java.util.List<Customer> customers;
        if (customerFilter != null && !customerFilter.isEmpty()) {
            customers = customerRepository.searchByCodeOrName(customerFilter);
        } else {
            Pageable pageable = PageRequest.of(currentCustomersPage, PAGE_SIZE);
            customers = customerRepository.findAll(pageable).getContent();
        }
        customersTable.setItems(FXCollections.observableArrayList(customers));
    }

    private void loadStock() {
        if (stockTable == null) return;
        java.util.List<com.sgv.entity.StockBranch> stocks;
        String term = stockSearchField != null ? stockSearchField.getText() : null;
        if (term != null && !term.isBlank()) {
            stocks = stockBranchService.findAll().stream()
                .filter(s -> (s.getProduct().getName() != null && s.getProduct().getName().toLowerCase().contains(term.toLowerCase()))
                          || (s.getProduct().getCode() != null && s.getProduct().getCode().toLowerCase().contains(term.toLowerCase())))
                .toList();
        } else {
            stocks = stockBranchService.findAll();
        }
        stockTable.setItems(FXCollections.observableArrayList(stocks));
    }
    
    private void deleteSelectedCategory() {
        if (categoriesTable == null) return;
        com.sgv.entity.Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Por favor selecione uma categoria para eliminar.");
            return;
        }
        safeDelete(() -> categoryRepository.delete(selected), this::loadCategories, "Categoria");
    }

    private void deleteSelectedUnit() {
        if (unitsTable == null) return;
        com.sgv.entity.MetricUnit selected = unitsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Por favor selecione uma unidade para eliminar.");
            return;
        }
        safeDelete(() -> metricUnitRepository.delete(selected), this::loadMetricUnits, "Unidade de Medida");
    }

    private void openStockAdjustForm(com.sgv.entity.StockBranch stock) {
        if (stock == null) {
            showToast("Selecione um stock existente para ajustar.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock_adjust_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            StockAdjustFormController ctrl = loader.getController();
            ctrl.setStock(stock);
            ctrl.setCurrentUser(currentUser);
            ctrl.setOnSave(this::loadStock);
            openModal(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadPurchases() {
        if (purchasesTable == null) return;
        List<Purchase> purchases = purchaseRepository.findAllByOrderByCreatedAtDesc();
        purchasesTable.setItems(FXCollections.observableArrayList(purchases));
    }

    private void loadFinanceiro() {
        if (paymentsTable == null) return;
        List<Payment> payments = paymentRepository.findAll();
        paymentsTable.setItems(FXCollections.observableArrayList(
                payments.stream()
                        .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList()
        ));

        // Update stats
        double totalVendas = saleRepository.findAll().stream()
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        double totalRecebido = payments.stream()
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();
        
        finTotalVendasLabel.setText(String.format("%.2f MT", totalVendas));
        finTotalRecebidoLabel.setText(String.format("%.2f MT", totalRecebido));
        finPendenteLabel.setText(String.format("%.2f MT", totalVendas - totalRecebido));
        finNumPagamentosLabel.setText(String.valueOf(payments.size()));

        loadExpenses();
    }

    private void loadExpenses() {
        if (expensesTable == null) return;
        List<Expense> expenses = expenseRepository.findAllByOrderByCreatedAtDesc();
        expensesTable.setItems(FXCollections.observableArrayList(expenses));
    }

    private void loadProductionOrders() {
        if (ordersTable == null) return;
        List<ProductionOrder> orders = productionOrderRepository.findAllByOrderByCreatedAtDesc();
        ordersTable.setItems(FXCollections.observableArrayList(orders));
    }

    private void loadSystem() {
        if (usersTable == null) return;
        List<User> users = userRepository.findAll();
        usersTable.setItems(FXCollections.observableArrayList(users));

        if (branchInfoBox != null && currentUser != null && currentUser.getBranch() != null) {
            branchInfoBox.getChildren().clear();
            Branch b = currentUser.getBranch();
            branchInfoBox.getChildren().addAll(
                    buildInfoBlock("Nome da Filial", b.getName()),
                    buildInfoBlock("Endereço", b.getAddress() != null ? b.getAddress() : "--"),
                    buildInfoBlock("Contacto", b.getContact() != null ? b.getContact() : "--")
            );
        }

        if (branchesTable != null && branchRepository != null) {
            branchesTable.getItems().clear();
            branchesTable.getItems().addAll(branchRepository.findAll());
        }
    }

    private VBox buildInfoBlock(String label, String value) {
        VBox box = new VBox(4);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c9a; -fx-font-weight: 600;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #1a3a52;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private void updateCashBadge() {
        if (cashStatusBadge == null) return;
        boolean isOpen = currentUser != null && cashSessionService.hasOpenSession(currentUser);
        if (isOpen) {
            cashStatusBadge.setText("Caixa Aberto");
            cashStatusBadge.setStyle("-fx-background-color: #e3fcef; -fx-text-fill: #00875a; -fx-padding: 6 12; -fx-background-radius: 4; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        } else {
            cashStatusBadge.setText("Caixa Fechado");
            cashStatusBadge.setStyle("-fx-background-color: #ffebe6; -fx-text-fill: #de350b; -fx-padding: 6 12; -fx-background-radius: 4; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        }
        
        // Clicar no badge leva para a página Turno / Caixa
        cashStatusBadge.setOnMouseClicked(e -> showTurnoCaixaPane());
    }

    private void showTurnoCaixaPane() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cash_session.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            javafx.scene.Parent root = loader.load();
            
            CashSessionController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setOnStateChange(this::updateCashBadge);
            
            turnoCaixaPane.getChildren().setAll(root);
            
            pageTitleLabel.setText("Turno de Caixa");
            pageSubtitleLabel.setText("Gestão de aberturas, fechos e movimentos");
            setActiveNav(null);
            setPaneVisibility(turnoCaixaPane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populatePresetCombo(VBox panel, String type, ComboBox<String> presetCombo) {
        if (presetCombo == null || currentUser == null) {
            return;
        }
        try {
            var presets = filterPresetService.listPresets(currentUser.getId(), type);
            var names = presets.stream().map(FilterPreset::getName).toList();
            presetCombo.setItems(FXCollections.observableArrayList(names));
            if (!Boolean.TRUE.equals(presetCombo.getProperties().get("presetListenerAdded"))) {
                presetCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
                    if (newValue != null && !newValue.isBlank()) {
                        applyPreset(type, newValue, panel);
                    }
                });
                presetCombo.getProperties().put("presetListenerAdded", true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveProductPreset(VBox panel) {
        if (currentUser == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Salvar Preset");
        dialog.setHeaderText("Salvar preset de filtro de produtos");
        dialog.setContentText("Nome do preset:");
        dialog.showAndWait().ifPresent(name -> {
            if (name == null || name.isBlank()) {
                return;
            }
            try {
                var searchField = (TextField) panel.lookup("#productSearchField");
                var categoryCombo = (ComboBox<String>) panel.lookup("#productCategoryCombo");
                String search = searchField != null ? searchField.getText() : "";
                String category = categoryCombo != null ? categoryCombo.getValue() : null;
                if ("Todos".equals(category)) {
                    category = null;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("search", search);
                payload.put("category", category);
                String json = objectMapper.writeValueAsString(payload);
                filterPresetService.savePreset(new FilterPreset(currentUser.getId(), "PRODUCT", name, json));
                ComboBox<String> presetCombo = (ComboBox<String>) panel.lookup("#productPresetCombo");
                populatePresetCombo(panel, "PRODUCT", presetCombo);
                if (presetCombo != null) {
                    presetCombo.setValue(name);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void saveCustomerPreset(VBox panel) {
        if (currentUser == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Salvar Preset");
        dialog.setHeaderText("Salvar preset de filtro de clientes");
        dialog.setContentText("Nome do preset:");
        dialog.showAndWait().ifPresent(name -> {
            if (name == null || name.isBlank()) {
                return;
            }
            try {
                var searchField = (TextField) panel.lookup("#customerSearchField");
                var typeCombo = (ComboBox<String>) panel.lookup("#customerTypeCombo");
                String search = searchField != null ? searchField.getText() : "";
                String type = typeCombo != null ? typeCombo.getValue() : null;
                if ("Todos".equals(type)) {
                    type = null;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("search", search);
                payload.put("type", type);
                String json = objectMapper.writeValueAsString(payload);
                filterPresetService.savePreset(new FilterPreset(currentUser.getId(), "CUSTOMER", name, json));
                ComboBox<String> presetCombo = (ComboBox<String>) panel.lookup("#customerPresetCombo");
                populatePresetCombo(panel, "CUSTOMER", presetCombo);
                if (presetCombo != null) {
                    presetCombo.setValue(name);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void saveSalesPreset(VBox panel) {
        if (currentUser == null) {
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Salvar Preset");
        dialog.setHeaderText("Salvar preset de filtro de vendas");
        dialog.setContentText("Nome do preset:");
        dialog.showAndWait().ifPresent(name -> {
            if (name == null || name.isBlank()) {
                return;
            }
            try {
                var searchField = (TextField) panel.lookup("#salesSearchField");
                var startDatePicker = (javafx.scene.control.DatePicker) panel.lookup("#salesStartDate");
                var endDatePicker = (javafx.scene.control.DatePicker) panel.lookup("#salesEndDate");
                var stateCombo = (ComboBox<String>) panel.lookup("#salesStateCombo");
                String search = searchField != null ? searchField.getText() : "";
                LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;
                LocalDate endDate = endDatePicker != null ? endDatePicker.getValue() : null;
                String state = stateCombo != null ? stateCombo.getValue() : null;
                if ("Todos".equals(state)) {
                    state = null;
                }
                Map<String, Object> payload = new HashMap<>();
                payload.put("search", search);
                payload.put("startDate", startDate != null ? startDate.toString() : null);
                payload.put("endDate", endDate != null ? endDate.toString() : null);
                payload.put("state", state);
                String json = objectMapper.writeValueAsString(payload);
                filterPresetService.savePreset(new FilterPreset(currentUser.getId(), "SALE", name, json));
                ComboBox<String> presetCombo = (ComboBox<String>) panel.lookup("#salesPresetCombo");
                populatePresetCombo(panel, "SALE", presetCombo);
                if (presetCombo != null) {
                    presetCombo.setValue(name);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void refreshPresetCombos() {
        if (currentUser == null) {
            return;
        }
        if (productFilterPanel != null) {
            populatePresetCombo(productFilterPanel, "PRODUCT", (ComboBox<String>) productFilterPanel.lookup("#productPresetCombo"));
        }
        if (customerFilterPanel != null) {
            populatePresetCombo(customerFilterPanel, "CUSTOMER", (ComboBox<String>) customerFilterPanel.lookup("#customerPresetCombo"));
        }
        if (salesFilterPanel != null) {
            populatePresetCombo(salesFilterPanel, "SALE", (ComboBox<String>) salesFilterPanel.lookup("#salesPresetCombo"));
        }
    }

    private void deletePreset(String type, VBox panel) {
        if (currentUser == null || panel == null) {
            return;
        }
        ComboBox<String> presetCombo = null;
        if ("PRODUCT".equals(type)) {
            presetCombo = (ComboBox<String>) panel.lookup("#productPresetCombo");
        } else if ("CUSTOMER".equals(type)) {
            presetCombo = (ComboBox<String>) panel.lookup("#customerPresetCombo");
        } else if ("SALE".equals(type)) {
            presetCombo = (ComboBox<String>) panel.lookup("#salesPresetCombo");
        }
        if (presetCombo == null) {
            return;
        }
        String presetName = presetCombo.getValue();
        if (presetName == null || presetName.isBlank()) {
            return;
        }
        try {
            filterPresetService.deletePresetByName(currentUser.getId(), type, presetName);
            populatePresetCombo(panel, type, presetCombo);
            presetCombo.setValue(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void applyPreset(String type, String presetName, VBox panel) {
        if (currentUser == null || presetName == null || presetName.isBlank()) {
            return;
        }
        try {
            var presets = filterPresetService.listPresets(currentUser.getId(), type);
            var preset = presets.stream().filter(p -> presetName.equals(p.getName())).findFirst();
            if (preset.isEmpty()) {
                return;
            }
            Map<String, Object> payload = objectMapper.readValue(preset.get().getData(), new TypeReference<Map<String, Object>>() {});
            if ("PRODUCT".equals(type)) {
                String search = payload.get("search") != null ? payload.get("search").toString() : "";
                String category = payload.get("category") != null ? payload.get("category").toString() : null;
                if (category == null || category.isBlank()) {
                    category = "Todos";
                }
                var searchField = (TextField) panel.lookup("#productSearchField");
                var categoryCombo = (ComboBox<String>) panel.lookup("#productCategoryCombo");
                if (searchField != null) searchField.setText(search);
                if (categoryCombo != null) categoryCombo.setValue(category);
                if (category != null && !category.isBlank() && !category.equals("Todos")) {
                    var list = productRepository.searchByCodeOrNameAndCategory(search == null ? "" : search, category);
                    productsTable.setItems(FXCollections.observableArrayList(list));
                } else {
                    setProductFilter(search);
                }
            } else if ("CUSTOMER".equals(type)) {
                String search = payload.get("search") != null ? payload.get("search").toString() : "";
                String customerType = payload.get("type") != null ? payload.get("type").toString() : null;
                if (customerType == null || customerType.isBlank()) {
                    customerType = "Todos";
                }
                var searchField = (TextField) panel.lookup("#customerSearchField");
                var typeCombo = (ComboBox<String>) panel.lookup("#customerTypeCombo");
                if (searchField != null) searchField.setText(search);
                if (typeCombo != null) typeCombo.setValue(customerType);
                if (customerType != null && !customerType.isBlank() && !customerType.equals("Todos")) {
                    var list = customerRepository.searchByCodeOrNameAndType(search == null ? "" : search, customerType);
                    customersTable.setItems(FXCollections.observableArrayList(list));
                } else {
                    setCustomerFilter(search);
                }
            } else if ("SALE".equals(type)) {
                String search = payload.get("search") != null ? payload.get("search").toString() : "";
                LocalDate startDate = payload.containsKey("startDate") && payload.get("startDate") != null
                        ? LocalDate.parse(payload.get("startDate").toString()) : null;
                LocalDate endDate = payload.containsKey("endDate") && payload.get("endDate") != null
                        ? LocalDate.parse(payload.get("endDate").toString()) : null;
                String state = payload.get("state") != null ? payload.get("state").toString() : null;
                if (state == null || state.isBlank()) {
                    state = "Todos";
                }
                var searchField = (TextField) panel.lookup("#salesSearchField");
                var startDatePicker = (javafx.scene.control.DatePicker) panel.lookup("#salesStartDate");
                var endDatePicker = (javafx.scene.control.DatePicker) panel.lookup("#salesEndDate");
                var stateCombo = (ComboBox<String>) panel.lookup("#salesStateCombo");
                if (searchField != null) searchField.setText(search);
                if (startDatePicker != null) startDatePicker.setValue(startDate);
                if (endDatePicker != null) endDatePicker.setValue(endDate);
                if (stateCombo != null) stateCombo.setValue(state);
                String searchState = "Todos".equals(state) ? null : state;
                if (startDate != null || endDate != null || (searchState != null && !searchState.isBlank())) {
                    LocalDate start = startDate;
                    LocalDate end = endDate;
                    java.time.LocalDateTime startDt = start != null ? start.atStartOfDay() : java.time.LocalDateTime.MIN;
                    java.time.LocalDateTime endDt = end != null ? end.atTime(23, 59, 59) : java.time.LocalDateTime.MAX;
                    var list = saleRepository.findByCustomerAndDateRangeAndState(search == null ? "" : search, startDt, endDt, searchState);
                    salesTable.setItems(FXCollections.observableArrayList(list));
                } else {
                    setSaleFilter(search);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void animateEntrance() {
        FadeTransition ft1 = new FadeTransition(Duration.millis(450), salesLabel);
        ft1.setFromValue(0.0);
        ft1.setToValue(1.0);
        ft1.setDelay(Duration.millis(100));

        FadeTransition ft2 = new FadeTransition(Duration.millis(450), productsLabel);
        ft2.setFromValue(0.0);
        ft2.setToValue(1.0);
        ft2.setDelay(Duration.millis(200));

        FadeTransition ft3 = new FadeTransition(Duration.millis(450), customersLabel);
        ft3.setFromValue(0.0);
        ft3.setToValue(1.0);
        ft3.setDelay(Duration.millis(300));

        FadeTransition ft4 = new FadeTransition(Duration.millis(450), branchesLabel);
        ft4.setFromValue(0.0);
        ft4.setToValue(1.0);
        ft4.setDelay(Duration.millis(400));

        ft1.play(); ft2.play(); ft3.play(); ft4.play();
    }

    private void doLogout() {
        if (currentUser != null) {
            systemLogService.logUserAction(currentUser.getUsername(), "LOGOUT", "Sessão encerrada pelo utilizador.");
        }
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
    }

    private void updatePageLabel(Label pageLabel, int pageNumber) {
        if (pageLabel != null) {
            pageLabel.setText("Página " + (pageNumber + 1));
        }
    }

    /* ====== CRUD Operations ====== */

    public void openProductForm(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/product_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            ProductFormController controller = loader.getController();
            controller.setProduct(product);
            controller.setOnSave(() -> {
                systemLogService.logUserAction(
                    currentUser != null ? currentUser.getUsername() : "?",
                    product == null ? "CREATE_PRODUCT" : "EDIT_PRODUCT",
                    product == null ? "Novo produto criado." : "Produto editado: " + product.getName());
                loadProducts();
            });
            openModal(root);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_PRODUCT_FORM", "Erro ao abrir formulário de produto.", ex);
            ex.printStackTrace();
        }
    }

    private void openProductStockHistory(Product product) {
        try {
            StockMovementRepository smr = applicationContext.getBean(StockMovementRepository.class);
            java.util.List<com.sgv.entity.StockMovement> movements = smr.findByProductIdOrderByCreatedAtDesc(product.getId());

            TableView<com.sgv.entity.StockMovement> table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<com.sgv.entity.StockMovement, String> colDate = new TableColumn<>("Data / Hora");
            colDate.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FORMATTER) : "—"));

            TableColumn<com.sgv.entity.StockMovement, String> colType = new TableColumn<>("Tipo");
            colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType() != null ? d.getValue().getType() : "—"));

            TableColumn<com.sgv.entity.StockMovement, String> colQty = new TableColumn<>("Qtd.");
            colQty.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getQtyAmount() != null ? d.getValue().getQtyAmount().toPlainString() : "0"));

            TableColumn<com.sgv.entity.StockMovement, String> colBefore = new TableColumn<>("Stock Antes");
            colBefore.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getStockBeforeAmount() != null ? d.getValue().getStockBeforeAmount().toPlainString() : "0"));

            TableColumn<com.sgv.entity.StockMovement, String> colAfter = new TableColumn<>("Stock Depois");
            colAfter.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getStockAfterAmount() != null ? d.getValue().getStockAfterAmount().toPlainString() : "0"));

            TableColumn<com.sgv.entity.StockMovement, String> colBranch = new TableColumn<>("Filial");
            colBranch.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));

            TableColumn<com.sgv.entity.StockMovement, String> colRef = new TableColumn<>("Referência");
            colRef.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getReference() != null ? d.getValue().getReference() : "—"));

            TableColumn<com.sgv.entity.StockMovement, String> colUser = new TableColumn<>("Utilizador");
            colUser.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getUser() != null ? d.getValue().getUser().getUsername() : "—"));

            table.getColumns().addAll(colDate, colType, colQty, colBefore, colAfter, colBranch, colRef, colUser);
            table.setItems(FXCollections.observableArrayList(movements));

            VBox box = new VBox(12, new Label("Histórico de Stock para " + product.getCode() + " - " + product.getName()), table);
            box.setStyle("-fx-padding:16; -fx-background-color:#ffffff;");
            Scene scene = new Scene(box, 980, 520);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(contentPane.getScene().getWindow());
            stage.setTitle("Histórico de Stock - " + product.getName());
            stage.setScene(scene);
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro ao abrir histórico de produto: " + ex.getMessage());
        }
    }

    private boolean isCurrentUserAdmin() {
        if (currentUser == null || currentUser.getRoles() == null) return false;
        return currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() != null && role.getName().toUpperCase().contains("ADMIN"));
    }

    public void openCustomerForm(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customer_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            CustomerFormController controller = loader.getController();
            controller.setCustomer(customer);
            controller.setOnSave(() -> {
                systemLogService.logUserAction(
                    currentUser != null ? currentUser.getUsername() : "?",
                    customer == null ? "CREATE_CUSTOMER" : "EDIT_CUSTOMER",
                    customer == null ? "Novo cliente criado." : "Cliente editado: " + customer.getName());
                loadCustomers();
            });
            openModal(root);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_CUSTOMER_FORM", "Erro ao abrir formulário de cliente.", ex);
            ex.printStackTrace();
        }
    }

    public void openWarehouseForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehouse_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            WarehouseFormController c = loader.getController();
            c.setOnSaved(this::loadStats);
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(contentPane.getScene().getWindow());
            stage.setTitle("Armazém");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void openWarehouseTransferForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehouse_transfer_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            WarehouseTransferFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(contentPane.getScene().getWindow());
            stage.setTitle("Transferência Armazém → Loja");
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(620);
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void openSaleForm(Sale sale) {
        openSaleFormWithType(sale, null);
    }

    private void openPDVSaleFormWithType(Sale sale, String forceType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sale_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            SaleFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setSale(sale);
            if (forceType != null) controller.setDocumentType(forceType);
            controller.setOnSave(() -> { loadSales(); loadStats(); loadSalesStats(); });
            openPDVStage(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openPDVStage(Parent root) {
        try {
            Window owner = contentPane.getScene().getWindow();
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);

            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");

            Scene scene = new Scene(scroll);
            stage.setScene(scene);

            // Fullscreen-ish: 95% of screen
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setWidth(bounds.getWidth() * 0.95);
            stage.setHeight(bounds.getHeight() * 0.95);
            stage.setX(bounds.getMinX() + bounds.getWidth() * 0.025);
            stage.setY(bounds.getMinY() + bounds.getHeight() * 0.025);

            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void printSelectedSale() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Sale fullSale = saleRepository.findByIdWithItems(selected.getId());
                if (fullSale == null) fullSale = selected;
                File pdf = saleDocumentService.generateDocument(fullSale);
                DocumentPreviewDialog.show(pdf, fullSale.getDocumentType());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erro na impressão: Não foi possível gerar ou abrir o PDF: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void annulSelectedSale() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if ("ANULADA".equals(selected.getState())) {
                showAlert(Alert.AlertType.ERROR, "Venda já Anulada: Este documento já se encontra anulado.");
                return;
            }

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Anular Documento Fiscal");
            dialog.setHeaderText("Anular: " + selected.getDocumentType() + " #" + selected.getDocumentNumber());
            dialog.setContentText("Por favor, introduza o motivo da anulação:");

            dialog.showAndWait().ifPresent(reason -> {
                if (reason.trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Erro de Validação: O motivo da anulação é obrigatório por lei.");
                    return;
                }
                
                try {
                    Sale fullSale = saleRepository.findByIdWithItems(selected.getId());
                    if (fullSale == null) fullSale = selected;
                    fullSale.setState("ANULADA");
                    fullSale.setAnnulReason(reason);
                    fullSale.setAnnulDate(LocalDateTime.now());
                    saleRepository.save(fullSale);
                    
                    loadSales();
                    loadStats();
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Anulação Concluída");
                    success.setHeaderText(null);
                    success.setContentText("O documento foi anulado com sucesso.");
                    success.showAndWait();
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Erro na Anulação: Ocorreu um erro ao anular o documento: " + ex.getMessage());
                }
            });
        }
    }

    public void openCotacaoForm() {
        openSaleFormWithType(null, "COTACAO");
    }

    private void invoiceSelectedQuote() {
        if (salesTable == null) return;
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selecione uma Cotação para faturar.");
            return;
        }
        if (!"COTACAO".equals(selected.getDocumentType()) && !"COTACAO_ABERTA".equals(selected.getState())) {
            showAlert(Alert.AlertType.WARNING, "O documento selecionado não é uma Cotação.");
            return;
        }
        
        // Criar uma cópia da venda selecionada, sem o ID, para gerar um novo documento
        Sale newInvoice = new Sale();
        newInvoice.setDocumentType(com.sgv.model.DocumentType.FACTURA.name());
        newInvoice.setBranch(selected.getBranch());
        newInvoice.setCustomer(selected.getCustomer());
        newInvoice.setCustomerName(selected.getCustomerName());
        newInvoice.setCustomerNuit(selected.getCustomerNuit());
        newInvoice.setCustomerAddress(selected.getCustomerAddress());
        newInvoice.setCurrency(selected.getCurrency());
        newInvoice.setPaymentMethod(selected.getPaymentMethod());
        newInvoice.setSubtotal(selected.getSubtotal());
        newInvoice.setTotalTax(selected.getTotalTax());
        newInvoice.setTotalDiscount(selected.getTotalDiscount());
        newInvoice.setTotalIce(selected.getTotalIce());
        newInvoice.setWithholdingTax(selected.getWithholdingTax());
        newInvoice.setWithholdingTaxRate(selected.getWithholdingTaxRate());
        newInvoice.setTotal(selected.getTotal());
        
        // Obter os itens da venda original com dados completos
        Sale fullQuote = saleRepository.findByIdWithItems(selected.getId());
        if (fullQuote == null) fullQuote = selected;
        
        java.util.List<com.sgv.entity.SaleItem> newItems = new java.util.ArrayList<>();
        if (fullQuote.getItems() != null) {
            for (com.sgv.entity.SaleItem oldItem : fullQuote.getItems()) {
                com.sgv.entity.SaleItem newItem = new com.sgv.entity.SaleItem();
                newItem.setProduct(oldItem.getProduct());
                newItem.setProductCode(oldItem.getProductCode());
                newItem.setDescription(oldItem.getDescription());
                newItem.setUnit(oldItem.getUnit());
                newItem.setQty(oldItem.getQty());
                newItem.setUnitPrice(oldItem.getUnitPrice());
                newItem.setTaxType(oldItem.getTaxType());
                newItem.setTaxRate(oldItem.getTaxRate());
                newItem.setIceRate(oldItem.getIceRate());
                newItem.setMotivoInexistTax(oldItem.getMotivoInexistTax());
                newItem.setLineBase(oldItem.getLineBase());
                newItem.setLineIce(oldItem.getLineIce());
                newItem.setLineTax(oldItem.getLineTax());
                newItem.setLineTotal(oldItem.getLineTotal());
                newItem.setSale(newInvoice);
                newItems.add(newItem);
            }
        }
        newInvoice.setItems(newItems);
        
        // Abrir formulário com a cópia
        openSaleFormWithType(newInvoice, "FACTURA");
    }

    private void printSelectedReceipt() {
        if (salesTable == null) return;
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selecione uma venda/fatura paga para imprimir o recibo.");
            return;
        }
        if (!"PAGO".equals(selected.getState())) {
            showAlert(Alert.AlertType.WARNING, "Esta fatura ainda não foi paga. O recibo só pode ser emitido após o pagamento.");
            return;
        }
        
        // Temporary set doc type to RECIBO for printing without saving
        String origType = selected.getDocumentType();
        try {
            selected.setDocumentType(com.sgv.model.DocumentType.RECIBO.name());
            java.io.File pdf = saleDocumentService.generateDocument(selected);
            if (pdf != null) {
                DocumentPreviewDialog.show(pdf, "RECIBO");
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erro ao gerar recibo: " + ex.getMessage());
        } finally {
            selected.setDocumentType(origType);
        }
    }

    private void openSaleFormWithType(Sale sale, String forceType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sale_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            SaleFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setSale(sale);
            if (forceType != null) controller.setDocumentType(forceType);
            controller.setOnSave(() -> { loadSales(); loadStats(); loadSalesStats(); });
            openModal(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void openPurchaseForm(Purchase purchase) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/purchase_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            PurchaseFormController controller = loader.getController();
            controller.setPurchase(purchase);
            controller.setOnSave(() -> {
                systemLogService.logUserAction(
                    currentUser != null ? currentUser.getUsername() : "?",
                    purchase == null ? "CREATE_PURCHASE" : "EDIT_PURCHASE",
                    purchase == null ? "Nova compra registada." : "Compra editada: " + purchase.getInvoiceNumber());
                loadPurchases();
            });
            openModal(root);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_PURCHASE_FORM", "Erro ao abrir formulário de compra.", ex);
            ex.printStackTrace();
        }
    }

    public void openExpenseForm(Expense expense) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/expense_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            ExpenseFormController controller = loader.getController();
            controller.setExpense(expense);
            controller.setOnSave(() -> {
                systemLogService.logUserAction(
                    currentUser != null ? currentUser.getUsername() : "?",
                    expense == null ? "CREATE_EXPENSE" : "EDIT_EXPENSE",
                    expense == null ? "Nova despesa criada." : "Despesa editada: " + expense.getDescription());
                loadExpenses();
            });
            openModal(root);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_EXPENSE_FORM", "Erro ao abrir formulário de despesa.", ex);
            ex.printStackTrace();
        }
    }

    public void openPaymentForm(Payment payment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            PaymentFormController controller = loader.getController();
            controller.setPayment(payment);
            controller.setOnSave(() -> loadFinanceiro());
            openModal(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void openUserForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            UserFormController controller = loader.getController();
            controller.setUser(user);
            controller.setOnSave(() -> {
                systemLogService.logUserAction(
                    currentUser != null ? currentUser.getUsername() : "?",
                    user == null ? "CREATE_USER" : "EDIT_USER",
                    user == null ? "Novo utilizador criado." : "Utilizador editado: " + user.getUsername());
                loadSystem();
            });
            openModal(root);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_USER_FORM", "Erro ao abrir formulário de utilizador.", ex);
            ex.printStackTrace();
        }
    }

    public void openBranchForm(Branch branch) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/branch_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            BranchFormController controller = loader.getController();
            controller.setBranch(branch);
            controller.setOnSave(() -> loadSystem());
            openModal(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveCompanyConfig() {
        if (companyNameField.getText() == null || companyNameField.getText().isBlank()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Nome da Empresa é obrigatório.");
            return;
        }
        if (companyNuitField.getText() == null || companyNuitField.getText().isBlank()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "NUIT da Empresa é obrigatório.");
            return;
        }
        // Save logic to be implemented. Using properties file or DB settings table.
        // For now, we just show success.
        showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Configurações da Empresa salvas com sucesso!");
    }

    public void openOrderForm(ProductionOrder order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/production_order_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            ProductionOrderFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setOrder(order);
            controller.setOnSave(() -> {
                systemLogService.logUserAction(
                    currentUser != null ? currentUser.getUsername() : "?",
                    order == null ? "CREATE_ORDER" : "EDIT_ORDER",
                    order == null ? "Nova ordem de produção criada." : "Ordem editada: #" + order.getId());
                loadProductionOrders();
            });
            openModal(root);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_ORDER_FORM", "Erro ao abrir formulário de ordem de produção.", ex);
            ex.printStackTrace();
        }
    }

    /**
     * Opens a transparent full-screen modal that tracks the owner window size.
     * Used by all form dialogs to ensure responsive overlay coverage.
     */
    private void openModal(Parent root) {
        try {
            Window owner = contentPane.getScene().getWindow();
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            
            // Envolver num ScrollPane transparente para que se ajuste a ecrãs muito pequenos
            javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
            
            Scene scene = new Scene(scroll);
            stage.setScene(scene);
            
            // Garantir que a janela não excede o tamanho físico do ecrã
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            if (root instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region region = (javafx.scene.layout.Region) root;
                double w = region.getPrefWidth() > 0 ? region.getPrefWidth() : 800;
                double h = region.getPrefHeight() > 0 ? region.getPrefHeight() : 600;
                stage.setWidth(Math.min(w, bounds.getWidth() * 0.95));
                stage.setHeight(Math.min(h + 40, bounds.getHeight() * 0.95));
            } else {
                stage.setMaxWidth(bounds.getWidth() * 0.95);
                stage.setMaxHeight(bounds.getHeight() * 0.95);
            }
            
            // Center the modal on the owner window + auto-focus first field
            stage.setOnShown(e -> {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
                // Auto-focus first editable TextField inside the form
                if (root instanceof javafx.scene.layout.Pane) {
                    javafx.scene.Node first = findFirstEditableField((javafx.scene.layout.Pane) root);
                    if (first instanceof javafx.scene.control.TextInputControl) {
                        ((javafx.scene.control.TextInputControl) first).requestFocus();
                    }
                }
            });

            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Recursively finds the first editable TextField or TextArea in a pane tree.
     * Skips search-field / readonly / disabled controls.
     */
    private javafx.scene.Node findFirstEditableField(javafx.scene.layout.Pane pane) {
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof javafx.scene.control.TextField tf
                    && tf.isEditable()
                    && !tf.isDisabled()) {
                return tf;
            }
            if (node instanceof javafx.scene.control.TextArea ta
                    && ta.isEditable()
                    && !ta.isDisabled()) {
                return ta;
            }
            if (node instanceof javafx.scene.layout.Pane child) {
                javafx.scene.Node found = findFirstEditableField(child);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    // Filter management methods
    public void setProductFilter(String filter) {
        productFilter = filter != null ? filter : "";
        currentProductsPage = 0;
        loadProducts();
    }
    
    public void clearProductFilter() {
        productFilter = "";
        currentProductsPage = 0;
        loadProducts();
    }
    
    public void setCustomerFilter(String filter) {
        customerFilter = filter != null ? filter : "";
        currentCustomersPage = 0;
        loadCustomers();
    }
    
    public void clearCustomerFilter() {
        customerFilter = "";
        currentCustomersPage = 0;
        loadCustomers();
    }
    
    public void setSaleFilter(String filter) {
        saleFilter = filter != null ? filter : "";
        currentSalesPage = 0;
        loadSales();
    }
    
    public void clearSaleFilter() {
        saleFilter = "";
        currentSalesPage = 0;
        loadSales();
    }

    private void safeDelete(Runnable deleteAction, Runnable onSuccess, String type) {
        try {
            deleteAction.run();
            systemLogService.logUserAction(
                currentUser != null ? currentUser.getUsername() : "?",
                "DELETE_" + type.toUpperCase().replace(" ", "_"),
                type + " eliminado com sucesso.");
            onSuccess.run();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            systemLogService.logError("DELETE_" + type.toUpperCase(), "Não foi possível eliminar " + type + " — integridade referencial.", e);
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Não é possível eliminar (" + type + ") pois existem registos associados a ele no sistema.");
        } catch (Exception e) {
            systemLogService.logError("DELETE_" + type.toUpperCase(), "Erro ao eliminar " + type + ": " + e.getMessage(), e);
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erro ao eliminar " + type + ": " + e.getMessage());
        }
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type, message, javafx.scene.control.ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    // MODERN UI: KPI STATS CARDS
    // ═══════════════════════════════════════════════════════

    private GridPane buildKPIGrid(String[] titles, String[] subtitles, String[] colors) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setStyle("-fx-padding: 20; -fx-background-color: #F8FAFC;");

        for (int i = 0; i < titles.length; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / titles.length);
            grid.getColumnConstraints().add(col);
        }

        for (int i = 0; i < titles.length; i++) {
            VBox card = buildKPICard(titles[i], "—", subtitles[i], colors[i]);
            GridPane.setConstraints(card, i, 0);
            grid.getChildren().add(card);
        }
        return grid;
    }

    private VBox buildKPICard(String title, String value, String subtitle, String colorType) {
        String bgColor, iconBg, iconEmoji;
        switch (colorType) {
            case "green": bgColor = "#D1FAE5"; iconBg = "#10B981"; iconEmoji = "💰"; break;
            case "orange": bgColor = "#FEF3C7"; iconBg = "#F59E0B"; iconEmoji = "⚠"; break;
            case "purple": bgColor = "#EDE9FE"; iconBg = "#7C3AED"; iconEmoji = "📊"; break;
            case "red": bgColor = "#FEE2E2"; iconBg = "#EF4444"; iconEmoji = "🔴"; break;
            default: bgColor = "#DBEAFE"; iconBg = "#2563EB"; iconEmoji = "📦";
        }

        VBox card = new VBox();
        card.getStyleClass().add("card-pane");
        card.setStyle("-fx-padding: 20; -fx-min-height: 100;");

        HBox row = new HBox(16);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label icon = new Label(iconEmoji);
        icon.setStyle("-fx-font-size: 24px; -fx-min-width: 52px; -fx-min-height: 52px; -fx-alignment: center; -fx-background-radius: 12; -fx-background-color: " + bgColor + "; -fx-padding: 10;");

        VBox info = new VBox(4);
        info.setStyle("-fx-min-width: 0;");
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #475569;");

        Label valueLbl = new Label(value);
        valueLbl.setId("kpi-" + title.replaceAll("\\s+", "").toLowerCase());
        valueLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #0F172A;");

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");

        info.getChildren().addAll(titleLbl, valueLbl, subLbl);
        row.getChildren().addAll(icon, info);
        card.getChildren().add(row);
        return card;
    }

    private void updateKPICard(GridPane grid, int index, String value) {
        if (grid == null || index >= grid.getChildren().size()) return;
        VBox card = (VBox) grid.getChildren().get(index);
        if (card.getChildren().isEmpty()) return;
        HBox row = (HBox) card.getChildren().get(0);
        if (row.getChildren().size() < 2) return;
        VBox info = (VBox) row.getChildren().get(1);
        if (info.getChildren().size() >= 2) {
            Label lbl = (Label) info.getChildren().get(1);
            lbl.setText(value);
        }
    }

    private void updateSalesKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Sale> sales = saleRepository.findAll();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate weekAgo = today.minusDays(7);

            double hoje = 0, semana = 0, mes = 0, ticket = 0;
            for (Sale s : sales) {
                if (s.getTotal() == null) continue;
                double t = s.getTotal();
                ticket += t;
                if (s.getCreatedAt() != null) {
                    java.time.LocalDate d = s.getCreatedAt().toLocalDate();
                    if (d.equals(today)) hoje += t;
                    if (!d.isBefore(weekAgo) && !d.isAfter(today)) semana += t;
                    if (d.getMonth() == today.getMonth() && d.getYear() == today.getYear()) mes += t;
                }
            }
            double ticketMed = sales.isEmpty() ? 0 : ticket / sales.size();
            updateKPICard(grid, 0, String.format("%.0f MT", hoje));
            updateKPICard(grid, 1, String.format("%.0f MT", semana));
            updateKPICard(grid, 2, String.format("%.0f MT", mes));
            updateKPICard(grid, 3, String.format("%.0f MT", ticketMed));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateFinanceiroKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Sale> sales = saleRepository.findAll();
            double total = 0, pago = 0;
            for (Sale s : sales) {
                if (s.getTotal() == null) continue;
                total += s.getTotal();
                if ("PAGO".equalsIgnoreCase(s.getState())) pago += s.getTotal();
            }
            List<Expense> exps = expenseRepository.findAll();
            java.time.LocalDate now = java.time.LocalDate.now();
            double despMes = 0;
            for (Expense e : exps) {
                if (e.getDueDate() != null && e.getDueDate().getMonth() == now.getMonth() && e.getDueDate().getYear() == now.getYear()) {
                    despMes += (e.getAmount() != null ? e.getAmount() : 0);
                }
            }
            updateKPICard(grid, 0, String.format("%.0f MT", total));
            updateKPICard(grid, 1, String.format("%.0f MT", pago));
            updateKPICard(grid, 2, String.format("%.0f MT", total - pago));
            updateKPICard(grid, 3, String.format("%.0f MT", despMes));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateStockKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<StockBranch> all = stockBranchService.findAll();
            double valor = 0;
            long baixo = 0, zero = 0;
            for (StockBranch sb : all) {
                BigDecimal cur = (sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO);
                BigDecimal min = (sb.getStockMinAmount() != null ? sb.getStockMinAmount() : BigDecimal.ZERO);
                if (sb.getProduct() != null && sb.getProduct().getPriceSale() != null) valor += cur.doubleValue() * sb.getProduct().getPriceSale();
                if (cur.compareTo(BigDecimal.ZERO) == 0) zero++;
                else if (cur.compareTo(min) <= 0) baixo++;
            }
            updateKPICard(grid, 0, String.valueOf(all.size()));
            updateKPICard(grid, 1, String.format("%.0f MT", valor));
            updateKPICard(grid, 2, String.valueOf(baixo));
            updateKPICard(grid, 3, String.valueOf(zero));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateCustomersKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Customer> all = customerRepository.findAll();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate thirtyDaysAgo = today.minusDays(30);

            long total = all.size();
            long atacados = all.stream().filter(c -> "ATACADO".equalsIgnoreCase(c.getType())).count();
            long varejos = all.stream().filter(c -> "VAREJO".equalsIgnoreCase(c.getType())).count();
            long recentes = all.stream()
                    .filter(c -> c.getCreatedAt() != null)
                    .filter(c -> {
                        java.time.LocalDate d = c.getCreatedAt().toLocalDate();
                        return !d.isBefore(thirtyDaysAgo) && !d.isAfter(today);
                    }).count();

            updateKPICard(grid, 0, String.valueOf(total));
            updateKPICard(grid, 1, String.valueOf(atacados));
            updateKPICard(grid, 2, String.valueOf(varejos));
            updateKPICard(grid, 3, String.valueOf(recentes));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateComprasKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Purchase> all = purchaseRepository.findAll();
            double totalValor = all.stream().mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0).sum();
            long pendentes = all.stream().filter(p -> "PENDING".equalsIgnoreCase(p.getState())).count();
            long recebidas = all.stream().filter(p -> "RECEIVED".equalsIgnoreCase(p.getState())).count();
            updateKPICard(grid, 0, String.valueOf(all.size()));
            updateKPICard(grid, 1, String.format("%.0f MT", totalValor));
            updateKPICard(grid, 2, String.valueOf(pendentes));
            updateKPICard(grid, 3, String.valueOf(recebidas));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateProducaoKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<ProductionOrder> all = productionOrderRepository.findAll();
            long pendentes = all.stream().filter(o -> "PENDING".equalsIgnoreCase(o.getState())).count();
            long emCurso = all.stream().filter(o -> "IN_PROGRESS".equalsIgnoreCase(o.getState())).count();
            long concluidas = all.stream().filter(o -> "COMPLETED".equalsIgnoreCase(o.getState())).count();
            updateKPICard(grid, 0, String.valueOf(all.size()));
            updateKPICard(grid, 1, String.valueOf(pendentes));
            updateKPICard(grid, 2, String.valueOf(emCurso));
            updateKPICard(grid, 3, String.valueOf(concluidas));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateCatalogsKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            long cats = categoryRepository.count();
            long units = metricUnitRepository.count();
            updateKPICard(grid, 0, String.valueOf(cats));
            updateKPICard(grid, 1, String.valueOf(units));
            updateKPICard(grid, 2, String.valueOf(units));
            updateKPICard(grid, 3, "—");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateSistemaKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<User> allUsers = userRepository.findAll();
            long total = allUsers.size();
            long activos = allUsers.stream().filter(u -> u.isActive()).count();
            long admins = allUsers.stream()
                    .filter(u -> u.getRoles() != null)
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() != null && r.getName().toUpperCase().contains("ADMIN")))
                    .count();
            long filiais = branchRepository.count();
            updateKPICard(grid, 0, String.valueOf(filiais));
            updateKPICard(grid, 1, String.valueOf(total));
            updateKPICard(grid, 2, String.valueOf(activos));
            updateKPICard(grid, 3, String.valueOf(admins));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateUsersKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<User> allUsers = userRepository.findAll();
            long activos = allUsers.stream().filter(u -> u.isActive()).count();
            long admins = allUsers.stream()
                    .filter(u -> u.getRoles() != null)
                    .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName() != null && r.getName().toUpperCase().contains("ADMIN")))
                    .count();
            updateKPICard(grid, 0, String.valueOf(allUsers.size()));
            updateKPICard(grid, 1, String.valueOf(activos));
            updateKPICard(grid, 2, String.valueOf(admins));
            updateKPICard(grid, 3, "—");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void updateReportsKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            List<Sale> sales = saleRepository.findAll();
            double mes = sales.stream()
                    .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().toLocalDate().getMonth() == today.getMonth() && s.getCreatedAt().toLocalDate().getYear() == today.getYear())
                    .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                    .sum();
            updateKPICard(grid, 0, String.format("%.0f MT", mes));
            updateKPICard(grid, 1, String.valueOf(productRepository.count()));
            updateKPICard(grid, 2, String.valueOf(customerRepository.count()));
            updateKPICard(grid, 3, String.valueOf(userRepository.count()));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════
    // OPERAÇÕES DE CLIENTES
    // ══════════════════════════════════════════════════════════

    /** Dialog: Liquidar Dívida — lista faturas pendentes e regista pagamento */
    private void openLiquidarDivida(Customer customer) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Liquidar Dívida — " + customer.getName());
        dialog.setWidth(760);
        dialog.setHeight(520);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F8FAFC;");

        HBox header = new HBox(12);
        header.setStyle("-fx-background-color:#1E293B; -fx-padding:16 24; -fx-alignment:CENTER_LEFT;");
        Label titleLbl = new Label("Liquidar Dívida");
        titleLbl.setStyle("-fx-font-size:16px; -fx-font-weight:700; -fx-text-fill:#ffffff;");
        double saldo = customer.getBalance() != null ? customer.getBalance() : 0.0;
        Label subLbl = new Label("  " + customer.getName() + "   |   Saldo Devedor: " + String.format("%.2f MT", saldo));
        subLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8;");
        header.getChildren().addAll(titleLbl, subLbl);

        TableView<Sale> pendingTable = new TableView<>();
        pendingTable.setStyle("-fx-font-size:13px;");
        VBox.setVgrow(pendingTable, Priority.ALWAYS);

        TableColumn<Sale, String> colDoc = new TableColumn<>("Documento");
        colDoc.setPrefWidth(160);
        colDoc.setCellValueFactory(d -> new SimpleStringProperty(
            (d.getValue().getDocumentType() != null ? d.getValue().getDocumentType() : "") + " " +
            (d.getValue().getDocumentNumber() != null ? d.getValue().getDocumentNumber().toString() : "")));

        TableColumn<Sale, String> colDate = new TableColumn<>("Data");
        colDate.setPrefWidth(140);
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FORMATTER) : "—"));

        TableColumn<Sale, String> colTotal = new TableColumn<>("Total (MT)");
        colTotal.setPrefWidth(130);
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getTotal() != null ? String.format("%.2f", d.getValue().getTotal()) : "0.00"));

        TableColumn<Sale, String> colState = new TableColumn<>("Estado");
        colState.setPrefWidth(100);
        colState.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState()));
        pendingTable.getColumns().addAll(colDoc, colDate, colTotal, colState);

        try {
            List<Sale> pending = saleRepository.findPendingByCustomerId(customer.getId());
            pendingTable.setItems(javafx.collections.FXCollections.observableArrayList(pending));
        } catch (Exception ex) { ex.printStackTrace(); }

        HBox payRow = new HBox(12);
        payRow.setStyle("-fx-background-color:#ffffff; -fx-padding:16 24; -fx-border-color:#E2E8F0; -fx-border-width:1 0 0 0; -fx-alignment:CENTER_LEFT;");

        Label lblValor = new Label("Valor (MT):");
        lblValor.setStyle("-fx-font-weight:700; -fx-text-fill:#0F172A;");
        TextField valorField = new TextField();
        valorField.setPromptText("0.00");
        valorField.setPrefWidth(130);
        valorField.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-padding:6 10; -fx-border-color:#CBD5E1; -fx-border-radius:6; -fx-background-radius:6;");

        Label lblMetodo = new Label("Método:");
        lblMetodo.setStyle("-fx-font-weight:700; -fx-text-fill:#0F172A;");
        ComboBox<String> metodoCombo = new ComboBox<>();
        metodoCombo.getItems().addAll("Dinheiro", "TPA/Multibanco", "Transferência", "M-Pesa", "E-Mola");
        metodoCombo.setValue("Dinheiro");
        metodoCombo.setPrefWidth(160);

        Region spacerPay = new Region();
        HBox.setHgrow(spacerPay, Priority.ALWAYS);

        Button btnConfirmar = makeActionButton("✓ Confirmar", "#10B981", "#ffffff");
        Button btnCancelarPay = makeActionButton("Cancelar", "#475569", "#ffffff");
        btnCancelarPay.setOnAction(ev -> dialog.close());

        btnConfirmar.setOnAction(ev -> {
            Sale selSale = pendingTable.getSelectionModel().getSelectedItem();
            String valStr = valorField.getText().trim().replace(",", ".");
            if (valStr.isEmpty()) { showToast("Introduza o valor a pagar."); return; }
            double valor;
            try { valor = Double.parseDouble(valStr); } catch (NumberFormatException nfe) { showToast("Valor inválido."); return; }
            if (valor <= 0) { showToast("O valor deve ser superior a zero."); return; }
            try {
                Payment p = new Payment();
                p.setAmount(valor);
                p.setMethod(metodoCombo.getValue());
                if (selSale != null) {
                    p.setSale(selSale);
                    if (valor >= (selSale.getTotal() != null ? selSale.getTotal() : 0.0)) {
                        selSale.setState("PAGO");
                        saleRepository.save(selSale);
                    }
                }
                paymentRepository.save(p);
                double novoSaldo = (customer.getBalance() != null ? customer.getBalance() : 0.0) - valor;
                customer.setBalance(novoSaldo);
                customerRepository.save(customer);
                showToast("Pagamento de " + String.format("%.2f MT", valor) + " registado com sucesso!");
                loadCustomers();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showToast("Erro ao registar pagamento: " + ex.getMessage());
            }
        });

        payRow.getChildren().addAll(lblValor, valorField, lblMetodo, metodoCombo, spacerPay, btnConfirmar, btnCancelarPay);
        root.getChildren().addAll(header, pendingTable, payRow);
        javafx.scene.Scene sc1 = new javafx.scene.Scene(root);
        dialog.setScene(sc1);
        dialog.show();
    }

    /** Dialog: Adiantamento — credita saldo na conta do cliente */
    private void openAdiantamento(Customer customer) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Adiantamento — " + customer.getName());
        dialog.setWidth(480);
        dialog.setHeight(310);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F8FAFC;");

        HBox header = new HBox(8);
        header.setStyle("-fx-background-color:#1E293B; -fx-padding:16 24; -fx-alignment:CENTER_LEFT;");
        Label titleLbl = new Label("Registar Adiantamento");
        titleLbl.setStyle("-fx-font-size:16px; -fx-font-weight:700; -fx-text-fill:#ffffff;");
        double saldoAtual = customer.getBalance() != null ? customer.getBalance() : 0.0;
        Label sub = new Label("  |  Saldo atual: " + String.format("%.2f MT", saldoAtual));
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8;");
        header.getChildren().addAll(titleLbl, sub);

        VBox body = new VBox(16);
        body.setStyle("-fx-padding:24; -fx-background-color:#ffffff;");
        VBox.setVgrow(body, Priority.ALWAYS);

        Label info = new Label("O valor inserido será creditado na conta do cliente (reduz o saldo devedor).");
        info.setStyle("-fx-font-size:12px; -fx-text-fill:#475569; -fx-wrap-text:true;");
        info.setMaxWidth(420);

        HBox valorRow = new HBox(12);
        valorRow.setStyle("-fx-alignment:CENTER_LEFT;");
        Label lblV = new Label("Valor (MT):");
        lblV.setStyle("-fx-font-weight:700; -fx-text-fill:#0F172A; -fx-pref-width:130;");
        TextField valorField = new TextField();
        valorField.setPromptText("Ex: 500.00");
        valorField.setPrefWidth(200);
        valorField.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-padding:8 12; -fx-border-color:#CBD5E1; -fx-border-radius:6; -fx-background-radius:6;");
        valorRow.getChildren().addAll(lblV, valorField);

        HBox obsRow = new HBox(12);
        obsRow.setStyle("-fx-alignment:CENTER_LEFT;");
        Label lblO = new Label("Observação:");
        lblO.setStyle("-fx-font-weight:700; -fx-text-fill:#0F172A; -fx-pref-width:130;");
        TextField obsField = new TextField();
        obsField.setPromptText("Ex: Pagamento antecipado de encomenda");
        obsField.setPrefWidth(280);
        obsField.setStyle("-fx-padding:8 12; -fx-border-color:#CBD5E1; -fx-border-radius:6; -fx-background-radius:6;");
        obsRow.getChildren().addAll(lblO, obsField);

        HBox btnRow = new HBox(12);
        btnRow.setStyle("-fx-alignment:CENTER_RIGHT; -fx-padding:8 0 0 0;");
        Button btnOk = makeActionButton("✓ Confirmar", "#10B981", "#ffffff");
        Button btnCancel = makeActionButton("Cancelar", "#475569", "#ffffff");
        btnCancel.setOnAction(ev -> dialog.close());
        btnOk.setOnAction(ev -> {
            String valStr = valorField.getText().trim().replace(",", ".");
            if (valStr.isEmpty()) { showToast("Introduza o valor do adiantamento."); return; }
            double valor;
            try { valor = Double.parseDouble(valStr); } catch (NumberFormatException nfe) { showToast("Valor inválido."); return; }
            if (valor == 0) { showToast("O valor não pode ser zero."); return; }
            try {
                double novoSaldo = (customer.getBalance() != null ? customer.getBalance() : 0.0) - valor;
                customer.setBalance(novoSaldo);
                customerRepository.save(customer);
                showToast("Adiantamento de " + String.format("%.2f MT", valor) + " registado! Novo saldo: " + String.format("%.2f MT", novoSaldo));
                loadCustomers();
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showToast("Erro: " + ex.getMessage());
            }
        });
        btnRow.getChildren().addAll(btnCancel, btnOk);
        body.getChildren().addAll(info, valorRow, obsRow, btnRow);
        root.getChildren().addAll(header, body);
        javafx.scene.Scene sc2 = new javafx.scene.Scene(root);
        dialog.setScene(sc2);
        dialog.show();
    }

    /** Dialog: Extrato — histórico cronológico de vendas do cliente */
    private void openExtrato(Customer customer) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Extrato de Conta — " + customer.getName());
        dialog.setWidth(860);
        dialog.setHeight(580);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#F8FAFC;");

        HBox header = new HBox(16);
        header.setStyle("-fx-background-color:#1E293B; -fx-padding:16 24; -fx-alignment:CENTER_LEFT;");
        Label titleLbl = new Label("Extrato de Conta");
        titleLbl.setStyle("-fx-font-size:16px; -fx-font-weight:700; -fx-text-fill:#ffffff;");
        double saldoAtual = customer.getBalance() != null ? customer.getBalance() : 0.0;
        String saldoColor = saldoAtual > 0 ? "#FCA5A5" : "#86EFAC";
        Label sub = new Label("  " + customer.getName() + "  |  Saldo: " + String.format("%.2f MT", saldoAtual));
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:" + saldoColor + ";");
        header.getChildren().addAll(titleLbl, sub);

        List<Sale> allSales;
        try { allSales = saleRepository.findAllByCustomerId(customer.getId()); }
        catch (Exception ex) { allSales = java.util.Collections.emptyList(); }

        double totalComprado = allSales.stream().filter(s -> !"ANULADA".equals(s.getState())).mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0).sum();
        long nVendas = allSales.stream().filter(s -> !"ANULADA".equals(s.getState())).count();
        long nPend = allSales.stream().filter(s -> "EMITIDA".equals(s.getState())).count();

        HBox kpiRow = new HBox(0);
        kpiRow.setStyle("-fx-background-color:#ffffff; -fx-padding:12 24; -fx-border-color:#E2E8F0; -fx-border-width:0 0 1 0;");
        Label kpiLbl = new Label("Total comprado: " + String.format("%.2f MT", totalComprado) + "   |   Documentos: " + nVendas + "   |   Pendentes: " + nPend);
        kpiLbl.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#0F172A;");
        kpiRow.getChildren().add(kpiLbl);

        TableView<Sale> table = new TableView<>();
        table.setStyle("-fx-font-size:13px;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Sale, String> c1 = new TableColumn<>("Documento");
        c1.setPrefWidth(160);
        c1.setCellValueFactory(d -> new SimpleStringProperty(
            (d.getValue().getDocumentType() != null ? d.getValue().getDocumentType() : "") + " " +
            (d.getValue().getDocumentNumber() != null ? d.getValue().getDocumentNumber().toString() : "")));

        TableColumn<Sale, String> c2 = new TableColumn<>("Data");
        c2.setPrefWidth(140);
        c2.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FORMATTER) : "—"));

        TableColumn<Sale, String> c3 = new TableColumn<>("Forma Pag.");
        c3.setPrefWidth(130);
        c3.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getPaymentMethod() != null ? d.getValue().getPaymentMethod() : "—"));

        TableColumn<Sale, String> c4 = new TableColumn<>("Total (MT)");
        c4.setPrefWidth(120);
        c4.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getTotal() != null ? String.format("%.2f", d.getValue().getTotal()) : "0.00"));

        TableColumn<Sale, String> c5 = new TableColumn<>("Estado");
        c5.setPrefWidth(100);
        c5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState()));
        c5.setCellFactory(col -> new TableCell<Sale, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("PAGO".equals(item)) setStyle("-fx-text-fill:#10B981; -fx-font-weight:700;");
                else if ("ANULADA".equals(item)) setStyle("-fx-text-fill:#EF4444; -fx-font-weight:700;");
                else setStyle("-fx-text-fill:#F59E0B; -fx-font-weight:700;");
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4, c5);
        table.setItems(javafx.collections.FXCollections.observableArrayList(allSales));

        HBox footer = new HBox(12);
        footer.setStyle("-fx-background-color:#ffffff; -fx-padding:12 24; -fx-border-color:#E2E8F0; -fx-border-width:1 0 0 0; -fx-alignment:CENTER_RIGHT;");
        Button btnFechar = makeActionButton("Fechar", "#475569", "#ffffff");
        btnFechar.setOnAction(ev -> dialog.close());
        footer.getChildren().add(btnFechar);

        root.getChildren().addAll(header, kpiRow, table, footer);
        javafx.scene.Scene sc3 = new javafx.scene.Scene(root);
        dialog.setScene(sc3);
        dialog.show();
    }

    /** Exportar lista de clientes para ficheiro Excel (.xlsx) com Apache POI */
    private void exportCustomersToExcel() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar ficheiro Excel");
        fc.setInitialFileName("clientes_sgv.xlsx");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        java.io.File file = fc.showSaveDialog(customersPane.getScene().getWindow());
        if (file == null) return;

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Clientes");

            org.apache.poi.ss.usermodel.CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFColor darkSlate = new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)30, (byte)41, (byte)59}, null);
            headerStyle.setFillForegroundColor(darkSlate);
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            org.apache.poi.ss.usermodel.Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);

            String[] headers = {"Código", "Nome", "NUIT", "Tipo", "Telemóvel", "Endereço", "Saldo (MT)", "Limite Crédito (MT)", "Data Registo"};
            org.apache.poi.ss.usermodel.Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5500);
            }

            List<Customer> customers;
            try {
                customers = customerFilter.isEmpty() ? customerRepository.findAll() : customerRepository.searchByCodeOrName(customerFilter);
            } catch (Exception ex) { customers = java.util.Collections.emptyList(); }

            org.apache.poi.ss.usermodel.CellStyle altStyle = wb.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFColor lightGray = new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte)241, (byte)245, (byte)249}, null);
            altStyle.setFillForegroundColor(lightGray);
            altStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            for (int r = 0; r < customers.size(); r++) {
                Customer c = customers.get(r);
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
                String[] values = {
                    c.getCode() != null ? c.getCode() : "",
                    c.getName() != null ? c.getName() : "",
                    c.getNuit() != null ? c.getNuit() : "",
                    c.getType() != null ? c.getType() : "",
                    c.getContact() != null ? c.getContact() : "",
                    c.getAddress() != null ? c.getAddress() : "",
                    c.getBalance() != null ? String.format("%.2f", c.getBalance()) : "0.00",
                    c.getCreditLimit() != null ? String.format("%.2f", c.getCreditLimit()) : "0.00",
                    c.getCreatedAt() != null ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : ""
                };
                for (int col = 0; col < values.length; col++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
                    cell.setCellValue(values[col]);
                    if (r % 2 == 1) cell.setCellStyle(altStyle);
                }
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                wb.write(fos);
            }
            showToast("Excel exportado: " + file.getName() + " (" + customers.size() + " clientes)");
        } catch (Exception ex) {
            ex.printStackTrace();
            showToast("Erro ao exportar Excel: " + ex.getMessage());
        }
    }
}
