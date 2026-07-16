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
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class CashSessionController {

    @FXML private Label statusBadge;
    @FXML private Label openedAtLabel;
    @FXML private Label initialValueLabel;
    @FXML private Label systemValueLabel;
    
    @FXML private Button openSessionButton;
    @FXML private Button addMovementButton;
    @FXML private Button closeSessionButton;
    
    @FXML private TableView<CashMovement> movementsTable;
    @FXML private TableColumn<CashMovement, String> timeColumn;
    @FXML private TableColumn<CashMovement, String> typeColumn;
    @FXML private TableColumn<CashMovement, String> amountColumn;
    @FXML private TableColumn<CashMovement, String> descriptionColumn;
    @FXML private TableColumn<CashMovement, String> userColumn;

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
        amountColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                String.format("%.2f", cell.getValue().getAmount())));
        descriptionColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getDescription()));
        userColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getCreatedBy() != null ? cell.getValue().getCreatedBy().getFullName() : ""));

        openSessionButton.setOnAction(e -> openSessionModal());
        addMovementButton.setOnAction(e -> addMovementModal());
        closeSessionButton.setOnAction(e -> closeSessionModal());
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
            for (CashMovement m : movements) {
                if ("IN".equals(m.getType())) total = total.add(m.getAmount());
                else total = total.subtract(m.getAmount());
            }
            systemValueLabel.setText(String.format("%.2f MT", total));
            
            movementsTable.setItems(FXCollections.observableArrayList(movements));
            
            openSessionButton.setDisable(true);
            addMovementButton.setDisable(false);
            closeSessionButton.setDisable(false);
        } else {
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
        
        if (onStateChange != null) {
            onStateChange.run();
        }
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
            ex.printStackTrace();
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
            ex.printStackTrace();
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
            ex.printStackTrace();
        }
    }
}
