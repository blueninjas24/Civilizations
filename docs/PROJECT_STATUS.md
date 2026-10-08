# Civilizations — implementation status and handoff

**Verified against local source, Git, Wrapper and disposable Paper on October 8, 2026.** Phase 1 prototype gate passed (10/10 milestones). Phase 2 accounting foundation passed (1/4 reporting milestones); live food, household needs, saved planning priorities, explicit delivery accounting and confirmed player food donations are implemented; treasury accounting is prepared, while funding, complete wellbeing and response execution remain pending. Phases 3–9 remain planned. This is not a balanced survival release; see PHASE1_PLAYTEST.md for limits.

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
1. Economy tuning approved and implemented in commented `economy.yml`. Next:
   a safe player inventory handoff for the tested food-delivery ledger, execution
   of feasible responses, actual treasury funding/spending integration and
   household allocation/full wellbeing. Saved household needs and explicit treasury
   accounting are now implemented; no automatic taxation or personal wealth inferred.
2. Human client playtest can follow later; user authorized Phase 2 without it.
   Preserve vanilla profession/trades/inventory when physical villagers are recreated,
   and handle other native transformations before recommending established survival worlds.
3. Current roadmap phase: needs, commodities and explainable economy decisions; no
   construction or canonical roadmap changes. Latest feature work has not been pushed.

## Confirmed player food donations — October 8, 2026

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

## Player food-aid previews — October 8, 2026

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

## Explicit food delivery accounting — October 8, 2026

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

## Evidence-based village priorities — October 8, 2026
- Branch `codex/phase2-village-priorities`: villages choose MONITOR, SEEK_FOOD or
  INVESTIGATE_HOUSING from saved unmet food and observed household crowding. Unknown
  housing cannot justify crowding; infeasible priorities are excluded under every seed.
- Editable prototype priority weights/cadence in optional `economy.yml` section.
  Minimum relative weight excludes irrelevant alternatives before a seeded weighted
  draw. Five-period default reviews reconsider early on appearance/severity/recovery,
  crowding appearance/recovery or priority-setting changes. No invented leader/jobs.
- Schema 17 saves review inputs, policy, seed, replay version and chosen priority.
  Historical candidate explanations reconstruct from original inputs. Food/needs/
  reviews commit together; retries cannot duplicate effects. Policy tuning is forward
  and does not discard food-clock time. Old periods get no fabricated decisions.
- Twelve new cases; Windows Wrapper clean verify passed 300 cases (298 unit/two
  packaged integration), no failures/errors/skips. Blocked choices over 1,000 seeds,
  reasonable alternatives, crisis filtering, 1,200-period replay, rollback/retry,
  schema-16 upgrade failure/recovery and existing treasury retention verified.
- Guarded Paper probe passed actual food crisis selecting SEEK_FOOD, original choice
  surviving restart/feast retuning and recovery returning to MONITOR, plus earlier
  food/needs/unloaded scenarios. Marked dev schema-16 snapshot taken first; original
  identities/families preserved and integrity/FKs checked. No production data reset.
- Priority decisions are nonbinding planning intentions. No suppliers, purchases,
  stock transfers, jobs or construction are fabricated. Execution, outcome feedback,
  broader wellbeing and player trade remain. Phase 2 stays 1/4 full reporting groups;
  PROGRESS now lists verified partial pieces rather than hiding them behind 25%.
  See VILLAGE_PRIORITIES.md. No push, merge or main modification.

## Household needs and treasury accounting — October 8, 2026
- Branch `codex/phase2-household-needs`: food periods now retain household adults,
  children, configured demand, observed housing and unassigned living residents.
  Unknown capacity is distinct from crowded housing. Shared food pressure is not
  presented as proof of individual rations or complete prosperity.
- Needs and food effects/checkpoint share one transaction. Historical housing and
  crisis evidence remain intact through restart, retuning and later observations.
  Schema 16 adds needs snapshots and an independent exact Trade Value treasury
  ledger; old periods are not backfilled with invented household observations.
- Explicit treasury opening/credit/debit APIs require a source/reason and operation
  UUID. Idempotent retries, rollback, insufficient funds and overflow are tested.
  No automatic live account, money creation, income, tax, salary or personal wallet.
- Fourteen added automated cases; Wrapper clean verify passed 288 cases (286 unit,
  two packaged integration), no failures/errors/skips. Includes two repeatable
  1,200-period needs scenarios and schema-15 upgrade failure/retry.
- Guarded real Paper food probe passed needs output, actual household demand/housing,
  preserved crisis assessment after restart/retuning, no invented treasury and the
  earlier food/unloaded behaviors. Consistent schema-15 dev snapshot first. Original
  people/families retained, integrity/FKs verified; no production save/world reset.
