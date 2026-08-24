# SGV — Database Schema Reference

> Auto-generated from JPA entity analysis. Last updated: 2026-07-08.
> Build: ✅ `mvn compile` passes (121 source files).

---

## Entity Overview (27 tables)

| # | Entity | Table | Notes |
|---|--------|-------|-------|
| 1 | Branch | `branches` | Filiais / loja matriz |
| 2 | User | `users` | `forceChangePassword`, `canViewStats`, `commissionPercent` |
| 3 | Role | `roles` | M–N via `user_roles` |
| 4 | Category | `categories` | Apenas `name` — sem hierarquia |
| 5 | MetricUnit | `metric_units` | `abbreviation` UNIQUE |
| 6 | Product | `products` | Core stock item; `category` → `categories` |
| 7 | ProductBarcode | `product_barcodes` | Barcode alternativo por produto |
| 8 | Supplier | `suppliers` | `name` UNIQUE |
| 9 | Customer | `customers` | `code` UNIQUE; `balance`, `creditLimit`, `fidelityPoints` |
| 10 | Sale | `sales` | Unique: `(branch, series, documentNumber, documentType, documentYear)` |
| 11 | SaleItem | `sale_items` | `sale_id` FK cascade ALL |
| 12 | Payment | `payments` | `sale_id` FK; método de pagamento |
| 13 | Purchase | `purchases` | `state`: RECEIVED / PENDING / CANCELLED |
| 14 | PurchaseItem | `purchase_items` | `purchase_id` FK cascade ALL |
| 15 | StockMovement | `stock_movements` | `type`: VENDA / COMPRA / AJUSTE / TRANSFER_IN / TRANSFER_OUT / PRODUCAO |
| 16 | StockBranch | `stock_branch` | Stock por filial; `stockCurrent`, `stockMin`, `stockMax` |
| 17 | CashSession | `cash_sessions` | `state`: OPEN / CLOSED |
| 18 | CashMovement | `cash_movements` | `type`: IN / OUT |
| 19 | Expense | `expenses` | `state`: PENDING / PAID / OVERDUE / CANCELLED; `category`: RENT/SALARY/UTILITIES/SUPPLIES/OTHER |
| 20 | ProductionOrder | `production_orders` | `state`: PENDING / IN_PROGRESS / COMPLETED / CANCELLED; `orderNumber` UNIQUE |
| 21 | AuditLog | `audit_logs` | Sem FK para user — guarda apenas `username` string |
| 22 | FilterPreset | `filter_presets` | `userId` (Long, não FK); `type`: PRODUCT/CUSTOMER/SALE; `data` = JSON/Lob |
| 23 | Transfer | `transfers` | ⭐ NEW (2026-07-08) — fluxo PENDING → APPROVED → COMPLETED |
| 24 | TransferItem | `transfer_items` | ⭐ NEW (2026-07-08) — linha de produto na transferência |
| — | FiscalDocumentType | `fiscal_document_types` | **Enum**, não entity |

---

## Table Details

---

### 1. `branches`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `name` | VARCHAR(255) | | |
| `nuit` | VARCHAR(50) | | Número de Identificação Tributária |
| `address` | VARCHAR(255) | | |
| `contact` | VARCHAR(100) | | |
| `is_head` | BOOLEAN | DEFAULT false | `true` = loja matriz |

**Relationships:** `users` → `branches(id)`, `sales` → `branches(id)`, `purchases` → `branches(id)`, `stock_movements` → `branches(id)`, `expenses` → `branches(id)`, `cash_sessions` → `branches(id)`, `transfers` → `branches(id)` (source + dest)

---

