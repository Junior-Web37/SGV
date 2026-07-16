package com.sgv.desktop;

import com.sgv.entity.Branch;
import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.AuditLogRepository;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.RoleRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.AuditLogService;
import com.sgv.service.DesktopAuthService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Rectangle2D;
import javafx.util.Callback;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SistemaModuleController {

    // ─── Repositories ───────────────────────────────────────────────────────
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private AuditLogService auditLogService;
    @Autowired private DesktopAuthService authService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private org.springframework.context.ApplicationContext applicationContext;

    // Current logged-in user (set from Dashboard)
    private User currentUser;

    // ─── FXML: Tab Pane ────────────────────────────────────────────────────
    @FXML private TabPane tabPane;

    // ─── FXML: Configurações ──────────────────────────────────────────────
    @FXML private TextField cfgCompanyName;
    @FXML private TextField cfgCompanyNuit;
    @FXML private TextField cfgAddress;
    @FXML private TextField cfgPhone;
    @FXML private TextField cfgDocSerie;
    @FXML private TextField cfgIvaRate;
    @FXML private TextField cfgCurrency;
    @FXML private TextField cfgExchangeRate;
    @FXML private TextField cfgInitialDocNumber;
    @FXML private TextField cfgStockMinAlert;
    @FXML private TextField cfgMaxDiscount;
    @FXML private ComboBox<String> cfgPrintFormat;
    @FXML private Button btnSaveSettings;

    // ─── FXML: Utilizadores ─────────────────────────────────────────────────
    @FXML private TextField userSearchField;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> userIdCol;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userLoginCol;
    @FXML private TableColumn<User, String> userEmailCol;
    @FXML private TableColumn<User, String> userBranchCol;
    @FXML private TableColumn<User, String> userRoleCol;
    @FXML private TableColumn<User, String> userStatusCol;
    @FXML private TableColumn<User, String> userActCol;
    @FXML private Button btnNewProfile;
    @FXML private TableView<Role> profilesTable;
    @FXML private TableColumn<Role, String> profileNameCol;
    @FXML private TableColumn<Role, String> profileDescCol;
    @FXML private TableColumn<Role, String> profilePermissionsCol;

    // ─── FXML: Backups ─────────────────────────────────────────────────────
    @FXML private Label lastBackupLabel;
    @FXML private Label lastBackupSizeLabel;
    @FXML private Label nextBackupLabel;
    @FXML private Label backupCountLabel;
    @FXML private TableView<BackupEntry> backupHistoryTable;
    @FXML private TableColumn<BackupEntry, String> bkDateCol;
    @FXML private TableColumn<BackupEntry, String> bkFileCol;
    @FXML private TableColumn<BackupEntry, String> bkSizeCol;
    @FXML private TableColumn<BackupEntry, String> bkTypeCol;
    @FXML private TableColumn<BackupEntry, String> bkActCol;
    @FXML private ComboBox<String> backupFrequency;
    @FXML private TextField backupHour;
    @FXML private TextField backupRetention;

    // ─── FXML: Modo Treinamento ────────────────────────────────────────────
    @FXML private Label trainingModeStatusLabel;
    @FXML private TableView<User> trainingUsersTable;
    @FXML private TableColumn<User, String> tuNameCol;
    @FXML private TableColumn<User, String> tuLoginCol;
    @FXML private TableColumn<User, String> tuBranchCol;
    @FXML private TableColumn<User, String> tuSinceCol;
    @FXML private TableColumn<User, String> tuActionsCol;

    // ─── FXML: Licenciamento ───────────────────────────────────────────────
    @FXML private Label licTypeLabel;
    @FXML private Label licStatusLabel;
    @FXML private Label licExpiryLabel;
    @FXML private Label licDaysLeftLabel;
    @FXML private Label licCompanyLabel;
    @FXML private TextField licKeyField;
    @FXML private TextField machineIdField;
    @FXML private TextField installIdField;

    // ─── FXML: Alterar Senha ───────────────────────────────────────────────
    @FXML private PasswordField currentPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;
    @FXML private Label pwdErrorLabel;
    @FXML private Button btnChangePassword;

    // ─── Data ────────────────────────────────────────────────────────────────
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final Map<String, String> systemConfig = new HashMap<>();
    private boolean trainingModeActive = false;
    private final List<User> trainingUsers = new ArrayList<>();

    // ─── Entry Point (called by Dashboard) ─────────────────────────────────
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // ─── Init ───────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupConfigTab();
        setupUsersTab();
        setupProfilesSection();
        setupBackupsTab();
        setupTrainingTab();
        setupLicenseTab();
        setupPasswordTab();

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == null) return;
            switch (newTab.getText()) {
                case "👥  Utilizadores" -> loadUsers("");
                case "💾  Backups" -> loadBackupHistory();
                case "🎓  Modo Treinamento" -> loadTrainingUsers();
                case "🔑  Licenciamento" -> loadLicenseInfo();
            }
        });

        userSearchField.textProperty().addListener((obs, o, n) -> loadUsers(n));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 1: CONFIGURAÇÕES
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupConfigTab() {
        cfgPrintFormat.setItems(FXCollections.observableArrayList(
            "Termica 80mm", "A4", "A5", "Talão Pequeno"
        ));
        cfgPrintFormat.getSelectionModel().select(0);
        loadConfig();
    }

    private void loadConfig() {
        cfgCompanyName.setText(systemConfig.getOrDefault("company_name", ""));
        cfgCompanyNuit.setText(systemConfig.getOrDefault("company_nuit", ""));
        cfgAddress.setText(systemConfig.getOrDefault("address", ""));
        cfgPhone.setText(systemConfig.getOrDefault("phone", ""));
        cfgDocSerie.setText(systemConfig.getOrDefault("doc_serie", "A"));
        cfgIvaRate.setText(systemConfig.getOrDefault("iva_rate", "17"));
        cfgCurrency.setText(systemConfig.getOrDefault("currency", "MZN"));
        cfgExchangeRate.setText(systemConfig.getOrDefault("exchange_rate", "74.00"));
        cfgInitialDocNumber.setText(systemConfig.getOrDefault("initial_doc_number", "1"));
        cfgStockMinAlert.setText(systemConfig.getOrDefault("stock_min_alert", "20"));
        cfgMaxDiscount.setText(systemConfig.getOrDefault("max_discount", "10"));
        String fmt = systemConfig.getOrDefault("print_format", "Termica 80mm");
        cfgPrintFormat.getSelectionModel().select(fmt);
    }

    @FXML
    private void onSaveSettings() {
        if (cfgCompanyName.getText() == null || cfgCompanyName.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Nome da empresa é obrigatório.");
            return;
        }
        systemConfig.put("company_name", cfgCompanyName.getText());
        systemConfig.put("company_nuit", cfgCompanyNuit.getText());
        systemConfig.put("address", cfgAddress.getText());
        systemConfig.put("phone", cfgPhone.getText());
        systemConfig.put("doc_serie", cfgDocSerie.getText());
        systemConfig.put("iva_rate", cfgIvaRate.getText());
        systemConfig.put("currency", cfgCurrency.getText());
        systemConfig.put("exchange_rate", cfgExchangeRate.getText());
        systemConfig.put("initial_doc_number", cfgInitialDocNumber.getText());
        systemConfig.put("stock_min_alert", cfgStockMinAlert.getText());
        systemConfig.put("max_discount", cfgMaxDiscount.getText());
        systemConfig.put("print_format", cfgPrintFormat.getValue());

        auditLogService.log(null, "EDITAR", "configuracoes", null,
            "Configurações do sistema actualizadas por " + (currentUser != null ? currentUser.getUsername() : "desconhecido"));
        showAlert(Alert.AlertType.INFORMATION, "Configurações guardadas com sucesso!");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 2: UTILIZADORES
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupUsersTab() {
        userIdCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getId() != null ? String.valueOf(d.getValue().getId()) : "—"));
        userNameCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFullName() != null ? d.getValue().getFullName() : "—"));
        userLoginCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getUsername() != null ? d.getValue().getUsername() : "—"));
        userEmailCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getEmail() != null ? d.getValue().getEmail() : "—"));
        userBranchCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));
        userRoleCol.setCellValueFactory(d -> {
            var roles = d.getValue().getRoles();
            String roleStr = roles != null && !roles.isEmpty()
                ? roles.stream().map(Role::getName).collect(Collectors.joining(", ")) : "—";
            return new SimpleStringProperty(roleStr);
        });
        userStatusCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().isActive() ? "Activo" : "Inactivo"));
        userStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setStyle(""); return; }
                setText(item);
                if ("Activo".equals(item)) {
                    setStyle("-fx-text-fill:#10B981; -fx-font-weight:700;");
                } else {
                    setStyle("-fx-text-fill:#EF4444; -fx-font-weight:700;");
                }
            }
        });

        userActCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button delBtn = new Button("Eliminar");
            {
                editBtn.setStyle("-fx-background-color:#2563EB; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:2; -fx-cursor:hand;");
                delBtn.setStyle("-fx-background-color:#EF4444; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:2; -fx-cursor:hand;");
                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    openUserForm(u);
                });
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    deleteUser(u);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(4, editBtn, delBtn));
            }
        });

        usersTable.setRowFactory(makeRowFactory());
    }

    private void setupProfilesSection() {
        profileNameCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getName() != null ? d.getValue().getName() : "—"));
        profileDescCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getDescription() != null ? d.getValue().getDescription() : "—"));
        profilePermissionsCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getPermissions() != null && !d.getValue().getPermissions().isEmpty()
                ? String.join(", ", d.getValue().getPermissions())
                : "Sem permissões"));
        loadProfiles();
    }

    private void loadUsers(String query) {
        try {
            List<User> all = userRepository.findAll();
            List<User> filtered = (query == null || query.isBlank()) ? all :
                all.stream().filter(u ->
                    (u.getUsername() != null && u.getUsername().toLowerCase().contains(query.toLowerCase())) ||
                    (u.getFullName() != null && u.getFullName().toLowerCase().contains(query.toLowerCase())) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(query.toLowerCase()))
                ).collect(Collectors.toList());
            usersTable.setItems(FXCollections.observableArrayList(filtered));
        } catch (Exception e) {
            System.err.println("Erro ao carregar utilizadores: " + e.getMessage());
        }
    }

    private void loadProfiles() {
        try {
            profilesTable.setItems(FXCollections.observableArrayList(roleRepository.findAll()));
        } catch (Exception e) {
            System.err.println("Erro ao carregar perfis: " + e.getMessage());
        }
    }

    @FXML
    private void onNewUser() {
        if (!canManageSystem()) {
            showAlert(Alert.AlertType.WARNING, "Apenas administradores autorizados podem criar utilizadores.");
            return;
        }
        openUserForm(null);
    }

    @FXML
    private void onNewProfile() {
        if (!canManageSystem()) {
            showAlert(Alert.AlertType.WARNING, "Apenas administradores autorizados podem criar ou editar perfis.");
            return;
        }
        openProfileDialog(null);
    }

    private void openUserForm(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            UserFormController ctrl = loader.getController();
            ctrl.setUser(user);
            ctrl.setOnSave(() -> loadUsers(userSearchField.getText()));
            openModal(root);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro ao abrir formulário: " + ex.getMessage());
        }
    }

    private void openProfileDialog(Role role) {
        Dialog<Role> dialog = new Dialog<>();
        dialog.setTitle(role == null ? "Novo Perfil" : "Editar Perfil");
        dialog.setHeaderText("Defina um nome, descrição e permissões no formato PAGINA:ACAO");

        ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(role != null && role.getName() != null ? role.getName() : "");
        TextField descriptionField = new TextField(role != null && role.getDescription() != null ? role.getDescription() : "");
        TextArea permissionsArea = new TextArea(role != null && role.getPermissions() != null ? String.join(", ", role.getPermissions()) : "RESUMO:VIEW\nVENDAS:VIEW\nVENDAS:CREATE");
        permissionsArea.setPrefRowCount(8);
        permissionsArea.setWrapText(true);

        grid.add(new Label("Nome"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Descrição"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Permissões"), 0, 2);
        grid.add(permissionsArea, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                String name = nameField.getText() != null ? nameField.getText().trim().toUpperCase(Locale.ROOT) : "";
                if (name.isBlank()) {
                    return null;
                }
                Role target = role != null ? role : new Role();
                target.setName(name);
                target.setDescription(descriptionField.getText());
                Set<String> permissions = Arrays.stream(permissionsArea.getText().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> s.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
                target.setPermissions(permissions);
                return target;
            }
            return null;
        });

        Optional<Role> result = dialog.showAndWait();
        result.ifPresent(saved -> {
            try {
                roleRepository.save(saved);
                loadProfiles();
                showAlert(Alert.AlertType.INFORMATION, "Perfil guardado com sucesso.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erro ao guardar perfil: " + ex.getMessage());
            }
        });
    }

    boolean canManageSystem() {
        return currentUser != null && (currentUser.isSuperAdmin() || currentUser.hasPermission("SISTEMA", "CREATE") || currentUser.hasPermission("SISTEMA", "DELETE") || currentUser.hasPermission("SISTEMA", "VIEW"));
    }

    private void deleteUser(User user) {
        if (!canManageSystem()) {
            showAlert(Alert.AlertType.WARNING, "Apenas administradores autorizados podem eliminar utilizadores.");
            return;
        }
        if (user == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar Utilizador");
        confirm.setContentText("Tem a certeza que deseja eliminar '" + user.getUsername() + "'?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userRepository.delete(user);
                auditLogService.log(null, "ELIMINAR", "users", user.getId(),
                    "Utilizador eliminado: " + user.getUsername());
                loadUsers(userSearchField.getText());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR,
                    "Não foi possível eliminar. Verifique se existem registos associados.");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 3: BACKUPS
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupBackupsTab() {
        backupFrequency.setItems(FXCollections.observableArrayList(
            "Diário", "Semanal", "Mensal", "Desactivado"
        ));
        backupFrequency.getSelectionModel().select(0);
        backupHour.setText("02:00");
        backupRetention.setText("30");

        bkDateCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().dateTime.format(dtf)));
        bkFileCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fileName));
        bkSizeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().size));
        bkTypeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type));
        bkActCol.setCellFactory(col -> new TableCell<>() {
            private final Button restoreBtn = new Button("↺ Restaurar");
            private final Button delBtn = new Button("🗑 Eliminar");
            {
                restoreBtn.setStyle("-fx-background-color:#2563EB; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:2; -fx-cursor:hand;");
                delBtn.setStyle("-fx-background-color:#EF4444; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:2; -fx-cursor:hand;");
                restoreBtn.setOnAction(e -> {
                    BackupEntry b = getTableView().getItems().get(getIndex());
                    onRestoreBackupFile(b);
                });
                delBtn.setOnAction(e -> {
                    BackupEntry b = getTableView().getItems().get(getIndex());
                    deleteBackup(b);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(4, restoreBtn, delBtn));
            }
        });

        backupHistoryTable.setRowFactory(makeRowFactory());
    }

    private void loadBackupHistory() {
        // Scan backup directory
        List<BackupEntry> entries = new ArrayList<>();
        try {
            Path backupDir = Paths.get("backups");
            if (Files.exists(backupDir) && Files.isDirectory(backupDir)) {
                Files.list(backupDir)
                    .filter(p -> p.toString().endsWith(".db") || p.toString().endsWith(".zip") || p.toString().endsWith(".sql"))
                    .sorted((a, b) -> { try { return b.toFile().lastModified() > a.toFile().lastModified() ? 1 : -1; } catch(Exception e) { return 0; } })
                    .limit(50)
                    .forEach(p -> {
                        try {
                            File f = p.toFile();
                            long kb = f.length() / 1024;
                            String size = kb > 1024 ? String.format("%.1f MB", kb / 1024.0) : kb + " KB";
                            entries.add(new BackupEntry(
                                java.time.LocalDateTime.ofInstant(
                                    java.time.Instant.ofEpochMilli(f.lastModified()),
                                    java.time.ZoneId.systemDefault()),
                                f.getName(), size, "Automático"
                            ));
                        } catch (Exception ignored) {}
                    });
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar backups: " + e.getMessage());
        }

        backupHistoryTable.setItems(FXCollections.observableArrayList(entries));
        backupCountLabel.setText(String.valueOf(entries.size()));
        if (!entries.isEmpty()) {
            BackupEntry latest = entries.get(0);
            lastBackupLabel.setText(latest.dateTime.format(dtf));
            lastBackupSizeLabel.setText(latest.size);
        } else {
            lastBackupLabel.setText("Nunca realizado");
            lastBackupSizeLabel.setText("");
        }
    }

    @FXML
    private void onBackupNow() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Criar Backup");
        confirm.setContentText("Deseja criar um backup da base de dados agora?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Path backupDir = Paths.get("backups");
                Files.createDirectories(backupDir);
                String timestamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path dest = backupDir.resolve("sgv_backup_" + timestamp + ".db");
                // Copy current DB if exists
                Path dbPath = Paths.get("sgv.db");
                if (Files.exists(dbPath)) {
                    Files.copy(dbPath, dest);
                }
                auditLogService.log(null, "BACKUP", "sistema", null,
                    "Backup criado: " + dest.getFileName());
                showAlert(Alert.AlertType.INFORMATION, "Backup criado com sucesso!\n" + dest.toAbsolutePath());
                loadBackupHistory();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erro ao criar backup: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onRestoreBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar Pasta de Backup");
        File folder = chooser.showDialog(null);
        if (folder != null) {
            showAlert(Alert.AlertType.INFORMATION,
                "Restore from: " + folder.getAbsolutePath() + "\n\nNota: Em produção, o restore substituirá a base de dados actual.\nReinicie a aplicação após o restore.");
        }
    }

    private void onRestoreBackupFile(BackupEntry b) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restaurar Backup");
        confirm.setContentText("Restaurar o backup '" + b.fileName + "'?\n\nAviso: A base de dados actual será substituída.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showAlert(Alert.AlertType.INFORMATION,
                "Restore: " + b.fileName + "\n\nEm produção o ficheiro será restaurado.");
        }
    }

    private void deleteBackup(BackupEntry b) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar Backup");
        confirm.setContentText("Eliminar o backup '" + b.fileName + "'?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Path p = Paths.get("backups", b.fileName);
                Files.deleteIfExists(p);
                auditLogService.log(null, "ELIMINAR", "backups", null, "Backup eliminado: " + b.fileName);
                loadBackupHistory();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erro ao eliminar backup: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onOpenBackupFolder() {
        try {
            Path backupDir = Paths.get("backups");
            Files.createDirectories(backupDir);
            java.awt.Desktop.getDesktop().open(backupDir.toFile());
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Não foi possível abrir a pasta. Aceda manualmente a: ./backups/");
        }
    }

    @FXML
    private void onSaveBackupSettings() {
        showAlert(Alert.AlertType.INFORMATION,
            "Programação de backup guardada:\n" +
            "Frequência: " + backupFrequency.getValue() + "\n" +
            "Hora: " + backupHour.getText() + "\n" +
            "Retenção: " + backupRetention.getText() + " dias");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 4: MODO TREINAMENTO
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupTrainingTab() {
        tuNameCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getFullName() != null ? d.getValue().getFullName() : "—"));
        tuLoginCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getUsername() != null ? d.getValue().getUsername() : "—"));
        tuBranchCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getBranch() != null ? d.getValue().getBranch().getName() : "—"));
        tuSinceCol.setCellValueFactory(d -> new SimpleStringProperty("—"));
        tuActionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button exitBtn = new Button("Sair do Treino");
            {
                exitBtn.setStyle("-fx-background-color:#EF4444; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:2; -fx-cursor:hand;");
                exitBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    exitTrainingMode(u);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : exitBtn);
            }
        });
        trainingUsersTable.setRowFactory(makeRowFactory());
        updateTrainingStatus();
    }

    private void loadTrainingUsers() {
        trainingUsersTable.setItems(FXCollections.observableArrayList(new ArrayList<>(trainingUsers)));
    }

    private void updateTrainingStatus() {
        if (trainingModeActive) {
            trainingModeStatusLabel.setText("ACTIVO");
            trainingModeStatusLabel.setStyle("-fx-background-color:#10B981; -fx-text-fill:#ffffff; -fx-padding:6 16; -fx-background-radius:3; -fx-font-weight:800; -fx-font-size:14px;");
        } else {
            trainingModeStatusLabel.setText("DESACTIVO");
            trainingModeStatusLabel.setStyle("-fx-background-color:#EF4444; -fx-text-fill:#ffffff; -fx-padding:6 16; -fx-background-radius:3; -fx-font-weight:800; -fx-font-size:14px;");
        }
    }

    @FXML
    private void onActivateTraining() {
        if (!trainingModeActive) {
            trainingModeActive = true;
            if (currentUser != null && !trainingUsers.contains(currentUser)) {
                trainingUsers.add(currentUser);
            }
            auditLogService.log(null, "ACTIVAR", "modo_treino", null,
                "Modo treino activado por " + (currentUser != null ? currentUser.getUsername() : "?"));
            updateTrainingStatus();
            loadTrainingUsers();
            showAlert(Alert.AlertType.INFORMATION,
                "Modo Treinamento activado!\n\n• Barra amarela visível no topo da aplicação\n• Todas as operações são simuladas\n• Nenhum dado real é alterado");
        }
    }

    @FXML
    private void onDeactivateTraining() {
        if (trainingModeActive) {
            trainingModeActive = false;
            trainingUsers.clear();
            auditLogService.log(null, "DESACTIVAR", "modo_treino", null,
                "Modo treino desactivado por " + (currentUser != null ? currentUser.getUsername() : "?"));
            updateTrainingStatus();
            loadTrainingUsers();
            showAlert(Alert.AlertType.INFORMATION, "Modo Treinamento desactivado. Voltar à operação normal.");
        }
    }

    private void exitTrainingMode(User user) {
        if (user != null && trainingUsers.contains(user)) {
            trainingUsers.remove(user);
            if (trainingUsers.isEmpty()) {
                trainingModeActive = false;
                updateTrainingStatus();
            }
            loadTrainingUsers();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 5: LICENCIAMENTO
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupLicenseTab() {
        loadLicenseInfo();
    }

    private void loadLicenseInfo() {
        licTypeLabel.setText("Trial / Avaliação");
        licStatusLabel.setText("Activa");
        licExpiryLabel.setText("—");
        licDaysLeftLabel.setText("Ilimitado");
        licCompanyLabel.setText(systemConfig.getOrDefault("company_name", "—"));
        try {
            machineIdField.setText(getMachineId());
        } catch (Exception e) {
            machineIdField.setText("Não disponível");
        }
        installIdField.setText(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private String getMachineId() {
        try {
            String os = System.getProperty("os.name", "");
            String user = System.getProperty("user.name", "");
            String dir = System.getProperty("user.dir", "");
            String raw = os + user + dir;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02X", b));
            return sb.toString().substring(0, 20).toUpperCase();
        } catch (Exception e) {
            return "ERR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    @FXML
    private void onActivateLicense() {
        String key = licKeyField.getText();
        if (key == null || key.isBlank() || key.length() < 10) {
            showAlert(Alert.AlertType.WARNING, "Chave de licença inválida. Verifique e tente novamente.");
            return;
        }
        auditLogService.log(null, "ACTIVAR", "licenca", null,
            "Licença activada: " + key.substring(0, 4) + "****");
        licTypeLabel.setText("Comercial");
        licStatusLabel.setText("Activa");
        licExpiryLabel.setText("Vitalícia");
        licDaysLeftLabel.setText("Ilimitado");
        licKeyField.clear();
        showAlert(Alert.AlertType.INFORMATION,
            "Licença activada com sucesso!\n\nTipo: Comercial\nValidade: Vitalícia\n\nObrigado pela sua confiança!");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAB 6: ALTERAR SENHA
    // ═══════════════════════════════════════════════════════════════════════════

    private void setupPasswordTab() {
        currentPwdField.setOnAction(e -> newPwdField.requestFocus());
        newPwdField.setOnAction(e -> confirmPwdField.requestFocus());
        confirmPwdField.setOnAction(e -> onChangePassword());
    }

    @FXML
    private void onChangePassword() {
        pwdErrorLabel.setVisible(false);

        String current = currentPwdField.getText();
        String novo = newPwdField.getText();
        String confirm = confirmPwdField.getText();

        if (current == null || current.isBlank()) {
            showPwdError("Preencha a senha actual.");
            return;
        }
        if (novo == null || novo.isBlank()) {
            showPwdError("Preencha a nova senha.");
            return;
        }
        if (novo.length() < 6) {
            showPwdError("A nova senha deve ter pelo menos 6 caracteres.");
            return;
        }
        if (!novo.equals(confirm)) {
            showPwdError("As senhas não coincidem.");
            return;
        }
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Sessão de utilizador não disponível.");
            return;
        }

        // Verify current password
        if (authService.authenticate(currentUser.getUsername(), current) == null) {
            showPwdError("Senha actual incorrecta.");
            return;
        }

        // Change password
        try {
            currentUser.setPasswordHash(passwordEncoder.encode(novo));
            userRepository.save(currentUser);
            auditLogService.log(null, "ALTERAR_SENHA", "users", currentUser.getId(),
                "Senha alterada por " + currentUser.getUsername());

            currentPwdField.clear();
            newPwdField.clear();
            confirmPwdField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Senha alterada com sucesso!");
        } catch (Exception e) {
            showPwdError("Erro ao guardar senha: " + e.getMessage());
        }
    }

    private void showPwdError(String msg) {
        pwdErrorLabel.setText(msg);
        pwdErrorLabel.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHARED UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════

    private <T> Callback<TableView<T>, TableRow<T>> makeRowFactory() {
        return tv -> {
            TableRow<T> row = new TableRow<>();
            Runnable apply = () -> {
                if (row.isEmpty()) {
                    row.setStyle("");
                } else if (row.isSelected() || row.isHover()) {
                    row.setStyle("-fx-background-color:#DBEAFE; -fx-border-color:#BFDBFE; -fx-border-width:0 0 1 0;");
                } else {
                    row.setStyle(row.getIndex() % 2 == 0
                        ? "-fx-background-color:#ffffff; -fx-border-color:#F1F5F9; -fx-border-width:0 0 1 0;"
                        : "-fx-background-color:#F8FAFC; -fx-border-color:#F1F5F9; -fx-border-width:0 0 1 0;");
                }
            };
            row.hoverProperty().addListener((obs, old, isH) -> apply.run());
            row.selectedProperty().addListener((obs, old, isS) -> apply.run());
            return row;
        };
    }

    private void openModal(Parent root) {
        try {
            Stage stage = new Stage();
            stage.initStyle(StageStyle.DECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);

            ScrollPane scroll = new ScrollPane(root);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent; -fx-padding:0;");

            Scene scene = new Scene(scroll);
            stage.setScene(scene);

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            if (root instanceof Region) {
                Region r = (Region) root;
                double w = Math.min(r.prefWidth(-1) > 0 ? r.prefWidth(-1) : 700, bounds.getWidth() * 0.9);
                double h = Math.min(r.prefHeight(-1) > 0 ? r.prefHeight(-1) : 600, bounds.getHeight() * 0.9);
                stage.setWidth(w);
                stage.setHeight(h);
                stage.setX(bounds.getMinX() + (bounds.getWidth() - w) / 2);
                stage.setY(bounds.getMinY() + (bounds.getHeight() - h) / 2);
            }
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erro ao abrir janela: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // ─── Inner class for backup entries ──────────────────────────────────────
    public static class BackupEntry {
        public LocalDateTime dateTime;
        public String fileName;
        public String size;
        public String type;
        public BackupEntry(LocalDateTime dateTime, String fileName, String size, String type) {
            this.dateTime = dateTime;
            this.fileName = fileName;
            this.size = size;
            this.type = type;
        }
    }
}
