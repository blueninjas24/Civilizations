# Processing workshops — bakeries and general craftsman

## Latest - several recipes in the same general workshop

Admins may now add several `industry: craftsman` entries under `recipes` in
`economy.yml`; the existing global32-recipe cap remains. The worker considers
supported recipes in alphabetical recipe-ID order, preferring actual productive
owned inputs/storage, then paid incoming ingredients, then a bounded complete
review of affordable real warehouse stock. The supported baseline remains the
fallback for normal input planning. No incoming item is usable before delivery.
A recipe selects work, not a guaranteed finished-goods customer.

For example, this optional alternative can accompany `craft_storage_fittings`:

```yaml
recipes:
  # Keep your other existing entries here. Do not add a second recipes: section.
  z_stone_fittings:
    industry: craftsman
    inputs:
      stone: 3
    outputs:
      storage_fittings: 1
    batches-per-worker: 1
    site-batch-capacity: 4
```

This is an optional abstract fitting recipe, not a default balance change or a
physical chest. Startup can choose it when timber is unavailable, provided actual
fitting customers, protected public surplus, qualification, prices, funding and
marker checks pass. Runtime buys only actual offered warehouse goods with its
own money. Product/ingredient storage sizes and prices still need explicit setup.
Old saved catalogues stay fixed; adding an unsupported new product does not grant
space or stock. Wider furniture products, household purchasing and physical
construction remain separate work. Final temporary-database and isolated Paper
verification is recorded in PROJECT_STATUS/DEVLOG; schema remains98.

## October16 — gradual first-craftsman study and productive work learning

A first-craftsman opportunity can now sponsor one eligible unemployed adult.
The same real fitting customers, protected startup funding, current complete
crafting-table evidence and paid first-input feasibility must remain available.
Students cannot already own an active business, hold a job or study another
course. Enrollment pages rotate and existing village study limits still apply.

New-file `resident-study.skills.crafting` targets10 experience, consumes one real
timber per productive economic period and uses the existing editable study gain,
food/material reserves and student limits. No past queued period enrolls a new
student or grants practice. Current future study is distinct from subsequent
actual craft work. Absent worlds/downtime, missing/stale markers, unavailable
customers/funding and insufficient supplies pause without awarding experience.
If another craftsman takes the first-firm opportunity, the learner is released.
Restart preserves the accepted course, its expenses and actual earned skill.

The automatic review now sponsors training when there is no qualified founder;
after qualification it uses the existing main-thread point confirmation and
atomic paid opening. `work-learning.industry-skills.craftsman: crafting` awards
bounded experience only for actual recorded productive work, once per operation.
No zero-output or repeated pulse can create an extra award.

Schema81 adds initially empty founder review/course tables and their index,
retaining old people, accepted courses, jobs, stocks, balances and history.
Foreign keys tie a sponsorship to its actual course, village/world and saved
review. Migration and late-write failure/retry are transactional.

Existing installed files need explicit opt-in: the crafting study course,
`craftsman` in `automatic-career-study.industries`, the crafting work-learning
mapping, and the earlier craftsman founding/recipe/customer terms. Old lists and
catalogues are not overwritten. Extra employee hiring still requires explicitly
adding `craftsman` to `automatic-business-staffing.industries`; opening supplies
one actual qualified working founder. The first paid input route still requires
public surplus. Wider products and household furnishing demand remain unfinished.
Verification results are recorded in PROJECT_STATUS.md and DEVLOG.md.

## October16 — qualified general craftsman opening

The first general craftsman now prepares and atomically accepts an opening when
current funded storage-fitting customers, a fresh available crafting-table marker,
qualified unemployed adult, shared protected grant and real paid public surplus
all exist. A conservative bounded source review considers already stocked local
fittings before inferring a gap. It creates no shoppers, guaranteed revenue or
reservation; actual customer cash/stock/needs can change before any later sale.

