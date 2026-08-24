-- V11: Add missing columns to app_config for backup settings and exchange rate

ALTER TABLE app_config ADD COLUMN IF NOT EXISTS exchange_rate DOUBLE DEFAULT 74.0 AFTER default_currency;
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS backup_frequency VARCHAR(32) DEFAULT 'Diario' AFTER demo_mode;
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS backup_hour VARCHAR(16) DEFAULT '02:00' AFTER backup_frequency;
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS backup_retention_days INT DEFAULT 30 AFTER backup_hour;
