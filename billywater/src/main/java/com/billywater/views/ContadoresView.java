package com.billywater.views;

import com.billywater.dao.ContadorDAO;
import com.billywater.domain.Contador;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Contadores/medidores — CRUD. */
public class ContadoresView extends BaseView {

    private final TableView<Contador> tabela = new TableView<>();
    private final Label estado = new Label("");

    public ContadoresView() {
        super("Contadores", "Medidores de água e estado");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Novo"), Ui.fantasma("Editar"));
        List<TableColumn<Contador, ?>> cols = List.of(
                Ui.col("Número", "numero", 110), Ui.col("Série", "numeroSerie", 140),
                Ui.col("Marca", "marca", 120), Ui.col("Modelo", "modelo", 120),
                Ui.col("Estado", "estado", 120), Ui.col("Última leitura", "ultimaLeitura", 120));
        Ui.tabela(tabela, cols, 480);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.acao(filtros.getChildren().get(0), this::novo);
        Ui.acao(filtros.getChildren().get(1), this::editar);
        attach();
    }

    private void novo() {
        FormDialog f = new FormDialog();
        f.add("Número", Ui.campo("CT-0001")); f.add("Nº série", Ui.campo("K-2024-0001"));
        f.add("Marca", Ui.campo("Kamstrup")); f.add("Modelo", Ui.campo("MultiJet"));
        f.add("Estado", Ui.campo("ACTIVO"));
        if (!f.mostrar("Novo contador")) return;
        try {
            Contador c = new Contador();
            c.numero = f.texto("Número"); c.numeroSerie = f.texto("Nº série"); c.marca = f.texto("Marca");
            c.modelo = f.texto("Modelo"); c.estado = f.texto("Estado"); c.multiplicador = 1;
            new ContadorDAO().salvar(c);
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void editar() {
        Contador sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um contador."); return; }
        FormDialog f = new FormDialog();
        f.add("Estado", Ui.campo("Estado")); f.add("Última leitura", Ui.campo("0"));
        f.setTexto("Estado", sel.estado); f.setValor("Última leitura", sel.ultimaLeitura);
        if (!f.mostrar("Editar contador")) return;
        try { sel.estado = f.texto("Estado"); sel.ultimaLeitura = f.valor("Última leitura"); new ContadorDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Contador> lista = new ContadorDAO().pesquisar("%"); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " contador(es)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