Default qualification reuses the small workshop bounds: age216 months, founder
crafting experience10, worker experience1 and two worker slots. Evidence uses at
most eight tables,60-second freshness,96 horizontal/48 vertical blocks. Opening
cost500 plus4000 working capital shares existing food-protected startup caps;
there is no extra craftsman allowance. The default first batch buys three timber
and one stone for1500 hundredths of Trade Value, leaving2500 working cash.
Material reserves and configured recipe/storage/haul limits still apply.

`craftsman-founding` and `craftsman-opportunities` are separately editable. Missing
sections disable the new opening. Existing installed files must explicitly add
these sections, enable crafting-table recognition, fitting storage/recipe/customer
terms and the existing shared grant eligibility. No file/catalogue is overwritten.
The first-input route currently uses protected public surplus; a private starter
fallback is not claimed. Normal later supply still buys real local inputs.

`CraftsmanStartupCycle` keeps one proposal or acceptance in flight and rotates
saved living villages. `PaperCraftsmanStartup` confirms an already loaded crafting
table on the main thread; it never loads chunks, moves blocks, claims buildings
or reads container contents. The SQL owner repeats current eligibility before the
entire business/site/worker/grant/material order/marker commits. Replay preserves
accepted identity and cannot charge again. A fresh present-world point is still
required by the existing marker startup contract, including historical replay.
Diagnostics explain deferrals, and the plugin schedules/cancels this loop normally.

This opens a qualified owner. Gradual first-craftsman study, crafting work-learning
and extra employee staffing opt-in remain next; no resident receives free skill
or an automatic vanilla profession change. Existing worlds with no crafting skill
will wait rather than manufacture a founder. Broader useful products and household
demand remain unfinished. Schema stays80; old data needs no new migration.
Verification results are recorded in PROJECT_STATUS.md and DEVLOG.md.

## October16 — first real craftsman customers

Limited business storage fittings now have a production recipe, actual funded
customers, paid orders and delayed installation in the industry runtime. A saved
qualified craftsman can make and sell them; a warehouse can resell actual stock.
The supplier-independent demand review will support first-craftsman opportunities
without requiring that a craftsman already exist. No automatic craftsman opening
or free crafting experience is claimed. See STORAGE_UPGRADES.md for editable
terms, installed-file opt-in, schema80 and preservation limits.

## October15 — first-baker study, commercial competition and automatic opening

The approved bakery market forecast can qualify an opening from actual recent
paid village food orders even if other suppliers already cover those orders.
It averages receipts across the complete observed lookback (including quiet
periods), caps by current resident demand, then applies an editable market share,
unit limit, real post-startup cash, food budget and haul limit. No future sale is
guaranteed or reserved. It neither counts old deliveries as missing food nor adds
food orders/consumption/money. The existing rotating supplier selection still
places only legitimate current orders. More than3,200 indexed receipts defers
this forecast; the independent uncovered-demand route remains available.

New-file `bakery-market` defaults: enabled,12 economic periods,10000 basis points
(up to100% of observed paid demand),32 units per period. This is investment
eligibility for the first bakery, not an exclusive sales allocation. A missing
section preserves installed uncovered-meal/reserve-demand behavior. Empty paid history cannot
justify competition; declining paid demand may pause training before opening.

A prospective baker must be an eligible unemployed adult, not a current active
business owner or student. One first-owner course per village is sponsored only
for current smoker/funding/customer/input evidence. New-file baking study earns1
experience per productive economic period toward10 and consumes1 real timber per
period for abstract practice/equipment/fuel. Existing food and material reserves,
student budgets and actual supplies apply. The course gets no past queued work;
unknown opportunities pause without spending, and a taken first-bakery opportunity
cancels the course to release the learner. Unloaded worlds/downtime remain paused.
Training may inspect one currently processing future food period; purchases and
opening acceptance still require current completed evidence with no backlog.

Schema79 adds initially empty bakery founder review/course tables with existing
resident/course/world references, plus a paid-food history index. It does not
rewrite balances, people, jobs or old accepted grants/shipments. Enrollment,
reviews and spending retain the existing atomic owner-thread transaction model.

`BakeryStartupCycle` polls one SQL proposal/acceptance at a time and rotates saved
villages. `PaperBakeryStartup` checks one smoker on the main thread, only in an
already loaded chunk; no chunk tickets, block placement, container access or
property claim. The SQL owner then repeats all eligibility and funding checks.
The plugin schedules this loop and includes its explanation in diagnostics.

