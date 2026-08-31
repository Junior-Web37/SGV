package com.billywater.servico;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

/** Backup real (dump + checksum + retenção). */
public class ServicoBackup {

    private final String pasta;
    private final int retencao;

    public ServicoBackup(String pasta, int retencao) {
        this.pasta = pasta == null || pasta.isBlank() ? "backups" : pasta;
        this.retencao = retencao;
    }

    public Path criarBackup() throws IOException {
        Path dir = Paths.get(pasta);
        Files.createDirectories(dir);
        String nome = "billywater_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".mv.db";
        Path destino = dir.resolve(nome);
        Path origem = Paths.get("data", "billywater.mv.db");
        if (Files.exists(origem)) {
            Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(dir.resolve(nome + ".md5"), md5(origem));
        } else {
            Files.writeString(destino, "Sem base de dados local (perfil REAL/PostgreSQL). Backup do ficheiro de configuração:\n");
            Path cfg = Paths.get("config", "config.properties");
            if (Files.exists(cfg)) Files.copy(cfg, dir.resolve(nome + ".cfg"), StandardCopyOption.REPLACE_EXISTING);
        }
        podar(retencao, dir);
        return destino;
    }

    private static String md5(Path p) throws IOException {
        try (var in = Files.newInputStream(p)) {
            var md = java.security.MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private void podar(int retencao, Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().startsWith("billywater_"))
                    .sorted(Comparator.reverseOrder())
                    .skip(Math.max(0, retencao))
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
