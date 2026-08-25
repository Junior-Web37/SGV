package com.sgv.desktop;

import com.sgv.entity.Transfer;
import com.sgv.service.AppConfigService;
import com.sgv.service.LicenseService;
import com.sgv.service.StockBranchService;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Centro de Notificações do SGV — janela evoluída que agrega todos os alertas
 * do sistema: stock crítico/esgotado, artigos sem ficha, licenciamento,
 * transferências pendentes de receção e modo treinamento activo.
 *
 * Cada notificação tem severidade (CRÍTICO/AVISO/INFO), categoria e acção
 * directa para a página correspondente.
 */
@Component
public class NotificationsCenterDialog {

    private static final Logger log = LoggerFactory.getLogger(NotificationsCenterDialog.class);

    private final StockBranchService stockBranchService;
    private final LicenseService licenseService;
    private final AppConfigService appConfigService;
    private final com.sgv.repository.TransferRepository transferRepository;

    /** Registo interno de uma notificação. */
    public record Notification(String severity, String icon, String category, String title,
                               String description, String actionLabel, Runnable action) {
        String severityColor() {
            return switch (severity) {
                case "CRITICO" -> "#DC2626";
                case "AVISO" -> "#F59E0B";
                default -> "#2563EB";
            };
        }
        String chipColor() {
            return switch (severity) {
                case "CRITICO" -> "rgba(220,38,38,0.12); -fx-text-fill:#B91C1C; -fx-border-color:rgba(220,38,38,0.35)";
                case "AVISO" -> "rgba(245,158,11,0.14); -fx-text-fill:#B45309; -fx-border-color:rgba(245,158,11,0.4)";
                default -> "rgba(37,99,235,0.10); -fx-text-fill:#1D4ED8; -fx-border-color:rgba(37,99,235,0.30)";
            };
        }
    }

    public NotificationsCenterDialog(StockBranchService stockBranchService,
                                     LicenseService licenseService,
                                     AppConfigService appConfigService,
                                     com.sgv.repository.TransferRepository transferRepository) {
        this.stockBranchService = stockBranchService;
        this.licenseService = licenseService;
        this.appConfigService = appConfigService;
        this.transferRepository = transferRepository;
    }

    // ══════════════════════════════════════════════════
    // RECOLHA DE NOTIFICAÇÕES
    // ══════════════════════════════════════════════════

