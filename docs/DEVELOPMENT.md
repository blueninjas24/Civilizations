# Development and recovery procedures

October 11 tool-upkeep extension: WarehouseSupplyProbe adds an independent
tools-run database, real smith output and actual PopulationSession mine/tool
periods. Check real cash conservation, only productive kit use, fractional slower
work, saved service, delayed warehouse purchases, restart and world absence.
Back up the stopped marked fixture before schema 51 deployment. Its ordinary
population calendar can advance during the probe; compare identities/assets and
explain any exact normal month separately. Stop normally, verify integrity/FKs,
restore exact properties and archive the test JAR. Never deploy probes to human
servers. BUSINESS_TOOLS.md and DEVLOG.md record completed validation.

## Warehouse restocking smoke test

WarehouseSupplyProbe is a separate test-only plugin, name
CivilizationsWarehouseSupplyProbe, main WarehouseSupplyProbe, API 1.21,
dependency Civilizations. Require the marked localhost disposable fixture,
port 25578 and no players. Back up the stopped DB/JAR/properties and compile
separately against the verified packaged JAR/cached dependencies, with empty
sourcepath. Never deploy the probe to a human server.

It creates synthetic firms/reserves in its own random database, then runs actual
PopulationSession pulses for paid mine → warehouse → workshop deliveries and tools.
Check replay, shared hauling, absence, clean restart and disabled new restocking
without losing already paid cargo. Require NATIVE WAREHOUSE SUPPLY PASS. It edits
no world blocks/entities and adds no tickets. Stop normally, check both saves
read-only, restore exact properties and archive the probe outside plugins.
Schema remains 50. This is not automatic founding or natural customer/site proof.
See WAREHOUSE_RESTOCKING.md and DEVLOG.md for completed results.

## Funded industry demand smoke test

IndustryDemandProbe is a separate test-only plugin, name
CivilizationsIndustryDemandProbe, main IndustryDemandProbe, API 1.21,
dependency Civilizations. Require the marked localhost disposable fixture,
port 25578 and no players. Back up stopped DB/JAR/properties; compile separately
against the verified packaged JAR/cached dependencies with an empty sourcepath.
Never install this probe in a human/production server.

It uses the actual Paper world UUID with synthetic explicit firms/reserve/evidence
in its own random SQLite folder. Check actual workshop funds/labor/input gaps,
paid mine-to-warehouse goods, warehouse coverage, paid pending workshop cargo,
conserved treasury/business cash and a real PopulationSession reopen/world pause.
The review must create no orders or firms itself. No block/entity changes or tickets.
Require NATIVE FUNDED INDUSTRY DEMAND PASS; stop normally, verify both saves
read-only, restore exact properties and archive the probe outside plugins.
Schema remains 50; this does not test natural startup or finished-goods customers.
See INDUSTRY_DEMAND.md for the read-only boundary.

## Current mineral survey smoke test

CurrentMineralSurveyProbe is a separate test-only plugin, name
CivilizationsCurrentSurveyProbe, main CurrentMineralSurveyProbe, API 1.21,
dependency Civilizations. Use only the marked localhost disposable fixture on
port 25578 without players, after backing up its stopped DB/JAR/properties.
Compile separately against the verified packaged JAR and cached dependencies
with an empty sourcepath. Never install in a human or production server.

The probe uses the actual Paper world UUID, synthetic bed/person/ore observations
and an accelerated clock in its own random PopulationSession SQLite save. It checks
fresh candidates, absent-world pause, expiry, newer empty replacement, older replay,
retained historical sightings and a clean owner restart. It edits no blocks/entities,
adds no tickets and claims no natural discovery or automatically founded business.
Require NATIVE CURRENT MINERAL SURVEY PASS; stop normally, verify both SQLite saves
read-only, restore exact properties, check no forced chunks and archive the probe
outside plugins. Schema 50 adds empty current survey tables, without fabricating
current evidence from legacy sightings. See MINERAL_OPPORTUNITIES.md.

