package com.billywater.views;

import com.billywater.servico.GeradorPDF;
import com.billywater.servico.ServicoRelatorios;
import com.billywater.servico.ServicoRelatorios.Relatorio;
import com.billywater.servico.ServicoRelatorios.Rows;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Relatórios (estrutura SGV): 15 relatórios com dados reais + exportação PDF/CSV. */
public class RelatoriosView extends BaseView {

    private final TextField periodo = new TextField();
    private final Label estado = new Label("");
    private final TableView<Object[]> tabela = new TableView<>();
    private final ServicoRelatorios servico = new ServicoRelatorios();
    private Relatorio actual;

    public RelatoriosView() {
        super("Relatórios", "15 relatórios com paridade de números e exportação (PDF/CSV)");
        periodo.setText(java.time.YearMonth.now().toString());
        periodo.setPromptText("Período AAAA-MM");
        periodo.setPrefWidth(140);
        Button bPDF = Ui.primario("Exportar PDF");
        Button bCSV = Ui.fantasma("Exportar CSV");
        HBox top = new HBox(10);
        top.setPadding(new Insets(2, 0, 0, 0));
        top.getChildren().addAll(periodo, bPDF, bCSV);
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(6));
        int idx = 0;
        for (Relatorio r : servico.listar()) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card-pane");
            card.setPadding(new Insets(14)); card.setPrefWidth(300);
            Label t = new Label(r.titulo()); t.getStyleClass().add("view-subtitle"); t.setWrapText(true);
            Label d = new Label(r.descricao()); d.getStyleClass().add("kpi-unidade"); d.setWrapText(true);
            Button abrir = Ui.fantasma("Ver dados");
            abrir.setMaxWidth(Double.MAX_VALUE);
            final String cod = r.codigo();
            abrir.setOnAction(e -> carregar(cod));
            card.getChildren().addAll(t, d, abrir);
            grid.add(card, idx % 3, idx / 3); idx++;
        }
        estado.getStyleClass().add("totais"); estado.setWrapText(true); estado.setMaxWidth(900);
        tabela.setPrefHeight(220); tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        body().getChildren().addAll(top, grid, tabela, estado);
        bPDF.setOnAction(e -> exportar("pdf"));
        bCSV.setOnAction(e -> exportar("csv"));
    }

    private void carregar(String cod) {
        Relatorio r = servico.listar().stream().filter(x -> x.codigo().equals(cod)).findFirst().orElse(null);
        if (r == null) return;
        actual = r;
        Ui.emSegundoPlano(() -> {
            try {
                Rows rows = servico.consultar(r, periodo.getText().trim());
                Platform.runLater(() -> { montarTabela(rows.colunas()); tabela.getItems().setAll(rows.linhas()); estado.setText(r.titulo() + " — " + rows.linhas().size() + " linha(s)"); });
            } catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
        });
    }

    private void montarTabela(List<String> colunas) {
        List<TableColumn<Object[], String>> cols = new ArrayList<>();
        for (int i = 0; i < colunas.size(); i++) {
            final int idx = i;
            TableColumn<Object[], String> c = new TableColumn<>(colunas.get(i));
            c.setCellValueFactory(f -> new javafx.beans.property.SimpleStringProperty(f.getValue().length > idx && f.getValue()[idx] != null ? f.getValue()[idx].toString() : ""));
            c.setPrefWidth(150);
            cols.add(c);
        }
        tabela.getColumns().setAll(cols);
    }

    private void exportar(String tipo) {
        if (actual == null) { Ui.aviso("Relatório", "Carregue primeiro um relatório."); return; }
        Ui.emSegundoPlano(() -> {
            try {
                Rows rows = servico.consultar(actual, periodo.getText().trim());
                java.nio.file.Path out = "pdf".equals(tipo)
                        ? GeradorPDF.gerarRelatorio(actual.codigo(), periodo.getText().trim(), rows.colunas(), rows.linhas())
                        : GeradorPDF.exportarCsv(actual.codigo(), periodo.getText().trim(), rows.colunas(), rows.linhas());
                Platform.runLater(() -> estado.setText(actual.titulo() + " exportado em " + out.toAbsolutePath()));
            } catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
        });
    }
}
