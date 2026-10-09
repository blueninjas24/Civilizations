# Civilizations progress tracker

Updated October 9, 2026. Based on verified work, not planned features.

Bars count completed milestones in the checklist below. Milestones differ in
size, so percentages are not estimates of hours, release dates or total project
completion. A phase is complete only after its canonical roadmap gate is passed.
Partial work does not fill a completed milestone. These are reporting groups,
not a replacement for ROADMAP.md or its design requirements.

| Phase | What you will experience | Milestones complete | Progress |
|---|---|---|---|
| 1. Foundation & Population | Villages remember their people; families and population change over time | 10 / 10 | `██████████` 100% — prototype gate passed |
| 2. Needs & Economy | Villages track shortages, learn from food deliveries and offer affordable responses | 4 / 4 | `██████████` 100% — prototype gate passed |
| 3. Businesses & Industry | Farms, mines and workshops create jobs and shape each town | 0 / 4 | `░░░░░░░░░░` 0% |
| 4. Physical Growth | Villages build and improve structures gradually | 0 / 4 | `░░░░░░░░░░` 0% |
| 5. Trade & Roads | Settlements trade, connect by roads and depend on each other | 0 / 4 | `░░░░░░░░░░` 0% |
| 6. Identity & History | Leaders, reputation and relationships create memorable local histories | 0 / 4 | `░░░░░░░░░░` 0% |
| 7. Growth & Decline | Towns thrive, struggle, leave ruins and sometimes recover | 0 / 4 | `░░░░░░░░░░` 0% |
| 8. Advanced Civilization | Territory, player settlements and rare crises enrich the world | 0 / 4 | `░░░░░░░░░░` 0% |
| 9. Polish & Release | Many settlements run smoothly in a balanced, tested survival world | 0 / 4 | `░░░░░░░░░░` 0% |

## Phase 1 — prototype gate passed

- [x] Reliable plugin build and automated testing.
- [x] Settlement saving and safe database upgrade machinery.
- [x] Repeatable simulation test runner, separate from Minecraft.
- [x] Validate automatic online builds, disposable server setup and backup recovery. Hosted Windows/Linux CI passed on approved snapshot e85416e; later local work passed Wrapper and real Paper checks. Latest feature branch remains unpushed.
- [x] Save named individual residents across restarts. Headless storage and actual Paper projection/restart verified.
- [x] Households, basic family ties and the foundation for jobs. Saved households/relationships/skills and explainable age/skill qualification checks verified. Actual businesses/hiring belong to later phases.
- [x] Aging, births and deaths. Saved headless rules, visible babies/adulthood, native deaths and simulated death removal verified on Paper.
- [x] Immigration and emigration. Saved external arrivals and whole-household moves verified; five migrants materialized near their destination with permanent person IDs.
- [x] Civilization calendar and saved simulation/catch-up state. Owner-thread timer, elapsed months, restart, bounded offline catch-up and read-only diagnostics verified.
- [x] Connect simulated people to visible villagers and in-game diagnostics; validate the population experience on Paper. Safe nearby spawning, names/ages, pending/activation restart recovery, native breeding coordination, approved zombie-conversion prevention, migration and death reconciliation verified with real Paper entities.

First playable prototype reached: named residents persist, families/population change,
and nearby villagers reflect saved births, ages, deaths and moves. The real Paper
probe uses synthetic observation positions; a human Minecraft client playtest is still
needed. This is the Phase 1 foundation gate, not a balanced survival release. See
PHASE1_PLAYTEST.md for setup and material limitations, including trade resets on moves.

## Phase 2 — prototype gate passed

- [x] Commodity supplies, demand, reserves and shortages. Saved accounting/history,
  actual field-backed food, shortage duration/recovery and forward famine/feast tuning.
- [x] Basic labor, treasury and household welfare. Adult/field capacity, finite funds,
  real spending/refunds, family food/housing welfare separate from money.
- [x] Feasible autonomous decisions with explanations. Seeded priorities, bounded
  learning from verified earlier deliveries, saved donation/emerald/timber/stone/wait
  comparison, blocked reasons and player-visible preferences. Confirmed exchanges
  retain actual execution receipts and exact future import outcomes.
- [x] Player trade/prices and long-running stability tests. Donations, finite-budget
  emerald sales, bargaining/cooldowns, finite-stock barter, multiple 240-period
  conservation/learning/publication/restart scenarios and native Paper exchanges.

**All four prototype groups are verified.** Final Windows Wrapper clean verify
passed 494 cases (492 unit/two packaged integration), zero failures/errors/skips.
Pinned disposable Paper passed publication, learning, native donations, bargaining,
food-for-emerald/log exchange, failed-save/duplicate/restart protection, welfare
recovery and unloaded operation. Final normal startup/diagnostics/shutdown and
read-only migration/integrity/history checks passed. See PHASE2_ACCEPTANCE.md.

Players can see what a village prefers and choose another available option.
Every exchange rechecks current stock, funds, food need and consent. Pending food
reduces additional need. No supplies, income or successful supplier are invented.
Admin settings remain editable in economy.yml; old owner files remain unchanged
and explicitly opt in to new sections. See FOOD_RESPONSES.md and PRIORITY_LEARNING.md.

