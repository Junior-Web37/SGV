package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.*;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

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
    @FXML private VBox topContainer;

    /** Banner de modo treino — inserido no topo quando demoMode=true */
    private Label trainingBanner;

    @FXML private Button navResumo;
    @FXML private Button shortcutPDVButton;
    @FXML private Button shortcutCotacaoButton;

    @FXML private StackPane navStackPane;
    @FXML private HBox navRootPane;
    @FXML private HBox navSubPane;
    @FXML private Label navSubModuleLabel;
    @FXML private Button navBackButton;

    @FXML private Button navModComercial;
    @FXML private Button navModFinanceiro;
    @FXML private Button navModOperacoes;
    @FXML private Button navModAdministracao;
    @FXML private Button navModArmazens;
    @FXML private Button navModLogs;

    @FXML private HBox navComercialItems;
    @FXML private HBox navFinanceiroItems;
    @FXML private HBox navOperacoesItems;
    @FXML private HBox navAdminItems;
    @FXML private HBox navArmazensItems;

    @FXML private Button navMenuNovaVenda;
    @FXML private Button navMenuVendas;
    @FXML private Button navMenuProdutos;
    @FXML private Button navMenuClientes;
    @FXML private Button navMenuStock;
    @FXML private Button navMenuArmazens;
    @FXML private Button navMenuTransferir;
    @FXML private Button navMenuCatalogos;
    @FXML private Button navMenuCaixa;
    @FXML private Button navMenuFinanceiro;
    @FXML private Button navMenuPagamentos;
    @FXML private Button navMenuDespesas;
    @FXML private Button navMenuCompras;
    @FXML private Button navMenuFornecedores;
    @FXML private Button navMenuProducao;
    @FXML private Button navMenuRelatorios;
    @FXML private Button navMenuSistema;

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

    @FXML private Button btnNovaCategoria;
    @FXML private Button btnEditCategoria;
    @FXML private Button btnDeleteCategoria;
    @FXML private Button btnNovaUnidade;
    @FXML private Button btnEditUnidade;
    @FXML private Button btnDeleteUnidade;
    @FXML private TableView<Category> categoriesTable;
    @FXML private TableView<MetricUnit> unitsTable;

    @FXML private TextField stockSearchField;
    @FXML private Button btnAjustarStock;
    @FXML private TableView<StockBranch> stockTable;

    @FXML private Label salesStatsRevenueTodayLabel;
    @FXML private Label salesStatsCountTodayLabel;
    @FXML private Label salesStatsAvgTicketLabel;
    @FXML private Label salesStatsPendingLabel;
    @FXML private javafx.scene.chart.BarChart<String, Number> salesStatsBarChart;
    @FXML private javafx.scene.chart.PieChart salesStatsPieChart;

    @FXML private Button createSaleButton;
    @FXML private Button createCotacaoButton;
    @FXML private Button invoiceQuoteButton;
    @FXML private Button printSaleButton;
    @FXML private Button printReceiptButton;
    @FXML private Button annulSaleButton;
    @FXML private TableView<Sale> salesTable;

    @FXML private Button createProductButton;
    @FXML private Button editProductButton;
    @FXML private Button deleteProductButton;
    @FXML private TableView<Product> productsTable;

    @FXML private Button createCustomerButton;
    @FXML private Button editCustomerButton;
    @FXML private Button deleteCustomerButton;
    @FXML private TableView<Customer> customersTable;

    @FXML private Button createPurchaseButton;
    @FXML private Button editPurchaseButton;
    @FXML private Button deletePurchaseButton;
    @FXML private TableView<Purchase> purchasesTable;

    @FXML private Button createPaymentButton;
    @FXML private Button deletePaymentButton;
    @FXML private Label finTotalVendasLabel;
    @FXML private Label finTotalRecebidoLabel;
    @FXML private Label finPendenteLabel;
    @FXML private Label finNumPagamentosLabel;
    @FXML private TableView<Payment> paymentsTable;

    @FXML private Button createExpenseButton;
    @FXML private Button editExpenseButton;
    @FXML private Button deleteExpenseButton;
    @FXML private TableView<Expense> expensesTable;

    @FXML private Button createOrderButton;
    @FXML private Button editOrderButton;
    @FXML private Button deleteOrderButton;
    @FXML private Button completeOrderButton;
    @FXML private TableView<ProductionOrder> ordersTable;

    @FXML private Button createUserButton;
    @FXML private Button editUserButton;
    @FXML private Button toggleUserButton;
    @FXML private TableView<User> usersTable;

    @FXML private Button createBranchButton;
    @FXML private Button editBranchButton;
    @FXML private TableView<Branch> branchesTable;

    @FXML private VBox branchInfoBox;

    @FXML private VBox productsFilterPane;
    @FXML private VBox customersFilterPane;

    @FXML private javafx.scene.chart.LineChart<String, Number> salesLineChart;
    @FXML private javafx.scene.chart.PieChart salesPieChart;

    private final DashboardNavigationManager navManager;
    private final DashboardCrudManager crudManager;
    private final DashboardKpiManager kpiManager;
    private final CashSessionService cashSessionService;
    private final SystemLogService systemLogService;
    private final ApplicationContext applicationContext;
    private final TrainingModeService trainingModeService;
    private final AppConfigService appConfigService;

    private User currentUser;
    private VBox productFilterPanel;
    private VBox customerFilterPanel;

    private int currentSalesPage = 0;
    private int currentProductsPage = 0;
    private int currentCustomersPage = 0;
    private String productFilter = "";
    private String customerFilter = "";
    private String saleFilter = "";
    private String saleStateFilter = "TODOS";
    private String saleDocTypeFilter = "TODOS";
    private java.time.LocalDateTime saleStartDate = null;
    private java.time.LocalDateTime saleEndDate = null;

    public DashboardController(DashboardNavigationManager navManager,
                               DashboardCrudManager crudManager,
                               DashboardKpiManager kpiManager,
                               CashSessionService cashSessionService,
                               SystemLogService systemLogService,
                               ApplicationContext applicationContext,
                               TrainingModeService trainingModeService,
                               AppConfigService appConfigService) {
        this.navManager = navManager;
        this.crudManager = crudManager;
        this.kpiManager = kpiManager;
        this.cashSessionService = cashSessionService;
        this.systemLogService = systemLogService;
        this.applicationContext = applicationContext;
        this.trainingModeService = trainingModeService;
        this.appConfigService = appConfigService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean hasPermission(String page, String action) {
        if (currentUser == null || currentUser.getRoles() == null || currentUser.getRoles().isEmpty()) return false;
        boolean isAdmin = currentUser.getRoles().stream()
            .filter(java.util.Objects::nonNull)
            .anyMatch(role -> role.getName() != null && role.getName().toUpperCase(Locale.ROOT).contains("ADMIN"));
        if (isAdmin) return true;
        return currentUser.getRoles().stream()
            .filter(java.util.Objects::nonNull)
            .anyMatch(role -> role.hasPermission(page, action));
    }

    private boolean ensurePermission(String page, String action, String label) {
        if (hasPermission(page, action)) return true;
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

        setVisible(navResumo, hasPermission("RESUMO", "VIEW"));
        setVisible(navMenuNovaVenda, canVendas); setVisible(navMenuVendas, canVendas);
        setVisible(navMenuProdutos, canProdutos); setVisible(navMenuClientes, canClientes);
        setVisible(navMenuStock, canStock); setVisible(navMenuArmazens, canArmazens);
        setVisible(navMenuTransferir, canTransferir); setVisible(navMenuCatalogos, canCatalogos);
        setVisible(navMenuCaixa, canCaixa); setVisible(navMenuFinanceiro, canFinanceiro);
        setVisible(navMenuCompras, canCompras); setVisible(navMenuFornecedores, canCompras);
        setVisible(navMenuDespesas, canFinanceiro);
        setVisible(navMenuPagamentos, canFinanceiro); setVisible(navMenuProducao, canProducao);
        setVisible(navMenuRelatorios, canRelatorios); setVisible(navMenuSistema, canSistema);
        setVisible(navModComercial, canVendas || canProdutos || canClientes);
        setVisible(navModFinanceiro, canCaixa || canFinanceiro || canCompras);
        setVisible(navModOperacoes, canStock || canCaixa || canCompras);
        setVisible(navModArmazens, canArmazens || canTransferir || canCompras);
        setVisible(navModAdministracao, canCatalogos || canRelatorios || canSistema);
        setVisible(navModLogs, canSistema);

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

    private void setVisible(Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        String css = getClass().getResource("/css/dashboard.css").toExternalForm();
        dashboardRoot.getStylesheets().add(css);

        UiUtils.attachSafe(logoutButton, this::doLogout, systemLogService, "LOGOUT");
        UiUtils.attachSafe(navResumo, () -> { if (ensurePermission("RESUMO", "VIEW", "Resumo")) showSummaryPane(); }, systemLogService, "NAV_RESUMO");
        if (shortcutPDVButton != null) UiUtils.attachSafe(shortcutPDVButton, () -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openPDVSaleFormWithType(null, null); }, systemLogService, "SHORTCUT_PDV");
        if (shortcutCotacaoButton != null) UiUtils.attachSafe(shortcutCotacaoButton, () -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openSaleFormWithType(null, "COTACAO"); }, systemLogService, "SHORTCUT_COTACAO");

        if (navMenuNovaVenda != null) UiUtils.attachSafe(navMenuNovaVenda, () -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openPDVSaleFormWithType(null, null); }, systemLogService, "NAV_NOVA_VENDA");
        if (navMenuVendas != null) UiUtils.attachSafe(navMenuVendas, () -> { if (ensurePermission("VENDAS", "VIEW", "Vendas")) showSalesStatsPane(); }, systemLogService, "NAV_VENDAS");
        if (navMenuProdutos != null) UiUtils.attachSafe(navMenuProdutos, () -> { if (ensurePermission("PRODUTOS", "VIEW", "Produtos")) showProductsPane(); }, systemLogService, "NAV_PRODUTOS");
        if (navMenuClientes != null) UiUtils.attachSafe(navMenuClientes, () -> { if (ensurePermission("CLIENTES", "VIEW", "Clientes")) showCustomersPane(); }, systemLogService, "NAV_CLIENTES");
        if (navMenuStock != null) UiUtils.attachSafe(navMenuStock, () -> { if (ensurePermission("STOCK", "VIEW", "Stock")) showStockPane(); }, systemLogService, "NAV_STOCK");
        if (navModArmazens != null) UiUtils.attachSafe(navModArmazens, () -> showSubNav("Armazém", navArmazensItems), systemLogService, "NAV_MOD_ARMAZENS");
        if (navMenuArmazens != null) UiUtils.attachSafe(navMenuArmazens, () -> { if (ensurePermission("ARMAZENS", "VIEW", "Armazéns")) showWarehousesPane(); }, systemLogService, "NAV_ARMAZENS");
        if (navMenuTransferir != null) UiUtils.attachSafe(navMenuTransferir, () -> { if (ensurePermission("TRANSFERENCIAS", "VIEW", "Transferências")) openWarehouseTransferForm(); }, systemLogService, "NAV_TRANSFERIR");
        if (navMenuCatalogos != null) UiUtils.attachSafe(navMenuCatalogos, () -> { if (ensurePermission("CATALOGOS", "VIEW", "Catálogos")) showCatalogsPane(); }, systemLogService, "NAV_CATALOGOS");

        if (navMenuCaixa != null) UiUtils.attachSafe(navMenuCaixa, () -> { if (ensurePermission("CAIXA", "VIEW", "Caixa")) showTurnoCaixaPane(); }, systemLogService, "NAV_CAIXA");
        if (cashStatusBadge != null) cashStatusBadge.setOnMouseClicked(e -> { if (ensurePermission("CAIXA", "VIEW", "Caixa")) showTurnoCaixaPane(); });
        if (navMenuFinanceiro != null) UiUtils.attachSafe(navMenuFinanceiro, () -> { if (ensurePermission("FINANCEIRO", "VIEW", "Financeiro")) showFinanceiroPane(); }, systemLogService, "NAV_FINANCEIRO");
        if (navMenuCompras != null) UiUtils.attachSafe(navMenuCompras, () -> { if (ensurePermission("COMPRAS", "VIEW", "Compras")) showComprasPane(); }, systemLogService, "NAV_COMPRAS");
        if (navMenuFornecedores != null) UiUtils.attachSafe(navMenuFornecedores, () -> { if (ensurePermission("COMPRAS", "VIEW", "Fornecedores")) showFornecedoresPane(); }, systemLogService, "NAV_FORNECEDORES");
        if (navMenuProducao != null) UiUtils.attachSafe(navMenuProducao, () -> { if (ensurePermission("PRODUCAO", "VIEW", "Produção")) showProducaoPane(); }, systemLogService, "NAV_PRODUCAO");
        if (navMenuRelatorios != null) UiUtils.attachSafe(navMenuRelatorios, () -> { if (ensurePermission("RELATORIOS", "VIEW", "Relatórios")) showReportsPane(); }, systemLogService, "NAV_RELATORIOS");
        if (navMenuSistema != null) UiUtils.attachSafe(navMenuSistema, () -> { if (ensurePermission("SISTEMA", "VIEW", "Sistema")) showSistemaPane(); }, systemLogService, "NAV_SISTEMA");
        if (navMenuDespesas != null) UiUtils.attachSafe(navMenuDespesas, this::showFinanceiroPane, systemLogService, "NAV_DESPESAS");
        if (navMenuPagamentos != null) UiUtils.attachSafe(navMenuPagamentos, this::showFinanceiroPane, systemLogService, "NAV_PAGAMENTOS");

        if (navModComercial != null) UiUtils.attachSafe(navModComercial, () -> showSubNav("Comercial", navComercialItems), systemLogService, "NAV_MOD_COMERCIAL");
        if (navModOperacoes != null) UiUtils.attachSafe(navModOperacoes, () -> showSubNav("Operações", navOperacoesItems), systemLogService, "NAV_MOD_OPERACOES");
        if (navModFinanceiro != null) UiUtils.attachSafe(navModFinanceiro, () -> showSubNav("Financeiro", navFinanceiroItems), systemLogService, "NAV_MOD_FINANCEIRO");
        if (navModAdministracao != null) UiUtils.attachSafe(navModAdministracao, () -> showSubNav("Admin", navAdminItems), systemLogService, "NAV_MOD_ADMIN");
        if (navModLogs != null) UiUtils.attachSafe(navModLogs, () -> showLogsPane(), systemLogService, "NAV_MOD_LOGS");
        if (navBackButton != null) UiUtils.attachSafe(navBackButton, () -> navManager.drillBack(navRootPane, navSubPane), systemLogService, "NAV_BACK");

        setupCrudButtons();
        setupFilterPanels();

        if (notificationBellButton != null) notificationBellButton.setOnAction(e -> kpiManager.showNotificationPopup(notificationBellButton));

        navManager.loadReportsPane(reportsPane);
        showSummaryPane();
    }

    private void setupCrudButtons() {
        if (createCustomerButton != null) {
            createCustomerButton.setOnAction(e -> { if (ensurePermission("CLIENTES", "CREATE", "Clientes")) openCustomerForm(null); });
            editCustomerButton.setOnAction(e -> {
                if (!ensurePermission("CLIENTES", "CREATE", "Clientes")) return;
                TableView<Customer> cTbl = navManager.getCustomersTable();
                Customer sel = cTbl != null ? cTbl.getSelectionModel().getSelectedItem() : null;
                if (sel != null) openCustomerForm(sel);
            });
            deleteCustomerButton.setOnAction(e -> {
                if (!ensurePermission("CLIENTES", "DELETE", "Clientes")) return;
                TableView<Customer> cTbl = navManager.getCustomersTable();
                Customer sel = cTbl != null ? cTbl.getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja apagar este cliente?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) crudManager.safeDelete(() -> applicationContext.getBean(CustomerRepository.class).deleteById(sel.getId()), this::loadCustomers, "Cliente", currentUser); });
                }
            });
        }

        if (createSaleButton != null) {
            createSaleButton.setOnAction(e -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openPDVSaleFormWithType(null, null); });
            if (createCotacaoButton != null) createCotacaoButton.setOnAction(e -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) openCotacaoForm(); });
            if (invoiceQuoteButton != null) invoiceQuoteButton.setOnAction(e -> { if (ensurePermission("VENDAS", "CREATE", "Vendas")) invoiceSelectedQuote(); });
            printSaleButton.setOnAction(e -> { if (ensurePermission("VENDAS", "VIEW", "Vendas")) printSelectedSale(); });
            annulSaleButton.setOnAction(e -> { if (ensurePermission("VENDAS", "DELETE", "Vendas")) annulSelectedSale(); });
        }

        if (createPurchaseButton != null) {
            createPurchaseButton.setOnAction(e -> openPurchaseForm(null));
            editPurchaseButton.setOnAction(e -> { if (purchasesTable != null && purchasesTable.getSelectionModel().getSelectedItem() != null) openPurchaseForm(purchasesTable.getSelectionModel().getSelectedItem()); });
            deletePurchaseButton.setOnAction(e -> {
                Purchase sel = purchasesTable != null ? purchasesTable.getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja apagar esta compra?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) crudManager.safeDelete(() -> applicationContext.getBean(PurchaseRepository.class).deleteById(sel.getId()), this::loadPurchases, "Compra", currentUser); });
                }
            });
        }

        if (createExpenseButton != null) {
            createExpenseButton.setOnAction(e -> openExpenseForm(null));
            editExpenseButton.setOnAction(e -> { if (expensesTable != null && expensesTable.getSelectionModel().getSelectedItem() != null) openExpenseForm(expensesTable.getSelectionModel().getSelectedItem()); });
            deleteExpenseButton.setOnAction(e -> {
                Expense sel = expensesTable != null ? expensesTable.getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja apagar esta despesa?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) crudManager.safeDelete(() -> applicationContext.getBean(ExpenseRepository.class).deleteById(sel.getId()), this::loadExpenses, "Despesa", currentUser); });
                }
            });
        }

        if (createPaymentButton != null) {
            createPaymentButton.setOnAction(e -> openPaymentForm(null));
            deletePaymentButton.setOnAction(e -> {
                Payment sel = paymentsTable != null ? paymentsTable.getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja apagar este pagamento?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) crudManager.safeDelete(() -> applicationContext.getBean(PaymentRepository.class).deleteById(sel.getId()), this::loadFinanceiro, "Pagamento", currentUser); });
                }
            });
        }

        if (createUserButton != null) {
            createUserButton.setOnAction(e -> openUserForm(null));
            editUserButton.setOnAction(e -> { if (usersTable != null && usersTable.getSelectionModel().getSelectedItem() != null) openUserForm(usersTable.getSelectionModel().getSelectedItem()); });
            toggleUserButton.setOnAction(e -> {
                User sel = usersTable != null ? usersTable.getSelectionModel().getSelectedItem() : null;
                if (sel != null) { sel.setActive(!sel.isActive()); applicationContext.getBean(UserRepository.class).save(sel); loadSystem(); }
            });
        }

        if (createBranchButton != null) {
            createBranchButton.setOnAction(e -> openBranchForm(null));
            editBranchButton.setOnAction(e -> { if (branchesTable != null && branchesTable.getSelectionModel().getSelectedItem() != null) openBranchForm(branchesTable.getSelectionModel().getSelectedItem()); });
        }

        if (createOrderButton != null) {
            createOrderButton.setOnAction(e -> openOrderForm(null));
            editOrderButton.setOnAction(e -> { if (ordersTable != null && ordersTable.getSelectionModel().getSelectedItem() != null) openOrderForm(ordersTable.getSelectionModel().getSelectedItem()); });
            deleteOrderButton.setOnAction(e -> {
                ProductionOrder sel = ordersTable != null ? ordersTable.getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja apagar esta ordem?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) crudManager.safeDelete(() -> applicationContext.getBean(ProductionOrderRepository.class).deleteById(sel.getId()), this::loadProductionOrders, "Ordem de Produção", currentUser); });
                }
            });
            if (completeOrderButton != null) {
                completeOrderButton.setOnAction(e -> {
                    ProductionOrder sel = ordersTable != null ? ordersTable.getSelectionModel().getSelectedItem() : null;
                    if (sel != null && !"COMPLETED".equals(sel.getState())) {
                        sel.setState("COMPLETED");
                        sel.setCompletedAt(LocalDateTime.now());
                        applicationContext.getBean(ProductionOrderRepository.class).save(sel);
                        loadProductionOrders();
                    }
                });
            }
        }
    }

    private void setupFilterPanels() {
        try {
            if (productsFilterPane != null) {
                productFilterPanel = FilterPanelBuilder.createProductFilterPanel(criteria -> {
                    if (criteria != null && productsTable != null) {
                        if (criteria.category() != null && !criteria.category().isBlank() && !criteria.category().equals("Todos")) {
                            var list = applicationContext.getBean(ProductRepository.class).searchByCodeOrNameAndCategory(
                                    criteria.search() == null ? "" : criteria.search(), criteria.category());
                            TableView<Product> table = navManager.getProductsTable();
                            if (table != null) table.setItems(FXCollections.observableArrayList(list));
                        } else { setProductFilter(criteria.search()); }
                    }
                }, () -> clearProductFilter());
                productsFilterPane.getChildren().add(productFilterPanel);
            }
            if (customersFilterPane != null) {
                customerFilterPanel = FilterPanelBuilder.createCustomerFilterPanel(criteria -> {
                    if (criteria != null) {
                        if (criteria.type() != null && !criteria.type().isBlank() && !criteria.type().equals("Todos")) {
                            var list = applicationContext.getBean(CustomerRepository.class).searchByCodeOrNameAndType(
                                    criteria.search() == null ? "" : criteria.search(), criteria.type());
                            TableView<Customer> cTable = navManager.getCustomersTable();
                            if (cTable != null) cTable.setItems(FXCollections.observableArrayList(list));
                        } else { setCustomerFilter(criteria.search()); }
                    }
                }, () -> clearCustomerFilter());
                customersFilterPane.getChildren().add(customerFilterPanel);
            }
            // Painel de filtros (datas, tipo, estado) é construído no showSalesStatsPane()
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER / PROFILE
    // ═══════════════════════════════════════════════════════════════════════════

    public void setUser(User user) {
        this.currentUser = user;
        loadProfile();
        loadStats();
        loadSales();
        loadProducts();
        loadCustomers();
        applyPermissions();
        updateTrainingBanner();
        systemLogService.logSystem("DASHBOARD_LOADED", "Painel carregado para o utilizador: " + (user != null ? user.getUsername() : "?"));
    }

    private void loadProfile() {
        if (currentUser == null) {
            userNameLabel.setText("Usuário"); userRoleLabel.setText(""); userAvatar.setText("U");
            welcomeText.setText("Bem-vindo ao SGV.");
            return;
        }
        String fullName = currentUser.getFullName() != null && !currentUser.getFullName().isBlank() ? currentUser.getFullName() : currentUser.getUsername();
        userNameLabel.setText(fullName);
        String role = currentUser.getRoles() != null && !currentUser.getRoles().isEmpty() ? currentUser.getRoles().iterator().next().getName() : "";
        userRoleLabel.setText(role);
        userAvatar.setText(fullName.isEmpty() ? "U" : fullName.substring(0, 1).toUpperCase());
        if (welcomeText != null) welcomeText.setText("Bem-vindo ao SGV, " + fullName + ".");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TRAINING MODE BANNER
    // ═══════════════════════════════════════════════════════════════════════════

    private void updateTrainingBanner() {
        boolean active = trainingModeService.isTrainingMode();
        if (active && trainingBanner == null) {
            trainingBanner = new Label("⚠  MODO TREINAMENTO ACTIVO — Todas as operações são simuladas e não alteram dados reais.");
            trainingBanner.setStyle(
                "-fx-background-color:#F59E0B;" +
                "-fx-text-fill:#78350F;" +
                "-fx-font-weight:800;" +
                "-fx-font-size:12px;" +
                "-fx-padding:8 20;" +
                "-fx-alignment:CENTER;");
            if (topContainer != null) {
                topContainer.getChildren().add(0, trainingBanner);
            }
        } else if (!active && trainingBanner != null) {
            if (topContainer != null) {
                topContainer.getChildren().remove(trainingBanner);
            }
            trainingBanner = null;
        }
    }

    public boolean isTrainingMode() {
        return trainingModeService.isTrainingMode();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW PANE DELEGATES
    // ═══════════════════════════════════════════════════════════════════════════

    private VBox[] allPanes() {
        return new VBox[]{summaryPane, salesStatsPane, salesPane, productsPane, customersPane, stockPane,
            turnoCaixaPane, comprasPane, financeiroPane, catalogsPane, reportsPane, producaoPane,
            sistemaPane, usersPane, warehousesPane, logsPane};
    }

    private Button[] allNavButtons() {
        return new Button[]{navResumo, navMenuNovaVenda, navMenuVendas, navMenuProdutos, navMenuClientes,
            navMenuStock, navMenuArmazens, navMenuTransferir, navMenuFornecedores, navMenuCatalogos,
            navMenuCaixa, navMenuCompras, navMenuPagamentos, navMenuDespesas, navMenuRelatorios,
            navMenuSistema};
    }

    private void showSubNav(String moduleLabel, HBox targetItems) {
        navManager.showSubNav(moduleLabel, targetItems, navRootPane, navSubPane, navSubModuleLabel,
            navComercialItems, navOperacoesItems, navFinanceiroItems, navAdminItems, navArmazensItems);
    }

    private void showSummaryPane() {
        navManager.setActiveNav(navResumo, allNavButtons());
        navManager.setPaneVisibility(summaryPane, allPanes());
        pageTitleLabel.setText("Dashboard");
        pageSubtitleLabel.setText("Visão geral do sistema");
        kpiManager.loadStats(salesLabel, productsLabel, customersLabel, branchesLabel, salesLineChart, salesPieChart,
            () -> kpiManager.checkAlerts(notificationBadge), this::animateEntrance);
        updateCashBadge();
    }

    private void showSalesStatsPane() {
        navManager.showSalesStatsPane(navMenuVendas, pageTitleLabel, pageSubtitleLabel,
            salesStatsPane, allPanes()[0], salesPane, allPanes()[3], allPanes()[4], allPanes()[5],
            allPanes()[6], allPanes()[7], allPanes()[8], allPanes()[9], allPanes()[10], allPanes()[11],
            allPanes()[12], allPanes()[13], allPanes()[14], allPanes()[15], allNavButtons(),
            salesTable, currentUser,
            criteria -> {
                if (criteria != null) {
                    saleFilter = criteria.search() == null ? "" : criteria.search();
                    saleStartDate = criteria.startDate() != null ? criteria.startDate().atStartOfDay() : null;
                    saleEndDate = criteria.endDate() != null ? criteria.endDate().atTime(23, 59, 59) : null;
                    saleStateFilter = criteria.state() != null && !criteria.state().isBlank() && !"Todos".equals(criteria.state())
                            ? criteria.state() : "TODOS";
                    saleDocTypeFilter = criteria.documentType() != null && !criteria.documentType().isBlank() && !"Todos".equals(criteria.documentType())
                            ? criteria.documentType() : "TODOS";
                    currentSalesPage = 0;
                    loadSales();
                }
            },
            this::clearSaleFilter,
            this::loadSales,
            () -> kpiManager.loadSalesStats(salesStatsRevenueTodayLabel, salesStatsCountTodayLabel, salesStatsAvgTicketLabel, salesStatsPendingLabel, salesStatsBarChart, salesStatsPieChart),
            () -> kpiManager.loadStats(salesLabel, productsLabel, customersLabel, branchesLabel, salesLineChart, salesPieChart, null, null),
            this::updateCashBadge,
            () -> openSaleFormWithType(null, null),
            this::printSelectedSale,
            this::viewSelectedSale,
            this::invoiceSelectedQuote,
            this::annulSelectedSale);
    }

    private void showCustomersPane() {
        navManager.showCustomersPane(navMenuClientes, pageTitleLabel, pageSubtitleLabel,
            customersPane, currentUser, allPanes(), allNavButtons(), customersTable,
            this::setCustomerFilter,
            this::loadCustomers, this::updateCashBadge,
            () -> openCustomerForm(null),
            () -> {
                Customer sel = navManager.getCustomersTable() != null ? navManager.getCustomersTable().getSelectionModel().getSelectedItem() : null;
                if (sel != null) {
                    openCustomerForm(sel);
                } else {
                    Alert a = new Alert(Alert.AlertType.WARNING, "Selecione um cliente para editar.");
                    a.setHeaderText(null);
                    a.showAndWait();
                }
            },
            () -> crudManager.deleteSelectedCustomer(navManager.getCustomersTable(), this::loadCustomers, currentUser),
            () -> crudManager.viewSelectedCustomer(navManager.getCustomersTable(), getOwner()));
    }

    private void showWarehousesPane() {
        Runnable[] loadCards = new Runnable[1];
        loadCards[0] = () -> crudManager.loadWarehousesCards(warehousesPane, getOwner(), currentUser, loadCards[0]);
        navManager.showWarehousesPane(navMenuArmazens, pageTitleLabel, pageSubtitleLabel,
            warehousesPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge, loadCards[0],
            () -> crudManager.openWarehouseForm(null, getOwner(), loadCards[0]),
            () -> {
                Warehouse sel = crudManager.getSelectedWarehouse(warehousesPane);
                if (sel != null) crudManager.openWarehouseForm(sel, getOwner(), loadCards[0]);
                else { Alert a = new Alert(Alert.AlertType.WARNING, "Selecione um armazém (clique nele)."); a.setHeaderText(null); a.showAndWait(); }
            },
            () -> {
                Warehouse sel = crudManager.getSelectedWarehouse(warehousesPane);
                if (sel != null) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Eliminar armazém \"" + sel.getName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.setHeaderText(null);
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            crudManager.safeDelete(() -> applicationContext.getBean(WarehouseRepository.class).deleteById(sel.getId()), () -> { warehousesPane.getChildren().clear(); loadCards[0].run(); }, "Armazém", currentUser);
                        }
                    });
                } else { Alert a = new Alert(Alert.AlertType.WARNING, "Selecione um armazém (clique nele)."); a.setHeaderText(null); a.showAndWait(); }
            });
    }

    private void showLogsPane() {
        navManager.showLogsPane(pageTitleLabel, pageSubtitleLabel, logsPane, currentUser, allPanes(), allNavButtons());
    }

    private void showComprasPane() {
        navManager.showComprasPane(navMenuCompras, pageTitleLabel, pageSubtitleLabel,
            comprasPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showFornecedoresPane() {
        navManager.showComprasPane(navMenuFornecedores, pageTitleLabel, pageSubtitleLabel,
            comprasPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showProducaoPane() {
        navManager.showProducaoPane(navMenuProducao, pageTitleLabel, pageSubtitleLabel,
            producaoPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showReportsPane() {
        navManager.showReportsPane(navMenuRelatorios, pageTitleLabel, pageSubtitleLabel,
            reportsPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showSistemaPane() {
        navManager.showSistemaPane(navMenuSistema, pageTitleLabel, pageSubtitleLabel,
            sistemaPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showCatalogsPane() {
        navManager.showCatalogsPane(navMenuCatalogos, pageTitleLabel, pageSubtitleLabel,
            catalogsPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge,
            this::loadCategories, this::loadMetricUnits, categoriesTable, unitsTable);
    }

    private void showStockPane() {
        navManager.showStockPane(pageTitleLabel, pageSubtitleLabel, stockPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showFinanceiroPane() {
        navManager.showFinanceiroPane(pageTitleLabel, pageSubtitleLabel, financeiroPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showUsersPane() {
        navManager.showUsersPane(pageTitleLabel, pageSubtitleLabel, usersPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showTurnoCaixaPane() {
        navManager.showTurnoCaixaPane(pageTitleLabel, pageSubtitleLabel, turnoCaixaPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge);
    }

    private void showProductsPane() {
        navManager.showProductsPane(pageTitleLabel, pageSubtitleLabel, productsPane, currentUser, allPanes(), allNavButtons(), this::updateCashBadge, this::loadProducts,
            product -> { if (ensurePermission("PRODUTOS", "CREATE", "Produtos")) openProductForm(product); },
            product -> { if (ensurePermission("PRODUTOS", "DELETE", "Produtos")) deleteProduct(product); },
            product -> { if (product != null) crudManager.viewSelectedProduct(navManager.getProductsTable(), getOwner()); },
            this::setProductFilter);
    }

    private void deleteProduct(Product product) {
        if (product == null) return;
        long saleCount = applicationContext.getBean(SaleItemRepository.class).countByProductId(product.getId());
        if (saleCount > 0) {
            Alert warn = new Alert(Alert.AlertType.WARNING,
                "Não é possível apagar este produto.\n\nO produto possui " + saleCount + " registro(s) em vendas. " +
                "Em vez disso, desative o produto na edição.");
            warn.setTitle("Produto não pode ser apagado");
            warn.setHeaderText(null);
            warn.showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Tem certeza que deseja apagar este produto?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(res -> { if (res == ButtonType.OK) crudManager.safeDelete(() -> applicationContext.getBean(ProductRepository.class).deleteById(product.getId()), this::loadProducts, "Produto", currentUser); });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOAD DELEGATES
    // ═══════════════════════════════════════════════════════════════════════════

    private void loadStats() {
        kpiManager.loadStats(salesLabel, productsLabel, customersLabel, branchesLabel, salesLineChart, salesPieChart,
            () -> kpiManager.checkAlerts(notificationBadge), this::animateEntrance);
    }

    private void loadSales() { crudManager.loadSales(navManager.getSalesTable(), saleFilter, saleStateFilter, saleDocTypeFilter, saleStartDate, saleEndDate); }
    private void loadProducts() { crudManager.loadProducts(navManager.getProductsTable(), productFilter, currentProductsPage); }
    private void loadCustomers() { crudManager.loadCustomers(navManager.getCustomersTable(), customerFilter, currentCustomersPage); }
    private void loadStock() { crudManager.loadStock(stockTable, stockSearchField); }
    private void loadPurchases() { crudManager.loadPurchases(purchasesTable); }
    private void loadExpenses() { crudManager.loadExpenses(expensesTable); }
    private void loadProductionOrders() { crudManager.loadProductionOrders(ordersTable); }
    private void loadCategories() { crudManager.loadCategories(navManager.getCategoriesTable()); }
    private void loadMetricUnits() { crudManager.loadMetricUnits(navManager.getUnitsTable()); }
    private void loadSystem() { crudManager.loadSystem(usersTable, branchesTable, branchInfoBox, currentUser); }
    private void loadFinanceiro() { crudManager.loadFinanceiro(paymentsTable, finTotalVendasLabel, finTotalRecebidoLabel, finPendenteLabel, finNumPagamentosLabel, this::loadExpenses); }

    // ═══════════════════════════════════════════════════════════════════════════
    // CRUD FORM DELEGATES
    // ═══════════════════════════════════════════════════════════════════════════

    private Window getOwner() { return contentPane != null ? contentPane.getScene().getWindow() : null; }

    private void openProductForm(Product product) {
        crudManager.openProductForm(product, getOwner(), currentUser, this::loadProducts);
    }
    private void openCustomerForm(Customer customer) {
        crudManager.openCustomerForm(customer, getOwner(), currentUser, this::loadCustomers);
    }
    private void openPurchaseForm(Purchase purchase) {
        crudManager.openPurchaseForm(purchase, getOwner(), currentUser, this::loadPurchases);
    }
    private void openExpenseForm(Expense expense) {
        crudManager.openExpenseForm(expense, getOwner(), currentUser, this::loadExpenses);
    }
    private void openPaymentForm(Payment payment) {
        crudManager.openPaymentForm(payment, getOwner(), this::loadFinanceiro);
    }
    private void openUserForm(User user) {
        crudManager.openUserForm(user, getOwner(), currentUser, this::loadSystem);
    }
    private void openBranchForm(Branch branch) {
        crudManager.openBranchForm(branch, getOwner(), this::loadSystem);
    }
    private void openOrderForm(ProductionOrder order) {
        crudManager.openOrderForm(order, getOwner(), currentUser, this::loadProductionOrders);
    }
    private void openSaleFormWithType(Sale sale, String forceType) {
        crudManager.openSaleFormWithType(sale, forceType, getOwner(), currentUser, () -> { loadSales(); loadStats(); });
    }
    private void openPDVSaleFormWithType(Sale sale, String forceType) {
        crudManager.openPDVSaleFormWithType(sale, forceType, getOwner(), currentUser, () -> { loadSales(); loadStats(); });
    }
    private void openCotacaoForm() { crudManager.openCotacaoForm(getOwner(), currentUser, () -> { loadSales(); loadStats(); }); }
    private void openWarehouseTransferForm() { crudManager.openWarehouseTransferForm(getOwner(), currentUser); }
    private void printSelectedSale() { crudManager.printSelectedSale(navManager.getSalesTable(), getOwner()); }
    private void annulSelectedSale() { crudManager.annulSelectedSale(navManager.getSalesTable(), currentUser, () -> { loadSales(); loadStats(); }); }
    private void invoiceSelectedQuote() { crudManager.invoiceSelectedQuote(navManager.getSalesTable(), getOwner(), currentUser, () -> { loadSales(); loadStats(); }); }
    private void viewSelectedSale() { crudManager.viewSelectedSale(navManager.getSalesTable(), getOwner()); }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    public void setProductFilter(String filter) { productFilter = filter != null ? filter : ""; currentProductsPage = 0; loadProducts(); }
    public void clearProductFilter() { productFilter = ""; currentProductsPage = 0; loadProducts(); }
    public void setCustomerFilter(String filter) { customerFilter = filter != null ? filter : ""; currentCustomersPage = 0; loadCustomers(); }
    public void clearCustomerFilter() { customerFilter = ""; currentCustomersPage = 0; loadCustomers(); }
    public void setSaleFilter(String filter) { saleFilter = filter != null ? filter : ""; currentSalesPage = 0; loadSales(); }
    public void setSaleStateFilter(String state) { saleStateFilter = state != null ? state : "TODOS"; currentSalesPage = 0; loadSales(); }
    public void setSaleDocTypeFilter(String docType) { saleDocTypeFilter = docType != null ? docType : "TODOS"; currentSalesPage = 0; loadSales(); }
    public void clearSaleFilter() { saleFilter = ""; saleStateFilter = "TODOS"; saleDocTypeFilter = "TODOS"; saleStartDate = null; saleEndDate = null; currentSalesPage = 0; loadSales(); }

    // ═══════════════════════════════════════════════════════════════════════════
    // CASH / COMPANY / UTILITY
    // ═══════════════════════════════════════════════════════════════════════════

    private void updateCashBadge() { kpiManager.updateCashBadge(cashStatusBadge, currentUser); }

    private void animateEntrance() {
        FadeTransition ft1 = new FadeTransition(Duration.millis(450), salesLabel); ft1.setFromValue(0.0); ft1.setToValue(1.0); ft1.setDelay(Duration.millis(100));
        FadeTransition ft2 = new FadeTransition(Duration.millis(450), productsLabel); ft2.setFromValue(0.0); ft2.setToValue(1.0); ft2.setDelay(Duration.millis(200));
        FadeTransition ft3 = new FadeTransition(Duration.millis(450), customersLabel); ft3.setFromValue(0.0); ft3.setToValue(1.0); ft3.setDelay(Duration.millis(300));
        FadeTransition ft4 = new FadeTransition(Duration.millis(450), branchesLabel); ft4.setFromValue(0.0); ft4.setToValue(1.0); ft4.setDelay(Duration.millis(400));
        ft1.play(); ft2.play(); ft3.play(); ft4.play();
    }

    private void doLogout() {
        if (currentUser != null) systemLogService.logUserAction(currentUser.getUsername(), "LOGOUT", "Sessão encerrada pelo utilizador.");
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
    }

    private void showToast(String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Info"); alert.setHeaderText(null); alert.setContentText(message); alert.show();
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(2.5));
            delay.setOnFinished(ev -> alert.close()); delay.play();
        } catch (Exception ignored) {
            log.debug("Não foi possível mostrar toast: {}", ignored.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null); alert.showAndWait();
    }
}
// Forçar recompilação
