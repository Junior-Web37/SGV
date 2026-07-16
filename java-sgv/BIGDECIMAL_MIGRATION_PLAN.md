# BigDecimal Migration Plan for java-sgv

## Goal
Migrate monetary and quantity calculations from `Double` to `BigDecimal` incrementally, preserving existing business rules and avoiding regressions.

## Scope
- Entities: `Sale`, `SaleItem`, `StockBranch`, `Product`, and any other domain objects holding prices, totals, quantities, tax, or stock values.
- Services and controllers that compute totals, line amounts, or stock adjustments.
- Persistence mapping and JSON/form binding if applicable.
- Tests covering sale processing, stock decrement, and document generation.

## Step-by-step plan

1. Identify current numeric fields
   - Search for `Double`, `double`, `qty`, `unitPrice`, `lineBase`, `lineIce`, `lineTax`, `lineTotal`, `subtotal`, `totalIce`, `totalTax`, `total`, `stockCurrent`, `stockMinimum`, `stockMaximum`, etc.

2. Replace entity fields in small groups
   - Prefer `BigDecimal` for currency values and precise stock quantities.
   - Keep `Integer`/`Long` for counts and IDs.
   - Use `@Column(precision=15, scale=2)` for money fields if JPA mapping is needed.

3. Adjust constructors and setters/getters
   - Use `BigDecimal.valueOf(double)` only when converting from legacy doubles.
   - Prefer `BigDecimal.ZERO` as default.
   - Avoid `double` arithmetic in business logic.

4. Refactor arithmetic in services
   - Replace `+`, `-`, `*`, `/` with `BigDecimal.add()`, `subtract()`, `multiply()`, `divide()`.
   - Use `MathContext` or explicit scale/rounding only where business rules require rounding.
   - Keep stock decrement as `stockCurrent.subtract(qty)`.

5. Update UI and binding code as needed
   - Convert text field inputs to `BigDecimal` safely.
   - Normalize decimal separators if necessary.
   - Ensure `TableView` cell values display `BigDecimal` properly.

6. Expand tests
   - Add unit tests that assert exact totals and stock balances with `BigDecimal` values.
   - Keep existing focused tests, then add migration-specific coverage.

7. Validate persistence and legacy data
   - If the schema stores numeric values in SQLite or DB, verify no precision loss.
   - Run end-to-end scenarios for sale save and stock adjustment.

## Recommended first targets
- `SaleItem.lineBase`, `lineIce`, `lineTax`, `lineTotal`
- `Sale.subtotal`, `totalIce`, `totalTax`, `total`
- `StockBranch.stockCurrent`
- `Product.price` / `Product.cost` if present

## Notes
- Start with non-production code paths first, such as `SaleService` tests.
- Keep separate commits for entity mapping changes versus business logic changes.
- Avoid broad migration in one pass; validate each group with tests.
