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

    public static boolean isHarmlessJavaFxControlBug(Throwable e) {
        Throwable t = e;
        while (t != null) {
            String msg = t.getMessage() != null ? t.getMessage() : "";
            if (t instanceof IndexOutOfBoundsException
                    && (msg.contains("fromIndex") || msg.contains("toIndex") || msg.contains("Index"))) {
                return true;
            }
            if (t instanceof IllegalArgumentException && msg.toLowerCase().contains("start must be")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static void handleSafeExecution(Runnable r, SystemLogService logService, String action, String defaultMessage) {
        try {
            r.run();
        } catch (Throwable ex) {
            if (isHarmlessJavaFxControlBug(ex)) {
                log.debug("Bug interno JavaFX ignorado (ComboBox/ListView): {}", ex.getMessage());
                return;
            }
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

    /**
     * Protege ComboBox contra bugs conhecidos do JavaFX 21:
     * - IndexOutOfBoundsException ao clicar numa lista vazia
     * - IllegalArgumentException "The start must be &lt;= the end" no editor
     * - setAll() com o popup aberto
     */
    public static void hardenComboBox(javafx.scene.control.ComboBox<?> combo) {
        if (combo == null) return;
        if (Boolean.TRUE.equals(combo.getProperties().get("sgv.combo.hardened"))) return;
        combo.getProperties().put("sgv.combo.hardened", Boolean.TRUE);
        combo.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (combo.getItems() == null || combo.getItems().isEmpty()) {
                combo.hide();
                e.consume();
            }
        });
        combo.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (combo.getItems() == null || combo.getItems().isEmpty()) {
                combo.hide();
            }
        });
        combo.addEventHandler(javafx.scene.control.ComboBoxBase.ON_SHOWING, e -> {
            if (combo.getItems() == null || combo.getItems().isEmpty()) {
                Platform.runLater(combo::hide);
            }
        });
        if (combo.isEditable() && combo.getEditor() != null) {
            javafx.scene.control.TextField editor = combo.getEditor();
            editor.addEventFilter(javafx.scene.input.KeyEvent.ANY, ev -> fixComboEditorRange(editor));
            editor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, ev -> fixComboEditorRange(editor));
            editor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, ev -> fixComboEditorRange(editor));
        }
    }

    private static void fixComboEditorRange(javafx.scene.control.TextField editor) {
        if (editor == null) return;
        try {
            String text = editor.getText();
            int len = text != null ? text.length() : 0;
            int caret = editor.getCaretPosition();
            int anchor = editor.getAnchor();
            if (caret < 0 || anchor < 0 || caret > len || anchor > len || caret != anchor && Math.min(caret, anchor) > len) {
                editor.deselect();
                editor.positionCaret(Math.max(0, Math.min(Math.max(caret, anchor), len)));
            }
        } catch (Exception ignored) {
            try { editor.deselect(); } catch (Exception ignore) {}
        }
    }

    public static void hardenAllComboBoxes(javafx.scene.Node node) {
        if (node == null) return;
        if (node instanceof javafx.scene.control.ComboBox<?> combo) {
            hardenComboBox(combo);
        }
        if (node instanceof javafx.scene.control.ScrollPane scroll && scroll.getContent() != null) {
            hardenAllComboBoxes(scroll.getContent());
        }
        if (node instanceof javafx.scene.control.TabPane tabs) {
            for (javafx.scene.control.Tab tab : tabs.getTabs()) {
                hardenAllComboBoxes(tab.getContent());
            }
        }
        if (node instanceof javafx.scene.control.TitledPane titled) {
            hardenAllComboBoxes(titled.getContent());
        }
        if (node instanceof javafx.scene.control.SplitPane split) {
            for (javafx.scene.Node item : split.getItems()) {
                hardenAllComboBoxes(item);
            }
        }
        if (node instanceof javafx.scene.control.Accordion accordion) {
            for (javafx.scene.control.TitledPane pane : accordion.getPanes()) {
                hardenAllComboBoxes(pane);
            }
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                hardenAllComboBoxes(child);
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

    /**
     * Aplica micro-interacção suave de hover com elevação subtil (Scale 1.015, TranslateY -1.5px)
     * e transição suave de 120ms com curva Ease-Out.
     */
    public static void applyHoverElevation(javafx.scene.Node node) {
        if (node == null) return;
        node.setOnMouseEntered(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), node);
            tt.setToY(-2.0);
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(120), node);
            st.setToX(1.015);
            st.setToY(1.015);
            tt.play();
            st.play();
        });
        node.setOnMouseExited(e -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(120), node);
            tt.setToY(0.0);
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(120), node);
            st.setToX(1.0);
            st.setToY(1.0);
            tt.play();
            st.play();
        });
    }

    /**
     * Aplica micro-feedback tátil de clique / toque (Scale 0.96 durante 80ms).
     */
    public static void applyPressFeedback(javafx.scene.Node node) {
        if (node == null) return;
        node.setOnMousePressed(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(80), node);
            st.setToX(0.96);
            st.setToY(0.96);
            st.play();
        });
        node.setOnMouseReleased(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(80), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    /**
     * Animação escalonada (Staggered Fade-In) de uma lista de cartões/nós.
     */
    public static void staggeredFadeIn(java.util.List<? extends javafx.scene.Node> nodes, int baseDelayMs, int stepDelayMs) {
        if (nodes == null || nodes.isEmpty()) return;
        for (int i = 0; i < nodes.size(); i++) {
            javafx.scene.Node node = nodes.get(i);
            if (node == null) continue;
            node.setOpacity(0.0);
            node.setTranslateY(8.0);
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(baseDelayMs + (i * stepDelayMs)));
            delay.setOnFinished(e -> {
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), node);
                ft.setToValue(1.0);
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(180), node);
                tt.setToY(0.0);
                ft.play();
                tt.play();
            });
            delay.play();
        }
    }
}
