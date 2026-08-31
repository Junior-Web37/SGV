package com.billywater.servico;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Geração de relatórios (OpenPDF) + CSV + talão REC. */
public final class GeradorPDF {

    private static final Font TITLE = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUB = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font HDR = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL);

    private GeradorPDF() {}

    public static Path gerarRelatorio(String codigo, String periodo, List<String> colunas, List<Object[]> linhas) throws Exception {
        Path dir = Paths.get("relatorios");
        Files.createDirectories(dir);
        Path out = dir.resolve(codigo + "_" + (periodo == null ? LocalDate.now() : periodo) + ".pdf");
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(out.toFile()));
        doc.open();
        doc.add(new Paragraph("BILLY WATER", TITLE));
        doc.add(new Phrase("Relatório " + codigo + " · Período " + (periodo == null ? "" : periodo) + " · " + LocalDateTime.now().toString().replace("T", " "), SUB));
        doc.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(Math.max(1, colunas.size()));
        table.setWidthPercentage(100);
        for (String h : colunas) { PdfPCell c = new PdfPCell(new Phrase(h, HDR)); c.setPadding(6); c.setHorizontalAlignment(Element.ALIGN_CENTER); table.addCell(c); }
        if (linhas.isEmpty()) { PdfPCell vazio = new PdfPCell(new Phrase("(sem dados)", BODY)); vazio.setColspan(Math.max(1, colunas.size())); vazio.setPadding(8); table.addCell(vazio); }
        else for (Object[] row : linhas) for (Object v : row) { PdfPCell c = new PdfPCell(new Phrase(v == null ? "" : v.toString(), BODY)); c.setPadding(5); table.addCell(c); }
        doc.add(table);
        doc.close();
        return out;
    }

    public static Path exportarCsv(String codigo, String periodo, List<String> colunas, List<Object[]> linhas) throws Exception {
        Path dir = Paths.get("relatorios");
        Files.createDirectories(dir);
        Path out = dir.resolve(codigo + "_" + (periodo == null ? LocalDate.now() : periodo) + ".csv");
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(";", colunas)).append('\n');
        for (Object[] row : linhas) {
            for (int i = 0; i < row.length; i++) { if (i > 0) sb.append(';'); Object v = row[i]; sb.append(v == null ? "" : String.valueOf(v).replace(";", ",")); }
            sb.append('\n');
        }
        Files.writeString(out, sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
        return out;
    }

    public static Path gerarRecibo(Long recNumero, String cliente, String operador, String metodo, String valor, String recebido, String troco, String empresa) throws Exception {
        Path dir = Paths.get("recibos");
        Files.createDirectories(dir);
        Path out = dir.resolve("REC_" + recNumero + ".pdf");
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(out.toFile()));
        doc.open();
        doc.add(new Paragraph(empresa == null ? "BILLY WATER" : empresa, TITLE));
        doc.add(new Phrase("Recibo de pagamento (REC " + recNumero + ")", SUB));
        doc.add(new Paragraph(" "));
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        linhaDoc(t, "Cliente", cliente);
        linhaDoc(t, "Método", metodo);
        linhaDoc(t, "Valor (MT)", valor);
        linhaDoc(t, "Recebido (MT)", recebido);
        linhaDoc(t, "Troco (MT)", troco);
        linhaDoc(t, "Operador", operador);
        linhaDoc(t, "Emissão", LocalDateTime.now().toString().replace("T", " "));
        doc.add(t);
        doc.close();
        return out;
    }

    private static void linhaDoc(PdfPTable t, String rotulo, String valor) {
        PdfPCell a = new PdfPCell(new Phrase(rotulo, HDR)); a.setPadding(5);
        PdfPCell b = new PdfPCell(new Phrase(valor == null ? "" : valor, BODY)); b.setPadding(5);
        t.addCell(a); t.addCell(b);
    }
}
