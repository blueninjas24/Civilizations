# Persistent ore placement exclusions

The user-approved exposed-ore prototype accepts unknown old origin but excludes
newly tracked player placements. This batch establishes durable exclusion storage
and enforcement. The subsequent LIVE_ORE_TRACKING.md batch registers native
placement capture with a clean-stop/failure fence. Bounded read-only live surveys
now exist (LIVE_RESOURCE_SURVEYS.md); no automatic mine founding/operation yet.

## Storage and owner boundary

Schema 33 adds player_ore_placements keyed by permanent world UUID and absolute
XYZ, referencing observed_worlds. No player identity or private address is stored.
Existing worlds, resource claims, depletion, businesses and economic history remain.
The migration invents no placement history for old blocks.

OrePlacementRepository.record accepts 1–512 unique positions and a nonnegative
timestamp. All new exclusions commit together; write failure rolls them all back.
Replays preserve the first saved timestamp. Exclusions are sticky: changing/breaking
a block does not erase provenance or make replacement ore eligible. No reset API
is introduced. Permanent coordinates in different worlds are independent.

inChunk returns bounded immutable coordinates, including negative chunk positions.
If the requested evidence budget is exceeded, it fails instead of returning a
truncated set that could wrongly admit player ore. Indexed world/X lookup limits
reads. The configured cap is a caller budget, not a claim about CPU time.

PopulationSession queues immutable placements and reads to its existing SQL owner.
It can observe a world before any settlement is enrolled. Accepted work drains on
normal shutdown. Failed futures report errors; no Bukkit object crosses the queue.
World-name observation and the exclusion batch are separate transactions; a failed
batch can leave the harmless world identity row but never partial exclusions.

## Discovery defense

MineralDiscoveryRepository checks every survey coordinate against saved exclusions
inside its discovery transaction. A stale snapshot or trusted caller cannot bypass
the scanner filter. The entire survey fails with a diagnostic so the observer can
refresh evidence; filtering silently would change deposit terms and grouping.

Recording a placement after old discovery does not delete old claims or reset
reserves. Future surveys containing the excluded coordinate fail; already saved
history and finite extraction accounting stay intact. This is not block ownership.

## Remaining live bridge

The production listener now captures successful native placements and uses a
clean-stop/failure fence to protect missing pending writes across crashes.
Piston movement now has conservative verified exclusions; bulk-edit integrations need coverage before automatic
resource founding is enabled. In-memory pending positions alone are insufficient.
Native place events do not certify all non-place mutations.

Observed patch grouping and bounded live surveys exist. Automatic mine founding, ongoing exploration, extra
mineral/stone/sand catalogs and automatic chain scheduling remain subsequent work.
The original site-only discovery API is still trusted legacy code and does not
provide coordinate/provenance safeguards; live observers must use the new path.

## Native verification fixture

WorkshopProductionProbe calls ExposedOreProbeSupport only after its marked local
disposable-server guard. On the main thread, six temporary blocks in an already
loaded chunk create positive coal/iron exposure; original BlockData is restored in
finally and checked before scanning the captured snapshot. No forced chunk loading
or permanent excavation occurs in the scanner/probe. On an empty test server, a
console-issued test loading ticket supplies the chunk; remove it after verification.
The separate probe database checks sticky placement
replay, rejection of stale ore evidence, preserved reserves/claims and restart,
alongside the existing paid workshop chain. Probe sources are excluded from the
shipped production JAR. This is synthetic fixture validation, not a client playtest
or a natural-resource/founding demonstration. Final Windows Wrapper clean verify
passed 605 cases (603 unit/two packaged integration), with eight new persistence/
discovery/session cases and no failures/errors/skips. Positive native Paper fixture,
schema upgrade, normal diagnostics/shutdown and read-only integrity/history checks
passed. Test blocks were restored, loading ticket removed and probe archived.
