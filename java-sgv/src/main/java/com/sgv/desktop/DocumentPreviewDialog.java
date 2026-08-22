package com.sgv.desktop;

import com.sgv.entity.Sale;
import com.sgv.service.SaleDocumentService;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.printing.PDFPageable;

import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

/**
 * Diálogo de preview/print dos documentos SGV.
 *
 * Características:
 *  - Mostra botões VISÍVEIS para escolher o formato de impressão:
 *      * Térmica 80mm (padrão AT para VENDA / RECIBO / TV)
 *      * A4          (padrão AT para FACTURA / COTAÇÃO / FA / NC / ND)
 *      * A5          (alternativa compacta)
 *      * Talão Pequeno (rolo 58mm — alternativa)
 *  - O formato inicial é o AT-padrão (passado por parâmetro); ao clicar noutro
 *    formato, o PDF é regenerado e o preview recarregado.
 *  - O preview ocupa toda a viewport disponível (sem espaço em branco extra
 *    lateral) e o scroll só aparece se a página for genuinamente mais alta
 *    do que a janela.
 */
public class DocumentPreviewDialog {

    // ── Formatos disponíveis ───────────────────────────────────────────────
    public static final String FMT_THERMAL_80MM = "thermal-80mm";
    public static final String FMT_A4           = "a4";
    public static final String FMT_A5           = "a5";
    public static final String FMT_THERMAL_58MM = "thermal-58mm";

    /**
     * Overload legacy — mantém compatibilidade. Detecta o tipo pelo nome do
     * ficheiro e usa o formato AT-padrão.
     */
    public static void show(File pdfFile) {
        String name = pdfFile.getName().toLowerCase();
        String type;
        if (name.contains("cotac") || name.contains("quot")) {
            type = "COTACAO";
        } else if (name.contains("encom") || name.contains("order")) {
            type = "ENCOMENDA";
        } else {
            type = "VENDA";
        }
        show(pdfFile, type, atDefaultFormat(type), null, null, null);
    }

    /** Overload — sem serviços (não permite trocar formato). */
    public static void show(File pdfFile, String docType) {
        show(pdfFile, docType, atDefaultFormat(docType), null, null, null);
    }

    /** Overload — formato AT-padrão, sem serviços. */
    public static void show(File pdfFile, String docType, String format) {
        show(pdfFile, docType, format, null, null, null);
    }

    /**
     * Método principal — abre o preview com o seletor de formato visível.
     *
     * @param pdfFile          Ficheiro PDF inicial (já no formato {@code format}).
     * @param docType          "VENDA" / "COTACAO" / "ENCOMENDA" / "FACTURA" / "RECIBO".
     * @param format           Formato inicial. Use {@link #atDefaultFormat(String)} para o AT-padrão.
     * @param sale             Venda (opcional, necessária para regenerar noutro formato).
     * @param thermalGenerator Função que devolve o PDF térmico (opcional).
     * @param a4Generator      Função que devolve o PDF A4 (opcional).
     */
    public static void show(File pdfFile,
                            String docType,
                            String format,
                            Sale sale,
                            Function<Sale, File> thermalGenerator,
                            Function<Sale, File> a4Generator) {
        show(pdfFile, docType, format, sale, thermalGenerator, null, a4Generator, null);
    }