## Startup-grant smoke test

StartupGrantProbe is a separate test-only plugin, name CivilizationsStartupGrantProbe,
main StartupGrantProbe, API 1.21, dependency Civilizations. Require marked localhost
disposable fixture, port 25578, no players. Back up stopped DB/JAR/properties first;
temporarily change only the test port and restore exact original bytes afterward.
Compile separately against verified packaged JAR/cached binary dependencies with an
empty sourcepath. Never install this probe in a human/production server.

It parses actual bundled economy.yml and uses its own executor/random SQLite save,
synthetic resident and explicit synthetic ore evidence. Check protected-food refusal,
real startup/capital/worker receipt, mining and disabled-policy restart replay.
This is not automatic career/founding, natural ore or client validation. No block/
entity edits or chunk tickets. Require NATIVE STARTUP GRANT PASS, stop normally,
verify both saves read-only, no forced chunks, archive probe outside plugins and
restore settings. Schema 49 must add no main grant receipts/retroactive payments.
See STARTUP_GRANTS.md and DEVLOG.md for exact verified results.

Adult-study probe follow-up: schema 43, a separate study-run DB with 0→1→2 skill,
COMPLETED course, stone/timber8 remaining from10 each, four material spending rows.
Hungry first review must spend nothing; absence/restart/replay add no experience.
Keep all 16 old dev asset/skill/audit snapshots unchanged and no invented main courses.
Latest Wrapper clean verify770 passed. RESIDENT_STUDY.md describes settings/scope.
Migration failure assertions must read prior-schema tables directly, not new-schema
view diagnostics. Stop/restore/archive as with the existing guarded industry probe.


Work-learning probe follow-up: verify schema 42, eight participant entries and
actual mining skills 15/8 from starting 10/5. No awards during world absence,
closure or replay; existing asset/skill snapshots must survive upgrade unchanged.
The latest Windows Wrapper verification passed 756 tests. Every probe read connection
must enable foreign keys when using repositories; direct read-only assertions must
not require widening application APIs. See WORK_LEARNING.md for defaults/limits.


Latest IndustryRuntimeProbe also expires a separate staffed deceased-owner mine.
Verify schema41, one retained closure, no open jobs on that firm, preserved coal3/
reserve2 and no further output after restart; the inherited mine reaches coal5/
reserve5 and the single saved paid sale retains funds8500/750/250. All assertion
connections enable SQLite foreign keys. Back up stopped disposable assets/settings,
stop normally, compare retained main asset rows, check integrity/FKs, restore exact
properties and archive the test plugin. See BUSINESS_CLOSURE.md.

Latest IndustryRuntimeProbe also opens an explicit saved 250-price offer through
the SQL owner before payment and checks SOLD linkage after restart. Its fixture
database must be schema 40 with one paid sale, business cash 750, estate 250 and
treasury 8500. Backup the stopped marked development database/settings first;
after normal stop verify old asset rows retained, integrity/FKs healthy, archive
the probe outside plugins and restore network properties byte-for-byte. No probe
belongs on the player's own server. See BUSINESS_SALE_LISTINGS.md for limits.

WorkshopSupplyProbe.java is another test-only plugin, name
CivilizationsWorkshopSupplyProbe, main WorkshopSupplyProbe, API 1.21, dependent on
Civilizations. Compile/package separately as described below. Only the marked
localhost disposable server with no players is accepted. The fixture uses its own
SQLite database and actual PopulationSession owner, explicit synthetic businesses,
depleted mined materials, real capital and an unloaded site; no world changes.
It checks paid orders, delayed usable inputs, tools, absence/restart and conserved
funds. Stop normally, inspect integrity/FKs and retained main settings/state, then
archive the probe outside plugins. Never install it in the player test server.
See WORKSHOP_SUPPLIES.md for settings, migration and current gameplay limits.

