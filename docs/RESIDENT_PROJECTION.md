# Resident projection preparation — October 8, 2026

## Implemented and verified

Schema 13 adds a current physical incarnation per resident and a physical death
ledger. Existing single-entity identities become ACTIVE without modifying residents
or family ties. Multiple historic aliases without a current incarnation remain
unresolved and cannot spawn copies automatically. Historical aliases are retained.

The SQL owner exposes immutable population/identity/event views and asynchronous
claim, activation, retirement and actual-death operations. No Paper types or world
operations are introduced into this persistence/reconciliation path.

Claims require a living resident, its current household and permanent world. An
entity UUID is reserved before any future world spawn. Only one CLAIMED or ACTIVE
incarnation is allowed. Retirement preserves its alias; a replacement needs a new
entity UUID. Stale callbacks cannot activate, retire or kill a newer incarnation.
Actual deaths save the completed month (including month zero), retain age/name/family
links and update town population transactionally. These APIs are tested but
**not yet connected to Paper death/spawn listeners**.

The pure deterministic reconciliation planner handles:

- Missing ACTIVE villager: no death inference or replacement, even in loaded chunks.
- Saved deceased person: remove its loaded incarnation and retire its binding.
- Moved household: retire the old incarnation before considering a replacement.
- New person: spawn only with a caller-certified nearby, loaded, safe home and budget.
- Frozen pending spawn: activate when found, or retire only when its saved location
  is fully loaded and the entity is absent. Unloaded claims remain pending.
- Loaded obsolete alias: remove the incarnation without losing its saved person.
- Ambiguous aliases: diagnose, do not delete entities or guess ownership.

The planner never invokes Paper or loads chunks. Its world actions are **decisions
only** until the remaining Paper adapter executes them.

`/civ debug explain <settlement UUID>` uses immutable caches of up to 100 demographic
events and 100 physical deaths globally, displaying at most ten demographic outcomes
and up to twenty combined event lines for a settlement. Migration appears for both
origin and destination. This is recent evidence, not exhaustive town history or an
explanation of every failed random draw. Complete existing events remain in SQLite.

Fixed stale `settlements.population` after births/deaths/migration. Enrolled towns
emptied by migration become zero; unbound legacy census rows without residents remain
unchanged. Counts commit with demographic events and clock, not on the next scan.

## Remaining protocol and decision

The future Paper adapter must create an unspawned entity on the main thread, commit
its claim on the SQL owner, recheck loaded placement and life/home, then add it to
the world. Pending entities must remain frozen until activation is durable. Recovery
can rely on the saved location only under that invariant. Current code does not yet
spawn, freeze, rename, age-lock or remove world entities.

Main-thread safe placement, nearby/day-night density, native breeding/aging
coordination, death listeners and visible birth/migration smoke tests remain.
No chunk tickets, block changes or villager trade changes were introduced.

Zombie conversion changes physical entity UUIDs without necessarily ending the
saved person's life. The user has been asked to choose preservation through infection
and curing (recommended) or prevention of conversion for managed prototype villagers.
No rule is enabled pending this decision. Other native transformations also need
explicit handling before broad survival claims.

## Verification

- Windows `.\mvnw.cmd clean verify -B -ntp`: 215 tests (213 unit, two packaged
  integration), zero failures/errors/skips. Eighteen added cases cover claims/restart,
  death/history/counts, migration, rollback/retry, ambiguous aliases, unloaded
  reconciliation, immutable owner views, explanations and permissions.
- Verified marker/checksum/EULA/loopback settings of existing disposable Paper 26.2
  build 129 server. Consistent schema-12 snapshot before deployment; schema 13 upgraded
  without reset. Original two names/UUIDs/household/partnership retained.
- Calendar progressed from month 35 to 48; originals aged 254 to 267. External adult
  arrival at month 45 was saved and explained while the fixture remained unloaded.
  Second clean startup retained all three people/current bindings; no forced chunk
  tickets added. Server stopped cleanly.
- SQLite integrity `ok`, foreign-key check empty. Observed small-data owner batch
  2.004 ms including view publication; this is not a large-city performance bound.
- Physical projection, client play, Paper lifecycle events and large-city profiling
  remain unverified. Phase 1 remains nine of ten complete milestones (90%).

Source/config/world files and history were preserved. Only the marked disposable
server DB advanced/migrated; automated tests use temporary databases.
