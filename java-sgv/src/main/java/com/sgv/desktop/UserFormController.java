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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;
import com.sgv.service.SystemLogService;

@Component
public class UserFormController extends BaseFormController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Branch> branchCombo;
    @FXML private ComboBox<Role> roleCombo;
    @FXML private CheckBox activeCheckbox;
    @FXML private CheckBox forcePasswordCheckbox;
    @FXML private CheckBox canViewStatsCheck;
    @FXML private TextField commissionField;

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final RoleRepository roleRepository;
    private final SystemLogService systemLogService;
    private final PasswordEncoder passwordEncoder;
    private User user;
    @Value("${desktop.useRest:false}")
    private boolean useRest;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserFormController(UserRepository userRepository,
                              BranchRepository branchRepository,
                              RoleRepository roleRepository,
                              SystemLogService systemLogService,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.roleRepository = roleRepository;
        this.systemLogService = systemLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        branchCombo.setItems(javafx.collections.FXCollections.observableArrayList(branchRepository.findAll()));
        roleCombo.setItems(javafx.collections.FXCollections.observableArrayList(roleRepository.findAll()));

        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "USER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "USER_CANCEL");

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

    @Override
    protected void validateRealTime() {
        StringBuilder errors = new StringBuilder();
        boolean valid = true;

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

        String fullName = fullNameField.getText();
        if (fullName == null || fullName.isBlank()) {
            valid = false;
        } else if (fullName.trim().length() < 3) {
            errors.append("Nome completo muito curto (mín. 3 caracteres). ");
            valid = false;
        }

        String email = emailField.getText();
        if (email != null && !email.isBlank()) {
            if (!email.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                errors.append("Email inválido. ");
                valid = false;
            }
        }

        if (roleCombo.getValue() == null) { valid = false; }
        if (branchCombo.getValue() == null) { valid = false; }

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
            canViewStatsCheck.setSelected(u.isCanViewStats());
            commissionField.setText(String.valueOf(u.getCommissionPercent()));
            passwordField.setPromptText("Deixe vazio para manter a password actual");
        } else {
            this.user = new User();
            activeCheckbox.setSelected(true);
            canViewStatsCheck.setSelected(false);
            commissionField.setText("0");
            usernameField.setDisable(false);
            passwordField.setPromptText("Password (mín. 6 caracteres)");
        }
        validateRealTime();
    }

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;

        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                boolean isNew = user.getId() == null;

                if (isNew) {
                    if (userRepository.findByUsername(usernameField.getText().trim()).isPresent()) {
                        throw new Exception("Username já existe");
                    }
                    user.setUsername(usernameField.getText().trim());
                    user.setPasswordHash(passwordEncoder.encode(passwordField.getText()));
                }

                user.setFullName(fullNameField.getText().trim());
                String emailText = emailField.getText() != null ? emailField.getText().trim() : "";
                user.setEmail(emailText.isEmpty() ? null : emailText);
                user.setBranch(branchCombo.getValue());
                user.setActive(activeCheckbox.isSelected());
                user.setForceChangePassword(forcePasswordCheckbox.isSelected());
                user.setCanViewStats(canViewStatsCheck.isSelected());
                user.setCommissionPercent(parseDoubleSafe(commissionField.getText()));

                String pwd = passwordField.getText();
                if (!isNew && pwd != null && !pwd.isBlank() && pwd.length() >= 6) {
                    user.setPasswordHash(passwordEncoder.encode(pwd));
                }

                Set<Role> roles = new HashSet<>();
                roles.add(roleCombo.getValue());
                user.setRoles(roles);
                if (useRest) {
                    try {
                        String url = "http://localhost:8080/api/users" + (isNew ? "" : "/" + user.getId());
                        var payload = new java.util.HashMap<String,Object>();
                        payload.put("username", user.getUsername());
                        payload.put("fullName", user.getFullName());
                        payload.put("email", user.getEmail());
                        payload.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);
                        payload.put("active", user.isActive());
                        payload.put("forceChangePassword", user.isForceChangePassword());
                        payload.put("canViewStats", user.isCanViewStats());
                        payload.put("commissionPercent", user.getCommissionPercent());
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
            hideSaveSpinner();
        });

        new Thread(saveTask).start();
    }

    private double parseDoubleSafe(String text) {
        if (text == null || text.isBlank()) return 0.0;
        try {
            return Double.parseDouble(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
