package com.sgv.desktop;

import com.sgv.entity.Branch;
import com.sgv.entity.Product;
import com.sgv.entity.StockWarehouse;
import com.sgv.entity.Warehouse;
import com.sgv.entity.WarehouseTransfer;
import com.sgv.entity.WarehouseTransferItem;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.StockWarehouseRepository;
import com.sgv.repository.WarehouseRepository;
import com.sgv.service.WarehouseService;
import com.sgv.service.WarehouseTransferService;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class WarehouseTransferFormController extends BaseFormController {

    private static final Logger log = LoggerFactory.getLogger(WarehouseTransferFormController.class);

    @FXML private ComboBox<Warehouse> warehouseCombo;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private TextField productSearchField;
    @FXML private ListView<Product> productList;
    @FXML private TextField quantityField;
    @FXML private Button addItemButton;
    @FXML private TableView<ItemRow> itemsTable;
    @FXML private TableColumn<ItemRow, String> codeColumn;
    @FXML private TableColumn<ItemRow, String> productColumn;
    @FXML private TableColumn<ItemRow, String> stockColumn;
    @FXML private TableColumn<ItemRow, String> quantityColumn;
    @FXML private TableColumn<ItemRow, Void> actionColumn;
    @FXML private Label stockSummaryLabel;

    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final StockWarehouseRepository stockWarehouseRepository;
    private final WarehouseService warehouseService;
    private final WarehouseTransferService transferService;
    private final com.sgv.service.SystemLogService systemLogService;

    private final ObservableList<ItemRow> items = FXCollections.observableArrayList();
    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final ObservableList<Product> productChoices = FXCollections.observableArrayList();
    private final Map<Long, BigDecimal> warehouseStockMap = new HashMap<>();
    private Product selectedProduct;

    public WarehouseTransferFormController(WarehouseRepository warehouseRepository,
                                           BranchRepository branchRepository,
                                           ProductRepository productRepository,
                                           StockWarehouseRepository stockWarehouseRepository,
                                           WarehouseService warehouseService,
                                           WarehouseTransferService transferService, com.sgv.service.SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
        this.warehouseRepository = warehouseRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
        this.stockWarehouseRepository = stockWarehouseRepository;
        this.warehouseService = warehouseService;
        this.transferService = transferService;
    }

    @FXML
    public void initialize() {
        items.clear();
        selectedProduct = null;
        warehouseStockMap.clear();
        initCommonFields();
        bindSimpleCombo(warehouseCombo,
                warehouseRepository.findByIsActiveTrueOrderByNameAsc(),
                w -> w.getName() + " (" + w.getCode() + ")",
                "Seleccione o armazém...");
        bindSimpleCombo(branchCombo,
                branchRepository.findAll(),
                Branch::getName,
                "Seleccione a filial...");

        setupProductSearch();
        setupColumns();
        setupActionColumn();

        UiUtils.attachSafe(addItemButton, this::handleAddItem, null, "WAREHOUSE_TRANSFER_ADD_ITEM");
        UiUtils.attachSafe(saveButton, this::doSave, null, "WH_TRANSFER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "WH_TRANSFER_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);
        UiUtils.applyHoverElevation(addItemButton);
        UiUtils.applyPressFeedback(addItemButton);

        UiUtils.applyNumericFormatter(quantityField);

        warehouseCombo.valueProperty().addListener((obs, ov, nv) -> {
            loadWarehouseStockCache(nv);
            refreshProductList();
            validateRealTime();
        });

        branchCombo.valueProperty().addListener((obs, ov, nv) -> validateRealTime());

        itemsTable.setItems(items);
        items.addListener((javafx.collections.ListChangeListener.Change<? extends ItemRow> c) -> validateRealTime());

        quantityField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                handleAddItem();
            }
        });

        Platform.runLater(() -> {
            if (warehouseCombo.getValue() == null && warehouseCombo.getItems() != null && !warehouseCombo.getItems().isEmpty()) {
                warehouseCombo.setValue(warehouseCombo.getItems().get(0));
            }
            if (branchCombo.getValue() == null && branchCombo.getItems() != null && !branchCombo.getItems().isEmpty()) {
                branchCombo.setValue(branchCombo.getItems().get(0));
            }
            loadWarehouseStockCache(warehouseCombo.getValue());
            refreshProductList();
            validateRealTime();
        });
    }

    private <T> void bindSimpleCombo(ComboBox<T> combo, List<T> values, java.util.function.Function<T, String> label, String emptyText) {
        if (combo == null) return;
        UiUtils.hardenComboBox(combo);
        combo.setItems(FXCollections.observableArrayList(values != null ? values : List.of()));
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(T value) { return value != null ? label.apply(value) : ""; }
            @Override public T fromString(String s) { return null; }
        });
        combo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : label.apply(value));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(T value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? emptyText : label.apply(value));
            }
        });
    }

    private void loadWarehouseStockCache(Warehouse wh) {
        warehouseStockMap.clear();
        if (wh == null || wh.getId() == null) return;
        List<StockWarehouse> stocks = stockWarehouseRepository.findByWarehouseId(wh.getId());
        for (StockWarehouse sw : stocks) {
            if (sw.getProduct() != null && sw.getProduct().getId() != null) {
                BigDecimal qty = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : BigDecimal.ZERO;
                warehouseStockMap.put(sw.getProduct().getId(), qty);
            }
        }
    }

    private BigDecimal getAvailableStock(Product p) {
        if (p == null || p.getId() == null) return BigDecimal.ZERO;
        return warehouseStockMap.getOrDefault(p.getId(), BigDecimal.ZERO);
    }

    private void setupProductSearch() {
        allProducts.setAll(productRepository.findAllActive().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getService()))
                .toList());
        if (productList != null) {
            productList.setItems(productChoices);
            productList.setPlaceholder(new Label("Sem artigos para mostrar"));
            productList.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Product p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    BigDecimal stock = getAvailableStock(p);
                    String unit = p.getUnit() != null ? p.getUnit().getAbbreviation() : "UN";
                    setText(String.format("%s - %s | Stock: %,.1f %s", p.getCode(), p.getName(), stock, unit));
                    if (stock.compareTo(BigDecimal.ZERO) <= 0) {
                        setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                    } else {
                        setStyle("-fx-text-fill: #0F172A; -fx-font-weight: 600;");
                    }
                }
            });
            productList.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
                selectedProduct = nv;
                updateSelectedProductStock(nv);
                validateRealTime();
            });
            productList.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && productList.getSelectionModel().getSelectedItem() != null) {
                    handleAddItem();
                }
            });
        }
        if (productSearchField != null) {
            UiUtils.setupDebounce(productSearchField, () -> filterProductsInMemory(productSearchField.getText()), 120);
            productSearchField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    if (selectedProduct == null && productList != null && !productChoices.isEmpty()) {
                        productList.getSelectionModel().selectFirst();
                        selectedProduct = productList.getSelectionModel().getSelectedItem();
                    }
                    handleAddItem();
                } else if (e.getCode() == KeyCode.DOWN && productList != null && !productChoices.isEmpty()) {
                    e.consume();
                    productList.requestFocus();
                    if (productList.getSelectionModel().getSelectedItem() == null) {
                        productList.getSelectionModel().selectFirst();
                    }
                }
            });
        }
    }

    private void filterProductsInMemory(String text) {
        String query = text != null ? text.trim().toLowerCase() : "";
        List<Product> matches = allProducts.stream()
                .filter(p -> {
                    if (query.isEmpty()) return true;
                    boolean codeMatch = p.getCode() != null && p.getCode().toLowerCase().contains(query);
                    boolean nameMatch = p.getName() != null && p.getName().toLowerCase().contains(query);
                    return codeMatch || nameMatch;
                })
                .sorted(Comparator.comparing((Product p) -> getAvailableStock(p)).reversed()
                        .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(80)
                .toList();
        productChoices.setAll(matches);
        if (selectedProduct != null && matches.stream().noneMatch(p -> p.getId().equals(selectedProduct.getId()))) {
            selectedProduct = null;
            if (productList != null) productList.getSelectionModel().clearSelection();
            updateSelectedProductStock(null);
        }
    }

    private void refreshProductList() {
        filterProductsInMemory(productSearchField != null ? productSearchField.getText() : "");
    }

    private void updateSelectedProductStock(Product p) {
        if (stockSummaryLabel == null) return;
        if (p == null) {
            stockSummaryLabel.setText("");
            return;
        }
        BigDecimal stock = getAvailableStock(p);
        String unit = p.getUnit() != null ? p.getUnit().getAbbreviation() : "UN";
        if (stock.compareTo(BigDecimal.ZERO) > 0) {
            stockSummaryLabel.setText(String.format("Disponível no armazém: %,.1f %s", stock, unit));
            stockSummaryLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: 700;");
        } else {
            stockSummaryLabel.setText("Sem existências disponíveis neste armazém");
            stockSummaryLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 700;");
        }
    }

    private void setupColumns() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-font-weight: 800; -fx-padding: 3 8; -fx-background-radius: 4; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    items.remove(row);
                    validateRealTime();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    private void handleAddItem() {
        hideError();
        Warehouse wh = warehouseCombo.getValue();
        Branch br = branchCombo.getValue();
        Product p = resolveSelectedProduct();

        if (wh == null) { showError("Seleccione o armazém de origem."); return; }
        if (br == null) { showError("Seleccione a filial de destino."); return; }
        if (p == null) { showError("Seleccione o produto a transferir."); return; }

        double qty;
        try {
            qty = Double.parseDouble(quantityField.getText().trim().replace(",", "."));
        } catch (Exception e) {
            showError("Quantidade inválida.");
            return;
        }
        if (qty <= 0) { showError("Quantidade deve ser maior que zero."); return; }

        BigDecimal availableStock = getAvailableStock(p);
        if (availableStock.compareTo(BigDecimal.valueOf(qty)) < 0) {
            showError(String.format("Stock insuficiente no armazém. Disponível: %,.1f %s", availableStock, p.getUnit() != null ? p.getUnit().getAbbreviation() : "UN"));
            return;
        }

        // Se já existe na tabela, somar quantidade
        for (ItemRow row : items) {
            if (row.product.getId().equals(p.getId())) {
                double newTotalQty = row.getQuantityAsDouble() + qty;
                if (BigDecimal.valueOf(newTotalQty).compareTo(availableStock) > 0) {
                    showError(String.format("Quantidade total (%,.1f) excede o stock disponível (%,.1f).", newTotalQty, availableStock));
                    return;
                }
                row.addQuantity(qty);
                itemsTable.refresh();
                clearProductSelection();
                validateRealTime();
                return;
            }
        }

        items.add(new ItemRow(p, availableStock.doubleValue(), qty));
        clearProductSelection();
        validateRealTime();
    }

    private Product resolveSelectedProduct() {
        if (selectedProduct != null) return selectedProduct;
        if (productList != null && productList.getSelectionModel().getSelectedItem() != null) {
            return productList.getSelectionModel().getSelectedItem();
        }
        String text = productSearchField != null && productSearchField.getText() != null
                ? productSearchField.getText().trim() : "";
        if (!text.isEmpty()) {
            for (Product prod : allProducts) {
                if (prod.getCode() != null && prod.getCode().equalsIgnoreCase(text)) return prod;
                if (prod.getName() != null && prod.getName().equalsIgnoreCase(text)) return prod;
            }
            for (Product prod : productChoices) {
                if (prod.getName() != null && prod.getName().toLowerCase().startsWith(text.toLowerCase())) return prod;
            }
        }
        return productChoices.size() == 1 ? productChoices.get(0) : null;
    }

    private void clearProductSelection() {
        selectedProduct = null;
        if (productList != null) productList.getSelectionModel().clearSelection();
        if (productSearchField != null) productSearchField.clear();
        quantityField.setText("1");
        if (stockSummaryLabel != null) stockSummaryLabel.setText("");
        refreshProductList();
        Platform.runLater(() -> {
            if (productSearchField != null) productSearchField.requestFocus();
        });
    }

    @Override
    protected void validateRealTime() {
        boolean valid = true;
        StringBuilder errors = new StringBuilder();

        if (warehouseCombo.getValue() == null) { errors.append("Seleccione o armazém de origem. "); valid = false; }
        if (branchCombo.getValue() == null) { errors.append("Seleccione a filial de destino. "); valid = false; }
        if (items.isEmpty()) { errors.append("Adicione pelo menos um produto ao lote. "); valid = false; }

        formValidProperty.set(valid);
        if (!valid) showError(errors.toString().trim());
        else hideError();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        hideError();

        Warehouse wh = warehouseCombo.getValue();
        Branch br = branchCombo.getValue();

        showSaveSpinner();

        javafx.concurrent.Task<WarehouseTransfer> task = new javafx.concurrent.Task<>() {
            @Override
            protected WarehouseTransfer call() throws Exception {
                WarehouseTransfer t = new WarehouseTransfer();
                Warehouse managedWarehouse = warehouseRepository.findById(wh.getId()).orElse(wh);
                Branch managedBranch = branchRepository.findById(br.getId()).orElse(br);
                t.setWarehouse(managedWarehouse);
                t.setBranch(managedBranch);
                t.setNotes("Transferência emitida via SGV Desktop");

                for (ItemRow row : items) {
                    WarehouseTransferItem item = new WarehouseTransferItem();
                    Product managedProduct = productRepository.findById(row.product.getId()).orElse(row.product);
                    item.setProduct(managedProduct);
                    item.setQuantity(row.getQuantityAsDouble());
                    item.setQuantityReceived(0.0);
                    t.getItems().add(item);
                }

                if (currentUser == null) {
                    throw new IllegalStateException("Utilizador não autenticado.");
                }

                return transferService.createAndComplete(t, currentUser);
            }
        };

        task.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "TRANSFERENCIA_EMITIDA", "Guia de transferência emitida com sucesso");
            WarehouseTransfer done = task.getValue();
            SgvDialog.info("Transferência Concluída",
                String.format("Guia de Transferência Emitida com Sucesso!\n\nGuia %s/%d concluída.\n\nStock do armazém debitado e filial creditada.",
                    done.getSeries(), done.getDocumentNumber()));
            if (onSave != null) onSave.run();
            doCancel();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            systemLogService.logError("TRANSFER_SAVE_FAILED", "Erro ao salvar transferência: " + (ex != null ? ex.getMessage() : ""), ex);
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            log.error("Erro ao gravar transferência", ex);
            showError("Erro ao transferir: " + msg);
            hideSaveSpinner();
        });

        UiUtils.runTask(task);
    }

    public static class ItemRow {
        private final Product product;
        private final double stock;
        private final SimpleDoubleProperty quantity = new SimpleDoubleProperty(1.0);

        public ItemRow(Product p, double stock, double qty) {
            this.product = p;
            this.stock = stock;
            this.quantity.set(qty);
        }
        public String getCode() { return product.getCode(); }
        public String getProductName() { return product.getName(); }
        public String getStock() { return String.format(Locale.US, "%.1f", stock); }
        public String getQuantity() { return String.format(Locale.US, "%.1f", quantity.get()); }
        public double getQuantityAsDouble() { return quantity.get(); }
        public void addQuantity(double q) { quantity.set(quantity.get() + q); }
    }
}
