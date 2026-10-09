# Development and recovery procedures

This guide supports Phase 1; it does not establish new gameplay rules or claim
a Paper smoke test has been completed.

## Build and CI

Use a JDK 25 and the checked-in Maven Wrapper (Maven 3.9.11):

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress clean verify
```

Linux/macOS: `sh ./mvnw --batch-mode --no-transfer-progress clean verify`.
`verify` runs unit tests and the packaged-JAR backup integration test;
`package` runs unit tests but does not run the integration test.
GitHub Actions runs `verify` on Windows and Linux with Temurin 25.
Action implementations are pinned to reviewed commit IDs, the token is read-only,
and builds do not deploy or publish. Hosted CI results require a pushed branch or PR;
do not equate local success with a hosted run. Pushing/merging main requires approval.

## Disposable local Paper environment

1. Keep the checkout, server and local backups outside OneDrive/cloud sync.
   Example directory layout: `C:\Dev\Civilizations`, `C:\DevServers\Civilizations-dev`,
   and `C:\DevBackups\Civilizations`. These are examples, not paths created by tooling.
2. Obtain the exact development Paper 26.2 build 129 server JAR through official
   Paper distribution channels. The POM's Paper API artifact is not a server JAR.
   Record the downloaded filename, source URL and `Get-FileHash <jar> -Algorithm SHA256`
   in a local environment manifest alongside `java -version` and plugin commit ID.
   Never silently substitute a newer server build.
3. Start the server in its own disposable directory using Java 25:
   `java -Xms1G -Xmx2G -jar paper.jar --nogui`. Memory values are local starting
   examples, not simulation limits. Review and accept Minecraft's EULA yourself.
4. Before exposing the server, set `server-ip=127.0.0.1`, keep `online-mode=true`,
   and choose an unused local port. Do not put real credentials or player data in Git.
5. Stop the server, copy only `target/civilizations-0.0.1-SNAPSHOT.jar` into its
   `plugins` directory, then start it. Do not install `original-*.jar` or test files.
6. Confirm database initialization and plugin enable logs. Visit a village in the
   disposable world; verify discovery, stop cleanly, restart and verify stable
   identity. Inspect logs for SQL errors. Existing discovery/census limitations still apply.
7. Record server JAR SHA256, Java version, commit, observations and failures in a
   smoke-test report. Do not mark the gate passed solely because a JAR compiled.

Official setup reference: https://docs.papermc.io/paper/getting-started/
No script here accepts the EULA, opens ports, deletes worlds, resets a DB or replaces
an existing server. Creating or using a real server remains a separate verified step.

## Consistent SQLite snapshot

The packaged JAR contains a standalone tool. It opens an existing source read-only;
it does not initialize, migrate or restore it. Use explicit absolute paths:

```powershell
java -cp .\target\civilizations-0.0.1-SNAPSHOT.jar `
  com.blueninjas24.civilizations.database.DatabaseBackup `
  'C:\DevServers\Civilizations-dev\plugins\Civilizations\civilizations.db' `
  'C:\DevBackups\Civilizations\civilizations-2026-10-07.db'
```

The destination parent directory must already exist and the destination must not
exist, even as an empty file. The tool creates a private staging file, runs SQLite
`VACUUM INTO`, checks `PRAGMA integrity_check`, then moves the completed snapshot to
the requested filename without replacement. Normal failures clean up staging files.
An interrupted process may leave a `.civilizations-backup-*.db` file; it is not a
finished backup. Backups preserve logical content/schema version, not byte layout.

This supports a live SQLite snapshot, including committed WAL data, with a bounded
lock wait. It can still fail under contention or insufficient disk space. Do not
schedule this blocking operation on Paper's main thread or share its JDBC connection
with server tasks. No in-game backup command or scheduler is implemented yet.
SQLite behavior reference: https://www.sqlite.org/lang_vacuum.html

Never back up an open SQLite database by copying only its `.db` file; WAL may contain
committed changes. Keep successful snapshots outside Git and the live server folder.
Retain multiple versions and test recovery before relying on backups.

## Source, world/config backups and recovery drill

- Commit coherent source changes on feature branches; keep an independent Git
  backup, such as `git bundle create <new-backup.bundle> --all`. A bundle contains
  committed history/refs, not ignored files or uncommitted work. Do not overwrite an
  existing bundle. Backups must not substitute for reviewing a clean working tree.
- Stop Paper cleanly before backing up the complete disposable server directory
  (worlds, configs, plugins, database and metadata). Record the commit and server
  checksum alongside it. Audit configs/player data before storing or sharing archives.
