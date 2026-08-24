-- V16: Enforce NOT NULL and sensible defaults for columns with entity defaults
-- Warning: backup recommended before applying to production.

-- cash_sessions.state (entity default = 'OPEN')
UPDATE cash_sessions SET state = 'OPEN' WHERE state IS NULL;
ALTER TABLE cash_sessions MODIFY COLUMN state VARCHAR(20) NOT NULL DEFAULT 'OPEN';

-- products.is_active (entity default = true)
UPDATE products SET is_active = 1 WHERE is_active IS NULL;
ALTER TABLE products MODIFY COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1;

-- system_backups: backup_type, status, created_at
UPDATE system_backups SET backup_type = 'MANUAL' WHERE backup_type IS NULL;
ALTER TABLE system_backups MODIFY COLUMN backup_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL';

UPDATE system_backups SET status = 'COMPLETED' WHERE status IS NULL;
ALTER TABLE system_backups MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED';

UPDATE system_backups SET created_at = NOW() WHERE created_at IS NULL;
ALTER TABLE system_backups MODIFY COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- stock_branch numeric defaults (entity default = 0)
UPDATE stock_branch SET stock_current = 0 WHERE stock_current IS NULL;
ALTER TABLE stock_branch MODIFY COLUMN stock_current DECIMAL(19,4) NOT NULL DEFAULT 0;

UPDATE stock_branch SET stock_min = 0 WHERE stock_min IS NULL;
ALTER TABLE stock_branch MODIFY COLUMN stock_min DECIMAL(19,4) NOT NULL DEFAULT 0;

UPDATE stock_branch SET stock_max = 0 WHERE stock_max IS NULL;
ALTER TABLE stock_branch MODIFY COLUMN stock_max DECIMAL(19,4) NOT NULL DEFAULT 0;