### 2. `users`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `username` | VARCHAR(255) | **UNIQUE**, NOT NULL | Login |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt |
| `full_name` | VARCHAR(255) | | |
| `email` | VARCHAR(255) | | |
| `branch_id` | BIGINT | FK → `branches` | Filial do utilizador |
| `force_change_password` | BOOLEAN | DEFAULT false | |
| `can_view_stats` | BOOLEAN | DEFAULT false | |
| `commission_percent` | DOUBLE | DEFAULT 0.0 | % comissão por venda |
| `active` | BOOLEAN | DEFAULT true | |

**Relationships:** M–N via `user_roles` → `roles(id)`

**Indexes:** `username` UNIQUE

---

### 3. `roles`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `name` | VARCHAR(255) | **UNIQUE**, NOT NULL | e.g. ADMIN, VENDAS, GERENTE |
| `description` | VARCHAR(255) | | |

**Join table:** `user_roles` (`user_id`, `role_id`)

---

### 4. `categories`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `name` | VARCHAR(255) | **UNIQUE**, NOT NULL | Sem hierarquia (plana) |

**Relationships:** `products` → `categories(id)`

---

### 5. `metric_units`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `abbreviation` | VARCHAR(10) | **UNIQUE**, NOT NULL | e.g. UN, KG, L, M |
| `description` | VARCHAR(255) | NOT NULL | e.g. Unidade, Quilograma |

---

### 6. `products`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `code` | VARCHAR(100) | **UNIQUE** | |
| `name` | VARCHAR(255) | NOT NULL | |
| `category_id` | BIGINT | FK → `categories` | |
| `unit_id` | BIGINT | FK → `metric_units` | |
| `purchase_price` | DOUBLE | DEFAULT 0 | |
| `sale_price` | DOUBLE | DEFAULT 0 | |
| `tax_rate` | DOUBLE | DEFAULT 0.0 | IVA % (e.g. 16) |
| `ice_rate` | DOUBLE | DEFAULT 0.0 | ICE % |
| `stock` | DOUBLE | DEFAULT 0 | Stock global (⚠ não é por filial) |
| `min_stock` | DOUBLE | DEFAULT 0 | Alerta mínimo |
| `barcode` | VARCHAR(100) | | |
| `image_url` | VARCHAR(500) | | |
| `active` | BOOLEAN | DEFAULT true | |
| `created_at` | DATETIME | | |
| `updated_at` | DATETIME | | |

**Indexes:** `code` UNIQUE

**⚠ Design issue:** `stock` é global, não por filial. Use `stock_branch` para stock por filial.

---

### 7. `product_barcodes`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `product_id` | BIGINT | FK → `products`, NOT NULL | |
| `barcode` | VARCHAR(255) | NOT NULL | |
| `quantity` | DOUBLE | DEFAULT 1.0 | Qtd. quando lido com este código |

**Relationships:** `product` → `products(id)`

---

### 8. `suppliers`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `name` | VARCHAR(255) | NOT NULL | |
| `nuit` | VARCHAR(50) | | |
| `contact` | VARCHAR(100) | | |
| `address` | VARCHAR(255) | | |
| `active` | BOOLEAN | DEFAULT true | |

**Relationships:** `purchases` → `suppliers(id)`

---

### 9. `customers`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `code` | VARCHAR(100) | **UNIQUE** | |
| `name` | VARCHAR(255) | NOT NULL | |
| `nuit` | VARCHAR(50) | | |
| `type` | VARCHAR(50) | | e.g. CONSUMIDOR, EMPRESA |
| `address` | VARCHAR(255) | | |
| `contact` | VARCHAR(100) | | |
| `credit_limit` | DOUBLE | DEFAULT 0 | |
| `balance` | DOUBLE | DEFAULT 0 | Saldo em dívida |
| `default_discount` | DOUBLE | DEFAULT 0 | % desconto padrão |
| `fidelity_points` | INTEGER | DEFAULT 0 | |
| `created_at` | DATETIME | | |

**Indexes:** `code` UNIQUE

---

