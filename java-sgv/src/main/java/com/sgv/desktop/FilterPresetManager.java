package com.sgv.desktop;

import com.sgv.entity.FilterPreset;
import com.sgv.service.FilterPresetService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.Optional;

/**
 * Manages filter presets (save/load/delete) for products, customers, and sales.
 * Extracted from DashboardController to reduce its size and isolate concerns.
 */
public class FilterPresetManager {

    private final FilterPresetService filterPresetService;
    private final ObjectMapper objectMapper = new ObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final Long userId;

    public FilterPresetManager(FilterPresetService filterPresetService, Long userId) {
        this.filterPresetService = filterPresetService;
        this.userId = userId;
    }

    public void populatePresetCombo(VBox filterPane, String type, ComboBox<String> combo) {
        if (combo == null) return;
        combo.getItems().clear();
        combo.getItems().add("(Nenhum)");
        var presets = filterPresetService.listPresets(userId, type);
        for (FilterPreset p : presets) combo.getItems().add(p.getName());
        combo.getSelectionModel().selectFirst();
    }

    public void saveProductPreset(VBox filterPane) { savePreset("produto", filterPane); }
    public void saveCustomerPreset(VBox filterPane) { savePreset("cliente", filterPane); }
    public void saveSalesPreset(VBox filterPane) { savePreset("venda", filterPane); }

    private void savePreset(String type, VBox filterPane) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Guardar Filtro");
        dialog.setHeaderText("Nome do preset:");
        dialog.setContentText("Nome:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                String data = objectMapper.writeValueAsString(Map.of("type", type));
                FilterPreset preset = new FilterPreset();
                preset.setUserId(userId);
                preset.setName(name);
                preset.setType(type);
                preset.setData(data);
                filterPresetService.savePreset(preset);
            } catch (Exception e) {
                UiWidgetFactory.showAlert(Alert.AlertType.ERROR, "Erro ao guardar preset: " + e.getMessage());
            }
        });
    }

    public void deletePreset(String presetName, String type) {
        if (presetName == null || presetName.startsWith("(")) return;
        filterPresetService.deletePresetByName(userId, type, presetName);
    }

    public Map<String, Object> loadPresetData(String presetName, String type) {
        if (presetName == null || presetName.startsWith("(")) return null;
        Optional<FilterPreset> preset = filterPresetService.findPreset(userId, type, presetName);
        if (preset.isEmpty()) return null;
        try {
            return objectMapper.readValue(preset.get().getData(), new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
