package com.billywater.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Ligação à base de dados.
 *  - TESTE: H2 cifrada (AES) em {@code data/billywater.mv.db}.
 *  - REAL:  PostgreSQL via {@code real.bd.url}.
 * As migrações são aplicadas no arranque.
 */
public final class ConfigBanco {

    private static Connection connection;
    private static final String H2_URL = "jdbc:h2:file:./data/billywater;AES";

    private ConfigBanco() {}

    public static synchronized Connection ensureConnection() {
        if (connection != null && !isClosed()) return connection;
        try {
            if (Config.isRealProfile()) {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(Config.dbUrl(), Config.dbUser(), Config.dbPassword());
            } else {
                Class.forName("org.h2.Driver");
                String chave = lerChaveLocal();
                connection = DriverManager.getConnection(H2_URL + ";CIPHER=AES", "sa", chave);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ligar à base de dados: " + e.getMessage(), e);
        }
        return connection;
    }

    private static String lerChaveLocal() throws Exception {
        java.nio.file.Path p = java.nio.file.Paths.get("config", "chave.local");
        if (java.nio.file.Files.exists(p)) return new String(java.nio.file.Files.readAllBytes(p)).trim();
        return "billywater-local-default-key";
    }

    private static boolean isClosed() {
        try { return connection == null || connection.isClosed(); } catch (SQLException e) { return true; }
    }

    public static void applyMigrations() { MigracaoBanco.migrar(); }

    public static Connection get() { return ensureConnection(); }

    public static void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }
}