### 10. `sales`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `document_number` | BIGINT | | Número do documento fiscal |
| `document_year` | INTEGER | | |
| `series` | VARCHAR(50) | | Série fiscal e.g. "A" |
| `document_type` | VARCHAR(50) | | VENDA / COTACAO / ENCOMENDA |
| `branch_id` | BIGINT | FK → `branches` | |
| `customer_id` | BIGINT | FK → `customers` | |
| `customer_name` | VARCHAR(255) | | Nome impresso no documento |
| `customer_nuit` | VARCHAR(50) | | NUIT impresso no documento |
| `customer_address` | VARCHAR(255) | | Endereço impresso no documento |
| `subtotal` | DOUBLE | | |
| `total_tax` | DOUBLE | | Total IVA |
| `total_ice` | DOUBLE | DEFAULT 0 | Total ICE |
| `total_discount` | DOUBLE | DEFAULT 0 | Total desconto |
| `total` | DOUBLE | | Valor total |
| `currency` | VARCHAR(10) | DEFAULT 'MZN' | |
| `exchange_rate` | DOUBLE | DEFAULT 1.0 | Taxa câmbio (para MZN) |
| `payment_method` | VARCHAR(50) | | e.g. DINHEIRO, TRANSFERENCIA |
| `state` | VARCHAR(50) | DEFAULT 'EMITIDA' | EMITIDA / ANULADA / COTACAO_ABERTA / COTACAO_PAGA |
| `signature_hash` | VARCHAR(255) | | Hash assinatura AT |
| `qr_code` | VARCHAR(255) | | QR Code AT |
| `reprint_count` | INTEGER | DEFAULT 0 | Contagem de reimpressões |
| `pending_sync` | BOOLEAN | DEFAULT false | Sincronização offline |
| `origin_sale_id` | BIGINT | FK → `sales` | Venda de origem (anulação/correção) |
| `annul_reason` | VARCHAR(255) | | Motivo da anulação |
| `annul_date` | DATETIME | | Data da anulação |
| `offline_flag` | BOOLEAN | DEFAULT false | |
| `created_at` | DATETIME | | |

**Unique constraint:** `(branch_id, series, documentNumber, documentType, documentYear)`

**Relationships:** `items` → `sale_items` (1:N cascade ALL), `payments` → `payments` (1:N cascade ALL), `originSale` → `sales`

**⚠ Fiscal rules enforced by JPA:** `@PreRemove` blocks delete of EMITIDA/ANULADA docs. `@PreUpdate` throws if EMITIDA (no direct DB updates).

---

### 11. `sale_items`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `sale_id` | BIGINT | FK → `sales`, NOT NULL | CASCADE ALL |
| `product_id` | BIGINT | FK → `products` | |
| `quantity` | DOUBLE | | |
| `unit_price` | DOUBLE | | |
| `discount_percent` | DOUBLE | DEFAULT 0 | |
| `tax_rate` | DOUBLE | DEFAULT 0 | |
| `ice_amount` | DOUBLE | DEFAULT 0 | |
| `subtotal` | DOUBLE | | (quantity × unit_price) − desconto |
| `created_at` | DATETIME | | |

**Relationships:** `sale` → `sales(id)` CASCADE ALL, `product` → `products(id)`

---

### 12. `payments`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINCT | PK, AUTO | |
| `sale_id` | BIGINT | FK → `sales` | |
| `amount` | DOUBLE | | |
| `method` | VARCHAR(50) | | DINHEIRO / TRANSFERENCIA / CHEQUE / DEPOSITO |
| `created_at` | DATETIME | | |

**Relationships:** `sale` → `sales(id)`

**⚠ Missing:** No repository custom query to find payments by sale — `PaymentRepository` only has `findBySaleId()` derived from JPA naming.

---

