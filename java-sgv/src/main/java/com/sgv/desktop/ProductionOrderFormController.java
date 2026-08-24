package com.sgv.desktop;

import com.sgv.entity.MetricUnit;
import com.sgv.entity.Product;
import com.sgv.entity.ProductRecipe;
import com.sgv.entity.ProductionOrder;
import com.sgv.entity.User;
import com.sgv.repository.MetricUnitRepository;
import com.sgv.repository.ProductRecipeRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.ProductionOrderRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.StockBranchService;
import com.sgv.service.SystemLogService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @FXML private TableView<RecipeItemRow> recipeTable;
    @FXML private TableColumn<RecipeItemRow, String> ingredientColumn;
    @FXML private TableColumn<RecipeItemRow, String> requiredColumn;
    @FXML private TableColumn<RecipeItemRow, String> totalConsumedColumn;
    @FXML private TableColumn<RecipeItemRow, String> unitColumn;
    @FXML private Label recipeStatusLabel;

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final ProductRecipeRepository productRecipeRepository;
    private final MetricUnitRepository metricUnitRepository;
    private final UserRepository userRepository;
    private final StockBranchService stockBranchService;
    private final SystemLogService systemLogService;
    private ProductionOrder order;

    public ProductionOrderFormController(ProductionOrderRepository productionOrderRepository,
                                         ProductRepository productRepository,
                                         ProductRecipeRepository productRecipeRepository,
                                         MetricUnitRepository metricUnitRepository,
                                         UserRepository userRepository,
                                         StockBranchService stockBranchService,
                                         SystemLogService systemLogService) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.productRecipeRepository = productRecipeRepository;
        this.metricUnitRepository = metricUnitRepository;
        this.userRepository = userRepository;
        this.stockBranchService = stockBranchService;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.hardenComboBox(productCombo);
        UiUtils.hardenComboBox(unitCombo);
        UiUtils.hardenComboBox(responsibleCombo);

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
        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);
        UiUtils.applyNumericFormatter(quantityField);

        if (orderNumberField.getText() == null || orderNumberField.getText().isEmpty()) {
            orderNumberField.setText("ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        setupRecipeTable();

        productCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null && n.getUnit() != null && n.getUnit().getAbbreviation() != null) {
                unitCombo.setValue(n.getUnit().getAbbreviation());
            }
            updateRecipePreview();
            validateRealTime();
        });
        quantityField.textProperty().addListener((obs, o, n) -> {
            updateRecipePreview();
            validateRealTime();
        });
        if (registrationDatePicker != null) {
            registrationDatePicker.valueProperty().addListener((obs, o, n) -> validateRealTime());
        }

        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void setupRecipeTable() {
        if (recipeTable == null) return;
        ingredientColumn.setCellValueFactory(new PropertyValueFactory<>("ingredientName"));
        requiredColumn.setCellValueFactory(new PropertyValueFactory<>("requiredPerUnit"));
        totalConsumedColumn.setCellValueFactory(new PropertyValueFactory<>("totalToConsume"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
    }

    private void updateRecipePreview() {
        if (recipeTable == null) return;
        Product selectedProduct = productCombo.getValue();
        if (selectedProduct == null || selectedProduct.getId() == null) {
            recipeTable.getItems().clear();
            if (recipeStatusLabel != null) recipeStatusLabel.setText("Selecione um produto para carregar a receita");
            return;
        }

        BigDecimal qty = BigDecimal.ONE;
        try {
            String qText = quantityField.getText();
            if (qText != null && !qText.isBlank()) {
                qty = new BigDecimal(qText.trim().replace(",", "."));
            }
        } catch (Exception ignored) {}

        List<ProductRecipe> recipes = productRecipeRepository.findByParentProductId(selectedProduct.getId());
        if (recipes == null || recipes.isEmpty()) {
            recipeTable.getItems().clear();
            if (recipeStatusLabel != null) {
                recipeStatusLabel.setText("Sem receita vinculada (entrada direta de produto acabado)");
                recipeStatusLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-style: italic;");
            }
            return;
        }

        List<RecipeItemRow> rows = new ArrayList<>();
        for (ProductRecipe r : recipes) {
            String ingName = r.getIngredientProduct() != null ? r.getIngredientProduct().getName() : "Ingrediente";
            BigDecimal req = r.getQuantityRequired() != null ? r.getQuantityRequired() : BigDecimal.ZERO;
            BigDecimal totalCons = req.multiply(qty);
            String u = r.getUnit() != null ? r.getUnit() : "UN";
            rows.add(new RecipeItemRow(ingName, String.format("%.4f", req.doubleValue()), String.format("%.4f", totalCons.doubleValue()), u));
        }

        recipeTable.setItems(FXCollections.observableArrayList(rows));
        if (recipeStatusLabel != null) {
            recipeStatusLabel.setText(String.format("Ficha técnica ativa: %d matéria(s)-prima(s) serão abatidas do stock", recipes.size()));
            recipeStatusLabel.setStyle("-fx-text-fill: #15803D; -fx-font-size: 11px; -fx-font-weight: 700;");
        }
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
        updateRecipePreview();
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
        UiUtils.runTask(saveTask);
    }

    public static class RecipeItemRow {
        private final String ingredientName;
        private final String requiredPerUnit;
        private final String totalToConsume;
        private final String unit;

        public RecipeItemRow(String ingredientName, String requiredPerUnit, String totalToConsume, String unit) {
            this.ingredientName = ingredientName;
            this.requiredPerUnit = requiredPerUnit;
            this.totalToConsume = totalToConsume;
            this.unit = unit;
        }

        public String getIngredientName() { return ingredientName; }
        public String getRequiredPerUnit() { return requiredPerUnit; }
        public String getTotalToConsume() { return totalToConsume; }
        public String getUnit() { return unit; }
    }
}
