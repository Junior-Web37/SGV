package com.sgv.service;

import com.sgv.repository.SaleRepository;
import com.sgv.entity.Sale;
import com.sgv.entity.AppConfig;
import com.sgv.entity.Branch;
import com.sgv.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Dashboard de conformidade AT — visão resumida para o dono da loja.
 */
@Service
public class ComplianceDashboardService {

    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final AppConfigService appConfigService;

    public ComplianceDashboardService(SaleRepository saleRepository,
                                       BranchRepository branchRepository,
                                       AppConfigService appConfigService) {
        this.saleRepository = saleRepository;
        this.branchRepository = branchRepository;
        this.appConfigService = appConfigService;
    }

    public Map<String, Object> snapshot(Long branchId) {
        Map<String, Object> result = new LinkedHashMap<>();
        AppConfig cfg = appConfigService.get();

        // ─── Filial ─────────────────────────────────────────────────────────────
        Branch branch = branchId != null ? branchRepository.findById(branchId).orElse(null) : null;

        // ─── Última factura emitida ─────────────────────────────────────────────
        List<Sale> all = saleRepository.findAll();
        Optional<Sale> lastSaleOpt = all.stream()
                .filter(s -> branchId == null || (s.getBranch() != null && s.getBranch().getId().equals(branchId)))
                .filter(s -> !"ANULADA".equals(s.getState()))
                .filter(s -> !Boolean.TRUE.equals(s.getDemoFlag()))
                .max(Comparator.comparing(Sale::getCreatedAt));

        Map<String, Object> lastSale = new LinkedHashMap<>();
        if (lastSaleOpt.isPresent()) {
            Sale s = lastSaleOpt.get();
            lastSale.put("id", s.getId());
            lastSale.put("documentType", s.getDocumentType());
            lastSale.put("series", s.getSeries());
            lastSale.put("documentNumber", s.getDocumentNumber());
            lastSale.put("createdAt", s.getCreatedAt());
            lastSale.put("total", s.getTotal());
            lastSale.put("minutesAgo", Duration.between(s.getCreatedAt(), LocalDateTime.now()).toMinutes());
        } else {
            lastSale.put("minutesAgo", -1);
            lastSale.put("hint", "Nenhuma factura emitida ainda.");
        }
        result.put("lastSale", lastSale);

        // ─── Documentos por enviar (modo offline) ──────────────────────────────
        long pendingSync = all.stream()
                .filter(s -> Boolean.TRUE.equals(s.getPendingSync()))
                .count();
        result.put("pendingSyncCount", pendingSync);

        // ─── Facturas hoje ──────────────────────────────────────────────────────
        LocalDate today = LocalDate.now();
        List<Sale> todaySales = all.stream()
                .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().toLocalDate().equals(today))
                .filter(s -> !"ANULADA".equals(s.getState()))
                .filter(s -> !Boolean.TRUE.equals(s.getDemoFlag()))
                .toList();

        double todayTotal = todaySales.stream()
                .mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0)
                .sum();
        double todayTax = todaySales.stream()
                .mapToDouble(s -> s.getTotalTax() != null ? s.getTotalTax() : 0)
                .sum();

        Map<String, Object> todayBlock = new LinkedHashMap<>();
        todayBlock.put("count", todaySales.size());
        todayBlock.put("total", round(todayTotal));
        todayBlock.put("totalTax", round(todayTax));
        result.put("today", todayBlock);

        // ─── Anomalias / Alertas ────────────────────────────────────────────────
        List<Map<String, Object>> alerts = new ArrayList<>();

        if (!Boolean.TRUE.equals(cfg.getSetupCompleted())) {
            alerts.add(Map.of(
                    "level", "critical",
                    "code", "SETUP_PENDING",
                    "message", "Configuração inicial não concluída — abra /api/setup/wizard"
            ));
        }
        if (cfg.getSoftwareCertNumber() == null || cfg.getSoftwareCertNumber().isBlank()) {
            alerts.add(Map.of(
                    "level", "critical",
                    "code", "NO_AT_CERT",
                    "message", "Número de certificado AT em falta"
            ));
        }
        if (pendingSync > 50) {
            alerts.add(Map.of(
                    "level", "warning",
                    "code", "PENDING_SYNC_HIGH",
                    "message", pendingSync + " documentos por sincronizar com a AT"
            ));
        }
        if (lastSaleOpt.isPresent() && Duration.between(lastSaleOpt.get().getCreatedAt(), LocalDateTime.now()).toHours() > 24) {
            alerts.add(Map.of(
                    "level", "info",
                    "code", "NO_RECENT_SALE",
                    "message", "Sem facturas nas últimas 24h"
            ));
        }
        if (Boolean.TRUE.equals(cfg.getDemoMode())) {
            alerts.add(Map.of(
                    "level", "warning",
                    "code", "DEMO_MODE",
                    "message", "SGV em MODO DEMONSTRAÇÃO — documentos não contam para relatórios oficiais"
            ));
        }

        result.put("alerts", alerts);
        result.put("alertCount", alerts.size());

        // ─── Resumo da filial ───────────────────────────────────────────────────
        if (branch != null) {
            Map<String, Object> branchInfo = new LinkedHashMap<>();
            branchInfo.put("id", branch.getId());
            branchInfo.put("name", branch.getName());
            branchInfo.put("nuit", branch.getNuit());
            branchInfo.put("softwareCertNumber", branch.getSoftwareCertNumber());
            branchInfo.put("licenseNumber", branch.getLicenseNumber());
            result.put("branch", branchInfo);
        }

        // ─── Modo demo ──────────────────────────────────────────────────────────
        result.put("demoMode", Boolean.TRUE.equals(cfg.getDemoMode()));

        return result;
    }

    private double round(Double v) {
        return v == null ? 0.0 : Math.round(v * 100.0) / 100.0;
    }
}