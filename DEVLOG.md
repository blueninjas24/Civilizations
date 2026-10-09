# Civilizations Development Log

## Exact finance quotes and shared treasury transactions — October 8, 2026

- Branch `codex/phase2-finance-transaction-foundation`: pure TradeQuotes provides
  exact full-stack Trade Value cost and maximum affordable whole items. Unknown
  budget differs from a measured zero; partial affordability never rounds up.
  Existing scarcity price rounding is reused once, with overflow/invalid inputs
  rejected. Quotes neither reserve funds nor accept goods or promise a payout.
- Treasury standalone APIs preserve their behavior. Internal row operations now
  join an existing SQL-owner transaction, allowing money and receipt writes to
  commit together. They never finish the caller's transaction. Tests combine an
  explicit fixture debit and food-delivery receipt; injected receipt failure rolls
  back both, retry retains both and later food accounting consumes the receipt once.
- Twelve new cases; final Windows Wrapper `clean verify -B -ntp` passed 369 tests
  (367 unit/two packaged integration), no failures/errors/skips. Covers 10,000 seeded
  affordability cases, scarcity conversion, cent/long boundaries, insufficient funds,
  shared opening/credit/debit rollback, operation retries and restart.
- Initial verification exposed an intermittent existing preview fixture failure:
  Paper Location retains a weak world reference and the unregistered proxy could be
  collected. Test now retains its fixture worlds strongly; pinned Paper source
  confirmed the lifetime contract. No gameplay behavior was changed for that fix.
- Schema remains 20. No real/server DB, world, inventory or owner config changed;
  all writes used temporary test databases. No new Paper-facing behavior or server
  smoke run this batch; funding/payment adapters will need native Paper/client tests.
- Updated finance guide, status, decisions, devlog and progress detail. Phase 2
  remains 1/4 full reporting groups. Initial funding source and player currency
  questions are pending; no starting money, income, emerald exchange rate, live
  purchase or barter policy has been assumed. Next: approved funding and payment
  handoff, then budget-backed purchase execution and integrated economy balance.

## Voluntary village food appeals — October 8, 2026

- Branch `codex/phase2-village-food-appeals`: a selected SEEK_FOOD priority now
  publishes the actual latest-period deficit in Village supplies. Empty-hand or
  unsupported/custom-item viewing is read-only; reopen with plain food for the
  existing confirmation. Disabled transfers/permissions retain a protected button.
- New period snapshots retain OPEN/NONE/RESOLVED/WITHDRAWN status, observed pressure
  improvement/worsening, actual production/imports and the governing priority review.
  Pending deliveries are separate; queued catch-up is disclosed. No invented quest
  target, payout, stock creation, supplier or building consent. Recovery observations
  do not prove the appeal caused it and do not train weights without attribution.
- Schema 20 transactionally adds food_appeals without backfilling past observations.
  Appeal save failure rolls back stocks, needs, priority and receipt use together;
  restart preserves exact history. No production reset or owner config rewrite.
- Twelve added cases; final Windows Wrapper `clean verify -B -ntp` passed 357 tests
  (355 unit/two packaged integration), zero failures/errors/skips. Includes all appeal
  states, mixed/conflicting evidence, non-causal local-production recovery, a repeated
  1,200-period scenario, temporary-SQLite restart, upgrade and write rollback/retry.
- Disposable native Paper checks passed empty-hand report/card and protected click,
  actual confirmed player-file debit, famine appeal across restart, recovery with
  twelve real imported food units and no duplicate receipt. Farm/unloaded checks
  still passed. Normal single-plugin startup/status/shutdown passed; no forced chunks.
- Consistent confirmed-dev schema-19 backup first; schema 20 integrity/FKs verified.
  All original settlement/resident/household IDs and family/entity links retained.
  Production JAR excludes probes/JUnit. Human client UX and scale profiling remain
  pending. Treasury funding/spending, paid trade/barter, broader action feasibility
  and attributable outcome feedback remain ahead. Phase 2 stays 1/4 full groups.

## Household food allocation — October 8, 2026

- Branch `codex/phase2-household-food-allocation`: completed food periods now divide
  fulfilled supplies proportionally between occupied families and the unassigned
  resident pool. Whole-unit largest remainders conserve food; permanent IDs resolve
  exact ties. Adult/child demand remains editable. This is simulated coverage, not
  physical meals, invented wealth or a new mortality/fertility rule.
- Saved SUPPLIED/UNDERSUPPLIED diagnostics and consecutive unmet family periods
  survive restart and clear with relief. Historic schema-18 allocation stays UNKNOWN;
  disappeared/reoccupied households begin a new consecutive streak. Exact tie bias
  and future social rationing policy are documented rather than silently locked.
