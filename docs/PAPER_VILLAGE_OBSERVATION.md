# Loaded-world village observer prototype

`VillageObserver` runs only on Paper's main thread after PopulationSession startup.
It enumerates already loaded chunks and checks `isLoaded`/`isEntitiesLoaded` before
entity access: Paper's `Chunk.getEntities()` would otherwise load entities. No
chunk ticket, forced chunk, POI search, block write or entity mutation is used.

Loaded villager chunks seed observation windows. Default radius four chunks gives
a 9x9 window. Missing chunks are skipped rather than loaded. Each frame captures
at most one read-only chunk snapshot/tick and scans it incrementally: at most
4,096 block reads/tick, checking elapsed time every 64 reads against a 2 ms target.
Empty sections skip without block reads. Each physical bed contributes only its
HEAD half. Paper 26.2-129's empty-section API indexes the snapshot array from zero;
the adapter translates world section Y using the world's minimum height.

Entity capture checks at most two inspected chunks/tick and deduplicates source
UUIDs. Immutable observations alone go to the SQL owner. Names, age classification,
world UUID, bed coordinates and entity UUIDs provide enrollment evidence. Repeat
visits use the existing transactional identity/import rules, never a new region ID
or replacement loaded-population census.

Completeness requires every window chunk available, entity data loaded, chunks
still loaded at completion, and a clear border wider than the maximum bed-connection
and resident-matching radii. A connected village reaching the border is partial.
Partial frames may import positive identity evidence, but housing stays unknown.
Homes/people outside the frame are never treated as zero or deleted. Frames above
the existing 512-bed/256-villager defaults reject rather than silently truncate.

Main-thread discovery refreshes its bounded seed list every five seconds when idle,
checks up to two chunks/tick and retries the same villager chunk after 60 seconds.
The retained seed list and cooldown map cap at 4,096 chunks. Only one frame and one
submitted import are in flight. `/civ debug observation` reports scan progress,
complete/partial quality or deferral; creation/new import IDs are logged on the main
thread. Config exposes radius, read/time budgets and refresh interval; older configs
without the observer section retain defaults, while an explicit invalid section fails.

## Limits and remaining work

- Snapshots span multiple ticks; this is timestamped observational evidence, not an
  atomic live-world census. Nearby edits/moving entities can require a later revisit.
- The time target cannot preempt one Paper snapshot/entity enumeration/API call.
  Loaded-chunk enumeration and seed coverage at the 4,096-chunk ceiling need profiling
  and fair coverage work before large-server claims. No guaranteed 2 ms hard deadline.
- A large village or incomplete window may keep housing unknown; administrators can
  tune the observation radius within bounds. No forced loading to obtain certainty.
- Entities are only read in this batch. Naming/aging/birth/death/migration projection,
  native villager lifecycle coordination and context-sensitive density remain pending.
- Legacy unbound settlement rows still require explicit identity adoption; no guessed
  world binding, database reset, implicit household/town merge or block-edit consent.
- No construction, economy, jobs or template work is introduced here.
