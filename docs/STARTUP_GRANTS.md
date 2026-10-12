# Limited village startup grants

User approval, October 10: qualified first mines/workshops may receive limited
configurable startup funding from actual village treasury after food protection.
October 11 approval includes warehouses under the same limits and protection.
October 12 approval adds logging, fishing, quarry and sandpit under those same caps.
This is public investment in a privately owned business, not a personal wallet,
salary, loan or automatic government mechanic.

`BusinessStartupGrants` composes checked grant approval, existing startup debits,
business/site registration, store creation and the first qualified worker in one
transaction. Mine startup also establishes its finite coordinate-backed deposit.
`startWarehouse` requires an explicit warehouse request, registers its qualified
first worker and opens empty stock with real purchasing capital. It does not
invent inventory, a production recipe, a building or a separate funding pool.
One grant receipt per business is saved in schema 49. Failed funding, qualification,
ore validation, storage, employment or receipt insertion rolls back the whole
operation. Startup expense and working capital both count toward the grant; working
capital moves into business cash, startup expense remains a recorded expense.

Accepted replay uses the original receipt and existing immutable startup terms.
It does not repeat payment, recreate inventory, refill ore or consume quota again.
Retuning/disabling new grants does not alter historic accepted terms. Changed
founding/store/first-worker inputs remain rejected by the underlying startup seams.
Existing businesses cannot obtain retroactive grants. Migration adds an empty table
and index, with no fabricated payments, backfill or reinterpretation of old farms.

New requests need the current village food/economic period's complete saved needs
assessment. Unmet food demand, an absent/stale assessment or a different living
population count makes startup wait. The treasury must be known. Protected cash is:

`max(minimum treasury, assessed food demand × reserve periods × estimated cost per food unit)`

This planning estimate is separate from merchant prices and is not a guarantee of
future supply or famine prevention. Both startup expense and working capital must
fit above that floor. Arithmetic overflow fails safely. No commodity is consumed by
the cash guard itself; ordinary food accounting still records actual consumption.

Admin settings live in `plugins/Civilizations/economy.yml`, under `startup-grants`.
Edit while stopped and restart. Unknown keys, fractional/negative limits, invalid
food protection and duplicate/nontext industry lists fail before database startup.
Missing legacy sections disable new grants. Bundled defaults are experimental:

| Setting | Default | Meaning |
|---|---:|---|
| enabled | true | Allows checked requests; does not create requests automatically |
| maximum-grant-hundredths | 5000 | 50 Trade Value per startup, including business cash |
| per-period-limit-hundredths | 5000 | 50 Trade Value shared across a village's current food period |
| lifetime-limit-hundredths | 20000 | 200 Trade Value total public startup spending per village |
| minimum-treasury-hundredths | 2500 | Keep at least 25 Trade Value for village spending |
| food-reserve-periods | 2 | Food demand periods used to calculate the protected cash buffer |
| hundredths-per-food-unit | 200 | Conservative estimate of 2 Trade Value per abstract food unit |
| industries | mining, blacksmith, warehouse, logging, fishing, quarry, sandpit | Industry names eligible for this funding source |

100 hundredths equal 1 Trade Value. Periods are the village's saved food/economic
period, not calendar years or wall-clock days. Lifetime spending survives closure,
owner changes, restart and retuning; raising a limit permits only future requests.
These caps restrict public spending, not unlimited approved private ownership.
The backend does not yet fund startup from private business/estate cash.

Existing industry lists are never rewritten: owners adding the new industry keys
opt them in while stopped. Maximum, period, lifetime and food-protection defaults are unchanged.
Warehouse and mine/workshop receipts count in the same village sums, including
closed businesses. Replay retains actual stock, funds and original worker episode;
changed storage/capital terms fail. This extension reuses schema 49 receipts and
introduced no additional migration in that extension; the current database also
contains later independently versioned features.

Automatic mines for current observed ore and real existing customers are implemented
(see AUTOMATIC_MINES.md); first warehouse/workshop founding and gradual first-founder
training are now verified. Wider producer resource/startup adapters remain
unfinished. This trusted backend requires its caller to establish actual demand,
current suitable evidence, qualification requirements and safe site/storage terms.
It has no public grant command, scheduler, block edits or chunk loading. It does not
certify synthetic/old ore evidence or bypass provenance guards. Existing explicit
startup APIs remain trusted test/composition seams; future automatic founding must
use the guarded funding path. Old farm terms/operation remain unchanged.

## Read-only startup affordability

`preview` checks the same current food assessment, living population, treasury,
shared period/lifetime grant spending and editable limits used by acceptance.
It requires no hypothetical business ID or site and creates no receipt, account,
stock, job or reservation. Repeated previews do not consume a grant allowance.
Missing food evidence and ordinary policy refusals become explicit blocked reasons;
actual SQL errors still propagate and restore the owner transaction state.

An eligible preview is only current affordability. Demand, site, qualification and
resource trust must still pass; actual acceptance rechecks money and saves all
assets/payments atomically. Automatic mines now check funding before looking for
a founder, so a food-protected village reports its real financial obstacle instead
of suggesting that skill training alone would make the operation possible.
Historic accepted receipts continue to replay original terms after grant retuning.
No migration, cash creation, new public command or changed grant defaults.

## Earlier affordability verification

Six tests added; focused34 and full Wrapper1,009 passed with no failures/errors/skips.
Updated native Paper mine probe passed affordability, protected-cash refusal, real
startup, finite production, world pause and restart. Current schema58 unchanged;
see PROJECT_STATUS.md for the complete isolation/restoration audit.

## October12 broader producer eligibility

All seven industries use the same persisted village sums, including previous mine,
workshop and warehouse receipts. No migration or old grant rewrite: schema63 stays
unchanged. Temporary SQLite tests exercise each new industry against an existing
mine's period/lifetime spending, food shortage, empty paid capital, late receipt
failure rollback and restart/disabled-policy replay. These are trusted composition
tests with explicit fixture sites/owners; they do not certify natural terrain or
claim automatic forestry/fishing/quarry/sandpit gameplay is implemented.

Verification: 105 focused cases passed. Windows `.\mvnw.cmd clean verify` passed
1,148 tests (1,146 unit +2 packaged integration), zero failures/errors/skips,
6:09, October12 at14:13:03. Added final assertions prove new producer receipts
also reduce later mine budgets; all29 grant tests passed after recompilation
(14.524s). Application code unchanged after full build. No Paper-facing changes,
so no additional native run; the human server and databases remain untouched.
