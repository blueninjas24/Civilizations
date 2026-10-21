# Bounded local road-access planning

Status: implemented read-only domain preparation. This does not enable automatic
roads or relax the current requirement that home sites touch an existing path.
No command, SQLite migration, supplier payment, material estimate, labor grant,
land lease or world modification is introduced.

LocalRoadAccessPlanner finds a shortest flat one-column access corridor from a
specified ground surface to a verified existing road within a supplied frame.
It reuses ConstructionSitePlanner.Survey and ConstructionLandPolicy classification;
there are no Paper types or direct block/chunk reads. The caller must provide
complete recent world-matching evidence and complete protection information.

Ground must be stable and classified as natural-looking ground or existing road.
Two clear cells above every step are required. Protected cells/areas and observed
entities block passage. Arbitrary player-built stone is not assumed to be a road.
No obstacle clearing, slope excavation, bridge, water crossing or property takeover.

A bounded breadth-first search gives the shortest known safe connection; fixed
seed-based neighbor order resolves equal-length alternatives reproducibly. Result
contains an immutable start-to-existing-road corridor and counts only non-road
surface cells. These counts are geometry, not commodity units or a priced quote.
An already connected start has zero new surface cells. No partial route is READY.

Prototype policy: at most16 new surface cells and1024 dequeued search nodes.
Explicit policy bounds are1..64 new cells and1..4096 reviewed nodes. The supplied
flat frame is at most4096 ground cells; its entire three-layer evidence is checked.
The fixed preflight is separate from the search-node budget. Missing, contradictory,
stale, wrong-world or unknown-protection evidence refuses. Exhausted distance/search
allowances report BUDGET_LIMIT. No qualifying road reports NO_ROAD; an unreachable
road reports NO_SAFE_ROUTE. Terrain/configuration changes require fresh evidence.

The result is not a construction permission. Next integration must reserve the
actual corridor, choose/freeze an authored physical road design, buy real supplies,
book actual labor and use the existing journal/native guards. Support for replacing
natural ground must be explicit at that future write boundary; this planner cannot
turn a clear-looking ground observation into a block replacement. Completed Civ
road provenance must also feed future connectors without adopting arbitrary
player paving. Regional routes/trade stay Phase5.
