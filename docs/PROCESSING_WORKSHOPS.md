# Processing workshops — startup funding and bakery prospects

## October15 — qualified opening and paid farm grain

`BakeryInvestment` now prepares a seeded qualified founder and accepts only after
rechecking current customers, food protection, supplies, identity, skills, jobs,
study and marker evidence in one SQL-owner transaction. Changes require a fresh
proposal. Exact accepted replay cannot charge again, refill stock or restore a
confirmed missing marker. `PopulationSession` exposes preparation/acceptance on its
own database thread; no connection is shared with Paper callers.

New-file prototype settings are editable: adulthood216 months, founder baking10,
worker baking1, two worker slots; startup500 and capital4000 hundredths, within the
existing shared grants. `recipes.bakery_bread` consumes3 grain for3 abstract food
units, at one batch per worker and site cap4 per industry period. Grain has unit
storage size1 and base Trade Value1; its generic opening grant is0. These are
experimental economic units, not a physical item recipe or a free starting crop.
Old installed files retain their recipes and missing bakery sections remain disabled.

Existing dedicated public grain harvests stay in `grain_stocks`. Starter purchases
review that stock plus any explicitly configured generic public grain balance,
debit harvested grain first, retain the combined material reserve and record the
actual total before/after. They never copy or reset balances. Both ledgers, payment,
founding and cargo roll back together on failure. Historic accepted orders replay
under their original terms. No schema change is needed.

Registered hybrid farms are now recognized as producers in local demand and
warehouse restocking. A valid farm catalogue is still required; its reserved toolkit
is not sale stock. Warehouses pay the farm for owned grain, deliver it, and sell it
to a funded bakery under existing shared transport/storage constraints. The
80-period regression tracks actual harvests, all grain in stores/cargo/processing,
paid village meals and total treasury/business cash. No public grain or private
cash is created to start that scenario.

This still does not automatically found bakeries in ordinary play. The current
startup backend buys one public-surplus starter bundle; new farm surplus is
private. A first-batch private purchase, supply-backed first-owner training and the
main-thread opening cycle remain integration work. Existing recipes can run only
with appropriate saved stores, staff and real inputs. Craftsman customer/production
integration and cakes/multiple product selection remain unfinished. Construction
continues to belong to Phase4.

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
live founding adapters and wider product chains remain unfinished; existing funded
bakeries can use the bread prototype described above. The new
backend also does not create individual wallets, salaries, ingredients or shoppers.

## Implemented backend

The existing transactional marker-workshop startup can now accept these approved
industries. Founding, real treasury debits, an empty funded store, the first
qualified worker, one paid surplus-material cargo and the fixed marker commit
together. A late marker failure rolls back every table. Actual delivered inputs
can then be processed by an explicitly supplied matching recipe; changed retries
are rejected. Initial processing tests use fixture recipes; subsequent qualified
investment tests exercise the actual configurable bread prototype.

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

October15 full `.\mvnw.cmd clean verify` passed1,496 (1,494 unit/two packaged
integration), zero failures/errors/skips;14 additional cases. Focused143 and
SQL-owner10 passed. A stale new-file storage expectation was corrected; installed
and missing-legacy catalogues explicitly remain unchanged. Paper26.2 build129
packaged SQL-owner proof passed21:12:10 ET, including preserved grain, timed bread,
paid meals, replay and absence. Main test-save audit retained133 tables,130 exact;
only three normal bookkeeping tables changed, age delta0. Fixture stopped/settings
restored/helper archived. No natural automatic-opening or client-interaction proof.

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
catalogues, goods, receipts and history are unchanged. No new block/entity behavior
is introduced. The bakery SQL-owner bridge is configured at plugin startup; live
automatic opening, first private inputs and owner training still need integration
and native verification before being declared playable. Existing paid farm grain
restocking is connected separately from first-firm bootstrap.
