package com.sgv.service;

import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Relatórios simplificados para médias e pequenas lojas.
 * Inclui:
 *  - Vendas de hoje / período
 *  - Top produtos
 *  - Vendas por categoria (via produtos)
 *  - Resumo por forma de pagamento
 *  - Histórico de hashes (auditoria AT)
 *  - Detecção de hashes inconsistentes (fraude interna)
 */
@Service
public class ReportService {

    private final SaleRepository saleRepository;

    public ReportService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public Map<String, Object> today() {
        return period(LocalDate.now(), LocalDate.now());
    }

    /**
     * Universo reportável (correção do BUG-004/028): documentos efectivamente
     * facturados — VENDA, FACTURA e NC (a NC entra com total negativo e abate
     * o apuramento), nunca anulados e nunca demo. Cotações e encomendas são
     * documentos pré-venda: não são receita, não são IVA e não entram em
     * relatório de vendas. Este é o MESMO critério usado pelos agregados da
     * SaleRepository e pelos KPIs do dashboard (paridade verificada por
     * ReportParityTest).
     */
    static boolean isReportable(Sale s) {
        if (s == null) return false;
        String dt = s.getDocumentType() != null ? s.getDocumentType() : "";
        if (!("VENDA".equals(dt) || "FACTURA".equals(dt) || "NC".equals(dt))) return false;
        if ("ANULADA".equals(s.getState())) return false;
        if (Boolean.TRUE.equals(s.getDemoFlag())) return false;
        return true;
    }

