package com.sgv.desktop;

import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardFinanceiroKpiTest {

    private static final boolean TOOLKIT_OK;
    static {
        boolean ok = false;
        try {
            Platform.startup(() -> { });
            ok = true;
        } catch (Throwable t) {
            ok = false;
        }
        TOOLKIT_OK = ok;
    }

    @Mock SaleRepository saleRepository;
    @Mock PaymentRepository paymentRepository;

    private GridPane financeiroGridLikeFxml() {
        GridPane grid = new GridPane();
        String[][] cards = {
                {"finTotalVendasLabel", "Total Vendas"},
                {"finTotalRecebidoLabel", "Total Recebido"},
                {"finPendenteLabel", "Pendente"},
                {"finNumPagamentosLabel", "Pagamentos"}
        };
        for (int i = 0; i < cards.length; i++) {
            VBox card = new VBox(4);
            Label lbl = new Label("—");
            lbl.setId(cards[i][0]);
            card.getChildren().add(lbl);
            grid.add(card, i, 0);
        }
        return grid;
    }

    @Test
    void updateFinanceiroKPIsShouldUpdateFxmlStyleLabelsWithoutCrash() {
        Assumptions.assumeTrue(TOOLKIT_OK, "JavaFX toolkit unavailable");
        Sale s = new Sale();
        s.setTotal(1000.0);
        when(saleRepository.findAll()).thenReturn(List.of(s));
        Payment p = new Payment();
        p.setAmount(400.0);
        when(paymentRepository.findAll()).thenReturn(List.of(p));

        DashboardKpiManager kpi = new DashboardKpiManager(saleRepository, null, null, null, null, null,
                null, null, null, null, null, paymentRepository, null, null);
        GridPane grid = financeiroGridLikeFxml();
        kpi.updateFinanceiroKPIs(grid);

        Label vendas = (Label) grid.lookup("#finTotalVendasLabel");
        Label recebido = (Label) grid.lookup("#finTotalRecebidoLabel");
        Label pendente = (Label) grid.lookup("#finPendenteLabel");
        Label count = (Label) grid.lookup("#finNumPagamentosLabel");
        assertNotNull(vendas);
        assertNotNull(recebido);
        assertNotNull(pendente);
        assertNotNull(count);
        assertNotEquals("—", vendas.getText());
        assertNotEquals("—", recebido.getText());
        assertNotEquals("—", pendente.getText());
        assertTrue(vendas.getText().endsWith(" MT"));
        assertTrue(recebido.getText().endsWith(" MT"));
        assertTrue(pendente.getText().endsWith(" MT"));
        assertEquals("1", count.getText());
    }

    @Test
    void dashboardFxmlShouldDefineAllFinanceiroLabelIds() throws Exception {
        String fxml;
        try (InputStream in = DashboardFinanceiroKpiTest.class.getResourceAsStream("/fxml/dashboard.fxml")) {
            assertNotNull(in, "dashboard.fxml not found on classpath");
            fxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(fxml.contains("finTotalVendasLabel"), "FXML must define finTotalVendasLabel");
        assertTrue(fxml.contains("finTotalRecebidoLabel"), "FXML must define finTotalRecebidoLabel");
        assertTrue(fxml.contains("finPendenteLabel"), "FXML must define finPendenteLabel");
        assertTrue(fxml.contains("finNumPagamentosLabel"), "FXML must define finNumPagamentosLabel");
        assertTrue(fxml.contains("property=\"saleCustomerName\""), "paymentsTable must use simple saleCustomerName property");
        assertTrue(!fxml.contains("sale.customerName"), "FXML must not use dotted sale.customerName PropertyValueFactory");
    }

    @Test
    void paymentSaleCustomerNameShouldResolveReadably() {
        Sale s = new Sale();
        s.setCustomerName("Maria");
        Payment p = new Payment();
        p.setSale(s);
        assertEquals("Maria", p.getSaleCustomerName());

        Payment withoutSale = new Payment();
        assertEquals("Consumidor Final", withoutSale.getSaleCustomerName());

        Sale noName = new Sale();
        Payment withNullName = new Payment();
        withNullName.setSale(noName);
        assertEquals("Consumidor Final", withNullName.getSaleCustomerName());
    }
}
