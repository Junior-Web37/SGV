package com.sgv.desktop;

import com.sgv.entity.CashMovement;
import com.sgv.entity.CashSession;
import com.sgv.entity.User;
import com.sgv.service.CashSessionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CashSessionController {

    private static final Logger log = LoggerFactory.getLogger(CashSessionController.class);

    @FXML private Label statusBadge;
    @FXML private Label openedAtLabel;
    @FXML private Label operatorLabel;
    @FXML private Label branchLabel;
    @FXML private Label initialValueLabel;
    @FXML private Label systemValueLabel;
    @FXML private Label totalEntriesLabel;
    @FXML private Label totalExitsLabel;
    @FXML private Label movementCountLabel;
    @FXML private Label lastMovementLabel;
    @FXML private Label statusSummaryLabel;
    
    @FXML private Button openSessionButton;
    @FXML private Button addMovementButton;
    @FXML private Button closeSessionButton;
    @FXML private Button refreshMovementsButton;
    @FXML private Button refreshHistoryButton;
    @FXML private Button viewSessionButton;
    @FXML private Label movementsHintLabel;

    @FXML private VBox closedStatePane;
    @FXML private VBox openStatePane;
    
    @FXML private TableView<CashMovement> movementsTable;
    @FXML private TableColumn<CashMovement, String> timeColumn;
    @FXML private TableColumn<CashMovement, String> typeColumn;
    @FXML private TableColumn<CashMovement, String> operationKindColumn;
    @FXML private TableColumn<CashMovement, String> reasonColumn;
    @FXML private TableColumn<CashMovement, String> amountColumn;
    @FXML private TableColumn<CashMovement, String> descriptionColumn;
    @FXML private TableColumn<CashMovement, String> userColumn;

    @FXML private TableView<CashSession> historyTable;
    @FXML private TableColumn<CashSession, String> historyOpenedColumn;
    @FXML private TableColumn<CashSession, String> historyClosedColumn;
    @FXML private TableColumn<CashSession, String> historyStateColumn;
    @FXML private TableColumn<CashSession, String> historyInitialValueColumn;
    @FXML private TableColumn<CashSession, String> historySystemValueColumn;
    @FXML private TableColumn<CashSession, String> historyReportedValueColumn;
    @FXML private TableColumn<CashSession, String> historyOperatorColumn;

    private final CashSessionService cashSessionService;
    private final ApplicationContext applicationContext;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private User currentUser;
    private CashSession activeSession;
    private Runnable onStateChange;

    public CashSessionController(CashSessionService cashSessionService, ApplicationContext applicationContext) {
        this.cashSessionService = cashSessionService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        timeColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedAt() != null ? cell.getValue().getCreatedAt().format(TIME_FORMATTER) : ""));
        typeColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                "IN".equals(cell.getValue().getType()) ? "🟢 ENTRADA" : "🔴 SAÍDA"));
        operationKindColumn.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getOperationKind() != null ? cell.getValue().getOperationKind().getLabel() : ""));
        amountColumn.setCellValueFactory(cell -> new SimpleStringProperty(
            String.format("%.2f", cell.getValue().getAmount())));
        reasonColumn.setCellValueFactory(cell -> new SimpleStringProperty(
            cell.getValue().getReason() != null ? cell.getValue().getReason() : ""));
        descriptionColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDescription()));
        userColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedBy() != null ? cell.getValue().getCreatedBy().getFullName() : ""));

        UiUtils.attachSafe(openSessionButton, this::openSessionModal, null, "CASH_SESSION_OPEN");
        UiUtils.attachSafe(addMovementButton, this::addMovementModal, null, "CASH_SESSION_ADD_MOVEMENT");
        UiUtils.attachSafe(closeSessionButton, this::closeSessionModal, null, "CASH_SESSION_CLOSE");
        UiUtils.attachSafe(refreshMovementsButton, this::refreshState, null, "CASH_SESSION_REFRESH_MOVEMENTS");
        UiUtils.attachSafe(refreshHistoryButton, this::loadHistory, null, "CASH_SESSION_REFRESH_HISTORY");
        UiUtils.attachSafe(viewSessionButton, this::showSelectedHistorySession, null, "CASH_SESSION_VIEW_HISTORY");

        for (Button b : List.of(openSessionButton, addMovementButton, closeSessionButton, refreshMovementsButton, refreshHistoryButton, viewSessionButton)) {
            UiUtils.applyHoverElevation(b);
            UiUtils.applyPressFeedback(b);
        }

        setupHistoryTable();
        loadHistory();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        refreshState();
    }

    public void setOnStateChange(Runnable onStateChange) {
        this.onStateChange = onStateChange;
    }

    public void refreshState() {
        if (currentUser == null) return;
        
        String operatorName = currentUser.getFullName() != null ? currentUser.getFullName() : "-";
        String branchName = currentUser.getBranch() != null && currentUser.getBranch().getName() != null
                ? currentUser.getBranch().getName() : "-";
        operatorLabel.setText(operatorName);
        branchLabel.setText(branchName);
        
        Optional<CashSession> sessionOpt = cashSessionService.getOpenSession(currentUser);
        if (sessionOpt.isPresent()) {
            activeSession = sessionOpt.get();
            statusBadge.setText("🟢  CAIXA ABERTO");
            statusBadge.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #10b981; -fx-padding: 6 14; -fx-background-radius: 20; -fx-font-weight: 700; -fx-font-size: 13px;");
            openedAtLabel.setText("Aberto desde: " + activeSession.getOpenedAt().format(DATETIME_FORMATTER));
            
            initialValueLabel.setText(String.format("%.2f MT", activeSession.getInitialValue()));
            
            // Re-calculate the system value
            List<CashMovement> movements = cashSessionService.getMovements(activeSession);
            BigDecimal total = activeSession.getInitialValue();
            BigDecimal totalIn = BigDecimal.ZERO;
            BigDecimal totalOut = BigDecimal.ZERO;
            String lastMovementText = "-";
            for (CashMovement m : movements) {
                if ("IN".equals(m.getType())) {
                    total = total.add(m.getAmount());
                    totalIn = totalIn.add(m.getAmount());
                } else {
                    total = total.subtract(m.getAmount());
                    totalOut = totalOut.add(m.getAmount());
                }
                lastMovementText = m.getDescription() != null && !m.getDescription().isBlank()
                        ? m.getDescription() : m.getType();
            }
            systemValueLabel.setText(String.format("%.2f MT", total));
            totalEntriesLabel.setText(String.format("%.2f MT", totalIn));
            totalExitsLabel.setText(String.format("%.2f MT", totalOut));
            movementCountLabel.setText(String.valueOf(movements.size()));
            lastMovementLabel.setText(lastMovementText);
            statusSummaryLabel.setText("Turno aberto — controle completo do caixa");
            
            movementsTable.setItems(FXCollections.observableArrayList(movements));
            movementsHintLabel.setText("Movimentos do turno aberto.");
            
            openSessionButton.setDisable(true);
            addMovementButton.setDisable(false);
            closeSessionButton.setDisable(false);
        } else {
            movementsHintLabel.setText("Abra o caixa para registar movimentos.");
            totalEntriesLabel.setText("0.00 MT");
            totalExitsLabel.setText("0.00 MT");
            movementCountLabel.setText("0");
            lastMovementLabel.setText("-");
            statusSummaryLabel.setText("Nenhum turno em andamento");
            activeSession = null;
            statusBadge.setText("🔴  FECHADO");
            statusBadge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444; -fx-padding: 6 14; -fx-background-radius: 20; -fx-font-weight: 700; -fx-font-size: 13px;");
            openedAtLabel.setText("Nenhum turno aberto de momento.");
            initialValueLabel.setText("0.00 MT");
            systemValueLabel.setText("0.00 MT");
            movementsTable.setItems(FXCollections.observableArrayList());
            
            openSessionButton.setDisable(false);
            addMovementButton.setDisable(true);
            closeSessionButton.setDisable(true);
        }

        if (closedStatePane != null) {
            boolean opened = activeSession != null;
            closedStatePane.setVisible(!opened);
            closedStatePane.setManaged(!opened);
        }
        if (openStatePane != null) {
            boolean opened = activeSession != null;
            openStatePane.setVisible(opened);
            openStatePane.setManaged(opened);
        }
        
        if (onStateChange != null) {
            onStateChange.run();
        }
        loadHistory();
    }

    private void setupHistoryTable() {
        historyOpenedColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getOpenedAt() != null ? cell.getValue().getOpenedAt().format(DATETIME_FORMATTER) : "-"));
        historyClosedColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getClosedAt() != null ? cell.getValue().getClosedAt().format(DATETIME_FORMATTER) : "-"));
        historyStateColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getState() != null ? cell.getValue().getState() : "-"));
        historyInitialValueColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f MT", cell.getValue().getInitialValue() != null ? cell.getValue().getInitialValue() : BigDecimal.ZERO)));
        historySystemValueColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSystemValue() != null ? String.format("%.2f MT", cell.getValue().getSystemValue()) : "-") );
        historyReportedValueColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getReportedValue() != null ? String.format("%.2f MT", cell.getValue().getReportedValue()) : "-"));
        historyOperatorColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getUser() != null && cell.getValue().getUser().getFullName() != null ? cell.getValue().getUser().getFullName() : "-"));
        historyTable.setItems(FXCollections.observableArrayList());
    }


    private void loadHistory() {
        if (currentUser == null) return;
        historyTable.setItems(FXCollections.observableArrayList(cashSessionService.findSessionsWithUser(currentUser)));
    }

    private void showSelectedHistorySession() {
        CashSession selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            SgvDialog.warning("Histórico de Turnos", "Selecione um turno para ver os detalhes.");
            return;
        }
        showHistoryDetails(selected);
    }

    private void showHistoryDetails(CashSession session) {
        List<CashMovement> movements = cashSessionService.getMovements(session);

        List<Map<String, String>> movRows = new java.util.ArrayList<>();
        double totalIn = 0, totalOut = 0;
        for (CashMovement m : movements) {
            double amt = m.getAmount() != null ? m.getAmount().doubleValue() : 0.0;
            boolean isIn = "IN".equals(m.getType());
            if (isIn) totalIn += amt; else totalOut += amt;
            Map<String, String> row = new java.util.LinkedHashMap<>();
            row.put("Hora", m.getCreatedAt() != null ? m.getCreatedAt().format(TIME_FORMATTER) : "-");
            row.put("Tipo", isIn ? "ENTRADA" : "SAÍDA");
            row.put("Valor", String.format("%.2f MT", amt));
            row.put("Descrição", m.getDescription() != null ? m.getDescription() : "-");
            movRows.add(row);
        }

        String estado = session.getState() != null ? session.getState() : "-";
        boolean aberta = "ABERTA".equalsIgnoreCase(estado);

        javafx.stage.Window owner = historyTable.getScene() != null ? historyTable.getScene().getWindow() : null;
        DetailDialog.create(owner)
                .title("Detalhes do Turno")
                .subtitle("Sessão de Caixa #" + (session.getId() != null ? session.getId() : "—"))
                .statusBadge(estado, aberta ? "#10B981" : "#64748B")
                .width(640).height(580)
                .section("Resumo do Turno")
                .field("Operador", session.getUser() != null ? session.getUser().getFullName() : "—")
                .field("Aberto em", session.getOpenedAt() != null ? session.getOpenedAt().format(DATETIME_FORMATTER) : "—")
                .field("Fechado em", session.getClosedAt() != null ? session.getClosedAt().format(DATETIME_FORMATTER) : "—")
                .field("Fundo Inicial", session.getInitialValue() != null ? String.format("%.2f MT", session.getInitialValue()) : "0.00 MT")
                .section("Valores")
                .field("Valor Sistema", session.getSystemValue() != null ? String.format("%.2f MT", session.getSystemValue()) : "—", "#2563EB")
                .field("Valor Reportado", session.getReportedValue() != null ? String.format("%.2f MT", session.getReportedValue()) : "—")
                .field("Total Entradas", String.format("%.2f MT", totalIn), "#10B981")
                .field("Total Saídas", String.format("%.2f MT", totalOut), "#DC2626")
                .tableSection("Movimentos de Caixa (" + movements.size() + ")",
                        new String[]{"Hora", "Tipo", "Valor", "Descrição"}, movRows)
                .show();
    }

    private void openSessionModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/open_session_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            OpenSessionFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setOnSuccess(this::refreshState);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(openSessionButton.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    private void addMovementModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cash_movement_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            CashMovementFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setOnSuccess(this::refreshState);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(addMovementButton.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }

    private void closeSessionModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/close_session_form.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            CloseSessionFormController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setExpectedValue(new BigDecimal(systemValueLabel.getText().replace(" MT", "").replace(",", ".")));
            controller.setOnSuccess(this::refreshState);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(closeSessionButton.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception ex) {
            log.error("Erro inesperado", ex);
        }
    }
}
