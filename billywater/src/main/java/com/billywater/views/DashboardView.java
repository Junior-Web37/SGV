package com.billywater.views;

import com.billywater.dao.FacturaDAO;
import com.billywater.dao.PagamentoDAO;
import com.billywater.dao.VendaDAO;
import com.billywater.ui.Ui;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Painel (Dashboard) — KPIs com consultas assíncronas. */
public class DashboardView extends BaseView {

    private final Label facturado = new Label("…");
    private final Label cobrado = new Label("…");
    private final Label divida = new Label("…");
    private final Label vendasLoja = new Label("…");
    private final Label clientes = new Label("…");
    private final Label estado = new Label("");

    public DashboardView() {
        super("Painel", "Visão geral — facturação, cobrança, dívida e loja");
        HBox kpis = new HBox(16);
        kpis.getChildren().addAll(
                kpi("Facturado de Água do Período", facturado),
                kpi("Cobrado no Mês", cobrado),
                kpi("Dívida Total", divida),
                kpi("Vendas da Loja", vendasLoja),
                kpi("Clientes", clientes));
        Button bFT = Ui.primario("Fazer Factura (lote FT)");
        bFT.setOnAction(e -> Ui.info("Facturação", "Abra o módulo Facturação no menu Água ▾ para emitir o lote FT."));
        estado.getStyleClass().add("totais");
        estado.setWrapText(true);
        body().getChildren().addAll(kpis, bFT, estado);
        attach();
    }

    private VBox kpi(String titulo, Label valor) {
        VBox v = new VBox(4);
        v.getStyleClass().add("card-pane");
        Label t = new Label(titulo);
        t.getStyleClass().add("kpi-unidade");
        valor.getStyleClass().add("kpi-valor");
        v.getChildren().addAll(t, valor);
        return v;
    }

    @Override
    protected void attach() {
        String periodo = YearMonth.now().toString();
        Ui.emSegundoPlano(() -> {
            try {
                BigDecimal fact = new FacturaDAO().buscarPorPeriodo(periodo).stream().map(f -> f.total).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal dividaTotal = new FacturaDAO().contasEmAberto().stream().map(f -> f.total).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal cobradoMes = new PagamentoDAO().totalPeriodo(periodo);
                BigDecimal vendaLoja = new VendaDAO().hoje().stream().map(v -> v.total).reduce(BigDecimal.ZERO, BigDecimal::add);
                long nClientes = new com.billywater.dao.ClienteDAO().contar();
                Platform.runLater(() -> {
                    facturado.setText(Ui.moeda(fact) + " MT");
                    cobrado.setText(Ui.moeda(cobradoMes) + " MT");
                    divida.setText(Ui.moeda(dividaTotal) + " MT");
                    vendasLoja.setText(Ui.moeda(vendaLoja) + " MT");
                    clientes.setText(String.valueOf(nClientes));
                    estado.setText("Dados de " + periodo);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage()));
            }
        });
    }
}
