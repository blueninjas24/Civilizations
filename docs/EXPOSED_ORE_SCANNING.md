# Exposed ore prototype evidence

The user approved "Allow exposed existing ore" on October 9, 2026. Pre-existing
coal/iron with unknown natural/player origin can supply finite resource evidence.
Newly tracked player ore placements must be excluded. Discovery grants no rights
to remove blocks or modify player builds; extraction remains abstract in Phase 3.

## Implemented scanner

OreScan is plain Java and reads an immutable Source. Each advance visits at most
the supplied cell budget and makes at most seven source reads per cell. Traversal
is deterministic, accepts negative world heights and converts chunk-local X/Z to
permanent absolute coordinates. It copies the caller's exclusion set.

Coal/iron counts only when at least one of six face neighbors is known air. Water,
lava, diagonal openings and other minerals do not count as exposure. Known air is
sufficient even when another neighbor is unavailable. Unknown cells or ore with
uncertain exposure invalidate the survey. Buried ore with six known non-air faces
is ignored. Excluded coordinates cannot supply evidence.

At most 512 total exposed positions are retained across coal and iron. Overflow
marks both observations incomplete, preventing a truncated survey from creating
reserves. Finished traversal and reliable completeness are different concepts.
Unfinished publication fails explicitly. Empty evidence does not mean a saved
deposit has depleted. MineralDiscoveryRepository rejects partial/empty discovery.

PaperOreSource classifies normal/deepslate coal and iron and all air variants from
a ChunkSnapshot using absolute Y. Out-of-chunk or out-of-world neighbors return
UNKNOWN without a snapshot read. Callers must capture already-loaded chunks on
Paper's main thread. The adapter has no World access, chunk-loading API or writes.
Cell budgets bound calls, not a hard CPU deadline or snapshot-capture time.

## Remaining runtime work

OreScan provides scanner components, not automatic mining. No ore observer or mine
scheduler is registered. Durable placement records and native live capture with
a clean-stop/failure fence now exist (see LIVE_ORE_TRACKING.md). Conservative piston
exclusions are verified; bulk-editor coverage and the actual discovery/operation bridge remain unfinished, so automatic
ore opportunities remain disabled.

A chunk survey is not a geological deposit. OreClusters now supplies conservative
face-connected observed patches (RESOURCE_PATCHES.md). The next adapter must form
bounded opportunities, resolve saved coordinates/sites first and reject conflicts.
Different sides of a chunk can contain unrelated resources; do not pass arbitrary
chunk-wide observations directly to discovery as one deposit. Boundary uncertainty
must never be repaired by forcing neighboring chunks to load.

Permanent coordinate claims and saved depletion remain schema 32. This batch needs
no database migration and changes no production save, world, owner configuration
or mine yields. Repeat scans must use the existing no-refill discovery path.

## Verification

Domain tests cover bounded reads, batch-size independence, buried/exposed ore,
negative coordinates/heights, copied exclusions, immutable output, missing data,
border exposure, mixed-mineral overflow, the exact 512 boundary, empty evidence,
retry after read failure and invalid/unfinished requests. Snapshot-adapter tests
cover both ore variants, all air variants, fluids/unsupported ores, absolute
negative Y and unavailable-boundary reads. These are automated component checks;
the initial component batch did not run a native Paper scan. The subsequent
RESOURCE_PROVENANCE.md fixture verified real snapshot exposure/exclusion after
restoring all test blocks; live automatic discovery remains unfinished. Windows Wrapper
`.\mvnw.cmd clean verify -B -ntp` passed 597 tests (595 unit/two packaged integration),
zero failures/errors/skips, including eleven new scanner/adapter cases.
