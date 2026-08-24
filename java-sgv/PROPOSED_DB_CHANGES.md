# Proposed DB Changes — Double -> DECIMAL review

This report lists Java entity properties typed as `Double` that may need conversion to `DECIMAL(19,4)` (or appropriate precision) in the DB for monetary/stock-critical accuracy.

Scan summary (found occurrences):

- `Product` (src/main/java/com/sgv/entity/Product.java)
  - `bulkQuantity` (Double) — likely column: `bulk_quantity` (confirm schema)
  - `conversionFactor` (Double) — likely column: `conversion_factor`
  - `profitMargin` (Double) — calculation field; persisted amount is `profitMarginAmount` (BigDecimal)
  - `taxRate`, `iceRate`, `defaultTaxRate`, `defaultIceRate` (Double) — rates (percentages), may remain `DOUBLE` but can be DECIMAL(5,2)
  - `stockMax`, `stockMin` (Double) — consider DECIMAL(19,4) for fractional stock

- `ProductionOrder` (src/main/java/com/sgv/entity/ProductionOrder.java)
  - `quantity` (Double) — consider DECIMAL(19,4)

- `AppConfig` (src/main/java/com/sgv/entity/AppConfig.java)
  - Several `Double` defaults (tax rates) — low risk, optional conversion to DECIMAL(5,2)

- Other areas where doubles used for quantities or percents: `WarehouseService`, `Dashboard`, `MetricUnit` etc. See full grep results in commit/scan.

Suggested action plan:
1. Review each `Double` property and confirm the corresponding DB column name and current type (SHOW COLUMNS FROM <table>).
2. For quantity/stock/monetary fields, convert DB column to `DECIMAL(19,4)`:

Example SQL template:

```sql
ALTER TABLE products MODIFY COLUMN stock_max DECIMAL(19,4) NULL;
ALTER TABLE production_orders MODIFY COLUMN quantity DECIMAL(19,4) NOT NULL DEFAULT 0;
```

3. For percentage rates, consider `DECIMAL(5,2)`:

```sql
ALTER TABLE products MODIFY COLUMN default_tax_rate DECIMAL(5,2) NOT NULL DEFAULT 16.00;
```

4. After applying DB changes, update Java entity fields to `BigDecimal` and adjust getters/setters to use `BigDecimal` consistently, then run tests.

Caveats:
- Changing column types in production must be done carefully with backups and testing on staging.
- Some `Double` fields are transient or used only in UI calculations; converting them is optional.

If you want, I can prepare explicit `ALTER TABLE` statements for the exact columns — grant me permission to run `SHOW COLUMNS` against your DB (provide credentials) or paste `SHOW CREATE TABLE <table>` outputs. Otherwise I will prepare a set of conservative ALTERs as patch files you can review and apply manually.
