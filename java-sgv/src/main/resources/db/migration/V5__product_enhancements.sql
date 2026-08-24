-- =============================================
-- SGV Migration V5 — Melhorias no Cadastro de Produtos
-- Adiciona: activo/inactivo, stock máx/mín, comissão, observações
-- Modifica: fornecedor para FK, IVA/ICE como percentuais padrão
-- =============================================

-- ─── 1. PRODUTO ACTIVO/INACTIVO ──────────────────────────────────────────
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_active BIT(1) NOT NULL DEFAULT 1 AFTER name;

-- ─── 2. STOCK MÁXIMO E MÍNIMO ────────────────────────────────────────────
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock_max DOUBLE DEFAULT 0 AFTER ice_rate;
ALTER TABLE products ADD COLUMN IF NOT EXISTS stock_min DOUBLE DEFAULT 0 AFTER stock_max;

-- ─── 3. COMISSÃO POR PRODUTO ──────────────────────────────────────────────
ALTER TABLE products ADD COLUMN IF NOT EXISTS commission_percent DOUBLE DEFAULT 0 AFTER stock_min;

-- ─── 4. CAMPO DE OBSERVAÇÕES ─────────────────────────────────────────────
ALTER TABLE products ADD COLUMN IF NOT EXISTS observations TEXT AFTER commission_percent;

-- ─── 5. TAXAS PADRÃO IVA/ICE (usado quando não definido por categoria) ───
ALTER TABLE products ADD COLUMN IF NOT EXISTS default_tax_rate DOUBLE DEFAULT 16.0 AFTER observations;
ALTER TABLE products ADD COLUMN IF NOT EXISTS default_ice_rate DOUBLE DEFAULT 0 AFTER default_tax_rate;

-- ─── 6. CONFIGURAÇÃO DE TAXAS PADRÃO NA APP_CONFIG ───────────────────────
INSERT IGNORE INTO app_config (id) VALUES (1);
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS default_tax_rate DOUBLE DEFAULT 16.0;
ALTER TABLE app_config ADD COLUMN IF NOT EXISTS default_ice_rate DOUBLE DEFAULT 0.0;
UPDATE app_config SET 
    default_tax_rate = 16.0,
    default_ice_rate = 0.0
WHERE id = 1;

-- ─── 7. ÍNDICES PARA NOVOS CAMPOS ────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_products_is_active    ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_stock_max    ON products(stock_max);
CREATE INDEX IF NOT EXISTS idx_products_stock_min    ON products(stock_min);
CREATE INDEX IF NOT EXISTS idx_products_commission   ON products(commission_percent);

-- ─── 8. MIGRAR PRODUTOS EXISTENTES: activo=1, stock_max=0, stock_min=0 ─
UPDATE products SET is_active = 1 WHERE is_active IS NULL OR is_active = 0;
UPDATE products SET stock_max = 0 WHERE stock_max IS NULL;
UPDATE products SET stock_min = 0 WHERE stock_min IS NULL;
UPDATE products SET commission_percent = 0 WHERE commission_percent IS NULL;
UPDATE products SET default_tax_rate = 16.0 WHERE default_tax_rate IS NULL;
UPDATE products SET default_ice_rate = 0.0 WHERE default_ice_rate IS NULL;
