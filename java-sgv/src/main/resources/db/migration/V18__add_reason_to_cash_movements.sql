-- Add missing reason column for cash movements
ALTER TABLE cash_movements
    ADD COLUMN reason VARCHAR(50);
