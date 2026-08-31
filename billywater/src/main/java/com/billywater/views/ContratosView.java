package com.billywater.views;

import com.billywater.dao.ClienteDAO;
import com.billywater.dao.ContadorDAO;
import com.billywater.dao.ContratoDAO;
import com.billywater.dao.TarifaDAO;
import com.billywater.domain.Contrato;
import com.billywater.ui.ComboItem;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Contratos — CRUD com numero_conta / categoria_tarifaria / limite_credito. */
public class ContratosView extends BaseView {

    private final TableView<Contrato> tabela = new TableView<>();
    private final Label estado = new Label("");

    public ContratosView() {
        super("Contratos", "Contratos de fornecimento de água");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Novo"), Ui.fantasma("Editar"));
        List<TableColumn<Contrato, ?>> cols = List.of(
                Ui.col("Nº Conta", "numeroConta", 110), Ui.col("Código", "codigo", 90),
                Ui.col("Cliente", "clienteId", 90), Ui.col("Contador", "contadorId", 90),
                Ui.col("Categoria", "categoriaTarifaria", 130), Ui.col("Estado", "estado", 100));
        Ui.tabela(tabela, cols, 480);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.acao(filtros.getChildren().get(0), this::novo);
        Ui.acao(filtros.getChildren().get(1), this::editar);
        attach();
    }

    private void novo() {
        FormDialog f = new FormDialog();
        try {
            ComboBox<ComboItem> cli = Ui.combo(new ClienteDAO().pesquisar("%").stream().map(c -> new ComboItem(c.id, c.nomeCompleto)).toList());
            f.add("Cliente", cli);
            ComboBox<ComboItem> cont = Ui.combo(new ContadorDAO().pesquisar("%").stream().map(c -> new ComboItem(c.id, c.numero)).toList());
            f.add("Contador", cont);
            ComboBox<ComboItem> tar = Ui.combo(new TarifaDAO().todos().stream().map(t -> new ComboItem(t.id, t.nome)).toList());
            f.add("Tarifa", tar);
            f.add("Nº Conta", Ui.campo("N-000001"));
            f.add("Categoria", Ui.campo("DOMESTICO"));
            if (!f.mostrar("Novo contrato")) return;
            Contrato c = new Contrato();
            c.numeroConta = f.texto("Nº Conta"); c.categoria = f.texto("Categoria");
            c.categoriaTarifaria = f.texto("Categoria"); c.clienteId = f.comboId("Cliente");
            c.contadorId = f.comboId("Contador"); c.tarifaId = f.comboId("Tarifa");
            c.estado = "ACTIVO"; c.cicloFacturacao = "MENSAL"; c.diaLeitura = 10;
            new ContratoDAO().salvar(c);
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void editar() {
        Contrato sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um contrato."); return; }
        FormDialog f = new FormDialog();
        f.add("Estado", Ui.campo("ACTIVO/SUSPENSO/CORTADO"));
        f.setTexto("Estado", sel.estado);
        if (!f.mostrar("Editar contrato")) return;
        try { sel.estado = f.texto("Estado"); new ContratoDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try {
            List<Contrato> lista = new ContratoDAO().activos();
            Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " contrato(s) activo(s)"); });
        } catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