### 13. `purchases`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `document_number` | BIGINT | | |
| `series` | VARCHAR(50) | | |
| `state` | VARCHAR(50) | DEFAULT 'PENDING' | RECEIVED / PENDING / CANCELLED |
| `branch_id` | BIGINT | FK → `branches` | |
| `supplier_id` | BIGINT | FK → `suppliers` | |
| `subtotal` | DOUBLE | | |
| `total_tax` | DOUBLE | | |
| `total_discount` | DOUBLE | DEFAULT 0 | |
| `total` | DOUBLE | | |
| `payment_method` | VARCHAR(50) | | |
| `notes` | VARCHAR(500) | | |
| `created_at` | DATETIME | | |

**Relationships:** `items` → `purchase_items` (1:N), `branch` → `branches(id)`, `supplier` → `suppliers(id)`

---

### 14. `purchase_items`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `purchase_id` | BIGINT | FK → `purchases`, NOT NULL | CASCADE ALL |
| `product_id` | BIGINT | FK → `products` | |
| `quantity` | DOUBLE | | |
| `unit_price` | DOUBLE | | |
| `discount_percent` | DOUBLE | DEFAULT 0 | |
| `subtotal` | DOUBLE | | |
| `tax_amount` | DOUBLE | DEFAULT 0 | |

**Relationships:** `purchase` → `purchases(id)` CASCADE ALL, `product` → `products(id)`

---

### 15. `stock_movements`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `product_id` | BIGINT | FK → `products` | |
| `branch_id` | BIGINT | FK → `branches` | |
| `type` | VARCHAR(50) | NOT NULL | VENDA / COMPRA / AJUSTE / TRANSFER_IN / TRANSFER_OUT / PRODUCAO |
| `subtype` | VARCHAR(50) | | e.g. VENDA, TRANSFER |
| `qty` | DOUBLE | DEFAULT 0 | Qtd. (negativa para saída) |
| `stock_before` | DOUBLE | | |
| `stock_after` | DOUBLE | | |
| `user_id` | BIGINT | FK → `users` | Quem fez o movimento |
| `reference` | VARCHAR(255) | | Ref. documento origem e.g. "FAC-123/A" ou "TR-1/A" |
| `created_at` | DATETIME | | |

**Relationships:** `product` → `products(id)`, `branch` → `branches(id)`, `user` → `users(id)`

**⚠ No custom repository queries** — can't filter movements by date, branch, or type via repository methods.

---

### 16. `stock_branch`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `product_id` | BIGINT | FK → `products` | |
| `branch_id` | BIGINT | FK → `branches` | |
| `stock_current` | DOUBLE | DEFAULT 0 | Stock actual nesta filial |
| `stock_min` | DOUBLE | DEFAULT 0 | Alerta mínimo |
| `stock_max` | DOUBLE | DEFAULT 0 | Capacidade máxima |

**Unique constraint:** `(product_id, branch_id)`

**Relationships:** `product` → `products(id)`, `branch` → `branches(id)`

---

### 17. `cash_sessions`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `user_id` | BIGINT | FK → `users`, NOT NULL | |
| `branch_id` | BIGINT | FK → `branches` | |
| `opened_at` | DATETIME | | |
| `closed_at` | DATETIME | | |
| `initial_value` | DECIMAL(15,2) | DEFAULT 0 | Valor inicial na caixa |
| `reported_value` | DECIMAL(15,2) | | Valor contado pelo operador |
| `system_value` | DECIMAL(15,2) | | Valor calculado pelo sistema |
| `state` | VARCHAR(20) | DEFAULT 'OPEN' | OPEN / CLOSED |
| `notes` | VARCHAR(500) | | |

**Relationships:** `user` → `users(id)`, `branch` → `branches(id)`, `movements` → `cash_movements`

---

### 18. `cash_movements`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `session_id` | BIGINT | FK → `cash_sessions`, NOT NULL | |
| `type` | VARCHAR(20) | NOT NULL | IN / OUT |
| `amount` | DECIMAL(15,2) | NOT NULL | |
| `description` | VARCHAR(255) | | |
| `created_at` | DATETIME | | |
| `created_by` | BIGINT | FK → `users` | |

