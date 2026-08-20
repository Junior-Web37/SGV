-- V25: Criar tabela series_counters para numeração atómica com lock e adicionar bloqueio otimista (version) em stock_branch e stock_warehouse

CREATE TABLE IF NOT EXISTS series_counters (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id            BIGINT NULL,
    series               VARCHAR(10) NOT NULL DEFAULT 'A',
    document_type        VARCHAR(30) NOT NULL DEFAULT 'VENDA',
    current_number       BIGINT NOT NULL DEFAULT 0,
    last_hash_control    BIGINT NOT NULL DEFAULT 0,
    last_signature_hash  VARCHAR(255) NULL,
    updated_at           DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_series_counter_branch_series_type UNIQUE (branch_id, series, document_type),
    CONSTRAINT fk_series_counter_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bloqueio Otimista contra Race Conditions de Inventário
ALTER TABLE stock_branch
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE stock_warehouse
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Índices de Alta Performance para Eliminação de Gargalos
CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales (customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_branch_id ON sales (branch_id);
CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales (created_at);
CREATE INDEX IF NOT EXISTS idx_sales_state ON sales (state);

CREATE INDEX IF NOT EXISTS idx_stock_movements_product_id ON stock_movements (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_branch_id ON stock_movements (branch_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements (created_at);

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_supplier_id ON products (supplier_id);
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products (is_active);

-- End V25
