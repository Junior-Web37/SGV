package com.billywater.views;

import com.billywater.dao.ClienteDAO;
import com.billywater.dao.EscalaoTarifaDAO;
import com.billywater.dao.FacturaDAO;
import com.billywater.dao.LinhaFacturaDAO;
import com.billywater.dao.ParametroDAO;
import com.billywater.dao.TarifaDAO;
import com.billywater.domain.Cliente;
import com.billywater.domain.Factura;
import com.billywater.domain.LinhaFactura;
import com.billywater.domain.Parametro;
import com.billywater.servico.MotorTarifario;
import com.billywater.servico.ServicoFacturacao;
import com.billywater.ui.ComboItem;
import com.billywater.ui.FormDialog;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Facturação de água (FT) — emissão real com tarifário e anulação. */
public class FacturacaoView extends BaseView {

    private final ComboBox<String> periodo = new ComboBox<>();
    private final TableView<Factura> tabela = new TableView<>();
    private final Label totais = new Label("");

    public FacturacaoView() {
        super("Facturação", "Emissão e consulta de facturas de água (FT)");
        for (int i = 0; i < 12; i++) periodo.getItems().add(YearMonth.now().minusMonths(i).toString());
        periodo.setValue(YearMonth.now().toString());
        HBox filtros = new HBox(10);
        Button bGerar = Ui.primario("Gerar lote");
        Button bEmissao = Ui.primario("Nova FT");
        Button bNC = Ui.perigo("Anular (NC)");
        filtros.getChildren().addAll(periodo, bGerar, bEmissao, bNC);
        List<TableColumn<Factura, ?>> cols = List.of(
                Ui.col("Série", "serie", 70), Ui.col("Nº", "numero", 60), Ui.col("Período", "periodo", 90),
                Ui.col("Consumo", "consumo", 90), Ui.col("Água", "valorAgua", 110), Ui.col("IVA", "iva", 90),
                Ui.col("Total", "total", 120), Ui.col("Estado", "estado", 100));
        Ui.tabela(tabela, cols, 460);
        totais.getStyleClass().add("totais"); totais.setWrapText(true);
        body().getChildren().addAll(filtros, totais, tabela);
        bGerar.setOnAction(e -> gerarLote());
        bEmissao.setOnAction(e -> novaFT());
        bNC.setOnAction(e -> anular());
        periodo.setOnAction(e -> carregar());
        attach();
    }

    private void carregar() {
        try {
            List<Factura> lista = new FacturaDAO().buscarPorPeriodo(periodo.getValue());
            BigDecimal tot = lista.stream().map(f -> f.total).reduce(BigDecimal.ZERO, BigDecimal::add);
            Platform.runLater(() -> { tabela.getItems().setAll(lista); totais.setText("Período " + periodo.getValue() + " — " + lista.size() + " factura(s) · Total " + Ui.moeda(tot) + " MT"); });
        } catch (Exception ex) { Platform.runLater(() -> totais.setText("Erro: " + ex.getMessage())); }
    }

    private void novaFT() {
        FormDialog f = new FormDialog();
        try {
            var clientes = new ClienteDAO().pesquisar("%");
            ComboBox<ComboItem> cli = Ui.combo(clientes.stream().map(c -> new ComboItem(c.id, c.nomeCompleto)).toList());
            f.add("Cliente", cli);
            TextField cons = Ui.campo("0 (m³)"); f.add("Consumo (m³)", cons);
            if (!f.mostrar("Nova Factura de Água")) return;
            Long clienteId = f.comboId("Cliente");
            BigDecimal consumo = f.valor("Consumo (m³)");
            Cliente cliente = new ClienteDAO().buscarPorId(clienteId);
            var tarifa = new TarifaDAO().buscarDefault();
            var escaloes = new EscalaoTarifaDAO().buscarPorTarifa(tarifa.id);
            var r = MotorTarifario.calcular(consumo, tarifa, escaloes, ivaGeral());
            StringBuilder sb = new StringBuilder();
            sb.append("Consumo facturado: ").append(r.consumo).append(" m³\n");
            sb.append("Valor da água: ").append(Ui.moeda(r.valorAgua)).append("\n");
            sb.append("Taxa de disponibilidade: ").append(Ui.moeda(r.taxaDisponibilidade)).append("\n");
            sb.append("Saneamento: ").append(Ui.moeda(r.saneamento)).append("\n");
            sb.append("Subtotal: ").append(Ui.moeda(r.subtotal)).append("\n");
            sb.append("IVA: ").append(Ui.moeda(r.iva)).append("\n");
            sb.append("TOTAL: ").append(Ui.moeda(r.total)).append(" MT");
            if (!Ui.confirmar("Pré-visualização", sb.toString())) return;
            Factura ft = new Factura();
            ft.tipoDocumento = "FT"; ft.serie = "FT"; ft.clienteId = clienteId;
            ft.contadorId = cliente.contadorId; ft.contratoId = cliente.contratoId;
            ft.periodo = periodo.getValue(); ft.criadoEm = LocalDateTime.now();
            ft.consumo = r.consumo; ft.valorAgua = r.valorAgua; ft.taxaDisponibilidade = r.taxaDisponibilidade;
            ft.saneamento = r.saneamento; ft.subtotal = r.subtotal; ft.iva = r.iva; ft.total = r.total; ft.limitePagamento = r.total;
            ft.estado = "EMITIDA";
            FacturaDAO dao = new FacturaDAO();
            ft.numero = dao.proximoNumero("FT", periodo.getValue());
            ft.id = dao.inserir(ft);
            for (LinhaFactura lf : r.linhas) { lf.facturaId = ft.id; new LinhaFacturaDAO().inserir(lf); }
            Ui.info("FT emitida", "Factura FT " + ft.numero + " emitida. Total " + Ui.moeda(ft.total) + " MT.");
            carregar();
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void gerarLote() {
        try {
            int n = new ServicoFacturacao(new FacturaDAO(), new TarifaDAO(), new EscalaoTarifaDAO()).emitirLote(periodo.getValue());
            Platform.runLater(() -> { totais.setText("Lote " + periodo.getValue() + ": " + n + " facturas geradas."); carregar(); });
        } catch (Exception ex) { Platform.runLater(() -> totais.setText("Erro: " + ex.getMessage())); }
    }

    private void anular() {
        Factura sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { Ui.aviso("Selecção", "Seleccione uma factura."); return; }
        if (!Ui.confirmar("Anular FT", "Anular a factura " + sel.serie + sel.numero + "?")) return;
        try { new ServicoFacturacao(new FacturaDAO(), new TarifaDAO(), new EscalaoTarifaDAO()).anular(sel, "Anulação manual"); carregar(); }
        catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private BigDecimal ivaGeral() {
        try { Parametro p = new ParametroDAO().unico(); return p != null && p.ivaGeral != null ? p.ivaGeral : BigDecimal.valueOf(16); }
        catch (Exception ex) { return BigDecimal.valueOf(16); }
    }

    @Override protected void attach() { carregar(); }
}
