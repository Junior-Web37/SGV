package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.model.DocumentType;
import com.sgv.repository.*;
import com.sgv.service.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class DashboardCrudManager {

    private static final Logger log = LoggerFactory.getLogger(DashboardCrudManager.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int PAGE_SIZE = 10;
    private transient Warehouse selectedWarehouse;

    private final ApplicationContext applicationContext;
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
    private final CashSessionService cashSessionService;
    private final MetricUnitRepository metricUnitRepository;
    private final SaleDocumentService saleDocumentService;
    private final WarehouseRepository warehouseRepository;
    private final StockWarehouseRepository stockWarehouseRepository;
    private final SystemLogService systemLogService;
    private final TrainingModeService trainingModeService;

    public DashboardCrudManager(ApplicationContext applicationContext,
                                SaleRepository saleRepository,
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
                                CashSessionService cashSessionService,
                                MetricUnitRepository metricUnitRepository,
                                SaleDocumentService saleDocumentService,
                                WarehouseRepository warehouseRepository,
                                StockWarehouseRepository stockWarehouseRepository,
                                SystemLogService systemLogService,
                                TrainingModeService trainingModeService) {
        this.applicationContext = applicationContext;
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
        this.cashSessionService = cashSessionService;
        this.metricUnitRepository = metricUnitRepository;
        this.saleDocumentService = saleDocumentService;
        this.warehouseRepository = warehouseRepository;
        this.stockWarehouseRepository = stockWarehouseRepository;
        this.systemLogService = systemLogService;
        this.trainingModeService = trainingModeService;
    }

    public String getDateFormatPattern() { return "dd/MM/yyyy HH:mm"; }

    private void showTrainingBlockedAlert() {
        showAlert(Alert.AlertType.WARNING,
            "MODO TREINAMENTO ACTIVO\n\n" +
            "Esta operação está bloqueada enquanto o modo treino estiver activo.\n" +
            "Desative o modo treino em: Sistema > Modo Treinamento > Desactivar");
    }

    public void loadSales(TableView<Sale> salesTable, String filter, String stateFilter) {
        loadSales(salesTable, filter, stateFilter, "TODOS", null, null);
    }

    public void loadSales(TableView<Sale> salesTable, String filter, String stateFilter,
                          String docTypeFilter, LocalDateTime startDate, LocalDateTime endDate) {
        if (salesTable == null) return;
        List<Sale> allSales = saleRepository.findAllWithCustomerAndItems();
        List<Sale> filtered = allSales.stream()
                .filter(s -> {
                    if (filter == null || filter.isBlank()) return true;
                    String term = filter.toLowerCase();
                    if (s.getDocumentNumber() != null && s.getDocumentNumber().toString().contains(term)) return true;
                    if (s.getSeries() != null && s.getSeries().toLowerCase().contains(term)) return true;
                    if (s.getDocumentType() != null && s.getDocumentType().toLowerCase().contains(term)) return true;
                    if (s.getCustomer() != null && s.getCustomer().getName() != null && s.getCustomer().getName().toLowerCase().contains(term)) return true;
                    if (s.getCustomerName() != null && s.getCustomerName().toLowerCase().contains(term)) return true;
                    if (s.getCustomerNuit() != null && s.getCustomerNuit().toLowerCase().contains(term)) return true;
                    return false;
                })
                .filter(s -> {
                    if (stateFilter == null || "TODOS".equals(stateFilter)) return true;
                    return stateFilter.equals(s.getState());
                })
                .filter(s -> {
                    if (docTypeFilter == null || "TODOS".equals(docTypeFilter)) return true;
                    return docTypeFilter.equals(s.getDocumentType());
                })
                .filter(s -> {
                    if (startDate == null && endDate == null) return true;
                    LocalDateTime dt = s.getCreatedAt();
                    if (dt == null) return false;
                    if (startDate != null && dt.isBefore(startDate)) return false;
                    if (endDate != null && dt.isAfter(endDate)) return false;
                    return true;
                })
                .sorted(Comparator.comparing(Sale::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        salesTable.setItems(FXCollections.observableArrayList(filtered));
    }

    public void loadProducts(TableView<Product> productsTable, String filter, int page) {
        if (productsTable == null) return;
        List<Product> products;
        if (filter != null && !filter.isEmpty()) {
            products = productRepository.searchByCodeOrName(filter);
        } else {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);
            products = productRepository.findAll(pageable).getContent();
        }
        productsTable.setItems(FXCollections.observableArrayList(products));
    }

    public void loadCustomers(TableView<Customer> customersTable, String filter, int page) {
        if (customersTable == null) return;
        List<Customer> customers;
        if (filter != null && !filter.isEmpty()) {
            customers = customerRepository.searchByCodeOrName(filter);
        } else {
            Pageable pageable = PageRequest.of(page, PAGE_SIZE);
            customers = customerRepository.findAll(pageable).getContent();
        }
        customersTable.setItems(FXCollections.observableArrayList(customers));
    }

    public void loadStock(TableView<StockBranch> stockTable, TextField stockSearchField) {
        if (stockTable == null) return;
        List<StockBranch> stocks;
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

    public void loadPurchases(TableView<Purchase> purchasesTable) {
        if (purchasesTable == null) return;
        List<Purchase> purchases = purchaseRepository.findAllByOrderByCreatedAtDesc();
        purchasesTable.setItems(FXCollections.observableArrayList(purchases));
    }

    public void loadFinanceiro(TableView<Payment> paymentsTable,
                               Label finTotalVendasLabel, Label finTotalRecebidoLabel,
                               Label finPendenteLabel, Label finNumPagamentosLabel,
                               Runnable loadExpensesCallback) {
        if (paymentsTable == null) return;
        List<Payment> payments = paymentRepository.findAll();
        paymentsTable.setItems(FXCollections.observableArrayList(
                payments.stream()
                        .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .toList()
        ));
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
        if (loadExpensesCallback != null) loadExpensesCallback.run();
    }

    public void loadExpenses(TableView<Expense> expensesTable) {
        if (expensesTable == null) return;
        List<Expense> expenses = expenseRepository.findAllByOrderByCreatedAtDesc();
        expensesTable.setItems(FXCollections.observableArrayList(expenses));
    }

    public void loadProductionOrders(TableView<ProductionOrder> ordersTable) {
        if (ordersTable == null) return;
        List<ProductionOrder> orders = productionOrderRepository.findAllByOrderByCreatedAtDesc();
        ordersTable.setItems(FXCollections.observableArrayList(orders));
    }

    public void loadCategories(TableView<Category> categoriesTable) {
        if (categoriesTable == null) return;
        List<Category> list = categoryRepository.findAll();
        categoriesTable.setItems(FXCollections.observableArrayList(list));
    }

    public void loadMetricUnits(TableView<MetricUnit> unitsTable) {
        if (unitsTable == null) return;
        List<MetricUnit> list = metricUnitRepository.findAll();
        unitsTable.setItems(FXCollections.observableArrayList(list));
    }

    public void loadSystem(TableView<User> usersTable, TableView<Branch> branchesTable, VBox branchInfoBox, User currentUser) {
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

    public void openProductForm(Product product, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
                onDataChanged.run();
            });
            openModal(root, owner);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_PRODUCT_FORM", "Erro ao abrir formulário de produto.", ex);
            log.error("Erro inesperado", ex);
        }
    }

    public void openCustomerForm(Customer customer, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
                onDataChanged.run();
            });
            openModal(root, owner);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_CUSTOMER_FORM", "Erro ao abrir formulário de cliente.", ex);
            log.error("Erro inesperado", ex);
        }
    }

    public void openSupplierForm(Supplier supplier, Window owner, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/supplier_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            SupplierFormController controller = loader.getController();
            controller.setSupplier(supplier);
            controller.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_SUPPLIER_FORM", "Erro ao abrir formulário de fornecedor.", ex);
            log.error("Erro inesperado", ex);
        }
    }

    public void openWarehouseForm(Window owner, Runnable onSaved) {
        openWarehouseForm(null, owner, onSaved);
    }

    public void openWarehouseForm(Warehouse existing, Window owner, Runnable onSaved) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehouse_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            WarehouseFormController c = loader.getController();
            c.setOnSaved(onSaved);
            if (existing != null) c.setWarehouse(existing);
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setTitle(existing != null ? "Editar Armazém" : "Novo Armazém");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public Warehouse getSelectedWarehouse(VBox warehousesPane) {
        return selectedWarehouse;
    }

    public void openWarehouseTransferForm(Window owner, User currentUser) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/warehouse_transfer_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            WarehouseTransferFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setTitle("Transferência Armazém → Loja");
            stage.setScene(new Scene(root));
            stage.setWidth(900);
            stage.setHeight(620);
            stage.showAndWait();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openSaleForm(Sale sale, Window owner, User currentUser, Runnable onDataChanged) {
        openSaleFormWithType(sale, null, owner, currentUser, onDataChanged);
    }

    public void openPDVSaleFormWithType(Sale sale, String forceType, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sale_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            SaleFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setSale(sale);
            if (forceType != null) controller.setDocumentType(forceType);
            controller.setOnSave(onDataChanged);
            openPDVStage(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openPDVStage(Parent root, Window owner) {
        try {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            ScrollPane scroll = new ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
            Scene scene = new Scene(scroll);
            stage.setScene(scene);
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            stage.setWidth(bounds.getWidth() * 0.95);
            stage.setHeight(bounds.getHeight() * 0.95);
            stage.setX(bounds.getMinX() + bounds.getWidth() * 0.025);
            stage.setY(bounds.getMinY() + bounds.getHeight() * 0.025);
            stage.showAndWait();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openSaleFormWithType(Sale sale, String forceType, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sale_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            SaleFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setSale(sale);
            if (forceType != null) controller.setDocumentType(forceType);
            controller.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openCotacaoForm(Window owner, User currentUser, Runnable onDataChanged) {
        openSaleFormWithType(null, "COTACAO", owner, currentUser, onDataChanged);
    }

    public void openPurchaseForm(Purchase purchase, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
                onDataChanged.run();
            });
            openModal(root, owner);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_PURCHASE_FORM", "Erro ao abrir formulário de compra.", ex);
            log.error("Erro inesperado", ex);
        }
    }

    public void openExpenseForm(Expense expense, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
                onDataChanged.run();
            });
            openModal(root, owner);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_EXPENSE_FORM", "Erro ao abrir formulário de despesa.", ex);
            log.error("Erro inesperado", ex);
        }
    }

    public void openPaymentForm(Payment payment, Window owner, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            PaymentFormController controller = loader.getController();
            controller.setPayment(payment);
            controller.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openUserForm(User user, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
                onDataChanged.run();
            });
            openModal(root, owner);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_USER_FORM", "Erro ao abrir formulário de utilizador.", ex);
            log.error("Erro inesperado", ex);
        }
    }

    public void openBranchForm(Branch branch, Window owner, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/branch_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            BranchFormController controller = loader.getController();
            controller.setBranch(branch);
            controller.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openOrderForm(ProductionOrder order, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
                onDataChanged.run();
            });
            openModal(root, owner);
        } catch (Exception ex) {
            systemLogService.logError("OPEN_ORDER_FORM", "Erro ao abrir formulário de ordem de produção.", ex);
            log.error("Erro inesperado", ex);
        }
    }

    public void openCategoryForm(Category category, Window owner, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/category_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            CategoryFormController ctrl = loader.getController();
            ctrl.setCategory(category);
            ctrl.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openMetricUnitForm(MetricUnit unit, Window owner, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/metric_unit_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            MetricUnitFormController ctrl = loader.getController();
            ctrl.setMetricUnit(unit);
            ctrl.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openStockAdjustForm(StockBranch stock, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        if (stock == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock_adjust_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            StockAdjustFormController ctrl = loader.getController();
            ctrl.setStock(stock);
            ctrl.setCurrentUser(currentUser);
            ctrl.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openStockNewForm(Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/stock_new_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            StockNewFormController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            ctrl.setOnSave(onDataChanged);
            openModal(root, owner);
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void openProductStockHistory(Product product, Window owner) {
        try {
            List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
            TableView<StockMovement> table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

            TableColumn<StockMovement, String> colDate = new TableColumn<>("Data / Hora");
            colDate.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DATE_FORMATTER) : "—"));
            TableColumn<StockMovement, String> colType = new TableColumn<>("Tipo");
            colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType() != null ? d.getValue().getType() : "—"));
            TableColumn<StockMovement, String> colQty = new TableColumn<>("Qtd.");
            colQty.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getQtyAmount() != null ? d.getValue().getQtyAmount().toPlainString() : "0"));
            TableColumn<StockMovement, String> colBefore = new TableColumn<>("Stock Antes");
            colBefore.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getStockBeforeAmount() != null ? d.getValue().getStockBeforeAmount().toPlainString() : "0"));
            TableColumn<StockMovement, String> colAfter = new TableColumn<>("Stock Depois");
            colAfter.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getStockAfterAmount() != null ? d.getValue().getStockAfterAmount().toPlainString() : "0"));
            TableColumn<StockMovement, String> colBranch = new TableColumn<>("Filial");
            colBranch.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));
            TableColumn<StockMovement, String> colRef = new TableColumn<>("Referência");
            colRef.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getReference() != null ? d.getValue().getReference() : "—"));
            TableColumn<StockMovement, String> colUser = new TableColumn<>("Utilizador");
            colUser.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getUser() != null ? d.getValue().getUser().getUsername() : "—"));
            table.getColumns().addAll(List.of(colDate, colType, colQty, colBefore, colAfter, colBranch, colRef, colUser));
            table.setItems(FXCollections.observableArrayList(movements));

            VBox box = new VBox(12, new Label("Histórico de Stock para " + product.getCode() + " - " + product.getName()), table);
            box.setStyle("-fx-padding:16; -fx-background-color:#ffffff;");
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setTitle("Histórico de Stock - " + product.getName());
            stage.setScene(new Scene(box, 980, 520));
            stage.show();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void printSelectedSale(TableView<Sale> salesTable, Window owner) {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Sale fullSale = saleRepository.findByIdWithItems(selected.getId());
                if (fullSale == null) fullSale = selected;
                File pdf = saleDocumentService.generateDocument(fullSale);
                DocumentPreviewDialog.show(pdf, fullSale.getDocumentType());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erro na impressão: " + ex.getMessage());
                log.error("Erro inesperado", ex);
            }
        }
    }

    public void viewSelectedSale(TableView<Sale> salesTable, Window owner) {
        if (salesTable == null) return;
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selecione uma venda para ver detalhes.");
            return;
        }
        Sale fullSale = saleRepository.findByIdWithItems(selected.getId());
        if (fullSale == null) fullSale = selected;

        String stateColor = switch (fullSale.getState() != null ? fullSale.getState() : "") {
            case "EMITIDA" -> "#10B981";
            case "ANULADA" -> "#EF4444";
            case "COTACAO_ABERTA" -> "#F59E0B";
            case "COTACAO_PAGA" -> "#8B5CF6";
            case "PAGO" -> "#3B82F6";
            default -> "#64748B";
        };

        String custName = fullSale.getCustomer() != null ? fullSale.getCustomer().getName()
            : (fullSale.getCustomerName() != null ? fullSale.getCustomerName() : "Consumidor Final");

        DetailDialog dd = DetailDialog.create(owner)
            .title(fullSale.getDocumentType() + " #" + fullSale.getDocumentNumber())
            .subtitle("Série " + (fullSale.getSeries() != null ? fullSale.getSeries() : "—") +
                "  |  " + (fullSale.getCreatedAt() != null ? fullSale.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"))
            .statusBadge(fullSale.getState() != null ? fullSale.getState() : "—", stateColor)
            .section("Documento")
            .field("Filial", fullSale.getBranch() != null ? fullSale.getBranch().getName() : "—")
            .field("Tipo", fullSale.getDocumentType())
            .field("Nº Documento", String.valueOf(fullSale.getDocumentNumber()))
            .field("Série", fullSale.getSeries())
            .field("Ano", fullSale.getDocumentYear() != null ? String.valueOf(fullSale.getDocumentYear()) : "—")
            .field("Pagamento", fullSale.getPaymentMethod() != null ? fullSale.getPaymentMethod() : "—")
            .section("Cliente")
            .field("Nome", custName)
            .field("NUIT", fullSale.getCustomerNuit())
            .section("Totais")
            .field("Subtotal", String.format("%.2f MZN", fullSale.getSubtotal()))
            .field("IVA", String.format("%.2f MZN", fullSale.getTotalTax()))
            .field("Desconto", String.format("%.2f MZN", fullSale.getTotalDiscount()))
            .field("Total", String.format("%.2f MZN", fullSale.getTotal()), "#2563EB");

        if ("ANULADA".equals(fullSale.getState()) && fullSale.getAnnulReason() != null) {
            dd.section("Anulação")
                .field("Motivo", fullSale.getAnnulReason())
                .field("Data Anulação", fullSale.getAnnulDate() != null
                    ? fullSale.getAnnulDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
        }

        if (fullSale.getItems() != null && !fullSale.getItems().isEmpty()) {
            String[] cols = {"Produto", "Descrição", "Qtd", "Preço Unit.", "Total"};
            List<Map<String, String>> rows = new java.util.ArrayList<>();
            for (SaleItem item : fullSale.getItems()) {
                Map<String, String> row = new java.util.LinkedHashMap<>();
                row.put("Produto", item.getProductCode() != null ? item.getProductCode() : "—");
                row.put("Descrição", item.getDescription() != null ? item.getDescription() : "—");
                row.put("Qtd", item.getQty() != null ? String.valueOf(item.getQty()) : "—");
                row.put("Preço Unit.", item.getUnitPrice() != null ? String.format("%.2f", item.getUnitPrice()) : "—");
                row.put("Total", item.getLineTotal() != null ? String.format("%.2f", item.getLineTotal()) : "—");
                rows.add(row);
            }
            dd.tableSection("Itens (" + fullSale.getItems().size() + ")", cols, rows);
        }

        dd.show();
    }

    public void viewSelectedCustomer(TableView<Customer> customersTable, Window owner) {
        if (customersTable == null) return;
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selecione um cliente para ver detalhes.");
            return;
        }

        String typeColor = "B2B".equals(selected.getType()) ? "#3B82F6" : "#10B981";

        DetailDialog.create(owner)
            .title(selected.getName() != null ? selected.getName() : "Cliente")
            .subtitle("Código: " + (selected.getCode() != null ? selected.getCode() : "—"))
            .statusBadge(selected.getType() != null ? selected.getType() : "—", typeColor)
            .section("Dados Pessoais")
            .field("Nome Completo", selected.getName())
            .field("Código", selected.getCode())
            .field("Tipo", selected.getType())
            .field("NUIT", selected.getNuit())
            .section("Contacto")
            .field("Telefone", selected.getContact())
            .field("Morada", selected.getAddress())
            .section("Financeiro")
            .field("Saldo", selected.getBalance() != null ? String.format("%.2f MT", selected.getBalance()) : "0.00 MT", "#2563EB")
            .field("Limite de Crédito", selected.getCreditLimit() != null ? String.format("%.2f MT", selected.getCreditLimit()) : "—")
            .field("Desconto Padrão", selected.getDefaultDiscount() != null ? String.format("%.1f%%", selected.getDefaultDiscount()) : "—")
            .field("Pontos de Fidelidade", selected.getFidelityPoints() != null ? String.valueOf(selected.getFidelityPoints()) : "0")
            .show();
    }

    public void viewSelectedProduct(TableView<Product> productsTable, Window owner) {
        if (productsTable == null) return;
        Product selected = productsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selecione um produto para ver detalhes.");
            return;
        }

        String activeColor = Boolean.TRUE.equals(selected.getIsActive()) ? "#10B981" : "#EF4444";
        String activeText = Boolean.TRUE.equals(selected.getIsActive()) ? "Activo" : "Inactivo";

        DetailDialog dd = DetailDialog.create(owner)
            .title(selected.getName() != null ? selected.getName() : "Produto")
            .subtitle("Código: " + (selected.getCode() != null ? selected.getCode() : "—"))
            .statusBadge(activeText, activeColor)
            .section("Dados do Produto")
            .field("Nome", selected.getName())
            .field("Código", selected.getCode())
            .field("Categoria", selected.getCategory() != null ? selected.getCategory().getName() : "—")
            .field("Unidade", selected.getUnit() != null ? selected.getUnit().getAbbreviation() : "—")
            .field("Descrição", selected.getDescription())
            .field("Tipo", selected.getService() != null && selected.getService() ? "Serviço" : "Produto Físico")
            .section("Preços")
            .field("Preço de Venda", selected.getPriceSale() != null ? String.format("%.2f MT", selected.getPriceSale()) : "—", "#2563EB")
            .field("Preço de Custo", selected.getPriceCost() != null ? String.format("%.2f MT", selected.getPriceCost()) : "—")
            .field("Margem de Lucro", selected.getProfitMargin() != null ? String.format("%.1f%%", selected.getProfitMargin()) : "—")
            .field("IVA", selected.getTaxRate() != null ? String.format("%.1f%%", selected.getTaxRate()) : "—")
            .section("Stock")
            .field("Stock Mínimo", selected.getStockMin() != null ? String.format("%.1f", selected.getStockMin()) : "—")
            .field("Stock Máximo", selected.getStockMax() != null ? String.format("%.1f", selected.getStockMax()) : "—")
            .field("Localização", selected.getLocation());

        if (selected.getObservations() != null && !selected.getObservations().isBlank()) {
            dd.section("Observações")
                .field("Notas", selected.getObservations());
        }

        dd.show();
    }

    public void annulSelectedSale(TableView<Sale> salesTable, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
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
                fullSale.setAnnulInProgress(true);
                saleRepository.save(fullSale);
                if (fullSale.getItems() != null) {
                    for (var item : fullSale.getItems()) {
                        if (item.getProduct() == null) continue;
                        if (Boolean.TRUE.equals(item.getProduct().getService())) continue;
                        BigDecimal qty = item.getQtyAmount() != null ? item.getQtyAmount() : BigDecimal.ZERO;
                        if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;
                        stockBranchService.increaseStock(
                                fullSale.getBranch(), item.getProduct(), qty,
                                "ANULACAO-" + fullSale.getDocumentNumber() + "/" + fullSale.getSeries(),
                                "ANULACAO_VENDA", currentUser);
                    }
                }
                if ("CREDITO".equalsIgnoreCase(fullSale.getPaymentMethod()) && fullSale.getCustomer() != null) {
                    try {
                        var custOpt = customerRepository.findById(fullSale.getCustomer().getId());
                        if (custOpt.isPresent()) {
                            var cust = custOpt.get();
                            BigDecimal totalAmount = fullSale.getTotalAmount() != null ? fullSale.getTotalAmount() : BigDecimal.ZERO;
                            BigDecimal currentBalance = cust.getBalanceAmount();
                            cust.setBalanceAmount(currentBalance.subtract(totalAmount));
                            customerRepository.save(cust);
                        }
                    } catch (Exception ex) {
                        log.error("Erro ao estornar saldo do cliente na anulação", ex);
                    }
                }
                onDataChanged.run();
                showAlert(Alert.AlertType.INFORMATION, "Anulação Concluída: O documento foi anulado com sucesso.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erro na Anulação: " + ex.getMessage());
            }
        });
    }

    public void invoiceSelectedQuote(TableView<Sale> salesTable, Window owner, User currentUser, Runnable onDataChanged) {
        if (salesTable == null) return;
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Selecione uma Cotação para faturar."); return; }
        if (!"COTACAO".equals(selected.getDocumentType()) && !"COTACAO_ABERTA".equals(selected.getState())) {
            showAlert(Alert.AlertType.WARNING, "O documento selecionado não é uma Cotação."); return;
        }
        Sale newInvoice = new Sale();
        newInvoice.setDocumentType(DocumentType.FACTURA.name());
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
        Sale fullQuote = saleRepository.findByIdWithItems(selected.getId());
        if (fullQuote == null) fullQuote = selected;
        newInvoice.setOriginSale(fullQuote);
        List<com.sgv.entity.SaleItem> newItems = new ArrayList<>();
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
        openSaleFormWithType(newInvoice, "FACTURA", owner, currentUser, onDataChanged);
    }

    public void printSelectedReceipt(TableView<Sale> salesTable) {
        if (salesTable == null) return;
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Selecione uma venda/fatura paga para imprimir o recibo."); return; }
        if (!"PAGO".equals(selected.getState())) {
            showAlert(Alert.AlertType.WARNING, "Esta fatura ainda não foi paga."); return;
        }
        String origType = selected.getDocumentType();
        try {
            selected.setDocumentType(DocumentType.RECIBO.name());
            File pdf = saleDocumentService.generateDocument(selected);
            if (pdf != null) DocumentPreviewDialog.show(pdf, "RECIBO");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erro ao gerar recibo: " + ex.getMessage());
        } finally {
            selected.setDocumentType(origType);
        }
    }

    public void deleteSelectedCategory(TableView<Category> categoriesTable, Runnable onDataChanged, User currentUser) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        if (categoriesTable == null) return;
        Category selected = categoriesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Por favor selecione uma categoria para eliminar."); return; }
        safeDelete(() -> categoryRepository.delete(selected), onDataChanged, "Categoria", currentUser);
    }

    public void deleteSelectedUnit(TableView<MetricUnit> unitsTable, Runnable onDataChanged, User currentUser) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        if (unitsTable == null) return;
        MetricUnit selected = unitsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Por favor selecione uma unidade para eliminar."); return; }
        safeDelete(() -> metricUnitRepository.delete(selected), onDataChanged, "Unidade de Medida", currentUser);
    }

    public void deleteSelectedCustomer(TableView<Customer> customersTable, Runnable onDataChanged, User currentUser) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        if (customersTable == null) return;
        Customer selected = customersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Por favor selecione um cliente para eliminar."); return; }
        safeDelete(() -> customerRepository.delete(selected), onDataChanged, "Cliente", currentUser);
    }

    public void deleteSelectedStock(TableView<StockBranch> stockTable, Runnable onDataChanged, User currentUser) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        if (stockTable == null) return;
        StockBranch selected = stockTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert(Alert.AlertType.WARNING, "Por favor selecione um item de stock para eliminar."); return; }
        safeDelete(() -> stockBranchService.deleteStock(selected, currentUser), onDataChanged, "Stock", currentUser);
    }

    public void saveCompanyConfig(TextField companyNameField, TextField companyNuitField) {
        if (companyNameField.getText() == null || companyNameField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Nome da Empresa é obrigatório."); return;
        }
        if (companyNuitField.getText() == null || companyNuitField.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "NUIT da Empresa é obrigatório."); return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Configurações da Empresa salvas com sucesso!");
    }

    public void safeDelete(Runnable deleteAction, Runnable onSuccess, String type, User currentUser) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
        try {
            deleteAction.run();
            String actor = currentUser != null ? currentUser.getUsername() : "system";
            systemLogService.logUserAction(actor, "DELETE_" + type.toUpperCase().replace(" ", "_"), type + " eliminado com sucesso.");
            onSuccess.run();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            systemLogService.logError("DELETE_" + type.toUpperCase(), "Não foi possível eliminar " + type + " — integridade referencial.", e);
            showAlert(Alert.AlertType.ERROR, "Não é possível eliminar (" + type + ") pois existem registos associados.");
        } catch (Exception e) {
            systemLogService.logError("DELETE_" + type.toUpperCase(), "Erro ao eliminar " + type + ": " + e.getMessage(), e);
            showAlert(Alert.AlertType.ERROR, "Erro ao eliminar " + type + ": " + e.getMessage());
        }
    }

    public void openLiquidarDivida(Customer customer, Window owner, User currentUser, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
        pendingTable.getColumns().addAll(List.of(colDoc, colDate, colTotal, colState));

        try {
            List<Sale> pending = saleRepository.findPendingByCustomerId(customer.getId());
            pendingTable.setItems(FXCollections.observableArrayList(pending));
        } catch (Exception ex) { log.error("Erro inesperado", ex); }

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
        Button btnConfirmar = new Button("✓ Confirmar");
        btnConfirmar.setStyle("-fx-padding: 7 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: #10B981; -fx-text-fill: #ffffff; -fx-cursor: hand;");
        Button btnCancelarPay = new Button("Cancelar");
        btnCancelarPay.setStyle("-fx-padding: 7 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: #475569; -fx-text-fill: #ffffff; -fx-cursor: hand;");
        btnCancelarPay.setOnAction(ev -> dialog.close());

        btnConfirmar.setOnAction(ev -> {
            Sale selSale = pendingTable.getSelectionModel().getSelectedItem();
            String valStr = valorField.getText().trim().replace(",", ".");
            if (valStr.isEmpty()) return;
            double valor;
            try { valor = Double.parseDouble(valStr); } catch (NumberFormatException nfe) { return; }
            if (valor <= 0) return;
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
                onDataChanged.run();
                dialog.close();
            } catch (Exception ex) {
                log.error("Erro inesperado", ex);
            }
        });
        payRow.getChildren().addAll(lblValor, valorField, lblMetodo, metodoCombo, spacerPay, btnConfirmar, btnCancelarPay);
        root.getChildren().addAll(header, pendingTable, payRow);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    public void openAdiantamento(Customer customer, Window owner, Runnable onDataChanged) {
        if (trainingModeService.isTrainingMode()) { showTrainingBlockedAlert(); return; }
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
        HBox btnRow = new HBox(12);
        btnRow.setStyle("-fx-alignment:CENTER_RIGHT; -fx-padding:8 0 0 0;");
        Button btnOk = new Button("✓ Confirmar");
        btnOk.setStyle("-fx-padding: 7 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: #10B981; -fx-text-fill: #ffffff; -fx-cursor: hand;");
        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle("-fx-padding: 7 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: #475569; -fx-text-fill: #ffffff; -fx-cursor: hand;");
        btnCancel.setOnAction(ev -> dialog.close());
        btnOk.setOnAction(ev -> {
            String valStr = valorField.getText().trim().replace(",", ".");
            if (valStr.isEmpty()) return;
            double valor;
            try { valor = Double.parseDouble(valStr); } catch (NumberFormatException nfe) { return; }
            if (valor == 0) return;
            try {
                double novoSaldo = (customer.getBalance() != null ? customer.getBalance() : 0.0) - valor;
                customer.setBalance(novoSaldo);
                customerRepository.save(customer);
                onDataChanged.run();
                dialog.close();
            } catch (Exception ex) { log.error("Erro inesperado", ex); }
        });
        btnRow.getChildren().addAll(btnCancel, btnOk);
        body.getChildren().addAll(info, valorRow, btnRow);
        root.getChildren().addAll(header, body);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    public void openExtrato(Customer customer) {
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
        catch (Exception ex) { allSales = Collections.emptyList(); }

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
        table.getColumns().addAll(List.of(c1, c2, c3, c4, c5));
        table.setItems(FXCollections.observableArrayList(allSales));

        HBox footer = new HBox(12);
        footer.setStyle("-fx-background-color:#ffffff; -fx-padding:12 24; -fx-border-color:#E2E8F0; -fx-border-width:1 0 0 0; -fx-alignment:CENTER_RIGHT;");
        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-padding: 7 14; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: #475569; -fx-text-fill: #ffffff; -fx-cursor: hand;");
        btnFechar.setOnAction(ev -> dialog.close());
        footer.getChildren().add(btnFechar);
        root.getChildren().addAll(header, table, footer);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    public void exportCustomersToExcel(String customerFilter, Window owner, Runnable showToastCallback) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar ficheiro Excel");
        fc.setInitialFileName("clientes_sgv.xlsx");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        File file = fc.showSaveDialog(owner);
        if (file == null) return;

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Clientes");
            org.apache.poi.ss.usermodel.CellStyle headerStyle = wb.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFColor darkSlate = new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 30, (byte) 41, (byte) 59}, null);
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
                customers = (customerFilter == null || customerFilter.isEmpty()) ? customerRepository.findAll() : customerRepository.searchByCodeOrName(customerFilter);
            } catch (Exception ex) { customers = Collections.emptyList(); }

            org.apache.poi.ss.usermodel.CellStyle altStyle = wb.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFColor lightGray = new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{(byte) 241, (byte) 245, (byte) 249}, null);
            altStyle.setFillForegroundColor(lightGray);
            altStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            for (int r = 0; r < customers.size(); r++) {
                Customer c = customers.get(r);
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
                String[] values = {
                        c.getCode() != null ? c.getCode() : "", c.getName() != null ? c.getName() : "",
                        c.getNuit() != null ? c.getNuit() : "", c.getType() != null ? c.getType() : "",
                        c.getContact() != null ? c.getContact() : "", c.getAddress() != null ? c.getAddress() : "",
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
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) { wb.write(fos); }
            if (showToastCallback != null) showToastCallback.run();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public void loadWarehousesCards(VBox warehousesPane, Window owner, User currentUser, Runnable onRefresh) {
        if (warehousesPane == null) return;

        VBox main = null;
        ScrollPane scroll = null;
        FlowPane cardsPane = null;
        FlowPane kpiRow = null;

        for (javafx.scene.Node child : warehousesPane.getChildren()) {
            if (child instanceof VBox vb) {
                main = vb;
                break;
            }
        }
        if (main == null) {
            main = new VBox(0);
            main.setStyle("-fx-background-color: #F8FAFC;");
            kpiRow = new FlowPane(12, 10);
            kpiRow.setStyle("-fx-padding: 20 20 8 20;");
            HBox toolbar = new HBox(10);
            toolbar.setStyle("-fx-background-color: #ffffff; -fx-padding: 12 16; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            toolbar.getChildren().add(spacer);
            cardsPane = new FlowPane();
            cardsPane.setHgap(16);
            cardsPane.setVgap(16);
            cardsPane.setStyle("-fx-padding: 8 20 20 20;");
            scroll = new ScrollPane(cardsPane);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color:transparent; -fx-background: #F8FAFC; -fx-border-color:transparent;");
            VBox.setVgrow(scroll, Priority.ALWAYS);
            main.getChildren().addAll(kpiRow, toolbar, scroll);
            warehousesPane.getChildren().add(main);
        } else {
            for (javafx.scene.Node child : main.getChildren()) {
                if (child instanceof ScrollPane sp) { scroll = sp; break; }
                if (child instanceof FlowPane fp && fp.getId() != null && fp.getId().equals("warehousesKpiRow")) { kpiRow = fp; }
            }
            if (kpiRow == null) {
                for (javafx.scene.Node child : main.getChildren()) {
                    if (child instanceof FlowPane fp) { kpiRow = fp; break; }
                }
            }
            if (scroll != null && scroll.getContent() instanceof FlowPane fp) {
                cardsPane = fp;
            }
        }

        if (cardsPane == null) return;
        cardsPane.getChildren().clear();

        String filter = "";
        if (main != null) {
            for (javafx.scene.Node child : main.getChildren()) {
                if (child instanceof HBox hb) {
                    for (javafx.scene.Node n : hb.getChildren()) {
                        if (n instanceof TextField tf && tf.getPromptText() != null && tf.getPromptText().contains("Pesquisar")) {
                            filter = tf.getText() != null ? tf.getText().trim().toLowerCase() : "";
                            break;
                        }
                    }
                }
            }
        }
        final String searchFilter = filter;

        List<Warehouse> list = warehouseRepository.findAllByOrderByNameAsc();
        if (!searchFilter.isBlank()) {
            list = list.stream().filter(w ->
                (w.getName() != null && w.getName().toLowerCase().contains(searchFilter)) ||
                (w.getCode() != null && w.getCode().toLowerCase().contains(searchFilter)) ||
                (w.getAddress() != null && w.getAddress().toLowerCase().contains(searchFilter))
            ).toList();
        }

        int totalProducts = 0;
        BigDecimal totalStockValue = BigDecimal.ZERO;
        int activeCount = 0;

        for (Warehouse w : list) {
            List<StockWarehouse> stockList = stockWarehouseRepository.findByWarehouseId(w.getId());
            int productCount = stockList.size();
            BigDecimal stockQty = BigDecimal.ZERO;
            BigDecimal stockValue = BigDecimal.ZERO;
            for (StockWarehouse sw : stockList) {
                BigDecimal qty = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : BigDecimal.ZERO;
                stockQty = stockQty.add(qty);
                if (sw.getProduct() != null && sw.getProduct().getPriceCost() != null) {
                    stockValue = stockValue.add(qty.multiply(BigDecimal.valueOf(sw.getProduct().getPriceCost())));
                }
            }
            totalProducts += productCount;
            totalStockValue = totalStockValue.add(stockValue);
            if (w.isActive()) activeCount++;

            VBox card = new VBox(6);
            card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6,0,0,2); -fx-min-width: 280; -fx-pref-width: 300;");

            HBox topBar = new HBox(8);
            topBar.setStyle("-fx-background-color: " + (w.isActive() ? "#EFF6FF" : "#FEF2F2") + "; -fx-padding: 14 16 10 16; -fx-background-radius: 10 10 0 0;");
            Label icon = new Label("\uD83C\uDFED");
            icon.setStyle("-fx-font-size: 28px;");
            Label badge = new Label(w.isActive() ? "Activo" : "Inactivo");
            badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 10; -fx-background-color: " + (w.isActive() ? "#DBEAFE" : "#FEE2E2") + "; -fx-text-fill: " + (w.isActive() ? "#1D4ED8" : "#DC2626") + ";");
            Region topSpacer = new Region();
            HBox.setHgrow(topSpacer, Priority.ALWAYS);
            topBar.getChildren().addAll(icon, topSpacer, badge);

            VBox info = new VBox(4);
            info.setStyle("-fx-padding: 12 16 6 16;");
            Label name = new Label(w.getName());
            name.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0F172A; -fx-wrap-text: true;");
            Label code = new Label(w.getCode());
            code.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
            info.getChildren().addAll(name, code);

            if (w.getAddress() != null && !w.getAddress().isBlank()) {
                Label addr = new Label("\uD83D\uDCCD " + w.getAddress());
                addr.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-wrap-text: true;");
                info.getChildren().add(addr);
            }

            Separator sep = new Separator();
            sep.setStyle("-fx-padding: 0 16;");

            FlowPane stats = new FlowPane(8, 4);
            stats.setStyle("-fx-padding: 6 16 14 16;");
            Label statProducts = new Label("\uD83D\uDCE6 " + productCount + " produtos");
            statProducts.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-background-color: #F1F5F9; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: 600;");
            Label statQty = new Label("\u2696\uFE0F " + String.format("%.1f", stockQty));
            statQty.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-background-color: #F1F5F9; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: 600;");
            Label statValue = new Label("\uD83D\uDCB0 " + String.format("%.0f", stockValue) + " MT");
            statValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #0F172A; -fx-background-color: #FEF3C7; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: 700;");
            stats.getChildren().addAll(statProducts, statQty, statValue);

            card.getChildren().addAll(topBar, info, sep, stats);
            card.setUserData(w);
            String normalStyle = card.getStyle();
            String hoverStyle = normalStyle.replace("-fx-border-color: #E2E8F0", "-fx-border-color: #3B82F6");
            card.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    enterWarehouse(w, warehousesPane, owner, currentUser, onRefresh);
                } else {
                    selectedWarehouse = w;
                }
            });
            card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
            card.setOnMouseExited(e -> card.setStyle(normalStyle));
            cardsPane.getChildren().add(card);
        }

        if (kpiRow != null) {
            kpiRow.getChildren().clear();
            Label kpiTotal = new Label("\uD83C\uDFED " + list.size() + " Armazéns");
            kpiTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1E40AF; -fx-background-color: #DBEAFE; -fx-padding: 8 16; -fx-background-radius: 8;");
            Label kpiActive = new Label("\u2705 " + activeCount + " Activos");
            kpiActive.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #065F46; -fx-background-color: #D1FAE5; -fx-padding: 8 16; -fx-background-radius: 8;");
            Label kpiProducts = new Label("\uD83D\uDCE6 " + totalProducts + " Produtos em Stock");
            kpiProducts.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #92400E; -fx-background-color: #FEF3C7; -fx-padding: 8 16; -fx-background-radius: 8;");
            Label kpiValue = new Label("\uD83D\uDCB0 " + String.format("%.0f", totalStockValue) + " MT em Stock");
            kpiValue.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #581C87; -fx-background-color: #EDE9FE; -fx-padding: 8 16; -fx-background-radius: 8;");
            kpiRow.getChildren().addAll(kpiTotal, kpiActive, kpiProducts, kpiValue);
        }
    }

    public void enterWarehouse(Warehouse w, VBox warehousesPane, Window owner, User currentUser, Runnable onRefresh) {
        warehousesPane.getChildren().clear();
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");

        HBox header = new HBox(12);
        header.setStyle("-fx-padding: 16 20; -fx-background-color: #1E293B; -fx-background-radius: 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        Button btnBack = new Button("\u25C2 Voltar");
        btnBack.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: #ffffff; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 8 14; -fx-background-radius: 6; -fx-font-size: 13px;");
        Label title = new Label(w.getName() + " (" + w.getCode() + ")");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #ffffff; -fx-padding: 0 0 0 8;");
        Label statusBadge = new Label(w.isActive() ? "Activo" : "Inactivo");
        statusBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 10; -fx-background-radius: 10; -fx-background-color: " + (w.isActive() ? "#10B981" : "#EF4444") + "; -fx-text-fill: #ffffff;");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button btnEditar = new Button("Editar");
        btnEditar.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: #ffffff; -fx-font-weight: 600; -fx-cursor: hand; -fx-padding: 8 14; -fx-background-radius: 6; -fx-font-size: 13px;");
        header.getChildren().addAll(btnBack, title, statusBadge, headerSpacer, btnEditar);
        btnBack.setOnAction(e -> { warehousesPane.getChildren().clear(); onRefresh.run(); });
        UiUtils.attachSafe(btnEditar, () -> openWarehouseForm(w, owner, () -> { warehousesPane.getChildren().clear(); onRefresh.run(); }), systemLogService, "WH_EDIT");

        List<StockWarehouse> stockList = stockWarehouseRepository.findByWarehouseId(w.getId());

        FlowPane kpiRow = new FlowPane(12, 10);
        kpiRow.setStyle("-fx-padding: 16 20 8 20;");
        int prodCount = stockList.size();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (StockWarehouse sw : stockList) {
            BigDecimal qty = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : BigDecimal.ZERO;
            totalQty = totalQty.add(qty);
            if (sw.getProduct() != null && sw.getProduct().getPriceCost() != null) {
                totalValue = totalValue.add(qty.multiply(BigDecimal.valueOf(sw.getProduct().getPriceCost())));
            }
        }
        Label kpi1 = makeWarehouseKpiCard("\uD83D\uDCE6 Produtos", String.valueOf(prodCount), "#DBEAFE", "#1E40AF");
        Label kpi2 = makeWarehouseKpiCard("\u2696\uFE0F Quantidade Total", String.format("%.1f", totalQty), "#D1FAE5", "#065F46");
        Label kpi3 = makeWarehouseKpiCard("\uD83D\uDCB0 Valor em Stock", String.format("%.0f", totalValue) + " MT", "#FEF3C7", "#92400E");
        kpiRow.getChildren().addAll(kpi1, kpi2, kpi3);

        HBox toolbar = new HBox(10);
        toolbar.setStyle("-fx-padding: 10 20; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1 0;");
        Button btnTransferir = new Button("Transferir para Filial");
        btnTransferir.setStyle("-fx-background-color: #2563EB; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        Button btnReceber = new Button("Receber Compra");
        btnReceber.setStyle("-fx-background-color: #10B981; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        Button btnRefresh = new Button("Atualizar");
        btnRefresh.setStyle("-fx-background-color: #475569; -fx-text-fill: #ffffff; -fx-font-weight: 700; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        UiUtils.attachSafe(btnTransferir, () -> openWarehouseTransferForm(owner, currentUser), systemLogService, "WH_TRANSFER");
        UiUtils.attachSafe(btnReceber, () -> openPurchaseForm(null, owner, currentUser, () -> { warehousesPane.getChildren().clear(); onRefresh.run(); }), systemLogService, "WH_RECEIVE");
        UiUtils.attachSafe(btnRefresh, () -> { warehousesPane.getChildren().clear(); onRefresh.run(); }, systemLogService, "WH_REFRESH");
        Region tbSpacer = new Region();
        HBox.setHgrow(tbSpacer, Priority.ALWAYS);
        Label addressLabel = new Label(w.getAddress() != null ? "\uD83D\uDCCD " + w.getAddress() : "");
        addressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        toolbar.getChildren().addAll(btnTransferir, btnReceber, btnRefresh, tbSpacer, addressLabel);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-tab-min-width: 140; -fx-font-size: 13px;");
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab tabStock = new Tab("Stock Actual");
        TableView<StockWarehouse> stockTable = new TableView<>();
        stockTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 1; -fx-table-cell-border-color: #F1F5F9;");
        stockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        stockTable.setPlaceholder(new Label("Sem stock neste armazém"));

        TableColumn<StockWarehouse, String> cCod = new TableColumn<>("Código");
        cCod.setPrefWidth(90);
        cCod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null && d.getValue().getProduct().getCode() != null ? d.getValue().getProduct().getCode() : "—"));

        TableColumn<StockWarehouse, String> cProd = new TableColumn<>("Produto");
        cProd.setPrefWidth(220);
        cProd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null ? d.getValue().getProduct().getName() : "—"));

        TableColumn<StockWarehouse, String> cCat = new TableColumn<>("Categoria");
        cCat.setPrefWidth(120);
        cCat.setCellValueFactory(d -> {
            Product p = d.getValue().getProduct();
            return new SimpleStringProperty(p != null && p.getCategory() != null ? p.getCategory().getName() : "—");
        });

        TableColumn<StockWarehouse, String> cQtd = new TableColumn<>("Quantidade");
        cQtd.setPrefWidth(100);
        cQtd.setCellValueFactory(d -> {
            BigDecimal q = d.getValue().getStockCurrentAmount();
            return new SimpleStringProperty(q != null ? String.format("%.2f", q) : "0.00");
        });
        cQtd.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    try {
                        double v = Double.parseDouble(item);
                        if (v <= 0) lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #EF4444;");
                        else if (v < 10) lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #F59E0B;");
                        else lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #10B981;");
                    } catch (Exception ex) { lbl.setStyle("-fx-font-weight: 700;"); }
                    setGraphic(lbl); setText(null);
                }
            }
        });

        TableColumn<StockWarehouse, String> cPrecoCusto = new TableColumn<>("Preço Custo");
        cPrecoCusto.setPrefWidth(100);
        cPrecoCusto.setCellValueFactory(d -> {
            Product p = d.getValue().getProduct();
            return new SimpleStringProperty(p != null && p.getPriceCost() != null ? String.format("%.2f MT", p.getPriceCost()) : "—");
        });

        TableColumn<StockWarehouse, String> cValorTotal = new TableColumn<>("Valor Total");
        cValorTotal.setPrefWidth(110);
        cValorTotal.setCellValueFactory(d -> {
            BigDecimal q = d.getValue().getStockCurrentAmount() != null ? d.getValue().getStockCurrentAmount() : BigDecimal.ZERO;
            Product p = d.getValue().getProduct();
            double cost = (p != null && p.getPriceCost() != null) ? p.getPriceCost() : 0.0;
            return new SimpleStringProperty(String.format("%.2f MT", q.multiply(BigDecimal.valueOf(cost))));
        });
        cValorTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #0F172A;");
                    setGraphic(lbl); setText(null);
                }
            }
        });

        stockTable.getColumns().addAll(List.of(cCod, cProd, cCat, cQtd, cPrecoCusto, cValorTotal));
        stockTable.setItems(FXCollections.observableArrayList(stockList));
        tabStock.setContent(stockTable);

        Tab tabMovs = new Tab("Movimentações Recentes");
        TableView<StockMovement> movsTable = new TableView<>();
        movsTable.setStyle("-fx-font-size: 13px; -fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-table-cell-border-color: #F1F5F9;");
        movsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        movsTable.setPlaceholder(new Label("Sem movimentações registadas"));

        TableColumn<StockMovement, String> mTipo = new TableColumn<>("Tipo");
        mTipo.setPrefWidth(140);
        mTipo.setCellValueFactory(d -> {
            String t = d.getValue().getType();
            String display = switch (t != null ? t : "") {
                case "ENTRADA_ARMAZEM" -> "Entrada";
                case "SAIDA_ARMAZEM" -> "Saída";
                default -> t != null ? t : "—";
            };
            return new SimpleStringProperty(display);
        });
        mTipo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    Label lbl = new Label(item);
                    String color = "Entrada".equals(item) ? "#10B981" : "Saída".equals(item) ? "#EF4444" : "#475569";
                    String bg = "Entrada".equals(item) ? "#D1FAE5" : "Saída".equals(item) ? "#FEE2E2" : "#F1F5F9";
                    lbl.setStyle("-fx-font-weight: 700; -fx-text-fill: " + color + "; -fx-background-color: " + bg + "; -fx-padding: 2 8; -fx-background-radius: 4;");
                    setGraphic(lbl); setText(null);
                }
            }
        });

        TableColumn<StockMovement, String> mProduto = new TableColumn<>("Produto");
        mProduto.setPrefWidth(200);
        mProduto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProduct() != null ? d.getValue().getProduct().getName() : "—"));

        TableColumn<StockMovement, String> mQtd = new TableColumn<>("Qtd");
        mQtd.setPrefWidth(80);
        mQtd.setCellValueFactory(d -> {
            BigDecimal q = d.getValue().getQtyAmount();
            return new SimpleStringProperty(q != null ? String.format("%.2f", q) : "—");
        });

        TableColumn<StockMovement, String> mRef = new TableColumn<>("Referência");
        mRef.setPrefWidth(140);
        mRef.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getReference() != null ? d.getValue().getReference() : "—"));

        TableColumn<StockMovement, String> mData = new TableColumn<>("Data");
        mData.setPrefWidth(140);
        mData.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"));

        movsTable.getColumns().addAll(List.of(mTipo, mProduto, mQtd, mRef, mData));
        List<StockMovement> movs = stockMovementRepository.findTop50ByWarehouseIdAndTypeInOrderByCreatedAtDesc(
                w.getId(), java.util.List.of("ENTRADA_ARMAZEM", "SAIDA_ARMAZEM"));
        movsTable.setItems(FXCollections.observableArrayList(movs));
        tabMovs.setContent(movsTable);

        tabPane.getTabs().addAll(tabStock, tabMovs);

        main.getChildren().addAll(header, kpiRow, toolbar, tabPane);
        warehousesPane.getChildren().add(main);
    }

    private Label makeWarehouseKpiCard(String title, String value, String bgColor, String textColor) {
        Label lbl = new Label(title + ": " + value);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + textColor + "; -fx-background-color: " + bgColor + "; -fx-padding: 10 18; -fx-background-radius: 8;");
        return lbl;
    }

    public void openModal(Parent root, Window owner) {
        try {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            ScrollPane scroll = new ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
            Scene scene = new Scene(scroll);
            stage.setScene(scene);
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            if (root instanceof Region) {
                Region region = (Region) root;
                double w = region.getPrefWidth() > 0 ? region.getPrefWidth() : 800;
                double h = region.getPrefHeight() > 0 ? region.getPrefHeight() : 600;
                stage.setWidth(Math.min(w, bounds.getWidth() * 0.95));
                stage.setHeight(Math.min(h + 40, bounds.getHeight() * 0.95));
            } else {
                stage.setMaxWidth(bounds.getWidth() * 0.95);
                stage.setMaxHeight(bounds.getHeight() * 0.95);
            }
            stage.setOnShown(e -> {
                if (owner != null) {
                    stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                    stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2);
                }
            });
            stage.showAndWait();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    public boolean isCurrentUserAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRoles() == null) return false;
        return currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() != null && role.getName().toUpperCase().contains("ADMIN"));
    }

    public void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public SaleRepository getSaleRepository() { return saleRepository; }
    public StockWarehouseRepository getStockWarehouseRepository() { return stockWarehouseRepository; }
    public ApplicationContext getApplicationContext() { return applicationContext; }
}