    public List<Notification> collect() {
        List<Notification> out = new ArrayList<>();

        try {
            for (StockBranchService.StockAlert a : stockBranchService.listStockAlerts(null)) {
                boolean esgotado = "ESGOTADO".equals(a.kind());
                boolean semFicha = "SEM_FICHA".equals(a.kind());
                String desc;
                if (semFicha) {
                    desc = "O artigo não tem ficha de stock em nenhuma filial — defina stock mínimo e inicial.";
                } else if (esgotado) {
                    desc = String.format("Stock actual: 0 · Mínimo: %s · Filial: %s",
                            Formatters.formatNumber(a.min()),
                            a.branch() != null ? a.branch().getName() : "—");
                } else {
                    desc = String.format("Stock actual: %s · Mínimo: %s · Filial: %s",
                            Formatters.formatNumber(a.current()), Formatters.formatNumber(a.min()),
                            a.branch() != null ? a.branch().getName() : "—");
                }
                out.add(new Notification(
                        esgotado ? "CRITICO" : "AVISO",
                        esgotado ? "🔴" : (semFicha ? "⚪" : "🟡"),
                        semFicha ? "STOCK · SEM FICHA" : (esgotado ? "STOCK · ESGOTADO" : "STOCK · REPOSIÇÃO"),
                        a.product() != null ? a.product().getName() : "Artigo",
                        desc,
                        "Abrir Inventário", null));
            }
        } catch (Exception ex) {
            log.error("Erro ao recolher alertas de stock", ex);
        }

        try {
            var lic = licenseService.getCurrentLicenseStatus();
            if (!lic.isValid()) {
                out.add(new Notification("CRITICO", "🔑", "LICENCIAMENTO", "Licença inválida ou expirada",
                        lic.getMessage() + "\nA aplicação está em modo de avaliação.",
                        "Gerir Licença", null));
            } else if (lic.getDaysRemaining() != Integer.MAX_VALUE && lic.getDaysRemaining() <= 30) {
                out.add(new Notification("AVISO", "🔑", "LICENCIAMENTO", "Licença expira em " + lic.getDaysRemaining() + " dia(s)",
                        "Tipo: " + lic.getType() + " · Expira: " + lic.getExpiryDate()
                                + "\nRenove para manter o sistema em conformidade.",
                        "Gerir Licença", null));
            }
        } catch (Exception ex) {
            log.debug("Licença indisponível para notificações", ex);
        }

        try {
            List<Transfer> pendentes = new ArrayList<>();
            if (transferRepository != null) {
                pendentes.addAll(transferRepository.findByStatus("PENDING"));
                pendentes.addAll(transferRepository.findByStatus("IN_TRANSIT"));
            }
            if (!pendentes.isEmpty()) {
                long origens = pendentes.stream().map(t -> t.getSourceBranch() != null ? t.getSourceBranch().getName() : "?").distinct().count();
                out.add(new Notification("INFO", "🔁", "OPERAÇÕES", pendentes.size() + " transferência(s) por receber",
                        "Guias aguardam confirmação de receção nas filiais destino (" + origens + " origem(ns)).\n"
                                + "Receba-as para creditar o stock da filial correcta.",
                        "Ver Transferências", null));
            }
        } catch (Exception ex) {
            log.error("Erro ao recolher transferências pendentes", ex);
        }

        try {
            if (Boolean.TRUE.equals(appConfigService.get().getDemoMode())) {
                out.add(new Notification("AVISO", "🎓", "SISTEMA", "Modo Treinamento activo",
                        "As operações estão bloqueadas e os dados gravados são de demonstração.\n"
                                + "Desactive para voltar ao modo real.",
                        "Desactivar Modo", null));
            }
        } catch (Exception ignore) {
        }

        return out;
    }

    public int countAll() {
        return collect().size();
    }

    // ══════════════════════════════════════════════════
    // JANELA
    // ══════════════════════════════════════════════════