- Transactional schema 19 adds four nullable fields; explicit-column INSERTs prevent
  schema-extension breakage. Allocation shares the food/needs/priority/receipt commit.
  No reset, forced chunk loads, owner config rewrite or production save changes.
- Eight added automated cases; final Windows Wrapper `clean verify -B -ntp` passed
  345 cases (343 unit/two packaged integration), zero failures/errors/skips. Includes
  10,000 seeded conservation scenarios, integer overflow boundaries, ordering/ties,
  a 1,200-period crisis/recovery, real family persistence and migration failure/retry.
- Disposable Paper probe passed supplied family, famine streak, unchanged history
  on restart and recovery after a verified native food donation; unloaded accounting
  still passed. Normal single-plugin startup/status/shutdown passed, no forced chunks.
- Consistent schema-18 confirmed-dev backup first; schema 19 integrity/FKs checked.
  Original settlement/resident/household IDs and family/entity links retained.
  Production JAR excludes probes/JUnit. Human client playtesting and scale profiling
  remain pending, alongside treasury funding/spending, autonomous response execution,
  outcome feedback and paid trade/barter. Phase 2 remains 1/4 complete reporting groups.

### Confirmed player food donations — October 8, 2026

- Branch `codex/phase2-player-food-donations`: sneak-right-click a saved living
  villager with ordinary food, then explicitly confirm the whole held stack in Village
  supplies. Ordinary right-click trading and read-only `/civ aid` remain available.
- Recovery receipt and inventory debit share Minecraft player data. A bounded worker
  verifies the full saved gzip/NBT receipt before the existing SQL owner credits food.
  Failed void saves, repeat clicks, pending operations and stale saved markers cannot
  create duplicate credit or a second debit. Offline/shutdown work resumes from markers.
- Editable donation enable flag; new configs true, existing files without it false.
  Pending recovery continues when new donations are disabled. Protected menu clicks/
  drags and next-tick confirmation; player permissions separate from admin debugging.
- Fourteen added automated cases; final Wrapper clean verify passed 337 (335 unit/two
  packaged integration), no failures/errors/skips. Covers bounded/corrupt/truncated NBT,
  receipt identity, failed saves, retries/shutdown, stale markers and consent guards.
- Real Paper native menu/inventory/player-data checks passed with a synthetic offline
  player facade: intentionally disabled save prevented credit, then saved debit/marker,
  reload, one receipt, crisis relief and duplicate rejection passed. Actual 26.2 path
  players/data verified after first attempt correctly withheld credit on the old path.
- Schema remains 18. Confirmed-dev backup first; original identities/links and
  integrity/FKs retained. Normal single-plugin startup, diagnostics, unloaded fixture
  and shutdown passed. No actual player/production save edits.
  Donation does not pay money/reputation or implement barter/autonomous purchasing.
  Pinned-format upgrade compatibility, human client UX, inventory plugins, scale and
  consistent cross-save backup recovery remain limits. See FOOD_DONATIONS.md.
- Phase 2 still 1/4 full reporting groups; actual player aid is now a verified partial
  feature. Next: household food allocation/wellbeing and broader economic responses.


### Player food-aid previews — October 8, 2026

- Branch `codex/phase2-food-aid-preview`: `/civ aid` now previews an ordinary held
  food stack for the nearest ready village home, names the saved village and shows
  its latest food report. It takes no items, creates no receipt and pays nothing.
- Editable optional `aid-preview` economy.yml section: enabled, same-world 3D range
  (default 32), six ordinary plant-food conversions. Custom items, partial evidence,
  unsupported quantities and ambiguous town ties fail clearly. Explicit target IDs
  still require nearby evidence. Old owner files retain edits and documented defaults.
- Public preview permission defaults true; administrative debug permission remains
  op-only inside its handler. Cached name/identity/food reads avoid main-thread SQL,
  block scans, chunk loads or waits. Schema remains 18; no migration/reset.
- Sixteen added automated cases; final Windows Wrapper clean verify passed 323
  (321 unit/two packaged integration), zero failures/errors/skips. Earlier test fixture
  used a live-registry-dependent ItemStack constructor; fixed the headless facade.
- Real disposable Paper passed native item conversion/custom rejection with a
  synthetic read-only player facade and unchanged goods/food, plus earlier food,
  needs, relief, restart and unloaded scenarios. This is not a human client test. Confirmed-dev schema-18 backup taken first;
  original IDs/family/entity links and integrity/FKs retained. Normal single-plugin
  startup, console routing, unloaded fixture and clean shutdown also passed. Existing
  economy.yml remains untouched. Production JAR excludes probes/JUnit.
- Actual donation/merchant inventory handoff remains blocked on durable cross-save
  recovery. Pinned Paper saveData returns void and PlayerDataStorage may return on
  disabled saving or log a save exception; merely calling it cannot certify a durable
  debit. No actual player data files edited. See PLAYER_FOOD_AID.md.
