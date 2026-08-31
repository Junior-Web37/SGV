package com.billywater.views;

import com.billywater.dao.AuditoriaDAO;
import com.billywater.domain.Auditoria;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Logs / auditoria (Registo de Acções — SGV). */
public class LogsView extends BaseView {

    private final TableView<Auditoria> tabela = new TableView<>();
    private final Label estado = new Label("");

    public LogsView() {
        super("Logs / Auditoria", "Registo de acções do utilizador");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.fantasma("Actualizar"));
        List<TableColumn<Auditoria, ?>> cols = List.of(
                Ui.col("Hora", "data", 170), Ui.col("Utilizador", "utilizador", 130),
                Ui.col("Acção", "accao", 200), Ui.col("Detalhe", "detalhe", 300));
        Ui.tabela(tabela, cols, 470);
        estado.getStyleClass().add("totais"); estado.setWrapText(true);
        body().getChildren().addAll(filtros, tabela, estado,
                pendencia("Conexão, login, edição e facturação são gravadas em SEG_AUDITORIA."));
        Ui.acao(filtros.getChildren().get(0), this::recarregar);
        attach();
    }

    private void recarregar() {
        Ui.emSegundoPlano(() -> {
            try { List<Auditoria> lista = new AuditoriaDAO().recentes(200); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " registo(s) recentes"); }); }
            catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
        });
    }

    @Override protected void attach() { recarregar(); }
}
