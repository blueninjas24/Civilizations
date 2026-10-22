# Civilizations — architecture and engineering contracts

## Native road steps — exact ground rather than home AIR placement

PaperRoadWorkSurvey/Writer are separate bounded adapters. Frozen road Geometry
can be read from its draft; source recipe hash remains planning provenance rather
than fabricated native recovery metadata. RoadGroundReview derives configured
buffer/headroom evidence and rejects frames above255 cells before native reads.
Complete loaded entity census includes a bent route's bounding-box chunks.

PopulationSession.prepareAuthorizedRoadStep repeats current paid/material/labor/
protection/scene validation through ConstructionRoadIntents.prepareCurrent before
capturing an owner permit. Historical prepare replay remains distinct. New intents
alone receive one first-write proof; renewal never replenishes consumed/lost proof.
The main-thread writer performs full exact scene/entity recheck and a single CAS
under the nonblocking owner gate; no SQL or future wait there. Post-restart exact
output may acknowledge; original ground without first-write certainty holds.
Automatic road scheduling and completed-road publication are subsequent work.

## Schema117 — separate exact road-ground preparation

The journal preserves its existing column/order/identity semantics. A versioned
transactional rebuild widens only its closed before-state set; an exact matching
immutable construction_road_ground receipt is mandatory for non-AIR inserts.
Receipt and intent have deferred referential integrity and commit together.
ConstructionRoadIntents reuses the existing paid public stage/stock/site guards;
it checks saved protection over the retained endpoint as well as the corridor.
Positive native ground/headroom evidence and validated acknowledged prefix are
facts for SQL preparation, never native authority. Historical replay remains a
receipt only. Normal home/air preparation and the current native writer remain
unchanged; a dedicated gated road writer must be connected before paving.
No direct SQL/world calls on Paper thread, no extra money/labor or chunk loading.

## October21 frozen local road recipe conversion

LocalRoadDesign is pure construction-domain preparation using existing immutable
ConstructionTemplate/Stage/Anchor types. It validates whole flat corridor geometry,
excludes the existing road endpoint, normalizes only actual cells and scales each
editable recipe material/work quantity upward by new cells / authored batch size.
The resulting template can use existing snapshot hashing/draft persistence; no
new save schema or native authority. Its returned recipe hash is contextual
provenance, not a newly persisted table. Exact-ground before-state authorization
and road completion remain separate. Existing AIR-only writer is unchanged.

## October21 local access corridors remain domain-only

LocalRoadAccessPlanner consumes existing immutable survey/classification types;
it has no Paper, JDBC, timer or financial dependency. Complete three-layer flat
frame preflight is capped at4096 ground cells; bounded BFS search has a separate
review/distance policy. Stable seed tie ordering and explicit refusal statuses
make decisions explainable. Its immutable corridor is geometry, never a material
quote, land lease or write permission. Future integration must independently
freeze design, reserve space, fund/book work, prove native before-state and record
Civ-road provenance. Automatic home sites still need existing road connectors.

## October21 housing-to-native resident view

ResidentProjectionHomes reads current positive dwelling assignments/consent and
qualified unit reviews on the SQL owner. It publishes immutable household homes
with distinct bed-head coordinates per living resident in stable UUID/coordinate
order. Managed assignment history suppresses fallback to obsolete legacy beds;
authored/claimed beds cannot leak into legacy homes. Untransitioned households
retain legacy behavior. Reads never assign a family, alter capacity or spawn.

Durable spawn claims now accept a current qualified modern home without requiring
legacy village_beds household links. Existing incarnation/alias checks remain.
Housing review and consent/protection changes publish the new immutable view
without waiting for a population month. Paper refreshes active bodies' HOME memory
through its existing bounded loaded-chunk census. It checks actual loaded bed
heads, never loads chunks or teleports. Explicit managed housing loss clears HOME;
unknown/unloaded observation alone does not. Vanilla pathfinding still controls
walking/sleeping and may respond to surrounding POIs.

## October21 selected-plan survey height

HousingPlanningJobs carries the exact recommended template height (matched ID
and fingerprint); PaperAutomaticHomePlanning passes it into the bounded two-pass
site reader. The full roof must fit above a normal bed-height settlement center.
A pure checked bounds function refuses overflow or oversized observation frames
before allocation/reads. Final preparation still revalidates current family,
plan, funds, protection and scene. Native block/journal authority is unchanged.
The original starter plan is preserved; optional six/eight-bed originals fit the
existing256-read writer cap in all rotations/mirrors. No migration is needed.

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
direct marked fixture writer remains test-only. The automatic family-home scheduler now uses this boundary; broader activation
and started-work repair remain separate.

Schema118 completed local roads retain immutable public origin and exact paid
cell/journal identity in civ_roads/civ_road_cells. Current exact inspection, paid
effort and saved protection precede atomic registration/site release. Expected
saved states are evidence for later native matching, never replacement rights or
proof of current block presence. Historical replay grants no new authority.

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

## Automatic home scheduling

PaperAutomaticConstruction is the bounded main-thread adapter, separate from
PopulationSession transactions and the simulation's labor/cargo clocks. Only active
formal household-bound home proposals enter its scoped owner queue. Stable paged
UUID rotation preserves later villages after local holds. One survey and accepted
owner operation at a time; no blocking future waits, SQL or new time accrual on
the native thread. Survey and writer execute on distinct ticks; paid intent must
precede native CAS and acknowledgement must finish before another action. Current
owner contention permits at most two exact renewals inside original evidence
freshness, with native pacing; no consumed first-write proof is recreated. New
settings opt existing installations in explicitly. Normal housing reconciliation
activates completed village homes; scheduler registration alone grants no beds.
