package com.billywater.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/** Configuração da empresa/perfis. Lê de {@code config/config.properties} ou dos recursos embebidos. */
public final class Config {

    private static final Properties props = new Properties();
    private static final Path CONFIG_DIR = Paths.get("config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    private Config() {}

    public static void load() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                try (InputStream in = Files.newInputStream(CONFIG_FILE)) { props.load(in); }
            } else {
                try (InputStream in = Config.class.getResourceAsStream("/config.properties")) {
                    if (in != null) props.load(in);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível carregar config.properties", e);
        }
    }

    public static String get(String key) { return props.getProperty(key, ""); }
    public static String get(String key, String def) { return props.getProperty(key, def); }

    public static boolean isRealProfile() {
        return "REAL".equalsIgnoreCase(get("billywater.perfil", "TESTE").trim());
    }

    public static String dbUrl() {
        return isRealProfile() ? get("real.bd.url", "jdbc:postgresql://localhost:5432/billywater") : "";
    }
    public static String dbUser() { return isRealProfile() ? get("real.bd.user", "postgres") : "sa"; }
    public static String dbPassword() { return isRealProfile() ? get("real.bd.password", "") : ""; }

    public static String pastaBackups() { return get("pasta.backups", "backups"); }
}
