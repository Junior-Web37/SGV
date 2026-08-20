package com.sgv.desktop;

import com.sgv.entity.User;
import com.sgv.service.DesktopAuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.sgv.service.SystemLogService;

@Component
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private final DesktopAuthService authService;
    private final ApplicationContext applicationContext;
    private final SystemLogService systemLogService;

    public LoginController(DesktopAuthService authService, ApplicationContext applicationContext, SystemLogService systemLogService) {
        this.authService = authService;
        this.applicationContext = applicationContext;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        log.debug("[LOGIN] LoginController.initialize() chamado");
        errorLabel.setText("");
        UiUtils.attachSafe(loginButton, this::doLogin, systemLogService, "LOGIN_SUBMIT");
        loginButton.setDefaultButton(true);

        usernameField.setOnKeyTyped(e -> errorLabel.setText(""));
        passwordField.setOnKeyTyped(e -> errorLabel.setText(""));
        passwordField.setOnAction(UiUtils.safeOnAction(this::doLogin, systemLogService, "LOGIN_SUBMIT"));
        log.debug("[LOGIN] LoginController.initialize() concluido - botoes e campos ligados");
    }

    private void doLogin() {
        log.debug("[LOGIN] doLogin() iniciado");
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.debug("[LOGIN] Campos vazios, username='{}'", username);
            errorLabel.setText("Preencha os campos");
            return;
        }

        log.debug("[LOGIN] Autenticando utilizador '{}'", username);
        User user = authService.authenticate(username, password);
        if (user == null) {
            log.warn("[LOGIN] Credenciais invalidas para '{}'", username);
            errorLabel.setText("Credenciais inválidas");
            systemLogService.logSecurity(username, "LOGIN_FAILED", "Tentativa de login falhada para o utilizador: " + username);
            return;
        }
        log.info("[LOGIN] Login bem-sucedido para '{}' (id={}, roles={})", username, user.getId(), user.getRoles());
        systemLogService.logUserAction(username, "LOGIN_SUCCESS", "Sessão iniciada com sucesso.");

        try {
            log.debug("[LOGIN] A carregar dashboard.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            log.debug("[LOGIN] dashboard.fxml carregado com sucesso");
            DashboardController dc = loader.getController();
            dc.setUser(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setResizable(true);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
            stage.setTitle("SGV - Sistema de Gestão de Vendas & Facturação");
            try {
                stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icons/app-icon.png")));
            } catch (Exception ignored) {}
            stage.setScene(scene);
            stage.setWidth(1280);
            stage.setHeight(800);
            stage.centerOnScreen();
            stage.setMaximized(true);
            log.info("[LOGIN] Dashboard apresentado com sucesso");
        } catch (Exception ex) {
            log.error("[LOGIN] Erro ao carregar dashboard apos login de '{}'", username, ex);
            systemLogService.logError("LOAD_DASHBOARD", "Erro ao carregar o painel principal após login de '" + username + "'.", ex);
            if (ex.getStackTrace().length > 0) {
                StackTraceElement ste = ex.getStackTrace()[0];
                errorLabel.setText("Linha " + ste.getLineNumber() + " do " + ste.getFileName());
            } else {
                errorLabel.setText("Erro interno");
            }
        }
    }
}
