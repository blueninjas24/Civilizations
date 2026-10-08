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
- Offline/unloaded simulation uses elapsed time in bounded steps; reconcile world effects over multiple ticks. Never force-load chunks just to progress statistics.

## Identity and discovery
- Use stable UUIDs, avoid identifying settlements by current name, player's position or fixed 128×128 region. World UUID/identity strategy and POI cluster matching require design confirmation.
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
- Current repository is a six-class prototype (see PROJECT_STATUS); add cohesive services only as needs emerge. Preserve behavior with tests during refactors. Keep future Paper upgrades isolated near adapters.
