-- Add operation_kind column to cash_movements
ALTER TABLE cash_movements ADD COLUMN operation_kind VARCHAR(20);
-- Optionally backfill based on existing reason/description; keep null by default
-- UPDATE cash_movements SET operation_kind = 'OTHER' WHERE operation_kind IS NULL;
