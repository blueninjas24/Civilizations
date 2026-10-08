# Civilizations — implementation status and handoff

**Verified against local source, Git and wrapper build on October 7, 2026.** Canonical docs agree on the nine-phase dependency order; roadmap dates are estimates, not completion claims. Phase 1 is in progress.

## Initial audit
Prototype contained six Java source files: `Civilizations`, `DatabaseManager`, `VillageDiscoveryListener`, `Settlement`, `SettlementNameGenerator`, `NeedsEngine`. Discovery via player chunk movement/HOME POIs, simple settlement persistence, population/bed census and binary housing shortage. No actual economy, business, roads, physical construction, history or simulation yet.

Audit identified: malformed INSERT; player-centered settlement/census; global permanent region suppression; ambiguous nearby lookup; new settlements not immediately censused; silent migration errors; synchronous DB/world work in movement listener; no tests or Maven Wrapper initially.

## Verified persistence/build foundation
- Foundation work prepared on `foundation/persistence-build`; local commits are now authorized. No merge or push to main is authorized.
- Added Maven Wrapper pinned 3.9.11, JUnit 5 and test class `DatabaseManagerTest`.
- Fixed INSERT to persist all nine model fields, including beds; added isolated test constructor.
- Updated `pom.xml`, `.gitignore`, README.
- `.\mvnw.cmd clean verify -B -ntp` passed on Windows/Java 25: six tests, zero failures/errors/skips. Earlier `clean package` also passed; packaged JAR shaded SQLite and excluded test/JUnit/Paper API.
- Outstanding reported warnings: Maven/SQLite Java 25 and Shade manifest overlap. Discovery and migration behavior not changed.
- Both wrappers are present at the repository root, readable and not hidden: `mvnw` (11,790 bytes), `mvnw.cmd` (8,481 bytes); Maven 3.9.11 is pinned in `.mvn/wrapper/maven-wrapper.properties`.
- Checkout is under `C:\Users\noahw\OneDrive\Desktop\IdeaProjects\Civilizations`; wrapper files have OneDrive reparse attributes. Older compiler records referenced a different checkout path. Explorer visibility itself has not been inspected.
- Prefer a local checkout and server outside cloud sync. `.gitignore` does not exclude files from OneDrive. Cloud sync is not an atomic backup of Git metadata or an open SQLite DB. No relocation or server-data reset has been performed.
- No Paper server smoke test has been run for this foundation; headless persistence tests are verified.

## Immediate next work
1. Add job/skill eligibility foundation, calendar scheduling and automatic village import/projections; preserve open identity/safety rules.
2. Complete a disposable Paper smoke test and recovery drill when an exact server JAR and approved local setup are available.
3. Resolve settlement recognition/identity and census semantics before changing discovery or seeding authoritative residents.

## Transactional schema migration milestone
- Work continues on `foundation/schema-migrations`, branched from the committed persistence/build foundation and canonical documents. Main remains unchanged; nothing pushed or merged.
- `SchemaMigrator` uses SQLite `PRAGMA user_version`: 1 = base settlement identity, 2 = population, 3 = beds. All pending DDL and version markers share one transaction.
- Unversioned prototype databases are adopted by inspecting columns, preserving existing rows and adding only missing census columns with zero defaults. No reset is required.
- Newer schema versions and missing required columns fail explicitly. Failed initialization closes the connection. No arbitrary migration exceptions are swallowed.
- Ten migration test cases plus the six persistence tests pass with Windows `.\mvnw.cmd clean verify -B -ntp`: fresh install/reopen, three legacy shapes, incremental upgrades, rollback/recovery, newer-version rejection, malformed schema and connection cleanup.
- Tests use only temporary SQLite files. Actual server DBs were not opened or upgraded. No gameplay behavior, world identity, discovery or housing rule was redesigned.
- Migration limits: no migration smoke test on Paper yet; column presence is checked, not exhaustive validation of legacy constraints/data types. Schema steps are intentionally small and internal, without a migration framework. Standalone snapshot tooling was added in the subsequent milestone below.

