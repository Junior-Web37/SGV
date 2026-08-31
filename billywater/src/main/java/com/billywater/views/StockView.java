package com.billywater.views;

import com.billywater.dao.ArmazemDAO;
import com.billywater.dao.StockDAO;
import com.billywater.domain.Armazem;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Stock — materiais por local e painel "Por transferir". */
public class StockView extends BaseView {

    private final ComboBox<Armazem> local = new ComboBox<>();
    private final TableView<Object[]> tabela = new TableView<>();
    private final Label estado = new Label("");

    public StockView() {
        super("Stock", "Existemências por local e por transferir");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(local, Ui.fantasma("Actualizar"));
        montarTabela();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPrefHeight(470);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.acao(filtros.getChildren().get(1), this::recarregar);
        local.setOnAction(e -> recarregar());
        attach();
    }

    private void montarTabela() {
        TableColumn<Object[], String> c1 = new TableColumn<>("Produto");
        c1.setCellValueFactory(f -> new javafx.beans.property.SimpleStringProperty(f.getValue().length > 0 && f.getValue()[0] != null ? f.getValue()[0].toString() : ""));
        TableColumn<Object[], String> c2 = new TableColumn<>("Quantidade");
        c2.setCellValueFactory(f -> new javafx.beans.property.SimpleStringProperty(f.getValue().length > 1 && f.getValue()[1] != null ? f.getValue()[1].toString() : ""));
        c1.setPrefWidth(300); c2.setPrefWidth(120);
        tabela.getColumns().setAll(c1, c2);
    }

    private void recarregar() {
        try {
            Armazem a = local.getValue();
            if (a == null) return;
            List<Object[]> lista = new StockDAO().listarPorLocal(a.id);
            Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText("Stock em " + a.nome + " — " + lista.size() + " item(ns)"); });
        } catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() {
        try { List<Armazem> locais = new ArmazemDAO().activos(); Platform.runLater(() -> { local.getItems().setAll(locais); if (!locais.isEmpty()) local.setValue(locais.get(0)); recarregar(); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }
}
