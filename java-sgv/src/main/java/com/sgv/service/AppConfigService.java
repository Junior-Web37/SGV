package com.sgv.service;

import com.sgv.entity.AppConfig;
import com.sgv.repository.AppConfigRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Serviço de configuração global. Garante que existe sempre 1 linha (id=1).
 */
@Service
public class AppConfigService {

    private final AppConfigRepository appConfigRepository;

    public AppConfigService(AppConfigRepository appConfigRepository) {
        this.appConfigRepository = appConfigRepository;
    }

    public AppConfig get() {
        AppConfig cfg = appConfigRepository.findById(1L).orElseGet(() -> {
            AppConfig c = new AppConfig();
            c.setId(1L);
            return appConfigRepository.save(c);
        });
        // Garante que os defaults obrigatórios estão populados (defesa contra INSERT IGNORE da V2)
        boolean needsSave = false;
        if (cfg.getDefaultSeries() == null) { cfg.setDefaultSeries("A"); needsSave = true; }
        if (cfg.getDefaultCurrency() == null) { cfg.setDefaultCurrency("MZN"); needsSave = true; }
        if (cfg.getConsumerFinalNuit() == null) { cfg.setConsumerFinalNuit("999999999"); needsSave = true; }
        if (cfg.getInitialDocumentNumber() == null) { cfg.setInitialDocumentNumber(1L); needsSave = true; }
        if (cfg.getDuplicateWindowSeconds() == null) { cfg.setDuplicateWindowSeconds(60); needsSave = true; }
        if (cfg.getThermalPrinterWidth() == null) { cfg.setThermalPrinterWidth(80); needsSave = true; }
        if (cfg.getAutoBackupEnabled() == null) { cfg.setAutoBackupEnabled(true); needsSave = true; }
        if (cfg.getOfflineModeEnabled() == null) { cfg.setOfflineModeEnabled(true); needsSave = true; }
        if (cfg.getDuplicateDetectionEnabled() == null) { cfg.setDuplicateDetectionEnabled(true); needsSave = true; }
        if (cfg.getDemoMode() == null) { cfg.setDemoMode(false); needsSave = true; }
        if (cfg.getSetupCompleted() == null) { cfg.setSetupCompleted(false); needsSave = true; }
        return needsSave ? appConfigRepository.save(cfg) : cfg;
    }

    public AppConfig save(AppConfig cfg) {
        cfg.setId(1L);
        if (Boolean.TRUE.equals(cfg.getSetupCompleted()) && cfg.getSetupCompletedAt() == null) {
            cfg.setSetupCompletedAt(LocalDateTime.now());
        }
        cfg.setUpdatedAt(LocalDateTime.now());
        return appConfigRepository.save(cfg);
    }

    public boolean isSetupCompleted() {
        return Boolean.TRUE.equals(get().getSetupCompleted());
    }

    public boolean isDemoMode() {
        return Boolean.TRUE.equals(get().getDemoMode());
    }
}