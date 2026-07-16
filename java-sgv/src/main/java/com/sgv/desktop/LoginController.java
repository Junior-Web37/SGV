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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.sgv.service.SystemLogService;

@Component
public class LoginController {

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
        errorLabel.setText("");
        loginButton.setOnAction(e -> doLogin());
        loginButton.setDefaultButton(true);

        usernameField.setOnKeyTyped(e -> errorLabel.setText(""));
        passwordField.setOnKeyTyped(e -> errorLabel.setText(""));
        passwordField.setOnAction(e -> doLogin());
    }

    private void doLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            errorLabel.setText("Preencha os campos");
            return;
        }

        User user = authService.authenticate(username, password);
        if (user == null) {
            errorLabel.setText("Credenciais inválidas");
            systemLogService.logSecurity(username, "LOGIN_FAILED", "Tentativa de login falhada para o utilizador: " + username);
            return;
        }
        systemLogService.logUserAction(username, "LOGIN_SUCCESS", "Sessão iniciada com sucesso.");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            DashboardController dc = loader.getController();
            dc.setUser(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/dashboard.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception ex) {
            ex.printStackTrace();
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
