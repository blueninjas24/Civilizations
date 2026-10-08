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
1. Build persistent resident/household domain and tested storage; connect it to the headless runner without guessing open tuning rules.
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
- Current branch: `foundation/simulation-harness`, based on `foundation/ci-backups`.
- Added plain-Java `SimulationCheckpoint`, `SimulationRunner` and `SimulationHarness`, with no Paper/SQL/world/time dependencies in the simulation package.
- Explicit seed and completed-step checkpoints, deterministic per-step randomness, caller-supplied targets/budgets, and explicit remaining catch-up work. Pure immutable steps can resume or retry without hidden PRNG state.
- Packaged CLI runs a labeled synthetic checksum probe only; it does not simulate residents or convert logical steps to years. No in-game command, scheduler or persistent simulation checkpoint yet.
- Fourteen runner unit cases and one new packaged-harness case pass. Full Windows `.\mvnw.cmd clean verify -B -ntp`: 37 unit cases plus two integration cases, zero failures/errors/skips.
- Tests include batch-size independence, checkpoint reconstruction, failure retry, step ordering, invalid inputs, the long boundary, a fixed replay fixture and repeatable one-million-step probes. This is not real multi-century population simulation or a Paper performance guarantee.
- `docs/SIMULATION_HARNESS.md` documents immutable/pure steps, stable ordering, random replay conventions, bounded-step versus CPU-time limits and exact CLI usage.
- Schema remains version 3; no live database/world/server changes. Household capacity, time scale, demographic rates, authoritative seeding and recognition remain open. Paper smoke testing and hosted CI still pending.

## User workflow
This ChatGPT thread is product/design HQ. Codex is implementation agent. It may proceed autonomously on scoped engineering work, committing to feature branches, but must escalate consequential design decisions and destructive operations. Preserve the full nine-phase roadmap.
