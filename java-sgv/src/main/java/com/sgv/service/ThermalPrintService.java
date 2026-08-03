package com.sgv.service;

import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Impressão térmica em PDF (80mm). Compatível com impressoras térmicas de 80mm
 * (Epson, Star, Xprinter, etc.) que interpretam PDFs simples.
 *
 * Para integração directa ESC/POS via impressora USB/serial, ver {@link com.sgv.service.ThermalPrinterService}.
 */
@Service
public class ThermalPrintService {

    // Largura típica de um rolo 80mm ≈ 226pt
    private static final float PAGE_WIDTH = 226f;
    private static final float PAGE_HEIGHT = 800f;
    private static final float MARGIN = 6f;
    private static final float LINE_HEIGHT = 11f;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public File printReceipt(Sale sale) throws IOException {
        File outputDir = new File(System.getProperty("user.home"), "Documents/SGV/recibos");
        outputDir.mkdirs();
        String docType = sale.getDocumentType() != null ? sale.getDocumentType() : "DOC";
        String fileName = "T_" + docType + "_" + (sale.getSeries() != null ? sale.getSeries() : "A")
                + "_" + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : sale.getId())
                + ".pdf";
        File outputFile = new File(outputDir, fileName);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            doc.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.COURIER);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // HEADER
                String branchName = sale.getBranch() != null ? sale.getBranch().getName() : "SGV";
                String branchNuit = sale.getBranch() != null && sale.getBranch().getNuit() != null ? sale.getBranch().getNuit() : "";
                String branchAddr = sale.getBranch() != null && sale.getBranch().getAddress() != null ? sale.getBranch().getAddress() : "";

                y = centeredText(cs, fontBold, 12, branchName, y);
                if (!branchNuit.isBlank()) y = centeredText(cs, fontReg, 8, "NUIT: " + branchNuit, y);
                if (!branchAddr.isBlank()) y = centeredText(cs, fontReg, 7, branchAddr, y);

                String cert = sale.getBranch() != null ? sale.getBranch().getSoftwareCertNumber() : null;
                if (cert != null && !cert.isBlank()) y = centeredText(cs, fontReg, 7, "Cert: " + cert, y);

                y = drawLine(cs, y);

                // DOCUMENT TITLE
                String title = docType + " " + (sale.getSeries() != null ? sale.getSeries() : "A") + "/"
                        + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : sale.getId());
                y = centeredText(cs, fontBold, 11, title, y);
                if (sale.getCreatedAt() != null) y = centeredText(cs, fontReg, 8, sale.getCreatedAt().format(DT_FMT), y);
                y = drawLine(cs, y);

                // CLIENT
                String custName = sale.getCustomerName() != null ? sale.getCustomerName() : "Consumidor Final";
                String custNuit = sale.getCustomerNuit() != null ? sale.getCustomerNuit() : "999999999";
                y = leftText(cs, fontReg, 8, "Cliente: " + truncate(custName, 26), MARGIN, y);
                y = leftText(cs, fontReg, 8, "NUIT:    " + custNuit, MARGIN, y);
                if (sale.getCustomerAddress() != null && !sale.getCustomerAddress().isBlank()) {
                    y = leftText(cs, fontReg, 7, "Morada:  " + truncate(sale.getCustomerAddress(), 26), MARGIN, y);
                }
                y = drawLine(cs, y);

                // ITEMS HEADER
                y = leftText(cs, fontBold, 8, "Desc                    Qtd  Preco  Total", MARGIN, y);
                y = drawLine(cs, y);

                // ITEMS
                if (sale.getItems() != null) {
                    for (SaleItem item : sale.getItems()) {
                        String desc = item.getProductName() != null ? item.getProductName() :
                                (item.getDescription() != null ? item.getDescription() : "");
                        if (desc.length() > 22) desc = desc.substring(0, 22);
                        y = leftText(cs, fontReg, 8, pad(desc, 22), MARGIN, y);
                        String line = String.format("%6.2f %7.2f %8.2f",
                                item.getQty() != null ? item.getQty() : 0,
                                item.getUnitPrice() != null ? item.getUnitPrice() : 0,
                                item.getLineTotal() != null ? item.getLineTotal() : 0);
                        y = leftText(cs, fontReg, 8, line, MARGIN, y);
                        if (y < 200) break; // limite do rolo
                    }
                }
                y = drawLine(cs, y);

                // TOTALS
                y = rightText(cs, fontReg, 8, "Subtotal:  " + fmt(sale.getSubtotal()), y);
                if (sale.getTotalDiscount() != null && sale.getTotalDiscount() > 0)
                    y = rightText(cs, fontReg, 8, "Desconto:  " + fmt(sale.getTotalDiscount()), y);
                if (sale.getTotalIce() != null && sale.getTotalIce() > 0)
                    y = rightText(cs, fontReg, 8, "ICE:       " + fmt(sale.getTotalIce()), y);
                y = rightText(cs, fontReg, 8, "IVA:       " + fmt(sale.getTotalTax()), y);
                if (sale.getWithholdingTax() != null && sale.getWithholdingTax() > 0)
                    y = rightText(cs, fontReg, 8, "Retencao: -" + fmt(sale.getWithholdingTax()), y);
                y = drawLine(cs, y);
                y = rightText(cs, fontBold, 11, "TOTAL:     " + fmt(sale.getTotal()) + " MT", y);
                if (sale.getPaidAmount() != null && sale.getPaidAmount() > 0) {
                    y = rightText(cs, fontReg, 8, "Pago:      " + fmt(sale.getPaidAmount()), y);
                }
                if (sale.getChangeAmount() != null && sale.getChangeAmount() > 0) {
                    y = rightText(cs, fontReg, 8, "Troco:     " + fmt(sale.getChangeAmount()), y);
                }
                y = drawLine(cs, y);

                // PAGAMENTOS
                if (sale.getPayments() != null && !sale.getPayments().isEmpty()) {
                    y = leftText(cs, fontBold, 8, "Pagamentos:", MARGIN, y);
                    for (Payment p : sale.getPayments()) {
                        String method = p.getMethod() != null ? p.getMethod() : "";
                        y = leftText(cs, fontReg, 8, "  " + method + ": " + fmt(p.getAmount()), MARGIN, y);
                    }
                    y = drawLine(cs, y);
                }

                // HASH AT
                if (sale.getHashHash() != null && !sale.getHashHash().isBlank()) {
                    y = leftText(cs, fontBold, 8, "HASH AT: " + sale.getHashHash(), MARGIN, y);
                    y = leftText(cs, fontReg, 7, "         " + sale.getHashHash().substring(0, Math.min(0, 0)), MARGIN, y);
                }
                if (sale.getHashControl() != null) {
                    y = leftText(cs, fontReg, 7, "Controle: " + sale.getHashControl(), MARGIN, y);
                }

                // QR (texto simples — o cliente pode converter)
                if (sale.getQrCode() != null && !sale.getQrCode().isBlank()) {
                    y -= 5;
                    y = leftText(cs, fontReg, 7, "QR: " + truncate(sale.getQrCode(), 30), MARGIN, y);
                }

                y = drawLine(cs, y);
                y = centeredText(cs, fontReg, 7, "Documento processado por SGV", y);
                y = centeredText(cs, fontReg, 7, "Obrigado pela preferencia!", y);

                if ("ANULADA".equals(sale.getState())) {
                    y -= 10;
                    y = centeredText(cs, fontBold, 10, "*** DOCUMENTO ANULADO ***", y);
                    if (sale.getAnnulReason() != null) y = centeredText(cs, fontReg, 7, "Motivo: " + sale.getAnnulReason(), y);
                }
            }
            doc.save(outputFile);
        }
        return outputFile;
    }

    private float drawLine(PDPageContentStream cs, float y) throws IOException {
        cs.setLineWidth(0.3f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(PAGE_WIDTH - MARGIN, y);
        cs.stroke();
        return y - 4;
    }

    private float centeredText(PDPageContentStream cs, PDType1Font font, float size, String text, float y) throws IOException {
        float w = font.getStringWidth(text) / 1000 * size;
        float x = (PAGE_WIDTH - w) / 2;
        return leftText(cs, font, size, text, x, y);
    }

    private float leftText(PDPageContentStream cs, PDType1Font font, float size, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - LINE_HEIGHT;
    }

    private float rightText(PDPageContentStream cs, PDType1Font font, float size, String text, float y) throws IOException {
        float w = font.getStringWidth(text) / 1000 * size;
        return leftText(cs, font, size, text, PAGE_WIDTH - MARGIN - w, y);
    }

    private String fmt(Double v) { return v != null ? String.format("%9.2f", v) : "     0.00"; }
    private String pad(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }
    private String truncate(String s, int len) {
        return s.length() > len ? s.substring(0, len - 1) + "…" : s;
    }
    private String sanitize(String text) {
        if (text == null) return "";
        return text.replaceAll("[^\u0000-\u00FF]", "?");
    }
}
