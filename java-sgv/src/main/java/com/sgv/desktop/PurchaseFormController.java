package com.sgv.desktop;

import com.sgv.entity.*;
import com.sgv.service.ProductService;
import com.sgv.service.SupplierService;
import com.sgv.service.SystemLogService;
import com.sgv.service.WarehouseService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.concurrent.Task;

@Component
public class PurchaseFormController extends BaseFormController {


    @FXML private TextField invoiceNumberField;
    @FXML private DatePicker invoiceDatePicker;
    @FXML private ComboBox<Supplier> supplierCombo;
    @FXML private ComboBox<Warehouse> warehouseCombo;
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
    @FXML private Label subtotalLabel;
    @FXML private Label totalLabel;
    @FXML private Label taxLabel;
    @FXML private Label headerTotalLabel;
    @FXML private TextArea notesField;

    private final com.sgv.service.PurchaseService purchaseService;
    private final ProductService productService;
    private final SupplierService supplierService;
    private final WarehouseService warehouseService;
    private final SystemLogService systemLogService;
    private final com.sgv.repository.PurchaseRepository purchaseRepository;

    private Purchase editingPurchase;
    private User currentUser;
    private final ObservableList<PurchaseItem> items = FXCollections.observableArrayList();

    private ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;
    private final ObservableList<Product> comboDisplayList = FXCollections.observableArrayList();
    private boolean isRefreshingProducts = false;
    private Product lastSelectedProduct = null;
    private Task<Boolean> duplicateCheckTask;

    public PurchaseFormController(com.sgv.repository.PurchaseRepository purchaseRepository,
                                  ProductService productService,
                                  SupplierService supplierService,
                                  WarehouseService warehouseService,
                                  SystemLogService systemLogService,
                                  com.sgv.service.PurchaseService purchaseService) {
        this.purchaseRepository = purchaseRepository;
        this.productService = productService;
        this.supplierService = supplierService;
        this.warehouseService = warehouseService;
        this.systemLogService = systemLogService;
        this.purchaseService = purchaseService;
    }