Existing installed files must explicitly opt in: copy/tune the bakery-founding,
bakery-opportunities and bakery-market sections, bakery bread recipe, grain
storage/supply terms, baking study course, bakery career-study permission and
bakery→baking work-learning mapping from the bundled default. Nothing overwrites
an administrator's installed economy file or expands saved store catalogues.
Additional bakery employee hiring requires opting `bakery` into the existing
`automatic-business-staffing.industries` list; this batch opens the working
founder and preserves existing staffing lists. It does not give unqualified
residents free baking experience.

This connects the first bread bakery. Cakes, egg/milk ingredient integration,
multiple product selection, the general craftsman and physical construction
remain unfinished. No human-server deployment is implied by a passing test.
Verification results for this batch are recorded in PROJECT_STATUS and DEVLOG.

## October15 — private first-batch grain purchases (schema78)

If preserved public surplus cannot supply the first bread batch, preparation can
quote real grain from a staffed, operating local warehouse or a valid registered
hybrid farm. Only the current grain-only bread input is supported by this private
bootstrap. The same editable starter prices, purchase cap, retained material
reserve, startup grants, output storage and transport limits apply. Suppliers
with insufficient actual stock, the wrong world, unknown farm catalogues or no
qualified staff cannot justify startup. More than64 eligible stocked source rows
defers review instead of choosing from a partial sample.

The bakery pays from its real granted working capital; money goes to the supplier,
not the treasury. The seller's stock becomes owned cargo once, destination space
is reserved, and the bakery's new delivery clock begins at the actual acceptance
time with no past earned work. Normal timed delivery makes the grain available.
The first-batch purchase and later ordinary restocking use different operation IDs.

Schema78 adds one initially empty `bakery_private_starters` table. Its receipt binds
the saved business, buyer-linked shipment, seller and versioned price/recipe/cargo
terms. Startup, cash, source stock, worker, marker, clock, shipment and receipt
commit together. A final receipt failure rolls everything back. Restart/replay
cannot change suppliers/prices/recipes, repay or redeliver. An accepted starter
cannot switch between public and private routes to receive a second bundle.
Existing public starters retain their original receipts and behavior. No existing
assets, catalogues, ownership or history are rewritten by the migration.

Live automatic opening and first-owner training are still unfinished. Current
investment still requires an uncovered funded food/reserve need. A separate
question asks whether bakers may compete for existing real paid food orders in
healthy villages; that commercial rule remains unchanged while the answer is pending.

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

This still does not automatically found bakeries in ordinary play. The original
qualified-startup batch used public surplus; the subsequent private first-batch
route above supports new farm surplus without taking ownership from the farmer.
Supply-backed first-owner training and the main-thread opening cycle remain
integration work. Existing recipes can run only
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

October15 private-input batch: seven added cases, focused56 and targeted84 historic
migration checks passed. Older fixtures now explicitly require the new receipt
table to be empty and retain every predecessor-row comparison. Final
`.\mvnw.cmd clean verify` passed1,503 (1,501 unit/two packaged integration), zero
failures/errors/skips,7:07 at21:43:51 ET. Paper26.2 build129 proof passed21:44:30 ET
for actual SQL-owner private startup, source payment, receipt/restart replay,
normal timed delivery/bread, paid food and absence. Schema77→78 audit retained all
133 predecessor tables,130 exact; three normal bookkeeping changes, no aging,
new main private-receipt table empty. Fixture stopped/settings restored/helper
archived. This does not establish natural automatic opening or human playtest.

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

The qualified-public-startup batch required no migration (schema77). The subsequent
private-input batch adds only the empty schema78 receipt table. Existing stock
catalogues, goods, receipts and history are unchanged. No new block/entity behavior
is introduced. The bakery SQL-owner bridge is configured at plugin startup; live
automatic opening and owner training still need integration
and native verification before being declared playable. Existing paid farm grain
restocking is connected separately from first-firm bootstrap.
