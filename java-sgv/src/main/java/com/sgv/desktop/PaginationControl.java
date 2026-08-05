package com.sgv.desktop;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class PaginationControl extends HBox {

    private final IntegerProperty pageIndex = new SimpleIntegerProperty(1);
    private final Label pageLabel;

    public PaginationControl(Runnable onPrevious, Runnable onNext) {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setPadding(new Insets(8));
        getStyleClass().add("pagination-control");

        Button prevButton = new Button("Anterior");
        prevButton.getStyleClass().addAll("button", "secondary-button");
        prevButton.setOnAction(evt -> {
            if (pageIndex.get() > 1) {
                pageIndex.set(pageIndex.get() - 1);
                if (onPrevious != null) {
                    onPrevious.run();
                }
            }
        });

        pageLabel = new Label();
        pageLabel.getStyleClass().add("pagination-label");
        pageLabel.textProperty().bind(pageIndex.asString("Página %d"));

        Button nextButton = new Button("Próxima");
        nextButton.getStyleClass().addAll("button", "secondary-button");
        nextButton.setOnAction(evt -> {
            pageIndex.set(pageIndex.get() + 1);
            if (onNext != null) {
                onNext.run();
            }
        });

        getChildren().addAll(prevButton, pageLabel, nextButton);
    }

    public int getPageIndex() {
        return pageIndex.get();
    }

    public void setPageIndex(int index) {
        pageIndex.set(index);
    }

    public IntegerProperty pageIndexProperty() {
        return pageIndex;
    }
}
