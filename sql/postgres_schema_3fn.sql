-- PostgreSQL 3NF schema for SGV (Moçambique)
-- Roles and users with normalization and many-to-many mapping

CREATE TABLE IF NOT EXISTS branches (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  nuit TEXT,
  address TEXT,
  contact TEXT,
  is_head BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS roles (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  description TEXT
);

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  full_name TEXT,
  email TEXT,
  branch_id BIGINT REFERENCES branches(id),
  force_change_password BOOLEAN DEFAULT FALSE,
  can_view_stats BOOLEAN DEFAULT FALSE,
  commission_percent NUMERIC(5,2) DEFAULT 0,
  active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
  role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, role_id)
);

-- Customers normalized
CREATE TABLE IF NOT EXISTS customers (
  id BIGSERIAL PRIMARY KEY,
  code TEXT UNIQUE,
  name TEXT NOT NULL,
  nuit TEXT,
  type TEXT, -- e.g., 'SINGULAR', 'COLECTIVA', 'CORRENTE', 'DIVERSO'
  credit_limit NUMERIC DEFAULT 0,
  balance NUMERIC DEFAULT 0,
  default_discount NUMERIC(5,2) DEFAULT 0,
  fidelity_points INTEGER DEFAULT 0,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Products and related tables
CREATE TABLE IF NOT EXISTS categories (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS products (
  id BIGSERIAL PRIMARY KEY,
  code TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  category_id BIGINT REFERENCES categories(id),
  is_service BOOLEAN DEFAULT FALSE,
  unit TEXT,
  unit_bulk TEXT,
  price_cost NUMERIC DEFAULT 0,
  price_sale NUMERIC DEFAULT 0,
  price_sale_bulk NUMERIC DEFAULT 0,
  bulk_quantity NUMERIC DEFAULT 0,
  conversion_factor NUMERIC DEFAULT 1,
  profit_margin NUMERIC DEFAULT 0,
  tax_rate NUMERIC DEFAULT 0,
  expiry_date DATE,
  supplier_id BIGINT REFERENCES suppliers(id)
);

CREATE TABLE IF NOT EXISTS product_barcodes (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
  barcode TEXT NOT NULL,
  quantity NUMERIC DEFAULT 1
);

CREATE TABLE IF NOT EXISTS suppliers (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  nuit TEXT,
  contact TEXT
);

-- Stock per branch (normalized)
CREATE TABLE IF NOT EXISTS stock_branch (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
  branch_id BIGINT REFERENCES branches(id) ON DELETE CASCADE,
  stock_current NUMERIC DEFAULT 0,
  stock_min NUMERIC DEFAULT 0,
  stock_max NUMERIC DEFAULT 0
);

CREATE TABLE IF NOT EXISTS stock_movements (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT REFERENCES products(id),
  branch_id BIGINT REFERENCES branches(id),
  type TEXT NOT NULL,
  subtype TEXT,
  qty NUMERIC DEFAULT 0,
  stock_before NUMERIC,
  stock_after NUMERIC,
  user_id BIGINT REFERENCES users(id),
  reference TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Sales and documents
CREATE TABLE IF NOT EXISTS sales (
  id BIGSERIAL PRIMARY KEY,
  document_number BIGINT,
  series TEXT,
  document_type TEXT,
  branch_id BIGINT REFERENCES branches(id),
  customer_id BIGINT REFERENCES customers(id),
  customer_name TEXT,
  customer_nuit TEXT,
  subtotal NUMERIC,
  total_tax NUMERIC,
  total NUMERIC,
  currency TEXT DEFAULT 'MZN',
  exchange_rate NUMERIC DEFAULT 1,
  payment_method TEXT,
  state TEXT DEFAULT 'EMITIDA',
  signature_hash TEXT,
  origin_sale_id BIGINT REFERENCES sales(id),
  annul_reason TEXT,
  annul_date TIMESTAMP WITHOUT TIME ZONE,
  offline_flag BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sale_items (
  id BIGSERIAL PRIMARY KEY,
  sale_id BIGINT REFERENCES sales(id) ON DELETE CASCADE,
  product_id BIGINT REFERENCES products(id),
  description TEXT,
  qty NUMERIC DEFAULT 1,
  unit_price NUMERIC DEFAULT 0,
  tax_rate NUMERIC DEFAULT 0,
  line_total NUMERIC DEFAULT 0
);

CREATE TABLE IF NOT EXISTS payments (
  id BIGSERIAL PRIMARY KEY,
  sale_id BIGINT REFERENCES sales(id) ON DELETE CASCADE,
  amount NUMERIC,
  method TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Cash sessions
CREATE TABLE IF NOT EXISTS sessions_cash (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  branch_id BIGINT REFERENCES branches(id),
  opened_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  closed_at TIMESTAMP WITHOUT TIME ZONE,
  initial_amount NUMERIC DEFAULT 0,
  system_amount NUMERIC DEFAULT 0,
  declared_amount NUMERIC DEFAULT 0,
  state TEXT DEFAULT 'ABERTA'
);

CREATE TABLE IF NOT EXISTS movements_cash (
  id BIGSERIAL PRIMARY KEY,
  session_id BIGINT REFERENCES sessions_cash(id) ON DELETE CASCADE,
  type TEXT,
  amount NUMERIC,
  note TEXT,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Purchases
CREATE TABLE IF NOT EXISTS purchases (
  id BIGSERIAL PRIMARY KEY,
  supplier_id BIGINT REFERENCES suppliers(id),
  invoice_number TEXT,
  date TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  subtotal NUMERIC,
  total_tax NUMERIC,
  total NUMERIC,
  state TEXT DEFAULT 'PENDENTE'
);

CREATE TABLE IF NOT EXISTS purchase_items (
  id BIGSERIAL PRIMARY KEY,
  purchase_id BIGINT REFERENCES purchases(id) ON DELETE CASCADE,
  product_id BIGINT REFERENCES products(id),
  qty NUMERIC,
  unit_price NUMERIC
);

-- Audit and configurations
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now(),
  user_id BIGINT REFERENCES users(id),
  action TEXT,
  table_name TEXT,
  target_id BIGINT,
  details JSONB,
  verification_hash TEXT
);

CREATE TABLE IF NOT EXISTS configurations (
  key TEXT PRIMARY KEY,
  value TEXT
);

-- =====================================================
-- MÓDULO: DESPESAS (NOVO - 2026-07-02)
-- =====================================================
CREATE TABLE IF NOT EXISTS expenses (
  id BIGSERIAL PRIMARY KEY,
  description TEXT NOT NULL,
  category TEXT NOT NULL,              -- RENT, SALARY, UTILITIES, SUPPLIES, TRANSPORT, MARKETING, MAINTENANCE, TAXES, OTHER
  amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  due_date DATE,                       -- Data de vencimento
  paid_at TIMESTAMP WITHOUT TIME ZONE,  -- Data de pagamento
  state TEXT DEFAULT 'PENDING',         -- PENDING, PAID, OVERDUE, CANCELLED
  notes TEXT,
  branch_id BIGINT REFERENCES branches(id),
  user_id BIGINT REFERENCES users(id),
  created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now()
);

-- Índices para pesquisas rápidas
CREATE INDEX IF NOT EXISTS idx_expenses_state ON expenses(state);
CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(category);
CREATE INDEX IF NOT EXISTS idx_expenses_branch ON expenses(branch_id);
CREATE INDEX IF NOT EXISTS idx_expenses_due_date ON expenses(due_date);

-- =====================================================
-- MÓDULO: UNIDADES DE MEDIDA (NOVO - 2026-07-02)
-- =====================================================
CREATE TABLE IF NOT EXISTS metric_units (
  id BIGSERIAL PRIMARY KEY,
  abbreviation VARCHAR(10) NOT NULL UNIQUE,
  description TEXT NOT NULL
);

-- Unidades padrão
INSERT INTO metric_units (abbreviation, description) VALUES
  ('UN', 'Unidade'),
  ('KG', 'Quilograma'),
  ('G', 'Grama'),
  ('LT', 'Litro'),
  ('ML', 'Mililitro'),
  ('M', 'Metro'),
  ('CM', 'Centímetro'),
  ('PC', 'Peça'),
  ('CX', 'Caixa'),
  ('SAC', 'Saco'),
  ('BAR', 'Barra'),
  ('LTR', 'Lote'),
  ('H', 'Hora'),
  ('D', 'Dia'),
  ('M2', 'Metro Quadrado'),
  ('M3', 'Metro Cúbico')
ON CONFLICT (abbreviation) DO NOTHING;
