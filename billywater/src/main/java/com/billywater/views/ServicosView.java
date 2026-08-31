package com.billywater.views;

import com.billywater.dao.TrabalhoDAO;
import com.billywater.domain.Trabalho;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Serviços / ordens de trabalho (instalação COT/FS). */
public class ServicosView extends BaseView {

    private final TableView<Trabalho> tabela = new TableView<>();
    private final Label estado = new Label("");

    public ServicosView() {
        super("Serviços", "Trabalhos / ordens de serviço (COT & FS)");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Novo trabalho"), Ui.fantasma("Actualizar"));
        List<TableColumn<Trabalho, ?>> cols = List.of(
                Ui.col("Código", "codigo", 90), Ui.col("Cliente", "clienteId", 90),
                Ui.col("Descrição", "descricao", 260), Ui.col("Técnico", "tecnico", 120),
                Ui.col("Total", "total", 120), Ui.col("Estado", "estado", 100));
        Ui.tabela(tabela, cols, 470);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.acao(filtros.getChildren().get(0), this::novo);
        Ui.acao(filtros.getChildren().get(1), this::recarregar);
        attach();
    }

    private void novo() {
        FormDialog f = new FormDialog();
        f.add("Cliente ID", Ui.campo("ID")); f.add("Descrição", Ui.campo("Instalação…"));
        f.add("Técnico", Ui.campo("Nome")); f.add("Mão-de-obra", Ui.campo("0,00"));
        if (!f.mostrar("Novo trabalho")) return;
        try {
            Trabalho t = new Trabalho();
            t.clienteId = Long.parseLong(f.texto("Cliente ID")); t.descricao = f.texto("Descrição");
            t.tecnico = f.texto("Técnico"); t.maoObra = f.valor("Mão-de-obra");
            t.subtotal = t.maoObra; t.total = t.maoObra; t.estado = "ABERTO";
            new TrabalhoDAO().salvar(t); recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Trabalho> lista = new TrabalhoDAO().todos(); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " trabalho(s)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
