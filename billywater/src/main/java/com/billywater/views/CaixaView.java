package com.billywater.views;

import com.billywater.dao.MovimentoCaixaDAO;
import com.billywater.dao.PagamentoDAO;
import com.billywater.dao.SessaoCaixaDAO;
import com.billywater.domain.Pagamento;
import com.billywater.domain.SessaoCaixa;
import com.billywater.servico.ServicoCaixa;
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
import java.util.List;
import java.util.Map;

/** Caixa — sessões, cobrança (REC), fecho cego, Fita Z e estorno. */
public class CaixaView extends BaseView {

    private final TableView<Pagamento> tabela = new TableView<>();
    private final Label estado = new Label("");
    private final SessaoCaixaDAO sessaoDAO = new SessaoCaixaDAO();

    public CaixaView() {
        super("Caixa", "Sessões de caixa, cobranças, fecho e estorno");
        HBox filtros = new HBox(10);
        Button bAbrir = Ui.primario("Abrir sessão");
        Button bReceber = Ui.primario("Cobrar");
        Button bFechar = Ui.fantasma("Fechar cego");
        Button bZ = Ui.fantasma("Fita Z");
        Button bEstornar = Ui.perigo("Estornar REC");
        filtros.getChildren().addAll(bAbrir, bReceber, bFechar, bZ, bEstornar);
        List<TableColumn<Pagamento, ?>> cols = List.of(
                Ui.col("REC", "recNumero", 80), Ui.col("Factura", "facturaId", 90), Ui.col("Cliente", "clienteId", 110),
                Ui.col("Método", "metodo", 130), Ui.col("Valor", "valor", 120), Ui.col("Recebido", "valorRecebido", 120),
                Ui.col("Troco", "troco", 90), Ui.col("Operador", "operador", 130), Ui.col("Estornado", "estornado", 90));
        Ui.tabela(tabela, cols, 470);
        estado.getStyleClass().add("totais"); estado.setWrapText(true);
        body().getChildren().addAll(filtros, estado, tabela,
                pendencia("Regra: só DINHEIRO move a gaveta. Estorno de REC é operação independente."));
        bAbrir.setOnAction(e -> abrir());
        bReceber.setOnAction(e -> receber());
        bFechar.setOnAction(e -> fechar());
        bZ.setOnAction(e -> fitaZ());
        bEstornar.setOnAction(e -> estornarRec());
        attach();
    }

    private void abrir() {
        FormDialog f = new FormDialog();
        TextField fundo = Ui.campo("0,00"); f.add("Fundo de maneio", fundo);
        if (!f.mostrar("Abrir sessão de caixa")) return;
        try {
            SessaoCaixa s = new ServicoCaixa(sessaoDAO, new MovimentoCaixaDAO(), new PagamentoDAO()).abrir(Sessao.atual().username, f.valor("Fundo de maneio"));
            Ui.info("Sessão aberta", "Sessão #" + s.id + " aberta com fundo " + Ui.moeda(s.fundoManeio) + " MT.");
            atualizarEstado();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void receber() {
        SessaoCaixa s = sessaoAbertaAtual();
        if (s == null) { Ui.aviso("Caixa", "Abra uma sessão primeiro."); return; }
        FormDialog f = new FormDialog();
        TextField cli = Ui.campo("ID do cliente"); f.add("Cliente ID", cli);
        TextField fac = Ui.campo("ID da factura (opcional)"); f.add("Factura ID", fac);
        TextField valor = Ui.campo("0,00"); f.add("Valor", valor);
        ComboBox<String> metodo = new ComboBox<>();
        metodo.getItems().addAll("DINHEIRO", "MPESA", "EMOLA", "MKESH", "POS", "BANCO");
        metodo.setValue("DINHEIRO"); f.add("Método", metodo);
        if (!f.mostrar("Cobrança (REC)")) return;
        try {
            Pagamento p = new Pagamento();
            p.sessaoCaixaId = s.id;
            p.clienteId = parseLong(f.texto("Cliente ID"), null);
            p.facturaId = parseLong(f.texto("Factura ID"), null);
            p.metodo = f.comboText("Método"); p.valor = f.valor("Valor"); p.operador = Sessao.atual().username;
            p = new ServicoCaixa(sessaoDAO, new MovimentoCaixaDAO(), new PagamentoDAO()).receber(p);
            Ui.info("REC gerado", "REC #" + p.recNumero + " · " + Ui.moeda(p.valor) + " MT por " + p.metodo);
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void fechar() {
        SessaoCaixa s = sessaoAbertaAtual();
        if (s == null) { Ui.aviso("Caixa", "Não há sessão aberta."); return; }
        FormDialog f = new FormDialog();
        TextField contado = Ui.campo("0,00"); f.add("Valor contado", contado);
        if (!f.mostrar("Fecho cego")) return;
        try {
            SessaoCaixa fechada = new ServicoCaixa(sessaoDAO, new MovimentoCaixaDAO(), new PagamentoDAO()).fechar(s.id, f.valor("Valor contado"));
            Ui.info("Fecho", "Esperado " + Ui.moeda(fechada.valorEsperado) + " · Contado " + Ui.moeda(fechada.valorContado) + " · Diferença " + Ui.moeda(fechada.diferenca));
            atualizarEstado();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void fitaZ() {
        try {
            SessaoCaixa s = sessaoAbertaAtual();
            if (s == null) { Ui.aviso("Fita Z", "Sem sessão seleccionada."); return; }
            Map<String, BigDecimal> r = new ServicoCaixa(sessaoDAO, new MovimentoCaixaDAO(), new PagamentoDAO()).fitaZ(s.id, Sessao.atual().username);
            StringBuilder sb = new StringBuilder("Fita Z por método:\n");
            r.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(Ui.moeda(v)).append(" MT\n"));
            Ui.info("Fita Z", sb.toString());
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void estornarRec() {
        Pagamento sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione um REC para estornar."); return; }
        if (!Ui.confirmar("Estornar REC", "Estornar o REC #" + sel.recNumero + " de " + Ui.moeda(sel.valor) + " MT?")) return;
        try {
            Pagamento p = new ServicoCaixa(sessaoDAO, new MovimentoCaixaDAO(), new PagamentoDAO()).estornar(sel);
            Ui.info("Estorno", "REC #" + p.recNumero + " estornado. A gaveta/documentos foram revertidos.");
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private SessaoCaixa sessaoAbertaAtual() {
        try { return sessaoDAO.buscarSessaoAberta(Sessao.atual().username); } catch (Exception ex) { return null; }
    }

    private void atualizarEstado() {
        SessaoCaixa s = sessaoAbertaAtual();
        Platform.runLater(() -> estado.setText(s == null ? "Sessão fechada." : "Sessão #" + s.id + " aberta · Fundo " + Ui.moeda(s.fundoManeio) + " MT"));
    }

    private void recarregar() {
        Ui.emSegundoPlano(() -> {
            try { List<Pagamento> lista = new PagamentoDAO().todos(); Platform.runLater(() -> tabela.getItems().setAll(lista)); }
            catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
        });
    }

    private Long parseLong(String s, Long def) {
        try { return s.isBlank() ? def : Long.parseLong(s); } catch (NumberFormatException e) { return def; }
    }

    @Override protected void attach() { atualizarEstado(); recarregar(); }
}
