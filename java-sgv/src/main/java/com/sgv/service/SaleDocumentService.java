package com.sgv.service;

import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SaleDocumentService {

    private static final float MARGIN = 40f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Generates a PDF for the given Sale and saves it to the user's Documents/SGV/recibos folder.
     * Returns the file path.
     */
    public File generateDocument(Sale sale) throws IOException {
        File outputDir = new File(System.getProperty("user.home"), "Documents/SGV/recibos");
        outputDir.mkdirs();
        String docType = sale.getDocumentType() != null ? sale.getDocumentType() : "DOCUMENTO";
        String fileName = docType + "_" + (sale.getSeries() != null ? sale.getSeries() : "A")
                + "_" + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : sale.getId())
                + ".pdf";
        File outputFile = new File(outputDir, fileName);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fontMono = new PDType1Font(Standard14Fonts.FontName.COURIER);

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                float y = PAGE_HEIGHT - MARGIN;

                // ─── HEADER ──────────────────────────────────────────────
                // Company name
                String branchName = sale.getBranch() != null ? sale.getBranch().getName() : "SGV Sistema";
                String branchNuit = sale.getBranch() != null ? (sale.getBranch().getNuit() != null ? sale.getBranch().getNuit() : "") : "";
                String branchAddress = sale.getBranch() != null ? (sale.getBranch().getAddress() != null ? sale.getBranch().getAddress() : "") : "";

                y = drawCenteredText(cs, fontBold, 16, branchName, y);
                if (!branchNuit.isBlank()) y = drawCenteredText(cs, fontRegular, 10, "NUIT: " + branchNuit, y - 2);
                if (!branchAddress.isBlank()) y = drawCenteredText(cs, fontRegular, 10, branchAddress, y - 2);

                // ─── Certificado de software / Licença (AT) ─────────────────────
                String certNum = sale.getBranch() != null ? sale.getBranch().getSoftwareCertNumber() : null;
                String licenseNum = sale.getBranch() != null ? sale.getBranch().getLicenseNumber() : null;
                if (certNum != null && !certNum.isBlank()) y = drawCenteredText(cs, fontRegular, 8, "Certificado de Software AT: " + certNum, y - 2);
                if (licenseNum != null && !licenseNum.isBlank()) y = drawCenteredText(cs, fontRegular, 8, "Licença: " + licenseNum, y - 2);
                // ─────────────────────────────────────────────────────────────────

                y -= 10;
                drawHorizontalLine(cs, y, 1.5f);
                y -= 12;

                // Document title
                String title = docType + " Nº " + (sale.getSeries() != null ? sale.getSeries() : "A") + "/"
                        + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : sale.getId())
                        + "/" + (sale.getDocumentYear() != null ? sale.getDocumentYear() : "");
                y = drawCenteredText(cs, fontBold, 14, title, y);
                y -= 6;
                drawHorizontalLine(cs, y, 0.5f);
                y -= 14;

                // ─── DATE & STATUS ────────────────────────────────────────
                String dateStr = sale.getCreatedAt() != null ? sale.getCreatedAt().format(DT_FMT) : "—";
                y = drawTwoColumnRow(cs, fontRegular, fontBold, 10, "Data:", dateStr, y);
                y = drawTwoColumnRow(cs, fontRegular, fontBold, 10, "Estado:", sale.getState() != null ? sale.getState() : "EMITIDA", y);
                String payMethod = sale.getPaymentMethod() != null ? sale.getPaymentMethod() : "—";
                y = drawTwoColumnRow(cs, fontRegular, fontBold, 10, "Forma de Pagamento:", payMethod, y);
                y -= 10;

                // ─── CLIENT INFO ──────────────────────────────────────────
                drawHorizontalLine(cs, y, 0.5f);
                y -= 14;
                y = drawText(cs, fontBold, 11, "DADOS DO CLIENTE", MARGIN, y);
                y -= 6;
                String custName = sale.getCustomerName() != null ? sale.getCustomerName() : "Consumidor Final";
                String custNuit = sale.getCustomerNuit() != null ? sale.getCustomerNuit() : "999999999";
                String custAddr = sale.getCustomerAddress() != null ? sale.getCustomerAddress() : "";
                y = drawTwoColumnRow(cs, fontRegular, fontRegular, 10, "Nome:", custName, y);
                y = drawTwoColumnRow(cs, fontRegular, fontRegular, 10, "NUIT:", custNuit, y);
                if (!custAddr.isBlank()) y = drawTwoColumnRow(cs, fontRegular, fontRegular, 10, "Endereço:", custAddr, y);
                y -= 10;

                // ─── ITEMS TABLE ──────────────────────────────────────────
                drawHorizontalLine(cs, y, 0.5f);
                y -= 14;

                // Table header
                float col0 = MARGIN;           // Desc
                float col1 = col0 + 170;       // Qty
                float col2 = col1 + 50;        // Unit Price
                float col3 = col2 + 70;        // Tax Type
                float col4 = col3 + 60;        // Tax%
                float col5 = col4 + 55;        // Total

                y = drawTableHeader(cs, fontBold, y, col0, col1, col2, col3, col4, col5);
                drawHorizontalLine(cs, y, 0.5f);
                y -= 12;

                List<SaleItem> items = sale.getItems();
                if (items != null) {
                    for (SaleItem item : items) {
                        String desc = item.getProductName() != null ? item.getProductName() : (item.getDescription() != null ? item.getDescription() : "—");
                        if (desc.length() > 30) desc = desc.substring(0, 28) + "..";
                        String qty = String.format("%.2f", item.getQty() != null ? item.getQty() : 0);
                        String price = String.format("%.2f", item.getUnitPrice() != null ? item.getUnitPrice() : 0);
                        String tax = String.format("%.0f%%", item.getTaxRate() != null ? item.getTaxRate() : 0);
                        String total = String.format("%.2f", item.getLineTotal() != null ? item.getLineTotal() : 0);
                        String taxTypeAndReason = item.getTaxType() != null ? item.getTaxType() : "IVA";
                        if (item.getMotivoInexistTax() != null && !item.getMotivoInexistTax().isBlank()) {
                            taxTypeAndReason += " (" + item.getMotivoInexistTax() + ")";
                        }

                        cs.beginText();
                        cs.setFont(fontRegular, 9);
                        cs.newLineAtOffset(col0, y); cs.showText(desc);
                        cs.endText();
                        drawTextAt(cs, fontRegular, 9, qty, col1, y);
                        drawTextAt(cs, fontRegular, 9, price, col2, y);
                        drawTextAt(cs, fontRegular, 9, taxTypeAndReason, col3, y);
                        drawTextAt(cs, fontRegular, 9, tax, col4, y);
                        drawTextAt(cs, fontRegular, 9, total, col5, y);

                        y -= 14;
                        if (y < MARGIN + 80) {
                            // Fix #2: Add new page instead of truncating
                            cs.close();
                            page = new PDPage(PDRectangle.A4);
                            doc.addPage(page);
                            cs = new PDPageContentStream(doc, page);
                            y = PAGE_HEIGHT - MARGIN;
                            y = drawTableHeader(cs, fontBold, y, col0, col1, col2, col3, col4, col5);
                            drawHorizontalLine(cs, y, 0.5f);
                            y -= 12;
                        }
                    }
                }

                drawHorizontalLine(cs, y, 0.5f);
                y -= 14;

                // ─── TOTALS ───────────────────────────────────────────────
                float totLabelX = PAGE_WIDTH - MARGIN - 200;
                float totValueX = PAGE_WIDTH - MARGIN - 70;

                y = drawTotalsRow(cs, fontRegular, 10, "Subtotal:", fmtVal(sale.getSubtotal()), totLabelX, totValueX, y);
                if (sale.getTotalDiscount() != null && sale.getTotalDiscount() > 0) {
                    y = drawTotalsRow(cs, fontRegular, 10, "Desconto:", "- " + fmtVal(sale.getTotalDiscount()), totLabelX, totValueX, y);
                }
                y = drawTotalsRow(cs, fontRegular, 10, "IVA (" + getTaxLabel(sale) + "):", fmtVal(sale.getTotalTax()), totLabelX, totValueX, y);
                if (sale.getTotalIce() != null && sale.getTotalIce() > 0) {
                    y = drawTotalsRow(cs, fontRegular, 10, "ICE:", fmtVal(sale.getTotalIce()), totLabelX, totValueX, y);
                }
                if (sale.getWithholdingTax() != null && sale.getWithholdingTax() > 0) {
                    y = drawTotalsRow(cs, fontRegular, 10, "Retenção na Fonte:", "- " + fmtVal(sale.getWithholdingTax()), totLabelX, totValueX, y);
                }
                y -= 4;
                drawHorizontalLine(cs, y, 1f);
                y -= 14;
                y = drawTotalsRow(cs, fontBold, 12, "TOTAL " + (sale.getCurrency() != null ? sale.getCurrency() : "MT") + ":", fmtVal(sale.getTotal()), totLabelX, totValueX, y);
                y -= 4;
                if (sale.getPaidAmount() != null && sale.getPaidAmount() > 0) {
                    y = drawTotalsRow(cs, fontRegular, 10, "Total Pago:", fmtVal(sale.getPaidAmount()), totLabelX, totValueX, y);
                }
                if (sale.getChangeAmount() != null && sale.getChangeAmount() > 0) {
                    y = drawTotalsRow(cs, fontRegular, 10, "Troco:", fmtVal(sale.getChangeAmount()), totLabelX, totValueX, y);
                }
                y -= 14;

                // ─── HASH / SIGNATURE ─────────────────────────────────────
                if (sale.getSignatureHash() != null && !sale.getSignatureHash().isBlank()) {
                    drawHorizontalLine(cs, y, 0.5f);
                    y -= 12;
                    y = drawText(cs, fontRegular, 8, "Assinatura Digital (SHA-256): " + sale.getSignatureHash().substring(0, Math.min(32, sale.getSignatureHash().length())) + "...", MARGIN, y);
                    if (sale.getHashHash() != null && !sale.getHashHash().isBlank()) {
                        y -= 4; // Espaço extra para o próximo hash
                        y = drawText(cs, fontRegular, 8, "HashHash (MD5, AT): " + sale.getHashHash(), MARGIN, y);
                    }
                    if (sale.getHashControl() != null) {
                        y -= 4; // Espaço extra
                        y = drawText(cs, fontRegular, 8, "Controle Hash (AT): " + sale.getHashControl(), MARGIN, y);
                    }
                }
                // ──────────────────────────────────────────────────────────

                // ─── REPRINT NOTICE ───────────────────────────────────────
                if (sale.getReprintCount() != null && sale.getReprintCount() > 0) {
                    y -= 8;
                    y = drawCenteredText(cs, fontBold, 10, "*** SEGUNDA VIA (Reimpressão Nº " + sale.getReprintCount() + ") ***", y);
                }

                // ─── ANULAÇÃO ─────────────────────────────────────────────
                if ("ANULADA".equals(sale.getState()) && sale.getAnnulReason() != null) {
                    y -= 8;
                    y = drawCenteredText(cs, fontBold, 11, "⚠ DOCUMENTO ANULADO", y);
                    y = drawCenteredText(cs, fontRegular, 9, "Motivo: " + sale.getAnnulReason(), y - 2);
                    if (sale.getAnnulDate() != null) {
                        y = drawCenteredText(cs, fontRegular, 9, "Data Anulação: " + sale.getAnnulDate().format(DT_FMT), y - 2);
                    }
                }

                // ─── FOOTER ───────────────────────────────────────────────
                float footerY = MARGIN + 20;
                drawHorizontalLine(cs, footerY + 10, 0.5f);
                drawCenteredText(cs, fontRegular, 8, "Documento processado por SGV — Sistema de Gestão de Vendas", footerY);
            } finally {
                if (cs != null) {
                    cs.close();
                }
            }

            doc.save(outputFile);
        }

        return outputFile;
    }

    private String fmtVal(Double v) {
        return v != null ? String.format("%.2f", v) : "0.00";
    }

    private String getTaxLabel(Sale sale) {
        if (sale.getItems() == null || sale.getItems().isEmpty()) return "IVA (17%)";

        // Check if all items have the same taxType and taxRate
        boolean allSameTax = sale.getItems().stream()
                .map(item -> (item.getTaxType() != null ? item.getTaxType() : "IVA") + "_" + (item.getTaxRate() != null ? item.getTaxRate() : 0) + "_" + (item.getMotivoInexistTax() != null ? item.getMotivoInexistTax() : ""))
                .distinct()
                .count() == 1;

        if (allSameTax) {
            SaleItem firstItem = sale.getItems().get(0);
            String taxType = firstItem.getTaxType() != null ? firstItem.getTaxType() : "IVA";
            double rate = firstItem.getTaxRate() != null ? firstItem.getTaxRate() : 0;
            String motivo = firstItem.getMotivoInexistTax() != null ? firstItem.getMotivoInexistTax() : "";

            if (rate == 0 && !motivo.isBlank()) {
                return String.format("%s (Isenção %s)", taxType, motivo);
            } else if (rate == 0) {
                return taxType + " (Isento)";
            } else {
                return String.format("%s (%.0f%%)", taxType, rate);
            }
        } else {
            return "IVA (várias taxas)";
        }
    }

    private float drawText(PDPageContentStream cs, PDType1Font font, float size, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - (size + 4);
    }

    private void drawTextAt(PDPageContentStream cs, PDType1Font font, float size, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    private float drawCenteredText(PDPageContentStream cs, PDType1Font font, float size, String text, float y) throws IOException {
        float textWidth = font.getStringWidth(sanitize(text)) / 1000 * size;
        float x = (PAGE_WIDTH - textWidth) / 2;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - (size + 4);
    }

    private float drawTwoColumnRow(PDPageContentStream cs, PDType1Font labelFont, PDType1Font valueFont,
                                    float size, String label, String value, float y) throws IOException {
        cs.beginText();
        cs.setFont(labelFont, size);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitize(label));
        cs.endText();
        cs.beginText();
        cs.setFont(valueFont, size);
        cs.newLineAtOffset(MARGIN + 150, y);
        cs.showText(sanitize(value));
        cs.endText();
        return y - (size + 4);
    }

    private float drawTotalsRow(PDPageContentStream cs, PDType1Font font, float size,
                                 String label, String value, float labelX, float valueX, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(labelX, y);
        cs.showText(sanitize(label));
        cs.endText();
        cs.beginText();
        cs.setFont(font, size);
        float textWidth = font.getStringWidth(sanitize(value)) / 1000 * size;
        cs.newLineAtOffset(valueX - textWidth, y);
        cs.showText(sanitize(value));
        cs.endText();
        return y - (size + 5);
    }

    private float drawTableHeader(PDPageContentStream cs, PDType1Font font, float y,
                                   float col0, float col1, float col2, float col3, float col4, float col5) throws IOException {
        cs.setFont(font, 9);
        drawTextAt(cs, font, 9, "Descrição", col0, y);
        drawTextAt(cs, font, 9, "Qtd.", col1, y);
        drawTextAt(cs, font, 9, "Preço Unit.", col2, y);
        drawTextAt(cs, font, 9, "Tipo IVA", col3, y);
        drawTextAt(cs, font, 9, "IVA", col4, y);
        drawTextAt(cs, font, 9, "Total", col5, y);
        return y - 12;
    }

    private void drawHorizontalLine(PDPageContentStream cs, float y, float lineWidth) throws IOException {
        cs.setLineWidth(lineWidth);
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
    }

    /** Fix #9: Remove characters that PDType1Font cannot encode, using better normalization */
    private String sanitize(String text) {
        if (text == null) return "";
        // Replace known special chars not in WinAnsi
        text = text.replace("⚠", "!")
                   .replace("€", "EUR")
                   .replace("“", "\"").replace("”", "\"")
                   .replace("‘", "'").replace("’", "'");
        
        // Remove accents for unsupported chars if possible, then drop the rest
        text = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                   .replaceAll("\\p{M}", "");
                   
        return text.replaceAll("[^\u0000-\u00FF]", "?");
    }

    /**
     * Opens the generated PDF using the system default PDF viewer.
     */
    public void openDocument(File pdfFile) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                throw new UnsupportedOperationException("Ação OPEN não suportada pelo Desktop");
            }
        } catch (Exception e) {
            // Fallback para Windows caso o Desktop falhe
            try {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", pdfFile.getAbsolutePath()});
                } else {
                    throw new RuntimeException("Não foi possível abrir o PDF: " + (e.getMessage() != null ? e.getMessage() : e.toString()), e);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Não foi possível abrir o PDF: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()), ex);
            }
        }
    }
}
