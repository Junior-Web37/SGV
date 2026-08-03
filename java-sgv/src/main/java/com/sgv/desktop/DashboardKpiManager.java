package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DashboardKpiManager {

    private static final Logger log = LoggerFactory.getLogger(DashboardKpiManager.class);

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final CategoryRepository categoryRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final StockBranchService stockBranchService;
    private final CashSessionService cashSessionService;

    public DashboardKpiManager(SaleRepository saleRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               BranchRepository branchRepository,
                               UserRepository userRepository,
                               ExpenseRepository expenseRepository,
                               PurchaseRepository purchaseRepository,
                               ProductionOrderRepository productionOrderRepository,
                               CategoryRepository categoryRepository,
                               MetricUnitRepository metricUnitRepository,
                               StockBranchService stockBranchService,
                               CashSessionService cashSessionService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.purchaseRepository = purchaseRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.categoryRepository = categoryRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.stockBranchService = stockBranchService;
        this.cashSessionService = cashSessionService;
    }

    public void loadStats(Label salesLabel, Label productsLabel, Label customersLabel, Label branchesLabel,
                          LineChart<String, Number> salesLineChart, PieChart salesPieChart,
                          Runnable onCheckAlerts, Runnable onAnimateEntrance) {
        long salesCount = saleRepository.count();
        double salesTotal = saleRepository.findAll().stream()
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        salesLabel.setText(salesCount + " (" + String.format("%.2f", salesTotal) + ")");
        productsLabel.setText(String.valueOf(productRepository.count()));
        customersLabel.setText(String.valueOf(customerRepository.count()));
        branchesLabel.setText(String.valueOf(branchRepository.count()));

        if (salesLineChart != null) {
            salesLineChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Vendas");
            LocalDate today = LocalDate.now();
            List<Sale> allSales = saleRepository.findAll();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                double dailyTotal = allSales.stream()
                        .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().toLocalDate().equals(date))
                        .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                        .sum();
                series.getData().add(new XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("dd/MM")), dailyTotal));
            }
            salesLineChart.getData().add(series);
        }

        if (salesPieChart != null) {
            List<Sale> allSales = saleRepository.findAll();
            long paid = allSales.stream().filter(s -> "PAGO".equalsIgnoreCase(s.getState()) || "PAID".equalsIgnoreCase(s.getState())).count();
            long pending = allSales.stream().filter(s -> "PENDENTE".equalsIgnoreCase(s.getState()) || "PENDING".equalsIgnoreCase(s.getState())).count();
            long cancelled = allSales.stream().filter(s -> "CANCELADO".equalsIgnoreCase(s.getState()) || "CANCELLED".equalsIgnoreCase(s.getState())).count();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Pago (" + paid + ")", paid),
                    new PieChart.Data("Pendente (" + pending + ")", pending),
                    new PieChart.Data("Cancelado (" + cancelled + ")", cancelled)
            );
            salesPieChart.setData(pieData);
        }

        if (onCheckAlerts != null) onCheckAlerts.run();
        if (onAnimateEntrance != null) onAnimateEntrance.run();
    }

    public void loadSalesStats(Label revenueLabel, Label countLabel, Label avgLabel, Label pendingLabel,
                               BarChart<String, Number> barChart, PieChart pieChart) {
        LocalDate today = LocalDate.now();
        java.time.LocalDateTime startOfDay = today.atStartOfDay();
        java.time.LocalDateTime endOfDay = today.atTime(23, 59, 59);
        List<Sale> allSales = saleRepository.findAll();

        double revenueToday = allSales.stream()
                .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(startOfDay) && !s.getCreatedAt().isAfter(endOfDay))
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        if (revenueLabel != null) revenueLabel.setText(String.format("%.2f AKZ", revenueToday));

        long countToday = allSales.stream()
                .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(startOfDay) && !s.getCreatedAt().isAfter(endOfDay))
                .count();
        if (countLabel != null) countLabel.setText(String.valueOf(countToday));

        long totalDocs = allSales.size();
        double totalRevenue = allSales.stream().mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0).sum();
        double avgTicket = totalDocs > 0 ? totalRevenue / totalDocs : 0.0;
        if (avgLabel != null) avgLabel.setText(String.format("%.2f AKZ", avgTicket));

        double pendingVal = allSales.stream()
                .filter(s -> "PENDENTE".equalsIgnoreCase(s.getState()) || "PENDING".equalsIgnoreCase(s.getState()))
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                .sum();
        if (pendingLabel != null) pendingLabel.setText(String.format("%.2f AKZ", pendingVal));

        if (barChart != null) {
            barChart.getData().clear();
            XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
            barSeries.setName("Faturação");
            LocalDate start = today.minusMonths(5).withDayOfMonth(1);
            for (int i = 0; i < 6; i++) {
                LocalDate monthStart = start.plusMonths(i);
                LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                double monthTotal = allSales.stream()
                        .filter(s -> s.getCreatedAt() != null &&
                                !s.getCreatedAt().toLocalDate().isBefore(monthStart) &&
                                !s.getCreatedAt().toLocalDate().isAfter(monthEnd))
                        .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                        .sum();
                String label = monthStart.format(DateTimeFormatter.ofPattern("MMM"));
                barSeries.getData().add(new XYChart.Data<>(label, monthTotal));
            }
            barChart.getData().add(barSeries);
        }

        if (pieChart != null) {
            long paid = allSales.stream().filter(s -> "PAGO".equalsIgnoreCase(s.getState()) || "PAID".equalsIgnoreCase(s.getState())).count();
            long pendingCount = allSales.stream().filter(s -> "PENDENTE".equalsIgnoreCase(s.getState()) || "PENDING".equalsIgnoreCase(s.getState())).count();
            long cancelled = allSales.stream().filter(s -> "CANCELADO".equalsIgnoreCase(s.getState()) || "CANCELLED".equalsIgnoreCase(s.getState())).count();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                    new PieChart.Data("Pago (" + paid + ")", paid),
                    new PieChart.Data("Pendente (" + pendingCount + ")", pendingCount),
                    new PieChart.Data("Cancelado (" + cancelled + ")", cancelled)
            );
            pieChart.setData(pieData);
        }
    }

    public void checkAlerts(Label notificationBadge) {
        int alertCount = 0;
        List<StockBranch> stocks = stockBranchService.findAll();
        for (StockBranch stock : stocks) {
            BigDecimal current = stock.getStockCurrentAmount();
            if (current != null && current.compareTo(BigDecimal.valueOf(10)) <= 0 && stock.getProduct() != null && !Boolean.TRUE.equals(stock.getProduct().getService())) {
                alertCount++;
            }
        }
        List<Sale> allSales = saleRepository.findAll();
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

    public void showNotificationPopup(Button notificationBellButton) {
        if (notificationBellButton == null) return;

        VBox popupContent = new VBox(10);
        popupContent.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 4);");
        popupContent.setPrefWidth(300);
        popupContent.setMaxHeight(400);

        Label titleLabel = new Label("Notificações do Sistema");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");
        popupContent.getChildren().add(titleLabel);
        popupContent.getChildren().add(new Separator());

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: white;");
        scrollPane.setMaxHeight(300);

        VBox alertsBox = new VBox(8);

        List<StockBranch> stocks = stockBranchService.findAll();
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

        List<Sale> allSales = saleRepository.findAll();
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

    public void updateCashBadge(Label cashStatusBadge, User currentUser) {
        if (cashStatusBadge == null) return;
        boolean isOpen = currentUser != null && cashSessionService.hasOpenSession(currentUser);
        if (isOpen) {
            cashStatusBadge.setText("Caixa Aberto");
            cashStatusBadge.setStyle("-fx-background-color: #e3fcef; -fx-text-fill: #00875a; -fx-padding: 6 12; -fx-background-radius: 4; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        } else {
            cashStatusBadge.setText("Caixa Fechado");
            cashStatusBadge.setStyle("-fx-background-color: #ffebe6; -fx-text-fill: #de350b; -fx-padding: 6 12; -fx-background-radius: 4; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        }
    }

    public void updateSalesKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Sale> sales = saleRepository.findAll();
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(7);
            double hoje = 0, semana = 0, mes = 0, ticket = 0;
            for (Sale s : sales) {
                if (s.getTotal() == null) continue;
                double t = s.getTotal();
                ticket += t;
                if (s.getCreatedAt() != null) {
                    LocalDate d = s.getCreatedAt().toLocalDate();
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
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateFinanceiroKPIs(GridPane grid) {
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
            LocalDate now = LocalDate.now();
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
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateStockKPIs(GridPane grid) {
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
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateCustomersKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Customer> all = customerRepository.findAll();
            LocalDate today = LocalDate.now();
            LocalDate thirtyDaysAgo = today.minusDays(30);
            long total = all.size();
            long atacados = all.stream().filter(c -> "ATACADO".equalsIgnoreCase(c.getType())).count();
            long varejos = all.stream().filter(c -> "VAREJO".equalsIgnoreCase(c.getType())).count();
            long recentes = all.stream()
                    .filter(c -> c.getCreatedAt() != null)
                    .filter(c -> { LocalDate d = c.getCreatedAt().toLocalDate(); return !d.isBefore(thirtyDaysAgo) && !d.isAfter(today); }).count();
            updateKPICard(grid, 0, String.valueOf(total));
            updateKPICard(grid, 1, String.valueOf(atacados));
            updateKPICard(grid, 2, String.valueOf(varejos));
            updateKPICard(grid, 3, String.valueOf(recentes));
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateComprasKPIs(GridPane grid) {
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
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateProducaoKPIs(GridPane grid) {
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
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateCatalogsKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            long cats = categoryRepository.count();
            long units = metricUnitRepository.count();
            updateKPICard(grid, 0, String.valueOf(cats));
            updateKPICard(grid, 1, String.valueOf(units));
            updateKPICard(grid, 2, String.valueOf(units));
            updateKPICard(grid, 3, "—");
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateSistemaKPIs(GridPane grid) {
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
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateUsersKPIs(GridPane grid) {
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
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateReportsKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            LocalDate today = LocalDate.now();
            List<Sale> sales = saleRepository.findAll();
            double mes = sales.stream()
                    .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().toLocalDate().getMonth() == today.getMonth() && s.getCreatedAt().toLocalDate().getYear() == today.getYear())
                    .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0)
                    .sum();
            updateKPICard(grid, 0, String.format("%.0f MT", mes));
            updateKPICard(grid, 1, String.valueOf(productRepository.count()));
            updateKPICard(grid, 2, String.valueOf(customerRepository.count()));
            updateKPICard(grid, 3, String.valueOf(userRepository.count()));
        } catch (Exception ex) { log.error("Erro inesperado", ex); }
    }

    public void updateKPICard(GridPane grid, int index, String value) {
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
}
