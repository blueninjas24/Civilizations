# Civilizations Development Log

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
