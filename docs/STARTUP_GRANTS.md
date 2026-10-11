# Limited village startup grants

User approval, October 10: qualified first mines/workshops may receive limited
configurable startup funding from actual village treasury after food protection.
This is public investment in a privately owned business, not a personal wallet,
salary, loan or automatic government mechanic.

`BusinessStartupGrants` composes checked grant approval, existing startup debits,
business/site registration, store creation and the first qualified worker in one
transaction. Mine startup also establishes its finite coordinate-backed deposit.
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
| industries | mining, blacksmith | Industry names eligible for this funding source |

100 hundredths equal 1 Trade Value. Periods are the village's saved food/economic
period, not calendar years or wall-clock days. Lifetime spending survives closure,
owner changes, restart and retuning; raising a limit permits only future requests.
These caps restrict public spending, not unlimited approved private ownership.
The backend does not yet fund startup from private business/estate cash.

Automatic opportunity selection, career choice and mine/workshop founding remain
unfinished. This trusted backend requires its caller to establish actual demand,
current suitable evidence, qualification requirements and safe site/storage terms.
It has no public grant command, scheduler, block edits or chunk loading. It does not
certify synthetic/old ore evidence or bypass provenance guards. Existing explicit
startup APIs remain trusted test/composition seams; future automatic founding must
use the guarded funding path. Old farm terms/operation remain unchanged.
