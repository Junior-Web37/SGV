-- V29: Adicionar checksum SHA-256 a system_backups (correção do BUG-001).
-- O restore passa a validar a integridade do ficheiro (SHA-256) antes de
-- aplicar o dump; backups gravados sem ficheiro/erro passam a registar FAILED.

ALTER TABLE system_backups
    ADD COLUMN IF NOT EXISTS checksum_sha256 VARCHAR(64) NULL COMMENT 'SHA-256 do ficheiro de backup';

-- End V29
