package com.billywater.views;

import com.billywater.dao.LeituraDAO;
import com.billywater.domain.Leitura;
import com.billywater.servico.ServicoLeituras;
import com.billywater.ui.ComboItem;
import com.billywater.ui.FormDialog;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.time.YearMonth;
import java.util.List;

/** Leituras — folha de leitura e validação AURA. */
public class LeiturasView extends BaseView {

    private final TableView<Leitura> tabela = new TableView<>();
    private final Label estado = new Label("");
    private final ComboBox<String> periodo = new ComboBox<>();

    public LeiturasView() {
        super("Leituras", "Folha de leituras e validação (critérios AURA)");
        for (int i = 0; i < 12; i++) periodo.getItems().add(YearMonth.now().minusMonths(i).toString());
        periodo.setValue(YearMonth.now().toString());
        HBox filtros = new HBox(10);
        filtros.getChildren().addAll(periodo, Ui.primario("Nova leitura"), Ui.fantasma("Validar"));
        List<TableColumn<Leitura, ?>> cols = List.of(
                Ui.col("Período", "periodo", 90), Ui.col("Contador", "contadorId", 90),
                Ui.col("Anterior", "leituraAnterior", 110), Ui.col("Actual", "leituraActual", 110),
                Ui.col("Consumo", "consumo", 100), Ui.col("Estado", "estado", 110),
                Ui.col("Anomalia", "anomalia", 260));
        Ui.tabela(tabela, cols, 460);
        estado.getStyleClass().add("totais");
        estado.setWrapText(true);
        body().getChildren().addAll(filtros, tabela, estado,
                pendencia("Critérios AURA: leitura<anterior, consumo zero, >=3x média; estimativa se contador avariado."));
        Ui.acao(filtros.getChildren().get(1), this::nova);
        Ui.acao(filtros.getChildren().get(2), this::validar);
        periodo.setOnAction(e -> recarregar());
        attach();
    }

    private void nova() {
        FormDialog f = new FormDialog();
        f.add("Contador ID", Ui.campo("ID"));
        f.add("Leitura anterior", Ui.campo("0"));
        f.add("Leitura actual", Ui.campo("0"));
        if (!f.mostrar("Nova leitura")) return;
        try {
            Leitura l = new Leitura();
            l.contadorId = Long.parseLong(f.texto("Contador ID"));
            l.periodo = periodo.getValue();
            l.leituraAnterior = f.valor("Leitura anterior");
            l.leituraActual = f.valor("Leitura actual");
            l.tipo = "MEDIDA"; l.estado = "SUBMETIDA"; l.estimada = false;
            l.dataLeitura = java.time.LocalDate.now();
            new ServicoLeituras(new LeituraDAO()).validar(l);
            LeituraDAO dao = new LeituraDAO();
            // resolver contrato via contador
            var contrato = new com.billywater.dao.ContratoDAO().porContador(l.contadorId);
            if (contrato != null) { l.contratoId = contrato.id; l.clienteId = contrato.clienteId; l.rotaId = contrato.rotaId; }
            dao.inserir(l);
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void validar() {
        Leitura sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione uma leitura."); return; }
        try { sel.estado = "VALIDADA"; new LeituraDAO().actualizar(sel); recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void recarregar() {
        try { List<Leitura> lista = new LeituraDAO().porPeriodo(periodo.getValue());
            Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " leitura(s) em " + periodo.getValue()); }); }
        catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    @Override protected void attach() { recarregar(); }
}
