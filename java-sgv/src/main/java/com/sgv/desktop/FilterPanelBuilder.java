package com.sgv.desktop;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.function.Consumer;

public class FilterPanelBuilder {

    public static record ProductFilterCriteria(String search, String category) {}
    public static record CustomerFilterCriteria(String search, String type) {}
    public static record SalesFilterCriteria(String search, LocalDate startDate, LocalDate endDate, String state, String documentType) {}

    public static VBox createProductFilterPanel(Consumer<ProductFilterCriteria> onFilter, Runnable onClear) {
        VBox panel = new VBox();
        panel.setSpacing(10);
        panel.setStyle("-fx-padding: 12; -fx-border-color: #e6e6e6; -fx-border-radius: 6; -fx-background-color: #f9f9f9;");

        Label filterLabel = new Label("🔍 Filtros");
        filterLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #666;");

        HBox filterRow = new HBox();
        filterRow.setSpacing(10);

        TextField searchField = new TextField();
        searchField.setId("productSearchField");
        searchField.setPromptText("Buscar por nome ou código...");
        searchField.setPrefWidth(300);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setId("productCategoryCombo");
        categoryCombo.setPromptText("Categoria");
        categoryCombo.setPrefWidth(150);

        ComboBox<String> presetCombo = new ComboBox<>();
        presetCombo.setId("productPresetCombo");
        presetCombo.setPromptText("Presets");
        presetCombo.setPrefWidth(180);
        Button savePresetButton = new Button("Salvar Preset");
        savePresetButton.setId("productSavePresetButton");
        Button deletePresetButton = new Button("Excluir Preset");
        deletePresetButton.setId("productDeletePresetButton");

        Button filterButton = new Button("Filtrar");
        filterButton.setStyle("-fx-padding: 6 16; -fx-background-color: #3498db; -fx-text-fill: white;");
        filterButton.setOnAction(UiUtils.safeOnAction(() -> onFilter.accept(new ProductFilterCriteria(
            searchField.getText(),
            categoryCombo.getValue())), null, "FILTER_PRODUCT"));

        Button clearButton = new Button("Limpar");
        clearButton.setStyle("-fx-padding: 6 16; -fx-background-color: #95a5a6; -fx-text-fill: white;");
        clearButton.setOnAction(UiUtils.safeOnAction(() -> {
            searchField.clear();
            categoryCombo.setValue(null);
            onClear.run();
        }, null, "FILTER_PRODUCT_CLEAR"));

        filterRow.getChildren().addAll(searchField, categoryCombo, filterButton, clearButton);
        HBox presetRow = new HBox(8);
        presetRow.getChildren().addAll(presetCombo, savePresetButton, deletePresetButton);
        panel.getChildren().addAll(filterLabel, presetRow, filterRow);

        return panel;
    }

    public static VBox createCustomerFilterPanel(Consumer<CustomerFilterCriteria> onFilter, Runnable onClear) {
        VBox panel = new VBox();
        panel.setSpacing(10);
        panel.setStyle("-fx-padding: 12; -fx-border-color: #e6e6e6; -fx-border-radius: 6; -fx-background-color: #f9f9f9;");

        Label filterLabel = new Label("🔍 Filtros");
        filterLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #666;");

        HBox filterRow = new HBox();
        filterRow.setSpacing(10);

        TextField searchField = new TextField();
        searchField.setId("customerSearchField");
        searchField.setPromptText("Buscar por nome ou código...");
        searchField.setPrefWidth(300);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setId("customerTypeCombo");
        typeCombo.getItems().addAll("PESSOA_FISICA", "PESSOA_JURIDICA", "EMPRESA");
        typeCombo.setPromptText("Tipo");
        typeCombo.setPrefWidth(150);

        ComboBox<String> presetCombo = new ComboBox<>();
        presetCombo.setId("customerPresetCombo");
        presetCombo.setPromptText("Presets");
        presetCombo.setPrefWidth(180);
        Button savePresetButton = new Button("Salvar Preset");
        savePresetButton.setId("customerSavePresetButton");
        Button deletePresetButton = new Button("Excluir Preset");
        deletePresetButton.setId("customerDeletePresetButton");

        Button filterButton = new Button("Filtrar");
        filterButton.setStyle("-fx-padding: 6 16; -fx-background-color: #3498db; -fx-text-fill: white;");
        filterButton.setOnAction(UiUtils.safeOnAction(() -> onFilter.accept(new CustomerFilterCriteria(
            searchField.getText(),
            typeCombo.getValue())), null, "FILTER_CUSTOMER"));

        Button clearButton = new Button("Limpar");
        clearButton.setStyle("-fx-padding: 6 16; -fx-background-color: #95a5a6; -fx-text-fill: white;");
        clearButton.setOnAction(UiUtils.safeOnAction(() -> {
            searchField.clear();
            typeCombo.setValue(null);
            onClear.run();
        }, null, "FILTER_CUSTOMER_CLEAR"));

        filterRow.getChildren().addAll(searchField, typeCombo, filterButton, clearButton);
        HBox presetRow = new HBox(8);
        presetRow.getChildren().addAll(presetCombo, savePresetButton, deletePresetButton);
        panel.getChildren().addAll(filterLabel, presetRow, filterRow);

        return panel;
    }

