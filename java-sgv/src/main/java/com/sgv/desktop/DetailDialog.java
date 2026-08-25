package com.sgv.desktop;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Professional reusable detail dialog for any entity.
 * Usage:
 *   DetailDialog.create(window)
 *       .title("Detalhes da Venda")
 *       .subtitle("VENDA #99655")
 *       .statusBadge("EMITIDA", "#10B981")
 *       .section("Documento")
 *       .field("Tipo", "VENDA")
 *       .field("Nº", "99655")
 *       .section("Cliente")
 *       .field("Nome", "João Silva")
 *       .tableSection("Itens", new String[]{"Produto", "Qtd", "Preço", "Total"}, data)
 *       .show();
 */
public class DetailDialog {

    private String title = "Detalhes";
    private String subtitle = "";
    private String statusText = null;
    private String statusColor = null;
    private String icon = "📋";
    private final List<Section> sections = new ArrayList<>();
    private final List<String> styles = new ArrayList<>();
    private Window owner;
    private double width = 560;
    private double height = 500;

    private DetailDialog() {}

    public static DetailDialog create(Window owner) {
        DetailDialog d = new DetailDialog();
        d.owner = owner;
        return d;
    }

    public DetailDialog title(String t) { this.title = t; return this; }
    public DetailDialog subtitle(String s) { this.subtitle = s; return this; }
    public DetailDialog statusBadge(String text, String color) { this.statusText = text; this.statusColor = color; return this; }
    public DetailDialog width(double w) { this.width = w; return this; }
    public DetailDialog height(double h) { this.height = h; return this; }
    public DetailDialog style(String css) { this.styles.add(css); return this; }
    /** Ícone emoji apresentado no cabeçalho (ex.: "📦", "🧾", "👥"). */
    public DetailDialog icon(String i) { if (i != null && !i.isBlank()) this.icon = i; return this; }

    public DetailDialog section(String header) {
        sections.add(new Section(header, null, null, null, null));
        return this;
    }

    public DetailDialog field(String label, String value) {
        if (!sections.isEmpty()) {
            sections.get(sections.size() - 1).fields.put(label, value != null ? value : "—");
        }
        return this;
    }

    public DetailDialog field(String label, String value, String valueColor) {
        if (!sections.isEmpty()) {
            sections.get(sections.size() - 1).fields.put(label, value != null ? value : "—");
            sections.get(sections.size() - 1).fieldColors.put(label, valueColor);
        }
        return this;
    }

    public DetailDialog htmlField(String label, String htmlValue) {
        if (!sections.isEmpty()) {
            sections.get(sections.size() - 1).fields.put(label, htmlValue != null ? htmlValue : "—");
            sections.get(sections.size() - 1).fieldRich.put(label, true);
        }
        return this;
    }

    public DetailDialog tableSection(String header, String[] columns, List<Map<String, String>> rows) {
        sections.add(new Section(header, columns, rows, null, null));
        return this;
    }

    public DetailDialog kvSection(String header, Map<String, String> kvPairs) {
        sections.add(new Section(header, null, null, kvPairs, null));
        return this;
    }

