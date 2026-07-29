-- SGV Baseline Schema
-- V1: Migrates from Hibernate ddl-auto=update to Flyway-managed schema
-- Generated from existing MariaDB sgv database

-- =============================================
-- MASTER DATA (branches first — referenced by users)
-- =============================================

CREATE TABLE IF NOT EXISTS branches (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(255),
    address VARCHAR(255),
    contact VARCHAR(255),
    nuit    VARCHAR(255),
    is_head BIT(1) NOT NULL DEFAULT 0
);

-- =============================================
-- USERS & AUTH
-- =============================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    email           VARCHAR(255),
    active          BIT(1) NOT NULL DEFAULT 1,
    force_change_password BIT(1) NOT NULL DEFAULT 0,
    commission_percent DOUBLE NOT NULL DEFAULT 0,
    can_view_stats  BIT(1) NOT NULL DEFAULT 1,
    branch_id       BIGINT,
    CONSTRAINT fk_users_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS categories (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metric_units (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    abbreviation VARCHAR(10) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS suppliers (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    nuit    VARCHAR(255),
    address VARCHAR(255),
    contact VARCHAR(255),
    active  BIT(1) DEFAULT 1
);

CREATE TABLE IF NOT EXISTS customers (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    code              VARCHAR(255) UNIQUE,
    nuit              VARCHAR(255),
    address           VARCHAR(255),
    contact           VARCHAR(255),
    type              VARCHAR(255),
    balance           DOUBLE DEFAULT 0,
    credit_limit      DOUBLE DEFAULT 0,
    default_discount  DOUBLE DEFAULT 0,
    fidelity_points  INT DEFAULT 0,
    created_at        DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
);

-- =============================================
-- PRODUCTS
-- =============================================

CREATE TABLE IF NOT EXISTS products (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(255) NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    price_cost          DOUBLE,
    price_sale          DOUBLE,
    price_sale_bulk     DOUBLE,
    profit_margin       DOUBLE,
    tax_rate            DOUBLE,
    ice_rate            DOUBLE,
    unit                VARCHAR(255),
    unit_bulk           VARCHAR(255),
    bulk_quantity       DOUBLE,
    conversion_factor   DOUBLE,
    expiry_date         VARCHAR(255),
    is_service          BIT(1) DEFAULT 0,
    category_id         BIGINT,
    supplier_id         BIGINT,
    unit_id             BIGINT,
    unit_bulk_id        BIGINT,
    CONSTRAINT fk_products_category      FOREIGN KEY (category_id)    REFERENCES categories(id),
    CONSTRAINT fk_products_supplier      FOREIGN KEY (supplier_id)    REFERENCES suppliers(id),
    CONSTRAINT fk_products_unit          FOREIGN KEY (unit_id)        REFERENCES metric_units(id),
    CONSTRAINT fk_products_unit_bulk    FOREIGN KEY (unit_bulk_id)   REFERENCES metric_units(id)
);

CREATE TABLE IF NOT EXISTS product_barcodes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    barcode     VARCHAR(255) NOT NULL,
    quantity    DOUBLE DEFAULT 1,
    product_id  BIGINT,
    CONSTRAINT fk_barcodes_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =============================================
-- STOCK
-- =============================================

CREATE TABLE IF NOT EXISTS stock_branch (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_current DOUBLE DEFAULT 0,
    stock_min     DOUBLE DEFAULT 0,
    stock_max     DOUBLE DEFAULT 0,
    branch_id     BIGINT,
    product_id    BIGINT,
    CONSTRAINT fk_stock_branch     FOREIGN KEY (branch_id)  REFERENCES branches(id),
    CONSTRAINT fk_stock_product    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY uk_stock_branch_product (branch_id, product_id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    type          VARCHAR(255) NOT NULL,
    subtype       VARCHAR(255),
    qty           DOUBLE DEFAULT 0,
    stock_before  DOUBLE,
    stock_after   DOUBLE,
    reference     VARCHAR(255),
    created_at    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    branch_id     BIGINT,
    product_id    BIGINT,
    user_id       BIGINT,
    CONSTRAINT fk_movements_branch  FOREIGN KEY (branch_id)  REFERENCES branches(id),
    CONSTRAINT fk_movements_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_movements_user    FOREIGN KEY (user_id)    REFERENCES users(id)
);

-- =============================================
-- PURCHASES
-- =============================================

CREATE TABLE IF NOT EXISTS purchases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number  VARCHAR(255),
    purchase_date   DATETIME(6),
    subtotal        DOUBLE DEFAULT 0,
    total_tax       DOUBLE DEFAULT 0,
    total           DOUBLE DEFAULT 0,
    state           VARCHAR(255) DEFAULT 'PENDING',
    notes           VARCHAR(255),
    created_at      DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    branch_id       BIGINT,
    supplier_id     BIGINT NOT NULL,
    user_id         BIGINT,
    CONSTRAINT fk_purchases_branch   FOREIGN KEY (branch_id)   REFERENCES branches(id),
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_purchases_user     FOREIGN KEY (user_id)     REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS purchase_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity    DOUBLE DEFAULT 1,
    cost_price  DOUBLE DEFAULT 0,
    subtotal    DOUBLE DEFAULT 0,
    product_id  BIGINT NOT NULL,
    purchase_id BIGINT NOT NULL,
    CONSTRAINT fk_purchase_items_product FOREIGN KEY (product_id)  REFERENCES products(id),
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id)
);

-- =============================================
-- SALES
-- =============================================

CREATE TABLE IF NOT EXISTS sales (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    series              VARCHAR(255),
    document_type       VARCHAR(255),
    document_number     BIGINT,
    document_year       INT,
    created_at          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    customer_name       VARCHAR(255),
    customer_nuit      VARCHAR(255),
    customer_address   VARCHAR(255),
    subtotal            DOUBLE DEFAULT 0,
    total_discount     DOUBLE DEFAULT 0,
    total_tax          DOUBLE DEFAULT 0,
    total_ice          DOUBLE DEFAULT 0,
    total              DOUBLE DEFAULT 0,
    currency           VARCHAR(255) DEFAULT 'MZN',
    exchange_rate      DOUBLE DEFAULT 1,
    payment_method     VARCHAR(255),
    state              VARCHAR(255) DEFAULT 'EMITIDA',
    signature_hash     VARCHAR(255),
    qr_code            VARCHAR(255),
    reprint_count      INT DEFAULT 0,
    offline_flag       BIT(1) DEFAULT 0,
    pending_sync       BIT(1) DEFAULT 0,
    annul_reason       VARCHAR(255),
    annul_date         DATETIME(6),
    branch_id          BIGINT,
    customer_id        BIGINT,
    origin_sale_id    BIGINT,
    CONSTRAINT fk_sales_branch        FOREIGN KEY (branch_id)       REFERENCES branches(id),
    CONSTRAINT fk_sales_customer      FOREIGN KEY (customer_id)     REFERENCES customers(id),
    CONSTRAINT fk_sales_origin        FOREIGN KEY (origin_sale_id)  REFERENCES sales(id),
    UNIQUE KEY uk_sales_doc (branch_id, series, document_number, document_type, document_year)
);

CREATE TABLE IF NOT EXISTS sale_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code    VARCHAR(255),
    description     VARCHAR(255),
    unit            VARCHAR(255),
    qty             DOUBLE DEFAULT 1,
    unit_price      DOUBLE DEFAULT 0,
    discount        DOUBLE DEFAULT 0,
    ice_rate        DOUBLE DEFAULT 0,
    tax_rate        DOUBLE DEFAULT 0,
    line_base       DOUBLE DEFAULT 0,
    line_discount   DOUBLE DEFAULT 0,
    line_ice        DOUBLE DEFAULT 0,
    line_tax        DOUBLE DEFAULT 0,
    line_total      DOUBLE DEFAULT 0,
    product_id      BIGINT,
    sale_id         BIGINT,
    CONSTRAINT fk_sale_items_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_sale_items_sale   FOREIGN KEY (sale_id)    REFERENCES sales(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount      DOUBLE,
    method      VARCHAR(255),
    created_at  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    sale_id     BIGINT,
    CONSTRAINT fk_payments_sale FOREIGN KEY (sale_id) REFERENCES sales(id)
);

-- =============================================
-- EXPENSES
-- =============================================

CREATE TABLE IF NOT EXISTS expenses (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255),
    category    VARCHAR(255),
    amount      DOUBLE DEFAULT 0,
    due_date    DATE,
    paid_at     DATETIME(6),
    state       VARCHAR(255) DEFAULT 'PENDING',
    notes       VARCHAR(255),
    created_at  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    branch_id   BIGINT,
    user_id     BIGINT,
    CONSTRAINT fk_expenses_branch FOREIGN KEY (branch_id) REFERENCES branches(id),
    CONSTRAINT fk_expenses_user   FOREIGN KEY (user_id)   REFERENCES users(id)
);

