package com.sgv.desktop;

import com.sgv.entity.FilterPreset;
import com.sgv.service.FilterPresetService;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class FilterPanelBuilder {

    public static record ProductFilterCriteria(String search, String category) {}
    public static record CustomerFilterCriteria(String search, String type) {}
    public static record SalesFilterCriteria(String search, LocalDate startDate, LocalDate endDate, String state, String documentType) {}

    private static final String SEP = "\u0001";

    public static VBox createProductFilterPanel(FilterPresetService presetService, long userId,
                                                Consumer<ProductFilterCriteria> onFilter, Runnable onClear) {
        VBox panel = basePanel();

        TextField searchField = new TextField();
        searchField.setId("productSearchField");
        searchField.setPromptText("Buscar por nome ou código...");
        searchField.setPrefWidth(300);

        ComboBox<String> categoryCombo = new ComboBox<>();
        UiUtils.hardenComboBox(categoryCombo);
        categoryCombo.setId("productCategoryCombo");
        categoryCombo.setPromptText("Categoria");
        categoryCombo.setPrefWidth(150);

        HBox filterRow = new HBox(10);
        Button filterButton = styledButton("Filtrar", "#3498db");
        filterButton.setOnAction(UiUtils.safeOnAction(() -> onFilter.accept(new ProductFilterCriteria(
            searchField.getText(), categoryCombo.getValue())), null, "FILTER_PRODUCT"));
        Button clearButton = styledButton("Limpar", "#95a5a6");
        clearButton.setOnAction(UiUtils.safeOnAction(() -> {
            searchField.clear();
            categoryCombo.setValue(null);
            onClear.run();
        }, null, "FILTER_PRODUCT_CLEAR"));

        filterRow.getChildren().addAll(searchField, categoryCombo, filterButton, clearButton);

        HBox presetRow = buildPresetRow(presetService, userId, "PRODUCT", searchField, categoryCombo, null, null,
            () -> onFilter.accept(new ProductFilterCriteria(searchField.getText(), categoryCombo.getValue())));

        panel.getChildren().addAll(filterLabel(), presetRow, filterRow);
        return panel;
    }

    public static VBox createCustomerFilterPanel(FilterPresetService presetService, long userId,
                                                  Consumer<CustomerFilterCriteria> onFilter, Runnable onClear) {
        VBox panel = basePanel();

        TextField searchField = new TextField();
        searchField.setId("customerSearchField");
        searchField.setPromptText("Buscar por nome ou código...");
        searchField.setPrefWidth(300);

        ComboBox<String> typeCombo = new ComboBox<>();
        UiUtils.hardenComboBox(typeCombo);
        typeCombo.setId("customerTypeCombo");
        typeCombo.getItems().addAll("PESSOA_FISICA", "PESSOA_JURIDICA", "EMPRESA");
        typeCombo.setPromptText("Tipo");
        typeCombo.setPrefWidth(150);

        HBox filterRow = new HBox(10);
        Button filterButton = styledButton("Filtrar", "#3498db");
        filterButton.setOnAction(UiUtils.safeOnAction(() -> onFilter.accept(new CustomerFilterCriteria(
            searchField.getText(), typeCombo.getValue())), null, "FILTER_CUSTOMER"));
        Button clearButton = styledButton("Limpar", "#95a5a6");
        clearButton.setOnAction(UiUtils.safeOnAction(() -> {
            searchField.clear();
            typeCombo.setValue(null);
            onClear.run();
        }, null, "FILTER_CUSTOMER_CLEAR"));

        filterRow.getChildren().addAll(searchField, typeCombo, filterButton, clearButton);

        HBox presetRow = buildPresetRow(presetService, userId, "CUSTOMER", searchField, typeCombo, null, null,
            () -> onFilter.accept(new CustomerFilterCriteria(searchField.getText(), typeCombo.getValue())));

        panel.getChildren().addAll(filterLabel(), presetRow, filterRow);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static HBox buildPresetRow(FilterPresetService presetService, long userId, String type,
                                       TextField searchField, ComboBox<String> singleCombo,
                                       ComboBox<String> stateCombo, ComboBox<String> docTypeCombo,
                                       Runnable applyAndFilter) {
        ComboBox<String> presetCombo = new ComboBox<>();
        UiUtils.hardenComboBox(presetCombo);
        presetCombo.setPromptText("Presets");
        presetCombo.setPrefWidth(200);

        Runnable refreshPresets = () -> {
            String current = presetCombo.getValue();
            List<FilterPreset> presets = presetService.listPresets(userId, type);
            presetCombo.getItems().setAll(presets.stream().map(FilterPreset::getName).toList());
            if (current != null && presetCombo.getItems().contains(current)) {
                presetCombo.setValue(current);
            }
        };

        Function<String, String[]> parts = data -> data == null ? new String[]{"", ""} : data.split(SEP, -1);

        presetCombo.valueProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            presetService.findPreset(userId, type, n).ifPresent(p -> {
                String[] v = parts.apply(p.getData());
                searchField.setText(v.length > 0 ? v[0] : "");
                if (singleCombo != null) singleCombo.setValue(v.length > 1 ? v[1] : null);
                if (stateCombo != null) stateCombo.setValue(v.length > 1 ? v[1] : "Todos");
                if (docTypeCombo != null) docTypeCombo.setValue(v.length > 2 ? v[2] : "Todos");
                applyAndFilter.run();
            });
        });

        Button savePresetButton = new Button("Salvar Preset");
        savePresetButton.setOnAction(UiUtils.safeOnAction(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Salvar Preset");
            dialog.setHeaderText(null);
            dialog.setContentText("Nome do preset:");
            Optional<String> name = dialog.showAndWait();
            if (name.isPresent() && !name.get().isBlank()) {
                String data = searchField.getText() + SEP +
                        (singleCombo != null ? (singleCombo.getValue() == null ? "" : singleCombo.getValue())
                            : (stateCombo.getValue() == null ? "" : stateCombo.getValue())) + SEP +
                        (docTypeCombo != null && docTypeCombo.getValue() != null ? docTypeCombo.getValue() : "");
                presetService.savePreset(new FilterPreset(userId, type, name.get().trim(), data));
                refreshPresets.run();
            }
        }, null, "FILTER_PRESET_SAVE_" + type));

        Button deletePresetButton = new Button("Excluir Preset");
        deletePresetButton.setOnAction(UiUtils.safeOnAction(() -> {
            String name = presetCombo.getValue();
            if (name != null) {
                presetService.deletePresetByName(userId, type, name);
                refreshPresets.run();
            }
        }, null, "FILTER_PRESET_DELETE_" + type));

        HBox presetRow = new HBox(8);
        presetRow.getChildren().addAll(presetCombo, savePresetButton, deletePresetButton);
        refreshPresets.run();
        return presetRow;
    }

    private static Label filterLabel() {
        Label filterLabel = new Label("🔍 Filtros");
        filterLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #666;");
        return filterLabel;
    }

    private static VBox basePanel() {
        VBox panel = new VBox();
        panel.setSpacing(10);
        panel.setStyle("-fx-padding: 12; -fx-border-color: #e6e6e6; -fx-border-radius: 6; -fx-background-color: #f9f9f9;");
        return panel;
    }

    private static Button styledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-padding: 6 16; -fx-background-color: " + color + "; -fx-text-fill: white;");
        return button;
    }
}
