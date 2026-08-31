package com.billywater.views;

import com.billywater.dao.ClienteDAO;
import com.billywater.domain.Cliente;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.List;

/** Clientes — CRUD. */
public class ClientesView extends BaseView {

    private final TableView<Cliente> tabela = new TableView<>();
    private final Label estado = new Label("");
    private final TextField busca = new TextField();

    public ClientesView() {
        super("Clientes & Contratos", "Cadastro de consumidores de água");
        HBox filtros = new HBox(10);
        busca.setPromptText("Pesquisar (nome, código, NUIT)…");
        busca.setPrefWidth(260);
        filtros.getChildren().addAll(busca, Ui.primario("Novo"), Ui.fantasma("Editar"), Ui.perigo("Activar/Desactivar"));
        List<TableColumn<Cliente, ?>> cols = List.of(
                Ui.col("Código", "codigo", 90), Ui.col("Nome", "nomeCompleto", 300),
                Ui.col("Telefone", "telefone", 130), Ui.col("Bairro", "bairro", 130),
                Ui.col("Categoria", "categoriaTarifaria", 130));
        Ui.tabela(tabela, cols, 480);
        estado.getStyleClass().add("totais");
        body().getChildren().addAll(filtros, tabela, estado);
        Ui.setupDebounce(busca, this::recarregar, 180);
        Ui.acao(filtros.getChildren().get(1), this::novo);
        Ui.acao(filtros.getChildren().get(2), this::editar);
        Ui.acao(filtros.getChildren().get(3), this::alternar);
        attach();
    }

    private void novo() {
        FormDialog f = new FormDialog();
        f.add("Código", Ui.campo("C-0001")); f.add("Nome", Ui.campo("Nome completo"));
        f.add("Telefone", Ui.campo("+258 ……")); f.add("Endereço", Ui.campo("Rua"));
        f.add("Bairro", Ui.campo("Bairro")); f.add("Cidade", Ui.campo("Maputo"));
        if (!f.mostrar("Novo cliente")) return;
        try {
            Cliente c = new Cliente();
            c.codigo = f.texto("Código"); c.nomeCompleto = f.texto("Nome"); c.telefone = f.texto("Telefone");
            c.endereco = f.texto("Endereço"); c.bairro = f.texto("Bairro"); c.cidade = f.texto("Cidade");
            c.ativo = true; c.limiteCredito = BigDecimal.ZERO; c.saldo = BigDecimal.ZERO; c.tipo = "SINGULAR";
            new ClienteDAO().salvar(c);
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void editar() {
        Cliente sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um cliente."); return; }
        FormDialog f = new FormDialog();
        f.add("Telefone", Ui.campo("Telefone")); f.add("Endereço", Ui.campo("Endereço"));
        f.add("Limite crédito", Ui.campo("0,00"));
        f.setTexto("Telefone", sel.telefone); f.setTexto("Endereço", sel.endereco); f.setValor("Limite crédito", sel.limiteCredito);
        if (!f.mostrar("Editar cliente")) return;
        try { sel.telefone = f.texto("Telefone"); sel.endereco = f.texto("Endereço"); sel.limiteCredito = f.valor("Limite crédito"); new ClienteDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void alternar() {
        Cliente sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um cliente."); return; }
        try { sel.ativo = !Boolean.TRUE.equals(sel.ativo); new ClienteDAO().salvar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try {
            List<Cliente> lista = new ClienteDAO().pesquisar(busca.getText().isEmpty() ? "%" : busca.getText());
            Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " cliente(s)"); });
        } catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
