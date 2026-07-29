package com.sgv.service;

import com.sgv.entity.Sale;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SafTExportService {

    public Path exportSalesToXml(List<Sale> sales, Path outputPath) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Sales>");
        xml.append("<GeneratedAt>").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))).append("</GeneratedAt>");
        for (Sale sale : sales) {
            xml.append("<Sale>");
            xml.append("<Document>").append(escape(safe(sale.getSeries()) + "/" + safe(sale.getDocumentNumber()))).append("</Document>");
            xml.append("<Date>").append(sale.getCreatedAt() != null ? sale.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "").append("</Date>");
            xml.append("<Customer>").append(escape(safe(sale.getCustomerName()))).append("</Customer>");
            xml.append("<Subtotal>").append(formatMoney(sale.getSubtotal())).append("</Subtotal>");
            xml.append("<Tax>").append(formatMoney(sale.getTotalTax())).append("</Tax>");
            xml.append("<Total>").append(formatMoney(sale.getTotal())).append("</Total>");
            xml.append("</Sale>");
        }
        xml.append("</Sales>");

        Files.createDirectories(outputPath.toAbsolutePath().getParent());
        Files.writeString(outputPath, xml.toString(), StandardCharsets.UTF_8);
        return outputPath;
    }

    private String safe(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String formatMoney(Object value) {
        if (value == null) return "0.00";
        if (value instanceof Number number) {
            return String.format(java.util.Locale.US, "%.2f", number.doubleValue());
        }
        return String.format(java.util.Locale.US, "%.2f", Double.parseDouble(String.valueOf(value)));
    }
}
