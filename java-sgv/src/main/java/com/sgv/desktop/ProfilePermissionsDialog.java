package com.sgv.desktop;

import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.RoleRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class ProfilePermissionsDialog {

    private static final Logger log = LoggerFactory.getLogger(ProfilePermissionsDialog.class);

    private final RoleRepository roleRepository;

    public ProfilePermissionsDialog(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void show(Role initialRole, Runnable onSaved, User currentUser) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("SGV — Gestão de Perfis & Matriz de Permissões (RBAC)");
        dialog.setResizable(true);
        dialog.setWidth(880);
        dialog.setHeight(680);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #F8FAFC;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
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

        VBox profileConfigCard = new VBox(10);
        profileConfigCard.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 16 20; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        HBox profileRow = new HBox(12);
        profileRow.setAlignment(Pos.CENTER_LEFT);

        Label lblPerfilSel = new Label("Perfil Selecionado:");
        lblPerfilSel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #0F172A;");

        ComboBox<Role> roleCombo = new ComboBox<>();
        UiUtils.hardenComboBox(roleCombo);
        roleCombo.setStyle("-fx-pref-width: 220; -fx-font-size: 12px; -fx-font-weight: 700;");
        List<Role> allRoles = roleRepository.findAll();
        roleCombo.setItems(javafx.collections.FXCollections.observableArrayList(allRoles));

        Button btnNovoPerfil = new Button("➕ Novo Perfil");
        btnNovoPerfil.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-border-color: #93C5FD; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12px; -fx-padding: 6 12; -fx-cursor: hand;");

        profileRow.getChildren().addAll(lblPerfilSel, roleCombo, btnNovoPerfil);

        HBox fieldsRow = new HBox(16);
        fieldsRow.setAlignment(Pos.CENTER_LEFT);

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

        Map<String, CheckBox> checkMap = new LinkedHashMap<>();

        java.util.function.BiFunction<String, String, CheckBox> mkCheck = (perm, text) -> {
            CheckBox cb = new CheckBox(text);
            cb.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
            checkMap.put(perm, cb);
            return cb;
        };

        GridPane matrixGrid = new GridPane();
        matrixGrid.setHgap(16);
        matrixGrid.setVgap(16);
        matrixGrid.setStyle("-fx-padding: 20;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        matrixGrid.getColumnConstraints().addAll(col1, col2);

        java.util.function.Function<String, VBox> mkModuleCard = (modTitle) -> {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 14 16; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 6, 0, 0, 2);");
            Label t = new Label(modTitle);
            t.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #1E40AF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0; -fx-padding: 0 0 6 0;");
            card.getChildren().add(t);
            return card;
        };

        VBox cardVendas = mkModuleCard.apply("⚡ 1. Vendas & Facturação (PDV)");
        cardVendas.getChildren().addAll(
            mkCheck.apply("VENDAS:VIEW", "Visualizar Facturação & Vendas"),
            mkCheck.apply("VENDAS:CREATE", "Emitir Vendas / Facturas / PDV"),
            mkCheck.apply("VENDAS:DELETE", "Anular / Cancelar Documentos de Venda"),
            mkCheck.apply("VENDAS:DISCOUNT", "Conceder Descontos Especiais")
        );
        matrixGrid.add(cardVendas, 0, 0);

        VBox cardCaixa = mkModuleCard.apply("💼 2. Caixa & Tesouraria");
        cardCaixa.getChildren().addAll(
            mkCheck.apply("CAIXA:VIEW", "Visualizar Sessões de Caixa"),
            mkCheck.apply("CAIXA:CREATE", "Abrir & Fechar Turno de Caixa"),
            mkCheck.apply("CAIXA:CASH_MOVEMENT", "Efetuar Sangrias e Reforços"),
            mkCheck.apply("FINANCEIRO:VIEW", "Registar Despesas & Resumo Financeiro")
        );
        matrixGrid.add(cardCaixa, 1, 0);

        VBox cardStock = mkModuleCard.apply("📊 3. Stock & Artigos");
        cardStock.getChildren().addAll(
            mkCheck.apply("STOCK:VIEW", "Consultar Inventário em Loja"),
            mkCheck.apply("PRODUTOS:CREATE", "Cadastrar & Editar Artigos / Preços"),
            mkCheck.apply("STOCK:CREATE", "Ajustes de Inventário / Quebras"),
            mkCheck.apply("CATALOGOS:VIEW", "Gerir Famílias & Categorias")
        );
        matrixGrid.add(cardStock, 0, 1);

        VBox cardArmazens = mkModuleCard.apply("🏢 4. Armazém & Compras");
        cardArmazens.getChildren().addAll(
            mkCheck.apply("ARMAZENS:VIEW", "Consultar Stock em Armazéns Centrais"),
            mkCheck.apply("COMPRAS:CREATE", "Registar Facturas de Compra"),
            mkCheck.apply("TRANSFERENCIAS:VIEW", "Transferências de Stock Armazém ↔ Loja"),
            mkCheck.apply("COMPRAS:VIEW", "Gestão de Fornecedores & Pagamentos")
        );
        matrixGrid.add(cardArmazens, 1, 1);

        VBox cardClientes = mkModuleCard.apply("👥 5. Gestão de Clientes");
        cardClientes.getChildren().addAll(
            mkCheck.apply("CLIENTES:VIEW", "Consultar Ficha de Clientes"),
            mkCheck.apply("CLIENTES:CREATE", "Criar & Editar Clientes"),
            mkCheck.apply("CLIENTES:CREDIT", "Gerir Limites de Crédito & Saldos")
        );
        matrixGrid.add(cardClientes, 0, 2);

        VBox cardProducao = mkModuleCard.apply("🥖 6. Produção & Fabrico");
        cardProducao.getChildren().addAll(
            mkCheck.apply("PRODUCAO:VIEW", "Consultar Ordens de Produção"),
            mkCheck.apply("PRODUCAO:CREATE", "Criar & Concluir Fabrico / Padaria")
        );
        matrixGrid.add(cardProducao, 1, 2);

        VBox cardRelatorios = mkModuleCard.apply("📈 7. Mapas & Relatórios");
        cardRelatorios.getChildren().addAll(
            mkCheck.apply("RELATORIOS:VIEW", "Visualizar Mapas Fiscais & Vendas"),
            mkCheck.apply("RELATORIOS:CREATE", "Exportar Ficheiro SAF-T MZ")
        );
        matrixGrid.add(cardRelatorios, 0, 3);

        VBox cardSistema = mkModuleCard.apply("⚙️ 8. Configurações & Sistema");
        cardSistema.getChildren().addAll(
            mkCheck.apply("SISTEMA:VIEW", "Parâmetros da Empresa & Licenciamento"),
            mkCheck.apply("SISTEMA:CREATE", "Gestão de Utilizadores & Permissões"),
            mkCheck.apply("SISTEMA:DELETE", "Cópias de Segurança (Backups)")
        );
        matrixGrid.add(cardSistema, 1, 3);
        matrixGrid.setPadding(new Insets(20));

        ScrollPane scroll = new ScrollPane(matrixGrid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F8FAFC; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox presetsBar = new HBox(8);
        presetsBar.setAlignment(Pos.CENTER_LEFT);
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

        Consumer<Role> loadRoleIntoUI = (r) -> {
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

        if (initialRole != null) {
            roleCombo.getSelectionModel().select(initialRole);
        } else if (!allRoles.isEmpty()) {
            roleCombo.getSelectionModel().selectFirst();
        }

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
                log.info("Perfil '{}' guardado com {} permissões por {}", target.getName(), selectedPerms.size(),
                    currentUser != null ? currentUser.getUsername() : "?");
                showAlert(Alert.AlertType.INFORMATION, "Perfil '" + target.getName() + "' e permissões salvas com sucesso!");
                if (onSaved != null) onSaved.run();
                dialog.close();
            } catch (Exception ex) {
                log.error("Erro ao guardar perfil", ex);
                showAlert(Alert.AlertType.ERROR, "Erro ao guardar perfil: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(header, profileConfigCard, scroll, presetsBar);
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String message) {
        switch (type) {
            case ERROR -> SgvDialog.error("Erro", message);
            case WARNING -> SgvDialog.warning("Atenção", message);
            default -> SgvDialog.info("Informação", message);
        }
    }
}
