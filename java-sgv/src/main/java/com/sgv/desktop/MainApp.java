package com.sgv.desktop;

import com.sgv.SgvApplication;
import com.sgv.service.LicenseService;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
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
    private ProgressBar splashProgressBar;
    private Label splashStatusLabel;

    public static ConfigurableApplicationContext getContext() {
        return context;
    }

    @Override
    public void start(Stage primaryStage) {

        // ── Configura handler global de excepções não capturadas ──────────────
        Thread.UncaughtExceptionHandler handler = (t, e) -> {
            if (UiUtils.isHarmlessJavaFxControlBug(e)) {
                log.debug("Bug interno JavaFX ignorado (ComboBox/ListView): {}", e != null ? e.getMessage() : "");
                return;
            }
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter("app-crash.log");
                if (e != null) e.printStackTrace(pw);
                pw.flush(); pw.close();
            } catch (Exception ex) {
                log.error("Falha ao escrever app-crash.log", ex);
            }
            log.error("Exceção não capturada na thread {}: {}", t.getName(), e != null ? e.getMessage() : "null", e);
            try {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Erro crítico: " + (e != null ? e.getMessage() : "Desconhecido"));
                    a.setHeaderText(null);
                    a.showAndWait();
                });
            } catch (Exception ignore) {
                log.warn("Não foi possível mostrar alerta de erro crítico", ignore);
            }
        };
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);

        // Definir localização padrão para Moçambique em toda a aplicação
        Locale.setDefault(Locale.forLanguageTag("pt-MZ"));

        // ── 1. Mostrar o Splash Screen com design 2026 instantaneamente ──────
        Stage splashStage = buildSplashScreen();
        splashStage.show();
        log.info("[APP] Splash Screen futurista apresentado.");

        // ── 2. Criar Task que corre o Spring Boot numa thread separada ─────────
        Task<ConfigurableApplicationContext> bootTask = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() throws Exception {
                updateMessage("A inicializar base de dados MariaDB...");
                updateProgress(0.2, 1.0);
                Thread.sleep(250);

                log.info("[APP] A iniciar contexto Spring Boot na background thread...");
                String[] springArgs = resolveSpringStartupArgs(getParameters().getRaw().toArray(new String[0]));
                
                updateMessage("A carregar migrações Flyway e schemas...");
                updateProgress(0.5, 1.0);
                
                ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SgvApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(springArgs);
                
                updateMessage("A carregar módulos comerciais e regras fiscais...");
                updateProgress(0.85, 1.0);
                Thread.sleep(200);

                updateMessage("Pronto a iniciar.");
                updateProgress(1.0, 1.0);
                log.info("[APP] Contexto Spring Boot iniciado com sucesso.");
                return ctx;
            }
        };

        if (splashProgressBar != null && splashStatusLabel != null) {
            splashProgressBar.progressProperty().bind(bootTask.progressProperty());
            splashStatusLabel.textProperty().bind(bootTask.messageProperty());
        }

        // ── 3. Quando o Spring Boot terminar com sucesso ───────────────────────
        bootTask.setOnSucceeded(event -> {
            log.info("[APP] Background boot concluído — a transitar para login.");
            context = bootTask.getValue();

            try {
                // Verificar licença (já na UI thread)
                checkLicenseStartup();

                // Carregar o login.fxml
                log.info("[APP] A carregar login.fxml...");
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                loader.setControllerFactory(context::getBean);
                Parent root = loader.load();
                log.info("[APP] login.fxml carregado com sucesso.");

                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

                // Transição suave: Splash → Login
                FadeTransition fade = new FadeTransition(Duration.millis(400), splashStage.getScene().getRoot());
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(e -> {
                    splashStage.close();
                    primaryStage.setTitle("SGV - Sistema de Gestão de Vendas & Facturação");
                    try {
                        primaryStage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/app-icon.png")));
                    } catch (Exception ignored) {}
                    primaryStage.setScene(scene);
                    primaryStage.setWidth(440);
                    primaryStage.setHeight(660);
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
     * Constrói o Splash Screen Moderno 2026 com Efeitos Neon, Gradientes Profundos e Animações.
     */
    private Stage buildSplashScreen() {
        Stage splash = new Stage();
        splash.initStyle(StageStyle.UNDECORATED);
        try {
            splash.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/app-icon.png")));
        } catch (Exception ignored) {}

        // 1. EMBLEMA DO LOGÓTIPO COM BRILHO NEON
        StackPane emblem = new StackPane();
        emblem.setPrefSize(72, 72);
        emblem.setMaxSize(72, 72);

        Circle outerRing = new Circle(36);
        outerRing.setFill(Color.TRANSPARENT);
        outerRing.setStroke(Color.web("#3B82F6", 0.4));
        outerRing.setStrokeWidth(2);

        Circle innerCircle = new Circle(28);
        innerCircle.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2563EB")),
                new Stop(1, Color.web("#1D4ED8"))));

        DropShadow glowEffect = new DropShadow();
        glowEffect.setColor(Color.web("#38BDF8", 0.6));
        glowEffect.setRadius(20);
        glowEffect.setSpread(0.2);
        innerCircle.setEffect(glowEffect);

        Label emblemText = new Label("⚡");
        emblemText.setStyle("-fx-font-size:24px; -fx-text-fill:#FFFFFF;");

        emblem.getChildren().addAll(outerRing, innerCircle, emblemText);

        // Animação de pulso contínuo no emblema
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1200), innerCircle);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.06);
        pulse.setToY(1.06);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        // 2. TÍTULO E SUBTÍTULO
        Label logo = new Label("SGV");
        logo.setStyle("-fx-font-size:52px; -fx-font-weight:900; -fx-text-fill:linear-gradient(to right, #FFFFFF, #93C5FD); -fx-font-family:'Segoe UI', sans-serif; -fx-letter-spacing:1px;");

        DropShadow logoShadow = new DropShadow();
        logoShadow.setColor(Color.web("#2563EB", 0.5));
        logoShadow.setRadius(25);
        logo.setEffect(logoShadow);

        Label subtitle = new Label("SISTEMA DE GESTÃO DE VENDAS & FACTURAÇÃO");
        subtitle.setStyle("-fx-font-size:11px; -fx-font-weight:800; -fx-text-fill:#38BDF8; -fx-letter-spacing:2px;");

        VBox titleBox = new VBox(4, logo, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        VBox headerContent = new VBox(14, emblem, titleBox);
        headerContent.setAlignment(Pos.CENTER);

        // 3. BARRA DE PROGRESSO ELEGANTE E STATUS DINÂMICO
        splashProgressBar = new ProgressBar(0.0);
        splashProgressBar.setPrefWidth(420);
        splashProgressBar.setPrefHeight(6);
        splashProgressBar.setStyle(
            "-fx-accent: linear-gradient(to right, #2563EB, #38BDF8, #60A5FA);" +
            "-fx-control-inner-background: #1E293B;" +
            "-fx-background-radius: 4;" +
            "-fx-border-radius: 4;"
        );

        splashStatusLabel = new Label("A carregar componentes do sistema...");
        splashStatusLabel.setStyle("-fx-font-size:12px; -fx-font-weight:600; -fx-text-fill:#94A3B8;");

        VBox progressBox = new VBox(10, splashProgressBar, splashStatusLabel);
        progressBox.setAlignment(Pos.CENTER);

        // 4. RODAPÉ DE CERTIFICAÇÃO E VERSÃO (MOÇAMBIQUE)
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-padding:10 0 0 0;");

        Label badgeMz = new Label("🇲🇿 Moçambique • v2026 Enterprise");
        badgeMz.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:#64748B; -fx-background-color:#1E293B; -fx-padding:3 8; -fx-background-radius:10;");

        Label badgeFiscal = new Label("🔒 Certificação Fiscal AT / CIVA 16%");
        badgeFiscal.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:#38BDF8; -fx-background-color:#1E293B; -fx-padding:3 8; -fx-background-radius:10;");

        footer.getChildren().addAll(badgeMz, badgeFiscal);

        // 5. CONTENTOR PRINCIPAL COM GRADIENTE PROFUNDO E BRILHO AMBIENTE
        VBox root = new VBox(24, headerContent, progressBox, footer);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(36, 40, 24, 40));
        root.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #090E17, #0F172A 50%, #172554);" +
            "-fx-border-color: #2563EB;" +
            "-fx-border-width: 1.5;" +
            "-fx-background-radius: 16;" +
            "-fx-border-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(37, 99, 235, 0.45), 35, 0.2, 0, 10);"
        );

        StackPane wrapper = new StackPane(root);
        wrapper.setStyle("-fx-background-color:transparent; -fx-padding:20;");

        Scene scene = new Scene(wrapper, 540, 390);
        scene.setFill(Color.TRANSPARENT);
        splash.setScene(scene);
        splash.centerOnScreen();

        // Animação de entrada suave do cabeçalho
        FadeTransition ft = new FadeTransition(Duration.millis(600), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(600), root);
        st.setFromX(0.92);
        st.setFromY(0.92);
        st.setToX(1.0);
        st.setToY(1.0);

        ParallelTransition pt = new ParallelTransition(ft, st);
        pt.play();

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
