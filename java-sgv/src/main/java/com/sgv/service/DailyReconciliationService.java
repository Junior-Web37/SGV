package com.sgv.service;

import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Reconciliação de fim de dia — confere séries, totais por tipo de imposto,
 * total retido, facturas vs anuladas.
 */
@Service
public class DailyReconciliationService {

    private final SaleRepository saleRepository;

    public DailyReconciliationService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public Map<String, Object> reconcile(LocalDate date, Long branchId) {
        List<Sale> allSales = saleRepository.findAll();
        List<Sale> daySales = allSales.stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().toLocalDate().equals(date))
                .filter(s -> branchId == null || (s.getBranch() != null && s.getBranch().getId().equals(branchId)))
                .filter(s -> !Boolean.TRUE.equals(s.getDemoFlag()))
                .toList();

        List<Sale> emitted = daySales.stream().filter(s -> "EMITIDA".equals(s.getState())).toList();
        List<Sale> annulled = daySales.stream().filter(s -> "ANULADA".equals(s.getState())).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("branchId", branchId);
        result.put("totalDocuments", daySales.size());
        result.put("emittedCount", emitted.size());
        result.put("annulledCount", annulled.size());

        // ─── Totais gerais ─────────────────────────────────────────────────────
        double totalVendas = emitted.stream().mapToDouble(s -> safe(s.getTotal())).sum();
        double totalIva = emitted.stream().mapToDouble(s -> safe(s.getTotalTax())).sum();
        double totalIce = emitted.stream().mapToDouble(s -> safe(s.getTotalIce())).sum();
        double totalDesconto = emitted.stream().mapToDouble(s -> safe(s.getTotalDiscount())).sum();
        double totalRetencao = emitted.stream().mapToDouble(s -> safe(s.getWithholdingTax())).sum();

        result.put("totalVendas", round(totalVendas));
        result.put("totalIva", round(totalIva));
        result.put("totalIce", round(totalIce));
        result.put("totalDesconto", round(totalDesconto));
        result.put("totalRetencao", round(totalRetencao));

        // ─── Total por tipo de documento ───────────────────────────────────────
        Map<String, Map<String, Object>> porTipo = new LinkedHashMap<>();
        for (Sale s : emitted) {
            String type = s.getDocumentType() != null ? s.getDocumentType() : "OUTROS";
            porTipo.computeIfAbsent(type, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("count", 0);
                m.put("total", 0.0);
                return m;
            });
            Map<String, Object> m = porTipo.get(type);
            m.put("count", (int) m.get("count") + 1);
            m.put("total", round((double) m.get("total") + safe(s.getTotal())));
        }
        result.put("porTipoDocumento", porTipo);

        // ─── Total por taxa de IVA ─────────────────────────────────────────────
        Map<String, Map<String, Object>> porTaxa = new TreeMap<>();
        for (Sale s : emitted) {
            if (s.getItems() == null) continue;
            for (SaleItem item : s.getItems()) {
                String key = String.format("%.2f", safe(item.getTaxRate()));
                porTaxa.computeIfAbsent(key, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("taxRate", Double.parseDouble(k));
                    m.put("base", 0.0);
                    m.put("tax", 0.0);
                    m.put("count", 0);
                    return m;
                });
                Map<String, Object> m = porTaxa.get(key);
                m.put("base", round((double) m.get("base") + safe(item.getLineBase()) - safe(item.getLineDiscount())));
                m.put("tax", round((double) m.get("tax") + safe(item.getLineTax())));
                m.put("count", (int) m.get("count") + 1);
            }
        }
        result.put("porTaxaIva", new ArrayList<>(porTaxa.values()));

        // ─── Séries: primeira e última factura ─────────────────────────────────
        Map<String, Map<String, Object>> series = new TreeMap<>();
        for (Sale s : daySales) {
            String key = (s.getSeries() != null ? s.getSeries() : "A") + "/" + s.getDocumentType();
            series.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("first", s.getDocumentNumber());
                m.put("last", s.getDocumentNumber());
                m.put("count", 0);
                return m;
            });
            Map<String, Object> m = series.get(key);
            long num = s.getDocumentNumber() != null ? s.getDocumentNumber() : 0L;
            m.put("first", Math.min((long) m.get("first"), num));
            m.put("last", Math.max((long) m.get("last"), num));
            m.put("count", (int) m.get("count") + 1);
        }
        result.put("series", series);

        // ─── Anulações ─────────────────────────────────────────────────────────
        List<Map<String, Object>> annulledList = new ArrayList<>();
        for (Sale s : annulled) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", s.getId());
            a.put("document", s.getDocumentType() + " " + s.getSeries() + "/" + s.getDocumentNumber());
            a.put("reason", s.getAnnulReason());
            a.put("annulDate", s.getAnnulDate());
            a.put("total", s.getTotal());
            annulledList.add(a);
        }
        result.put("annulled", annulledList);

        // ─── Formas de pagamento ───────────────────────────────────────────────
        Map<String, Double> porFormaPagamento = new LinkedHashMap<>();
        for (Sale s : emitted) {
            if (s.getPayments() != null) {
                for (var p : s.getPayments()) {
                    String method = p.getMethod() != null ? p.getMethod() : "OUTROS";
                    porFormaPagamento.merge(method, safe(p.getAmount()), Double::sum);
                }
            } else if (s.getPaymentMethod() != null) {
                porFormaPagamento.merge(s.getPaymentMethod(), safe(s.getTotal()), Double::sum);
            }
        }
        Map<String, Double> porFormaPagamentoRounded = new LinkedHashMap<>();
        porFormaPagamento.forEach((k, v) -> porFormaPagamentoRounded.put(k, round(v)));
        result.put("porFormaPagamento", porFormaPagamentoRounded);

        // ─── Discrepâncias (placeholder para futura lógica avançada) ──────────
        List<String> discrepancies = new ArrayList<>();
        for (Map<String, Object> ser : series.values()) {
            long first = (long) ser.get("first");
            long last = (long) ser.get("last");
            int count = (int) ser.get("count");
            long expected = last - first + 1;
            if (expected != count) {
                discrepancies.add(String.format(
                    "Série %s: contagem %d mas intervalo [%d-%d] espera %d documentos",
                    ser.containsKey("series") ? ser.get("series") : "?", count, first, last, expected));
            }
        }
        result.put("discrepancies", discrepancies);
        result.put("hasDiscrepancies", !discrepancies.isEmpty());

        return result;
    }

    private double safe(Double v) { return v != null ? v : 0.0; }
    private double round(double v) { return Math.round(v * 100.0) / 100.0; }
}