package com.billywater;

import com.billywater.config.Config;
import com.billywater.config.ConfigBanco;
import com.billywater.ui.Sessao;
import com.billywater.views.LoginView;
import com.billywater.views.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Arranque do SGF (BILLY WATER) — JavaFX. */
public class BillyWaterApp extends Application {

    private static Stage primaryStage;

    public static Stage getStage() { return primaryStage; }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        try { Config.load(); ConfigBanco.applyMigrations(); } catch (Exception e) {
            // base não disponível — the login screen reports it
        }
        Scene s = new Scene(new LoginView());
        s.getStylesheets().add(getClass().getResource("/css/billywater.css").toExternalForm());
        stage.setTitle("BILLY WATER — SGF");
        stage.setScene(s);
        stage.setWidth(440);
        stage.setHeight(660);
        stage.setResizable(false);
        stage.show();
    }

    public static void mostrarPrincipal() {
        Scene s = new Scene(new MainWindow());
        s.getStylesheets().add(BillyWaterApp.class.getResource("/css/billywater.css").toExternalForm());
        primaryStage.setScene(s);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
    }

    public static void voltarLogin() {
        Sessao.sair();
        com.billywater.ui.Permissoes.limpar();
        primaryStage.setScene(LoginView.cena());
        primaryStage.setWidth(440);
        primaryStage.setHeight(660);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