-- =============================================
-- PRODUCTION
-- =============================================

CREATE TABLE IF NOT EXISTS production_orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number  VARCHAR(255) UNIQUE,
    quantity      DOUBLE,
    unit          VARCHAR(255),
    state         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes         VARCHAR(255),
    created_at    DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    completed_at  DATETIME(6),
    created_by    BIGINT,
    product_id    BIGINT,
    CONSTRAINT fk_prod_orders_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_prod_orders_product    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =============================================
-- CASH MANAGEMENT
-- =============================================

CREATE TABLE IF NOT EXISTS cash_sessions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    initial_value   DECIMAL(15,2),
    reported_value  DECIMAL(15,2),
    system_value    DECIMAL(15,2),
    opened_at       DATETIME(6),
    closed_at       DATETIME(6),
    state           VARCHAR(20) NOT NULL,
    notes           VARCHAR(255),
    branch_id       BIGINT,
    user_id         BIGINT NOT NULL,
    CONSTRAINT fk_cash_sessions_branch FOREIGN KEY (branch_id) REFERENCES branches(id),
    CONSTRAINT fk_cash_sessions_user   FOREIGN KEY (user_id)   REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS cash_movements (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(20) NOT NULL,
    amount      DECIMAL(15,2) NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    session_id  BIGINT NOT NULL,
    created_by  BIGINT,
    CONSTRAINT fk_cash_movements_session FOREIGN KEY (session_id) REFERENCES cash_sessions(id),
    CONSTRAINT fk_cash_movements_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- =============================================
-- TRANSFERS
-- =============================================

CREATE TABLE IF NOT EXISTS transfers (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    series              VARCHAR(255),
    document_number     BIGINT,
    document_year       INT,
    status              VARCHAR(255),
    notes               VARCHAR(255),
    cancel_reason       VARCHAR(255),
    created_at          DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    processed_at        DATETIME(6),
    source_branch_id    BIGINT,
    destination_branch_id BIGINT,
    requested_by        BIGINT,
    processed_by        BIGINT,
    CONSTRAINT fk_transfers_source      FOREIGN KEY (source_branch_id)      REFERENCES branches(id),
    CONSTRAINT fk_transfers_destination  FOREIGN KEY (destination_branch_id) REFERENCES branches(id),
    CONSTRAINT fk_transfers_requested_by FOREIGN KEY (requested_by)          REFERENCES users(id),
    CONSTRAINT fk_transfers_processed_by  FOREIGN KEY (processed_by)          REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS transfer_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity            DOUBLE DEFAULT 0,
    quantity_received   DOUBLE DEFAULT 0,
    product_id          BIGINT,
    transfer_id         BIGINT,
    CONSTRAINT fk_transfer_items_product  FOREIGN KEY (product_id)  REFERENCES products(id),
    CONSTRAINT fk_transfer_items_transfer FOREIGN KEY (transfer_id) REFERENCES transfers(id)
);

-- =============================================
-- AUDIT & SYSTEM
-- =============================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(255),
    action      VARCHAR(255),
    details     VARCHAR(1024),
    created_at  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS filter_presets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255),
    type        VARCHAR(255),
    data        TEXT,
    created_at  DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    user_id     BIGINT
);

