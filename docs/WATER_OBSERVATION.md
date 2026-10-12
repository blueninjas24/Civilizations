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

Minimum depth is an explicit caller argument (1–64); tests use two. No installed
admin file is rewritten and no automatic survey policy/default is enabled. The
classifier is evidence infrastructure, not a locked ecological rule or certification
of player ponds. It does not distinguish flowing/still water, infer historic block
origins or infer property ownership. Contiguous site selection, multi-chunk quality,
freshness/revalidation, editable live survey settings, SQL-owner submission,
qualified owners/training and protected atomic founding remain future wiring.

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