- `/civ debug economy` uses cached period evidence, limits household output to twenty
  entries and distinguishes spending budget from prosperity. Large-city profiling,
  rationing, full wellbeing, actual budget flows and player trade remain outstanding.
- Phase 2 stays 1/4 (25%) by reporting groups. The second group is partial: observable
  needs and budget machinery do not complete the broader labor/prosperity gameplay.
  Next gate: feasible decision pressures/responses with tested causal prerequisites.
  See HOUSEHOLD_NEEDS_AND_TREASURY.md. No merge/push/main changes.

## Live food economy — October 8, 2026
- Branch `codex/phase2-live-food`: loaded main-thread crop snapshots around saved
  homes feed the existing SQL owner. Saved living adults supply bounded prototype
  labor; children consume food. Default capacity is one worker per 16 planted crops.
  No harvesting, ownership claim, player-block edit or invented offscreen facility.
- Schema 15 adds evidence, saved independent economic clocks and recorded forward
  tuning. Per-period policies preserve old explanations across threshold edits.
  Stocks/inputs/checkpoint commit together; partial/conflicting scans pause work.
  Unloaded villages use last reliable capacity without forcing chunks.
- New read-only `/civ debug economy <UUID>` shows reserves, evidence, shortage
  explanations and indicative prices. These are not merchant offers.
- Windows Wrapper clean verify: 274 cases (272 unit/two integration), no failures,
  errors or skips. Tests cover actual saved demographics, temporary databases,
  clock/restart limits, pauses, failed-save rollback/retry, history-preserving tuning,
  migration backfill, negative-height crop scanning and old owner configuration.
- Guarded real Paper probe passed planted-field supply, depleted food/severe shortage,
  forward feast retuning, identity/history restart preservation and unloaded accounting.
  Consistent disposable schema-14 backup taken first. No production reset or world
  deletion. Test helper is excluded from the production JAR.
- Phase 2 stays 1/4 (25%): the second milestone is partial. Treasury, family allocation
  and wellbeing, business hiring, autonomous responses and player trade remain.
  No starvation population penalty. Catch-up uses current saved demographics and last
  reliable field evidence rather than full historical event interleaving. Scale and
  human survival playtesting remain pending. See LIVE_FOOD_ECONOMY.md.

## Editable economy prototype settings — October 8, 2026
- User authorized configurable economy defaults and requested easy famine/feast tuning.
  `economy.yml` is created only if missing and validated before database startup.
  Comments, owner edits and matching custom commodity stock/price entries are preserved.
- Plain-Java EconomySettings computes explicit first-enrollment supplies, adult/child
  demand, production bounded by workers plus evidenced capacity, and exact abstract
  scarcity prices. No fabricated facility, block change or merchant transaction.
- Defaults/recipes/ranges documented in ECONOMY_TUNING.md. Nineteen new cases test
  the shipped file, malformed/partial/unsafe edits, exact prices, unknown capacity,
  overflow, immutable settings and distinct normal/famine/feast accounting scenarios.
- Wrapper clean verify passed 261 cases (259 unit/two integration), no failures/errors/
  skips. Paper first-run creation, 25% edit/comment surviving restart byte-for-byte,
  normal population diagnostics/unloaded fixture and clean shutdown verified. Restored
  test settings to normal; schema remains 14 and no economy accounts were auto-created.
- At that batch settings/calculations were prepared, not live consumption or trading.
  Recorded tuning transitions and live food were subsequently implemented above. No approval blocker remains for prototype tuning.
  Phase 2 stays 1/4 (25%) with partial labor/price work; no merge/push/main changes.

## Phase 2 commodity accounting — October 8, 2026
- Branch `codex/phase2-commodity-accounting`: plain-Java commodity keys, exact stock
  conservation, explicit incoming/demand categories, reserve use, per-commodity
  shortage duration, protected caller policy and quantitative report explanations.
- Schema 14 adds accounts/stocks/retained period inputs; no automatic inventory
  seeding or live economy scheduling. No inferred production or starvation effects.
  Initialization, advance, history and checkpoint operations are transactional.
- Eighteen new tests; Windows Wrapper clean verify passed 242 cases (240 unit,
  two packaged integration), no failures/errors/skips. Pure and saved 1,200-period
  crisis/recovery fixtures, mid-crisis restart, conservation, write rollback/retry,
  stale-period and tuning rejection, upgrades/malformed schema verified.
