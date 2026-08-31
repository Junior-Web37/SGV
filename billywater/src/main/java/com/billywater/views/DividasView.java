package com.billywater.views;

import com.billywater.dao.FacturaDAO;
import com.billywater.dao.PrestacaoDAO;
import com.billywater.domain.Factura;
import com.billywater.domain.Prestacao;
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

/** Dívidas / contas correntes — aging completo + plano de prestações. */
public class DividasView extends BaseView {

    private final TableView<Factura> tabela = new TableView<>();
    private final Label estado = new Label("");
    private final Label aging = new Label("");

    public DividasView() {
        super("Dívidas", "Contas correntes: facturas em aberto, aging e planos de pagamento");
        HBox filtros = new HBox(10);
        Button bCliente = Ui.primario("Ver dívidas do cliente");
        Button bPlano = Ui.primario("Criar plano de prestações");
        filtros.getChildren().addAll(bCliente, bPlano);
        List<TableColumn<Factura, ?>> cols = List.of(
                Ui.col("Série", "serie", 60), Ui.col("Nº", "numero", 60), Ui.col("Período", "periodo", 90),
                Ui.col("Cliente", "clienteId", 90), Ui.col("Total", "total", 120), Ui.col("Estado", "estado", 100),
                Ui.col("Limite", "limitePagamento", 110));
        Ui.tabela(tabela, cols, 480);
        estado.getStyleClass().add("totais"); estado.setWrapText(true);
        aging.getStyleClass().add("totais"); aging.setWrapText(true);
        body().getChildren().addAll(filtros, tabela, aging, estado);
        bCliente.setOnAction(e -> clienteActivo());
        bPlano.setOnAction(e -> criarPlano());
        attach();
    }

    private void clienteActivo() {
        FormDialog f = new FormDialog();
        f.add("Cliente ID", Ui.campo("ID do cliente"));
        if (!f.mostrar("Dívidas do cliente")) return;
        try {
            Long id = Long.parseLong(f.texto("Cliente ID"));
            List<Factura> lista = new FacturaDAO().buscarPorCliente(id).stream().filter(x -> !"PAGO".equalsIgnoreCase(x.estado)).toList();
            Platform.runLater(() -> { tabela.getItems().setAll(lista); aging.setText(""); estado.setText(lista.size() + " factura(s) em aberto para o cliente " + id); });
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private void criarPlano() {
        FormDialog f = new FormDialog();
        TextField clienteId = Ui.campo("ID do cliente"); f.add("Cliente ID", clienteId);
        TextField n = Ui.campo("Nº de prestações"); f.add("Nº prestações", n);
        TextField dias = Ui.campo("30"); f.add("Dias entre prestações", dias);
        if (!f.mostrar("Criar plano")) return;
        try {
            Long cid = Long.parseLong(f.texto("Cliente ID"));
            int nr = Integer.parseInt(f.texto("Nº prestações"));
            int diasEntre = Integer.parseInt(f.texto("Dias entre prestações"));
            BigDecimal totalEmAberto = dividaTotal(cid);
            if (totalEmAberto.signum() <= 0) { Ui.aviso("Plano", "Cliente sem dívida em aberto."); return; }
            BigDecimal valor = totalEmAberto.divide(BigDecimal.valueOf(nr), 2, java.math.RoundingMode.HALF_UP);
            Long planoId = java.lang.System.currentTimeMillis();
            java.time.LocalDate venc = java.time.LocalDate.now();
            for (int i = 1; i <= nr; i++) {
                Prestacao p = new Prestacao();
                p.planoId = planoId; p.numero = i; p.valor = (i == nr ? totalEmAberto.subtract(valor.multiply(BigDecimal.valueOf(nr - 1))) : valor);
                p.vencimento = venc.plusDays((long) i * diasEntre); p.estado = "PENDENTE";
                new PrestacaoDAO().salvar(p);
            }
            Ui.info("Plano criado", "Plano de " + nr + " prestações de " + Ui.moeda(valor) + " MT criado.");
        } catch (Exception ex) { Ui.erro("Erro", ex.getMessage()); }
    }

    private BigDecimal dividaTotal(Long clienteId) throws Exception {
        return new FacturaDAO().buscarPorCliente(clienteId).stream()
                .filter(x -> !"PAGO".equals(x.estado)).map(f -> f.total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void recarregarAging() {
        try {
            List<Factura> lista = new FacturaDAO().contasEmAberto();
            Platform.runLater(() -> {
                tabela.getItems().setAll(lista);
                BigDecimal[] f = envelhecer(lista);
                aging.setText(String.format("Aging (dias de dívida): A vencer %s · 1–30 %s · 31–60 %s · 61–90 %s · >90 %s  —  Total %s",
                        Ui.moeda(f[0]), Ui.moeda(f[1]), Ui.moeda(f[2]), Ui.moeda(f[3]), Ui.moeda(f[4]), Ui.moeda(f[5])));
                estado.setText(lista.size() + " factura(s) em aberto");
            });
        } catch (Exception ex) { Platform.runLater(() -> estado.setText("Erro: " + ex.getMessage())); }
    }

    private BigDecimal[] envelhecer(List<Factura> lista) {
        BigDecimal[] f = new BigDecimal[6];
        for (int i = 0; i < 6; i++) f[i] = BigDecimal.ZERO;
        java.time.LocalDate hoje = java.time.LocalDate.now();
        for (Factura x : lista) {
            long dias = 0;
            if (x.criadoEm != null) dias = java.time.Duration.between(x.criadoEm.toLocalDate().atStartOfDay(), hoje.atStartOfDay()).toDays();
            BigDecimal t = x.total == null ? BigDecimal.ZERO : x.total;
            if (dias <= 0) f[0] = f[0].add(t);
            else if (dias <= 30) f[1] = f[1].add(t);
            else if (dias <= 60) f[2] = f[2].add(t);
            else if (dias <= 90) f[3] = f[3].add(t);
            else f[4] = f[4].add(t);
            f[5] = f[5].add(t);
        }
        return f;
    }

    @Override protected void attach() { recarregarAging(); }
}
