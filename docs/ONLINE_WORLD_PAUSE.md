# Saved worlds pause instead of catching up

User approved October 9, 2026: no years of civilization progress while the server
is stopped or an original world is absent. This applies to demographics and the
live food/needs economy. A world without players is still loaded and continues
simulation. Unloaded chunks also continue; they are different from an absent world.

## Time and saved identity

Paper supplies immutable loaded world UUIDs on the main thread at startup and on
successful world load/unload. Canceled unloads do not pause the world. Only IDs
cross the SQL-owner queue; this adapter reads no blocks or chunks and loads none.

Startup rebases saved time anchors rather than accruing the stopped interval.
Absent economic accounts retain inventory, funds, needs, earned pending periods
and fractional time, while their wall-clock anchors advance without earning time.
Returning worlds resume at their saved state. Global demographic months save the
world IDs eligible when each interval was earned, in the same transaction as time
accrual. Bounded queued replay uses these masks even after a restart/world change.
Missing masks fail explicitly rather than aging every world.

Already earned work is retained. A small queued population batch can finish after
unload because its months were earned before the pause; economic pending periods
wait until the world returns. No absence interval creates additional work.

The shared calendar remains an **administrative event date**, so it can advance
while another world is paused. A paused person's actual age and town economy do
not advance. Population boundaries still use the shared monthly cadence: partial
months carry across world-set changes, and each completed month uses the world
mask at accrual. This can shift a resumed world's first update by less than one
month; independent per-world fractional demographic calendars are not implemented.
Economic fractional clocks are already independent per settlement.

New Phase 3 backend industry has no automatic timer yet. Future industry scheduling
must use eligible town economic periods, not infer elapsed absent-world time from
the shared administrative date. This batch does not enable new industries.

## Database transition and compatibility

Transactional schema 29 adds online-calendar adoption metadata and compressed
active-world month ranges. First adoption records the legacy target/completed
month before rebasing an uncompleted legacy demographic queue to completed history:
its old eligibility cannot be reconstructed safely. Completed ages/events, village
identities, families, inventory and financial history are retained. Subsequent
online starts preserve earned queues and masks. Take a consistent backup before
upgrading any real server; no production database is upgraded by development tests.

Low-level headless APIs retain explicit legacy elapsed-time behavior for existing
accelerated harnesses. Live Paper always supplies world availability and uses the
approved online mode, regardless of old maximum-offline settings. Legacy calendar
accrual/demographic replay is rejected after adopting an online database, protecting
against accidental unrestricted replay. Existing owner YAML files are not rewritten.

Keep the original world UUID/data and matching plugins/Civilizations database when
switching worlds. A new world has its own identity and does not delete old towns.
An empty plugin data folder loses simulation history even if the world is preserved.
Back up worlds/plugin/player data consistently, especially during pending exchanges.

## Verification scope

Automated tests use temporary SQLite files and controlled clocks. Cases cover two
worlds with different availability, paused age/life/family/arrival rules, all-world
absence, long downtime, fractions, bounded pending replay, food stocks/queues,
transaction failure/retry, missing masks, migration rollback and Paper event ordering.
Native Paper startup/restart checks and exact build results are recorded in the
current PROJECT_STATUS and DEVLOG entry. Human client balancing remains separate.
