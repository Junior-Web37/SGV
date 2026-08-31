package com.billywater.views;

import com.billywater.dao.ArmazemDAO;
import com.billywater.domain.Armazem;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Armazéns / locais — CRUD. */
public class ArmazensView extends BaseView {

    private final TableView<Armazem> tabela = new TableView<>();
    private final Label estado = new Label("");

    public ArmazensView() {
        super("Armazéns", "Locais (armazém / loja) que detêm stock");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Novo"), Ui.fantasma("Editar"));
        List<TableColumn<Armazem, ?>> cols = List.of(
                Ui.col("Código", "codigo", 90), Ui.col("Nome", "nome", 260),
                Ui.col("Localização", "localizacao", 220), Ui.col("Tipo", "tipo", 100), Ui.col("Activo", "ativo", 80));
        Ui.tabela(tabela, cols, 470);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado,
                pendencia("Consistência: stock único por local; transferência atómica em StockView."));
        Ui.acao(filtros.getChildren().get(0), this::novo);
        Ui.acao(filtros.getChildren().get(1), this::editar);
        attach();
    }

    private void novo() {
        FormDialog f = new FormDialog();
        f.add("Código", Ui.campo("AR-01")); f.add("Nome", Ui.campo("Armazém Central"));
        f.add("Localização", Ui.campo("Polana, Maputo")); f.add("Tipo", Ui.campo("ARMAZEM"));
        if (!f.mostrar("Novo local")) return;
        try {
            Armazem a = new Armazem(); a.codigo = f.texto("Código"); a.nome = f.texto("Nome");
            a.localizacao = f.texto("Localização"); a.tipo = f.texto("Tipo"); a.ativo = true;
            new ArmazemDAO().salvar(a); recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void editar() {
        Armazem sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um local."); return; }
        FormDialog f = new FormDialog();
        f.add("Nome", Ui.campo("Nome")); f.setTexto("Nome", sel.nome);
        if (!f.mostrar("Editar local")) return;
        try { sel.nome = f.texto("Nome"); new ArmazemDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Armazem> lista = new ArmazemDAO().activos(); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " local(is)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
