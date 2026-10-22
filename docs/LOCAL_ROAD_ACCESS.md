# Bounded local road-access planning

Status: implemented read-only planner and operator preview. This does not enable automatic
roads or relax the current requirement that home sites touch an existing path.
The preview itself makes no supplier payment, labor grant, land lease or world
modification. The separate schema117 backend now records exact paid road intents.

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

## Read-only operator preview

Use /civ build access while standing at the intended ground level and facing a
possible connection point. It checks a point eight blocks ahead, in a17x17 flat
frame at one block below your feet, with two clear cells above. Hills/water and
unknown terrain do not count as flat usable access. The player/entity itself can
block a corridor; move away from the point being inspected if necessary.

Permission is civilizations.construction.preview (operators by default). It shares
the existing eight-request/per-player limits, global64 cell reads per tick and
30-second lifetime with template previews. World/permission/disconnect are checked
before reads/results. No chunk loading. Actual saved protection comes from the SQL
owner future; failure or missing protection holds the result. Entity evidence is
capped at128. Output gives status, search nodes, new ground cells and endpoints,
not a material quote, native road design or reserved permission. The terrain is a
series of observations, so new work must independently inspect it again. Unknown
or missing evidence cannot become READY. Existing construction settings still
govern the separate automatic home builder.

## Frozen physical road designs

LocalRoadDesign now converts a complete flat READY corridor into the existing
ConstructionTemplate format, plus its ground origin, existing-road connection and
source-recipe fingerprint. This backend does not run automatically. It does not
change the read-only access preview's geometric output into a paid quote.

Use the existing editable starter_path.yml as the default recipe: five cobblestone
surface cells, two abstract stone units and five work units. Scale each material
and work separately by new cells / recipe-authored cells, rounding upward once per
project. Eight new cells therefore need four stone units and eight work. This is
the approved material abstraction, not one physical block per economic unit. The
existing connecting path is not rebuilt or charged as new surface. Admin changes
to the source recipe apply to future designs; source files are preserved and
normal configuration changes require restart. No new settings framework.

Recipes must be PATH, one flat stage and one supported solid surface throughout:
cobblestone, stone, stone bricks, andesite, diorite, granite or cobbled deepslate.
Decorative, mixed-state, multi-stage or sloped custom schematics cannot be silently
flattened into this prototype recipe. Positive bounded existing material/work
rules still apply. A complete route must contain unique flat cardinal neighbors,
at most64 new cells and exactly one final existing-road endpoint. A zero-new-cell
connection returns no design; rejected/partial routes never produce a blueprint.

Only actual new corridor cells are authored, so bends do not fill their bounding
rectangle. The ground-level road anchor records the retained endpoint and facing.
The generated template/cost/work can be frozen in ConstructionDraftRepository;
temporary SQLite verification proves exact restart persistence, no site/orders/
block intents created by saving, and refusal to overwrite that ID after recipe
edits. The source fingerprint is returned with the design; this does not add a
new persisted recipe-provenance table or save migration.

The physical design targets the ground surface. Schema117 now supports exact paid ground intents; the current native writer
still requires AIR and therefore cannot execute these road designs on ground. Exact natural-ground replacement, its versioned migration,
current corridor protection, paid effort/supplies and saved road completion remain
the next integration step. Do not weaken the writer to make a frozen plan run.

## Exact before-state review

Exact road-ground evidence review now preserves each proposed cell's actual natural before-state and requires clear headroom and the retained dirt-path connection. Changed/player-like blocks, protection, missing cells and stale/wrong-world evidence refuse the whole before-state map. This is read-only domain preparation, not land provenance, entity clearance or replacement authority. That read-only review checkpoint changed no journal; schema117 now adds typed
paid SQL intents, while the native AIR writer remains unchanged. Focused clean Wrapper verify5 unit+3 packaged passed26.981s. No native test needed: no Paper calls, database or world mutations. P4 remains5/16=31%; next is transactional versioned ground-state journaling and paid corridor integration. No decisions required.

