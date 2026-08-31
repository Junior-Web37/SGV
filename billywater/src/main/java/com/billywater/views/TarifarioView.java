package com.billywater.views;

import com.billywater.dao.EscalaoTarifaDAO;
import com.billywater.dao.TarifaDAO;
import com.billywater.domain.Tarifa;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Tarifário AURA — tarifas e escalões. */
public class TarifarioView extends BaseView {

    private final TableView<Tarifa> tabela = new TableView<>();
    private final Label estado = new Label("");

    public TarifarioView() {
        super("Tarifário", "Tarifas AURA e escalões de consumo");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Nova"), Ui.fantasma("Escalões"));
        List<TableColumn<Tarifa, ?>> cols = List.of(
                Ui.col("Código", "codigo", 90), Ui.col("Nome", "nome", 200),
                Ui.col("Categoria", "categoria", 130), Ui.col("Mínimo (m³)", "minimoFacturavel", 120),
                Ui.col("Activa", "ativo", 80));
        Ui.tabela(tabela, cols, 460);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.acao(filtros.getChildren().get(0), this::nova);
        Ui.acao(filtros.getChildren().get(1), this::escaloes);
        attach();
    }

    private void nova() {
        FormDialog f = new FormDialog();
        f.add("Código", Ui.campo("T-DOM")); f.add("Nome", Ui.campo("Doméstico"));
        f.add("Categoria", Ui.campo("DOMESTICO")); f.add("Mínimo (m³)", Ui.campo("5"));
        if (!f.mostrar("Nova tarifa")) return;
        try {
            Tarifa t = new Tarifa();
            t.codigo = f.texto("Código"); t.nome = f.texto("Nome"); t.categoria = f.texto("Categoria");
            t.minimoFacturavel = f.valor("Mínimo (m³)"); t.ativo = true;
            new TarifaDAO().salvar(t); recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void escaloes() {
        Tarifa sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione uma tarifa."); return; }
        FormDialog f = new FormDialog();
        f.add("Limite inferior (m³)", Ui.campo("0")); f.add("Limite superior (m³)", Ui.campo("5"));
        f.add("Preço por m³ (MT)", Ui.campo("28,60"));
        if (!f.mostrar("Escalão de " + sel.nome)) return;
        try {
            var e = new com.billywater.domain.EscalaoTarifa();
            e.tarifaId = sel.id; e.limiteInferior = f.valor("Limite inferior (m³)");
            e.limiteSuperior = f.valor("Limite superior (m³)"); e.precoPorM3 = f.valor("Preço por m³ (MT)");
            new EscalaoTarifaDAO().salvar(e);
            Ui.info("Escalão", "Escalão adicionado.");
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Tarifa> lista = new TarifaDAO().todos(); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " tarifa(s)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
