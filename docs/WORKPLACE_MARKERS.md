# Existing workplace marker observation

The user approved configurable existing barrels and smithing tables as data-only
workplace markers before Phase 4 construction. A barrel is potential warehouse
evidence; a smithing table is potential blacksmith evidence. Neither block grants
property rights. Recognition never opens its inventory, consumes its contents,
moves it, or creates business stock, money, skills or employment.

## Settings

Bundled `economy.yml` enables `workplace-observer` with these editable defaults:

```yaml
workplace-observer:
  enabled: true
  vertical-radius: 16
  cells-per-tick: 512
  interval-ticks: 400
  horizontal-radius-chunks: 1
  markers: [barrel, smithing_table]
```

Missing sections in existing files remain disabled; no owner configuration is
overwritten. Select either or both supported marker types. These settings control
observation, not granting a business permission to operate. The SQL-owner review
also applies the currently enabled types so disabled types are not offered from
retained sightings.

The scanner uses one representative saved home per town and bounded nearby loaded
chunks (default a 3-by-3 ring). It captures a snapshot on Paper's main thread and
traverses at most the configured cells per tick, visiting each cell once. It caps
markers at 512 per chunk and villages at 1024 per observation queue. It cannot
force-load a chunk. Unloaded chunks are skipped; unloading during traversal submits
incomplete evidence, not a trustworthy empty result. `/civ debug observation`
includes its diagnostic. This is not exhaustive village-wide building inspection.

## Persistence and candidate safety

Schema 58 adds three empty tables: per-village/world/chunk latest capture headers,
current marker membership, and retained positive sightings with first/last times.
Upgrade, version and validation are transactional; no old block history is invented
and existing businesses/assets remain unchanged.

A newer complete frame replaces only that chunk's current membership. Empty frames
clear current candidates while keeping sightings. Partial frames make that chunk
ineligible without deleting its previous membership/history. Older frames cannot
restore candidates; conflicting evidence at the same timestamp is rejected. Each
candidate remains tied to the permanent world UUID and its captured block position.

Reviews exclude absent worlds, future/stale/partial captures, current overlapping
town observations, and coordinates already saved as business sites or beds. They
defer if the candidate budget is exceeded instead of silently using a partial list.
No candidate is an ownership/occupancy claim. Before automatic founding, adapters
still need real demand, qualification, funding, site checks and a fresh main-thread
block revalidation. This batch does **not** automatically found warehouses or
workshops, train first owners or perform physical construction.

## Verification

Thirteen automated tests added. Focused tests: 38 passed. Windows
.\mvnw.cmd clean verify: 1,003 passed (1,001 unit and two packaged integration),
zero failures/errors/skips, 4:46. A test initially expected the wrong exception
for coordinate overflow; corrected to match exact arithmetic, then verified.
WorkplaceMarkerProbe compiled and passed on the marked disposable Paper server:
scheduled scanner and SQL owner recognized both markers; three wheat in the test
barrel remained untouched; complete empty re-observation and restart preserved
history without recreating candidates. Both temporary test blocks were restored,
and the helper released its temporary test-only chunk ticket.

Server stopped normally; original properties restored byte-exact; helper archived.
All 96 original database tables compared: 92 unchanged; four differences were
clock/food-clock timestamps, current farm evidence and ore-trust bookkeeping.
No calendar month or food period advanced; original business assets, finances,
residents, skills, courses and history were unchanged. New main-fixture workplace
tables stayed empty under legacy opt-out. Human server untouched; no push/merge.

October 12 update: marker removal policy is now approved: pause until restored at
the saved spot. The explicit marker startup and point-confirmation seams are
described in WORKPLACE_OPERATIONS.md; automatic founder selection/training remains.