This completes the food/housing economy foundation, not a balanced survival release.
Human client UX, inventory-plugin/native-death compatibility, cross-save backup
restoration, long-server profiling and balance remain release checks. No starvation
mortality, actual business employment, recurring nonfood industry, physical housing
construction, regional trade or reputation is claimed here. Those remain in the
canonical later phases or require separate consequential tuning.

Phase 3 — persistent businesses and industry — is next. Phases 3–9 remain 0%.
Percentages count verified reporting groups, not hours or overall plugin completion.

Earlier batch notes below describe progress at that time; the current Phase 2 gate
above supersedes their partial percentages.

Verified paid-food batch: protected Sell confirmation, configurable prices/need target,
reserved funds, saved item exchange, refund/retry and restart passed 401 automated
cases and disposable Paper. Native fixture exchanged 8 wheat for 3 emeralds once.
This advances part of player trade; it does not complete the barter/bargaining,
feasible-response and wider balance gates. The subsequent welfare batch passed
419 automated cases, including a 240-period finite-budget exchange/restart scenario,
and real Paper famine/recovery. See HOUSEHOLD_WELLBEING.md.

The bargaining batch adds `/civ bargain <price>` and explicit acceptance: current
value/budget counters, editable patience/deadlines and restart-safe cooldowns.
Wrapper passed 435 automated cases; disposable Paper verified a saved counter
surviving restart and one native 8-wheat/3-emerald exchange. Barter and the other
unfinished gates keep the phase at 2/4 (50%). See FOOD_BARGAINING.md.

The accounting batch established real timber/stone reservations, protected
stock floors and once-only cancellation/completion, including a 240-period finite-
stock famine/restart scenario. The 450-case Wrapper build and existing Paper economy
regressions passed. See BARTER_ACCOUNTING.md for stock semantics.

The subsequent player barter batch implements `/civ barter timber|stone` and
explicit acceptance, with saved inventory proof, stock reservation/refund and
restart recovery. Actual Paper exchanged 8 wheat for 6 oak logs once without
spending money. A mixed two-village 240-period scenario accounts for every food,
currency and material unit across donations, paid sales, barter and restart. The
final Wrapper build passed 460 cases, with zero failures/errors/skips. The
trade prototype group is complete, advancing Phase 2 to 75%; broader autonomous
responses/outcome learning still block 100%. Human survival/client/plugin testing
remains required for release. See PLAYER_BARTER.md.

## Phase 3 — started: business storage and staffing foundation

- [ ] Persistent businesses, owners, workers and sites.
- [ ] Resources and connected production chains.
- [ ] Business founding, competition, closure and succession.
- [ ] Emergent specialization and tested business turnover.

First milestone is partially implemented: real business/site identity, founders,
initial owners, qualified hiring, finite worker capacity, once-only startup spending
and retained employment history now have tested backend APIs. Dead/migrated people
cannot count as current workers. Automatic site recognition, founding/hiring and
in-game business diagnostics remain the next gate; registration produces no goods
yet. Phase 3 stays 0/4 completed gameplay milestones rather than counting backend
work as a finished in-game feature. See BUSINESS_FOUNDATION.md.

Resource/production group now also has verified partial work: finite mining with
shared rock byproducts, bounded business storage and actual local warehouse buying/
resale, reserved cargo and recorded causal-use APIs. Editable commented defaults
are supplied. Final Wrapper passed 522 tests; disposable Paper schema-28 startup/
shutdown and preserved history/config passed. Automatic mine recognition, business
operation, NPC shopping and production chains remain unwired, so the group stays
unchecked. See FINITE_MINING_AND_WAREHOUSING.md. This is concrete backend progress,
not a finished player-visible industry system.

## Phase 4 — planned

- [ ] Building templates and authoring foundation.
- [ ] Terrain-aware layouts and road access.
- [ ] Player-build protection and safe staged construction.
- [ ] Upgrades, reuse, decay and disposable-world safety tests.

## Phase 5 — planned

- [ ] Inter-settlement imports/exports and economic dependency.
- [ ] Persistent roads, bridges and routes.
- [ ] Nearby merchants and abstract long-distance trade/migration.
- [ ] Route maintenance/disruption and unloaded-region tests.

## Phase 6 — planned

- [ ] Governments, leaders and succession.
- [ ] Settlement relations and limited diplomacy.
- [ ] Earned player reputation and causal structured histories.
- [ ] Notable people/infrastructure and identity-preserving rename/merger tests.

## Phase 7 — planned

- [ ] Prosperity and decline emerge from interacting conditions.
- [ ] Maintenance problems and gradual deterioration.
- [ ] Abandonment, ruins, preservation and recovery.
- [ ] Repeatable long histories and recovery scenarios.

## Phase 8 — planned

- [ ] Organic territory, outposts and discovery.
- [ ] Cultural/historic structures and rare contextual crises.
- [ ] Abstract conflict with appropriate nearby evidence.
- [ ] Player settlements and earned influence.

## Phase 9 — planned

- [ ] Performance at 20+ settlements, including rare large cities.
- [ ] Compatibility, migrations, recovery and administration.
- [ ] Templates/content and balanced simulation.
- [ ] Documentation and long survival playtesting; all release gates verified.

For detailed requirements see ROADMAP.md. For exact tests, limitations and code
status see PROJECT_STATUS.md. Future batches update this checklist from verified
implementation; planning or a synthetic test alone does not complete gameplay.
