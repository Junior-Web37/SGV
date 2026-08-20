-- V26: Criar tabelas para Gestão de Lotes & Validades (product_batches) e Encomendas/Pedidos de Compra (purchase_orders)

CREATE TABLE IF NOT EXISTS product_batches (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT NOT NULL,
    batch_number      VARCHAR(100) NOT NULL,
    manufacture_date  DATE NULL,
    expiry_date       DATE NOT NULL,
    quantity          DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    branch_id         BIGINT NULL,
    warehouse_id      BIGINT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, NEAR_EXPIRY, EXPIRED, DEPLETED',
    created_at        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_batch_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_batch_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE SET NULL,
    CONSTRAINT fk_batch_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_product_batches_product ON product_batches (product_id);
CREATE INDEX IF NOT EXISTS idx_product_batches_expiry ON product_batches (expiry_date);
CREATE INDEX IF NOT EXISTS idx_product_batches_status ON product_batches (status);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number        VARCHAR(100) NOT NULL UNIQUE,
    supplier_id         BIGINT NOT NULL,
    target_warehouse_id BIGINT NULL,
    expected_date       DATE NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, RECEIVED, CANCELLED',
    subtotal            DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_tax           DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total               DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    notes               VARCHAR(1024) NULL,
    created_by          BIGINT NULL,
    created_at          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_warehouse FOREIGN KEY (target_warehouse_id) REFERENCES warehouses (id) ON DELETE SET NULL,
    CONSTRAINT fk_po_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier ON purchase_orders (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_status ON purchase_orders (status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_created ON purchase_orders (created_at);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id   BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,
    quantity            DECIMAL(19, 4) NOT NULL DEFAULT 1.0000,
    unit_cost           DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    subtotal            DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    CONSTRAINT fk_poi_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_poi_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_purchase_order_items_order ON purchase_order_items (purchase_order_id);

-- End V26
