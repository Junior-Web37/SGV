package com.billywater.views;

import com.billywater.config.Config;
import com.billywater.config.ConfigBanco;
import com.billywater.config.Licenca;
import com.billywater.servico.ServicoLicenca;
import com.billywater.servico.ServicoSeguranca;
import com.billywater.ui.Sessao;
import com.billywater.ui.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Login do SGF — autentica, valida licença (chave pública) e entra no MainWindow. */
public class LoginView extends VBox {

    public LoginView() {
        getStyleClass().add("view-root");
        setPadding(new Insets(40, 40, 40, 40));
        setSpacing(16);
        setAlignment(Pos.TOP_CENTER);

        Label logo = new Label("💧 BILLY WATER");
        logo.getStyleClass().add("top-titulo");
        Label sub = new Label("Sistema de Gestão de Facturação de Água");
        sub.getStyleClass().add("view-subtitle");

        TextField user = new TextField();
        user.setPromptText("Utilizador");
        user.setPrefWidth(320);
        PasswordField pass = new PasswordField();
        pass.setPromptText("Senha");
        pass.setPrefWidth(320);

        Button b = Ui.primario("Entrar");
        b.setPrefWidth(320);

        Label status = new Label("");
        status.getStyleClass().add("kpi-unidade");
        status.setWrapText(true);

        getChildren().addAll(logo, sub, user, pass, b, status);

        b.setOnAction(e -> {
            // Aplica migrações (teste) / lê config em primeiro arranque
            try {
                Config.load();
                ConfigBanco.applyMigrations();
            } catch (Exception ex) { status.setText("Erro de base: " + ex.getMessage()); return; }
            var u = new ServicoSeguranca().autenticar(user.getText().trim(), pass.getText());
            if (u == null) { status.setText("Credenciais inválidas ou conta bloqueada."); return; }
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
        s.getStylesheets().add(LoginView.class.getResource("/css/billywater.css").toExternalForm());
        return s;
    }
}