## CI, backups and development procedures milestone
- CI/backup milestone branch: `foundation/ci-backups`, based on the schema-migration branch. No merge or push performed; main is untouched.
- GitHub Actions workflow verifies Windows and Linux with JDK 25 and the Maven Wrapper. Official actions are pinned to commit IDs; permissions are read-only, checkout credentials are not retained, and no publishing/deployment runs.
- `.gitattributes` explicitly keeps the Unix wrapper LF and the Windows wrapper CRLF. No broad source renormalization performed.
- Workflow validated with actionlint 1.7.7 (shellcheck/pyflakes disabled). Hosted Windows/Linux CI has not run because the branch remains local.
- Standalone `DatabaseBackup` uses a separate read-only SQLite connection, `VACUUM INTO`, integrity checking and a private staging file. It refuses existing destinations and never initializes/migrates/restores the source. No in-game backup command or scheduler yet.
- Seven new backup tests cover committed WAL data, exclusion of uncommitted writes, independent restored content, schema version retention, source/destination protection, missing paths and corrupt-source cleanup. A Failsafe integration test launches the CLI from only the packaged JAR.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 23 unit cases plus one packaged-JAR integration case. A second `clean verify` with a new empty temporary Maven repository also passed all 24 cases; dependency resolution does not rely on the old IntelliJ cache.
- `docs/DEVELOPMENT.md` records exact-build/JDK/checksum requirements for a disposable localhost Paper environment, source bundles, stopped-server world/config backups, consistent SQLite snapshots and restore drills.
- No actual Paper server created or launched, EULA accepted, live DB backed up, worlds copied, production data changed or checkout moved. Recovery procedures are documented, not claimed as a completed real-world drill.
- Schema remains version 3. Prototype discovery/population/housing rules remain unchanged; persistent residents/households, simulation clock and materialization are still pending. Maven/SQLite Java 25 and Shade manifest warnings remain non-failing.

## Deterministic headless runner milestone
- Headless runner milestone branch: `foundation/simulation-harness`, based on `foundation/ci-backups`.
- Added plain-Java `SimulationCheckpoint`, `SimulationRunner` and `SimulationHarness`, with no Paper/SQL/world/time dependencies in the simulation package.
- Explicit seed and completed-step checkpoints, deterministic per-step randomness, caller-supplied targets/budgets, and explicit remaining catch-up work. Pure immutable steps can resume or retry without hidden PRNG state.
- Packaged CLI runs a labeled synthetic checksum probe only; it does not simulate residents or convert logical steps to years. No in-game command, scheduler or persistent simulation checkpoint yet.
- Fourteen runner unit cases and one new packaged-harness case pass. Full Windows `.\mvnw.cmd clean verify -B -ntp`: 37 unit cases plus two integration cases, zero failures/errors/skips.
- Tests include batch-size independence, checkpoint reconstruction, failure retry, step ordering, invalid inputs, the long boundary, a fixed replay fixture and repeatable one-million-step probes. This is not real multi-century population simulation or a Paper performance guarantee.
- `docs/SIMULATION_HARNESS.md` documents immutable/pure steps, stable ordering, random replay conventions, bounded-step versus CPU-time limits and exact CLI usage.
- Schema remains version 3; no live database/world/server changes. Household capacity, time scale, demographic rates, authoritative seeding and recognition remain open. Paper smoke testing and hosted CI still pending.

## Persistent people and households milestone
- Resident milestone branch: `population/resident-storage`, based on the progress-tracker branch. Main untouched; no merge or push.
- Plain-Java immutable Resident/Household records save UUID identity, names, settlement/household membership, logical age and living/deceased status. No calendar ratios or demographic probabilities chosen.
- Transactional schema version 4 adds population tables/indexes while preserving legacy settlements/census. SQLite foreign keys enforce settlement/household references, including same-settlement household membership; protected referenced rows do not cascade away.
- PopulationRepository supports stable immutable reads, insertion, resident updates, living counts and atomic household/member insertion. Duplicates, mismatched references, unknown updates, implicit transfers and nested batch transactions fail explicitly.
- Deceased people remain in stored history-ready records but are excluded from living counts. Families, jobs, skills/prosperity, real homes and simulated demographic events remain pending.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 56 unit cases plus two packaged-JAR integration cases (58 total), zero failures/errors/skips. New tests cover persistence/restart, constraints, atomic rollback/retry, protected identities, schema-v3 upgrades/failure recovery, and population snapshots.
- All tests use temporary SQLite DBs; no production/server data, villagers or worlds changed. No Paper smoke test of schema v4 yet. Existing listener census remains separate and unchanged; no authoritative residents automatically seeded.
- Phase 1 tracker is four of ten reporting milestones complete (40% by milestone count, not time). See docs/POPULATION.md for boundary contracts and pending gameplay gates.

## Family memory milestone
- Family milestone branch: `codex/population-family-links`, based on resident storage. Schema v5 adds parent/child and canonical partner links; siblings derive from shared parents.
- Constraints reject missing people, self-links, reversed duplicates, ancestry cycles and in-place overwrites. Links survive cross-settlement residence, death, restarts and snapshots.
- Six new temporary-database tests pass, including v4 migration failure/retry. Full Windows `.\mvnw.cmd clean verify -B -ntp`: 62 unit and two integration cases (64 total), no failures/errors/skips.
- No family generation, parent/partner counts, marriage lifecycle or age/rate policies introduced. No Paper test, live DB change, merge or push. Phase 1 remains 4/10 (40%); jobs still needed for the household/family/job milestone.

