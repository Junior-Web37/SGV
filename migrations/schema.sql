-- SGV initial schema (SQLite)
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS branches (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  nuit TEXT,
  address TEXT,
  contact TEXT,
  is_head INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  full_name TEXT,
  email TEXT,
  branch_id INTEGER,
  force_change_password INTEGER DEFAULT 0,
  can_view_stats INTEGER DEFAULT 0,
  commission_percent REAL DEFAULT 0,
  active INTEGER DEFAULT 1,
  FOREIGN KEY(branch_id) REFERENCES branches(id)
);

CREATE TABLE IF NOT EXISTS roles (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE NOT NULL,
  description TEXT
);

CREATE TABLE IF NOT EXISTS role_permissions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  role_id INTEGER NOT NULL,
  permissions_json TEXT,
  FOREIGN KEY(role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS products (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  category_id INTEGER,
  is_service INTEGER DEFAULT 0,
  unit TEXT,
  unit_bulk TEXT,
  price_cost REAL DEFAULT 0,
  price_sale REAL DEFAULT 0,
  price_sale_bulk REAL DEFAULT 0,
  bulk_quantity REAL DEFAULT 0,
  conversion_factor REAL DEFAULT 1,
  profit_margin REAL DEFAULT 0,
  tax_rate REAL DEFAULT 0,
  stock_current REAL DEFAULT 0,
  stock_min REAL DEFAULT 0,
  stock_max REAL DEFAULT 0,
  expiry_date TEXT,
  supplier_id INTEGER,
  image_path TEXT
);

CREATE TABLE IF NOT EXISTS product_barcodes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  barcode TEXT NOT NULL,
  quantity REAL DEFAULT 1,
  FOREIGN KEY(product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS stock_branch (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  branch_id INTEGER NOT NULL,
  stock_current REAL DEFAULT 0,
  stock_min REAL DEFAULT 0,
  stock_max REAL DEFAULT 0,
  FOREIGN KEY(product_id) REFERENCES products(id),
  FOREIGN KEY(branch_id) REFERENCES branches(id)
);

CREATE TABLE IF NOT EXISTS stock_movements (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  product_id INTEGER NOT NULL,
  branch_id INTEGER,
  type TEXT NOT NULL,
  subtype TEXT,
  qty REAL DEFAULT 0,
  stock_before REAL,
  stock_after REAL,
  user_id INTEGER,
  reference TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS customers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code TEXT UNIQUE,
  name TEXT NOT NULL,
  nuit TEXT,
  type TEXT,
  credit_limit REAL DEFAULT 0,
  balance REAL DEFAULT 0,
  default_discount REAL DEFAULT 0,
  fidelity_points INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sales (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  document_number INTEGER,
  series TEXT,
  document_type TEXT,
  branch_id INTEGER,
  customer_id INTEGER,
  customer_name TEXT,
  customer_nuit TEXT,
  subtotal REAL,
  total_tax REAL,
  total REAL,
  currency TEXT DEFAULT 'MZN',
  exchange_rate REAL DEFAULT 1,
  payment_method TEXT,
  state TEXT DEFAULT 'EMITIDA',
  signature_hash TEXT,
  origin_sale_id INTEGER,
  annul_reason TEXT,
  annul_date TEXT,
  offline_flag INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS sale_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sale_id INTEGER NOT NULL,
  product_id INTEGER,
  description TEXT,
  qty REAL DEFAULT 1,
  unit_price REAL DEFAULT 0,
  tax_rate REAL DEFAULT 0,
  line_total REAL DEFAULT 0,
  FOREIGN KEY(sale_id) REFERENCES sales(id)
);

CREATE TABLE IF NOT EXISTS payments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sale_id INTEGER,
  amount REAL,
  method TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS sessions_cash (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER,
  branch_id INTEGER,
  opened_at TEXT DEFAULT (datetime('now')),
  closed_at TEXT,
  initial_amount REAL DEFAULT 0,
  system_amount REAL DEFAULT 0,
  declared_amount REAL DEFAULT 0,
  state TEXT DEFAULT 'ABERTA'
);

CREATE TABLE IF NOT EXISTS movements_cash (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id INTEGER,
  type TEXT,
  amount REAL,
  note TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS suppliers (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT,
  nuit TEXT,
  contact TEXT
);

CREATE TABLE IF NOT EXISTS purchases (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  supplier_id INTEGER,
  invoice_number TEXT,
  date TEXT,
  subtotal REAL,
  total_tax REAL,
  total REAL,
  state TEXT DEFAULT 'PENDENTE'
);

CREATE TABLE IF NOT EXISTS purchase_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  purchase_id INTEGER,
  product_id INTEGER,
  qty REAL,
  unit_price REAL
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  created_at TEXT DEFAULT (datetime('now')),
  user_id INTEGER,
  action TEXT,
  table_name TEXT,
  target_id INTEGER,
  details TEXT,
  verification_hash TEXT
);

CREATE TABLE IF NOT EXISTS configurations (
  key TEXT PRIMARY KEY,
  value TEXT
);
