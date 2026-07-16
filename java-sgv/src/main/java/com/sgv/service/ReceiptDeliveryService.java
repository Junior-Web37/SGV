package com.sgv.service;

import com.sgv.entity.AppConfig;
import com.sgv.entity.Sale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Envio do recibo digital por WhatsApp/SMS.
 *
 * Para WhatsApp Business API, utiliza o endpoint /messages do Meta.
 * Para SMS, pode usar Twilio, Africa's Talking, ou outro provider.
 *
 * Se as credenciais não estiverem configuradas, devolve um link de partilha manual
 * via wa.me/{numero}?text=... (o caixa pode enviar directamente do telemóvel).
 */
@Service
public class ReceiptDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptDeliveryService.class);

    private final AppConfigService appConfigService;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ReceiptDeliveryService(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    public Map<String, Object> deliverWhatsApp(Sale sale, String phoneNumber) {
        AppConfig cfg = appConfigService.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", "whatsapp");
        result.put("recipient", phoneNumber);

        String message = buildMessage(sale);

        // ─── Modo fallback: link wa.me ─────────────────────────────────────────
        if (cfg.getWhatsappApiKey() == null || cfg.getWhatsappApiKey().isBlank()) {
            String normalized = normalizePhone(phoneNumber);
            String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String shareUrl = "https://wa.me/" + normalized + "?text=" + encoded;
            result.put("status", "manual_link");
            result.put("shareUrl", shareUrl);
            result.put("hint", "API WhatsApp Business não configurada — abra este link para enviar manualmente.");
            log.info("Recibo {} enviado via wa.me link para {}", sale.getId(), normalized);
            return result;
        }

        // ─── Modo API WhatsApp Business ────────────────────────────────────────
        try {
            String url = "https://graph.facebook.com/v17.0/" + cfg.getWhatsappPhoneNumber() + "/messages";
            String body = "{\"messaging_product\":\"whatsapp\",\"to\":\"" + normalizePhone(phoneNumber) + "\","
                    + "\"type\":\"text\",\"text\":{\"body\":\"" + escapeJson(message) + "\"}}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + cfg.getWhatsappApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            result.put("status", resp.statusCode() == 200 ? "sent" : "error");
            result.put("response", resp.body());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            log.error("Erro ao enviar WhatsApp", e);
        }
        return result;
    }

    public Map<String, Object> deliverSms(Sale sale, String phoneNumber) {
        AppConfig cfg = appConfigService.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", "sms");
        result.put("recipient", phoneNumber);

        String message = buildShortMessage(sale);

        if (cfg.getSmsApiKey() == null || cfg.getSmsApiKey().isBlank()) {
            result.put("status", "not_configured");
            result.put("hint", "Configure smsApiKey e smsSender no setup para activar SMS.");
            return result;
        }

        try {
            // Africa's Talking API (exemplo) — substitua pelo seu provider
            String url = "https://api.africastalking.com/version1/messaging";
            String body = "username=" + URLEncoder.encode("sandbox", StandardCharsets.UTF_8)
                    + "&to=" + URLEncoder.encode(normalizePhone(phoneNumber), StandardCharsets.UTF_8)
                    + "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                    + "&from=" + URLEncoder.encode(cfg.getSmsSender() != null ? cfg.getSmsSender() : "SGV", StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apiKey", cfg.getSmsApiKey())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            result.put("status", resp.statusCode() == 201 ? "sent" : "error");
            result.put("response", resp.body());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        return result;
    }

    /** Devolve apenas o conteúdo textual do recibo (útil para UI / preview). */
    public String preview(Sale sale) {
        return buildMessage(sale);
    }

    private String buildMessage(Sale sale) {
        StringBuilder sb = new StringBuilder();
        String branchName = sale.getBranch() != null && sale.getBranch().getName() != null ? sale.getBranch().getName() : "SGV";
        sb.append("*").append(branchName).append("*\n");
        sb.append("Documento: ").append(sale.getDocumentType()).append(" ")
                .append(sale.getSeries()).append("/").append(sale.getDocumentNumber()).append("\n");
        if (sale.getCreatedAt() != null) sb.append("Data: ").append(sale.getCreatedAt().toLocalDate()).append("\n");
        sb.append("Total: *").append(String.format("%.2f MT", sale.getTotal() != null ? sale.getTotal() : 0)).append("*\n");
        if (sale.getHashHash() != null) sb.append("HASH AT: ").append(sale.getHashHash()).append("\n");
        if (sale.getBranch() != null && sale.getBranch().getNuit() != null)
            sb.append("NUIT: ").append(sale.getBranch().getNuit()).append("\n");
        sb.append("\nObrigado pela preferência!");
        return sb.toString();
    }

    private String buildShortMessage(Sale sale) {
        return String.format("%s %s/%d Total %.2f MT",
                sale.getDocumentType(), sale.getSeries(),
                sale.getDocumentNumber(),
                sale.getTotal() != null ? sale.getTotal() : 0);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String s = phone.replaceAll("[^0-9+]", "");
        // Adiciona prefixo MZ se faltar
        if (s.startsWith("8") && s.length() == 9) s = "+258" + s;
        else if (!s.startsWith("+") && !s.startsWith("258")) s = "+258" + s;
        return s.replace("+", "");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}