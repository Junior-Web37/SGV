-- =============================================
-- SGV Migration V6 — Sistema de Backups
-- Cria tabela para histórico de backups na BD
-- =============================================

-- Tabela de registo de backups
CREATE TABLE IF NOT EXISTS system_backups (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    file_size       BIGINT DEFAULT 0,
    backup_type     VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    status          VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    error_message   TEXT,
    created_at      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    created_by      VARCHAR(255),
    notes           TEXT,
    UNIQUE KEY uk_backup_file (file_name)
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_backups_created ON system_backups(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_backups_type ON system_backups(backup_type);
CREATE INDEX IF NOT EXISTS idx_backups_status ON system_backups(status);
