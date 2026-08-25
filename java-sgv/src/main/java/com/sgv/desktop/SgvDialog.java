package com.sgv.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Diálogos estilizados do SGV — substituem os Alert nativos do JavaFX.
 * Visual consistente com a identidade do sistema (gradiente azul, cards, radius).
 */
public final class SgvDialog {

    private SgvDialog() {}

    public enum Type { INFO, WARNING, ERROR, CONFIRM, DANGER }

    // ═══════════════ API pública ═══════════════

    public static void info(String title, String message) { show(Type.INFO, title, message, null); }

    public static void warning(String title, String message) { show(Type.WARNING, title, message, null); }

    public static void error(String title, String message) { show(Type.ERROR, title, message, null); }

    /** Confirmação padrão (acções neutras). Retorna true se confirmado. */
    public static boolean confirm(String title, String message) {
        return show(Type.CONFIRM, title, message, null);
    }

    /** Confirmação de perigo (eliminações/anulações) — botão principal vermelho "Eliminar". */
    public static boolean confirmDanger(String title, String message) {
        return show(Type.DANGER, title, message, null);
    }

    /** Confirmação com texto do botão de confirmação personalizado (ex.: "Anular", "Restaurar"). */
    public static boolean confirmAction(String title, String message, String actionLabel) {
        return show(Type.CONFIRM, title, message, actionLabel);
    }