- A standalone SQLite snapshot is not a coordinated backup of Minecraft worlds.
  For consistent world-plus-database recovery, take the stopped-server backup.
- Practice restoration in a **new disposable directory**: extract the stopped-server
  archive there, verify expected files and DB integrity, launch locally with the
  recorded server/plugin versions, and confirm settlement identities.
- Replacing live files, overwriting a world, downgrading a schema or resetting a DB
  requires explicit target verification and approval. Do not automate these actions
  against production paths.

`.gitignore` is a Git rule, not a OneDrive exclusion. Moving the active checkout or
restoring server data has not been performed by this development batch.

## Manual automatic-farm native fixture

The piston extension also dispatches synthetic BlockPistonExtendEvent and
BlockPistonRetractEvent through the registered listener. It verifies all seven
fixture exclusions (placement, horizontal movement and vertical movement) on the
SQL owner after restoring blocks. These synthetic events do not drive actual piston
physics or validate another plugin's event ordering. No piston or neighboring block
is changed by these events; the ore/air fixture remains the only temporary edits.
Native acceptance and the unchanged-schema restart must be recorded after the run.

The live-tracking extension dispatches a synthetic BlockPlaceEvent through the
registered Civilizations listener and verifies the resulting sticky exclusion on
the SQL owner. It uses a dummy Player with no online client/inventory; no player
identity is saved. Reflection in the test-only probe locates the private session.
Unlike the earlier snapshot-only fixture, this leaves one legitimate test-event
placement record in the marked dev database, while restoring all world blocks.
After archiving the probe, verify a normal restart reports healthy capture and
no forced chunks, then stop normally. See LIVE_ORE_TRACKING.md for the clean-stop
fence and incomplete piston/editor coverage. Never deploy this probe to a live server.

The latest WorkshopProductionProbe also calls ExposedOreProbeSupport after its
marked disposable-server guard. It waits for a loaded chunk; on the empty server,
use console `execute in minecraft:overworld run forceload add 0 0` only in this
verified disposable environment, and remove that ticket immediately after the
check with `execute in minecraft:overworld run forceload remove 0 0`. Compile both
test source files together. Six temporary ore/air blocks are restored in finally
and checked before the captured snapshot is scanned. Production scans do not force
chunks or edit blocks. The separate probe DB tests persistent placement exclusions,
stale-survey rejection and unchanged finite reserves/claims after restart. No live
placement listener was activated by the original provenance batch. The live capture
extension above is now registered, but automatic mine discovery remains disabled.
See RESOURCE_PROVENANCE.md and LIVE_ORE_TRACKING.md.

WorkshopProductionProbe under the same test-only directory is a separate guarded
plugin, main/name WorkshopProductionProbe/CivilizationsWorkshopProbe with the same
API version and Civilizations dependency. Compile it against binary dependency JARs
(exclude sources/javadoc JARs, set empty sourcepath). It uses its own randomly named
database and dedicated SQL owner, explicit synthetic industries/sites/deposits and
saved ore coordinates. The latest mineral identity extension checks overlapping
evidence without adding reserves, including restart (see MINERAL_IDENTITY.md). The
synthetic survey is not natural-origin or actual Minecraft ore-snapshot validation.
The fixture also uses
saved residents; no world blocks/entities or production DB are changed. It verifies
the packaged paid chain, processing, capital conservation, duplicate/restart safety,
dead-worker exclusion and immutable diagnostic output. Archive it outside plugins
after normal stop. Neither probe belongs in a distributed plugin artifact.

src/test/paper/FarmBusinessProbe.java is compiled separately against the packaged
plugin and Paper API dependencies, then packaged with a test-only plugin.yml:
name CivilizationsFarmProbe, main FarmBusinessProbe, version 1, api-version 1.21,
depend Civilizations. It is outside Maven's production and unit-test source roots;
never install it on a real server or ship it in the plugin JAR.

Only the marked disposable localhost server on port 25577 with no players is
accepted. Back up its stopped Civilizations DB consistently before deploying a
new plugin. The probe uses its own randomly named data-directory session and
temporary database, two real test villagers, and 32 crop blocks around 1024/1024.
It creates test-only chunk tickets and clears space above its small crop fixture.
It verifies real snapshot evidence, founding, workers, food, startup spending and
world-pause behavior. Its disable handler closes the session and removes only its
own villagers/tickets. Stop normally, inspect databases read-only, and move the
test probe JAR out of plugins afterward. The crop fixture may remain in this
disposable world. This is not a human client acceptance test.
