package com.sgv.desktop;

import com.sgv.entity.Branch;
import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.RoleRepository;
import com.sgv.repository.UserRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;
import com.sgv.service.SystemLogService;

@Component
public class UserFormController {

    @FXML private javafx.scene.layout.VBox rootPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private CheckBox activeCheckbox;
    @FXML private CheckBox forcePasswordCheckbox;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private javafx.scene.control.ProgressIndicator saveSpinner;

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final SystemLogService systemLogService;
    private User user;
    private Runnable onSave;
    @Value("${desktop.useRest:false}")
    private boolean useRest;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // MVVM Data Binding Properties
    private final javafx.beans.property.BooleanProperty formValidProperty = new javafx.beans.property.SimpleBooleanProperty(false);

    public UserFormController(UserRepository userRepository,
                              BranchRepository branchRepository,
                              RoleRepository roleRepository,
                              SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.systemLogService = systemLogService;
    }

    @FXML
    public void initialize() {
        branchCombo.setItems(javafx.collections.FXCollections.observableArrayList(branchRepository.findAll()));
        roleCombo.setItems(javafx.collections.FXCollections.observableArrayList(roleRepository.findAll()));

        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // UX: Transição de entrada fluida (Fade-in)
        if (rootPane != null) {
            rootPane.setOpacity(0.0);
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(350), rootPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        // UX/MVVM: Data-Binding do botão de salvar
        saveButton.disableProperty().bind(formValidProperty.not());

        // Setup real-time listeners
        setupRealTimeValidation();
    }

    private void setupRealTimeValidation() {
        usernameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        fullNameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        roleCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        branchCombo.valueProperty().addListener((obs, o, n) -> validateRealTime());
        passwordField.textProperty().addListener((obs, o, n) -> validateRealTime());
        emailField.textProperty().addListener((obs, o, n) -> validateRealTime());

        javafx.application.Platform.runLater(this::validateRealTime);
    }

    private void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

        // 1. Username: obrigatório (mín. 4 chars, só letras/números)
        String username = usernameField.getText();
        if (username == null || username.isBlank()) {
            valid = false;
        } else if (username.trim().length() < 4) {
            errors.append("Username muito curto (mín. 4 caracteres). ");
            valid = false;
        } else if (!username.matches("\\w+")) {
            errors.append("Username só pode ter letras e números. ");
            valid = false;
        }

        // 2. Nome completo: obrigatório (mín. 3 chars)
        String fullName = fullNameField.getText();
        if (fullName == null || fullName.isBlank()) {
            valid = false;
        } else if (fullName.trim().length() < 3) {
            errors.append("Nome completo muito curto (mín. 3 caracteres). ");
            valid = false;
        }

        // 3. Email: opcional mas se preenchido deve ser válido
        String email = emailField.getText();
        if (email != null && !email.isBlank()) {
            if (!email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                errors.append("Email inválido. ");
                valid = false;
            }
        }

        // 4. Role: obrigatória
        if (roleCombo.getValue() == null) {
            valid = false;
        }

        // 5. Branch: obrigatória
        if (branchCombo.getValue() == null) {
            valid = false;
        }

        // 6. Password: obrigatório só para novo utilizador (mín. 6 chars)
        boolean isNewUser = (user == null || user.getId() == null);
        if (isNewUser) {
            String pwd = passwordField.getText();
            if (pwd == null || pwd.isBlank()) {
                valid = false;
            } else if (pwd.length() < 6) {
                errors.append("Password muito curta (mín. 6 caracteres). ");
                valid = false;
            }
        } else {
            // Para editar: password opcional, mas se preenchida >= 6 chars
            String pwd = passwordField.getText();
            if (pwd != null && !pwd.isBlank() && pwd.length() < 6) {
                errors.append("Password muito curta (mín. 6 caracteres). ");
                valid = false;
            }
        }

        formValidProperty.set(valid);

        if (!valid && errors.length() > 0) {
            showError(errors.toString().trim());
        } else {
            hideError();
        }
    }

