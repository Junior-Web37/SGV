-- V28: Criar o livro de conta corrente do cliente (customer_account_entries) — correcção do BUG-005/BUG-010.
-- Cada variação de customers.balance passa a registar um lançamento sinalizado nesta tabela;
-- a invariante Σ(customer_account_entries.amount) == customers.balance é garantida pelo
-- CustomerAccountLedger (escrita única de saldo + lançamento na mesma transação).

CREATE TABLE IF NOT EXISTS customer_account_entries (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id   BIGINT NOT NULL,
    sale_id       BIGINT NULL,
    payment_id    BIGINT NULL,
    entry_type    VARCHAR(40) NOT NULL COMMENT 'CREDITO_SALE | PAYMENT | RECEIPT_WITHOUT_SALE | RECONCILIATION | CREDIT_NOTE | ANNULMENT | ADJUSTMENT',
    amount        DECIMAL(19, 4) NOT NULL COMMENT 'Valor sinalizado: positivo aumenta a dívida, negativo diminui',
    reference     VARCHAR(100) NULL COMMENT 'Referência curta do documento (ex.: FT A/12)',
    description   VARCHAR(500) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cae_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_cae_sale FOREIGN KEY (sale_id) REFERENCES sales (id) ON DELETE SET NULL,
    CONSTRAINT fk_cae_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_customer_account_entries_customer ON customer_account_entries (customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_customer_account_entries_sale ON customer_account_entries (sale_id);
CREATE INDEX IF NOT EXISTS idx_customer_account_entries_payment ON customer_account_entries (payment_id);

-- End V28
