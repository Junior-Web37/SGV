package com.billywater.views;

import com.billywater.dao.FacturaDAO;
import com.billywater.dao.ProducaoDAO;
import com.billywater.domain.Producao;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Rede — produção/captação e cálculo de NRW (Água Não Facturada). */
public class RedeView extends BaseView {

    private final TableView<Producao> tabela = new TableView<>();
    private final Label estado = new Label("");

    public RedeView() {
        super("Rede / NRW", "Produção de água e perdas (Água Não Facturada)");
        HBox filtros = new HBox(10);
        Button bNovo = Ui.primario("Registar produção");
        Button bCalc = Ui.primario("Calcular NRW");
        Button bActualizar = Ui.fantasma("Actualizar facturado");
        filtros.getChildren().addAll(bNovo, bCalc, bActualizar);
        List<TableColumn<Producao, ?>> cols = List.of(
                Ui.col("Período", "periodo", 90), Ui.col("Data", "data", 110),
                Ui.col("Produzido (m³)", "volumeProduzido", 130), Ui.col("Facturado (m³)", "volumeFacturado", 130),
                Ui.col("Perdas (m³)", "perdas", 120), Ui.col("NRW (%)", "nrwPercent", 110));
        Ui.tabela(tabela, cols, 440);
        estado.getStyleClass().add("totais"); estado.setWrapText(true);
        body().getChildren().addAll(filtros, tabela, estado,
                pendencia("NRW = (produção − facturada) / produção. Dados de produção são operacionais; reportável."));
        bNovo.setOnAction(e -> novo());
        bCalc.setOnAction(e -> calcular());
        bActualizar.setOnAction(e -> actualizarFacturado());
        attach();
    }

    private void novo() {
        FormDialog f = new FormDialog();
        f.add("Volume produzido", Ui.campo("m³")); f.add("Fonte", Ui.campo("Captação/furo"));
        if (!f.mostrar("Registar produção")) return;
        try {
            Producao p = new Producao();
            p.data = LocalDate.now(); p.periodo = YearMonth.now().toString();
            p.volumeProduzido = f.valor("Volume produzido"); p.fonte = f.texto("Fonte");
            p.volumeFacturado = BigDecimal.ZERO; p.perdas = BigDecimal.ZERO; p.nrwPercent = BigDecimal.ZERO;
            new ProducaoDAO().salvar(p); recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void actualizarFacturado() {
        try {
            Producao sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { Ui.aviso("Selecção", "Seleccione uma produção."); return; }
            BigDecimal facturado = new FacturaDAO().buscarPorPeriodo(sel.periodo).stream()
                    .filter(f -> "FT".equalsIgnoreCase(f.tipoDocumento))
                    .map(f -> f.consumo == null ? BigDecimal.ZERO : f.consumo).reduce(BigDecimal.ZERO, BigDecimal::add);
            sel.volumeFacturado = facturado;
            calcularLinha(sel);
            new ProducaoDAO().salvar(sel);
            recarregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void calcular() {
        try { for (Producao p : tabela.getItems()) { calcularLinha(p); new ProducaoDAO().salvar(p); } recarregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void calcularLinha(Producao p) {
        BigDecimal produ = p.volumeProduzido == null ? BigDecimal.ZERO : p.volumeProduzido;
        BigDecimal fact = p.volumeFacturado == null ? BigDecimal.ZERO : p.volumeFacturado;
        p.perdas = produ.subtract(fact).max(BigDecimal.ZERO);
        p.nrwPercent = produ.signum() == 0 ? BigDecimal.ZERO : p.perdas.multiply(BigDecimal.valueOf(100)).divide(produ, 2, RoundingMode.HALF_UP);
    }

    private void recarregar() {
        Ui.emSegundoPlano(() -> {
            try { List<Producao> lista = new ProducaoDAO().porPeriodo(YearMonth.now().toString());
                Platform.runLater(() -> { tabela.getItems().setAll(lista); estado.setText(lista.size() + " lançamento(s) deste mês"); }); }
            catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
        });
    }

    @Override protected void attach() { recarregar(); }
}
