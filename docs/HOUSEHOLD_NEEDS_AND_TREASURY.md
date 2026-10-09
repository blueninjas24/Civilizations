# Household needs and settlement treasury

This Phase 2 batch records observable needs and establishes explicit budget accounting.
It now includes simulated household food allocation; full prosperity and the economy milestone remain incomplete.

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
supplies. New periods also record each occupied family's simulated share of that
fulfilled food and its consecutive undersupplied periods. This does not prove
physical meals or place items in villager inventories. Food and housing are evidence for future wellbeing/decision rules,
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

## Household food allocation — October 8 prototype

Schema 19 adds nullable allocation fields to needs history. Existing periods retain
unknown allocation; the upgrade never invents historical meals or shortage streaks.
New periods distribute only the food actually fulfilled by commodity accounting.
An older plugin will refuse schema 19; reverting requires a consistent pre-upgrade
backup, not manually lowering the database version.
Each occupied household's configured adult/child demand determines its proportional
share. People without a household share a separate pool using the same demand rules.
Whole units round down, then the largest fractional remainders receive leftovers.
Exact ties use household UUID order, followed by the unassigned pool. This is a
repeatable prototype bookkeeping rule, not a final social rationing policy; persistent
ties can favor the same household. No child-first, wealth or leadership privileges
are introduced. Adult/child food needs remain editable in economy.yml.

Allocated food sums exactly to village fulfilled food; no bucket receives more than
its demand. Zero demand and zero supply are valid. Arithmetic uses exact integers,
including overflow-safe products. A family's unmet-period counter increments only
when its simulated share falls short, survives restart, and clears when supplied.
An extinct/disappearing household loses its consecutive streak when later reoccupied;
its earlier historical entries remain. Unknown pre-upgrade counters begin with the
first observed deficit, without inferring old shortages. Diagnostics report SUPPLIED,
UNDERSUPPLIED or historical UNKNOWN alongside independently observed housing.

The allocation, streaks, village stock changes, aid receipt use and planning choice
share one SQL transaction. No new chunks are loaded and no blocks/entities/inventories
are changed. No mortality, fertility, migration, payment or numerical prosperity
modifier consumes this allocation yet. Those effects and a different configurable
social rationing strategy remain future work.

Validation covers proportional conservation, whole-unit ties, stable household order,
zero demand, long-integer products, 1,200-period repeatability, crisis/relief streaks,
SQLite restart, historical schema-18 migration and transactional migration failure.
The disposable Paper probe checks family supply, famine history across restart and
recovery from an actual confirmed food donation. Scale profiling and human survival
playtesting remain pending.
