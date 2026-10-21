# Civilizations — architecture and engineering contracts

## Boundaries
- **Domain/simulation (plain Java):** resident, household, settlement, resources, business, economy, pressure, decisions, calendar, history, relationships. No `org.bukkit` types in core calculation interfaces where practical.
- **Application/services:** orchestrate settlement lifecycle, simulation ticks, decisions, transactions, projects and world observations; avoid monolithic listener logic.
- **Persistence:** SQLite repositories and transactional, versioned migrations. Stable IDs and event history. Explicit constraints and meaningful diagnostics.
- **Paper adapter:** discovery/POIs, block placement, entity materialization, schedules, chunk awareness, commands, events, templates. All unsafe world operations remain on main server thread; schedule DB work deliberately without unsafely sharing connections.
- **Integration:** publish structured Civ events for optional consumers such as Chronicler; no LLM or Discord dependency in simulation.

Illustrative dependency direction: `Paper listener -> application service -> domain + repository interfaces`; persistence implements repository interfaces; domain never calls Paper. This is a direction, **not an instruction to build a huge abstraction framework while six classes exist**.

## Data ownership and time
- Database is source of truth for simulated population, businesses, resources, decisions and history; nearby Minecraft entities are projections of persistent residents.
- Observation quality/timestamps distinguish unknown from zero. Do not overwrite authoritative state with partial loaded-chunk observations.
- Two clocks: civilization calendar for generations/history; gameplay pacing for construction, trade and economic updates. Exact ratios are configurable/tunable, not yet locked.
- User-approved October 9: server downtime and absent worlds pause civilization.
  Unloaded chunks within a loaded world still receive bounded data-only simulation;
  reconcile physical effects over multiple ticks without forcing chunks to load.
  Live time accrual saves active-world masks with earned population months. The
  shared calendar is administrative chronology, not the age of a paused world.

## Identity and discovery
- Use stable UUIDs, avoid identifying settlements by current name, player's position
  or fixed 128×128 region. User approved permanent world UUIDs and saved bed/villager
  identity matching. Diagnose multi-town overlaps rather than merging automatically;
  center/border evolution and merger/split rules remain open.
- Notifications are per player and separately throttled from authoritative census/simulation updates.
- Discovery should be idempotent; failures retry safely; overlapping settlements resolved deterministically with diagnostics.

## Database and migrations
- Versioned migrations with atomic upgrade/rollback where supported; test fresh install, incremental upgrade and failure/recovery. Never treat arbitrary SQL exceptions as “already exists.”
- Transactions for multi-field/multi-entity updates; indices for common queries. Back up live SQLite with a consistent snapshot mechanism rather than blindly copying open files.
- Development DB currently disposable after target verification; worlds and future production saves are not. Do not put DBs, sidecars, secrets or worlds in Git.

## Testing/debugging
- Seeded, headless deterministic simulation should execute centuries quickly without a Paper server. Scenario tests: housing shortage, food crisis, mine depletion, growth, trade isolation, migration, business succession, abandonment and recovery.
- Developer commands planned: `/civ debug settlement <id>`, `/civ debug explain <id>`, `/civ debug simulate <duration>`; design permissions and safeguards for mutation commands.
- Test Minecraft-facing code separately on a disposable local Paper server. CI runs wrapper build and unit tests. Record exact verification commands and results.

## Performance and safety
- No blocking scans/expensive DB work per player-move event. Throttle observation, batch work, cap tick budgets, avoid N×N unnecessary full scans.
- Use safe, documented server-thread boundaries. Never async-access Bukkit world/entities/blocks unless a specific API explicitly permits it.
- Protect player-built blocks and historical Civ structures before construction, decay, war or disaster modification.
- Build for 20+ settlements and rare 500+ populations; optimize measured bottlenecks, not imaginary ones.

## Code evolution
- Repository began as a six-class prototype (see PROJECT_STATUS for verified additions); add cohesive services only as needs emerge. Preserve behavior with tests during refactors. Keep future Paper upgrades isolated near adapters.

## Construction SQL/native exclusion

PopulationSession serializes all owner tasks through NativeConstructionGate.
Each task invalidates earlier generations; paid intent captures its exact owner
stamp and original native observation time. A future bounded main-thread native
step tries the read side without waiting. Stale/busy/expired/stopped authority
refuses; a successful step excludes SQL mutations until it returns. No SQL or
future waits are permitted inside that callback. The owner thread may wait for
that brief callback, never the reverse. Native Paper boundaries must still check
main thread, current chunks/entities/blocks and exact frozen intent. This gate
alone grants no land or free effort. PaperConstructionWriter now supplies the
internal guarded native boundary: exact intent, full final scene and entity
recheck,256 cell reads/tick, one-time session-local first-write claim. A resumed
before-state stays ambiguous; existing after-state may acknowledge. The older
direct marked fixture writer remains test-only. Ordinary automatic scheduling
and completed-home activation are unfinished.

## Civ-built housing origin and completion

Schema116 keeps explicit completed Civ origin beside shared immutable building/
unit/use records. Existing player records remain PLAYER; Civ records have village
ownership and no public player donor. The legacy SQL donor_id slot stores that
typed village contributor only for positive Civ-origin rows; no authenticated
player donation/reclamation/renovation route accepts them. Shared housing consumes
the use grant through existing inspected occupancy, never through a bed count at
completion. Source proposal/site release and origin/layout/use are one transaction;
completed household identity remains historical and no longer blocks new needs.
Authored bed head identity/provenance is committed before physical placement and
excluded from legacy housing. Existing positive bed journal history backfills in
migration; conflicts abort rather than rewriting another village's identity.