- Phase 2 stays 1/4 full groups; player-facing preview is a verified partial piece,
  not completed trade/barter. Next: a recoverable actual inventory transfer and
  broader household/economic responses. No new gameplay decision needed here.


### Explicit food delivery accounting — October 8, 2026

- Branch `codex/phase2-food-relief`: trusted internal adapters can record explicit
  delivered food with source, quantity and operation ID. Pending goods enter a future
  food period; retries cannot duplicate imports and new arrivals cannot rewrite old
  catch-up periods. Receipt processing is bounded to 64 per village period.
- Food, needs, priorities, checkpoint and applied receipt marks share one transaction.
  Failure rolls back effects for retry; partial evidence pauses delivery use. Schema 18
  adds the ledger without backfilling or resetting people, histories or reserves.
- Seven new temporary-SQLite cases cover relief, duplicates, arrival timing, rollback,
  restart/pause, input validation, batch limits and schema-17 migration recovery.
  Windows Wrapper clean verify passed 307 cases (305 unit/two packaged integration),
  zero failures/errors/skips. Production JAR excludes probes/JUnit.
- Disposable Paper probe passed a synthetic 12-unit shipment surviving restart,
  clearing zero-production shortage and rejecting duplication, followed by farm
  recovery/unloaded operation. Consistent schema-17 dev backup first; original people,
  households/family/entity links retained, schema 18 integrity/FKs verified. Normal
  single-plugin startup, unloaded fixture and clean shutdown also verified.
- This is backend delivery accounting, not a player inventory handoff, donation
  command, merchant or autonomous supplier. No normal gameplay caller submits goods.
  Player item exchange and full economic responses remain; Phase 2 stays 1/4 full
  reporting groups with these verified partial pieces listed in PROGRESS.md.
  See FOOD_RELIEF.md. No production reset, push, merge or main modification.


### Editable economy prototype — October 8, 2026

- User authorized prototype defaults and easy server-owner famine/feast tuning.
  Added commented economy.yml and strict startup parser; existing edits never overwritten.
- Added independent immutable settings, demand/initial-supply calculations, food output
  bounded by evidenced worker slots/labor, and exact abstract scarcity prices. Recipes
  use 100% normal, 25% famine, 200% feast; no invented farms or automatic deaths.
- Nineteen new cases; Wrapper clean verify passed 261 (259 unit/two integration),
  all passing. Real Paper created the file, accepted 25% edit, preserved its hash/comment
  on restart, served population diagnostics and stopped cleanly. Restored normal settings.
- Schema remains 14, no automatic inventory seeding/live economy/merchant behavior.
  Saved threshold transition still pending; current API rejects silent history changes.
  Phase 2 stays 25% with partial labor/price work; next is observed facility evidence,
  saved economic pacing and runtime integration. No decisions needed for approved tuning.
  Documentation/status/progress updated; no production reset, merge or push.

### Phase 2 commodity accounting — October 8, 2026

- Built commodity reserves, explicit supply/demand flows, duration-sensitive shortage
  reports and exact conservation without Minecraft dependencies or invented production.
- Schema 14 adds explicit account initialization, saved stocks, retained period inputs
  and atomic checkpoints. Rejects repeated/skipped periods and silent policy changes.
- Eighteen added tests: Windows Wrapper clean verify passed 242 (240 unit/two packaged
  integration), all passing. Two 1,200-period crisis/recovery scenarios include saved
  mid-crisis restart; rollback/retry, overflow, immutable ordering and upgrade checks.
- Backed up confirmed disposable DB, deployed on pinned Paper, verified schema-14
  startup/diagnostics/unloaded fixture/clean stop. Original identities/families retained,
  integrity/FKs OK, zero invented economy accounts. No production/world reset.
- Phase 2 25% by one of four reporting milestones; live economy remains pending.
  User deferred personal playtest. Stopping at economic tuning approval before choosing
  consumption/cadence/price rules; no gameplay economy defaults silently selected.
  Feature work local, no merge/push; canonical roadmap unchanged.

### Phase 1 live population gate — October 8, 2026

- Implemented approved managed-villager zombie conversion prevention. Paper verified
  infection canceled and lethal damage still saved the resident's actual death.
- Wired names/age locks, safe nearby births/arrivals, household migration and simulated
  death removal. Native breeding coordinates with the saved calendar/families.
- Fixed accepted-save shutdown draining, enrollment/death publication race and the
  activation-before-unfreeze restart window. Added persistent freeze marker, retry
  backoff and editable nearby/day/night reconciliation budgets. No new schema beyond 13.
- Wrapper clean verify: 224 tests (222 unit, two integration), zero failures/errors/skips;
  nine new automated cases. Real Paper probe passed safe-placement deferral, names,
  babies/adulthood, restart/pending/freeze recovery, native breeding/infection/death,
  five-person migration, simulated death and enrollment/death race. Test code excluded
  from production JAR; normal plugin-only restart/commands/unloaded checks also passed.
