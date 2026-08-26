package com.sgv.service;

import com.sgv.entity.SystemBackup;
import com.sgv.repository.SystemBackupRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.7 — Backup/Restore: runBackup() cria ficheiro com size&gt;0 e a BD
 * regista o backup (status COMPLETED com caminho real + checksum SHA-256);
 * restauro devolve o dataset.
 *
 * Estado actual (antes das correções da auditoria):
 *  - FALHA (BUG-001): a UI regista "sucesso" sem produzir cópia (copiava um
 *    "sgv.db" inexistente e gravava COMPLETED incondicionalmente); o único
 *    backup real (mysqldump) não é chamado pela UI.
 *
 * Depois da correção do BUG-001: a UI chama {@code backupService.runBackup}
 * (mesmo fluxo testado aqui) e o restore aplica o dump via cliente mysql com
 * validação de checksum.
 *
 * Execução: requer MariaDB real + binário mysqldump — impossível no H2
 * in-memory dos testes locais. Rodar em CI com container (serviço mysql) e
 * remover o @Disabled.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requer MariaDB real + mysqldump (H2 in-memory não é alvo de backup). Rodar em CI com container.")
class BackupRestoreTest {

    @Autowired private BackupService backupService;
    @Autowired private SystemBackupRepository systemBackupRepository;

    @Test
    void runBackup_createsRealFileAndDbRecord() throws Exception {
        long before = systemBackupRepository.count();

        BackupService.BackupOutcome out = backupService.runBackup("MANUAL", "test");

        assertTrue(out.success(), "runBackup deve ter sucesso: " + out.error());
        File file = out.file();
        assertNotNull(file, "runBackup deve devolver o ficheiro criado");
        assertTrue(file.exists(), "ficheiro de backup deve existir em disco: " + file);
        assertTrue(file.length() > 0,
                "BUG-001: ficheiro de backup com 0 bytes — registo de sucesso falso");

        long after = systemBackupRepository.count();
        assertEquals(before + 1, after, "cada backup deve deixar registo em system_backups");
        SystemBackup last = systemBackupRepository.findAll().stream()
                .sorted((x, y) -> Long.compare(
                        x.getCreatedAt() != null ? x.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) : 0,
                        y.getCreatedAt() != null ? y.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) : 0))
                .reduce((a, b) -> b).orElseThrow();
        assertEquals("COMPLETED", last.getStatus());
        assertEquals(file.length(), (long) last.getFileSize(),
                "fileSize em BD deve corresponder ao tamanho real do ficheiro");
        assertNotNull(last.getFilePath());
        assertTrue(new File(last.getFilePath()).exists(),
                "o caminho gravado em BD tem de apontar para um ficheiro real");
        assertNotNull(last.getChecksumSha256(),
                "o registo deve conter o checksum SHA-256 para validação no restore");
        assertEquals(64, last.getChecksumSha256().length(), "SHA-256 em hexadecimal tem 64 caracteres");
    }

    /**
     * Restauro (round-trip): criar dataset conhecido → backup → apagar →
     * restaurar → verificar vendas/clientes/stock/caixa/fornecedores/config.
     * Implementar com o comando de restauro real (mysql &lt; backup.sql) após a
     * correção do BUG-001; a verificação final deve comparar:
     *   - sales + sale_items completos
     *   - customers.balance reconstruída
     *   - stock_branch == Σ stock_movements (kardex intacto)
     *   - cash_sessions/cash_movements íntegros
     *   - users/roles/permissions e app_config devolvidos
     */
    @Test
    void restore_roundTrip_restoresDataset() {
        fail("Implementar após a correção do BUG-001 (pipeline de restauro real). "
                + "Critério: dataset criado antes do backup deve ser idêntico após o restauro.");
    }
}
