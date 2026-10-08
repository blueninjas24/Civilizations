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
1. Establish CI, consistent backups and reproducible local Paper development environment.
2. Resolve settlement recognition/identity and census semantics; separate listener observation from simulation state.
3. Implement deterministic accelerated simulation/debug tooling and persistent population domain.

## Transactional schema migration milestone
- Work continues on `foundation/schema-migrations`, branched from the committed persistence/build foundation and canonical documents. Main remains unchanged; nothing pushed or merged.
- `SchemaMigrator` uses SQLite `PRAGMA user_version`: 1 = base settlement identity, 2 = population, 3 = beds. All pending DDL and version markers share one transaction.
- Unversioned prototype databases are adopted by inspecting columns, preserving existing rows and adding only missing census columns with zero defaults. No reset is required.
- Newer schema versions and missing required columns fail explicitly. Failed initialization closes the connection. No arbitrary migration exceptions are swallowed.
- Ten migration test cases plus the six persistence tests pass with Windows `.\mvnw.cmd clean verify -B -ntp`: fresh install/reopen, three legacy shapes, incremental upgrades, rollback/recovery, newer-version rejection, malformed schema and connection cleanup.
- Tests use only temporary SQLite files. Actual server DBs were not opened or upgraded. No gameplay behavior, world identity, discovery or housing rule was redesigned.
- Known limits: no live backup tooling or migration smoke test on Paper yet; column presence is checked, not exhaustive validation of legacy constraints/data types. Schema steps are intentionally small and internal, without a migration framework.

## User workflow
This ChatGPT thread is product/design HQ. Codex is implementation agent. It may proceed autonomously on scoped engineering work, committing to feature branches, but must escalate consequential design decisions and destructive operations. Preserve the full nine-phase roadmap.
