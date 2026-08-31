package com.billywater.views;

import com.billywater.dao.PagamentoDAO;
import com.billywater.dao.ParametroDAO;
import com.billywater.domain.Pagamento;
import com.billywater.servico.GeradorPDF;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.util.List;

/** Recibos (REC) — listagem e reimpressão em PDF. */
public class RecibosView extends BaseView {

    private final TableView<Pagamento> tabela = new TableView<>();
    private final Label estado = new Label("");

    public RecibosView() {
        super("Recibos", "Listagem de cobranças (REC) e reimpressão");
        HBox filtros = new HBox(10);
        javafx.scene.control.Button bImprimir = Ui.primario("Reimprimir");
        filtros.getChildren().add(bImprimir);
        List<TableColumn<Pagamento, ?>> cols = List.of(
                Ui.col("REC", "recNumero", 80), Ui.col("Cliente", "clienteId", 120),
                Ui.col("Método", "metodo", 130), Ui.col("Valor", "valor", 120), Ui.col("Recebido", "valorRecebido", 120),
                Ui.col("Troco", "troco", 90), Ui.col("Operador", "operador", 130), Ui.col("Data", "criadoEm", 160));
        Ui.tabela(tabela, cols, 500);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        bImprimir.setOnAction(e -> reimprimir());
        attach();
    }

    private void reimprimir() {
        Pagamento sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Recibo", "Seleccione um recibo."); return; }
        Ui.emSegundoPlano(() -> {
            try {
                String cliente = sel.clienteId == null ? "" : String.valueOf(sel.clienteId);
                ParametroDAO par = new ParametroDAO();
                var p = par.unico();
                String empresa = p == null || p.nomeEmpresa == null ? "BILLY WATER" : p.nomeEmpresa;
                java.nio.file.Path out = GeradorPDF.gerarRecibo(sel.recNumero, cliente, sel.operador, sel.metodo,
                        Ui.moeda(sel.valor), Ui.moeda(sel.valorRecebido), Ui.moeda(sel.troco), empresa);
                Platform.runLater(() -> estado.setText("Recibo reimpresso em " + out.toAbsolutePath()));
            } catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
        });
    }

    @Override protected void attach() {
        try { List<Pagamento> lista = new PagamentoDAO().todos(); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " recibo(s)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }
}
