-- =============================================
-- SGV Migration V3 — Sistema de Armazém
-- Adiciona: warehouses, stock_warehouse, warehouse_transfers, warehouse_transfer_items
-- Modifica: purchases (target_warehouse_id) para suportar entrada no armazém
-- =============================================

-- ─── Armazéns ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS warehouses (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(64) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    nuit          VARCHAR(64),
    address       VARCHAR(255),
    contact       VARCHAR(255),
    is_active     BIT(1) NOT NULL DEFAULT 1,
    notes         TEXT,
    created_at    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

-- ─── Stock por armazém ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS stock_warehouse (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_current   DOUBLE NOT NULL DEFAULT 0,
    stock_min       DOUBLE DEFAULT 0,
    stock_max       DOUBLE DEFAULT 0,
    warehouse_id    BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    CONSTRAINT fk_stock_wh_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_stock_wh_product   FOREIGN KEY (product_id)   REFERENCES products(id),
    UNIQUE KEY uk_stock_wh (warehouse_id, product_id)
) ENGINE=InnoDB;

CREATE INDEX idx_stock_wh_warehouse ON stock_warehouse(warehouse_id);
CREATE INDEX idx_stock_wh_product   ON stock_warehouse(product_id);

-- ─── Transferências armazém → filial ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS warehouse_transfers (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_number          BIGINT,
    document_year            INT,
    series                   VARCHAR(32) DEFAULT 'TWA',
    status                   VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    notes                    TEXT,
    cancel_reason            VARCHAR(512),
    created_at               DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    processed_at             DATETIME(6),
    warehouse_id             BIGINT NOT NULL,
    branch_id                BIGINT NOT NULL,
    requested_by             BIGINT,
    processed_by             BIGINT,
    CONSTRAINT fk_wht_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_wht_branch    FOREIGN KEY (branch_id)    REFERENCES branches(id),
    CONSTRAINT fk_wht_reqby     FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT fk_wht_procby    FOREIGN KEY (processed_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS warehouse_transfer_items (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity               DOUBLE NOT NULL DEFAULT 0,
    quantity_received      DOUBLE DEFAULT 0,
    warehouse_transfer_id  BIGINT NOT NULL,
    product_id             BIGINT NOT NULL,
    CONSTRAINT fk_whti_transfer FOREIGN KEY (warehouse_transfer_id) REFERENCES warehouse_transfers(id),
    CONSTRAINT fk_whti_product  FOREIGN KEY (product_id)            REFERENCES products(id)
) ENGINE=InnoDB;

CREATE INDEX idx_wht_warehouse      ON warehouse_transfers(warehouse_id);
CREATE INDEX idx_wht_branch         ON warehouse_transfers(branch_id);
CREATE INDEX idx_wht_status         ON warehouse_transfers(status);
CREATE INDEX idx_whti_transfer      ON warehouse_transfer_items(warehouse_transfer_id);
CREATE INDEX idx_whti_product       ON warehouse_transfer_items(product_id);

-- ─── Modificar purchases: target_warehouse_id ───────────────────────────
ALTER TABLE purchases
    ADD COLUMN IF NOT EXISTS target_warehouse_id BIGINT AFTER branch_id,
    ADD CONSTRAINT fk_purchases_warehouse FOREIGN KEY (target_warehouse_id) REFERENCES warehouses(id);

-- ─── Armazém demo (Matola Central) para não quebrar dados existentes ─────
INSERT IGNORE INTO warehouses (id, code, name, nuit, address, contact, is_active, notes)
VALUES (1, 'ARM-01', 'Armazém Central', NULL, 'Av. de Moçambique, Matola', '+258 84 000 0000', 1, 'Armazém central - criado na migração V3');
