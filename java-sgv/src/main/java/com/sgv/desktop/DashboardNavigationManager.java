package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.*;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class DashboardNavigationManager {

    private static final Logger log = LoggerFactory.getLogger(DashboardNavigationManager.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ApplicationContext applicationContext;
    private final DashboardCrudManager crudManager;
    private final DashboardKpiManager kpiManager;
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final StockBranchService stockBranchService;
    private final CategoryRepository categoryRepository;
    private final PaymentRepository paymentRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final StockWarehouseRepository stockWarehouseRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SystemLogService systemLogService;
    private final CashSessionService cashSessionService;
    private final WarehouseTransferRepository warehouseTransferRepository;
    private final WarehouseTransferService warehouseTransferService;

    private TableView<Product> productsTable;
    private HBox productsStatsCardsBox;
    private TableView<Sale> salesTable;
    private TableView<Customer> customersTable;
    private TableView<Category> categoriesTable;
    private TableView<MetricUnit> unitsTable;
    private TableView<Supplier> suppliersTable;
    private Runnable suppliersLoadRunnable;
    private Runnable suppliersPaymentsLoadRunnable;
    private Map<Long, BigDecimal> supplierBalances = new HashMap<>();

    private TableView<Purchase> purchasesTable;
    private Runnable purchasesLoadRunnable;

    public TableView<Product> getProductsTable() { return productsTable; }
    public TableView<Sale> getSalesTable() { return salesTable; }
    public TableView<Customer> getCustomersTable() { return customersTable; }
    public TableView<Category> getCategoriesTable() { return categoriesTable; }
    public TableView<MetricUnit> getUnitsTable() { return unitsTable; }
    public TableView<Supplier> getSuppliersTable() { return suppliersTable; }
    public TableView<Purchase> getPurchasesTable() { return purchasesTable; }
    public void reloadPurchases() { if (purchasesLoadRunnable != null) purchasesLoadRunnable.run(); }

    private String fmtMt(BigDecimal v) {
        return Formatters.moneyMT(v);
    }

    public void reloadSuppliers() {
        if (suppliersLoadRunnable != null) suppliersLoadRunnable.run();
        if (suppliersPaymentsLoadRunnable != null) suppliersPaymentsLoadRunnable.run();
    }

    public DashboardNavigationManager(ApplicationContext applicationContext,
                                       DashboardCrudManager crudManager,
                                       DashboardKpiManager kpiManager,
                                       SaleRepository saleRepository,
                                       ProductRepository productRepository,
                                       CustomerRepository customerRepository,
                                       BranchRepository branchRepository,
                                       StockBranchService stockBranchService,
                                       CategoryRepository categoryRepository,
                                       PaymentRepository paymentRepository,
                                       ProductionOrderRepository productionOrderRepository,
                                       UserRepository userRepository,
                                       PurchaseRepository purchaseRepository,
                                       ExpenseRepository expenseRepository,
                                       MetricUnitRepository metricUnitRepository,
                                       StockWarehouseRepository stockWarehouseRepository,
                                       WarehouseRepository warehouseRepository,
                                       SupplierRepository supplierRepository,
                                       SupplierPaymentRepository supplierPaymentRepository,
                                       SystemLogService systemLogService,
                                       CashSessionService cashSessionService,
                                       WarehouseTransferRepository warehouseTransferRepository,
                                       WarehouseTransferService warehouseTransferService) {
        this.applicationContext = applicationContext;
        this.crudManager = crudManager;
        this.kpiManager = kpiManager;
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.stockBranchService = stockBranchService;
        this.categoryRepository = categoryRepository;
        this.paymentRepository = paymentRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.expenseRepository = expenseRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.stockWarehouseRepository = stockWarehouseRepository;
        this.warehouseRepository = warehouseRepository;
        this.supplierRepository = supplierRepository;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.systemLogService = systemLogService;
        this.cashSessionService = cashSessionService;
        this.warehouseTransferRepository = warehouseTransferRepository;
        this.warehouseTransferService = warehouseTransferService;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NAVIGATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    public void setActiveNav(Button btn, Button... allButtons) {
        for (Button b : allButtons) {
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

    public void setPaneVisibility(VBox activePane, VBox... allPanes) {
        for (VBox v : allPanes) {
            if (v != null) {
                v.setVisible(false);
                v.setManaged(false);
            }
        }
        if (activePane != null) {
            activePane.setVisible(true);
            activePane.setManaged(true);
        }
    }

    public void showSubNav(String moduleLabel, HBox targetItems,
                           HBox navRootPane, HBox navSubPane, Label navSubModuleLabel,
                           HBox navComercialItems, HBox navOperacoesItems,
                           HBox navFinanceiroItems, HBox navAdminItems, HBox navArmazensItems,
                           HBox navRelatoriosItems) {
        if (navRootPane == null || navSubPane == null) return;
        navSubModuleLabel.setText(moduleLabel);

        if (navComercialItems != null) { navComercialItems.setVisible(false); navComercialItems.setManaged(false); }
        if (navOperacoesItems != null) { navOperacoesItems.setVisible(false); navOperacoesItems.setManaged(false); }
        if (navFinanceiroItems != null) { navFinanceiroItems.setVisible(false); navFinanceiroItems.setManaged(false); }
        if (navAdminItems != null) { navAdminItems.setVisible(false); navAdminItems.setManaged(false); }
        if (navArmazensItems != null) { navArmazensItems.setVisible(false); navArmazensItems.setManaged(false); }
        if (navRelatoriosItems != null) { navRelatoriosItems.setVisible(false); navRelatoriosItems.setManaged(false); }

        if (targetItems != null) { targetItems.setVisible(true); targetItems.setManaged(true); }

        navRootPane.setVisible(false);
        navRootPane.setManaged(false);
        navSubPane.setVisible(true);
        navSubPane.setManaged(true);
    }

    public void drillBack(HBox navRootPane, HBox navSubPane) {
        if (navRootPane == null || navSubPane == null) return;

        FadeTransition ft = new FadeTransition(Duration.millis(180), navRootPane);
        navSubPane.setVisible(false);
        navSubPane.setManaged(false);
        navRootPane.setVisible(true);
        navRootPane.setManaged(true);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public void loadReportsPane(VBox reportsPane) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reports.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Node reportsContent = loader.load();
            if (reportsPane != null) {
                reportsPane.getChildren().add(reportsContent);
                VBox.setVgrow(reportsContent, Priority.ALWAYS);
            }
        } catch (Exception ex) {
            log.error("Falha ao carregar painel de relatórios: {}", ex.getMessage(), ex);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UI FACTORY HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    public Button makeActionButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-padding: 7 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-cursor: hand; -fx-min-width: 70;");
        return btn;
    }

    public Button makeIconButton(String icon, String bg, String fg) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-padding: 7 10; -fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: 700; -fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-cursor: hand;");
        return btn;
    }

    public <T> Callback<TableView<T>, TableRow<T>> makeTableRowFactory() {
        return makeTableRowFactory(null);
    }

    public <T> Callback<TableView<T>, TableRow<T>> makeTableRowFactory(Runnable onDoubleClick) {
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
            if (onDoubleClick != null) {
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        onDoubleClick.run();
                    }
                });
            }
            return row;
        };
    }

    public Callback<TableView<Sale>, TableRow<Sale>> makeSalesRowFactory(Runnable onDoubleClick) {
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
                    onDoubleClick.run();
                }
            });
            return row;
        };
    }

    public <T> Callback<TableColumn<T, String>, TableCell<T, String>> coloredStateCell() {
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
                    } else if (upper.contains("COMPLETED") || upper.contains("RECEIVED") || upper.contains("ACTIVE") || upper.equals("ACTIVO") || upper.equals("CONCLÍDA")) {
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

    public HBox buildCrudToolbar(TableView<?> table, String entityName,
                                  Runnable onNew, Runnable onEdit, Runnable onDelete, Runnable onShowToast) {
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
                onShowToast.run();
                return;
            }
            onEdit.run();
        });

        Button deleteBtn = makeActionButton("Eliminar", "#EF4444", "#ffffff");
        deleteBtn.setOnAction(e -> {
            if (table.getSelectionModel().getSelectedItem() == null) {
                onShowToast.run();
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

    // ═══════════════════════════════════════════════════════════════════════════
    // KPI GRID / CARD HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    public GridPane buildKPIGrid(String[] titles, String[] subtitles, String[] colors) {
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

    public VBox buildKPICard(String title, String value, String subtitle, String colorType) {
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
        row.setAlignment(Pos.CENTER_LEFT);

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

    // ═══════════════════════════════════════════════════════════════════════════
    // SHOW PANE METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public void showSummaryPane(Label pageTitleLabel, Label pageSubtitleLabel,
                                 VBox summaryPane, VBox salesStatsPane, VBox salesPane,
                                 VBox productsPane, VBox customersPane, VBox stockPane,
                                 VBox turnoCaixaPane, VBox comprasPane, VBox financeiroPane,
                                 VBox catalogsPane, VBox reportsPane, VBox producaoPane,
                                 VBox sistemaPane, VBox usersPane, VBox warehousesPane, VBox logsPane,
                                 User currentUser) {
        pageTitleLabel.setText("Painel Geral");
        pageSubtitleLabel.setText("Visão geral das operações, vendas e tesouraria");
        setPaneVisibility(summaryPane, salesStatsPane, salesPane, productsPane, customersPane,
            stockPane, turnoCaixaPane, comprasPane, financeiroPane, catalogsPane, reportsPane,
            producaoPane, sistemaPane, usersPane, warehousesPane, logsPane);
    }

    public void showSalesStatsPane(Button navMenuVendas, Label pageTitleLabel, Label pageSubtitleLabel,
                                    VBox salesStatsPane, VBox summaryPane, VBox salesPane,
                                    VBox productsPane, VBox customersPane, VBox stockPane,
                                    VBox turnoCaixaPane, VBox comprasPane, VBox financeiroPane,
                                    VBox catalogsPane, VBox reportsPane, VBox producaoPane,
                                    VBox sistemaPane, VBox usersPane, VBox warehousesPane, VBox logsPane,
                                    Button[] allNavButtons,
                                    TableView<Sale> salesTable, User currentUser,
                                    Consumer<FilterPanelBuilder.SalesFilterCriteria> onAdvancedFilter,
                                    Runnable onAdvancedFilterClear,
                                    Runnable loadSales, Runnable loadSalesStats, Runnable loadStats,
                                    Runnable updateCashBadge,
                                    Runnable onNewSale, Runnable onPrint,
                                    Runnable onViewDetails, Runnable onConvertQuote, Runnable onCreateCreditNote, Runnable onAnnul) {
        setActiveNav(null, allNavButtons);
        pageTitleLabel.setText("Vendas & Facturação");
        pageSubtitleLabel.setText("Consulta, emissão e anulação de Facturas e Vendas a Dinheiro (VD)");
        setPaneVisibility(salesStatsPane, summaryPane, salesPane, productsPane, customersPane,
            stockPane, turnoCaixaPane, comprasPane, financeiroPane, catalogsPane, reportsPane,
            producaoPane, sistemaPane, usersPane, warehousesPane, logsPane);

        if (salesStatsPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button newBtn = makeActionButton("+ Nova Venda", "#2563EB", "#ffffff");
            Button printBtn = makeActionButton("Imprimir", "#475569", "#ffffff");
            Button viewBtn = makeActionButton("Ver Detalhes", "#10B981", "#ffffff");
            Button convertBtn = makeActionButton("Faturar Cotação", "#F59E0B", "#ffffff");
            Button creditNoteBtn = makeActionButton("Nota Crédito", "#F59E0B", "#ffffff");
            Button annulBtn = makeActionButton("Anular", "#EF4444", "#ffffff");
            Button refreshBtn = makeIconButton("↻", "#10B981", "#ffffff");

            for (Button b : List.of(newBtn, printBtn, viewBtn, convertBtn, creditNoteBtn, annulBtn, refreshBtn)) {
                UiUtils.applyHoverElevation(b);
                UiUtils.applyPressFeedback(b);
            }

            UiUtils.attachSafe(newBtn, onNewSale, systemLogService, "SALE_NEW");
            UiUtils.attachSafe(printBtn, onPrint, systemLogService, "SALE_PRINT");
            UiUtils.attachSafe(viewBtn, onViewDetails, systemLogService, "SALE_VIEW");
            UiUtils.attachSafe(convertBtn, onConvertQuote, systemLogService, "SALE_CONVERT");
            UiUtils.attachSafe(creditNoteBtn, onCreateCreditNote, systemLogService, "SALE_CREDIT_NOTE");
            UiUtils.attachSafe(annulBtn, onAnnul, systemLogService, "SALE_ANNUL");
            UiUtils.attachSafe(refreshBtn, loadSales, systemLogService, "SALE_REFRESH");

            toolbar.getChildren().addAll(
                newBtn, printBtn, viewBtn, convertBtn, creditNoteBtn, annulBtn, refreshBtn
            );
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            toolbar.getChildren().add(0, spacer);

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Vendas Hoje", "Vendas Semana", "Vendas Mês", "Ticket Médio"},
                new String[]{"Hoje", "Últimos 7 dias", "Este mês", "Por venda"},
                new String[]{"blue", "green", "purple", "orange"}
            );
            kpiGrid.setId("salesKPI");

            TableView<Sale> table = new TableView<>();
            this.salesTable = table;
            table.setStyle(
                "-fx-font-size: 13px; -fx-background-color: #ffffff; " +
                "-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1;"
            );
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            table.setPlaceholder(new Label("Nenhuma venda encontrada"));

            TableColumn<Sale, String> docCol = new TableColumn<>("Documento");
            docCol.setPrefWidth(160);
            docCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDocumentType() + " #" +
                (d.getValue().getDocumentNumber() != null ? d.getValue().getDocumentNumber() : "—")
            ));

            TableColumn<Sale, String> seriesCol = new TableColumn<>("Série");
            seriesCol.setPrefWidth(80);
            seriesCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSeries() != null ? d.getValue().getSeries() : "—"
            ));

            TableColumn<Sale, String> custCol = new TableColumn<>("Cliente");
            custCol.setPrefWidth(220);
            custCol.setCellValueFactory(d -> {
                Sale s = d.getValue();
                String name = s.getCustomer() != null ? s.getCustomer().getName()
                    : (s.getCustomerName() != null ? s.getCustomerName() : "Consumidor Final");
                return new SimpleStringProperty(name);
            });

            TableColumn<Sale, String> totalCol = new TableColumn<>("Total");
            totalCol.setPrefWidth(140);
            totalCol.setCellValueFactory(d -> {
                Double t = d.getValue().getTotal();
                return new SimpleStringProperty(t != null ? String.format("%.2f MT", t) : "0.00 MT");
            });
            totalCol.setCellFactory(col -> {
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
                return cell;
            });

            TableColumn<Sale, String> stateCol = new TableColumn<>("Estado");
            stateCol.setPrefWidth(140);
            stateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getState() != null ? d.getValue().getState() : "—"
            ));
            stateCol.setCellFactory(coloredStateCell());

            TableColumn<Sale, String> dateCol = new TableColumn<>("Data");
            dateCol.setPrefWidth(160);
            dateCol.setCellValueFactory(d -> {
                LocalDateTime dt = d.getValue().getCreatedAt();
                return new SimpleStringProperty(dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
            });

            TableColumn<Sale, String> payCol = new TableColumn<>("Pagamento");
            payCol.setPrefWidth(140);
            payCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPaymentMethod() != null ? d.getValue().getPaymentMethod() : "—"
            ));

            table.getColumns().addAll(List.of(docCol, seriesCol, custCol, totalCol, stateCol, dateCol, payCol));

            HBox filterRow = new HBox(10);
            filterRow.setStyle("-fx-padding: 10 16; -fx-background-color: #f9f9f9; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            TextField filterSearch = new TextField();
            filterSearch.setPromptText("Pesquisar...");
            filterSearch.setPrefWidth(220);
            filterSearch.setStyle("-fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 4; -fx-border-color: #d0d0d0; -fx-border-radius: 4;");

            DatePicker filterDate = new DatePicker();
            filterDate.setPromptText("Data");
            filterDate.setPrefWidth(140);
            filterDate.setStyle("-fx-font-size: 12px;");

            ComboBox<String> filterDocType = new ComboBox<>();
            filterDocType.getItems().addAll("Todos", "VENDA", "FACTURA", "RECIBO", "COTACAO", "ENCOMENDA", "NC", "ND");
            filterDocType.setValue("Todos");
            filterDocType.setPromptText("Tipo");
            filterDocType.setPrefWidth(110);
            filterDocType.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");

            ComboBox<String> filterState = new ComboBox<>();
            filterState.getItems().addAll("Todos", "EMITIDA", "PAGO", "ANULADA", "COTACAO_ABERTA", "COTACAO_PAGA", "ENCOMENDA_ABERTA");
            filterState.setValue("Todos");
            filterState.setPromptText("Estado");
            filterState.setPrefWidth(120);
            filterState.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");

            Runnable fireFilter = () -> {
                if (onAdvancedFilter != null) onAdvancedFilter.accept(
                    new FilterPanelBuilder.SalesFilterCriteria(
                        filterSearch.getText(), filterDate.getValue(), filterDate.getValue(),
                        filterState.getValue(), filterDocType.getValue()));
            };

            UiUtils.setupDebounce(filterSearch, fireFilter, 300);
            filterDate.valueProperty().addListener((obs, old, val) -> fireFilter.run());
            filterDocType.valueProperty().addListener((obs, old, val) -> fireFilter.run());
            filterState.valueProperty().addListener((obs, old, val) -> fireFilter.run());

            Button filterClearBtn = new Button("Limpar");
            filterClearBtn.setStyle("-fx-padding: 6 16; -fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12px;");
            filterClearBtn.setOnAction(e -> {
                filterSearch.clear();
                filterDate.setValue(null);
                filterDocType.setValue(null);
                filterState.setValue(null);
                if (onAdvancedFilterClear != null) onAdvancedFilterClear.run();
            });

            filterRow.getChildren().addAll(filterSearch, filterDate, filterDocType, filterState, filterClearBtn);

            main.getChildren().addAll(kpiGrid, toolbar, filterRow, table);
            salesStatsPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) salesStatsPane.lookup("#salesKPI");
        kpiManager.updateSalesKPIs(kpi, currentUser);
        loadSalesStats.run();
        loadSales.run();
        updateCashBadge.run();
    }

    public void showCustomersPane(Button navMenuClientes, Label pageTitleLabel, Label pageSubtitleLabel,
                                   VBox customersPane, User currentUser,
                                   VBox[] allPanes,
                                   Button[] allNavButtons,
                                   TableView<Customer> customersTableParam,
                                   Consumer<String> onSearch,
                                   Runnable loadCustomers, Runnable updateCashBadge,
                                   Runnable onNewCustomer, Runnable onEditCustomer, Runnable onDeleteCustomer,
                                   Runnable onViewCustomer, Runnable onSettleDebt, Runnable onReconcileCustomerCredits) {
        setActiveNav(navMenuClientes, allNavButtons);
        pageTitleLabel.setText("Clientes & Contas Correntes");
        pageSubtitleLabel.setText("Gestão de clientes, NUIT, limites de crédito e saldos devedores");
        setPaneVisibility(customersPane, allPanes);

        if (customersTableParam == null) {
            customersTableParam = new TableView<>();
        }
        this.customersTable = customersTableParam;
        TableView<Customer> customersTable = customersTableParam;

        if (customersPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnNovo = makeActionButton("+ Novo Cliente", "#2563EB", "#ffffff");
            Button btnEditar = makeActionButton("Editar", "#475569", "#ffffff");
            Button btnDetalhes = makeActionButton("Ver Detalhes", "#0EA5E9", "#ffffff");
            Button btnLiquidar = makeActionButton("Liquidar Dívida", "#7C3AED", "#ffffff");
            Button btnReconciliar = makeActionButton("Reconciliar Crédito", "#10B981", "#ffffff");
            Button btnEliminar = makeActionButton("Eliminar", "#EF4444", "#ffffff");
            Button btnAtualizar = makeActionButton("Atualizar", "#10B981", "#ffffff");
            Button btnExportar = makeActionButton("Exportar Excel", "#475569", "#ffffff");

            for (Button b : List.of(btnNovo, btnEditar, btnDetalhes, btnLiquidar, btnReconciliar, btnEliminar, btnAtualizar, btnExportar)) {
                UiUtils.applyHoverElevation(b);
                UiUtils.applyPressFeedback(b);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TextField searchField = new TextField();
            searchField.setPromptText("Pesquisar por código, nome...");
            searchField.setPrefWidth(260);
            searchField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-background-color: #F8FAFC;");

            UiUtils.attachSafe(btnNovo, onNewCustomer, systemLogService, "CUSTOMER_NEW");
            UiUtils.attachSafe(btnEditar, onEditCustomer, systemLogService, "CUSTOMER_EDIT");
            UiUtils.attachSafe(btnDetalhes, onViewCustomer, systemLogService, "CUSTOMER_VIEW");
            UiUtils.attachSafe(btnLiquidar, onSettleDebt, systemLogService, "CUSTOMER_SETTLE_DEBT");
            UiUtils.attachSafe(btnReconciliar, onReconcileCustomerCredits, systemLogService, "CUSTOMER_RECONCILE");
            UiUtils.attachSafe(btnEliminar, onDeleteCustomer, systemLogService, "CUSTOMER_DELETE");
            UiUtils.attachSafe(btnAtualizar, loadCustomers, systemLogService, "CUSTOMER_REFRESH");
            UiUtils.attachSafe(btnExportar, () -> {
                if (crudManager != null) crudManager.exportCustomersToExcel(searchField.getText(), null, null);
            }, systemLogService, "CUSTOMER_EXPORT");

            UiUtils.setupDebounce(searchField, () -> {
                if (onSearch != null) onSearch.accept(searchField.getText());
            }, 400);

            toolbar.getChildren().addAll(btnNovo, btnEditar, btnDetalhes, btnLiquidar, btnReconciliar, btnEliminar, btnAtualizar, btnExportar, spacer, searchField);

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Clientes", "Atacado", "Varejo", "Recentes 30d"},
                new String[]{"Registos totais", "Clientes grossa", "Clientes finais", "Novos este mês"},
                new String[]{"blue", "purple", "green", "orange"}
            );
            kpiGrid.setId("customersKPI");

            customersTable.getColumns().clear();
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

            TableColumn<Customer, String> c5 = new TableColumn<>("Saldo");
            c5.setPrefWidth(120);
            c5.setCellValueFactory(d -> {
                Double bal = d.getValue().getBalance();
                return new SimpleStringProperty(bal != null ? String.format("%.2f MT", bal) : "0.00 MT");
            });

            customersTable.getColumns().addAll(List.of(c1, c2, c3, c4, c5));
            customersTable.setRowFactory(makeTableRowFactory());

            main.getChildren().addAll(kpiGrid, toolbar, customersTable);
            customersPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) customersPane.lookup("#customersKPI");
        kpiManager.updateCustomersKPIs(kpi);
        loadCustomers.run();
        updateCashBadge.run();
    }

    public void showWarehousesPane(Button navArmazens, Label pageTitleLabel, Label pageSubtitleLabel,
                                    VBox warehousesPane, User currentUser,
                                    VBox[] allPanes, Button[] allNavButtons,
                                    Runnable updateCashBadge, Runnable loadWarehousesCards,
                                    Runnable onNewWarehouse, Runnable onEditWarehouse, Runnable onDeleteWarehouse) {
        setActiveNav(navArmazens, allNavButtons);
        pageTitleLabel.setText("Armazéns Centrais");
        pageSubtitleLabel.setText("Depósitos centrais, centros de distribuição e abastecimento de lojas");
        setPaneVisibility(warehousesPane, allPanes);

        if (warehousesPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            FlowPane kpiRow = new FlowPane(12, 0);
            kpiRow.setStyle("-fx-padding: 20 20 8 20;");
            kpiRow.setId("warehousesKpiRow");

            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnNovo = makeActionButton("+ Novo Armazém", "#2563EB", "#ffffff");
            Button btnEditar = makeActionButton("Editar", "#475569", "#ffffff");
            Button btnEliminar = makeActionButton("Eliminar", "#EF4444", "#ffffff");
            Button btnAtualizar = makeActionButton("Atualizar", "#10B981", "#ffffff");

            for (Button b : List.of(btnNovo, btnEditar, btnEliminar, btnAtualizar)) {
                UiUtils.applyHoverElevation(b);
                UiUtils.applyPressFeedback(b);
            }

            UiUtils.attachSafe(btnNovo, onNewWarehouse, systemLogService, "WAREHOUSE_NEW");
            UiUtils.attachSafe(btnEditar, onEditWarehouse, systemLogService, "WAREHOUSE_EDIT");
            UiUtils.attachSafe(btnEliminar, onDeleteWarehouse, systemLogService, "WAREHOUSE_DELETE");
            UiUtils.attachSafe(btnAtualizar, loadWarehousesCards, systemLogService, "WAREHOUSE_REFRESH");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TextField searchField = new TextField();
            searchField.setPromptText("Pesquisar armazém...");
            searchField.setPrefWidth(260);
            searchField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-background-color: #F8FAFC;");
            UiUtils.setupDebounce(searchField, loadWarehousesCards, 400);

            toolbar.getChildren().addAll(btnNovo, btnEditar, btnEliminar, btnAtualizar, spacer, searchField);

            FlowPane cardsPane = new FlowPane();
            cardsPane.setHgap(16);
            cardsPane.setVgap(16);
            cardsPane.setStyle("-fx-padding: 8 20 20 20;");
            cardsPane.setId("warehousesCardsPane");

            ScrollPane scroll = new ScrollPane(cardsPane);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color:transparent; -fx-background: #F8FAFC; -fx-border-color:transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);

            main.getChildren().addAll(kpiRow, toolbar, scroll);
            warehousesPane.getChildren().add(main);
        }

        loadWarehousesCards.run();
        updateCashBadge.run();
    }

    public void showLogsPane(Label pageTitleLabel, Label pageSubtitleLabel,
                              VBox logsPane, User currentUser,
                              VBox[] allPanes, Button[] allNavButtons) {
        setActiveNav(null, allNavButtons);
        pageTitleLabel.setText("Auditoria & Segurança");
        pageSubtitleLabel.setText("Rastreabilidade de operações, registo de acessos e eventos do sistema");
        setPaneVisibility(logsPane, allPanes);
        logsPane.getChildren().clear();

        HBox header = new HBox(16);
        header.setStyle("-fx-background-color:#1E293B; -fx-padding:16 24; -fx-alignment:CENTER_LEFT;");
        Label titleLbl = new Label("Logs e Auditoria do Sistema");
        titleLbl.setStyle("-fx-font-size:18px; -fx-font-weight:700; -fx-text-fill:#ffffff;");
        Label subLbl = new Label("Registo de todas as ações, erros e eventos");
        subLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#94A3B8; -fx-padding:2 0 0 8;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar logs...");
        searchField.setStyle("-fx-font-size:13px; -fx-padding:6 12; -fx-background-radius:6; -fx-background-color:#0F172A; -fx-text-fill:#ffffff; -fx-border-color:#334155; -fx-border-radius:6; -fx-min-width:240;");

        Button btnRefresh = makeActionButton("Atualizar", "#475569", "#ffffff");
        header.getChildren().addAll(titleLbl, subLbl, spacer, searchField, btnRefresh);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-min-width:120; -fx-font-size:13px;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabAll = buildLogTab("Todos", null, searchField);
        Tab tabErrors = buildLogTab("Erros", "ERROR", searchField);
        Tab tabSec = buildLogTab("Segurança", "SECURITY", searchField);
        Tab tabUser = buildLogTab("Utilizadores", "USER_ACTION", searchField);
        Tab tabSys = buildLogTab("Sistema", "SYSTEM", searchField);

        tabPane.getTabs().addAll(tabAll, tabErrors, tabSec, tabUser, tabSys);

        Runnable doSearch = () -> {
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            if (selected != null) refreshLogTab(selected, (String) selected.getUserData(), searchField.getText());
        };

        btnRefresh.setOnAction(e -> doSearch.run());
        UiUtils.setupDebounce(searchField, doSearch, 400);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                refreshLogTab(newTab, (String) newTab.getUserData(), searchField.getText());
            }
        });

        logsPane.getChildren().addAll(header, tabPane);
    }

    public Tab buildLogTab(String title, String category, TextField searchField) {
        Tab tab = new Tab(title);
        tab.setUserData(category);
        TableView<AuditLog> table = buildLogTable();
        tab.setContent(table);
        refreshLogTab(tab, category, searchField != null ? searchField.getText() : "");
        return tab;
    }

    public void refreshLogTab(Tab tab, String category, String searchQuery) {
        try {
            com.sgv.repository.AuditLogRepository repo = applicationContext.getBean(com.sgv.repository.AuditLogRepository.class);
            List<AuditLog> logs;
            if (category == null) {
                logs = repo.findAllByOrderByCreatedAtDesc();
            } else {
                logs = repo.findByCategoryOrderByCreatedAtDesc(category);
            }
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String q = searchQuery.trim().toLowerCase();
                logs = logs.stream().filter(l -> 
                    (l.getAction() != null && l.getAction().toLowerCase().contains(q)) ||
                    (l.getDetails() != null && l.getDetails().toLowerCase().contains(q)) ||
                    (l.getUsername() != null && l.getUsername().toLowerCase().contains(q))
                ).toList();
            }
            @SuppressWarnings("unchecked")
            TableView<AuditLog> table = (TableView<AuditLog>) tab.getContent();
            if (table != null) {
                table.setItems(FXCollections.observableArrayList(logs));
            }
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public TableView<AuditLog> buildLogTable() {
        TableView<AuditLog> table = new TableView<>();
        table.setStyle("-fx-font-size:12px; -fx-background-color:#F8FAFC;");
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<AuditLog, String> colCat = new TableColumn<>("Categoria");
        colCat.setPrefWidth(110);
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory() != null ? d.getValue().getCategory() : "—"));
        colCat.setCellFactory(col -> new TableCell<>() {
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

        TableColumn<AuditLog, String> colDate = new TableColumn<>("Data / Hora");
        colDate.setPrefWidth(140);
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCreatedAt() != null
                ? d.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : "—"));

        TableColumn<AuditLog, String> colUser = new TableColumn<>("Utilizador");
        colUser.setPrefWidth(120);
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername() != null ? d.getValue().getUsername() : "—"));

        TableColumn<AuditLog, String> colAction = new TableColumn<>("Ação");
        colAction.setPrefWidth(200);
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAction() != null ? d.getValue().getAction() : "—"));

        TableColumn<AuditLog, String> colDetails = new TableColumn<>("Detalhes");
        colDetails.setPrefWidth(340);
        colDetails.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDetails() != null ? d.getValue().getDetails() : ""));

        TableColumn<AuditLog, String> colStack = new TableColumn<>("Stacktrace");
        colStack.setPrefWidth(60);
        colStack.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStackTrace() != null ? "Ver" : ""));
        colStack.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); setGraphic(null); return; }
                Button btn = new Button("Ver");
                btn.setStyle("-fx-background-color:#6366F1; -fx-text-fill:white; -fx-font-size:10px; -fx-padding:2 6; -fx-background-radius:3; -fx-cursor:hand;");
                btn.setOnAction(e -> {
                    AuditLog log = getTableView().getItems().get(getIndex());
                    TextArea area = new TextArea(log.getStackTrace());
                    area.setEditable(false);
                    area.setWrapText(true);
                    area.setPrefSize(700, 400);
                    Dialog<Void> dlg = new Dialog<>();
                    dlg.setTitle("Stacktrace — " + log.getAction());
                    dlg.getDialogPane().setContent(area);
                    ButtonType btnCopy = new ButtonType("Copiar Erro");
                    dlg.getDialogPane().getButtonTypes().addAll(btnCopy, ButtonType.CLOSE);
                    javafx.scene.Node copyNode = dlg.getDialogPane().lookupButton(btnCopy);
                    if (copyNode instanceof Button copyBtn) {
                        copyBtn.setOnAction(ev -> {
                            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                            cc.putString(area.getText());
                            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
                            copyBtn.setText("Copiado!");
                        });
                    }
                    dlg.showAndWait();
                });
                setGraphic(btn);
                setText(null);
            }
        });

        table.getColumns().addAll(List.of(colCat, colDate, colUser, colAction, colDetails, colStack));
        table.setPlaceholder(new Label("Sem registos para esta categoria."));

        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(AuditLog item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                String cat = item.getCategory();
                if ("ERROR".equals(cat)) setStyle("-fx-background-color:#FEF2F2;");
                else if ("SECURITY".equals(cat)) setStyle("-fx-background-color:#FFFBEB;");
                else if ("SYSTEM".equals(cat)) setStyle("-fx-background-color:#F5F3FF;");
                else setStyle("");
            }
        });

        return table;
    }

    public void showFornecedoresPane(Button navFornecedores, Label pageTitleLabel, Label pageSubtitleLabel,
                                     VBox fornecedoresPane, User currentUser,
                                     VBox[] allPanes, Button[] allNavButtons,
                                     Runnable updateCashBadge,
                                     Runnable onNewSupplier, Runnable onEditSupplier, Runnable onViewSupplier, Runnable onNewSupplierPayment, Runnable onDeleteSupplier) {
        setActiveNav(navFornecedores, allNavButtons);
        pageTitleLabel.setText("Gestão de Fornecedores");
        pageSubtitleLabel.setText("Entrada de mercadorias, facturas de fornecedores e abastecimento");
        setPaneVisibility(fornecedoresPane, allPanes);

        if (fornecedoresPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnNovo = makeActionButton("+ Novo Fornecedor", "#2563EB", "#ffffff");
            Button btnDetalhes = makeActionButton("Ver Detalhes", "#0EA5E9", "#ffffff");
            Button btnEditar = makeActionButton("Editar", "#475569", "#ffffff");
            Button btnPagamento = makeActionButton("+ Pagamento", "#10B981", "#ffffff");
            Button btnEliminar = makeActionButton("Eliminar", "#EF4444", "#ffffff");
            Button btnAtualizar = makeActionButton("Atualizar", "#64748B", "#ffffff");

            for (Button b : List.of(btnNovo, btnDetalhes, btnEditar, btnPagamento, btnEliminar, btnAtualizar)) {
                UiUtils.applyHoverElevation(b);
                UiUtils.applyPressFeedback(b);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TextField searchField = new TextField();
            searchField.setPromptText("Pesquisar por nome, NUIT, contacto...");
            searchField.setPrefWidth(280);
            searchField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-background-color: #F8FAFC;");

            UiUtils.attachSafe(btnNovo, onNewSupplier, systemLogService, "SUPPLIER_NEW");
            UiUtils.attachSafe(btnDetalhes, onViewSupplier, systemLogService, "SUPPLIER_VIEW");
            UiUtils.attachSafe(btnEditar, onEditSupplier, systemLogService, "SUPPLIER_EDIT");
            UiUtils.attachSafe(btnPagamento, onNewSupplierPayment, systemLogService, "SUPPLIER_PAYMENT_NEW");
            UiUtils.attachSafe(btnEliminar, onDeleteSupplier, systemLogService, "SUPPLIER_DELETE");
            UiUtils.attachSafe(btnAtualizar, () -> reloadSuppliers(), systemLogService, "SUPPLIER_REFRESH");

            toolbar.getChildren().addAll(btnNovo, btnDetalhes, btnEditar, btnPagamento, btnEliminar, btnAtualizar, spacer, searchField);

            TableView<Supplier> table = new TableView<>();
            table.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");
            table.setRowFactory(makeTableRowFactory(onViewSupplier));
            this.suppliersTable = table;

            TableColumn<Supplier, String> s1 = new TableColumn<>("Nome");
            s1.setPrefWidth(260);
            s1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName() != null ? d.getValue().getName() : "—"));

            TableColumn<Supplier, String> s2 = new TableColumn<>("NUIT");
            s2.setPrefWidth(140);
            s2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNuit() != null && !d.getValue().getNuit().isBlank() ? d.getValue().getNuit() : "—"));

            TableColumn<Supplier, String> s3 = new TableColumn<>("Contacto");
            s3.setPrefWidth(180);
            s3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getContact() != null ? d.getValue().getContact() : "—"));

            TableColumn<Supplier, String> s4 = new TableColumn<>("Endereço");
            s4.setPrefWidth(260);
            s4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAddress() != null ? d.getValue().getAddress() : "—"));

            TableColumn<Supplier, String> s5 = new TableColumn<>("Estado");
            s5.setPrefWidth(100);
            s5.setCellValueFactory(d -> new SimpleStringProperty(Boolean.TRUE.equals(d.getValue().getActive()) ? "Activo" : "Inactivo"));
            s5.setCellFactory(coloredStateCell());

            TableColumn<Supplier, String> s6 = new TableColumn<>("Saldo a Pagar");
            s6.setPrefWidth(140);
            s6.setCellValueFactory(d -> {
                BigDecimal bal = supplierBalances.getOrDefault(d.getValue().getId(), BigDecimal.ZERO);
                return new SimpleStringProperty(fmtMt(bal));
            });
            s6.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(item);
                    boolean zero = "0,00 MT".equals(item);
                    setStyle(zero
                            ? "-fx-text-fill:#10B981; -fx-font-weight:700;"
                            : "-fx-text-fill:#DC2626; -fx-font-weight:700;");
                }
            });

            table.getColumns().addAll(List.of(s1, s2, s3, s4, s5, s6));

            suppliersLoadRunnable = () -> {
                String q = searchField.getText() == null ? "" : searchField.getText().trim();
                List<Supplier> list;
                if (q.isEmpty()) {
                    list = supplierRepository.findAllByOrderByNameAsc();
                } else {
                    list = supplierRepository.findByNameContainingIgnoreCaseOrNuitContainingOrContactContaining(q, q, q);
                }
                Map<Long, BigDecimal> balances = new HashMap<>();
                for (Object[] row : purchaseRepository.findOutstandingBalanceBySupplier()) {
                    Long sid = (Long) row[0];
                    BigDecimal bal = (BigDecimal) row[1];
                    if (sid != null && bal != null && bal.compareTo(BigDecimal.ZERO) != 0) {
                        balances.put(sid, bal);
                    }
                }
                supplierBalances = balances;
                table.setItems(FXCollections.observableArrayList(list));
                if (suppliersPaymentsLoadRunnable != null) suppliersPaymentsLoadRunnable.run();
            };
            UiUtils.setupDebounce(searchField, suppliersLoadRunnable, 400);
            suppliersLoadRunnable.run();

            // ── Histórico de pagamentos a fornecedores ─────────────────────
            Label paymentsTitle = new Label("HISTÓRICO DE PAGAMENTOS A FORNECEDORES");
            paymentsTitle.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#475569; -fx-font-family:monospace;");
            Label paymentsContext = new Label("Pagamentos recentes (todos os fornecedores) — selecione um fornecedor para filtrar");
            paymentsContext.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#64748B;");
            TableView<SupplierPaymentRow> paymentsTable = new TableView<>();
            paymentsTable.setPrefHeight(230);
            paymentsTable.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff; -fx-border-color:#E2E8F0; -fx-border-width:1;");
            paymentsTable.setPlaceholder(new Label("Sem pagamentos registados"));

            TableColumn<SupplierPaymentRow, String> p1 = new TableColumn<>("Data");
            p1.setPrefWidth(140);
            p1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getData()));
            TableColumn<SupplierPaymentRow, String> p2 = new TableColumn<>("Fornecedor");
            p2.setPrefWidth(200);
            p2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFornecedor()));
            TableColumn<SupplierPaymentRow, String> p3 = new TableColumn<>("Compra");
            p3.setPrefWidth(130);
            p3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCompra()));
            TableColumn<SupplierPaymentRow, String> p4 = new TableColumn<>("Valor");
            p4.setPrefWidth(120);
            p4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getValor()));
            TableColumn<SupplierPaymentRow, String> p5 = new TableColumn<>("Método");
            p5.setPrefWidth(160);
            p5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMetodo()));
            TableColumn<SupplierPaymentRow, String> p6 = new TableColumn<>("Referência");
            p6.setPrefWidth(170);
            p6.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReferencia()));
            paymentsTable.getColumns().addAll(List.of(p1, p2, p3, p4, p5, p6));

            HBox paymentsHeader = new HBox(10);
            paymentsHeader.getChildren().add(paymentsContext);
            VBox paymentsCard = new VBox(10);
            paymentsCard.setStyle("-fx-background-color:#ffffff; -fx-padding:20; -fx-background-radius:6; -fx-border-color:#E2E8F0; -fx-border-width:1; -fx-border-radius:6;");
            paymentsCard.getChildren().addAll(paymentsTitle, paymentsHeader, paymentsTable);

            suppliersPaymentsLoadRunnable = () -> {
                Supplier sel = table.getSelectionModel().getSelectedItem();
                List<SupplierPayment> payments;
                if (sel != null) {
                    payments = supplierPaymentRepository.findBySupplierId(sel.getId());
                    paymentsContext.setText("Pagamentos de " + sel.getName());
                } else {
                    payments = supplierPaymentRepository.findAllByOrderByCreatedAtDesc();
                    paymentsContext.setText("Pagamentos recentes (todos os fornecedores)");
                }
                if (payments.size() > 200) {
                    payments = new ArrayList<>(payments.subList(0, 200));
                }
                List<SupplierPaymentRow> rows = payments.stream().map(sp -> new SupplierPaymentRow(
                        sp.getCreatedAt() != null ? sp.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—",
                        sp.getSupplier() != null ? sp.getSupplier().getName() : "—",
                        sp.getPurchase() != null
                                ? (sp.getPurchase().getInvoiceNumber() != null ? sp.getPurchase().getInvoiceNumber() : "Compra #" + sp.getPurchase().getId())
                                : "—",
                        sp.getAmountValue() != null ? fmtMt(sp.getAmountValue()) : "—",
                        sp.getMethod() != null ? sp.getMethod() : "—",
                        sp.getReference() != null ? sp.getReference() : "—"
                )).toList();
                paymentsTable.setItems(FXCollections.observableArrayList(rows));
            };
            table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
                if (suppliersPaymentsLoadRunnable != null) suppliersPaymentsLoadRunnable.run();
            });

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Fornecedores", "Activos", "Inactivos", "Dívida Total (MT)"},
                new String[]{"Registos de fornecedores", "Fornecedores activos", "Fornecedores inactivos", "Total a pagar em aberto"},
                new String[]{"blue", "green", "orange", "purple"}
            );
            kpiGrid.setId("suppliersKPI");

            main.getChildren().addAll(kpiGrid, toolbar, table, paymentsCard);
            fornecedoresPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) fornecedoresPane.lookup("#suppliersKPI");
        if (kpi != null) {
            kpiManager.updateSuppliersKPIs(kpi);
        }

        updateCashBadge.run();
    }

            main.getChildren().addAll(kpiGrid, toolbar, table, paymentsCard);
            fornecedoresPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) fornecedoresPane.lookup("#suppliersKPI");
        if (kpi != null) {
            kpiManager.updateSuppliersKPIs(kpi);
        }

        updateCashBadge.run();
    }

    public void showStockWarehousePane(Button navStockArmazem, Label pageTitleLabel, Label pageSubtitleLabel,
                                       VBox stockArmazemPane, User currentUser,
                                       VBox[] allPanes, Button[] allNavButtons,
                                       Runnable updateCashBadge,
                                       Runnable onNewPurchase, Runnable onTransfer) {
        setActiveNav(navStockArmazem, allNavButtons);
        pageTitleLabel.setText("Stock Central por Armazém");
        pageSubtitleLabel.setText("Existências consolidadas em armazéns centrais e depósitos");
        setPaneVisibility(stockArmazemPane, allPanes);

        if (stockArmazemPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnReceber = makeActionButton("Receber Compra", "#10B981", "#ffffff");
            Button btnTransferir = makeActionButton("Transferir para Filial", "#2563EB", "#ffffff");
            Button btnAtualizar = makeActionButton("Atualizar", "#475569", "#ffffff");

            ComboBox<Warehouse> warehouseFilter = new ComboBox<>();
            warehouseFilter.setPromptText("Todos os armazéns");
            warehouseFilter.setPrefWidth(220);
            warehouseFilter.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-background-color: #F8FAFC;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TextField searchField = new TextField();
            searchField.setPromptText("Pesquisar produto...");
            searchField.setPrefWidth(260);
            searchField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-background-color: #F8FAFC;");

            UiUtils.attachSafe(btnReceber, onNewPurchase, systemLogService, "SW_RECEIVE");
            UiUtils.attachSafe(btnTransferir, onTransfer, systemLogService, "SW_TRANSFER");

            toolbar.getChildren().addAll(btnReceber, btnTransferir, btnAtualizar, warehouseFilter, spacer, searchField);

            TableView<StockWarehouse> table = new TableView<>();
            table.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");
            table.setPlaceholder(new Label("Sem stock registado em armazéns"));
            table.setRowFactory(makeTableRowFactory());

            TableColumn<StockWarehouse, String> w1 = new TableColumn<>("Armazém");
            w1.setPrefWidth(160);
            w1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWarehouse() != null ? d.getValue().getWarehouse().getName() : "—"));

            TableColumn<StockWarehouse, String> w2 = new TableColumn<>("Código");
            w2.setPrefWidth(90);
            w2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null && d.getValue().getProduct().getCode() != null ? d.getValue().getProduct().getCode() : "—"));

            TableColumn<StockWarehouse, String> w3 = new TableColumn<>("Produto");
            w3.setPrefWidth(240);
            w3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null ? d.getValue().getProduct().getName() : "—"));

            TableColumn<StockWarehouse, String> w4 = new TableColumn<>("Categoria");
            w4.setPrefWidth(130);
            w4.setCellValueFactory(d -> {
                Product p = d.getValue().getProduct();
                return new SimpleStringProperty(p != null && p.getCategory() != null ? p.getCategory().getName() : "—");
            });

            TableColumn<StockWarehouse, String> w5 = new TableColumn<>("Quantidade");
            w5.setPrefWidth(110);
            w5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockCurrentAmount() != null ? String.format("%.1f", d.getValue().getStockCurrentAmount()) : "0"));

            TableColumn<StockWarehouse, String> w6 = new TableColumn<>("Unidade");
            w6.setPrefWidth(90);
            w6.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null && d.getValue().getProduct().getUnit() != null ? d.getValue().getProduct().getUnit().getAbbreviation() : "—"));

            TableColumn<StockWarehouse, String> w7 = new TableColumn<>("Valor");
            w7.setPrefWidth(120);
            w7.setCellValueFactory(d -> {
                BigDecimal qty = d.getValue().getStockCurrentAmount() != null ? d.getValue().getStockCurrentAmount() : BigDecimal.ZERO;
                Double cost = d.getValue().getProduct() != null ? d.getValue().getProduct().getPriceCost() : null;
                return new SimpleStringProperty(cost != null ? String.format("%.2f MT", qty.doubleValue() * cost) : "—");
            });

            table.getColumns().addAll(List.of(w1, w2, w3, w4, w5, w6, w7));

            Runnable load = () -> {
                String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
                Warehouse selected = warehouseFilter.getValue();
                List<StockWarehouse> list = stockWarehouseRepository.findAll();
                list.sort(Comparator.comparing((StockWarehouse sw) -> sw.getWarehouse() != null ? sw.getWarehouse().getName() : "").thenComparing(sw -> sw.getProduct() != null ? sw.getProduct().getName() : ""));
                List<StockWarehouse> filtered = list.stream()
                        .filter(sw -> selected == null || (sw.getWarehouse() != null && sw.getWarehouse().getId().equals(selected.getId())))
                        .filter(sw -> q.isEmpty() || (sw.getProduct() != null && (sw.getProduct().getName().toLowerCase().contains(q) || (sw.getProduct().getCode() != null && sw.getProduct().getCode().toLowerCase().contains(q)))))
                        .toList();
                table.setItems(FXCollections.observableArrayList(filtered));
                GridPane kpi = (GridPane) stockArmazemPane.lookup("#stockArmazemKPI");
                if (kpi != null) {
                    int whCount = (int) list.stream().map(sw -> sw.getWarehouse() != null ? sw.getWarehouse().getId() : -1L).distinct().count();
                    BigDecimal totalQty = list.stream().filter(sw -> sw.getStockCurrentAmount() != null).map(StockWarehouse::getStockCurrentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalValue = list.stream().mapToDouble(sw -> {
                        BigDecimal qty = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : BigDecimal.ZERO;
                        Double cost = sw.getProduct() != null ? sw.getProduct().getPriceCost() : null;
                        return cost != null ? qty.doubleValue() * cost : 0.0;
                    }).mapToObj(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
                    kpiManager.updateKPICard(kpi, 0, String.valueOf(list.size()));
                    kpiManager.updateKPICard(kpi, 1, String.format("%.1f", totalQty));
                    kpiManager.updateKPICard(kpi, 2, String.format("%.0f MT", totalValue));
                    kpiManager.updateKPICard(kpi, 3, String.valueOf(whCount));
                }
            };

            List<Warehouse> warehouses = warehouseRepository.findAll();
            warehouses.sort(Comparator.comparing(Warehouse::getName));
            warehouseFilter.setItems(FXCollections.observableArrayList(warehouses));
            warehouseFilter.setCellFactory(cb -> new ListCell<>() {
                @Override
                protected void updateItem(Warehouse w, boolean empty) {
                    super.updateItem(w, empty);
                    setText(empty || w == null ? null : w.getName());
                }
            });
            warehouseFilter.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Warehouse w, boolean empty) {
                    super.updateItem(w, empty);
                    setText(empty || w == null ? "Todos os armazéns" : w.getName());
                }
            });
            warehouseFilter.valueProperty().addListener((obs, o, n) -> load.run());

            UiUtils.setupDebounce(searchField, load, 400);
            UiUtils.attachSafe(btnAtualizar, load, systemLogService, "SW_REFRESH");

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Linhas de Stock", "Quantidade Total", "Valor em Stock", "Armazéns"},
                new String[]{"Produtos em armazém", "Unidades totais", "Custo em stock", "Armazéns com stock"},
                new String[]{"blue", "green", "purple", "orange"}
            );
            kpiGrid.setId("stockArmazemKPI");

            main.getChildren().addAll(kpiGrid, toolbar, table);
            stockArmazemPane.getChildren().add(main);

            load.run();
        }

        updateCashBadge.run();
    }

    public void showComprasPane(Button navCompras, Label pageTitleLabel, Label pageSubtitleLabel,
                                 VBox comprasPane, User currentUser,
                                 VBox[] allPanes, Button[] allNavButtons,
                                 Runnable updateCashBadge,
                                 Runnable onNewPurchase, Runnable onViewPurchase,
                                 Runnable onNewPayment, Runnable onAnnulPurchase, Runnable onRefresh) {
        setActiveNav(navCompras, allNavButtons);
        pageTitleLabel.setText("Facturas de Compra");
        pageSubtitleLabel.setText("Entrada de mercadorias, facturas de fornecedores e abastecimento");
        setPaneVisibility(comprasPane, allPanes);

        if (comprasPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Compras", "Volume Comprado", "Por Liquidar", "Liquidadas"},
                new String[]{"Todas as compras", "Total faturado (MT)", "Com saldo pendente", "Totalmente pagas"},
                new String[]{"blue", "green", "orange", "purple"}
            );
            kpiGrid.setId("comprasKPI");

            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnNovo = makeActionButton("+ Nova Compra", "#2563EB", "#ffffff");
            Button btnDetalhes = makeActionButton("Ver Detalhes", "#0EA5E9", "#ffffff");
            Button btnPagamento = makeActionButton("+ Pagamento", "#10B981", "#ffffff");
            Button btnAnular = makeActionButton("Anular Compra", "#EF4444", "#ffffff");
            Button btnAtualizar = makeActionButton("Atualizar", "#64748B", "#ffffff");

            for (Button b : List.of(btnNovo, btnDetalhes, btnPagamento, btnAnular, btnAtualizar)) {
                UiUtils.applyHoverElevation(b);
                UiUtils.applyPressFeedback(b);
            }

            UiUtils.attachSafe(btnNovo, onNewPurchase, systemLogService, "PURCHASE_NEW");
            UiUtils.attachSafe(btnDetalhes, onViewPurchase, systemLogService, "PURCHASE_VIEW");
            UiUtils.attachSafe(btnPagamento, onNewPayment, systemLogService, "PURCHASE_PAYMENT_NEW");
            UiUtils.attachSafe(btnAnular, onAnnulPurchase, systemLogService, "PURCHASE_ANNUL");
            UiUtils.attachSafe(btnAtualizar, () -> { if (onRefresh != null) onRefresh.run(); }, systemLogService, "PURCHASE_REFRESH");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TextField searchField = new TextField();
            searchField.setPromptText("Pesquisar por nº factura ou fornecedor...");
            searchField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-background-color: #F8FAFC; -fx-min-width: 280; -fx-pref-width: 320;");

            toolbar.getChildren().addAll(btnNovo, btnDetalhes, btnPagamento, btnAnular, btnAtualizar, spacer, searchField);

            TableView<Purchase> purchasesTable = new TableView<>();
            purchasesTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0 20 20 20;");
            purchasesTable.setPlaceholder(new Label("Sem facturas de compra registadas"));
            purchasesTable.setRowFactory(makeTableRowFactory(onViewPurchase));
            this.purchasesTable = purchasesTable;

            TableColumn<Purchase, String> p1 = new TableColumn<>("Factura");
            p1.setPrefWidth(140);
            p1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getInvoiceNumber() != null ? d.getValue().getInvoiceNumber() : "—"));

            TableColumn<Purchase, String> p2 = new TableColumn<>("Data");
            p2.setPrefWidth(110);
            p2.setCellValueFactory(d -> {
                LocalDateTime dt = d.getValue().getPurchaseDate() != null ? d.getValue().getPurchaseDate() : d.getValue().getCreatedAt();
                return new SimpleStringProperty(dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—");
            });

            TableColumn<Purchase, String> p3 = new TableColumn<>("Fornecedor");
            p3.setPrefWidth(220);
            p3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSupplier() != null ? d.getValue().getSupplier().getName() : "—"));

            TableColumn<Purchase, String> p4 = new TableColumn<>("Armazém");
            p4.setPrefWidth(150);
            p4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTargetWarehouse() != null ? d.getValue().getTargetWarehouse().getName() : "—"));

            TableColumn<Purchase, String> p5 = new TableColumn<>("Total Factura");
            p5.setPrefWidth(130);
            p5.setCellValueFactory(d -> new SimpleStringProperty(fmtMt(d.getValue().getTotalAmount() != null ? d.getValue().getTotalAmount() : BigDecimal.ZERO)));

            TableColumn<Purchase, String> p6 = new TableColumn<>("Valor Pago");
            p6.setPrefWidth(120);
            p6.setCellValueFactory(d -> new SimpleStringProperty(fmtMt(d.getValue().getPaidAmountValue() != null ? d.getValue().getPaidAmountValue() : BigDecimal.ZERO)));

            TableColumn<Purchase, String> p7 = new TableColumn<>("Saldo Pendente");
            p7.setPrefWidth(130);
            p7.setCellValueFactory(d -> {
                BigDecimal tot = d.getValue().getTotalAmount() != null ? d.getValue().getTotalAmount() : BigDecimal.ZERO;
                BigDecimal paid = d.getValue().getPaidAmountValue() != null ? d.getValue().getPaidAmountValue() : BigDecimal.ZERO;
                BigDecimal pend = tot.subtract(paid);
                return new SimpleStringProperty(fmtMt(pend));
            });
            p7.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(item);
                    boolean zero = "0,00 MT".equals(item);
                    setStyle(zero
                            ? "-fx-text-fill:#10B981; -fx-font-weight:700;"
                            : "-fx-text-fill:#DC2626; -fx-font-weight:700;");
                }
            });

            TableColumn<Purchase, String> p8 = new TableColumn<>("Estado");
            p8.setPrefWidth(110);
            p8.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState() != null ? d.getValue().getState() : "—"));
            p8.setCellFactory(coloredStateCell());

            purchasesTable.getColumns().addAll(List.of(p1, p2, p3, p4, p5, p6, p7, p8));

            Runnable loadData = () -> {
                String q = searchField.getText() == null ? "" : searchField.getText().trim();
                List<Purchase> list;
                if (q.isEmpty()) {
                    list = purchaseRepository.findAllByOrderByCreatedAtDesc();
                } else {
                    list = purchaseRepository.searchByInvoice(q);
                }
                purchasesTable.setItems(FXCollections.observableArrayList(list));
                GridPane kpi = (GridPane) comprasPane.lookup("#comprasKPI");
                if (kpi != null) {
                    kpiManager.updateComprasKPIs(kpi);
                }
            };
            this.purchasesLoadRunnable = loadData;
            UiUtils.setupDebounce(searchField, loadData, 400);
            loadData.run();

            main.getChildren().addAll(kpiGrid, toolbar, purchasesTable);
            comprasPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) comprasPane.lookup("#comprasKPI");
        if (kpi != null) {
            kpiManager.updateComprasKPIs(kpi);
        }
        updateCashBadge.run();
    }

    public void showProducaoPane(Button navProducao, Label pageTitleLabel, Label pageSubtitleLabel,
                                  VBox producaoPane, User currentUser,
                                  VBox[] allPanes, Button[] allNavButtons,
                                  Runnable updateCashBadge,
                                  Runnable onNewOrder, Runnable onViewOrder, Runnable onCompleteOrder, Runnable onDeleteOrder) {
        setActiveNav(navProducao, allNavButtons);
        pageTitleLabel.setText("Fabrico & Padaria");
        pageSubtitleLabel.setText("Ordens de fabrico e abate automático de matérias-primas por receita");
        setPaneVisibility(producaoPane, allPanes);

        if (producaoPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Total Ordens", "Pendentes", "Em Curso", "Concluídas"},
                new String[]{"Ordens criadas", "Por iniciar", "A produzir", "Finalizadas"},
                new String[]{"blue", "orange", "purple", "green"}
            );
            kpiGrid.setId("producaoKPI");

            TableView<ProductionOrder> ordersTable = new TableView<>();
            this.ordersTable = ordersTable;
            ordersTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-padding: 0;");

            TableColumn<ProductionOrder, String> o1 = new TableColumn<>("Nº Ordem");
            o1.setPrefWidth(130);
            o1.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderNumber() != null ? d.getValue().getOrderNumber() : "—"));

            TableColumn<ProductionOrder, String> o2 = new TableColumn<>("Produto");
            o2.setPrefWidth(240);
            o2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null ? d.getValue().getProduct().getName() : "—"));

            TableColumn<ProductionOrder, String> o3 = new TableColumn<>("Quantidade");
            o3.setPrefWidth(120);
            o3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getQuantity() != null ? String.valueOf(d.getValue().getQuantity()) + " " + (d.getValue().getUnit() != null ? d.getValue().getUnit() : "") : "—"));

            TableColumn<ProductionOrder, String> o4 = new TableColumn<>("Criada em");
            o4.setPrefWidth(140);
            o4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString().substring(0, 10) : "—"));

            TableColumn<ProductionOrder, String> o5 = new TableColumn<>("Estado");
            o5.setPrefWidth(120);
            o5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getState() != null ? d.getValue().getState() : "—"));
            o5.setCellFactory(coloredStateCell());

            ordersTable.getColumns().addAll(List.of(o1, o2, o3, o4, o5));
            ordersTable.setRowFactory(makeTableRowFactory());

            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnNova = makeActionButton("+ Nova Ordem", "#2563EB", "#ffffff");
            Button btnDetalhes = makeActionButton("Ver Detalhes", "#0EA5E9", "#ffffff");
            Button btnConcluir = makeActionButton("Concluir Ordem", "#10B981", "#ffffff");
            Button btnEliminar = makeActionButton("Eliminar", "#EF4444", "#ffffff");
            Button btnAtualizar = makeActionButton("Atualizar", "#475569", "#ffffff");

            for (Button b : List.of(btnNova, btnDetalhes, btnConcluir, btnEliminar, btnAtualizar)) {
                UiUtils.applyHoverElevation(b);
                UiUtils.applyPressFeedback(b);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TextField searchField = new TextField();
            searchField.setPromptText("Pesquisar ordens (Nº ou Produto)...");
            searchField.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-background-color: #F8FAFC; -fx-pref-width: 280;");

            Runnable loadData = () -> {
                String q = searchField.getText() == null ? "" : searchField.getText().trim();
                List<ProductionOrder> list = q.isEmpty() ? productionOrderRepository.findAllByOrderByCreatedAtDesc() : productionOrderRepository.searchByNumberOrProduct(q);
                ordersTable.setItems(FXCollections.observableArrayList(list));
            };

            UiUtils.attachSafe(btnNova, onNewOrder, systemLogService, "PROD_NEW");
            UiUtils.attachSafe(btnDetalhes, onViewOrder, systemLogService, "PROD_VIEW");
            UiUtils.attachSafe(btnConcluir, onCompleteOrder, systemLogService, "PROD_COMPLETE");
            UiUtils.attachSafe(btnEliminar, onDeleteOrder, systemLogService, "PROD_DELETE");
            UiUtils.attachSafe(btnAtualizar, loadData, systemLogService, "PROD_REFRESH");

            UiUtils.setupDebounce(searchField, loadData, 400);
            loadData.run();

            toolbar.getChildren().addAll(btnNova, btnDetalhes, btnConcluir, btnEliminar, btnAtualizar, spacer, searchField);
            main.getChildren().addAll(kpiGrid, toolbar, ordersTable);
            producaoPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) producaoPane.lookup("#producaoKPI");
        kpiManager.updateProducaoKPIs(kpi);
        updateCashBadge.run();
    }

    public void showReportsPane(Button navRelatorios, Label pageTitleLabel, Label pageSubtitleLabel,
                                 VBox reportsPane, User currentUser,
                                 VBox[] allPanes, Button[] allNavButtons,
                                 Runnable updateCashBadge) {
        setActiveNav(navRelatorios, allNavButtons);
        pageTitleLabel.setText("Mapas Fiscais & SAF-T MZ");
        pageSubtitleLabel.setText("Apuramento de IVA (16%), ficheiro SAF-T oficial AT e mapas de vendas");
        setPaneVisibility(reportsPane, allPanes);

        if (reportsPane.getChildren().isEmpty()) {
            loadReportsPane(reportsPane);
        }

        updateCashBadge.run();
    }

    public void showSistemaPane(Button navSistema, Label pageTitleLabel, Label pageSubtitleLabel,
                                 VBox sistemaPane, User currentUser,
                                 VBox[] allPanes, Button[] allNavButtons,
                                 Runnable updateCashBadge) {
        setActiveNav(navSistema, allNavButtons);
        pageTitleLabel.setText("Parâmetros da Empresa");
        pageSubtitleLabel.setText("Dados fiscais da empresa, cópias de segurança (backup) e licenciamento");
        setPaneVisibility(sistemaPane, allPanes);

        if (sistemaPane.getChildren().isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sistema_module.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                Parent root = loader.load();
                SistemaModuleController ctrl = loader.getController();
                ctrl.setCurrentUser(currentUser);
                sistemaPane.getChildren().add(root);
            } catch (Exception ex) {
                log.error("Erro inesperado", ex);
                sistemaPane.getChildren().add(new Label("Erro ao carregar módulo Sistema: " + ex.getMessage()));
            }
        }
        updateCashBadge.run();
    }

    public void showCatalogsPane(Button navCatalogos, Label pageTitleLabel, Label pageSubtitleLabel,
                                   VBox catalogsPane, User currentUser,
                                   VBox[] allPanes, Button[] allNavButtons,
                                   Runnable updateCashBadge,
                                   Runnable loadCategories, Runnable loadMetricUnits,
                                   TableView<Category> categoriesTableParam, TableView<MetricUnit> unitsTableParam) {
        setActiveNav(navCatalogos, allNavButtons);
        pageTitleLabel.setText("Famílias & Unidades de Medida");
        pageSubtitleLabel.setText("Categorias de artigos e unidades comerciais (UN, KG, L, CX)");
        setPaneVisibility(catalogsPane, allPanes);

        if (categoriesTableParam == null) categoriesTableParam = new TableView<>();
        if (unitsTableParam == null) unitsTableParam = new TableView<>();
        this.categoriesTable = categoriesTableParam;
        this.unitsTable = unitsTableParam;
        TableView<Category> categoriesTable = categoriesTableParam;
        TableView<MetricUnit> unitsTable = unitsTableParam;

        if (catalogsPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Categorias", "Unidades Métricas", "Activas", "—"},
                new String[]{"Total de categorias", "Tipos de unidade", "Unidades activas", "—"},
                new String[]{"blue", "purple", "green", "gray"}
            );
            kpiGrid.setId("catalogsKPI");

            GridPane splitGrid = new GridPane();
            splitGrid.setStyle("-fx-padding: 0 20 20 20;");
            splitGrid.setHgap(16);

            ColumnConstraints half = new ColumnConstraints();
            half.setPercentWidth(50);
            half.setHgrow(Priority.ALWAYS);
            splitGrid.getColumnConstraints().addAll(half, half);

            VBox leftPane = new VBox(0);
            leftPane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 3;");

            HBox catHeader = new HBox(8);
            catHeader.setStyle("-fx-padding: 10 12; -fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
            catHeader.setAlignment(Pos.CENTER_LEFT);
            Label leftTitle = new Label("Categorias");
            leftTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
            Region catSpacer = new Region();
            HBox.setHgrow(catSpacer, Priority.ALWAYS);
            TextField searchCatField = new TextField();
            searchCatField.setPromptText("Pesquisar...");
            searchCatField.setStyle("-fx-font-size: 12px; -fx-padding: 4 8; -fx-background-radius: 4; -fx-border-color: #E2E8F0; -fx-border-radius: 4; -fx-min-width: 120;");
            Button catNewBtn = new Button("+ Nova");
            catNewBtn.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-padding: 6 12; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px;");
            catHeader.getChildren().addAll(leftTitle, catSpacer, searchCatField, catNewBtn);
            
            Runnable doSearchCat = () -> {
                String q = searchCatField.getText() == null ? "" : searchCatField.getText().trim().toLowerCase();
                List<Category> cats = categoryRepository.findAll();
                if (!q.isEmpty()) {
                    cats = cats.stream().filter(c -> c.getName() != null && c.getName().toLowerCase().contains(q)).toList();
                }
                if (categoriesTable != null) categoriesTable.setItems(FXCollections.observableArrayList(cats));
            };
            UiUtils.setupDebounce(searchCatField, doSearchCat, 400);

            if (categoriesTable != null) {
                categoriesTable.getColumns().clear();
                TableColumn<Category, String> catIdCol = new TableColumn<>("ID");
                catIdCol.setPrefWidth(60);
                catIdCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getId() != null ? String.valueOf(d.getValue().getId()) : "—"));

                TableColumn<Category, String> catNomeCol = new TableColumn<>("Nome");
                catNomeCol.setPrefWidth(260);
                catNomeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName() != null ? d.getValue().getName() : "—"));

                categoriesTable.getColumns().addAll(List.of(catIdCol, catNomeCol));
                categoriesTable.setRowFactory(makeTableRowFactory());
            }

            if (categoriesTable != null) {
                leftPane.getChildren().addAll(catHeader, categoriesTable);
                VBox.setVgrow(categoriesTable, Priority.ALWAYS);
            } else {
                leftPane.getChildren().add(catHeader);
            }

            VBox rightPane = new VBox(0);
            rightPane.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 3;");

            HBox unitHeader = new HBox(8);
            unitHeader.setStyle("-fx-padding: 10 12; -fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
            unitHeader.setAlignment(Pos.CENTER_LEFT);
            Label rightTitle = new Label("Unidades Métricas");
            rightTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");
            Region unitSpacer = new Region();
            HBox.setHgrow(unitSpacer, Priority.ALWAYS);
            TextField searchUnitField = new TextField();
            searchUnitField.setPromptText("Pesquisar...");
            searchUnitField.setStyle("-fx-font-size: 12px; -fx-padding: 4 8; -fx-background-radius: 4; -fx-border-color: #E2E8F0; -fx-border-radius: 4; -fx-min-width: 120;");
            Button unitNewBtn = new Button("+ Nova");
            unitNewBtn.setStyle("-fx-background-color: #7C3AED; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-padding: 6 12; -fx-background-radius: 3; -fx-cursor: hand; -fx-font-size: 12px;");
            unitHeader.getChildren().addAll(rightTitle, unitSpacer, searchUnitField, unitNewBtn);
            
            Runnable doSearchUnit = () -> {
                String q = searchUnitField.getText() == null ? "" : searchUnitField.getText().trim().toLowerCase();
                List<MetricUnit> units = metricUnitRepository.findAll();
                if (!q.isEmpty()) {
                    units = units.stream().filter(u -> 
                        (u.getDescription() != null && u.getDescription().toLowerCase().contains(q)) ||
                        (u.getAbbreviation() != null && u.getAbbreviation().toLowerCase().contains(q))
                    ).toList();
                }
                if (unitsTable != null) unitsTable.setItems(FXCollections.observableArrayList(units));
            };
            UiUtils.setupDebounce(searchUnitField, doSearchUnit, 400);

            if (unitsTable != null) {
                unitsTable.getColumns().clear();
                TableColumn<MetricUnit, String> uAbbrCol = new TableColumn<>("Abreviação");
                uAbbrCol.setPrefWidth(100);
                uAbbrCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAbbreviation() != null ? d.getValue().getAbbreviation() : "—"));

                TableColumn<MetricUnit, String> uDescCol = new TableColumn<>("Descrição");
                uDescCol.setPrefWidth(220);
                uDescCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription() != null ? d.getValue().getDescription() : "—"));

                unitsTable.getColumns().addAll(List.of(uAbbrCol, uDescCol));
                unitsTable.setRowFactory(makeTableRowFactory());
            }

            if (unitsTable != null) {
                rightPane.getChildren().addAll(unitHeader, unitsTable);
                VBox.setVgrow(unitsTable, Priority.ALWAYS);
            } else {
                rightPane.getChildren().add(unitHeader);
            }

            splitGrid.add(leftPane, 0, 0);
            splitGrid.add(rightPane, 1, 0);

            main.getChildren().addAll(kpiGrid, splitGrid);
            catalogsPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) catalogsPane.lookup("#catalogsKPI");
        kpiManager.updateCatalogsKPIs(kpi);
        loadCategories.run();
        loadMetricUnits.run();
        updateCashBadge.run();
    }

    public void showStockPane(Label pageTitleLabel, Label pageSubtitleLabel,
                               VBox stockPane, User currentUser,
                               VBox[] allPanes, Button[] allNavButtons,
                               Runnable updateCashBadge) {
        setActiveNav(null, allNavButtons);
        pageTitleLabel.setText("Inventário & Stock em Loja");
        pageSubtitleLabel.setText("Controlo de existências na filial, ajustes manuais e quebras");
        setPaneVisibility(stockPane, allPanes);

        stockPane.getChildren().clear();
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");

        GridPane kpiGrid = buildKPIGrid(
            new String[]{"Total Items", "Valor Stock", "Stock Baixo", "Esgotados"},
            new String[]{"No inventário", "Capital investido", "Precisa reposição", "Fora de stock"},
            new String[]{"blue", "green", "orange", "red"}
        );
        kpiGrid.setId("stockKPI");

        HBox toolbar = new HBox(10);
        toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar produto...");
        searchField.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 8 12; " +
            "-fx-background-radius: 6; -fx-border-color: #E2E8F0; -fx-border-radius: 6; " +
            "-fx-background-color: #F8FAFC; -fx-min-width: 280; -fx-pref-width: 320;"
        );
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);

        Button adjustBtn = makeActionButton("Registar Perda/Dano", "#EF4444", "#ffffff");
        Button detalhesBtn = makeActionButton("Ver Detalhes", "#0EA5E9", "#ffffff");
        Button deleteBtn = makeActionButton("Eliminar", "#EF4444", "#ffffff");
        Button refreshBtn = makeIconButton("↻", "#475569", "#ffffff");

        for (Button b : List.of(adjustBtn, detalhesBtn, deleteBtn, refreshBtn)) {
            UiUtils.applyHoverElevation(b);
            UiUtils.applyPressFeedback(b);
        }

        toolbar.getChildren().addAll(searchField, adjustBtn, detalhesBtn, deleteBtn, refreshBtn);

        TableView<StockBranch> stockTable = new TableView<>();
        stockTable.setId("stockTable");
        stockTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-table-cell-border-color: #F1F5F9;");
        stockTable.setPlaceholder(new Label("Nenhum stock encontrado"));

        Runnable loadStockData = () -> {
            List<StockBranch> all = stockBranchService.findAll();
            String search = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            List<StockBranch> filtered = search.isEmpty() ? all : all.stream()
                .filter(sb -> (sb.getProduct() != null && sb.getProduct().getName() != null && sb.getProduct().getName().toLowerCase().contains(search))
                    || (sb.getProduct() != null && sb.getProduct().getCode() != null && sb.getProduct().getCode().toLowerCase().contains(search))
                    || (sb.getBranch() != null && sb.getBranch().getName() != null && sb.getBranch().getName().toLowerCase().contains(search)))
                .toList();
            stockTable.setItems(FXCollections.observableArrayList(filtered));
            kpiManager.updateStockKPIs(kpiGrid);
        };

        TableColumn<StockBranch, String> c1 = new TableColumn<>("Produto");
        c1.setPrefWidth(250);
        c1.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getProduct() != null ? d.getValue().getProduct().getCode() + " - " + d.getValue().getProduct().getName() : "—"));

        TableColumn<StockBranch, String> c2 = new TableColumn<>("Filial");
        c2.setPrefWidth(150);
        c2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));

        TableColumn<StockBranch, String> c3 = new TableColumn<>("Stock Actual");
        c3.setPrefWidth(120);
        c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockCurrentAmount() != null ? String.format("%.2f", d.getValue().getStockCurrentAmount()) : "0"));
        c3.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    try {
                        double val = Double.parseDouble(item);
                        if (val <= 0) lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #EF4444; -fx-background-color: #FEE2E2; -fx-padding: 2 6; -fx-background-radius: 4;");
                        else if (val < 10) lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #F59E0B; -fx-background-color: #FEF3C7; -fx-padding: 2 6; -fx-background-radius: 4;");
                        else lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #10B981;");
                    } catch (Exception ex) { lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #475569;"); }
                    setGraphic(lbl); setText(null);
                }
            }
        });

        TableColumn<StockBranch, String> c4 = new TableColumn<>("Stock Mín.");
        c4.setPrefWidth(100);
        c4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockMinAmount() != null ? String.format("%.2f", d.getValue().getStockMinAmount()) : "0"));

        TableColumn<StockBranch, String> c5 = new TableColumn<>("Stock Máx.");
        c5.setPrefWidth(100);
        c5.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStockMaxAmount() != null ? String.format("%.2f", d.getValue().getStockMaxAmount()) : "0"));

        stockTable.getColumns().addAll(List.of(c1, c2, c3, c4, c5));

        stockTable.setRowFactory(tv -> {
            TableRow<StockBranch> row = new TableRow<>();
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
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    StockBranch selected = row.getItem();
                    if (selected != null) {
                        showStockDetails(selected, stockPane.getScene().getWindow());
                    }
                }
            });
            return row;
        });

        // Botão + Stock Inicial removido.
        UiUtils.attachSafe(adjustBtn, () -> {
            StockBranch selected = stockTable.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Seleccione um item de stock para ajustar."); return; }
            crudManager.openStockAdjustForm(selected, stockPane.getScene().getWindow(), currentUser, loadStockData);
        }, systemLogService, "STOCK_ADJUST");
        UiUtils.attachSafe(detalhesBtn, () -> {
            StockBranch selected = stockTable.getSelectionModel().getSelectedItem();
            if (selected == null) { showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Seleccione um item de stock para ver detalhes."); return; }
            showStockDetails(selected, stockPane.getScene().getWindow());
        }, systemLogService, "STOCK_VIEW");
        UiUtils.attachSafe(deleteBtn, () -> crudManager.deleteSelectedStock(stockTable, loadStockData, currentUser), systemLogService, "STOCK_DELETE");
        UiUtils.attachSafe(refreshBtn, loadStockData, systemLogService, "STOCK_REFRESH");

        UiUtils.setupDebounce(searchField, loadStockData, 400);

        main.getChildren().addAll(kpiGrid, toolbar, stockTable);
        stockPane.getChildren().add(main);

        FadeTransition fade = new FadeTransition(Duration.millis(400), main);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        loadStockData.run();
        updateCashBadge.run();
    }

    public void showFinanceiroPane(Label pageTitleLabel, Label pageSubtitleLabel,
                                    VBox financeiroPane, User currentUser,
                                    VBox[] allPanes, Button[] allNavButtons,
                                    Runnable updateCashBadge) {
        setActiveNav(null, allNavButtons);
        pageTitleLabel.setText("Tesouraria & Finanças");
        pageSubtitleLabel.setText("Balanço de tesouraria, despesas operacionais e recebimentos");
        setPaneVisibility(financeiroPane, allPanes);

        GridPane kpi = (GridPane) financeiroPane.lookup("#financeiroKPI");
        kpiManager.updateFinanceiroKPIs(kpi);
        updateCashBadge.run();
    }

    public void showUsersPane(Label pageTitleLabel, Label pageSubtitleLabel,
                               VBox usersPane, User currentUser,
                               VBox[] allPanes, Button[] allNavButtons,
                               Runnable updateCashBadge) {
        setActiveNav(null, allNavButtons);
        pageTitleLabel.setText("Utilizadores & Acessos");
        pageSubtitleLabel.setText("Operadores de caixa, fiscais, gerentes e perfis de segurança");
        setPaneVisibility(usersPane, allPanes);

        if (usersPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            GridPane kpiGrid = buildKPIGrid(
                new String[]{"Utilizadores", "Activos", "Admin", "—"},
                new String[]{"Total de users", "Users activos", "Users admin", "—"},
                new String[]{"blue", "green", "orange", "gray"}
            );
            kpiGrid.setId("usersKPI");

            TableView<User> usersTable = new TableView<>();
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

            usersTable.getColumns().addAll(List.of(uc1, uc2, uc3, uc4, uc5, uc6));
            usersTable.setRowFactory(makeTableRowFactory());

            List<User> users = userRepository.findAll();
            usersTable.setItems(FXCollections.observableArrayList(users));

            main.getChildren().addAll(kpiGrid, usersTable);
            usersPane.getChildren().add(main);
        }

        GridPane kpi = (GridPane) usersPane.lookup("#usersKPI");
        kpiManager.updateUsersKPIs(kpi);
        updateCashBadge.run();
    }

