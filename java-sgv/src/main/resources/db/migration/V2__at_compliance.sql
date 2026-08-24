-- =============================================
-- SGV Migration V2 — Conformidade AT (Decreto 7/2024)
-- Adiciona campos de facturação electrónica, retenção na fonte,
-- motivos de isenção, e tabelas para wizard de setup e auditoria.
-- Idempotente: pode ser executada múltiplas vezes sem erro.
-- =============================================

-- ─── branches: certificação AT ────────────────────────────────────────────
ALTER TABLE branches ADD COLUMN IF NOT EXISTS software_cert_number VARCHAR(255) AFTER is_head;
ALTER TABLE branches ADD COLUMN IF NOT EXISTS license_number       VARCHAR(255) AFTER software_cert_number;

-- ─── sales: campos AT obrigatórios ────────────────────────────────────────
ALTER TABLE sales ADD COLUMN IF NOT EXISTS hash_hash               VARCHAR(255) AFTER qr_code;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS hash_control            BIGINT       AFTER hash_hash;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS withholding_tax_rate    DOUBLE       DEFAULT 0 AFTER hash_control;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS withholding_tax         DOUBLE       DEFAULT 0 AFTER withholding_tax_rate;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS paid_amount             DOUBLE       DEFAULT 0 AFTER withholding_tax;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS change_amount           DOUBLE       DEFAULT 0 AFTER paid_amount;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS demo_flag               BIT(1)       DEFAULT 0 AFTER change_amount;

-- ─── sale_items: tipo de imposto e motivo de isenção ─────────────────────
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS tax_type           VARCHAR(64)  DEFAULT 'IVA' AFTER tax_rate;
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS motivo_inexist_tax VARCHAR(16)  AFTER tax_type;

-- ─── payments: detalhes de pagamento electrónico ─────────────────────────
ALTER TABLE payments ADD COLUMN IF NOT EXISTS terminal_ref         VARCHAR(255) AFTER method;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS card_type            VARCHAR(32)  AFTER terminal_ref;

-- ─── app_config: configuração global (uma única linha, id=1) ─────────────
CREATE TABLE IF NOT EXISTS app_config (
    id                            BIGINT       NOT NULL PRIMARY KEY,
    company_name                  VARCHAR(255),
    company_nuit                  VARCHAR(255),
    company_address               VARCHAR(255),
    company_phone                 VARCHAR(255),
    company_email                 VARCHAR(255),
    company_website               VARCHAR(255),
    software_cert_number          VARCHAR(255),
    license_number                VARCHAR(255),
    default_series                VARCHAR(32)  DEFAULT 'A',
    initial_document_number       BIGINT       DEFAULT 1,
    default_currency              VARCHAR(8)   DEFAULT 'MZN',
    consumer_final_nuit           VARCHAR(16)  DEFAULT '999999999',
    demo_mode                     BIT(1)       DEFAULT 0,
    auto_backup_enabled           BIT(1)       DEFAULT 1,
    offline_mode_enabled          BIT(1)       DEFAULT 1,
    duplicate_detection_enabled   BIT(1)       DEFAULT 1,
    duplicate_window_seconds      INT          DEFAULT 60,
    whatsapp_api_key              VARCHAR(255),
    whatsapp_phone_number         VARCHAR(64),
    sms_api_key                   VARCHAR(255),
    sms_sender                    VARCHAR(64),
    thermal_printer_name          VARCHAR(255),
    thermal_printer_width         INT          DEFAULT 80,
    setup_completed               BIT(1)       DEFAULT 0,
    setup_completed_at            DATETIME(6),
    created_at                    DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                    DATETIME(6)  DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

INSERT IGNORE INTO app_config (id, company_name, default_series, initial_document_number)
VALUES (1, 'Minha Empresa, Lda', 'A', 1);

-- ─── index adicional para hashHash (auditoria AT) ─────────────────────────
CREATE INDEX IF NOT EXISTS idx_sales_hash_hash     ON sales(hash_hash);
CREATE INDEX IF NOT EXISTS idx_sales_hash_control  ON sales(hash_control);
CREATE INDEX IF NOT EXISTS idx_sales_demo_flag     ON sales(demo_flag);
CREATE INDEX IF NOT EXISTS idx_payments_method     ON payments(method);
CREATE INDEX IF NOT EXISTS idx_sale_items_tax_type ON sale_items(tax_type);