The latest WorkshopSupplyProbe guard uses port 25578; temporarily change only the
stopped marked dev server's port with a backup, then restore original properties
after normal stop. The player server stays on 25577. To check legacy cargo upgrade,
prepare a consistent SQLite backup of a known earlier schema-36 synthetic supply
fixture as plugins/CivilizationsWorkshopSupplyProbe/legacy-upgrade-fixture/civilizations.db.
Never copy a player/production save. The source fixture remains read-only; only the
separate copy upgrades. The native PASS must report legacy checked=true to claim
that case. Fresh work also verifies saved cadence identity; temp SQLite tests cover
the actual retuning collision, shared quota, replay and failed-migration recovery.

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

LiveOreSurveyProbe is a separate guarded test-only plugin, name
CivilizationsLiveSurveyProbe/main LiveOreSurveyProbe, API 1.21, dependency
Civilizations. Its latest guard requires the marked localhost/no-player dev server
on port 25578, leaving the player-test address 25577 free. Back up server.properties
while stopped, change only that port, and restore it after a normal stop. It uses a separate random
data directory/session, synthetic saved homes/people and four temporary ore/air
blocks in already-loaded chunk 1,0, with empty synthetic homes in chunk 0,0.
No actual villagers or inventories are changed.
It runs the production PaperOreObserver with explicit small test budgets, saves
one known placement exclusion in its own DB, and verifies the actual scheduled
snapshot/SQL-owner pipeline reaches the nearby chunk and reports one admitted patch.
The schema48 opportunity batch also reads the resulting saved coordinate and capture
timestamp through the SQL owner, then reopens its separate stopped SQLite DB to
confirm durable positive evidence, no new business/deposit and healthy integrity/FKs.
Its injected clock advances during traversal; diagnostics must retain capture time.
Disable cancels observation,
closes the owner and restores/checks every fixture block. No mine is founded.
Back up the stopped dev DB, compile against binary dependencies, archive the probe
after a passing normal stop, and remove any console-only fixture loading ticket.
On this verified empty disposable server only, use console
`execute in minecraft:overworld run forceload add 0 0 16 0` for the two fixture
chunks, and remove it with `execute in minecraft:overworld run forceload remove 0 0 16 0`.
Production observation never adds tickets or loads these chunks.
Keep owner economy.yml unchanged; the explicit test policy does not opt an old
owner file in. Never ship this fixture in a production JAR. See LIVE_RESOURCE_SURVEYS.md.

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

OreEditorGuardProbe.java is a separate test-only plugin (name CivilizationsEditorProbe,
main OreEditorGuardProbe, version 1, api-version 1.21, depend Civilizations). Compile
against the packaged plugin/binary dependencies with empty sourcepath and package
its descriptor separately. It requires the same marked localhost disposable server
with no players, registers the production guard for its own configured name, and
checks the actual enable event plus clean reopen using a separate empty temporary
SQLite owner. It changes no world blocks, tickets or main settings. Stop normally,
verify both fences/integrity read-only, archive the probe outside plugins. Never ship
it with the distributed plugin. See ORE_EDITOR_GUARD.md.

IndustryRuntimeProbe.java is a separate test-only plugin (name
CivilizationsIndustryRuntimeProbe, main IndustryRuntimeProbe, version 1, API 1.21,
depend Civilizations). Compile/package separately as above. It requires the marked
localhost no-player server on port 25578 and an unloaded abstract site chunk. Back
up/restore server.properties while stopped, keeping the player port free. A separate SQL
fixture owner seeds explicit funded industry, then real PopulationSession owners
run accelerated deterministic clocks/pulses and world masks. Check finite output,
absence, downtime/restart and preserved capital. Its succession fixture records a
qualified child, deceased founder and actual child employment, then verifies current
owner, immutable founding and exactly one ownership event across restart. A second
firm uses PopulationSession.sellBusiness to verify real cash/estate conservation
and accepted replay after restart. No world
operations are required. A separate deceased owner's mine uses a qualified employee
whose skill is below the saved founder threshold; it must produce finite goods
without promotion, retain ownership and pause with its world. No world
edits/tickets or main data changes beyond the plugin's additive schema upgrade.
Back up the stopped marked dev DB first. Stop normally, inspect both DBs read-only,
archive the probe. Never ship it
with the plugin. See TIMED_INDUSTRY.md; TONIGHT_PLAYTEST.md is the player checklist.