    /** Notificação rápida não-modal (auto-fecha em ~2.5s). */
    public static void toast(String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Info");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.5));
            delay.setOnFinished(ev -> alert.close());
            delay.play();
        } catch (Exception ignored) { }
    }

    /** Diálogo de introdução de texto estilizado. Retorna Optional.empty() se cancelado. */
    public static java.util.Optional<String> prompt(String title, String message, String defaultValue) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E2E8F0;");

        Label accentBar = new Label();
        accentBar.setMinHeight(5);
        accentBar.setMaxWidth(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: #2563EB; -fx-background-radius: 12 12 0 0;");

        VBox body = new VBox(14);
        body.setPadding(new Insets(26, 28, 22, 28));
        Label titleLbl = new Label(title);
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0F172A;");
        Label msgLbl = new Label(message != null ? message : "");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(430);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        javafx.scene.control.TextField input = new javafx.scene.control.TextField(defaultValue != null ? defaultValue : "");
        input.setStyle("-fx-font-size: 13px; -fx-padding: 9 12; -fx-background-radius: 6; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");
        input.setPrefWidth(430);
        body.getChildren().addAll(titleLbl, msgLbl, input);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 28, 22, 28));
        Button cancelBtn = new Button("Cancelar");
        cancelBtn.setStyle("-fx-padding: 9 22; -fx-font-size: 12px; -fx-font-weight: 700;"
            + " -fx-background-color: #ffffff; -fx-text-fill: #334155;"
            + " -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
        Button okBtn = new Button("✓ Confirmar");
        okBtn.setDefaultButton(true);
        okBtn.setStyle("-fx-padding: 9 24; -fx-font-size: 12px; -fx-font-weight: 800;"
            + " -fx-background-color: #2563EB; -fx-text-fill: #ffffff; -fx-background-radius: 6; -fx-cursor: hand;");
        java.util.concurrent.atomic.AtomicBoolean confirmed = new java.util.concurrent.atomic.AtomicBoolean(false);
        okBtn.setOnAction(e -> { confirmed.set(true); dialog.close(); });
        cancelBtn.setOnAction(e -> dialog.close());
        Region pad = new Region();
        HBox.setHgrow(pad, Priority.ALWAYS);
        footer.getChildren().addAll(pad, cancelBtn, okBtn);

        root.getChildren().addAll(accentBar, body, footer);
        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> {
            if ("ESCAPE".equals(e.getCode().getName())) dialog.close();
        });
        input.setOnAction(e -> { confirmed.set(true); dialog.close(); });
        Window owner = findOwner();
        if (owner != null) dialog.initOwner(owner);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
        input.requestFocus();
        return confirmed.get() ? java.util.Optional.ofNullable(input.getText()) : java.util.Optional.empty();
    }

    // ═══════════════ Núcleo ═══════════════

    private static boolean show(Type type, String title, String message, String customActionLabel) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title);
        dialog.setResizable(false);

        String accent, accentSoft, iconEmoji;
        switch (type) {
            case WARNING:  accent = "#F59E0B"; accentSoft = "#FEF3C7"; iconEmoji = "⚠"; break;
            case ERROR:    accent = "#EF4444"; accentSoft = "#FEE2E2"; iconEmoji = "✕"; break;
            case DANGER:   accent = "#EF4444"; accentSoft = "#FEE2E2"; iconEmoji = "🗑"; break;
            case CONFIRM:  accent = "#2563EB"; accentSoft = "#DBEAFE"; iconEmoji = "?"; break;
            default:       accent = "#10B981"; accentSoft = "#D1FAE5"; iconEmoji = "✓";
        }

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E2E8F0;");

        Label accentBar = new Label();
        accentBar.setMinHeight(5);
        accentBar.setMaxWidth(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 12 12 0 0;");

        HBox body = new HBox(18);
        body.setPadding(new Insets(26, 28, 22, 28));
        body.setAlignment(Pos.TOP_LEFT);

        Label iconLbl = new Label(iconEmoji);
        iconLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: " + accent + ";"
            + " -fx-min-width: 54px; -fx-min-height: 54px; -fx-alignment: center;"
            + " -fx-background-color: " + accentSoft + "; -fx-background-radius: 27;");

        VBox textCol = new VBox(8);
        textCol.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setWrapText(true);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #0F172A;");
        Label msgLbl = new Label(message != null ? message : "");
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(430);
        msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-line-spacing: 2;");
        textCol.getChildren().addAll(titleLbl, msgLbl);
        body.getChildren().addAll(iconLbl, textCol);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 28, 22, 28));

        boolean needsCancel = (type == Type.CONFIRM || type == Type.DANGER);
        if (needsCancel) {
            Button cancelBtn = new Button("Cancelar");
            cancelBtn.setStyle("-fx-padding: 9 22; -fx-font-size: 12px; -fx-font-weight: 700;"
                + " -fx-background-color: #ffffff; -fx-text-fill: #334155;"
                + " -fx-border-color: #CBD5E1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> dialog.close());
            cancelBtn.setOnKeyPressed(e -> { if ("ENTER".equals(e.getCode().getName())) dialog.close(); });
            footer.getChildren().add(cancelBtn);
        }

        String okLabel;
        if (type == Type.DANGER) okLabel = customActionLabel != null ? customActionLabel : "🗑 Eliminar";
        else if (type == Type.CONFIRM) okLabel = customActionLabel != null ? customActionLabel : "✓ Confirmar";
        else okLabel = "OK";

        Button okBtn = new Button(okLabel);
        okBtn.setStyle("-fx-padding: 9 24; -fx-font-size: 12px; -fx-font-weight: 800;"
            + " -fx-background-color: " + (type == Type.DANGER || type == Type.ERROR ? "#EF4444" : "#2563EB") + ";"
            + " -fx-text-fill: #ffffff; -fx-background-radius: 6; -fx-cursor: hand;");
        okBtn.setDefaultButton(true);
        Boolean[] confirmed = {false};
        okBtn.setOnAction(e -> { confirmed[0] = true; dialog.close(); });
        footer.getChildren().add(okBtn);

        Region pad = new Region();
        HBox.setHgrow(pad, Priority.ALWAYS);
        footer.getChildren().add(0, pad);

        root.getChildren().addAll(accentBar, body, footer);

        Scene scene = new Scene(root);
        scene.setOnKeyPressed(e -> { if ("ESCAPE".equals(e.getCode().getName())) dialog.close(); });
        Window owner = findOwner();
        if (owner != null) dialog.initOwner(owner);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        UiUtils.applyHoverElevation(okBtn);
        if (needsCancel) UiUtils.applyHoverElevation((Button) footer.getChildren().get(footer.getChildren().size() - 2));
        dialog.showAndWait();
        return confirmed[0];
    }

    private static Window findOwner() {
        for (Window w : Window.getWindows()) {
            if (w instanceof Stage s && s.isShowing() && !s.isIconified()) return s;
        }
        return null;
    }
}
