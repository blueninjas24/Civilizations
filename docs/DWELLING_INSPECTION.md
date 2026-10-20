# P4 bed and access inspection foundation

Implemented components: HomeBuildingLayout, DwellingInspection and the incremental
PaperDwellingSurvey adapter. These assess declared layouts; they do not discover
apartments, grant consent, register occupancy or add settlement housing capacity.
/civ building inspect <building UUID> now schedules read-only diagnostics for
already registered housing. No automatic simulation task activates housing.

## Authored home declarations

The existing single-family home plan becomes one explicitly identified unit with
four declared bed pairs, not four apartments. Its draft UUID identifies the proposed
building and deterministically derives the unit UUID. Rotation/mirroring transforms
entrances, bed feet and facing together; plot and physical structure remain distinct.
Non-home plans, invalid authored bed pairs, ambiguous primary entrances and plots
too small to contain the home are rejected. This conversion does not complete a
project or give it ownership or capacity.

## Current immutable evidence

DwellingInspection accepts a complete bounded structure observation tied to the
permanent world UUID and exact layout fingerprint. Missing/unknown cells, wrong
worlds, future timestamps, changed layouts and stale observations yield no assessed
units. This is an unverified assessment, not evidence that a saved home disappeared.
Callers must retain previous state when evidence is incomplete.

For each explicitly declared unit, check actual bed foot/head material and facing,
support, clear headroom, no extra undeclared private bed halves, and access to its
entrance and beds. Only that unit's private space and the building's shared space
are traversable during its review. A route across another family's apartment is
rejected. One unit's broken bed need not disable a different valid unit.

Routes require supported two-block-high standing space and cardinal steps of at
most one block with head clearance. No vertical flying or diagonal corner edges.
The shared path-visit budget is262,144 per assessment; a budget-exhausted unit is
explicitly unverified. The result reports accessible bed pairs, not legal or fully
habitable village capacity. Overhead cover is now checked by default. Full enclosure quality, broader building condition,
permission, occupancy and current protection remain separate gates.

## Paper read boundary

PaperDwellingSurvey requires the main server thread. A caller advances it with
at most256 cell reads per call; it has no hidden recurring task or per-player
multiplier. Future orchestration must share a global tick budget. The constructor
uses a coordinate cursor rather than allocating/sorting every world cell on the
server thread. The pure assessment can run outside the Paper thread afterward.

The survey reads only already-loaded chunks in the named present world, within
height and border limits. Missing chunks/worlds and expiry produce incomplete
evidence. It neither force-loads chunks nor modifies blocks/entities. Completed
observations are immutable and exact retry returns the same observation.

Native classification is deliberately narrow: air and wooden doors can provide
access; actual beds retain material/part/facing; safe non-gravity full occluding
blocks provide support. Known damaging blocks and waterlogged shapes are unsafe.
Safe stairs/slabs and full glass blocks provide roof-only cover evidence. They do
not become traversable standing routes. Vegetation, fluids and non-wooden doors stay blocked.
Native stair/slab geometry needs a later collision-shape extension; the current
domain route proof supports ordinary full-block steps. This may reject a usable
building, so an unsuccessful review is not demolition or ownership evidence.

An incremental scan is not an atomic world snapshot. A future occupancy/writer
adapter must detect intervening edits and revalidate before activation. Nothing
in this batch enables a permission or physical housing writer.

## Native check

src/test/paper/DwellingInspectionProbe.java is restricted to a marked, empty,
isolated loopback server. It stages an original four-bed home in a disposable
footprint to exercise real block classification; no paid construction or donated
housing is claimed by this fixture. It checks a broken bed half, absent worlds
and unloaded targets, restores its original cells and stops the private server.
See DEVLOG.md for actual run results; source availability is not a passed run.

## Verified October20

Ten new JUnit checks cover the inspection and authored-home bridge, including all8
orientations, separate private access, real paired beds, supported upstairs routes,
per-unit damage, incomplete/stale/wrong-world evidence and extreme coordinate/time
budgets. The final focused Wrapper clean verify passed49 unit +3 packaged checks
in26.678s. The preceding schema108 checkpoint full build passed2096 +3 in16:19.

