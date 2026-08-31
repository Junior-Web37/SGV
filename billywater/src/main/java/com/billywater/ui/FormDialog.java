package com.billywater.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Formulário genérico (rótulo + controlo) numa grelha. */
public class FormDialog {

    private final GridPane grid = new GridPane();
    private final Map<String, javafx.scene.Node> campos = new LinkedHashMap<>();
    private final Dialog<Void> dialog = new Dialog<>();

    public FormDialog() {
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    public FormDialog add(String rotulo, TextField f) { return addCampo(rotulo, f); }
    public FormDialog add(String rotulo, ComboBox<?> cb) { return addCampo(rotulo, cb); }
    public FormDialog add(String rotulo, CheckBox c) { return addCampo(rotulo, c); }
    public FormDialog add(String rotulo, DatePicker d) { return addCampo(rotulo, d); }

    private FormDialog addCampo(String rotulo, javafx.scene.Node ctrl) {
        int row = campos.size();
        Label l = new Label(rotulo);
        l.getStyleClass().add("view-subtitle");
        grid.add(l, 0, row);
        grid.add(ctrl, 1, row);
        campos.put(rotulo, ctrl);
        return this;
    }

    public String texto(String rotulo) {
        Node n = campos.get(rotulo);
        return n instanceof TextField t ? t.getText() == null ? "" : t.getText().trim() : "";
    }

    public void setTexto(String rotulo, String valor) {
        Node n = campos.get(rotulo);
        if (n instanceof TextField t) t.setText(valor == null ? "" : valor);
    }

    public BigDecimal valor(String rotulo) {
        Node n = campos.get(rotulo);
        if (n instanceof TextField t) {
            String s = t.getText().trim().replace(".", "").replace(",", ".");
            if (s.isEmpty()) return BigDecimal.ZERO;
            try { return new BigDecimal(s); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
        }
        return BigDecimal.ZERO;
    }

    public void setValor(String rotulo, BigDecimal v) {
        Node n = campos.get(rotulo);
        if (n instanceof TextField t) t.setText(v == null ? "" : v.toPlainString());
    }

    public String comboText(String rotulo) {
        Node n = campos.get(rotulo);
        if (n instanceof ComboBox<?> cb && cb.getValue() != null) return cb.getValue().toString();
        return "";
    }

    public Long comboId(String rotulo) {
        Node n = campos.get(rotulo);
        if (n instanceof ComboBox<?> cb && cb.getValue() instanceof ComboItem i) return i.id();
        return null;
    }

    public void setComboSelecionado(String rotulo, Object valor) {
        Node n = campos.get(rotulo);
        if (n instanceof ComboBox<?> cb) { @SuppressWarnings("unchecked") ComboBox<Object> c = (ComboBox<Object>) cb; c.setValue(valor); }
    }

    public boolean mostrar(String titulo) {
        dialog.setTitle(titulo);
        dialog.setHeaderText(titulo);
        Optional<Void> r = dialog.showAndWait();
        return r.isPresent();
    }
}
