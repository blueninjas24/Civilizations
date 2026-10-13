# Real village supply customers

October12 emergency-reserve addition: full Windows Wrapper clean verify passed
1,264 tests (1,262 unit/two packaged integration), zero failures/errors/skips.
Guarded isolated Paper ordinary-owner timer check passed healthy reserve buying,
real cash/goods conservation, deliveries and absence/disabled-restart at21:32:09.
Main save audited, exact settings restored and helper archived; human server untouched.
No new migration. Automatic fishery founding is still unfinished.

The village is now a real customer of local staffed businesses. Its treasury
pays the supplier for goods the supplier already owns. Payment, seller stock
removal and saved village-owned cargo are one SQL-owner transaction. This is
not payroll, a personal bank account, a new tax, or an unlimited public subsidy.

## What creates a customer

Food orders cover current unmet food plus an approved emergency stock reserve,
with a complete population assessment, present matching world and no historical
food backlog. Target = current unmet food + assessed demand × configured future
reserve periods. Stored public food, incoming paid public cargo, donated deliveries,
reserved player food purchases and barter count first. This is a legitimate limited
customer before hunger, not invented consumption or a guarantee every firm sells.
An unrepresentable target declines new spending; no residents means no reserve.

The new `food-stock-reserve-periods` default is2 in bundled economy.yml, range0–100.
0 retains shortage-only purchases; absent keys in installed files also mean0.
It is distinct from `food-reserve-periods`, which protects cash from MATERIAL
purchases. Food spending retains the existing minimum treasury, per-period budget,
prices and haul cap. Grants still refuse an actual shortage and protect food cash.

Training orders require actual active courses whose latest practice review was
productive or paused specifically for missing supplies. Completed/cancelled
courses, disabled skills, opportunity/food/labor pauses, a departed/deceased
learner or a learner now employed/owning a business do not justify purchases.
Orders cover bounded upcoming practice plus the editable study material reserve;
public stocks and owned incoming goods count first. This does not invent new
students or bypass the existing founder/opportunity enrollment requirements.

`VillageSupplyPurchases.demand` provides read-only, current funded customer
evidence for later resource investment. It counts food first, then shares actual
remaining cash, the material budget and haul capacity across material needs.
It records the settlement as the customer; it does not invent a shop or resident.
It does not reserve money or establish a new producer by itself.

## Editable prototype

`economy.yml` has an optional `village-supply-purchases` section. Money values
are hundredths of an emerald; prices buy abstract commodity units, not individual
Minecraft block items. Initial prototype settings:

- Food spending cap: 2,000 per completed food period.
- Shared material spending cap: 1,000 per completed food period.
- Minimum retained treasury: 2,500.
- Emergency food stock target: two future periods of assessed household demand.
- Material spending also retains two periods of assessed food cost, valued at
  the configured food purchase price; the larger reserve wins.
- Food/stone/timber unit prices: 200/100/150.
- Delivery delay: one future food period; shared public cargo cap: 32 units per period.

Old installed configuration files without this section opt out. They are not
rewritten. Accepted orders retain their original price, due period and policy.
Disabling new orders does not erase already-owned cargo.

## Runtime, persistence and safety

Normal food scheduling reviews bounded local suppliers after the final current
food period, with food prioritized. Sellers must be local, operating, marker
permitted and actually staffed by qualified living workers. Supplier rotation is
deterministic; the current review examines at most 16 saved suppliers. This
bounded prototype is not an auction or supplier profitability comparison.

Schema65 adds an initially empty `village_supply_orders` audit/cargo table and a
pending-delivery index. Treasury entries, businesses and original/delivered food
assessments are referenced. It creates no cash, goods, firms or courses. Migration
and purchase failures roll back; operation identities replay exact accepted terms
and reject reuse with different participants/quantities or unrelated payments.

At arrival the normal economy transaction imports actual cargo before household
food use or practice consumption. Failed consumption/delivery rolls back all
period effects and keeps cargo pending. The existing runtime separately retains
earned clock time so the period can retry. Absent worlds and server downtime
pause the clock; unloaded chunks do not cause block access or force loading.
Food-response planning counts this incoming food so it does not ask players for
the same shortage again. Village diagnostics show retained incoming order totals.

Temporary SQLite tests cover actual shortages/practice, automatic scheduling,
owned cargo, money/stock transfer, shared budgets, protected cash, read-only demand,
replay/restart, absence, historical backlog, cancelled learners, late failures and
schema64-to65 rollback/retry. Windows .\mvnw.cmd clean verify passed 1,181 tests,
zero failures/errors/skips. Guarded native Paper normal-session timers verified
paid practice materials, conserved cash, owned arrival/consumption, absence pause
and restart with new orders disabled; normal shutdown and exact save audit passed.
Full build results and limitations are recorded in PROJECT_STATUS.

This customer path does not yet recognize or found fisheries, forests or quarries.
It gives those future producers a legitimate source of demand. No new Paper world
adapter or Phase4 construction has been added; the human server is unchanged.
