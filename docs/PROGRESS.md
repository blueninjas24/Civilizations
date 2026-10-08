# Civilizations progress tracker

Updated October 7, 2026. Based on verified work, not planned features.

Bars count completed milestones in the checklist below. Milestones differ in
size, so percentages are not estimates of hours, release dates or total project
completion. A phase is complete only after its canonical roadmap gate is passed.
Partial work does not fill a completed milestone. These are reporting groups,
not a replacement for ROADMAP.md or its design requirements.

| Phase | What you will experience | Milestones complete | Progress |
|---|---|---|---|
| 1. Foundation & Population | Villages remember their people; families and population change over time | 5 / 10 | `█████░░░░░` 50% — active |
| 2. Needs & Economy | Villages need food, materials and housing, and choose responses | 0 / 4 | `░░░░░░░░░░` 0% |
| 3. Businesses & Industry | Farms, mines and workshops create jobs and shape each town | 0 / 4 | `░░░░░░░░░░` 0% |
| 4. Physical Growth | Villages build and improve structures gradually | 0 / 4 | `░░░░░░░░░░` 0% |
| 5. Trade & Roads | Settlements trade, connect by roads and depend on each other | 0 / 4 | `░░░░░░░░░░` 0% |
| 6. Identity & History | Leaders, reputation and relationships create memorable local histories | 0 / 4 | `░░░░░░░░░░` 0% |
| 7. Growth & Decline | Towns thrive, struggle, leave ruins and sometimes recover | 0 / 4 | `░░░░░░░░░░` 0% |
| 8. Advanced Civilization | Territory, player settlements and rare crises enrich the world | 0 / 4 | `░░░░░░░░░░` 0% |
| 9. Polish & Release | Many settlements run smoothly in a balanced, tested survival world | 0 / 4 | `░░░░░░░░░░` 0% |

## Phase 1 — active

- [x] Reliable plugin build and automated testing.
- [x] Settlement saving and safe database upgrade machinery.
- [x] Repeatable simulation test runner, separate from Minecraft.
- [ ] Validate automatic online builds, disposable server setup and backup recovery. Tools/procedures exist; hosted CI, Paper testing and recovery drill remain pending.
- [x] Save named individual residents across restarts. Verified headlessly; in-game seeding/projection remains in the final milestone.
- [ ] Households, basic family ties and the foundation for jobs. Household membership and parent/partner links are saved; siblings are recognized. Job foundation remains.
- [x] Aging, births and deaths. Headless rules, family/event saving and restart recovery verified; live gameplay connection remains in the final milestone.
- [ ] Immigration and emigration.
- [ ] Civilization calendar and saved simulation/catch-up state. Aging checkpoints survive restarts with bounded catch-up; calendar/scheduler integration remains.
- [ ] Connect simulated people to visible villagers and in-game diagnostics; validate the population experience on Paper.

First playable target: a village whose named residents survive restarts and whose
population changes over time. No new population gameplay is implemented yet.

## Phase 2 — planned

- [ ] Commodity supplies, demand, reserves and shortages.
- [ ] Labor, production, treasury and household wellbeing.
- [ ] Feasible autonomous decisions with explanations.
- [ ] Player trade/prices and long-running economy/shortage tests.

The prototype has a simple population-versus-beds check; that does not complete
the household-driven needs/economy milestone.

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
