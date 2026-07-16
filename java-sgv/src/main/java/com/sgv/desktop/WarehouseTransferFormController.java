package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.WarehouseService;
import com.sgv.service.WarehouseTransferService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WarehouseTransferFormController {

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
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final StockWarehouseRepository stockWarehouseRepository;
    private final WarehouseService warehouseService;
    private final WarehouseTransferService transferService;

    private final ObservableList<ItemRow> items = FXCollections.observableArrayList();
    private User currentUser;

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

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    @FXML
    public void initialize() {
        warehouseCombo.setItems(FXCollections.observableArrayList(warehouseRepository.findByIsActiveTrueOrderByNameAsc()));
        if (!warehouseCombo.getItems().isEmpty()) warehouseCombo.getSelectionModel().selectFirst();

        branchCombo.setItems(FXCollections.observableArrayList(branchRepository.findAll()));
        if (!branchCombo.getItems().isEmpty()) branchCombo.getSelectionModel().selectFirst();

        setupProductCombo();
        setupColumns();
        setupActionColumn();

        addItemButton.setOnAction(e -> handleAddItem());
        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Resumo do stock
        warehouseCombo.valueProperty().addListener((obs, ov, nv) -> refreshSummary());
        productCombo.valueProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                String stock = warehouseService
                        .getStock(warehouseCombo.getValue() != null ? warehouseCombo.getValue().getId() : null, nv.getId())
                        .map(sw -> {
                            java.math.BigDecimal current = sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO;
                            return String.format("%.2f", current.doubleValue());
                        })
                        .orElse("0.00");
                stockSummaryLabel.setText("Stock: " + stock);
            } else {
                stockSummaryLabel.setText("");
            }
        });
    }

    private void setupProductCombo() {
        productCombo.setItems(FXCollections.observableArrayList(productRepository.findAll()));
        productCombo.setConverter(new StringConverter<Product>() {
            @Override public String toString(Product p) {
                return p == null ? "" : p.getCode() + " - " + p.getName();
            }
            @Override public Product fromString(String string) {
                return productCombo.getItems().stream()
                        .filter(p -> (p.getCode() + " - " + p.getName()).equals(string))
                        .findFirst().orElse(null);
            }
        });
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
                btn.setOnAction(e -> {
                    ItemRow row = getTableView().getItems().get(getIndex());
                    items.remove(row);
                });
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
        productCombo.getItems().setAll(productRepository.findAll());
        stockSummaryLabel.setText("");
    }

    private void handleAddItem() {
        errorLabel.setVisible(false);
        Warehouse wh = warehouseCombo.getValue();
        Product p = productCombo.getValue();
        if (wh == null) { showError("Seleccione o armazém de origem"); return; }
        if (p == null) { showError("Seleccione um produto"); return; }
        double qty;
        try { qty = Double.parseDouble(quantityField.getText().trim()); }
        catch (NumberFormatException e) { showError("Quantidade inválida"); return; }
        if (qty <= 0) { showError("Quantidade deve ser > 0"); return; }

        java.math.BigDecimal stock = warehouseService.getStock(wh.getId(), p.getId())
                .map(sw -> sw.getStockCurrentAmount() != null ? sw.getStockCurrentAmount() : java.math.BigDecimal.ZERO)
                .orElse(java.math.BigDecimal.ZERO);
        if (stock.compareTo(java.math.BigDecimal.valueOf(qty)) < 0) { showError("Stock insuficiente. Disponível: " + stock); return; }

        // Se já existe na lista, soma
        for (ItemRow row : items) {
            if (row.product.getId().equals(p.getId())) {
                if (java.math.BigDecimal.valueOf(row.getQuantityAsDouble()).add(java.math.BigDecimal.valueOf(qty)).compareTo(stock) > 0) {
                    showError("Stock insuficiente para o total (linha + novo)");
                    return;
                }
                row.addQuantity(qty);
                itemsTable.refresh();
                clearProductSelection();
                return;
            }
        }
        items.add(new ItemRow(p, stock.doubleValue(), qty));
        clearProductSelection();
    }

    private void clearProductSelection() {
        productCombo.setValue(null);
        productCombo.getEditor().clear();
        quantityField.setText("1");
        productCombo.requestFocus();
    }

    private void doSave() {
        errorLabel.setVisible(false);
        Warehouse wh = warehouseCombo.getValue();
        Branch br = branchCombo.getValue();
        if (wh == null) { showError("Seleccione o armazém de origem"); return; }
        if (br == null) { showError("Seleccione a loja de destino"); return; }
        if (items.isEmpty()) { showError("Adicione pelo menos um item"); return; }

        try {
            WarehouseTransfer t = new WarehouseTransfer();
            t.setWarehouse(wh);
            t.setBranch(br);
            t.setNotes("");
            for (ItemRow row : items) {
                WarehouseTransferItem item = new WarehouseTransferItem();
                item.setProduct(row.product);
                item.setQuantity(row.getQuantityAsDouble());
                item.setQuantityReceived(0.0);
                t.getItems().add(item);
            }

            if (currentUser == null) {
                showError("Utilizador não autenticado");
                return;
            }

            WarehouseTransfer saved = transferService.create(t, currentUser);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("OK");
            ok.setHeaderText("Transferência criada com sucesso!");
            ok.setContentText(saved.getSeries() + "/" + saved.getDocumentNumber() + " - Aguardando aprovação.");
            ok.showAndWait();
            doCancel();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erro: " + ex.getMessage());
        }
    }

    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    // Linha da tabela
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
