# Paid business resale and family estates

This is a transaction backend and internal SQL-owner application boundary. It does
not yet advertise businesses, choose buyers, value a sale automatically or close an
unsold firm. Qualified family/employee inheritance remains ahead of any sale.

## Rules and accounting

A new sale needs the current deceased owner, an eligible living local buyer with
the saved founder qualifications, and no existing job in a different industry.
An available qualified heir/employee prevents paid resale. Shared housing is not
kinship. Recorded partners, parents/children and shared-parent siblings qualify to
use the corresponding estate; eligibility is checked again at purchase.

The buyer may use one actually owned local business cash account and one existing
family estate, or either alone. The business being purchased cannot finance its
own purchase. A seller's estate starts at zero and receives only actual paid sale
proceeds. There is no estate opening grant, personal wallet, wage, village subsidy
or stock-to-cash conversion. Proceeds remain keyed to the deceased resident's UUID,
so moving households does not transfer family money to unrelated occupants.

One transaction checks source balances/limits, records immutable accepted terms,
debits real buyer funds, credits the seller's estate, records before/after balances
and changes ownership. All payments sum to the estate credit. Failure restores
everything; exact replay returns the saved sale even after retuning/disabling.
Changed replay terms and stale sellers are refused. The business keeps its founder,
site, cash, stock, deposits, jobs and cargo. Sale proceeds are separate assets.

Schema 39 additively creates family_estates, business_sales and business_sale_entries
with indexed histories. Existing inheritance records and assets remain unchanged.
Upgrade failure rolls back the entire pending migration, with retry coverage.

PopulationSession.sellBusiness queues work on the SQL owner. New acquisitions require
the business world to be present and the current completed calendar step. Historical
accepted replay remains valid while absent; it does not move funds again. There is
no player/admin sale command yet, and no Paper world operation is performed here.

## Editable prototype funding limits

In plugins/Civilizations/economy.yml:

```yaml
business-resale-funding:
  minimum-retained-business-cash-hundredths: 1000
  maximum-spare-business-percent: 25
  maximum-estate-percent: 100
```

1000 hundredths means 10.00 abstract Trade Value retained for operations. A business
holding 60.00 may contribute up to 25% of the 50.00 above that floor: 12.50 per
purchase. Estate funds may contribute their full balance by default. Percentages
are 0..100, round down to hundredths, and never overflow intermediate multiplication.
These are per-purchase balance limits, not lifetime or monthly allowances. They do
not reserve additional working capital for speculative future costs. Future buyer
selection must also assess actual opportunity/operating needs before spending.

Set both percentages to zero to refuse new acquisitions. Missing old sections keep
new purchases disabled; no owner file is overwritten. Malformed/unknown keys fail
configuration validation before the database starts. Edit while stopped/restart.

## Remaining work

Automatic listings, editable valuations, absence-safe sale deadlines, buyer review,
unsold closure, competition, and estate-backed new-business investment remain
unfinished. The ledger can already fund a qualified family's later acquisition of
an existing business. Suitable vacant building evaluation/reuse stays Phase 4.
This backend alone does not complete another Phase 3 reporting group.

Verification October 9: 81 focused tests passed; Windows Wrapper clean verify
passed 715 tests (713 unit/two packaged integration), zero failures/errors/skips.
Seventeen new cases cover actual prior-sale estate funding, mixed sources, eligibility,
limits, conservation, owner queue/world/date gates, restart/replay, failure/retry,
configuration and schema 38->39 rollback/recovery. Real Paper PopulationSession
fixture passed inheritance followed by a separate paid acquisition, replay after
restart, world pause and finite production. Working cash 750 plus estate 250 retained
the original 1000; treasury stayed 8500 and coal 3/reserve 7. No world writes/tickets
or user-server changes. Main disposable dev assets retained; integrity/FKs healthy.
Probe archived, original network settings restored after normal shutdown.