- Phase 1 now 10/10 (100% of prototype milestones). Human client/balance/scale checks
  and vanilla trade-state preservation on recreation remain explicit limits; use a
  disposable play world. No merge/push/main/production/world deletion/roadmap change.
- Final deployment review found an older duplicate Civilizations JAR in the disposable
  server. Archived it outside plugins, confirmed the remaining JAR matches the build,
  and repeated the complete Paper probe and ordinary plugin-only restart successfully.

### Resident projection preparation — October 8, 2026

- Schema 13 current incarnations, retained aliases, durable spawn claims, actual-death
  history and immutable owner views. Deterministic reconciliation handles unloaded,
  pending, moved, deceased and ambiguous identities; actual Paper mutation remains pending.
- Fixed stale settlement population after demographic changes, including emptied
  source towns. Read-only debug explanations show saved reasons and migration paths.
- Windows wrapper clean verify: 215 tests (213 unit, two integration), all passing;
  eighteen added cases. Disposable Paper upgrade/restart preserved identities/families,
  showed a month-45 saved arrival while its area stayed unloaded. Integrity/FKs passed,
  backup taken first, server stopped. No production/main/remote changes.
- Phase 1 remains 90%. Zombie conversion/curing choice pending before native lifecycle
  integration; no consequential rule silently chosen. See docs/RESIDENT_PROJECTION.md.

### Work qualification foundation — October 7, 2026

- Schema v9 stores resident skill totals across restarts, migration, death and backups.
  Added immutable job requirements and explainable living/age/skill qualification checks.
- No fabricated employers, hiring, salaries, occupations or learning rates. This is
  the Phase 1 qualification base for later real labor/business systems.
- Windows wrapper clean verify passed 117 cases (115 unit, two integration), including
  skill snapshots/upgrade failure/retry and migrant skill retention.
- Phase 1 7/10 (70%); calendar and Minecraft integration/validation remain. No actual
  DB/world/server changes, merge or push. Centralized temporary prior-schema fixtures.

### Saved migration — October 7, 2026

- Added deterministic household movement driven by observed housing pressure and
  occasional external adults. Families move together; identities and family links stay.
- Schema v8 preserves event sequences and records migration origins. Replay 3 guards
  algorithm/settings changes. All changes/progress/events share one transaction.
- Windows wrapper clean verify passed 107 cases (105 unit, two integration), including
  restored century replay/accounting, whole-family rollback and migration-then-death.
- Phase 1 now 6/10 (60% by checklist); jobs, runtime calendar and Minecraft integration
  plus real-server validation remain. No actual DB/world/server changes, merge or push.

### Atomic demographic persistence — October 7, 2026

- Schema v7 saves monthly clock/settings, observed housing capacity and sequenced
  birth/death explanations. People/links/events/progress commit together or roll back.
- Settings and replay protection prevent silent changes; age-only/global engines
  cannot activate together. Event paging retains every event within a shared month.
- Windows wrapper clean verify passed 94 cases (92 unit, two integration), including
  snapshot-restored century replay across batches and event-write rollback/retry.
- Phase 1 advances to 5/10 (50%): saved headless aging/birth/death verified. Minecraft
  import/projection, jobs, migration, calendar scheduling and server validation remain.
- No actual server DB/world changes, merge or push. Global snapshot cost still needs
  measurement before tick-loop integration.

### Headless births and deaths — October 7, 2026

- Added immutable population snapshots, editable prototype monthly demographic
  policy, eligible housing-constrained births and age-dependent deaths.
- Seeded newborn identities and parent links; structured reasons; deceased records
  retained. Missing capacity blocks births; impossible close-relative births rejected.
- Windows wrapper clean verify passed 85 cases (83 unit, two integration), with
  100-year accounting and replay scenarios. This is not a balanced live simulation.
- Phase 1 remains 40%; demographic saving/runtime integration next. No server/world
  or actual DB changes, merge or push. Settings follow user's prototype authorization.

### Saved logical aging — October 7, 2026

- Added pure immutable resident aging and opt-in schema v6 seed/progress clocks.
  Ages/checkpoints commit together; completed targets never double-age, failures retry.
- Windows wrapper clean verify passed 76 cases (74 unit, two integration), including
  restart/snapshot, write/overflow rollback and a 500-person/1,000-step pure replay.
- Phase 1 remains 40%; aging/catch-up groundwork advances, full demographic and
  calendar/runtime milestones still pending. No server/world/actual DB operations.
- User clarified automatic village participation and approved configurable,
  documented experimental population tuning. No manual join step will be required.

### Family memory — October 7, 2026

