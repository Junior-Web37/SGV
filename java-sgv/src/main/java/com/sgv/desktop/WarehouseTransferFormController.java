package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.WarehouseService;
import com.sgv.service.WarehouseTransferService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class WarehouseTransferFormController extends BaseFormController {

    private static final Logger log = LoggerFactory.getLogger(WarehouseTransferFormController.class);

    @FXML private ComboBox<Warehouse> warehouseCombo;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private ComboBox<Product> productCombo;
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

    private final ObservableList<ItemRow> items = FXCollections.observableArrayList();

    private ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;
    private final ObservableList<Product> comboDisplayList = FXCollections.observableArrayList();
    private boolean isRefreshingProducts = false;
    private Product lastSelectedProduct = null;

    public WarehouseTransferFormController(WarehouseRepository warehouseRepository,
                                          BranchRepository branchRepository,
                                          ProductRepository productRepository,
                                          StockWarehouseRepository stockWarehouseRepository,
                                          WarehouseService warehouseService,
                                          WarehouseTransferService transferService) {
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
        lastSelectedProduct = null;
        isRefreshingProducts = false;
        currentUser = null;
        onSave = null;
        initCommonFields();
        warehouseCombo.setItems(FXCollections.observableArrayList(warehouseRepository.findByIsActiveTrueOrderByNameAsc()));
        if (!warehouseCombo.getItems().isEmpty()) warehouseCombo.getSelectionModel().selectFirst();

        branchCombo.setItems(FXCollections.observableArrayList(branchRepository.findAll()));
        if (!branchCombo.getItems().isEmpty()) branchCombo.getSelectionModel().selectFirst();

        setupProductSearch();
        setupColumns();
        setupActionColumn();

        UiUtils.attachSafe(addItemButton, this::handleAddItem, null, "WAREHOUSE_TRANSFER_ADD_ITEM");
        UiUtils.attachSafe(saveButton, this::doSave, null, "WH_TRANSFER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "WH_TRANSFER_CANCEL");

        UiUtils.applyNumericFormatter(quantityField);

        productCombo.valueProperty().addListener((obs, o, n) -> {
            validateRealTime();
        });

        warehouseCombo.valueProperty().addListener((obs, ov, nv) -> {
            refreshSummary();
        });

        itemsTable.setItems(items);
    }

    private void setupProductSearch() {
        allProducts.setAll(productRepository.findAllActive());
        filteredProducts = new FilteredList<>(allProducts, p -> true);
        productCombo.setItems(comboDisplayList);

        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal instanceof Product) {
                lastSelectedProduct = (Product) newVal;
            }
        });

        productCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Product p) { return p == null ? "" : productDisplayText(p); }
            @Override public Product fromString(String string) { return findBestMatch(string); }
        });

        productCombo.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); setStyle(""); return; }
                setText(productDisplayText(p));
                double stock = getWarehouseStock(p);
                if (stock <= 0) {
                    setStyle("-fx-text-fill: #9ca3af; -fx-font-style: italic;");
                } else {
                    setStyle("");
                }
            }
        });
        productCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : productDisplayText(p));
            }
        });

        productCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (isRefreshingProducts) return;
            if (productCombo.isShowing()) return;
            String currentText = newVal != null ? newVal.trim() : "";
            Product selected = productCombo.getSelectionModel().getSelectedItem();
            if (selected != null && productDisplayText(selected).equals(currentText)) {
                return;
            }
            if (selected != null && !productDisplayText(selected).equals(currentText)) {
                if (currentText.length() < 60) {
                    productCombo.getSelectionModel().clearSelection();
                } else {
                    return;
                }
            }
            refreshProductSearchResults();
            if (!productCombo.isShowing() && productCombo.isFocused()) productCombo.show();
        });

        productCombo.getEditor().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleAddItem();
            }
        });

        quantityField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                handleAddItem();
            }
        });
    }

    private String productDisplayText(Product p) {
        if (p == null) return "";
        double stock = getWarehouseStock(p);
        String stockInfo = " (Stock: " + String.format("%.2f", stock) + ")";
        if (stock <= 0) stockInfo = " (Sem stock)";
        return p.getCode() + " - " + p.getName() + stockInfo;
    }

    private double getWarehouseStock(Product p) {
        Warehouse wh = warehouseCombo != null ? warehouseCombo.getValue() : null;
        if (wh == null || p == null) return 0;
        return warehouseService.getStock(wh.getId(), p.getId())
                .map(sw -> sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO)
                .orElse(java.math.BigDecimal.ZERO)
                .doubleValue();
    }

    private String extractCodeFromDisplayText(String text) {
        if (text == null) return null;
        int dashIdx = text.indexOf(" - ");
        if (dashIdx > 0) return text.substring(0, dashIdx).trim();
        return null;
    }

    private Product findBestMatch(String text) {
        if (text == null || text.isBlank()) return null;
        String lower = text.toLowerCase();

        Product match = allProducts.stream()
                .filter(p -> p != null)
                .filter(p -> {
                    if (p.getCode() != null && p.getCode().equalsIgnoreCase(text)) return true;
                    if (p.getName() != null && p.getName().equalsIgnoreCase(text)) return true;
                    if (p.getCode() != null && p.getCode().toLowerCase().startsWith(lower)) return true;
                    if (p.getName() != null && p.getName().toLowerCase().startsWith(lower)) return true;
                    if (p.getName() != null && p.getName().toLowerCase().contains(lower)) return true;
                    if (p.getCode() != null && p.getCode().toLowerCase().contains(lower)) return true;
                    return false;
                })
                .min(Comparator.comparingInt((Product p) -> {
                    if (p.getCode() != null && p.getCode().equalsIgnoreCase(text)) return 1;
                    if (p.getName() != null && p.getName().equalsIgnoreCase(text)) return 2;
                    if (p.getCode() != null && p.getCode().toLowerCase().startsWith(lower)) return 3;
                    if (p.getName() != null && p.getName().toLowerCase().startsWith(lower)) return 4;
                    if (p.getName() != null && p.getName().toLowerCase().contains(lower)) return 5;
                    return 6;
                }).thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .orElse(null);
        if (match != null) return match;

        String extractedCode = extractCodeFromDisplayText(text);
        if (extractedCode != null) {
            return allProducts.stream()
                    .filter(p -> p != null && p.getCode() != null)
                    .filter(p -> p.getCode().equalsIgnoreCase(extractedCode))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void refreshProductSearchResults() {
        if (isRefreshingProducts) return;
        isRefreshingProducts = true;
        try {
            String search = productCombo.getEditor() != null ? productCombo.getEditor().getText() : null;
            boolean emptySearch = search == null || search.isBlank();
            Warehouse wh = warehouseCombo != null ? warehouseCombo.getValue() : null;

            List<Long> inStockIds = wh != null
                    ? stockWarehouseRepository.findByWarehouseId(wh.getId()).stream()
                        .filter(sw -> sw.getStockCurrentAmount() != null && sw.getStockCurrentAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                        .map(sw -> sw.getProduct().getId()).collect(Collectors.toList())
                    : allProducts.stream().map(Product::getId).collect(Collectors.toList());

            filteredProducts.setPredicate(p -> {
                if (wh != null && !inStockIds.contains(p.getId())) return false;
                if (emptySearch) return true;
                String lower = search.toLowerCase();
                return (p.getCode() != null && p.getCode().toLowerCase().contains(lower)) ||
                       (p.getName() != null && p.getName().toLowerCase().contains(lower));
            });

            List<Product> snapshot = filteredProducts.stream()
                    .sorted(Comparator.comparingDouble((Product p) -> getWarehouseStock(p)).reversed()
                            .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();

            boolean hadPrevValue = productCombo.getValue() != null;
            if (productCombo.isShowing()) productCombo.hide();
            if (snapshot.isEmpty()) {
                comboDisplayList.setAll(snapshot);
                productCombo.setDisable(true);
                productCombo.setPromptText("Sem stock disponível no armazém");
            } else {
                productCombo.setDisable(false);
                productCombo.setPromptText("Pesquisar produto...");
                comboDisplayList.setAll(snapshot);
                if (hadPrevValue && snapshot.stream().anyMatch(p -> p.getId().equals(productCombo.getValue().getId()))) {
                    productCombo.getSelectionModel().select(productCombo.getValue());
                }
            }
            if (!emptySearch && !productCombo.isShowing() && productCombo.isFocused() && !snapshot.isEmpty()) {
                productCombo.show();
            }
        } finally {
            isRefreshingProducts = false;
        }
    }

    private void setupColumns() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
    }

    private void setupActionColumn() {
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("X");
            {
                btn.setStyle("-fx-background-color:transparent; -fx-text-fill:#EF4444; -fx-font-weight:900; -fx-cursor:hand;");
                btn.setOnAction(UiUtils.safeOnAction(() -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    items.remove(row);
                    validateRealTime();
                }, null, "WAREHOUSE_TRANSFER_REMOVE_ITEM"));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void refreshSummary() {
        productCombo.setValue(null);
        productCombo.getEditor().clear();
        refreshProductSearchResults();
        stockSummaryLabel.setText("");
    }

    private void handleAddItem() {
        errorLabel.setVisible(false);
        Warehouse wh = warehouseCombo.getValue();
        Product p = resolveSelectedProduct();
        if (wh == null) { showError("Seleccione o armazém de origem"); return; }
        if (p == null) { showError("Produto não encontrado"); return; }
        double qty;
        try { qty = Double.parseDouble(quantityField.getText().trim()); }
        catch (NumberFormatException e) { showError("Quantidade inválida"); return; }
        if (qty <= 0) { showError("Quantidade deve ser > 0"); return; }

        java.math.BigDecimal stock = warehouseService.getStock(wh.getId(), p.getId())
                .map(sw -> sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO)
                .orElse(java.math.BigDecimal.ZERO);
        if (stock.compareTo(java.math.BigDecimal.valueOf(qty)) < 0) { showError("Stock insuficiente. Disponível: " + stock); return; }

        for (ItemRow row : items) {
            if (row.product.getId().equals(p.getId())) {
                if (java.math.BigDecimal.valueOf(row.getQuantityAsDouble()).add(java.math.BigDecimal.valueOf(qty)).compareTo(stock) > 0) {
                    showError("Stock insuficiente para o total (linha + novo)");
                    return;
                }
                row.addQuantity(qty);
                itemsTable.refresh();
                clearProductSelection();
                validateRealTime();
                return;
            }
        }
        items.add(new ItemRow(p, stock.doubleValue(), qty));
        clearProductSelection();
        validateRealTime();
    }

    private Product resolveSelectedProduct() {
        Product val = productCombo.getValue();
        if (val != null) return val;

        Product selected = productCombo.getSelectionModel().getSelectedItem();
        if (selected != null) return selected;

        if (lastSelectedProduct != null) {
            String editorText = productCombo.getEditor() != null ? productCombo.getEditor().getText().trim() : "";
            if (!editorText.isBlank() && productDisplayText(lastSelectedProduct).equals(editorText)) {
                return lastSelectedProduct;
            }
        }

        String text = productCombo.getEditor() != null ? productCombo.getEditor().getText().trim() : "";
        if (!text.isBlank()) {
            return findBestMatch(text);
        }
        return null;
    }

    private void clearProductSelection() {
        productCombo.setValue(null);
        productCombo.getEditor().clear();
        quantityField.setText("1");
        refreshProductSearchResults();
        Platform.runLater(productCombo::requestFocus);
    }

    @Override
    protected void validateRealTime() {
        boolean valid = !items.isEmpty();
        formValidProperty.set(valid);
        if (valid) hideError();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        errorLabel.setVisible(false);
        Warehouse wh = warehouseCombo.getValue();
        Branch br = branchCombo.getValue();
        if (wh == null) { showError("Seleccione o armazém de origem"); return; }
        if (br == null) { showError("Seleccione a loja de destino"); return; }
        if (items.isEmpty()) { showError("Adicione pelo menos um item"); return; }

        try {
            WarehouseTransfer t = new WarehouseTransfer();
            Warehouse managedWarehouse = warehouseRepository.findById(wh.getId()).orElse(wh);
            Branch managedBranch = branchRepository.findById(br.getId()).orElse(br);
            t.setWarehouse(managedWarehouse);
            t.setBranch(managedBranch);
            t.setNotes("");
            for (ItemRow row : items) {
                WarehouseTransferItem item = new WarehouseTransferItem();
                Product managedProduct = productRepository.findById(row.product.getId()).orElse(row.product);
                item.setProduct(managedProduct);
                item.setQuantity(row.getQuantityAsDouble());
                item.setQuantityReceived(0.0);
                t.getItems().add(item);
            }

            if (currentUser == null) {
                showError("Utilizador não autenticado");
                return;
            }

            WarehouseTransfer saved = transferService.create(t, currentUser);
            transferService.complete(saved.getId(), currentUser);
            WarehouseTransfer done = transferService.listAll().stream()
                    .filter(x -> x.getId().equals(saved.getId()))
                    .findFirst().orElse(saved);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("OK");
            ok.setHeaderText("Transferência concluída com sucesso!");
            ok.setContentText(done.getSeries() + "/" + done.getDocumentNumber() + " - Stock actualizado.");
            ok.showAndWait();
            doCancel();
        } catch (Exception ex) {
            log.error("Erro ao criar transferência", ex);
            showError("Erro: " + ex.getMessage());
        }
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
        public String getStock() { return String.format("%.2f", stock); }
        public String getQuantity() { return String.format("%.2f", quantity.get()); }
        public double getQuantityAsDouble() { return quantity.get(); }
        public void addQuantity(double q) { quantity.set(quantity.get() + q); }
    }
}
