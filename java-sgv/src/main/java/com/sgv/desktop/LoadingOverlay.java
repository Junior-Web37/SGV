package com.sgv.desktop;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public final class LoadingOverlay {

    private static final String OVERLAY_ID = "sgv-loading-overlay";

    private LoadingOverlay() {}

    public static void show(Pane parent, String message) {
        if (parent == null) return;
        Platform.runLater(() -> {
            StackPane overlay = createOverlay(message);
            overlay.setId(OVERLAY_ID);
            parent.getChildren().add(overlay);
        });
    }

    public static void hide(Pane parent) {
        if (parent == null) return;
        Platform.runLater(() -> parent.getChildren().removeIf(node -> OVERLAY_ID.equals(node.getId())));
    }

    private static StackPane createOverlay(String message) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(64, 64);

        Label label = new Label(message != null ? message : "A processar...");
        label.getStyleClass().add("loading-label");

        VBox content = new VBox(12, spinner, label);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.setBackground(new Background(new BackgroundFill(Color.web("#ffffff"), new CornerRadii(12), Insets.EMPTY)));

        StackPane wrapper = new StackPane(content);
        wrapper.setStyle("-fx-background-color: rgba(15, 23, 42, 0.35);");
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPickOnBounds(true);
        return wrapper;
    }
}
