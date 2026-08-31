package com.billywater.views;

import com.billywater.ui.Sessao;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;

/** Janela principal (shell) — estrutura §3: Painel, Água▾, Loja▾, Rede▾, Relatórios▾, Configurações▾, Logs. */
public class MainWindow extends BorderPane {

    private final HBox subBar = new HBox(8);
    private final Map<String, Node> cache = new LinkedHashMap<>();
    private final HBox topBar = new HBox(16);
    private final VBox centerHolder = new VBox();

    public MainWindow() {
        getStyleClass().add("main-window");

        topBar.getStyleClass().add("top-bar");
        topBar.setPrefHeight(58);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 20, 0, 20));

        Label logo = new Label("‎");
        logo.getStyleClass().add("top-logo");
        Label titulo = new Label("BILLY WATER");
        titulo.getStyleClass().add("top-titulo");
        Label perfil = new Label(String.format("Operador: %s  ·  %s", operador(), perfil()));
        perfil.getStyleClass().add("top-operador");

        Button bloqueio = new Button("Bloquear");
        bloqueio.getStyleClass().add("top-link");
        bloqueio.setOnAction(e -> com.billywater.BillyWaterApp.voltarLogin());
        Button sair = new Button("⏻ Sair");
        sair.getStyleClass().add("top-link");
        sair.setOnAction(e -> com.billywater.BillyWaterApp.voltarLogin());

        Label caixa = new Label("○ Caixa");
        caixa.getStyleClass().add("top-link");
        HBox right = new HBox(8);
        right.getChildren().addAll(caixa, bloqueio, sair);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(perfil, Priority.ALWAYS);
        topBar.getChildren().addAll(logo, titulo, perfil, right);

        subBar.getStyleClass().add("sub-bar");
        subBar.setPrefHeight(40);
        subBar.setPadding(new Insets(0, 20, 0, 20));
        subBar.setAlignment(Pos.CENTER_LEFT);
        construirMenu();

        centerHolder.getStyleClass().add("view-container");
        setCenter(navegar("principal"));
    }

    private void construirMenu() {
        Button painel = navBotao("⌂ Painel", "principal");
        MenuButton agua = menu("Água ▾",
                item("Clientes & Contratos", "clientes"),
                item("Contadores", "contadores"),
                item("Zonas / Rotas", "zonas"),
                item("Leituras", "leituras"),
                item("Facturação", "facturacao"),
                item("Tarifário", "tarifario"),
                item("Dívidas & Cortes", "dividas"),
                item("Caixa / Cobrança", "caixa"),
                item("Histórico REC", "recibos"));
        MenuButton loja = menu("Loja ▾",
                item("Vendas (POS/VD)", "loja"),
                item("Stock / Produtos", "stock"),
                item("Armazéns", "armazens"),
                item("Transferências", "transferencias"),
                item("Serviços / COT", "servicos"),
                item("Fornecedores", "fornecedores"),
                item("Compras (entrada)", "compras"));
        MenuButton rede = menu("Rede ▾",
                item("Produção / NRW", "rede"),
                item("Compras (entrada)", "compras"),
                item("Fornecedores", "fornecedores"));
        MenuButton relatorios = menu("Relatórios ▾", item("Todos os Relatórios", "relatorios"));
        MenuButton config = menu("Configurações ▾",
                item("Parâmetros & Empresa", "config"),
                item("Utilizadores & Permissões", "config"),
                item("Backups / Treino / Licença", "config"),
                item("Segurança", "config"));
        Button logs = navBotao("🛡️ Logs", "logs");

        subBar.getChildren().addAll(painel, agua, loja, rede, relatorios, config, logs);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        subBar.getChildren().add(spacer);

        ScrollPane sp = new ScrollPane(subBar);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setFitToHeight(true);
        sp.setPrefHeight(40);
        sp.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        sp.setHvalue(0);
        setTop(new VBox(topBar, sp));
    }

    private Button navBotao(String texto, String chave) {
        Button b = new Button(texto);
        b.getStyleClass().add("nav-item");
        b.setOnAction(e -> setCenter(navegar(chave)));
        return b;
    }

    private MenuItem item(String texto, String chave) {
        MenuItem m = new MenuItem(texto);
        m.setOnAction(e -> setCenter(navegar(chave)));
        return m;
    }

    private MenuButton menu(String texto, MenuItem... itens) {
        MenuButton mb = new MenuButton(texto);
        mb.getStyleClass().add("nav-item");
        mb.getItems().addAll(itens);
        return mb;
    }

    private Node navegar(String chave) {
        if (cache.containsKey(chave)) return cache.get(chave);
        Node v = switch (chave) {
            case "clientes" -> new ClientesView();
            case "contadores" -> new ContadoresView();
            case "zonas" -> new ZonasView();
            case "leituras" -> new LeiturasView();
            case "facturacao" -> new FacturacaoView();
            case "tarifario" -> new TarifarioView();
            case "dividas" -> new DividasView();
            case "caixa" -> new CaixaView();
            case "recibos" -> new RecibosView();
            case "loja" -> new LojaView();
            case "stock" -> new StockView();
            case "armazens" -> new ArmazensView();
            case "transferencias" -> new TransferenciasView();
            case "servicos" -> new ServicosView();
            case "fornecedores" -> new FornecedoresView();
            case "compras" -> new ComprasView();
            case "rede" -> new RedeView();
            case "relatorios" -> new RelatoriosView();
            case "logs" -> new LogsView();
            case "config" -> new ConfiguracoesView();
            default -> new DashboardView();
        };
        cache.put(chave, v);
        return v;
    }

    private String operador() { return Sessao.atual() != null ? Sessao.atual().username : "n/d"; }
    private String perfil() { return Sessao.nomePerfil(); }
}
