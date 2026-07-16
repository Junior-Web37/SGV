package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.repository.*;
import com.sgv.service.SystemLogService;
import com.sgv.service.WarehouseService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PurchaseFormController {

    @FXML private Label titleLabel;
    @FXML private TextField invoiceNumberField;
    @FXML private DatePicker invoiceDatePicker;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField quantityField;
    @FXML private TextField unitCostField;
    @FXML private Button addItemButton;
    @FXML private TableView<PurchaseItem> itemsTable;
    @FXML private TableColumn<PurchaseItem, String> productColumn;
    @FXML private TableColumn<PurchaseItem, String> quantityColumn;
    @FXML private TableColumn<PurchaseItem, String> priceColumn;
    @FXML private TableColumn<PurchaseItem, String> totalColumn;
    @FXML private TableColumn<PurchaseItem, String> actionColumn;
    @FXML private Label errorLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label totalLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    
    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseService warehouseService;
    private final BranchRepository branchRepository;
    private final SystemLogService systemLogService;

    private Purchase editingPurchase;
    private User currentUser;
    private Runnable onSave;
    private final ObservableList<PurchaseItem> items = FXCollections.observableArrayList();
    
    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public PurchaseFormController(PurchaseRepository purchaseRepository,
                                  ProductRepository productRepository,
                                  WarehouseRepository warehouseRepository,
                                  WarehouseService warehouseService,
                                  BranchRepository branchRepository,
                                  SystemLogService systemLogService) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseService = warehouseService;
        this.branchRepository = branchRepository;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        productCombo.setItems(FXCollections.observableArrayList(productRepository.findAll()));
        productCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Product p) { return p != null ? p.getCode() + " — " + p.getName() : ""; }
            public Product fromString(String s) { return null; }
        });
        productCombo.setOnAction(e -> {
            Product p = productCombo.getValue();
            if (p != null && p.getPriceCost() != null) {
                unitCostField.setText(String.format("%.2f", p.getPriceCost()).replace(",", "."));
            }
        });

        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        if (invoiceDatePicker != null) invoiceDatePicker.setValue(LocalDate.now());

        // Table columns
        productColumn.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getProduct() != null ? d.getValue().getProduct().getName() : ""));
        quantityColumn.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.2f %s", d.getValue().getQuantity(),
                d.getValue().getProduct() != null && d.getValue().getProduct().getUnit() != null
                    ? d.getValue().getProduct().getUnit().getAbbreviation() : "")));
        priceColumn.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.2f MT", d.getValue().getCostPrice())));
        totalColumn.setCellValueFactory(d -> new SimpleStringProperty(
            String.format("%.2f MT", d.getValue().getSubtotal())));
        actionColumn.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("🗑");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    PurchaseItem item = getTableView().getItems().get(getIndex());
                    items.remove(item);
                    updateTotals();
                });
            }
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btn);
            }
        });

        itemsTable.setItems(items);

        addItemButton.setOnAction(e -> addItem());
        saveButton.setOnAction(e -> save());
        cancelButton.setOnAction(e -> close());
        
        // UX: Transição de entrada fluida (Fade-in)
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // UX/MVVM: Data-Binding do botão de salvar
        saveButton.disableProperty().bind(formValidProperty.not());

        // Setup real-time listeners
        setupRealTimeValidation();
    }
    
    private void setupRealTimeValidation() {
        invoiceNumberField.textProperty().addListener((obs, o, n) -> validateRealTime());
        invoiceDatePicker.valueProperty().addListener((obs, o, n) -> validateRealTime());
        
        // Ouça mudanças na lista para validar também
        items.addListener((javafx.collections.ListChangeListener.Change<? extends PurchaseItem> c) -> validateRealTime());
        
        javafx.application.Platform.runLater(this::validateRealTime);
    }
    
    private void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;
        
        if (invoiceNumberField.getText() == null || invoiceNumberField.getText().isBlank()) { valid = false; }
        if (invoiceDatePicker.getValue() == null) { valid = false; }
        if (items.isEmpty()) {
            valid = false;
        }
        
        formValidProperty.set(valid);
        if (valid && errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else if (!valid && errorLabel != null) {
            errorLabel.setText("Preencha fornecedor, número da factura e adicione pelo menos um item.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: 600;");
        }
    }

    private void addItem() {
        Product product = productCombo.getValue();
        if (product == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Selecione um produto.", ButtonType.OK);
            a.setHeaderText(null); a.showAndWait();
            return;
        }
        double qty = 0;
        double cost = 0;
        try {
            qty = Double.parseDouble((quantityField.getText() != null ? quantityField.getText() : "0").replace(",", "."));
            cost = Double.parseDouble((unitCostField.getText() != null ? unitCostField.getText() : "0").replace(",", "."));
        } catch (Exception ex) { /* will be caught below */ }
        
        if (qty <= 0) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Quantidade deve ser maior que 0.", ButtonType.OK);
            a.setHeaderText(null); a.showAndWait();
            return;
        }
        if (cost < 0) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Custo não pode ser negativo.", ButtonType.OK);
            a.setHeaderText(null); a.showAndWait();
            return;
        }

        PurchaseItem item = new PurchaseItem();
        item.setProduct(product);
        item.setQuantity(qty);
        item.setCostPrice(cost);
        item.setSubtotal(qty * cost);
        items.add(item);

        productCombo.setValue(null);
        quantityField.clear();
        unitCostField.clear();
        updateTotals();
    }

    private void updateTotals() {
        double sub = items.stream().mapToDouble(i -> i.getSubtotal() != null ? i.getSubtotal() : 0.0).sum();
        subtotalLabel.setText(String.format("%.2f MT", sub));
        totalLabel.setText(String.format("%.2f MT", sub));
    }

    private void save() {
        if (!formValidProperty.get()) return;

        saveButton.setVisible(false);
        if (saveSpinner != null) {
            saveSpinner.setVisible(true);
            saveSpinner.setManaged(true);
        }
        cancelButton.setDisable(true);
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                Purchase p = editingPurchase != null ? editingPurchase : new Purchase();
                p.setInvoiceNumber(invoiceNumberField.getText() != null ? invoiceNumberField.getText().trim() : "");
                p.setState("RECEIVED");
                p.setNotes("");
                p.setUser(currentUser);
                if (currentUser != null) p.setBranch(currentUser.getBranch());
                if (invoiceDatePicker.getValue() != null)
                    p.setPurchaseDate(invoiceDatePicker.getValue().atStartOfDay());

                double total = items.stream().mapToDouble(i -> i.getSubtotal() != null ? i.getSubtotal() : 0.0).sum();
                p.setSubtotal(total);
                p.setTotal(total);

                // Associate items to purchase and update stock
                List<PurchaseItem> savedItems = new ArrayList<>();
                for (PurchaseItem item : items) {
                    item.setPurchase(p);
                    savedItems.add(item);
                }
                p.getItems().clear();
                p.getItems().addAll(savedItems);

                Warehouse targetWarehouse = p.getTargetWarehouse();
                if (targetWarehouse == null) {
                    targetWarehouse = warehouseRepository.findByIsActiveTrueOrderByNameAsc().stream().findFirst()
                            .orElseThrow(() -> new IllegalStateException("Nenhum armazém activo encontrado."));
                    p.setTargetWarehouse(targetWarehouse);
                }

                purchaseRepository.save(p);

                // Update warehouse stock for each item if state is RECEIVED
                if ("RECEIVED".equals(p.getState())) {
                    for (PurchaseItem item : savedItems) {
                        if (item.getProduct() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                            warehouseService.addStock(
                                targetWarehouse.getId(),
                                item.getProduct().getId(),
                                item.getQuantity(),
                                p.getInvoiceNumber() != null ? p.getInvoiceNumber() : "COMPRA",
                                currentUser);
                        }
                    }
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("PURCHASE_SAVE_FAILED", "Erro ao salvar compra: " + ex.getMessage(), ex);
            showAlert("Erro ao salvar: " + ex.getMessage());
            
            saveButton.setVisible(true);
            if (saveSpinner != null) {
                saveSpinner.setVisible(false);
                saveSpinner.setManaged(false);
            }
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }

    public void setPurchase(Purchase purchase) {
        this.editingPurchase = purchase;
        if (purchase != null) {
            if (titleLabel != null) titleLabel.setText("Editar Compra");
            invoiceNumberField.setText(purchase.getInvoiceNumber() != null ? purchase.getInvoiceNumber() : "");
            if (purchase.getPurchaseDate() != null && invoiceDatePicker != null)
                invoiceDatePicker.setValue(purchase.getPurchaseDate().toLocalDate());
            items.setAll(purchase.getItems());
            updateTotals();
        }
    }

    public void setCurrentUser(User user) { this.currentUser = user; }
    public void setOnSave(Runnable onSave) { this.onSave = onSave; }

    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
}