- Backed up marked disposable schema-13 DB; real Paper schema-14 startup/commands,
  unloaded fixture, no forced chunks and clean shutdown verified. Integrity/FKs OK;
  original identities/families preserved; zero economy accounts as intended.
- At this accounting milestone Phase 2 was 1/4 (25%), not a running/balanced economy.
  Tuning approval was pending then; subsequently approved and implemented above.
  Human client playtest deferred explicitly; known Phase 1 recreation limits remain.
  No production reset, construction, main modification, remote push or merge.
  See COMMODITY_ACCOUNTING.md for contract and remaining gates.

## Live population and approved zombie rule — October 8, 2026
- User chose to disable zombie conversion for managed villagers. Registered INFECTION
  guard leaves unmanaged villagers, curing, lightning and unrelated transforms alone.
  Verified lethal zombie attacks still kill people and save death/history; no general immunity.
- Paper projector applies saved names/age locks; nearby births/arrivals materialize only
  at loaded safe homes. Retired/moved/deceased incarnations reconcile incrementally.
  One asynchronous claim/add/activation handshake at a time; persistent freeze marker
  recovers shutdown between activation commit and main-thread acknowledgement.
- Vanilla breeding for managed parents cannot bypass saved families/calendar. Actual
  deaths retry on SQL owner, including deaths during asynchronous enrollment. Accepted
  shutdown saves drain before DB close. Entity removal for simulated death does not
  emit a second saved native death.
- Editable projection limits: radius 64, day budget 32/world, night budget eight,
  two inspected chunks/sixteen residents per tick. Night budget defers additional
  spawns; existing villagers use vanilla behavior and are not culled at sunset.
- Windows `.\mvnw.cmd clean verify -B -ntp`: 224 tests (222 unit/two integration),
  no failures/errors/skips. Real Paper probe passed hazardous placement deferral,
  babies/adulthood/names, restart/freeze/claim recovery, native breeding/infection/death,
  five-person migration, simulated deaths and enrollment/death race. Probe never ships
  in production JAR; guarded by disposable marker/no online players.
- Normal plugin-only startup/commands/unloaded simulation checked separately; test
  fixture stayed unloaded and no forced chunks remained. SQL integrity/FKs passed for
  all scenario DBs. Owner batch observed 9.215 ms on three people: not a tick or scale
  bound. Large-server profiling and human client/balance testing remain.
- Schema remains 13; no reset, new migration or production/main/remote changes.
  Vanilla trade/profession/inventory preservation on recreation is not implemented;
  use disposable worlds for playtesting. Source/history/roadmap remain preserved.

## Resident projection preparation — October 8, 2026
- Branch `codex/phase1-resident-projection`: schema 13 incarnation registry,
  claim-before-spawn APIs, retained aliases, actual-death ledger and immutable owner
  views. Pure planner covers unloaded/pending/moved/deceased identities. No entity
  mutation adapter or death listeners are wired yet.
- Fixed stale settlement counts after demographic outcomes, including a town emptied
  by migration; unbound legacy census rows remain preserved.
- Read-only `/civ debug explain <uuid>` shows cached saved reasons, migration origins
  and identity conflicts; complete history remains in SQLite.
- Wrapper clean verify: 215 tests (213 unit, two integration), no failures/errors/skips;
  eighteen added cases. Real disposable Paper schema-12-to-13 upgrade, command,
  unloaded simulation and clean restart verified with backup first. Original IDs,
  families/homes preserved; month-45 adult arrival explained without loading its area.
  SQLite integrity/FK checks passed; server stopped cleanly.
- At this preparation milestone the zombie decision was pending; subsequently approved
  and implemented above. The following milestone history describes earlier snapshots.

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

## Schema-11 Paper follow-up
- Source 62550ee/154-test JAR enabled on the authorized disposable Paper server;
  schema 9 upgraded to 11, integrity verified and clean stop confirmed.
- Signature audit found no common GitHub/AWS credential/private-key patterns in
  reachable tracked history; no remote publication. Hosted CI remains pending.
- Population import/scheduling/projection remain upcoming; Phase 1 stays 70%.

## Atomic village enrollment and hosted CI
- Branch `codex/village-enrollment`; schema 12 adds optional bed/home links.
  One transaction imports names/ages/identities, shared homes, housing evidence,
  approved prototype partners and authoritative headcount. Failure rolls all back.
- Eleven new temporary-DB cases include restart/rename, partial observations,
  deceased identities, final-write failure/retry, protected old/overlapping homes
  and actual calendar-driven saved births independent of visible census.
- Windows wrapper clean verify passed 163 unit and two integration cases (165 total),
  zero failures/errors/skips. Observer/runtime scheduling/projection still unwired.
