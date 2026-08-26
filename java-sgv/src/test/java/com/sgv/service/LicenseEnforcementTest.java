package com.sgv.service;

import com.sgv.entity.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.12 — Licenciamento: chave válida aceite, chave corrompida/expirada
 * rejeitada, trial sem chave com prazo real, binding a máquina e bloqueio
 * em expirado.
 *
 * Estado actual (antes das correções da auditoria):
 *  - generatedKey_isAccepted: PASSA.
 *  - corruptedKey_rejected: PASSA (a verificação de assinatura existe).
 *  - expiredKey_rejected: PASSA (a expiração é validada na leitura).
 *  - noActiveKey_trialHasRealDeadline: FALHA (BUG-027 — sem chave activa o
 *    sistema responde "Trial válido" com daysRemaining = Integer.MAX_VALUE:
 *    avaliação ILIMITADA).
 *  - keyBoundToMachine: @Disabled (BUG-027 — a assinatura não inclui o
 *    machineId; requer suporte de API com machineId por máquina).
 *  - expiredBlocksWrites: @Disabled (BUG-027 — a expiração só mostra aviso;
 *    não existe API de bloqueio de escrita).
 */
@SpringBootTest
@ActiveProfiles("test")
class LicenseEnforcementTest {

    /** Salt do cliente (fonte: LicenseService). A correção do BUG-027 remove
     *  o salt do source e os geradores públicos — este teste fixa o
     *  comportamento actual da validação. */
    private static final String SALT = "SGV-SECRET-2024-MOZ";

    @Autowired private LicenseService licenseService;
    @Autowired private AppConfigService appConfigService;

    private String originalKey;

    @BeforeEach
    void saveOriginalLicense() {
        originalKey = appConfigService.get().getLicenseKey();
    }

    @AfterEach
    void restoreOriginalLicense() {
        AppConfig cfg = appConfigService.get();
        cfg.setLicenseKey(originalKey);
        appConfigService.save(cfg);
    }

    private static String sha256First16Hex(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            try (Formatter fmt = new Formatter()) {
                for (int i = 0; i < 16; i++) fmt.format("%02x", digest[i]);
                return fmt.out().toString();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String keyFor(String type, String expiryIso, String signature) {
        String raw = type + "|" + expiryIso + "|" + signature;
        return "SGV-" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generatedKey_isAccepted() {
        assertTrue(licenseService.validateLicense(licenseService.generateAnnualLicense()).isValid(),
                "chave anual gerada deve ser válida");
        assertTrue(licenseService.validateLicense(licenseService.generateLifetimeLicense()).isValid(),
                "chave vitalícia gerada deve ser válida");
    }

    @Test
    void corruptedKey_rejected() {
        String expiry = LocalDateTime.now().plusDays(10)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String bad = keyFor("ANNUAL", expiry, "0000000000000000"); // assinatura errada

        LicenseService.LicenseResult r = licenseService.validateLicense(bad);
        assertFalse(r.isValid(), "chave com assinatura inválida deve ser rejeitada");
        assertEquals("INVALID", r.getType());
    }

    @Test
    void expiredKey_rejected() {
        String expiry = LocalDateTime.now().minusDays(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String sig = sha256First16Hex("TRIAL|" + expiry + "|" + SALT); // assinatura correcta
        String expiredKey = keyFor("TRIAL", expiry, sig);

        LicenseService.LicenseResult r = licenseService.validateLicense(expiredKey);
        assertFalse(r.isValid(), "chave expirada deve ser rejeitada mesmo com assinatura válida");
        assertTrue(r.getMessage() != null && r.getMessage().toLowerCase().contains("expir"),
                "a mensagem deve indicar expiração: " + r.getMessage());
    }

    @Test
    void noActiveKey_trialHasRealDeadline() {
        // BUG-027 — FALHA AGORA: sem chave, getCurrentLicenseStatus devolve
        // Trial válido com daysRemaining = Integer.MAX_VALUE (avaliação
        // ilimitada). Depois da correção: trial com prazo real (30 dias a
        // partir da 1ª utilização).
        AppConfig cfg = appConfigService.get();
        cfg.setLicenseKey(null);
        appConfigService.save(cfg);

        LicenseService.LicenseResult status = licenseService.getCurrentLicenseStatus();
        assertTrue(status.isValid(), "sem chave o sistema deve estar em modo de avaliação (activo)");
        assertTrue(status.getDaysRemaining() > 0 && status.getDaysRemaining() <= 30,
                "BUG-027: trial sem chave tem de ter prazo real (<= 30 dias) — "
                        + "obtido daysRemaining=" + status.getDaysRemaining());
    }

    @Disabled("BUG-027: a assinatura da chave ainda não inclui o machineId — "
            + "uma chave de máquina A é aceite em qualquer máquina. "
            + "Activar após a correção (geração server-side + machineId no payload).")
    @Test
    void keyGeneratedForMachineA_rejectedOnMachineB() {
        // Critério: chave emitida para o machineId de A não valida em B.
        // Requer API com machineId por instalação (hoje a validação nem
        // recebe o machineId — getMachineId() é apenas exibido na UI).
        fail("Implementar após a correção do BUG-027: validar(chave, machineIdB) == inválida");
    }

    @Disabled("BUG-027: a expiração só mostra SgvDialog.warning no arranque — "
            + "não bloqueia escrita nem leitura. Activar após a correção "
            + "(modo só-leitura em expirado).")
    @Test
    void expiredLicense_blocksWrites() {
        // Critério: com licença expirada, operações de escrita
        // (venda/compra/caixa) são bloqueadas e o utilizador informado.
        fail("Implementar após a correção do BUG-027: escrita bloqueada em expirado");
    }
}
