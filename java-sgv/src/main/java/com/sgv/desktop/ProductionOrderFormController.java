package com.sgv.desktop;

import com.sgv.entity.Product;
import com.sgv.entity.ProductionOrder;
import com.sgv.entity.User;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.ProductionOrderRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import com.sgv.service.SystemLogService;

@Component
public class ProductionOrderFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private TextField orderNumberField;
    @FXML private ComboBox<String> stateCombo;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField quantityField;
    @FXML private TextField unitField;
    @FXML private TextArea notesArea;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final SystemLogService systemLogService;

    private ProductionOrder order;
    private User currentUser;
    private Runnable onSave;
    
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public ProductionOrderFormController(ProductionOrderRepository productionOrderRepository,
                                         ProductRepository productRepository,
                                         SystemLogService systemLogService) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.systemLogService = systemLogService;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void initialize() {
        productCombo.setItems(javafx.collections.FXCollections.observableArrayList(productRepository.findAll()));
        stateCombo.getItems().addAll("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED");

        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        
        // Setup simple ID generator if empty
        if (orderNumberField.getText() == null || orderNumberField.getText().isEmpty()) {
            orderNumberField.setText("ORD-" + System.currentTimeMillis() % 1000000);
        }

        // UX: Transição de entrada fluida (Fade-in)
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // UX/MVVM: Data-Binding
        saveButton.disableProperty().bind(formValidProperty.not());

        // Listeners
        productCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        quantityField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }
    
    private void validateRealTime() {
        boolean valid = true;
        if (productCombo.getValue() == null) valid = false;
        try {
            double q = Double.parseDouble(quantityField.getText().replace(",", "."));
            if (q <= 0) valid = false;
        } catch (Exception e) {
            valid = false;
        }
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

    public void setOnSave(Runnable callback) {
        this.onSave = callback;
    }

    private void doSave() {
        if (!formValidProperty.get()) return;

        saveButton.setVisible(false);
        saveSpinner.setVisible(true);
        saveSpinner.setManaged(true);
        cancelButton.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                order.setOrderNumber(orderNumberField.getText());
                order.setState(stateCombo.getValue());
                order.setProduct(productCombo.getValue());
                order.setQuantity(Double.parseDouble(quantityField.getText().replace(",", ".")));
                order.setUnit(unitField.getText());
                order.setNotes(notesArea.getText());

                if (order.getId() == null) {
                    order.setCreatedAt(LocalDateTime.now());
                    order.setCreatedBy(currentUser);
                }

                if ("COMPLETED".equals(order.getState()) && order.getCompletedAt() == null) {
                    order.setCompletedAt(LocalDateTime.now());
                }

                productionOrderRepository.save(order);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("PRODUCTION_ORDER_SAVE_FAILED", "Erro ao salvar ordem de produção: " + ex.getMessage(), ex);
            errorLabel.setText("Erro ao salvar: " + ex.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }


    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
