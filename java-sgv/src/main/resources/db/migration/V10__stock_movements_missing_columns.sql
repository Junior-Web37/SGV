-- V10: Add missing stock_movements columns for fresh installs
-- V7 is a placeholder, V9 assumes these columns exist. Fresh DB needs them created.

ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS unit_cost_price DOUBLE DEFAULT 0 AFTER user_id;
ALTER TABLE stock_movements ADD COLUMN IF NOT EXISTS unit_sale_price DOUBLE DEFAULT 0 AFTER unit_cost_price;