Actual checksum-verified Paper26.2 build129 passed the restricted probe: four real
bed pairs with a closed wooden doorway; removing a real bed head was detected;
remote chunks remained unloaded; absent worlds stayed incomplete. Restored144
original cells, stopped the private server and archived its helper. Read-only
post-stop audit found schema108 integrity/FKs healthy and empty building tables.
No donation, occupancy, population capacity or earned construction was activated.

## Read-only inspection command - October20

PaperDwellingReview queues at most4 requests, one per actor. Inspection requests share
one128-cell advance per tick, never a per-player multiplier. A request expires
at30 seconds; disconnect, world changes and permission loss discard pending output.
Only the saved donor or a current building administrator may inspect. Registry
lookup and bounded pure path review run on PopulationSession's owner worker;
Paper reads and current player facts stay on the server thread. No chunk is loaded.

The report gives aggregate accessible beds and up to8 individual-unit results,
with readable bed/support/headroom/entrance/path reasons. Missing world/chunk/cell
evidence is unverified; no result mutates consent, occupancy or village capacity.
The existing building protection permission grants access only subject to saved
ownership, not permission to inspect another player's home. Read-only review is
also allowed after reclamation; this does not reinstate donation rights.

This is for saved layouts, not arbitrary beds or a new donation workflow. Existing
servers without registered layouts receive an explicit unknown-building response.
Covered beds are approved and now checked; full enclosure, edit-aware revalidation
and live assignments remain ahead.
Focused Wrapper clean verify passed47 unit +3 packaged checks in30.231s, zero
failures/errors/skips. Four new adapter checks cover queue/global budget, current
authority/world/disconnect/expiry/close, partial evidence, failures and diagnostic
results. Expanded temporary-SQLite owner checks verify saved donor/admin lookup,
wrong world/unknown identity refusal and read-only assessment preserving consent.
No new native Paper run was performed for this orchestration batch.

## Covered sleeping areas and explicit home declarations

DwellingInspection.Policy records whether overhead cover was required in each
assessment. Default true: both halves of every declared bed need a safe cover
cell at least two blocks above the bed's block, inside the surveyed structure.
A single bounded pass indexes cover columns. The floor of a room above can also
provide cover. Leaves, hazards, waterlogged shapes and unknown blocked cells do
not. Safe solid blocks and roof-only glass/stairs/slabs can qualify. Cover is not
proof of complete walls, weather simulation or occupancy authority. Setting false
still requires real paired beds, headroom, support and reachable private access.

HomeDeclaration.singleHousehold is an explicit one-home operation: complete,
fresh region evidence tied to world/selection -> stable one-unit geometry -> exact
layout-bound inspection. It never guesses apartments or splits one home per bed.
PaperDwellingSurvey.forHomeDeclaration collects the same bounded loaded-only
region without a pre-existing bed declaration. All cells, floor and roof must lie
inside the selected structure; no distant roof or incomplete scan can fill gaps.

PopulationSession.previewSingleHomeDonation runs that review on the SQL owner,
requires a village in the saved present world and a permitted caller, and refuses
failed inspection before issuing the existing player-bound consent preview.
No building is registered by preview. Existing explicit ownership confirmation
remains necessary. These APIs now have the explicit single-home controls described in HOME_DONATIONS.md.
Edit-aware capacity activation and household assignment remain unfinished.

Final covered-declaration verification: focused Wrapper clean verify51 unit
+3 packaged checks passed30.476s, no failures/errors/skips. This includes two
new declaration scenarios, one temporary-SQLite end-to-end preview/explicit save
scenario, covered-bed per-unit/override coverage and strict configuration checks.
The first helper compile had an ambiguous Java Policy import; narrowed imports.
The initial test fixture had only one villager, below existing recognition rules;
corrected to two adults without changing application recognition behavior.

Actual Paper26.2 build129 passed missing native bed/roof refusal, glass/stair cover,
waterlogged refusal, explicit outdoor override and raw-region conversion to one
stable four-bed family declaration. Remote chunks stayed unloaded; absent worlds
incomplete. Restored144 original cells, stopped the marked private server and
archived its helper. Read-only post-stop database integrity/FKs were clean at108.
No user-server save, housing capacity, inventory or real world was modified.

Latest controls: inspection reports now use128 cell reads/tick, reserving128 for
new home donation scans; combined live housing maximum256 per tick. Saved home
permission can be registered through HOME_DONATIONS.md; inspection alone still
changes neither consent nor capacity. No automatic household occupancy is enabled.
