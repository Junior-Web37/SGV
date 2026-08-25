package com.sgv.desktop;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public final class SearchBar extends HBox {

    private final TextField searchField;
    private final Button clearButton;

    public SearchBar(String promptText, Runnable onSearch, Runnable onClear) {
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(8));
        getStyleClass().add("search-bar");

        searchField = new TextField();
        searchField.setPromptText(promptText);
        searchField.getStyleClass().addAll("text-field", "search-input");
        if (onSearch != null) {
            UiUtils.setupDebounce(searchField, onSearch, 250);
        }

        clearButton = new Button("Limpar");
        clearButton.getStyleClass().addAll("button", "secondary-button");
        clearButton.setOnAction(evt -> {
            searchField.clear();
            if (onClear != null) {
                onClear.run();
            }
        });

        getChildren().addAll(searchField, clearButton);
    }

    public String getText() {
        return searchField.getText();
    }

    public void setText(String text) {
        searchField.setText(text);
    }

    public StringProperty textProperty() {
        return searchField.textProperty();
    }
}
