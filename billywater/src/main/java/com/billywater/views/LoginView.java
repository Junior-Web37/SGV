package com.billywater.views;

import com.billywater.config.Config;
import com.billywater.config.ConfigBanco;
import com.billywater.servico.ServicoSeguranca;
import com.billywater.servico.ServicoLicenca;
import com.billywater.ui.Sessao;
import com.billywater.ui.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Login do SGF — autentica, valida licença (chave pública) e entra no MainWindow.
 * Visual portado do SGV: fundo em gradiente royal blue com caixa de login branca.
 */
public class LoginView extends StackPane {

    public LoginView() {
        getStyleClass().add("login-root");
        setAlignment(Pos.CENTER);

        // ── Caixa de login branca ─────────────────────────────
        VBox box = new VBox(14);
        box.getStyleClass().add("login-box");
        box.setMaxWidth(420);
        box.setMaxHeight(USE_PREF_SIZE);
        box.setPadding(new Insets(36, 36, 36, 36));
        box.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("💧 BILLY WATER");
        logo.getStyleClass().add("top-titulo");
        logo.setStyle("-fx-text-fill: #1D4ED8; -fx-font-size: 22px; -fx-font-weight: 900;");

        Label sub = new Label("Sistema de Gestão de Facturação de Água");
        sub.getStyleClass().add("view-subtitle");
        sub.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 700; -fx-wrap-text: true;");
        sub.setMaxWidth(340);

        HBox userRow = new HBox();
        TextField user = new TextField();
        user.setPromptText("Utilizador");
        user.setPrefWidth(320);
        user.setStyle("-fx-pref-width: 320px;");
        userRow.getChildren().add(user);

        HBox passRow = new HBox();
        PasswordField pass = new PasswordField();
        pass.setPromptText("Senha");
        pass.setPrefWidth(320);
        pass.setStyle("-fx-pref-width: 320px;");
        passRow.getChildren().add(pass);

        Button b = Ui.primario("Entrar");
        b.setPrefWidth(320);
        b.setMaxWidth(Double.MAX_VALUE);

        Label status = new Label("");
        status.getStyleClass().add("kpi-unidade");
        status.setWrapText(true);
        status.setMaxWidth(340);

        box.getChildren().addAll(logo, sub, userRow, passRow, b, status);
        getChildren().add(box);

        b.setOnAction(e -> {
            try {
                Config.load();
                ConfigBanco.applyMigrations();
            } catch (Exception ex) {
                status.setText("Erro de base: " + ex.getMessage());
                return;
            }
            var u = new ServicoSeguranca().autenticar(user.getText().trim(), pass.getText());
            if (u == null) {
                status.setText("Credenciais inválidas ou conta bloqueada.");
                return;
            }
            // Licença: sem licença entra em modo DEMONSTRAÇÃO (não bloqueia)
            if (!ServicoLicenca.autorizada()) {
                Sessao.setModoTreino(true);
                Ui.aviso("Modo demonstração", "Não há licença válida.\nO BILLY WATER vai correr em modo DEMONSTRAÇÃO.\nPara produção active uma licença em Configurações → Licença.");
            }
            Sessao.entrar(u, u.perfil);
            com.billywater.BillyWaterApp.mostrarPrincipal();
        });
    }

    public static Scene cena() {
        Scene s = new Scene(new LoginView());
        try {
            var css = LoginView.class.getResource("/css/billywater.css");
            if (css != null) s.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}
        return s;
    }
}
