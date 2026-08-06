-- V22: Convert remaining DOUBLE columns (percentages, stock limits, exchange rate) to DECIMAL
-- Aligns the DB with the BigDecimal entity fields for products/app_config.

-- products: stock limits and percentages
ALTER TABLE products MODIFY COLUMN stock_max DECIMAL(19,4) DEFAULT 0.0000;
ALTER TABLE products MODIFY COLUMN stock_min DECIMAL(19,4) DEFAULT 0.0000;
ALTER TABLE products MODIFY COLUMN commission_percent DECIMAL(9,4) DEFAULT 0.0000;
ALTER TABLE products MODIFY COLUMN default_tax_rate DECIMAL(9,4) DEFAULT 16.0000;
ALTER TABLE products MODIFY COLUMN default_ice_rate DECIMAL(9,4) DEFAULT 0.0000;

-- app_config: percentages and exchange rate
ALTER TABLE app_config MODIFY COLUMN default_tax_rate DECIMAL(9,4) DEFAULT 16.0000;
ALTER TABLE app_config MODIFY COLUMN default_ice_rate DECIMAL(9,4) DEFAULT 0.0000;
ALTER TABLE app_config MODIFY COLUMN stock_min_alert_percent DECIMAL(9,4) DEFAULT 20.0000;
ALTER TABLE app_config MODIFY COLUMN max_discount_percent DECIMAL(9,4) DEFAULT 10.0000;
ALTER TABLE app_config MODIFY COLUMN exchange_rate DECIMAL(19,4) DEFAULT 74.0000;

-- End V22
