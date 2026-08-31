package com.billywater.views;

import com.billywater.dao.ArmazemDAO;
import com.billywater.dao.LinhaVendaDAO;
import com.billywater.dao.MovimentoStockDAO;
import com.billywater.dao.ProdutoDAO;
import com.billywater.dao.StockDAO;
import com.billywater.dao.TransferenciaArmazemDAO;
import com.billywater.dao.VendaDAO;
import com.billywater.domain.Armazem;
import com.billywater.domain.LinhaVenda;
import com.billywater.domain.Produto;
import com.billywater.domain.Venda;
import com.billywater.servico.ServicoArmazens;
import com.billywater.servico.ServicoLoja;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Loja / POS — vendas VD com baixa de stock na loja. */
public class LojaView extends BaseView {

    private final TableView<Venda> tabela = new TableView<>();
    private final Label estado = new Label("");

    public LojaView() {
        super("Loja", "Vendas (POS/VD) e devoluções");
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(Ui.primario("Nova venda"), Ui.fantasma("Histórico"));
        List<TableColumn<Venda, ?>> cols = List.of(
                Ui.col("Série", "serie", 70), Ui.col("Nº", "numero", 60), Ui.col("Cliente", "clienteId", 90),
                Ui.col("Subtotal", "subtotal", 120), Ui.col("Total", "total", 120),
                Ui.col("Pagamento", "formaPagamento", 130), Ui.col("Devolvida", "devolvida", 90));
        Ui.tabela(tabela, cols, 470);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.acao(filtros.getChildren().get(0), this::nova);
        Ui.acao(filtros.getChildren().get(1), this::recarregar);
        attach();
    }

    private void nova() {
        FormDialog f = new FormDialog();
        try {
            Armazem loja = new ArmazemDAO().buscarPorTipo("LOJA");
            ComboBox<ComboItem> prod = Ui.combo(new ProdutoDAO().todos().stream().map(p -> new ComboItem(p.id, p.nome)).toList());
            f.add("Produto", prod);
            TextField qtd = Ui.campo("1"); f.add("Quantidade", qtd);
            ComboBox<String> pg = new ComboBox<>();
            pg.getItems().addAll("DINHEIRO", "MPESA", "EMOLA", "MKESH", "POS", "BANCO");
            pg.setValue("DINHEIRO"); f.add("Pagamento", pg);
            if (!f.mostrar("Nova venda")) return;
            Long produtoId = f.comboId("Produto");
            BigDecimal quantidade = f.valor("Quantidade");
            Produto p = new ProdutoDAO().buscarPorId(produtoId);
            if (p == null || loja == null) { Ui.aviso("Loja", "Produto ou loja não encontrados."); return; }
            LinhaVenda lv = new LinhaVenda();
            lv.produtoId = produtoId; lv.quantidade = quantidade; lv.preco = p.precoVenda;
            lv.subtotal = quantidade.multiply(p.precoVenda);
            Venda v = new Venda();
            v.serie = "VD"; v.clienteId = null; v.formaPagamento = f.comboText("Pagamento");
            v.desconto = BigDecimal.ZERO; v.operador = Sessao.atual().username;
            new ServicoLoja(new VendaDAO(), new LinhaVendaDAO(),
                    new ServicoArmazens(new StockDAO(), new MovimentoStockDAO(), new TransferenciaArmazemDAO()))
                    .registarVenda(v, new ArrayList<>(List.of(lv)), loja.id);
            Ui.info("Venda", "VD #" + v.numero + " registada. Total " + Ui.moeda(v.total) + " MT.");
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Venda> lista = new VendaDAO().hoje(); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " venda(s) hoje"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
