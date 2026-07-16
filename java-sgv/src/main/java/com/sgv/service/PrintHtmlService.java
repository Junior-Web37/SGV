package com.sgv.service;

import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gera o HTML pronto para impressão do documento fiscal SGV.
 *
 * Suporta 3 formatos exigidos pela AT Moçambique (Decreto 7/2024):
 *  - "thermal-80mm"  → Talão / Recibo (papel rolo 80mm, AT-padrão para TV e RC)
 *  - "a4"            → Factura / Cotação / NC / ND (papel A4, AT-padrão para FA e cotação)
 *  - "a5"            → Alternativa compacta (papel A5)
 *
 * O HTML resultante carrega o CSS de impressão embutido com @page adequado a cada
 * formato, e oculta os controlos antes de enviar para a impressora.
 */
@Service
public class PrintHtmlService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-MZ"));

    public enum Format {
        THERMAL_80MM("thermal-80mm"),
        A4("a4"),
        A5("a5");

        public final String key;
        Format(String k) { this.key = k; }

        public static Format fromKey(String s) {
            if (s == null) return THERMAL_80MM;
            return switch (s.toLowerCase()) {
                case "a4" -> A4;
                case "a5" -> A5;
                default -> THERMAL_80MM;
            };
        }
    }

    public String render(Sale sale, Format format) {
        return switch (format) {
            case THERMAL_80MM -> renderThermal(sale);
            case A4 -> renderA4(sale);
            case A5 -> renderA5(sale);
        };
    }

    // ─── Formatos ──────────────────────────────────────────────────────────

    private String renderThermal(Sale sale) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html lang=\"pt\"><head><meta charset=\"utf-8\"><title>Recibo ")
         .append(esc(sale.getDocumentType())).append(' ').append(esc(sale.getSeries()))
         .append('/').append(sale.getDocumentNumber() == null ? sale.getId() : sale.getDocumentNumber())
         .append("</title>");
        b.append("<style>")
         .append("@page { size: 80mm auto; margin: 0; }")
         .append("@media print { html, body { margin: 0 !important; padding: 0 !important; }")
         .append(" .no-print { display: none !important; } }")
         .append("body { font-family: 'Courier New', monospace; font-size: 12px; color: #000;")
         .append(" width: 80mm; margin: 0 auto; padding: 4mm 3mm; box-sizing: border-box; }")
         .append("h1 { font-size: 14px; margin: 0; text-align: center; }")
         .append(".center { text-align: center; } .right { text-align: right; }")
         .append(".muted { color: #444; }")
         .append("hr { border: none; border-top: 1px dashed #000; margin: 4px 0; }")
         .append("table { width: 100%; border-collapse: collapse; }")
         .append("td { vertical-align: top; }")
         .append(".line { display: flex; justify-content: space-between; gap: 4px; }")
         .append(".toolbar { position: fixed; top: 0; left: 0; right: 0; background: #1f2937; color: #fff;")
         .append(" padding: 10px 14px; display: flex; gap: 8px; align-items: center; z-index: 9999;")
         .append(" box-shadow: 0 2px 6px rgba(0,0,0,0.2); font-family: system-ui; }")
         .append(".toolbar button { background: #10b981; color: #fff; border: 0; padding: 6px 12px;")
         .append(" border-radius: 4px; cursor: pointer; font-size: 13px; }")
         .append(".toolbar .meta { margin-left: auto; font-size: 12px; opacity: 0.85; }")
         .append("body { padding-top: 56px; }")
         .append("</style></head><body>");

        // Toolbar (visível no ecrã, oculta na impressão)
        b.append("<div class=\"toolbar no-print\">")
         .append("<button onclick=\"window.print()\">🖨 Imprimir (80mm)</button>")
         .append("<span class=\"meta\">Formato: Térmica 80mm — Padrão AT para TV/RC</span>")
         .append("</div>");

        // Header
        String branch = sale.getBranch() != null ? sale.getBranch().getName() : "SGV";
        b.append("<h1>").append(esc(branch)).append("</h1>");
        if (sale.getBranch() != null && sale.getBranch().getNuit() != null && !sale.getBranch().getNuit().isBlank())
            b.append("<div class=\"center muted\">NUIT: ").append(esc(formatNuit(sale.getBranch().getNuit()))).append("</div>");
        if (sale.getBranch() != null && sale.getBranch().getAddress() != null && !sale.getBranch().getAddress().isBlank())
            b.append("<div class=\"center muted\" style=\"font-size:11px\">").append(esc(sale.getBranch().getAddress())).append("</div>");
        if (sale.getBranch() != null && sale.getBranch().getSoftwareCertNumber() != null && !sale.getBranch().getSoftwareCertNumber().isBlank())
            b.append("<div class=\"center muted\" style=\"font-size:10px\">Cert AT: ").append(esc(sale.getBranch().getSoftwareCertNumber())).append("</div>");
        b.append("<hr>");

        // Doc
        String docType = sale.getDocumentType() != null ? sale.getDocumentType() : "DOC";
        b.append("<div class=\"center\"><strong>").append(esc(docType)).append(' ').append(esc(sale.getSeries()))
         .append('/').append(sale.getDocumentNumber() == null ? sale.getId() : sale.getDocumentNumber())
         .append("</strong></div>");
        if (sale.getCreatedAt() != null)
            b.append("<div class=\"center muted\">").append(sale.getCreatedAt().format(DT_FMT)).append("</div>");
        b.append("<hr>");

        // Cliente
        String custName = sale.getCustomerName() != null ? sale.getCustomerName() : "Consumidor Final";
        String custNuit = sale.getCustomerNuit() != null ? sale.getCustomerNuit() : "999999999";
        b.append("<div>Cliente: <strong>").append(esc(truncate(custName, 24))).append("</strong></div>");
        b.append("<div>NUIT: ").append(esc(formatNuit(custNuit))).append("</div>");
        if (sale.getCustomerAddress() != null && !sale.getCustomerAddress().isBlank())
            b.append("<div>Morada: ").append(esc(truncate(sale.getCustomerAddress(), 24))).append("</div>");
        b.append("<hr>");

        // Itens
        if (sale.getItems() != null) {
            b.append("<table>");
            for (SaleItem i : sale.getItems()) {
                String desc = i.getProductName() != null ? i.getProductName()
                        : (i.getDescription() != null ? i.getDescription() : "");
                b.append("<tr><td colspan=\"3\">").append(esc(truncate(desc, 26))).append("</td></tr>");
                b.append("<tr><td>").append(fmtQty(i.getQty())).append(" x ")
                 .append(fmt(i.getUnitPrice())).append("</td>")
                 .append("<td class=\"right\" colspan=\"2\">").append(fmt(i.getLineTotal())).append("</td></tr>");
            }
            b.append("</table>");
        }
        b.append("<hr>");

        // Totais
        line(b, "Subtotal", fmt(sale.getSubtotal()));
        if (sale.getTotalDiscount() != null && sale.getTotalDiscount() > 0) line(b, "Desconto", "-" + fmt(sale.getTotalDiscount()));
        if (sale.getTotalIce() != null && sale.getTotalIce() > 0) line(b, "ICE", fmt(sale.getTotalIce()));
        line(b, "IVA", fmt(sale.getTotalTax()));
        if (sale.getWithholdingTax() != null && sale.getWithholdingTax() > 0) line(b, "Retenção", "-" + fmt(sale.getWithholdingTax()));
        b.append("<hr>");
        b.append("<div class=\"line\"><strong>TOTAL</strong><strong>").append(fmt(sale.getTotal())).append(" MT</strong></div>");
        if (sale.getPaidAmount() != null && sale.getPaidAmount() > 0) line(b, "Pago", fmt(sale.getPaidAmount()));
        if (sale.getChangeAmount() != null && sale.getChangeAmount() > 0) line(b, "Troco", fmt(sale.getChangeAmount()));
        b.append("<hr>");

        // Pagamentos
        if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
            b.append("<div><strong>Pagamentos:</strong></div>");
            for (Payment p : sale.getPayments()) {
                b.append("<div class=\"line\"><span>").append(esc(p.getMethod())).append("</span><span>")
                 .append(fmt(p.getAmount())).append("</span></div>");
            }
            b.append("<hr>");
        }

        // Hash AT
        if (sale.getHashHash() != null && !sale.getHashHash().isBlank()) {
            b.append("<div class=\"muted\" style=\"font-size:10px\">HASH AT (MD5):<br>").append(esc(sale.getHashHash())).append("</div>");
        }
        if (sale.getHashControl() != null) {
            b.append("<div class=\"muted\" style=\"font-size:10px\">Controle: ").append(sale.getHashControl()).append("</div>");
        }
        if (sale.getQrCode() != null && !sale.getQrCode().isBlank()) {
            b.append("<div class=\"muted\" style=\"font-size:10px\">QR: ").append(esc(truncate(sale.getQrCode(), 30))).append("</div>");
        }

        b.append("<hr>");
        b.append("<div class=\"center muted\" style=\"font-size:10px\">Processado por SGV</div>");
        b.append("<div class=\"center muted\" style=\"font-size:10px\">Obrigado pela preferência!</div>");
        if ("ANULADA".equals(sale.getState())) {
            b.append("<div class=\"center\" style=\"font-weight:bold;margin-top:6px\">*** DOCUMENTO ANULADO ***</div>");
            if (sale.getAnnulReason() != null)
                b.append("<div class=\"center muted\" style=\"font-size:10px\">Motivo: ").append(esc(sale.getAnnulReason())).append("</div>");
        }

        b.append("<script>window.addEventListener('load',function(){var b=document.querySelector('button');if(b)b.focus();});</script>");
        b.append("</body></html>");
        return b.toString();
    }

    private String renderA4(Sale sale) {
        return renderSheet(sale, "a4", "A4", "210mm 297mm", "Factura / Cotação — A4 (padrão AT para FA e Cotação)");
    }

    private String renderA5(Sale sale) {
        return renderSheet(sale, "a5", "A5", "148mm 210mm", "Documento A5");
    }

    /**
     * Gera folha A4/A5 com layout AT. Usado para Factura (FA), Cotação, NC e ND.
     */
    private String renderSheet(Sale sale, String size, String sizeLabel, String pageSize, String toolbarHint) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html lang=\"pt\"><head><meta charset=\"utf-8\"><title>")
         .append(esc(sale.getDocumentType())).append(' ').append(esc(sale.getSeries()))
         .append('/').append(sale.getDocumentNumber() == null ? sale.getId() : sale.getDocumentNumber())
         .append("</title>");
        b.append("<style>")
         .append("@page { size: ").append(pageSize).append("; margin: 12mm; }")
         .append("@media print { html, body { margin: 0; padding: 0; }")
         .append(" body { padding: 0; box-shadow: none; }")
         .append(" .no-print { display: none !important; }")
         .append(" .sheet { width: auto; max-width: 100%; margin: 0; box-shadow: none; border: 0; } }")
         .append("body { font-family: 'Helvetica', 'Arial', sans-serif; color: #111;")
         .append(" background: #e5e7eb; margin: 0; padding: 16px; }")
         .append(".toolbar { position: sticky; top: 0; background: #0f172a; color: #fff; padding: 10px 14px;")
         .append(" display: flex; gap: 8px; align-items: center; z-index: 10; box-shadow: 0 2px 6px rgba(0,0,0,0.2);")
         .append(" font-family: system-ui; }")
         .append(".toolbar button { background: #10b981; color: #fff; border: 0; padding: 6px 12px;")
         .append(" border-radius: 4px; cursor: pointer; font-size: 13px; }")
         .append(".toolbar .meta { margin-left: auto; font-size: 12px; opacity: 0.85; }")
         .append(".sheet { background: #fff; max-width: ").append(size.equals("a4") ? "210mm" : "148mm").append(";")
         .append(" margin: 16px auto; padding: ").append(size.equals("a4") ? "12mm" : "8mm").append(";")
         .append(" box-shadow: 0 1px 4px rgba(0,0,0,0.15); font-size: ").append(size.equals("a4") ? "11pt" : "9pt").append("; }")
         .append("h1 { text-align: center; margin: 0 0 4px; font-size: ").append(size.equals("a4") ? "18pt" : "14pt").append("; }")
         .append(".center { text-align: center; } .muted { color: #555; }")
         .append(".doc-title { text-align: center; font-size: ").append(size.equals("a4") ? "14pt" : "11pt").append(";")
         .append(" font-weight: 700; margin: 10px 0 4px; text-transform: uppercase; }")
         .append("hr { border: none; border-top: 1px solid #888; margin: 8px 0; }")
         .append(".grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 6px 16px; }")
         .append("table { width: 100%; border-collapse: collapse; margin-top: 6px; }")
         .append("th, td { padding: 4px 6px; border-bottom: 1px solid #e5e7eb; text-align: left; }")
         .append("th { background: #f3f4f6; font-weight: 600; }")
         .append(".num { text-align: right; }")
         .append(".totals { margin-left: auto; width: 60%; margin-top: 8px; }")
         .append(".totals div { display: flex; justify-content: space-between; padding: 2px 0; }")
         .append(".grand { font-size: ").append(size.equals("a4") ? "13pt" : "11pt").append("; font-weight: 700;")
         .append(" border-top: 2px solid #111; padding-top: 6px; margin-top: 6px; }")
         .append(".footer { text-align: center; margin-top: 12px; font-size: 9pt; color: #555; }")
         .append(".hash { font-family: 'Courier New', monospace; font-size: 9pt; word-break: break-all; }")
         .append("</style></head><body>");

        // Toolbar
        b.append("<div class=\"toolbar no-print\">")
         .append("<button onclick=\"window.print()\">🖨 Imprimir (").append(sizeLabel).append(")</button>")
         .append("<span class=\"meta\">").append(esc(toolbarHint)).append("</span>")
         .append("</div>");

        b.append("<div class=\"sheet\">");

        // Header
        String branch = sale.getBranch() != null ? sale.getBranch().getName() : "SGV Sistema";
        String nuit = sale.getBranch() != null && sale.getBranch().getNuit() != null ? sale.getBranch().getNuit() : "";
        String addr = sale.getBranch() != null && sale.getBranch().getAddress() != null ? sale.getBranch().getAddress() : "";
        String cert = sale.getBranch() != null ? sale.getBranch().getSoftwareCertNumber() : null;
        String lic = sale.getBranch() != null ? sale.getBranch().getLicenseNumber() : null;

        b.append("<h1>").append(esc(branch)).append("</h1>");
        if (!nuit.isBlank()) b.append("<div class=\"center muted\">NUIT: ").append(esc(formatNuit(nuit))).append("</div>");
        if (!addr.isBlank()) b.append("<div class=\"center muted\">").append(esc(addr)).append("</div>");
        if (cert != null && !cert.isBlank()) b.append("<div class=\"center muted\" style=\"font-size:9pt\">Certificado de Software AT: ").append(esc(cert)).append("</div>");
        if (lic != null && !lic.isBlank()) b.append("<div class=\"center muted\" style=\"font-size:9pt\">Licença: ").append(esc(lic)).append("</div>");
        b.append("<hr>");

        String docType = sale.getDocumentType() != null ? sale.getDocumentType() : "DOC";
        b.append("<div class=\"doc-title\">").append(esc(docType)).append(" Nº ")
         .append(esc(sale.getSeries())).append('/')
         .append(sale.getDocumentNumber() == null ? sale.getId() : sale.getDocumentNumber())
         .append('/').append(sale.getDocumentYear() == null ? "" : sale.getDocumentYear())
         .append("</div>");
        if (sale.getCreatedAt() != null)
            b.append("<div class=\"center muted\">Data: ").append(sale.getCreatedAt().format(DT_FMT)).append("</div>");
        b.append("<hr>");

        // Cliente
        b.append("<div style=\"font-weight:600;margin-bottom:4px\">DADOS DO CLIENTE</div>");
        b.append("<div class=\"grid-2\">");
        b.append("<div><strong>Nome:</strong> ").append(esc(sale.getCustomerName() == null ? "Consumidor Final" : sale.getCustomerName())).append("</div>");
        b.append("<div><strong>NUIT:</strong> ").append(esc(formatNuit(sale.getCustomerNuit() == null ? "999999999" : sale.getCustomerNuit()))).append("</div>");
        if (sale.getCustomerAddress() != null && !sale.getCustomerAddress().isBlank()) {
            b.append("<div style=\"grid-column:span 2\"><strong>Endereço:</strong> ").append(esc(sale.getCustomerAddress())).append("</div>");
        }
        b.append("</div>");
        b.append("<hr>");

        // Itens
        b.append("<table><thead><tr>")
         .append("<th>Descrição</th>")
         .append("<th class=\"num\">Qtd</th>")
         .append("<th class=\"num\">Preço Unit.</th>")
         .append("<th class=\"num\">IVA</th>")
         .append("<th class=\"num\">Total</th>")
         .append("</tr></thead><tbody>");
        if (sale.getItems() != null) {
            for (SaleItem i : sale.getItems()) {
                String desc = i.getProductName() != null ? i.getProductName()
                        : (i.getDescription() != null ? i.getDescription() : "—");
                if (desc.length() > 40) desc = desc.substring(0, 38) + "..";
                String tipoIva = (i.getTaxType() != null ? i.getTaxType() : "IVA")
                        + (i.getMotivoInexistTax() != null && !i.getMotivoInexistTax().isBlank()
                            ? " (" + i.getMotivoInexistTax() + ")" : "");
                b.append("<tr>")
                 .append("<td>").append(esc(desc)).append("<br><span class=\"muted\" style=\"font-size:9pt\">").append(esc(tipoIva)).append("</span></td>")
                 .append("<td class=\"num\">").append(fmtQty(i.getQty())).append("</td>")
                 .append("<td class=\"num\">").append(fmt(i.getUnitPrice())).append("</td>")
                 .append("<td class=\"num\">").append(i.getTaxRate() == null ? "" : String.format("%.0f%%", i.getTaxRate())).append("</td>")
                 .append("<td class=\"num\">").append(fmt(i.getLineTotal())).append("</td>")
                 .append("</tr>");
            }
        }
        b.append("</tbody></table>");

        // Totais
        b.append("<div class=\"totals\">");
        line(b, "Subtotal", fmt(sale.getSubtotal()));
        if (sale.getTotalDiscount() != null && sale.getTotalDiscount() > 0) line(b, "Desconto", "- " + fmt(sale.getTotalDiscount()));
        if (sale.getTotalIce() != null && sale.getTotalIce() > 0) line(b, "ICE", fmt(sale.getTotalIce()));
        line(b, "IVA", fmt(sale.getTotalTax()));
        if (sale.getWithholdingTax() != null && sale.getWithholdingTax() > 0) line(b, "Retenção", "- " + fmt(sale.getWithholdingTax()));
        b.append("<div class=\"grand\"><span>TOTAL ").append(sale.getCurrency() == null ? "MT" : sale.getCurrency()).append("</span><span>")
         .append(fmt(sale.getTotal())).append("</span></div>");
        if (sale.getPaidAmount() != null && sale.getPaidAmount() > 0) line(b, "Total Pago", fmt(sale.getPaidAmount()));
        if (sale.getChangeAmount() != null && sale.getChangeAmount() > 0) line(b, "Troco", fmt(sale.getChangeAmount()));
        b.append("</div>");

        // Hash / assinatura
        if (sale.getSignatureHash() != null && !sale.getSignatureHash().isBlank()) {
            b.append("<hr>");
            b.append("<div class=\"muted\" style=\"font-size:9pt\"><strong>Assinatura Digital (SHA-256):</strong></div>");
            b.append("<div class=\"hash\">").append(esc(sale.getSignatureHash())).append("</div>");
        }
        if (sale.getHashHash() != null && !sale.getHashHash().isBlank()) {
            b.append("<div class=\"muted\" style=\"font-size:9pt;margin-top:4px\"><strong>Hash AT (MD5):</strong></div>");
            b.append("<div class=\"hash\">").append(esc(sale.getHashHash())).append("</div>");
            if (sale.getHashControl() != null)
                b.append("<div class=\"muted\" style=\"font-size:9pt\">Controle: ").append(sale.getHashControl()).append("</div>");
        }
        if (sale.getQrCode() != null && !sale.getQrCode().isBlank()) {
            b.append("<div class=\"muted\" style=\"font-size:9pt;margin-top:4px\"><strong>QR Code:</strong></div>");
            b.append("<div class=\"hash\">").append(esc(sale.getQrCode())).append("</div>");
        }

        if (sale.getReprintCount() != null && sale.getReprintCount() > 0) {
            b.append("<div class=\"center\" style=\"font-weight:700;margin-top:8px\">*** SEGUNDA VIA (Reimpressão Nº ")
             .append(sale.getReprintCount()).append(") ***</div>");
        }
        if ("ANULADA".equals(sale.getState()) && sale.getAnnulReason() != null) {
            b.append("<div class=\"center\" style=\"font-weight:700;margin-top:8px\">⚠ DOCUMENTO ANULADO</div>");
            b.append("<div class=\"center muted\" style=\"font-size:9pt\">Motivo: ").append(esc(sale.getAnnulReason())).append("</div>");
            if (sale.getAnnulDate() != null)
                b.append("<div class=\"center muted\" style=\"font-size:9pt\">Data Anulação: ").append(sale.getAnnulDate().format(DT_FMT)).append("</div>");
        }

        b.append("<div class=\"footer\">Documento processado por SGV — Sistema de Gestão de Vendas</div>");
        b.append("</div>"); // .sheet

        b.append("<script>window.addEventListener('load',function(){var b=document.querySelector('button');if(b)b.focus();});</script>");
        b.append("</body></html>");
        return b.toString();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private void line(StringBuilder b, String label, String value) {
        b.append("<div><span>").append(esc(label)).append("</span><span>").append(esc(value)).append("</span></div>");
    }

    private String fmt(Double v) { return v == null ? "0.00" : String.format("%,.2f", v); }
    private String fmtQty(Double v) { return v == null ? "0" : String.format("%,.2f", v); }
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() > len ? s.substring(0, len - 1) + "…" : s;
    }
    private String formatNuit(String nuit) {
        if (nuit == null) return "";
        String n = nuit.replaceAll("\\D", "");
        if (n.length() != 9) return nuit;
        return n.substring(0, 3) + " " + n.substring(3, 6) + " " + n.substring(6, 9);
    }
}