    /**
     * Mostra o centro de notificações.
     *
     * @param goInventario   navegação para a página de Inventário
     * @param goTransferencias navegação para Transferências
     * @param goLicenca      navegação para Licenciamento
     * @param onDisableTraining desactiva o modo treinamento
     */
    public void show(javafx.scene.Node ownerNode, Consumer<Runnable> navigator,
                     Runnable goInventario, Runnable goTransferencias,
                     Runnable goLicenca, Runnable onDisableTraining) {

        List<Notification> all = collect();

        Stage dialog = new Stage();
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (ownerNode != null && ownerNode.getScene() != null) {
            dialog.initOwner(ownerNode.getScene().getWindow());
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:14; -fx-border-color:#E2E8F0; -fx-border-radius:14;");
        DropShadow shadow = new DropShadow();
        shadow.setRadius(22); shadow.setOffsetY(5);
        shadow.setColor(Color.rgb(15, 23, 42, 0.30));
        root.setEffect(shadow);

        // ─── HEADER ──────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #1E3A8A, #1D4ED8); -fx-background-radius:14 14 0 0; -fx-padding:18 22;");
        Label bellIcon = new Label("🔔");
        bellIcon.setStyle("-fx-font-size:22px;");
        VBox titleBox = new VBox(2);
        Label title = new Label("Centro de Notificações");
        title.setStyle("-fx-text-fill:#FFFFFF; -fx-font-size:17px; -fx-font-weight:800;");
        long criticos = all.stream().filter(n -> "CRITICO".equals(n.severity())).count();
        Label subtitle = new Label(criticos > 0
                ? criticoLabel(all.size(), criticos)
                : all.size() + " notificação(ões) — nenhum item crítico");
        subtitle.setStyle("-fx-text-fill:#BFDBFE; -fx-font-size:12px; -fx-font-weight:600;");
        titleBox.getChildren().addAll(title, subtitle);
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Button btnCloseTop = new Button("✕");
        btnCloseTop.setStyle("-fx-background-color:rgba(255,255,255,0.15); -fx-text-fill:#FFFFFF; -fx-font-weight:800; -fx-padding:6 10; -fx-background-radius:8; -fx-cursor:hand;");
        btnCloseTop.setOnAction(e -> dialog.close());
        header.getChildren().addAll(bellIcon, titleBox, hSpacer, btnCloseTop);

        // ─── RESUMO (chips KPI) ─────────────────────────────
        long stockN = all.stream().filter(n -> n.category().startsWith("STOCK")).count();
        long opsN = all.stream().filter(n -> n.category().startsWith("OPERAÇÕES")).count();
        long sysN = all.stream().filter(n -> n.category().startsWith("SISTEMA") || n.category().startsWith("LICENCIAMENTO")).count();

        HBox summaryRow = new HBox(10);
        summaryRow.setStyle("-fx-padding:14 22 6 22;");
        summaryRow.getChildren().addAll(
                summaryChip("📦", "Stock", stockN, "#DC2626"),
                summaryChip("🔁", "Operações", opsN, "#F59E0B"),
                summaryChip("⚙️", "Sistema & Licença", sysN, "#2563EB"),
                summaryChip("🔔", "Total", all.size(), "#10B981"));

        // ─── LISTA ──────────────────────────────────────────
        VBox list = new VBox(10);
        list.setStyle("-fx-padding:8 22 16 22;");
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        rebuildList(list, all, navigator, goInventario, goTransferencias, goLicenca, onDisableTraining, dialog);

        root.setTop(new VBox(header, summaryRow));
        root.setCenter(scroll);

        // ─── FOOTER ────────────────────────────────────────
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:12 22; -fx-border-color:#E2E8F0; -fx-border-width:1 0 0 0;");
        Label genLbl = new Label("Actualizado agora · " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        genLbl.setStyle("-fx-text-fill:#94A3B8; -fx-font-size:11px; -fx-font-weight:600;");
        Region fSpacer = new Region();
        HBox.setHgrow(fSpacer, Priority.ALWAYS);
        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color:#F1F5F9; -fx-text-fill:#0F172A; -fx-font-weight:700; -fx-font-size:12px; -fx-padding:8 20; -fx-background-radius:8; -fx-cursor:hand; -fx-border-color:#CBD5E1; -fx-border-radius:8;");
        btnFechar.setOnAction(e -> dialog.close());
        footer.getChildren().addAll(genLbl, fSpacer, btnFechar);
        root.setBottom(footer);

        Scene scene = new Scene(root, 660, 640);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        UiUtils.applyHoverElevation(btnFechar);
        dialog.showAndWait();
    }

    private String criticoLabel(int total, long criticos) {
        return total + " notificação(ões) — " + criticos + " crítica(s) requerem atenção";
    }

    private void rebuildList(VBox list, List<Notification> all, Consumer<Runnable> navigator,
                             Runnable goInventario, Runnable goTransferencias, Runnable goLicenca,
                             Runnable onDisableTraining, Stage dialog) {
        list.getChildren().clear();
        if (all.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setStyle("-fx-alignment:center; -fx-padding:40 20;");
            Label ok = new Label("✅");
            ok.setStyle("-fx-font-size:34px;");
            Label msg = new Label("Tudo em ordem!");
            msg.setStyle("-fx-font-size:15px; -fx-font-weight:800; -fx-text-fill:#0F172A;");
            Label sub = new Label("Sem alertas de stock, operações pendentes ou avisos de sistema.");
            sub.setStyle("-fx-font-size:12px; -fx-text-fill:#64748B; -fx-text-alignment:center; -fx-wrap-text:true;");
            empty.getChildren().addAll(ok, msg, sub);
            list.getChildren().add(empty);
            return;
        }

        // Críticos primeiro, depois avisos, depois info
        List<Notification> sorted = all.stream()
                .sorted(java.util.Comparator.comparingInt(n -> switch (n.severity()) {
                    case "CRITICO" -> 0;
                    case "AVISO" -> 1;
                    default -> 2;
                }))
                .toList();

        int shown = 0;
        for (Notification n : sorted) {
            if (shown >= 40) {
                Label more = new Label("… +" + (sorted.size() - shown) + " outras notificações");
                more.setStyle("-fx-text-fill:#64748B; -fx-font-weight:700; -fx-padding:6 4;");
                list.getChildren().add(more);
                break;
            }
            list.getChildren().add(buildCard(n, navigator, goInventario, goTransferencias, goLicenca, onDisableTraining, dialog));
            shown++;
        }
    }

    private HBox buildCard(Notification n, Consumer<Runnable> navigator,
                           Runnable goInventario, Runnable goTransferencias, Runnable goLicenca,
                           Runnable onDisableTraining, Stage dialog) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:10; -fx-border-color:#E2E8F0; -fx-border-radius:10;"
                + "-fx-border-width:1; -fx-padding:12 14; -fx-effect:dropshadow(gaussian,rgba(15,23,42,0.06),4,0,0,1);");

        // Barra lateral colorida por severidade
        Region accent = new Region();
        accent.setStyle("-fx-background-color:" + n.severityColor() + "; -fx-background-radius:6; -fx-min-width:5; -fx-max-width:5;");

        VBox body = new VBox(4);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(n.icon());
        icon.setStyle("-fx-font-size:14px;");
        Label ttl = new Label(n.title());
        ttl.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:#0F172A;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label chip = new Label(n.category());
        chip.setStyle("-fx-background-color:" + n.chipColor() + "; -fx-font-size:9px; -fx-font-weight:800; -fx-padding:2 8; -fx-background-radius:8; -fx-border-radius:8; -fx-border-width:1;");
        titleRow.getChildren().addAll(icon, ttl, spacer, chip);

        Label desc = new Label(n.description());
        desc.setStyle("-fx-font-size:11.5px; -fx-font-weight:600; -fx-text-fill:#475569; -fx-wrap-text:true;");
        desc.setMaxWidth(430);

        body.getChildren().addAll(titleRow, desc);

        Button action = null;
        if (n.actionLabel() != null) {
            action = new Button(n.actionLabel());
            action.setStyle("-fx-background-color:#EFF6FF; -fx-text-fill:#1D4ED8; -fx-font-weight:800; -fx-font-size:11px; -fx-padding:7 12; -fx-background-radius:8; -fx-cursor:hand; -fx-border-color:#BFDBFE; -fx-border-radius:8;");
            action.setOnAction(e -> {
                dialog.close();
                Runnable target = switch (n.actionLabel()) {
                    case "Abrir Inventário" -> goInventario;
                    case "Ver Transferências" -> goTransferencias;
                    case "Gerir Licença" -> goLicenca;
                    case "Desactivar Modo" -> onDisableTraining;
                    default -> null;
                };
                if (target != null && navigator != null) navigator.accept(target);
            });
        }

        card.getChildren().addAll(accent, body);
        if (action != null) {
            VBox btnWrap = new VBox(action);
            btnWrap.setAlignment(Pos.CENTER);
            card.getChildren().add(btnWrap);
            UiUtils.applyHoverElevation(action);
        }
        return card;
    }

    private HBox summaryChip(String emoji, String labelTxt, long count, String color) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color:#F8FAFC; -fx-background-radius:10; -fx-border-color:#E2E8F0; -fx-border-radius:10; -fx-padding:10 14;");
        Label ic = new Label(emoji);
        ic.setStyle("-fx-font-size:15px;");
        VBox txt = new VBox(1);
        Label val = new Label(String.valueOf(count));
        val.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:" + color + ";");
        Label lbl = new Label(labelTxt);
        lbl.setStyle("-fx-font-size:10px; -fx-font-weight:700; -fx-text-fill:#64748B;");
        txt.getChildren().addAll(val, lbl);
        box.getChildren().addAll(ic, txt);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }
}