## Saved logical aging milestone
- Current branch: `codex/population-saved-aging`. Schema v6 adds explicit age-only seed/progress/replay-version clocks; existing settlements, residents and family records preserved.
- Pure immutable aging and transactional persistence resume within caller-provided step budgets. Completed targets do not double-age; failure rolls back progress and all person updates. Deceased people retain ages and family links.
- Windows `.\mvnw.cmd clean verify -B -ntp` passed 74 unit plus two integration cases (76 total), no failures/errors/skips. Tests include restart, snapshots, write/overflow rollback, v5 upgrade recovery and a repeated 500-person/1,000-step pure probe.
- No real-server scheduler or births/deaths/migration yet. Logical age-only replay is version 1 and not silently repurposed as a calendar/demographic engine. Phase 1 remains 40%; partial work advanced two milestone groups.
- User clarified automatic village participation and authorized configurable prototype population settings. See docs/POPULATION_SIMULATION.md and the decisions log. No actual world/server DB changes, merge or push.

## Headless birth/death prototype
- Demographic domain milestone branch: `codex/population-demographics`. Pure immutable monthly population step emits birth/death explanations, retains deceased people and uses observed household capacity.
- User-approved editable prototype policy documented in POPULATION_SIMULATION.md; no closed numeric canon. No automatic partnership formation, crisis modifiers, demographic persistence or scheduler yet.
- Nine new tests, including two 100-year (1,200-month) accounting/replay scenarios. Windows `.\mvnw.cmd clean verify -B -ntp`: 83 unit plus two integration (85 total), no failures/errors/skips.
- Phase 1 remains 40% pending demographic persistence and remaining milestone gates. No server/world/actual database changes, merge or push.

## Atomic demographic saving milestone
- Demographic storage milestone branch: `codex/population-demographic-storage`. Schema v7 adds a global monthly clock, capacity observations and sequenced birth/death events. Additive upgrades preserve existing people/family/settlements.
- People, newborn identities, parent links, event reasons and progress commit together. Retry/restart/snapshot replay preserves outcomes; settings/replay changes and mixed aging/demographic clocks fail explicitly.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 92 unit and two integration (94 total), no failures/errors/skips. Tests include a snapshot-restored 100-year run across batch sizes, failure after newborn/link writes and safe event pagination.
- Phase 1: 5/10 reporting milestones (50% by count). Headless saved aging/birth/death is complete; runtime calendar/scheduler, import/projections, migration, job foundation and Paper validation remain. Global snapshot/writes require profiling before tick-loop integration.
- No actual server database/world changes, merge or push. Demographic engine remains explicit opt-in through Java APIs; original listener unchanged.

## Saved migration milestone
- Migration milestone branch: `codex/population-migration`. Schema v8 preserves/rebuilds event history with structured migration origins and external arrivals. Current demographic replay convention is 3; older clocks/settings fail explicitly rather than changing silently.
- Pure seeded migration resolves observed housing pressure, moves whole living households to vacant same-world housing and retains every identity/family link. Occasional external adults require observed space. No world or entity work.
- Seven new domain and six new storage cases. Windows `.\mvnw.cmd clean verify -B -ntp`: 105 unit plus two integration (107 total), no failures/errors/skips. Restored 100-year combined replay, every event page, rollback/retry, v7 upgrade, cross-world safety and move-then-death in a single batch verified.
- Phase 1: 6/10 (60% by reporting milestone count). Job foundation, runtime calendar/catch-up, import/projection/diagnostics and real Paper/CI/recovery validation remain.
- No actual server/world/production DB changes, merge or push. Prototype uses legacy stored world names for boundaries; stable world identity remains a discovery integration decision. No automatic partnership formation or proven long-term balance yet.

## Work qualification foundation milestone
- Current branch: `codex/population-work-foundation`. Schema v9 adds resident skill counters without changing existing population/history. Saved skill totals are idempotent and retain identity across movement/death/restarts/snapshots.
- Immutable WorkProfile/JobRequirements and explainable eligibility reject deceased, underage and unqualified candidates. No fabricated employer/occupation/hiring, salary or retirement rule; real business/labor integration remains in later phases.
- Five new domain and five new storage tests, plus migration skill-retention coverage. Windows `.\mvnw.cmd clean verify -B -ntp`: 115 unit and two integration (117 total), no failures/errors/skips.
- Phase 1 7/10 (70%): household/family/job qualification foundation complete. Calendar scheduling, automatic village import/projection/diagnostics and real Paper/CI/recovery remain.
- No server/world/actual DB changes, merge or push. See WORK_FOUNDATION.md for exact scope.

