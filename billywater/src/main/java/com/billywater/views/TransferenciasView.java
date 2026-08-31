package com.billywater.views;

import com.billywater.dao.ArmazemDAO;
import com.billywater.dao.MovimentoStockDAO;
import com.billywater.dao.ProdutoDAO;
import com.billywater.dao.StockDAO;
import com.billywater.dao.TransferenciaArmazemDAO;
import com.billywater.domain.Armazem;
import com.billywater.domain.Produto;
import com.billywater.domain.TransferenciaArmazem;
import com.billywater.servico.ServicoArmazens;
import com.billywater.ui.ComboItem;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Sessao;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.List;

/** Transferências armazém↔loja — atómicas. */
public class TransferenciasView extends BaseView {

    private final TableView<TransferenciaArmazem> tabela = new TableView<>();
    private final Label estado = new Label("");

    public TransferenciasView() {
        super("Transferências", "Transferência atómica de stock entre locais");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Nova transferência"));
        List<TableColumn<TransferenciaArmazem, ?>> cols = List.of(
                Ui.col("Série", "serie", 70), Ui.col("Nº", "numero", 60), Ui.col("Origem", "origemId", 90),
                Ui.col("Destino", "destinoId", 90), Ui.col("Estado", "estado", 120), Ui.col("Referência", "referencia", 140));
        Ui.tabela(tabela, cols, 470);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado,
                pendencia("Regras: proibida transferência para o próprio; origem≠destino; qtd>0; stock na origem."));
        Ui.acao(filtros.getChildren().get(0), this::nova);
        attach();
    }

    private void nova() {
        FormDialog f = new FormDialog();
        try {
            ComboBox<ComboItem> origem = Ui.combo(new ArmazemDAO().activos().stream().map(a -> new ComboItem(a.id, a.nome)).toList());
            f.add("Origem", origem);
            ComboBox<ComboItem> destino = Ui.combo(new ArmazemDAO().activos().stream().map(a -> new ComboItem(a.id, a.nome)).toList());
            f.add("Destino", destino);
            ComboBox<ComboItem> prod = Ui.combo(new ProdutoDAO().todos().stream().map(p -> new ComboItem(p.id, p.nome)).toList());
            f.add("Produto", prod);
            TextField qtd = Ui.campo("1"); f.add("Quantidade", qtd);
            if (!f.mostrar("Nova transferência")) return;
            TransferenciaArmazem t = new TransferenciaArmazem();
            t.serie = "TR"; t.origemId = f.comboId("Origem"); t.destinoId = f.comboId("Destino");
            t.referencia = "TR " + System.currentTimeMillis(); t.operador = Sessao.atual().username;
            long numero = System.currentTimeMillis() % 100000; t.numero = numero;
            Long produtoId = f.comboId("Produto");
            new ServicoArmazens(new StockDAO(), new MovimentoStockDAO(), new TransferenciaArmazemDAO())
                    .transferir(t, produtoId, f.valor("Quantidade"));
            Ui.info("Transferência", "Transferência concluída.");
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<TransferenciaArmazem> lista = new TransferenciaArmazemDAO().todos(); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " transferência(s)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
