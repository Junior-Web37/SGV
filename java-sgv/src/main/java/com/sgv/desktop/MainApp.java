package com.sgv.desktop;

import com.sgv.SgvApplication;
import com.sgv.service.LicenseService;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.Locale;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
    private static ConfigurableApplicationContext context;

    public static ConfigurableApplicationContext getContext() {
        return context;
    }

    @Override
    public void start(Stage primaryStage) {

        // ── Configura handler global de excepções não capturadas ──────────────
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter("app-crash.log");
                e.printStackTrace(pw);
                pw.flush(); pw.close();
            } catch (Exception ex) {
                log.error("Falha ao escrever app-crash.log", ex);
            }
            log.error("Exceção não capturada na thread {}: {}", t.getName(), e.getMessage(), e);
            try {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Erro crítico: " + e.getMessage());
                    a.setHeaderText(null);
                    a.showAndWait();
                });
            } catch (Exception ignore) {
                log.warn("Não foi possível mostrar alerta de erro crítico (plataforma pode estar encerrada)", ignore);
            }
        });

        // Definir localização padrão para Moçambique em toda a aplicação
        Locale.setDefault(Locale.forLanguageTag("pt-MZ"));

        // ── 1. Mostrar o Splash Screen instantaneamente (apenas JavaFX puro) ──
        Stage splashStage = buildSplashScreen();
        splashStage.show();
        log.info("[APP] Splash Screen apresentado.");

        // ── 2. Criar Task que corre o Spring Boot numa thread separada ─────────
        Task<ConfigurableApplicationContext> bootTask = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() {
                updateMessage("A inicializar base de dados...");
                log.info("[APP] A iniciar contexto Spring Boot na background thread...");
                String[] springArgs = resolveSpringStartupArgs(getParameters().getRaw().toArray(new String[0]));
                ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SgvApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(springArgs);
                log.info("[APP] Contexto Spring Boot iniciado com sucesso.");
                updateMessage("Pronto.");
                return ctx;
            }
        };

        // ── 3. Quando o Spring Boot terminar com sucesso ───────────────────────
        bootTask.setOnSucceeded(event -> {
            log.info("[APP] Background boot concluído — a transitar para login.");
            context = bootTask.getValue();

            try {
                // Verificar licença (já na UI thread, Spring está pronto)
                checkLicenseStartup();

                // Carregar o login.fxml
                log.info("[APP] A carregar login.fxml...");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                loader.setControllerFactory(context::getBean);
                Parent root = loader.load();
                log.info("[APP] login.fxml carregado com sucesso.");

                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

                // Transição com fade: Splash → Login
                FadeTransition fade = new FadeTransition(Duration.millis(350), splashStage.getScene().getRoot());
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(e -> {
                    splashStage.close();
                    primaryStage.setTitle("SGV - Sistema de Gestão de Vendas & Facturação");
                    try {
                        primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/app-icon.png")));
                    } catch (Exception ignored) {}
                    primaryStage.setScene(scene);
                    primaryStage.setWidth(420);
                    primaryStage.setHeight(640);
                    primaryStage.setResizable(false);
                    primaryStage.show();
                    primaryStage.centerOnScreen();
                    log.info("[APP] Janela de login apresentada com sucesso.");
                });
                fade.play();

            } catch (Throwable t) {
                log.error("[APP] Erro ao carregar login.fxml após boot do Spring.", t);
                splashStage.close();
                showFatalError("Erro ao carregar o ecrã de login:\n" + t.getMessage());
            }
        });

        // ── 4. Se o Spring Boot falhar ────────────────────────────────────────
        bootTask.setOnFailed(event -> {
            Throwable ex = bootTask.getException();
            log.error("[APP] Erro fatal ao iniciar contexto Spring Boot.", ex);
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter("app-crash.log");
                if (ex != null) ex.printStackTrace(pw);
                pw.flush(); pw.close();
            } catch (Exception e) {
                log.error("Falha ao escrever app-crash.log", e);
            }
            splashStage.close();

            String friendlyMsg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido";
            Throwable root = ex;
            while (root != null && root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String rootMsg = root != null && root.getMessage() != null ? root.getMessage().toLowerCase() : "";
            if (rootMsg.contains("connection refused") || rootMsg.contains("communications link failure") || rootMsg.contains("communicationsexception") || rootMsg.contains("access denied")) {
                friendlyMsg = "Não foi possível ligar à Base de Dados MariaDB/MySQL na porta 3306.\n\n" +
                              "Como resolver:\n" +
                              "1. Abra o painel de controlo do XAMPP;\n" +
                              "2. Clique no botão 'Start' do módulo MySQL;\n" +
                              "3. Inicie novamente o SGV.";
            }

            showFatalError("Não foi possível iniciar o SGV:\n\n" + friendlyMsg +
                    "\n\n(Consulte o ficheiro app-crash.log para o relatório técnico completo)");
        });

        // ── 5. Lançar a task numa daemon thread ────────────────────────────────
        Thread bootThread = new Thread(bootTask, "sgv-spring-boot-thread");
        bootThread.setDaemon(true);
        bootThread.start();
    }

    /**
     * Constrói o Stage do Splash Screen com design premium.
     * Não depende de qualquer recurso externo — apenas JavaFX puro.
     */
    private Stage buildSplashScreen() {
        Stage splash = new Stage();
        splash.initStyle(StageStyle.UNDECORATED);
        try {
            splash.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/app-icon.png")));
        } catch (Exception ignored) {}

        // Logo/título
        Label logo = new Label("SGV");
        logo.setStyle("-fx-font-size:56px; -fx-font-weight:800; -fx-text-fill:#2563EB;");

        Label subtitle = new Label("Sistema de Gestão de Vendas & Facturação — Moçambique");
        subtitle.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#94A3B8;");

        // Indicador de progresso
        ProgressIndicator progress = new ProgressIndicator();
        progress.setStyle("-fx-progress-color:#2563EB;");
        progress.setPrefSize(36, 36);

        Label status = new Label("A inicializar...");
        status.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B;");

        VBox center = new VBox(12, logo, subtitle);
        center.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(8, progress, status);
        bottom.setAlignment(Pos.CENTER);

        VBox root = new VBox(40, center, bottom);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60, 80, 50, 80));
        root.setStyle(
            "-fx-background-color:#0F172A;" +
            "-fx-border-color:#1E293B;" +
            "-fx-border-width:1;" +
            "-fx-background-radius:12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 30, 0.3, 0, 8);"
        );

        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color:transparent;");

        Scene scene = new Scene(wrapper, 400, 320);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        splash.setScene(scene);
        splash.centerOnScreen();
        return splash;
    }

    /**
     * Mostra um alerta de erro fatal e encerra a aplicação.
     */
    private void showFatalError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro Fatal — SGV");
        alert.setHeaderText("Não foi possível iniciar o SGV");
        alert.setContentText(message);
        alert.showAndWait();
        Platform.exit();
    }

    /**
     * Verifica o estado da licença na arranque.
     * Se não houver licença ou estiver expirada, mostra aviso mas permite continuar (modo avaliação).
     */
    private void checkLicenseStartup() {
        try {
            LicenseService licenseService = context.getBean(LicenseService.class);
            LicenseService.LicenseResult result = licenseService.getCurrentLicenseStatus();

            if (!result.isValid()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Licença");
                    alert.setHeaderText(null);
                    alert.setContentText(
                        "Licença: " + result.getMessage() + "\n\n" +
                        "A aplicação está em modo de avaliação.\n" +
                        "Para activar uma licença, aceda a: Sistema > Licenciamento.\n\n" +
                        "Contacte-nos para obter uma licença comercial.");
                    alert.showAndWait();
                });
            } else if (result.getDaysRemaining() != Integer.MAX_VALUE && result.getDaysRemaining() <= 7) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Licença");
                    alert.setHeaderText(null);
                    alert.setContentText(
                        "A sua licença expira em " + result.getDaysRemaining() + " dia(s).\n" +
                        "Tipo: " + result.getType() + "\n" +
                        "Expira: " + result.getExpiryDate());
                    alert.showAndWait();
                });
            }
        } catch (Exception e) {
            log.warn("Erro ao verificar licença no arranque: {}", e.getMessage(), e);
        }
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.close();
        }
        super.stop();
    }

    static String[] resolveSpringStartupArgs(String[] rawArgs) {
        if (rawArgs == null || rawArgs.length == 0) {
            return new String[]{"--spring.profiles.active=mysql"};
        }

        boolean hasExplicitProfile = Arrays.stream(rawArgs)
                .anyMatch(arg -> arg.startsWith("--spring.profiles.active=")
                        || arg.startsWith("spring.profiles.active=")
                        || arg.startsWith("-Dspring.profiles.active="));

        if (hasExplicitProfile) {
            return rawArgs;
        }

        String[] normalized = Arrays.copyOf(rawArgs, rawArgs.length + 1);
        normalized[rawArgs.length] = "--spring.profiles.active=mysql";
        return normalized;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