- Added saved parent/child and canonical partner relationships; shared parents
  identify siblings across households/settlements. Deceased relatives remain linked.
- Schema v5 protects identities and prevents self-links, duplicate/reversed partners,
  ancestry cycles and in-place overwrites. No demographic or marriage policy chosen.
- Windows wrapper clean verify passed 64 cases (62 unit, two integration), including
  family restart/snapshot preservation and v4 upgrade rollback/retry.
- Phase 1 remains 40%: family portion advanced; jobs and in-game integration pending.
- No actual Minecraft/server database changes, merge or push.

A running record of Civilizations development, major decisions, experiments, successes, and spectacular failures.

---

## 2026-09-27 — Day 1: It Begins

Development of Civilizations officially started.

### The Idea

Make vanilla Minecraft villages feel alive.

Instead of villages remaining static forever, settlements will grow and evolve based on their population, resources, safety, economy, environment, and relationships with other settlements.

### Early Design Decisions

- Civilizations will remain separate from Chronicler/T.O.D.D.
- Built for PaperMC 26.2.
- SQLite will provide persistent settlement data.
- Autonomous construction is an early priority.
- Buildings will be constructed gradually rather than appearing instantly.
- Roads will connect new development.
- Buildings will have functional effects on the settlement simulation.
- Player-created structures must be protected from autonomous development.
- Eventually, settlements will trade, specialize, form relationships, and connect into regional networks.

### First Major Goal

A vanilla village recognizes that it needs additional housing, chooses an appropriate location, and autonomously constructs its first house over time.

If that works, Civilizations is alive.

### Project Foundation

- Created the Maven project using Java 25.
- Initialized Git and published the repository to GitHub.
- Added README and development log.
- Configured the Paper API.
- Paper API pinned to `26.2.build.129-stable` to match the server.
- Confirmed the project successfully builds with Maven.

**Status:** BUILD SUCCESS
### First Successful Server Launch

Civilizations was packaged and installed on a local Paper test server.

The plugin loaded successfully and reported:

> Civilizations is awakening...

Civilizations is officially running inside Minecraft.

### First Settlement Discovered

Civilizations successfully detected its first vanilla settlement in-game.

The initial prototype detects nearby HOME POIs when a player crosses a chunk boundary and announces a newly discovered settlement.

This is intentionally a temporary detection method. Future settlement identification will use multiple signals such as villagers, beds, job sites, bells, and POI clustering.

**Milestone:** Civilizations can now recognize settlement activity in the Minecraft world.

### Stonevale — First Persistent Settlement

The first permanently recognized settlement was created: **Stonevale**.

Civilizations now:
- Generates settlement identities and names.
- Stores settlements persistently in SQLite.
- Records their world, center coordinates, UUID, and discovery time.
- Searches for existing nearby settlements before creating new ones.
- Preserves settlement identity across server restarts.

Stonevale survived its first restart and was correctly recognized by name.

### Stonevale Gets a Census

Civilizations can now track settlement population.

Population was added to the persistent settlement model and SQLite database, including a migration path for existing databases.

When a known settlement is encountered, Civilizations:
- Counts nearby villagers.
- Updates the settlement's population.
- Saves the census to SQLite.
- Displays the current population to the player.

Stonevale's first recorded census reported **11 residents**.

The current census uses nearby villager entities and is intentionally approximate. Future residency logic will associate villagers more precisely with settlement boundaries and POIs.

### Next Goal

Give settlements awareness of their housing capacity.

Once Civilizations understands both **population** and **available beds**, Stonevale can identify a housing shortage — the first step toward autonomous construction.

### Stonevale Develops Needs

Civilizations can now evaluate basic settlement housing needs.

Settlements track:
- Population
- Available beds
- Housing status

Stonevale's first housing census recorded:
- Population: 11
- Beds: 13
- Housing: ADEQUATE

A new Needs Engine compares population against housing capacity. When population meets or exceeds available beds, the settlement identifies a `HOUSING_SHORTAGE`.

This establishes the first autonomous decision-making system in Civilizations.

### Next Goal

Turn a housing shortage into action:

`HOUSING_SHORTAGE → Construction Request → Site Selection → House Construction`

This will be the beginning of autonomous settlement development.

## 2026-10-07 — Reproducible persistence foundation

- Fixed the settlement INSERT to persist all nine model fields, including beds.
- Added the official Maven Wrapper pinned to Maven 3.9.11, JUnit 5 and pinned build plugins.
- Six temporary-SQLite tests cover initialization, insertion, restart persistence, retrieval and isolated census updates.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed: six tests, zero failures/errors/skips.
- Read and verified AGENTS.md and all six canonical design documents. The nine-phase roadmap supersedes the historical construction-next goal above: foundation and persistent population come first.
- Verified both wrapper scripts exist and are not hidden in the OneDrive checkout. Recommend keeping active Git and live server databases outside cloud sync; no relocation or live database changes performed.
- Next engineering milestone: explicit transactional schema migrations with fresh-install, legacy-upgrade and failure/recovery tests. Gameplay semantics remain open as documented.

