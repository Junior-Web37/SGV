package com.billywater.views;

import com.billywater.dao.ArmazemDAO;
import com.billywater.dao.CompraDAO;
import com.billywater.dao.FornecedorDAO;
import com.billywater.dao.LinhaCompraDAO;
import com.billywater.dao.MovimentoStockDAO;
import com.billywater.dao.ProdutoDAO;
import com.billywater.dao.StockDAO;
import com.billywater.dao.TransferenciaArmazemDAO;
import com.billywater.domain.Compra;
import com.billywater.domain.Fornecedor;
import com.billywater.domain.LinhaCompra;
import com.billywater.domain.Produto;
import com.billywater.servico.ServicoArmazens;
import com.billywater.ui.ComboItem;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Sessao;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** Compras — entrada de materiais no armazém (guia GC → stock). */
public class ComprasView extends BaseView {

    private final TableView<Compra> tabela = new TableView<>();
    private final Label estado = new Label("");

    public ComprasView() {
        super("Compras", "Entrada de materiais no armazém (guia de compra)");
        HBox filtros = new HBox(10);
        Button bNova = Ui.primario("Nova compra");
        filtros.getChildren().add(bNova);
        List<TableColumn<Compra, ?>> cols = List.of(
                Ui.col("Série", "serie", 70), Ui.col("Nº", "numero", 60), Ui.col("Fornecedor", "fornecedorId", 90),
                Ui.col("Armazém", "armazemId", 90), Ui.col("Valor total", "valorTotal", 130),
                Ui.col("Data", "data", 110), Ui.col("Estado", "estado", 100));
        Ui.tabela(tabela, cols, 460);
        estado.getStyleClass().add("totais"); estado.setWrapText(true);
        body().getChildren().addAll(filtros, tabela, estado);
        bNova.setOnAction(e -> nova());
        attach();
    }

    private void nova() {
        FormDialog f = new FormDialog();
        try {
            ComboBox<ComboItem> forn = Ui.combo(new FornecedorDAO().pesquisar("%").stream().map(x -> new ComboItem(x.id, x.nome)).toList());
            f.add("Fornecedor", forn);
            ComboBox<ComboItem> arm = Ui.combo(new ArmazemDAO().activos().stream().map(a -> new ComboItem(a.id, a.nome)).toList());
            f.add("Armazém", arm);
            ComboBox<ComboItem> prod = Ui.combo(new ProdutoDAO().todos().stream().map(p -> new ComboItem(p.id, p.nome)).toList());
            f.add("Produto", prod);
            TextField qtd = Ui.campo("0"); f.add("Quantidade", qtd);
            TextField custo = Ui.campo("0,00"); f.add("Custo unitário", custo);
            if (!f.mostrar("Nova compra")) return;
            Long fornId = f.comboId("Fornecedor");
            Long armazemId = f.comboId("Armazém");
            Long produtoId = f.comboId("Produto");
            BigDecimal q = f.valor("Quantidade");
            BigDecimal custoU = f.valor("Custo unitário");
            if (armazemId == null) { Ui.aviso("Compra", "Seleccione o armazém de destino."); return; }
            Compra c = new Compra();
            c.serie = "GC"; c.fornecedorId = fornId; c.armazemId = armazemId; c.estado = "CONCLUIDA";
            c.data = LocalDate.now(); c.operador = Sessao.atual().username;
            c.numero = new CompraDAO().proximoNumero("GC");
            c.valorTotal = q.multiply(custoU).setScale(2, RoundingMode.HALF_UP);
            c.referencia = "GC " + c.numero;
            c.id = new CompraDAO().inserir(c);
            LinhaCompra lc = new LinhaCompra();
            lc.compraId = c.id; lc.produtoId = produtoId; lc.quantidade = q; lc.custoUnitario = custoU; lc.subtotal = c.valorTotal;
            new LinhaCompraDAO().inserir(lc);
            new ServicoArmazens(new StockDAO(), new MovimentoStockDAO(), new TransferenciaArmazemDAO())
                    .entrarStock(produtoId, armazemId, q, "ENTRADA_ARMAZEM", c.referencia);
            Produto p = new ProdutoDAO().buscarPorId(produtoId);
            if (p != null) { p.precoCompra = custoU; new ProdutoDAO().salvar(p); }
            Ui.info("Compra", "Guia " + c.referencia + " registada. Stock do armazém actualizado.");
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        Ui.emSegundoPlano(() -> {
            try { List<Compra> lista = new CompraDAO().todos(); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " compra(s)"); }); }
            catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
        });
    }

    @Override protected void attach() { recarregar(); }
}
