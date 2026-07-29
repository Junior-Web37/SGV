-- V8: Add missing schema objects for Hibernate validate mode
-- Adds columns and tables that exist in JPA entities but were missing from V1-V7

-- =============================================
-- role_permissions table (used by Role entity @ElementCollection)
-- =============================================
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id     BIGINT NOT NULL,
    permission  VARCHAR(255),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions(role_id);

-- =============================================
-- products: missing columns (description, supplierReference, entryDate, exitDate, location)
-- =============================================
ALTER TABLE products ADD COLUMN IF NOT EXISTS description         VARCHAR(1024) AFTER expiry_date;
ALTER TABLE products ADD COLUMN IF NOT EXISTS supplier_reference  VARCHAR(255)  AFTER description;
ALTER TABLE products ADD COLUMN IF NOT EXISTS entry_date          VARCHAR(255)  AFTER supplier_reference;
ALTER TABLE products ADD COLUMN IF NOT EXISTS exit_date           VARCHAR(255)  AFTER entry_date;
ALTER TABLE products ADD COLUMN IF NOT EXISTS location            VARCHAR(255)  AFTER exit_date;

-- =============================================
-- app_config: missing columns (stockMinAlertPercent, maxDiscountPercent)
-- =============================================
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS stock_min_alert_percent DOUBLE DEFAULT 20.0;
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS max_discount_percent    DOUBLE DEFAULT 10.0;

-- =============================================
-- stock_movements: missing notes column
-- =============================================
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS notes VARCHAR(1024) AFTER user_id;

-- =============================================
-- products: also add default_tax_rate and default_ice_rate if V5 didn't
-- (V5 adds these to products, but only if the product ALTER succeeded)
-- These are already in V5; this is a safety net for edge cases
-- =============================================
ALTER TABLE products ADD COLUMN IF NOT EXISTS default_tax_rate DOUBLE DEFAULT 16.0 AFTER observations;
ALTER TABLE products ADD COLUMN IF NOT EXISTS default_ice_rate DOUBLE DEFAULT 0.0 AFTER default_tax_rate;
