package com.sgv.desktop;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public final class StatusBadge extends StackPane {

    public enum BadgeType {
        SUCCESS, DANGER, WARNING, INFO
    }

    public StatusBadge(String text, BadgeType type) {
        Label label = new Label(text);
        label.getStyleClass().add("status-badge-label");

        getChildren().add(label);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(4, 10, 4, 10));
        getStyleClass().add("status-badge");
        getStyleClass().add(getTypeClass(type));
        setBackground(new Background(new BackgroundFill(getBackgroundColor(type), new CornerRadii(12), Insets.EMPTY)));
    }

    private String getTypeClass(BadgeType type) {
        return switch (type) {
            case SUCCESS -> "status-badge-success";
            case DANGER -> "status-badge-danger";
            case WARNING -> "status-badge-warning";
            case INFO -> "status-badge-info";
        };
    }

    private Color getBackgroundColor(BadgeType type) {
        return switch (type) {
            case SUCCESS -> Color.web("#D1FAE5");
            case DANGER -> Color.web("#FEE2E2");
            case WARNING -> Color.web("#FEF3C7");
            case INFO -> Color.web("#DBEAFE");
        };
    }
}
