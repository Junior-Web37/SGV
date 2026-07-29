package com.sgv.desktop;

import com.sgv.entity.Product;
import com.sgv.entity.ProductionOrder;
import com.sgv.entity.User;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.ProductionOrderRepository;
import com.sgv.service.StockBranchService;
import com.sgv.service.SystemLogService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class ProductionOrderFormController extends BaseFormController {

    @FXML private TextField orderNumberField;
    @FXML private ComboBox<String> stateCombo;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField quantityField;
    @FXML private TextField unitField;
    @FXML private TextArea notesArea;

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final StockBranchService stockBranchService;
    private final SystemLogService systemLogService;
    private ProductionOrder order;

    public ProductionOrderFormController(ProductionOrderRepository productionOrderRepository,
                                         ProductRepository productRepository,
                                         StockBranchService stockBranchService,
                                         SystemLogService systemLogService) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.stockBranchService = stockBranchService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        productCombo.setItems(javafx.collections.FXCollections.observableArrayList(productRepository.findAllActive()));
        stateCombo.getItems().addAll("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");

        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "PRODUCTION_ORDER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "PRODUCTION_ORDER_CANCEL");

        if (orderNumberField.getText() == null || orderNumberField.getText().isEmpty())
            orderNumberField.setText("ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        productCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        quantityField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        boolean valid = true;
        if (productCombo.getValue() == null) valid = false;
        try { double q = Double.parseDouble(quantityField.getText().replace(",", ".")); if (q <= 0) valid = false; }
        catch (Exception e) { valid = false; }
        formValidProperty.set(valid);
    }

    public void setOrder(ProductionOrder o) {
        this.order = o;
        if (o != null && o.getId() != null) {
            orderNumberField.setText(o.getOrderNumber());
            orderNumberField.setDisable(true);
            stateCombo.setValue(o.getState());
            productCombo.setValue(o.getProduct());
            quantityField.setText(String.valueOf(o.getQuantity() != null ? o.getQuantity() : 0.0));
            unitField.setText(o.getUnit());
            notesArea.setText(o.getNotes());
        } else {
            this.order = new ProductionOrder();
            stateCombo.setValue("PENDING");
        }
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                order.setOrderNumber(orderNumberField.getText());
                order.setState(stateCombo.getValue());
                order.setProduct(productCombo.getValue());
                order.setQuantity(Double.parseDouble(quantityField.getText().replace(",", ".")));
                order.setUnit(unitField.getText());
                order.setNotes(notesArea.getText());
                if (order.getId() == null) { order.setCreatedAt(LocalDateTime.now()); order.setCreatedBy(currentUser); }
                if ("COMPLETED".equals(order.getState()) && order.getCompletedAt() == null) order.setCompletedAt(LocalDateTime.now());
                productionOrderRepository.save(order);

                if ("COMPLETED".equals(order.getState()) && order.getProduct() != null
                        && currentUser != null && currentUser.getBranch() != null) {
                    java.math.BigDecimal qty = java.math.BigDecimal.valueOf(order.getQuantity() != null ? order.getQuantity() : 0.0);
                    if (qty.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        stockBranchService.increaseStock(
                                currentUser.getBranch(),
                                order.getProduct(),
                                qty,
                                "PROD-" + order.getOrderNumber(),
                                "PRODUCAO",
                                currentUser
                        );
                    }
                }
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> { if (onSave != null) onSave.run(); doCancel(); });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("PRODUCTION_ORDER_SAVE_FAILED", "Erro ao salvar ordem de produção: " + ex.getMessage(), ex);
            showError("Erro ao salvar: " + ex.getMessage());
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
