package com.sgv.desktop;

import com.sgv.service.SystemLogService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class UiUtils {

    private static final Logger log = LoggerFactory.getLogger(UiUtils.class);
    private static final java.util.concurrent.ExecutorService bgPool = java.util.concurrent.Executors.newFixedThreadPool(8, r -> {
        Thread t = new Thread(r);
        t.setName("sgv-ui-bg-pool-" + t.getId());
        t.setDaemon(true);
        return t;
    });

    private UiUtils() {}

    public static void runAsync(Runnable task) {
        if (task != null) {
            bgPool.submit(task);
        }
    }

    public static void runTask(javafx.concurrent.Task<?> task) {
        if (task != null) {
            bgPool.submit(task);
        }
    }

    public static EventHandler<ActionEvent> safeOnAction(Runnable r, SystemLogService logService, String action) {
        Objects.requireNonNull(r);
        return evt -> {
            handleSafeExecution(r, logService, action, "Erro na acao UI");
        };
    }

    public static <T extends javafx.event.Event> EventHandler<T> safeEventHandler(Runnable r, SystemLogService logService, String action) {
        Objects.requireNonNull(r);
        return evt -> {
            handleSafeExecution(r, logService, action, "Erro no evento UI");
        };
    }

    private static void handleSafeExecution(Runnable r, SystemLogService logService, String action, String defaultMessage) {
        try {
            r.run();
        } catch (Throwable ex) {
            log.error("[{}] {}", action != null ? action : "UI_ACTION_FAILED", defaultMessage, ex);
            try {
                if (logService != null) logService.logError(action != null ? action : "UI_ACTION_FAILED", defaultMessage, ex);
            } catch (Exception ignore) {
                log.debug("Falha ao registar erro no SystemLogService", ignore);
            }
            try {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Erro: " + ex.getMessage());
                    a.setHeaderText(null);
                    a.showAndWait();
                });
            } catch (Exception ignore) {
                log.debug("Não foi possível mostrar alerta de erro UI", ignore);
            }
        }
    }

    public static void attachSafe(Button b, Runnable r, SystemLogService logService, String action) {
        if (b == null) return;
        b.setOnAction(safeOnAction(r, logService, action));
    }

    public static void safeRun(Runnable r, SystemLogService logService, String action) {
        try {
            r.run();
        } catch (Throwable ex) {
            try {
                if (logService != null) logService.logError(action != null ? action : "UI_RUN_FAILED", "Erro em operação segura", ex);
            } catch (Exception ignore) {
                log.debug("Falha ao registar erro no SystemLogService", ignore);
            }
        }
    }

    public static void applyNumericFormatter(javafx.scene.control.TextField field) {
        if (field == null) return;
        javafx.scene.control.TextFormatter<String> formatter = new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            // Allows empty, or a number with an optional single decimal dot/comma
            if (newText.matches("^[0-9]*[.,]?[0-9]*$")) {
                return change;
            }
            return null;
        });
        field.setTextFormatter(formatter);
    }

    public static void setupDebounce(javafx.scene.control.TextField field, Runnable action, int delayMs) {
        if (field == null || action == null) return;
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMs));
        pause.setOnFinished(e -> action.run());
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            pause.playFromStart();
        });
    }
}