    public void setUser(User u) {
        this.user = u;
        if (u != null && u.getId() != null) {
            usernameField.setText(u.getUsername());
            usernameField.setDisable(true);
            fullNameField.setText(u.getFullName());
            emailField.setText(u.getEmail());
            branchCombo.setValue(u.getBranch());
            if (u.getRoles() != null && !u.getRoles().isEmpty()) {
                roleCombo.setValue(u.getRoles().iterator().next());
            }
            activeCheckbox.setSelected(u.isActive());
            forcePasswordCheckbox.setSelected(u.isForceChangePassword());
            passwordField.setPromptText("Deixe vazio para manter a password actual");
        } else {
            this.user = new User();
            activeCheckbox.setSelected(true);
            usernameField.setDisable(false);
            passwordField.setPromptText("Password (mín. 6 caracteres)");
        }
        validateRealTime();
    }

    public void setOnSave(Runnable callback) {
        this.onSave = callback;
    }

    private void doSave() {
        if (!formValidProperty.get()) return;

        saveButton.setVisible(false);
        saveSpinner.setVisible(true);
        saveSpinner.setManaged(true);
        cancelButton.setDisable(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                boolean isNew = user.getId() == null;

                if (isNew) {
                    // Check if username exists
                    if (userRepository.findByUsername(usernameField.getText().trim()).isPresent()) {
                        throw new Exception("Username já existe");
                    }
                    user.setUsername(usernameField.getText().trim());
                    user.setPasswordHash(passwordField.getText()); // In real app, hash this
                }

                user.setFullName(fullNameField.getText().trim());
                user.setEmail(emailField.getText() != null ? emailField.getText().trim() : "");
                user.setBranch(branchCombo.getValue());
                user.setActive(activeCheckbox.isSelected());
                user.setForceChangePassword(forcePasswordCheckbox.isSelected());

                // Only update password if a new one was provided
                String pwd = passwordField.getText();
                if (!isNew && pwd != null && !pwd.isBlank() && pwd.length() >= 6) {
                    user.setPasswordHash(pwd);
                }

                Set<Role> roles = new HashSet<>();
                roles.add(roleCombo.getValue());
                user.setRoles(roles);
                if (useRest) {
                    // POST to REST API /users to create/update
                    try {
                        String url = "http://localhost:8080/api/users" + (isNew ? "" : "/" + user.getId());
                        var payload = new java.util.HashMap<String,Object>();
                        payload.put("username", user.getUsername());
                        payload.put("fullName", user.getFullName());
                        payload.put("email", user.getEmail());
                        payload.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);
                        payload.put("active", user.isActive());
                        payload.put("forceChangePassword", user.isForceChangePassword());
                        payload.put("password", (isNew ? user.getPasswordHash() : null));
                        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                            payload.put("roles", user.getRoles().stream().map(r -> r.getName()).toArray());
                        }
                        String body = objectMapper.writeValueAsString(payload);
                        HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
                        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() >= 400) {
                            throw new Exception("API error: " + resp.statusCode() + " " + resp.body());
                        }
                    } catch (Exception ex) {
                        throw new Exception("Failed REST user save: " + ex.getMessage(), ex);
                    }
                } else {
                    userRepository.save(user);
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (onSave != null) onSave.run();
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        });

        saveTask.setOnFailed(e -> {
            Throwable ex = saveTask.getException();
            systemLogService.logError("USER_SAVE_FAILED", "Erro ao salvar utilizador: " + ex.getMessage(), ex);
            showError("Erro ao guardar: " + (ex.getMessage() != null ? ex.getMessage() : "Erro desconhecido"));

            saveButton.setVisible(true);
            saveSpinner.setVisible(false);
            saveSpinner.setManaged(false);
            cancelButton.setDisable(false);
        });

        new Thread(saveTask).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 4 0;");
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
