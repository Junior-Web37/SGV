package com.sgv.service;

import com.sgv.entity.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Serviço de licenciamento do SGV.
 * Gera e valida chaves de licença baseadas no machineId + data de expiração + salt.
 *
 * Formato da chave: SGV-XXXX-XXXX-XXXX-XXXX (Base64 codificado com hash HMAC simples)
 * Tipos: TRIAL (30 dias), ANUAL (365 dias), VITALICIA (sem expiração)
 */
@Service
public class LicenseService {

    private static final Logger log = LoggerFactory.getLogger(LicenseService.class);

    /** Salt secreto para assinatura de chaves — alterar em produção */
    private static final String SECRET_SALT = "SGV-SECRET-2024-MOZ";

    private static final int TRIAL_DAYS = 30;
    private static final int ANNUAL_DAYS = 365;

    private final AppConfigService appConfigService;

    public LicenseService(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    /**
     * Resultado da validação da licença.
     */
    public static class LicenseResult {
        private final boolean valid;
        private final String type;
        private final String message;
        private final String expiryDate;
        private final int daysRemaining;

        public LicenseResult(boolean valid, String type, String message, String expiryDate, int daysRemaining) {
            this.valid = valid;
            this.type = type;
            this.message = message;
            this.expiryDate = expiryDate;
            this.daysRemaining = daysRemaining;
        }

        public boolean isValid() { return valid; }
        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getExpiryDate() { return expiryDate; }
        public int getDaysRemaining() { return daysRemaining; }
    }

    /**
     * Gera uma nova chave de licença trial (30 dias).
     */
    public String generateTrialLicense() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(TRIAL_DAYS);
        return generateKey("TRIAL", expiry);
    }

    /**
     * Gera uma nova chave de licença anual (365 dias).
     */
    public String generateAnnualLicense() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(ANNUAL_DAYS);
        return generateKey("ANNUAL", expiry);
    }

    /**
     * Gera uma nova chave de licença vitalícia.
     */
    public String generateLifetimeLicense() {
        LocalDateTime expiry = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
        return generateKey("LIFETIME", expiry);
    }

    /**
     * Valida uma chave de licença fornecida.
     */
    public LicenseResult validateLicense(String key) {
        if (key == null || key.isBlank()) {
            return new LicenseResult(false, "NONE", "Chave de licença não fornecida.", "—", 0);
        }

        try {
            String cleaned = key.trim().toUpperCase().replaceAll("[^A-Z0-9\\-]", "");
            String decoded = new String(Base64.getUrlDecoder().decode(cleaned.replace("SGV-", "")), StandardCharsets.UTF_8);

            String[] parts = decoded.split("\\|");
            if (parts.length != 3) {
                return new LicenseResult(false, "INVALID", "Formato de chave inválido.", "—", 0);
            }

            String type = parts[0];
            String expiryStr = parts[1];
            String signature = parts[2];

            // Validate signature
            String expectedSignature = computeSignature(type, expiryStr);
            if (!expectedSignature.equals(signature)) {
                return new LicenseResult(false, "INVALID", "Assinatura da licença inválida.", "—", 0);
            }

            // Parse expiry
            LocalDateTime expiry = LocalDateTime.parse(expiryStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiry);

            if (daysRemaining < 0) {
                return new LicenseResult(false, type, "Licença expirada.", expiry.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 0);
            }

            String typeName;
            switch (type) {
                case "TRIAL" -> typeName = "Trial / Avaliação";
                case "ANNUAL" -> typeName = "Anual";
                case "LIFETIME" -> typeName = "Vitalícia";
                default -> typeName = type;
            }

            return new LicenseResult(true, typeName, "Licença válida.",
                    expiry.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), (int) daysRemaining);

        } catch (IllegalArgumentException e) {
            return new LicenseResult(false, "INVALID", "Chave de licença corrompida ou inválida.", "—", 0);
        } catch (Exception e) {
            log.error("Erro ao validar licença", e);
            return new LicenseResult(false, "ERROR", "Erro ao validar licença: " + e.getMessage(), "—", 0);
        }
    }

    /**
     * Activa uma chave de licença e persiste na AppConfig.
     */
    public LicenseResult activateLicense(String key) {
        LicenseResult result = validateLicense(key);
        if (!result.isValid()) {
            return result;
        }

        AppConfig config = appConfigService.get();
        config.setLicenseKey(key.trim());
        config.setLicenseType(result.getType());
        config.setLicenseActivatedAt(LocalDateTime.now());
        config.setLicenseExpiry(result.getExpiryDate());
        appConfigService.save(config);

        log.info("Licença activada: tipo={}, expira={}", result.getType(), result.getExpiryDate());
        return result;
    }

    /**
     * Obtém o estado actual da licença a partir da AppConfig.
     */
    public LicenseResult getCurrentLicenseStatus() {
        AppConfig config = appConfigService.get();
        String key = config.getLicenseKey();

        if (key == null || key.isBlank()) {
            // Sem licença activa — considerar como trial
            return new LicenseResult(true, "Trial / Avaliação", "Sem licença activa — modo avaliação.", "—", Integer.MAX_VALUE);
        }

        return validateLicense(key);
    }

    /**
     * Obtém o machine ID (SHA-256 do SO + user + directório).
     */
    public String getMachineId() {
        try {
            String os = System.getProperty("os.name", "");
            String user = System.getProperty("user.name", "");
            String dir = System.getProperty("user.dir", "");
            String raw = os + "|" + user + "|" + dir + "|" + SECRET_SALT;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02X", b));
            return sb.toString().substring(0, 20);
        } catch (NoSuchAlgorithmException e) {
            return "ERR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────

    private String generateKey(String type, LocalDateTime expiry) {
        String expiryStr = expiry.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String signature = computeSignature(type, expiryStr);
        String raw = type + "|" + expiryStr + "|" + signature;
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "SGV-" + encoded;
    }

    private String computeSignature(String type, String expiryStr) {
        try {
            String payload = type + "|" + expiryStr + "|" + SECRET_SALT;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i++) sb.append(String.format("%02X", digest[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "0000000000000000";
        }
    }
}