-- =============================================
-- INDEXES
-- =============================================

CREATE INDEX idx_products_code         ON products(code);
CREATE INDEX idx_products_category      ON products(category_id);
CREATE INDEX idx_products_supplier      ON products(supplier_id);
CREATE INDEX idx_sales_branch           ON sales(branch_id);
CREATE INDEX idx_sales_customer         ON sales(customer_id);
CREATE INDEX idx_sales_state            ON sales(state);
CREATE INDEX idx_sales_created          ON sales(created_at);
CREATE INDEX idx_purchases_supplier     ON purchases(supplier_id);
CREATE INDEX idx_purchases_state        ON purchases(state);
CREATE INDEX idx_stock_product          ON stock_branch(product_id);
CREATE INDEX idx_stock_branch           ON stock_branch(branch_id);
CREATE INDEX idx_expenses_branch        ON expenses(branch_id);
CREATE INDEX idx_expenses_state         ON expenses(state);
CREATE INDEX idx_expenses_due_date      ON expenses(due_date);
CREATE INDEX idx_sessions_branch         ON cash_sessions(branch_id);
CREATE INDEX idx_sessions_state          ON cash_sessions(state);
CREATE INDEX idx_movements_product       ON stock_movements(product_id);
CREATE INDEX idx_movements_branch        ON stock_movements(branch_id);
CREATE INDEX idx_movements_created       ON stock_movements(created_at);
CREATE INDEX idx_transfers_source        ON transfers(source_branch_id);
CREATE INDEX idx_transfers_dest         ON transfers(destination_branch_id);
CREATE INDEX idx_transfers_status       ON transfers(status);
