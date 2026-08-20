package com.sgv.desktop;

import com.sgv.entity.MetricUnit;
import com.sgv.entity.Product;
import com.sgv.entity.ProductionOrder;
import com.sgv.entity.User;
import com.sgv.repository.MetricUnitRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.ProductionOrderRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.StockBranchService;
import com.sgv.service.SystemLogService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ProductionOrderFormController extends BaseFormController {

    @FXML private TextField orderNumberField;
    @FXML private DatePicker registrationDatePicker;
    @FXML private ComboBox<Product> productCombo;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> unitCombo;
    @FXML private ComboBox<User> responsibleCombo;
    @FXML private TextArea notesArea;

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final UserRepository userRepository;
    private final StockBranchService stockBranchService;
    private final SystemLogService systemLogService;
    private ProductionOrder order;

    public ProductionOrderFormController(ProductionOrderRepository productionOrderRepository,
                                         ProductRepository productRepository,
                                         MetricUnitRepository metricUnitRepository,
                                         UserRepository userRepository,
                                         StockBranchService stockBranchService,
                                         SystemLogService systemLogService) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.userRepository = userRepository;
        this.stockBranchService = stockBranchService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();

        // 1. Carregar produtos activos
        List<Product> products = productRepository.findAllActive();
        productCombo.setItems(FXCollections.observableArrayList(products));
        productCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return p != null ? p.getCode() + " - " + p.getName() : "";
            }
            @Override
            public Product fromString(String s) { return null; }
        });

        // 2. Carregar unidades de medida
        List<MetricUnit> units = metricUnitRepository.findAll();
        List<String> unitAbbrs = new java.util.ArrayList<>();
        unitAbbrs.add("UN");
        unitAbbrs.add("KG");
        unitAbbrs.add("L");
        unitAbbrs.add("CX");
        unitAbbrs.add("PCT");
        for (MetricUnit u : units) {
            if (u.getAbbreviation() != null && !unitAbbrs.contains(u.getAbbreviation())) {
                unitAbbrs.add(u.getAbbreviation());
            }
        }
        unitCombo.setItems(FXCollections.observableArrayList(unitAbbrs));
        if (!unitAbbrs.isEmpty()) {
            unitCombo.setValue(unitAbbrs.get(0));
        }

        // 3. Carregar utilizadores responsáveis
        List<User> users = userRepository.findAll();
        responsibleCombo.setItems(FXCollections.observableArrayList(users));
        responsibleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User u) {
                return u != null ? u.getFullName() + " (" + u.getUsername() + ")" : "";
            }
            @Override
            public User fromString(String s) { return null; }
        });

        if (registrationDatePicker != null) {
            registrationDatePicker.setValue(LocalDate.now());
        }

        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "PRODUCTION_ORDER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "PRODUCTION_ORDER_CANCEL");
        UiUtils.applyNumericFormatter(quantityField);

        if (orderNumberField.getText() == null || orderNumberField.getText().isEmpty()) {
            orderNumberField.setText("ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        productCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null && n.getUnit() != null && n.getUnit().getAbbreviation() != null) {
                unitCombo.setValue(n.getUnit().getAbbreviation());
            }
            validateRealTime();
        });
        quantityField.textProperty().addListener((obs, o, n) -> validateRealTime());
        if (registrationDatePicker != null) {
            registrationDatePicker.valueProperty().addListener((obs, o, n) -> validateRealTime());
        }

        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        if (productCombo.getValue() == null) {
            errors.append("Seleccione o produto a produzir. ");
            valid = false;
        }

        if (registrationDatePicker != null && registrationDatePicker.getValue() == null) {
            errors.append("Data de registo é obrigatória. ");
            valid = false;
        }

        String qtyText = quantityField.getText();
        if (qtyText == null || qtyText.isBlank()) {
            errors.append("Quantidade é obrigatória. ");
            valid = false;
        } else {
            try {
                BigDecimal q = new BigDecimal(qtyText.trim().replace(",", "."));
                if (q.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.append("Quantidade deve ser maior que zero. ");
                    valid = false;
                }
            } catch (Exception e) {
                errors.append("Quantidade inválida. ");
                valid = false;
            }
        }

        formValidProperty.set(valid);
        if (!valid) {
            showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios.");
        } else {
            hideError();
        }
    }

    public void setOrder(ProductionOrder o) {
        this.order = o;
        if (o != null && o.getId() != null) {
            orderNumberField.setText(o.getOrderNumber());
            orderNumberField.setDisable(true);
            productCombo.setValue(o.getProduct());
            quantityField.setText(o.getQuantityAmount() != null ? o.getQuantityAmount().toPlainString() : "0");
            if (o.getUnit() != null) unitCombo.setValue(o.getUnit());
            if (o.getCreatedBy() != null) responsibleCombo.setValue(o.getCreatedBy());
            if (o.getCreatedAt() != null && registrationDatePicker != null) {
                registrationDatePicker.setValue(o.getCreatedAt().toLocalDate());
            }
            notesArea.setText(o.getNotes() != null ? o.getNotes() : "");
        } else {
            this.order = new ProductionOrder();
            order.setState("COMPLETED");
            if (currentUser != null && responsibleCombo != null) {
                responsibleCombo.setValue(currentUser);
            }
        }
        validateRealTime();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        if (productCombo.getValue() == null) { showError("Seleccione o produto."); return; }
        if (quantityField.getText() == null || quantityField.getText().isBlank()) { showError("Quantidade é obrigatória."); return; }
        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                ProductionOrder target = (order != null) ? order : new ProductionOrder();
                target.setOrderNumber(orderNumberField.getText().trim());
                target.setProduct(productCombo.getValue());
                BigDecimal qty = new BigDecimal(quantityField.getText().trim().replace(",", "."));
                target.setQuantityAmount(qty);
                target.setUnit(unitCombo.getValue() != null ? unitCombo.getValue() : "UN");
                target.setNotes(notesArea.getText() != null ? notesArea.getText().trim() : "");
                
                User responsible = (responsibleCombo != null && responsibleCombo.getValue() != null)
                        ? responsibleCombo.getValue() : currentUser;
                target.setCreatedBy(responsible);

                LocalDate regDate = (registrationDatePicker != null && registrationDatePicker.getValue() != null)
                        ? registrationDatePicker.getValue() : LocalDate.now();
                if (target.getId() == null) {
                    target.setCreatedAt(regDate.atStartOfDay());
                    target.setState("COMPLETED");
                    target.setCompletedAt(LocalDateTime.now());
                }

                productionOrderRepository.save(target);

                // Entrada automática em stock na filial do utilizador/responsável
                User stockUser = (currentUser != null) ? currentUser : responsible;
                if (target.getProduct() != null && stockUser != null && stockUser.getBranch() != null) {
                    if (qty.compareTo(BigDecimal.ZERO) > 0) {
                        stockBranchService.increaseStock(
                                stockUser.getBranch(),
                                target.getProduct(),
                                qty,
                                "PROD-" + target.getOrderNumber(),
                                "PRODUCAO",
                                stockUser
                        );
                        // Abate automático de matérias-primas / ingredientes
                        stockBranchService.consumeIngredientsForProduction(
                                stockUser.getBranch(),
                                target.getProduct(),
                                qty,
                                "PROD-" + target.getOrderNumber(),
                                stockUser
                        );
                    }
                }
                return null;
            }
        };
        saveTask.setOnSucceeded(e -> {
            systemLogService.logUserAction(
                currentUser != null ? currentUser.getUsername() : "Sistema",
                "PRODUCTION_ORDER_SAVED",
                "Ordem de produção gravada com sucesso: " + orderNumberField.getText()
            );
            if (onSave != null) onSave.run();
            doCancel();
        });
        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            systemLogService.logError("PRODUCTION_ORDER_SAVE_FAILED", "Erro ao salvar ordem de produção: " + msg, ex);
            showError("Erro ao salvar: " + msg);
            hideSaveSpinner();
        });
        new Thread(saveTask).start();
    }
}
