package com.sgv.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

public final class ToastNotification {

    private ToastNotification() {
    }

    public static void showSuccess(String message) {
        show(message, "✔", "toast-success");
    }

    public static void showError(String message) {
        show(message, "✖", "toast-error");
    }

    public static void showWarning(String message) {
        show(message, "⚠", "toast-warning");
    }

    public static void showInfo(String message) {
        show(message, "ℹ", "toast-info");
    }

    private static void show(String message, String icon, String styleClass) {
        Platform.runLater(() -> {
            Window window = getActiveWindow();
            if (window == null) {
                return;
            }
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            Label iconLabel = new Label(icon);
            iconLabel.getStyleClass().addAll("toast-icon");
            iconLabel.setMinWidth(20);
            iconLabel.setAlignment(Pos.CENTER_LEFT);

            Label messageLabel = new Label(message);
            messageLabel.getStyleClass().addAll("toast-message");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(320);

            HBox toast = new HBox(12, iconLabel, messageLabel);
            toast.getStyleClass().addAll("toast", styleClass);
            toast.setPadding(new Insets(14));
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.setBackground(new Background(new BackgroundFill(Color.web("#ffffff"), new CornerRadii(10), Insets.EMPTY)));
            toast.setMinWidth(Region.USE_PREF_SIZE);
            toast.setMaxWidth(360);

            popup.getContent().add(toast);
            popup.show(window, window.getX() + window.getWidth() - 400, window.getY() + 20);

            new Thread(() -> {
                try {
                    Thread.sleep(3200);
                } catch (InterruptedException ignored) {
                }
                Platform.runLater(popup::hide);
            }).start();
        });
    }

    private static Window getActiveWindow() {
        if (Window.getWindows().isEmpty()) {
            return null;
        }
        return Window.getWindows().stream().filter(Window::isShowing).findFirst().orElse(null);
    }
}
