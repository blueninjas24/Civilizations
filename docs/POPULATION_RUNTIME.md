# Population owner and live clock foundation

`PopulationSession` owns initialization, JDBC, imports and simulation on one
dedicated platform thread. Immutable observations enter through futures; immutable
status/population snapshots leave through atomic references. There are no Bukkit
calls. A real fixed-delay timer pulses once per second; accelerated tests can disable
that timer and invoke the identical operation explicitly.

Editable prototype Settings: seed 872613, 30 real seconds/civilization month,
12 months/year from the demographic policy, two months maximum per batch, and
12 months maximum newly credited offline time per startup. This makes a prototype
year six real minutes; it is deliberately fast for disposable testing, not balanced
survival pacing. Existing queued work is never capped away. Discarded new offline
milliseconds are logged. Seed, saved demographic settings and cadence mismatch
fail initialization instead of silently reinterpreting an existing save.

Queued calendar targets save before each demographic batch. A failed write retains
the target for identical retry; status reports the failure and pending work. Imports
wait for historical catch-up so newly seen people are not aged through years before
their enrollment. Per-candidate SQL failures return diagnostics while successful
independent candidates retain their transactions. No partial loaded census replaces
saved people. No chunks/entities/blocks are loaded or touched by this worker.

At most 64 submitted operations can be pending. Excess requests explicitly fail
for retry. The timer has one fixed-delay invocation, so it cannot pile up scheduled
ticks. Shutdown stops accepting work and cancels the timer; queued writes are
aborted before starting and the owning thread closes its connection. External callers
wait up to five seconds for shutdown; an owner-thread completion callback never waits
for itself. Step limits cap months, not CPU time; measured batch nanoseconds are
exposed for later Paper profiling.

Ten tests use only temporary databases: imported residents/aging, restart/fractional
time/backlog, offline cap independent of existing backlog, catch-up enrollment delay,
actual timer/owner identity, changed seed/cadence rejection, write failure and retry,
closed-session lifecycle, callback shutdown and explicit overload rejection.
Full Windows wrapper clean verify passed 175 cases (173 unit, two integration).
Injected failures intentionally emit diagnostic logs; tests passed with no errors,
failures or skipped cases.

## Paper lifecycle and editable settings

The plugin now starts this session asynchronously on enable, polls its initialization
future from Paper's main thread, and closes it on disable. SQL initialization and
simulation do not run on the server thread. Startup errors disable the plugin and
retain the database; no automatic reset or replay transition occurs.

Bundled `config.yml` exposes all existing prototype Settings, including recognition,
enrollment and partnership inputs for the upcoming observer. YAML is loaded strictly;
missing keys, wrong types, fractional integer values, overflows and invalid policy
bounds fail startup. Seed, cadence and population/migration policy changes remain
protected by the saved replay checks. Restart is required; no reload/mutation command.

`/civ debug status` reports readiness, completed date, queued months, saved living
headcount, batch duration, pace and failure. `/civ debug settlement <uuid>` reports
saved living/deceased residents, homes and known versus unknown housing capacity.
Both read immutable snapshots, never JDBC/world state, and require
`civilizations.debug` (operators/console by default). No simulate command exists yet.

The old player-movement census listener remains in source for reference but is no
longer registered. Its loaded-villager counts cannot overwrite authoritative people.
Automatic recognition/import is now wired through the bounded Paper observer; see
PAPER_VILLAGE_OBSERVATION.md. NPC reconciliation and full playable population
validation remain pending. `/civ debug observation` exposes observation quality and
progress without SQL access.
