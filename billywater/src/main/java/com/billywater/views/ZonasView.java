package com.billywater.views;

import com.billywater.dao.ZonaDAO;
import com.billywater.domain.Zona;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Zonas / rotas — CRUD. */
public class ZonasView extends BaseView {

    private final TableView<Zona> tabela = new TableView<>();
    private final Label estado = new Label("");

    public ZonasView() {
        super("Zonas / Rotas", "Zonas de distribuição e rotas de leitura");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Nova"), Ui.fantasma("Editar"));
        List<TableColumn<Zona, ?>> cols = List.of(
                Ui.col("Código", "codigo", 90), Ui.col("Nome", "nome", 220),
                Ui.col("Município", "municipio", 140), Ui.col("Rota", "rota", 100),
                Ui.col("Activa", "ativa", 80));
        Ui.tabela(tabela, cols, 480);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.acao(filtros.getChildren().get(0), this::nova);
        Ui.acao(filtros.getChildren().get(1), this::editar);
        attach();
    }

    private void nova() {
        FormDialog f = new FormDialog();
        f.add("Código", Ui.campo("Z-01")); f.add("Nome", Ui.campo("Central"));
        f.add("Município", Ui.campo("Maputo")); f.add("Rota", Ui.campo("R1"));
        if (!f.mostrar("Nova zona")) return;
        try {
            Zona z = new Zona(); z.codigo = f.texto("Código"); z.nome = f.texto("Nome"); z.municipio = f.texto("Município"); z.rota = f.texto("Rota"); z.ativa = true; z.ativo = true;
            new ZonaDAO().salvar(z); recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void editar() {
        Zona sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione uma zona."); return; }
        FormDialog f = new FormDialog();
        f.add("Nome", Ui.campo("Nome")); f.setTexto("Nome", sel.nome);
        if (!f.mostrar("Editar zona")) return;
        try { sel.nome = f.texto("Nome"); new ZonaDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Zona> lista = new ZonaDAO().pesquisar("%"); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " zona(s)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