**Relationships:** `session` → `cash_sessions(id)`, `createdBy` → `users(id)`

---

### 19. `expenses`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `description` | VARCHAR(255) | | |
| `category` | VARCHAR(50) | | RENT / SALARY / UTILITIES / SUPPLIES / OTHER |
| `amount` | DOUBLE | DEFAULT 0 | |
| `due_date` | DATE | | |
| `paid_at` | DATETIME | | Quando foi pago |
| `state` | VARCHAR(50) | DEFAULT 'PENDING' | PENDING / PAID / OVERDUE / CANCELLED |
| `notes` | VARCHAR(500) | | |
| `branch_id` | BIGINT | FK → `branches` | |
| `user_id` | BIGINT | FK → `users` | |
| `created_at` | DATETIME | | |

**Relationships:** `branch` → `branches(id)`, `user` → `users(id)`

---

### 20. `production_orders`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `order_number` | VARCHAR(100) | **UNIQUE** | |
| `product_id` | BIGINT | FK → `products` | Produto acabado |
| `quantity` | DOUBLE | DEFAULT 0 | Qtd. a produzir |
| `unit` | VARCHAR(50) | | |
| `state` | VARCHAR(20) | DEFAULT 'PENDING' | PENDING / IN_PROGRESS / COMPLETED / CANCELLED |
| `created_at` | DATETIME | | |
| `completed_at` | DATETIME | | |
| `notes` | VARCHAR(500) | | |
| `created_by` | BIGINT | FK → `users` | |

**Relationships:** `product` → `products(id)`, `createdBy` → `users(id)`

---

### 21. `audit_logs`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `username` | VARCHAR(255) | | ⚠ String, não FK para `users` |
| `action` | VARCHAR(255) | | e.g. LOGIN, CREATE_SALE, UPDATE_PRODUCT |
| `details` | VARCHAR(1024) | | JSON ou texto livre |
| `created_at` | DATETIME | | |

**⚠ Issue:** `username` is a plain string column — if a user is deleted, logs keep the name as orphan string. No FK enforcement.

---

### 22. `filter_presets`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `user_id` | BIGINT | | ⚠ Long, não FK — filtro é do sistema, não de utilizador |
| `type` | VARCHAR(50) | | PRODUCT / CUSTOMER / SALE |
| `name` | VARCHAR(255) | | Nome do filtro guardado |
| `data` | TEXT (Lob) | | JSON serializado com critérios |
| `created_at` | DATETIME | | |

---

### 23. `transfers` ⭐ NEW

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `document_number` | BIGINT | | Número auto-incremental por filial/série |
| `document_year` | INTEGER | | |
| `series` | VARCHAR(50) | DEFAULT 'TR' | |
| `status` | VARCHAR(50) | DEFAULT 'PENDING' | PENDING / APPROVED / REJECTED / COMPLETED / CANCELLED |
| `source_branch_id` | BIGINT | FK → `branches` | Filial de origem |
| `destination_branch_id` | BIGINT | FK → `branches` | Filial de destino |
| `requested_by` | BIGINT | FK → `users` | Quem pediu |
| `processed_by` | BIGINT | FK → `users` | Quem aprovou/completou |
| `notes` | VARCHAR(500) | | |
| `cancel_reason` | VARCHAR(255) | | |
| `created_at` | DATETIME | | |
| `processed_at` | DATETIME | | |

**Relationships:** `sourceBranch` → `branches(id)`, `destinationBranch` → `branches(id)`, `requestedBy` → `users(id)`, `processedBy` → `users(id)`, `items` → `transfer_items` (1:N cascade ALL)

**Workflow:** PENDING → APPROVED → COMPLETED (or PENDING → REJECTED/CANCELLED)

---