October 10 resale extension: IndustryRuntimeProbe also runs a separate random
PopulationSession database with automatic resale enabled. Four deceased owners'
firms are bought with real funds by one qualified resident, then reopened without
repeat payments. Start while the world is absent to prove no offers appear, resume
and verify actual saved proceeds, unchanged total funds and foreign keys. Read-only
repository connections must enable PRAGMA foreign_keys=ON. Store opening funds
are debited from the treasury; do not count them as an additional grant. No player
jobs/world blocks are changed. Use a persistent console (exec tty=true) so `stop`
remains available; restore settings/archive the probe after normal shutdown.
See AUTOMATIC_BUSINESS_RESALE.md for config, schema 44 and limits.


October 10 farm-learning extension: IndustryRuntimeProbe uses another separate random
SQL fixture with saved adults and explicit synthetic crop evidence, then real
PopulationSession food owners. It verifies output8, once-only skills1/1 through
absence, reopen with no catch-up, reduced fields yielding3/2 cumulative experience,
zero-output pause, five participant entries and unchanged actual startup funds.
It changes no world blocks/chunk tickets. This validates the application loop, not
new crop snapshot behavior; the prior FarmBusinessProbe covers actual crop evidence.
Stop normally, verify schema 45/backup snapshots read-only, restore exact settings,
archive test JAR. See FARM_WORK_LEARNING.md.


October 10 mineral-source extension: IndustryRuntimeProbe uses another separate
random SQLite fixture to allocate 100 batches across four coordinates, mine 7,
explicitly remove one share 23, reopen without repeated loss, mine 10 and verify
reserve 60 plus coal 17/stone 34. This is packaged accounting, not native block-break
capture. Compile against the current packaged JAR with an empty sourcepath. Back up
the stopped marked fixture first, run only on localhost 25578 without players, stop
normally, verify PRAGMA user_version 46/integrity/FKs and preexisting asset snapshots,
restore original properties exactly and archive the probe. Production clocks may
tick during the short server run; distinguish age/clock bookkeeping from changes
to identity, goods, cash or history. See MINERAL_SOURCE_LEDGER.md.


October 10 native removal probe: src/test/paper/OreRemovalProbe.java is a separate
test-only plugin, name CivilizationsOreRemovalProbe, main OreRemovalProbe, version 1,
API 1.21, depend Civilizations. Compile against the packaged JAR/cached binary
dependencies with empty sourcepath. It requires the marked localhost no-player
fixture on port 25578 and an already loaded chunk. While guarded on this disposable
server only, the console may temporarily `execute in minecraft:overworld run
forceload add 0 0`; remove that same ticket and query no forced chunks before stop.
The probe itself adds none. It saves/restores four real blocks and dispatches
synthetic native break, piston and explosion events through production listeners
registered for a separate SQL-owner database. The main fixture also records the
unknown-coordinate exclusions from dispatched events, intentionally. It verifies
cancelled break, individual loss, piston origin versus destination, immediate/
restart failure fence, retained goods and further recording while fenced. This is
not a physical piston/explosion-engine or human client test. Re-run the separate
IndustryRuntimeProbe alongside it for prior application regression checks. Back up
the stopped fixture before deployment, stop normally, verify schema 47/integrity/FKs
and old assets/history, restore exact properties and archive both test JARs.
