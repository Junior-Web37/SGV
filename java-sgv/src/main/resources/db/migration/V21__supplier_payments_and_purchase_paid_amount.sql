-- =============================================
-- SGV Migration V21 — Supplier payments and purchase paid amount
-- =============================================

ALTER TABLE purchases
    ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(19,4) DEFAULT 0.0000;

CREATE TABLE IF NOT EXISTS supplier_payments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id   BIGINT,
    purchase_id   BIGINT,
    amount        DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    method        VARCHAR(128),
    reference     VARCHAR(255),
    created_at    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_supplier_payments_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_supplier_payments_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id)
) ENGINE=InnoDB;

CREATE INDEX IF NOT EXISTS idx_supplier_payments_supplier ON supplier_payments(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_payments_purchase ON supplier_payments(purchase_id);
CREATE INDEX IF NOT EXISTS idx_supplier_payments_created_at ON supplier_payments(created_at);
