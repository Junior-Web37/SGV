package com.sgv.service;

import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Gestão de séries documentais — alerta de séries a esgotar, transição entre séries.
 */
@Service
public class SeriesManagementService {

    private final SaleRepository saleRepository;

    /** Limite recomendado pela AT para uma série antes de abrir outra. */
    private static final long MAX_PER_SERIES = 999_999L;
    /** Threshold para alerta: 90% da capacidade. */
    private static final double ALERT_THRESHOLD = 0.9;

    public SeriesManagementService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public Map<String, Object> status() {
        List<Object[]> rows = saleRepository.findAll().stream()
                .filter(s -> !"ANULADA".equals(s.getState()))
                .filter(s -> !Boolean.TRUE.equals(s.getDemoFlag()))
                .map(s -> new Object[]{
                        s.getSeries() != null ? s.getSeries() : "A",
                        s.getDocumentType(),
                        s.getDocumentNumber() != null ? s.getDocumentNumber() : 0L,
                        s.getDocumentYear() != null ? s.getDocumentYear() : 0
                })
                .toList();

        Map<String, SeriesStatus> map = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String series = (String) r[0];
            String type = (String) r[1];
            long num = (Long) r[2];
            int year = (Integer) r[3];
            String key = year + "|" + series + "|" + type;
            map.computeIfAbsent(key, k -> new SeriesStatus(year, series, type));
            map.get(key).update(num);
        }

        List<Map<String, Object>> seriesList = new ArrayList<>();
        List<Map<String, Object>> alerts = new ArrayList<>();

        for (SeriesStatus s : map.values()) {
            double pct = (double) s.max / (double) MAX_PER_SERIES;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("series", s.series);
            item.put("documentType", s.type);
            item.put("year", s.year);
            item.put("current", s.max);
            item.put("limit", MAX_PER_SERIES);
            item.put("usage", String.format("%.2f%%", pct * 100));
            item.put("remaining", MAX_PER_SERIES - s.max);
            item.put("status", pct >= 1.0 ? "exhausted" : pct >= ALERT_THRESHOLD ? "warning" : "ok");
            seriesList.add(item);

            if (pct >= ALERT_THRESHOLD) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("level", pct >= 1.0 ? "critical" : "warning");
                a.put("code", "SERIES_NEAR_LIMIT");
                a.put("message", String.format("Série %s/%s/%d a %.0f%% da capacidade (restam %d). Abra nova série.",
                        s.series, s.type, s.year, pct * 100, MAX_PER_SERIES - s.max));
                a.put("recommendation", "Abrir série '" + nextSeries(s.series) + "' para novos documentos");
                alerts.add(a);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("series", seriesList);
        result.put("alerts", alerts);
        result.put("totalSeries", map.size());
        return result;
    }

    public String nextSeries(String current) {
        if (current == null || current.isEmpty()) return "A";
        char last = current.charAt(current.length() - 1);
        if (last == 'Z') return current + "A";
        return current.substring(0, current.length() - 1) + (char) (last + 1);
    }

    private static class SeriesStatus {
        int year;
        String series;
        String type;
        long max = 0L;

        SeriesStatus(int year, String series, String type) {
            this.year = year;
            this.series = series;
            this.type = type;
        }

        void update(long num) { if (num > max) max = num; }
    }
}