    @FXML
    public void initialize() {
        editingPurchase = null;
        currentUser = null;
        items.clear();
        lastSelectedProduct = null;
        isRefreshingProducts = false;
        onSave = null;
        initCommonFields();
        setupProductSearch();
        productCombo.setOnAction(e -> {
            Product p = productCombo.getValue();
            if (p != null && p.getPriceCost() != null) {
                unitCostField.setText(String.format("%.2f", p.getPriceCost()).replace(",", "."));
            }
        });

        if (invoiceDatePicker != null) invoiceDatePicker.setValue(LocalDate.now());

        supplierCombo.setItems(FXCollections.observableArrayList(supplierService.findAll()));
        supplierCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Supplier s) { return s != null ? s.getName() : ""; }
            public Supplier fromString(String s) { return null; }
        });

        warehouseCombo.setItems(FXCollections.observableArrayList(warehouseService.listActive()));
        warehouseCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Warehouse w) { return w != null ? w.getName() : ""; }
            public Warehouse fromString(String s) { return null; }
        });

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
                btn.setOnAction(UiUtils.safeOnAction(() -> {
                    PurchaseItem item = getTableView().getItems().get(getIndex());
                    items.remove(item);
                    updateTotals();
                }, systemLogService, "PURCHASE_REMOVE_ITEM"));
            }
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btn);
            }
        });

        itemsTable.setItems(items);

        UiUtils.attachSafe(addItemButton, this::addItem, systemLogService, "PURCHASE_ADD_ITEM");
        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "PURCHASE_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "PURCHASE_CANCEL");
        
        UiUtils.applyNumericFormatter(quantityField);
        UiUtils.applyNumericFormatter(unitCostField);

        setupRealTimeValidation();
    }

    private void setupProductSearch() {
        allProducts.setAll(productService.findAllActive());
        filteredProducts = new FilteredList<>(allProducts, p -> true);
        productCombo.setItems(comboDisplayList);

        productCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof Product p) {
                lastSelectedProduct = p;
            }
        });

        productCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Product p) { return p != null ? productDisplayText(p) : ""; }
            public Product fromString(String s) { return findBestMatch(s); }
        });

        productCombo.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(Product p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); setStyle(""); return; }
                setText(productDisplayText(p));
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
                addItem();
            }
        });

        quantityField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) { e.consume(); addItem(); }
        });
        unitCostField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) { e.consume(); addItem(); }
        });
    }

    private String productDisplayText(Product p) {
        if (p == null) return "";
        String cost = p.getPriceCost() != null ? String.format("%.2f", p.getPriceCost()) : "0.00";
        return p.getCode() + " - " + p.getName() + " (Custo: " + cost + " MT)";
    }

    private String extractCodeFromDisplayText(String text) {
        if (text == null) return null;
        int dashIdx = text.indexOf(" - ");
        if (dashIdx > 0) return text.substring(0, dashIdx).trim();
        return null;
    }

    private Product findBestMatch(String text) {
        if (text == null || text.isBlank()) return null;

        String extractedCode = extractCodeFromDisplayText(text);
        String searchTarget = extractedCode != null ? extractedCode : text.trim();

        return allProducts.stream()
                .filter(p -> p != null)
                .filter(p -> (p.getCode() != null && p.getCode().equalsIgnoreCase(searchTarget)) ||
                             (p.getName() != null && p.getName().equalsIgnoreCase(searchTarget)))
                .findFirst()
                .orElse(null);
    }

    private void refreshProductSearchResults() {
        if (isRefreshingProducts) return;
        isRefreshingProducts = true;
        try {
            String search = productCombo.getEditor() != null ? productCombo.getEditor().getText() : null;
            boolean emptySearch = search == null || search.isBlank();

            filteredProducts.setPredicate(p -> {
                if (emptySearch) return true;
                String lower = search.toLowerCase();
                return (p.getCode() != null && p.getCode().toLowerCase().contains(lower)) ||
                       (p.getName() != null && p.getName().toLowerCase().contains(lower));
            });

            List<Product> snapshot = filteredProducts.stream()
                    .sorted(Comparator.comparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(Product::getCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();

            Product prev = productCombo.getValue();
            if (productCombo.isShowing()) productCombo.hide();
            comboDisplayList.setAll(snapshot);
            if (prev != null && snapshot.stream().anyMatch(p -> p.getId().equals(prev.getId()))) {
                productCombo.getSelectionModel().select(prev);
            }
            if (!emptySearch && !productCombo.isShowing() && productCombo.isFocused()) productCombo.show();
        } finally {
            isRefreshingProducts = false;
        }
    }

    private Product resolveSelectedProduct() {
        Product val = productCombo.getValue();
        if (val != null) return val;
        Product sel = productCombo.getSelectionModel().getSelectedItem();
        if (sel != null) return sel;
        if (lastSelectedProduct != null) {
            String editorText = productCombo.getEditor() != null ? productCombo.getEditor().getText().trim() : "";
            if (!editorText.isBlank() && productDisplayText(lastSelectedProduct).equals(editorText)) {
                return lastSelectedProduct;
            }
        }
        String text = productCombo.getEditor() != null ? productCombo.getEditor().getText().trim() : "";
        if (!text.isBlank()) return findBestMatch(text);
        return null;
    }
    
    private void setupRealTimeValidation() {
        invoiceNumberField.textProperty().addListener((obs, o, n) -> validateRealTime());
        invoiceDatePicker.valueProperty().addListener((obs, o, n) -> validateRealTime());
        
        // Ouça mudanças na lista para validar também
        items.addListener((javafx.collections.ListChangeListener.Change<? extends PurchaseItem> c) -> validateRealTime());
        
        javafx.application.Platform.runLater(this::validateRealTime);
    }
    
    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;
        
        String invoice = invoiceNumberField.getText();
        if (invoice == null || invoice.isBlank()) {
            errors.append("Número da factura é obrigatório. ");
            valid = false;
        }
        if (supplierCombo.getValue() == null) {
            errors.append("Seleccione o fornecedor. ");
            valid = false;
        }
        if (invoiceDatePicker.getValue() == null) {
            errors.append("Data da factura é obrigatória. ");
            valid = false;
        }
        if (items.isEmpty()) {
            errors.append("Adicione pelo menos um item. ");
            valid = false;
        }
        
        formValidProperty.set(valid);
        if (!valid) {
            showError(errors.toString().trim());
        } else {
            hideError();
        }

        if (valid && invoice != null && !invoice.isBlank() && supplierCombo.getValue() != null) {
            String trimmedInvoice = invoice.trim();
            Supplier selectedSupplier = supplierCombo.getValue();
            if (duplicateCheckTask != null) duplicateCheckTask.cancel(false);
            duplicateCheckTask = new Task<>() {
                @Override
                protected Boolean call() {
                    java.util.Optional<Purchase> existing = purchaseRepository.findByInvoiceNumberIgnoreCase(trimmedInvoice);
                    if (existing.isEmpty()) return false;
                    Purchase p = existing.get();
                    boolean sameSupplier = p.getSupplier() != null && selectedSupplier.getId() != null
                            && selectedSupplier.getId().equals(p.getSupplier().getId());
                    boolean differentId = editingPurchase == null || editingPurchase.getId() == null
                            || !editingPurchase.getId().equals(p.getId());
                    return sameSupplier && differentId;
                }
            };
            duplicateCheckTask.setOnSucceeded(e -> {
                if (duplicateCheckTask.getValue()) {
                    formValidProperty.set(false);
                    showError("Já existe uma compra com este número de factura para este fornecedor.");
                }
            });
            new Thread(duplicateCheckTask).start();
        }
    }

    private void addItem() {
        Product product = resolveSelectedProduct();
        if (product == null) {
            showError("Produto não encontrado");
            return;
        }
        double qty = 0;
        double cost = 0;
        try {
            qty = Double.parseDouble((quantityField.getText() != null ? quantityField.getText() : "1").replace(",", "."));
            cost = Double.parseDouble((unitCostField.getText() != null ? unitCostField.getText() : "0").replace(",", "."));
        } catch (Exception ex) { /* will be caught below */ }
        
        if (qty <= 0) {
            showError("Quantidade deve ser maior que 0");
            return;
        }
        if (cost < 0) {
            showError("Custo não pode ser negativo");
            return;
        }

        PurchaseItem item = new PurchaseItem();
        item.setProduct(product);
        item.setQuantity(qty);
        item.setCostPrice(cost);
        item.setSubtotal(qty * cost);
        items.add(item);

        productCombo.setValue(null);
        productCombo.getEditor().clear();
        quantityField.setText("1");
        unitCostField.clear();
        updateTotals();
        refreshProductSearchResults();
        Platform.runLater(productCombo::requestFocus);
    }

    private void updateTotals() {
        double sub = 0.0;
        double tax = 0.0;
        for (PurchaseItem item : items) {
            double lineSubtotal = item.getSubtotal() != null ? item.getSubtotal() : 0.0;
            sub += lineSubtotal;
            if (item.getProduct() != null) {
                double taxRate = item.getProduct().getEffectiveTaxRate();
                tax += lineSubtotal * (taxRate / 100.0);
            }
        }
        subtotalLabel.setText(String.format("%.2f MT", sub));
        if (taxLabel != null) taxLabel.setText(String.format("%.2f MT", tax));
        double total = sub + tax;
        totalLabel.setText(String.format("%.2f MT", total));
        if (headerTotalLabel != null) headerTotalLabel.setText(String.format("Total: %.2f", total));
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;

        Supplier selectedSupplier = supplierCombo.getValue();
        if (selectedSupplier == null) {
            showError("Selecione um fornecedor antes de salvar a compra.");
            return;
        }

        showSaveSpinner();

        final String invoiceNumber = invoiceNumberField.getText() != null ? invoiceNumberField.getText().trim() : "";
        final Supplier selectedSupplierValue = selectedSupplier;
        final Long prevId = editingPurchase != null && editingPurchase.getId() != null ? editingPurchase.getId() : null;

        if (selectedSupplierValue != null && purchaseService.isDuplicateInvoiceForSupplier(invoiceNumber, selectedSupplierValue.getId(), prevId)) {
            hideSaveSpinner();
            showError("Já existe uma compra com este número de factura para este fornecedor.");
            return;
        }

        final boolean isEdit = editingPurchase != null && editingPurchase.getId() != null;
        final Long previousId = isEdit ? editingPurchase.getId() : null;

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                Purchase p = editingPurchase != null ? editingPurchase : new Purchase();
                p.setInvoiceNumber(invoiceNumberField.getText() != null ? invoiceNumberField.getText().trim() : "");
                p.setState("RECEIVED");
                p.setNotes(notesField != null && notesField.getText() != null ? notesField.getText().trim() : "");
                p.setUser(currentUser);
                if (currentUser != null) p.setBranch(currentUser.getBranch());

                Supplier managedSupplier = resolveSelectedSupplier();
                p.setSupplier(managedSupplier);

                if (invoiceDatePicker.getValue() != null)
                    p.setPurchaseDate(invoiceDatePicker.getValue().atStartOfDay());

                // copy items into purchase
                p.getItems().clear();
                p.getItems().addAll(new ArrayList<>(items));

                if (warehouseCombo.getValue() != null) p.setTargetWarehouse(warehouseCombo.getValue());

                if (purchaseService.isDuplicateInvoiceForSupplier(p.getInvoiceNumber(), managedSupplier.getId(), previousId)) {
                    throw new IllegalArgumentException("Já existe uma compra com este número de factura para este fornecedor.");
                }

                purchaseService.savePurchase(p, currentUser, isEdit, previousId);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> { systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "COMPRA_GRAVADA", "Compra gravada - Factura nº " + invoiceNumberField.getText());
            if (onSave != null) onSave.run();
            doCancel();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("PURCHASE_SAVE_FAILED", "Erro ao salvar compra: " + ex.getMessage(), ex);
            showError("Erro ao salvar: " + ex.getMessage());
            hideSaveSpinner();
        });

        new Thread(saveTask).start();
    }

    private Supplier resolveSelectedSupplier() {
        Supplier selectedSupplier = supplierCombo.getValue();
        if (selectedSupplier == null) {
            throw new IllegalStateException("Fornecedor selecionado inválido.");
        }
        if (selectedSupplier.getId() == null) {
            throw new IllegalStateException("Fornecedor selecionado inválido.");
        }
        return supplierService.findById(selectedSupplier.getId())
                .orElseThrow(() -> new IllegalStateException("Fornecedor selecionado não existe (id=" + selectedSupplier.getId() + ")"));
    }

    public void setPurchase(Purchase purchase) {
        this.editingPurchase = purchase;
        if (purchase != null) {

            invoiceNumberField.setText(purchase.getInvoiceNumber() != null ? purchase.getInvoiceNumber() : "");
            if (purchase.getSupplier() != null) {
                Supplier managedSupplier = supplierService.findById(purchase.getSupplier().getId()).orElse(purchase.getSupplier());
                supplierCombo.setValue(managedSupplier);
            }
            if (purchase.getPurchaseDate() != null && invoiceDatePicker != null)
                invoiceDatePicker.setValue(purchase.getPurchaseDate().toLocalDate());
            if (purchase.getTargetWarehouse() != null) {
                Warehouse managedWarehouse = warehouseService.findById(purchase.getTargetWarehouse().getId())
                        .orElse(purchase.getTargetWarehouse());
                warehouseCombo.setValue(managedWarehouse);
            }
            if (notesField != null) notesField.setText(purchase.getNotes() != null ? purchase.getNotes() : "");
            items.setAll(purchase.getItems());
            updateTotals();
        }
    }

    public void setCurrentUser(User user) { this.currentUser = user; }

    private double parseDoubleSafe(String text) {
        try {
            return text == null || text.isBlank() ? 0.0 : Double.parseDouble(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