## Disposable Paper and recovery verification
- Branch `codex/paper-test-preparation`: guarded launcher validates test marker,
  pinned JAR checksum, accepted EULA, loopback properties and Java 25.
- Following explicit user EULA acceptance, exact Paper 26.2 build 129 started,
  restarted and started from a restored test-world/config/SQLite backup; all
  stopped cleanly. Plugin bd8f4ce enabled and initialized schema 9 successfully.
- Verified complete source Git bundle and archive, all 68 restored world/config
  files, SQLite integrity, and recovered-source wrapper build: 117 tests passed.
- Only new disposable temporary directories outside OneDrive were used. No
  production save, existing world, main branch, remote push or merge changed.
- See PAPER_SMOKE_TEST.md for download recovery and non-fatal Windows warnings.
  Hosted CI and integrated population playtesting remain pending. Phase 1 stays 70%.

## Saved calendar foundation
- Branch `codex/civilization-calendar`; schema 10 adds a saved elapsed-time anchor,
  fractional month and queued demographic target. Existing identities/history retained.
- Pure calendar conversion handles backward wall clocks, explicit elapsed caps and
  overflow. Queued targets survive demographic failure and bounded restart catch-up.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 129 unit and two integration tests
  (131 total), zero failures/errors/skips. Fourteen new clock cases cover restart,
  snapshots, fractional time, rollback, caps, overflow and schema-9 upgrade retry.
- Calendar APIs remain explicit opt-in; runtime scheduler/configuration and village
  import/projection are pending. Phase 1 remains 70% until the whole calendar gate passes.
- User approved automatic recognition of both natural villages and player-built
  areas containing beds and villagers. No server/world/production DB changed in
  this calendar batch; no merge or push.

## Automatic village observation foundation
- Branch `codex/village-observation`: immutable UUID-tagged observations and bounded,
  deterministic populated-bed cluster candidates. No Paper/chunk/SQL calls.
- Editable detection inputs, duplicate/budget rejection, nearest-bed ties and partial
  observation safety verified by seven new tests. Windows wrapper clean verify passed
  136 unit and two integration tests (138 total), zero failures/errors/skips.
- Runtime importer/identity matching, household observation and projections remain
  pending. Existing listener unchanged; schema 10. Phase 1 stays 70%.
- User approved configurable prototype adult partnerships; implementation upcoming.
  No new world/server/production DB changes, merge or push in this batch.

## Approved partnership prototype
- Branch `codex/population-partner-formation`: explicit pure pairing of unrelated,
  unpartnered living adults sharing a home. Enable/minimum-age inputs configurable;
  repeatable UUID order, retained deceased partners, no invented existing child ancestry.
- Seven new tests, including birth integration. Windows wrapper clean verify passed
  143 unit and two integration cases (145 total), zero failures/errors/skips.
- Import/monthly runtime integration remains pending; existing demographic replay 3
  is unchanged. No schema change or actual DB/world/server change in this batch.
- User approved permanent world UUID and saved bed/villager identity matching;
  overlapping existing towns will be diagnosed instead of automatically merged.
- Phase 1 remains 70%; no merge or push.

## Permanent village identity storage
- Branch `codex/village-identity-storage`; schema 11 adds world UUID/name evidence,
  settlement bindings, bed ownership and entity/resident links, preserving all old data.
- Repeat visits/renames match identities; conflicting town evidence is reported and
  never merged. Bindings cannot silently change owners. Legacy rows are not auto-bound.
- Migration uses bound world UUIDs and prevents same-folder-name cross-world moves;
  old unbound rows retain separated legacy boundaries.
- Nine new temporary-DB tests. Windows wrapper clean verify passed 152 unit and
  two integration cases (154 total), zero failures/errors/skips.
- Runtime importer, observer, projections, calendar scheduling and diagnostics remain
  pending. Phase 1 stays 70%; no actual DB/world/server changes, merge or push here.

## User workflow
`docs/PROGRESS.md` tracks verified roadmap milestones in plain English. Current snapshot: Phase 1 has seven of ten reporting milestones complete; Phases 2–9 have none complete. These counts are not time estimates or release-readiness percentages. Future batches update it alongside this status file.

This ChatGPT thread is product/design HQ. Codex is implementation agent. It may proceed autonomously on scoped engineering work, committing to feature branches, but must escalate consequential design decisions and destructive operations. Preserve the full nine-phase roadmap.