- User approved pushing exact snapshot e85416e on `codex/phase1-foundation-ci`.
  Both hosted Windows and Ubuntu jobs passed:
  https://github.com/blueninjas24/Civilizations/actions/runs/37725399200
- That snapshot has 154 tests; newer enrollment work remains local. Main unchanged,
  no merge. Phase 1 now 8/10 (80%), with the CI/Paper/recovery gate verified.
- No actual DB/world/server change in this enrollment batch. See VILLAGE_ENROLLMENT.md.

## Dedicated population owner and timer
- Branch `codex/population-runtime-clock`: PopulationSession owns JDBC and imports
  on one thread, queues immutable observations, publishes immutable snapshots and
  runs bounded calendar catch-up on a real fixed-delay timer.
- Editable documented test cadence/cap, protected saved settings, historical import
  delay, explicit 64-operation overload rejection, failed-write retry and safe shutdown.
- Ten new temporary-DB tests. Windows wrapper clean verify passed 173 unit and
  two integration cases (175 total), zero failures/errors/skips.
- Plugin wiring/config/commands/observer/projection still pending, so Phase 1 stays
  80%. No new world/server/actual database changes, merge or push in this batch.

## Paper calendar and read-only diagnostics
- Branch `codex/paper-population-clock`: asynchronous owner-thread startup, strict
  editable config, main-thread initialization polling and shutdown. No shared JDBC.
- Read-only `/civ debug status` and `/civ debug settlement <uuid>` inspect immutable
  snapshots with operator/console permission. No mutation or reload command.
- Old census listener retained but unregistered to protect authoritative residents;
  automatic observer/import/projections still pending.
- Windows wrapper clean verify passed 188 tests (186 unit, two integration), zero
  failures/errors/skips. Thirteen new config/permission/command/diagnostic cases.
  Initial sandbox build failed to resolve OneDrive target with AccessDeniedException;
  expanded-access build passed without build/OS/location changes.
- Backed up marked disposable schema-11 DB before deployment. Pinned Paper 26.2-129
  enabled the JAR, created config, advanced a real month, served status/validation
  messages, stopped and restarted at saved month 1. Second run advanced to month 3
  and stopped cleanly. Only approved temporary dev files changed; schema now 12.
- No populated village/player scenario, production change, merge, push or main
  modification. Phase 1 now 9/10 reporting milestones (90%).

## Automatic Paper village observation
- Branch `codex/paper-village-observer`: main-thread incremental chunk snapshots,
  loaded-entity guards, bed-head deduplication and coverage/border completeness checks
  now feed the existing SQL-owner importer automatically. No chunk tickets, POI
  searches, block writes, entity mutations or loaded-census replacement.
- Configurable budgets and read-only `/civ debug observation`; scan/import/cooldown
  queues bounded. Partial windows invalidate housing confidence. Indivisible API calls
  mean the time budget is a target, not a hard deadline; seed fairness needs profiling.
- Windows wrapper clean verify passed 197 tests (195 unit, two integration), zero
  failures/errors/skips. Nine new cases cover scan budgets, negative heights,
  bed halves, boundary closure, observer config and command diagnostics.
- Real Paper caught negative-height snapshot section indexing. Inspected pinned
  implementation, added adapter/regression test and reran successfully. Rejected
  initial frame performed no import.
- Disposable fixture automatically imported two beds/two adult villagers as one
  settlement/home with housing capacity four. Repeat loaded scans preserved people.
  Removing console test tickets unloaded the area; calendar continued to month 27
  with two saved residents and a partial-frame diagnostic, without loading it again.
- Final JAR restart/reload/re-observation retained exact settlement, household,
  resident and entity-link IDs. Read-only DB comparison: ages advanced exactly eight
  months with clock 27 to 35; two residents, schema 12, integrity ok, no FK errors.
- No actual client play, NPC projection or demographic balance claim. Test server
  stopped and console loading tickets removed. Only approved disposable dev files
  changed; no schema change, production data, merge or push. Phase 1 stays 90%.

## User workflow
`docs/PROGRESS.md` tracks verified roadmap milestones in plain English. Current snapshot: Phase 1 has ten of ten prototype milestones complete; Phase 2 has one of four reporting milestones complete (accounting complete; live food and household needs added, later groups partial); Phases 3–9 have none complete. These counts are not time estimates or release-readiness percentages. Future batches update it alongside this status file.

This ChatGPT thread is product/design HQ. Codex is implementation agent. It may proceed autonomously on scoped engineering work, committing to feature branches, but must escalate consequential design decisions and destructive operations. Preserve the full nine-phase roadmap.
