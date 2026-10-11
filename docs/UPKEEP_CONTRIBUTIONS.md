# Business contributions to public farm upkeep

Approved October 11, 2026. This reuses real cash and never creates money.

If a verified public farm toolkit invoice cannot be paid after food protection,
its actual local blacksmith/warehouse supplier can contribute the exact shortage.
Village funds sufficient for the invoice means no collection. There is no separate
tax timer, blanket collection or household/estate payment.

Current worker-backed food, complete fields, qualified farm staff, present permanent
world, current population food assessment and fulfilled food needs remain required.
Contributions cannot override disabled spending or the public period budget. Actual
supplier operation, stock, destination space and hauling still determine purchase.

## Editable settings

Edit economy.yml while stopped and restart. Bundled new-server defaults:

```yaml
upkeep-contributions:
  enabled: true
  operating-reserve-hundredths: 8000
  spare-cash-percent: 50
  per-business-period-limit-hundredths: 4000
  per-village-period-limit-hundredths: 4000
```

Values are hundredths of Trade Value (8000 means 80), not emerald counts.
Missing section in existing files disables contributions. Unknown keys, invalid
numbers and types are rejected. Retuning is forward-only under the existing food
economy rules; earned backlog cannot be silently retuned.

Supplier retains original village-funded store capital PLUS the operating reserve.
Only the percentage of cash above that floor is available, capped by cumulative
actual recorded sales minus earlier contributions and both saved food-period
limits. Every bound must cover the whole shortfall or the purchase waits; no
partial collection. Currently only the actual supplier contributes, without
searching unrelated businesses.

## Accounting and persistence

One SQL-owner transaction debits supplier cash, credits treasury, pays the actual
invoice, moves real stock to paid pending cargo and saves both receipts. Farm
cash stays zero. Audit stores exact before/after balances and policy, linked by
foreign keys to treasury credit and invoice. Missing goods, failed hauling/space
or any audit fault rolls back all transfers and budget use together.

Accepted invoice replay never contributes/pays again, including after restart,
disabling and absence. New contributions pause with absent worlds. Schema 55 adds
empty upkeep_contributions and budget/supplier-sales indexes; no saved money,
stock, identities or history is rewritten or backfilled.

## Scope and limits

This is conservative cash-surplus protection, not net-profit accounting or a
forecast of every future obligation. Admins using expensive custom recipes should
raise operating reserve accordingly. Poor suppliers, depleted inputs, absent
stock and capped public budgets can still stop work. No free industries, payroll
or guaranteed economic stability. Player food trading remains a public outflow.

Temporary SQLite tests cover conservation, exact shortfalls, reserve/quota limits,
rollback, restart replay, stale food, absent worlds and transactional 54 upgrade.
A test-only UpkeepContributionsProbe exercises actual PopulationSession on marked
disposable Paper with its own synthetic save, no block/entity edits or forced
chunks. See PROJECT_STATUS.md and DEVLOG.md for completed validation results.