The retained endpoint also needs both clear headroom cells and protection checks
through that space; otherwise the exact-ground review refuses the whole proposal.

## Schema117 paid SQL intents

ConstructionRoadIntents prepares only exact frozen connected local-road geometry,
with current world/site/protection, earned public work and actually delivered
escrowed goods. It checks saved protection at the retained road endpoint even if
an earlier native observation claimed it was clear. Complete fresh observations
must preserve all remaining natural cells, two clear headroom cells and the old
path connection. Acknowledged prefix must still match exact native output.

construction_road_ground stores each exact natural before-state and observation
time. Receipt plus ordinary journal intent commit atomically, with deferred FK,
immutable history and a trigger against unproved non-AIR insertion. Earlier
ordinals must be acknowledged. Replaying an accepted operation returns history;
it is not a new native permit, refund, material source or labor credit. Late SQL
failure rolls both rows back. Generic home preparation still records AIR only.

The migration preserves old journal identities/states and its authored-position
index. Populated116 upgrade/reopen and injected late rollback passed. Current
PaperConstructionWriter still rejects non-AIR, so these intents are not automatic
road construction. Native scene/proximity/entity review, gated one-time exact
replacement, corridor scheduling/funding and saved road completion remain next.

## Dedicated native boundary, not automatic scheduling

PaperRoadWorkSurvey now collects two exact loaded-only passes under a caller
budget. It covers configured construction clearances: ground/first-air layer in
the buffer plus both walking-headroom cells over every planned cell/endpoint.
Neighbor structures/fences and changed states hold. All block/entity chunks in the
entity census bounding box must already be loaded, even in a bent route's interior.
No chunk loading. The complete scene must fit255 cells; an atomic write/post-check
fits the existing256-read cap. Large custom route/buffer combinations wait.

prepareAuthorizedRoadStep repeats current SQL paid/material/labor/site/protection
checks and captures the existing owner gate. Historical receipt replay is not a
native permit. PaperRoadWriter rechecks exact plan/ordinal/states, full scene and
entities under that gate, then consumes a one-time newly prepared intent proof
before replacing only its original natural ground. It uses no mining/drop API.
An exact after-state acknowledges without writing. Missing certainty after restart
holds original ground; no free repair or replacement. Home writer stays AIR-only.

Actual disposable Paper paved two paid stone-brick cells, held a changed buffered
neighbor and real entity, recovered lost acknowledgement across actual session
restart, and held a separate prepared-before-write restart as ambiguous. Original
world cells restored and both117 saves healthy. Final25 unit+3 packaged checks
passed. Seven solid recipe surfaces are now supported by the native catalogue.

This completes the path/home example-world acceptance item, not normal automatic
road growth. Completed-road provenance/registration, home-connector recognition
and ordinary demand/preparation/supply scheduling remain next. No instant paving
command was added; /civ build access is still read-only. Regional routes remain P5.

Saved protection checks also cover every required external ROAD anchor during
initial site reservation, current paid/native work review and home completion.
Only the actual footprint and required access columns are screened; unrelated
corners of the enclosing rectangle do not exclude a site. Access is screened,
not added to the project's reserved footprint. A historical reservation replay
does not refresh permission after protection changes. No schema change.

## Permanent completed-road provenance (schema118)

CivRoadCompletion now registers a stable UUID from world/draft identity, original
village/frozen plan and exact paid ground-journal cells. Every block must already
be APPLIED with real supplies/earned work, and a new complete current native scene
must still match the output, walking headroom and configured clearances. Current
saved protection overrides an older clear frame. Origin/cells/site release are
one transaction; replay is a receipt, never repair authority. Immutable history
continues excluding old road ground from new projects after physical damage.

expectedStates returns bounded positive saved provenance, not a claim that the
blocks are presently there or safe. Callers must match current exact world states
and protection. No automatic road ordering or home connector recognition yet.
24 unit+3 packaged checks and actual Paper completion/restart passed; both118
saves healthy, original fixture cells restored. Regional routing remains Phase5.