    public void show() {
        if ("📋".equals(icon)) icon = inferIcon(title);
        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.UNDECORATED);
        if (owner != null) dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 20, 0, 0, 4);");

        // ─── HEADER ───────────────────────────────────────────
        VBox headerBox = new VBox(6);
        headerBox.setStyle("-fx-background-color: linear-gradient(to right, #1E3A8A, #1D4ED8); -fx-padding: 20 28 18 24;");
        headerBox.setMaxWidth(Double.MAX_VALUE);

        HBox titleRow = new HBox(14);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconPane = new StackPane();
        iconPane.setStyle("-fx-background-color: rgba(255,255,255,0.16); -fx-background-radius: 12; -fx-min-width: 46; -fx-min-height: 46; -fx-max-width: 46; -fx-max-height: 46;");
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconPane.getChildren().add(iconLabel);

        VBox titleText = new VBox(3);
        HBox.setHgrow(titleText, Priority.ALWAYS);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 19px; -fx-font-weight: 800;");
        Label subtitleLabel = new Label(subtitle == null || subtitle.isBlank() ? " " : subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #BFDBFE; -fx-font-size: 12.5px; -fx-font-weight: 600;");
        subtitleLabel.setWrapText(true);
        titleText.getChildren().addAll(titleLabel, subtitleLabel);

        titleRow.getChildren().addAll(iconPane, titleText);

        if (statusText != null && statusColor != null) {
            Label badge = new Label(statusText);
            badge.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; " +
                "-fx-font-weight: 800; -fx-padding: 5 14; -fx-background-radius: 12;",
                statusColor
            ));
            titleRow.getChildren().add(badge);
        }

        headerBox.getChildren().add(titleRow);

        // ─── CONTENT ──────────────────────────────────────────
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 0;");
        VBox.setVgrow(content, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F8FAFC; -fx-border-color: transparent; -fx-background-color: #F8FAFC;");

        VBox scrollContent = new VBox(0);
        scrollContent.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 16 20;");

        for (Section sec : sections) {
            if (sec.fields != null && !sec.fields.isEmpty() && sec.columns == null) {
                // Field section
                VBox secBox = new VBox(0);
                secBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; " +
                    "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 6, 0, 0, 1);");
                HBox.setHgrow(secBox, Priority.ALWAYS);

                if (sec.header != null) {
                    HBox secHeaderRow = new HBox(8);
                    secHeaderRow.setAlignment(Pos.CENTER_LEFT);
                    secHeaderRow.setStyle("-fx-background-color: #F1F5F9; -fx-padding: 9 14; " +
                        "-fx-background-radius: 10 10 0 0;");
                    Region accentBar = new Region();
                    accentBar.setStyle("-fx-background-color: #1D4ED8; -fx-min-width: 4; -fx-max-width: 4; -fx-min-height: 14; -fx-background-radius: 2;");
                    Label secHeader = new Label(sec.header);
                    secHeader.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #334155;");
                    Region hSpacer = new Region();
                    HBox.setHgrow(hSpacer, Priority.ALWAYS);
                    secHeaderRow.getChildren().addAll(accentBar, secHeader, hSpacer);
                    secHeaderRow.setMaxWidth(Double.MAX_VALUE);
                    secBox.getChildren().add(secHeaderRow);
                }

                GridPane grid = new GridPane();
                grid.setHgap(0);
                grid.setVgap(0);
                grid.setStyle("-fx-padding: 4 0;");

                int row = 0;
                for (Map.Entry<String, String> entry : sec.fields.entrySet()) {
                    Label lblKey = new Label("  " + entry.getKey());
                    lblKey.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 600; " +
                        "-fx-padding: 8 16 8 16;");
                    lblKey.setMinWidth(140);

                    String valStyle = "-fx-text-fill: #0F172A; -fx-font-size: 13px; -fx-font-weight: 500; " +
                        "-fx-padding: 8 16 8 8;";
                    if (sec.fieldColors.containsKey(entry.getKey())) {
                        valStyle = String.format(
                            "-fx-text-fill: %s; -fx-font-size: 13px; -fx-font-weight: 700; -fx-padding: 8 16 8 8;",
                            sec.fieldColors.get(entry.getKey()));
                    }
                    Label lblVal = new Label(entry.getValue());
                    lblVal.setStyle(valStyle);
                    lblVal.setWrapText(true);
                    lblVal.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(lblVal, Priority.ALWAYS);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    HBox fieldRow = new HBox(0);
                    fieldRow.setAlignment(Pos.CENTER_LEFT);
                    fieldRow.getChildren().addAll(lblKey, lblVal);
                    HBox.setHgrow(fieldRow, Priority.ALWAYS);
                    GridPane.setHgrow(fieldRow, Priority.ALWAYS);
                    grid.add(fieldRow, 0, row);

                    if (row > 0) {
                        Separator sep = new Separator();
                        sep.setStyle("-fx-padding: 0 16;");
                    }
                    row++;
                }
                secBox.getChildren().add(grid);
                scrollContent.getChildren().add(secBox);

            } else if (sec.columns != null) {
                // Table section
                VBox secBox = new VBox(0);
                secBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; " +
                    "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 10; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 6, 0, 0, 1);");
                HBox.setHgrow(secBox, Priority.ALWAYS);

                if (sec.header != null) {
                    HBox secHeaderRow = new HBox(8);
                    secHeaderRow.setAlignment(Pos.CENTER_LEFT);
                    secHeaderRow.setStyle("-fx-background-color: #F1F5F9; -fx-padding: 9 14; " +
                        "-fx-background-radius: 10 10 0 0;");
                    Region accentBar = new Region();
                    accentBar.setStyle("-fx-background-color: #1D4ED8; -fx-min-width: 4; -fx-max-width: 4; -fx-min-height: 14; -fx-background-radius: 2;");
                    Label secHeader = new Label(sec.header);
                    secHeader.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #334155;");
                    Region hSpacer = new Region();
                    HBox.setHgrow(hSpacer, Priority.ALWAYS);
                    secHeaderRow.getChildren().addAll(accentBar, secHeader, hSpacer);
                    secHeaderRow.setMaxWidth(Double.MAX_VALUE);
                    secBox.getChildren().add(secHeaderRow);
                }

                if (sec.rows != null && !sec.rows.isEmpty()) {
                    GridPane table = new GridPane();
                    table.setHgap(0);
                    table.setVgap(0);
                    table.setStyle("-fx-padding: 0;");
                    table.setPrefWidth(Double.MAX_VALUE);
                    table.setMaxWidth(Double.MAX_VALUE);

                    double colPercent = 100.0 / sec.columns.length;
                    table.getColumnConstraints().clear();
                    for (int c = 0; c < sec.columns.length; c++) {
                        ColumnConstraints cc = new ColumnConstraints();
                        cc.setPercentWidth(colPercent);
                        cc.setHgrow(Priority.ALWAYS);
                        table.getColumnConstraints().add(cc);
                    }

                    // Header row
                    for (int c = 0; c < sec.columns.length; c++) {
                        Label hdr = new Label(sec.columns[c]);
                        hdr.setStyle("-fx-background-color: #E2E8F0; -fx-padding: 8 12; " +
                            "-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #334155; -fx-alignment: CENTER_LEFT;");
                        hdr.setMaxWidth(Double.MAX_VALUE);
                        GridPane.setHgrow(hdr, Priority.ALWAYS);
                        GridPane.setColumnIndex(hdr, c);
                        GridPane.setRowIndex(hdr, 0);
                        table.getChildren().add(hdr);
                    }

                    int r = 1;
                    for (Map<String, String> rowData : sec.rows) {
                        String bg = (r % 2 == 0) ? "#F8FAFC" : "#FFFFFF";
                        for (int c = 0; c < sec.columns.length; c++) {
                            String val = rowData.get(sec.columns[c]);
                            Label cell = new Label(val != null ? val : "—");
                            cell.setStyle(String.format(
                                "-fx-background-color: %s; -fx-padding: 8 12; " +
                                "-fx-font-size: 12px; -fx-text-fill: #1E293B; -fx-alignment: CENTER_LEFT;",
                                bg));
                            cell.setMaxWidth(Double.MAX_VALUE);
                            cell.setWrapText(true);
                            GridPane.setHgrow(cell, Priority.ALWAYS);
                            GridPane.setColumnIndex(cell, c);
                            GridPane.setRowIndex(cell, r);
                            table.getChildren().add(cell);
                        }
                        r++;
                    }
                    secBox.getChildren().add(table);
                } else {
                    Label empty = new Label("  Sem dados");
                    empty.setStyle("-fx-padding: 16; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
                    secBox.getChildren().add(empty);
                }
                scrollContent.getChildren().add(secBox);
            }
        }

        scrollPane.setContent(scrollContent);

        // ─── FOOTER ───────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 12 24; " +
            "-fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0;");

        Label escHint = new Label("ESC para fechar");
        escHint.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-weight: 600;");
        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);

        Button closeBtn = new Button("Fechar");
        closeBtn.setStyle(
            "-fx-background-color: #1D4ED8; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; " +
            "-fx-font-weight: 700; -fx-padding: 8 26; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        closeBtn.setOnAction(e -> dialog.close());
        UiUtils.applyHoverElevation(closeBtn);
        UiUtils.applyPressFeedback(closeBtn);
        footer.getChildren().addAll(escHint, fSpacer, closeBtn);

        root.getChildren().addAll(headerBox, scrollPane, footer);

        Scene scene = new Scene(root, width, height);
        scene.setOnKeyPressed(ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                dialog.close();
            }
        });
        dialog.setScene(scene);
        dialog.setTitle(title);
        if (owner != null) {
            dialog.setX(owner.getX() + (owner.getWidth() - width) / 2);
            dialog.setY(owner.getY() + (owner.getHeight() - height) / 2);
        }
        dialog.show();
    }

    /** Escolhe um ícone adequado com base nas palavras-chave do título. */
    private static String inferIcon(String t) {
        if (t == null) return "📋";
        String s = t.toLowerCase(java.util.Locale.ROOT);
        if (s.contains("venda") || s.contains("factura") || s.contains("fatura") || s.contains("cotação") || s.contains("nota")) return "🧾";
        if (s.contains("produto") || s.contains("stock") || s.contains("kardex") || s.contains("artigo")) return "📦";
        if (s.contains("cliente")) return "👥";
        if (s.contains("fornecedor")) return "🚚";
        if (s.contains("compra")) return "🛒";
        if (s.contains("transferência") || s.contains("transferencia") || s.contains("guia")) return "🔁";
        if (s.contains("despesa")) return "💸";
        if (s.contains("produção") || s.contains("producao")) return "🏭";
        if (s.contains("categoria") || s.contains("família") || s.contains("familia")) return "🏷️";
        if (s.contains("unidade")) return "📐";
        if (s.contains("turno") || s.contains("caixa")) return "💰";
        if (s.contains("log") || s.contains("evento")) return "📜";
        if (s.contains("armazém") || s.contains("armazem")) return "🏢";
        return "📋";
    }

    private static class Section {
        String header;
        Map<String, String> fields = new LinkedHashMap<>();
        Map<String, String> fieldColors = new LinkedHashMap<>();
        Map<String, Boolean> fieldRich = new LinkedHashMap<>();
        String[] columns;
        List<Map<String, String>> rows;
        Map<String, String> kvPairs;

        Section(String header, String[] columns, List<Map<String, String>> rows,
                Map<String, String> kvPairs, Map<String, String> fieldColors) {
            this.header = header;
            this.columns = columns;
            this.rows = rows;
            this.kvPairs = kvPairs;
        }
    }
}
