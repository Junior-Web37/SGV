-- V23: Criar tabela payment_allocation para reconciliação de crédito e adicionar coluna last_movement_at em products

CREATE TABLE IF NOT EXISTS payment_allocation (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id    BIGINT NULL,
    sale_id       BIGINT NULL,
    amount_value  DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    created_at    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_payment_allocation_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL,
    CONSTRAINT fk_payment_allocation_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_payment_allocation_payment_id ON payment_allocation (payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_allocation_sale_id ON payment_allocation (sale_id);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS last_movement_at DATETIME(6) NULL COMMENT 'Data e hora da última movimentação de stock do produto';

-- End V23
