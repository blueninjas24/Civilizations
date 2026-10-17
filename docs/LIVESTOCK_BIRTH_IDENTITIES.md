# Permanent livestock birth identities

Each new simulated birth now receives one permanent logical identity in the same
transaction that saves its real feed/work costs, products and herd checkpoint.
This is a prerequisite for physical newborns, not an animal spawning feature.
Automatic adoption, physical claiming/reconciliation and live feed/output remain
unfinished. The player market continues using actual recorded stock.

## Identity and transaction

`LivestockBirth` uses a fixed version-one UUID namespace with the immutable
business ID, birth period and sibling ordinal. Identical history produces the same
IDs on Windows/Linux/restart. Changing future maturity settings does not reroll an
identity or change an existing saved growing-up date. The namespace must remain
stable. A future Paper entity UUID will be a separate physical mapping.

The existing policy permits at most 1,000 births per herd period. The registry uses
ordinals 0–999, validates the actual production receipt's count and stores the
absolute maturity period. Zero births create no rows and need no future-date
calculation. A failed identity insert rolls back the whole production transition;
it cannot leave paid feed, goods, cohorts or anonymous newly committed births.
Historical operation replay returns its existing receipt without inserting again.
No worker, feed, capacity, breeding rate, money or physical animal is invented.

`LivestockBirths.page` supplies immutable chronological pages of at most 256 rows.
Its cursor includes birth period and ordinal so sibling pages neither skip nor
repeat children. Reads validate canonical identities and maturity bounds.
`unmappedBirths` reports count coverage for an existing herd and rejects excess
registry quantities. This is not a full row or physical-presence certificate;
future consumers must also read validated records and reconcile actual entities.

## Schema 85 and older saves

The additive migration creates an initially empty `livestock_births` table with
business/production foreign keys and unique sibling slots. It changes no prior
row, stock, cash, animal ownership, original roster, cohort or historical receipt.
All pending schema/version changes roll back if the migration fails.

Old anonymous logical births remain anonymous and visible as unmapped coverage.
The upgrade does not guess which cow/chicken represents an old cohort, invent a
physical UUID or silently replace missing registry rows. Old herd headcounts and
products remain intact. Such a herd needs explicit trustworthy reconciliation
before future physical activation. Current live adoption/output is still disabled;
this batch exposes no destructive reset or automatic repair command.

## Validation

Thirteen added cases cover cross-business/period/sibling identity uniqueness,
stable replay, overflow/zero cases, strict bounds, atomic failure/retry, saved
maturity through retuning, restart, stable pages, missing/forged/excess entries,
legacy preservation and schema 84 failure/rollback/retry. Older complete
save-comparison tests also require the new table empty while keeping every prior
row assertion. All tests use temporary SQLite databases.

Focused 102 checks passed, then 77 affected migration/newborn checks passed.
The initial full run found seven comparisons in five old fixture lists missing
only the new empty table. Every predecessor value matched; extended explicit
empty-table checks without removing any old-row assertions. Final Windows
.\mvnw.cmd clean verify -B -ntp passed 1,694 tests (1,692 unit/two integration),
zero failures/errors/skips, 10:43 at 2026-10-16T22:40:40-04:00.

Guarded Paper 26.2 build 129 passed at 22:41:56: stable newborn IDs and maturity
survived actual SQL-owner restart alongside the prior native market/save tests.
Main test-save upgrade 84->85 preserved all 142 predecessor tables (138 exact,
ordinary metadata only, no aging); new registry empty, integrity/FKs clean.
Fixture stopped, original settings restored, helper archived and backups retained.
The helper uses synthetic player/death events and a separate herd database; it
neither materializes offspring nor certifies a human-client death or actual crash.
Human server untouched. Physical claims/losses/reconciliation and live adoption/
feed/output remain next work; no new gameplay rule is introduced by this registry.
