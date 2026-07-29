package com.sgv.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

import java.util.function.Consumer;

/**
 * Reusable UI widget factory extracted from DashboardController.
 */
public final class UiWidgetFactory {

    private UiWidgetFactory() {}

    public static VBox makeKpiCard(String icon, String title, String value, String... subtitle) {
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(18));
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 22px; -fx-font-weight: bold;");
        valueLabel.setWrapText(true);
        Label subLabel = new Label(subtitle.length > 0 ? subtitle[0] : "");
        subLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");

        VBox card = new VBox(2, iconLabel, titleLabel, valueLabel, subLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        card.setMinWidth(140);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    public static GridPane buildKPIGrid(String[] titles, String[] values, String[] colors) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(0, 0, 16, 0));
        for (int i = 0; i < titles.length; i++) {
            String icon = getKpiIconEmoji(colors[i]);
            VBox card = makeKpiCard(icon, titles[i], values[i]);
            grid.add(card, i % 4, i / 4);
        }
        return grid;
    }

    public static String getKpiIconEmoji(String colorClass) {
        return switch (colorClass != null ? colorClass : "") {
            case "blue" -> "\uD83D\uDCCA";
            case "green" -> "\u2705";
            case "red" -> "\u26A0\uFE0F";
            case "yellow" -> "\uD83D\uDD34";
            case "purple" -> "\uD83D\uDC8E";
            default -> "\uD83D\uDCCB";
        };
    }

    public static void updateKPICard(GridPane grid, int index, String newValue) {
        if (grid == null) return;
        var nodes = grid.getChildren();
        if (index < nodes.size() && nodes.get(index) instanceof VBox card) {
            for (var child : card.getChildren()) {
                if (child instanceof Label lbl && lbl.getFont().getSize() > 20) {
                    lbl.setText(newValue);
                    break;
                }
            }
        }
    }

    public static Button makeActionButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px;");
        addHoverEffect(btn, bg, fg);
        return btn;
    }

    public static Button makeIconButton(String symbol, String bg, String fg) {
        Button btn = new Button(symbol);
        btn.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-background-radius: 4; -fx-padding: 4 8; -fx-font-size: 14px;");
        return btn;
    }

    public static void addHoverEffect(Button btn, String baseBg, String baseFg) {
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: derive(" + baseBg + ", -10%); -fx-text-fill: " + baseFg + "; -fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + baseBg + "; -fx-text-fill: " + baseFg + "; -fx-background-radius: 6; -fx-padding: 6 14; -fx-font-size: 12px;"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Callback makeTableRowFactory() {
        return (Callback<TableView, TableRow>) tv -> new TableRow() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    boolean even = getIndex() % 2 == 0;
                    setStyle(even ? "-fx-background-color: #f8fafc;" : "-fx-background-color: white;");
                }
            }
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Callback makeSalesRowFactory(Consumer onDoubleClick) {
        return (Callback<TableView, TableRow>) tv -> new TableRow() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    boolean even = getIndex() % 2 == 0;
                    setStyle(even ? "-fx-background-color: #f8fafc;" : "-fx-background-color: white;");
                }
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> coloredStateCell() {
        return col -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(item);
                    lbl.setWrapText(true);
                    lbl.setMaxWidth(Double.MAX_VALUE);
                    String bg, fg;
                    switch (item) {
                        case "PAGO", "COMPLETED", "APROVADA", "ACTIVE", "active" -> { bg = "#dcfce7"; fg = "#166534"; }
                        case "EMITIDA", "PENDING", "PENDING_SYNC", "PENDING_APPROVAL" -> { bg = "#dbeafe"; fg = "#1e40af"; }
                        case "ANULADA", "CANCELLED", "REJECTED", "inactive" -> { bg = "#fee2e2"; fg = "#991b1b"; }
                        case "EM_PROCESSAMENTO", "IN_PROGRESS" -> { bg = "#fef9c3"; fg = "#854d0e"; }
                        case "COTACAO_ABERTA", "DRAFT" -> { bg = "#f3e8ff"; fg = "#6b21a8"; }
                        default -> { bg = "#f1f5f9"; fg = "#475569"; }
                    }
                    lbl.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px;");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        };
    }

    @SuppressWarnings("rawtypes")
    public static HBox buildCrudToolbar(TableView table, String newLabel, Runnable onCreate, Runnable onEdit, Runnable onDelete) {
        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar...");
        searchField.setPrefWidth(200);

        Button btnNew = makeActionButton(newLabel, "#3b82f6", "white");
        btnNew.setOnAction(e -> onCreate.run());

        Button btnEdit = makeActionButton("Editar", "#f59e0b", "white");
        btnEdit.setOnAction(e -> onEdit.run());
        btnEdit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Button btnDelete = makeActionButton("Eliminar", "#ef4444", "white");
        btnDelete.setOnAction(e -> onDelete.run());
        btnDelete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        HBox toolbar = new HBox(8, searchField, btnNew, btnEdit, btnDelete);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        return toolbar;
    }

    public static void showToast(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("SGV");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
        new Thread(() -> {
            try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(alert::close);
        }).start();
    }

    public static void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("SGV");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static VBox buildInfoBlock(String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 14px; -fx-font-weight: bold;");
        VBox box = new VBox(2, lbl, val);
        box.setPadding(new Insets(8));
        return box;
    }

    public static void updatePageLabel(Label lbl, int page) {
        if (lbl != null) lbl.setText("Pagina " + (page + 1));
    }
}
