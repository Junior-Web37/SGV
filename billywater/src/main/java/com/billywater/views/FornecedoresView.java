package com.billywater.views;

import com.billywater.dao.FornecedorDAO;
import com.billywater.domain.Fornecedor;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.List;

/** Fornecedores de materiais — CRUD. */
public class FornecedoresView extends BaseView {

    private final TableView<Fornecedor> tabela = new TableView<>();
    private final Label estado = new Label("");

    public FornecedoresView() {
        super("Fornecedores", "Cadastro de fornecedores e saldo em aberto");
        HBox filtros = new HBox(10);
        Button bNovo = Ui.primario("Novo");
        Button bEditar = Ui.fantasma("Editar");
        Button bAnular = Ui.perigo("Activar/Desactivar");
        filtros.getChildren().addAll(bNovo, bEditar, bAnular);
        List<TableColumn<Fornecedor, ?>> cols = List.of(
                Ui.col("Código", "codigo", 90), Ui.col("Nome", "nome", 280), Ui.col("NUIT", "nuit", 120),
                Ui.col("Telefone", "telefone", 130), Ui.col("Cidade", "cidade", 120), Ui.col("Saldo", "saldo", 120));
        Ui.tabela(tabela, cols, 480);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        bNovo.setOnAction(e -> novo());
        bEditar.setOnAction(e -> editar());
        bAnular.setOnAction(e -> alternarAtivo());
        attach();
    }

    private void novo() {
        FormDialog f = new FormDialog();
        f.add("Código", Ui.campo("Código")); f.add("Nome", Ui.campo("Nome")); f.add("NUIT", Ui.campo("NUIT"));
        f.add("Telefone", Ui.campo("Telefone")); f.add("E-mail", Ui.campo("E-mail")); f.add("Endereço", Ui.campo("Endereço")); f.add("Cidade", Ui.campo("Cidade"));
        if (!f.mostrar("Novo fornecedor")) return;
        try {
            Fornecedor x = new Fornecedor();
            x.codigo = f.texto("Código"); x.nome = f.texto("Nome"); x.nuit = f.texto("NUIT"); x.telefone = f.texto("Telefone");
            x.email = f.texto("E-mail"); x.endereco = f.texto("Endereço"); x.cidade = f.texto("Cidade");
            x.ativo = true; x.saldo = BigDecimal.ZERO;
            new FornecedorDAO().salvar(x); recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void editar() {
        Fornecedor sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um fornecedor."); return; }
        FormDialog f = new FormDialog();
        f.add("Nome", Ui.campo("Nome")); f.add("Telefone", Ui.campo("Telefone")); f.add("Cidade", Ui.campo("Cidade"));
        f.add("Saldo", Ui.campo("0,00"));
        f.setTexto("Nome", sel.nome); f.setTexto("Telefone", sel.telefone); f.setTexto("Cidade", sel.cidade); f.setValor("Saldo", sel.saldo);
        if (!f.mostrar("Editar fornecedor")) return;
        try { sel.nome = f.texto("Nome"); sel.telefone = f.texto("Telefone"); sel.cidade = f.texto("Cidade"); sel.saldo = f.valor("Saldo"); new FornecedorDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void alternarAtivo() {
        Fornecedor sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um fornecedor."); return; }
        try { sel.ativo = !Boolean.TRUE.equals(sel.ativo); new FornecedorDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Fornecedor> lista = new FornecedorDAO().pesquisar("%"); Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " fornecedor(es)"); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