    public static VBox createSalesFilterPanel(Consumer<SalesFilterCriteria> onFilter, Runnable onClear) {
        VBox panel = new VBox();
        panel.setSpacing(10);
        panel.setStyle("-fx-padding: 12; -fx-border-color: #e6e6e6; -fx-border-radius: 6; -fx-background-color: #f9f9f9;");

        Label filterLabel = new Label("🔍 Filtros");
        filterLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #666;");

        HBox filterRow = new HBox();
        filterRow.setSpacing(10);

        TextField searchField = new TextField();
        searchField.setId("salesSearchField");
        searchField.setPromptText("Buscar por cliente...");
        searchField.setPrefWidth(220);

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setId("salesStartDate");
        startDatePicker.setPromptText("Data Início");
        startDatePicker.setPrefWidth(150);

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setId("salesEndDate");
        endDatePicker.setPromptText("Data Fim");
        endDatePicker.setPrefWidth(150);

        ComboBox<String> stateCombo = new ComboBox<>();
        stateCombo.setId("salesStateCombo");
        stateCombo.getItems().addAll("Todos", "EMITIDA", "PAGO", "ANULADA", "COTACAO_ABERTA", "COTACAO_PAGA", "ENCOMENDA_ABERTA");
        stateCombo.setValue("Todos");
        stateCombo.setPromptText("Estado");
        stateCombo.setPrefWidth(120);

        ComboBox<String> docTypeCombo = new ComboBox<>();
        docTypeCombo.setId("salesDocTypeCombo");
        docTypeCombo.getItems().addAll("Todos", "VENDA", "FACTURA", "RECIBO", "COTACAO", "ENCOMENDA", "NC", "ND");
        docTypeCombo.setValue("Todos");
        docTypeCombo.setPromptText("Tipo");
        docTypeCombo.setPrefWidth(110);

        ComboBox<String> presetCombo = new ComboBox<>();
        presetCombo.setId("salesPresetCombo");
        presetCombo.setPromptText("Presets");
        presetCombo.setPrefWidth(180);
        Button savePresetButton = new Button("Salvar Preset");
        savePresetButton.setId("salesSavePresetButton");
        Button deletePresetButton = new Button("Excluir Preset");
        deletePresetButton.setId("salesDeletePresetButton");

        Button filterButton = new Button("Filtrar");
        filterButton.setStyle("-fx-padding: 6 16; -fx-background-color: #3498db; -fx-text-fill: white;");
        filterButton.setOnAction(UiUtils.safeOnAction(() -> onFilter.accept(new SalesFilterCriteria(
            searchField.getText(),
            startDatePicker.getValue(),
            endDatePicker.getValue(),
            stateCombo.getValue(),
            docTypeCombo.getValue())), null, "FILTER_SALES"));

        Button clearButton = new Button("Limpar");
        clearButton.setStyle("-fx-padding: 6 16; -fx-background-color: #95a5a6; -fx-text-fill: white;");
        clearButton.setOnAction(UiUtils.safeOnAction(() -> {
            searchField.clear();
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            stateCombo.setValue(null);
            docTypeCombo.setValue(null);
            onClear.run();
        }, null, "FILTER_SALES_CLEAR"));

        filterRow.getChildren().addAll(searchField, startDatePicker, endDatePicker, docTypeCombo, stateCombo, filterButton, clearButton);
        HBox presetRow = new HBox(8);
        presetRow.getChildren().addAll(presetCombo, savePresetButton, deletePresetButton);
        panel.getChildren().addAll(filterLabel, presetRow, filterRow);

        return panel;
    }
}