### Transactional schema migrations

- Added ordered SQLite user-version migrations for base settlement identity, population and beds. Existing prototype rows are preserved rather than reset.
- Removed exception-swallowing ALTER logic; upgrades commit atomically and roll back on failure. Unsupported newer schemas fail clearly, and initialization closes failed connections.
- Added ten migration cases using temporary DBs: fresh/reopen, legacy and incremental upgrades, rollback/recovery, malformed/newer schema rejection and connection cleanup.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed all 16 tests with zero failures/errors/skips. No Paper smoke test or live database upgrade performed.
- Next milestone: CI and backup/development-environment procedures, before simulation gameplay work.

### CI, SQLite snapshots and recovery procedures

- Added Windows/Linux JDK 25 wrapper CI with pinned official action commits, read-only permissions and no deployment. actionlint 1.7.7 passed; hosted CI has not run on this local branch.
- Added explicit cross-platform wrapper line endings and Git ignores for local dev servers, backup directories and Git bundles.
- Built a standalone read-only SQLite snapshot tool using VACUUM INTO and integrity checks, with refusal to overwrite destinations. It never initializes/migrates/restores the source DB.
- Added seven temporary-database backup tests and one packaged-JAR CLI integration test via Maven Failsafe. Normal and empty-cache Windows wrapper `clean verify` builds passed all 24 cases.
- Documented exact Paper build/checksum setup, localhost isolation, source/world/config/SQLite backup procedures and restoration into a new disposable directory.
- No live database/world/server operation or Paper smoke test performed. No merge or push. Phase 1 continues with headless simulation/debug tooling and persistent population foundations; consequential discovery/occupancy/time rules remain open.

### Deterministic headless simulation runner

- Added an immutable-checkpoint stepping API with explicit seeds, logical targets, per-call step budgets and remaining work. Random draws are stable across batching and reconstructed checkpoints.
- Added a packaged synthetic checksum harness without Paper/world/database/time access. It is a diagnostic probe, not implemented population gameplay.
- Fourteen runner unit cases cover replay, bounded work, failure retry, ordering, long boundaries, invalid inputs and one-million-step acceleration; one packaged-harness case checks separate JVM runs across budgets.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 39 cases (37 unit, two integration), zero failures/errors/skips. A fixed probe fixture guards the replay convention.
- Documented pure immutable step requirements, CPU-budget limitations, CLI usage and outstanding persistence/versioning needs. Schema unchanged; no world/server/production DB changes.
- Next: persistent resident/household domain and storage. Birth/death/migration rates, calendar pacing and discovery/seeding policy are not silently chosen.

### Plain-English roadmap tracker

- Added docs/PROGRESS.md with bars for every phase and checklists explaining the eventual Minecraft experience.
- Initial reporting snapshot: three of ten Phase 1 milestones complete; later phases have no completed reporting milestones. Percentages count milestones, not hours or deadlines.
- Added an AGENTS.md maintenance instruction and linked the tracker from README. Canonical roadmap requirements and gameplay behavior are unchanged.
- Reviewed tracker against ROADMAP and verified PROJECT_STATUS. Documentation-only change; no build/test rerun needed.

### Persistent residents and household identities

- Added immutable Resident/Household records and a focused population repository. People retain stable identities, names, logical age, household assignment and living/deceased status across restarts.
- Schema v4 adds indexed population tables with foreign key/age/status constraints. Household member batches commit atomically; duplicate/missing/mismatched identities fail, and failed batches can retry.
- Existing settlement rows/census preserved; no resets, automatic villagers-to-residents conversion, live DB operations or new gameplay tuning.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 58 cases (56 unit, two integration), including restart, constraints, rollback/retry, v3 upgrade/recovery and population backup round-trip coverage.
- Phase 1 advances to four of ten reporting milestones complete (40% by checklist). Household storage is present; family ties/jobs and live gameplay integration remain pending. Docs/POPULATION.md records limitations.

## October 7, 2026 — disposable Paper startup and recovery

- User accepted the EULA for the isolated localhost test. Guarded launcher checks
  the disposable marker, pinned Paper checksum, Java 25 and isolation properties.
- Paper 26.2-129 enabled schema-9 Civilizations, restarted, and ran a restored
  source/world/config/SQLite copy. Clean shutdown verified after each run.
- Verified Git bundle, 68 world/config file hashes, SQLite integrity and restored
  source wrapper build (117 passing tests). No production data or main/remote changed.
- Incomplete Minecraft download retried with exact required checksum; non-fatal
  Windows performance-counter error documented without OS changes.