    public static void show(File pdfFile,
                            String docType,
                            String format,
                            Sale sale,
                            Function<Sale, File> thermalGenerator,
                            Function<Sale, File> thermal58Generator,
                            Function<Sale, File> a4Generator,
                            Function<Sale, File> a5Generator) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(pdfFile, docType, format, sale, thermalGenerator, thermal58Generator, a4Generator, a5Generator));
            return;
        }

        // ── Aparência por tipo ────────────────────────────────────────────
        String windowTitle;
        String headerBg;
        String headerText;
        String badgeLabel;
        String exportLabel;
        String atDefaultHint;

        switch (docType == null ? "VENDA" : docType.toUpperCase()) {
            case "COTACAO":
                windowTitle  = "Visualização da Cotação";
                headerBg     = "#7C3AED";
                headerText   = "Cotação";
                badgeLabel   = "COTAÇÃO";
                exportLabel  = "💾 Exportar Cotação";
                atDefaultHint = "AT-padrão: A4";
                break;
            case "ENCOMENDA":
                windowTitle  = "Visualização da Encomenda";
                headerBg     = "#D97706";
                headerText   = "Encomenda";
                badgeLabel   = "ENCOMENDA";
                exportLabel  = "💾 Exportar Encomenda";
                atDefaultHint = "AT-padrão: A4";
                break;
            case "FACTURA":
            case "FA":
                windowTitle  = "Visualização da Factura";
                headerBg     = "#0369A1";
                headerText   = "Factura";
                badgeLabel   = "FACTURA";
                exportLabel  = "💾 Exportar Factura";
                atDefaultHint = "AT-padrão: A4";
                break;
            default: // VENDA / RECIBO / TV
                windowTitle  = "Recibo de Venda";
                headerBg     = "#059669";
                headerText   = "Recibo de Venda";
                badgeLabel   = "RECIBO";
                exportLabel  = "💾 Exportar Recibo";
                atDefaultHint = "AT-padrão: Térmica 80mm";
                break;
        }

        // ── Janela ────────────────────────────────────────────────────────
        Stage stage = new Stage();
        stage.setTitle(windowTitle);
        stage.initStyle(javafx.stage.StageStyle.DECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);

        // Janela grande (90% do ecrã) — antes era 640×820 fixo, demasiado
        // pequeno para A4 e demasiado alto para térmica 80mm.
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.max(720, bounds.getWidth() * 0.85));
        stage.setHeight(Math.max(600, bounds.getHeight() * 0.90));

        final VBox root = new VBox();
        root.setStyle("-fx-background-color: #F1F5F9;");

        // ── HEADER ────────────────────────────────────────────────────────
        HBox header = new HBox(12);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: " + headerBg + ";");

        Label badge = new Label(badgeLabel);
        badge.setStyle(
            "-fx-background-color: rgba(255,255,255,0.2); " +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; " +
            "-fx-padding: 3 8; -fx-background-radius: 20;"
        );
        Label titleLabel = new Label(headerText);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Label fileLabel = new Label("📄 " + pdfFile.getName());
        fileLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 11px;");
        header.getChildren().addAll(badge, titleLabel, headerSpacer, fileLabel);

        // ── TOOLBAR — seletor de formato VISÍVEL + ações ──────────────────
        HBox toolbar = new HBox(8);
        toolbar.setPadding(new Insets(10, 16, 10, 16));
        toolbar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label formatLabel = new Label("Formato:");
        formatLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155; -fx-font-size: 12px; -fx-padding: 0 4 0 0;");

        // Botões de formato
        Button btnFmt80mm = makeFormatButton("🖨️ Térmica 80mm", FMT_THERMAL_80MM, format, atDefaultHint);
        Button btnFmtA4   = makeFormatButton("📄 A4",           FMT_A4,           format, atDefaultHint);
        Button btnFmtA5   = makeFormatButton("📄 A5",           FMT_A5,           format, atDefaultHint);
        Button btnFmt58mm = makeFormatButton("🖨️ Talão 58mm",   FMT_THERMAL_58MM, format, atDefaultHint);

        // Ações à direita
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnPrint = new Button("🖨️ Imprimir (Ctrl+P)");
        btnPrint.setStyle("-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 4; -fx-cursor: hand;");

        Button btnWhatsapp = new Button("📱 WhatsApp");
        btnWhatsapp.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 4; -fx-cursor: hand;");

        Button btnExport = new Button(exportLabel);
        btnExport.setStyle("-fx-background-color: " + headerBg + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 4; -fx-cursor: hand;");

        Button btnClose = new Button("✕ Fechar (ESC)");
        btnClose.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 7 14; -fx-background-radius: 4; -fx-cursor: hand;");

        toolbar.getChildren().addAll(
            formatLabel, btnFmt80mm, btnFmtA4, btnFmtA5, btnFmt58mm,
            spacer, btnWhatsapp, btnPrint, btnExport, btnClose
        );

        // ── Hint do formato AT activo ────────────────────────────────────
        Label atHintLabel = new Label(atDefaultHint);
        atHintLabel.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #047857; -fx-font-size: 11px; -fx-padding: 4 16; -fx-border-color: #A7F3D0; -fx-border-width: 0 0 1 0;");
        atHintLabel.setMaxWidth(Double.MAX_VALUE);

        // ── PDF VIEWER (auto-fit) ─────────────────────────────────────────
        // Usamos um StackPane para o loader e o conteúdo; o VBox dentro do
        // ScrollPane centra-se horizontalmente e preenche a largura.
        // Importante: NÃO usamos setFitToWidth(true) porque isso força o
        // conteúdo a esticar até à largura, e o preserveRatio da ImageView
        // depois mantém a altura proporcional — fica uma página gigante que
        // sai do ecrã verticalmente. Em vez disso, calculamos manualmente o
        // tamanho em renderPdfInBox() para caber em AMBAS as dimensões.
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);   // nunca scroll horizontal
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setStyle("-fx-background-color: #CBD5E1; -fx-background: #CBD5E1;");

        VBox pagesBox = new VBox(15);
        pagesBox.setAlignment(Pos.TOP_CENTER);
        pagesBox.setFillWidth(true);
        pagesBox.setPadding(new Insets(12));
        pagesBox.setStyle("-fx-background-color: #CBD5E1;");

        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(48, 48);
        pagesBox.getChildren().add(loadingIndicator);
        scrollPane.setContent(pagesBox);

        // ── AÇÕES ────────────────────────────────────────────────────────
        btnClose.setOnAction(e -> stage.close());

        // Estado partilhado entre handlers
        final File[] currentPdf = { pdfFile };
        final String[] currentFormat = { format };

        Runnable renderCurrent = () -> {
            renderPdfInBox(currentPdf[0], pagesBox, scrollPane, loadingIndicator, currentFormat[0]);
            updateFormatButtons(btnFmt80mm, btnFmtA4, btnFmtA5, btnFmt58mm, currentFormat[0], atDefaultHint);
            atHintLabel.setText(formatHint(currentFormat[0], atDefaultHint));
        };

        // Re-render automático quando o utilizador redimensiona a janela —
        // o recibo deve continuar a caber em ambas as dimensões.
        // Usamos um pequeno debounce via PauseTransition para evitar reflows
        // múltiplos durante o drag.
        javafx.animation.PauseTransition resizeDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(80));
        Runnable scheduleRerender = () -> {
            resizeDebounce.stop();
            resizeDebounce.setOnFinished(ev -> renderCurrent.run());
            resizeDebounce.playFromStart();
        };
        scrollPane.widthProperty().addListener((obs, o, n) -> scheduleRerender.run());
        scrollPane.heightProperty().addListener((obs, o, n) -> scheduleRerender.run());

        // Handlers de mudança de formato
        btnFmt80mm.setOnAction(e -> switchFormat(
            stage, sale, thermalGenerator, thermal58Generator, a4Generator, a5Generator,
            FMT_THERMAL_80MM, currentPdf, currentFormat, renderCurrent,
            atDefaultHint
        ));
        btnFmtA4.setOnAction(e -> switchFormat(
            stage, sale, thermalGenerator, thermal58Generator, a4Generator, a5Generator,
            FMT_A4, currentPdf, currentFormat, renderCurrent,
            atDefaultHint
        ));
        btnFmtA5.setOnAction(e -> switchFormat(
            stage, sale, thermalGenerator, thermal58Generator, a4Generator, a5Generator,
            FMT_A5, currentPdf, currentFormat, renderCurrent,
            atDefaultHint
        ));
        btnFmt58mm.setOnAction(e -> switchFormat(
            stage, sale, thermalGenerator, thermal58Generator, a4Generator, a5Generator,
            FMT_THERMAL_58MM, currentPdf, currentFormat, renderCurrent,
            atDefaultHint
        ));

        btnPrint.setOnAction(e -> {
            btnPrint.setDisable(true);
            new Thread(() -> {
                PDDocument document = null;
                try {
                    document = Loader.loadPDF(currentPdf[0]);
                    final PDDocument finalDoc = document;
                    PrinterJob job = PrinterJob.getPrinterJob();
                    job.setPageable(new PDFPageable(finalDoc));
                    
                    // Executa a impressão no background thread sem fechar prematuramente o documento
                    boolean proceed = job.printDialog();
                    if (proceed) {
                        job.print();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Documento enviado para a impressora com sucesso.");
                            alert.showAndWait();
                        });
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao imprimir: " + ex.getMessage());
                        alert.showAndWait();
                    });
                } finally {
                    if (document != null) {
                        try { document.close(); } catch (Exception ignored) {}
                    }
                    Platform.runLater(() -> btnPrint.setDisable(false));
                }
            }).start();
        });

        btnWhatsapp.setOnAction(e -> {
            if (sale == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Dados da venda não disponíveis para partilha directa.");
                alert.showAndWait();
                return;
            }
            String defaultPhone = (sale.getCustomer() != null && sale.getCustomer().getContact() != null)
                    ? sale.getCustomer().getContact() : "";
            javafx.scene.control.TextInputDialog phoneDialog = new javafx.scene.control.TextInputDialog(defaultPhone);
            phoneDialog.setTitle("Enviar Recibo por WhatsApp");
            phoneDialog.setHeaderText("Envio Digital de Recibo / Documento");
            phoneDialog.setContentText("Número de Telemóvel (+258):");
            phoneDialog.showAndWait().ifPresent(phone -> {
                if (phone.isBlank()) return;
                try {
                    String raw = phone.replaceAll("[^0-9+]", "");
                    if (raw.startsWith("8") && raw.length() == 9) raw = "258" + raw;
                    else if (raw.startsWith("+")) raw = raw.substring(1);
                    else if (!raw.startsWith("258")) raw = "258" + raw;

                    String docName = (sale.getDocumentType() != null ? sale.getDocumentType() : "DOC")
                            + " " + (sale.getSeries() != null ? sale.getSeries() : "A")
                            + "/" + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : sale.getId());

                    String msg = String.format("*%s*\n*Documento Fiscal:* %s\n*Data:* %s\n*Total:* *%,.2f MT*\nObrigado pela preferência!",
                            sale.getBranch() != null ? sale.getBranch().getName() : "SGV Moçambique",
                            docName,
                            sale.getCreatedAt() != null ? sale.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "",
                            sale.getTotal() != null ? sale.getTotal() : 0.0);

                    String url = "https://wa.me/" + raw + "?text=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                    } else {
                        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", url});
                    }
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao abrir WhatsApp: " + ex.getMessage());
                    alert.showAndWait();
                }
            });
        });

        btnExport.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exportar " + headerText);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
            fileChooser.setInitialFileName(currentPdf[0].getName());
            File dest = fileChooser.showSaveDialog(stage);
            if (dest != null) {
                try {
                    Files.copy(currentPdf[0].toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        headerText + " guardado com sucesso em:\n" + dest.getAbsolutePath());
                    alert.showAndWait();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao guardar: " + ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        // Render inicial
        renderCurrent.run();

        root.getChildren().addAll(header, toolbar, atHintLabel, scrollPane);
        Scene scene = new Scene(root);

        // Atalhos de teclado no preview: ESC fecha, Ctrl+P imprime, Enter imprime
        scene.setOnKeyPressed(ke -> {
            if (ke.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                stage.close();
            } else if (ke.isControlDown() && ke.getCode() == javafx.scene.input.KeyCode.P) {
                btnPrint.fire();
            }
        });

        stage.setScene(scene);

        // Quando a janela terminar de aparecer, força um re-render para que o
        // ImageView calcule o tamanho correcto (o 1º render corre antes do
        // scrollPane ter dimensão real). Pequeno delay para a layout passar.
        stage.setOnShown(ev -> Platform.runLater(() -> {
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(60));
            delay.setOnFinished(e2 -> renderCurrent.run());
            delay.play();
        }));

        stage.show();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public static String atDefaultFormat(String docType) {
        if (docType == null) return FMT_THERMAL_80MM;
        switch (docType.toUpperCase()) {
            case "VENDA":
            case "RECIBO":
            case "RC":
            case "TV":
                return FMT_THERMAL_80MM;
            case "COTACAO":
            case "COTACAO_ABERTA":
            case "FACTURA":
            case "FA":
            case "NC":
            case "ND":
            case "ENCOMENDA":
            case "ENCOMENDA_ABERTA":
                return FMT_A4;
            default:
                return FMT_THERMAL_80MM;
        }
    }

    private static Button makeFormatButton(String label, String fmt, String active, String atHint) {
        Button b = new Button(label);
        b.setUserData(fmt);
        styleFormatButton(b, fmt.equals(active), isAtDefault(fmt, atHint));
        return b;
    }

    private static void styleFormatButton(Button b, boolean active, boolean isAtDefault) {
        String base = "-fx-padding: 6 12; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 12px;";
        if (active) {
            // Activo: preenchido na cor primária
            b.setStyle(base + " -fx-background-color: #0F766E; -fx-text-fill: white; -fx-font-weight: bold;");
        } else if (isAtDefault) {
            // AT-padrão mas não activo: outline verde-água
            b.setStyle(base + " -fx-background-color: white; -fx-text-fill: #047857; -fx-border-color: #10B981; -fx-border-width: 1.5; -fx-border-radius: 4; -fx-font-weight: bold;");
        } else {
            b.setStyle(base + " -fx-background-color: white; -fx-text-fill: #475569; -fx-border-color: #CBD5E1; -fx-border-width: 1; -fx-border-radius: 4;");
        }
    }

    private static void updateFormatButtons(Button b80, Button bA4, Button bA5, Button b58, String active, String atHint) {
        styleFormatButton(b80, FMT_THERMAL_80MM.equals(active), isAtDefaultForAny(FMT_THERMAL_80MM, atHint));
        styleFormatButton(bA4, FMT_A4.equals(active),           isAtDefaultForAny(FMT_A4, atHint));
        styleFormatButton(bA5, FMT_A5.equals(active),           isAtDefaultForAny(FMT_A5, atHint));
        styleFormatButton(b58, FMT_THERMAL_58MM.equals(active), isAtDefaultForAny(FMT_THERMAL_58MM, atHint));
    }

    private static boolean isAtDefault(String fmt, String atHint) {
        if (atHint == null) return false;
        return atHint.toLowerCase().contains(fmtLabel(fmt).toLowerCase());
    }

    private static boolean isAtDefaultForAny(String fmt, String atHint) {
        return isAtDefault(fmt, atHint);
    }

    private static String formatHint(String fmt, String atDefaultHint) {
        String label = fmtLabel(fmt);
        if (atDefaultHint != null && atDefaultHint.toLowerCase().contains(label.toLowerCase())) {
            return "✓ " + atDefaultHint + " — formato AT seleccionado";
        }
        return "Formato: " + label + " (manual)";
    }

    private static String fmtLabel(String fmt) {
        if (fmt == null) return "Térmica 80mm";
        return switch (fmt) {
            case FMT_A4           -> "A4";
            case FMT_A5           -> "A5";
            case FMT_THERMAL_58MM -> "Talão 58mm";
            default               -> "Térmica 80mm";
        };
    }

    private static void switchFormat(Stage stage,
                                     Sale sale,
                                     Function<Sale, File> thermal80Gen,
                                     Function<Sale, File> thermal58Gen,
                                     Function<Sale, File> a4Gen,
                                     Function<Sale, File> a5Gen,
                                     String targetFormat,
                                     File[] currentPdf,
                                     String[] currentFormat,
                                     Runnable renderCurrent,
                                     String atDefaultHint) {
        if (targetFormat.equals(currentFormat[0])) return;
        // Se não temos venda + geradores, mantemos o PDF actual
        if (sale == null) {
            currentFormat[0] = targetFormat;
            renderCurrent.run();
            return;
        }

        File newPdf = null;
        try {
            if (FMT_THERMAL_80MM.equals(targetFormat)) {
                if (thermal80Gen != null) newPdf = thermal80Gen.apply(sale);
            } else if (FMT_THERMAL_58MM.equals(targetFormat)) {
                if (thermal58Gen != null) newPdf = thermal58Gen.apply(sale);
                else if (thermal80Gen != null) newPdf = thermal80Gen.apply(sale);
            } else if (FMT_A4.equals(targetFormat)) {
                if (a4Gen != null) newPdf = a4Gen.apply(sale);
            } else if (FMT_A5.equals(targetFormat)) {
                if (a5Gen != null) newPdf = a5Gen.apply(sale);
                else if (a4Gen != null) newPdf = a4Gen.apply(sale);
            }
        } catch (Exception ex) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR,
                    "Erro a gerar PDF no formato " + fmtLabel(targetFormat) + ":\n" + ex.getMessage());
                a.showAndWait();
            });
            return;
        }

        if (newPdf == null || !newPdf.exists()) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.WARNING,
                    "Não foi possível gerar o PDF no formato " + fmtLabel(targetFormat)
                    + ". A manter o formato actual.");
                a.showAndWait();
            });
            return;
        }

        currentPdf[0] = newPdf;
        currentFormat[0] = targetFormat;
        renderCurrent.run();
    }

    /**
     * Renderiza o PDF no VBox, fazendo-o caber em AMBAS as dimensões da
     * viewport (largura E altura) para que o utilizador veja o documento
     * completo sem scroll e sem espaço em branco extra.
     *
     * @param currentFormat Formato actual — controla a proporção (A4, A5, 80mm, 58mm).
     */
    private static void renderPdfInBox(File pdfFile, VBox pagesBox, ScrollPane scrollPane,
                                       ProgressIndicator loader, String currentFormat) {
        pagesBox.getChildren().clear();
        pagesBox.getChildren().add(loader);

        Thread t = new Thread(() -> {
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                PDFRenderer renderer = new PDFRenderer(document);
                int numPages = document.getNumberOfPages();
                Image[] fxImages = new Image[numPages];

                for (int i = 0; i < numPages; i++) {
                    BufferedImage bim = renderer.renderImageWithDPI(i, 150);
                    fxImages[i] = SwingFXUtils.toFXImage(bim, null);
                }

                // Proporção alvo (largura/altura) consoante o formato
                // (o PDF gerado pode não ser exactamente estes números, mas
                // usamos isto para forçar a preview a ter o aspect-ratio certo)
                double targetRatio = pageRatio(currentFormat);

                Platform.runLater(() -> {
                    pagesBox.getChildren().clear();
                    for (Image img : fxImages) {
                        ImageView iv = new ImageView(img);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);

                        // Largura e altura da área disponível (ScrollPane).
                        // Subtraímos padding para não colar nas bordas.
                        double availableW = scrollPane.getWidth()  > 0 ? scrollPane.getWidth()  - 32 : 600;
                        double availableH = scrollPane.getHeight() > 0 ? scrollPane.getHeight() - 32 : 700;

                        // Calculamos o tamanho final para que o documento
                        // caiba em ambas as dimensões mantendo a proporção do
                        // formato. Se o PDF for mais alto que o formato alvo,
                        // limitamos pela altura; caso contrário, pela largura.
                        double ratio = availableW / availableH;
                        double fitW, fitH;
                        if (ratio > targetRatio) {
                            // Janela mais "larga" que a página → limita pela altura
                            fitH = availableH;
                            fitW = fitH * targetRatio;
                        } else {
                            // Janela mais "alta" que a página → limita pela largura
                            fitW = availableW;
                            fitH = fitW / targetRatio;
                        }

                        iv.setFitWidth(fitW);
                        iv.setFitHeight(fitH);

                        VBox pageContainer = new VBox(iv);
                        pageContainer.setAlignment(Pos.CENTER);
                        pageContainer.setFillWidth(false);
                        pageContainer.setPadding(new Insets(0, 0, 16, 0));
                        pageContainer.setStyle(
                            "-fx-background-color: white; " +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 6, 0, 0, 1);"
                        );
                        pagesBox.getChildren().add(pageContainer);
                    }
                    // Volta ao topo do scroll
                    scrollPane.setVvalue(0.0);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    pagesBox.getChildren().clear();
                    Label errorLabel = new Label("Erro ao carregar o PDF: " + ex.getMessage());
                    errorLabel.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold; -fx-padding: 20;");
                    errorLabel.setWrapText(true);
                    pagesBox.getChildren().add(errorLabel);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Devolve a proporção largura/altura esperada para cada formato de papel
     * (em mm, paisagem ignorada — o recibo/factura é sempre retrato).
     *  - A4:          210 × 297
     *  - A5:          148 × 210
     *  - Térmica 80:   80 × 297 (altura variável, usamos 1 página)
     *  - Térmica 58:   58 × 297
     */
    private static double pageRatio(String fmt) {
        if (fmt == null) return 210.0 / 297.0;
        return switch (fmt) {
            case FMT_A4           -> 210.0 / 297.0;
            case FMT_A5           -> 148.0 / 210.0;
            case FMT_THERMAL_58MM -> 58.0  / 200.0;  // 58mm de largura, ~200mm visíveis
            case FMT_THERMAL_80MM -> 80.0  / 200.0;
            default               -> 210.0 / 297.0;
        };
    }
}
