package com.sgv.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class EmptyState extends StackPane {

    public EmptyState(String title, String subtitle, String actionLabel, Runnable action) {
        setAlignment(Pos.CENTER);
        getStyleClass().add("empty-state");
        setPadding(new Insets(32));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("empty-subtitle");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(420);

        VBox content = new VBox(12, titleLabel, subtitleLabel);
        content.setAlignment(Pos.CENTER);

        getChildren().add(content);

        if (action != null && actionLabel != null && !actionLabel.isBlank()) {
            Button actionButton = new Button(actionLabel);
            actionButton.getStyleClass().addAll("button", "secondary-button");
            actionButton.setOnAction(evt -> action.run());
            content.getChildren().add(actionButton);
        }
    }
}