### 24. `transfer_items` ⭐ NEW

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | |
| `transfer_id` | BIGINT | FK → `transfers`, NOT NULL | CASCADE ALL |
| `product_id` | BIGINT | FK → `products` | |
| `quantity` | DOUBLE | | Qtd. a transferir |
| `quantity_received` | DOUBLE | DEFAULT 0 | Qtd. realmente recebida |

**Relationships:** `transfer` → `transfers(id)` CASCADE ALL, `product` → `products(id)`

---

### 25. `fiscal_document_types` (Enum — not a table)

```
FACTURA       → FT  (Fatura)
FACTURA_RECEBIDA → FR  (Fatura Recebida)
TALAO         → TD  (Talão / Nota de Entrega)  ⚠ MoH may use TV
RECIBO        → RC  (Recibo)
GUIA          → CS  (Guia de Circulação/Secretaria)
NOTA_CREDITO  → NC  (Nota de Crédito)
NOTA_DEBITO   → ND  (Nota de Débito)
PROFORMA      → PF  (Proforma)
```

**⚠ Tax API mapping:** `TV` is used in code for Talão — verify this matches MoH API `documentTypeCode`.

---

## Relationships Diagram

```
branches
  ├── users (1:N)
  ├── sales (1:N)
  ├── purchases (1:N)
  ├── stock_movements (1:N)
  ├── expenses (1:N)
  ├── cash_sessions (1:N)
  └── transfers ──────► branches (source: 1:N, dest: 1:N)

users
  ├── user_roles ────► roles (N:M)
  ├── cash_sessions (1:N)
  ├── cash_movements (1:N)
  ├── expenses (1:N)
  ├── production_orders (1:N)
  ├── stock_movements (1:N)
  └── transfers ──────► users (requestedBy, processedBy)

roles ◄──── user_roles (N:M)

categories
  └── products (1:N)

metric_units
  └── products (1:N)

products
  ├── product_barcodes (1:N)
  ├── sale_items (1:N)
  ├── purchase_items (1:N)
  ├── stock_movements (1:N)
  ├── stock_branch (1:N)
  ├── production_orders (1:N)
  └── transfer_items (1:N)

suppliers
  └── purchases (1:N)

customers
  └── sales (1:N)

sales
  ├── sale_items (1:N) ──────► products
  ├── payments (1:N)
  └── originSale ──────► sales (self-ref, for cancellations)

purchases
  └── purchase_items (1:N) ──► products

cash_sessions
  └── cash_movements (1:N) ──► users

transfers
  └── transfer_items (1:N) ───► products
```

---

## Critical Design Notes

### 1. Stock is duplicated across `products.stock` and `stock_branch`
- `products.stock` = global sum (maintained manually or via triggers)
- `stock_branch` = per-filial (correct source of truth)
- **Fix needed:** Use `stock_branch` as the single source; drop `products.stock` or keep as cache

### 2. `audit_logs.username` is a string, not a FK
- Orphan strings if user deleted — consider FK or soft-delete users instead

### 3. `filter_presets.userId` is a Long, not a FK
- Possible inconsistency with actual `users.id`

### 4. `products.stock` is Double — consider BigDecimal for financial data
- Floating point errors possible on large quantities

### 5. `StockMovement` has no custom queries
- Cannot efficiently query movements by date/branch/type via repository
- **Recommendation:** Add `findByTypeAndCreatedAtBetween`, `findByBranchAndType`

### 6. `Payment` has no custom repository queries
- No efficient way to query payments by date range or method

### 7. `Transfer` workflow requires stock validation on complete()
- If source branch stock is insufficient, throws `IllegalStateException` and rolls back

### 8. `Sale.state` is fiscal state only
- Payment status is tracked via `payments` table (or `Sale.paymentMethod` + `Sale.state`)
- No explicit PAID / PENDING payment state on Sale itself

---

## Repository Custom Queries (full inventory)