- Phase 1 remains 70%; hosted CI and real population integration remain pending.

## October 7, 2026 — saved civilization calendar

- Schema 10 adds elapsed-time anchor, partial month and durable queued target.
  Population catch-up can fail/restart without losing elapsed time or double-aging.
- Pure year/month conversion, backward-clock handling, explicit elapsed caps and
  overflow failure; runtime scheduling remains pending, so progress stays 70%.
- Fourteen new tests; Windows wrapper clean verify passed 131 cases (129 unit,
  two integration), zero failures/errors/skips. Temporary databases only.
- User approved automatic participation for natural and player-built populated
  villages. No merge/push, production database or world modification.

## October 7, 2026 — automatic village observation foundation

- Added immutable world/bed/villager snapshots and pure bounded cluster recognition,
  including deterministic nearest-bed assignment and partial-observation flags.
- No fixed grid/player position identity, duplicate counting, bed-only enrollment
  or silent oversized scan truncation. Existing runtime listener remains unchanged.
- Seven new tests; Windows wrapper clean verify passed 138 cases (136 unit,
  two integration). Phase 1 remains 70% pending Minecraft integration.
- User approved configurable adult partner formation without invented child ancestry.
  No new server/world/production DB changes, merge or push.

## October 7, 2026 — approved adult partnership prototype

- Explicit pure configurable adult pairing, with known-blood-relative exclusion,
  living-partner exclusivity and retained deceased relationship history.
- Existing child ancestry remains unknown. Seven tests include repeatability,
  eligibility, historical partners and birth integration. Wrapper clean verify:
  145 passing cases (143 unit, two integration), zero failures/errors/skips.
- Not yet wired into import/monthly simulation; existing replay/schema unchanged.
- User approved world UUID and saved bed/villager matching; ambiguous town overlaps
  must produce diagnostics, not an automatic merge. Phase 1 stays 70%.

## October 7, 2026 — permanent village identity storage

- Schema 11 saves world UUID/display-name evidence and immutable-owner settlement,
  bed and entity/resident links. Matching reports new/existing/conflicting evidence.
- No implicit merge, legacy adoption, census overwrite or absent-data deletion.
  Migration respects permanent world IDs rather than duplicate/renamed folders.
- Nine new temporary-DB tests; wrapper clean verify passed 154 cases (152 unit,
  two integration), zero failures/errors/skips. Runtime integration remains pending.
- No actual DB/world/server change, merge or push; Phase 1 remains 70%.

## October 7, 2026 — schema-11 Paper follow-up

- 154-test JAR from 62550ee enabled on the authorized disposable Paper server.
  Test database upgraded from schema 9 to 11; read-only integrity check ok.
- Server reached Done and stopped cleanly. No production data changed.
- Reachable tracked history checked for common credential/private-key signatures;
  no matches. Hosted CI/publication still pending; gameplay integration still upcoming.

## October 8, 2026 — atomic enrollment and hosted CI

- Schema 12/atomic enrollment imports stable people, shared homes, housing evidence,
  approved prototype partners and headcount. Repeat visits never overwrite saved ages
  or resurrect deceased people. Partial observations invalidate housing confidence.
- Eleven new tests include rollback/retry, explicit legacy/overlap protection and
  calendar-driven saved births. Wrapper clean verify passed 165 cases (163 unit,
  two integration), zero failures/errors/skips; Paper runtime integration remains.
- With explicit approval, pushed only e85416e on codex/phase1-foundation-ci. Hosted
  Windows/Linux both passed run 37725399200. Enrollment remains local; main unchanged.
- CI/Paper/recovery milestone complete: Phase 1 8/10 (80%). No merge or production change.

## October 8, 2026 — dedicated population owner and timer

- One thread owns JDBC/imports and a real fixed-delay monthly catch-up timer.
  Immutable snapshots, documented editable test pacing/offline cap, bounded request
  queue and explicit failure/backlog diagnostics; no Bukkit calls.
- Historical catch-up precedes new enrollment; lifecycle/callback shutdown avoids
  waiting on itself. Ten new temporary-DB tests, including timer and write retry.
- Wrapper clean verify passed 175 cases (173 unit, two integration). Plugin wiring
  remains upcoming; Phase 1 stays 80%. No new server/real data/remote/main changes.

## October 8, 2026 — Paper calendar and inspection

- Wired asynchronous owner-thread startup/shutdown, strict configurable prototype
  inputs and read-only admin status/settlement commands. Old census listener retained
  but unregistered; observer/import/projections remain pending.
- Thirteen new configuration/permission/diagnostic cases; wrapper clean verify passed
  188 tests (186 unit, two integration), no failures/errors/skips. Initial sandbox
  target resolution denied; expanded-access verification passed.
