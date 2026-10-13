# Read-only water snapshot evidence

WaterScan is a bounded plain-Java full-height scan of one immutable chunk snapshot.
PaperWaterSource reads block types only, using absolute world Y coordinates. The
caller must capture an already-loaded chunk on Paper's main thread and provide the
world's full minimum/maximum height, not a village-height slice. Neither class
loads chunks, changes blocks/entities, opens a database or starts a business.

Each advance call reads at most its requested number of cells. All 256 columns
are scanned through the complete supplied height range. The result retains world,
chunk, capture time, height bounds, caller-selected minimum depth and each column's
position/status/suitable surface Y. Evidence is immutable; unfinished scans cannot
produce observations. Negative coordinates/heights use the same absolute identity
as saved fishery supports. A snapshot is historical evidence at capture time, not
proof that a block has not changed since capture.

The current conservative read-only classifier requires an exposed topmost water
surface and consecutive water depth. Roofs/ice/leaves/lily pads are OTHER; cave
water beneath a solid surface is excluded. Waterlogged slabs are not a water block.
An unknown cell anywhere makes its whole column UNKNOWN, even when other cells
look suitable. Complete empty results mean no qualifying columns in that snapshot;
they do not by themselves retire a fishery. Saved habitat loss still requires fresh
complete evidence for every original support through FisheryRepository.habitat.

Live PaperWaterObserver now captures only already-loaded nearby chunks on the
main thread, traverses their full height incrementally, and submits immutable
observations to the existing SQL owner. One capture/scan or pending submission is
active at a time. It never changes blocks, reads inventories or loads chunks.
Unloading during traversal invalidates that capture as UNKNOWN, not habitat loss.
Diagnostics are included in `/civ debug observation`; submission failures are logged.

The optional `water-observer` economy.yml section controls enabled, minimum-depth
(default2), cells-per-tick(default4096, maximum4096), interval-ticks(default400,
minimum20), and horizontal-radius-chunks(default1, maximum2). Missing old sections
remain disabled; installed files are never rewritten. Restart after editing.
Scanning is centered on one deterministic saved household home per settlement,
with at most1,024 settlement centers. Full-height scanning costs256 times world
height reads per chunk; capture/SQL waiting can make total sweep time longer than
the configured interval. No nearby-player requirement or production chunk ticket
is added. Wider footprints and performance profiling remain future work.

Schema68 adds initially empty water_surveys/water_columns and an index. Complete
newer captures replace only their chunk's current positives. Partial captures
retain old columns but invalidate eligibility. Older captures cannot overwrite
newer evidence; equal timestamp replay is exact after canonical ordering, and
conflicts reject. World UUID, height bounds, depth, capture time and fingerprint
are saved together in an owner transaction; late failure rolls back everything.
Current opportunity review excludes future/stale/partial/depth-mismatched evidence,
absent/unknown worlds, other towns' fresh conflicting columns and original fishery
claims. Exceeding the requested candidate budget returns no partial selection.

Evidence grants no land, ownership, fish stock, skill, funding or business. The
classifier does not distinguish flowing/still water or historic block origin.
Contiguous site selection, fresh live revalidation, saved recognition terms for
habitat transitions, qualified founders/training, actual customers and protected
atomic founding still remain. These observations do not automatically apply
habitat loss to existing fisheries, whose original depth terms must be respected.

The user approved counting sufficiently large/deep player-built ponds alongside
natural water under editable requirements, without property claims or block changes.
Automatic startup remains unfinished, not awaiting that decision. See
DECISIONS_AND_OPEN_QUESTIONS.md.

Eight added unit/adapter cases and focused38 tests passed with zero failures/errors/
skips (19.764s, October12 17:38:13 ET). Full Windows .\mvnw.cmd clean verify passed
1,229 (1,227 unit/two packaged integration), zero failures/errors/skips,6:37 at
17:46:19 ET. The guarded native probe passed at17:48:52: 98,304 actual snapshot
reads matched 256 independent surface/depth checks, finding220 suitable columns,
with512 cells/tick. No helper block/entity changes. A fixture-only capture ticket
is allowed if the marked disposable world has no loaded chunk; released immediately,
not production simulation behavior. Normal shutdown completed at17:48:53.

Read-only save audit:108/115 old tables exact; seven permitted clock/mask/ore,
resident-age-only and farm-freshness changes; identities/assets/cash/history
preserved, fishery/public-order tables empty, schema67 integrity/FK healthy.
Automatic approval review initially reached its usage limit before cleanup, so
that mutation was deferred without bypassing review. On user resume the approved
audit/cleanup succeeded: exact original properties restored, helper archived,
consistent backups retained and fixture stopped. Human playable server untouched;
no merge/push. The temporary execution-capacity blocker is resolved.

## Live evidence verification — October12

Focused70 tests passed, zero failures/errors/skips,27.880s at19:54:03 ET.
Full Windows .\mvnw.cmd clean verify passed1,238 (1,236 unit/two packaged
integration), zero failures/errors/skips,6:58 at20:01:16 ET. Guarded native
PaperWaterObserver/normal SQL-owner check passed20:02:20:220 actual suitable columns
matched independent snapshot reference, newer capture/absence/restart preserved,
no invented businesses/resources or helper block/entity changes. Normal shutdown
20:02:21. Main-save audit111/115 old tables exact; four ordinary clock/economy/
farm-freshness/ore changes, all identities/assets/cash/history preserved; schema68
integrity/FK healthy, new water and fishery tables empty. Exact original settings
restored, helper archived, backups retained; human server untouched.
