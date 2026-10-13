# Processing workshops — startup funding and bakery prospects

## Approved direction

Bakeries and the single general craftsman may use the existing protected village
startup grants. They share the same total period and lifetime budgets with mines,
warehouses and the other approved producers. Adding another industry does not
create a separate allowance or increase the village's money. Required experience,
workplace evidence, actual ingredients and real customers still apply.

Existing smokers mark bakery candidates; crafting tables mark craftsman candidates.
Recognition does not read container contents, claim a player building or grant
permission to change blocks. Construction remains Phase 4.

## Admin settings

Edit `plugins/Civilizations/economy.yml` while the server is stopped, then restart.
The repository's new-file defaults now include:

```yaml
startup-grants:
  enabled: true
  maximum-grant-hundredths: 5000
  per-period-limit-hundredths: 5000
  lifetime-limit-hundredths: 20000
  minimum-treasury-hundredths: 2500
  food-reserve-periods: 2
  hundredths-per-food-unit: 200
  industries: [mining, blacksmith, warehouse, logging, fishing, quarry, sandpit, bakery, craftsman]
```

Amounts are hundredths of abstract Trade Value. The protected treasury is the
larger of the fixed floor and the actual resident food-demand buffer. A current
shortage, missing assessment or population change defers new grants. Accepted
startup costs and business capital spend real treasury money exactly once.

Installed files are never rewritten to add these industries. Admins may retain
their existing list, remove either workshop, or disable grants. Missing legacy
grant sections stay disabled. Accepted receipts, businesses and assets persist;
replaying a previously accepted operation cannot grant more money or restore a
missing workplace.

**This funding setting does not automatically open bakeries or craftsmen.** Their
live founding adapters and default product chains remain unfinished. The new
backend also does not create individual wallets, salaries, ingredients or shoppers.

## Implemented backend

The existing transactional marker-workshop startup can now accept these approved
industries. Founding, real treasury debits, an empty funded store, the first
qualified worker, one paid surplus-material cargo and the fixed marker commit
together. A late marker failure rolls back every table. Actual delivered inputs
can then be processed by an explicitly supplied matching recipe; changed retries
are rejected. The tests use fixture recipes, not newly enabled gameplay defaults.

`BakeryOpportunities` adds a read-only first-firm investment review for a configured
meal recipe. It requires fresh complete smoker evidence, current protected grant
eligibility, one affordable paid starter batch with material reserves, and enough
real village meal/reserve demand to buy at least one batch **after** deducting the
entire proposed startup investment. It does not spend anticipated starter-sale
income or reserve the same cash for both opening and food purchases.

Existing available food from qualified operating local businesses covers the
customer need first. Unstaffed, closed, wrong-world and paused suppliers cannot
sell their stock. More than 64 stocked supplier identities defers review instead
of inventing a shortage from a partial sample. Marker caps are applied to smokers
only. Existing or closed bakery identities are retained; this bootstrap cannot
bypass competition, vacancy, resale or reopening checks. Out-of-range, partial,
stale, conflicting and absent-world evidence cannot establish a candidate.

This review is not a reservation or a scheduler. Future acceptance must repeat it
in the SQL owner's complete founding transaction, choose a qualified founder and
recheck the actual loaded marker on Paper's main thread. No chunk loading or world
access occurs in the review. A craftsman customer review is still required before
automatic craftsman founding; construction demand is not assumed to exist today.

## Verification boundaries

Temporary SQLite tests cover shared caps and food protection across industries,
configuration opt-outs, conserved startup/cargo/production, real paid bakery food
sales, qualification, marker/world absence, final-write rollback, once-only replay,
restart and read-only opportunity safeguards. Explicit fixture stock and skill
setup are confined to temporary test databases.

October13 full `.\mvnw.cmd clean verify` passed 1,482 tests (1,480 unit and two
packaged integration), zero failures/errors/skips, in 8:20. Twenty executed cases
were added by this batch. Focused runs of 128 and 135 cases also passed; the final
15-case processing class passed within the complete build.

No schema migration is needed; the database remains at version 77. Existing stock
catalogues, goods, receipts and history are unchanged. No new Paper adapter or
entity behavior is introduced by this batch. Live automatic bakery/craftsman
opening and their sustained ingredient/customer chains still need integration
and a disposable native-server check before being declared playable.
