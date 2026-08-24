-- V12: Add license columns to app_config
ALTER TABLE app_config ADD COLUMN license_key VARCHAR(500) NULL;
ALTER TABLE app_config ADD COLUMN license_type VARCHAR(50) NULL;
ALTER TABLE app_config ADD COLUMN license_expiry VARCHAR(30) NULL;
ALTER TABLE app_config ADD COLUMN license_activated_at DATETIME NULL;
