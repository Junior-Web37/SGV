package com.billywater.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Factories e helpers da UI (estrutura SGV, azul claro). */
public final class Ui {

    private static final ExecutorService BG = Executors.newFixedThreadPool(4);

    private Ui() {}

    public static String moeda(BigDecimal v) {
        DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.GERMANY));
        return df.format(v == null ? BigDecimal.ZERO : v);
    }
    public static String moeda(BigDecimal v, String sufixo) { return moeda(v) + (sufixo == null ? "" : " " + sufixo); }

    public static void emSegundoPlano(Runnable trabalho, Consumer<Throwable> aoTerminar) {
        BG.submit(() -> {
            try { trabalho.run(); }
            catch (Throwable t) { if (aoTerminar != null) aoTerminar.accept(t); else t.printStackTrace(); }
        });
    }
    public static void emSegundoPlano(Runnable trabalho) { emSegundoPlano(trabalho, null); }

    public static void info(String titulo, String msg) { alerta(Alert.AlertType.INFORMATION, titulo, msg); }
    public static void aviso(String titulo, String msg) { alerta(Alert.AlertType.WARNING, titulo, msg); }
    public static void erro(String titulo, String msg) { alerta(Alert.AlertType.ERROR, titulo, msg); }

    public static boolean confirmar(String titulo, String msg, ButtonType... extras) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(titulo); a.setHeaderText(titulo); a.setContentText(msg);
        if (extras != null) a.getButtonTypes().addAll(extras);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void alerta(Alert.AlertType t, String titulo, String msg) {
        Alert a = new Alert(t);
        a.setTitle(titulo); a.setHeaderText(titulo); a.setContentText(msg);
        a.showAndWait();
    }

    public static TextField campo(String prompt) { TextField t = new TextField(); t.setPromptText(prompt); return t; }

    public static <T> TableColumn<T, ?> col(String titulo, String prop, double w) {
        TableColumn<T, ?> c = new TableColumn<>(titulo);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setPrefWidth(w);
        return c;
    }

    public static <T> void tabela(TableView<T> t, List<TableColumn<T, ?>> cols, double altura) {
        t.getColumns().setAll(cols);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPrefHeight(altura);
    }

    public static <T> ComboBox<T> combo(List<T> itens) {
        ComboBox<T> cb = new ComboBox<>();
        cb.getItems().addAll(itens);
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    public static DatePicker data() { DatePicker d = new DatePicker(); d.setMaxWidth(Double.MAX_VALUE); return d; }

    public static Button botao(String texto, String estilo) { Button b = new Button(texto); b.getStyleClass().add(estilo); return b; }
    public static Button primario(String texto) { return botao(texto, "primary-button"); }
    public static Button fantasma(String texto) { return botao(texto, "ghost-button"); }
    public static Button perigo(String texto) { return botao(texto, "danger-button"); }

    /** Liga um acção a um nó da barra (seguro: só Button/ComboBox têm setOnAction). */
    public static void acao(Node n, Runnable onAction) {
        if (n instanceof Button b) b.setOnAction(e -> onAction.run());
        else if (n instanceof ComboBox<?> cb) cb.setOnAction(e -> onAction.run());
    }

    public static void setupDebounce(TextInputControl campo, Runnable acao, int ms) {
        javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(ms));
        campo.textProperty().addListener((o, a, b) -> { p.setOnFinished(e -> acao.run()); p.playFromStart(); });
    }

    public static <T> Object[] linha(TableView<T> t) { return new Object[0]; }
}
