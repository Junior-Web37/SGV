package com.sgv.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import javafx.scene.layout.*;
import javafx.scene.control.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import com.sgv.entity.AppConfig;
import com.sgv.entity.Role;
import com.sgv.entity.SystemBackup;
import com.sgv.entity.User;
import com.sgv.repository.AuditLogRepository;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.RoleRepository;
import com.sgv.repository.SystemBackupRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.AppConfigService;
import com.sgv.service.AuditLogService;
import com.sgv.service.BackupService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SistemaModuleController {

    private static final Logger log = LoggerFactory.getLogger(SistemaModuleController.class);

    // ─── Repositories ───────────────────────────────────────────────────────
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private AuditLogService auditLogService;
    @Autowired private DesktopAuthService authService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AppConfigService appConfigService;
    @Autowired private SystemBackupRepository systemBackupRepository;
    @Autowired private org.springframework.context.ApplicationContext applicationContext;
    @Autowired private com.sgv.service.LicenseService licenseService;
    @Autowired private BackupService backupService;

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
    @FXML private Button btnNewUser;
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
    @FXML private Button btnBackupNow;
    @FXML private Button btnRestoreBackup;
    @FXML private Button btnOpenBackupFolder;
    @FXML private Button btnSaveBackupSettings;
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
    @FXML private Button btnActivateTraining;
    @FXML private Button btnDeactivateTraining;
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
    @FXML private Button btnActivateLic;
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
    private AppConfig appConfig;  // Configuração real da BD
    private boolean trainingModeActive = false;
    private final List<User> trainingUsers = new ArrayList<>();

    // ─── Entry Point (called by Dashboard) ─────────────────────────────────
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // ─── Init ───────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        UiUtils.hardenComboBox(cfgPrintFormat);
        UiUtils.hardenComboBox(backupFrequency);
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

        UiUtils.setupDebounce(userSearchField, () -> loadUsers(userSearchField.getText()), 400);
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
        // Carrega configuração real da base de dados
        appConfig = appConfigService.get();
        
        // Carrega modo treino da BD
        trainingModeActive = Boolean.TRUE.equals(appConfig.getDemoMode());
        
        cfgCompanyName.setText(appConfig.getCompanyName() != null ? appConfig.getCompanyName() : "");
        cfgCompanyNuit.setText(appConfig.getCompanyNuit() != null ? appConfig.getCompanyNuit() : "");
        cfgAddress.setText(appConfig.getCompanyAddress() != null ? appConfig.getCompanyAddress() : "");
        cfgPhone.setText(appConfig.getCompanyPhone() != null ? appConfig.getCompanyPhone() : "");
        cfgDocSerie.setText(appConfig.getDefaultSeries() != null ? appConfig.getDefaultSeries() : "A");
        // Taxa IVA padrão do sistema
        cfgIvaRate.setText(String.format("%.1f", appConfig.getDefaultTaxRate() != null ? appConfig.getDefaultTaxRate() : 16.0));
        cfgCurrency.setText(appConfig.getDefaultCurrency() != null ? appConfig.getDefaultCurrency() : "MZN");
        cfgExchangeRate.setText(String.format("%.2f", appConfig.getExchangeRate()));
        cfgInitialDocNumber.setText(appConfig.getInitialDocumentNumber() != null ? String.valueOf(appConfig.getInitialDocumentNumber()) : "1");
        cfgMaxDiscount.setText(String.format("%.0f", appConfig.getMaxDiscountPercent() != null ? appConfig.getMaxDiscountPercent() : 10.0));
        cfgPrintFormat.getSelectionModel().select(appConfig.getThermalPrinterWidth() != null && appConfig.getThermalPrinterWidth() == 80 ? "Termica 80mm" : "A4");
    }

    @FXML
    private void onSaveSettings() {
        if (cfgCompanyName.getText() == null || cfgCompanyName.getText().isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Nome da empresa é obrigatório.");
            return;
        }

        String rawNuit = cfgCompanyNuit.getText() != null ? cfgCompanyNuit.getText().replaceAll("\\D", "") : "";
        if (!rawNuit.isEmpty()) {
            if (rawNuit.length() != 9 || !com.sgv.util.NuitValidator.isValid(rawNuit)) {
                showAlert(Alert.AlertType.WARNING, "NUIT da empresa inválido. Deve conter 9 dígitos válidos segundo as regras da AT (Módulo 11).");
                return;
            }
        }
        
        // Atualiza a configuração real na base de dados
        appConfig.setCompanyName(cfgCompanyName.getText().trim());
        appConfig.setCompanyNuit(cfgCompanyNuit.getText().trim());
        appConfig.setCompanyAddress(cfgAddress.getText().trim());
        appConfig.setCompanyPhone(cfgPhone.getText().trim());
        appConfig.setDefaultSeries(cfgDocSerie.getText().trim().toUpperCase());
        
        // Taxa IVA - guarda como double
        try {
            double taxRate = Double.parseDouble(cfgIvaRate.getText().trim().replace(",", "."));
            appConfig.setDefaultTaxRate(taxRate);
        } catch (NumberFormatException e) {
            appConfig.setDefaultTaxRate(16.0); // Padrão Moçambique
        }
        
        appConfig.setDefaultCurrency(cfgCurrency.getText().trim().toUpperCase());
        
        // Taxa de câmbio
        try {
            double exchRate = Double.parseDouble(cfgExchangeRate.getText().trim().replace(",", "."));
            appConfig.setExchangeRate(exchRate);
        } catch (NumberFormatException e) {
            appConfig.setExchangeRate(74.0);
        }
        
        // Número inicial de documentos
        try {
            long initialNum = Long.parseLong(cfgInitialDocNumber.getText().trim());
            appConfig.setInitialDocumentNumber(initialNum);
        } catch (NumberFormatException e) {
            appConfig.setInitialDocumentNumber(1L);
        }
        
        // Largura da impressora térmica
        String printFormat = cfgPrintFormat.getValue();
        if (printFormat != null && printFormat.contains("80")) {
            appConfig.setThermalPrinterWidth(80);
        } else if (printFormat != null && printFormat.contains("A5")) {
            appConfig.setThermalPrinterWidth(58);
        } else {
            appConfig.setThermalPrinterWidth(80); // Padrão
        }
        
        // Desconto máximo
        try {
            double maxDisc = Double.parseDouble(cfgMaxDiscount.getText().trim().replace(",", "."));
            appConfig.setMaxDiscountPercent(maxDisc);
        } catch (NumberFormatException e) {
            appConfig.setMaxDiscountPercent(10.0);
        }
        
        // Salva na base de dados
        try {
            appConfigService.save(appConfig);
            auditLogService.log(null, "EDITAR", "configuracoes", null,
                "Configurações do sistema actualizadas por " + (currentUser != null ? currentUser.getUsername() : "desconhecido"));
            showAlert(Alert.AlertType.INFORMATION, "Configurações guardadas com sucesso!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao guardar configurações: " + e.getMessage());
        }
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
        if (profilesTable == null || profileNameCol == null || profileDescCol == null || profilePermissionsCol == null) {
            return;
        }
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
            log.error("Erro ao carregar utilizadores: {}", e.getMessage(), e);
        }
    }

    private void loadProfiles() {
        if (profilesTable == null) return;
        try {
            profilesTable.setItems(FXCollections.observableArrayList(roleRepository.findAll()));
        } catch (Exception e) {
            log.error("Erro ao carregar perfis: {}", e.getMessage(), e);
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
            log.error("Erro ao abrir formulário de utilizador", ex);
            showAlert(Alert.AlertType.ERROR, "Erro ao abrir formulário: " + ex.getMessage());
        }
    }

    private void openProfileDialog(Role initialRole) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("SGV — Gestão de Perfis & Matriz de Permissões (RBAC)");
        dialog.setResizable(true);
        dialog.setWidth(880);
        dialog.setHeight(680);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F8FAFC;");

        // 1. Header no estilo corporativo do menu SGV
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #1E3A8A, #1D4ED8, #1E40AF); -fx-padding: 16 24; -fx-border-color: #172554; -fx-border-width: 0 0 2 0;");
        Label iconLbl = new Label("🛡️");
        iconLbl.setStyle("-fx-font-size: 20px;");
        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Matriz de Permissões & Controlo de Acesso (RBAC)");
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #FFFFFF;");
        Label subLbl = new Label("Atribuição granular de operações por página e perfil de utilizador");
        subLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #93C5FD;");
        titleBox.getChildren().addAll(titleLbl, subLbl);
        header.getChildren().addAll(iconLbl, titleBox);

        // 2. Barra de Selecção / Criação de Perfil
        VBox profileConfigCard = new VBox(10);
        profileConfigCard.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 16 20; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        HBox profileRow = new HBox(12);
        profileRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblPerfilSel = new Label("Perfil Selecionado:");
        lblPerfilSel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        ComboBox<Role> roleCombo = new ComboBox<>();
        UiUtils.hardenComboBox(roleCombo);
        roleCombo.setStyle("-fx-pref-width: 220; -fx-font-size: 12px; -fx-font-weight: 700;");
        List<Role> allRoles = roleRepository.findAll();
        roleCombo.setItems(FXCollections.observableArrayList(allRoles));

        Button btnNovoPerfil = new Button("➕ Novo Perfil");
        btnNovoPerfil.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-border-color: #93C5FD; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12px; -fx-padding: 6 12; -fx-cursor: hand;");

        profileRow.getChildren().addAll(lblPerfilSel, roleCombo, btnNovoPerfil);

        HBox fieldsRow = new HBox(16);
        fieldsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox nameBox = new VBox(4);
        Label nameLbl = new Label("Nome do Perfil *");
        nameLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        TextField nameField = new TextField();
        nameField.setPromptText("Ex: OPERADOR_CAIXA, GERENTE_LOJA");
        nameField.setStyle("-fx-pref-width: 240; -fx-padding: 6 10; -fx-background-radius: 6; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700;");
        nameBox.getChildren().addAll(nameLbl, nameField);

        VBox descBox = new VBox(4);
        HBox.setHgrow(descBox, Priority.ALWAYS);
        Label descLbl = new Label("Descrição");
        descLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Ex: Acesso a vendas, caixa e consulta de artigos");
        descriptionField.setStyle("-fx-padding: 6 10; -fx-background-radius: 6; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-font-size: 12px;");
        descBox.getChildren().addAll(descLbl, descriptionField);

        fieldsRow.getChildren().addAll(nameBox, descBox);
        profileConfigCard.getChildren().addAll(profileRow, fieldsRow);

        // 3. Matriz de Checkboxes por Página / Módulo
        Map<String, CheckBox> checkMap = new LinkedHashMap<>();

        // Helper para criar checkbox
        java.util.function.BiFunction<String, String, CheckBox> mkCheck = (perm, text) -> {
            CheckBox cb = new CheckBox(text);
            cb.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
            checkMap.put(perm, cb);
            return cb;
        };

        // Grid de Módulos
        GridPane matrixGrid = new GridPane();
        matrixGrid.setHgap(16);
        matrixGrid.setVgap(16);
        matrixGrid.setStyle("-fx-padding: 20;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        matrixGrid.getColumnConstraints().addAll(col1, col2);

        // Helper para criar cartão de módulo
        java.util.function.Function<String, VBox> mkModuleCard = (modTitle) -> {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 14 16; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 6, 0, 0, 2);");
            Label t = new Label(modTitle);
            t.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #1E40AF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 6 0;");
            card.getChildren().add(t);
            return card;
        };

        // Módulo 1: Vendas & Facturação
        VBox cardVendas = mkModuleCard.apply("⚡ 1. Vendas & Facturação (PDV)");
        cardVendas.getChildren().addAll(
            mkCheck.apply("VENDAS:VIEW", "Visualizar Facturação & Vendas"),
            mkCheck.apply("VENDAS:CREATE", "Emitir Vendas / Facturas / PDV"),
            mkCheck.apply("VENDAS:DELETE", "Anular / Cancelar Documentos de Venda"),
            mkCheck.apply("VENDAS:DISCOUNT", "Conceder Descontos Especiais")
        );
        matrixGrid.add(cardVendas, 0, 0);

        // Módulo 2: Caixa & Tesouraria
        VBox cardCaixa = mkModuleCard.apply("💼 2. Caixa & Tesouraria");
        cardCaixa.getChildren().addAll(
            mkCheck.apply("CAIXA:VIEW", "Visualizar Sessões de Caixa"),
            mkCheck.apply("CAIXA:CREATE", "Abrir & Fechar Turno de Caixa"),
            mkCheck.apply("CAIXA:CASH_MOVEMENT", "Efetuar Sangrias e Reforços"),
            mkCheck.apply("FINANCEIRO:VIEW", "Registar Despesas & Resumo Financeiro")
        );
        matrixGrid.add(cardCaixa, 1, 0);

        // Módulo 3: Stock & Artigos
        VBox cardStock = mkModuleCard.apply("📊 3. Stock & Artigos");
        cardStock.getChildren().addAll(
            mkCheck.apply("STOCK:VIEW", "Consultar Inventário em Loja"),
            mkCheck.apply("PRODUTOS:CREATE", "Cadastrar & Editar Artigos / Preços"),
            mkCheck.apply("STOCK:CREATE", "Ajustes de Inventário / Quebras"),
            mkCheck.apply("CATALOGOS:VIEW", "Gerir Famílias & Categorias")
        );
        matrixGrid.add(cardStock, 0, 1);

        // Módulo 4: Armazéns & Compras
        VBox cardArmazens = mkModuleCard.apply("🏢 4. Armazém & Compras");
        cardArmazens.getChildren().addAll(
            mkCheck.apply("ARMAZENS:VIEW", "Consultar Stock em Armazéns Centrais"),
            mkCheck.apply("COMPRAS:CREATE", "Registar Facturas de Compra"),
            mkCheck.apply("TRANSFERENCIAS:VIEW", "Transferências de Stock Armazém ↔ Loja"),
            mkCheck.apply("COMPRAS:VIEW", "Gestão de Fornecedores & Pagamentos")
        );
        matrixGrid.add(cardArmazens, 1, 1);

        // Módulo 5: Clientes & Contas Correntes
        VBox cardClientes = mkModuleCard.apply("👥 5. Gestão de Clientes");
        cardClientes.getChildren().addAll(
            mkCheck.apply("CLIENTES:VIEW", "Consultar Ficha de Clientes"),
            mkCheck.apply("CLIENTES:CREATE", "Criar & Editar Clientes"),
            mkCheck.apply("CLIENTES:CREDIT", "Gerir Limites de Crédito & Saldos")
        );
        matrixGrid.add(cardClientes, 0, 2);

        // Módulo 6: Produção & Fabrico
        VBox cardProducao = mkModuleCard.apply("🥖 6. Produção & Fabrico");
        cardProducao.getChildren().addAll(
            mkCheck.apply("PRODUCAO:VIEW", "Consultar Ordens de Produção"),
            mkCheck.apply("PRODUCAO:CREATE", "Criar & Concluir Fabrico / Padaria")
        );
        matrixGrid.add(cardProducao, 1, 2);

        // Módulo 7: Mapas & Relatórios
        VBox cardRelatorios = mkModuleCard.apply("📈 7. Mapas & Relatórios");
        cardRelatorios.getChildren().addAll(
            mkCheck.apply("RELATORIOS:VIEW", "Visualizar Mapas Fiscais & Vendas"),
            mkCheck.apply("RELATORIOS:CREATE", "Exportar Ficheiro SAF-T MZ")
        );
        matrixGrid.add(cardRelatorios, 0, 3);

        // Módulo 8: Configurações & Sistema
        VBox cardSistema = mkModuleCard.apply("⚙️ 8. Configurações & Sistema");
        cardSistema.getChildren().addAll(
            mkCheck.apply("SISTEMA:VIEW", "Parâmetros da Empresa & Licenciamento"),
            mkCheck.apply("SISTEMA:CREATE", "Gestão de Utilizadores & Permissões"),
            mkCheck.apply("SISTEMA:DELETE", "Cópias de Segurança (Backups)")
        );
        matrixGrid.add(cardSistema, 1, 3);

        ScrollPane scroll = new ScrollPane(matrixGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F8FAFC; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // 4. Barra de Atalhos Rápidos / Presets
        HBox presetsBar = new HBox(8);
        presetsBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        presetsBar.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10 20; -fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0;");

        Label lblPresets = new Label("Predefinições Rápidas:");
        lblPresets.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748B;");

        Button btnAll = new Button("✓ Marcar Todas");
        btnAll.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
        btnAll.setOnAction(e -> checkMap.values().forEach(cb -> cb.setSelected(true)));

        Button btnNone = new Button("✗ Desmarcar");
        btnNone.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
        btnNone.setOnAction(e -> checkMap.values().forEach(cb -> cb.setSelected(false)));

        Button btnPresetCaixa = new Button("💼 Operador Caixa");
        btnPresetCaixa.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
        btnPresetCaixa.setOnAction(e -> {
            checkMap.values().forEach(cb -> cb.setSelected(false));
            Set.of("VENDAS:VIEW", "VENDAS:CREATE", "CAIXA:VIEW", "CAIXA:CREATE", "CLIENTES:VIEW", "STOCK:VIEW")
                .forEach(p -> { if (checkMap.containsKey(p)) checkMap.get(p).setSelected(true); });
        });

        Button btnPresetAdmin = new Button("⭐ Admin Total");
        btnPresetAdmin.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 4; -fx-cursor: hand;");
        btnPresetAdmin.setOnAction(e -> checkMap.values().forEach(cb -> cb.setSelected(true)));

        Region spacerPres = new Region();
        HBox.setHgrow(spacerPres, Priority.ALWAYS);

        Button btnCancel = new Button("Cancelar");
        btnCancel.getStyleClass().add("btn-secondary-ux");
        btnCancel.setStyle("-fx-padding: 8 18; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("💾 Guardar Permissões do Perfil");
        btnSave.getStyleClass().add("btn-primary-ux");
        btnSave.setStyle("-fx-padding: 8 20; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-color: #2563EB; -fx-text-fill: #FFFFFF; -fx-background-radius: 6; -fx-cursor: hand;");

        presetsBar.getChildren().addAll(lblPresets, btnAll, btnNone, btnPresetCaixa, btnPresetAdmin, spacerPres, btnCancel, btnSave);

        // Lógica de Carregamento de Perfil
        java.util.function.Consumer<Role> loadRoleIntoUI = (r) -> {
            if (r != null) {
                nameField.setText(r.getName() != null ? r.getName() : "");
                descriptionField.setText(r.getDescription() != null ? r.getDescription() : "");
                Set<String> perms = r.getPermissions();
                boolean isSuper = perms != null && (perms.contains("*:*") || "ADMIN".equalsIgnoreCase(r.getName()));
                checkMap.forEach((permKey, cb) -> {
                    cb.setSelected(isSuper || (perms != null && perms.contains(permKey)));
                });
            } else {
                nameField.setText("");
                descriptionField.setText("");
                checkMap.values().forEach(cb -> cb.setSelected(false));
            }
        };

        roleCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadRoleIntoUI.accept(n);
        });

        btnNovoPerfil.setOnAction(e -> {
            roleCombo.getSelectionModel().clearSelection();
            loadRoleIntoUI.accept(null);
            nameField.requestFocus();
        });

        // Se veio perfil inicial, seleciona
        if (initialRole != null) {
            roleCombo.getSelectionModel().select(initialRole);
        } else if (!allRoles.isEmpty()) {
            roleCombo.getSelectionModel().selectFirst();
        }

        // Lógica de Salvar
        btnSave.setOnAction(e -> {
            String name = nameField.getText() != null ? nameField.getText().trim().toUpperCase(Locale.ROOT) : "";
            if (name.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Nome do perfil é obrigatório.");
                return;
            }

            Role target = roleCombo.getValue();
            if (target == null) {
                target = roleRepository.findByName(name).orElse(new Role());
            }
            target.setName(name);
            target.setDescription(descriptionField.getText() != null ? descriptionField.getText().trim() : "");

            Set<String> selectedPerms = new HashSet<>();
            checkMap.forEach((permKey, cb) -> {
                if (cb.isSelected()) selectedPerms.add(permKey);
            });
            if ("ADMIN".equalsIgnoreCase(name)) {
                selectedPerms.add("*:*");
            }
            target.setPermissions(selectedPerms);

            try {
                roleRepository.save(target);
                loadProfiles();
                loadUsers(userSearchField != null ? userSearchField.getText() : null);
                showAlert(Alert.AlertType.INFORMATION, "Perfil '" + target.getName() + "' e permissões salvas com sucesso!");
                dialog.close();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erro ao guardar perfil: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(header, profileConfigCard, scroll, presetsBar);
        dialog.setScene(new javafx.scene.Scene(root));
        dialog.showAndWait();
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
        if (user.isProtectedAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Este utilizador não pode ser eliminado.");
            return;
        }
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
            "Diario", "Semanal", "Mensal", "Desactivado"
        ));
        
        // Carrega configurações de backup da BD
        backupFrequency.getSelectionModel().select(appConfig.getBackupFrequency() != null ? appConfig.getBackupFrequency() : "Diario");
        backupHour.setText(appConfig.getBackupHour() != null ? appConfig.getBackupHour() : "02:00");
        backupRetention.setText(appConfig.getBackupRetentionDays() != null ? String.valueOf(appConfig.getBackupRetentionDays()) : "30");

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
        // Carrega histórico de backups da base de dados
        try {
            List<SystemBackup> backups = systemBackupRepository.findAllByOrderByCreatedAtDesc();
            
            // Converte para BackupEntry para compatibilidade com a UI existente
            List<BackupEntry> entries = new ArrayList<>();
            for (SystemBackup b : backups) {
                String sizeStr = b.getFormattedSize();
                entries.add(new BackupEntry(b.getCreatedAt(), b.getFileName(), sizeStr, b.getBackupType()));
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
        } catch (Exception e) {
            log.error("Erro ao carregar backups: {}", e.getMessage(), e);
            backupHistoryTable.setItems(FXCollections.observableArrayList());
            backupCountLabel.setText("0");
            lastBackupLabel.setText("Erro ao carregar");
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
                // Correção do BUG-001: backup REAL via mysqldump (BackupService),
                // registado só como COMPLETED se o ficheiro existir com conteúdo.
                // (Antes copiava um "sgv.db" inexistente e gravava COMPLETED
                // incondicionalmente — o registo nunca era um backup real.)
                BackupService.BackupOutcome out = backupService.runBackup(
                        "MANUAL", currentUser != null ? currentUser.getUsername() : "system");
                if (out.success()) {
                    auditLogService.log(null, "BACKUP", "sistema", null,
                        "Backup criado: " + out.file().getName() + " (sha256 " + out.sha256() + ")");
                    showAlert(Alert.AlertType.INFORMATION,
                        "Backup criado com sucesso!\n\n" + out.file().getAbsolutePath() + "\n"
                                + (out.sizeBytes() / 1024) + " KB — SHA-256: " + out.sha256());
                } else {
                    showAlert(Alert.AlertType.ERROR,
                        "O backup falhou (registado como FAILED):\n" + out.error());
                }
                loadBackupHistory();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erro ao criar backup: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onRestoreBackup() {
        // Correção do BUG-001: o botão "Restaurar" era morto (apenas mostrava
        // uma nota). Agora escolhe um dump .sql e aplica-o ao servidor via
        // BackupService.restoreFrom (mysql < backup.sql).
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Seleccionar Ficheiro de Backup (dump .sql)");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Dump SQL (*.sql)", "*.sql"));
        File file = chooser.showOpenDialog(null);
        if (file == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restaurar Backup");
        confirm.setContentText("Aplicar o dump '" + file.getName() + "' à base de dados actual?\n\nAviso: a base de dados actual será SUBSTITUÍDA. A aplicação tem de ser reiniciada após o restore.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) return;
        try {
            BackupService.RestoreOutcome out = backupService.restoreFrom(file.toPath(), null);
            SystemBackup restoreLog = new SystemBackup();
            restoreLog.setFileName("RESTORE_" + file.getName());
            restoreLog.setFilePath(file.getAbsolutePath());
            restoreLog.setBackupType("RESTORE");
            restoreLog.setStatus(out.success() ? "COMPLETED" : "FAILED");
            restoreLog.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
            if (!out.success()) restoreLog.setErrorMessage(out.detail());
            systemBackupRepository.save(restoreLog);
            auditLogService.log(null, "RESTORE", "backups", null,
                "Restore " + (out.success() ? "com sucesso" : "FALHOU") + " a partir do ficheiro: " + file.getName());
            if (out.success()) {
                showAlert(Alert.AlertType.INFORMATION, out.detail());
            } else {
                showAlert(Alert.AlertType.ERROR, "O restore não foi aplicado:\n" + out.detail());
            }
            loadBackupHistory();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao restaurar backup: " + e.getMessage());
        }
    }

    private void onRestoreBackupFile(BackupEntry b) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restaurar Backup");
        confirm.setContentText("Restaurar o backup '" + b.fileName + "'?\n\nAviso: A base de dados actual será substituída. A aplicação será reiniciada.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Obtém o caminho do ficheiro a partir do registo na BD
                Optional<SystemBackup> bkOpt = systemBackupRepository.findByFileName(b.fileName);
                if (bkOpt.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Registo de backup não encontrado na base de dados.");
                    return;
                }
                String storedPath = bkOpt.get().getFilePath();
                Path src = (storedPath != null && !storedPath.isBlank()) ? Paths.get(storedPath) : Paths.get("backups", b.fileName);
                if (!Files.exists(src)) {
                    showAlert(Alert.AlertType.ERROR, "Ficheiro de backup não encontrado:\n" + src.toAbsolutePath());
                    return;
                }

                // Correção do BUG-001: restore REAL — o dump SQL é aplicado ao
                // servidor via cliente mysql, com validação de integridade
                // (SHA-256) antes de aplicar. (Antes copiava o ficheiro para o
                // directório de dados do XAMPP — caminho fixo de Windows — e
                // marcava RESTORED sem confirmar que o restore funcionou.)
                BackupService.RestoreOutcome out = backupService.restoreFrom(src, bkOpt.get().getChecksumSha256());
                boolean copyOk = out.success();
                String errorMsg = out.success() ? "" : out.detail();

                if (copyOk) {
                    // Marca o backup restaurado como RESTORED na BD
                    bkOpt.ifPresent(bk -> {
                        bk.setStatus("RESTORED");
                        systemBackupRepository.save(bk);
                    });
                }
                // Regista a operação de restore
                SystemBackup restoreLog = new SystemBackup();
                restoreLog.setFileName("RESTORE_" + b.fileName);
                restoreLog.setFilePath(src.toAbsolutePath().toString());
                restoreLog.setBackupType("RESTORE");
                restoreLog.setStatus(copyOk ? "COMPLETED" : "FAILED");
                restoreLog.setNotes("Restore de: " + b.fileName + " por " + (currentUser != null ? currentUser.getUsername() : "system"));
                restoreLog.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
                if (!copyOk && !errorMsg.isEmpty()) {
                    restoreLog.setErrorMessage(errorMsg);
                }
                systemBackupRepository.save(restoreLog);
                auditLogService.log(null, "RESTORE", "backups", null,
                    "Restore " + (copyOk ? "com sucesso" : "FALHOU") + " a partir de: " + b.fileName);

                if (copyOk) {
                    showAlert(Alert.AlertType.INFORMATION, out.detail());
                } else {
                    showAlert(Alert.AlertType.ERROR,
                        "O restore não foi aplicado:\n" + out.detail());
                }
                loadBackupHistory();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erro ao restaurar backup: " + e.getMessage());
            }
        }
    }

    private void deleteBackup(BackupEntry b) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar Backup");
        confirm.setContentText("Eliminar o backup '" + b.fileName + "'?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Elimina ficheiro
                Path p = Paths.get("backups", b.fileName);
                Files.deleteIfExists(p);
                // Elimina registo da base de dados
                systemBackupRepository.findByFileName(b.fileName)
                    .ifPresent(systemBackupRepository::delete);
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
        try {
            appConfig.setBackupFrequency(backupFrequency.getValue());
            appConfig.setBackupHour(backupHour.getText().trim());
            try {
                appConfig.setBackupRetentionDays(Integer.parseInt(backupRetention.getText().trim()));
            } catch (NumberFormatException e) {
                appConfig.setBackupRetentionDays(30);
            }
            appConfigService.save(appConfig);
            auditLogService.log(null, "EDITAR", "backup_settings", null,
                "Configurações de backup actualizadas por " + (currentUser != null ? currentUser.getUsername() : "desconhecido"));
            showAlert(Alert.AlertType.INFORMATION,
                "Programação de backup guardada:\n" +
                "Frequência: " + backupFrequency.getValue() + "\n" +
                "Hora: " + backupHour.getText() + "\n" +
                "Retenção: " + backupRetention.getText() + " dias");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erro ao guardar configurações de backup: " + e.getMessage());
        }
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
            // Persiste na BD
            appConfig.setDemoMode(true);
            appConfigService.save(appConfig);
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
            // Persiste na BD
            appConfig.setDemoMode(false);
            appConfigService.save(appConfig);
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
                // Persiste na BD
                appConfig.setDemoMode(false);
                appConfigService.save(appConfig);
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
        com.sgv.service.LicenseService.LicenseResult result = licenseService.getCurrentLicenseStatus();

        licTypeLabel.setText(result.getType());
        if (result.isValid()) {
            licStatusLabel.setText("Activa");
            licStatusLabel.setStyle("-fx-text-fill:#10B981; -fx-font-weight:700;");
        } else {
            licStatusLabel.setText("Inactiva");
            licStatusLabel.setStyle("-fx-text-fill:#EF4444; -fx-font-weight:700;");
        }
        licExpiryLabel.setText(result.getExpiryDate());
        if (result.getDaysRemaining() == Integer.MAX_VALUE) {
            licDaysLeftLabel.setText("Ilimitado");
        } else {
            licDaysLeftLabel.setText(String.valueOf(result.getDaysRemaining()) + " dias");
        }
        licCompanyLabel.setText(appConfig != null && appConfig.getCompanyName() != null ? appConfig.getCompanyName() : "—");
        try {
            machineIdField.setText(licenseService.getMachineId());
        } catch (Exception e) {
            machineIdField.setText("Não disponível");
        }
        installIdField.setText(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private String getMachineId() {
        return licenseService.getMachineId();
    }

    @FXML
    private void onActivateLicense() {
        String key = licKeyField.getText();
        if (key == null || key.isBlank() || key.length() < 10) {
            showAlert(Alert.AlertType.WARNING, "Chave de licença inválida. Verifique e tente novamente.");
            return;
        }

        com.sgv.service.LicenseService.LicenseResult result = licenseService.activateLicense(key);
        if (result.isValid()) {
            auditLogService.log(null, "ACTIVAR", "licenca", null,
                "Licença activada: tipo=" + result.getType() + ", expira=" + result.getExpiryDate());
            loadLicenseInfo();
            licKeyField.clear();
            showAlert(Alert.AlertType.INFORMATION,
                "Licença activada com sucesso!\n\n" +
                "Tipo: " + result.getType() + "\n" +
                "Validade: " + result.getExpiryDate() + "\n" +
                "Dias restantes: " + (result.getDaysRemaining() == Integer.MAX_VALUE ? "Ilimitado" : result.getDaysRemaining()) +
                "\n\nObrigado pela sua confiança!");
        } else {
            showAlert(Alert.AlertType.ERROR, "Licença inválida: " + result.getMessage());
        }
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
            log.error("Erro ao abrir janela", e);
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
