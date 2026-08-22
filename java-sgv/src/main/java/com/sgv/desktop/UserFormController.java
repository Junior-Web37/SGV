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
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.HashSet;
import java.util.Set;
import javafx.concurrent.Task;
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
    private Task<Boolean> duplicateCheckTask;

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
        UiUtils.hardenComboBox(branchCombo);
        UiUtils.hardenComboBox(roleCombo);
        branchCombo.setItems(javafx.collections.FXCollections.observableArrayList(branchRepository.findAll()));
        roleCombo.setItems(javafx.collections.FXCollections.observableArrayList(roleRepository.findAll()));

        UiUtils.attachSafe(saveButton, this::doSave, systemLogService, "USER_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, systemLogService, "USER_CANCEL");

        UiUtils.applyHoverElevation(saveButton);
        UiUtils.applyPressFeedback(saveButton);
        UiUtils.applyHoverElevation(cancelButton);
        UiUtils.applyPressFeedback(cancelButton);

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
            errors.append("Username é obrigatório. ");
            valid = false;
        } else if (username.trim().length() < 4) {
            errors.append("Username muito curto (mín. 4 caracteres). ");
            valid = false;
        } else if (!username.matches("\\w+")) {
            errors.append("Username só pode conter letras e números. ");
            valid = false;
        }

        String fullName = fullNameField.getText();
        if (fullName == null || fullName.isBlank()) {
            errors.append("Nome completo é obrigatório. ");
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

        if (roleCombo.getValue() == null) {
            errors.append("Perfil é obrigatório. ");
            valid = false;
        }
        if (branchCombo.getValue() == null) {
            errors.append("Filial é obrigatória. ");
            valid = false;
        }

        if (commissionField.getText() != null && !commissionField.getText().isBlank()) {
            try {
                double comm = Double.parseDouble(commissionField.getText().trim().replace(",", "."));
                if (comm < 0 || comm > 100) {
                    errors.append("Comissão deve estar entre 0 e 100%. ");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                errors.append("Comissão inválida. ");
                valid = false;
            }
        }

        boolean isNewUser = (user == null || user.getId() == null);
        if (isNewUser) {
            String pwd = passwordField.getText();
            if (pwd == null || pwd.isBlank()) {
                errors.append("Password é obrigatória. ");
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

        if (!valid) {
            showError(errors.length() > 0 ? errors.toString().trim() : "Preencha todos os campos obrigatórios.");
            return;
        } else {
            hideError();
        }

        if (isNewUser && username != null && !username.isBlank() && username.trim().length() >= 4 && username.matches("\\w+")) {
            String trimmedUsername = username.trim();
            if (duplicateCheckTask != null) duplicateCheckTask.cancel(false);
            duplicateCheckTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return userRepository.findByUsername(trimmedUsername).isPresent();
                }
            };
            duplicateCheckTask.setOnSucceeded(e -> {
                if (Boolean.TRUE.equals(duplicateCheckTask.getValue())) {
                    formValidProperty.set(false);
                    showError("Nome de utilizador '" + trimmedUsername + "' já existe.");
                }
            });
            Thread dupThread = new Thread(duplicateCheckTask);
            dupThread.setDaemon(true);
            dupThread.start();
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

        String username = usernameField.getText();
        String fullName = fullNameField.getText();
        if (username == null || username.isBlank()) { showError("Username é obrigatório."); return; }
        if (fullName == null || fullName.isBlank()) { showError("Nome completo é obrigatório."); return; }
        if (roleCombo.getValue() == null) { showError("Perfil é obrigatório."); return; }
        if (branchCombo.getValue() == null) { showError("Filial é obrigatória."); return; }
        boolean isNewUser = (user == null || user.getId() == null);
        if (isNewUser && (passwordField.getText() == null || passwordField.getText().isBlank())) {
            showError("Password é obrigatória.");
            return;
        }

        showSaveSpinner();

        javafx.concurrent.Task<Void> saveTask = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                boolean isNew = user.getId() == null;

                if (isNew) {
                    String trimmedUsername = username.trim();
                    if (userRepository.findByUsername(trimmedUsername).isPresent()) {
                        throw new IllegalArgumentException("Username '" + trimmedUsername + "' já existe.");
                    }
                    user.setUsername(trimmedUsername);
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
                userRepository.save(user);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "UTILIZADOR_GRAVADO", "Utilizador gravado com sucesso: " + usernameField.getText());
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

        Thread saveThread = new Thread(saveTask);
        saveThread.setDaemon(true);
        saveThread.start();
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
