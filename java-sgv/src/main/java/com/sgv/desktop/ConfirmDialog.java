package com.sgv.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static boolean show(String title, String message, String confirmText, String cancelText) {
        final boolean[] result = {false};
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("confirm-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("confirm-message");
        messageLabel.setWrapText(true);

        Button cancelButton = new Button(cancelText);
        cancelButton.getStyleClass().addAll("button", "secondary-button");
        cancelButton.setOnAction(event -> dialog.close());

        Button confirmButton = new Button(confirmText);
        confirmButton.getStyleClass().addAll("button", "danger-button");
        confirmButton.setOnAction(event -> {
            result[0] = true;
            dialog.close();
        });

        HBox actions = new HBox(10, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(16, titleLabel, messageLabel, actions);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_LEFT);
        content.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(12), Insets.EMPTY)));
        content.setMaxWidth(420);

        StackPane root = new StackPane(content);
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.35), CornerRadii.EMPTY, Insets.EMPTY)));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(ConfirmDialog.class.getResource("/styles/styles.css").toExternalForm());

        dialog.setScene(scene);
        dialog.showAndWait();
        return result[0];
    }
}