| Repository | Custom Methods |
|------------|----------------|
| `ProductRepository` | `findByCode`, `searchByCodeOrName`, `searchByCodeOrNameAndCategory` ⭐ FIXED |
| `SaleRepository` | `findByIdWithItems`, `findMaxDocumentNumber…`, `findAllByYearAndMonth`, `findAllByPendingSync`, `searchByCustomerName`, `findByState`, `findByDateRangeAndState`, `findByCustomerAndDateRangeAndState`, `findAllByCustomerId`, `findPendingByCustomerId`, `countByDateRange` ⭐ NEW, `sumTotalByDateRange` ⭐ NEW |
| `CustomerRepository` | `findByType`, `findByCode`, `findByNuit` |
| `SupplierRepository` | `findByName` |
| `BranchRepository` | (none — only inherited) |
| `UserRepository` | `findByUsername`, `findByEmail`, `findByBranchId` |
| `PurchaseRepository` | `findByState`, `findBySupplierId`, `findByDateRange` |
| `PaymentRepository` | (none) |
| `StockMovementRepository` | (none) |
| `StockBranchRepository` | `findByProductIdAndBranchId`, `findByProductAndBranch` ⭐ NEW, `findByStockCurrentLessThanEqual` ⭐ NEW, `findByBranchId` ⭐ NEW |
| `SaleItemRepository` | `findBySaleDateRange` ⭐ NEW (filtra items por data da venda) |
| `CashSessionRepository` | `findByUserAndState`, `findByBranchAndState`, `findByOpenedAtBetween` |
| `ExpenseRepository` | `findByState`, `findByCategory`, `findByDateRange`, `findByBranchId` |
| `AuditLogRepository` | (none) |
| `TransferRepository` ⭐ | `findByIdWithItems`, `findByStatus`, `findByDateRange`, `findByDateRangeAndStatus`, `findByStatusAndBranch`, `findMaxDocumentNumber…` |
| `TransferItemRepository` ⭐ | (inherited only) |

---

## Column Naming Conventions

| Java field | DB column |
|---|---|
| `isHead` | `is_head` |
| `nuit` | `nuit` |
| `userId` (in join) | `user_id` |
| `createdAt` | `created_at` |
| `offlineFlag` | `offline_flag` |
| `pendingSync` | `pending_sync` |
| `defaultDiscount` | `default_discount` |
| `forceChangePassword` | `force_change_password` |
| `canViewStats` | `can_view_stats` |
| `commissionPercent` | `commission_percent` |
| `stockCurrent` | `stock_current` |
| `stockMin` | `stock_min` |
| `stockMax` | `stock_max` |
| `initialValue` | `initial_value` |
| `reportedValue` | `reported_value` |
| `systemValue` | `system_value` |
| `documentNumber` | `document_number` |
| `documentYear` | `document_year` |
| `fidelityPoints` | `fidelity_points` |
| `creditLimit` | `credit_limit` |
| `orderNumber` | `order_number` |
| `iceAmount` | `ice_amount` |
| `taxRate` | `tax_rate` |
| `unitPrice` | `unit_price` |
| `discountPercent` | `discount_percent` |
| `quantityReceived` | `quantity_received` |
| `purchasePrice` | `purchase_price` |
| `salePrice` | `sale_price` |
| `purchase_price` | `purchase_price` |
| `sale_price` | `sale_price` |
| `sourceBranch` | `source_branch_id` |
| `destinationBranch` | `destination_branch_id` |
| `requestedBy` | `requested_by` |
| `processedBy` | `processed_by` |
| `cancelReason` | `cancel_reason` |
| `reprintCount` | `reprint_count` |
| `pendingSync` | `pending_sync` |
| `offlineFlag` | `offline_flag` |
| `paymentMethod` | `payment_method` |
| `signatureHash` | `signature_hash` |
| `annulReason` | `annul_reason` |
| `annulDate` | `annul_date` |
| `originSale` | `origin_sale_id` |
| `originSale` | `origin_sale_id` |
