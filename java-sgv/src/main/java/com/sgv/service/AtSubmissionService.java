package com.sgv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgv.entity.Sale;
import com.sgv.entity.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Cliente HTTP para submissão de documentos fiscais à Autoridade Tributária.
 *
 * Existem 2 modos:
 *  1. PRODUÇÃO: envia para https://api.at.gov.mz/... (endpoint oficial, a confirmar)
 *  2. SANDBOX / DEMO: simula envio com resposta OK; nunca toca no exterior
 *
 * O endpoint oficial AT MZ é gerido pelo Portal e-Fatura. Para activar produção,
 * configure em /api/setup/flags os campos:
 *   - atProductionEndpoint
 *   - atApiKey
 *   - atApiSecret
 *
 * Enquanto não estão configurados, todas as submissões ficam marcadas como
 * "submittedToAt=true" sem sair do sistema.
 */
@Service
public class AtSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(AtSubmissionService.class);

    @Value("${sgv.at.submission-endpoint:}")
    private String productionEndpoint;

    private final AppConfigService appConfigService;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AtSubmissionService(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    /**
     * Submete um documento fiscal à AT.
     *
     * @param sale venda recém-emitida
     * @return resultado com status, código AT, mensagem
     */
    public Map<String, Object> submit(Sale sale) {
        AppConfig cfg = appConfigService.get();
        String endpoint = productionEndpoint;
        String apiKey = cfg.getSoftwareCertNumber();
        String license = cfg.getLicenseNumber();

        Map<String, Object> payload = buildPayload(sale);

        // ─── Modo sandbox/dry-run ───────────────────────────────────────────────
        if (endpoint == null || endpoint.isBlank() || "true".equals(System.getProperty("sgv.at.dryrun"))) {
            log.info("[AT dry-run] Documento {} {}/{} simulado (endpoint não configurado)",
                    sale.getDocumentType(), sale.getSeries(), sale.getDocumentNumber());
            return Map.of(
                    "status", "simulated",
                    "submittedAt", new Date().toString(),
                    "endpoint", "(dry-run)",
                    "payload", payload,
                    "hint", "Endpoint AT não configurado — configure sgv.at.submission-endpoint para submissão real"
            );
        }

        try {
            String body = mapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey != null ? apiKey : "")
                    .header("X-License", license != null ? license : "")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            log.info("AT submissão: {} {}/{} → HTTP {}",
                    sale.getDocumentType(), sale.getSeries(), sale.getDocumentNumber(), resp.statusCode());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", resp.statusCode() == 200 || resp.statusCode() == 201 ? "submitted" : "error");
            result.put("httpStatus", resp.statusCode());
            result.put("response", resp.body());

            try {
                Map<?, ?> body2 = mapper.readValue(resp.body(), Map.class);
                if (body2.containsKey("atDocumentId")) result.put("atDocumentId", body2.get("atDocumentId"));
                if (body2.containsKey("validationErrors")) result.put("validationErrors", body2.get("validationErrors"));
                if (body2.containsKey("atStatus")) result.put("atStatus", body2.get("atStatus"));
            } catch (Exception ex) {
                log.warn("Não foi possível analisar o corpo da resposta da AT como JSON: {}", ex.getMessage());
            }
            return result;
        } catch (Exception e) {
            log.error("Erro a submeter à AT", e);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("hint", "Verifique a ligação à internet e o endpoint AT configurado");
            return result;
        }
    }

    /**
     * Constrói o payload JSON no formato esperado pela AT.
     */
    private Map<String, Object> buildPayload(Sale sale) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("tipoDocumento", sale.getDocumentType());
        p.put("serie", sale.getSeries());
        p.put("numero", sale.getDocumentNumber());
        p.put("ano", sale.getDocumentYear());
        p.put("dataEmissao", sale.getCreatedAt() != null ? sale.getCreatedAt().toString() : null);
        p.put("hashHash", sale.getHashHash());
        p.put("hashControl", sale.getHashControl());

        if (sale.getBranch() != null) {
            Map<String, Object> emitter = new LinkedHashMap<>();
            emitter.put("nuit", sale.getBranch().getNuit());
            emitter.put("nome", sale.getBranch().getName());
            emitter.put("endereco", sale.getBranch().getAddress());
            emitter.put("softwareCertNumber", sale.getBranch().getSoftwareCertNumber());
            p.put("emissor", emitter);
        }

        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("nome", sale.getCustomerName());
        customer.put("nuit", sale.getCustomerNuit());
        customer.put("endereco", sale.getCustomerAddress());
        p.put("cliente", customer);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("subtotal", sale.getSubtotal());
        totals.put("desconto", sale.getTotalDiscount());
        totals.put("ice", sale.getTotalIce());
        totals.put("iva", sale.getTotalTax());
        totals.put("retencao", sale.getWithholdingTax());
        totals.put("total", sale.getTotal());
        totals.put("moeda", sale.getCurrency());
        p.put("totais", totals);

        List<Map<String, Object>> lines = new ArrayList<>();
        if (sale.getItems() != null) {
            for (var item : sale.getItems()) {
                Map<String, Object> line = new LinkedHashMap<>();
                line.put("descricao", item.getDescription());
                line.put("quantidade", item.getQty());
                line.put("precoUnitario", item.getUnitPrice());
                line.put("desconto", item.getLineDiscount());
                line.put("taxaIva", item.getTaxRate());
                line.put("tipoImposto", item.getTaxType());
                line.put("motivoInexistTax", item.getMotivoInexistTax());
                line.put("taxaIce", item.getIceRate());
                line.put("valorIce", item.getLineIce());
                line.put("valorIva", item.getLineTax());
                line.put("totalLinha", item.getLineTotal());
                lines.add(line);
            }
        }
        p.put("linhas", lines);

        List<Map<String, Object>> payments = new ArrayList<>();
        if (sale.getPayments() != null) {
            for (var pay : sale.getPayments()) {
                Map<String, Object> p2 = new LinkedHashMap<>();
                p2.put("metodo", pay.getMethod());
                p2.put("valor", pay.getAmount());
                p2.put("terminalRef", pay.getTerminalRef());
                p2.put("tipoCartao", pay.getCardType());
                payments.add(p2);
            }
        }
        p.put("pagamentos", payments);

        return p;
    }
}