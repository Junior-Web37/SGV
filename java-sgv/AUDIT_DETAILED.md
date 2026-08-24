# AUDIT DETAILED — SGV

## Test summary
- Total tests executed: 60
- Failures: 0
- Errors: 0
- Skipped: 0
- Full results: `target/surefire-reports` (XML and plain text reports)

Key passing tests:
- `SaleServiceAnnulmentTest` — verifies annulation restores stock and customer balance and prevents updates/deletes on annulled sale.
- Several service and entity unit tests validating BigDecimal math, persistence callbacks, and stock flows.

## Migrations applied during verification
- `V15__filter_presets_data_longtext.sql` — converted `filter_presets.data` to `LONGTEXT` to match `@Lob` mapping.
- `V16__enforce_default_notnull.sql` — applied safe NOT NULL/defaults to columns with entity defaults (see file for details).

## Important logs
- Application startup with `--spring.profiles.active=mysql` shows:
  - Flyway: migrated to v16
  - Triggers applied by `DbTriggerConfig`
  - Desktop login success (admin)

Relevant files:
- `AUDIT.md` — short summary and reproduction steps.
- `target/surefire-reports/` — test reports.
- `src/main/resources/db/migration/` — migration SQLs.

## Next recommended hardening
1. Review other `@Lob` mappings and long text columns across schema.
2. Replace `Double` primitive usage on monetary/stock-critical fields with `BigDecimal` where precision matters.
3. Add DB-level constraints and checks for invariants (non-negative stock, non-negative balances).
4. Add integration tests against a MariaDB test container to increase production fidelity.