    public Map<String, Object> period(LocalDate from, LocalDate to) {
        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null)
                .filter(s -> !Boolean.TRUE.equals(s.getDemoFlag()))
                .filter(s -> {
                    LocalDate d = s.getCreatedAt().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .toList();

        // Correção do BUG-004: o apuramento usa o universo reportável
        // (VENDA/FACTURA/NC) — antes, cotações e encomendas contavam como vendas.
        List<Sale> emitted = sales.stream().filter(ReportService::isReportable).toList();
        List<Sale> annulled = sales.stream().filter(s -> "ANULADA".equals(s.getState())).toList();

        double totalVendas = emitted.stream().mapToDouble(s -> safe(s.getTotal())).sum();
        double totalIva = emitted.stream().mapToDouble(s -> safe(s.getTotalTax())).sum();
        double totalDesconto = emitted.stream().mapToDouble(s -> safe(s.getTotalDiscount())).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("totalSales", emitted.size());
        result.put("annulledCount", annulled.size());
        result.put("totalAmount", round(totalVendas));
        result.put("totalTax", round(totalIva));
        result.put("totalDiscount", round(totalDesconto));
        result.put("averageTicket", emitted.isEmpty() ? 0 : round(totalVendas / emitted.size()));
        return result;
    }

    public Map<String, Object> topProducts(int limit) {
        Map<String, ProductStats> byCode = new LinkedHashMap<>();
        for (Sale sale : saleRepository.findAll()) {
            // Correção do BUG-031/BUG-004: "mais vendidos" contava apenas
            // estado EMITIDA (vendas já pagas) e ignorava o tipo de documento;
            // agora usa o universo reportável — PAGO/EMITIDA/NC, não anulados,
            // não demo.
            if (!isReportable(sale)) continue;
            if (sale.getItems() == null) continue;
            for (SaleItem item : sale.getItems()) {
                String code = item.getProductCode() != null ? item.getProductCode() : item.getDescription();
                if (code == null) continue;
                byCode.computeIfAbsent(code, k -> new ProductStats(item.getDescription() != null ? item.getDescription() : code));
                ProductStats s = byCode.get(code);
                s.qty += safe(item.getQty());
                s.revenue += safe(item.getLineTotal());
                s.count++;
            }
        }

        List<Map<String, Object>> top = byCode.values().stream()
                .sorted((a, b) -> Double.compare(b.revenue, a.revenue))
                .limit(limit)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", s.code);
                    m.put("name", s.name);
                    m.put("qtySold", round(s.qty));
                    m.put("revenue", round(s.revenue));
                    m.put("transactions", s.count);
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("top", top);
        result.put("limit", limit);
        return result;
    }

    public Map<String, Object> paymentMethods(LocalDate from, LocalDate to) {
        Map<String, Double> totals = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Sale sale : saleRepository.findAll()) {
            if (sale.getCreatedAt() == null) continue;
            // Correção do BUG-004: cotações/encomendas não têm vendas — não
            // contam no resumo por forma de pagamento.
            if (!isReportable(sale)) continue;
            LocalDate d = sale.getCreatedAt().toLocalDate();
            if (d.isBefore(from) || d.isAfter(to)) continue;

            if (sale.getPayments() != null) {
                for (var p : sale.getPayments()) {
                    String m = p.getMethod() != null ? p.getMethod() : "OUTROS";
                    totals.merge(m, safe(p.getAmount()), Double::sum);
                    counts.merge(m, 1, Integer::sum);
                }
            } else if (sale.getPaymentMethod() != null) {
                totals.merge(sale.getPaymentMethod(), safe(sale.getTotal()), Double::sum);
                counts.merge(sale.getPaymentMethod(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (var e : totals.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("method", e.getKey());
            m.put("total", round(e.getValue()));
            m.put("count", counts.getOrDefault(e.getKey(), 0));
            m.put("percentage", 0.0);
            list.add(m);
        }
        double grandTotal = list.stream().mapToDouble(m -> (double) m.get("total")).sum();
        for (Map<String, Object> m : list) {
            m.put("percentage", String.format("%.1f%%", ((double) m.get("total")) / grandTotal * 100));
        }
        list.sort((a, b) -> Double.compare((double) b.get("total"), (double) a.get("total")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("totalAmount", round(grandTotal));
        result.put("breakdown", list);
        return result;
    }

    /**
     * NH-15: Histórico de hashes AT para auditoria.
     */
    public List<Map<String, Object>> hashHistory(Long branchId, LocalDate from, LocalDate to) {
        return saleRepository.findAll().stream()
                .filter(s -> s.getHashHash() != null && !s.getHashHash().isBlank())
                .filter(s -> branchId == null || (s.getBranch() != null && s.getBranch().getId().equals(branchId)))
                .filter(s -> {
                    if (s.getCreatedAt() == null) return false;
                    LocalDate d = s.getCreatedAt().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .sorted(Comparator.comparing(Sale::getCreatedAt))
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("document", s.getDocumentType() + " " + s.getSeries() + "/" + s.getDocumentNumber());
                    m.put("hashHash", s.getHashHash());
                    m.put("hashControl", s.getHashControl());
                    m.put("signatureHash", s.getSignatureHash());
                    m.put("createdAt", s.getCreatedAt());
                    m.put("total", s.getTotal());
                    m.put("customerNuit", s.getCustomerNuit());
                    m.put("state", s.getState());
                    return m;
                })
                .toList();
    }

    /**
     * NH-15: Verifica inconsistências no histórico de hashes (auditoria de fraude interna).
     * Detecta: hashes duplicados, número de controlo fora de sequência.
     */
    public Map<String, Object> hashAudit(Long branchId) {
        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> s.getHashHash() != null && !s.getHashHash().isBlank())
                .filter(s -> branchId == null || (s.getBranch() != null && s.getBranch().getId().equals(branchId)))
                .sorted(Comparator.comparing(Sale::getHashControl, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<String, Integer> hashCounts = new LinkedHashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        long expectedControl = 1L;

        for (Sale s : sales) {
            hashCounts.merge(s.getHashHash(), 1, Integer::sum);
            if (s.getHashControl() != null) {
                if (s.getHashControl() < expectedControl) {
                    issues.add(Map.of(
                            "level", "warning",
                            "code", "HASHCONTROL_OUT_OF_ORDER",
                            "document", s.getDocumentType() + " " + s.getSeries() + "/" + s.getDocumentNumber(),
                            "expectedMin", expectedControl,
                            "actual", s.getHashControl()
                    ));
                } else {
                    expectedControl = s.getHashControl() + 1;
                }
            }
        }

        for (var e : hashCounts.entrySet()) {
            if (e.getValue() > 1) {
                issues.add(Map.of(
                        "level", "critical",
                        "code", "DUPLICATE_HASHHASH",
                        "hashHash", e.getKey(),
                        "count", e.getValue(),
                        "message", "Mesmo hashHash em " + e.getValue() + " documentos — possível fraude"
                ));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDocuments", sales.size());
        result.put("issues", issues);
        result.put("issueCount", issues.size());
        result.put("status", issues.isEmpty() ? "clean" : "issues_found");
        return result;
    }

    public Map<String, Object> getAccountsReceivableAging(LocalDate from, LocalDate to) {
        List<Sale> sales = saleRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null)
                // Correção do BUG-004: contas a receber = só VENDA/FACTURA
                // (cotações/encomendas abertas não são dívidas).
                .filter(s -> !"ANULADA".equalsIgnoreCase(s.getState()))
                .filter(s -> {
                    String dt = s.getDocumentType() != null ? s.getDocumentType() : "";
                    return "VENDA".equals(dt) || "FACTURA".equals(dt);
                })
                .filter(s -> {
                    LocalDate date = s.getCreatedAt().toLocalDate();
                    return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
                })
                .toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        double totalDue = 0.0;
        double overdue30 = 0.0;
        double overdue60 = 0.0;
        double overdue90 = 0.0;

        LocalDate today = LocalDate.now();
        for (Sale sale : sales) {
            double total = safe(sale.getTotal());
            double paid = safe(sale.getPaidAmount());
            double due = Math.max(0.0, total - paid);
            if (due <= 0.0) continue;
            LocalDate issueDate = sale.getCreatedAt().toLocalDate();
            long daysOpen = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(issueDate, today));
            String bucket = "0-30 dias";
            if (daysOpen > 90) {
                bucket = ">90 dias";
                overdue90 += due;
            } else if (daysOpen > 60) {
                bucket = "61-90 dias";
                overdue60 += due;
            } else if (daysOpen > 30) {
                bucket = "31-60 dias";
                overdue30 += due;
            }
            totalDue += due;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("document", sale.getSeries() + "/" + sale.getDocumentNumber());
            row.put("customer", sale.getCustomerName() != null ? sale.getCustomerName() : "Consumidor Final");
            row.put("date", issueDate.toString());
            row.put("total", String.format("%.2f", total));
            row.put("paid", String.format("%.2f", paid));
            row.put("due", String.format("%.2f", due));
            row.put("daysOpen", String.valueOf(daysOpen));
            row.put("bucket", bucket);
            row.put("days", daysOpen);
            row.put("id", sale.getId());
            rows.add(row);
        }
        rows.sort((a, b) -> Long.compare((Long) b.get("days"), (Long) a.get("days")));

        List<Map<String, Object>> rowsCopy = rows.stream().map(r -> {
            Map<String, Object> copy = new LinkedHashMap<>(r);
            copy.remove("days");
            return copy;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rowsCopy);
        result.put("totalDue", String.format("%.2f", totalDue));
        result.put("overdue30", String.format("%.2f", overdue30));
        result.put("overdue60", String.format("%.2f", overdue60));
        result.put("overdue90", String.format("%.2f", overdue90));
        return result;
    }

    private double safe(Double v) { return v != null ? v : 0.0; }
    private double round(double v) { return Math.round(v * 100.0) / 100.0; }

    private static class ProductStats {
        String code;
        String name;
        double qty = 0;
        double revenue = 0;
        int count = 0;
        ProductStats(String name) { this.name = name; this.code = name; }
    }
}
