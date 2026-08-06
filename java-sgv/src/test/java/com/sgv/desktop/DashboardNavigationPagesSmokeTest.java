package com.sgv.desktop;

import com.sgv.entity.User;
import com.sgv.repository.UserRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke runtime das páginas Financeiro / Pagamentos / Despesas:
 * carrega dashboard.fxml com o contexto Spring real (perfil H2) e dispara os botões
 * de navegação, verificando que cada página fica visível de forma independente.
 */
@SpringBootTest
@ActiveProfiles("test")
class DashboardNavigationPagesSmokeTest {

    private static final boolean TOOLKIT_OK;
    static {
        boolean ok = false;
        try {
            Platform.startup(() -> { });
            ok = true;
        } catch (Throwable t) {
            ok = true;
        }
        TOOLKIT_OK = ok;
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    private static Object fxField(Object target, String name) throws Exception {
        java.lang.reflect.Field f = DashboardController.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void navigationSplitsFinanceiroPagamentosDespesasIntoIndependentPages() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(TOOLKIT_OK, "JavaFX toolkit unavailable");

        FXMLLoader loader = new FXMLLoader(DashboardNavigationPagesSmokeTest.class.getResource("/fxml/dashboard.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();

        DashboardController dc = loader.getController();
        User admin = userRepository.findByUsername("admin").orElseThrow();
        dc.setUser(admin);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Button fin = (Button) root.lookup("#navMenuFinanceiro");
                Button pag = (Button) root.lookup("#navMenuPagamentos");
                Button desp = (Button) root.lookup("#navMenuDespesas");
                assertNotNull(fin);
                assertNotNull(pag);
                assertNotNull(desp);

                VBox financeiroPane = (VBox) fxField(dc, "financeiroPane");
                VBox pagamentosPane = (VBox) fxField(dc, "pagamentosPane");
                VBox despesasPane = (VBox) fxField(dc, "despesasPane");
                TableView<?> paymentsTable = (TableView<?>) fxField(dc, "paymentsTable");
                TableView<?> expensesTable = (TableView<?>) fxField(dc, "expensesTable");
                Label finTotalVendasLabel = (Label) fxField(dc, "finTotalVendasLabel");

                fin.fire();
                assertTrue(financeiroPane.isVisible(), "financeiroPane deve ficar visível");
                assertTrue(!pagamentosPane.isVisible(), "pagamentosPane deve estar oculto ao abrir Financeiro");

                pag.fire();
                assertTrue(pagamentosPane.isVisible(), "pagamentosPane deve ficar visível");
                assertTrue(!financeiroPane.isVisible(), "financeiroPane deve ficar oculto ao abrir Pagamentos");
                assertTrue(!despesasPane.isVisible(), "despesasPane deve ficar oculto ao abrir Pagamentos");
                assertNotNull(paymentsTable.getItems(), "paymentsTable deve carregar itens sem exceção");
                assertNotNull(finTotalVendasLabel.getText());
                assertTrue(!finTotalVendasLabel.getText().isBlank(), "KPI Total Vendas deve ser atualizado");

                desp.fire();
                assertTrue(despesasPane.isVisible(), "despesasPane deve ficar visível");
                assertTrue(!pagamentosPane.isVisible(), "pagamentosPane deve ficar oculto ao abrir Despesas");
                assertNotNull(expensesTable.getItems(), "expensesTable deve carregar itens sem exceção");
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(60, TimeUnit.SECONDS), "navegação em timeout na FX thread");
        if (failure.get() != null) {
            throw new AssertionError("Falha no smoke de navegação", failure.get());
        }
    }
}
