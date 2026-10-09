# Live native ore placement capture

OrePlacementTracker is registered by Civilizations for successful native Paper
BlockPlaceEvent observations, including multi-place events. It records current ore
positions rather than replaced air states, deduplicates positions and ignores
cancelled/non-buildable/non-ore placements. All named ore materials and ancient
debris are tracked for provenance; this does not add them to the coal/iron production
catalog. No player identity, item inventory or Bukkit object crosses the SQL queue.

Capture happens on Paper's main thread; saving uses PopulationSession's existing
owner. Batches and pending futures are bounded. A synchronous capture error,
rejected queue or failed asynchronous save marks a gap immediately and logs the
failure once; player placement is not cancelled. No unbounded in-memory journal,
main-thread SQLite work, world writes or chunk loading is introduced.

## Recovery fence

Schema 34 adds a singleton ore_tracking clean-stop/trust fence. Legacy coordinates,
businesses, reserves, world identity and placement history remain unchanged. No
initial tracking row is invented by migration; a live owner explicitly adopts the
approved pre-existing unknown-origin baseline at its first begin.

Before live capture starts, the owner commits a dirty fence. Normal shutdown drains
accepted placement saves, then commits a clean fence. On the next begin, an unclean
prior stop means placement history may have a gap; trust stays false. A clean stop
cannot repair a previous capture failure or crash. Existing deposits/production and
population are not erased. The fence protects new coordinate discovery, not the
entire civilization calendar. Owner write failures also persist a gap where possible;
the dirty startup fence covers crashes if a final save cannot complete.

Session readiness gates new automatic discovery immediately. The coordinate
repository also rejects persisted untrusted tracking state transactionally. Trusted
test/legacy databases with no tracking adoption retain explicit caller APIs; this is
not permission for live observers to skip the session's readiness gate.

`/civ debug status` explains whether native placement capture is healthy or new ore
discovery needs reviewed recovery. No reset/adoption command is implemented. Recovery
must be an explicit reviewed future operation, not deletion of coordinates, reserve
refill or automatic certification of an interval whose history is missing.

## Scope limits

Native placement capture is not comprehensive block provenance. Piston extension
and retraction now conservatively exclude each moved ore's source and both adjacent
coordinates on the operation axis. This deliberately avoids relying on ambiguous
retraction direction semantics: one neighboring coordinate may be excluded even
though ore did not land there. The adapter reads no destination block, loads no
chunk, and does not cancel movement. All moved ore is excluded, including ore whose
old origin is unknown; movement is not evidence of a new deposit. Cancelled events
and movements without ore add no exclusions. Sticky/slime/honey collateral blocks
are taken from the API's moved-block list, not guessed from the piston head.

Coordinate exclusions remain sticky after movement or breaking. Existing claims,
reserve depletion and history are retained. Oversized/invalid evidence or failed
saves uses the same gap fence as placements. Bulk world-edit plugins, external
editors and other non-place mutations still need integration/conservative handling
before automatic mines are activated. No world-edit plugin compatibility is claimed.
The fence is verified for normal full server stops/restarts; plugin hot reload and
world edits while the plugin is absent are not certified tracking intervals.
Healthy capture does not certify natural origin of old blocks. The user approved
that old-origin limitation. Connected observed patches and a read-only live observer
now exist (RESOURCE_PATCHES.md, LIVE_RESOURCE_SURVEYS.md). Actual opportunity
selection, mine founding, scheduling and production are still unfinished.

## Verification design

Temporary databases test dirty/clean restart, retained gaps, failed write rollback,
schema-33 failure/recovery, preserved exclusions, queued shutdown saves, early capture
failure and unchanged population/deposits. Adapter tests exercise placement event
filters, multi-place deduplication, immutable data, async/sync failure and overload.

The guarded native probe now dispatches a synthetic BlockPlaceEvent through the
actual registered plugin listener while its disposable ore block fixture exists.
Its dummy Player is not an online client; no player inventory is changed or saved.
Original fixture blocks are restored, and the owner's read verifies the sticky
placement remains recorded. Test-only reflection locates the private session;
production code has no reflection or testing API. This validates live event dispatch,
not a human client's placement, inventory or compatibility with world-edit plugins.
Windows Wrapper clean verify passed 619 cases (617 unit/two packaged integration),
including fourteen new cases, with no failures/errors/skips. Native event capture,
restored snapshot fixture, schema upgrade, probe-free trusted restart and normal
shutdown passed. Read-only old histories, one actual synthetic-event placement,
clean/trusted fence and integrity/FKs passed; owner configuration unchanged. Test
loading ticket removed, probe archived/excluded from production JAR, server stopped.

Piston extension verification: five new adapter tests cover all six axes,
extend/retract, cancelled/non-ore events, overlapping positions, invalid/oversized
evidence and async failures. Wrapper clean verify passed 624 cases (622 unit/two
packaged integration), zero failures/errors/skips. Guarded Paper synthetic event
dispatch saved seven fixture exclusions, with every block restored; paid-chain
checks still passed. Six new piston exclusions persisted across probe-free restart.
Read-only original industry/funds, schema 34, clean/trusted fence, integrity/FKs
and owner settings passed. Ticket removed, probe archived and server stopped zero.

The first launch mistakenly omitted an interactive console. Only its exact verified
disposable JVM was terminated. Its unclean restart correctly paused discovery;
normal stop did not restore trust. After comparing all industry/funds/exclusion
rows with the consistent baseline, verifying no players/chunks/probe activity,
and backing up the untrusted state, a reviewed dev-only SQL fence rearm allowed
the positive run. Calendar/history were retained. This is not an automatic recovery
feature or permission to rearm an unknown production interval. Subsequent launches
used interactive consoles and normal stop. Actual piston physics, external plugin
ordering and bulk-editor mutations are not certified by synthetic events.
