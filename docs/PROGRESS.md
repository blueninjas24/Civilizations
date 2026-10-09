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
| 2. Needs & Economy | Villages need food, materials and housing, and choose responses | 2 / 4 | `█████░░░░░` 50% — basic supplies, labor, budget and family wellbeing verified; decisions/trade groups partial |
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

## Phase 2 — supplies and basic labor/treasury/wellbeing complete

- [x] Commodity supplies, demand, reserves and shortages. Explicit headless inputs,
  saved reserves/history, shortage duration/recovery and transactional restart verified.
  Live food production/consumption now uses observed fields. No starvation deaths.
- [x] Basic labor, food production, treasury/spending and household food/housing wellbeing. Verified saved worker capacity, finite funds/payments and family welfare; business employment belongs to Phase 3.
- [ ] Feasible autonomous decisions with explanations.
- [ ] Player trade/prices and long-running economy/shortage tests.

Live food runs on a saved independent clock with real planted-field evidence,
shortage/recovery explanations and editable famine/feast tuning. Household demand,
housing pressure and shared food conditions now save with each period. Treasury
accounting is tested separately from wellbeing; configurable one-time opening funding
is now wired. Confirmed food sales now spend actual village funds and pay emeralds;
emerald bargaining now adds saved counters and cooldowns; barter and broader execution remain ahead.
New periods now divide fulfilled food between occupied families and unassigned
residents, retaining each family's shortage streak. These simulated shares do not
establish physical meals. Basic family welfare now follows actual food/housing
evidence and retains resident scores/streaks across moves. It is separate from money
and future business/employment prosperity.

Explicit delivered-food receipts now survive restart and enter future periods once,
with transactional shortage relief and no retroactive catch-up aid. This is internal
accounting; confirmed player donations now use a protected village menu.
`/civ aid` now safely previews held food for a nearby named village and its latest
food situation. The preview leaves items and supplies untouched. Sneak-right-clicking a managed
villager opens a separate donation menu; only an explicit confirmation sets food
aside, and only a verified saved debit credits future supplies.

Villages now choose and save an explainable priority: seek food, investigate
observed crowding, or monitor. Seeded selection filters infeasible/irrelevant options,
reviews on a bounded cadence and reconsiders when pressures appear or clear. These
now include voluntary food appeals in the village menu, with saved shortage-change
and resolution evidence. Budget-backed food purchases are implemented; broader
response execution/learning and building permissions remain ahead.

### Phase 2 detail — see progress inside unfinished groups

| Reporting group | Verified pieces | What still blocks completion |
|---|---|---|
| Supplies and shortages — complete | Commodity accounting, saved reserves/history, duration/recovery, long crisis fixtures | Complete as an accounting foundation |
| Basic labor, treasury and wellbeing — complete | Field-backed adult production, household allocations, once-only opening funds, actual spending/payments, resident-linked welfare and crisis/recovery history; 419 automated cases and Paper | Complete for the Phase 2 food/housing prototype; employment/industry welfare belongs to Phase 3 |
| Autonomous decisions — partial | Real pressures, feasible priority candidates, seeded weighted choice, saved explanations, early review/recovery, player-readable food appeals and saved observed outcomes | Broader executed responses and learning weights from attributable outcomes |
| Player economy and stability — partial | Configurable scarcity prices, exact budget-limited quotes, long shortage/recovery tests, donations, emerald purchases and saved price bargaining/cooldowns | Barter, protected outbound goods and wider integrated balance/compatibility tests |

Finance direction is now approved: a moderate configurable opening financial
position, emerald payments and optional barter from real production stocks. These
choices resolve the earlier questions. One-time opening funds are implemented;
emerald food payments, basic family welfare and emerald bargaining are now implemented; barter
remains pending. The full milestone count remains 2/4. Actual business/workplace assignment
belongs to Phase 3.

Completed pieces in a partial group are real progress. The top bar counts whole
reporting groups, so it now shows 2/4 (50%); it does not measure coding hours or all
partial work. It will advance when a full group and its acceptance gate are verified.
See FINANCE_FOUNDATION.md, FOOD_APPEALS.md, FOOD_DONATIONS.md, PLAYER_FOOD_AID.md, FOOD_RELIEF.md, VILLAGE_PRIORITIES.md, LIVE_FOOD_ECONOMY.md,
HOUSEHOLD_NEEDS_AND_TREASURY.md and ECONOMY_TUNING.md.

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

Barter now has tested internal accounting: real timber/stone reservations, protected
stock floors and once-only cancellation/completion, including a 240-period finite-
stock famine/restart scenario. The 450-case Wrapper build and existing Paper economy
regressions passed. No live barter interface yet; this partial work does not complete
the trade group. Next is saved player-inventory confirmation. See BARTER_ACCOUNTING.md.

## Phase 3 — planned

- [ ] Persistent businesses, owners, workers and sites.
- [ ] Resources and connected production chains.
- [ ] Business founding, competition, closure and succession.
- [ ] Emergent specialization and tested business turnover.

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
