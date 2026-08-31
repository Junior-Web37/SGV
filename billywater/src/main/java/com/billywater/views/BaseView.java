package com.billywater.views;

import com.billywater.ui.Sessao;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Base das telas: cabeçalho com título/subtítulo, corpo e helpers de permissão. */
public abstract class BaseView extends VBox {

    private final VBox body = new VBox();

    public BaseView(String titulo, String subTitulo) {
        getStyleClass().add("view-root");
        setSpacing(14);
        VBox header = new VBox(2);
        header.getStyleClass().add("view-header");
        Label t = new Label(titulo);
        t.getStyleClass().add("view-title");
        Label s = new Label(subTitulo);
        s.getStyleClass().add("view-subtitle");
        header.getChildren().addAll(t, s);
        body.getStyleClass().add("view-body");
        body.setSpacing(14);
        getChildren().addAll(header, body);
    }

    protected VBox body() { return body; }
    protected void attach() {}
    protected boolean pode(String permissao) { return Sessao.pode(permissao); }
    protected boolean admin() { return Sessao.ehAdmin(); }
    protected Label pendencia(String titulo) {
        Label l = new Label(titulo);
        l.getStyleClass().add("pendencias-nota");
        return l;
    }
}
