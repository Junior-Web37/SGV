-- V4: Adiciona campos de categoria e stacktrace à tabela de audit_logs
-- para suportar o sistema global de logs por categoria (ERROR, SECURITY, USER_ACTION, SYSTEM)

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS category VARCHAR(50) NULL COMMENT 'Categoria do log: ERROR, SECURITY, USER_ACTION, SYSTEM',
    ADD COLUMN IF NOT EXISTS stack_trace TEXT NULL COMMENT 'Stacktrace completo para erros de sistema';

-- Indexar por categoria para pesquisas rápidas
CREATE INDEX IF NOT EXISTS idx_audit_logs_category ON audit_logs (category);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at);
