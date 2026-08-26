package com.sgv.service;

import com.sgv.entity.AppConfig;
import com.sgv.entity.SystemBackup;
import com.sgv.repository.SystemBackupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Backup automático da base de dados MariaDB (mysqldump) e exportação SAF-T.
 *
 * Para SGV a correr em desktop, este serviço:
 *  - Faz backup na hora/frequência configuradas em Sistema → Backups
 *    (autoBackupEnabled, backupHour, backupFrequency, backupRetentionDays)
 *  - Regista cada backup em system_backups com o estado VERDADEIRO
 *    (COMPLETED só se o ficheiro existir com conteúdo; senão FAILED) e o
 *    checksum SHA-256 para validação de integridade
 *  - Restore real: aplica o dump SQL ao servidor via cliente {@code mysql}
 *
 * Correções do BUG-001/BUG-047: antes, os painéis de backup copiavam um
 * "sgv.db" inexistente e gravavam COMPLETED incondicionalmente; o restore
 * copiava o ficheiro para "C:\xampp\mysql\data\sgv" (caminho fixo de
 * Windows) sem confirmar nada; e o agendamento ignorava as configurações
 * (cron fixo 03:00, retenção fixa de 7 dias).
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AppConfigService appConfigService;
    private final SystemBackupRepository systemBackupRepository;

    @Value("${spring.datasource.url:jdbc:mariadb://localhost:3306/sgv}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:root}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name:org.mariadb.jdbc.Driver}")
    private String driver;

    public BackupService(AppConfigService appConfigService,
                         SystemBackupRepository systemBackupRepository) {
        this.appConfigService = appConfigService;
        this.systemBackupRepository = systemBackupRepository;
    }

    /**
     * Resultado de um backup (correção do BUG-001): o estado reflecte o que
     * efectivamente aconteceu — {@code success} só é true se o ficheiro
     * existir com conteúdo; o registo em system_backups usa o mesmo estado.
     */
    public record BackupOutcome(File file, long sizeBytes, String sha256, boolean success, String error) {}

    /** Resultado de um restore. */
    public record RestoreOutcome(boolean success, String detail) {}

    private final AtomicReference<LocalDate> lastAutoBackupDay = new AtomicReference<>(null);

    /**
     * Correção do BUG-047/BUG-001: o agendamento respeita as configurações do
     * utilizador (autoBackupEnabled, backupFrequency, backupHour e
     * backupRetentionDays) em vez do cron fixo "0 0 3 * * *" com retenção de
     * 7 dias. O disparo é horário (minuto 0 de cada hora) e só executa quando
     * (1) o auto-backup está activado, (2) a frequência permite o dia actual,
     * (3) a hora configurada já passou e (4) ainda não correu hoje.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledBackup() {
        AppConfig cfg;
        try {
            cfg = appConfigService.get();
        } catch (Exception e) {
            return;
        }
        if (cfg == null || !Boolean.TRUE.equals(cfg.getAutoBackupEnabled())) {
            log.debug("Auto-backup desabilitado nas configurações.");
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate last = lastAutoBackupDay.get();
        if (last != null && !last.isBefore(today)) return; // já correu hoje
        if (!frequencyAllows(cfg.getBackupFrequency(), today)) return; // Semanal/Mensal
        if (LocalTime.now().isBefore(parseBackupHour(cfg.getBackupHour()))) return; // ainda não chegou à hora
        try {
            BackupOutcome out = runBackup("AUTOMATIC", "sistema");
            if (out.success()) {
                lastAutoBackupDay.set(today);
                int retention = cfg.getBackupRetentionDays() != null && cfg.getBackupRetentionDays() > 0
                        ? cfg.getBackupRetentionDays() : 30;
                cleanupOldBackups(retention);
                log.info("Backup automático criado: {} ({} KB)", out.file().getAbsolutePath(), out.sizeBytes() / 1024);
            } else {
                log.error("Falha no backup automático: {}", out.error());
            }
        } catch (Exception e) {
            log.error("Falha no backup automático", e);
        }
    }

    /**
     * Correção do BUG-001: executa o backup REAL (mysqldump) e regista o
     * resultado em system_backups com o estado verdadeiro — COMPLETED apenas
     * se o ficheiro existir com conteúdo; caso contrário FAILED com erro.
     *
     * @param backupType "MANUAL" (painel do utilizador) ou "AUTOMATIC" (agendado)
     * @param createdBy  utilizador ou "sistema"
     */
    @Transactional
    public BackupOutcome runBackup(String backupType, String createdBy) {
        try {
            File f = backupNow();
            long size = f.length();
            if (!f.exists() || size <= 0) {
                recordBackup(backupType, createdBy, f, 0L, null, "FAILED",
                        "O ficheiro de backup ficou vazio: " + f.getAbsolutePath());
                return new BackupOutcome(f, 0, null, false, "O ficheiro de backup ficou vazio: " + f.getAbsolutePath());
            }
            String sha = sha256(f);
            recordBackup(backupType, createdBy, f, size, sha, "COMPLETED", null);
            return new BackupOutcome(f, size, sha, true, null);
        } catch (Exception e) {
            recordBackup(backupType, createdBy, null, 0L, null, "FAILED", e.getMessage());
            return new BackupOutcome(null, 0, null, false, e.getMessage());
        }
    }

    private void recordBackup(String backupType, String createdBy, File f, long size,
                              String sha, String status, String error) {
        try {
            SystemBackup bk = new SystemBackup();
            bk.setFileName(f != null ? f.getName() : "sgv_backup_" + LocalDateTime.now().format(TS) + ".sql");
            bk.setFilePath(f != null ? f.getAbsolutePath()
                    : new File(System.getProperty("user.home"), "Documents/SGV/backups").getAbsolutePath());
            bk.setFileSize(size > 0 ? size : 0L);
            bk.setBackupType(backupType != null ? backupType : "MANUAL");
            bk.setStatus(status);
            bk.setChecksumSha256(sha);
            bk.setErrorMessage(error);
            bk.setCreatedBy(createdBy != null ? createdBy : "system");
            systemBackupRepository.save(bk);
        } catch (Exception e) {
            log.error("Falha ao registar o backup em system_backups", e);
        }
    }

    /**
     * Correção do BUG-001: restore REAL — o dump SQL é aplicado ao servidor
     * através do cliente {@code mysql} (equivalente a {@code mysql < backup.sql}),
     * em vez de copiar o ficheiro para "C:\xampp\mysql\data\sgv". Antes da
     * aplicação, o ficheiro é validado: existe, não é vazio e, se tiver
     * checksum registado, o SHA-256 actual tem de coincidir (o restore é
     * bloqueado se o ficheiro tiver sido alterado).
     *
     * @param sqlFile            ficheiro .sql do dump
     * @param expectedChecksum   SHA-256 registado no momento do backup (ou null)
     */
    public RestoreOutcome restoreFrom(Path sqlFile, String expectedChecksum) {
        if (sqlFile == null) return new RestoreOutcome(false, "Nenhum ficheiro de backup indicado.");
        if (!Files.exists(sqlFile)) {
            return new RestoreOutcome(false, "Ficheiro de backup não encontrado: " + sqlFile.toAbsolutePath());
        }
        long size;
        try {
            size = Files.size(sqlFile);
        } catch (IOException e) {
            return new RestoreOutcome(false, "Não foi possível ler o ficheiro: " + e.getMessage());
        }
        if (size <= 0) {
            return new RestoreOutcome(false, "Ficheiro de backup vazio: " + sqlFile.toAbsolutePath());
        }
        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            String actual;
            try {
                actual = sha256(sqlFile.toFile());
            } catch (Exception e) {
                return new RestoreOutcome(false, "Não foi possível calcular o checksum do ficheiro: " + e.getMessage());
            }
            if (!expectedChecksum.equalsIgnoreCase(actual)) {
                return new RestoreOutcome(false,
                        "Falha de integridade: o checksum SHA-256 do ficheiro (" + actual
                                + ") não corresponde ao registado (" + expectedChecksum
                                + "). O ficheiro pode ter sido alterado — restore bloqueado.");
            }
        }
        try {
            String mysqlCmd = isWindows() ? "mysql.exe" : "mysql";
            String dbName = extractDbName(datasourceUrl);
            File optionsFile = File.createTempFile("mysql_restore_", ".cnf");
            try {
                try (java.io.PrintWriter pw = new java.io.PrintWriter(optionsFile)) {
                    pw.println("[mysql]");
                    pw.println("user=" + dbUser);
                    if (dbPassword != null && !dbPassword.isEmpty()) {
                        pw.println("password=" + dbPassword);
                    }
                }
                ProcessBuilder pb = new ProcessBuilder(
                        mysqlCmd,
                        "--defaults-extra-file=" + optionsFile.getAbsolutePath(),
                        dbName);
                pb.redirectErrorStream(true);
                Process process;
                try (InputStream in = Files.newInputStream(sqlFile)) {
                    pb.redirectInput(in);
                    process = pb.start();
                }
                String output = new String(process.getInputStream().readAllBytes());
                int rc = process.waitFor();
                if (rc != 0) {
                    return new RestoreOutcome(false,
                            "mysql terminou com código " + rc + (output.isBlank() ? "" : " — " + tail(output, 500)));
                }
                return new RestoreOutcome(true,
                        "Base de dados '" + dbName + "' restaurada do ficheiro " + sqlFile.getFileName()
                                + ". Reinicie a aplicação para reflectir o estado restaurado.");
            } finally {
                optionsFile.delete();
            }
        } catch (Exception e) {
            return new RestoreOutcome(false, "Falha ao executar o restore (mysql): " + e.getMessage());
        }
    }

    /**
     * Cria um arquivo ZIP com o SAF-T do mês corrente + último backup.
     * Útil para o dono levar para casa num USB.
     */
    public File createSnapshot() throws IOException {
        File dir = new File(System.getProperty("user.home"), "Documents/SGV/backups");
        if (!dir.exists()) dir.mkdirs();

        File zip = new File(dir, "sgv_snapshot_" + LocalDateTime.now().format(TS) + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            File backup = latestBackup();
            if (backup != null) {
                zos.putNextEntry(new ZipEntry("backup/" + backup.getName()));
                Files.copy(backup.toPath(), zos);
                zos.closeEntry();
            }
            zos.putNextEntry(new ZipEntry("info.txt"));
            String info = "Snapshot criado em: " + LocalDateTime.now() + "\n"
                    + "Versão SGV: 1.0\n"
                    + "Empresa: " + safeString(appConfigService.get().getCompanyName()) + "\n"
                    + "NUIT: " + safeString(appConfigService.get().getCompanyNuit()) + "\n";
            zos.write(info.getBytes());
            zos.closeEntry();
        }
        return zip;
    }

    public File backupNow() throws IOException, InterruptedException {
        File dir = new File(System.getProperty("user.home"), "Documents/SGV/backups");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Não foi possível criar " + dir.getAbsolutePath());
        }
        File backupFile = new File(dir, "sgv_backup_" + LocalDateTime.now().format(TS) + ".sql");

        String dbName = extractDbName(datasourceUrl);

        // Create a temporary options file to avoid exposing password in process list
        File optionsFile = File.createTempFile("mysqldump_", ".cnf");
        try {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(optionsFile)) {
                pw.println("[mysqldump]");
                pw.println("user=" + dbUser);
                if (dbPassword != null && !dbPassword.isEmpty()) {
                    pw.println("password=" + dbPassword);
                }
            }

            // mysqldump em subprocess (Windows + Linux)
            String dumpCmd = isWindows() ? "mysqldump.exe" : "mysqldump";
            ProcessBuilder pb = new ProcessBuilder(
                    dumpCmd,
                    "--defaults-extra-file=" + optionsFile.getAbsolutePath(),
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    "--add-drop-table",
                    dbName
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.to(backupFile));

            Process process = pb.start();
            int rc = process.waitFor();
            if (rc != 0) {
                throw new IOException("mysqldump terminou com código " + rc);
            }
            log.info("Backup SQL criado em {}", backupFile.getAbsolutePath());
            return backupFile;
        } finally {
            optionsFile.delete();
        }
    }

    private File latestBackup() {
        File dir = new File(System.getProperty("user.home"), "Documents/SGV/backups");
        File[] files = dir.listFiles((d, n) -> n.startsWith("sgv_backup_") && n.endsWith(".sql"));
        if (files == null || files.length == 0) return null;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return files[0];
    }

    private void cleanupOldBackups(int keepDays) {
        File dir = new File(System.getProperty("user.home"), "Documents/SGV/backups");
        File[] files = dir.listFiles((d, n) -> n.startsWith("sgv_backup_") && n.endsWith(".sql"));
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - keepDays * 24L * 60 * 60 * 1000;
        for (File f : files) {
            if (f.lastModified() < cutoff) {
                if (f.delete()) log.info("Backup antigo removido: {}", f.getName());
            }
        }
    }

    private String extractDbName(String url) {
        int idx = url.lastIndexOf('/');
        if (idx == -1) return "sgv";
        String db = url.substring(idx + 1);
        int q = db.indexOf('?');
        return q == -1 ? db : db.substring(0, q);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private String safeString(String s) { return s != null ? s : ""; }

    /**
     * Correção do BUG-047: frequência configurável — "Diario" (todos os dias),
     * "Semanal" (segundas-feiras) ou "Mensal" (dia 1 do mês).
     */
    private boolean frequencyAllows(String frequency, LocalDate today) {
        String f = frequency != null ? frequency.trim().toLowerCase() : "diario";
        return switch (f) {
            case "semanal" -> today.getDayOfWeek() == DayOfWeek.MONDAY;
            case "mensal" -> today.getDayOfMonth() == 1;
            default -> true;
        };
    }

    /** Interpreta "HH:mm" da configuração; fallback 02:00 em caso de valor inválido. */
    private LocalTime parseBackupHour(String hour) {
        try {
            if (hour == null || hour.isBlank()) return LocalTime.of(2, 0);
            String[] parts = hour.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return LocalTime.of(h, m);
        } catch (Exception e) {
            return LocalTime.of(2, 0);
        }
    }

    private static String tail(String s, int n) {
        return s.length() <= n ? s : s.substring(s.length() - n);
    }

    private String sha256(File f) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (InputStream in = Files.newInputStream(f.toPath())) {
                int r;
                while ((r = in.read(buffer)) != -1) {
                    md.update(buffer, 0, r);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 indisponível: " + e.getMessage(), e);
        }
    }
}