public void showTurnoCaixaPane(Label pageTitleLabel, Label pageSubtitleLabel,
                                VBox turnoCaixaPane, User currentUser,
                                VBox[] allPanes, Button[] allNavButtons,
                                Runnable updateCashBadge) {
    pageTitleLabel.setText("Sessão & Fecho de Caixa");
    pageSubtitleLabel.setText("Abertura com Fundo de Maneio, Fecho cego com Fita Z, Sangrias e Reforços");
    setActiveNav(null, allNavButtons);
    setPaneVisibility(turnoCaixaPane, allPanes);
    updateCashBadge.run();

    try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cash_session.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            CashSessionController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setOnStateChange(updateCashBadge::run);
            turnoCaixaPane.getChildren().setAll(root);
        } catch (Exception e) {
            log.error("Erro ao carregar módulo de caixa", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRODUCTS PANE UI BUILDER
    // ═══════════════════════════════════════════════════════════════════════════

    public void showProductsPane(Label pageTitleLabel, Label pageSubtitleLabel,
                                  VBox productsPane, User currentUser,
                                  VBox[] allPanes, Button[] allNavButtons,
                                  Runnable updateCashBadge, Runnable loadProducts,
                                  Consumer<Product> onOpenProduct,
                                  Consumer<Product> onDeleteProduct,
                                  Consumer<Product> onViewProduct,
                                  Consumer<String> onSearch) {
        setActiveNav(null, allNavButtons);
        pageTitleLabel.setText("Artigos & Serviços");
        pageSubtitleLabel.setText("Catálogo de artigos, preços de venda, margem de lucro e códigos de barras");
        setPaneVisibility(productsPane, allPanes);
        updateCashBadge.run();

        Runnable loadProductsAndStats = () -> {
            loadProducts.run();
            if (productsStatsCardsBox != null) {
                loadProductStats(productsStatsCardsBox, currentUser);
            }
        };

        Consumer<String> onSearchAndStats = filter -> {
            if (onSearch != null) onSearch.accept(filter);
            if (productsStatsCardsBox != null) {
                loadProductStats(productsStatsCardsBox, currentUser);
            }
        };

        if (productsPane.getChildren().isEmpty()) {
            buildProductsPaneUI(productsPane, currentUser, loadProductsAndStats, onOpenProduct, onDeleteProduct, onViewProduct, onSearchAndStats);
        }
        loadProductsAndStats.run();
    }

    private void buildProductsPaneUI(VBox productsPane, User currentUser,
                                      Runnable loadProducts,
                                      Consumer<Product> onOpenProduct,
                                      Consumer<Product> onDeleteProduct,
                                      Consumer<Product> onViewProduct,
                                      Consumer<String> onSearch) {
        productsPane.setStyle("-fx-padding: 0;");
        if (productsTable == null) {
            productsTable = new TableView<>();
        }

        VBox mainContainer = new VBox(0);
        mainContainer.setStyle("-fx-background-color: #F8FAFC;");
        mainContainer.setOpacity(0);

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

        Button newBtn = makeActionButton("+ Novo", "#2563EB", "#ffffff");
        Button editBtn = makeActionButton("Editar", "#475569", "#ffffff");
        Button detalhesBtn = makeActionButton("Ver Detalhes", "#0EA5E9", "#ffffff");
        Button deleteBtn = makeActionButton("Eliminar", "#EF4444", "#ffffff");
        Button refreshBtn = makeIconButton("↻", "#10B981", "#ffffff");

        toolbar.getChildren().addAll(searchField, newBtn, editBtn, detalhesBtn, deleteBtn, refreshBtn);

        if (onOpenProduct != null) {
            UiUtils.attachSafe(newBtn, () -> onOpenProduct.accept(null), systemLogService, "PRODUCT_NEW");
            UiUtils.attachSafe(editBtn, () -> {
                Product sel = productsTable.getSelectionModel().getSelectedItem();
                if (sel != null) onOpenProduct.accept(sel);
            }, systemLogService, "PRODUCT_EDIT");
        }
        if (onViewProduct != null) {
            UiUtils.attachSafe(detalhesBtn, () -> {
                Product sel = productsTable.getSelectionModel().getSelectedItem();
                if (sel != null) onViewProduct.accept(sel);
            }, systemLogService, "PRODUCT_VIEW");
        }
        if (onDeleteProduct != null) {
            UiUtils.attachSafe(deleteBtn, () -> {
                Product sel = productsTable.getSelectionModel().getSelectedItem();
                if (sel != null) onDeleteProduct.accept(sel);
            }, systemLogService, "PRODUCT_DELETE");
        }
        UiUtils.attachSafe(refreshBtn, loadProducts, systemLogService, "PRODUCT_REFRESH");

        UiUtils.setupDebounce(searchField, () -> {
            if (onSearch != null) onSearch.accept(searchField.getText());
        }, 400);

        productsTable = new TableView<>();
        productsTable.setStyle(
            "-fx-font-size: 13px; -fx-background-color: #ffffff; " +
            "-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; " +
            "-fx-table-cell-border-color: #F1F5F9;"
        );
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        productsTable.setPlaceholder(new Label("Nenhum produto encontrado"));
        productsTable.setRowFactory(makeTableRowFactory());

        TableColumn<Product, String> productCodeColumn = new TableColumn<>("Código");
        productCodeColumn.setPrefWidth(120);
        productCodeColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCode() != null ? data.getValue().getCode() : ""));

        TableColumn<Product, String> productNameColumn = new TableColumn<>("Produto");
        productNameColumn.setPrefWidth(240);
        productNameColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getName() != null ? data.getValue().getName() : ""));

        TableColumn<Product, String> productTypeColumn = new TableColumn<>("Tipo");
        productTypeColumn.setPrefWidth(120);
        productTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(
            Boolean.TRUE.equals(data.getValue().getService()) ? "Serviço" : "Produto"));

        TableColumn<Product, String> productCategoryColumn = new TableColumn<>("Categoria");
        productCategoryColumn.setPrefWidth(160);
        productCategoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getCategory() != null ? data.getValue().getCategory().getName() : "—"));

        TableColumn<Product, String> productPriceColumn = new TableColumn<>("Preço Venda");
        productPriceColumn.setPrefWidth(140);
        productPriceColumn.setCellValueFactory(data -> {
            double price = data.getValue().getPriceSale() != null ? data.getValue().getPriceSale() : 0.0;
            return new SimpleStringProperty(String.format("%.2f MT", price));
        });
        productPriceColumn.setCellFactory(col -> new TableCell<>() {
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
        });

        TableColumn<Product, String> productStockColumn = new TableColumn<>("Stock");
        productStockColumn.setPrefWidth(120);
        productStockColumn.setCellValueFactory(data -> {
            try {
                if (currentUser != null && currentUser.getBranch() != null && data.getValue().getId() != null) {
                    return stockBranchService.findByProductIdAndBranchId(data.getValue().getId(), currentUser.getBranch().getId())
                        .map(sb -> new SimpleStringProperty(String.format("%.1f", sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO)))
                        .orElse(new SimpleStringProperty("0.0"));
                }
            } catch (Exception ex) { log.error("Erro ao carregar stock: " + ex.getMessage()); }
            return new SimpleStringProperty("—");
        });
        productStockColumn.setCellFactory(col -> new TableCell<>() {
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
        });

        productsTable.getColumns().addAll(List.of(productCodeColumn, productNameColumn, productTypeColumn, productCategoryColumn, productPriceColumn, productStockColumn));

        HBox statsCards = buildProductsStatsCards();
        mainContainer.getChildren().addAll(statsCards, toolbar, productsTable);
        productsPane.getChildren().add(mainContainer);

        FadeTransition fadeMain = new FadeTransition(Duration.millis(400), mainContainer);
        fadeMain.setFromValue(0);
        fadeMain.setToValue(1);
        fadeMain.setOnFinished(e -> {
            FadeTransition ftStats = new FadeTransition(Duration.millis(300), statsCards);
            ftStats.setFromValue(0); ftStats.setToValue(1); ftStats.play();
        });
        fadeMain.play();
    }

    private HBox buildProductsStatsCards() {
        HBox cardsBox = new HBox(16);
        cardsBox.setStyle("-fx-padding: 0 0 16 0;");

        VBox cardTotal = makeKpiCard("Total de Produtos", "—", "No catálogo", "card-pane", "kpi-icon kpi-icon-blue", "📦");
        VBox cardPreco = makeKpiCard("Preço Médio", "—", "Valor médio venda", "card-pane", "kpi-icon kpi-icon-green", "💰");
        VBox cardStock = makeKpiCard("Stock Baixo", "—", "Atenção necessária", "card-pane", "kpi-icon kpi-icon-orange", "⚠");
        VBox cardValor = makeKpiCard("Valor em Stock", "—", "Capital em produtos", "card-pane", "kpi-icon kpi-icon-purple", "📊");

        HBox.setHgrow(cardTotal, Priority.ALWAYS);
        HBox.setHgrow(cardPreco, Priority.ALWAYS);
        HBox.setHgrow(cardStock, Priority.ALWAYS);
        HBox.setHgrow(cardValor, Priority.ALWAYS);
        cardsBox.getChildren().addAll(cardTotal, cardPreco, cardStock, cardValor);
        this.productsStatsCardsBox = cardsBox;
        return cardsBox;
    }

    private VBox makeKpiCard(String title, String value, String subtitle, String... styleClasses) {
        VBox card = new VBox();
        for (String sc : styleClasses) card.getStyleClass().add(sc);
        card.setStyle(card.getStyle() + "; -fx-padding: 22; -fx-min-height: 110; -fx-pref-height: 110;");

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
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

    public void loadProductStats(HBox cardsBox, User currentUser) {
        if (cardsBox == null || cardsBox.getChildren().size() < 4) return;
        try {
            long total = productRepository.count();

            double avgPrice = 0;
            try {
                List<Product> all = productRepository.findAll();
                avgPrice = all.stream()
                    .filter(p -> p.getPriceSale() != null && p.getPriceSale() > 0)
                    .mapToDouble(Product::getPriceSale)
                    .average().orElse(0);
            } catch (Exception ex) { log.error("Erro ao calcular KPIs: " + ex.getMessage()); }

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
            } catch (Exception ex) { log.error("Erro ao calcular stock/valor: " + ex.getMessage()); }

            setKpiCardValue(cardsBox, 0, String.valueOf(total));
            setKpiCardValue(cardsBox, 1, String.format("%.0f MT", avgPrice));
            setKpiCardValue(cardsBox, 2, String.valueOf(lowStock));
            setKpiCardValue(cardsBox, 3, String.format("%.0f MT", totalValue));
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    private void setKpiCardValue(HBox cardsBox, int index, String value) {
        if (index < 0 || index >= cardsBox.getChildren().size()) return;
        javafx.scene.Node card = cardsBox.getChildren().get(index);
        if (!(card instanceof VBox)) return;
        VBox vbox = (VBox) card;
        if (vbox.getChildren().isEmpty()) return;
        javafx.scene.Node row = vbox.getChildren().get(0);
        if (!(row instanceof HBox)) return;
        javafx.scene.Node info = ((HBox) row).getChildren().get(1);
        if (!(info instanceof VBox)) return;
        javafx.scene.Node valueLbl = ((VBox) info).getChildren().get(1);
        if (valueLbl instanceof Label) ((Label) valueLbl).setText(value);
    }

    private void showAlert(javafx.scene.control.Alert.AlertType type, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showStockDetails(StockBranch selected, Window owner) {
        String productInfo = selected.getProduct() != null
            ? selected.getProduct().getCode() + " - " + selected.getProduct().getName() : "—";
        String branchInfo = selected.getBranch() != null ? selected.getBranch().getName() : "—";
        String stockActual = selected.getStockCurrentAmount() != null ? String.format("%.2f", selected.getStockCurrentAmount()) : "0";
        String stockMin = selected.getStockMinAmount() != null ? String.format("%.2f", selected.getStockMinAmount()) : "0";
        String stockMax = selected.getStockMaxAmount() != null ? String.format("%.2f", selected.getStockMaxAmount()) : "0";

        var dialog = DetailDialog.create(owner)
            .title("Ficha de Stock & Kardex")
            .subtitle(productInfo)
            .width(740)
            .height(580)
            .section("Dados do Artigo")
            .field("Código", selected.getProduct() != null ? selected.getProduct().getCode() : "—")
            .field("Nome do Artigo", selected.getProduct() != null ? selected.getProduct().getName() : "—")
            .field("Estabelecimento / Filial", branchInfo)
            .section("Posição de Inventário")
            .field("Stock Actual em Loja", stockActual, "#2563EB")
            .field("Stock Mínimo (Alerta)", stockMin)
            .field("Stock Máximo (Capacidade)", stockMax);

        if (selected.getProduct() != null && selected.getProduct().getId() != null) {
            List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(selected.getProduct().getId());
            if (movements != null && !movements.isEmpty()) {
                String[] cols = {"Data", "Tipo", "Subtipo", "Qtd", "Antes", "Depois", "Referência", "Operador"};
                List<Map<String, String>> rows = new ArrayList<>();
                for (StockMovement m : movements.stream().limit(15).toList()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("Data", m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FORMATTER) : "—");
                    row.put("Tipo", m.getType() != null ? m.getType() : "—");
                    row.put("Subtipo", m.getSubtype() != null ? m.getSubtype() : "—");
                    row.put("Qtd", m.getQtyAmount() != null ? String.format("%.2f", m.getQtyAmount()) : "—");
                    row.put("Antes", m.getStockBeforeAmount() != null ? String.format("%.2f", m.getStockBeforeAmount()) : "—");
                    row.put("Depois", m.getStockAfterAmount() != null ? String.format("%.2f", m.getStockAfterAmount()) : "—");
                    row.put("Referência", m.getReference() != null ? m.getReference() : "—");
                    row.put("Operador", m.getUser() != null ? m.getUser().getUsername() : "—");
                    rows.add(row);
                }
                dialog.tableSection("Histórico de Movimentações (Kardex)", cols, rows);
            }
        }

        dialog.show();
    }

    public void showTransfersPane(Button navTransferir, Label pageTitleLabel, Label pageSubtitleLabel,
                                  VBox transfersPane, User currentUser,
                                  VBox[] allPanes, Button[] allNavButtons,
                                  Runnable updateCashBadge, Runnable onNewTransfer) {
        setActiveNav(navTransferir, allNavButtons);
        pageTitleLabel.setText("Guias de Transferência de Stock");
        pageSubtitleLabel.setText("Movimentações entre Armazém Central e Lojas com Guia de Transporte");
        setPaneVisibility(transfersPane, allPanes);

        if (transfersPane.getChildren().isEmpty()) {
            VBox main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");

            // 1. KPI Row
            FlowPane kpiRow = new FlowPane(12, 0);
            kpiRow.setStyle("-fx-padding: 16 20 8 20;");

            Label kpiTotal = new Label("0");
            Label kpiCompleted = new Label("0");
            Label kpiInTransit = new Label("0");

            kpiRow.getChildren().addAll(
                makeKpiCard("TOTAL DE GUIAS", kpiTotal, "#2563EB"),
                makeKpiCard("RECEBIDAS NA LOJA", kpiCompleted, "#10B981"),
                makeKpiCard("EM TRÂNSITO / PENDENTES", kpiInTransit, "#F59E0B")
            );

            // 2. Toolbar de Filtros Avançados
            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

            Button btnNova = makeActionButton("+ Nova Guia de Transferência", "#2563EB", "#ffffff");
            Button btnAtualizar = makeActionButton("🔄 Actualizar", "#475569", "#ffffff");

            UiUtils.applyHoverElevation(btnNova);
            UiUtils.applyPressFeedback(btnNova);
            UiUtils.applyHoverElevation(btnAtualizar);
            UiUtils.applyPressFeedback(btnAtualizar);

            ComboBox<Warehouse> whFilter = new ComboBox<>();
            whFilter.setPromptText("Origem: Todos");
            whFilter.setPrefWidth(180);
            whFilter.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");

            ComboBox<Branch> brFilter = new ComboBox<>();
            brFilter.setPromptText("Destino: Todas");
            brFilter.setPrefWidth(180);
            brFilter.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");

            ComboBox<String> statusFilter = new ComboBox<>();
            statusFilter.getItems().addAll("Todos os Estados", "PENDING", "IN_TRANSIT", "COMPLETED", "CANCELLED");
            statusFilter.setValue("Todos os Estados");
            statusFilter.setPrefWidth(160);
            statusFilter.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TextField searchField = new TextField();
            searchField.setPromptText("Pesquisar guia, artigo ou operador...");
            searchField.setPrefWidth(240);
            searchField.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 6 10; -fx-background-radius: 4; -fx-border-color: #E2E8F0; -fx-border-radius: 4;");

            UiUtils.attachSafe(btnNova, onNewTransfer, systemLogService, "TRANSFERS_NEW");

            toolbar.getChildren().addAll(btnNova, btnAtualizar, whFilter, brFilter, statusFilter, spacer, searchField);

            // 3. Tabela de Transferências
            TableView<WarehouseTransfer> table = new TableView<>();
            table.setStyle("-fx-font-size: 12px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1;");
            table.setPlaceholder(new Label("Nenhuma guia de transferência encontrada"));
            table.setRowFactory(makeTableRowFactory());

            TableColumn<WarehouseTransfer, String> c1 = new TableColumn<>("Nº Documento");
            c1.setPrefWidth(130);
            c1.setCellValueFactory(d -> {
                WarehouseTransfer t = d.getValue();
                return new SimpleStringProperty((t.getSeries() != null ? t.getSeries() : "TWA") + " " + t.getDocumentYear() + "/" + t.getDocumentNumber());
            });

            TableColumn<WarehouseTransfer, String> c2 = new TableColumn<>("Data / Hora");
            c2.setPrefWidth(130);
            c2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FORMATTER) : "—"));

            TableColumn<WarehouseTransfer, String> c3 = new TableColumn<>("Origem (Armazém)");
            c3.setPrefWidth(160);
            c3.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWarehouse() != null ? d.getValue().getWarehouse().getName() : "—"));

            TableColumn<WarehouseTransfer, String> c4 = new TableColumn<>("Destino (Filial)");
            c4.setPrefWidth(160);
            c4.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));

            TableColumn<WarehouseTransfer, String> c5 = new TableColumn<>("Itens / Volume");
            c5.setPrefWidth(110);
            c5.setCellValueFactory(d -> {
                int count = d.getValue().getItems() != null ? d.getValue().getItems().size() : 0;
                return new SimpleStringProperty(count + " artigos");
            });

            TableColumn<WarehouseTransfer, String> c6 = new TableColumn<>("Estado");
            c6.setPrefWidth(120);
            c6.setCellValueFactory(d -> {
                String st = d.getValue().getStatus() != null ? d.getValue().getStatus() : "PENDING";
                String desc = switch (st) {
                    case "COMPLETED" -> "🟢 Recebido";
                    case "IN_TRANSIT" -> "🚚 Em Trânsito";
                    case "CANCELLED" -> "🔴 Cancelado";
                    default -> "🟡 Pendente";
                };
                return new SimpleStringProperty(desc);
            });

            TableColumn<WarehouseTransfer, String> c7 = new TableColumn<>("Responsável");
            c7.setPrefWidth(140);
            c7.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRequestedBy() != null ? d.getValue().getRequestedBy().getFullName() : "—"));

            TableColumn<WarehouseTransfer, Void> c8 = new TableColumn<>("Acções");
            c8.setPrefWidth(180);
            c8.setCellFactory(col -> new TableCell<>() {
                private final Button btnVer = new Button("👁️ Ver");
                private final Button btnReceber = new Button("✅ Confirmar");
                private final HBox box = new HBox(6, btnVer, btnReceber);

                {
                    btnVer.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #F1F5F9; -fx-text-fill: #0F172A; -fx-background-radius: 3; -fx-cursor: hand;");
                    btnReceber.setStyle("-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 3; -fx-cursor: hand;");

                    btnVer.setOnAction(e -> {
                        WarehouseTransfer t = getTableView().getItems().get(getIndex());
                        if (t != null) {
                            String st = t.getStatus() != null ? t.getStatus() : "PENDING";
                            String stColor = switch (st) {
                                case "COMPLETED" -> "#10B981";
                                case "IN_TRANSIT" -> "#F59E0B";
                                case "CANCELLED" -> "#EF4444";
                                default -> "#3B82F6";
                            };
                            var dd = DetailDialog.create(transfersPane.getScene().getWindow())
                                .title("Guia de Transferência " + (t.getSeries() != null ? t.getSeries() : "TWA") + " " + t.getDocumentYear() + "/" + t.getDocumentNumber())
                                .subtitle("Expedição de Stock do Armazém Central para Loja")
                                .statusBadge(st, stColor)
                                .width(680)
                                .height(540)
                                .section("Dados da Guia de Transporte")
                                .field("Armazém de Origem", t.getWarehouse() != null ? t.getWarehouse().getName() : "—")
                                .field("Filial de Destino", t.getBranch() != null ? t.getBranch().getName() : "—")
                                .field("Data de Emissão", t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FORMATTER) : "—")
                                .field("Responsável", t.getRequestedBy() != null ? t.getRequestedBy().getFullName() : "—");

                            if (t.getItems() != null && !t.getItems().isEmpty()) {
                                String[] cols = {"Código", "Artigo / Descrição", "Quantidade", "Unidade"};
                                List<Map<String, String>> rows = new ArrayList<>();
                                for (WarehouseTransferItem it : t.getItems()) {
                                    Map<String, String> row = new LinkedHashMap<>();
                                    row.put("Código", it.getProduct() != null ? it.getProduct().getCode() : "—");
                                    row.put("Artigo / Descrição", it.getProduct() != null ? it.getProduct().getName() : "—");
                                    row.put("Quantidade", String.format("%.2f", it.getQuantity() != null ? it.getQuantity() : 0));
                                    row.put("Unidade", it.getProduct() != null && it.getProduct().getUnit() != null ? it.getProduct().getUnit().getAbbreviation() : "UN");
                                    rows.add(row);
                                }
                                dd.tableSection("Artigos Transferidos (Guia de Transporte TWA)", cols, rows);
                            }
                            dd.show();
                        }
                    });

                    btnReceber.setOnAction(e -> {
                        WarehouseTransfer t = getTableView().getItems().get(getIndex());
                        if (t != null && !"COMPLETED".equals(t.getStatus()) && !"CANCELLED".equals(t.getStatus())) {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Confirmar receção de mercadoria na filial? O stock será actualizado.", ButtonType.YES, ButtonType.NO);
                            alert.setHeaderText(null);
                            alert.showAndWait().ifPresent(response -> {
                                if (response == ButtonType.YES) {
                                    try {
                                        warehouseTransferService.complete(t.getId(), currentUser);
                                        btnAtualizar.fire();
                                    } catch (Exception ex) {
                                        systemLogService.logError("TRANSFER_COMPLETE_FAILED", "Erro ao receber transferência", ex);
                                    }
                                }
                            });
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                    } else {
                        WarehouseTransfer t = getTableView().getItems().get(getIndex());
                        boolean isDone = t != null && ("COMPLETED".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus()));
                        btnReceber.setVisible(!isDone);
                        btnReceber.setManaged(!isDone);
                        setGraphic(box);
                    }
                }
            });

            table.getColumns().addAll(List.of(c1, c2, c3, c4, c5, c6, c7, c8));

            Runnable loadTransfers = () -> {
                List<WarehouseTransfer> all = warehouseTransferRepository.findAllByOrderByCreatedAtDesc();
                long total = all.size();
                long completed = all.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
                long inTransit = all.stream().filter(t -> "IN_TRANSIT".equals(t.getStatus()) || "PENDING".equals(t.getStatus())).count();

                kpiTotal.setText(String.valueOf(total));
                kpiCompleted.setText(String.valueOf(completed));
                kpiInTransit.setText(String.valueOf(inTransit));

                Warehouse selWh = whFilter.getValue();
                Branch selBr = brFilter.getValue();
                String selSt = statusFilter.getValue();
                String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

                List<WarehouseTransfer> filtered = all.stream()
                        .filter(t -> selWh == null || (t.getWarehouse() != null && t.getWarehouse().getId().equals(selWh.getId())))
                        .filter(t -> selBr == null || (t.getBranch() != null && t.getBranch().getId().equals(selBr.getId())))
                        .filter(t -> selSt == null || "Todos os Estados".equals(selSt) || selSt.equalsIgnoreCase(t.getStatus()))
                        .filter(t -> q.isEmpty() || (t.getDocumentNumber() != null && String.valueOf(t.getDocumentNumber()).contains(q))
                                || (t.getWarehouse() != null && t.getWarehouse().getName().toLowerCase().contains(q))
                                || (t.getBranch() != null && t.getBranch().getName().toLowerCase().contains(q)))
                        .toList();

                table.setItems(FXCollections.observableArrayList(filtered));
            };

            whFilter.setItems(FXCollections.observableArrayList(warehouseRepository.findAll()));
            brFilter.setItems(FXCollections.observableArrayList(branchRepository.findAll()));

            whFilter.valueProperty().addListener((o, ov, nv) -> loadTransfers.run());
            brFilter.valueProperty().addListener((o, ov, nv) -> loadTransfers.run());
            statusFilter.valueProperty().addListener((o, ov, nv) -> loadTransfers.run());
            searchField.textProperty().addListener((o, ov, nv) -> loadTransfers.run());
            btnAtualizar.setOnAction(e -> loadTransfers.run());

            main.getChildren().addAll(kpiRow, toolbar, table);
            VBox.setVgrow(table, Priority.ALWAYS);
            transfersPane.getChildren().add(main);
            loadTransfers.run();
        } else {
            // Recarregar dados se já inicializado
            Node main = transfersPane.getChildren().get(0);
            if (main instanceof VBox) {
                // Find table and refresh
            }
        }
        updateCashBadge.run();
    }

    public void showSupplierDetails(Supplier supplier, Window owner) {
        if (supplier == null) return;
        List<Purchase> purchases = purchaseRepository.findBySupplierIdOrderByCreatedAtDesc(supplier.getId());
        List<SupplierPayment> payments = supplierPaymentRepository.findBySupplierId(supplier.getId());

        BigDecimal totalPurchases = purchases.stream()
                .filter(p -> !"CANCELLED".equalsIgnoreCase(p.getState()))
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = payments.stream()
                .map(sp -> sp.getAmountValue() != null ? sp.getAmountValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalPurchases.subtract(totalPaid);

        var dialog = DetailDialog.create(owner)
                .title("Ficha de Fornecedor & Conta-Corrente")
                .subtitle(supplier.getName())
                .statusBadge(Boolean.TRUE.equals(supplier.getActive()) ? "ACTIVO" : "INACTIVO",
                        Boolean.TRUE.equals(supplier.getActive()) ? "#10B981" : "#EF4444")
                .width(740)
                .height(600)
                .section("Identificação da Empresa / Fornecedor")
                .field("Nome / Razão Social", supplier.getName())
                .field("NUIT", supplier.getNuit() != null && !supplier.getNuit().isBlank() ? supplier.getNuit() : "—")
                .field("Contacto / Telefone", supplier.getContact() != null ? supplier.getContact() : "—")
                .field("Endereço Físico", supplier.getAddress() != null ? supplier.getAddress() : "—")
                .section("Posição Financeira & Saldo devedor")
                .field("Total Faturado em Compras", fmtMt(totalPurchases))
                .field("Total Liquidado / Pago", fmtMt(totalPaid), "#10B981")
                .field("Saldo Devedor / A Pagar", fmtMt(balance), balance.compareTo(BigDecimal.ZERO) > 0 ? "#DC2626" : "#10B981");

        if (!purchases.isEmpty()) {
            List<Map<String, String>> pRows = purchases.stream().limit(10).map(p -> {
                Map<String, String> m = new LinkedHashMap<>();
                LocalDateTime dt = p.getPurchaseDate() != null ? p.getPurchaseDate() : p.getCreatedAt();
                m.put("Data", dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—");
                m.put("Factura", p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "#" + p.getId());
                m.put("Total", p.getTotalAmount() != null ? fmtMt(p.getTotalAmount()) : "—");
                m.put("Pago", p.getPaidAmountValue() != null ? fmtMt(p.getPaidAmountValue()) : "—");
                m.put("Estado", p.getState() != null ? p.getState() : "—");
                return m;
            }).toList();
            dialog.tableSection("Histórico de Facturas de Compra (Últimas 10)", new String[]{"Data", "Factura", "Total", "Pago", "Estado"}, pRows);
        }

        if (!payments.isEmpty()) {
            List<Map<String, String>> payRows = payments.stream().limit(10).map(sp -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("Data", sp.getCreatedAt() != null ? sp.getCreatedAt().format(DATE_FORMATTER) : "—");
                m.put("Valor", sp.getAmountValue() != null ? fmtMt(sp.getAmountValue()) : "—");
                m.put("Método", sp.getMethod() != null ? sp.getMethod() : "—");
                m.put("Ref.", sp.getReference() != null ? sp.getReference() : "—");
                return m;
            }).toList();
            dialog.tableSection("Histórico de Pagamentos (Últimos 10)", new String[]{"Data", "Valor", "Método", "Ref."}, payRows);
        }

        dialog.show();
    }

    public void showPurchaseDetails(Purchase purchase, Window owner) {
        if (purchase == null) return;
        Purchase full = purchaseRepository.findByIdWithItems(purchase.getId());
        if (full == null) full = purchase;

        BigDecimal tot = full.getTotalAmount() != null ? full.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = full.getPaidAmountValue() != null ? full.getPaidAmountValue() : BigDecimal.ZERO;
        BigDecimal pending = tot.subtract(paid);

        String statusColor = "PAID".equalsIgnoreCase(full.getState()) ? "#10B981"
                : "CANCELLED".equalsIgnoreCase(full.getState()) ? "#EF4444"
                : "PAGO_PARCIAL".equalsIgnoreCase(full.getState()) ? "#F59E0B" : "#2563EB";

        var dialog = DetailDialog.create(owner)
                .title("Factura de Compra")
                .subtitle(full.getInvoiceNumber() != null ? full.getInvoiceNumber() : "Compra #" + full.getId())
                .statusBadge(full.getState() != null ? full.getState() : "RECEIVED", statusColor)
                .width(740)
                .height(600)
                .section("Dados do Documento")
                .field("Nº Factura Fornecedor", full.getInvoiceNumber() != null ? full.getInvoiceNumber() : "—")
                .field("Fornecedor", full.getSupplier() != null ? full.getSupplier().getName() : "—")
                .field("NUIT Fornecedor", full.getSupplier() != null && full.getSupplier().getNuit() != null ? full.getSupplier().getNuit() : "—")
                .field("Armazém de Entrada", full.getTargetWarehouse() != null ? full.getTargetWarehouse().getName() : "—")
                .field("Data de Emissão", full.getPurchaseDate() != null ? full.getPurchaseDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : (full.getCreatedAt() != null ? full.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"))
                .field("Registado Por", full.getUser() != null ? full.getUser().getFullName() : "—")
                .section("Resumo Financeiro & IVA (CIVA 16%)")
                .field("Subtotal (s/ IVA)", fmtMt(full.getSubtotalAmount() != null ? full.getSubtotalAmount() : BigDecimal.ZERO))
                .field("IVA Suportado (16%)", fmtMt(full.getTotalTaxAmount() != null ? full.getTotalTaxAmount() : BigDecimal.ZERO), "#F59E0B")
                .field("TOTAL FACTURA", fmtMt(tot), "#2563EB")
                .field("Valor Já Amortizado", fmtMt(paid), "#10B981")
                .field("Saldo Pendente", fmtMt(pending), pending.compareTo(BigDecimal.ZERO) > 0 ? "#DC2626" : "#10B981");

        if (full.getItems() != null && !full.getItems().isEmpty()) {
            List<Map<String, String>> itemRows = full.getItems().stream().map(item -> {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("Produto", item.getProduct() != null ? item.getProduct().getName() : "—");
                m.put("Qtd", item.getQuantity() != null ? String.format(java.util.Locale.US, "%.2f %s", item.getQuantity(), (item.getProduct() != null && item.getProduct().getUnit() != null ? item.getProduct().getUnit().getAbbreviation() : "")) : "0");
                m.put("Custo Unit.", item.getCostPrice() != null ? String.format(java.util.Locale.US, "%.2f MT", item.getCostPrice()) : "—");
                m.put("Subtotal", item.getSubtotal() != null ? String.format(java.util.Locale.US, "%.2f MT", item.getSubtotal()) : "—");
                return m;
            }).toList();
            dialog.tableSection("Artigos / Itens da Factura de Compra", new String[]{"Produto", "Qtd", "Custo Unit.", "Subtotal"}, itemRows);
        }

        if (full.getNotes() != null && !full.getNotes().isBlank()) {
            dialog.section("Observações").field("Notas", full.getNotes());
        }

        dialog.show();
    }

}