- Disposable DB snapshot before deployment; pinned Paper startup, real month advance,
  commands, clean stop/restart at saved month 1, then month 3 verified. No player or
  projection test yet; no production/main/remote change. Phase 1 now 9/10 (90%).

## October 8, 2026 — automatic loaded-world village import

- Bounded main-thread bed snapshots and loaded-entity reads automatically feed the
  existing identity/enrollment owner. Clear-border/loaded coverage checks distinguish
  known housing from partial evidence; no chunk tickets or world/entity modifications.
- Read-only observation diagnostics and configurable adapter budgets. Caught/fixed
  pinned Paper negative-height section indexing during the first real smoke test;
  added regression coverage. Nine new cases; clean verify passed 197 tests
  (195 unit, two integration), no failures/errors/skips.
- Disposable Paper fixture imported two people/one household/one settlement, capacity
  four. Re-observation/restart retained IDs and links; saved ages advanced exactly
  eight months while clock progressed 27 to 35. Unloaded area stayed unloaded while
  simulation continued. Schema 12/integrity/FK checks passed; server stopped and
  manually applied fixture tickets removed. No production/main/remote changes.
- Phase 1 remains 90%; visible resident projection and vanilla lifecycle coordination
  are the next batch. Large-server profiling and actual client play remain pending.

## October 8, 2026 — live field-backed food economy

- Saved independent food clock and transactional period effects now use actual saved
  living residents plus loaded crop snapshots near household homes. No harvesting,
  player-block changes, invented farms, business jobs or chunk force-loading.
- Schema 15 retains evidence/clocks and forward tuning history, backfills original
  period policies and preserves old explanations. Partial/conflicting evidence pauses
  accounting. Offline villages use their last reliable observed capacity.
- Read-only economy diagnostics; commented farm settings with old-file compatibility.
  Added 13 automated cases. Windows Wrapper clean verify passed 274 cases (272 unit,
  two packaged integration), zero failures/errors/skips.
- Consistent disposable schema-14 backup before deployment. Guarded real Paper probe
  passed actual field supply, food depletion/severe shortage, forward feast recovery,
  restart person/history preservation and accounting while chunks remained unloaded.
  Probe archived outside plugins afterward; normal startup/shutdown also verified.
- Phase 2 remains 25% by completed reporting groups. Treasury/wellbeing, autonomous
  decisions and player transactions remain; no starvation deaths. Current demographic
  checkpoint is used during food catch-up, not exact historical interleaving. Large
  city profiling and human client play remain outstanding. No push/merge/main changes.

## October 8, 2026 — household needs and explicit treasury accounting

- Food periods now save household demand/age counts, known/unknown/crowded housing
  and unassigned living residents atomically with stocks/history/checkpoint. Food
  coverage remains shared village evidence, not invented family rations/prosperity.
- Transactional schema 16 preserves old data without fabricating past needs. Added
  exact Trade Value treasury accounts/ordered ledger: explicit sources, operation-ID
  retry protection, no negative funds or guessed live income/personal wallets.
- Fourteen added automated cases; Windows Wrapper clean verify passed 288 cases
  (286 unit/two integration), no failures/errors/skips. Long 1,200-period crisis/
  recovery replay, historic evidence, failed-save rollback/retry and migration tested.
- Real guarded Paper probe passed household diagnostics and preserved needs during
  restart/feast retuning alongside actual field/food/unloaded checks. Dev schema-15
  snapshot first; identities/families preserved, SQLite integrity and FKs verified.
- Status/progress/docs updated. Phase 2 remains 25%: broader wellbeing and live budget
  flows, autonomous responses and player trade remain. No demographic penalties,
  production save reset, remote push/merge or main modification.

## October 8, 2026 — explainable village priorities

- Pure evidence-to-priority selection now chooses seek food, investigate observed
  crowding or monitor. Feasibility is mandatory; seeded weighted draws choose among
  reasonable alternatives. Editable cadence/weights live in an optional economy
  section; old owner files remain untouched and compatible.
- Schema 17 retains facts/settings/seed for exact explanation replay, transactionally
  with food and needs. Changed pressures trigger early reviews; recovery clears crisis
  focus. No real purchases, supplier invention, project permissions or block changes.
- Twelve added cases; Wrapper clean verify passed 300 cases (298 unit/two integration),
  zero failures/errors/skips. Seed sweeps, alternative selection, long replay, rollback,
  upgrade failure/retry and original treasury balances/receipts tested.
- Guarded disposable Paper probe passed crisis priority, saved restart identity/history
  and feast recovery to monitoring, alongside actual food/household/unloaded checks.
  Consistent schema-16 dev backup first; original identity/family data preserved.
- Updated plain-English tracker with partial milestones for all four P2 groups. Phase
  gate still partial: execution/outcome feedback, full wellbeing, actual budget flows
  and player trade remain. No production reset, remote push, merge or main change.
