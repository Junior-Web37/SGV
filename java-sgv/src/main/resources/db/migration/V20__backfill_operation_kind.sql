-- Backfill operation_kind based on existing reason/description heuristics
-- Set SALE when description/reason mention 'venda'
UPDATE cash_movements
SET operation_kind = 'SALE'
WHERE operation_kind IS NULL
  AND (
    LOWER(COALESCE(reason, '')) LIKE '%venda%'
    OR LOWER(COALESCE(description, '')) LIKE '%venda%'
  );

-- Set QUOTE when mentions of cotação / cotacao / cota
UPDATE cash_movements
SET operation_kind = 'QUOTE'
WHERE operation_kind IS NULL
  AND (
    LOWER(COALESCE(reason, '')) LIKE '%cota%'
    OR LOWER(COALESCE(description, '')) LIKE '%cota%'
  );

-- Set REFUND when mentions of devolução / devolucao / reembolso / refund
UPDATE cash_movements
SET operation_kind = 'REFUND'
WHERE operation_kind IS NULL
  AND (
    LOWER(COALESCE(reason, '')) LIKE '%devol%'
    OR LOWER(COALESCE(description, '')) LIKE '%devol%'
    OR LOWER(COALESCE(reason, '')) LIKE '%reemb%'
    OR LOWER(COALESCE(description, '')) LIKE '%reemb%'
    OR LOWER(COALESCE(reason, '')) LIKE '%refund%'
    OR LOWER(COALESCE(description, '')) LIKE '%refund%'
  );

-- Set TRANSFER when mentions of transfer
UPDATE cash_movements
SET operation_kind = 'TRANSFER'
WHERE operation_kind IS NULL
  AND (
    LOWER(COALESCE(reason, '')) LIKE '%transfer%'
    OR LOWER(COALESCE(description, '')) LIKE '%transfer%'
  );

-- Remaining nulls -> OTHER
UPDATE cash_movements
SET operation_kind = 'OTHER'
WHERE operation_kind IS NULL;
