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
import java.time.LocalDateTime;
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
    private final SupplierRepository supplierRepository;
    private final PaymentRepository paymentRepository;
    private final StockBranchRepository stockBranchRepository;
    private final StockBranchService stockBranchService;
    private final CashSessionService cashSessionService;
    private final SaleItemRepository saleItemRepository;

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
                               SupplierRepository supplierRepository,
                               PaymentRepository paymentRepository,
                               StockBranchRepository stockBranchRepository,
                               StockBranchService stockBranchService,
                               CashSessionService cashSessionService,
                               SaleItemRepository saleItemRepository) {
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
        this.supplierRepository = supplierRepository;
        this.paymentRepository = paymentRepository;
        this.stockBranchRepository = stockBranchRepository;
        this.stockBranchService = stockBranchService;
        this.cashSessionService = cashSessionService;
        this.saleItemRepository = saleItemRepository;
    }

    /**
     * Carrega as métricas executivas e operacionais para o Painel Geral do SGV.
     * Suporta segmentação por filial do utilizador e agregação SQL directa (<15ms).
     */
    public void loadStats(User currentUser, Long branchId,
                          Label salesLabel, Label productsLabel, Label customersLabel, Label branchesLabel,
                          LineChart<String, Number> salesLineChart, PieChart salesPieChart,
                          Runnable onCheckAlerts, Runnable onAnimateEntrance) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        // Se o utilizador não for super-administrador, restringe automaticamente à sua filial
        Long effectiveBranchId = branchId;
        if (effectiveBranchId == null && currentUser != null && !currentUser.isSuperAdmin() && currentUser.getBranch() != null) {
            effectiveBranchId = currentUser.getBranch().getId();
        }

        // 1. Vendas de Hoje
        BigDecimal todayTotal = saleRepository.sumTotalByDateRangeAndBranch(startOfDay, endOfDay, effectiveBranchId);
        long todayCount = saleRepository.countByDateRangeAndBranch(startOfDay, endOfDay, effectiveBranchId);
        if (salesLabel != null) {
            salesLabel.setText(String.format("%,.2f MT (%d vendas)", todayTotal, todayCount));
        }

        // 2. Artigos em Falta / Rutura
        long lowStockCount = stockBranchRepository.countLowStockByBranch(effectiveBranchId);
        long zeroStockCount = stockBranchRepository.countZeroStockByBranch(effectiveBranchId);
        if (productsLabel != null) {
            productsLabel.setText(String.format("%d artigos (%d esgotados)", lowStockCount + zeroStockCount, zeroStockCount));
            productsLabel.setStyle(zeroStockCount > 0 ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;" : "-fx-text-fill: #0F172A; -fx-font-weight: 800;");
        }

        // 3. Contas a Receber (Créditos Pendentes de Clientes)
        BigDecimal pendingCredits = saleRepository.sumTotalPendingCreditsByBranch(effectiveBranchId);
        if (customersLabel != null) {
            customersLabel.setText(String.format("%,.2f MT", pendingCredits));
            customersLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: 800;");
        }

        // 4. Saldo Real em Caixa / Gaveta (Turno Atual do Operador)
        if (branchesLabel != null) {
            if (currentUser != null && cashSessionService != null) {
                var sessionOpt = cashSessionService.getOpenSession(currentUser);
                if (sessionOpt.isPresent()) {
                    List<CashMovement> movements = cashSessionService.getMovements(sessionOpt.get());
                    BigDecimal initVal = sessionOpt.get().getInitialValue() != null ? sessionOpt.get().getInitialValue() : BigDecimal.ZERO;
                    BigDecimal inVal = movements.stream().filter(m -> "IN".equals(m.getType())).map(CashMovement::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal outVal = movements.stream().filter(m -> "OUT".equals(m.getType())).map(CashMovement::getAmount).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal currentBalance = initVal.add(inVal).subtract(outVal);
                    branchesLabel.setText(String.format("%,.2f MT", currentBalance));
                    branchesLabel.setStyle("-fx-font-size:26px; -fx-font-weight:800; -fx-text-fill:#15803D;");
                } else {
                    branchesLabel.setText("Caixa Fechado");
                    branchesLabel.setStyle("-fx-font-size:22px; -fx-font-weight:800; -fx-text-fill:#94A3B8;");
                }
            } else {
                long branchCount = branchRepository.count();
                branchesLabel.setText(String.format("%d filiais", branchCount));
            }
        }

        // 5. Gráfico de Faturação dos Últimos 7 Dias (MT)
        if (salesLineChart != null) {
            salesLineChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Faturação (MT)");
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                LocalDateTime dStart = date.atStartOfDay();
                LocalDateTime dEnd = date.atTime(23, 59, 59);
                BigDecimal dailyTotal = saleRepository.sumTotalByDateRangeAndBranch(dStart, dEnd, effectiveBranchId);
                series.getData().add(new XYChart.Data<>(date.format(DateTimeFormatter.ofPattern("dd/MM")), dailyTotal.doubleValue()));
            }
            salesLineChart.getData().add(series);
        }

        // 6. Gráfico de Meios de Pagamento de Hoje (Numerário, M-Pesa, e-Mola, mKesh, POS)
        if (salesPieChart != null) {
            List<Object[]> paymentRows = saleRepository.sumTotalByPaymentMethodToday(startOfDay, endOfDay, effectiveBranchId);
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Object[] r : paymentRows) {
                String rawMethod = (r[0] != null) ? r[0].toString() : "Outro";
                com.sgv.model.PaymentMethod pm = com.sgv.model.PaymentMethod.fromString(rawMethod);
                String label = pm.getDescription();
                BigDecimal val = (r[1] instanceof BigDecimal bd) ? bd : BigDecimal.valueOf(((Number) r[1]).doubleValue());
                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    pieData.add(new PieChart.Data(String.format("%s (%,.0f MT)", label, val), val.doubleValue()));
                }
            }
            if (pieData.isEmpty()) {
                pieData.add(new PieChart.Data("Sem vendas hoje", 1));
            }
            salesPieChart.setData(pieData);
        }

        if (onCheckAlerts != null) onCheckAlerts.run();
        if (onAnimateEntrance != null) onAnimateEntrance.run();
    }

    /**
     * Sobrecarga de compatibilidade para carregamento sem parâmetros de utilizador explícitos.
     */
    public void loadStats(Label salesLabel, Label productsLabel, Label customersLabel, Label branchesLabel,
                          LineChart<String, Number> salesLineChart, PieChart salesPieChart,
                          Runnable onCheckAlerts, Runnable onAnimateEntrance) {
        loadStats(null, null, salesLabel, productsLabel, customersLabel, branchesLabel, salesLineChart, salesPieChart, onCheckAlerts, onAnimateEntrance);
    }

    public void loadSalesStats(Label revenueLabel, Label countLabel, Label avgLabel, Label pendingLabel,
                               BarChart<String, Number> barChart, PieChart pieChart) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        BigDecimal revenueToday = saleRepository.sumTotalByDateRangeAndBranch(startOfDay, endOfDay, null);
        if (revenueLabel != null) revenueLabel.setText(String.format("%,.2f MT", revenueToday));

        long countToday = saleRepository.countByDateRangeAndBranch(startOfDay, endOfDay, null);
        if (countLabel != null) countLabel.setText(String.valueOf(countToday));

        double avgTicket = countToday > 0 ? revenueToday.doubleValue() / countToday : 0.0;
        if (avgLabel != null) avgLabel.setText(String.format("%,.2f MT", avgTicket));

        BigDecimal pendingVal = saleRepository.sumTotalPendingCreditsByBranch(null);
        if (pendingLabel != null) pendingLabel.setText(String.format("%,.2f MT", pendingVal));

        if (barChart != null) {
            barChart.getData().clear();
            XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
            barSeries.setName("Faturação Mensal (MT)");
            LocalDate start = today.minusMonths(5).withDayOfMonth(1);
            for (int i = 0; i < 6; i++) {
                LocalDate monthStart = start.plusMonths(i);
                LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                BigDecimal monthTotal = saleRepository.sumTotalByDateRangeAndBranch(monthStart.atStartOfDay(), monthEnd.atTime(23, 59, 59), null);
                String label = monthStart.format(DateTimeFormatter.ofPattern("MMM/yy"));
                barSeries.getData().add(new XYChart.Data<>(label, monthTotal.doubleValue()));
            }
            barChart.getData().add(barSeries);
        }

        if (pieChart != null) {
            List<Object[]> paymentRows = saleRepository.sumTotalByPaymentMethodToday(startOfDay, endOfDay, null);
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Object[] r : paymentRows) {
                String method = (r[0] != null) ? r[0].toString() : "Outro";
                BigDecimal val = (r[1] instanceof BigDecimal bd) ? bd : BigDecimal.valueOf(((Number) r[1]).doubleValue());
                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    pieData.add(new PieChart.Data(String.format("%s (%,.0f MT)", method, val), val.doubleValue()));
                }
            }
            pieChart.setData(pieData);
        }
    }

    public void checkAlerts(Label notificationBadge) {
        long low = stockBranchRepository.countLowStockByBranch(null);
        long zero = stockBranchRepository.countZeroStockByBranch(null);
        long totalAlerts = low + zero;

        if (notificationBadge != null) {
            notificationBadge.setText(String.valueOf(totalAlerts));
            notificationBadge.setVisible(totalAlerts > 0);
            notificationBadge.setManaged(totalAlerts > 0);
        }
    }

    public void updateCashBadge(Label cashStatusBadge, User currentUser) {
        if (cashStatusBadge == null) return;
        try {
            boolean open = currentUser != null && cashSessionService != null && cashSessionService.hasOpenSession(currentUser);
            if (open) {
                cashStatusBadge.setText("● Caixa Aberto");
                cashStatusBadge.setStyle(
                    "-fx-background-color: #DCFCE7; " +
                    "-fx-text-fill: #15803D; " +
                    "-fx-font-weight: 700; " +
                    "-fx-font-size: 11px; " +
                    "-fx-padding: 4 10; " +
                    "-fx-background-radius: 12; " +
                    "-fx-cursor: hand;"
                );
            } else {
                cashStatusBadge.setText("○ Caixa Fechado");
                cashStatusBadge.setStyle(
                    "-fx-background-color: #FEE2E2; " +
                    "-fx-text-fill: #B91C1C; " +
                    "-fx-font-weight: 700; " +
                    "-fx-font-size: 11px; " +
                    "-fx-padding: 4 10; " +
                    "-fx-background-radius: 12; " +
                    "-fx-cursor: hand;"
                );
            }
        } catch (Exception ex) {
            log.error("Erro ao actualizar estado do caixa no badge", ex);
        }
    }

    public void showNotificationPopup(Button notificationBellButton) {
        if (notificationBellButton == null) return;
        List<StockBranch> lowStocks = stockBranchRepository.findAll().stream()
                .filter(sb -> {
                    BigDecimal cur = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO;
                    BigDecimal min = sb.getStockMinAmount() != null ? sb.getStockMinAmount() : BigDecimal.ZERO;
                    return cur.compareTo(min) <= 0;
                })
                .limit(10)
                .toList();

        ContextMenu menu = new ContextMenu();
        if (lowStocks.isEmpty()) {
            MenuItem item = new MenuItem("✅ Todos os artigos possuem stock regular");
            item.setDisable(true);
            menu.getItems().add(item);
        } else {
            MenuItem header = new MenuItem(String.format("⚠️ Alertas de Reposição (%d artigos):", lowStocks.size()));
            header.setStyle("-fx-font-weight: 800; -fx-text-fill: #EF4444;");
            menu.getItems().add(header);
            for (StockBranch sb : lowStocks) {
                String prodName = sb.getProduct() != null ? sb.getProduct().getName() : "Artigo";
                BigDecimal cur = sb.getStockCurrentAmount() != null ? sb.getStockCurrentAmount() : BigDecimal.ZERO;
                MenuItem item = new MenuItem(String.format("• %s — Stock actual: %.1f", prodName, cur.doubleValue()));
                menu.getItems().add(item);
            }
        }
        menu.show(notificationBellButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    public void updateSalesKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            BigDecimal hoje = saleRepository.sumTotalByDateRangeAndBranch(startOfDay, endOfDay, null);
            BigDecimal semana = saleRepository.sumTotalByDateRangeAndBranch(today.minusDays(7).atStartOfDay(), endOfDay, null);
            BigDecimal mes = saleRepository.sumTotalByDateRangeAndBranch(today.withDayOfMonth(1).atStartOfDay(), endOfDay, null);
            long countMes = saleRepository.countByDateRangeAndBranch(today.withDayOfMonth(1).atStartOfDay(), endOfDay, null);
            double ticketMed = countMes > 0 ? mes.doubleValue() / countMes : 0.0;

            updateKPICard(grid, 0, String.format("%,.0f MT", hoje));
            updateKPICard(grid, 1, String.format("%,.0f MT", semana));
            updateKPICard(grid, 2, String.format("%,.0f MT", mes));
            updateKPICard(grid, 3, String.format("%,.0f MT", ticketMed));
        } catch (Exception ex) { log.error("Erro inesperado ao actualizar KPIs de vendas", ex); }
    }

    public void updateFinanceiroKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            BigDecimal totalVendas = saleRepository.sumTotalAll();
            BigDecimal pendente = saleRepository.sumTotalPendingCreditsByBranch(null);
            BigDecimal recebido = totalVendas.subtract(pendente);
            long numPagamentos = paymentRepository.count();

            setFinanceiroLabel(grid, "finTotalVendasLabel", String.format("%,.2f MT", totalVendas));
            setFinanceiroLabel(grid, "finTotalRecebidoLabel", String.format("%,.2f MT", recebido));
            setFinanceiroLabel(grid, "finPendenteLabel", String.format("%,.2f MT", pendente));
            setFinanceiroLabel(grid, "finNumPagamentosLabel", String.valueOf(numPagamentos));
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs financeiros", ex); }
    }

    private void setFinanceiroLabel(GridPane grid, String id, String value) {
        javafx.scene.Node node = grid.lookup("#" + id);
        if (node instanceof javafx.scene.control.Label lbl) {
            lbl.setText(value);
        }
    }

    public void updateStockKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            long totalArtigos = productRepository.count();
            long baixo = stockBranchRepository.countLowStockByBranch(null);
            long zero = stockBranchRepository.countZeroStockByBranch(null);
            updateKPICard(grid, 0, String.valueOf(totalArtigos));
            updateKPICard(grid, 1, String.format("%d artigos em loja", totalArtigos));
            updateKPICard(grid, 2, String.valueOf(baixo));
            updateKPICard(grid, 3, String.valueOf(zero));
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de stock", ex); }
    }

    public void updateCustomersKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            long total = customerRepository.count();
            long atacados = customerRepository.findAll().stream().filter(c -> "GROSSO".equalsIgnoreCase(c.getType()) || "ATACADO".equalsIgnoreCase(c.getType())).count();
            long retalhos = total - atacados;
            updateKPICard(grid, 0, String.valueOf(total));
            updateKPICard(grid, 1, String.valueOf(atacados));
            updateKPICard(grid, 2, String.valueOf(retalhos));
            updateKPICard(grid, 3, String.valueOf(total));
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de clientes", ex); }
    }

    public void updateComprasKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Purchase> all = purchaseRepository.findAll();
            double totalValor = all.stream().mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0).sum();
            long pendentes = all.stream().filter(p -> "PENDING".equalsIgnoreCase(p.getState())).count();
            long recebidas = all.stream().filter(p -> "RECEIVED".equalsIgnoreCase(p.getState())).count();
            updateKPICard(grid, 0, String.valueOf(all.size()));
            updateKPICard(grid, 1, String.format("%,.0f MT", totalValor));
            updateKPICard(grid, 2, String.valueOf(pendentes));
            updateKPICard(grid, 3, String.valueOf(recebidas));
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de compras", ex); }
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
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de produção", ex); }
    }

    public void updateSuppliersKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            List<Supplier> all = supplierRepository.findAll();
            long total = all.size();
            long activos = all.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count();
            updateKPICard(grid, 0, String.valueOf(total));
            updateKPICard(grid, 1, String.valueOf(activos));
            updateKPICard(grid, 2, String.valueOf(total - activos));
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de fornecedores", ex); }
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
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de catálogos", ex); }
    }

    public void updateSistemaKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            long total = userRepository.count();
            long activos = userRepository.findAll().stream().filter(User::isActive).count();
            long filiais = branchRepository.count();
            updateKPICard(grid, 0, String.valueOf(filiais));
            updateKPICard(grid, 1, String.valueOf(total));
            updateKPICard(grid, 2, String.valueOf(activos));
            updateKPICard(grid, 3, String.valueOf(total - activos));
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de sistema", ex); }
    }

    public void updateUsersKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            long total = userRepository.count();
            long activos = userRepository.findAll().stream().filter(User::isActive).count();
            updateKPICard(grid, 0, String.valueOf(total));
            updateKPICard(grid, 1, String.valueOf(activos));
            updateKPICard(grid, 2, String.valueOf(total - activos));
            updateKPICard(grid, 3, "—");
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de utilizadores", ex); }
    }

    public void updateReportsKPIs(GridPane grid) {
        if (grid == null) return;
        try {
            LocalDate today = LocalDate.now();
            BigDecimal mes = saleRepository.sumTotalByDateRangeAndBranch(today.withDayOfMonth(1).atStartOfDay(), today.atTime(23, 59, 59), null);
            updateKPICard(grid, 0, String.format("%,.0f MT", mes));
            updateKPICard(grid, 1, String.valueOf(productRepository.count()));
            updateKPICard(grid, 2, String.valueOf(customerRepository.count()));
            updateKPICard(grid, 3, String.valueOf(userRepository.count()));
        } catch (Exception ex) { log.error("Erro ao actualizar KPIs de relatórios", ex); }
    }

    public void updateKPICard(GridPane grid, int index, String value) {
        if (grid == null || index >= grid.getChildren().size()) return;
        javafx.scene.Node cardNode = grid.getChildren().get(index);
        if (cardNode instanceof VBox card && !card.getChildren().isEmpty()) {
            javafx.scene.Node rowNode = card.getChildren().get(0);
            if (rowNode instanceof HBox row && row.getChildren().size() >= 2) {
                javafx.scene.Node infoNode = row.getChildren().get(1);
                if (infoNode instanceof VBox info && info.getChildren().size() >= 2) {
                    javafx.scene.Node lblNode = info.getChildren().get(1);
                    if (lblNode instanceof Label lbl) {
                        lbl.setText(value);
                    }
                }
            }
        }
    }
}
