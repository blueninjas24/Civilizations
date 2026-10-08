# Civilizations — canonical nine-phase roadmap

**Planning baseline (not a deadline):** 10–15 personal hours/week, originally 68 weeks / roughly 680–1,020 hours from September 28, 2026 to early 2028. Codex may compress implementation time, but simulation balance, world templates, integration and playtesting still require human validation. Dates below are original planning estimates, **not current status**.

| Phase | Original planning window | Delivery gate |
|---|---|---|
| 1. Foundation & Population Core | Weeks 1–4, Sep–Oct 2026 | Stable persistent settlements/residents, reproducible build, migrations and simulation tests |
| 2. Needs, Economy & Basic Decisions | Weeks 5–9, Nov 2026 | Needs/shortages and explainable autonomous choices |
| 3. Industry & Businesses | Weeks 10–14, Dec 2026–Jan 2027 | Persistent businesses and emergent specialization |
| 4. Physical Growth Engine | Weeks 15–23, Jan–Mar 2027 | Decisions produce plausible staged construction |
| 5. Trade, Roads & Regional Economy | Weeks 24–30, Mar–May 2027 | Interdependent settlements, routes and visible infrastructure |
| 6. Identity, Diplomacy & History | Weeks 31–37, May–Jun 2027 | Leaders, relationships, reputation, structured histories |
| 7. Growth, Decline & Living History | Weeks 38–44, Jul–Aug 2027 | Cascading prosperity/decline, abandonment, ruins, recovery |
| 8. Advanced Civilization | Weeks 45–56, Aug–Nov 2027 | Territory, discovery, player settlements, contextual crises/conflict |
| 9. Scale, Polish & v1.0 | Weeks 57–68, Nov 2027–Jan 2028 | 20+ settlement scale, migrations, compatibility, balanced survival experience |

## Phase 1 — foundation and population
Audit package/repo; Maven Wrapper/JDK/Paper compatibility; Git branches/CI; disposable local Paper server; backups of source/world/config/SQLite; versioned migrations; automated JUnit domain/persistence tests; deterministic accelerated simulation; `/civ debug` inspection/explain tools; permanent settlement and resident identity; households, basic family ties, employment foundation, aging, births/deaths, immigration/emigration; separate civilization clock; contextual NPC materialization. **Do not jump to physical construction.**

## Phase 2 — economy and decisions
Commodity catalog, reserves, supply/demand, escalating shortages, labor/production, treasury vs prosperity, household wellbeing, Trade Value/barter/player prices; pressures, feasible response candidates, weighted scoring and explanation logs. Test long-run starvation/shortage loops and economic stability.

## Phase 3 — businesses and specialization
Persistent founders/owners/employees/sites, resource discovery/potential/depletion, production chains, skill/capital eligibility, competition, closure/succession, emergent specializations. Test business turnover, industry dependency and rare exceptional firms.

## Phase 4 — physical growth
Template library and future authoring toolkit, road-first organic planner, land suitability, player-build protection, terrain-aware site selection, staged construction, civic/industrial/residential structures, building upgrades, reuse and decay. Test world modifications in disposable worlds and rollback/failure safety.

## Phase 5 — regional integration
Abstract imports/exports, trade value and scarcity, persistent roads/bridges/routes, route importance/maintenance/disruption, merchant materialization, migration and dependency. Test disconnected regions and long-distance unloaded trade.

## Phase 6 — social identity and history
Government modifiers, leaders/succession, settlement relations, limited diplomacy, reputation, causal history, structured event store, notable residents/businesses/infrastructure and rare mergers. Test stable UUID history across rename/merger.

## Phase 7 — boom and bust
Maintenance burden, cascading unemployment/migration/business decline, physical deterioration, abandonment, preservation, ruins, recovery and resettlement. Test multi-century histories with seeded simulation.

## Phase 8 — advanced systems
Organic territory and outposts, exploration/new deposits, cultural/historic structures, rare contextual environmental/industrial/social crises, abstract conflict with nearby visible evidence, player settlements and earned influence.

## Phase 9 — polish and v1.0
Profiling, 20+ settlement stress tests, bounded reconciliation, Paper API isolation, schema upgrade tests, template content, balancing, admin tools, recovery, documentation and long survival playtests.

## Phase gates
Every phase requires: deterministic unit/integration tests; accelerated multi-year scenario checks where applicable; a real local Paper smoke test for Minecraft-facing changes; performance/regression notes; documentation update; no unapproved design divergence. The milestone order reflects dependencies, not permission to defer all testing/performance until Phase 9.
