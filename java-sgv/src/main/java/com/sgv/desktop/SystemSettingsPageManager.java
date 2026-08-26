package com.sgv.desktop;

import com.sgv.entity.AppConfig;
import com.sgv.entity.SystemBackup;
import com.sgv.entity.User;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.SystemBackupRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.AppConfigService;
import com.sgv.service.AuditLogService;
import com.sgv.service.BackupService;
import com.sgv.service.DesktopAuthService;
import com.sgv.service.LicenseService;
import com.sgv.service.TrainingModeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class SystemSettingsPageManager {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingsPageManager.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AppConfigService appConfigService;
    private final SystemBackupRepository systemBackupRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final AuditLogService auditLogService;
    private final DesktopAuthService authService;
    private final LicenseService licenseService;
    private final TrainingModeService trainingModeService;
    private final PasswordEncoder passwordEncoder;
    private final BackupService backupService;

    public SystemSettingsPageManager(AppConfigService appConfigService,
                                     SystemBackupRepository systemBackupRepository,
                                     UserRepository userRepository,
                                     BranchRepository branchRepository,
                                     AuditLogService auditLogService,
                                     DesktopAuthService authService,
                                     LicenseService licenseService,
                                     TrainingModeService trainingModeService,
                                     PasswordEncoder passwordEncoder,
                                     BackupService backupService) {
        this.appConfigService = appConfigService;
        this.systemBackupRepository = systemBackupRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.auditLogService = auditLogService;
        this.authService = authService;
        this.licenseService = licenseService;
        this.trainingModeService = trainingModeService;
        this.passwordEncoder = passwordEncoder;
        this.backupService = backupService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PÁGINA: PARÂMETROS DA EMPRESA
    // ═══════════════════════════════════════════════════════════════════════

    public void buildParametrosPane(VBox container, User currentUser) {
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::isActive).count();
        GridPane kpiGrid = kpiGrid(
            new String[]{"Filiais / Lojas", "Utilizadores", "Activos", "Inactivos"},
            new String[]{"Pontos de venda registados", "Operadores do sistema", "Com acesso activo", "Sem acesso ao sistema"},
            new String[]{String.valueOf(branchRepository.count()), String.valueOf(totalUsers),
                String.valueOf(activeUsers), String.valueOf(totalUsers - activeUsers)},
            new String[]{"blue", "purple", "green", "orange"});

        HBox content = new HBox(16);
        content.setStyle("-fx-padding: 0 20 20 20;");
        content.setFillHeight(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        AppConfig cfg = appConfigService.get();

        VBox formCard = card("🏢 Dados da Empresa & Identificação Fiscal",
            "Campos obrigatórios para emissão de documentos certificados AT");

        TextField companyName = textField(cfg.getCompanyName());
        TextField companyNuit = textField(cfg.getCompanyNuit());
        companyNuit.setPromptText("9 dígitos (validação Módulo 11)");
        TextField address = textField(cfg.getCompanyAddress());
        TextField phone = textField(cfg.getCompanyPhone());
        TextField docSerie = textField(cfg.getDefaultSeries() != null ? cfg.getDefaultSeries() : "A");
        docSerie.setPrefWidth(90);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(16);
        formGrid.setVgap(14);
        ColumnConstraints colA = new ColumnConstraints();
        colA.setPercentWidth(50);
        ColumnConstraints colB = new ColumnConstraints();
        colB.setPercentWidth(50);
        formGrid.getColumnConstraints().addAll(colA, colB);

        ComboBox<String> printFormat = new ComboBox<>(FXCollections.observableArrayList(
            "Termica 80mm", "A4", "A5", "Talão Pequeno"));
        UiUtils.hardenComboBox(printFormat);
        printFormat.setStyle("-fx-font-size: 12px; -fx-font-weight: 600;");
        printFormat.getSelectionModel().select(cfg.getThermalPrinterWidth() != null && cfg.getThermalPrinterWidth() == 58 ? "A5" : "Termica 80mm");

        formGrid.add(labeledField("Nome da Empresa *", companyName), 0, 0);
        formGrid.add(labeledField("NUIT da Sede *", companyNuit), 1, 0);
        formGrid.add(labeledField("Endereço Completo", address), 0, 1, 2, 1);
        formGrid.add(labeledField("Telefone / Celular", phone), 0, 2);
        formGrid.add(labeledField("Série Documental Padrão", docSerie), 1, 2);

        TextField ivaRate = numericField(fmtDouble(cfg.getDefaultTaxRate() != null ? cfg.getDefaultTaxRate() : 16.0));
        TextField currency = textField(cfg.getDefaultCurrency() != null ? cfg.getDefaultCurrency() : "MZN");
        currency.setPrefWidth(110);
        TextField exchangeRate = numericField(fmtDouble(cfg.getExchangeRate()));
        TextField initialDocNumber = numericField(cfg.getInitialDocumentNumber() != null ? String.valueOf(cfg.getInitialDocumentNumber()) : "1");
        TextField maxDiscount = numericField(fmtDouble(cfg.getMaxDiscountPercent() != null ? cfg.getMaxDiscountPercent() : 10.0));

        HBox fiscalRow = new HBox(16);
        fiscalRow.setAlignment(Pos.CENTER_LEFT);
        fiscalRow.getChildren().addAll(
            labeledField("Taxa de IVA (%)", ivaRate),
            labeledField("Moeda", currency),
            labeledField("Câmbio Referência", exchangeRate));

        HBox docRow = new HBox(16);
        docRow.setAlignment(Pos.CENTER_LEFT);
        docRow.getChildren().addAll(
            labeledField("Nº Inicial de Documentos", initialDocNumber),
            labeledField("Desconto Máximo (%)", maxDiscount),
            labeledField("Formato de Impressão", printFormat));

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-padding: 18 0 4 0; -fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0;");
        Button saveBtn = primaryButton("💾 Guardar Parâmetros", "#2563EB");
        Button resetBtn = secondaryButton("↺ Restaurar Valores Guardados");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label hint = new Label("Alterações registadas na auditoria (Audit Log)");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        actions.getChildren().addAll(saveBtn, resetBtn, spacer, hint);

        Runnable loadValues = () -> {
            AppConfig c = appConfigService.get();
            companyName.setText(c.getCompanyName() != null ? c.getCompanyName() : "");
            companyNuit.setText(c.getCompanyNuit() != null ? c.getCompanyNuit() : "");
            address.setText(c.getCompanyAddress() != null ? c.getCompanyAddress() : "");
            phone.setText(c.getCompanyPhone() != null ? c.getCompanyPhone() : "");
            docSerie.setText(c.getDefaultSeries() != null ? c.getDefaultSeries() : "A");
            ivaRate.setText(fmtDouble(c.getDefaultTaxRate() != null ? c.getDefaultTaxRate() : 16.0));
            currency.setText(c.getDefaultCurrency() != null ? c.getDefaultCurrency() : "MZN");
            exchangeRate.setText(fmtDouble(c.getExchangeRate()));
            initialDocNumber.setText(c.getInitialDocumentNumber() != null ? String.valueOf(c.getInitialDocumentNumber()) : "1");
            maxDiscount.setText(fmtDouble(c.getMaxDiscountPercent() != null ? c.getMaxDiscountPercent() : 10.0));
            printFormat.getSelectionModel().select(c.getThermalPrinterWidth() != null && c.getThermalPrinterWidth() == 58 ? "A5" : "Termica 80mm");
        };

        saveBtn.setOnAction(e -> {
            if (companyName.getText() == null || companyName.getText().isBlank()) {
                alert(Alert.AlertType.WARNING, "Nome da empresa é obrigatório.");
                return;
            }
            String rawNuit = companyNuit.getText() != null ? companyNuit.getText().replaceAll("\\D", "") : "";
            if (!rawNuit.isEmpty() && (rawNuit.length() != 9 || !com.sgv.util.NuitValidator.isValid(rawNuit))) {
                alert(Alert.AlertType.WARNING, "NUIT inválido. Deve conter 9 dígitos válidos segundo as regras da AT (Módulo 11).");
                return;
            }
            try {
                AppConfig c = appConfigService.get();
                c.setCompanyName(companyName.getText().trim());
                c.setCompanyNuit(rawNuit);
                c.setCompanyAddress(address.getText() != null ? address.getText().trim() : "");
                c.setCompanyPhone(phone.getText() != null ? phone.getText().trim() : "");
                c.setDefaultSeries(docSerie.getText() != null ? docSerie.getText().trim().toUpperCase() : "A");
                c.setDefaultTaxRate(parseDoubleSafe(ivaRate.getText(), 16.0));
                c.setDefaultCurrency(currency.getText() != null ? currency.getText().trim().toUpperCase() : "MZN");
                c.setExchangeRate(parseDoubleSafe(exchangeRate.getText(), 74.0));
                c.setInitialDocumentNumber((long) parseIntSafe(initialDocNumber.getText(), 1));
                c.setMaxDiscountPercent(parseDoubleSafe(maxDiscount.getText(), 10.0));
                String pf = printFormat.getValue();
                if (pf != null && pf.contains("58")) c.setThermalPrinterWidth(58);
                else c.setThermalPrinterWidth(80);
                appConfigService.save(c);
                auditLogService.log(null, "EDITAR", "configuracoes", null,
                    "Configurações actualizadas por " + (currentUser != null ? currentUser.getUsername() : "desconhecido"));
                alert(Alert.AlertType.INFORMATION, "Parâmetros guardados com sucesso!");
                loadValues.run();
            } catch (Exception ex) {
                log.error("Erro ao guardar parâmetros", ex);
                alert(Alert.AlertType.ERROR, "Erro ao guardar parâmetros: " + ex.getMessage());
            }
        });
        resetBtn.setOnAction(e -> loadValues.run());

        ScrollPane formScroll = new ScrollPane(formCard);
        formScroll.setFitToWidth(true);
        formScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        HBox.setHgrow(formScroll, Priority.ALWAYS);
        formCard.getChildren().addAll(formGrid, fiscalRow, docRow, actions);

        VBox sideColumn = new VBox(16);
        sideColumn.setPrefWidth(320);
        sideColumn.setMinWidth(280);

        VBox fiscalCard = card("🧾 Conformidade Fiscal", "Requisitos legais aplicados automaticamente");
        fiscalCard.getChildren().addAll(infoLine("IVA padrão", "16% (taxa normal CIVA)"),
            infoLine("Regime", "Facturação Certificada AT"),
            infoLine("Diploma legal", "Decreto 7/2024 · SAF-T MZ"),
            infoLine("Validação NUIT", "Módulo 11 — Autoridade Tributária"));

        VBox statusCard = card("🖥️ Estado do Sistema", "Informação técnica da instalação");
        String licState;
        try {
            LicenseService.LicenseResult lr = licenseService.getCurrentLicenseStatus();
            licState = (lr.isValid() ? "✅ " : "⚠️ ") + lr.getType() + (lr.isValid() ? "" : " (avaliação)");
        } catch (Exception ex) {
            licState = "—";
        }
        statusCard.getChildren().addAll(infoLine("Versão", "SGV Desktop 1.0.10"),
            infoLine("Base de Dados", "MariaDB · localhost:3306/sgv"),
            infoLine("Licença", licState),
            infoLine("Modo de Operação", trainingModeService.isTrainingMode() ? "🎓 Treinamento (simulação)" : "💰 Produção (dados reais)"));
        VBox.setVgrow(statusCard, Priority.ALWAYS);

        sideColumn.getChildren().addAll(fiscalCard, statusCard);

        content.getChildren().addAll(formScroll, sideColumn);
        main.getChildren().addAll(kpiGrid, content);
        container.getChildren().setAll(main);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PÁGINA: CÓPIAS DE SEGURANÇA (BACKUP)
    // ═══════════════════════════════════════════════════════════════════════

    public void buildBackupsPane(VBox container, User currentUser) {
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");

        List<SystemBackup> backups = systemBackupRepository.findAllByOrderByCreatedAtDesc();
        AppConfig cfg = appConfigService.get();
        String lastDate = backups.isEmpty() ? "Nunca" : (backups.get(0).getCreatedAt() != null ? backups.get(0).getCreatedAt().format(DTF) : "—");
        GridPane kpiGrid = kpiGrid(
            new String[]{"Total de Backups", "Último Backup", "Programação", "Retenção"},
            new String[]{"Cópias registadas", "Cópia mais recente", "Automatização activa", "Limpeza automática"},
            new String[]{String.valueOf(backups.size()), lastDate,
                cfg.getBackupFrequency() != null ? cfg.getBackupFrequency() : "Diario",
                (cfg.getBackupRetentionDays() != null ? cfg.getBackupRetentionDays() : 30) + " dias"},
            new String[]{"blue", "green", "purple", "orange"});

        VBox tableCard = card("🗄️ Histórico de Cópias de Segurança",
            "Restaure ou elimine cópias anteriores da base de dados");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        TableView<SystemBackup> table = new TableView<>();
        table.setStyle("-fx-font-size: 12px; -fx-background-color: #ffffff;");
        table.setPlaceholder(new Label("Nenhuma cópia de segurança registada. Crie a primeira backup agora."));

        TableColumn<SystemBackup, String> dateCol = new TableColumn<>("Data / Hora");
        dateCol.setPrefWidth(160);
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(DTF) : "—"));

        TableColumn<SystemBackup, String> fileCol = new TableColumn<>("Ficheiro");
        fileCol.setPrefWidth(320);
        fileCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileName() != null ? d.getValue().getFileName() : "—"));

        TableColumn<SystemBackup, String> sizeCol = new TableColumn<>("Tamanho");
        sizeCol.setPrefWidth(110);
        sizeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedSize() != null ? d.getValue().getFormattedSize() : "—"));

        TableColumn<SystemBackup, String> typeCol = new TableColumn<>("Tipo");
        typeCol.setPrefWidth(120);
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBackupType() != null ? d.getValue().getBackupType() : "MANUAL"));

        TableColumn<SystemBackup, String> statusCol = new TableColumn<>("Estado");
        statusCol.setPrefWidth(120);
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus() : "—"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("COMPLETED".equals(item)) setStyle("-fx-text-fill:#10B981; -fx-font-weight:700;");
                else if ("FAILED".equals(item)) setStyle("-fx-text-fill:#EF4444; -fx-font-weight:700;");
                else if ("RESTORED".equals(item)) setStyle("-fx-text-fill:#2563EB; -fx-font-weight:700;");
                else setStyle("");
            }
        });

        TableColumn<SystemBackup, Void> actCol = new TableColumn<>("Acções");
        actCol.setPrefWidth(200);
        actCol.setCellFactory(col -> new TableCell<>() {
            private final Button restoreBtn = new Button("↺ Restaurar");
            private final Button delBtn = new Button("🗑 Eliminar");
            private final HBox box = new HBox(6, restoreBtn, delBtn);
            {
                restoreBtn.setStyle("-fx-background-color:#2563EB; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:3; -fx-cursor:hand;");
                delBtn.setStyle("-fx-background-color:#EF4444; -fx-text-fill:white; -fx-font-size:11px; -fx-padding:3 8; -fx-background-radius:3; -fx-cursor:hand;");
                restoreBtn.setOnAction(e -> confirmRestore(getTableView().getItems().get(getIndex()), currentUser));
                delBtn.setOnAction(e -> confirmDelete(getTableView().getItems().get(getIndex()), currentUser));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(List.of(dateCol, fileCol, sizeCol, typeCol, statusCol, actCol));
        table.setItems(FXCollections.observableArrayList(backups));
        table.setRowFactory(stripedRows());
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox toolbarCard = card("⚙️ Programação Automática & Acções",
            "Defina a frequência das cópias automáticas e crie backups manuais");
        ComboBox<String> frequency = new ComboBox<>(FXCollections.observableArrayList(
            "Diario", "Semanal", "Mensal", "Desactivado"));
        UiUtils.hardenComboBox(frequency);
        frequency.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-pref-width: 140;");
        frequency.getSelectionModel().select(cfg.getBackupFrequency() != null ? cfg.getBackupFrequency() : "Diario");
        TextField hour = textField(cfg.getBackupHour() != null ? cfg.getBackupHour() : "02:00");
        hour.setPrefWidth(90);
        hour.setPromptText("HH:mm");
        TextField retention = numericField(cfg.getBackupRetentionDays() != null ? String.valueOf(cfg.getBackupRetentionDays()) : "30");
        retention.setPrefWidth(90);

        Button saveScheduleBtn = primaryButton("💾 Guardar Programação", "#7C3AED");
        Button backupNowBtn = primaryButton("▶ Criar Backup Agora", "#2563EB");
        Button openFolderBtn = secondaryButton("📁 Abrir Pasta de Backups");

        HBox scheduleRow = new HBox(14);
        scheduleRow.setAlignment(Pos.CENTER_LEFT);
        scheduleRow.getChildren().addAll(
            labeledField("Frequência", frequency),
            labeledField("Hora Execução", hour),
            labeledField("Retenção (dias)", retention),
            saveScheduleBtn,
            new Region(),
            backupNowBtn,
            openFolderBtn);
        ((Region) scheduleRow.getChildren().get(4)).setMinWidth(20);
        HBox.setHgrow(scheduleRow.getChildren().get(4), Priority.ALWAYS);

        saveScheduleBtn.setOnAction(e -> {
            try {
                AppConfig c = appConfigService.get();
                c.setBackupFrequency(frequency.getValue());
                c.setBackupHour(hour.getText() != null ? hour.getText().trim() : "02:00");
                c.setBackupRetentionDays(parseIntSafe(retention.getText(), 30));
                appConfigService.save(c);
                auditLogService.log(null, "EDITAR", "backup_settings", null,
                    "Programação de backup actualizada por " + (currentUser != null ? currentUser.getUsername() : "desconhecido"));
                alert(Alert.AlertType.INFORMATION, "Programação guardada:\nFrequência: " + frequency.getValue()
                    + "\nHora: " + hour.getText() + "\nRetenção: " + retention.getText() + " dias");
                buildBackupsPane(container, currentUser);
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, "Erro ao guardar programação: " + ex.getMessage());
            }
        });

        backupNowBtn.setOnAction(e -> createBackupNow(container, currentUser));
        openFolderBtn.setOnAction(e -> {
            try {
                Path dir = Paths.get("backups");
                Files.createDirectories(dir);
                java.awt.Desktop.getDesktop().open(dir.toFile());
            } catch (Exception ex) {
                alert(Alert.AlertType.WARNING, "Não foi possível abrir a pasta. Aceda manualmente a ./backups/");
            }
        });

        toolbarCard.getChildren().add(scheduleRow);
        tableCard.getChildren().add(table);

        main.getChildren().addAll(kpiGrid, toolbarCard, tableCard);
        VBox.setMargin(toolbarCard, new Insets(0, 20, 16, 20));
        VBox.setMargin(tableCard, new Insets(0, 20, 20, 20));
        container.getChildren().setAll(main);
    }

    private void createBackupNow(VBox container, User currentUser) {
        if (!SgvDialog.confirmAction("Criar Backup",
                "Deseja criar uma cópia de segurança da base de dados agora?\n\nO processo pode demorar alguns segundos.",
                "▶ Criar Backup")) return;
        try {
            // Correção do BUG-001: backup REAL via mysqldump (BackupService),
            // registado só como COMPLETED se o ficheiro existir com conteúdo —
            // antes copiava um "sgv.db" inexistente (o registo "COMPLETED"
            // nunca correspondia a um backup verdadeiro).
            BackupService.BackupOutcome out = backupService.runBackup(
                    "MANUAL", currentUser != null ? currentUser.getUsername() : "system");
            if (out.success()) {
                auditLogService.log(null, "BACKUP", "sistema", null,
                        "Backup criado: " + out.file().getFileName() + " (" + out.sizeBytes() + " bytes, sha256 " + out.sha256() + ")");
                SgvDialog.info("Backup Criado",
                        "Backup criado com sucesso!\n\n" + out.file().getAbsolutePath() + "\n"
                                + (out.sizeBytes() / 1024) + " KB — SHA-256: " + out.sha256());
            } else {
                // O registo FAILED já foi gravado pelo BackupService
                SgvDialog.error("Erro no Backup",
                        "O backup falhou (registado como FAILED):\n" + out.error());
            }
            buildBackupsPane(container, currentUser);
        } catch (Exception ex) {
            SgvDialog.error("Erro no Backup", "Erro ao criar backup: " + ex.getMessage());
        }
    }

    private void confirmRestore(SystemBackup b, User currentUser) {
        if (!SgvDialog.confirmAction("Restaurar Backup",
                "Restaurar o backup '" + b.getFileName() + "'?\n\nA base de dados actual será substituída após reinício do serviço MariaDB.",
                "↺ Restaurar")) {
            return;
        }
        try {
            Optional<SystemBackup> bkOpt = systemBackupRepository.findByFileName(b.getFileName());
            if (bkOpt.isEmpty()) {
                SgvDialog.error("Erro no Restore", "Registo de backup não encontrado na base de dados.");
                return;
            }
            String storedPath = bkOpt.get().getFilePath();
            Path src = (storedPath != null && !storedPath.isBlank()) ? Paths.get(storedPath) : Paths.get("backups", b.getFileName());
            if (!Files.exists(src)) {
                SgvDialog.error("Erro no Restore", "Ficheiro de backup não encontrado:\n" + src.toAbsolutePath());
                return;
            }
            // Correção do BUG-001: restore REAL — o dump SQL é aplicado ao
            // servidor via cliente mysql, com validação de integridade
            // (SHA-256) antes de aplicar. Antes copiava o ficheiro para
            // "C:\xampp\mysql\data\sgv" (caminho fixo de Windows) e marcava
            // RESTORED sem confirmar que o restore funcionou.
            BackupService.RestoreOutcome out = backupService.restoreFrom(src, bkOpt.get().getChecksumSha256());

            if (out.success()) {
                SystemBackup bk = bkOpt.get();
                bk.setStatus("RESTORED");
                systemBackupRepository.save(bk);
            }
            SystemBackup restoreLog = new SystemBackup();
            restoreLog.setFileName("RESTORE_" + b.getFileName());
            restoreLog.setFilePath(src.toAbsolutePath().toString());
            restoreLog.setBackupType("RESTORE");
            restoreLog.setNotes("Restore de: " + b.getFileName() + " por " + (currentUser != null ? currentUser.getUsername() : "system"));
            restoreLog.setCreatedBy(currentUser != null ? currentUser.getUsername() : "system");
            restoreLog.setStatus(out.success() ? "COMPLETED" : "FAILED");
            if (!out.success()) restoreLog.setErrorMessage(out.detail());
            systemBackupRepository.save(restoreLog);
            auditLogService.log(null, "RESTORE", "backups", null,
                "Restore " + (out.success() ? "com sucesso" : "FALHOU") + " a partir de: " + b.getFileName());

            if (out.success()) {
                SgvDialog.info("Restore Concluído", out.detail());
            } else {
                SgvDialog.error("Restore Falhou", "O restore não foi aplicado:\n" + out.detail());
            }
        } catch (Exception ex) {
            SgvDialog.error("Erro no Restore", "Erro ao restaurar backup: " + ex.getMessage());
        }
    }

    private void confirmDelete(SystemBackup b, User currentUser) {
        if (!SgvDialog.confirmDanger("Eliminar Backup",
                "Eliminar o backup '" + b.getFileName() + "'?\n\nEsta operação não pode ser revertida.")) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get("backups", b.getFileName()));
            systemBackupRepository.findByFileName(b.getFileName()).ifPresent(systemBackupRepository::delete);
            auditLogService.log(null, "ELIMINAR", "backups", null, "Backup eliminado: " + b.getFileName());
        } catch (Exception ex) {
            SgvDialog.error("Erro ao Eliminar", "Erro ao eliminar backup: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PÁGINA: MODO TREINAMENTO
    // ═══════════════════════════════════════════════════════════════════════

    public void buildTreinamentoPane(VBox container, User currentUser) {
        buildTreinamentoPane(container, currentUser, null);
    }

    public void buildTreinamentoPane(VBox container, User currentUser, Runnable onTrainingModeChanged) {
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");

        boolean active = trainingModeService.isTrainingMode();

        VBox hero = new VBox(18);
        hero.setAlignment(Pos.CENTER);
        hero.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1; "
            + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 34; -fx-margin: 20;"
            + (active ? "-fx-border-color: #10B981;" : ""));
        Label badge = new Label(active ? "●  MODO TREINAMENTO ACTIVO" : "○  MODO TREINAMENTO DESACTIVO");
        badge.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-background-color: "
            + (active ? "#10B981" : "#EF4444") + "; -fx-padding: 12 28; -fx-background-radius: 24;");
        Label desc = new Label(active
            ? "Todas as operações de escrita estão SIMULADAS — nenhum dado real é alterado."
            : "O sistema está em operação real (Produção). Active o modo para formar novos operadores em segurança.");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-font-weight: 600;");

        HBox heroButtons = new HBox(12);
        heroButtons.setAlignment(Pos.CENTER);
        Button activateBtn = primaryButton("▶  Activar Modo Treinamento", "#10B981");
        Button deactivateBtn = primaryButton("■  Desactivar Modo Treinamento", "#EF4444");
        activateBtn.setDisable(active);
        deactivateBtn.setDisable(!active);
        Runnable refresh = () -> buildTreinamentoPane(container, currentUser, onTrainingModeChanged);
        activateBtn.setOnAction(e -> toggleTraining(true, currentUser, container, refresh, onTrainingModeChanged));
        deactivateBtn.setOnAction(e -> toggleTraining(false, currentUser, container, refresh, onTrainingModeChanged));
        heroButtons.getChildren().addAll(activateBtn, deactivateBtn);

        hero.getChildren().addAll(badge, desc, heroButtons);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(16);
        infoGrid.setVgap(16);
        infoGrid.setStyle("-fx-padding: 16 20 20 20;");
        ColumnConstraints third = new ColumnConstraints();
        third.setPercentWidth(33.33);
        third.setHgrow(Priority.ALWAYS);
        infoGrid.getColumnConstraints().addAll(third, third, third);

        VBox card1 = card("🔒 Dados Protegidos", null);
        card1.getChildren().addAll(bullet("Vendas, compras e stock NÃO são gravados"),
            bullet("Movimentos de caixa bloqueados com aviso"),
            bullet("Nenhuma factura é emitida para a AT"));
        VBox card2 = card("🟡 Aviso Visual Permanente", null);
        card2.getChildren().addAll(bullet("Indicação clara durante o treino"),
            bullet("Evita confusão com dados reais"),
            bullet("Relatórios continuam disponíveis para consulta"));
        VBox card3 = card("🧪 Simulação Segura", null);
        card3.getChildren().addAll(bullet("Ideal para formar novos operadores de caixa"),
            bullet("Prática livre do PDV e leitor de código de barras"),
            bullet("Erros de aprendizagem sem custo real"));

        infoGrid.add(card1, 0, 0);
        infoGrid.add(card2, 1, 0);
        infoGrid.add(card3, 2, 0);
        GridPane.setVgrow(card1, Priority.ALWAYS);
        GridPane.setVgrow(card2, Priority.ALWAYS);
        GridPane.setVgrow(card3, Priority.ALWAYS);

        VBox auditCard = card("📜 Auditoria do Modo Treinamento", null);
        auditCard.getChildren().add(new Label(
            "Activado por: " + (active ? (currentUser != null ? currentUser.getUsername() : "—") : "—")
            + "\nAs activações/desactivações ficam registadas no módulo Auditoria & Logs."));
        VBox.setVgrow(auditCard, Priority.ALWAYS);

        main.getChildren().addAll(hero, infoGrid, auditCard);
        VBox.setMargin(hero, new Insets(20, 20, 0, 20));
        container.getChildren().setAll(main);
    }

    private void toggleTraining(boolean activate, User currentUser, VBox container, Runnable refresh, Runnable onTrainingModeChanged) {
        try {
            AppConfig c = appConfigService.get();
            c.setDemoMode(activate);
            appConfigService.save(c);
            auditLogService.log(null, activate ? "ACTIVAR" : "DESACTIVAR", "modo_treino", null,
                "Modo treino " + (activate ? "activado" : "desactivado") + " por " + (currentUser != null ? currentUser.getUsername() : "?"));
            if (onTrainingModeChanged != null) onTrainingModeChanged.run();
            alert(Alert.AlertType.INFORMATION, activate
                ? "Modo Treinamento activado!\n\n• Todas as operações são simuladas\n• Nenhum dado real é alterado"
                : "Modo Treinamento desactivado. Sistema de volta à operação real.");
            refresh.run();
        } catch (Exception ex) {
            log.error("Erro ao alternar modo treinamento", ex);
            alert(Alert.AlertType.ERROR, "Erro: " + ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PÁGINA: LICENCIAMENTO
    // ═══════════════════════════════════════════════════════════════════════

    public void buildLicencaPane(VBox container, User currentUser) {
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");

        LicenseService.LicenseResult result;
        try {
            result = licenseService.getCurrentLicenseStatus();
        } catch (Exception ex) {
            result = new LicenseService.LicenseResult(false, "Desconhecido", ex.getMessage(), "—", 0);
        }
        AppConfig cfg = appConfigService.get();

        GridPane kpiGrid = kpiGrid(
            new String[]{"Tipo de Licença", "Estado", "Validade", "Dias Restantes"},
            new String[]{"Modalidade contratada", "Situação da activação", "Data de expiração", "Tempo restante"},
            new String[]{result.getType(),
                result.isValid() ? "Activa ✅" : "Inactiva ⚠️",
                result.getExpiryDate(),
                result.getDaysRemaining() == Integer.MAX_VALUE ? "Ilimitado" : result.getDaysRemaining() + " dias"},
            new String[]{"blue", result.isValid() ? "green" : "red", "purple", "orange"});
        for (int i = 0; i < 4; i++) {
            String v = switch (i) {
                case 1 -> result.isValid() ? "Activa ✅" : "Inactiva ⚠️";
                case 3 -> result.getDaysRemaining() == Integer.MAX_VALUE ? "∞" : String.valueOf(result.getDaysRemaining());
                default -> null;
            };
            if (v != null) updateKpiValue(kpiGrid, i, v);
        }

        HBox content = new HBox(16);
        content.setStyle("-fx-padding: 0 20 20 20;");
        content.setFillHeight(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox activateCard = card("🔑 Activar Licença", "Introduza a chave fornecida pela equipa SGV Moçambique");
        TextField keyField = new TextField();
        keyField.setPromptText("XXXX-XXXX-XXXX-XXXX");
        keyField.setStyle("-fx-font-size: 15px; -fx-padding: 10 14; -fx-background-radius: 6; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-font-weight: 700; -fx-alignment: center;");
        Label feedback = new Label("");
        feedback.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-wrap-text: true;");
        Button activateBtn = primaryButton("✔  Activar Licença", "#2563EB");
        activateBtn.setMaxWidth(Double.MAX_VALUE);
        activateBtn.setOnAction(e -> {
            String key = keyField.getText();
            if (key == null || key.isBlank() || key.trim().length() < 10) {
                feedback.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444; -fx-font-weight: 700; -fx-wrap-text: true;");
                feedback.setText("Chave inválida. Verifique e tente novamente.");
                return;
            }
            LicenseService.LicenseResult r = licenseService.activateLicense(key.trim());
            if (r.isValid()) {
                auditLogService.log(null, "ACTIVAR", "licenca", null,
                    "Licença activada: tipo=" + r.getType() + ", expira=" + r.getExpiryDate());
                alert(Alert.AlertType.INFORMATION, "Licença activada com sucesso!\n\nTipo: " + r.getType()
                    + "\nValidade: " + r.getExpiryDate()
                    + "\nDias restantes: " + (r.getDaysRemaining() == Integer.MAX_VALUE ? "Ilimitado" : r.getDaysRemaining()));
                buildLicencaPane(container, currentUser);
            } else {
                feedback.setStyle("-fx-font-size: 12px; -fx-text-fill: #EF4444; -fx-font-weight: 700; -fx-wrap-text: true;");
                feedback.setText("Licença inválida: " + r.getMessage());
            }
        });

        VBox benefitsCard = card("⭐ Benefícios da Licença Comercial", null);
        benefitsCard.getChildren().addAll(
            bullet("Actualizações e suporte técnico prioritário"),
            bullet("Emissão ilimitada de documentos fiscais certificados"),
            bullet("Exportação SAF-T MZ garantida para submissão AT"),
            bullet("Backup automático programado e multi-filial"));

        VBox machineCard = card("🖥️ Identificação da Instalação", "Dados técnicos para emissão da chave");
        TextField machineId = new TextField();
        try {
            machineId.setText(licenseService.getMachineId());
        } catch (Exception ex) {
            machineId.setText("Não disponível");
        }
        machineId.setEditable(false);
        machineId.setStyle("-fx-font-size: 12px; -fx-font-family: monospace; -fx-padding: 8 10; -fx-background-color: #F1F5F9; -fx-background-radius: 6;");
        machineId.setMaxWidth(Double.MAX_VALUE);

        Label licensedTo = new Label(cfg.getCompanyName() != null && !cfg.getCompanyName().isBlank() ? cfg.getCompanyName() : "—");
        licensedTo.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0F172A;");
        Label nuitLbl = new Label("NUIT: " + (cfg.getCompanyNuit() != null && !cfg.getCompanyNuit().isBlank() ? cfg.getCompanyNuit() : "—"));
        nuitLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        activateCard.getChildren().addAll(keyField, feedback, activateBtn);
        benefitsCard.getChildren().add(new Label(""));
        machineCard.getChildren().addAll(
            labeledField("Identificador da Máquina (Machine ID)", machineId),
            new Label("Licenciado a:"),
            licensedTo,
            nuitLbl);
        VBox.setVgrow(machineCard, Priority.ALWAYS);

        VBox leftCol = new VBox(16, activateCard);
        VBox leftColWrap = new VBox(leftCol);
        VBox rightCol = new VBox(16, benefitsCard, machineCard);
        HBox.setHgrow(leftColWrap, Priority.ALWAYS);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        VBox.setVgrow(leftCol, Priority.ALWAYS);
        content.getChildren().addAll(leftColWrap, rightCol);

        main.getChildren().addAll(kpiGrid, content);
        container.getChildren().setAll(main);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PÁGINA: SEGURANÇA (PALAVRA-PASSE)
    // ═══════════════════════════════════════════════════════════════════════

    public void buildSegurancaPane(VBox container, User currentUser) {
        VBox main = new VBox(0);
        main.setStyle("-fx-background-color: #F8FAFC;");

        HBox content = new HBox(16);
        content.setStyle("-fx-padding: 20 20 20 20;");
        content.setFillHeight(true);
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox pwdCard = card("🔒 Alterar a Minha Palavra-passe",
            "A palavra-passe é armazenada com encriptação BCrypt — ninguém a pode ler");
        PasswordField currentPwd = passwordField("Palavra-passe actual");
        PasswordField newPwd = passwordField("Nova palavra-passe (mínimo 6 caracteres)");
        PasswordField confirmPwd = passwordField("Confirmar nova palavra-passe");
        Label errorLabel = new Label("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: 700; -fx-font-size: 12px; -fx-wrap-text: true;");

        currentPwd.setOnAction(e -> newPwd.requestFocus());
        newPwd.setOnAction(e -> confirmPwd.requestFocus());
        confirmPwd.setOnAction(e -> submitPasswordChange(currentUser, currentPwd, newPwd, confirmPwd, errorLabel, container));

        Button submitBtn = primaryButton("✔ Confirmar Alteração", "#2563EB");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> submitPasswordChange(currentUser, currentPwd, newPwd, confirmPwd, errorLabel, container));

        VBox fields = new VBox(14, labeledField("Palavra-passe Actual *", currentPwd),
            labeledField("Nova Palavra-passe *", newPwd),
            labeledField("Confirmar Nova Palavra-passe *", confirmPwd),
            errorLabel);
        pwdCard.getChildren().addAll(fields, submitBtn);
        pwdCard.setMaxWidth(560);

        VBox policyCard = card("🛡️ Política de Palavras-passe Recomendada", null);
        policyCard.getChildren().addAll(
            bullet("Mínimo obrigatório: 6 caracteres (sistema)"),
            bullet("Recomendado: 10+ caracteres com letras, números e símbolos"),
            bullet("Nunca reutilize palavras-passe de outros serviços"),
            bullet("Altere a palavra-passe padrão admin/admin imediatamente após instalação"),
            bullet("Em caso de suspeita, o administrador deve desactivar o utilizador"));

        VBox sessionCard = card("👤 Sessão Actual", null);
        if (currentUser != null) {
            sessionCard.getChildren().addAll(
                infoLine("Utilizador", currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername()),
                infoLine("Username", currentUser.getUsername()),
                infoLine("Perfil / Roles", currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()
                    ? currentUser.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.joining(", ")) : "—"),
                infoLine("Filial", currentUser.getBranch() != null && currentUser.getBranch().getName() != null ? currentUser.getBranch().getName() : "—"));
        } else {
            sessionCard.getChildren().add(new Label("Sessão não disponível."));
        }
        VBox.setVgrow(sessionCard, Priority.ALWAYS);

        VBox rightCol = new VBox(16, policyCard, sessionCard);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        VBox.setVgrow(pwdCard, Priority.ALWAYS);
        content.getChildren().addAll(pwdCard, rightCol);

        main.getChildren().add(content);
        container.getChildren().setAll(main);
    }

    private void submitPasswordChange(User currentUser, PasswordField currentPwd, PasswordField newPwd,
                                      PasswordField confirmPwd, Label errorLabel, VBox container) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        String current = currentPwd.getText();
        String novo = newPwd.getText();
        String confirm = confirmPwd.getText();

        if (current == null || current.isBlank()) { showPwdError(errorLabel, "Preencha a palavra-passe actual."); return; }
        if (novo == null || novo.isBlank()) { showPwdError(errorLabel, "Preencha a nova palavra-passe."); return; }
        if (novo.length() < 6) { showPwdError(errorLabel, "A nova palavra-passe deve ter pelo menos 6 caracteres."); return; }
        if (!novo.equals(confirm)) { showPwdError(errorLabel, "As palavras-passe não coincidem."); return; }
        if (currentUser == null) { showPwdError(errorLabel, "Sessão de utilizador não disponível."); return; }
        if (authService.authenticate(currentUser.getUsername(), current) == null) {
            showPwdError(errorLabel, "Palavra-passe actual incorrecta.");
            return;
        }
        try {
            currentUser.setPasswordHash(passwordEncoder.encode(novo));
            userRepository.save(currentUser);
            auditLogService.log(currentUser.getId(), "ALTERAR_SENHA", "users", currentUser.getId(),
                "Senha alterada por " + currentUser.getUsername());
            alert(Alert.AlertType.INFORMATION, "Palavra-passe alterada com sucesso!");
            buildSegurancaPane(container, currentUser);
        } catch (Exception ex) {
            showPwdError(errorLabel, "Erro ao guardar palavra-passe: " + ex.getMessage());
        }
    }

    private void showPwdError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private GridPane kpiGrid(String[] titles, String[] subtitles, String[] values, String[] colors) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setStyle("-fx-padding: 20; -fx-background-color: #F8FAFC;");
        for (int i = 0; i < titles.length; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / titles.length);
            grid.getColumnConstraints().add(col);
        }
        for (int i = 0; i < titles.length; i++) {
            VBox card = kpiCard(titles[i], values[i], subtitles[i], colors[i]);
            GridPane.setConstraints(card, i, 0);
            grid.getChildren().add(card);
        }
        return grid;
    }

    private VBox kpiCard(String title, String value, String subtitle, String colorType) {
        String bgColor, iconBg, iconEmoji;
        switch (colorType) {
            case "green": bgColor = "#D1FAE5"; iconBg = "#10B981"; iconEmoji = "💰"; break;
            case "orange": bgColor = "#FEF3C7"; iconBg = "#F59E0B"; iconEmoji = "⚠"; break;
            case "purple": bgColor = "#EDE9FE"; iconBg = "#7C3AED"; iconEmoji = "📊"; break;
            case "red": bgColor = "#FEE2E2"; iconBg = "#EF4444"; iconEmoji = "🔴"; break;
            default: bgColor = "#DBEAFE"; iconBg = "#2563EB"; iconEmoji = "📦";
        }
        VBox card = new VBox();
        card.getStyleClass().add("card-pane");
        card.setStyle("-fx-padding: 20; -fx-min-height: 100;");
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(iconEmoji);
        icon.setStyle("-fx-font-size: 24px; -fx-min-width: 52px; -fx-min-height: 52px; -fx-alignment: center; -fx-background-radius: 12; -fx-background-color: " + bgColor + "; -fx-padding: 10;");
        VBox info = new VBox(4);
        info.setStyle("-fx-min-width: 0;");
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        Label valueLbl = new Label(value);
        valueLbl.setId("kpi-" + title.replaceAll("\\s+", "").toLowerCase());
        valueLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: #0F172A;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-wrap-text: true;");
        info.getChildren().addAll(titleLbl, valueLbl, subLbl);
        row.getChildren().addAll(icon, info);
        card.getChildren().add(row);
        return card;
    }

    private void updateKpiValue(GridPane grid, int index, String value) {
        if (grid == null || index >= grid.getChildren().size()) return;
        javafx.scene.Node cardNode = grid.getChildren().get(index);
        if (cardNode instanceof VBox card && !card.getChildren().isEmpty()
                && card.getChildren().get(0) instanceof HBox row && row.getChildren().size() >= 2
                && row.getChildren().get(1) instanceof VBox info) {
            for (javafx.scene.Node n : info.getChildren()) {
                if (n instanceof Label l && l.getId() != null && l.getId().startsWith("kpi-")) {
                    l.setText(value);
                    return;
                }
            }
        }
    }

    private VBox card(String title, String subtitle) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 18 20;");
        VBox header = new VBox(3);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        header.getChildren().add(t);
        if (subtitle != null) {
            Label s = new Label(subtitle);
            s.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-wrap-text: true;");
            header.getChildren().add(s);
        }
        card.getChildren().add(header);
        return card;
    }

    private VBox labeledField(String labelText, Region input) {
        VBox box = new VBox(5);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #475569;");
        box.getChildren().addAll(lbl, input);
        return box;
    }

    private HBox infoLine(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 0; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0;");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    private Label bullet(String text) {
        Label l = new Label("•  " + text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-wrap-text: true;");
        return l;
    }

    private TextField textField(String value) {
        TextField tf = new TextField(value != null ? value : "");
        tf.setStyle("-fx-font-size: 12px; -fx-padding: 8 10; -fx-background-radius: 6; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private TextField numericField(String value) {
        TextField tf = textField(value);
        tf.setPrefWidth(110);
        return tf;
    }

    private PasswordField passwordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle("-fx-font-size: 13px; -fx-padding: 10 12; -fx-background-radius: 6; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");
        pf.setMaxWidth(Double.MAX_VALUE);
        return pf;
    }

    private Button primaryButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-padding: 9 18; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-color: " + color + "; -fx-text-fill: #ffffff; -fx-cursor: hand;");
        UiUtils.applyHoverElevation(btn);
        UiUtils.applyPressFeedback(btn);
        return btn;
    }

    private Button secondaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-padding: 9 18; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-color: #FFFFFF; -fx-text-fill: #0F172A; -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-cursor: hand;");
        UiUtils.applyHoverElevation(btn);
        UiUtils.applyPressFeedback(btn);
        return btn;
    }

    private <T> Callback<TableView<T>, TableRow<T>> stripedRows() {
        return tv -> {
            TableRow<T> row = new TableRow<>();
            Runnable apply = () -> {
                if (row.isEmpty()) row.setStyle("");
                else if (row.isSelected() || row.isHover()) row.setStyle("-fx-background-color:#DBEAFE; -fx-border-color:#BFDBFE; -fx-border-width:0 0 1 0;");
                else row.setStyle(row.getIndex() % 2 == 0
                    ? "-fx-background-color:#ffffff; -fx-border-color:#F1F5F9; -fx-border-width:0 0 1 0;"
                    : "-fx-background-color:#F8FAFC; -fx-border-color:#F1F5F9; -fx-border-width:0 0 1 0;");
            };
            row.hoverProperty().addListener((obs, o, h) -> apply.run());
            row.selectedProperty().addListener((obs, o, s) -> apply.run());
            return row;
        };
    }

    private String fmtDouble(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.valueOf(v).replace(",", ".");
    }

    private double parseDoubleSafe(String raw, double fallback) {
        try {
            return Double.parseDouble(raw != null ? raw.trim().replace(",", ".") : "");
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parseIntSafe(String raw, int fallback) {
        try {
            return Integer.parseInt(raw != null ? raw.trim() : "");
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void alert(Alert.AlertType type, String message) {
        switch (type) {
            case ERROR -> SgvDialog.error("Erro", message);
            case WARNING -> SgvDialog.warning("Atenção", message);
            case CONFIRMATION -> SgvDialog.confirm("Confirmar", message);
            default -> SgvDialog.info("Informação", message);
        }
    }
}
