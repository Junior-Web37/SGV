package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.AppConfigService;
import com.sgv.service.ReportService;
import com.sgv.service.SafTExportService;
import com.sgv.service.StockBranchService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReportsController {

    @FXML private TabPane reportsTabPane;

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SaleItemRepository saleItemRepository;
    private final ReportService reportService;
    private final StockBranchService stockBranchService;
    private final TransferRepository transferRepository;
    private final SafTExportService safTExportService;
    private final AppConfigService appConfigService;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;

    // Vendas tab state (so filter button can access date pickers)
    private DatePicker vendasDe;
    private DatePicker vendasAte;
    private Label kpiTotalLabel;
    private Label kpiCountLabel;
    private Label kpiTicketLabel;
    private Label kpiIvaLabel;
    private Label kpiLucroLabel;
    private LineChart<String, Number> vendasChart;

    // Pagination
    private static final int PAGE_SIZE = 50;

    // IVA tab pagination state
    private int ivaPage = 0;
    private DatePicker ivaDe;
    private DatePicker ivaAte;
    private TableView<Sale> ivaTable;
    private Label ivaPageLabel;
    private Button ivaPrevBtn;
    private Button ivaNextBtn;
    private Label ivaCountLabel;
    private Label ivaBaseVal;
    private Label ivaIsentoVal;
    private Label ivaIvaVal;
    private Label ivaTotVal;

    // Stock Movimentos tab pagination state
    private int movementsPage = 0;
    private TableView<StockMovementRow> movementsTable;
    private Label movementsPageLabel;
    private Button movementsPrevBtn;
    private Button movementsNextBtn;
    private Label movementsCountLabel;

    // Pagamentos a Fornecedores tab state
    private int pagPage = 0;
    private DatePicker pagDe;
    private DatePicker pagAte;
    private ComboBox<String> pagFornecedorCombo;
    private TableView<SupplierPaymentRow> pagTable;
    private Label pagPageLabel;
    private Label pagCountLabel;
    private Button pagPrevBtn;
    private Button pagNextBtn;
    private Label pagKpiTotalLabel;
    private Label pagKpiCountLabel;
    private Label pagKpiDebtLabel;

    public ReportsController(SaleRepository saleRepository,
                             ProductRepository productRepository,
                             CustomerRepository customerRepository,
                             BranchRepository branchRepository,
                             StockMovementRepository stockMovementRepository,
                             SaleItemRepository saleItemRepository,
                             ReportService reportService,
                             StockBranchService stockBranchService,
                             TransferRepository transferRepository,
                             SafTExportService safTExportService,
                             AppConfigService appConfigService,
                             SupplierPaymentRepository supplierPaymentRepository,
                             PurchaseRepository purchaseRepository,
                             SupplierRepository supplierRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.saleItemRepository = saleItemRepository;
        this.reportService = reportService;
        this.stockBranchService = stockBranchService;
        this.transferRepository = transferRepository;
        this.safTExportService = safTExportService;
        this.appConfigService = appConfigService;
        this.supplierPaymentRepository = supplierPaymentRepository;
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
    }

    @FXML
    public void initialize() {
        buildAllTabs();
    }

    // ══════════════════════════════════════════════════
    // BUILD ALL TABS
    // ══════════════════════════════════════════════════

    private void buildAllTabs() {
        if (reportsTabPane == null) return;
        reportsTabPane.getTabs().clear();
        reportsTabPane.getTabs().addAll(
                buildVendasTab(),
                buildProdutosEmFaltaTab(),
                buildMaisVendidosTab(),
                buildIvaTab(),
                buildAccountsReceivableTab(),
                buildStockMovimentosTab(),
                buildStockMatrixTab(),
                buildTransferenciasTab(),
                buildPagamentosFornecedoresTab()
        );
    }

    // ── TAB 1: VENDAS CONSOLIDADAS ─────────────────────

    private Tab buildVendasTab() {
        Tab tab = new Tab("📊  Mapa Geral de Vendas");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        // ── Filtro (date pickers stored as fields so button can access them)
        vendasDe = new DatePicker(LocalDate.now().withDayOfMonth(1));
        vendasDe.setPrefWidth(130);
        vendasAte = new DatePicker(LocalDate.now());
        vendasAte.setPrefWidth(130);

        ComboBox<String> lojaCombo = new ComboBox<>();
        UiUtils.hardenComboBox(lojaCombo);
        List<String> lojas = new ArrayList<>();
        lojas.add("Todas as Lojas");
        branchRepository.findAll().forEach(b -> { if (b.getName() != null) lojas.add(b.getName()); });
        lojaCombo.setItems(FXCollections.observableArrayList(lojas));
        lojaCombo.getSelectionModel().selectFirst();
        lojaCombo.setPrefWidth(180);

        Button filtrar = btn("FILTRAR", "#2563EB");
        Button hoje = btn("HOJE", "#2563EB");
        hoje.setOnAction(e -> {
            LocalDate t = LocalDate.now();
            vendasDe.setValue(t); vendasAte.setValue(t);
            filtrar.fire();
        });
        Button mes = btn("ESTE MÊS", "#1E40AF");
        mes.setOnAction(e -> {
            LocalDate t = LocalDate.now();
            vendasDe.setValue(t.withDayOfMonth(1)); vendasAte.setValue(t.withDayOfMonth(t.lengthOfMonth()));
            filtrar.fire();
        });
        Button ano = btn("ESTE ANO", "#0F172A");
        ano.setOnAction(e -> {
            LocalDate t = LocalDate.now();
            vendasDe.setValue(t.withDayOfYear(1)); vendasAte.setValue(t.withDayOfYear(t.lengthOfYear()));
            filtrar.fire();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(
            label("De:", "12px"), vendasDe,
            label("Até:", "12px"), vendasAte,
            label("Loja:", "12px"), lojaCombo,
            filtrar, spacer, hoje, mes, ano
        );

        // Wire filter button
        filtrar.setOnAction(e -> {
            LocalDate from = vendasDe.getValue() != null ? vendasDe.getValue() : LocalDate.now().withDayOfMonth(1);
            LocalDate to = vendasAte.getValue() != null ? vendasAte.getValue() : LocalDate.now();
            loadVendasData(from, to, lojaCombo.getValue());
        });

        VBox filterCard = card("FILTRO DE PERÍODO", filterBar);

        // ── KPIs (stored as fields so loadVendasData can update them)
        kpiTotalLabel = new Label("0,00");
        kpiCountLabel = new Label("0");
        kpiTicketLabel = new Label("0,00");
        kpiIvaLabel = new Label("0,00");
        kpiLucroLabel = new Label("0,00");
        HBox kpiH = new HBox(12);
        kpiH.getChildren().addAll(
            kpiCard("Total Vendido",  kpiTotalLabel, "#2563EB"),
            kpiCard("Nº Transacções", kpiCountLabel,  "#7C3AED"),
            kpiCard("Ticket Médio",    kpiTicketLabel, "#2563EB"),
            kpiCard("IVA Cobrado",    kpiIvaLabel,    "#2563EB"),
            kpiCard("Lucro Estimado", kpiLucroLabel,  "#10B981")
        );

        // ── Gráfico
        vendasChart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        vendasChart.setLegendVisible(false);
        vendasChart.setPrefHeight(240);

        VBox chartCard = card("VENDAS POR DIA", new VBox(vendasChart));

        content.getChildren().addAll(filterCard, kpiH, chartCard);

        // Load initial data
        loadVendasData(vendasDe.getValue(), vendasAte.getValue(), null);

        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    private void loadVendasData(LocalDate from, LocalDate to, String selectedBranch) {
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        // Find branches that match the selected filter
        List<Branch> branches = new ArrayList<>();
        if (selectedBranch != null && !"Todas as Lojas".equals(selectedBranch)) {
            branches = branchRepository.findAll().stream()
                    .filter(b -> selectedBranch.equals(b.getName()))
                    .toList();
        }

        List<Sale> sales = saleRepository.findByDateRangeAndState(fromDt, toDt, null);
        if (!branches.isEmpty()) {
            Set<Long> branchIds = branches.stream().map(Branch::getId).collect(Collectors.toSet());
            sales = sales.stream().filter(s -> s.getBranch() != null && branchIds.contains(s.getBranch().getId())).toList();
        }

        double totalSold = sales.stream().mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0).sum();
        double ivaCobrado = sales.stream().mapToDouble(s -> s.getTotalTax() != null ? s.getTotalTax() : 0).sum();
        int numTx = sales.size();
        double ticket = numTx > 0 ? totalSold / numTx : 0;

        kpiTotalLabel.setText(fmt(totalSold) + " MZN");
        kpiCountLabel.setText(String.valueOf(numTx));
        kpiTicketLabel.setText(fmt(ticket) + " MZN");
        kpiIvaLabel.setText(fmt(ivaCobrado) + " MZN");

        Set<Long> saleIds = sales.stream().map(Sale::getId).collect(Collectors.toSet());
        List<SaleItem> allItems = saleItemRepository.findBySaleDateRange(fromDt, toDt);
        double totalProfit = 0.0;
        for (SaleItem item : allItems) {
            if (item.getSale() == null || !saleIds.contains(item.getSale().getId())) continue;
            double lineTotal = item.getLineTotal() != null ? item.getLineTotal() : 0.0;
            double costBasis = 0.0;
            if (item.getProduct() != null && item.getProduct().getPriceCost() != null) {
                double qty = item.getQty() != null ? item.getQty() : 0.0;
                costBasis = item.getProduct().getPriceCost() * qty;
            }
            totalProfit += (lineTotal - costBasis);
        }
        kpiLucroLabel.setText(fmt(totalProfit) + " MZN");

        vendasChart.getData().clear();
        Map<LocalDate, Double> daily = new TreeMap<>();
        sales.stream()
                .filter(s -> s.getCreatedAt() != null && s.getTotal() != null)
                .forEach(s -> daily.merge(s.getCreatedAt().toLocalDate(), s.getTotal(), Double::sum));
        var series = new XYChart.Series<String, Number>();
        daily.forEach((d, v) -> series.getData().add(new XYChart.Data<>(
                d.format(DateTimeFormatter.ofPattern("dd/MM")), v)));
        vendasChart.getData().setAll(List.of(series));
    }

    // ── TAB 2: PRODUTOS EM FALTA ─────────────────────

    private Tab buildProdutosEmFaltaTab() {
        Tab tab = new Tab("⚠️  Artigos para Reposição");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        TableView<LowStockRow> table = new TableView<>();
        table.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<LowStockRow, String> c1 = col("Código", 100);
        TableColumn<LowStockRow, String> c2 = col("Produto", 220);
        TableColumn<LowStockRow, String> c3 = col("Categoria", 140);
        TableColumn<LowStockRow, String> c4 = col("Stock Actual", 110);
        TableColumn<LowStockRow, String> c5 = col("Mínimo", 90);
        TableColumn<LowStockRow, String> c6 = col("Filial", 140);

        c1.setCellValueFactory(r -> sv(r.getValue().getCode()));
        c2.setCellValueFactory(r -> sv(r.getValue().getName()));
        c3.setCellValueFactory(r -> sv(r.getValue().getCategory()));
        c4.setCellValueFactory(r -> sv(r.getValue().getStockCurrent()));
        c5.setCellValueFactory(r -> sv(r.getValue().getStockMin()));
        c6.setCellValueFactory(r -> sv(r.getValue().getBranch()));

        table.getColumns().addAll(List.of(c1, c2, c3, c4, c5, c6));
        table.setPlaceholder(new Label("Nenhum produto em falta"));

        Label count = new Label("0 produtos em falta");
        count.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");

        Button refresh = btn("Actualizar", "#2563EB");
        refresh.setOnAction(e -> {
            List<LowStockRow> r = buildLowStockRows();
            table.setItems(FXCollections.observableArrayList(r));
            count.setText(r.size() + " produtos em falta");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8);
        toolbar.getChildren().addAll(count, spacer, refresh);

        // Load initial data
        List<LowStockRow> initial = buildLowStockRows();
        table.setItems(FXCollections.observableArrayList(initial));
        count.setText(initial.size() + " produtos em falta");

        VBox card = card("PRODUTOS COM STOCK INSUFICIENTE", new VBox(toolbar, table));

        content.getChildren().addAll(card);
        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    /** Builds LowStockRow items: artigos físicos esgotados, abaixo do mínimo ou sem ficha. */
    private List<LowStockRow> buildLowStockRows() {
        List<LowStockRow> rows = new ArrayList<>();
        for (com.sgv.service.StockBranchService.StockAlert alert : stockBranchService.listStockAlerts(null)) {
            Product p = alert.product();
            if (p == null) continue;
            rows.add(new LowStockRow(
                    p.getCode(),
                    p.getName(),
                    p.getCategory() != null ? p.getCategory().getName() : "—",
                    fmtD(alert.current()),
                    fmtD(alert.min()),
                    alert.branch() != null ? alert.branch().getName() : "—"
            ));
        }
        return rows;
    }

    // ── TAB 3: MAIS VENDIDOS ────────────────────────────

    private Tab buildMaisVendidosTab() {
        Tab tab = new Tab("🔥  Artigos Mais Vendidos");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setLegendVisible(false);
        chart.setPrefHeight(220);

        TableView<TopSoldRow> table = new TableView<>();
        table.setPrefHeight(200);
        table.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<TopSoldRow, String> tc1 = col("#", 40);
        TableColumn<TopSoldRow, String> tc2 = col("Código", 100);
        TableColumn<TopSoldRow, String> tc3 = col("Produto", 280);
        TableColumn<TopSoldRow, String> tc4 = col("Qtd. Vendida", 120);
        TableColumn<TopSoldRow, String> tc5 = col("Receita", 130);
        tc1.setCellValueFactory(r -> sv(r.getValue().getPos()));
        tc2.setCellValueFactory(r -> sv(r.getValue().getCode()));
        tc3.setCellValueFactory(r -> sv(r.getValue().getName()));
        tc4.setCellValueFactory(r -> sv(r.getValue().getQty()));
        tc5.setCellValueFactory(r -> sv(r.getValue().getRevenue()));
        table.getColumns().addAll(List.of(tc1, tc2, tc3, tc4, tc5));

        ComboBox<String> periodCombo = new ComboBox<>();
        UiUtils.hardenComboBox(periodCombo);
        periodCombo.setItems(FXCollections.observableArrayList("Hoje","Esta Semana","Este Mês","Este Ano"));
        periodCombo.getSelectionModel().select("Este Mês");
        periodCombo.setPrefWidth(150);
        periodCombo.setOnAction(e -> refreshTopSold(periodCombo.getValue(), chart, table));

        // Toolbar with period selector
        HBox toolbar = new HBox(8);
        Label lbl = label("Período:", "12px");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(lbl, periodCombo, spacer);

        // Initial load
        refreshTopSold("Este Mês", chart, table);

        VBox card1 = card("TOP 10 MAIS VENDIDOS", new VBox(toolbar, chart));
        VBox card2 = card("DETALHE POR PRODUTO", new VBox(table));

        content.getChildren().addAll(card1, card2);
        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    private void refreshTopSold(String period, BarChart<String, Number> chart, TableView<TopSoldRow> table) {
        // Compute date range from period label
        LocalDate today = LocalDate.now();
        LocalDate from;
        switch (period) {
            case "Hoje":           from = today; break;
            case "Esta Semana":    from = today.minusDays(6); break;
            case "Este Mês":      from = today.withDayOfMonth(1); break;
            case "Este Ano":      from = today.withDayOfYear(1); break;
            default:              from = today.withDayOfMonth(1);
        }
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = today.atTime(23, 59, 59);

        List<SaleItem> items = saleItemRepository.findBySaleDateRange(fromDt, toDt);
        Map<Long, Double> qtyMap = new HashMap<>();
        Map<Long, Double> revMap = new HashMap<>();
        items.forEach(it -> {
            Long pid = it.getProduct() != null ? it.getProduct().getId() : null;
            if (pid == null) return;
            double q = it.getQuantity() != null ? it.getQuantity() : 0;
            double p = it.getUnitPrice() != null ? it.getUnitPrice() : 0;
            qtyMap.merge(pid, q, Double::sum);
            revMap.merge(pid, q * p, Double::sum);
        });
        List<Product> all = productRepository.findAllActive();
        List<TopSoldRow> rows = qtyMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).limit(10)
                .map(e -> {
                    Product p = all.stream().filter(x -> Objects.equals(x.getId(), e.getKey())).findFirst().orElse(null);
                    return new TopSoldRow(
                            0,
                            p != null ? p.getCode() : "?",
                            p != null ? p.getName() : "?",
                            Math.round(e.getValue()),
                            fmt(revMap.getOrDefault(e.getKey(), 0.0)) + " MZN"
                    );
                }).collect(Collectors.toList());
        // Re-number after sort
        int[] idx = {1};
        List<TopSoldRow> numbered = rows.stream().map(r ->
                new TopSoldRow(idx[0]++, r.getCode(), r.getName(), r.getQtyNum(), r.getRevenue())
        ).toList();
        table.setItems(FXCollections.observableArrayList(numbered));
        chart.getData().clear();
        var s = new XYChart.Series<String, Number>();
        numbered.stream().limit(8).forEach(r -> s.getData().add(new XYChart.Data<>(truncate(r.getName(), 16), r.getQtyNum())));
        chart.getData().setAll(List.of(s));
    }

    // ── TAB 4: RELATÓRIO IVA ──────────────────────────

    private Tab buildIvaTab() {
        Tab tab = new Tab("🏛️  Apuramento de IVA (16%)");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        ivaDe = new DatePicker(LocalDate.now().withDayOfMonth(1));
        ivaDe.setPrefWidth(130);
        ivaAte = new DatePicker(LocalDate.now());
        ivaAte.setPrefWidth(130);

        Button filtrar = btn("FILTRAR", "#2563EB");
        Button mes = btn("ESTE MÊS", "#1E40AF");
        mes.setOnAction(e -> {
            LocalDate t = LocalDate.now();
            ivaDe.setValue(t.withDayOfMonth(1));
            ivaAte.setValue(t.withDayOfMonth(t.lengthOfMonth()));
            loadIvaData();
        });
        Button ano = btn("ESTE ANO", "#0F172A");
        ano.setOnAction(e -> {
            LocalDate t = LocalDate.now();
            ivaDe.setValue(t.withDayOfYear(1));
            ivaAte.setValue(t.withDayOfYear(t.lengthOfYear()));
            loadIvaData();
        });

        Region spacerFilter = new Region();
        HBox.setHgrow(spacerFilter, Priority.ALWAYS);
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(
                label("De:", "12px"), ivaDe,
                label("Até:", "12px"), ivaAte,
                filtrar, spacerFilter, mes, ano
        );
        filtrar.setOnAction(e -> { ivaPage = 0; loadIvaData(); });

        HBox kpiH = new HBox(12);
        ivaBaseVal = new Label("0,00 MZN");
        ivaIsentoVal = new Label("0,00 MZN");
        ivaIvaVal  = new Label("0,00 MZN");
        ivaTotVal  = new Label("0,00 MZN");
        kpiH.getChildren().addAll(
            kpiCard("Base Tributável (16%)",     ivaBaseVal,   "#2563EB"),
            kpiCard("Base Isenta (Art. 9 CIVA)", ivaIsentoVal, "#64748B"),
            kpiCard("IVA Liquidado (16%)",       ivaIvaVal,    "#F59E0B"),
            kpiCard("Total c/ IVA",              ivaTotVal,    "#10B981")
        );

        ivaTable = new TableView<>();
        ivaTable.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        ivaTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        ivaTable.setPlaceholder(new Label("Nenhum documento neste período"));

        TableColumn<Sale, String> col1 = col("Documento", 120);
        TableColumn<Sale, String> col2 = col("Data", 100);
        TableColumn<Sale, String> col3 = col("Cliente", 220);
        TableColumn<Sale, String> col4 = col("Base Incidência", 130);
        TableColumn<Sale, String> col5 = col("IVA (16%)", 110);
        TableColumn<Sale, String> col6 = col("Total Factura", 120);
        col1.setCellValueFactory(c -> sv((c.getValue().getDocumentType() != null ? c.getValue().getDocumentType() : "FT") + " " + c.getValue().getSeries() + "/" + c.getValue().getDocumentNumber()));
        col2.setCellValueFactory(c -> sv(c.getValue().getCreatedAt() != null
                ? c.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—"));
        col3.setCellValueFactory(c -> sv(c.getValue().getCustomerName() != null
                ? c.getValue().getCustomerName() : "Consumidor Final"));
        col4.setCellValueFactory(c -> sv(fmtD(c.getValue().getSubtotal()) + " MT"));
        col5.setCellValueFactory(c -> sv(fmtD(c.getValue().getTotalTax()) + " MT"));
        col6.setCellValueFactory(c -> sv(fmtD(c.getValue().getTotal()) + " MT"));
        ivaTable.getColumns().addAll(List.of(col1, col2, col3, col4, col5, col6));

        HBox toolbar = new HBox(8);
        Label title = new Label("DOCUMENTOS FISCAIS EMITIDOS");
        title.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#475569; -fx-font-family:monospace;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button export = btn("Exportar SAF-T MZ (XML)", "#2563EB");
        export.setOnAction(e -> {
            try {
                LocalDate from = ivaDe.getValue() != null ? ivaDe.getValue() : LocalDate.now().withDayOfMonth(1);
                LocalDate to = ivaAte.getValue() != null ? ivaAte.getValue() : LocalDate.now();
                LocalDateTime fromDt = from.atStartOfDay();
                LocalDateTime toDt = to.atTime(23, 59, 59);
                List<Sale> sales = saleRepository.findByDateRangeAndState(fromDt, toDt, null).stream()
                        .filter(s -> s.getCreatedAt() != null)
                        .sorted(Comparator.comparing(Sale::getCreatedAt).reversed())
                        .toList();
                java.nio.file.Path outputPath = java.nio.file.Paths.get("backups", "saf_t_mz_" + from + "_a_" + to + ".xml");
                java.nio.file.Files.createDirectories(outputPath.toAbsolutePath().getParent());
                safTExportService.exportSalesToXml(sales, outputPath);
                showAlert("Ficheiro SAF-T MZ 1.01 gerado com sucesso em:\n" + outputPath.toAbsolutePath());
            } catch (Exception ex) {
                showAlert("Não foi possível exportar SAF-T: " + ex.getMessage());
            }
        });
        ivaCountLabel = new Label("0 documentos");
        ivaCountLabel.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");
        toolbar.getChildren().addAll(title, ivaCountLabel, spacer, export);

        ivaPrevBtn = btn("← Anterior", "#2563EB");
        ivaNextBtn = btn("Próximo →", "#2563EB");
        ivaPageLabel = new Label("Página 1 de 1");
        ivaPageLabel.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");
        ivaPrevBtn.setOnAction(e -> { ivaPage--; loadIvaData(); });
        ivaNextBtn.setOnAction(e -> { ivaPage++; loadIvaData(); });
        HBox ivaPagination = new HBox(10);
        ivaPagination.setAlignment(javafx.geometry.Pos.CENTER);
        ivaPagination.getChildren().addAll(ivaPrevBtn, ivaPageLabel, ivaNextBtn);

        loadIvaData();

        VBox card0 = card("FILTRO DE PERÍODO FISCAL", filterBar);
        VBox card1 = card("RESUMO DO APURAMENTO DE IVA (MODELO 19)", kpiH);
        VBox card2 = card("", new VBox(toolbar, ivaTable, ivaPagination));

        content.getChildren().addAll(card0, card1, card2);
        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    private void loadIvaData() {
        LocalDate from = ivaDe != null && ivaDe.getValue() != null ? ivaDe.getValue() : LocalDate.now().withDayOfMonth(1);
        LocalDate to = ivaAte != null && ivaAte.getValue() != null ? ivaAte.getValue() : LocalDate.now();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        List<Sale> allPeriodSales = saleRepository.findByDateRangeAndState(fromDt, toDt, null).stream()
                .filter(s -> !"ANULADA".equalsIgnoreCase(s.getState()))
                .toList();

        double baseTributavel = 0.0;
        double baseIsenta = 0.0;
        double ivaCobrado = 0.0;
        double totalFacturado = 0.0;

        for (Sale s : allPeriodSales) {
            double sTotal = s.getTotal() != null ? s.getTotal() : 0.0;
            double sTax = s.getTotalTax() != null ? s.getTotalTax() : 0.0;
            double sSub = s.getSubtotal() != null ? s.getSubtotal() : 0.0;
            totalFacturado += sTotal;
            ivaCobrado += sTax;
            if (sTax > 0.001) {
                baseTributavel += sSub;
            } else {
                baseIsenta += sSub;
            }
        }

        ivaBaseVal.setText(fmt(baseTributavel) + " MZN");
        if (ivaIsentoVal != null) ivaIsentoVal.setText(fmt(baseIsenta) + " MZN");
        ivaIvaVal.setText(fmt(ivaCobrado) + " MZN");
        ivaTotVal.setText(fmt(totalFacturado) + " MZN");

        int totalSize = allPeriodSales.size();
        int totalPages = Math.max(1, (totalSize + PAGE_SIZE - 1) / PAGE_SIZE);
        if (ivaPage < 0) ivaPage = 0;
        if (ivaPage >= totalPages) ivaPage = totalPages - 1;

        List<Sale> pageSales = allPeriodSales.stream()
                .skip((long) ivaPage * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .toList();

        ivaTable.setItems(FXCollections.observableArrayList(pageSales));
        ivaCountLabel.setText(totalSize + " documentos");

        updatePaginationButtons(ivaPageLabel, ivaPrevBtn, ivaNextBtn, ivaPage, totalPages);
    }

    // ── TAB 5: CONTAS A RECEBER ─────────────────────

    private Tab buildAccountsReceivableTab() {
        Tab tab = new Tab("👥  Contas Correntes / Devedores");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        DatePicker fromDate = new DatePicker(LocalDate.now().minusMonths(1));
        fromDate.setPrefWidth(130);
        DatePicker toDate = new DatePicker(LocalDate.now());
        toDate.setPrefWidth(130);
        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar cliente ou documento...");
        searchField.setPrefWidth(260);

        Button refresh = btn("Filtrar", "#2563EB");
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(
                label("De:", "12px"), fromDate,
                label("Até:", "12px"), toDate,
                label("Cliente / Documento:", "12px"), searchField,
                refresh);

        Label totalDueLabel = new Label("0,00 MT");
        Label overdue30Label = new Label("0,00 MT");
        Label overdue60Label = new Label("0,00 MT");
        Label overdue90Label = new Label("0,00 MT");
        HBox kpiH = new HBox(12);
        kpiH.getChildren().addAll(
                kpiCard("Total em Aberto", totalDueLabel, "#2563EB"),
                kpiCard("Vencido >30d", overdue30Label, "#F59E0B"),
                kpiCard("Vencido >60d", overdue60Label, "#EA580C"),
                kpiCard("Vencido >90d", overdue90Label, "#DC2626")
        );

        TableView<ReceivableRow> table = new TableView<>();
        table.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Nenhuma conta a receber encontrada"));

        TableColumn<ReceivableRow, String> c1 = col("Documento", 130);
        TableColumn<ReceivableRow, String> c2 = col("Cliente", 220);
        TableColumn<ReceivableRow, String> c3 = col("Data", 120);
        TableColumn<ReceivableRow, String> c4 = col("Total", 110);
        TableColumn<ReceivableRow, String> c5 = col("Pago", 110);
        TableColumn<ReceivableRow, String> c6 = col("Saldo", 110);
        TableColumn<ReceivableRow, String> c7 = col("Dias em Aberto", 110);
        TableColumn<ReceivableRow, String> c8 = col("Faixa", 130);
        c1.setCellValueFactory(r -> r.getValue().document);
        c2.setCellValueFactory(r -> r.getValue().customer);
        c3.setCellValueFactory(r -> r.getValue().date);
        c4.setCellValueFactory(r -> r.getValue().total);
        c5.setCellValueFactory(r -> r.getValue().paid);
        c6.setCellValueFactory(r -> r.getValue().due);
        c7.setCellValueFactory(r -> r.getValue().daysOpen);
        c8.setCellValueFactory(r -> r.getValue().bucket);
        table.getColumns().addAll(List.of(c1, c2, c3, c4, c5, c6, c7, c8));

        Runnable loadAging = () -> {
            LocalDate from = fromDate.getValue() != null ? fromDate.getValue() : LocalDate.now().minusMonths(1);
            LocalDate to = toDate.getValue() != null ? toDate.getValue() : LocalDate.now();
            String filter = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";
            Map<String, Object> aging = reportService.getAccountsReceivableAging(from, to);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mapRows = (List<Map<String, Object>>) aging.get("rows");
            List<ReceivableRow> rows = mapRows.stream()
                    .map(row -> new ReceivableRow(
                            String.valueOf(row.get("document")),
                            String.valueOf(row.get("customer")),
                            String.valueOf(row.get("date")),
                            String.valueOf(row.get("total")),
                            String.valueOf(row.get("paid")),
                            String.valueOf(row.get("due")),
                            String.valueOf(row.get("daysOpen")),
                            String.valueOf(row.get("bucket"))
                    ))
                    .toList();
            if (!filter.isBlank()) {
                rows = rows.stream()
                        .filter(r -> r.document.getValue().toLowerCase().contains(filter)
                                || r.customer.getValue().toLowerCase().contains(filter))
                        .toList();
            }
            table.setItems(FXCollections.observableArrayList(rows));
            totalDueLabel.setText(aging.get("totalDue") + " MT");
            overdue30Label.setText(aging.get("overdue30") + " MT");
            overdue60Label.setText(aging.get("overdue60") + " MT");
            overdue90Label.setText(aging.get("overdue90") + " MT");
        };

        refresh.setOnAction(e -> loadAging.run());

        VBox card = card("CONTAS A RECEBER", new VBox(filterBar, kpiH, table));
        content.getChildren().addAll(card);
        sp.setContent(content);
        tab.setContent(sp);

        loadAging.run();
        return tab;
    }

    private static class ReceivableRow {
        private final SimpleStringProperty document;
        private final SimpleStringProperty customer;
        private final SimpleStringProperty date;
        private final SimpleStringProperty total;
        private final SimpleStringProperty paid;
        private final SimpleStringProperty due;
        private final SimpleStringProperty daysOpen;
        private final SimpleStringProperty bucket;

        ReceivableRow(String document, String customer, String date, String total, String paid, String due, String daysOpen, String bucket) {
            this.document = new SimpleStringProperty(document);
            this.customer = new SimpleStringProperty(customer);
            this.date = new SimpleStringProperty(date);
            this.total = new SimpleStringProperty(total);
            this.paid = new SimpleStringProperty(paid);
            this.due = new SimpleStringProperty(due);
            this.daysOpen = new SimpleStringProperty(daysOpen);
            this.bucket = new SimpleStringProperty(bucket);
        }
    }

    // ── TAB 6: MOVIMENTOS DE ESTOQUE ─────────────────

    private Tab buildStockMovimentosTab() {
        Tab tab = new Tab("📦  Extrato de Movimentos (Kardex)");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        movementsTable = new TableView<>();
        movementsTable.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        movementsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        movementsTable.setPlaceholder(new Label("Nenhum movimento encontrado"));

        TableColumn<StockMovementRow, String> sc1 = col("Data / Hora", 140);
        TableColumn<StockMovementRow, String> sc2 = col("Produto", 200);
        TableColumn<StockMovementRow, String> sc3 = col("Tipo", 90);
        TableColumn<StockMovementRow, String> sc4 = col("Qtd.", 80);
        TableColumn<StockMovementRow, String> sc5 = col("Stock Antes", 100);
        TableColumn<StockMovementRow, String> sc6 = col("Stock Depois", 100);
        TableColumn<StockMovementRow, String> sc7 = col("Filial", 120);
        TableColumn<StockMovementRow, String> sc8 = col("Utilizador", 110);
        sc1.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        sc2.setCellValueFactory(new PropertyValueFactory<>("productName"));
        sc3.setCellValueFactory(new PropertyValueFactory<>("type"));
        sc4.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        sc5.setCellValueFactory(new PropertyValueFactory<>("stockBefore"));
        sc6.setCellValueFactory(new PropertyValueFactory<>("stockAfter"));
        sc7.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        sc8.setCellValueFactory(new PropertyValueFactory<>("userName"));
        movementsTable.getColumns().addAll(List.of(sc1, sc2, sc3, sc4, sc5, sc6, sc7, sc8));

        movementsPrevBtn = btn("← Anterior", "#2563EB");
        movementsNextBtn = btn("Próximo →", "#2563EB");
        movementsPageLabel = new Label("Página 1 de 1");
        movementsPageLabel.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");
        movementsPrevBtn.setOnAction(e -> { movementsPage--; loadMovementsData(); });
        movementsNextBtn.setOnAction(e -> { movementsPage++; loadMovementsData(); });
        HBox movementsPagination = new HBox(10);
        movementsPagination.setAlignment(javafx.geometry.Pos.CENTER);
        movementsPagination.getChildren().addAll(movementsPrevBtn, movementsPageLabel, movementsNextBtn);

        HBox toolbar = new HBox(8);
        movementsCountLabel = new Label("0 movimentos");
        movementsCountLabel.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(movementsCountLabel, spacer);

        loadMovementsData();

        VBox card = card("HISTÓRICO DE MOVIMENTOS", new VBox(toolbar, movementsTable, movementsPagination));
        content.getChildren().addAll(card);
        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    private void loadMovementsData() {
        Pageable pageable = PageRequest.of(movementsPage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockMovement> page = stockMovementRepository.findAll(pageable);

        List<StockMovementRow> rows = page.getContent().stream()
                .map(sm -> new StockMovementRow(
                        sm.getCreatedAt() != null ? sm.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—",
                        sm.getProduct() != null ? sm.getProduct().getName() : "—",
                        sm.getType() != null ? sm.getType() : "—",
                        fmtD(sm.getQty()),
                        fmtD(sm.getStockBefore()),
                        fmtD(sm.getStockAfter()),
                        sm.getBranch() != null ? sm.getBranch().getName() : "—",
                        sm.getUser() != null ? sm.getUser().getUsername() : "—"
                ))
                .collect(Collectors.toList());
        movementsTable.setItems(FXCollections.observableArrayList(rows));
        movementsCountLabel.setText(page.getTotalElements() + " movimentos");

        updatePaginationButtons(movementsPageLabel, movementsPrevBtn, movementsNextBtn, movementsPage, page.getTotalPages());
    }

    // ── TAB 6: MATRIZ DE STOCK ──────────────────────

    private Tab buildStockMatrixTab() {
        Tab tab = new Tab("🏢  Stock Central por Armazém");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        // Determine actual branch names dynamically
        List<Branch> allBranches = branchRepository.findAll();
        List<String> branchNames = allBranches.stream().map(Branch::getName).filter(Objects::nonNull).toList();

        // Build column headers from real branch names (max 4 shown)
        TableView<StockMatrixRow> table = new TableView<>();
        table.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Nenhum produto"));

        TableColumn<StockMatrixRow, String> mc1 = col("Código", 100);
        TableColumn<StockMatrixRow, String> mc2 = col("Produto", 200);
        TableColumn<StockMatrixRow, String> mc3 = col("Categoria", 130);
        mc1.setCellValueFactory(new PropertyValueFactory<>("code"));
        mc2.setCellValueFactory(new PropertyValueFactory<>("name"));
        mc3.setCellValueFactory(new PropertyValueFactory<>("category"));

        // Dynamic branch columns (up to 4)
        List<TableColumn<StockMatrixRow, String>> branchCols = new ArrayList<>();
        for (int i = 0; i < Math.min(branchNames.size(), 4); i++) {
            final int idx = i;
            TableColumn<StockMatrixRow, String> bc = col(branchNames.get(i), 90);
            bc.setCellValueFactory(r -> sv(r.getValue().getBranchQty().size() > idx ? r.getValue().getBranchQty().get(idx) : "—"));
            branchCols.add(bc);
        }

        TableColumn<StockMatrixRow, String> mcTotal = col("Total", 90);
        mcTotal.setCellValueFactory(new PropertyValueFactory<>("totalQty"));

        table.getColumns().addAll(List.of(mc1, mc2, mc3));
        table.getColumns().addAll(branchCols);
        table.getColumns().add(mcTotal);

        // Combo to filter by specific branch
        ComboBox<String> branchFilterCombo = new ComboBox<>();
        UiUtils.hardenComboBox(branchFilterCombo);
        List<String> filterOpts = new ArrayList<>();
        filterOpts.add("Todas as Filiais");
        filterOpts.addAll(branchNames);
        branchFilterCombo.setItems(FXCollections.observableArrayList(filterOpts));
        branchFilterCombo.getSelectionModel().selectFirst();
        branchFilterCombo.setPrefWidth(180);

        Label count = new Label("0 produtos");
        count.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");

        Button refresh = btn("Actualizar", "#2563EB");
        refresh.setOnAction(e -> {
            List<StockMatrixRow> rows = buildStockMatrixRows(
                    branchFilterCombo.getValue(), allBranches, branchNames);
            table.setItems(FXCollections.observableArrayList(rows));
            count.setText(rows.size() + " produtos");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8);
        toolbar.getChildren().addAll(count, spacer, label("Filtrar filial:", "12px"), branchFilterCombo, refresh);

        // Initial load
        List<StockMatrixRow> initial = buildStockMatrixRows(null, allBranches, branchNames);
        table.setItems(FXCollections.observableArrayList(initial));
        count.setText(initial.size() + " produtos");

        VBox card = card("VISÃO DE STOCK POR FILIAL", new VBox(toolbar, table));
        content.getChildren().addAll(card);
        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    /**
     * Build StockMatrixRow list: for each product, get stockCurrent per branch.
     * If branchFilter != null and != "Todas as Filiais", show only that branch's stock.
     */
    private List<StockMatrixRow> buildStockMatrixRows(String branchFilter, List<Branch> allBranches, List<String> branchNames) {
        List<Product> products = productRepository.findAllActive();

        // Pre-load all stock_branch records keyed by (productId, branchId)
        Map<Long, Map<Long, BigDecimal>> stockMap = new HashMap<>();
        for (StockBranch sb : stockBranchService.findAll()) {
            if (sb.getProduct() == null || sb.getBranch() == null) continue;
            stockMap.computeIfAbsent(sb.getProduct().getId(), k -> new HashMap<>())
                    .put(sb.getBranch().getId(), sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO);
        }

        List<StockMatrixRow> rows = new ArrayList<>();
        for (Product p : products) {
            if (Boolean.TRUE.equals(p.getService())) continue; // skip services

            List<String> qtyByBranch = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            if (branchFilter != null && !"Todas as Filiais".equals(branchFilter)) {
                // Show only the selected branch
                Branch selectedBranch = allBranches.stream()
                        .filter(b -> branchFilter.equals(b.getName()))
                        .findFirst().orElse(null);
                if (selectedBranch != null) {
                    BigDecimal qty = stockMap.getOrDefault(p.getId(), Collections.emptyMap())
                            .getOrDefault(selectedBranch.getId(), BigDecimal.ZERO);
                    qtyByBranch.add(fmtD(qty));
                    total = qty;
                } else {
                    qtyByBranch.add("—");
                }
            } else {
                // Show all branches
                for (Branch b : allBranches) {
                    BigDecimal qty = stockMap.getOrDefault(p.getId(), Collections.emptyMap())
                            .getOrDefault(b.getId(), BigDecimal.ZERO);
                    qtyByBranch.add(fmtD(qty));
                    total = total.add(qty);
                }
            }

            rows.add(new StockMatrixRow(
                    p.getCode(),
                    p.getName(),
                    p.getCategory() != null ? p.getCategory().getName() : "—",
                    qtyByBranch,
                    fmtD(total)
            ));
        }
        return rows;
    }

    // ── TAB 7: TRANSFERÊNCIAS ────────────────────────

    private Tab buildTransferenciasTab() {
        Tab tab = new Tab("🔁  Guias de Transferência");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        TableView<TransferRow> table = new TableView<>();
        table.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Nenhuma transferência"));

        TableColumn<TransferRow, String> tc1 = col("Nº", 100);
        TableColumn<TransferRow, String> tc2 = col("Data", 100);
        TableColumn<TransferRow, String> tc3 = col("Origem", 130);
        TableColumn<TransferRow, String> tc4 = col("Destino", 130);
        TableColumn<TransferRow, String> tc5 = col("Produto", 200);
        TableColumn<TransferRow, String> tc6 = col("Qtd.", 80);
        TableColumn<TransferRow, String> tc7 = col("Estado", 90);
        tc1.setCellValueFactory(new PropertyValueFactory<>("transferNumber"));
        tc2.setCellValueFactory(new PropertyValueFactory<>("date"));
        tc3.setCellValueFactory(new PropertyValueFactory<>("origin"));
        tc4.setCellValueFactory(new PropertyValueFactory<>("destination"));
        tc5.setCellValueFactory(new PropertyValueFactory<>("productName"));
        tc6.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        tc7.setCellValueFactory(new PropertyValueFactory<>("status"));
        table.getColumns().addAll(List.of(tc1, tc2, tc3, tc4, tc5, tc6, tc7));

        // Combo to filter by status
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList("Todas","PENDING","APPROVED","COMPLETED","REJECTED","CANCELLED"));
        statusCombo.getSelectionModel().selectFirst();
        statusCombo.setPrefWidth(140);
        statusCombo.setOnAction(e -> {
            List<TransferRow> rows = loadTransferRows(statusCombo.getValue());
            table.setItems(FXCollections.observableArrayList(rows));
        });

        Button refresh = btn("Actualizar", "#2563EB");
        refresh.setOnAction(e -> {
            List<TransferRow> rows = loadTransferRows(statusCombo.getValue());
            table.setItems(FXCollections.observableArrayList(rows));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(8);
        toolbar.getChildren().addAll(label("Estado:", "12px"), statusCombo, spacer, refresh);

        // Initial load
        List<TransferRow> initial = loadTransferRows("Todas");
        table.setItems(FXCollections.observableArrayList(initial));

        VBox card = card("TRANSFERÊNCIAS ENTRE FILIAIS", new VBox(toolbar, table));
        content.getChildren().addAll(card);
        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    private List<TransferRow> loadTransferRows(String statusFilter) {
        List<Transfer> transfers = "Todas".equals(statusFilter) || statusFilter == null
                ? transferRepository.findAll()
                : transferRepository.findByStatus(statusFilter);

        List<TransferRow> rows = new ArrayList<>();
        for (Transfer t : transfers) {
            // Show first item as representative; subsequent items get empty rows
            if (t.getItems() == null || t.getItems().isEmpty()) {
                rows.add(new TransferRow(
                        "TR-" + t.getDocumentNumber(),
                        t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—",
                        t.getSourceBranch() != null ? t.getSourceBranch().getName() : "—",
                        t.getDestinationBranch() != null ? t.getDestinationBranch().getName() : "—",
                        "—", "—",
                        t.getStatus() != null ? t.getStatus() : "—"
                ));
            } else {
                boolean first = true;
                for (TransferItem ti : t.getItems()) {
                    rows.add(new TransferRow(
                            first ? "TR-" + t.getDocumentNumber() : "",
                            first ? (t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—") : "",
                            first ? (t.getSourceBranch() != null ? t.getSourceBranch().getName() : "—") : "",
                            first ? (t.getDestinationBranch() != null ? t.getDestinationBranch().getName() : "—") : "",
                            ti.getProduct() != null ? ti.getProduct().getName() : "—",
                            fmtD(ti.getQuantity()),
                            first ? (t.getStatus() != null ? t.getStatus() : "—") : ""
                    ));
                    first = false;
                }
            }
        }
        return rows;
    }

    // ── TAB 8: PAGAMENTOS A FORNECEDORES ──────────────

    private Tab buildPagamentosFornecedoresTab() {
        Tab tab = new Tab("📑  Pagamentos a Fornecedores");

        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:#F8FAFC;");

        VBox content = new VBox(16);
        content.setStyle("-fx-padding:20;");
        content.setMaxWidth(1100);

        pagDe = new DatePicker(LocalDate.now().withDayOfMonth(1));
        pagDe.setPrefWidth(130);
        pagAte = new DatePicker(LocalDate.now());
        pagAte.setPrefWidth(130);

        pagFornecedorCombo = new ComboBox<>();
        UiUtils.hardenComboBox(pagFornecedorCombo);
        List<String> fornecedores = new ArrayList<>();
        fornecedores.add("Todos os Fornecedores");
        supplierRepository.findAllByOrderByNameAsc().forEach(s -> {
            if (s.getName() != null) fornecedores.add(s.getName());
        });
        pagFornecedorCombo.setItems(FXCollections.observableArrayList(fornecedores));
        pagFornecedorCombo.getSelectionModel().selectFirst();
        pagFornecedorCombo.setPrefWidth(200);

        Button filtrar = btn("FILTRAR", "#2563EB");
        Button mes = btn("ESTE MÊS", "#1E40AF");
        mes.setOnAction(e -> {
            LocalDate t = LocalDate.now();
            pagDe.setValue(t.withDayOfMonth(1));
            pagAte.setValue(t.withDayOfMonth(t.lengthOfMonth()));
            loadPagamentosData();
        });
        Button ano = btn("ESTE ANO", "#0F172A");
        ano.setOnAction(e -> {
            LocalDate t = LocalDate.now();
            pagDe.setValue(t.withDayOfYear(1));
            pagAte.setValue(t.withDayOfYear(t.lengthOfYear()));
            loadPagamentosData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filterBar.getChildren().addAll(
                label("De:", "12px"), pagDe,
                label("Até:", "12px"), pagAte,
                label("Fornecedor:", "12px"), pagFornecedorCombo,
                filtrar, spacer, mes, ano
        );
        filtrar.setOnAction(e -> loadPagamentosData());

        pagKpiTotalLabel = new Label("0,00 MZN");
        pagKpiCountLabel = new Label("0");
        pagKpiDebtLabel = new Label("0,00 MZN");
        HBox kpiH = new HBox(12);
        kpiH.getChildren().addAll(
                kpiCard("Total Pago no Período", pagKpiTotalLabel, "#10B981"),
                kpiCard("Nº de Pagamentos",       pagKpiCountLabel,  "#2563EB"),
                kpiCard("Total em Dívida",        pagKpiDebtLabel,   "#DC2626")
        );

        pagTable = new TableView<>();
        pagTable.setStyle("-fx-font-size:13px; -fx-background-color:#ffffff;");
        pagTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        pagTable.setPlaceholder(new Label("Nenhum pagamento a fornecedor neste período"));

        TableColumn<SupplierPaymentRow, String> pc1 = col("Data", 140);
        TableColumn<SupplierPaymentRow, String> pc2 = col("Fornecedor", 220);
        TableColumn<SupplierPaymentRow, String> pc3 = col("Compra", 130);
        TableColumn<SupplierPaymentRow, String> pc4 = col("Valor", 120);
        TableColumn<SupplierPaymentRow, String> pc5 = col("Método", 170);
        TableColumn<SupplierPaymentRow, String> pc6 = col("Referência", 160);
        pc1.setCellValueFactory(r -> sv(r.getValue().getData()));
        pc2.setCellValueFactory(r -> sv(r.getValue().getFornecedor()));
        pc3.setCellValueFactory(r -> sv(r.getValue().getCompra()));
        pc4.setCellValueFactory(r -> sv(r.getValue().getValor()));
        pc5.setCellValueFactory(r -> sv(r.getValue().getMetodo()));
        pc6.setCellValueFactory(r -> sv(r.getValue().getReferencia()));
        pagTable.getColumns().addAll(List.of(pc1, pc2, pc3, pc4, pc5, pc6));

        pagPrevBtn = btn("← Anterior", "#2563EB");
        pagNextBtn = btn("Próximo →", "#2563EB");
        pagPageLabel = new Label("Página 1 de 1");
        pagPageLabel.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");
        pagPrevBtn.setOnAction(e -> { pagPage--; loadPagamentosData(); });
        pagNextBtn.setOnAction(e -> { pagPage++; loadPagamentosData(); });
        HBox pagination = new HBox(10);
        pagination.setAlignment(javafx.geometry.Pos.CENTER);
        pagination.getChildren().addAll(pagPrevBtn, pagPageLabel, pagNextBtn);

        pagCountLabel = new Label("0 pagamentos");
        pagCountLabel.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#475569;");
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox toolbar = new HBox(8);
        toolbar.getChildren().addAll(pagCountLabel, spacer2);

        VBox card1 = card("RESUMO DE PAGAMENTOS", kpiH);
        VBox card2 = card("", new VBox(toolbar, pagTable, pagination));
        VBox card3 = card("FILTRO DE PERÍODO", filterBar);

        content.getChildren().addAll(card3, card1, card2);
        sp.setContent(content);
        tab.setContent(sp);

        loadPagamentosData();
        return tab;
    }

    private void loadPagamentosData() {
        LocalDate from = pagDe.getValue() != null ? pagDe.getValue() : LocalDate.now().withDayOfMonth(1);
        LocalDate to = pagAte.getValue() != null ? pagAte.getValue() : LocalDate.now();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(23, 59, 59);

        List<SupplierPayment> all = supplierPaymentRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(fromDt, toDt);
        String supplierFilter = pagFornecedorCombo.getValue();
        if (supplierFilter != null && !"Todos os Fornecedores".equals(supplierFilter)) {
            all = all.stream()
                    .filter(p -> p.getSupplier() != null && supplierFilter.equals(p.getSupplier().getName()))
                    .toList();
        }

        double total = all.stream()
                .mapToDouble(p -> p.getAmountValue() != null ? p.getAmountValue().doubleValue() : 0)
                .sum();
        pagKpiTotalLabel.setText(fmt(total) + " MZN");
        pagKpiCountLabel.setText(String.valueOf(all.size()));

        BigDecimal debt = BigDecimal.ZERO;
        for (Object[] row : purchaseRepository.findOutstandingBalanceBySupplier()) {
            if (row == null || row.length < 2 || row[1] == null) continue;
            if (row[1] instanceof BigDecimal bd) {
                debt = debt.add(bd);
            } else if (row[1] instanceof Number n) {
                debt = debt.add(BigDecimal.valueOf(n.doubleValue()));
            }
        }
        pagKpiDebtLabel.setText(fmt(debt) + " MZN");

        int totalSize = all.size();
        int totalPages = Math.max(1, (totalSize + PAGE_SIZE - 1) / PAGE_SIZE);
        if (pagPage < 0) pagPage = 0;
        if (pagPage >= totalPages) pagPage = totalPages - 1;
        List<SupplierPayment> page = all.stream()
                .skip((long) pagPage * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .toList();

        List<SupplierPaymentRow> rows = page.stream().map(p -> new SupplierPaymentRow(
                p.getCreatedAt() != null ? p.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—",
                p.getSupplier() != null ? p.getSupplier().getName() : "—",
                p.getPurchase() != null
                        ? (p.getPurchase().getInvoiceNumber() != null ? p.getPurchase().getInvoiceNumber() : "Compra #" + p.getPurchase().getId())
                        : "—",
                p.getAmountValue() != null ? fmt(p.getAmountValue()) + " MZN" : "—",
                p.getMethod() != null ? p.getMethod() : "—",
                p.getReference() != null ? p.getReference() : "—"
        )).toList();
        pagTable.setItems(FXCollections.observableArrayList(rows));
        pagCountLabel.setText(totalSize + " pagamentos");

        updatePaginationButtons(pagPageLabel, pagPrevBtn, pagNextBtn, pagPage, totalPages);
    }

    // ══════════════════════════════════════════════════
    // REUSABLE UI FACTORIES (matching SGV design system)
    // ══════════════════════════════════════════════════

    /** White card with grey border + optional header label */
    private VBox card(String header, Node content) {
        VBox card = new VBox(header != null && !header.isEmpty() ? 12 : 0);
        card.setStyle(
            "-fx-background-color:#ffffff;" +
            "-fx-padding:20;" +
            "-fx-background-radius:10;" +
            "-fx-border-color:#E2E8F0;" +
            "-fx-border-width:1;" +
            "-fx-border-radius:10;");
        if (header != null && !header.isEmpty()) {
            Label h = new Label(header);
            h.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#475569; -fx-font-family:monospace;");
            card.getChildren().add(h);
        }
        if (content instanceof VBox) {
            VBox vb = (VBox) content;
            card.getChildren().add(vb);
        } else {
            card.getChildren().add(content);
        }
        return card;
    }

    /** Compact KPI card with coloured bottom accent */
    private VBox kpiCard(String title, Label value, String accentColor) {
        VBox box = new VBox(6);
        box.setStyle(
            "-fx-background-color:#ffffff;" +
            "-fx-padding:16;" +
            "-fx-background-radius:10;" +
            "-fx-border-color:#E2E8F0;" +
            "-fx-border-width:1;" +
            "-fx-border-radius:10;" +
            "-fx-border-insets:0 0 3 0;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:11px; -fx-font-weight:600; -fx-text-fill:#64748B;");
        value.setStyle("-fx-font-size:16px; -fx-font-weight:800; -fx-text-fill:#0F172A;");
        box.getChildren().addAll(t, value);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /** Small primary button */
    private Button btn(String text, String bgColor) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-text-fill:#ffffff;" +
            "-fx-font-weight:700;" +
            "-fx-font-size:12px;" +
            "-fx-padding:6 14;" +
            "-fx-background-radius:6;" +
            "-fx-cursor:hand;");
        UiUtils.applyHoverElevation(b);
        UiUtils.applyPressFeedback(b);
        return b;
    }

    /** Small label */
    private Label label(String text, String fontSize) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:" + fontSize + "; -fx-font-weight:600; -fx-text-fill:#475569;");
        return l;
    }

    /** Table column with width + cell factory */
    private <T> TableColumn<T, String> col(String title, int prefWidth) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setPrefWidth(prefWidth);
        return col;
    }

    private SimpleStringProperty sv(String v) {
        return new SimpleStringProperty(v != null ? v : "—");
    }

    // ══════════════════════════════════════════════════
    // DTOs
    // ══════════════════════════════════════════════════

    public static class TopSoldRow {
        private final String pos, code, name, qty, revenue;
        private final long qtyNum;
        public TopSoldRow(int pos, String code, String name, long qty, String revenue) {
            this.pos = String.valueOf(pos); this.code = code; this.name = name;
            this.qty = String.valueOf(qty); this.qtyNum = qty; this.revenue = revenue;
        }
        public String getPos()      { return pos; }
        public String getCode()    { return code; }
        public String getName()    { return name; }
        public String getQty()     { return qty; }
        public String getRevenue() { return revenue; }
        public long getQtyNum()   { return qtyNum; }
    }

    public static class StockMovementRow {
        private final String dateTime, productName, type, quantity, stockBefore, stockAfter, branchName, userName;
        public StockMovementRow(String dt, String pn, String t, String q, String sb, String sa, String bn, String un) {
            this.dateTime = dt; this.productName = pn; this.type = t; this.quantity = q;
            this.stockBefore = sb; this.stockAfter = sa; this.branchName = bn; this.userName = un;
        }
        public String getDateTime()    { return dateTime; }
        public String getProductName()  { return productName; }
        public String getType()         { return type; }
        public String getQuantity()     { return quantity; }
        public String getStockBefore() { return stockBefore; }
        public String getStockAfter()  { return stockAfter; }
        public String getBranchName()  { return branchName; }
        public String getUserName()    { return userName; }
    }

    public static class StockMatrixRow {
        private final String code, name, category, totalQty;
        private final List<String> branchQty; // dynamic per-branch quantities
        public StockMatrixRow(String code, String name, String cat, List<String> branchQty, String total) {
            this.code = code; this.name = name; this.category = cat;
            this.branchQty = branchQty; this.totalQty = total;
        }
        public String getCode()      { return code; }
        public String getName()      { return name; }
        public String getCategory()  { return category; }
        public List<String> getBranchQty() { return branchQty; }
        public String getTotalQty()  { return totalQty; }
    }

    public static class LowStockRow {
        private final String code, name, category, stockCurrent, stockMin, branch;
        public LowStockRow(String code, String name, String cat, String stock, String min, String branch) {
            this.code = code; this.name = name; this.category = cat;
            this.stockCurrent = stock; this.stockMin = min; this.branch = branch;
        }
        public String getCode()        { return code; }
        public String getName()        { return name; }
        public String getCategory()    { return category; }
        public String getStockCurrent(){ return stockCurrent; }
        public String getStockMin()   { return stockMin; }
        public String getBranch()      { return branch; }
    }

    public static class TransferRow {
        private final String transferNumber, date, origin, destination, productName, quantity, status;
        public TransferRow(String tn, String d, String o, String dt, String pn, String q, String s) {
            this.transferNumber = tn; this.date = d; this.origin = o;
            this.destination = dt; this.productName = pn; this.quantity = q; this.status = s;
        }
        public String getTransferNumber() { return transferNumber; }
        public String getDate()          { return date; }
        public String getOrigin()        { return origin; }
        public String getDestination()   { return destination; }
        public String getProductName()   { return productName; }
        public String getQuantity()      { return quantity; }
        public String getStatus()        { return status; }
    }

    // ══════════════════════════════════════════════════
    // UTIL
    // ══════════════════════════════════════════════════

    private void updatePaginationButtons(Label label, Button prev, Button next, int currentPage, int totalPages) {
        label.setText("Página " + (currentPage + 1) + " de " + Math.max(1, totalPages));
        prev.setDisable(currentPage <= 0);
        next.setDisable(currentPage >= totalPages - 1);
    }

    private String fmt(double v) {
        return Formatters.formatNumber(v);
    }

    private String fmt(BigDecimal v) {
        return Formatters.formatNumber(v);
    }

    private String fmtD(Double v) {
        return fmt(v != null ? v : 0.0);
    }

    private String fmtD(BigDecimal v) {
        return fmt(v != null ? v : BigDecimal.ZERO);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
