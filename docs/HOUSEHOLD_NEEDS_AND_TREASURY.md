# Household needs and settlement treasury

This Phase 2 batch records observable needs and establishes explicit budget accounting.
It does not complete prosperity, household allocation or the full economy milestone.

## Household evidence

Each completed food period saves a settlement needs assessment and a snapshot for
each occupied household. Living adults and children contribute the configured food
demand; deceased residents and other settlements do not. Living people with no
household remain part of village consumption and are reported separately.

Housing compares living occupants with the existing observed household capacity.
Equal occupancy is sufficient under the existing prototype; exceeding capacity is
crowded. Missing capacity remains unknown, distinct from a measured zero. This does
not change the approved prototype housing formula, households, beds or migration.

Food condition, fulfilled demand and shortage duration describe shared village
supplies. They do not prove what any family actually ate: household rationing remains
open. Family food demand and housing are evidence for future wellbeing/decision rules,
not a personal bank balance or complete prosperity score. Employment, safety and
material needs are not fabricated to complete such a score. No demographic penalty
or income is derived from these reports yet.

Needs, food stocks, retained inputs and period checkpoint commit together. A failed
needs save rolls back the food effects; the saved elapsed target remains retryable.
Historical housing/demand remain as originally observed after edits, moves or restart.
Schema 15 periods are preserved without inventing historic household observations;
their needs report is absent until a new period is processed.

`/civ debug economy <settlement UUID>` reads immutable cached period evidence and
shows up to twenty household entries. These are the last completed period's facts,
which may precede subsequent population or observation changes. No command performs
JDBC, player transactions or database mutation on the server thread.

## Treasury accounting foundation

Treasury is liquid settlement spending capacity, independent of shared food,
household needs and eventual prosperity. An absent account means uninitialized,
not a presumed zero budget. The live plugin never creates or fills a treasury by
guessing taxes, salaries, starting money or market revenue.

The SQL-owner `TreasuryRepository` exposes explicit account opening and credits/
debits with an operation UUID and source/reason. Values are exact integer hundredths
of abstract Trade Value, consistent with the existing price precision. They are not
emerald counts. There are no personal household wallets.

Account and ledger movement commit together. A repeated operation with identical
inputs returns the original receipt without changing the current balance; that
receipt's balance is its original post-payment balance, not today's balance.
Reusing the ID with changed inputs, repeated seeding, insufficient funds, unknown
settlements, invalid amounts and overflow fail explicitly. Debt is not supported.
The ordered ledger retains reasons and balances. A caller must run on the database
owner thread with no active transaction; there is no new public mutation command.

No donations, taxes, trade receipts, subsidies, project funding or automatic income
are wired. Their actual sources, authorization and game rules must be implemented
before live accounts are initialized. This is tested accounting machinery, not
a working settlement government or player economy.

## Upgrade and validation

Transactional schema 16 adds `needs_periods`, `household_needs`, `treasuries` and
`treasury_entries`. Existing identities, capacities, food history, reserves and
settings survive. No reset or invented backfill. Tests use temporary SQLite files.

Verification includes historical housing changes, restart, write-failure rollback/
retry, treasury idempotency and insufficient funds, migration failure/recovery,
and a repeatable 1,200-period food shortage/recovery assessment. The existing real
Paper food probe also checks saved household needs, cached diagnostics, preserved
crisis evidence after restart/retuning, and no invented treasury.

Large-server profiling and human survival playtesting remain outstanding. The
existing approximate food catch-up uses current saved people and last reliable farm
capacity; this batch does not reconstruct historical demographic/economy interleaving.
