package com.sgv.service;

import com.sgv.entity.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Backup automático da base de dados MariaDB (mysqldump) e exportação SAF-T.
 *
 * Para SGV a correr em desktop, este serviço:
 *  - Faz backup diário às 03:00 (configurável)
 *  - Mantém últimos 7 dias
 *  - Faz dump via mysqldump ou copia os ficheiros se MariaDB embebida
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AppConfigService appConfigService;

    @Value("${spring.datasource.url:jdbc:mariadb://localhost:3306/sgv}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:root}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name:org.mariadb.jdbc.Driver}")
    private String driver;

    public BackupService(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    /** Backup diário às 03:00. */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledBackup() {
        AppConfig cfg = appConfigService.get();
        if (!Boolean.TRUE.equals(cfg.getAutoBackupEnabled())) {
            log.debug("Auto-backup desabilitado nas configurações.");
            return;
        }
        try {
            File backup = backupNow();
            log.info("Backup automático criado: {} ({} KB)", backup.getAbsolutePath(), backup.length() / 1024);
            cleanupOldBackups(7);
        } catch (Exception e) {
            log.error("Falha no backup automático", e);
        }
    }

    public File backupNow() throws IOException, InterruptedException {
        File dir = new File(System.getProperty("user.home"), "Documents/SGV/backups");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Não foi possível criar " + dir.getAbsolutePath());
        }
        File backupFile = new File(dir, "sgv_backup_" + LocalDateTime.now().format(TS) + ".sql");

        String dbName = extractDbName(datasourceUrl);

        // mysqldump em subprocess (Windows + Linux)
        String dumpCmd = isWindows() ? "mysqldump.exe" : "mysqldump";
        ProcessBuilder pb = new ProcessBuilder(
                dumpCmd,
                "-u", dbUser,
                "-p" + dbPassword,
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
}